package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.example.toolbox.AppJson
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 机器人 WebSocket 管理器
 * 支持：自动重连、心跳保活、事件分发
 */
class BotWebSocketManager(
    private val token: String,
    private val onEvent: (JsonObject) -> Unit,      // 收到原始事件
    private val onStatusChanged: (Boolean) -> Unit, // 连接状态变化 (true=已连接, false=已断开)
    private val onError: (String) -> Unit           // 错误信息
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)  // OKHttp 自动发送 Pong 帧保活
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    @Volatile private var isConnected = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 20
    private val baseReconnectDelay = 1000L
    
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 心跳线程（发送自定义 ping）
    private var heartbeatThread: HandlerThread? = null
    private var heartbeatHandler: Handler? = null
    private val heartbeatInterval = 30000L // 30秒发送一次心跳
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 心跳任务
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (isConnected && webSocket != null) {
                try {
                    val heartbeatMsg = buildJsonObject {
                        put("type", "ping")
                        put("timestamp", System.currentTimeMillis())
                    }.toString()
                    webSocket?.send(heartbeatMsg)
                    Log.d("BotWS", "Heartbeat sent")
                } catch (e: Exception) {
                    Log.e("BotWS", "Heartbeat send failed", e)
                }
            }
            heartbeatHandler?.postDelayed(this, heartbeatInterval)
        }
    }
    
    fun connect() {
        if (webSocket != null) {
            Log.d("BotWS", "Already connecting or connected")
            return
        }
        
        ensureHeartbeatThread()
        
        val request = Request.Builder()
            .url("wss://ws.jwzhd.com/subscribe?token=$token")
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                reconnectAttempts = 0
                Log.d("BotWS", "WebSocket connected")
                
                // 启动心跳
                heartbeatHandler?.post(heartbeatRunnable)
                
                mainHandler.post {
                    onStatusChanged(true)
                }
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = AppJson.json.parseToJsonElement(text).jsonObject
                    Log.d("BotWS", "Received: ${text.take(500)}")
                    
                    mainHandler.post {
                        onEvent(json)
                    }
                } catch (e: Exception) {
                    Log.e("BotWS", "Parse message failed", e)
                    mainHandler.post {
                        onError("解析消息失败: ${e.message}")
                    }
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("BotWS", "Closing: $code $reason")
                webSocket.close(code, reason)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("BotWS", "Closed: $code $reason")
                isConnected = false
                heartbeatHandler?.removeCallbacks(heartbeatRunnable)
                
                mainHandler.post {
                    onStatusChanged(false)
                }
                
                scheduleReconnect()
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("BotWS", "WebSocket failure", t)
                isConnected = false
                heartbeatHandler?.removeCallbacks(heartbeatRunnable)
                
                mainHandler.post {
                    onStatusChanged(false)
                    onError("连接失败: ${t.message}")
                }
                
                scheduleReconnect()
            }
        })
    }
    

    private fun ensureHeartbeatThread() {
        if (heartbeatThread == null || !heartbeatThread!!.isAlive) {
            heartbeatThread = HandlerThread("BotWS-Heartbeat").apply { start() }
            heartbeatHandler = Handler(heartbeatThread!!.looper)
        }
    }
    
    private fun scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            Log.e("BotWS", "Max reconnect attempts reached")
            mainHandler.post {
                onError("已达到最大重连次数($maxReconnectAttempts)，请手动重启")
            }
            return
        }
        
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            // 指数退避：1s, 2s, 4s, 8s, 16s, 30s, 30s...
            val exponent = reconnectAttempts.coerceAtMost(5)
            val delay = baseReconnectDelay * (1L shl exponent)
            val finalDelay = delay.coerceAtMost(30000L)
            reconnectAttempts++
            
            Log.d("BotWS", "Reconnecting in ${finalDelay}ms (attempt $reconnectAttempts/$maxReconnectAttempts)")
            delay(finalDelay)
            
            withContext(Dispatchers.Main) {
                reconnect()
            }
        }
    }
    
    private fun reconnect() {
        if (isConnected) return
        
        webSocket?.close(1000, "Reconnecting")
        webSocket = null
        
        connect()
    }
    
    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        
        heartbeatHandler?.removeCallbacks(heartbeatRunnable)
        heartbeatThread?.quitSafely()
        heartbeatThread = null
        heartbeatHandler = null
        
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        isConnected = false
        
        mainHandler.post {
            onStatusChanged(false)
        }
    }
    
    fun isConnected(): Boolean = isConnected
}