package com.example.toolbox.liFangCommunity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolbox.data.liFangCommunity.LoginUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.jsoup.Jsoup

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // ViewModel 初始化时，先加载登录页面以获取 Session Cookie
        fetchLoginPage()
    }

    fun onUsernameChange(newValue: String) {
        _uiState.value = _uiState.value.copy(username = newValue, error = null)
    }

    fun onPasswordChange(newValue: String) {
        _uiState.value = _uiState.value.copy(password = newValue, error = null)
    }

    /**
     * 预加载登录页面，以建立 Session Cookie。
     */
    private fun fetchLoginPage() {
        _uiState.value = _uiState.value.copy(isLoginPageLoaded = false, isLoading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${CubeNetworkManager.BASE_URL}/login")
                    .get()
                    .build()

                // PersistentCookieJar 库会自动处理 Cookie 的发送和接收
                val response = CubeNetworkManager.client.newCall(request).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        _uiState.value = _uiState.value.copy(isLoginPageLoaded = true, isLoading = false)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoginPageLoaded = false,
                            isLoading = false,
                            error = "无法加载登录页面: ${response.code}"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoginPageLoaded = false,
                        isLoading = false,
                        error = "网络错误，无法加载登录页面"
                    )
                }
            }
        }
    }

    fun login(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        if (!currentState.isLoginPageLoaded) {
            _uiState.value = currentState.copy(error = "登录页面未加载完成，请稍候")
            return
        }
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(error = "用户名或密码不能为空")
            return
        }

        _uiState.value = currentState.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val formBody = FormBody.Builder()
                    .add("username", currentState.username)
                    .add("password", currentState.password)
                    // CSRF Token 相关逻辑已移除
                    .build()

                val request = Request.Builder()
                    .url("${CubeNetworkManager.BASE_URL}/login")
                    .post(formBody)
                    .build()

                val response = CubeNetworkManager.client.newCall(request).execute()
                val html = response.body.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val doc = Jsoup.parse(html)
                        val userSpan = doc.select(".user-info span").firstOrNull()

                        if (userSpan != null) {
                            val loggedInUsername = userSpan.text()
                            AuthManager.saveLoginState(loggedInUsername) // 仅保存用户名和登录标志
                            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                            onSuccess()
                        } else {
                            val bodyText = doc.body().text()
                            val errorMsg = when {
                                bodyText.contains("封禁") || bodyText.contains("banned") -> "登录失败：该账号已被封禁"
                                bodyText.contains("密码错误") || bodyText.contains("Invalid password") -> "登录失败：密码错误"
                                bodyText.contains("用户不存在") || bodyText.contains("User not found") -> "登录失败：用户不存在"
                                doc.select(".alert-danger").isNotEmpty() -> doc.select(".alert-danger").text()
                                else -> "登录失败，请检查用户名或密码"
                            }

                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = errorMsg
                            )
                            fetchLoginPage() // 登录失败后，重新加载登录页以获取新的 Session Cookie
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "服务器响应错误: ${response.code}"
                        )
                        fetchLoginPage() // 失败后重新加载
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "网络连接失败: ${e.message}"
                    )
                }
            }
        }
    }
}