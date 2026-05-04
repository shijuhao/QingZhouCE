package com.example.toolbox.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class MessageTag(
    val id: Int,
    val text: String,
    val color: String
)

@Serializable
data class MessageSender(
    @SerialName("chat_id") val chatId: String,
    @SerialName("chat_type") val chatType: Int,
    val name: String,
    @SerialName("avatar_url") val avatarUrl: String,
    val tag: List<MessageTag> = emptyList(),
    @SerialName("tag_old") val tagOld: String? = null
)

@Serializable
data class EditRecord(
    @SerialName("editor_id") val editorId: Int,
    @SerialName("editor_username") val editorUsername: String,
    @SerialName("edit_time") val editTime: String,
    @SerialName("old_content") val oldContent: String,
    @SerialName("new_content") val newContent: String,
    @SerialName("old_images") val oldImages: List<String> = emptyList(),
    @SerialName("new_images") val newImages: List<String> = emptyList(),
    @SerialName("old_is_markdown") val oldIsMarkdown: Boolean = false,
    @SerialName("new_is_markdown") val newIsMarkdown: Boolean = false
)

@Serializable
data class Message(
    val id: Int? = null,
    @SerialName("msg_id") val msgId: String = "",
    @SerialName("sender_id") val senderId: Int? = null,
    val sender: MessageSender? = null,
    @SerialName("sender_username") val senderUsername: String? = null,
    @SerialName("sender_avatar") val senderAvatar: String? = null,
    @SerialName("receiver_id") val receiverId: Int? = null,
    val direction: String = "left",
    @SerialName("content_type") val contentType: Int = 0,
    val content: String = "",
    val images: List<String> = emptyList(),
    @SerialName("is_markdown") val isMarkdown: Boolean = false,
    @SerialName("is_system") val isSystem: Boolean = false,
    val timestamp: String? = null,
    @SerialName("timestamp_display") val timestampDisplay: String? = null,
    @SerialName("send_time") val sendTime: Long = 0,
    @SerialName("send_time_formatted") val sendTimeFormatted: String? = null,
    @SerialName("send_time_display") val sendTimeDisplay: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("is_recalled") val isRecalled: Boolean = false,
    @SerialName("recall_hint") val recallHint: String? = null,
    @SerialName("recall_time") val recallTime: String? = null,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("is_read") val isRead: Boolean = true,
    @SerialName("is_mine") val isMine: Boolean = false,
    @SerialName("edit_records") val editRecords: List<EditRecord> = emptyList(),
    @SerialName("msg_seq") val msgSeq: Long = 0,
    @SerialName("quote_msg_id") val quoteMsgId: String? = null,
    @SerialName("quote_msg_info") val quoteMsgInfo: QuoteMsgInfo? = null,
    @SerialName("edit_time") val editTime: Long? = null,
    @SerialName("edit_time_formatted") val editTimeFormatted: String? = null,
    @SerialName("edit_time_display") val editTimeDisplay: String? = null,
    @SerialName("msg_delete_time") val msgDeleteTime: Long? = null,
    @SerialName("msg_delete_time_formatted") val msgDeleteTimeFormatted: String? = null,
    @SerialName("msg_delete_time_display") val msgDeleteTimeDisplay: String? = null
)

val Message.displayName: String
    get() = sender?.name ?: senderUsername ?: ""

val Message.displayAvatar: String
    get() = sender?.avatarUrl ?: senderAvatar ?: ""

val Message.effectiveMsgId: String
    get() = msgId.ifEmpty { id?.toString() ?: "" }

@Serializable
data class QuoteMsgInfo(
    val id: Int,
    val content: String = "",
    @SerialName("content_type") val contentType: Int = 0,
    val images: List<String> = emptyList(),
    @SerialName("is_markdown") val isMarkdown: Boolean = false,
    @SerialName("is_system") val isSystem: Boolean = false,
    @SerialName("sender_id") val senderId: Int = 0,
    @SerialName("sender_username") val senderUsername: String = "",
    val status: Int = 0
)

@Serializable
data class ChatStatus(
    val number: Int,
    val code: Int,
    val msg: String
)

@Serializable
data class OtherUser(
    val id: Int,
    val username: String,
    val avatar: String = "",
    val title: String = "",
    @SerialName("title_status") val titleStatus: Int = 0
)

@Serializable
data class GetMessagesResponse(
    val status: ChatStatus,
    val messages: List<Message> = emptyList(),
    @SerialName("can_send") val canSend: Boolean = true,
    val pagination: MessagePagination? = null,
    @SerialName("other_user") val otherUser: OtherUser? = null,
    @SerialName("chat_background_url") val chatBackgroundUrl: String = "",
    @SerialName("is_friend") val isFriend: Boolean = true,
    val relationship: String = "friend",
    @SerialName("temp_chat_expired") val tempChatExpired: Boolean = false,
    @SerialName("is_admin") val isAdmin: Boolean = false
)

@Serializable
data class MessagePagination(
    val page: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Int,
    val pages: Int
)

@Serializable
data class GetMessagesRequest(
    @SerialName("chat_type") val chatType: Int,
    @SerialName("chat_id") val chatId: Int,
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
    @SerialName("before_msg_id") val beforeMsgId: Int? = null
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_type") val chatType: Int,
    @SerialName("chat_id") val chatId: Int,
    val data: MessageData,
    @SerialName("quote_msg_id") val quoteMsgId: String? = null
)

@Serializable
data class MessageData(
    val text: String = "",
    val images: List<String> = emptyList(),
    @SerialName("is_markdown") val isMarkdown: Boolean = false
)

