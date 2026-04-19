package com.example.toolbox.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolbox.data.community.CommunityState
import com.example.toolbox.data.community.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class CommunityViewModel : ViewModel() {
    private val _state = MutableStateFlow(CommunityState())
    val state: StateFlow<CommunityState> = _state.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun handleSocketEvent(type: String, msg: Message?, msgId: Int?, count: Int?) {
        val currentCategoryId = _state.value.categoryId
        when (type) {
            "NEW" -> {
                if (msg != null && msg.category_id == currentCategoryId) {
                    _state.update { state ->
                        val newList = (listOf(msg) + state.messages).distinctBy { it.message_id }
                        state.copy(messages = newList)
                    }
                }
            }
            "EDIT" -> {
                if (msg != null) {
                    _state.update { state ->
                        val updatedList = state.messages.map {
                            if (it.message_id == msg.message_id) msg else it
                        }
                        state.copy(messages = updatedList)
                    }
                }
            }
            "DELETE" -> {
                if (msgId != null) {
                    _state.update { state ->
                        val filteredList = state.messages.filter { it.message_id != msgId }
                        state.copy(messages = filteredList)
                    }
                }
            }
            "LIKE" -> {
                if (msgId != null && count != null) {
                    _state.update { state ->
                        val updatedList = state.messages.map {
                            if (it.message_id == msgId) {
                                it.copy(like_count = count)
                            } else it
                        }
                        state.copy(messages = updatedList)
                    }
                }
            }
        }
    }

    private var communitySocket: CommunitySocket? = null

    fun initWebSocket(token: String) {
        if (communitySocket?.isConnected() == true) return

        viewModelScope.launch(Dispatchers.Default) {
            communitySocket?.disconnect()
            val currentCategoryId = _state.value.categoryId
            communitySocket = CommunitySocket(
                onStatusChanged = { isConnected ->
                    _state.update { it.copy(wsConnectState = if (isConnected) 2 else 1) }
                },
                onEvent = { type, msg, id, count ->
                    handleSocketEvent(type, msg, id, count)
                }
            )
            communitySocket?.connect(token, currentCategoryId)
        }
    }

    fun onCategoryChanged(newCategoryId: Int) {
        _state.update { it.copy(categoryId = newCategoryId) }
        communitySocket?.switchCategory(newCategoryId)
    }

    override fun onCleared() {
        super.onCleared()
        communitySocket?.disconnect()
    }

    fun fetchCategories(token: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = loadCategories(client, token)
                _state.update { it.copy(categories = response.categories, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }

    fun fetchMessages(
        token: String,
        page: Int = 1,
        isRefresh: Boolean = false
    ) {
        val categoryId = state.value.categoryId

        viewModelScope.launch {
            _state.update { state ->
                when {
                    isRefresh -> state.copy(isRefreshing = true, errorMessage = null)
                    page == 1 -> state.copy(isLoading = true, errorMessage = null)
                    else -> state.copy(isLoadingMore = true, errorMessage = null)
                }
            }

            try {
                val response = loadMessages(
                    client = client,
                    userToken = token,
                    page = page,
                    perPage = 20,
                    categoryId = categoryId
                )

                _state.update { state ->
                    val newMessages = if (page == 1) response.messages else state.messages + response.messages
                    state.copy(
                        messages = newMessages.distinctBy { it.message_id },
                        currentPage = response.pagination.current_page,
                        totalPages = response.pagination.total_pages,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _state.update { state ->
                    state.copy(
                        errorMessage = e.message,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false
                    )
                }
            }
        }
    }

    fun toggleLike(
        token: String,
        message: Message,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val (newIsLiked, newLikeCount) = toggleLike(
                    client,
                    token,
                    message.message_id,
                    message.is_liked,
                    message.like_count
                )

                _state.update { state ->
                    val updatedMessages = state.messages.map {
                        if (it.message_id == message.message_id) {
                            it.copy(like_count = newLikeCount, is_liked = newIsLiked)
                        } else it
                    }
                    state.copy(messages = updatedMessages)
                }
            } catch (e: Exception) {
                onError(e.message ?: "操作失败")
            }
        }
    }

    fun deleteMessage(
        token: String,
        messageId: Int,
        userStatus: Int,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                deleteMessage(client, token, userStatus, messageId)
                _state.update { state ->
                    state.copy(messages = state.messages.filter { it.message_id != messageId })
                }
            } catch (e: Exception) {
                onError(e.message ?: "删除失败")
            }
        }
    }

    fun postReply(
        token: String,
        content: String,
        categoryId: Int,
        refId: Int,
        isPrivate: Boolean,
        targetUserId: Int,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                postReply(client, token, content, categoryId, refId, isPrivate, targetUserId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "回复失败")
            }
        }
    }
}
