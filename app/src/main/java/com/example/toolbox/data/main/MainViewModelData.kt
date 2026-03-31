package com.example.toolbox.data.main

import kotlinx.serialization.Serializable

data class UiStatus(
    val showUserDialog: Boolean = false,
)

data class UserInfo(
    val isLoaded: Boolean = false,
    val tagStatus: Int = 0,
    val tag: String = "",
    val name: String = "正在登录",
    val bio: String = "正在登录",
    val level: Int = 1,
    val exp: Int = 0,
    val avatar: String = "null",
    val background: String? = "",
    val coin: Int = 0,
    val notice: Int = 0,
    val id: Int = 0,
    val signed: Int = 0,
    val hasPendingAudit: Int = 0,
    val following: Int = 0,
    val followers: Int = 0,
)

@Serializable
data class YiYanResponseHitokoto(val hitokoto: String)