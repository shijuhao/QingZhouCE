package com.example.toolbox.liFangCommunity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolbox.data.liFangCommunity.PostUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.jsoup.Jsoup

class PostViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PostUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // ViewModel 初始化时，先检查登录状态，然后尝试获取发帖表单
        checkLoginStatusAndFetchPostForm()
    }

    private fun checkLoginStatusAndFetchPostForm() {
        val isLoggedIn = AuthManager.getIsLoggedIn()
        _uiState.value = _uiState.value.copy(isLoggedIn = isLoggedIn)

        if (!isLoggedIn) {
            _uiState.value = _uiState.value.copy(
                isPreparingForm = false,
                error = "请先登录才能发帖。"
            )
            return
        }
        fetchPostForm() // 如果已登录，则获取发帖表单
    }

    /**
     * 获取发帖页面，解析其中的验证码问题。
     */
    private fun fetchPostForm() {
        _uiState.value = _uiState.value.copy(isPreparingForm = true, error = null, captchaQuestion = "")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${CubeNetworkManager.BASE_URL}/") // --- 核心修改：GET 请求 URL 改为根路径 "/" ---
                    .get()
                    .build()

                val response = CubeNetworkManager.client.newCall(request).execute()
                val html = response.body.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val doc = Jsoup.parse(html)

                        // 提取验证码文本，例如 "证明你不是机器人：4 + 2 = ?"
                        val captchaSpan = doc.select(".captcha-container .captcha-text").firstOrNull()
                        val captchaText = captchaSpan?.text() ?: ""

                        if (captchaText.isNotBlank()) {
                            _uiState.value = _uiState.value.copy(
                                isPreparingForm = false,
                                captchaQuestion = captchaText // 显示给用户的验证码问题
                            )
                            println("Post Captcha question fetched: $captchaText") // 打印日志方便调试
                        } else {
                            // 即使页面成功加载，但没有找到验证码，也可能是非预期的页面（如重定向到登录页）
                            _uiState.value = _uiState.value.copy(
                                isPreparingForm = false,
                                error = "无法找到发帖表单或验证码，请确认已登录。"
                            )
                        }
                    } else {
                        // 服务器响应失败，可能是 4xx/5xx 错误，或者重定向到登录页等
                        _uiState.value = _uiState.value.copy(
                            isPreparingForm = false,
                            error = "无法加载发帖表单。状态码: ${response.code}。请确认已登录或稍后重试。"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // 打印异常堆栈
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isPreparingForm = false,
                        error = "网络连接失败: ${e.message}。请检查网络或稍后重试。"
                    )
                }
            }
        }
    }

    fun onTitleChange(newValue: String) {
        _uiState.value = _uiState.value.copy(title = newValue, error = null)
    }

    fun onContentChange(newValue: String) {
        _uiState.value = _uiState.value.copy(content = newValue, error = null)
    }

    fun onAgreePrivacyChange(newValue: Boolean) {
        _uiState.value = _uiState.value.copy(agreePrivacy = newValue, error = null)
    }

    fun onCaptchaManualAnswerChange(newValue: String) {
        _uiState.value = _uiState.value.copy(captchaManualAnswer = newValue, error = null)
    }

    /**
     * 提交发帖请求。
     * @param onSuccess 发帖成功后的回调函数。
     */
    fun submitPost(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        // 客户端表单验证
        if (!currentState.isLoggedIn) {
            _uiState.value = currentState.copy(error = "请先登录才能发帖。")
            return
        }
        if (currentState.title.isBlank()) {
            _uiState.value = currentState.copy(error = "标题不能为空")
            return
        }
        if (currentState.content.isBlank()) {
            _uiState.value = currentState.copy(error = "内容不能为空")
            return
        }
        if (!currentState.agreePrivacy) {
            _uiState.value = currentState.copy(error = "请同意隐私协议")
            return
        }

        // 验证码答案必须由用户手动输入
        val captchaToSubmit = currentState.captchaManualAnswer
        if (captchaToSubmit.isBlank()) {
            _uiState.value = currentState.copy(error = "请输入验证码计算结果")
            return
        }

        _uiState.value = currentState.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 构建表单数据，包含标题、内容、验证码和隐私协议
                val formBody = FormBody.Builder()
                    .add("title", currentState.title)
                    .add("content", currentState.content)
                    .add("captcha", captchaToSubmit) // 提交用户手动输入的验证码答案
                    .add("agree_privacy", if (currentState.agreePrivacy) "on" else "") // 隐私协议参数
                    .build()

                val request = Request.Builder()
                    .url("${CubeNetworkManager.BASE_URL}/post") // POST 请求 URL 保持为 "/post"
                    .post(formBody)
                    .build()

                val response = CubeNetworkManager.client.newCall(request).execute()
                val html = response.body.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val doc = Jsoup.parse(html)
                        val errorAlert = doc.select(".alert-danger").firstOrNull()

                        if (errorAlert == null) {
                            // 没有错误提示，认为发帖成功
                            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                            onSuccess() // 调用成功回调
                        } else {
                            // 存在错误提示，发帖失败
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = errorAlert.text() // 显示服务器返回的错误信息
                            )
                            fetchPostForm() // 失败后重新获取表单，验证码可能已刷新
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "服务器响应错误: ${response.code}" // 显示 HTTP 错误码
                        )
                        fetchPostForm() // 失败后重新获取表单，验证码可能已刷新
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "网络请求失败: ${e.message}" // 显示网络请求异常信息
                    )
                }
            }
        }
    }
}