package com.example.toolbox.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.community.uploadImage
import com.example.toolbox.data.AuditJoinRequest
import com.example.toolbox.data.CreateGroupRequest
import com.example.toolbox.data.CreateGroupResponse
import com.example.toolbox.data.CreateTagRequest
import com.example.toolbox.data.DeleteTagRequest
import com.example.toolbox.data.EditTagRequest
import com.example.toolbox.data.GenericResponse
import com.example.toolbox.data.GroupDetailResponse
import com.example.toolbox.data.GroupInfo
import com.example.toolbox.data.GroupInfoResponse
import com.example.toolbox.data.GroupJoinRequest
import com.example.toolbox.data.GroupJoinRequestsResponse
import com.example.toolbox.data.GroupMember
import com.example.toolbox.data.GroupMembersResponse
import com.example.toolbox.data.GroupTag
import com.example.toolbox.data.GroupTagsResponse
import com.example.toolbox.data.GroupUiState
import com.example.toolbox.data.KickMemberRequest
import com.example.toolbox.data.MuteRequest
import com.example.toolbox.data.SetAdminRequest
import com.example.toolbox.data.SetMemberTagRequest
import com.example.toolbox.data.TagResponse
import com.example.toolbox.data.UnmuteRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GroupViewModel(
    private val token: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _navigateToGroupInfo = MutableSharedFlow<GroupInfo>()
    val navigateToGroupInfo: SharedFlow<GroupInfo> = _navigateToGroupInfo.asSharedFlow()

    private val client = OkHttpClient()
    private val json = AppJson.json

    // 搜索群聊
    fun searchGroup() {
        val groupId = _uiState.value.searchGroupId.trim()
        if (groupId.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("请输入群号") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }

            try {
                val url = "${ApiAddress}group/info/$groupId"
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .get()
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body.string()
                        json.decodeFromString<GroupInfoResponse>(responseBody)
                    } else {
                        null
                    }
                }

                if (result?.success == true && result.group != null) {
                    _uiState.update { it.copy(isSearching = false, foundGroup = result.group) }
                    _navigateToGroupInfo.emit(result.group)
                } else {
                    val errorMsg = result?.message ?: "群聊不存在"
                    _uiState.update { it.copy(isSearching = false, error = errorMsg) }
                    _toastMessage.emit(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearching = false, error = e.message) }
                _toastMessage.emit(e.message ?: "搜索失败")
            }
        }
    }

    // 创建群聊
    fun createGroup(avatarFilePath: String?) {
        val state = _uiState.value
        val name = state.createGroupName.trim()
        
        if (name.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("请输入群名称") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }

            try {
                // 上传群聊头像
                val avatarUrl = if (!avatarFilePath.isNullOrBlank()) {
                    uploadImage(avatarFilePath, token, 3) { _: Int -> }
                } else {
                    null
                }

                val url = "${ApiAddress}group/create"
                val requestBody = CreateGroupRequest(
                    name = name,
                    avatarUrl = avatarUrl ?: "",
                    description = state.createGroupDescription.trim(),
                    isPrivate = state.createGroupIsPrivate
                )
                val bodyJson = json.encodeToString(requestBody)
                val body = bodyJson.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(body)
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body.string()
                        json.decodeFromString<CreateGroupResponse>(responseBody)
                    } else {
                        null
                    }
                }

                if (result?.success == true && result.group != null) {
                    _toastMessage.emit("群聊创建成功")
                    _uiState.update { 
                        it.copy(
                            isCreating = false,
                            showCreateDialog = false,
                            createGroupName = "",
                            createGroupDescription = "",
                            createGroupIsPrivate = false,
                            createGroupAvatarUrl = ""
                        )
                    }
                    // 导航到群聊信息页
                    _navigateToGroupInfo.emit(result.group)
                } else {
                    val errorMsg = result?.message ?: "创建失败"
                    _uiState.update { it.copy(isCreating = false, error = errorMsg) }
                    _toastMessage.emit(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreating = false, error = e.message) }
                _toastMessage.emit(e.message ?: "创建失败")
            }
        }
    }

    // UI 状态更新方法
    fun updateSearchGroupId(value: String) {
        _uiState.update { it.copy(searchGroupId = value) }
    }

    fun updateCreateGroupName(value: String) {
        _uiState.update { it.copy(createGroupName = value) }
    }

    fun updateCreateGroupDescription(value: String) {
        _uiState.update { it.copy(createGroupDescription = value) }
    }

    fun updateCreateGroupIsPrivate(value: Boolean) {
        _uiState.update { it.copy(createGroupIsPrivate = value) }
    }

    fun updateCreateGroupAvatarUrl(value: String) {
        _uiState.update { it.copy(createGroupAvatarUrl = value) }
    }

    fun showJoinDialog() {
        _uiState.update { it.copy(showJoinDialog = true, showDropdownMenu = false, searchGroupId = "", foundGroup = null) }
    }

    fun hideJoinDialog() {
        _uiState.update { it.copy(showJoinDialog = false, searchGroupId = "", foundGroup = null) }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, showDropdownMenu = false) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(
            showCreateDialog = false,
            createGroupName = "",
            createGroupDescription = "",
            createGroupIsPrivate = false,
            createGroupAvatarUrl = ""
        ) }
    }

    fun showDropdownMenu() {
        _uiState.update { it.copy(showDropdownMenu = true) }
    }

    fun hideDropdownMenu() {
        _uiState.update { it.copy(showDropdownMenu = false) }
    }
}

