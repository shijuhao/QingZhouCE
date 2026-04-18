@file:Suppress(
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "PropertyName",
    "AssignedValueIsNeverRead"
)

package com.example.toolbox.community

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.TokenManager
import com.example.toolbox.data.community.UserInfo
import com.example.toolbox.data.community.UserMessage
import com.example.toolbox.data.community.UserReferencedMessage
import com.example.toolbox.data.community.ResourceItem
import com.example.toolbox.message.MessageDetailActivity
import com.example.toolbox.mine.getLevelIconRes
import com.example.toolbox.resourceLib.ResourceDetailActivity
import com.example.toolbox.settings.UserSettingsActivity
import com.example.toolbox.ui.theme.ToolBoxTheme
import com.example.toolbox.utils.MarkdownRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class UserInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = parseUserId(intent)
        enableEdgeToEdge()

        setContent {
            ToolBoxTheme {
                if (userId == 0) {
                    LaunchedEffect(Unit) {
                        Toast.makeText(this@UserInfoActivity, "无效的用户ID", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }
                } else {
                    UserInfoScreen(userId = userId)
                }
            }
        }
    }

    private fun parseUserId(intent: Intent): Int {
        intent.getIntExtra("userId", 0).takeIf { it != 0 }?.let { return it }

        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data!!
            uri.getQueryParameter("id")?.toIntOrNull()?.let { return it }
        }
        return 0
    }
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun UserInfoScreen(userId: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val coroutineScope = rememberCoroutineScope()

    // 状态管理
    var userInfo by remember { mutableStateOf<UserInfo?>(null) }
    var messages by remember { mutableStateOf<List<UserMessage>>(emptyList()) }
    var resources: List<ResourceItem> by remember { mutableStateOf(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentTab by remember { mutableIntStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var page by remember { mutableIntStateOf(1) }
    var canLoadMore by remember { mutableStateOf(true) }
    val isScrolling = rememberLazyListState()
    var isPageLoading by remember { mutableStateOf(false) }
    var showBanDialog by remember { mutableStateOf(false) }
    var banHours by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var isMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isScrolling) {
        snapshotFlow {
            val lastVisibleItem = isScrolling.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= isScrolling.layoutInfo.totalItemsCount - 3
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && !isPageLoading && canLoadMore && currentTab == 0) {
                isPageLoading = true

                scope.launch {
                    try {
                        loadNextPage(context, userId, page) { newMsgs ->
                            if (newMsgs.isEmpty()) {
                                canLoadMore = false
                            } else {
                                val filteredMsgs =
                                    newMsgs.filter { new -> messages.none { it.id == new.id } }
                                if (filteredMsgs.isEmpty()) {
                                    canLoadMore = false
                                } else {
                                    messages = messages + filteredMsgs
                                    page++
                                }
                            }
                            isPageLoading = false
                        }
                    } catch (_: Exception) {
                        isPageLoading = false
                    }
                }
            }
        }
    }

    // 初始化加载数据
    LaunchedEffect(Unit) {
        loadUserInfo(context, userId) { info, msgs, res ->
            userInfo = info
            messages = msgs
            resources = res
            isLoading = false
            info?.let { isFollowing = it.isFollowed }
        }
    }

    if (showBanDialog) {
        AlertDialog(
            onDismissRequest = { showBanDialog = false },
            title = { Text("封禁用户") },
            text = {
                OutlinedTextField(
                    value = banHours,
                    onValueChange = { banHours = it },
                    label = { Text("封禁时长 (小时)") },
                    placeholder = { Text("例如: 24") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val success = performBanUser(
                            context,
                            userInfo?.userId ?: 0,
                            banHours.toIntOrNull() ?: 0
                        )
                        if (success) {
                            Toast.makeText(context, "封禁成功", Toast.LENGTH_SHORT).show()
                            showBanDialog = false
                        }
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showBanDialog = false }) { Text("取消") }
            }
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("举报用户") },
            text = {
                Column {
                    Text("请说明举报理由：", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("违规行为描述...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReason.isNotBlank()) {
                            scope.launch {
                                val success = performReportUser(
                                    context,
                                    userInfo?.userId ?: 0,
                                    reportReason
                                )
                                if (success) {
                                    showReportDialog = false
                                    reportReason = "" // 清空理由
                                }
                            }
                        } else {
                            Toast.makeText(context, "理由不能为空", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("提交")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        scope.launch {
            loadUserInfo(context, userId) { info, msgs, res ->
                userInfo = info
                messages = msgs
                resources = res
                isRefreshing = false
                info?.let { isFollowing = it.isFollowed }
            }
        }
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val threshold = with(LocalDensity.current) { (statusBarHeight + 64.dp).toPx() }.toInt()
    var lastHeaderBottom by remember { mutableIntStateOf(Int.MAX_VALUE) }
    var isAdjusting by remember { mutableStateOf(false) }

    LaunchedEffect(isScrolling) {
        snapshotFlow {
            if (isAdjusting) return@snapshotFlow lastHeaderBottom

            val headerItem = isScrolling.layoutInfo.visibleItemsInfo.find { it.index == 0 }
            headerItem?.let {
                it.offset + it.size
            } ?: -1
        }.collect { headerBottom ->
            if (headerBottom > 0 && !isAdjusting) {
                when (threshold) {
                    in (headerBottom + 1)..lastHeaderBottom -> {
                        isAdjusting = true
                        val targetScroll =
                            isScrolling.firstVisibleItemScrollOffset + (threshold - headerBottom)
                        coroutineScope.launch {
                            isScrolling.scrollToItem(0, targetScroll)
                            isAdjusting = false
                        }
                    }

                    in lastHeaderBottom..<headerBottom -> {
                        isAdjusting = true
                        val targetScroll =
                            isScrolling.firstVisibleItemScrollOffset - (headerBottom - threshold)
                        coroutineScope.launch {
                            isScrolling.scrollToItem(0, targetScroll)
                            isAdjusting = false
                        }
                    }
                }
                lastHeaderBottom = headerBottom
            }
        }
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .fillMaxSize()
            ) {
                if (isLoading && !isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularWavyProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        state = isScrolling,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            userInfo?.let {
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    it.backgroundUrl?.let { backgroundUrl ->
                                        AsyncImage(
                                            model = backgroundUrl,
                                            contentDescription = "背景",
                                            modifier = Modifier
                                                .matchParentSize()
                                                .alpha(0.3f),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    UserInfoHeader(
                                        userInfo = it,
                                        isFollowing = isFollowing,
                                        onMessageClick = {
                                            val intent =
                                                Intent(context, MessageDetailActivity::class.java)
                                            intent.putExtra("user_id", it.userId)
                                            context.startActivity(intent)
                                        },
                                        onFollowClick = {
                                            scope.launch {
                                                toggleFollow(context, it.userId) { success ->
                                                    if (success) {
                                                        isFollowing = !isFollowing
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        stickyHeader {
                            val localDy = LocalDensity.current
                            val topPadding = remember(isScrolling) {
                                derivedStateOf {
                                    val headerItem =
                                        isScrolling.layoutInfo.visibleItemsInfo.find { it.index == 0 }
                                    if (headerItem != null) {
                                        val headerBottom = headerItem.offset + headerItem.size
                                        val threshold = paddingValues.calculateTopPadding() + 64.dp
                                        val thresholdPx = with(localDy) { threshold.toPx() }.toInt()

                                        if (headerBottom < thresholdPx) {
                                            val neededPadding = thresholdPx - headerBottom
                                            with(localDy) { neededPadding.toDp() }
                                        } else {
                                            0.dp
                                        }
                                    } else {
                                        paddingValues.calculateTopPadding() + 64.dp
                                    }
                                }
                            }

                            Surface(
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(top = topPadding.value)
                            ) {
                                SecondaryTabRow(selectedTabIndex = currentTab) {
                                    Tab(
                                        selected = currentTab == 0,
                                        onClick = { currentTab = 0 },
                                        text = { Text("帖子") }
                                    )
                                    Tab(
                                        selected = currentTab == 1,
                                        onClick = { currentTab = 1 },
                                        text = {
                                            Text("投稿${if (resources.isNotEmpty()) "(${resources.size})" else ""}")
                                        }
                                    )
                                }
                            }
                        }

                        // 内容区
                        if (currentTab == 0) {
                            if (messages.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("空空如也")
                                    }
                                }
                            } else {
                                items(messages) { userMessage ->
                                    MessageItem(
                                        message = userMessage,
                                        onLikeClick = {
                                            scope.launch {
                                                val token =
                                                    TokenManager.get(context) ?: return@launch
                                                val client = OkHttpClient()

                                                coroutineScope.launch {
                                                    try {
                                                        val (newIsLiked, newLikeCount) = toggleLike(
                                                            client,
                                                            token,
                                                            userMessage.id,
                                                            userMessage.is_liked,
                                                            userMessage.likeCount
                                                        )

                                                        messages = messages.map {
                                                            if (it.id == userMessage.id) {
                                                                it.copy(
                                                                    is_liked = newIsLiked,
                                                                    likeCount = newLikeCount
                                                                )
                                                            } else it
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(
                                                            context,
                                                            e.message ?: "操作失败",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        },
                                        onMenuClick = {
                                            // 显示菜单
                                        }
                                    )
                                }
                            }
                        } else {
                            if (resources.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("资源空空")
                                    }
                                }
                            } else {
                                items(resources) { resource ->
                                    ResourceItem(
                                        resource = resource,
                                        onClick = {
                                            val resourceJson = AppJson.json.encodeToString(resource)
                                            Log.e("1", resourceJson)
                                            val intent = Intent(
                                                context,
                                                ResourceDetailActivity::class.java
                                            ).apply {
                                                putExtra("item_json", resourceJson)
                                            }
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                            }
                        }

                        if (canLoadMore && currentTab == 0) {
                            item {
                                Box(
                                    Modifier
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
            }
            val shouldShowSolidBar = remember(isScrolling) {
                derivedStateOf {
                    val headerItem = isScrolling.layoutInfo.visibleItemsInfo.find { it.index == 0 }
                    if (headerItem != null) {
                        headerItem.offset < 0
                    } else {
                        true
                    }
                }
            }

            TopAppBar(
                colors = topAppBarColors(
                    containerColor = if (shouldShowSolidBar.value) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    },
                    scrolledContainerColor = if (shouldShowSolidBar.value) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    }
                ),
                title = {
                    if (shouldShowSolidBar.value) {
                        userInfo?.let {
                            Text(
                                it.username,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } ?: Text("用户信息")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { (context as Activity).finish() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    val userStatus = TokenManager.getTagStatus(context)
                    if (userStatus == 1) {
                        IconButton(onClick = { showBanDialog = true }) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = "封禁",
                                tint = if (shouldShowSolidBar.value)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "更多",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("举报该用户") },
                                onClick = {
                                    isMenuExpanded = false
                                    showReportDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Flag, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun UserInfoHeader(
    userInfo: UserInfo,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 64.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
            ) {
                AsyncImage(
                    model = userInfo.avatar,
                    contentDescription = "头像",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = userInfo.username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Image(
                        modifier = Modifier.size(16.dp),
                        contentDescription = null,
                        painter = painterResource(getLevelIconRes(userInfo.level.toString()))
                    )

                    Spacer(modifier = Modifier.width(3.dp))

                    Text(
                        text = "${userInfo.level}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 经验条
                    LinearProgressIndicator(
                        progress = { userInfo.experience.toFloat() / (userInfo.level * 100f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                    )
                }

                if (userInfo.bio.isNotEmpty()) {
                    Text(
                        text = userInfo.bio,
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 5.dp)
                    )
                }
            }

            val notMyself = userInfo.userId != TokenManager.getUserID(context)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (notMyself) {
                    Button(
                        onClick = onFollowClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAddAlt1,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text(if (isFollowing) "已关注" else "关注")
                    }

                    if (isFollowing) {
                        Button(
                            onClick = onMessageClick,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Message,
                                contentDescription = "私信",
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                            Text("私信")
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            val intent = Intent(context, UserSettingsActivity::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑资料",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text("编辑资料")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = userInfo.followingCount.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("关注", fontSize = 13.sp)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = userInfo.followersCount.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("粉丝", fontSize = 13.sp)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = userInfo.gold.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("金币", fontSize = 13.sp)
                }
            }

            // 封禁提示
            if (userInfo.isBanned) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "该账号已被封禁，于${userInfo.banEndTime ?: "未知时间"}解禁",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: UserMessage,
    onLikeClick: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        onClick = {
            context.startActivity(
                Intent(context, PostDetailActivity::class.java).apply {
                    putExtra("msgid", message.id)
                }
            )
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. 顶部用户信息
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = message.avatar,
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = message.username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 标题
            if (!message.title.isNullOrEmpty()) {
                Text(
                    text = message.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 3. 正文 (Markdown 逻辑)
            if (message.content.isNotEmpty()) {
                if (message.isMarkdown) {
                    MarkdownRenderer.Render(
                        content = message.content,
                    )
                } else {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. 图片展示
            if (message.images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 3
                ) {
                    message.images.take(9).forEach { img ->
                        AsyncImage(
                            model = img,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.325f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            if (message.is_referenced && message.referenced_message != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "回复 @${message.referenced_message.sender_username}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = message.referenced_message.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. 底部操作栏
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // 点赞按钮
                TextButton(
                    onClick = onLikeClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (message.is_liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Icon(
                        imageVector = if (message.is_liked) Icons.Filled.ThumbUp else Icons.Default.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = message.likeCount.toString(),
                        fontSize = 12.sp,
                        fontWeight = if (message.is_liked) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ResourceItem(resource: ResourceItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = resource.icon_url,
                contentDescription = "图标",
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "版本：${resource.version}",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "上传时间：${resource.release_date}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// 网络请求相关函数
suspend fun loadUserInfo(
    context: Context,
    userId: Int,
    onResult: (UserInfo?, List<UserMessage>, List<ResourceItem>) -> Unit
) {
    withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val token = TokenManager.get(context)

        try {
            // 获取用户信息
            val userInfo = token?.let { fetchUserInfo(client, it, userId) }

            // 获取用户帖子
            val messages = userInfo?.let {
                getUserMessages(client, token, it.userId)
            } ?: emptyList()

            // 获取用户资源
            val resources = userInfo?.let {
                getUserResources(client, token, it.userId)
            } ?: emptyList()

            withContext(Dispatchers.Main) {
                onResult(userInfo, messages, resources)
            }
        } catch (e: Exception) {
            Log.e("UserInfoActivity", "Error loading user info", e)
            withContext(Dispatchers.Main) {
                onResult(null, emptyList(), emptyList())
            }
        }
    }
}

private suspend fun fetchUserInfo(
    client: OkHttpClient,
    token: String,
    userId: Int
): UserInfo? {
    return withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiAddress}user_info")
                .post(
                    JSONObject().apply {
                        put("user_id", userId)
                    }.toString().toRequestBody("application/json".toMediaType())
                )
                .addHeader("x-access-token", token)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val json = JSONObject(response.body.string())
                val userJson = json.getJSONObject("user_info")

                UserInfo(
                    username = userJson.getString("username"),
                    avatar = userJson.getString("avatar_url"),
                    bio = userJson.getString("bio"),
                    level = userJson.getInt("level"),
                    experience = userJson.getInt("experience"),
                    followersCount = userJson.getInt("followers_count"),
                    followingCount = userJson.getInt("following_count"),
                    gold = userJson.getInt("gold"),
                    userId = userJson.getInt("id"),
                    isFollowed = userJson.getInt("is_followed") == 1,
                    isBanned = userJson.getBoolean("is_banned"),
                    banEndTime = if (userJson.has("ban_end_time") && !userJson.isNull("ban_end_time"))
                        userJson.getString("ban_end_time") else null,
                    lastActivity = userJson.getString("last_activity_user"),
                    title = if (userJson.has("title") && !userJson.isNull("title"))
                        userJson.getString("title") else null,
                    titleStatus = userJson.getInt("title_status"),
                    backgroundUrl = if (userJson.has("background_url") && !userJson.isNull("background_url"))
                        userJson.getString("background_url") else null
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UserInfoActivity", "Error fetching user info", e)
            null
        }
    }
}

private suspend fun getUserMessages(
    client: OkHttpClient,
    token: String,
    userId: Int,
    page: Int = 1
): List<UserMessage> = withContext(Dispatchers.IO) {
    try {
        val request = Request.Builder()
            .url("${ApiAddress}get_user_messages")
            .post(
                JSONObject().apply {
                    put("per_page", 25)
                    put("page", page)
                    put("user_id", userId)
                }.toString().toRequestBody("application/json".toMediaType())
            )
            .addHeader("x-access-token", token)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) return@withContext emptyList()

        val json = JSONObject(response.body.string())
        val messagesArray = json.getJSONArray("messages")
        val messages = mutableListOf<UserMessage>()

        for (i in 0 until messagesArray.length()) {
            val msgJson = messagesArray.getJSONObject(i)
            val content = msgJson.getJSONObject("content")
            val messageType = msgJson.getInt("message_type")
            val isReferenced = msgJson.optBoolean("is_referenced", false)

            // 解析引用消息
            var refMsg: UserReferencedMessage? = null
            if (isReferenced && msgJson.has("referenced_message") && !msgJson.isNull("referenced_message")) {
                val refJson = msgJson.getJSONObject("referenced_message")
                refMsg = UserReferencedMessage(
                    sender_username = refJson.optString("sender_username", "未知用户"),
                    content = refJson.optString("content", ""),
                    message_type = refJson.optInt("message_type", 0),
                    image_url = refJson.optString("image_url", null)
                )
            }

            // 解析内容文本和标题
            val (contentText, title) = when (messageType) {
                1 -> "" to null
                2 -> content.optString("text", "") to null
                3 -> content.optString("content", "") to content.optString("title", null)
                else -> content.optString("text", "") to null
            }

            // 解析图片
            val images = mutableListOf<String>()
            try {
                when (val imagesValue = content.opt("images")) {
                    is JSONArray -> {
                        for (j in 0 until imagesValue.length()) {
                            images.add(imagesValue.getString(j))
                        }
                    }

                    is String -> if (imagesValue.isNotEmpty()) images.add(imagesValue)
                }
            } catch (_: JSONException) {
                content.optString("url").takeIf { it.isNotEmpty() }?.let { images.add(it) }
            }

            messages.add(
                UserMessage(
                    id = msgJson.getInt("message_id"),
                    username = msgJson.getString("username"),
                    avatar = msgJson.getString("avatar_url"),
                    content = contentText,
                    type = messageType,
                    images = images,
                    title = title,
                    likeCount = msgJson.getInt("like_count"),
                    time = msgJson.getString("timestamp_user"),
                    is_liked = false,
                    isMarkdown = msgJson.getBoolean("is_markdown"),
                    is_referenced = isReferenced,
                    referenced_message = refMsg
                )
            )
        }
        messages

    } catch (e: IOException) {
        Log.e("UserInfoActivity", "Network error loading messages", e)
        emptyList()
    } catch (e: JSONException) {
        Log.e("UserInfoActivity", "JSON parse error", e)
        emptyList()
    } catch (e: Exception) {
        Log.e("UserInfoActivity", "Unexpected error loading messages", e)
        emptyList()
    }
}

suspend fun loadNextPage(
    context: Context,
    userId: Int,
    page: Int,
    onResult: (List<UserMessage>) -> Unit
) {
    val client = OkHttpClient()
    val token = TokenManager.get(context)

    val msgs = token?.let { getUserMessages(client, it, userId, page + 1) }
    msgs?.let { onResult(it) }
}

private suspend fun performBanUser(
    context: Context,
    targetUserId: Int,
    hours: Int
): Boolean {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val token = TokenManager.get(context) ?: return@withContext false
        try {
            val request = Request.Builder()
                .url("${ApiAddress}ban_user")
                .post(
                    JSONObject().apply {
                        put("ban_hours", hours)
                        put("target_user_id", targetUserId)
                    }.toString().toRequestBody("application/json".toMediaType())
                )
                .addHeader("x-access-token", token)
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}

private suspend fun performReportUser(
    context: Context,
    targetUserId: Int,
    reason: String
): Boolean {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val token = TokenManager.get(context) ?: return@withContext false
        try {
            val request = Request.Builder()
                .url("${ApiAddress}report")
                .post(
                    JSONObject().apply {
                        put("content", reason)
                        put("report_type", 1) // 1 通常代表举报用户
                        put("target_id", targetUserId)
                    }.toString().toRequestBody("application/json".toMediaType())
                )
                .addHeader("x-access-token", token)
                .build()

            val response = client.newCall(request).execute()
            val responseData = response.body.string()

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "举报成功", Toast.LENGTH_SHORT).show()
                    true
                } else {
                    val msg = JSONObject(responseData).optString("message", "未知错误")
                    Toast.makeText(context, "失败: $msg", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
}

private suspend fun getUserResources(
    client: OkHttpClient,
    token: String,
    userId: Int
): List<ResourceItem> = withContext(Dispatchers.IO) {
    try {
        val request = Request.Builder()
            .url("${ApiAddress}get_user_resources")
            .post(
                JSONObject().apply { put("user_id", userId) }
                    .toString()
                    .toRequestBody("application/json".toMediaType())
            )
            .addHeader("x-access-token", token)
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            try {
                val jsonObject = JSONObject(response.body.string())
                val resourcesArray = jsonObject.getJSONArray("resources")
                val resourcesJson = resourcesArray.toString()
                AppJson.json.decodeFromString<List<ResourceItem>>(resourcesJson)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private suspend fun toggleFollow(
    context: Context,
    userId: Int,
    onResult: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val token = TokenManager.get(context)

        try {
            val request = token?.let {
                Request.Builder()
                    .url("${ApiAddress}follow")
                    .post(
                        JSONObject().apply {
                            put("user_id", userId)
                        }.toString().toRequestBody("application/json".toMediaType())
                    )
                    .addHeader("x-access-token", it)
            }
                ?.build()

            val response = request?.let { client.newCall(it) }?.execute()

            withContext(Dispatchers.Main) {
                response?.let { onResult(it.isSuccessful) }
            }
        } catch (e: Exception) {
            Log.e("UserInfoActivity", "Error toggling follow", e)
            withContext(Dispatchers.Main) {
                onResult(false)
            }
        }
    }
}