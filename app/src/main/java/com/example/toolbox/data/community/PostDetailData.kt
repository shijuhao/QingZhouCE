@file:Suppress("PropertyName")

package com.example.toolbox.data.community

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LocalApiResponse(val success: Boolean, val message_info: MessageInfo?, val message: String?)

@Serializable
data class MessageInfo(
    val message_id: Int,
    val username: String,
    val user_level: Int? = null,
    val content: MessageContent,
    val message_type: Int,
    val timestamp_user: String?,
    val avatar_url: String?,
    val tag: String?,
    val like_count: Int,
    val is_liked: Boolean,
    val is_markdown: Boolean,
    val ip: String?,
    val is_private: Boolean,
    val can_view: Boolean,
    val visible_to: List<Int>?,
    val category: CategoryInfo,
    val like_users: List<LikeUser>?,
    val sender_info: SenderInfo,
    val is_edited: Int,
    val edit_records: List<LocalEditRecord>?,
    val referenced_message: ReferencedMessageInfo?
)

@Serializable
data class ReferencedMessageInfo(
    val message_id: Int,
    val content: String,
    val message_type: Int,
    val username: String?,
    val sender_info: ReferencedSender?,
    val referenced_message: ReferencedMessageInfo?
)

@Serializable
data class ReferencedSender(
    val id: Int,
    val username: String,
    val avatar_url: String?
)

@Serializable
data class MessageContent(
    val type: String,
    val text: String?,
    val url: String?,
    val title: String = "",
    val content: String?,
    val images: List<String>?
)

@Serializable
data class CategoryInfo(
    val id: Int = 0,
    val name: String = "",
    val avatar_url: String = ""
)

@Serializable
data class SenderInfo(
    val id: Int,
    val username: String,
    val avatar_url: String?,
    val title: String?,
    val gold: Int?,
    val activity_points: Int?
)

@Serializable
data class LocalEditRecord(
    val editor_username: String,
    val edit_time: String,
    val old_content: String,
    val new_content: String
)

@Serializable
data class LikeUser(
    val id: Int,
    val username: String,
    val avatar_url: String
)

@Serializable
data class ReplyOriginalMessage(
    val message_id: Int,
    val content: String,
    val message_type: Int,
    val timestamp: String,
    val timestamp_user: String?,
    val is_edited: Int,
    val status: Int,
    val sender_info: ReplySenderInfo,
    val edit_records: List<LocalEditRecord>?,
    val visible_to: List<Int>?
)

@Serializable
data class ReplyApiResponse(
    val success: Boolean,
    val message: String? = null,
    val original_message: ReplyOriginalMessage? = null,
    val replies: List<ReplyMessage>? = null,
    val total_replies: Int? = null,
    val pagination: ReplyPagination? = null
)

@Serializable
data class ReplyMessage(
    val message_id: Int,
    val content: String,
    val content_display: JsonElement? = null,
    val message_type: Int,
    val timestamp: String,
    val timestamp_user: String?,
    val is_edited: Int,
    val sender_info: ReplySenderInfo,
    val edit_records: List<LocalEditRecord>? = null,
    val visible_to: List<Int>? = null
)

@Serializable
data class ReplyContentDisplay(
    val text: String? = null,
    val images: List<String>? = null
)

@Serializable
data class ReplySenderInfo(
    val id: Int,
    val username: String,
    val avatar_url: String?,
    val title: String? = null,
    val title_status: Int? = null,
    val gold: Int? = null,
    val is_banned: Boolean? = null,
    val ban_end_time: String? = null,
    val activity_points: Int? = null,
    val total_check_ins: Int? = null,
    val consecutive_check_ins: Int? = null
)

@Serializable
data class ReplyPagination(
    val current_page: Int,
    val per_page: Int,
    val total: Int,
    val pages: Int
)