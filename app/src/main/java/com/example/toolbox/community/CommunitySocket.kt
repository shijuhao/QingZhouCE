@file:Suppress("PropertyName")

package com.example.toolbox.community

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.toolbox.AppJson
import com.example.toolbox.data.community.Message
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.serialization.json.Json
import org.json.JSONObject

class CommunitySocket(
    private val serverUrl: String = "wss://hehenya.dpdns.org:8505/",
    private val onStatusChanged: (Boolean) -> Unit, // 连接状态回调：true 成功, false 失败/断开
    private val onEvent: (type: String, msg: Message?, id: Int?, count:Int?) -> Unit
) {
    private var socket: Socket? = null
    private var currentToken: String? = null
    private var currentCategoryId: Int = 1

    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (socket?.connected() == true) {
                socket?.emit("heartbeat", JSONObject().apply {
                    put("client_time", System.currentTimeMillis().toString())
                })
            }
            heartbeatHandler.postDelayed(this, 30000)
        }
    }

    fun isConnected(): Boolean = socket?.connected() == true

    fun connect(token: String?, categoryId: Int) {
        this.currentToken = token
        this.currentCategoryId = categoryId

        try {
            val opts = IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = true // 开启自动重连
            }
            socket = IO.socket(serverUrl, opts)

            // --- 状态监听 ---
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("WS", "连接成功")
                heartbeatHandler.post(heartbeatRunnable)
                socket?.emit("authenticate", JSONObject().apply {
                    put("token", token)
                })
                emitJoin(currentCategoryId)
                onStatusChanged(true) // 通知调用处：连接成功
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("WS", "连接断开")
                onStatusChanged(false) // 通知调用处：连接断开
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) {
                Log.e("WS", "连接失败")
                onStatusChanged(false) // 通知调用处：连接失败
            }

            // --- 消息解析 ---
            socket?.on("new_message") { args ->
                val msg = AppJson.json.decodeFromString<Message>(args[0].toString())
                onEvent("NEW", msg, msg.message_id, null)
            }

            socket?.on("message_edited") { args ->
                val data = args[0] as JSONObject
                val updatedJson = data.optJSONObject("updated_message")?.toString()
                val msg = updatedJson?.let { AppJson.json.decodeFromString<Message>(it) }
                onEvent("EDIT", msg, msg?.message_id, null)
            }

            socket?.on("message_deleted") { args ->
                val id = (args[0] as JSONObject).optInt("message_id")
                onEvent("DELETE", null, id, null)
            }

            socket?.on("like_update") { args ->
                val data = args[0] as JSONObject
                val id = data.optInt("message_id")
                val count = data.optInt("like_count")
                onEvent("LIKE", null, id, count)
            }

            socket?.connect()
        } catch (_: Exception) {
            onStatusChanged(false)
        }
    }

    fun switchCategory(newCategoryId: Int) {
        if (socket?.connected() != true) return
        socket?.emit("leave_category", JSONObject().apply { put("category_id", currentCategoryId) })
        this.currentCategoryId = newCategoryId
        emitJoin(newCategoryId)
    }

    private fun emitJoin(categoryId: Int) {
        val data = JSONObject().apply {
            put("category_id", categoryId)
            currentToken?.let { put("x-access-token", it) }
        }
        socket?.emit("join_category", data)
    }

    fun disconnect() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        socket?.disconnect()
    }
}