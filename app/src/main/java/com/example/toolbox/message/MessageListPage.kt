package com.example.toolbox.message

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    onMenuClick: () -> Unit = {},
    mainViewModel: MainViewModel? = null
) {
    val token = TokenManager.get(LocalContext.current) ?: "null"
    val viewModel: MessageViewModel = viewModel(
        factory = MessageViewModelFactory(token)
    )
    val groupViewModel: GroupViewModel = viewModel(
        factory = GroupViewModelFactory(token)
    )

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val groupUiState by groupViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 图片选择器
    var selectedAvatarPath by remember { mutableStateOf<String?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val tempFile = java.io.File(context.cacheDir, "group_avatar_${System.currentTimeMillis()}.jpg")
                        java.io.FileOutputStream(tempFile).use { output ->
                            inputStream.copyTo(output)
                        }
                        selectedAvatarPath = tempFile.absolutePath
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "图片读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.connectWebSocket()
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= uiState.friends.size - 1) {
                    viewModel.loadMore()
                }
            }
    }

    // Toast 消息
    LaunchedEffect(groupViewModel) {
        groupViewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 导航到群聊信息页
    LaunchedEffect(groupViewModel) {
        groupViewModel.navigateToGroupInfo.collect { group ->
            val intent = Intent(context, GroupInfoActivity::class.java)
            intent.putExtra("group_id", group.id)
            intent.putExtra("is_joined", false)
            context.startActivity(intent)
            groupViewModel.hideJoinDialog()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("会话") },
                navigationIcon = {
                    IconButton(onClick = { onMenuClick() }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                },
                actions = {
                    if (token != "null") {
                        Box {
                            IconButton(onClick = { groupViewModel.showDropdownMenu() }) {
                                Icon(Icons.Default.Add, contentDescription = "添加")
                            }
                            
                            DropdownMenu(
                                expanded = groupUiState.showDropdownMenu,
                                onDismissRequest = { groupViewModel.hideDropdownMenu() }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("请求列表") },
                                    leadingIcon = {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                                    },
                                    onClick = {
                                        groupViewModel.hideDropdownMenu()
                                        val intent = Intent(context, FriendRequestActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("创建群聊") },
                                    leadingIcon = {
                                        Icon(Icons.Default.GroupAdd, contentDescription = null)
                                    },
                                    onClick = {
                                        groupViewModel.showCreateDialog()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("加入群") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Group, contentDescription = null)
                                    },
                                    onClick = {
                                        groupViewModel.showJoinDialog()
                                    }
                                )
                            }
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
                                    CircularProgressIndicator()
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

    // 加入群弹窗
    if (groupUiState.showJoinDialog) {
        JoinGroupDialog(
            groupId = groupUiState.searchGroupId,
            isSearching = groupUiState.isSearching,
            onGroupIdChange = { groupViewModel.updateSearchGroupId(it) },
            onSearch = { groupViewModel.searchGroup() },
            onDismiss = { groupViewModel.hideJoinDialog() }
        )
    }

    // 创建群聊弹窗
    if (groupUiState.showCreateDialog) {
        CreateGroupDialog(
            name = groupUiState.createGroupName,
            description = groupUiState.createGroupDescription,
            isPrivate = groupUiState.createGroupIsPrivate,
            avatarPath = selectedAvatarPath,
            isCreating = groupUiState.isCreating,
            onNameChange = { groupViewModel.updateCreateGroupName(it) },
            onDescriptionChange = { groupViewModel.updateCreateGroupDescription(it) },
            onPrivateChange = { groupViewModel.updateCreateGroupIsPrivate(it) },
            onSelectAvatar = { imagePickerLauncher.launch("image/*") },
            onCreate = {
                groupViewModel.createGroup(selectedAvatarPath)
                selectedAvatarPath = null
            },
            onDismiss = {
                groupViewModel.hideCreateDialog()
                selectedAvatarPath = null
            }
        )
    }
}

@Composable
fun JoinGroupDialog(
    groupId: String,
    isSearching: Boolean,
    onGroupIdChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入群") },
        text = {
            Column {
                OutlinedTextField(
                    value = groupId,
                    onValueChange = onGroupIdChange,
                    label = { Text("群号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSearch,
                enabled = !isSearching
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("搜索")
                }
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
fun CreateGroupDialog(
    name: String,
    description: String,
    isPrivate: Boolean,
    avatarPath: String?,
    isCreating: Boolean,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPrivateChange: (Boolean) -> Unit,
    onSelectAvatar: () -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建群聊") },
        text = {
            Column {
                // 群名称输入框（带加号按钮）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (avatarPath != null) {
                        Image(
                            painter = rememberAsyncImagePainter(avatarPath),
                            contentDescription = "群头像预览",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        IconButton(onClick = onSelectAvatar) {
                            Icon(Icons.Default.Add, contentDescription = "选择头像")
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("群聊名称") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 描述输入框
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("群描述") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 是否私有开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("私有群")
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = onPrivateChange
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCreate,
                enabled = !isCreating && name.isNotBlank()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("创建")
                }
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
fun FriendItem(friend: Friend) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, MessageDetailActivity::class.java)
                if (friend.type == "group") {
                    intent.putExtra("chat_type", 2)
                    intent.putExtra("chat_id", friend.id)
                } else {
                    intent.putExtra("chat_type", 1)
                    intent.putExtra("user_id", friend.id)
                }
                context.startActivity(intent)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
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

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (friend.type == "group") friend.name else friend.username,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (friend.title.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
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
                Spacer(modifier = Modifier.weight(1f))
                if (friend.type == "group") {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "群聊",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
