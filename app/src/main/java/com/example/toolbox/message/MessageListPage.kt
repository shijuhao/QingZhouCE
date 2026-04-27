package com.example.toolbox.message

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import com.example.toolbox.MainViewModel
import com.example.toolbox.TokenManager
import com.example.toolbox.data.Friend
import com.example.toolbox.mine.notice.FriendRequestActivity
import com.example.toolbox.mine.notice.snapshotFlow
import com.example.toolbox.utils.UserAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessageScreen(
    onMenuClick: () -> Unit = {},
    mainViewModel: MainViewModel? = null
) {
    val token = TokenManager.get(LocalContext.current) ?: "null"
    val viewModel: MessageViewModel = viewModel(
        factory = MessageViewModelFactory(token)
    )

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.connectWebSocket()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnectWebSocket()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= uiState.friends.size - 1) {
                    viewModel.loadMore()
                }
            }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("私信") },
                navigationIcon = {
                    IconButton(onClick = { onMenuClick() }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                },
                actions = {
                    if (token != "null") {
                        IconButton(onClick = {
                            val intent =
                                Intent(context, FriendRequestActivity::class.java)
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "请求列表")
                        }
                    }
                    
                    if (mainViewModel != null) {
                        val userInfo by mainViewModel.userInfo.collectAsState()
                        UserAvatar(
                            avatarUrl = userInfo.avatar,
                            userId = userInfo.id
                        )
                    }
                }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.friends, key = { it.id }) { friend ->
                            FriendItem(friend = friend)
                        }
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularWavyProgressIndicator()
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

                if (uiState.error != null && uiState.friends.isEmpty()) {
                    Text(
                        text = "错误: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (!uiState.isLoading && !uiState.isRefreshing && uiState.friends.isEmpty() && uiState.error == null) {
                    Text(
                        text = "暂无私信",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun FriendItem(friend: Friend) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable {
                val intent = Intent(context, MessageDetailActivity::class.java)
                intent.putExtra("user_id", friend.id)
                context.startActivity(intent)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Image(
                painter = rememberAsyncImagePainter(friend.avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            )
            if (friend.unreadCount > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                ) {
                    Text(text = friend.unreadCount.toString(), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 右侧内容
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = friend.username,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (friend.title.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier.size(14.dp),
                            contentDescription = null,
                            imageVector = Icons.Default.CheckCircle,
                            tint = when (friend.titleStatus) {
                                1 -> MaterialTheme.colorScheme.error
                                2 -> MaterialTheme.colorScheme.tertiary
                                4 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = friend.lastMessage ?: "暂无消息",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (friend.unreadCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f)
                )

                if (friend.lastMessageTime != null) {
                    Text(
                        text = formatRelativeTime(friend.lastMessageTime),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

fun formatRelativeTime(timeStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(timeStr) ?: return ""
        val now = Date()
        val diff = now.time - date.time
        when {
            diff < 60 * 1000 -> "刚刚"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
            diff < 24 * 60 * 60 * 1000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(date)
        }
    } catch (_: Exception) {
        timeStr.substring(11, 16) // 如果解析失败，至少显示时间部分
    }
}