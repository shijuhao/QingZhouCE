package com.example.toolbox.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Message(
    val id: Int,
    @SerialName("sender_id") val senderId: Int,
    @SerialName("sender_username") val senderUsername: String = "", // 默认空字符串
    @SerialName("sender_avatar") val senderAvatar: String = "", // 默认空字符串
    @SerialName("receiver_id") val receiverId: Int,
    val content: String,
    val images: List<String> = emptyList(), // 默认空列表
    @SerialName("is_markdown") val isMarkdown: Boolean = false,
    val timestamp: String,
    @SerialName("timestamp_display") val timestampDisplay: String = "",
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("is_recalled") val isRecalled: Boolean = false,
    @SerialName("recall_hint") val recallHint: String? = null,
    @SerialName("recall_time") val recallTime: String? = null,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("is_mine") val isMine: Boolean = false,
    @SerialName("edit_records") val editRecords: List<EditRecord> = emptyList()
)

@Serializable
data class EditRecord(
    @SerialName("edit_time") val editTime: String,
    @SerialName("old_content") val oldContent: String,
    @SerialName("old_images") val oldImages: List<String>
)

// ---------- 其他用户信息 ----------
@Serializable
data class OtherUser(
    val id: Int,
    val username: String,
    val avatar: String,
    val title: String,
    @SerialName("title_status") val titleStatus: Int
)

// ---------- 分页 ----------
@Serializable
data class MessagePagination(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Int,
    val pages: Int
)

@Serializable
data class GetMessagesResponse(
    val success: Boolean,
    val messages: List<Message>,
    @SerialName("other_user") val otherUser: OtherUser,
    val relationship: String = "friend",
    val pagination: MessagePagination
)

@Serializable
data class SendMessageResponse(
    val success: Boolean,
    val message: String,
    @SerialName("message_id") val messageId: Int,
    val timestamp: String,
    @SerialName("has_content") val hasContent: Boolean,
    @SerialName("has_images") val hasImages: Boolean,
    @SerialName("images_count") val imagesCount: Int,
    @SerialName("is_markdown") val isMarkdown: Boolean,
    @SerialName("care_message") val careMessage: CareMessage,
    @SerialName("experience_added") val experienceAdded: Int,
    @SerialName("daily_messages_left") val dailyMessagesLeft: Int
)

@Serializable
data class CareMessage(
    val warning: String
)

@Serializable
data class SimpleResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class EditMessageResponse(
    val success: Boolean,
    val message: String,
    @SerialName("edit_time") val editTime: String,
    @SerialName("has_images") val hasImages: Boolean,
    @SerialName("images_count") val imagesCount: Int,
    @SerialName("is_markdown") val isMarkdown: Boolean
)

// ---------- 发送消息请求体 ----------
@Serializable
data class SendMessageRequest(
    @SerialName("receiver_id") val receiverId: Int,
    val content: String?,
    val images: List<String>?,
    @SerialName("is_markdown") val isMarkdown: Boolean = false
)

// ---------- UI 状态 ----------
// UI 状态类不需要序列化，不需要加 @Serializable
data class MessageDetailUiState(
    val messages: List<Message> = emptyList(),
    val otherUser: OtherUser? = null,
    val isFriend: Boolean = true,
    val relationship: String = "friend",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val pagination: MessagePagination? = null,
    val hasMore: Boolean = false,
    val inputText: String = "",
    val selectedImages: List<String> = emptyList(),
    val isMarkdown: Boolean = false,
    val editingMessage: Message? = null,
    val dailyMessagesLeft: Int = 0
)

// 撤回确认弹窗状态
data class RecallDialogState(
    val isOpen: Boolean = false,
    val messageId: Int? = null
)

// 编辑弹窗状态
data class EditDialogState(
    val isOpen: Boolean = false,
    val message: Message? = null,
    val newContent: String = "",
    val newImages: List<String> = emptyList()
)