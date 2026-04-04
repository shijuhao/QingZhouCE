@file:Suppress("PropertyName")

package com.example.toolbox.data.community

import kotlinx.serialization.Serializable

@Serializable
data class CategoryStats(
    val has_unread: Boolean = false,
    val unread_count: Int = 0
)

@Serializable
data class Category(
    val id: Int,
    val name: String,
    val description: String,
    val avatar_url: String,
    val created_at: String,
    val stats: CategoryStats,
    val latest_message: LatestMessage?
)

@Serializable
data class LatestMessage(
    val content: String,
    val message_id: Int,
    val message_type: Int,
    val sender_username: String,
    val timestamp: String,
    val image_url: String? = null
)

@Serializable
data class CategoriesResponse(
    val categories: List<Category>,
    val success: Boolean
)

@Serializable
data class ApiResponse(
    val success: Boolean,
    val messages: List<Message>,
    val pagination: Pagination
)

@Serializable
data class EditRecord(
    val old_content: String?,
    val new_content: String?,
    val old_title: String?,
    val new_title: String?,
    val edit_time: String,
    val editor_username: String?
)

@Serializable
data class Message(
    val message_id: Int,
    val userid: Int,
    val username: String,
    val avatar_url: String?,
    val user_level: Int,
    val tag: String,
    val tag_status: Int,
    val content: Content,
    val message_type: Int,
    val timestamp_user: String,
    var like_count: Int,
    var is_liked: Boolean,
    val is_edited: Int,
    val edit_records: List<EditRecord> = emptyList(),
    val is_referenced: Boolean,
    val referenced_message: ReferencedMessage?,
    val reference_count: Int = 0,
    val is_private: Boolean = false,
    val visible_to: List<Int>? = null,
    val is_markdown: Boolean = false,

    //ws
    val category_id: Int? = 0,
    val action: String? = ""
)

@Serializable
data class Content(
    val type: String,
    val text: String?,
    val url: String?,
    val images: List<String>? = emptyList(),
    val title: String?,
    val content: String?
)

@Serializable
data class ReferencedMessage(
    val content: String,
    val sender_username: String,
    val image_url: String?
)

@Serializable
data class Pagination(
    val current_page: Int,
    val total_pages: Int,
    val total_messages: Int
)