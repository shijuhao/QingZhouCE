package com.example.toolbox

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolbox.data.LoginRequest
import com.example.toolbox.data.RegisterRequest
import com.example.toolbox.data.login.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    // 切换至注册页
    fun switchToRegister() {
        _state.update { it.copy(isLoginScreen = false) }
    }

    // 切换至登录页
    fun switchToLogin() {
        _state.update { it.copy(isLoginScreen = true) }
    }

    // 设置用户协议勾选状态
    fun setUserAgreementAccepted(accepted: Boolean) {
        _state.update { it.copy(userAgreementAccepted = accepted) }
    }

    // 显示用户协议对话框
    fun showUserAgreementDialog() {
        _state.update { it.copy(showUserAgreementDialog = true) }
    }

    // 关闭用户协议对话框
    fun dismissUserAgreementDialog() {
        _state.update { it.copy(showUserAgreementDialog = false) }
    }

    // 同意用户协议（对话框点击同意）
    fun acceptUserAgreement() {
        _state.update { it.copy(userAgreementAccepted = true, showUserAgreementDialog = false) }
    }

    // 登录操作
    fun login(
        context: Context,
        username: String,
        password: String,
        onePointLogin: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = ApiClient.login(context, LoginRequest(username, password, login_method = if (onePointLogin) 2 else 1))
            if (result.success && result.token != null) {
                TokenManager.save(context, result.token)
                onSuccess()
            } else {
                onError(result.message ?: "登录失败")
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    // 注册操作
    fun register(
        username: String,
        password: String,
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = ApiClient.register(RegisterRequest(username, password, email))
            if (result.success) {
                onSuccess()
            } else {
                onError(result.message ?: "注册失败")
            }
            _state.update { it.copy(isLoading = false) }
        }
    }
}