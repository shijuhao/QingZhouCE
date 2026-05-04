package com.example.toolbox.message

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.toolbox.TokenManager
import com.example.toolbox.community.UserInfoActivity
import com.example.toolbox.data.GroupJoinRequest
import com.example.toolbox.ui.theme.ToolBoxTheme
import androidx.compose.runtime.collectAsState
import java.text.SimpleDateFormat
import java.util.*

class JoinRequestsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val groupId = intent.getIntExtra("group_id", -1)
        val groupName = intent.getStringExtra("group_name") ?: "入群申请"

        setContent {
            ToolBoxTheme {
                val token = TokenManager.get(this)
                val viewModel: GroupInfoViewModel = viewModel(
                    factory = token?.let { GroupInfoViewModelFactory(it, groupId, null) }
                )

                JoinRequestsScreen(
                    viewModel = viewModel,
                    groupName = groupName,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRequestsScreen(
    viewModel: GroupInfoViewModel,
    groupName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState(initial = GroupInfoUiState())

    LaunchedEffect(Unit) {
        viewModel.loadJoinRequests()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadJoinRequests() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoadingRequests) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.joinRequests.isEmpty()) {
                Text(
                    text = "暂无入群申请",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.joinRequests, key = { it.userId }) { request ->
                        JoinRequestItem(
                            request = request,
                            onApprove = { viewModel.auditJoinRequest(request.userId, true) },
                            onReject = { viewModel.auditJoinRequest(request.userId, false) },
                            onUserClick = { userId ->
                                val intent = Intent(context, UserInfoActivity::class.java)
                                intent.putExtra("userId", userId)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JoinRequestItem(
    request: GroupJoinRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onUserClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = if (request.avatarUrl.startsWith("http")) request.avatarUrl
                    else "${com.example.toolbox.ApiAddress}uploads/${request.avatarUrl}",
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onUserClick(request.userId) }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatJoinTime(request.applyTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row {
                FilledTonalIconButton(
                    onClick = onApprove,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "通过",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                FilledTonalIconButton(
                    onClick = onReject,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "拒绝",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

fun formatJoinTime(timeStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(timeStr) ?: return timeStr
        val now = Date()
        val diff = now.time - date.time

        when {
            diff < 60 * 1000 -> "刚刚"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
            else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        }
    } catch (_: Exception) {
        timeStr
    }
}
