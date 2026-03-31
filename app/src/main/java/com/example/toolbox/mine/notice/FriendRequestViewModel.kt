package com.example.toolbox.mine.notice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.data.mine.notice.FriendRequestUiState
import com.example.toolbox.data.mine.notice.FriendRequestsResponse
import com.example.toolbox.data.mine.notice.RespondResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

enum class RequestType(val value: String) {
    RECEIVED("received"),
    SENT("sent")
}

// ---------- ViewModel ----------
class FriendRequestViewModel(
    private val token: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendRequestUiState())
    val uiState: StateFlow<FriendRequestUiState> = _uiState.asStateFlow()

    // 正在处理中的请求ID集合，用于按钮禁用和显示加载
    private val _processingIds = MutableStateFlow<Set<Int>>(emptySet())
    val processingIds: StateFlow<Set<Int>> = _processingIds.asStateFlow()

    // Toast 消息通道
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val client = OkHttpClient()
    private val json = AppJson.json

    init {
        loadRequests(page = 1, isRefresh = true)
    }

    fun switchType(type: RequestType) {
        if (_uiState.value.currentType == type) return
        _uiState.update { it.copy(currentType = type, requests = emptyList(), pagination = null, hasMore = true) }
        loadRequests(page = 1, isRefresh = true)
    }

    fun refresh() {
        loadRequests(page = 1, isRefresh = true)
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMore) return
        val nextPage = (currentState.pagination?.currentPage ?: 0) + 1
        loadRequests(page = nextPage, isRefresh = false)
    }

    fun respondToRequest(requestId: Int, accept: Boolean) {
        viewModelScope.launch {
            if (_processingIds.value.contains(requestId)) return@launch
            _processingIds.update { it + requestId }

            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val url = "${ApiAddress}friends/respond_request"
                    val bodyJson = buildJsonObject {
                        put("request_id", requestId)
                        put("accept", accept)
                    }.toString()
                    val requestBody = bodyJson.toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url(url)
                        .header("x-access-token", token)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        return@withContext null
                    }

                    val body = response.body.string()
                    json.decodeFromString<RespondResponse>(body)
                }
            }

            result.onSuccess { response ->
                if (response != null && response.success) {
                    _toastMessage.emit(response.message)
                    refresh()
                } else {
                    _toastMessage.emit(response?.message ?: "操作失败")
                }
            }.onFailure { e ->
                _toastMessage.emit(e.message ?: "未知错误")
            }

            _processingIds.update { it - requestId }
        }
    }

    private fun loadRequests(page: Int, isRefresh: Boolean) {
        viewModelScope.launch {
            // 1. 更新加载状态（主线程）
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoadingMore = true, error = null)
            }

            // 2. 切换到 IO 线程执行网络请求和响应读取
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val typeValue = _uiState.value.currentType.value
                    val url = "${ApiAddress}friends/requests?type=$typeValue&page=$page&per_page=20"
                    val request = Request.Builder()
                        .url(url)
                        .header("x-access-token", token)
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) return@withContext null

                    val body = response.body.string()
                    json.decodeFromString<FriendRequestsResponse>(body)
                }
            }

            result.onSuccess { resultData ->
                if (resultData != null && resultData.success) {
                    _uiState.update { current ->
                        val newRequests = if (isRefresh) {
                            resultData.data.requests
                        } else {
                            current.requests + resultData.data.requests
                        }
                        current.copy(
                            requests = newRequests,
                            pagination = resultData.data.pagination,
                            hasMore = resultData.data.pagination.hasNext,
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
}

// ViewModel 工厂
class FriendRequestViewModelFactory(private val token: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendRequestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendRequestViewModel(token) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}