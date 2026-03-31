package com.example.toolbox.data.mine.notice

data class Notification(
    val id: String,
    val type: Int,
    val title: String,
    val content: String,
    val isNew: Boolean,
    val sender: Sender? = null
)

data class Sender(
    val username: String
)