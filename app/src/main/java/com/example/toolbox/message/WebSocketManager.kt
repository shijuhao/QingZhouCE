package com.example.toolbox.message

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.data.Message
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

class WebSocketManager(
    private val onMessageReceived: (type: String, message: Message) -> Unit
) {
    private var socket: Socket? = null
    private var currentToken: String? = null

    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (socket?.connected() == true) {
                socket?.emit("heartbeat", JSONObject())
            }
            heartbeatHandler.postDelayed(this, 30000)
        }
    }

    fun connect(token: String) {
        this.currentToken = token

        try {
            val opts = IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = true
            }

            // WebSocket地址
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
                Log.e("WS", "连接失败")
            }

            // 认证成功
            socket?.on("auth_success") {
                Log.d("WS", "认证成功")
            }

            // 认证失败
            socket?.on("auth_error") { args ->
                val message = (args[0] as JSONObject).optString("message")
                Log.e("WS", "认证失败: $message")
            }

            // 私信消息推送
            socket?.on("private_message") { args ->
                val json = args[0] as JSONObject
                val type = json.optString("type")
                val data = json.optJSONObject("data")?.toString()
                if (data != null) {
                    val message = AppJson.json.decodeFromString<Message>(data)
                    onMessageReceived(type, message)
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
        socket?.disconnect()
    }
}