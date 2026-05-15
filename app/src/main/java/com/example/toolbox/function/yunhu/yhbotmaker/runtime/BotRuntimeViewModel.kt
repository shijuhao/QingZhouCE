package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BotRuntimeViewModel : ViewModel() {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()
    
    fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }
    
    fun clearMessages() {
        _messages.value = emptyList()
    }
}