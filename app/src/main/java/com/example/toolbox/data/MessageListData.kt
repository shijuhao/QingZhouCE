package com.example.toolbox.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Friend(
    val id: Int,
    val username: String = "",
    val name: String = "", // Add name for group
    val avatar: String,
    val title: String = "",
    @SerialName("title_status") val titleStatus: Int = 0,
    @SerialName("last_message") val lastMessage: String?,
    @SerialName("last_message_time") val lastMessageTime: String?,
    @SerialName("unread_count") val unreadCount: Int,
    @SerialName("added_at") val addedAt: String = "",
    val type: String = "private" // "private" or "group"
)

@Serializable
data class Pagination(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Int,
    val pages: Int,
    @SerialName("has_next") val hasNext: Boolean = false,
    @SerialName("has_prev") val hasPrev: Boolean = false
)

@Serializable
data class FriendsResponse(
    val success: Boolean,
    val friends: List<Friend>,
    val pagination: Pagination
)

// ---------- UI 状态 ----------
data class MessageUiState(
    val friends: List<Friend> = emptyList(),
    val isLoading: Boolean = false,          // 初始加载
    val isRefreshing: Boolean = false,       // 下拉刷新中
    val isLoadingMore: Boolean = false,      // 加载更多中
    val error: String? = null,
    val pagination: Pagination? = null,
    val hasMore: Boolean = true
)