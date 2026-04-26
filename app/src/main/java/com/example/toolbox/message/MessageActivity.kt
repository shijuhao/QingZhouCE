@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.message

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.example.toolbox.ApiAddress
import com.example.toolbox.TokenManager
import com.example.toolbox.community.UserInfoActivity
import com.example.toolbox.data.EditDialogState
import com.example.toolbox.data.Message
import com.example.toolbox.ui.theme.ToolBoxTheme
import com.example.toolbox.utils.MarkdownRenderer
import com.example.toolbox.utils.MultiImageViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private suspend fun sendFriendRequest(token: String, friendId: Int): Boolean {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val jsonBody = JSONObject().put("friend_id", friendId).toString()
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = jsonBody.toRequestBody(mediaType)

    val request = Request.Builder()
        .url("${ApiAddress}friends/send_request")
        .post(requestBody)
        .addHeader("x-access-token", token)
        .build()

    return withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext false
                }

                val responseBody = response.body.string()
                if (responseBody.isBlank()) {
                    return@withContext false
                }

                val jsonResponse = try {
                    JSONObject(responseBody)
                } catch (_: Exception) {
                    return@withContext false
                }

                jsonResponse.optBoolean("success", false)
            }
        } catch (e: Exception) {
            Log.e("NetworkError", "请求失败", e)
            false
        }
    }
}

class MessageDetailActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 从Intent获取聊天对象ID
        val otherUserId = intent.getIntExtra("user_id", 0)

        setContent {
            ToolBoxTheme {
                val token = TokenManager.get(this)
                val context = LocalContext.current
                val viewModel: MessageDetailViewModel = viewModel(
                    factory = token?.let { MessageDetailViewModelFactory(it, otherUserId) }
                )
                val uiState by viewModel.uiState.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                if (uiState.otherUser != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                startActivity(
                                                    Intent(this, UserInfoActivity::class.java).apply {
                                                        putExtra("userId", uiState.otherUser!!.id)
                                                    }
                                                )
                                            }
                                    ) {
                                        AsyncImage(
                                            model = uiState.otherUser!!.avatar,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // 用户名和称号
                                        Column {
                                            Text(
                                                text = uiState.otherUser!!.username,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            if (uiState.otherUser!!.title.isNotBlank()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        modifier = Modifier.size(14.dp),
                                                        contentDescription = null,
                                                        imageVector = Icons.Default.CheckCircle,
                                                        tint = when (uiState.otherUser?.titleStatus) {
                                                            1 -> MaterialTheme.colorScheme.error
                                                            2 -> MaterialTheme.colorScheme.tertiary
                                                            4 -> MaterialTheme.colorScheme.primary
                                                            else -> MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = uiState.otherUser?.title ?: "",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // 加载中或未知用户
                                    Text("聊天详情")
                                }
                            },
                            navigationIcon = {
                                FilledTonalIconButton(onClick = { finish() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回"
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(top = innerPadding.calculateTopPadding())) {
                        MessageDetailScreen(innerPadding, viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessageDetailScreen(
    innerPadding: PaddingValues,
    viewModel: MessageDetailViewModel
) {
    val context = LocalContext.current
    val token = TokenManager.get(context)
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val uiState by viewModel.uiState.collectAsState()
    var previousMessages by remember { mutableStateOf(uiState.messages) }

    LaunchedEffect(Unit) {
        viewModel.connectWebSocket()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnectWebSocket()
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.handleImageSelected(uri, context, coroutineScope)
    }

    val recallDialog by viewModel.recallDialog.collectAsState()
    val editDialog by viewModel.editDialog.collectAsState()
    val listState = rememberLazyListState()

    var showScrollToBottom by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableIntStateOf(0) }

    var showImageViewer by remember { mutableStateOf(false) }
    var imageViewerUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var imageViewerInitialPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val lastVisibleIndex = visibleItems.last().index
                val totalItems = layoutInfo.totalItemsCount
                lastVisibleIndex >= totalItems - 5 && uiState.hasMore && !uiState.isLoadingMore
            } else {
                false
            }
        }.collect { shouldLoadMore ->
            if (shouldLoadMore) {
                viewModel.loadMore()
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val firstVisibleIndex = visibleItems.first().index
                firstVisibleIndex == 0
            } else {
                true
            }
        }.collect { isAtBottom ->
            showScrollToBottom = !isAtBottom
            if (isAtBottom) {
                unreadCount = 0
            }
        }
    }

    LaunchedEffect(uiState.messages) {
        val newMessages = uiState.messages
        val oldMessages = previousMessages

        // 数量未变 → 可能是编辑/撤回，不处理滚动和未读
        if (newMessages.size == oldMessages.size) {
            previousMessages = newMessages
            return@LaunchedEffect
        }

        // 找出新增的消息（假设消息 id 唯一，equals 基于 id）
        val added = newMessages.filterNot { oldMessages.contains(it) }
        if (added.isEmpty()) {
            previousMessages = newMessages
            return@LaunchedEffect
        }

        val oldIndices = oldMessages.map { newMessages.indexOf(it) }
        val allAtHead = added.all { newMessages.indexOf(it) < (oldIndices.minOrNull() ?: 0) }

        if (allAtHead) {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val isAtBottom = visibleItems.isNotEmpty() && visibleItems.first().index == 0

            if (isAtBottom) {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
                unreadCount = 0
            } else {
                unreadCount += added.size

                val topVisibleMessageId = visibleItems.firstOrNull()?.let { item ->
                    newMessages.getOrNull(item.index)?.id
                }
                if (topVisibleMessageId != null) {
                    coroutineScope.launch {
                        delay(10)
                        val newIndex = newMessages.indexOfFirst { it.id == topVisibleMessageId }
                        if (newIndex != -1) {
                            listState.scrollToItem(newIndex, 0)
                        }
                    }
                }
            }
        }

        previousMessages = newMessages
    }

    val scrollToBottom: () -> Unit = {
        coroutineScope.launch {
            listState.animateScrollToItem(0)
            unreadCount = 0
        }
    }

    if (showImageViewer) {
        MultiImageViewer(
            images = imageViewerUrls,
            initialPage = imageViewerInitialPage,
            isVisible = showImageViewer,
            onDismiss = { showImageViewer = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.relationship != "friend") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "你们不是好友，此对话具有时限性",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            token?.let { tokenValue ->
                                scope.launch {
                                    val success = uiState.otherUser?.let {
                                        sendFriendRequest(token = tokenValue, friendId = it.id)
                                    }
                                    if (success == true) {
                                        Toast.makeText(context, "好友请求已发送", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "发送失败，请重试", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("添加好友")
                    }
                }
            }
        }
    
        Box(modifier = Modifier.weight(1f)) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onRecall = { viewModel.showRecallDialog(message.id) },
                            onEdit = { viewModel.showEditDialog(message) },
                            clipboard = clipboard,
                            context = context,
                            onImageClick = { urls, index ->
                                imageViewerUrls = urls
                                imageViewerInitialPage = index
                                showImageViewer = true
                            }
                        )
                    }

                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    }
                }
            }

            if (uiState.error != null && uiState.messages.isEmpty()) {
                Text(
                    text = "错误: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            AnimatedScrollToBottomButton(
                visible = showScrollToBottom,
                unreadCount = unreadCount,
                onClick = scrollToBottom,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }

        if (uiState.isChatExpired) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = innerPadding.calculateBottomPadding()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "此对话已过期，无法发送消息",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            MessageInput(
                inputText = uiState.inputText,
                selectedImages = uiState.selectedImages,
                isMarkdown = uiState.isMarkdown,
                onTextChange = { viewModel.updateInputText(it) },
                onSendClick = {
                    viewModel.sendMessage()
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                onAddImageClick = { imagePicker.launch("image/*") },
                onRemoveImage = { viewModel.removeImage(it) },
                onToggleMarkdown = { viewModel.toggleMarkdown() },
                innerPadding = innerPadding,
            )
        }
    }

    // 撤回确认弹窗
    if (recallDialog.isOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.hideRecallDialog() },
            title = { Text("撤回消息") },
            text = { Text("确定要撤回这条消息吗？") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.recallMessage() }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRecallDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    // 编辑消息弹窗
    if (editDialog.isOpen && editDialog.message != null) {
        EditMessageDialog(
            state = editDialog,
            onDismiss = { viewModel.hideEditDialog() },
            onContentChange = { viewModel.updateEditContent(it) },
            onSave = { viewModel.editMessage() }
        )
    }
}

// 新增：带动画的滚动到底部按钮
@Composable
fun AnimatedScrollToBottomButton(
    visible: Boolean,
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "scroll_button_alpha"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "scroll_button_scale"
    )

    Box(
        modifier = modifier
            .wrapContentSize()
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
            }
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        ) {
            FloatingActionButton(
                onClick = onClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "滚动到底部"
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    context: Context,
    clipboard: Clipboard,
    message: Message,
    onRecall: () -> Unit,
    onEdit: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isMine = message.isMine
    val isSystemMessage = message.isRecalled || message.isDeleted

    if (isSystemMessage) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 250.dp)
            ) {
                Text(
                    text = when {
                        message.isRecalled -> message.recallHint ?: "消息已撤回"
                        message.isDeleted -> "消息已删除"
                        else -> ""
                    },
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    } else {
        // 普通消息：左右排列
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
        ) {
            if (!isMine) {
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 250.dp)
                    .combinedClickable(
                        onClick = {

                        },
                        onLongClick = {
                            val hasContent = message.content.isNotBlank() || message.images.isNotEmpty()
                            val canRecall = isMine && !message.isRecalled && !message.isDeleted
                            if (hasContent || canRecall) {
                                showMenu = true
                            }
                        }
                    )
            ) {
                Card(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMine)
                            MaterialTheme.colorScheme.primary.copy(0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        if (message.content.isNotBlank()) {
                            if (message.isMarkdown) {
                                MarkdownRenderer.Render(
                                    modifier = Modifier.fillMaxWidth(),
                                    content = message.content
                                )
                            } else {
                                Text(
                                    text = message.content,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (message.images.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Column {
                                message.images.forEachIndexed { index, imageUrl ->
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onImageClick(message.images, index) }  // 添加点击
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.align(if (isMine) Alignment.End else Alignment.Start)
                        ) {
                            Text(
                                text = message.timestampDisplay,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (message.isEdited) {
                                Text(
                                    text = "已编辑",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.align(if (isMine) Alignment.TopStart else Alignment.TopEnd)
                ) {
                    if (message.content.isNotBlank()) {
                        DropdownMenuItem(
                            text = { Text("复制") },
                            onClick = {
                                clipboard.nativeClipboard.setPrimaryClip(
                                    ClipData.newPlainText("text", message.content)
                                )
                                showMenu = false
                                Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT).show()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    null,
                                    Modifier.size(18.dp)
                                )
                            }
                        )
                    }

                    if (isMine && !message.isRecalled && !message.isDeleted) {
                        DropdownMenuItem(
                            text = { Text("撤回") },
                            onClick = {
                                showMenu = false
                                onRecall()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Undo,
                                    null,
                                    Modifier.size(18.dp)
                                )
                            }
                        )
                        
                        if (message.content.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("编辑") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        null,
                                        Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (isMine) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun MessageInput(
    inputText: String,
    selectedImages: List<String>,
    isMarkdown: Boolean,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAddImageClick: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onToggleMarkdown: () -> Unit,
    innerPadding: PaddingValues,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
        shape = RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(selectedImages.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            AsyncImage(
                                model = selectedImages[index],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // 移除按钮
                            IconButton(
                                onClick = { onRemoveImage(index) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "移除",
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onAddImageClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "添加图片",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onToggleMarkdown,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (isMarkdown) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Markdown模式",
                        tint = if (isMarkdown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...") },
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.width(5.dp))

                IconButton(
                    onClick = onSendClick,
                    modifier = Modifier.size(40.dp),
                    enabled = inputText.isNotBlank() || selectedImages.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                }
            }
        }
    }
}

@Composable
fun EditMessageDialog(
    state: EditDialogState,
    onDismiss: () -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "编辑消息",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = state.newContent,
                    onValueChange = onContentChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("新内容") },
                    minLines = 3
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onSave) {
                        Text("保存")
                    }
                }
            }
        }
    }
}