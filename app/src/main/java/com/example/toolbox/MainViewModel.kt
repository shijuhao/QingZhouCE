package com.example.toolbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolbox.data.main.UiStatus
import com.example.toolbox.data.main.UserInfo
import com.example.toolbox.data.main.YiYanResponseHitokoto
import com.example.toolbox.data.mine.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainViewModel : ViewModel() {
    private val _showSidebar = MutableStateFlow(true)
    val showSidebar: StateFlow<Boolean> = _showSidebar.asStateFlow()

    private val _userInfo = MutableStateFlow(UserInfo())
    val userInfo: StateFlow<UserInfo> = _userInfo

    fun changeUserAvatar(avatar: String) {
        _userInfo.update { it.copy(avatar = avatar) }
    }

    fun changeUserBackground(value: String) {
        _userInfo.update { it.copy(background = value) }
    }

    private val _uiStatus = MutableStateFlow(UiStatus())
    val uiStatus: StateFlow<UiStatus> = _uiStatus

    fun changeUserDialogStatus(value: Boolean) {
        _uiStatus.update { it.copy(showUserDialog = value) }
    }

    // 加载状态
    private val _isLoadingUserInfo = MutableStateFlow(false)
    val isLoadingUserInfo: StateFlow<Boolean> = _isLoadingUserInfo

    private val _loadingUserInfoError = MutableStateFlow(false)
    val loadingUserInfoError: StateFlow<Boolean> = _loadingUserInfoError

    // 加载成功事件，供UI层执行持久化
    private val _loadSuccess = MutableSharedFlow<ApiResponse>()
    val loadSuccess = _loadSuccess.asSharedFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun refreshUserInfo(token: String) {
        viewModelScope.launch {
            _isLoadingUserInfo.value = true
            _loadingUserInfoError.value = false
            _userInfo.update { it.copy(isLoaded = false) }

            try {
                val headers = Headers.Builder()
                    .add("Content-Type", "application/json")
                    .add("x-access-token", token)
                    .build()

                val requestBody = "{}".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val url = "${ApiAddress}user_info_token"
                val request = Request.Builder()
                    .url(url)
                    .headers(headers)
                    .post(requestBody)
                    .build()

                val (responseData, isSuccessful) = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val body = response.body.string()
                        Pair(body, response.isSuccessful)
                    }
                }

                if (isSuccessful && responseData.isNotEmpty()) {
                    val apiResponse = AppJson.json.decodeFromString<ApiResponse>(responseData)
                    if (apiResponse.success) {
                        _userInfo.update {
                            it.copy(
                                isLoaded = true,
                                tagStatus = apiResponse.user_info.title_status,
                                tag = apiResponse.user_info.title.ifEmpty { it.tag },
                                name = apiResponse.user_info.username,
                                level = apiResponse.user_info.level,
                                exp = apiResponse.user_info.experience,
                                bio = apiResponse.user_info.bio,
                                avatar = apiResponse.user_info.avatar_url,
                                background = apiResponse.user_info.background_url ?: it.background,
                                coin = apiResponse.user_info.gold,
                                notice = apiResponse.user_info.has_unread_notifications,
                                id = apiResponse.user_info.id,
                                signed = apiResponse.user_info.has_checked_in,
                                hasPendingAudit = apiResponse.user_info.has_pending_audit,
                                following = apiResponse.user_info.following_count,
                                followers = apiResponse.user_info.followers_count
                            )
                        }
                        _loadSuccess.emit(apiResponse)
                    } else {
                        _loadingUserInfoError.value = true
                        _userInfo.update { it.copy(isLoaded = true) }
                    }
                } else {
                    _loadingUserInfoError.value = true
                    _userInfo.update { it.copy(isLoaded = true) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _loadingUserInfoError.value = true
                _userInfo.update { it.copy(isLoaded = true) }
            } finally {
                _isLoadingUserInfo.value = false
            }
        }
    }
}

class YiYanViewModel : ViewModel() {
    private val _hitokoto = MutableStateFlow("你好，用户！")
    val hitokoto: StateFlow<String> = _hitokoto

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    init {
        loadYiYan()
    }

    fun loadYiYan() {
        viewModelScope.launch {
            try {
                _hitokoto.value = fetchYiYan()
            } catch (_: IOException) { } catch (_: Exception) { }
        }
    }

    private suspend fun fetchYiYan(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://v1.hitokoto.cn/")
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val content = response.body.string()
                if (content.isNotEmpty()) {
                    val yiYanResponse = AppJson.json.decodeFromString<YiYanResponseHitokoto>(content)
                    yiYanResponse.hitokoto
                } else {
                    "返回数据为空"
                }
            } else {
                "HTTP错误: ${response.code}"
            }
        }
    }
}