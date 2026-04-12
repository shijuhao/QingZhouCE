@file:Suppress("PropertyName")

package com.example.toolbox.data.community

import kotlinx.serialization.Serializable

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