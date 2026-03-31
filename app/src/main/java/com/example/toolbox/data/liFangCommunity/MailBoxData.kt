package com.example.toolbox.data.liFangCommunity

// --- 数据模型：提醒 (Notification) ---
data class Notification(
    val id: String,          // 提醒的唯一ID，可以从帖子链接中提取
    val senderInfo: String,  // 发送者信息，如 "💬 admin01"
    val contentHtml: String, // 提醒内容，包含链接等 HTML
    val time: String,        // 提醒时间
    val isRead: Boolean,     // 是否已读
    val postLink: String     // 关联帖子的链接，如 "/#post-56"
)

// --- UI 状态 ---
data class MailboxUiState( // 保持 MailboxUiState 名称，但内容是 Notification
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false // 用户登录状态
)