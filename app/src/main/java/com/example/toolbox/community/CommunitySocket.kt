@file:Suppress("PropertyName")

package com.example.toolbox.community

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.example.toolbox.AppJson
import com.example.toolbox.data.community.Message
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

class CommunitySocket(
    private val serverUrl: String = "wss://hehenya.dpdns.org:8505/",
    private val onStatusChanged: (Boolean) -> Unit,
    private val onEvent: (type: String, msg: Message?, id: Int?, count:Int?) -> Unit
) {
    private var socket: Socket? = null
    private var currentToken: String? = null
    private var currentCategoryId: Int = 1

    private var heartbeatThread: HandlerThread? = null
    private var heartbeatHandler: Handler? = null
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (socket?.connected() == true) {
                socket?.emit("heartbeat", JSONObject().apply {
                    put("client_time", System.currentTimeMillis().toString())
                })
            }
            heartbeatHandler?.postDelayed(this, 30000)
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var scope: CoroutineScope? = null

    fun isConnected(): Boolean = socket?.connected() == true

    private fun ensureHeartbeatThread() {
        if (heartbeatThread == null || !heartbeatThread!!.isAlive) {
            heartbeatThread = HandlerThread("WS-Heartbeat").apply { start() }
            heartbeatHandler = Handler(heartbeatThread!!.looper)
        }
    }

    fun connect(token: String?, categoryId: Int) {
        this.currentToken = token
        this.currentCategoryId = categoryId

        try {
            ensureHeartbeatThread()
            
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

            val opts = IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = false
                timeout = 5000
            }
            socket = IO.socket(serverUrl, opts)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("WS", "连接成功")
                heartbeatHandler?.post(heartbeatRunnable)
                
                socket?.emit("authenticate", JSONObject().apply {
                    put("token", token)
                })
                emitJoin(currentCategoryId)
                
                mainHandler.post {
                    onStatusChanged(true)
                }
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("WS", "连接断开")
                heartbeatHandler?.removeCallbacks(heartbeatRunnable)
                mainHandler.post {
                    onStatusChanged(false)
                }
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) {
                Log.e("WS", "连接失败: ${it.firstOrNull()}")
                heartbeatHandler?.removeCallbacks(heartbeatRunnable)
                mainHandler.post {
                    onStatusChanged(false)
                }
            }

            socket?.on("new_message") { args ->
                scope?.launch {
                    try {
                        val msg = AppJson.json.decodeFromString<Message>(args[0].toString())
                        mainHandler.post {
                            onEvent("NEW", msg, msg.message_id, null)
                        }
                    } catch (e: Exception) {
                        Log.e("WS", "解析新消息失败", e)
                    }
                }
            }

            socket?.on("message_edited") { args ->
                scope?.launch {
                    try {
                        val data = args[0] as JSONObject
                        val updatedJson = data.optJSONObject("updated_message")?.toString()
                        val msg = updatedJson?.let { AppJson.json.decodeFromString<Message>(it) }
                        mainHandler.post {
                            onEvent("EDIT", msg, msg?.message_id, null)
                        }
                    } catch (e: Exception) {
                        Log.e("WS", "解析编辑消息失败", e)
                    }
                }
            }

            socket?.on("message_deleted") { args ->
                try {
                    val id = (args[0] as JSONObject).optInt("message_id")
                    mainHandler.post {
                        onEvent("DELETE", null, id, null)
                    }
                } catch (e: Exception) {
                    Log.e("WS", "解析删除消息失败", e)
                }
            }

            socket?.on("like_update") { args ->
                try {
                    val data = args[0] as JSONObject
                    val id = data.optInt("message_id")
                    val count = data.optInt("like_count")
                    mainHandler.post {
                        onEvent("LIKE", null, id, count)
                    }
                } catch (e: Exception) {
                    Log.e("WS", "解析点赞更新失败", e)
                }
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e("WS", "连接异常", e)
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
        heartbeatHandler?.removeCallbacks(heartbeatRunnable)
        
        scope?.cancel()
        scope = null
        
        socket?.disconnect()
        socket?.off()
        socket = null
    }
}
