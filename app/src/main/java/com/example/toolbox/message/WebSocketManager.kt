package com.example.toolbox.message

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.data.Message
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URISyntaxException

object PrivateChatSocketManager {
    private var instance: WebSocketManager? = null

    fun getInstance(): WebSocketManager {
        if (instance == null) {
            instance = WebSocketManager()
        }
        return instance!!
    }

    fun disconnect() {
        instance?.disconnect()
        instance = null
    }
}

class WebSocketManager internal constructor() {
    private var socket: Socket? = null
    private var currentToken: String? = null

    private val heartbeatThread = HandlerThread("WS-Heartbeat-Private").apply { start() }
    private val heartbeatHandler = Handler(heartbeatThread.looper)
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (socket?.connected() == true) {
                socket?.emit("heartbeat", JSONObject())
            }
            heartbeatHandler.postDelayed(this, 30000)
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val observers = mutableListOf<(type: String, message: Message) -> Unit>()

    fun addObserver(observer: (type: String, message: Message) -> Unit) {
        observers.add(observer)
    }

    fun removeObserver(observer: (type: String, message: Message) -> Unit) {
        observers.remove(observer)
    }

    fun connect(token: String) {
        this.currentToken = token

        try {
            val opts = IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = true
            }

            val wsUrl = ApiAddress.replace("http://", "ws://").replace("https://", "wss://")
            socket = IO.socket("${wsUrl}?type=2", opts)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("WS", "连接成功")
                authenticate()
                heartbeatHandler.post(heartbeatRunnable)
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("WS", "连接断开")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) {
                Log.e("WS", "连接失败: ${it.firstOrNull()}")
            }

            socket?.on("auth_success") {
                Log.d("WS", "认证成功")
            }

            socket?.on("auth_error") { args ->
                val message = (args[0] as JSONObject).optString("message")
                Log.e("WS", "认证失败: $message")
            }

            socket?.on("private_message") { args ->
                scope.launch {
                    try {
                        val json = args[0] as JSONObject
                        val type = json.optString("type")
                        val data = json.optJSONObject("data")?.toString()
                        if (data != null) {
                            val message = AppJson.json.decodeFromString<Message>(data)
                            mainHandler.post {
                                observers.forEach { it(type, message) }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WS", "解析私信消息失败", e)
                    }
                }
            }

            socket?.connect()
        } catch (e: URISyntaxException) {
            Log.e("WS", "连接地址错误", e)
        }
    }

    private fun authenticate() {
        val authData = JSONObject().apply {
            put("token", currentToken)
        }
        socket?.emit("authenticate", authData)
    }

    fun disconnect() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        heartbeatThread.quitSafely()
        
        scope.cancel()
        
        socket?.disconnect()
        socket?.off()
    }
}
