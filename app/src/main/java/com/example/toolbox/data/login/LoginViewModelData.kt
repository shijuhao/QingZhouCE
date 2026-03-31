package com.example.toolbox.data.login

data class LoginUiState(
    val isLoginScreen: Boolean = true,
    val isLoading: Boolean = false,
    val userAgreementAccepted: Boolean = false,
    val showUserAgreementDialog: Boolean = false
)