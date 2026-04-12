package com.example.toolbox.mine.notice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil3.compose.rememberAsyncImagePainter
import com.example.toolbox.ApiAddress
import com.example.toolbox.TokenManager
import com.example.toolbox.community.PostDetailActivity
import com.example.toolbox.community.UserInfoActivity
import com.example.toolbox.data.mine.notice.LikeInfo
import com.example.toolbox.data.mine.notice.Notification
import com.example.toolbox.data.mine.notice.Sender
import com.example.toolbox.data.mine.notice.SummaryData
import com.example.toolbox.data.mine.notice.TypeUnreadDetail
import com.example.toolbox.settings.SettingsGroup
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                NoticeNavHost(
                    onBack = { finish() },
                    okHttpClient = okHttpClient,
                    apiUrl = ApiAddress
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeNavHost(
    onBack: () -> Unit,
    okHttpClient: OkHttpClient,
    apiUrl: String
) {
    val navController = rememberNavController()

    var isNavigating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainNoticeScreen(
                onBack = onBack,
                onNavigateToType = { typeName, typeValue ->
                    if (!isNavigating) {
                        isNavigating = true
                        navController.navigate("type/$typeName?type=$typeValue")
                        scope.launch {
                            delay(500)
                            isNavigating = false
                        }
                    }
                },
                okHttpClient = okHttpClient,
                apiUrl = apiUrl
            )
        }
        composable(
            route = "type/{typeName}?type={type}",
            arguments = listOf(
                navArgument("typeName") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val typeName = backStackEntry.arguments?.getString("typeName") ?: ""
            val typeValue = backStackEntry.arguments?.getString("type") ?: ""

            TypeNoticeScreen(
                typeName = typeName,
                typeValue = typeValue,
                onBack = {
                    if (!isNavigating) {
                        if (navController.previousBackStackEntry != null) {
                            isNavigating = true
                            navController.popBackStack()
                            scope.launch {
                                delay(500)
                                isNavigating = false
                            }
                        } else {
                            onBack()
                        }
                    }
                },
                okHttpClient = okHttpClient,
                apiUrl = apiUrl
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNoticeScreen(
    onBack: () -> Unit,
    onNavigateToType: (String, String) -> Unit,
    okHttpClient: OkHttpClient,
    apiUrl: String
) {
    val context = LocalContext.current
    val token = TokenManager.get(context)

    // 主页面显示所有通知
    val notifications = remember { mutableStateListOf<Notification>() }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }

    // ========== 新增：存储各类型未读数量 ==========
    var likeUnreadCount by remember { mutableIntStateOf(0) }      // 类型2
    var resourceUnreadCount by remember { mutableIntStateOf(0) }  // 类型4,5
    var followUnreadCount by remember { mutableIntStateOf(0) }    // 类型6
    var systemUnreadCount by remember { mutableIntStateOf(0) }    // 类型0,7,8,9,10
    // ==========================================

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        token?.let {
            loadNotificationsV2WithSummary(
                notifications = notifications,
                okHttpClient = okHttpClient,
                apiUrl = apiUrl,
                token = it,
                page = 1,
                type = "3",  // 保持不变
                clearList = true
            ) { newItemsCount, summary ->
                isLoading = false
                hasMore = newItemsCount > 0

                if (summary != null) {
                    val typeCounts = summary.typeUnreadCounts
                    likeUnreadCount = typeCounts["2"] ?: 0
                    resourceUnreadCount = (typeCounts["4"] ?: 0) + (typeCounts["5"] ?: 0)
                    followUnreadCount = typeCounts["6"] ?: 0
                    systemUnreadCount = (typeCounts["0"] ?: 0) +
                            (typeCounts["7"] ?: 0) +
                            (typeCounts["8"] ?: 0) +
                            (typeCounts["9"] ?: 0) +
                            (typeCounts["10"] ?: 0)
                }
                // ======================================
            }
        }
    }

    // 分页加载
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
                            loadNotificationsV2WithSummary(
                                notifications = notifications,
                                okHttpClient = okHttpClient,
                                apiUrl = apiUrl,
                                token = it,
                                page = currentPage + 1,
                                type = "3",
                                clearList = false
                            ) { newItemsCount, _ ->
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (notifications.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无通知")
                        }
                    } else {
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                token?.let {
                                    loadNotificationsV2WithSummary(
                                        notifications = notifications,
                                        okHttpClient = okHttpClient,
                                        apiUrl = apiUrl,
                                        token = it,
                                        page = 1,
                                        type = "3",
                                        clearList = true
                                    ) { newItemsCount, summary ->
                                        isRefreshing = false
                                        currentPage = 1
                                        hasMore = newItemsCount > 0

                                        if (summary != null) {
                                            val typeCounts = summary.typeUnreadCounts
                                            likeUnreadCount = typeCounts["2"] ?: 0
                                            resourceUnreadCount = (typeCounts["4"] ?: 0) + (typeCounts["5"] ?: 0)
                                            followUnreadCount = typeCounts["6"] ?: 0
                                            systemUnreadCount = (typeCounts["0"] ?: 0) +
                                                    (typeCounts["7"] ?: 0) +
                                                    (typeCounts["8"] ?: 0) +
                                                    (typeCounts["9"] ?: 0) +
                                                    (typeCounts["10"] ?: 0)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = lazyListState
                            ) {
                                item {
                                    val typeButtons = listOf(
                                        @Composable {
                                            NoticeTypeRow(
                                                text = "点赞通知",
                                                icon = Icons.Default.Favorite,
                                                color = Color(0xFFE57373),
                                                unreadCount = likeUnreadCount,
                                                onClick = { onNavigateToType("点赞通知", "2") }
                                            )
                                        },
                                        @Composable {
                                            NoticeTypeRow(
                                                text = "资源通知",
                                                icon = Icons.Default.Inbox,
                                                color = Color(0xFF81C784),
                                                unreadCount = resourceUnreadCount,
                                                onClick = { onNavigateToType("资源通知", "4,5") }
                                            )
                                        },
                                        @Composable {
                                            NoticeTypeRow(
                                                text = "关注通知",
                                                icon = Icons.Default.Person,
                                                color = Color(0xFF64B5F6),
                                                unreadCount = followUnreadCount,
                                                onClick = { onNavigateToType("关注通知", "6") }
                                            )
                                        },
                                        @Composable {
                                            NoticeTypeRow(
                                                text = "系统通知",
                                                icon = Icons.Default.Key,
                                                color = Color(0xFF9E9E9E),
                                                unreadCount = systemUnreadCount,
                                                onClick = { onNavigateToType("系统通知", "0,7,8,9,10") }
                                            )
                                        }
                                    )

                                    SettingsGroup(
                                        items = typeButtons
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                items(notifications) { notification ->
                                    NotificationItem(
                                        notification = notification,
                                        onClick = { handleNotificationClick(context, notification) }
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
                                                modifier = Modifier.size(24.dp)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeNoticeScreen(
    typeName: String,
    typeValue: String,
    onBack: () -> Unit,
    okHttpClient: OkHttpClient,
    apiUrl: String
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
            loadNotificationsV2WithSummary(
                notifications = notifications,
                okHttpClient = okHttpClient,
                apiUrl = apiUrl,
                token = it,
                page = 1,
                type = typeValue,
                clearList = true
            ) { newItemsCount, _ ->
                isLoading = false
                hasMore = newItemsCount > 0
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
                            loadNotificationsV2WithSummary(
                                notifications = notifications,
                                okHttpClient = okHttpClient,
                                apiUrl = apiUrl,
                                token = it,
                                page = currentPage + 1,
                                type = typeValue,
                                clearList = false
                            ) { newItemsCount, _ ->
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
                title = { Text(typeName) },
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无${typeName}")
                    }
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            token?.let {
                                loadNotificationsV2WithSummary(
                                    notifications = notifications,
                                    okHttpClient = okHttpClient,
                                    apiUrl = apiUrl,
                                    token = it,
                                    page = 1,
                                    type = typeValue,
                                    clearList = true
                                ) { newItemsCount, _ ->
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
                                    onClick = { handleNotificationClick(context, notification) }
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
                                            modifier = Modifier.size(24.dp)
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
fun NoticeTypeRow(
    text: String,
    icon: ImageVector,
    color: Color,
    unreadCount: Int,
    onClick: () -> Unit
) {
    // SettingsGroup 已经提供了 Surface 容器，这里只需内容
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = color,
            contentColor = Color.White
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            modifier = Modifier.weight(1f),
            text = text,
            fontSize = 16.sp,
            fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
            color = if (unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (notification.sender != null && notification.sender.avatarUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(notification.sender.avatarUrl),
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
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
            }

            Spacer(modifier = Modifier.width(16.dp))

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

                if (notification.timestamp.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = notification.timestamp,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
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

private fun loadNotificationsV2WithSummary(
    notifications: SnapshotStateList<Notification>,
    okHttpClient: OkHttpClient,
    apiUrl: String,
    token: String,
    page: Int,
    type: String?,
    clearList: Boolean,
    onComplete: (Int, SummaryData?) -> Unit  // 修改回调，增加 summary 参数
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val urlBuilder = "${apiUrl}v2/notifications?page=$page&per_page=20"
            val finalUrl = if (!type.isNullOrBlank()) "$urlBuilder&type=$type" else urlBuilder

            val request = Request.Builder()
                .url(finalUrl)
                .addHeader("x-access-token", token)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val jsonObject = JSONObject(responseBody)
                val data = jsonObject.getJSONObject("data")
                val notificationsArray = data.getJSONArray("notifications")

                // ========== 新增：解析 summary ==========
                var summaryData: SummaryData? = null
                if (data.has("summary")) {
                    val summaryObj = data.getJSONObject("summary")
                    val totalUnread = summaryObj.optInt("total_unread", 0)

                    // 解析 type_unread_counts
                    val typeUnreadCounts = mutableMapOf<String, Int>()
                    val countsObj = summaryObj.optJSONObject("type_unread_counts")
                    if (countsObj != null) {
                        val keys = countsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            typeUnreadCounts[key] = countsObj.optInt(key, 0)
                        }
                    }

                    // 解析 type_unread_details（可选，暂时不用）
                    val typeUnreadDetails = mutableListOf<TypeUnreadDetail>()
                    val detailsArray = summaryObj.optJSONArray("type_unread_details")
                    if (detailsArray != null) {
                        for (i in 0 until detailsArray.length()) {
                            val detailObj = detailsArray.getJSONObject(i)
                            typeUnreadDetails.add(
                                TypeUnreadDetail(
                                    type = detailObj.optInt("type", 0),
                                    typeName = detailObj.optString("type_name", ""),
                                    unreadCount = detailObj.optInt("unread_count", 0)
                                )
                            )
                        }
                    }

                    summaryData = SummaryData(
                        totalUnread = totalUnread,
                        typeUnreadCounts = typeUnreadCounts,
                        typeUnreadDetails = typeUnreadDetails
                    )
                }
                // ========================================

                val newNotifications = mutableListOf<Notification>()

                for (i in 0 until notificationsArray.length()) {
                    val item = notificationsArray.getJSONObject(i)

                    val id = item.getString("id")
                    val typeInt = item.getInt("type")
                    val content = item.getString("content")
                    val timestamp = item.getString("timestamp")
                    val isNew = item.getBoolean("is_new")
                    val relatedId = item.optInt("related_id", 0)
                    val relatedType = item.optString("related_type", "")

                    val sender = if (item.has("sender") && !item.isNull("sender")) {
                        val senderObj = item.getJSONObject("sender")
                        Sender(
                            id = senderObj.optInt("id", 0),
                            username = senderObj.optString("username", "系统"),
                            avatarUrl = senderObj.optString("avatar_url", "")
                        )
                    } else {
                        null
                    }

                    var likeInfo: LikeInfo? = null
                    if (typeInt == 2 && item.has("like_info") && !item.isNull("like_info")) {
                        val likeInfoObj = item.getJSONObject("like_info")
                        likeInfo = LikeInfo(
                            messageId = likeInfoObj.optInt("message_id", 0),
                            messageTitle = likeInfoObj.optString("message_title", ""),
                            messageContent = likeInfoObj.optString("message_content", ""),
                            isValid = likeInfoObj.optBoolean("is_valid", true)
                        )
                    }

                    val title = when (typeInt) {
                        0 -> "登录提醒"
                        1 -> "被@"
                        2 -> "给你点赞"
                        3 -> "回复了你"
                        4 -> "资源审核通过"
                        5 -> "资源审核未通过"
                        6 -> "关注了你"
                        7 -> "升级通知"
                        8 -> "举报通知"
                        9 -> "系统消息"
                        10 -> "好友请求"
                        11 -> "新消息通知"
                        else -> "其他通知"
                    }

                    val displayContent = when (typeInt) {
                        1 -> "${sender?.username ?: "用户"} @了你"
                        2 -> {
                            val username = sender?.username ?: "用户"
                            if (likeInfo != null && likeInfo.messageTitle.isNotBlank()) {
                                "$username 给你点赞：${likeInfo.messageTitle}"
                            } else {
                                "$username 给你点赞"
                            }
                        }
                        3 -> content.ifBlank { "${sender?.username ?: "用户"} 回复了你的评论" }
                        6 -> "${sender?.username ?: "用户"} 关注了你"
                        else -> content.ifBlank { "您有一条新通知" }
                    }

                    newNotifications.add(
                        Notification(
                            id = id,
                            type = typeInt,
                            title = title,
                            content = displayContent,
                            timestamp = timestamp,
                            isNew = isNew,
                            sender = sender,
                            relatedId = relatedId,
                            relatedType = relatedType
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    if (clearList) {
                        notifications.clear()
                    }
                    notifications.addAll(newNotifications)
                    onComplete(newNotifications.size, summaryData)  // 传递 summary 数据
                }
            } else {
                withContext(Dispatchers.Main) {
                    onComplete(0, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onComplete(0, null)
            }
        }
    }
}

private fun handleNotificationClick(context: Context, notification: Notification) {
    when (notification.type) {
        3 -> {
            context.startActivity(
                Intent(context, PostDetailActivity::class.java).apply {
                    notification.replyInfo?.let { putExtra("msgid", it.originalMessageId) }
                }
            )
        }
        6 -> {
            context.startActivity(
                Intent(context, UserInfoActivity::class.java).apply {
                    notification.sender?.let { putExtra("userId", it.id) }
                }
            )
        }
    }
}

// 辅助扩展函数（用于分页监听）
fun <T> snapshotFlow(getter: () -> T): Flow<T> {
    return flow {
        var previous = getter()
        while (true) {
            val current = getter()
            if (current != previous) {
                emit(current)
                previous = current
            }
            delay(100)
        }
    }
}