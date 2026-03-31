package com.example.toolbox.data.liFangCommunity

data class PostUiState(
    val title: String = "",
    val content: String = "",
    val agreePrivacy: Boolean = false,
    val captchaQuestion: String = "",     // 显示给用户的验证码问题
    val captchaManualAnswer: String = "", // 用户手动输入的验证码答案
    val isPreparingForm: Boolean = true,  // 是否正在准备表单（获取验证码等）
    val isLoading: Boolean = false,       // 是否正在提交帖子
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isLoggedIn: Boolean = false       // 用户的登录状态
)