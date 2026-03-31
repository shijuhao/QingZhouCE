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
    val visible_to: List<Int>?, // 可见用户ID列表
    val category_name: String?,
    val like_users: List<LikeUser>?, // 私密消息时可能为 null
    val sender_info: SenderInfo?,
    val is_edited: Int,
    val edit_records: List<LocalEditRecord>?,
    val referenced_message: ReferencedMessageInfo? // 递归引用
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
    val type: String, // text/image/rich/post/markdown
    val text: String?,
    val url: String?, // type=image 时
    val title: String?, // type=post 时
    val content: String?,
    val images: List<String>?
)

@Serializable
data class SenderInfo(
    val id: Int,
    val username: String,
    val avatar_url: String?,
    val title: String?,
    val gold: Int?, // 私密消息不返回，设为可选
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
    val username: String,
    val avatar_url: String
)