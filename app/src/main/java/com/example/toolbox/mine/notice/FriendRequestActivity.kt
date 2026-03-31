package com.example.toolbox.mine.notice

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import com.example.toolbox.TokenManager
import com.example.toolbox.data.mine.notice.FriendRequest
import com.example.toolbox.ui.theme.ToolBoxTheme

class FriendRequestActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("好友请求") },
                            navigationIcon = {
                                FilledTonalIconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        val token = TokenManager.get(this@FriendRequestActivity)
                        val viewModel: FriendRequestViewModel = viewModel(
                            factory = token?.let { FriendRequestViewModelFactory(it) }
                        )
                        FriendRequestScreen(viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestScreen(viewModel: FriendRequestViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val processingIds by viewModel.processingIds.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // 显示操作结果的 Toast
    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 自动加载更多
    LaunchedEffect(listState, uiState.requests) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= uiState.requests.size - 1) {
                    viewModel.loadMore()
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab 切换
        SecondaryTabRow(
            selectedTabIndex = if (uiState.currentType == RequestType.RECEIVED) 0 else 1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = uiState.currentType == RequestType.RECEIVED,
                onClick = { viewModel.switchType(RequestType.RECEIVED) },
                text = { Text("收到的请求") }
            )
            Tab(
                selected = uiState.currentType == RequestType.SENT,
                onClick = { viewModel.switchType(RequestType.SENT) },
                text = { Text("发出的请求") }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.requests) { request ->
                        // 只有“收到的请求”且状态为“待处理”(status=0) 才显示接受/拒绝按钮
                        val showActions = uiState.currentType == RequestType.RECEIVED && request.status == 0
                        FriendRequestItem(
                            request = request,
                            processingIds = processingIds,
                            showActions = showActions,
                            onAccept = { viewModel.respondToRequest(request.requestId, true) },
                            onDecline = { viewModel.respondToRequest(request.requestId, false) }
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
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }

            // 错误提示
            if (uiState.error != null && uiState.requests.isEmpty()) {
                Text(
                    text = "错误: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 空状态
            if (!uiState.isLoading && !uiState.isRefreshing && uiState.requests.isEmpty() && uiState.error == null) {
                Text(
                    text = "暂无请求",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun FriendRequestItem(
    request: FriendRequest,
    processingIds: Set<Int>,
    showActions: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val isProcessing = request.requestId in processingIds

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 顶部：头像 + 用户信息（保持水平排列）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = rememberAsyncImagePainter(request.user.avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = request.user.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (request.user.title.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = request.user.title,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 时间显示
                    Text(
                        text = request.createdAtDisplay,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 状态标签
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (request.status) {
                            0 -> MaterialTheme.colorScheme.secondaryContainer
                            1 -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = when (request.status) {
                            0 -> MaterialTheme.colorScheme.onSecondaryContainer
                            1 -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ) {
                        Text(
                            text = request.statusText,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 3.dp)
                        )
                    }
                }
            }
        }

        if (showActions) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onAccept,
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("接受", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isProcessing
                ) {
                    Text("拒绝", fontSize = 12.sp)
                }
            }
        }
    }
}