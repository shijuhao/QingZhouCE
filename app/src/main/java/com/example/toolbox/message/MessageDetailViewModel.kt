package com.example.toolbox.message

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.community.uploadImage
import com.example.toolbox.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class MessageDetailViewModel(
    private val token: String,
    private val chatType: Int,  // 1: 私聊, 2: 群聊
    private val chatId: Int     // 私聊为对方用户ID，群聊为群ID
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageDetailUiState(chatType = chatType, chatId = chatId))
    val uiState: StateFlow<MessageDetailUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _recallDialog = MutableStateFlow(RecallDialogState())
    val recallDialog: StateFlow<RecallDialogState> = _recallDialog.asStateFlow()

    private val _editDialog = MutableStateFlow(EditDialogState())
    val editDialog: StateFlow<EditDialogState> = _editDialog.asStateFlow()

    private val client = OkHttpClient()
    private val json = AppJson.json

    init {
        loadMessages(page = 1, isRefresh = true)
        if (chatType == 2) {
            loadGroupInfo()
        }
    }

    // 加载聊天记录
    fun loadMessages(page: Int, isRefresh: Boolean, beforeMsgId: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoadingMore = true, error = null)
            }

            try {
                val url = "${ApiAddress}chat/messages"
                val requestObj = buildJsonObject {
                    put("chat_type", chatType)
                    put("chat_id", chatId)
                    put("page", page)
                    put("per_page", 20)
                    if (beforeMsgId != null) {
                        put("before_msg_id", beforeMsgId)
                    }
                }
                
                val body = requestObj.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(body)
                    .build()

                val result = withContext(Dispatchers.IO) {
                    try {
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            val responseBody = response.body.string()
                            json.decodeFromString<GetMessagesResponse>(responseBody)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("MessageDetailVM", "loadMessages error", e)
                        null
                    }
                }

                if (result != null && result.status.code == 0) {
                    val hasMore = result.pagination?.let { it.page < it.pages } ?: false
                    
                    _uiState.update { current ->
                        val newMessages = if (isRefresh) {
                            result.messages.sortedBy { it.sendTime }
                        } else {
                            result.messages.sortedBy { it.sendTime } + current.messages
                        }
                        current.copy(
                            messages = newMessages,
                            canSend = result.canSend,
                            pagination = result.pagination,
                            hasMore = hasMore,
                            otherUser = result.otherUser, // 保存对方用户信息
                            isRefreshing = false,
                            isLoadingMore = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = result?.status?.msg ?: "请求失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e.message) }
            }
        }
    }

    fun refresh() {
        loadMessages(page = 1, isRefresh = true)
        if (chatType == 2) {
            loadGroupInfo()
        }
    }

    private fun loadGroupInfo() {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/detail"
                val requestBody = buildJsonObject {
                    put("group_id", chatId)
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
                            if (parsed.success) parsed.group else null
                        } catch (_: Exception) {
                            null
                        }
                    } else null
                }

                if (result != null) {
                    _uiState.update { it.copy(groupInfo = result) }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMore || currentState.messages.isEmpty()) {
            return
        }
        // Use the oldest message's ID as before_msg_id for pagination
        val oldestMsgId = currentState.messages.firstOrNull()?.id
        if (oldestMsgId != null) {
            loadMessages(page = 1, isRefresh = false, beforeMsgId = oldestMsgId.toString())
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        if (state.inputText.isBlank() && state.selectedImages.isEmpty()) {
            viewModelScope.launch {
                _toastMessage.emit("请输入内容或选择图片")
            }
            return
        }

        viewModelScope.launch {
            try {
                val url = "${ApiAddress}chat/send"
                val requestData = SendMessageRequest(
                    chatType = chatType,
                    chatId = chatId,
                    data = MessageData(text = state.inputText),
                    quoteMsgId = null
                )
                val bodyJson = json.encodeToString(requestData)
                val body = bodyJson.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(body)
                    .build()

                val result = withContext(Dispatchers.IO) {
                    try {
                        val response = client.newCall(request).execute()
                        val responseBody = response.body.string()
                        if (response.isSuccessful) {
                            json.decodeFromString<SendMessageResponse>(responseBody)
                        } else {
                            val status = try {
                                json.decodeFromString<ChatStatus>(responseBody)
                            } catch (_: Exception) {
                                ChatStatus(number = response.code, code = -1, msg = "发送失败")
                            }
                            SendMessageResponse(status = status)
                        }
                    } catch (e: Exception) {
                        SendMessageResponse(status = ChatStatus(-1, -1, e.message ?: "未知错误"))
                    }
                }

                if (result.status.code == 0) {
                    _uiState.update {
                        it.copy(
                            inputText = "",
                            selectedImages = emptyList()
                        )
                    }
                    refresh()
                } else {
                    _toastMessage.emit(result.status.msg)
                }
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "发送失败")
            }
        }
    }

    // 显示撤回确认弹窗
    fun showRecallDialog(messageId: String) {
        _recallDialog.update { RecallDialogState(isOpen = true, messageId = messageId) }
    }

    fun hideRecallDialog() {
        _recallDialog.update { RecallDialogState() }
    }

    // 撤回消息
    fun recallMessage() {
        val msgId = _recallDialog.value.messageId ?: return
        viewModelScope.launch {
            try {
                val url = if (chatType == 1) {
                    "${ApiAddress}private/delete_message"
                } else {
                    "${ApiAddress}group/recall"
                }
                val requestBody = buildJsonObject {
                    put("message_id", msgId)
                }.toString()
                val body = requestBody.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(body)
                    .build()

                val result = withContext(Dispatchers.IO) {
                    try {
                        val response = client.newCall(request).execute()
                        val responseBody = response.body.string()
                        json.decodeFromString<RecallResponse>(responseBody)
                    } catch (e: Exception) {
                        RecallResponse(success = false, message = "操作失败: ${e.message}")
                    }
                }

                if (result.success) {
                    _toastMessage.emit(result.message ?: "撤回成功")
                    // Update the message locally to show as recalled
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.msgId == msgId) {
                                    msg.copy(
                                        msgDeleteTime = System.currentTimeMillis(),
                                        content = result.message ?: "消息已撤回",
                                        isSystem = true
                                    )
                                } else msg
                            }
                        )
                    }
                } else {
                    _toastMessage.emit(result.message ?: "撤回失败")
                }
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "撤回失败")
            } finally {
                hideRecallDialog()
            }
        }
    }

    // 显示编辑弹窗
    fun showEditDialog(message: Message) {
        _editDialog.update {
            EditDialogState(
                isOpen = true,
                message = message,
                newContent = message.content,
                newImages = emptyList()
            )
        }
    }

    fun hideEditDialog() {
        _editDialog.update { EditDialogState() }
    }

    fun updateEditContent(newContent: String) {
        _editDialog.update { it.copy(newContent = newContent) }
    }

    fun updateEditImages(newImages: List<String>) {
        _editDialog.update { it.copy(newImages = newImages) }
    }

    // 编辑消息
    fun editMessage() {
        val dialogState = _editDialog.value
        val message = dialogState.message ?: return

        viewModelScope.launch {
            try {
                val url = if (chatType == 1) {
                    "${ApiAddress}private/edit_message"
                } else {
                    "${ApiAddress}group/edit_message"
                }
                
                val jsonObject = buildJsonObject {
                    put("message_id", message.msgId)
                    if (chatType == 1) {
                        put("new_content", dialogState.newContent)
                        put("new_images", buildJsonArray {
                            dialogState.newImages.forEach { add(JsonPrimitive(it)) }
                        })
                        put("new_is_markdown", false)
                    } else {
                        put("content", dialogState.newContent)
                        put("images", buildJsonArray {
                            dialogState.newImages.forEach { add(JsonPrimitive(it)) }
                        })
                        put("is_markdown", false)
                    }
                }
                val bodyJson = jsonObject.toString()
                val body = bodyJson.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(body)
                    .build()

                val result = withContext(Dispatchers.IO) {
                    try {
                        val response = client.newCall(request).execute()
                        val responseBody = response.body.string()
                        json.decodeFromString<ChatStatus>(responseBody)
                    } catch (e: Exception) {
                        ChatStatus(number = -1, code = -1, msg = "编辑失败: ${e.message}")
                    }
                }

                if (result.code == 0) {
                    _toastMessage.emit("编辑成功")
                    refresh()
                } else {
                    _toastMessage.emit(result.msg)
                }
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "编辑失败")
            } finally {
                hideEditDialog()
            }
        }
    }

    // 更新输入框内容
    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun handleImageSelected(uri: Uri?, context: Context, coroutineScope: CoroutineScope) {
        if (uiState.value.selectedImages.size >= 9) {
            viewModelScope.launch {
                _toastMessage.emit("最多只能上传9张图片")
            }
            return
        }
        if (uri == null) return
    
        coroutineScope.launch {
            try {
                val filePath = try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val tempFile = File(context.cacheDir, "temp_img_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(tempFile).use { output ->
                            inputStream.copyTo(output)
                        }
                        tempFile.absolutePath
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
    
                if (filePath == null) {
                    _toastMessage.emit("无法读取图片")
                    return@launch
                }
    
                val url = uploadImage(filePath, token, 3) { _: Int -> }
    
                if (url != null) {
                    addImage(url)
                    File(filePath).delete()
                } else {
                    _toastMessage.emit("图片上传失败")
                }
            } catch (e: Exception) {
                _toastMessage.emit("上传出错: ${e.message}")
            }
        }
    }

    fun addImage(imageUrl: String) {
        val current = _uiState.value.selectedImages
        if (current.size < 9) {
            _uiState.update { it.copy(selectedImages = current + imageUrl) }
        } else {
            viewModelScope.launch {
                _toastMessage.emit("最多选择9张图片")
            }
        }
    }

    fun removeImage(index: Int) {
        val current = _uiState.value.selectedImages.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.update { it.copy(selectedImages = current) }
        }
    }

    fun toggleMarkdown() {
        _uiState.update { it.copy(isMarkdown = !it.isMarkdown) }
    }

    fun connectWebSocket() {
        if (token.isNotBlank()) {
            val manager = ChatSocketManager.getInstance()
            // 先移除旧的观察者，避免重复注册
            manager.removeObserver(messageObserver)
            // 再添加新的观察者
            manager.addObserver(messageObserver)
            manager.connect(token)
        }
    }

    fun disconnectWebSocket() {
        val manager = ChatSocketManager.getInstance()
        manager.removeObserver(messageObserver)
    }

    private val messageObserver: (type: String, chatId: String, chatType: Int, message: Message) -> Unit = 
        { type, pushChatId, pushChatType, message ->
            when (type) {
                "new", "edit", "recall" -> {
                    val isCurrentChat = pushChatType == this.chatType && pushChatId == this.chatId.toString()
                    if (isCurrentChat) {
                        when (type) {
                            "new" -> addNewMessage(message)
                            "edit" -> updateMessage(message)
                            "recall" -> removeMessage(message.msgId)
                        }
                    }
                }
            }
        }

    private fun addNewMessage(message: Message) {
        // reverseLayout = false: 新消息添加到列表末尾
        _uiState.update { it.copy(messages = it.messages + message) }
    }

    private fun updateMessage(message: Message) {
        _uiState.update { state ->
            val updated = state.messages.map { if (it.msgId == message.msgId) message else it }
            state.copy(messages = updated)
        }
    }

    private fun removeMessage(msgId: String) {
        _uiState.update { state ->
            val updated = state.messages.filter { it.msgId != msgId }
            state.copy(messages = updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}

class MessageDetailViewModelFactory(
    private val token: String,
    private val chatType: Int,
    private val chatId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageDetailViewModel::class.java)) {
            return MessageDetailViewModel(token, chatType, chatId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}