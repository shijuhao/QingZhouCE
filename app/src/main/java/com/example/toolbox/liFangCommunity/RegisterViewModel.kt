package com.example.toolbox.liFangCommunity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolbox.data.liFangCommunity.RegisterUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.regex.Pattern

class RegisterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    private var solvedCaptchaAnswer: String? = null

    init {
        fetchAndSolveCaptcha()
    }

    private fun fetchAndSolveCaptcha() {
        _uiState.value = _uiState.value.copy(isPreparing = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${CubeNetworkManager.BASE_URL}/register")
                    .get()
                    .build()

                // PersistentCookieJar 库会自动处理 Cookie 的发送和接收
                val response = CubeNetworkManager.client.newCall(request).execute()
                val html = response.body.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val doc = Jsoup.parse(html)

                        val captchaText = doc.select(".captcha-text").text()
                        val pattern = Pattern.compile("(\\d+)\\s*\\+\\s*(\\d+)")
                        val matcher = pattern.matcher(captchaText)

                        if (matcher.find()) {
                            val num1 = matcher.group(1)?.toIntOrNull() ?: 0
                            val num2 = matcher.group(2)?.toIntOrNull() ?: 0
                            solvedCaptchaAnswer = (num1 + num2).toString()

                            _uiState.value = _uiState.value.copy(
                                isPreparing = false
                            )
                            println("Register Captcha solved: $solvedCaptchaAnswer")
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isPreparing = false,
                                error = "无法解析验证码，请重试"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isPreparing = false,
                            error = "无法连接到服务器"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isPreparing = false,
                        error = "网络初始化失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun onUsernameChange(newValue: String) {
        _uiState.value = _uiState.value.copy(username = newValue, error = null)
    }

    fun onPasswordChange(newValue: String) {
        _uiState.value = _uiState.value.copy(password = newValue, error = null)
    }

    fun onUserLegalStatusChange(newValue: Boolean) {
        _uiState.value = _uiState.value.copy(isLegal = newValue, error = null)
    }

    fun register(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(error = "用户名或密码不能为空")
            return
        }

        if (solvedCaptchaAnswer == null) {
            fetchAndSolveCaptcha() // 尝试重新获取
            _uiState.value = currentState.copy(error = "正在初始化安全验证，请稍后再试...")
            return
        }

        _uiState.value = currentState.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val formBody = FormBody.Builder()
                    .add("username", currentState.username)
                    .add("password", currentState.password)
                    .add("captcha", solvedCaptchaAnswer!!)
                    // CSRF Token 相关逻辑已移除
                    .build()

                val request = Request.Builder()
                    .url("${CubeNetworkManager.BASE_URL}/register")
                    .post(formBody)
                    .build()

                // PersistentCookieJar 库会自动处理 Cookie 的发送和接收
                val response = CubeNetworkManager.client.newCall(request).execute()
                val html = response.body.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val doc = Jsoup.parse(html)
                        val hasCaptcha = doc.select(".captcha-text").isNotEmpty()

                        if (!hasCaptcha) {
                            onSuccess()
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "注册失败：用户名可能已存在或密码不符合要求"
                            )
                            fetchAndSolveCaptcha() // 注册失败后验证码可能刷新，重新获取
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "服务器响应错误: ${response.code}"
                        )
                        fetchAndSolveCaptcha() // 失败后重新加载
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "网络请求失败"
                    )
                    fetchAndSolveCaptcha() // 失败后重新加载
                }
            }
        }
    }
}