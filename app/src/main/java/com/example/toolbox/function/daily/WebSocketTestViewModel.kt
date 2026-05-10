package com.example.toolbox.function.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

// 消息类型
enum class MessageType {
    SUCCESS,    // ✅ 成功
    RECEIVED,   // 📨 收到
    ERROR,      // ❌ 错误/失败
    WARNING,    // ⚠️ 警告
    SENT,       // 📤 发送
    INFO        // 普通消息
}

// 消息数据类
data class WebSocketMessage(
    val content: String,
    val type: MessageType
)

class WebSocketTestViewModel : ViewModel() {
    private var webSocket: WebSocket? = null
    
    // 连接状态
    private val _connectionStatus = MutableStateFlow("未连接")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    // 消息列表
    private val _messages = MutableStateFlow<List<WebSocketMessage>>(emptyList())
    val messages: StateFlow<List<WebSocketMessage>> = _messages.asStateFlow()
    
    // 是否已连接
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // 连接WebSocket
    fun connect(url: String) {
        if (_isConnected.value) {
            disconnect()
        }
        
        _connectionStatus.value = "连接中..."
        
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        
        val request = Request.Builder()
            .url(url)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                viewModelScope.launch {
                    _isConnected.value = true
                    _connectionStatus.value = "已连接"
                    addMessage("连接成功: ${response.code}", MessageType.SUCCESS)
                }
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                viewModelScope.launch {
                    addMessage(text, MessageType.RECEIVED)
                }
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                viewModelScope.launch {
                    addMessage("${bytes.hex()}", MessageType.RECEIVED)
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                viewModelScope.launch {
                    _isConnected.value = false
                    _connectionStatus.value = "关闭中..."
                    addMessage("关闭中: $code - $reason", MessageType.WARNING)
                }
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                viewModelScope.launch {
                    _isConnected.value = false
                    _connectionStatus.value = "已断开"
                    addMessage("已断开: $code - $reason", MessageType.ERROR)
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                viewModelScope.launch {
                    _isConnected.value = false
                    _connectionStatus.value = "连接失败"
                    addMessage("错误: ${t.message}", MessageType.ERROR)
                }
            }
        })
    }
    
    // 发送消息
    fun sendMessage(message: String) {
        if (!_isConnected.value) {
            addMessage("未连接，无法发送", MessageType.WARNING)
            return
        }
        
        webSocket?.send(message)
        addMessage(message, MessageType.SENT)
    }
    
    // 断开连接
    fun disconnect() {
        webSocket?.close(1000, "用户主动断开")
        webSocket = null
        _isConnected.value = false
        _connectionStatus.value = "未连接"
    }
    
    // 清空消息
    fun clearMessages() {
        _messages.value = emptyList()
    }
    
    private fun addMessage(content: String, type: MessageType) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        _messages.value = _messages.value + WebSocketMessage(
            content = "[$timestamp] $content",
            type = type
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