// GroupInfoUiState for GroupInfoActivity
data class GroupInfoUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val group: GroupInfo? = null,
    val members: List<GroupMember> = emptyList(),
    val isLoadingMembers: Boolean = false,
    val error: String? = null,
    val isJoined: Boolean = true,
    val isJoining: Boolean = false,
    val myRole: Int = 0,  // 0: 普通成员, 1: 管理员, 2: 群主
    
    // 标签相关
    val tags: List<GroupTag> = emptyList(),
    val isLoadingTags: Boolean = false,
    
    // 入群申请相关
    val joinRequests: List<GroupJoinRequest> = emptyList(),
    val isLoadingRequests: Boolean = false,
    
    // 操作状态
    val isLeaving: Boolean = false,
    val isDissolving: Boolean = false,
    val showLeaveDialog: Boolean = false,
    val showDissolveDialog: Boolean = false,
    val showTagDialog: Boolean = false,
    val editingTag: GroupTag? = null,
    val newTagName: String = "",
    val newTagColor: String = "#FF6B6B",
    
    // 编辑群聊信息相关
    val showEditDialog: Boolean = false,
    val editingName: String = "",
    val editingDescription: String = "",
    val editingAvatarUrl: String = "",
    val editingJoinVerification: Boolean = false,
    val editingShareEnabled: Boolean = false,
    val isEditing: Boolean = false
)

