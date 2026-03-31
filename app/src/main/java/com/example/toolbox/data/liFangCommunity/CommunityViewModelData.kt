package com.example.toolbox.data.liFangCommunity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Comment(
    val author: String,
    val time: String,
    val contentHtml: String
) : Parcelable

@Parcelize
data class Post(
    val id: String,
    val title: String,
    val author: String,
    val time: String,
    val contentHtml: String,
    val comments: List<Comment> = emptyList()
) : Parcelable

data class CommunityUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)