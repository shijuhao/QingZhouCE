package com.example.toolbox.message

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.TokenManager
import com.example.toolbox.data.GroupInfo
import com.example.toolbox.ui.theme.ToolBoxTheme
import java.text.SimpleDateFormat
import java.util.Locale

class GroupInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val groupId = intent.getIntExtra("group_id", -1)
        
        val initialGroupInfo = if (intent.hasExtra("group_name")) {
            GroupInfo(
                id = groupId,
                name = intent.getStringExtra("group_name") ?: "",
                avatarUrl = intent.getStringExtra("group_avatar") ?: "",
                description = intent.getStringExtra("group_description") ?: "",
                isPrivate = intent.getBooleanExtra("group_is_private", false),
                membersCount = intent.getIntExtra("group_members_count", 0),
                createdAt = intent.getStringExtra("group_created_at") ?: "",
                creator = null
            )
        } else null
        
        setContent {
            ToolBoxTheme {
                val token = TokenManager.get(this)
                val viewModel: GroupInfoViewModel = viewModel(
                    factory = token?.let { GroupInfoViewModelFactory(it, groupId, initialGroupInfo) }
                )
                GroupInfoScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    viewModel: GroupInfoViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.joinSuccess.collect {
            onBack()
        }
    }

    // 退出群聊确认对话框
    if (uiState.showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideLeaveDialog() },
            title = { Text("退出群聊") },
            text = { Text("确定要退出该群聊吗？") },
            confirmButton = {
                Button(
                    onClick = { viewModel.leaveGroup(onBack) },
                    enabled = !uiState.isLeaving
                ) {
                    if (uiState.isLeaving) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    else Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideLeaveDialog() }) { Text("取消") }
            }
        )
    }

    // 解散群聊确认对话框
    if (uiState.showDissolveDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDissolveDialog() },
            title = { Text("解散群聊") },
            text = { Text("确定要解散该群聊吗？此操作不可撤销！") },
            confirmButton = {
                Button(
                    onClick = { viewModel.dissolveGroup(onBack) },
                    enabled = !uiState.isDissolving,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (uiState.isDissolving) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    else Text("解散")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDissolveDialog() }) { Text("取消") }
            }
        )
    }

    // 标签编辑对话框
    if (uiState.showTagDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideTagDialog() },
            title = { Text(if (uiState.editingTag != null) "编辑标签" else "创建标签") },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.newTagName,
                        onValueChange = { viewModel.updateNewTagName(it) },
                        label = { Text("标签名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.newTagColor,
                        onValueChange = { viewModel.updateNewTagColor(it) },
                        label = { Text("颜色 (如 #FF6B6B)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("预览:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = try { Color(android.graphics.Color.parseColor(uiState.newTagColor)) }
                            catch (_: Exception) { MaterialTheme.colorScheme.primary }
                    ) {
                        Text(
                            text = uiState.newTagName.ifEmpty { "标签" },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val editingTag = uiState.editingTag
                    if (editingTag != null) {
                        viewModel.editTag(editingTag.id, uiState.newTagName, uiState.newTagColor)
                    } else {
                        viewModel.createTag(uiState.newTagName, uiState.newTagColor)
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideTagDialog() }) { Text("取消") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("群聊信息") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.isJoined && uiState.myRole > 0) {
                        IconButton(onClick = { viewModel.loadJoinRequests() }) {
                            Icon(Icons.Default.Group, contentDescription = "入群申请")
                        }
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    if (uiState.isJoined) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (uiState.myRole == 2) {
                                DropdownMenuItem(
                                    text = { Text("解散群聊") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.showDissolveDialog()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("退出群聊") },
                                onClick = {
                                    showMenu = false
                                    viewModel.showLeaveDialog()
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) }
                            )
                        }
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
            if (uiState.isLoading && uiState.group == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.group != null) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val group = uiState.group!!
                        
                        // 群头像
                        AsyncImage(
                            model = if (group.avatarUrl.startsWith("http")) group.avatarUrl 
                                else "${ApiAddress}uploads/${group.avatarUrl}",
                            contentDescription = "群头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 群名称
                        Text(
                            text = group.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 群号
                        Text(
                            text = "群号: ${group.id}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 群信息卡片
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Group, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("成员数: ${group.membersCount}")
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (uiState.isJoined) {
                                        TextButton(onClick = {
                                            context.startActivity(
                                                Intent(context, GroupMembersActivity::class.java).apply {
                                                    putExtra("group_id", group.id)
                                                }
                                            )
                                        }) {
                                            Text("查看全部")
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("创建时间: ${formatGroupTime(group.createdAt)}")
                                }
                                
                                if (group.isPrivate) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("私有群", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 群简介
                        if (group.description.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("群简介", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(group.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 创建者信息
                        group.creator?.let { creator ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = if (creator.avatarUrl.startsWith("http")) creator.avatarUrl
                                            else "${ApiAddress}uploads/${creator.avatarUrl}",
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("群主", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(creator.username, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        
                        // 标签管理 (群主和管理员)
                        if (uiState.isJoined && uiState.myRole > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Tag, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("群标签", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(onClick = { viewModel.showTagDialog() }) {
                                            Icon(Icons.Default.Add, contentDescription = "添加标签")
                                        }
                                    }
                                    
                                    if (uiState.isLoadingTags) {
                                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                    } else if (uiState.tags.isEmpty()) {
                                        Text(
                                            "暂无标签",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            uiState.tags.forEach { tag ->
                                                var showTagMenu by remember { mutableStateOf(false) }
                                                Box {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = try { Color(android.graphics.Color.parseColor(tag.color)) }
                                                            catch (_: Exception) { MaterialTheme.colorScheme.primary },
                                                        modifier = Modifier.clickable {
                                                            if (uiState.myRole == 2) {
                                                                viewModel.showTagDialog(tag)
                                                            }
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = tag.name,
                                                                fontSize = 12.sp,
                                                                color = Color.White
                                                            )
                                                            if (uiState.myRole == 2) {
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Icon(
                                                                    Icons.Default.Delete,
                                                                    contentDescription = "删除",
                                                                    modifier = Modifier
                                                                        .size(14.dp)
                                                                        .clickable { showTagMenu = true },
                                                                    tint = Color.White
                                                                )
                                                            }
                                                        }
                                                    }
                                                    DropdownMenu(
                                                        expanded = showTagMenu,
                                                        onDismissRequest = { showTagMenu = false }
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text("编辑") },
                                                            onClick = {
                                                                showTagMenu = false
                                                                viewModel.showTagDialog(tag)
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("删除") },
                                                            onClick = {
                                                                showTagMenu = false
                                                                viewModel.deleteTag(tag.id)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 入群申请列表 (群主和管理员)
                        if (uiState.isJoined && uiState.myRole > 0 && uiState.joinRequests.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("入群申请", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    uiState.joinRequests.forEach { request ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = if (request.avatarUrl.startsWith("http")) request.avatarUrl
                                                    else "${ApiAddress}uploads/${request.avatarUrl}",
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(request.username, fontWeight = FontWeight.Medium)
                                                Text(
                                                    formatGroupTime(request.applyTime),
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.auditJoinRequest(request.userId, true) }
                                            ) {
                                                Text("通过")
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Button(
                                                onClick = { viewModel.auditJoinRequest(request.userId, false) }
                                            ) {
                                                Text("拒绝")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 底部按钮
                        if (!uiState.isJoined) {
                            Button(
                                onClick = { viewModel.joinGroup() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isJoining
                            ) {
                                if (uiState.isJoining) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("加入群聊")
                                }
                            }
                        }
                    }
                }
            } else if (uiState.error != null) {
                Text(
                    text = "错误: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

fun formatGroupTime(timeStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(timeStr) ?: return timeStr
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        timeStr
    }
}