// GroupInfoViewModel for GroupInfoActivity
class GroupInfoViewModel(
    private val token: String,
    private val groupId: Int,
    initialGroupInfo: GroupInfo? = null,
    private val shareKey: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupInfoUiState(
        group = initialGroupInfo,
        isLoading = initialGroupInfo == null && shareKey == null
    ))
    val uiState: StateFlow<GroupInfoUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _joinSuccess = MutableSharedFlow<Unit>()
    val joinSuccess: SharedFlow<Unit> = _joinSuccess.asSharedFlow()

    private val client = OkHttpClient()
    private val json = AppJson.json

    // 获取当前有效的群ID（优先使用已加载的群信息中的ID）
    private val currentGroupId: Int
        get() = _uiState.value.group?.id ?: groupId

    init {
        if (shareKey != null) {
            // 如果是分享链接，先解析 shareKey
            resolveShareKey(shareKey)
        } else {
            loadGroupDetail()
            loadMembers(groupId)
            loadTags(groupId)
        }
    }

    fun refresh() {
        loadGroupDetail(isRefresh = true)
        loadMembers(groupId)
        loadTags(groupId)
    }

    // 解析分享Key获取群ID
    private fun resolveShareKey(key: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val url = "${ApiAddress}group/share_info?key=$key"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val result: Result<Int?> = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val jsonElement = Json.parseToJsonElement(responseBody)
                            val success = jsonElement.jsonObject["success"]?.jsonPrimitive?.booleanOrNull ?: false
                            if (success) {
                                // 从 group 对象中获取 id
                                val groupObj = jsonElement.jsonObject["group"]?.jsonObject
                                val groupIdStr = groupObj?.get("id")?.jsonPrimitive?.content
                                val groupId = groupIdStr?.toIntOrNull()
                                Result.success(groupId)
                            } else {
                                val message = jsonElement.jsonObject["message"]?.jsonPrimitive?.content ?: "解析失败"
                                Result.failure(Exception(message))
                            }
                        } catch (e: Exception) {
                            Result.failure(Exception("解析响应失败: ${e.message}"))
                        }
                    } else {
                        Result.failure(Exception("请求失败: ${response.code}"))
                    }
                }

                result.fold(
                    onSuccess = { resolvedGroupId ->
                        if (resolvedGroupId != null) {
                            // 使用解析到的群ID加载群信息
                            _uiState.update { it.copy(isLoading = false) }
                            // 更新groupId并加载群详情
                            loadGroupDetailWithId(resolvedGroupId, key)
                        } else {
                            _uiState.update { it.copy(isLoading = false, error = "无效的分享链接") }
                            _toastMessage.emit("无效的分享链接")
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { it.copy(isLoading = false, error = exception.message) }
                        _toastMessage.emit(exception.message ?: "解析分享链接失败")
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                _toastMessage.emit(e.message ?: "解析分享链接失败")
            }
        }
    }

    // 使用指定的群ID加载群详情（用于分享链接）
    private fun loadGroupDetailWithId(targetGroupId: Int, key: String? = null, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            }
            
            try {
                val url = "${ApiAddress}group/detail"
                val requestBody = buildJsonObject {
                    put("group_id", targetGroupId)
                }
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GroupDetailResponse>(responseBody)
                            if (parsed.success) {
                                Result.success(Triple(parsed.group, parsed.myStatus, parsed.myRole))
                            } else {
                                Result.failure(Exception(parsed.message ?: "获取群聊信息失败"))
                            }
                        } catch (e: Exception) {
                            Result.failure(Exception("解析响应失败: ${e.message}"))
                        }
                    } else {
                        try {
                            val parsed = json.decodeFromString<GroupDetailResponse>(responseBody)
                            Result.failure(Exception(parsed.message ?: "请求失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("请求失败: ${response.code}"))
                        }
                    }
                }

                result.fold(
                    onSuccess = { (groupInfo, myStatus, myRole) ->
                        if (groupInfo != null) {
                            val isJoined = myStatus == "joined"
                            _uiState.update { it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                group = groupInfo,
                                error = null,
                                isJoined = isJoined,
                                myRole = myRole ?: 0
                            ) }
                            loadMembers(targetGroupId)
                            loadTags(targetGroupId)
                        } else {
                            _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = "群聊不存在") }
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = exception.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    private fun loadMembers(targetGroupId: Int? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMembers = true) }
            
            try {
                val gid = targetGroupId ?: groupId
                val url = "${ApiAddress}group/members/$gid"
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .get()
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GroupMembersResponse>(responseBody)
                            Result.success(parsed.members)
                        } catch (e: Exception) {
                            Result.failure(Exception("解析成员列表失败: ${e.message}"))
                        }
                    } else {
                        Result.failure(Exception("获取成员列表失败"))
                    }
                }

                result.fold(
                    onSuccess = { members ->
                        _uiState.update { it.copy(
                            isLoadingMembers = false,
                            members = members
                        ) }
                    },
                    onFailure = { _ ->
                        _uiState.update { it.copy(isLoadingMembers = false) }
                        // 成员列表加载失败不显示错误，只是不显示成员
                    }
                )
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingMembers = false) }
            }
        }
    }

    private fun loadGroupDetail(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            }
            
            try {
                val url = "${ApiAddress}group/detail"
                val requestBody = buildJsonObject {
                    put("group_id", currentGroupId)
                }
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GroupDetailResponse>(responseBody)
                            if (parsed.success) {
                                Result.success(Triple(parsed.group, parsed.myStatus, parsed.myRole))
                            } else {
                                Result.failure(Exception(parsed.message ?: "获取群聊信息失败"))
                            }
                        } catch (e: Exception) {
                            Result.failure(Exception("解析响应失败: ${e.message}"))
                        }
                    } else {
                        try {
                            val parsed = json.decodeFromString<GroupDetailResponse>(responseBody)
                            Result.failure(Exception(parsed.message ?: "请求失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("请求失败: ${response.code}"))
                        }
                    }
                }

                result.fold(
                    onSuccess = { (groupInfo, myStatus, myRole) ->
                        if (groupInfo != null) {
                            // my_status: "joined", "pending", "kicked", "not_joined"
                            val isJoined = myStatus == "joined"
                            _uiState.update { it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                group = groupInfo,
                                error = null,
                                isJoined = isJoined,
                                myRole = myRole ?: 0
                            ) }
                        } else {
                            _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = "群聊不存在") }
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = exception.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    fun joinGroup() {
        val group = _uiState.value.group ?: return
        // 如果有shareKey，使用分享方式加入；否则普通加入
        joinGroupWithKey(group.id, shareKey)
    }
    
    // 使用分享Key加入群聊
    private fun joinGroupWithKey(groupId: Int, shareKey: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true) }

            try {
                val url = "${ApiAddress}group/join"
                val requestBody = buildJsonObject {
                    put("group_id", currentGroupId)
                    if (shareKey != null) {
                        put("type", 2)
                        put("share_key", shareKey)
                    } else {
                        put("type", 1)
                    }
                }
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val jsonElement = Json.parseToJsonElement(responseBody)
                            val success = jsonElement.jsonObject["success"]?.jsonPrimitive?.booleanOrNull ?: false
                            if (success) {
                                Result.success(true)
                            } else {
                                val message = jsonElement.jsonObject["message"]?.jsonPrimitive?.content ?: "加入失败"
                                Result.failure(Exception(message))
                            }
                        } catch (e: Exception) {
                            Result.failure(Exception("解析响应失败: ${e.message}"))
                        }
                    } else {
                        Result.failure(Exception("请求失败: ${response.code}"))
                    }
                }

                result.fold(
                    onSuccess = {
                        _toastMessage.emit("加入成功")
                        _uiState.update { it.copy(isJoining = false, isJoined = true) }
                        _joinSuccess.emit(Unit)
                        // 重新加载群详情和成员列表
                        loadGroupDetailWithId(groupId)
                    },
                    onFailure = { exception ->
                        _toastMessage.emit(exception.message ?: "加入失败")
                        _uiState.update { it.copy(isJoining = false) }
                    }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "加入失败")
                _uiState.update { it.copy(isJoining = false) }
            }
        }
    }
    
    // 加载标签
    fun loadTags(targetGroupId: Int? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTags = true) }
            
            try {
                val gid = targetGroupId ?: groupId
                val url = "${ApiAddress}group/tags/list/$gid"
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .get()
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GroupTagsResponse>(responseBody)
                            Result.success(parsed.tags)
                        } catch (e: Exception) {
                            Result.failure(Exception("解析标签失败: ${e.message}"))
                        }
                    } else {
                        Result.failure(Exception("获取标签失败"))
                    }
                }

                result.fold(
                    onSuccess = { tags ->
                        _uiState.update { it.copy(isLoadingTags = false, tags = tags) }
                    },
                    onFailure = {
                        _uiState.update { it.copy(isLoadingTags = false) }
                    }
                )
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingTags = false) }
            }
        }
    }
    
    // 创建标签
    fun createTag(name: String, color: String) {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/tag/create"
                val requestBody = CreateTagRequest(currentGroupId, name, color)
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<TagResponse>(responseBody)
                            if (parsed.success) Result.success(parsed.tag) else Result.failure(Exception(parsed.message ?: "创建失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("解析响应失败"))
                        }
                    } else {
                        Result.failure(Exception("请求失败"))
                    }
                }

                result.fold(
                    onSuccess = { tag ->
                        if (tag != null) {
                            _uiState.update { it.copy(tags = it.tags + tag, showTagDialog = false, newTagName = "", newTagColor = "#FF6B6B") }
                            _toastMessage.emit("标签创建成功")
                        }
                    },
                    onFailure = { e -> _toastMessage.emit(e.message ?: "创建失败") }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "创建失败")
            }
        }
    }
    
    // 编辑标签
    fun editTag(tagId: Int, name: String, color: String) {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/tag/edit"
                val requestBody = EditTagRequest(tagId, name, color)
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<TagResponse>(responseBody)
                            if (parsed.success) Result.success(parsed.tag) else Result.failure(Exception(parsed.message ?: "编辑失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("解析响应失败"))
                        }
                    } else {
                        Result.failure(Exception("请求失败"))
                    }
                }

                result.fold(
                    onSuccess = { tag ->
                        if (tag != null) {
                            _uiState.update { it.copy(
                                tags = it.tags.map { t -> if (t.id == tagId) tag else t },
                                showTagDialog = false,
                                editingTag = null,
                                newTagName = "",
                                newTagColor = "#FF6B6B"
                            ) }
                            _toastMessage.emit("标签编辑成功")
                        }
                    },
                    onFailure = { e -> _toastMessage.emit(e.message ?: "编辑失败") }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "编辑失败")
            }
        }
    }
    
    // 加载入群申请
    fun loadJoinRequests(targetGroupId: Int? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRequests = true) }
            
            try {
                val gid = targetGroupId ?: groupId
                val url = "${ApiAddress}group/pending_list/$gid"
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .get()
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GroupJoinRequestsResponse>(responseBody)
                            Result.success(parsed.pendingList)
                        } catch (_: Exception) {
                            Result.failure(Exception("解析申请列表失败"))
                        }
                    } else {
                        Result.failure(Exception("获取申请列表失败"))
                    }
                }

                result.fold(
                    onSuccess = { requests ->
                        _uiState.update { it.copy(isLoadingRequests = false, joinRequests = requests) }
                    },
                    onFailure = {
                        _uiState.update { it.copy(isLoadingRequests = false) }
                    }
                )
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingRequests = false) }
            }
        }
    }
    
    // 审核入群申请
    fun auditJoinRequest(userId: Int, approve: Boolean) {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/audit_join"
                val requestBody = AuditJoinRequest(currentGroupId, userId, if (approve) "approve" else "reject")
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GenericResponse>(responseBody)
                            if (parsed.success) Result.success(true) else Result.failure(Exception(parsed.message ?: "操作失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("解析响应失败"))
                        }
                    } else {
                        Result.failure(Exception("请求失败"))
                    }
                }

                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(joinRequests = it.joinRequests.filter { r -> r.userId != userId }) }
                        _toastMessage.emit(if (approve) "已通过申请" else "已拒绝申请")
                        loadMembers(groupId) // 刷新成员列表
                    },
                    onFailure = { e -> _toastMessage.emit(e.message ?: "操作失败") }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "操作失败")
            }
        }
    }
    
    // 退出群聊
    fun leaveGroup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLeaving = true) }
            
            try {
                val url = "${ApiAddress}group/leave"
                val requestBody = buildJsonObject { put("group_id", currentGroupId) }
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GenericResponse>(responseBody)
                            if (parsed.success) Result.success(true) else Result.failure(Exception(parsed.message ?: "退出失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("解析响应失败"))
                        }
                    } else {
                        Result.failure(Exception("请求失败"))
                    }
                }

                result.fold(
                    onSuccess = {
                        _toastMessage.emit("已退出群聊")
                        onSuccess()
                    },
                    onFailure = { e ->
                        _toastMessage.emit(e.message ?: "退出失败")
                        _uiState.update { it.copy(isLeaving = false, showLeaveDialog = false) }
                    }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "退出失败")
                _uiState.update { it.copy(isLeaving = false, showLeaveDialog = false) }
            }
        }
    }
    
    // 解散群聊
    fun dissolveGroup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDissolving = true) }
            
            try {
                val url = "${ApiAddress}group/dissolve"
                val requestBody = buildJsonObject { put("group_id", currentGroupId) }
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GenericResponse>(responseBody)
                            if (parsed.success) Result.success(true) else Result.failure(Exception(parsed.message ?: "解散失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("解析响应失败"))
                        }
                    } else {
                        Result.failure(Exception("请求失败"))
                    }
                }

                result.fold(
                    onSuccess = {
                        _toastMessage.emit("群聊已解散")
                        onSuccess()
                    },
                    onFailure = { e ->
                        _toastMessage.emit(e.message ?: "解散失败")
                        _uiState.update { it.copy(isDissolving = false, showDissolveDialog = false) }
                    }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "解散失败")
                _uiState.update { it.copy(isDissolving = false, showDissolveDialog = false) }
            }
        }
    }
    
    // UI状态更新方法
    fun showLeaveDialog() { _uiState.update { it.copy(showLeaveDialog = true) } }
    fun hideLeaveDialog() { _uiState.update { it.copy(showLeaveDialog = false) } }
    fun showDissolveDialog() { _uiState.update { it.copy(showDissolveDialog = true) } }
    fun hideDissolveDialog() { _uiState.update { it.copy(showDissolveDialog = false) } }
    fun showTagDialog(tag: GroupTag? = null) {
        _uiState.update { it.copy(
            showTagDialog = true,
            editingTag = tag,
            newTagName = tag?.name ?: "",
            newTagColor = tag?.color ?: "#FF6B6B"
        ) }
    }
    fun hideTagDialog() { _uiState.update { it.copy(showTagDialog = false, editingTag = null) } }
    fun updateNewTagName(name: String) { _uiState.update { it.copy(newTagName = name) } }
    fun updateNewTagColor(color: String) { _uiState.update { it.copy(newTagColor = color) } }
    
    // 编辑群聊信息相关方法
    fun showEditDialog() {
        val group = _uiState.value.group ?: return
        _uiState.update { it.copy(
            showEditDialog = true,
            editingName = group.name,
            editingDescription = group.description,
            editingAvatarUrl = group.avatarUrl,
            editingJoinVerification = group.joinVerification,
            editingShareEnabled = group.shareEnabled
        ) }
    }
    fun hideEditDialog() { _uiState.update { it.copy(showEditDialog = false) } }
    fun updateEditingName(name: String) { _uiState.update { it.copy(editingName = name) } }
    fun updateEditingDescription(description: String) { _uiState.update { it.copy(editingDescription = description) } }
    fun updateEditingAvatarUrl(avatarUrl: String) { _uiState.update { it.copy(editingAvatarUrl = avatarUrl) } }
    fun updateEditingJoinVerification(verification: Boolean) { _uiState.update { it.copy(editingJoinVerification = verification) } }
    fun updateEditingShareEnabled(enabled: Boolean) { _uiState.update { it.copy(editingShareEnabled = enabled) } }
    
    // 创建分享链接
    fun createShareLink(expireHours: Int = 0, onSuccess: (String) -> Unit) {
        val group = _uiState.value.group ?: return
        
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/create_share"
                val requestBody = buildJsonObject {
                    put("group_id", group.id)
                    put("expire_hours", expireHours)
                }
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val jsonElement = Json.parseToJsonElement(responseBody)
                            val success = jsonElement.jsonObject["success"]?.jsonPrimitive?.booleanOrNull ?: false
                            if (success) {
                                val shareUrl = jsonElement.jsonObject["share_url"]?.jsonPrimitive?.content
                                Result.success(shareUrl)
                            } else {
                                val message = jsonElement.jsonObject["message"]?.jsonPrimitive?.content ?: "创建失败"
                                Result.failure(Exception(message))
                            }
                        } catch (e: Exception) {
                            Result.failure(Exception("解析响应失败: ${e.message}"))
                        }
                    } else {
                        Result.failure(Exception("请求失败: ${response.code}"))
                    }
                }

                result.fold(
                    onSuccess = { shareUrl ->
                        if (shareUrl != null) {
                            _toastMessage.emit("分享链接创建成功")
                            onSuccess(shareUrl)
                        }
                    },
                    onFailure = { exception ->
                        _toastMessage.emit(exception.message ?: "创建分享链接失败")
                    }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "创建分享链接失败")
            }
        }
    }
    
    // 删除标签
    fun deleteTag(tagId: Int) {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/tag/delete"
                val requestBody = DeleteTagRequest(tagId)
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GenericResponse>(responseBody)
                            if (parsed.success) Result.success(true) else Result.failure(Exception(parsed.message ?: "删除失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("解析响应失败"))
                        }
                    } else {
                        Result.failure(Exception("请求失败"))
                    }
                }

                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(tags = it.tags.filter { t -> t.id != tagId }) }
                        _toastMessage.emit("标签已删除")
                    },
                    onFailure = { e -> _toastMessage.emit(e.message ?: "删除失败") }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "删除失败")
            }
        }
    }
    
    // 编辑群聊信息
    fun editGroupInfo(
        name: String? = null,
        description: String? = null,
        avatarUrl: String? = null,
        joinVerification: Boolean? = null,
        shareEnabled: Boolean? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEditing = true) }
            try {
                val url = "${ApiAddress}group/edit"
                val requestBody = buildJsonObject {
                    put("group_id", currentGroupId)
                    name?.let { put("name", it) }
                    description?.let { put("description", it) }
                    avatarUrl?.let { put("avatar_url", it) }
                    joinVerification?.let { put("join_verification", it) }
                    shareEnabled?.let { put("share_enabled", it) }
                }
                
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .put(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GenericResponse>(responseBody)
                            if (parsed.success) Result.success(true) else Result.failure(Exception(parsed.message ?: "编辑失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("解析响应失败"))
                        }
                    } else {
                        Result.failure(Exception("请求失败"))
                    }
                }

                result.fold(
                    onSuccess = {
                        _toastMessage.emit("群聊信息已更新")
                        loadGroupDetail() // 重新加载群详情以获取最新信息
                    },
                    onFailure = { e -> _toastMessage.emit(e.message ?: "编辑失败") }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "编辑失败")
            } finally {
                _uiState.update { it.copy(isEditing = false) }
            }
        }
    }
    
    // 给成员设置标签
    fun setMemberTag(userId: Int, tagId: Int) {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/tag/set"
                val requestBody = SetMemberTagRequest(currentGroupId, userId, tagId)
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GenericResponse>(responseBody)
                            if (parsed.success) Result.success(true) else Result.failure(Exception(parsed.message ?: "设置失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("解析响应失败"))
                        }
                    } else {
                        Result.failure(Exception("请求失败"))
                    }
                }

                result.fold(
                    onSuccess = {
                        _toastMessage.emit("标签已添加")
                        loadMembers(groupId) // 刷新成员列表
                    },
                    onFailure = { e -> _toastMessage.emit(e.message ?: "设置失败") }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "设置失败")
            }
        }
    }
    
    // 移除成员标签
    fun unsetMemberTag(userId: Int, tagId: Int) {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/tag/unset"
                val requestBody = SetMemberTagRequest(currentGroupId, userId, tagId)
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GenericResponse>(responseBody)
                            if (parsed.success) Result.success(true) else Result.failure(Exception(parsed.message ?: "移除失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("解析响应失败"))
                        }
                    } else {
                        Result.failure(Exception("请求失败"))
                    }
                }

                result.fold(
                    onSuccess = {
                        _toastMessage.emit("标签已移除")
                        loadMembers(groupId)
                    },
                    onFailure = { e -> _toastMessage.emit(e.message ?: "移除失败") }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "移除失败")
            }
        }
    }
}

