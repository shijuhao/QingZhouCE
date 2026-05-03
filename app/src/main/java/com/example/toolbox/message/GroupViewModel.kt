package com.example.toolbox.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.community.uploadImage
import com.example.toolbox.data.CreateGroupRequest
import com.example.toolbox.data.CreateGroupResponse
import com.example.toolbox.data.GroupDetailResponse
import com.example.toolbox.data.GroupInfo
import com.example.toolbox.data.GroupInfoResponse
import com.example.toolbox.data.GroupMember
import com.example.toolbox.data.GroupMembersResponse
import com.example.toolbox.data.GroupUiState
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
import kotlinx.serialization.encodeToString
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

    // 加入群聊
    fun joinGroup(groupId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/join"
                val bodyJson = """{"group_id": $groupId}"""
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
                        val jsonObj = kotlinx.serialization.json.Json.parseToJsonElement(responseBody)
                        jsonObj.jsonObject["success"]?.jsonPrimitive?.booleanOrNull ?: false
                    } else false
                }

                if (result) {
                    _toastMessage.emit("加入成功")
                    onSuccess()
                } else {
                    _toastMessage.emit("加入失败")
                }
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "加入失败")
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
    val isJoined: Boolean = false,
    val isJoining: Boolean = false,
    val myRole: Int = 0  // 0: 普通成员, 1: 管理员, 2: 群主
)

// GroupInfoViewModel for GroupInfoActivity
class GroupInfoViewModel(
    private val token: String,
    private val groupId: Int,
    initialGroupInfo: GroupInfo? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupInfoUiState(
        group = initialGroupInfo,
        isLoading = initialGroupInfo == null
    ))
    val uiState: StateFlow<GroupInfoUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _joinSuccess = MutableSharedFlow<Unit>()
    val joinSuccess: SharedFlow<Unit> = _joinSuccess.asSharedFlow()

    private val client = OkHttpClient()
    private val json = AppJson.json

    init {
        if (_uiState.value.group == null) {
            loadGroupDetail()
        }
        loadMembers()
    }

    fun refresh() {
        loadGroupDetail(isRefresh = true)
        loadMembers()
    }

    private fun loadMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMembers = true) }
            
            try {
                val url = "${ApiAddress}group/members/$groupId"
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .get()
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
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
                    onFailure = { exception ->
                        _uiState.update { it.copy(isLoadingMembers = false) }
                        // 成员列表加载失败不显示错误，只是不显示成员
                    }
                )
            } catch (e: Exception) {
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
                    put("group_id", groupId)
                }
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
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
                            val parsed = json.decodeFromString<GroupDetailResponse>(responseBody ?: "{}")
                            Result.failure(Exception(parsed.message ?: "请求失败"))
                        } catch (e: Exception) {
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
        
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true) }

            try {
                val url = "${ApiAddress}group/join"
                val requestBody = buildJsonObject {
                    put("group_id", group.id)
                }
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
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
    private val initialGroupInfo: GroupInfo? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupInfoViewModel::class.java)) {
            return GroupInfoViewModel(token, groupId, initialGroupInfo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
