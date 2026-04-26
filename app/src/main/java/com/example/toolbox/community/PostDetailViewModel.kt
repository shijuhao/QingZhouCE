package com.example.toolbox.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.data.community.LocalApiResponse
import com.example.toolbox.data.community.MessageInfo
import com.example.toolbox.data.community.ReplyApiResponse
import com.example.toolbox.data.community.ReplyMessage
import com.example.toolbox.data.community.ReplyPagination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

data class PostDetailUiState(
    val messageInfo: MessageInfo? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
    
    val replies: List<ReplyMessage> = emptyList(),
    val replyPagination: ReplyPagination? = null,
    val replyError: String? = null,
    val isLoadingReplies: Boolean = false,
    
    val isLiked: Boolean = false,
    val likeCount: Int = 0,
    val isLiking: Boolean = false,
    val isReplying: Boolean = false,
    val replySuccess: Boolean = false
)

class PostDetailViewModel(
    private val messageId: Int,
    private val token: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient()

    init {
        loadMessageInfo()
    }

    fun loadMessageInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val headers = token?.let {
                    Headers.Builder()
                        .add("Content-Type", "application/json")
                        .add("x-access-token", it)
                }?.build()

                val request = headers?.let {
                    Request.Builder()
                        .url("${ApiAddress}get_message_info")
                        .post(
                            JSONObject().apply { put("message_id", messageId) }.toString()
                                .toRequestBody("application/json".toMediaType())
                        )
                        .headers(it)
                }?.build()

                if (request != null) {
                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                        client.newCall(request).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                viewModelScope.launch {
                                    _uiState.update { 
                                        it.copy(isLoading = false, error = "网络错误: ${e.message}") 
                                    }
                                }
                            }

                            override fun onResponse(call: Call, response: Response) {
                                response.use {
                                    if (!it.isSuccessful) {
                                        viewModelScope.launch {
                                            _uiState.update {
                                                it.copy(isLoading = false, error = "服务器错误: ${response.code}")
                                            }
                                        }
                                        return
                                    }
                                    
                                    val res = it.body.string()
                                        .let { string -> AppJson.json.decodeFromString<LocalApiResponse>(string) }
                                    
                                    viewModelScope.launch {
                                        if (res.success) {
                                            _uiState.update { state ->
                                                state.copy(
                                                    isLoading = false,
                                                    messageInfo = res.message_info,
                                                    isLiked = res.message_info?.is_liked ?: false,
                                                    likeCount = res.message_info?.like_count ?: 0
                                                )
                                            }
                                            loadReplies(page = 1, append = false)
                                        } else {
                                            _uiState.update { 
                                                it.copy(isLoading = false, error = res.message) 
                                            }
                                        }
                                    }
                                }
                            }
                        })
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = "请求失败: ${e.message}") 
                }
            }
        }
    }

    fun loadReplies(page: Int = 1, append: Boolean = false) {
        if (_uiState.value.isLoadingReplies) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReplies = true, replyError = null) }
            
            try {
                val headers = token?.let {
                    Headers.Builder()
                        .add("Content-Type", "application/json")
                        .add("x-access-token", it)
                }?.build()

                val jsonBody = JSONObject().apply {
                    put("message_id", messageId)
                    put("page", page)
                    put("per_page", 20)
                }

                val request = headers?.let {
                    Request.Builder()
                        .url("${ApiAddress}get_message_reply")
                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                        .headers(it)
                }?.build()

                if (request != null) {
                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) {
                                _uiState.update { 
                                    it.copy(isLoadingReplies = false, replyError = "服务器错误: ${response.code}") 
                                }
                                return@withContext
                            }
                            
                            val res = AppJson.json.decodeFromString<ReplyApiResponse>(response.body.string())
                            
                            if (res.success) {
                                _uiState.update { state ->
                                    val newReplies = if (append) {
                                        state.replies + (res.replies ?: emptyList())
                                    } else {
                                        res.replies ?: emptyList()
                                    }
                                    state.copy(
                                        isLoadingReplies = false,
                                        replies = newReplies,
                                        replyPagination = res.pagination
                                    )
                                }
                            } else {
                                _uiState.update { 
                                    it.copy(isLoadingReplies = false, replyError = res.message) 
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoadingReplies = false, replyError = "网络错误: ${e.message}") 
                }
            }
        }
    }

    fun toggleLikeRequest() {
        val currentState = _uiState.value
        if (currentState.isLiking || token == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLiking = true) }
            
            try {
                val (newIsLiked, newLikeCount) = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    toggleLike(
                        client = client,
                        token = token,
                        messageId = messageId,
                        wasLiked = currentState.isLiked,
                        currentLikeCount = currentState.likeCount
                    )
                }
                
                _uiState.update { 
                    it.copy(
                        isLiked = newIsLiked,
                        likeCount = newLikeCount,
                        isLiking = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLiking = false) }
            }
        }
    }

    fun postReply(content: String, categoryId: Int, refId: Int) {
        if (content.isBlank() || token == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isReplying = true) }
            
            try {
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    postReplyRequest(
                        client = client,
                        token = token,
                        content = content,
                        categoryId = categoryId,
                        refId = refId
                    )
                }
                
                _uiState.update { it.copy(isReplying = false, replySuccess = true) }
                loadMessageInfo()
            } catch (e: Exception) {
                _uiState.update { it.copy(isReplying = false, replyError = e.message) }
            }
        }
    }

    private suspend fun postReplyRequest(
        client: OkHttpClient,
        token: String,
        content: String,
        categoryId: Int,
        refId: Int
    ) {
        val headers = Headers.Builder()
            .add("Content-Type", "application/json")
            .add("x-access-token", token)
            .build()

        val jsonBody = JSONObject().apply {
            put("content", content)
            put("category_id", categoryId)
            put("referenced_message_id", refId)
        }

        val request = Request.Builder()
            .url("${ApiAddress}post_referenced_message")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .headers(headers)
            .build()

        withContext(kotlinx.coroutines.Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("回复失败: ${response.code}")
                }
            }
        }
    }

    fun refresh() {
        loadMessageInfo()
    }

    fun resetReplySuccess() {
        _uiState.update { it.copy(replySuccess = false) }
    }
}
