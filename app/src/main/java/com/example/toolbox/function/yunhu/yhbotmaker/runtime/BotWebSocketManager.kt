package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.example.toolbox.AppJson
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

object BotWebSocketManagerSingleton {
    private val managers = ConcurrentHashMap<String, BotWebSocketInstance>()
    
    fun getInstance(
        token: String,
        onEvent: (JsonObject) -> Unit,
        onStatusChanged: (Boolean) -> Unit,
        onError: (String) -> Unit
    ): BotWebSocketInstance {
        return managers.getOrPut(token) {
            BotWebSocketInstance(token, onEvent, onStatusChanged, onError)
        }.also { instance ->
            instance.updateCallbacks(onEvent, onStatusChanged, onError)
        }
    }
    
    fun connect(token: String) {
        val instance = managers[token]
        if (instance == null) {
            val newInstance = BotWebSocketInstance(token, {}, {}, {})
            managers[token] = newInstance
            newInstance.connect()
        } else {
            instance.connect()
        }
    }
    
    fun disconnect(token: String) {
        managers.remove(token)?.disconnect()
    }
}

class BotWebSocketInstance(
    private val token: String,
    private var onEvent: (JsonObject) -> Unit,
    private var onStatusChanged: (Boolean) -> Unit,
    private var onError: (String) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    @Volatile private var isConnected = false
    private var isManualDisconnect = false
    
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    fun updateCallbacks(
        onEvent: (JsonObject) -> Unit,
        onStatusChanged: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        this.onEvent = onEvent
        this.onStatusChanged = onStatusChanged
        this.onError = onError
        onStatusChanged(isConnected)
    }
    
    fun connect() {
        if (webSocket != null) {
            return
        }
        
        isManualDisconnect = false
        
        val request = Request.Builder()
            .url("wss://ws.jwzhd.com/subscribe?token=$token")
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d("BotWS-$token", "Connected")
                mainHandler.post { onStatusChanged(true) }
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = AppJson.json.parseToJsonElement(text).jsonObject
                    mainHandler.post { onEvent(json) }
                } catch (e: Exception) {
                    Log.e("BotWS-$token", "Parse error", e)
                    mainHandler.post { onError("解析消息失败: ${e.message}") }
                }
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d("BotWS-$token", "Closed: $code $reason")
                mainHandler.post { onStatusChanged(false) }
                scheduleReconnect()
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e("BotWS-$token", "Failure", t)
                mainHandler.post { onStatusChanged(false) }
                scheduleReconnect()
            }
        })
    }
    
    private fun scheduleReconnect() {
        if (isManualDisconnect) return
        
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            reconnect()
        }
    }
    
    private fun reconnect() {
        if (isConnected || isManualDisconnect) return
        webSocket?.close(1000, "Reconnecting")
        webSocket = null
        connect()
    }
    
    fun disconnect() {
        isManualDisconnect = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "Manual disconnect")
        webSocket = null
        isConnected = false
        mainHandler.post { onStatusChanged(false) }
    }
    
    fun isConnected(): Boolean = isConnected
}