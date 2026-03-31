@file:Suppress("AssignedValueIsNeverRead", "PropertyName")

package com.example.toolbox.community

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.R
import com.example.toolbox.TokenManager
import com.example.toolbox.data.community.PostPagination
import com.example.toolbox.data.community.SearchUsersResponse
import com.example.toolbox.data.community.UserSearchResult
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class PostArticleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取传递的数据
        val categoryId = intent.getStringExtra("categoryId") ?: ""
        val userId = intent.getIntExtra("userId", 0)
        val editMessageId = intent.getIntExtra("edit_message_id", -1)
        val isEditMode = editMessageId != -1
        val oldTitle = intent.getStringExtra("old_title") ?: ""
        val oldContent = intent.getStringExtra("old_content") ?: ""
        val oldImages = intent.getStringArrayListExtra("old_images") ?: arrayListOf()
        val oldPrivateList = intent.getIntegerArrayListExtra("old_private") ?: arrayListOf()
        val oldIsMarkdown = intent.getBooleanExtra("old_is_markdown", false)

        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                PostArticleScreen(
                    categoryId = categoryId,
                    userId = userId,
                    editMessageId = editMessageId,
                    isEditMode = isEditMode,
                    oldTitle = oldTitle,
                    oldContent = oldContent,
                    oldImages = oldImages,
                    oldVisibleTo = oldPrivateList,
                    oldIsMarkdown = oldIsMarkdown,
                    onBack = { finish() },
                    onSuccess = {
                        val resultIntent = Intent()
                        resultIntent.putExtra("post_success", true)
                        resultIntent.putExtra("message", "文章发布成功")
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}

// 可见性模式枚举
enum class VisibilityMode {
    PUBLIC,      // 公开
    PRIVATE,     // 仅自己
    SELECTED     // 指定用户
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PostArticleScreen(
    categoryId: String,
    userId: Int,
    editMessageId: Int,
    oldTitle: String,
    oldContent: String,
    oldImages: List<String>,
    oldVisibleTo: List<Int> = emptyList(),
    oldIsMarkdown: Boolean,
    isEditMode: Boolean,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val token = TokenManager.get(context)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 状态管理
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val imageUrls = remember { mutableStateListOf<String>() }
    var isMarkdown by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showImageUrlDialog by remember { mutableStateOf(false) }

    // 确认退出对话框
    var showExitDialog by remember { mutableStateOf(false) }
    var exitDialogResult by remember { mutableStateOf<String?>(null) }

    // 图片选择对话框
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var imagePickerResult by remember { mutableStateOf<String?>(null) }

    // 可见性相关状态
    var visibilityMode by remember { mutableStateOf(VisibilityMode.PUBLIC) }
    val selectedUserIds = remember { mutableStateListOf<Int>() }
    var showUserSelectionDialog by remember { mutableStateOf(false) }

    // 初始化/加载草稿
    LaunchedEffect(Unit) {
        if (isEditMode) {
            title = oldTitle
            content = oldContent
            isMarkdown = oldIsMarkdown
            imageUrls.clear()
            imageUrls.addAll(oldImages)
            if (oldVisibleTo.isEmpty()) {
                visibilityMode = VisibilityMode.PUBLIC
            } else if (oldVisibleTo.size == 1) {
                visibilityMode = VisibilityMode.PRIVATE
            } else {
                visibilityMode = VisibilityMode.SELECTED
                selectedUserIds.clear()
                selectedUserIds.addAll(oldVisibleTo)
            }
        } else {
            val prefs = context.getSharedPreferences("app_data", Context.MODE_PRIVATE)
            title = prefs.getString("draft_title", "") ?: ""
            content = prefs.getString("draft_content", "") ?: ""
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    val pickImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                coroutineScope.launch {
                    isLoading = true
                    token?.let { tk ->
                        val url = uploadImage(context, it, tk, 3) { _ -> }

                        isLoading = false
                        if (url != null) {
                            imageUrls.add(url)
                        } else {
                            snackbarHostState.showSnackbar("上传失败")
                        }
                    }
                }
            }
        }

    LaunchedEffect(exitDialogResult) {
        when (exitDialogResult) {
            "确定" -> onBack()
            "存草稿" -> {
                context.getSharedPreferences("app_data", Context.MODE_PRIVATE).edit {
                    putString("draft_title", title)
                    putString("draft_content", content)
                }
                onBack()
            }
        }
    }

    LaunchedEffect(imagePickerResult) {
        when (imagePickerResult) {
            "upload" -> {
                if (hasStoragePermission(context)) pickImageLauncher.launch("image/*")
                else permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }

            "link" -> showImageUrlDialog = true
        }
        imagePickerResult = null  // 重置
    }

    // 弹窗逻辑
    if (showImageUrlDialog) {
        ImageUrlDialog(
            onDismiss = { showImageUrlDialog = false },
            onConfirm = { url ->
                imageUrls.add(url)
                showImageUrlDialog = false
            }
        )
    }

    // 用户选择弹窗
    if (showUserSelectionDialog && token != null) {
        SelectUsersDialog(
            onDismiss = { showUserSelectionDialog = false },
            onConfirm = { ids ->
                selectedUserIds.clear()
                selectedUserIds.addAll(ids)
                showUserSelectionDialog = false
            },
            initialSelectedIds = selectedUserIds,
            token = token
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                showExitDialog = false
                exitDialogResult = null
            },
            title = { Text("退出") },
            text = { Text("确定退出吗？内容未保存。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        exitDialogResult = "确定"
                        showExitDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        exitDialogResult = "存草稿"
                        showExitDialog = false
                    }
                ) {
                    Text("存草稿")
                }
            }
        )
    }

    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = {
                showImagePickerDialog = false
                imagePickerResult = null
            },
            title = { Text("选择修改方式") },
            text = { Text("请选择图片上传方式") },
            confirmButton = {
                TextButton(
                    onClick = {
                        imagePickerResult = "upload"
                        showImagePickerDialog = false
                    }
                ) {
                    Text("上传")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        imagePickerResult = "link"
                        showImagePickerDialog = false
                    }
                ) {
                    Text("修改直链")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑留言" else "发布帖子") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = {
                        coroutineScope.launch {
                            val prefs =
                                context.getSharedPreferences("app_data", Context.MODE_PRIVATE)
                            val savedTitle = prefs.getString("draft_title", "")
                            val savedContent = prefs.getString("draft_content", "")

                            if (!isEditMode && (title != savedTitle || content != savedContent)) {
                                showExitDialog = true
                            } else {
                                onBack()
                            }
                        }
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isMarkdown)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isMarkdown)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            isMarkdown = !isMarkdown
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.markdown),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    FilledIconButton(
                        enabled = ((title.isNotEmpty() && content.isNotEmpty()) || content.isNotEmpty() || imageUrls.isNotEmpty()) && !isLoading,
                        onClick = {
                            isLoading = true
                            token?.let { tk ->
                                if (isEditMode) {
                                    updateArticle(
                                        messageId = editMessageId,
                                        title = title,
                                        content = content,
                                        imageUrls = imageUrls,
                                        isMarkdown = isMarkdown,
                                        visibilityMode = visibilityMode,
                                        selectedUserIds = selectedUserIds,
                                        userId = userId,
                                        token = tk,
                                        onSuccess = onSuccess,
                                        onError = { error ->
                                            isLoading = false
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    error
                                                )
                                            }
                                        }
                                    )
                                } else {
                                    postArticle(
                                        title = title,
                                        content = content,
                                        imageUrls = imageUrls,
                                        isMarkdown = isMarkdown,
                                        visibilityMode = visibilityMode,
                                        selectedUserIds = selectedUserIds,
                                        categoryId = categoryId,
                                        userId = userId,
                                        token = tk,
                                        onSuccess = {
                                            context.getSharedPreferences(
                                                "app_data",
                                                Context.MODE_PRIVATE
                                            ).edit {
                                                remove("draft_title")
                                                remove("draft_content")
                                            }
                                            onSuccess()
                                        },
                                        onError = { error ->
                                            isLoading = false
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    error
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                shape = RoundedCornerShape(12.dp),
                label = { Text("标题(可空)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                shape = RoundedCornerShape(12.dp),
                label = { Text("文章内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                minLines = 8
            )

            Text(
                text = "图片 (${imageUrls.size}/9)",
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                style = MaterialTheme.typography.titleSmall
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(imageUrls) { index, url ->
                    DraggableImageItem(
                        url = url,
                        index = index,
                        onDelete = { imageUrls.removeAt(index) },
                        onMove = { from, to ->
                            if (to in imageUrls.indices) {
                                val item = imageUrls.removeAt(from)
                                imageUrls.add(to, item)
                            }
                        }
                    )
                }

                if (imageUrls.size < 9) {
                    item {
                        Surface(
                            onClick = {
                                showImagePickerDialog = true
                            },
                            modifier = Modifier.size(100.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("添加", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // 可见性选项
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("可见范围", style = MaterialTheme.typography.titleMedium)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = visibilityMode == VisibilityMode.PUBLIC,
                        onClick = { visibilityMode = VisibilityMode.PUBLIC }
                    )
                    Text("公开", modifier = Modifier.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = visibilityMode == VisibilityMode.PRIVATE,
                        onClick = { visibilityMode = VisibilityMode.PRIVATE }
                    )
                    Text("仅自己", modifier = Modifier.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = visibilityMode == VisibilityMode.SELECTED,
                        onClick = { visibilityMode = VisibilityMode.SELECTED }
                    )
                    Text("指定用户", modifier = Modifier.padding(start = 4.dp))
                }

                // 当选择“指定用户”时，显示已选用户数量和“选择”按钮
                if (visibilityMode == VisibilityMode.SELECTED) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(" ${selectedUserIds.size} 个用户可见")
                        Button(
                            onClick = { showUserSelectionDialog = true },
                            enabled = token != null
                        ) {
                            Text("选择用户")
                        }
                    }
                    if (selectedUserIds.isNotEmpty()) {
                        Text(
                            text = "可见用户ID: ${selectedUserIds.joinToString()}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp, start = 5.dp)
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            )
        }
    }
}

suspend fun searchUsers(
    token: String,
    keyword: String,
    page: Int = 1,
    perPage: Int = 20
): Pair<List<UserSearchResult>, PostPagination> = withContext(Dispatchers.IO) {
    val requestBody = buildJsonObject {
        put("keyword", keyword)
        put("page", page)
        put("per_page", perPage)
    }.toString().toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url("${ApiAddress}search_users")
        .addHeader("x-access-token", token)
        .post(requestBody)
        .build()

    val response = OkHttpClient().newCall(request).execute()
    if (!response.isSuccessful) throw Exception("搜索失败: ${response.code}")

    val json = response.body.string()

    val result = try {
        AppJson.json.decodeFromString<SearchUsersResponse>(json)
    } catch (e: Exception) {
        throw Exception("JSON 解析失败: ${e.message}")
    }

    if (result.success) {
        val users = result.users ?: throw Exception("用户列表缺失")
        val pagination = result.pagination ?: throw Exception("分页数据缺失")
        Pair(users, pagination)
    } else {
        throw Exception(result.message ?: "未知错误")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectUsersDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
    initialSelectedIds: List<Int> = emptyList(),
    token: String
) {
    var searchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<UserSearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    val selectedIds = remember { mutableStateListOf<Int>().apply { addAll(initialSelectedIds) } }
    var searchError by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // 防抖搜索（关键词变化时重置并加载第一页）
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            users = emptyList()
            currentPage = 1
            totalPages = 1
            return@LaunchedEffect
        }
        isLoading = true
        searchError = null
        try {
            val (newUsers, pagination) = searchUsers(token, searchQuery, page = 1)
            users = newUsers
            currentPage = pagination.current_page
            totalPages = pagination.pages
        } catch (e: Exception) {
            searchError = e.message
            users = emptyList()
        } finally {
            isLoading = false
        }
    }

    // 监听列表滑动到底部，自动加载更多
    LaunchedEffect(listState, searchQuery, currentPage, totalPages, users) {
        if (searchQuery.isBlank()) return@LaunchedEffect
        val layoutInfo = listState.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

        // 当最后可见项接近列表末尾且当前页小于总页数且没有正在加载更多时触发加载
        if (totalItems > 0 && lastVisibleItemIndex >= totalItems - 3 &&
            currentPage < totalPages && !isLoadingMore && !isLoading
        ) {
            isLoadingMore = true
            try {
                val (moreUsers, pagination) = searchUsers(
                    token,
                    searchQuery,
                    page = currentPage + 1
                )
                users = users + moreUsers
                currentPage = pagination.current_page
                totalPages = pagination.pages
            } catch (_: Exception) {
                // 可显示错误提示，这里简单处理
            } finally {
                isLoadingMore = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择可见用户") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("搜索用户名或ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (searchError != null) {
                    Text(searchError!!, color = MaterialTheme.colorScheme.error)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(users) { user ->
                        ListItem(
                            headlineContent = { Text(user.username) },
                            leadingContent = {
                                AsyncImage(
                                    model = user.avatar_url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = user.id in selectedIds,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) selectedIds.add(user.id)
                                        else selectedIds.remove(user.id)
                                    }
                                )
                            }
                        )
                    }
                    // 底部加载指示器
                    if (isLoadingMore) {
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
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedIds.toList()) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun DraggableImageItem(
    url: String,
    index: Int,
    onDelete: () -> Unit,
    onMove: (from: Int, to: Int) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(100.dp)
            .offset(x = offsetX.dp)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x / 3f
                    },
                    onDragEnd = {
                        if (offsetX > 60f && index < 2) onMove(index, index + 1)
                        if (offsetX < -60f && index > 0) onMove(index, index - 1)
                        offsetX = 0f
                    },
                    onDragCancel = { offsetX = 0f }
                )
            }
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(20.dp)
                .clickable { onDelete() }
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier.padding(4.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(0.3f))
                .padding(vertical = 2.dp)
        ) {
            Text(
                "长按拖动",
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun hasStoragePermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED ||
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

@Composable
fun ImageUrlDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("图片链接") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("输入图片链接") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(url) },
                enabled = url.isNotEmpty()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


private fun updateArticle(
    messageId: Int,
    title: String,
    content: String,
    imageUrls: List<String>,
    isMarkdown: Boolean,
    visibilityMode: VisibilityMode,
    selectedUserIds: List<Int>,
    userId: Int,
    token: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val jsonObject = buildJsonObject {
        put("message_id", messageId)
        put("is_markdown", isMarkdown)

        when {
            title.isNotEmpty() -> {
                put("new_message_type", 3)
                put("new_title", title)
                put("new_content", content)
            }

            imageUrls.isNotEmpty() -> {
                put("new_message_type", if (content.isNotEmpty()) 2 else 1)
                put("new_content", buildJsonObject {
                    put("images", buildJsonArray {
                        imageUrls.forEach { add(JsonPrimitive(it)) }
                    })
                    put("text", content)
                })
            }

            else -> {
                put("new_message_type", 0)
                put("new_content", content)
            }
        }

        // 根据可见性模式设置 visible_to
        when (visibilityMode) {
            VisibilityMode.PUBLIC -> {
                put("visible_to", buildJsonArray { })
            }

            VisibilityMode.PRIVATE -> {
                put("visible_to", buildJsonArray {
                    add(JsonPrimitive(userId))
                })
            }

            VisibilityMode.SELECTED -> {
                put("visible_to", buildJsonArray {
                    selectedUserIds.forEach { add(JsonPrimitive(it)) }
                })
            }
        }
    }

    val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("${ApiAddress}update_message")
        .addHeader("x-access-token", token)
        .post(requestBody)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onError(e.message ?: "网络错误")
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) onSuccess() else onError("更新失败")
        }
    })
}

/**
 * 发布新文章
 * 修改：公开模式设置 visible_to 为空列表
 */
private fun postArticle(
    title: String,
    content: String,
    imageUrls: List<String>,
    isMarkdown: Boolean,
    visibilityMode: VisibilityMode,
    selectedUserIds: List<Int>,
    categoryId: String,
    userId: Int,
    token: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val jsonObject = buildJsonObject {
        put("category_id", categoryId)
        put("is_markdown", isMarkdown)

        when {
            title.isNotEmpty() -> {
                put("message_type", 3)
                put("title", title)
                put("content", content)
                if (imageUrls.isNotEmpty()) {
                    put("images", buildJsonArray {
                        imageUrls.forEach { add(JsonPrimitive(it)) }
                    })
                }
            }

            imageUrls.isNotEmpty() -> {
                put("message_type", if (content.isNotEmpty()) 2 else 1)
                if (content.isNotEmpty()) {
                    put("content", buildJsonObject {
                        put("images", buildJsonArray {
                            imageUrls.forEach { add(JsonPrimitive(it)) }
                        })
                        put("text", content)
                    })
                } else {
                    put("content", buildJsonArray {
                        imageUrls.forEach { add(JsonPrimitive(it)) }
                    })
                }
            }

            else -> {
                put("message_type", 0)
                put("content", content)
            }
        }

        // 根据可见性模式设置 visible_to
        when (visibilityMode) {
            VisibilityMode.PUBLIC -> {
                // 不添加 visible_to 字段，或者添加空数组
                // put("visible_to", buildJsonArray { })
            }

            VisibilityMode.PRIVATE -> {
                put("visible_to", buildJsonArray {
                    add(JsonPrimitive(userId))
                })
            }

            VisibilityMode.SELECTED -> {
                if (selectedUserIds.isNotEmpty()) {
                    put("visible_to", buildJsonArray {
                        selectedUserIds.forEach { add(JsonPrimitive(it)) }
                    })
                }
            }
        }
    }

    val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("${ApiAddress}post_message")
        .addHeader("x-access-token", token)
        .post(requestBody)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onError(e.message ?: "网络错误")
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) onSuccess() else onError("发布失败: ${response.code}")
        }
    })
}