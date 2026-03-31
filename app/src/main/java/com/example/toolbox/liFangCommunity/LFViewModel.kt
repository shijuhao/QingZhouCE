package com.example.toolbox.liFangCommunity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolbox.data.liFangCommunity.LiFangUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import org.jsoup.Jsoup

class LiFangViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LiFangUiState())
    val uiState: StateFlow<LiFangUiState> = _uiState.asStateFlow()

    init {
        val localIsLoggedIn = AuthManager.getIsLoggedIn()
        val localUsername = AuthManager.getUsername() ?: "未登录"
        _uiState.value = _uiState.value.copy(
            isLoggedIn = localIsLoggedIn,
            username = localUsername
        )
        checkLoginStatus()
    }

    fun checkLoginStatus() {
        _uiState.value = _uiState.value.copy(isLoading = true, isError = false, errorMessage = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${CubeNetworkManager.BASE_URL}/")
                    .get()
                    .build()

                val response = CubeNetworkManager.client.newCall(request).execute()
                val html = response.body.string()

                if (response.isSuccessful) {
                    val doc = Jsoup.parse(html)

                    val usernameSpan = doc.select(".user-info > span:not(.notif-badge)").firstOrNull()
                    val serverUsername = usernameSpan?.text() ?: "未登录"

                    val notifBadge = doc.select(".user-info .notif-badge").firstOrNull()
                    val unreadCount = notifBadge?.text()?.toIntOrNull() ?: 0

                    val logoutLink = doc.select("a[href*='/logout']").firstOrNull()

                    if (usernameSpan != null && logoutLink != null) {
                        AuthManager.saveLoginState(serverUsername) // 仅保存用户名和登录标志
                        _uiState.value = _uiState.value.copy(
                            isLoggedIn = true,
                            username = serverUsername,
                            unreadNotificationsCount = unreadCount, // 更新未读提醒数量
                            isLoading = false
                        )
                    } else {
                        AuthManager.clearLoginState() // 确保本地状态与服务器同步
                        _uiState.value = _uiState.value.copy(
                            isLoggedIn = false,
                            username = "未登录",
                            unreadNotificationsCount = 0, // 未登录则无提醒
                            isLoading = false
                        )
                    }
                } else {
                    AuthManager.clearLoginState()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isError = true,
                        errorMessage = "服务器响应错误: ${response.code}",
                        isLoggedIn = false,
                        username = "未登录",
                        unreadNotificationsCount = 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                AuthManager.clearLoginState()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = "网络连接失败",
                    isLoggedIn = false,
                    username = "未登录",
                    unreadNotificationsCount = 0
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${CubeNetworkManager.BASE_URL}/logout")
                    .get()
                    .build()

                CubeNetworkManager.client.newCall(request).execute()

                AuthManager.clearLoginState()
                CubeNetworkManager.clearCookies()

                checkLoginStatus()

            } catch (e: Exception) {
                e.printStackTrace()
                AuthManager.clearLoginState()
                CubeNetworkManager.clearCookies()
                checkLoginStatus()
            }
        }
    }
}