@Serializable
data class SendMessageResponse(
    val status: ChatStatus,
    val msg: Message? = null
)

// ---------- UI 状态 ----------
data class MessageDetailUiState(
    val messages: List<Message> = emptyList(),
    val chatType: Int = 1, // 1: private, 2: group
    val chatId: Int = 0,
    val canSend: Boolean = true,
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
    val isChatExpired: Boolean = false,
    val dailyMessagesLeft: Int = 0,
    val groupInfo: GroupInfo? = null,
    val otherUser: OtherUser? = null,
    val isAdmin: Boolean = false,
    val relationship: String = "friend"
)

// 撤回确认弹窗状态
data class RecallDialogState(
    val isOpen: Boolean = false,
    val messageId: String? = null
)

// 编辑弹窗状态
data class EditDialogState(
    val isOpen: Boolean = false,
    val message: Message? = null,
    val newContent: String = "",
    val newImages: List<String> = emptyList()
)

@Serializable
data class GroupCreator(
    val id: Int,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String = ""
)

@Serializable
data class GroupMember(
    @SerialName("user_id") val userId: Int,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String = "",
    val nickname: String = "",
    val role: Int = 0,  // 0: 普通成员, 1: 管理员, 2: 群主
    val tags: List<String> = emptyList(),
    @SerialName("joined_at") val joinedAt: String = ""
)

@Serializable
data class GroupMembersResponse(
    val members: List<GroupMember> = emptyList()
)

@Serializable
data class GroupInfo(
    val id: Int,
    val name: String,
    @SerialName("avatar_url") val avatarUrl: String = "",
    val description: String = "",
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("join_verification") val joinVerification: Boolean = false,
    @SerialName("members_count") val membersCount: Int = 0,
    val status: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    val creator: GroupCreator? = null
)

@Serializable
data class GroupDetailResponse(
    val success: Boolean,
    val group: GroupInfo? = null,
    @SerialName("my_status") val myStatus: String? = null,
    @SerialName("my_role") val myRole: Int? = null,
    val message: String? = null
)

@Serializable
data class GroupInfoResponse(
    val success: Boolean,
    val group: GroupInfo? = null,
    val message: String? = null
)

@Serializable
data class RecallResponse(
    val success: Boolean = false,
    val message: String? = null,
    val recall_hint: String? = null,
    val recall_time: String? = null
)

@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String,
    @SerialName("is_private") val isPrivate: Boolean,
    @SerialName("avatar_url") val avatarUrl: String = ""
)

@Serializable
data class CreateGroupResponse(
    val success: Boolean,
    val group: GroupInfo? = null,
    val message: String? = null
)

data class GroupUiState(
    val showDropdownMenu: Boolean = false,
    val showJoinDialog: Boolean = false,
    val showCreateDialog: Boolean = false,

    val searchGroupId: String = "",
    val isSearching: Boolean = false,
    val foundGroup: GroupInfo? = null,

    val createGroupName: String = "",
    val createGroupDescription: String = "",
    val createGroupIsPrivate: Boolean = false,
    val createGroupAvatarUrl: String = "",
    val isCreating: Boolean = false,

    val error: String? = null
)

// 群标签
@Serializable
data class GroupTag(
    val id: Int,
    val name: String,
    val color: String = "#FF6B6B"
)

@Serializable
data class GroupTagsResponse(
    val success: Boolean = false,
    val tags: List<GroupTag> = emptyList()
)

@Serializable
data class CreateTagRequest(
    @SerialName("group_id") val groupId: Int,
    val name: String,
    val color: String
)

@Serializable
data class EditTagRequest(
    @SerialName("tag_id") val tagId: Int,
    val name: String,
    val color: String
)

@Serializable
data class DeleteTagRequest(
    @SerialName("tag_id") val tagId: Int
)

@Serializable
data class SetMemberTagRequest(
    @SerialName("group_id") val groupId: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("tag_id") val tagId: Int
)

@Serializable
data class TagResponse(
    val success: Boolean = false,
    val tag: GroupTag? = null,
    val message: String? = null
)

// 群加入申请
@Serializable
data class GroupJoinRequest(
    @SerialName("user_id") val userId: Int,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("apply_time") val applyTime: String = ""
)

@Serializable
data class GroupJoinRequestsResponse(
    val success: Boolean = false,
    @SerialName("pending_list") val pendingList: List<GroupJoinRequest> = emptyList()
)

@Serializable
data class AuditJoinRequest(
    @SerialName("group_id") val groupId: Int,
    @SerialName("user_id") val userId: Int,
    val action: String  // "approve" or "reject"
)

// 群操作请求
@Serializable
data class GroupActionRequest(
    @SerialName("group_id") val groupId: Int
)

@Serializable
data class KickMemberRequest(
    @SerialName("group_id") val groupId: Int,
    @SerialName("user_id") val userId: Int
)

@Serializable
data class SetAdminRequest(
    @SerialName("group_id") val groupId: Int,
    @SerialName("user_id") val userId: Int,
    val action: String  // "set" or "remove"
)

@Serializable
data class MuteRequest(
    @SerialName("group_id") val groupId: Int,
    @SerialName("user_id") val userId: Int,
    val duration: Int  // 分钟
)

@Serializable
data class UnmuteRequest(
    @SerialName("group_id") val groupId: Int,
    @SerialName("user_id") val userId: Int
)

// 通用响应
@Serializable
data class GenericResponse(
    val success: Boolean = false,
    val message: String? = null
)
