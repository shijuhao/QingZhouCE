package com.example.toolbox.data.community

import kotlinx.serialization.Serializable

@Serializable
data class PostPagination(
    val current_page: Int,
    val per_page: Int,
    val total: Int,
    val pages: Int
)

@Serializable
data class SearchUsersResponse(
    val success: Boolean,
    val users: List<UserSearchResult>? = null,   // 成功时存在
    val pagination: PostPagination? = null,      // 成功时存在
    val message: String? = null                   // 失败时存在
)

@Serializable
data class UserSearchResult(
    val id: Int,
    val username: String,
    val avatar_url: String,
    val gold: Int,
    val title: String,
    val is_followed: Int,
    val activity_points: Int,
    val total_check_ins: Int
)