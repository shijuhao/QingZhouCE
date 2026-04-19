package com.example.toolbox.message

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.community.uploadImage
import com.example.toolbox.data.EditDialogState
import com.example.toolbox.data.EditMessageResponse
import com.example.toolbox.data.GetMessagesResponse
import com.example.toolbox.data.Message
import com.example.toolbox.data.MessageDetailUiState
import com.example.toolbox.data.RecallDialogState
import com.example.toolbox.data.SendMessageRequest
import com.example.toolbox.data.SendMessageResponse
import com.example.toolbox.data.SimpleResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MessageDetailViewModel(
    private val token: String,
    private val otherUserId: Int  // 聊天对象ID，从Intent传入
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageDetailUiState())
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
    }

    // 加载聊天记录
    fun loadMessages(page: Int, isRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoadingMore = true, error = null)
            }

            try {
                val url = "${ApiAddress}private/get_messages"
                val requestBody = json.encodeToString(mapOf(
                    "user_id" to otherUserId,
                    "page" to page,
                    "per_page" to 20
                ))
                val body = requestBody.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(body)
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body.string()
                        json.decodeFromString<GetMessagesResponse>(responseBody)
                    } else {
                        null
                    }
                }

                if (result?.success == true) {
                    _uiState.update { current ->
                        val newMessages = if (isRefresh) {
                            result.messages
                        } else {
                            current.messages + result.messages
                        }
                        current.copy(
                            relationship = result.relationship,
                            messages = newMessages,
                            otherUser = result.otherUser,
                            pagination = result.pagination,
                            hasMore = result.pagination.currentPage.let { it < result.pagination.pages },
                            isRefreshing = false,
                            isLoadingMore = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = "请求失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e.message) }
            }
        }
    }

    fun refresh() {
        loadMessages(page = 1, isRefresh = true)
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMore || currentState.pagination == null) {
            return
        }
        val nextPage = currentState.pagination.currentPage + 1
        loadMessages(page = nextPage, isRefresh = false)
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
                val url = "${ApiAddress}private/send_message"
                val requestBody = SendMessageRequest(
                    receiverId = otherUserId,
                    content = state.inputText.takeIf { it.isNotBlank() },
                    images = state.selectedImages.takeIf { it.isNotEmpty() },
                    isMarkdown = state.isMarkdown
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
                        responseBody.let { json.decodeFromString<SendMessageResponse>(it) }
                    } else {
                        val errorBody = response.body.string()
                        errorBody.let { json.decodeFromString<SimpleResponse>(it) }
                    }
                }

                when (result) {
                    is SendMessageResponse -> {
                        if (result.success) {
                            _uiState.update {
                                it.copy(
                                    inputText = "",
                                    selectedImages = emptyList(),
                                    dailyMessagesLeft = result.dailyMessagesLeft
                                )
                            }
                            refresh()
                        } else {
                            _toastMessage.emit(result.message)
                        }
                    }
                    is SimpleResponse -> {
                        _toastMessage.emit(result.message)
                    }
                    else -> {
                        _toastMessage.emit("发送失败，未知响应")
                    }
                }
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "发送失败")
            }
        }
    }

    // 显示撤回确认弹窗
    fun showRecallDialog(messageId: Int) {
        _recallDialog.update { RecallDialogState(isOpen = true, messageId = messageId) }
    }

    fun hideRecallDialog() {
        _recallDialog.update { RecallDialogState() }
    }

    // 撤回消息
    fun recallMessage() {
        val messageId = _recallDialog.value.messageId ?: return
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}private/delete_message"
                val requestBody = json.encodeToString(mapOf("message_id" to messageId))
                val body = requestBody.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(body)
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body.string()
                        responseBody.let { json.decodeFromString<SimpleResponse>(it) }
                    } else {
                        val errorBody = response.body.string()
                        errorBody.let { json.decodeFromString<SimpleResponse>(it) }
                    }
                }

                if (result.success) {
                    _toastMessage.emit(result.message)
                    refresh()
                } else {
                    _toastMessage.emit(result.message)
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
                newImages = message.images
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
                val url = "${ApiAddress}private/edit_message"
                val jsonObject = buildJsonObject {
                    put("message_id", message.id)
                    put("new_content", dialogState.newContent)
                    put("new_images", buildJsonArray {
                        dialogState.newImages.forEach { add(JsonPrimitive(it)) }
                    })
                    put("new_is_markdown", false)
                }
                val bodyJson = jsonObject.toString()
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
                        responseBody.let { json.decodeFromString<EditMessageResponse>(it) }
                    } else {
                        val errorBody = response.body.string()
                        errorBody.let { json.decodeFromString<SimpleResponse>(it) }
                    }
                }

                when (result) {
                    is EditMessageResponse -> {
                        if (result.success) {
                            _toastMessage.emit(result.message)
                            refresh()
                        } else {
                            _toastMessage.emit(result.message)
                        }
                    }
                    is SimpleResponse -> {
                        _toastMessage.emit(result.message)
                    }
                    else -> {
                        _toastMessage.emit("编辑失败，未知响应")
                    }
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
                        val tempFile = java.io.File(context.cacheDir, "temp_img_${System.currentTimeMillis()}.jpg")
                        java.io.FileOutputStream(tempFile).use { output ->
                            inputStream.copyTo(output)
                        }
                        tempFile.absolutePath
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
    
                if (filePath == null) {
                    _toastMessage.emit("无法读取图片")
                    return@launch
                }
    
                val url = uploadImage(filePath, token, 3) { _ -> }
    
                if (url != null) {
                    addImage(url)
                    java.io.File(filePath).delete()
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
            val manager = PrivateChatSocketManager.getInstance()
            manager.addObserver(messageObserver)
            manager.connect(token)
        }
    }

    fun disconnectWebSocket() {
        val manager = PrivateChatSocketManager.getInstance()
        manager.removeObserver(messageObserver)
    }

    private val messageObserver: (type: String, message: Message) -> Unit = { type, message ->
        when (type) {
            "new", "edit", "recall" -> {
                if (message.senderId == otherUserId || message.receiverId == otherUserId) {
                    when (type) {
                        "new" -> {
                            val processedMessage = message.copy(
                                isMine = message.senderId != otherUserId
                            )
                            addNewMessage(processedMessage)
                        }
                        "edit" -> {
                            val processedMessage = message.copy(
                                isMine = message.senderId != otherUserId
                            )
                            updateMessage(processedMessage)
                        }
                        "recall" -> removeMessage(message.id)
                    }
                }
            }
        }
    }

    private fun addNewMessage(message: Message) {
        val currentMessages = _uiState.value.messages.toMutableList()
        if (currentMessages.none { it.id == message.id }) {
            currentMessages.add(0, message)
            _uiState.update { it.copy(messages = currentMessages) }
        }
    }

    private fun updateMessage(message: Message) {
        val currentMessages = _uiState.value.messages.toMutableList()
        val index = currentMessages.indexOfFirst { it.id == message.id }
        if (index != -1) {
            currentMessages[index] = message
            _uiState.update { it.copy(messages = currentMessages) }
        }
    }

    private fun removeMessage(messageId: Int) {
        val currentMessages = _uiState.value.messages.toMutableList()
        val index = currentMessages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val message = currentMessages[index]
            val updatedMessage = message.copy(
                isRecalled = true,
                recallHint = "消息已撤回",
                content = "",
                images = emptyList()
            )
            currentMessages[index] = updatedMessage
            _uiState.update { it.copy(messages = currentMessages) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}

class MessageDetailViewModelFactory(
    private val token: String,
    private val otherUserId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MessageDetailViewModel(token, otherUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}