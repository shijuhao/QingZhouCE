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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.TokenManager
import com.example.toolbox.data.GroupMember
import com.example.toolbox.community.UserInfoActivity
import com.example.toolbox.ui.theme.ToolBoxTheme

class GroupMembersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val groupId = intent.getIntExtra("group_id", -1)
        
        setContent {
            ToolBoxTheme {
                val token = TokenManager.get(this)
                val viewModel: GroupMembersViewModel = viewModel(
                    factory = token?.let { GroupMembersViewModelFactory(it, groupId) }
                )
                GroupMembersScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersScreen(
    viewModel: GroupMembersViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showMuteDialog by remember { mutableStateOf(false) }
    var muteUserId by remember { mutableIntStateOf(0) }
    var muteDuration by remember { mutableStateOf("60") }
    var showKickDialog by remember { mutableStateOf(false) }
    var kickUserId by remember { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 禁言对话框
    if (showMuteDialog) {
        AlertDialog(
            onDismissRequest = { showMuteDialog = false },
            title = { Text("禁言成员") },
            text = {
                Column {
                    Text("请输入禁言时长（分钟）")
                    Spacer(modifier = Modifier.padding(8.dp))
                    OutlinedTextField(
                        value = muteDuration,
                        onValueChange = { muteDuration = it },
                        label = { Text("分钟") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val duration = muteDuration.toIntOrNull() ?: 60
                    viewModel.muteMember(muteUserId, duration)
                    showMuteDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMuteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 踢出确认对话框
    if (showKickDialog) {
        AlertDialog(
            onDismissRequest = { showKickDialog = false },
            title = { Text("踢出成员") },
            text = { Text("确定要踢出该成员吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.kickMember(kickUserId)
                        showKickDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKickDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("群成员 (${uiState.members.size})") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = "错误: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.members, key = { it.userId }) { member ->
                        MemberItem(
                            member = member,
                            myRole = uiState.myRole,
                            onKick = {
                                kickUserId = member.userId
                                showKickDialog = true
                            },
                            onSetAdmin = { viewModel.setAdmin(member.userId, true) },
                            onRemoveAdmin = { viewModel.setAdmin(member.userId, false) },
                            onMute = {
                                muteUserId = member.userId
                                showMuteDialog = true
                            },
                            onUnmute = { viewModel.unmuteMember(member.userId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberItem(
    member: GroupMember,
    myRole: Int,
    onKick: () -> Unit,
    onSetAdmin: () -> Unit,
    onRemoveAdmin: () -> Unit,
    onMute: () -> Unit,
    onUnmute: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable {
                val intent = Intent(context, UserInfoActivity::class.java)
                intent.putExtra("userId", member.userId)
                context.startActivity(intent)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = if (member.avatarUrl.startsWith("http")) member.avatarUrl
                else "${ApiAddress}uploads/${member.avatarUrl}",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.nickname.ifEmpty { member.username },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.padding(2.dp))
            
            Text(
                text = when (member.role) {
                    2 -> "群主"
                    1 -> "管理员"
                    else -> "成员"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (myRole > 0 && member.role < 2) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (myRole == 2) {
                        if (member.role == 0) {
                            DropdownMenuItem(
                                text = { Text("设为管理员") },
                                onClick = {
                                    showMenu = false
                                    onSetAdmin()
                                }
                            )
                        } else if (member.role == 1) {
                            DropdownMenuItem(
                                text = { Text("取消管理员") },
                                onClick = {
                                    showMenu = false
                                    onRemoveAdmin()
                                }
                            )
                        }
                    }
                    
                    DropdownMenuItem(
                        text = { Text("禁言") },
                        onClick = {
                            showMenu = false
                            onMute()
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { Text("解除禁言") },
                        onClick = {
                            showMenu = false
                            onUnmute()
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { Text("踢出群聊") },
                        onClick = {
                            showMenu = false
                            onKick()
                        }
                    )
                }
            }
        }
    }
}
