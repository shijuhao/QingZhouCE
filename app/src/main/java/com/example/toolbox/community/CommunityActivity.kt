@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.community

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import com.example.toolbox.ApiAddress
import com.example.toolbox.TokenManager
import com.example.toolbox.data.community.EditRecord
import com.example.toolbox.data.community.Message
import com.example.toolbox.ui.theme.ToolBoxTheme
import com.example.toolbox.utils.MultiImageViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class CommunityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = TokenManager.get(this) ?: ""

        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                val viewModel: CommunityViewModel = viewModel()
                val state by viewModel.state.collectAsState()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                
                var refreshTrigger by remember { mutableIntStateOf(0) }
                
                val postArticleLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        Toast.makeText(this@CommunityActivity, "发布成功", Toast.LENGTH_SHORT).show()
                        refreshTrigger++
                    }
                }
                
                val editArticleLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        Toast.makeText(this@CommunityActivity, "编辑成功", Toast.LENGTH_SHORT).show()
                        refreshTrigger++
                    }
                }
        
                LaunchedEffect(drawerState.isOpen) {
                    if (drawerState.isOpen && token.isNotEmpty()) {
                        viewModel.fetchCategories(token)
                    }
                }

                val currentCategoryId by remember { derivedStateOf { state.categoryId } }
                var currentCategoryName by remember { mutableStateOf("轻昼") }

                viewModel.initWebSocket(token)

                LaunchedEffect(Unit) {
                    if (token.isNotEmpty()) {
                        viewModel.fetchCategories(token)
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            onCloseDrawer = { scope.launch { drawerState.close() } },
                            onCategorySelected = { categoryId, categoryName ->
                                viewModel.onCategoryChanged(categoryId)
                                currentCategoryName = categoryName
                                scope.launch { drawerState.close() }
                            },
                            viewModel = viewModel,
                            token = token
                        )
                    }
                ) {
                    Surface {
                        CommunityScreen(
                            token = token,
                            drawerState = drawerState,
                            currentCategoryId = currentCategoryId,
                            currentCategoryName = currentCategoryName,
                            refreshTrigger = refreshTrigger,
                            viewModel = viewModel,
                            postArticleLauncher = postArticleLauncher,
                            editArticleLauncher = editArticleLauncher
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(
    onCloseDrawer: () -> Unit,
    onCategorySelected: (categoryId: Int, categoryName: String) -> Unit,
    viewModel: CommunityViewModel,
    token: String
) {
    val context = LocalContext.current
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    val tagStatus = TokenManager.getTagStatus(context)
    var showCreateDialog by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val client = OkHttpClient()

    fun createCategory(name: String, description: String, avatarUrl: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject()
                    .put("name", name)
                    .put("description", description)
                    .put("avatar_url", avatarUrl)
                    .toString()
                val request = Request.Builder()
                    .url("${ApiAddress}create_category")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .addHeader("x-access-token", token)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "新建成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "新建失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var avatarUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建分区") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("分区名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("分区简介") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = avatarUrl,
                        onValueChange = { avatarUrl = it },
                        label = { Text("分区头像") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank() && description.isNotBlank() && avatarUrl.isNotBlank(),
                    onClick = {
                        createCategory(name, description, avatarUrl)
                        showCreateDialog = false
                    }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("取消") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = { Text(text = "切换分区") },
            navigationIcon = {
                FilledTonalIconButton(onClick = onCloseDrawer) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }
        )

        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = {
                scope.launch {
                    viewModel.fetchCategories(token)
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "加载失败: ${state.errorMessage}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.fetchCategories(token) }
                        ) {
                            Text("重试")
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.categories) { category ->
                                CategoryItem(
                                    select = state.categoryId == category.id,
                                    category = category,
                                    onItemClick = {
                                        onCategorySelected(category.id, category.name)
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(systemBarsPadding.calculateBottomPadding()))
                            }
                        }

                        if (tagStatus == 1 || tagStatus == 4) {
                            FloatingActionButton(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .padding(bottom = systemBarsPadding.calculateBottomPadding()),
                                onClick = {
                                    showCreateDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Add, "新建分区")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    modifier: Modifier = Modifier,
    token: String,
    drawerState: DrawerState,
    currentCategoryId: Int,
    currentCategoryName: String,
    refreshTrigger: Int,
    viewModel: CommunityViewModel,
    postArticleLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    editArticleLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val userId = TokenManager.getUserID(context)

    val state by viewModel.state.collectAsState()

    val wsConnectState by remember { derivedStateOf { state.wsConnectState } }

    // 交互弹窗状态
    var showHistoryRecords by remember { mutableStateOf<List<EditRecord>?>(null) }
    var replyToMessage by remember { mutableStateOf<Message?>(null) }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }
    var replyText by remember { mutableStateOf("") }
    var replyPrivate by remember { mutableStateOf(false) }

    var viewerState by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }

    val lazyListState = rememberLazyListState()
    val messages = state.messages

    // 监听分类切换和外部刷新
    LaunchedEffect(currentCategoryId, refreshTrigger) {
        val shouldScroll = refreshTrigger > 0
        viewModel.fetchMessages(
            token = token,
            page = 1,
            isRefresh = true
        )
        if (shouldScroll) {
            lazyListState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(messages.size) {
        if (lazyListState.firstVisibleItemIndex <= 1) {
            lazyListState.animateScrollToItem(0)
        }
    }

    // 翻页加载监听
    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= lazyListState.layoutInfo.totalItemsCount - 3
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && !state.isLoadingMore && !state.isRefreshing && state.currentPage < state.totalPages) {
                viewModel.fetchMessages(
                    token = token,
                    page = state.currentPage + 1,
                    isRefresh = false
                )
            }
        }
    }

    // 历史记录弹窗
    showHistoryRecords?.let { records ->
        AlertDialog(
            onDismissRequest = { showHistoryRecords = null },
            title = { Text("编辑历史") },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn {
                        items(records) { record ->
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    "时间: ${record.edit_time}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                if (record.old_title != null || record.new_title != null) {
                                    Text(
                                        "标题变更: ${record.old_title ?: "无"} -> ${record.new_title ?: "无"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "原内容: ${record.old_content ?: "无内容"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 8.dp),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHistoryRecords = null }) { Text("关闭") } }
        )
    }

    // 回复弹窗
    replyToMessage?.let { msg ->
        var isSending by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { replyToMessage = null },
            title = { Text("回复 @${msg.username}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入回复内容...") },
                        minLines = 3
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "悄悄发送",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = replyPrivate,
                            onCheckedChange = { replyPrivate = it },
                            thumbContent = {
                                Icon(
                                    imageVector = if (replyPrivate) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    tint = if (replyPrivate) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    }
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = replyText.isNotBlank() && !isSending,
                    onClick = {
                        isSending = true
                        viewModel.postReply(
                            token = token,
                            content = replyText,
                            categoryId = currentCategoryId,
                            refId = msg.message_id,
                            isPrivate = replyPrivate,
                            targetUserId = msg.userid,
                            onSuccess = {
                                Toast.makeText(context, "回复成功", Toast.LENGTH_SHORT).show()
                                replyToMessage = null
                                isSending = false
                                replyText = ""
                                viewModel.fetchMessages(token, 1, true)
                            },
                            onError = { error ->
                                Toast.makeText(context, "回复失败: $error", Toast.LENGTH_SHORT)
                                    .show()
                                isSending = false
                            }
                        )
                    }
                ) { Text("发送") }
            },
            dismissButton = { TextButton(onClick = { replyToMessage = null }) { Text("取消") } }
        )
    }

    // 删除确认弹窗
    messageToDelete?.let { msg ->
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("删除确认") },
            text = { Text("确定要删除这条留言吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteMessage(
                            token = token,
                            userStatus = TokenManager.getTagStatus(context),
                            messageId = msg.message_id,
                            onError = { error ->
                                Toast.makeText(context, "删除失败: $error", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        )
                        messageToDelete = null
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("确定删除") }
            },
            dismissButton = { TextButton(onClick = { messageToDelete = null }) { Text("取消") } }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(4.dp))

                    Text(currentCategoryName)

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = when (wsConnectState) {
                                        1 -> MaterialTheme.colorScheme.error
                                        2 -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                        ) {}
                    }
                }
            },
            navigationIcon = {
                FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.fetchMessages(token, 1, true) }) {
                    Icon(Icons.Default.Refresh, "刷新")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.messages.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.errorMessage != null && state.messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("加载失败: ${state.errorMessage}")
                        Button(onClick = { viewModel.fetchMessages(token, 1, true) }) {
                            Text("重试")
                        }
                    }
                }

                else -> {
                    // 使用 PullToRefreshBox 替代原来的 pullRefresh 修饰符和 PullRefreshIndicator
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.fetchMessages(token, 1, true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(state.messages, key = { index, msg ->
                                "${msg.message_id}_$index"
                            }) { _, msg ->
                                Box(
                                    modifier = Modifier
                                        .animateItem(
                                            fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            fadeOutSpec = spring(stiffness = Spring.StiffnessLow)
                                        )
                                ) {
                                    MessageItem(
                                        message = msg,
                                        onLike = {
                                            viewModel.toggleLike(
                                                token = token,
                                                message = msg,
                                                onError = { error ->
                                                    Toast.makeText(
                                                        context,
                                                        error,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                        },
                                        onDelete = {
                                            messageToDelete = msg
                                        },
                                        onEdit = {
                                            val intent = Intent(context, PostArticleActivity::class.java).apply {
                                                putExtra("edit_message_id", msg.message_id)
                                                putExtra("userId", userId)
                                                putExtra("old_title", msg.content.title ?: "")
                                                val textContent = msg.content.text ?: msg.content.content ?: ""
                                                putExtra("old_content", textContent)
                                                val images = msg.content.images ?: emptyList()
                                                putStringArrayListExtra("old_images", ArrayList(images))
                                                val visibleList = msg.visible_to ?: emptyList()
                                                putIntegerArrayListExtra("old_private", ArrayList(visibleList))
                                                putExtra("old_is_markdown", msg.is_markdown)
                                            }
                                            editArticleLauncher.launch(intent)  // ← 改这一行
                                        },
                                        onReply = { replyToMessage = msg },
                                        onHistory = { showHistoryRecords = msg.edit_records },
                                        onImageClick = { urls, index -> viewerState = urls to index }
                                    )
                                }
                            }

                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }

                            item {
                                Spacer(
                                    modifier = Modifier.height(
                                        WindowInsets.navigationBars.asPaddingValues()
                                            .calculateBottomPadding()
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp).padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { drawerState.open() } },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Menu, "分类")
                }

                FloatingActionButton(
                    onClick = {
                        val intent = Intent(context, PostArticleActivity::class.java).apply {
                            putExtra("categoryId", currentCategoryId.toString())
                            putExtra("userId", userId)
                        }
                        postArticleLauncher.launch(intent)
                    }
                ) {
                    Icon(Icons.Default.Add, "发帖")
                }
            }
        }
    }
    viewerState?.let { (urls, initialPage) ->
        val painters = remember(urls) {
            urls.map { url ->
                @Composable {
                    rememberAsyncImagePainter(
                        model = url
                    )
                }
            }
        }

        MultiImageViewer(
            images = painters.map { it() },
            initialPage = initialPage,
            isVisible = true,
            onDismiss = { viewerState = null }
        )
    }
}