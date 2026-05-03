package com.example.toolbox.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.community.uploadImage
import com.example.toolbox.data.CreateGroupRequest
import com.example.toolbox.data.CreateGroupResponse
import com.example.toolbox.data.GroupInfo
import com.example.toolbox.data.GroupInfoResponse
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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
                // 先上传头像（如果有）
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

class GroupViewModelFactory(private val token: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupViewModel::class.java)) {
            return GroupViewModel(token) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
