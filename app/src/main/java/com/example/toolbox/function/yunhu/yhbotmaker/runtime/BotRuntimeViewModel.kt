package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BotRuntimeViewModel private constructor(private val botName: String) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isWsConnected = MutableStateFlow(false)
    val isWsConnected: StateFlow<Boolean> = _isWsConnected.asStateFlow()
    
    private val _currentLoopCode = MutableStateFlow("")
    val currentLoopCode: StateFlow<String> = _currentLoopCode.asStateFlow()
    
    private val _startupExecuted = MutableStateFlow(false)
    val startupExecuted: StateFlow<Boolean> = _startupExecuted.asStateFlow()
    
    var luaEngine: LuaEngine? = null
    
    fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }
    
    fun clearMessages() {
        _messages.value = emptyList()
    }
    
    fun setWsConnected(connected: Boolean) {
        _isWsConnected.value = connected
    }
    
    fun setCurrentLoopCode(code: String) {
        _currentLoopCode.value = code
    }
    
    fun setStartupExecuted(executed: Boolean) {
        _startupExecuted.value = executed
    }
    
    companion object {
        private val instances = mutableMapOf<String, BotRuntimeViewModel>()
        
        fun getInstance(botName: String): BotRuntimeViewModel {
            return instances.getOrPut(botName) {
                BotRuntimeViewModel(botName)
            }
        }
    }
}