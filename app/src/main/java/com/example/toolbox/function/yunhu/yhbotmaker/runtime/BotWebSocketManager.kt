package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import android.util.Log
import com.example.toolbox.AppJson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import java.net.URI
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
            BotWebSocketInstance(token, onEvent, onStatusChanged, onError).also {
                it.connect()
            }
        }.also { instance ->
            instance.updateCallbacks(onEvent, onStatusChanged, onError)
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
    private var socket: Socket? = null
    private var isConnected = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    fun updateCallbacks(
        onEvent: (JsonObject) -> Unit,
        onStatusChanged: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        this.onEvent = onEvent
        this.onStatusChanged = onStatusChanged
        this.onError = onError
        // 同步当前连接状态
        onStatusChanged(isConnected)
    }
    
    fun connect() {
        if (socket?.connected() == true) {
            return
        }
        
        try {
            val opts = IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = true
                reconnectionAttempts = 20
                reconnectionDelay = 1000
                reconnectionDelayMax = 30000
                timeout = 10000
            }
            
            val uri = URI("wss://ws.jwzhd.com/subscribe?token=$token")
            socket = IO.socket(uri, opts)
            
            socket?.on(Socket.EVENT_CONNECT) {
                isConnected = true
                Log.d("BotWS-$token", "Connected")
                scope.launch(Dispatchers.Main) {
                    onStatusChanged(true)
                }
            }
            
            socket?.on(Socket.EVENT_DISCONNECT) {
                isConnected = false
                Log.d("BotWS-$token", "Disconnected")
                scope.launch(Dispatchers.Main) {
                    onStatusChanged(false)
                }
            }
            
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                isConnected = false
                val error = args.firstOrNull()?.toString() ?: "Unknown"
                Log.e("BotWS-$token", "Connect error: $error")
                scope.launch(Dispatchers.Main) {
                    onError("连接错误: $error")
                }
            }
            
            socket?.on(Socket.EVENT_MESSAGE) { args ->
                args.firstOrNull()?.let { data ->
                    try {
                        val jsonStr = data.toString()
                        val json = AppJson.json.parseToJsonElement(jsonStr).jsonObject
                        scope.launch(Dispatchers.Main) {
                            onEvent(json)
                        }
                    } catch (e: Exception) {
                        Log.e("BotWS-$token", "Parse error", e)
                    }
                }
            }
            
            socket?.connect()
            
        } catch (e: Exception) {
            Log.e("BotWS-$token", "Connect exception", e)
            onError("连接异常: ${e.message}")
        }
    }
    
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        isConnected = false
        onStatusChanged(false)
    }
    
    fun isConnected(): Boolean = socket?.connected() ?: false
}