package com.example.toolbox.mine.notice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ApiAddress
import com.example.toolbox.TokenManager
import com.example.toolbox.data.mine.notice.Notification
import com.example.toolbox.data.mine.notice.Sender
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NoticeActivity : ComponentActivity() {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                NoticeScreen(
                    onBack = { finish() },
                    okHttpClient = okHttpClient,
                    apiUrl = ApiAddress
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoticeScreen(
    onBack: () -> Unit,
    okHttpClient: OkHttpClient,
    apiUrl: String,
) {
    val context = LocalContext.current
    val token = TokenManager.get(context)

    val notifications = remember { mutableStateListOf<Notification>() }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        token?.let {
            loadNotifications(notifications, okHttpClient, apiUrl, it, 1, true) { it ->
                isLoading = false
                hasMore = it > 0
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (visibleItems.isNotEmpty()) {
                    val lastVisibleItem = visibleItems.last()
                    val totalItems = lazyListState.layoutInfo.totalItemsCount

                    if (lastVisibleItem.index >= totalItems - 2 &&
                        hasMore &&
                        !isLoadingMore &&
                        !isLoading
                    ) {
                        isLoadingMore = true
                        token?.let {
                            loadNotifications(
                                notifications,
                                okHttpClient,
                                apiUrl,
                                it,
                                currentPage + 1,
                                false
                            ) { newItemsCount ->
                                if (newItemsCount > 0) {
                                    currentPage++
                                } else {
                                    hasMore = false
                                }
                                isLoadingMore = false
                            }
                        }
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的通知") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            } else {
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无通知")
                    }
                } else {
                    // 使用 PullToRefreshBox 替代 SwipeRefresh
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            token?.let {
                                loadNotifications(
                                    notifications, okHttpClient, apiUrl, it, 1,
                                    clearList = true
                                ) { newItemsCount ->
                                    isRefreshing = false
                                    currentPage = 1
                                    hasMore = newItemsCount > 0
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState
                        ) {
                            items(notifications) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    onClick = {
                                        handleNotificationClick(notification)
                                    }
                                )
                            }

                            if (isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(
                                                24.dp
                                            )
                                        )
                                    }
                                }
                            }

                            if (!hasMore && notifications.isNotEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "没有更多通知了",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp
                                        )
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
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isNew) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForType(notification.type),
                    contentDescription = "图标",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    if (notification.isNew) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getIconForType(type: Int): ImageVector {
    return when (type) {
        0 -> Icons.Default.Key
        1 -> Icons.Default.AlternateEmail
        2 -> Icons.Default.Favorite
        3 -> Icons.Default.ModeComment
        4, 5 -> Icons.Default.Inbox
        6 -> Icons.Default.Person
        7 -> Icons.Default.Upgrade
        8 -> Icons.Default.Warning
        9 -> Icons.Default.Settings
        else -> Icons.Default.Notifications
    }
}

private fun loadNotifications(
    notifications: SnapshotStateList<Notification>,
    okHttpClient: OkHttpClient,
    apiUrl: String,
    token: String,
    page: Int,
    clearList: Boolean,
    onComplete: (Int) -> Unit // 返回加载的新项目数量
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val request = Request.Builder()
                .url("${apiUrl}notification_list?page=$page")
                .addHeader("x-access-token", token)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                responseBody?.let {
                    val jsonObject = JSONObject(it)
                    val notificationsArray = jsonObject.getJSONArray("notifications")

                    val newNotifications = mutableListOf<Notification>()

                    for (i in 0 until notificationsArray.length()) {
                        val item = notificationsArray.getJSONObject(i)

                        val type = item.getInt("type")
                        val isNew = item.optBoolean("is_new", false)
                        val content = item.optString("content", "")
                        val sender = if (item.has("sender") && !item.isNull("sender")) {
                            val senderObj = item.getJSONObject("sender")
                            Sender(senderObj.optString("username", ""))
                        } else {
                            null
                        }

                        val title = when (type) {
                            0 -> "设备登录"
                            1 -> "被@"
                            2 -> "留言收到点赞"
                            3 -> "留言被回复"
                            4, 5 -> "资源审核"
                            6 -> "被关注"
                            7 -> "升级通知"
                            8 -> "举报通知"
                            9 -> "系统通知"
                            else -> "其他消息"
                        }

                        val displayContent = when (type) {
                            1 -> "${sender?.username ?: ""} @了你"
                            2 -> "${sender?.username ?: ""} 给你的留言点赞"
                            3 -> "${sender?.username ?: ""} 回复了你的留言"
                            6 -> "${sender?.username ?: ""} 关注了你"
                            else -> content
                        }

                        newNotifications.add(
                            Notification(
                                id = item.optString("id", ""),
                                type = type,
                                title = title,
                                content = displayContent,
                                isNew = isNew,
                                sender = sender
                            )
                        )
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        if (clearList) {
                            notifications.clear()
                        }
                        notifications.addAll(newNotifications)
                        onComplete(newNotifications.size)
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            CoroutineScope(Dispatchers.Main).launch {
                onComplete(0)
            }
        }
    }
}

private fun handleNotificationClick(notification: Notification) {
    when (notification.type) {
        6 -> { // 被关注

        }
    }
}

// 添加 snapshotFlow 扩展函数
fun <T> snapshotFlow(getter: () -> T): Flow<T> {
    return flow {
        var previous = getter()
        while (true) {
            val current = getter()
            if (current != previous) {
                emit(current)
                previous = current
            }
            delay(100) // 每100ms检查一次
        }
    }
}