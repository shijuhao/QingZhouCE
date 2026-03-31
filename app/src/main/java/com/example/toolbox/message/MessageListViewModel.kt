package com.example.toolbox.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.data.FriendsResponse
import com.example.toolbox.data.MessageUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class MessageViewModel(
    private val token: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient()
    private var webSocketManager: WebSocketManager? = null
    private var isWebSocketUpdate = false

    init {
        loadFriends(page = 1, isRefresh = true)
    }

    private fun silentRefresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(error = null) // 只清除错误，不改变刷新状态
            }

            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val requestBody = FormBody.Builder()
                        .add("page", "1")
                        .add("per_page", "20")
                        .build()

                    val request = Request.Builder()
                        .url("${ApiAddress}friends/list")
                        .post(requestBody)
                        .header("x-access-token", token)
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        return@withContext null
                    }

                    val responseBody = response.body.string()
                    AppJson.json.decodeFromString<FriendsResponse>(responseBody)
                }
            }

            result.onSuccess { friendsResponse ->
                if (friendsResponse != null && friendsResponse.success) {
                    _uiState.update { current ->
                        current.copy(
                            friends = friendsResponse.friends,
                            pagination = friendsResponse.pagination,
                            hasMore = friendsResponse.pagination.currentPage < friendsResponse.pagination.pages,
                            error = null
                            // 注意：没有修改 isRefreshing 状态
                        )
                    }
                }
            }

            // 重置标志
            isWebSocketUpdate = false
        }
    }

    // 修改原有的 refresh 方法
    fun refresh() {
        // 如果是WebSocket触发的更新，不执行刷新
        if (isWebSocketUpdate) {
            isWebSocketUpdate = false
            return
        }

        loadFriends(page = 1, isRefresh = true)
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMore) return
        val nextPage = (currentState.pagination?.currentPage ?: 0) + 1
        loadFriends(page = nextPage, isRefresh = false)
    }

    private fun loadFriends(page: Int, isRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoadingMore = true, error = null)
            }

            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val requestBody = FormBody.Builder()
                        .add("page", page.toString())
                        .add("per_page", "20")
                        .build()

                    val request = Request.Builder()
                        .url("${ApiAddress}friends/list")
                        .post(requestBody)
                        .header("x-access-token", token)
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        return@withContext null
                    }

                    val responseBody = response.body.string()
                    AppJson.json.decodeFromString<FriendsResponse>(responseBody)
                }
            }

            result.onSuccess { friendsResponse ->
                if (friendsResponse != null && friendsResponse.success) {
                    _uiState.update { current ->
                        val newFriends = if (isRefresh) friendsResponse.friends
                        else current.friends + friendsResponse.friends
                        current.copy(
                            friends = newFriends,
                            pagination = friendsResponse.pagination,
                            hasMore = friendsResponse.pagination.currentPage < friendsResponse.pagination.pages,
                            isRefreshing = false,
                            isLoadingMore = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(
                        isRefreshing = false,
                        isLoadingMore = false,
                        error = "请求失败或数据为空"
                    ) }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(
                    isRefreshing = false,
                    isLoadingMore = false,
                    error = e.message
                ) }
            }
        }
    }

    fun connectWebSocket() {
        if (token.isNotBlank()) {
            webSocketManager = WebSocketManager { type, message ->
                // 收到新消息时刷新好友列表
                when (type) {
                    "new", "edit", "recall" -> {
                        isWebSocketUpdate = true
                        silentRefresh()
                    }
                }
            }
            webSocketManager?.connect(token)
        }
    }

    fun disconnectWebSocket() {
        webSocketManager?.disconnect()
        webSocketManager = null
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}

class MessageViewModelFactory(private val token: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MessageViewModel(token) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}