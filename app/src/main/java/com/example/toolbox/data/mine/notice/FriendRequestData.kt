package com.example.toolbox.data.mine.notice

import com.example.toolbox.mine.notice.RequestType
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ---------- 数据模型 ----------
@Serializable
data class User(
    val id: Int,
    val username: String,
    val avatar: String,
    val title: String,
    @SerialName("title_status") val titleStatus: Int
)

@Serializable
data class FriendRequest(
    @SerialName("request_id") val requestId: Int,
    val user: User,
    @SerialName("created_at") val createdAt: String,
    @SerialName("created_at_display") val createdAtDisplay: String,
    val status: Int,
    @SerialName("status_text") val statusText: String
)

@Serializable
data class RequestPagination(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Int,
    val pages: Int,
    @SerialName("has_next") val hasNext: Boolean,
    @SerialName("has_prev") val hasPrev: Boolean
)

@Serializable
data class RequestData(
    val requests: List<FriendRequest>,
    val type: String,
    val pagination: RequestPagination
)

@Serializable
data class FriendRequestsResponse(
    val success: Boolean,
    val data: RequestData
)

// 处理请求的响应
@Serializable
data class RespondResponse(
    val success: Boolean,
    val message: String
)

data class FriendRequestUiState(
    val currentType: RequestType = RequestType.RECEIVED,
    val requests: List<FriendRequest> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val pagination: RequestPagination? = null,
    val hasMore: Boolean = true
)