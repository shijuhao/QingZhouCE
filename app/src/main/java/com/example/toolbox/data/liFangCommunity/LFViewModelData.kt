package com.example.toolbox.data.liFangCommunity

data class LiFangUiState(
    val isLoggedIn: Boolean = false,
    val username: String = "未登录",
    val unreadNotificationsCount: Int = 0,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null
)