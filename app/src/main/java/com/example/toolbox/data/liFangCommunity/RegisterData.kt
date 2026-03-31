package com.example.toolbox.data.liFangCommunity

data class RegisterUiState(
    val username: String = "",
    val password: String = "",
    val isPreparing: Boolean = true,
    val isLoading: Boolean = false,
    val isLegal: Boolean = false,
    val error: String? = null
)