package com.example.toolbox.data

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val login_method: Int,
    val device_type: String = android.os.Build.MODEL
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val token: String? = null,
    val message: String? = ""
)

@Serializable
data class RegisterResponse(
    val success: Boolean,
    val message: String? = ""
)