class GroupViewModelFactory(private val token: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupViewModel::class.java)) {
            return GroupViewModel(token) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class GroupInfoViewModelFactory(
    private val token: String,
    private val groupId: Int,
    private val initialGroupInfo: GroupInfo? = null,
    private val shareKey: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupInfoViewModel::class.java)) {
            return GroupInfoViewModel(token, groupId, initialGroupInfo, shareKey) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// GroupMembersUiState for GroupMembersActivity
data class GroupMembersUiState(
    val isLoading: Boolean = true,
    val members: List<GroupMember> = emptyList(),
    val myRole: Int = 0,
    val error: String? = null
)

// GroupMembersViewModel for GroupMembersActivity
class GroupMembersViewModel(
    private val token: String,
    private val groupId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupMembersUiState())
    val uiState: StateFlow<GroupMembersUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val client = OkHttpClient()
    private val json = AppJson.json

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // 先获取群详情以获取 myRole
            try {
                val detailUrl = "${ApiAddress}group/detail"
                val detailRequestBody = buildJsonObject { put("group_id", groupId) }
                val detailRequest = Request.Builder()
                    .url(detailUrl)
                    .header("x-access-token", token)
                    .post(json.encodeToString(detailRequestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val detailResult = withContext(Dispatchers.IO) {
                    val response = client.newCall(detailRequest).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GroupDetailResponse>(responseBody)
                            if (parsed.success) parsed.myRole ?: 0 else 0
                        } catch (_: Exception) { 0 }
                    } else 0
                }

                // 获取成员列表
                val membersUrl = "${ApiAddress}group/members/$groupId"
                val membersRequest = Request.Builder()
                    .url(membersUrl)
                    .header("x-access-token", token)
                    .get()
                    .build()

                val membersResult = withContext(Dispatchers.IO) {
                    val response = client.newCall(membersRequest).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GroupMembersResponse>(responseBody)
                            Result.success(parsed.members)
                        } catch (_: Exception) {
                            Result.failure(Exception("解析成员列表失败"))
                        }
                    } else {
                        Result.failure(Exception("获取成员列表失败"))
                    }
                }

                membersResult.fold(
                    onSuccess = { members ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            members = members,
                            myRole = detailResult,
                            error = null
                        ) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun kickMember(userId: Int) {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/kick"
                val requestBody = KickMemberRequest(groupId, userId)
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GenericResponse>(responseBody)
                            if (parsed.success) Result.success(true) else Result.failure(Exception(parsed.message ?: "踢出失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("解析响应失败"))
                        }
                    } else {
                        Result.failure(Exception("请求失败"))
                    }
                }

                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(members = it.members.filter { m -> m.userId != userId }) }
                        _toastMessage.emit("已踢出该成员")
                    },
                    onFailure = { e -> _toastMessage.emit(e.message ?: "踢出失败") }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "踢出失败")
            }
        }
    }

    fun setAdmin(userId: Int, set: Boolean) {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/set_admin"
                val requestBody = SetAdminRequest(groupId, userId, if (set) "set" else "remove")
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GenericResponse>(responseBody)
                            if (parsed.success) Result.success(true) else Result.failure(Exception(parsed.message ?: "操作失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("解析响应失败"))
                        }
                    } else {
                        Result.failure(Exception("请求失败"))
                    }
                }

                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(
                            members = it.members.map { m ->
                                if (m.userId == userId) m.copy(role = if (set) 1 else 0)
                                else m
                            }
                        ) }
                        _toastMessage.emit(if (set) "已设置为管理员" else "已取消管理员")
                    },
                    onFailure = { e -> _toastMessage.emit(e.message ?: "操作失败") }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "操作失败")
            }
        }
    }

    fun muteMember(userId: Int, duration: Int) {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/mute"
                val requestBody = MuteRequest(groupId, userId, duration)
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GenericResponse>(responseBody)
                            if (parsed.success) Result.success(true) else Result.failure(Exception(parsed.message ?: "禁言失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("解析响应失败"))
                        }
                    } else {
                        Result.failure(Exception("请求失败"))
                    }
                }

                result.fold(
                    onSuccess = { _toastMessage.emit("已禁言 $duration 分钟") },
                    onFailure = { e -> _toastMessage.emit(e.message ?: "禁言失败") }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "禁言失败")
            }
        }
    }

    fun unmuteMember(userId: Int) {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/unmute"
                val requestBody = UnmuteRequest(groupId, userId)
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GenericResponse>(responseBody)
                            if (parsed.success) Result.success(true) else Result.failure(Exception(parsed.message ?: "解除禁言失败"))
                        } catch (_: Exception) {
                            Result.failure(Exception("解析响应失败"))
                        }
                    } else {
                        Result.failure(Exception("请求失败"))
                    }
                }

                result.fold(
                    onSuccess = { _toastMessage.emit("已解除禁言") },
                    onFailure = { e -> _toastMessage.emit(e.message ?: "解除禁言失败") }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "解除禁言失败")
            }
        }
    }
}

class GroupMembersViewModelFactory(
    private val token: String,
    private val groupId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupMembersViewModel::class.java)) {
            return GroupMembersViewModel(token, groupId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
