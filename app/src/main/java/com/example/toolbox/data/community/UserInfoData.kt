package com.example.toolbox.data.community

import kotlinx.serialization.Serializable

data class UserInfo(
    val username: String,
    val avatar: String,
    val bio: String,
    val level: Int,
    val experience: Int,
    val followersCount: Int,
    val followingCount: Int,
    val gold: Int,
    val userId: Int,
    val isFollowed: Boolean,
    val isBanned: Boolean,
    val banEndTime: String?,
    val lastActivity: String,
    val title: String?,
    val titleStatus: Int,
    val backgroundUrl: String?
)

@Serializable
data class UserReferencedMessage(
    val sender_username: String,
    val content: String,
    val message_type: Int,
    val image_url: String?
)

@Serializable
data class UserMessage(
    val id: Int,
    val username: String,
    val avatar: String,
    val content: String,
    val type: Int,
    val images: List<String>,
    val title: String?,
    val likeCount: Int,
    val time: String,
    val is_liked: Boolean,
    val isMarkdown: Boolean,
    val is_referenced: Boolean,
    val referenced_message: UserReferencedMessage?
)