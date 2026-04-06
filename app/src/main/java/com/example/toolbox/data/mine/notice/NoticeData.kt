package com.example.toolbox.data.mine.notice

// 通知主数据类
data class Notification(
    val id: String,                     // 通知ID
    val type: Int,                      // 通知类型
    val title: String,                  // 标题（由客户端生成或服务端返回）
    val content: String,                // 通知内容
    val isNew: Boolean,                 // 是否未读
    val sender: Sender? = null,         // 发送者信息（系统通知为null）
    val timestamp: String = "",         // 友好时间格式（如“2分钟前”）
    val relatedId: Int = 0,             // 关联ID
    val relatedType: String = "",       // 关联类型（如“like”）
    // 以下为特定类型的额外信息，根据实际返回填充
    val likeInfo: LikeInfo? = null,
    val followInfo: FollowInfo? = null,
    val replyInfo: ReplyInfo? = null,
    val reviewInfo: ReviewInfo? = null,
    val friendRequestInfo: FriendRequestInfo? = null
)

data class SummaryData(
    val totalUnread: Int,
    val typeUnreadCounts: Map<String, Int>,
    val typeUnreadDetails: List<TypeUnreadDetail>
)

data class TypeUnreadDetail(
    val type: Int,
    val typeName: String,
    val unreadCount: Int
)

// 发送者信息
data class Sender(
    val id: Int = 0,                    // 发送者ID，系统通知为0
    val username: String,               // 发送者用户名，系统通知为"系统"
    val avatarUrl: String = ""          // 发送者头像URL
)

// 被点赞额外信息
data class LikeInfo(
    val messageId: Int,
    val messageTitle: String,
    val messageContent: String,
    val isValid: Boolean
)

// 被关注额外信息
data class FollowInfo(
    val isFollowingBack: Boolean
)

// 被回复额外信息
data class ReplyInfo(
    val originalMessageId: Int,
    val originalMessageTitle: String,
    val originalMessageContent: String,
    val originalAuthor: Sender
)

// 资源审核额外信息（type=4/5）
data class ReviewInfo(
    val resourceId: Int,
    val result: String,                 // "通过" 或 "未通过"
    val reviewer: Sender
)

// 好友请求额外信息（type=10）
data class FriendRequestInfo(
    val friendshipId: Int,
    val status: Int,                    // 0-待处理，1-已接受，2-已拒绝，3-已删除
    val createdAt: String
)

