@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.mine

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.example.toolbox.MainViewModel
import com.example.toolbox.TokenManager
import com.example.toolbox.settings.SettingsActivity
import com.example.toolbox.LoginActivity
import com.example.toolbox.R
import com.example.toolbox.community.CommunityActivity
import com.example.toolbox.community.UserInfoActivity
import com.example.toolbox.mine.notice.NoticeActivity

@Composable
fun OnResumeScreen(
    onResume: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResume()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    mainViewModel: MainViewModel,
    onStart: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_preferences", MODE_PRIVATE)
    var isDisabledNotice by remember { mutableStateOf(prefs.getBoolean("disabled_community_notices", false)) }

    var userToken: String? by remember { mutableStateOf(TokenManager.get(context) ?: "null") }
    var userTokenOld: String? by remember { mutableStateOf(TokenManager.getOld(context) ?: "null") }

    val isLoadingUserInfo by mainViewModel.isLoadingUserInfo.collectAsState()
    val loadingUserInfoFail by mainViewModel.loadingUserInfoError.collectAsState()

    var isShowSignInDialog by remember { mutableStateOf(false) }

    val userInfo by mainViewModel.userInfo.collectAsState()

    LaunchedEffect(Unit) {
        mainViewModel.loadSuccess.collect { apiResponse ->
            TokenManager.saveTagStatus(context, apiResponse.user_info.title_status)
            TokenManager.saveUserID(context, apiResponse.user_info.id)
        }
    }

    OnResumeScreen(
        onResume = {
            isDisabledNotice = prefs.getBoolean("disabled_community_notices", false)
            val currentToken = TokenManager.get(context) ?: "null"
            if (currentToken != "null") {
                userToken = currentToken
                mainViewModel.refreshUserInfo(userToken!!)
            } else {
                userToken = "null"
                mainViewModel.changeUserAvatar("null")
            }
        }
    )

    if (isShowSignInDialog) {
        SignInDialog(
            hasSigned = userInfo.signed,
            onDismiss = {
                isShowSignInDialog = false
                userToken?.let { mainViewModel.refreshUserInfo(it) }
            },
            onUserClick = { username ->
                val intent = Intent(context, UserInfoActivity::class.java)
                intent.putExtra("username", username)
                context.startActivity(intent)
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("我的") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, SettingsActivity::class.java)
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = 10.dp, end = 10.dp)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            isRefreshing = isLoadingUserInfo,
            onRefresh = {
                userToken?.takeIf { it != "null" }?.let { token ->
                    onStart?.invoke()
                    mainViewModel.refreshUserInfo(token)
                }
            }
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                item {
                    ItemGroup(
                        items = buildList<@Composable () -> Unit> {
                            if (loadingUserInfoFail) {
                                add {
                                    Column(
                                        modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer)
                                    ) {
                                        Text(
                                            text = "获取信息失败",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            if (userToken == "null") {
                                add {
                                    Column(
                                        modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer)
                                    ) {
                                        Text(
                                            text = if (userTokenOld == "null") "你还没有登录" else "轻昼已使用加密存储Token，请重新登录",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            add {
                                CustomItem(
                                    onClick = {
                                        if (userToken != "null") {
                                            mainViewModel.changeUserDialogStatus(true)
                                        } else {
                                            val prefs = context.getSharedPreferences(
                                                "app_preferences",
                                                MODE_PRIVATE
                                            )
                                            prefs.edit().apply {
                                                remove("token")
                                                apply()
                                            }
                                            val intent =
                                                Intent(context, LoginActivity::class.java)
                                            context.startActivity(intent)
                                        }
                                    }
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (userToken != "null") {
                                            AsyncImage(
                                                model = userInfo.background,
                                                contentDescription = "背景",
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .alpha(0.2f),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Start,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Image(
                                                    painter = if (userInfo.avatar != "null") rememberAsyncImagePainter(
                                                        userInfo.avatar
                                                    ) else painterResource(
                                                        id = R.drawable.user
                                                    ),
                                                    contentDescription = "头像",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(58.dp)
                                                        .clip(CircleShape),
                                                    colorFilter = if (userInfo.avatar != "null") null else ColorFilter.tint(
                                                        MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                                Column(
                                                    modifier = Modifier.padding(start = 15.dp),
                                                ) {
                                                    Text(
                                                        text = if (userToken != "null") userInfo.name else "未登录",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Text(
                                                        text = if (userToken != "null") (if (userInfo.bio != "") userInfo.bio else "这个人懒得写简介~") else "点击登录",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                            if (userToken != "null") {
                                                if (userInfo.tagStatus != 0 && userInfo.tagStatus != 3) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            modifier = Modifier.size(14.dp),
                                                            contentDescription = null,
                                                            imageVector = Icons.Default.CheckCircle,
                                                            tint = when (userInfo.tagStatus) {
                                                                1 -> MaterialTheme.colorScheme.error
                                                                2 -> MaterialTheme.colorScheme.tertiary
                                                                4 -> MaterialTheme.colorScheme.primary
                                                                else -> MaterialTheme.colorScheme.onSurface
                                                            }
                                                        )
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text(
                                                            userInfo.tag,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Image(
                                                        modifier = Modifier.size(16.dp),
                                                        contentDescription = null,
                                                        painter = painterResource(
                                                            getLevelIconRes(userInfo.level.toString())
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        "${userInfo.level}",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    LinearProgressIndicator(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(8.dp),
                                                        progress = {
                                                            userInfo.exp / (userInfo.level * 100f)
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        "${userInfo.exp}/${(userInfo.level * 100f.toInt())}",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (userToken != "null") {
                                add {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                text = userInfo.coin.toString(),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "金币",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                text = userInfo.followers.toString(),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "粉丝",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                text = userInfo.following.toString(),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "关注",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                        }
                                    }
                                }

                                add {
                                    ListItem(
                                        headlineContent = {
                                            Text(text = if (userInfo.signed == 0) "签到" else "已签到")
                                        },
                                        leadingContent = {
                                            Icon(
                                                imageVector = Icons.Outlined.AssignmentTurnedIn,
                                                contentDescription = "签到",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = if (userInfo.signed == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                isShowSignInDialog = true
                                            }
                                    )
                                }

                                if (!isDisabledNotice) {
                                    add {
                                        ListItem(
                                            headlineContent = {
                                                Text(text = if (userInfo.notice == 1) "有新通知" else "通知")
                                            },
                                            leadingContent = {
                                                Icon(
                                                    imageVector = Icons.Outlined.Notifications,
                                                    contentDescription = "通知",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            },
                                            colors = ListItemDefaults.colors(
                                                containerColor = if (userInfo.notice == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val intent =
                                                        Intent(context, NoticeActivity::class.java)
                                                    context.startActivity(intent)
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                if (userToken != "null") {
                    item {
                        ItemGroup(
                            items = buildList<@Composable () -> Unit> {
                                add {
                                    ListItem(
                                        headlineContent = { Text("查看社区") },
                                        supportingContent = { Text("查看轻昼社区") },
                                        leadingContent = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                                contentDescription = "社区",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val intent = Intent(
                                                    context, CommunityActivity::class.java
                                                ).apply {
                                                    putExtra("userId", userInfo.id)
                                                    putExtra("username", userInfo.name)
                                                    putExtra("avatar", userInfo.avatar)
                                                    putExtra("authLevel", userInfo.tagStatus)
                                                }
                                                context.startActivity(intent)
                                            }
                                    )
                                }
                                add {
                                    ListItem(
                                        headlineContent = { Text("我的资源") },
                                        supportingContent = { Text("查看我的资源、投稿资源") },
                                        leadingContent = {
                                            Icon(
                                                imageVector = Icons.Default.Category,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val intent = Intent(
                                                    context,
                                                    MyResourceActivity::class.java
                                                )
                                                context.startActivity(intent)
                                            }
                                    )
                                }
                                add {
                                    ListItem(
                                        headlineContent = {
                                            Text("排行榜")
                                        },
                                        leadingContent = {
                                            Icon(
                                                imageVector = Icons.Outlined.Leaderboard,
                                                contentDescription = "排行榜",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val intent =
                                                    Intent(context, RankActivity::class.java)
                                                context.startActivity(intent)
                                            }
                                    )
                                }
                            }
                        )
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

@Composable
fun CustomItem(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun ItemGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    items: List<@Composable () -> Unit>,
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (!title.isNullOrEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }

        val cornerRadius = 24.dp
        val smallRadius = 4.dp

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEachIndexed { index, item ->
                val shape = when {
                    items.size == 1 -> RoundedCornerShape(cornerRadius)
                    index == 0 -> RoundedCornerShape(
                        topStart = cornerRadius,
                        topEnd = cornerRadius,
                        bottomStart = smallRadius,
                        bottomEnd = smallRadius
                    )

                    index == items.size - 1 -> RoundedCornerShape(
                        topStart = smallRadius,
                        topEnd = smallRadius,
                        bottomStart = cornerRadius,
                        bottomEnd = cornerRadius
                    )

                    else -> RoundedCornerShape(smallRadius)
                }

                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item()
                }
            }
        }
    }
}