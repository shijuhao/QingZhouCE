@file:Suppress("PropertyName")

package com.example.toolbox.data.mine

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    val success: Boolean,
    val user_info: UserInfoData
)

@Serializable
data class UserInfoData(
    val title_status: Int,
    val title: String,
    val username: String,
    val id: Int,
    val avatar_url: String,
    val background_url: String?,
    val quick_login_enabled: Int = 0,
    val bio: String,
    val gold: Int,
    val level: Int,
    val experience: Int,
    val has_unread_notifications: Int,
    val has_checked_in: Int,
    val has_pending_audit: Int,
    val following_count: Int, //关注
    val followers_count: Int, //粉丝
)