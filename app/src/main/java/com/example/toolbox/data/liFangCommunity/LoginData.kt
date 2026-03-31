package com.example.toolbox.data.liFangCommunity

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoginPageLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)