package com.example.toolbox.message

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.toolbox.community.UserInfoActivity
import com.example.toolbox.community.uploadImage
import com.example.toolbox.settings.SettingsGroup
import com.example.toolbox.settings.SettingsItemCell
import com.example.toolbox.settings.SettingsCustomItem
import com.example.toolbox.ui.theme.ToolBoxTheme
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.launch

class GroupInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val shareKey = intent.data?.getQueryParameter("key")
        val groupId = if (shareKey != null) {
            -1
        } else {
            intent.getIntExtra("group_id", -1)
        }
        
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
                    factory = token?.let { 
                        GroupInfoViewModelFactory(it, groupId, initialGroupInfo, shareKey) 
                    }
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
    val scope = rememberCoroutineScope()
    
    // 图片选择器
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // 上传图片
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val tempFile = java.io.File(context.cacheDir, "group_avatar_${System.currentTimeMillis()}.jpg")
                        java.io.FileOutputStream(tempFile).use { output ->
                            inputStream.copyTo(output)
                        }
                        val token = TokenManager.get(context) ?: ""
                        val imageUrl = uploadImage(
                            filePath = tempFile.absolutePath,
                            token = token,
                            status = 3,
                            onProgress = { /* 可以显示进度 */ }
                        )
                        if (imageUrl != null) {
                            viewModel.updateEditingAvatarUrl(imageUrl)
                            Toast.makeText(context, "头像上传成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "头像上传失败", Toast.LENGTH_SHORT).show()
                        }
                        tempFile.delete()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "图片读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
                        color = try { Color(uiState.newTagColor.toColorInt()) }
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

    if (uiState.showEditDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideEditDialog() },
            title = { Text("编辑群信息") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // 头像选择区域
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            if (uiState.editingAvatarUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = if (uiState.editingAvatarUrl.startsWith("http")) uiState.editingAvatarUrl
                                        else "${ApiAddress}uploads/${uiState.editingAvatarUrl}",
                                    contentDescription = "群头像预览",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(Icons.Default.Add, contentDescription = "选择头像")
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = uiState.editingName,
                            onValueChange = { viewModel.updateEditingName(it) },
                            label = { Text("群名称") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = uiState.editingDescription,
                        onValueChange = { viewModel.updateEditingDescription(it) },
                        label = { Text("群简介") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 进群审核开关
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { viewModel.updateEditingJoinVerification(!uiState.editingJoinVerification) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "进群审核",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "进群审核",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "新成员加入需要管理员审核",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = uiState.editingJoinVerification,
                            onCheckedChange = null,
                            thumbContent = {
                                Icon(
                                    imageVector = if (uiState.editingJoinVerification) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    tint = if (uiState.editingJoinVerification) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    }
                                )
                            }
                        )
                    }
                    
                    // 允许分享开关
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { viewModel.updateEditingShareEnabled(!uiState.editingShareEnabled) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "允许分享",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "允许分享",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "允许成员生成分享链接邀请他人加入",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = uiState.editingShareEnabled,
                            onCheckedChange = null,
                            thumbContent = {
                                Icon(
                                    imageVector = if (uiState.editingShareEnabled) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    tint = if (uiState.editingShareEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    }
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.editGroupInfo(
                            name = uiState.editingName,
                            description = uiState.editingDescription,
                            avatarUrl = uiState.editingAvatarUrl,
                            joinVerification = uiState.editingJoinVerification,
                            shareEnabled = uiState.editingShareEnabled
                        )
                        viewModel.hideEditDialog()
                    },
                    enabled = !uiState.isEditing
                ) {
                    if (uiState.isEditing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("保存")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideEditDialog() }) { Text("取消") }
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
                    if (uiState.isJoined && uiState.myRole > 0 && uiState.group != null) {
                        val group = uiState.group!!
                        IconButton(onClick = { 
                            val intent = Intent(context, JoinRequestsActivity::class.java)
                            intent.putExtra("group_id", group.id)
                            intent.putExtra("group_name", "${group.name} - 入群申请")
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Group, contentDescription = "入群申请")
                        }
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    var showShareDialog by remember { mutableStateOf(false) }
                    
                    if (uiState.isJoined) {
                        // 如果是群主或管理员，显示编辑按钮
                        if (uiState.myRole > 0) {
                            IconButton(onClick = { viewModel.showEditDialog() }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑群信息")
                            }
                        }

                        IconButton(onClick = { 
                            showShareDialog = true
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "分享群聊")
                        }

                        Box {
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
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("退出群聊") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.showLeaveDialog()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ExitToApp,
                                                null
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (showShareDialog && uiState.group != null) {
                        val group = uiState.group!!
                        
                        LaunchedEffect(Unit) {
                            viewModel.createShareLink(expireHours = 0) {  }
                        }
                        
                        AlertDialog(
                            onDismissRequest = { 
                                showShareDialog = false
                                viewModel.clearShareLinks()
                            },
                            title = { Text("分享群聊") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // 外链
                                    Text(
                                        "外链",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = uiState.shareUrl,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                                                clipboard?.setPrimaryClip(
                                                    android.content.ClipData.newPlainText("share_url", uiState.shareUrl)
                                                )
                                                Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "复制外链")
                                            }
                                        },
                                        singleLine = true
                                    )
                                    
                                    // 内链（URL Scheme）
                                    Text(
                                        "内链",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val internalUrl = "qz://group?key=${uiState.shareKey}"
                                    OutlinedTextField(
                                        value = internalUrl,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                                                clipboard?.setPrimaryClip(
                                                    android.content.ClipData.newPlainText("share_url", internalUrl)
                                                )
                                                Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "复制内链")
                                            }
                                        },
                                        singleLine = true
                                    )
                                    
                                    if (uiState.isGeneratingShare) {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                TextButton(onClick = { 
                                    showShareDialog = false
                                    viewModel.clearShareLinks()
                                }) {
                                    Text("关闭")
                                }
                            }
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
            if (uiState.isLoading && uiState.group == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.group != null) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val group = uiState.group!!

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
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

                                Text(
                                    text = group.name,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "群号: ${group.id}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        item {
                            SettingsGroup(
                                title = "群聊信息",
                                items = listOf(
                                    {
                                        SettingsItemCell(
                                            icon = Icons.Default.Person,
                                            title = "成员数",
                                            subtitle = "${group.membersCount} 名成员",
                                            onClick = {
                                                if (uiState.isJoined) {
                                                    context.startActivity(
                                                        Intent(context, GroupMembersActivity::class.java).apply {
                                                            putExtra("group_id", group.id)
                                                        }
                                                    )
                                                }
                                            }
                                        )
                                    },
                                    {
                                        SettingsItemCell(
                                            icon = Icons.Default.DateRange,
                                            title = "创建时间",
                                            subtitle = formatGroupTime(group.createdAt),
                                            onClick = {}
                                        )
                                    },
                                    {
                                        if (group.isPrivate) {
                                            SettingsItemCell(
                                                icon = Icons.Default.Lock,
                                                title = "群类型",
                                                subtitle = "私有群",
                                                onClick = {},
                                                isDestructive = true
                                            )
                                        } else {
                                            SettingsItemCell(
                                                icon = Icons.Default.Public,
                                                title = "群类型",
                                                subtitle = "公开群",
                                                onClick = {}
                                            )
                                        }
                                    }
                                )
                            )
                        }

                        if (group.description.isNotBlank()) {
                            item {
                                SettingsGroup(
                                    title = "群聊简介",
                                    items = listOf(
                                        {
                                            SettingsCustomItem {
                                                Text(
                                                    text = group.description,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(16.dp)
                                                )
                                            }
                                        }
                                    )
                                )
                            }
                        }

                        group.creator?.let { creator ->
                            item {
                                SettingsGroup(
                                    title = "群主",
                                    items = listOf(
                                        {
                                            SettingsCustomItem {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            val intent = Intent(context, UserInfoActivity::class.java)
                                                            intent.putExtra("userId", creator.id)
                                                            context.startActivity(intent)
                                                        }
                                                        .padding(16.dp),
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
                                                    Text(creator.username, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    )
                                )
                            }
                        }

                        if (uiState.isJoined && uiState.myRole > 0) {
                            item {
                                SettingsGroup(
                                    title = "群标签",
                                    items = buildList {
                                        if (uiState.isLoadingTags) {
                                            add {
                                                SettingsCustomItem {
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator()
                                                    }
                                                }
                                            }
                                        } else if (uiState.tags.isEmpty()) {
                                            add {
                                                SettingsCustomItem {
                                                    Text(
                                                        "暂无标签",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(16.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            uiState.tags.forEach { tag ->
                                                add {
                                                    SettingsCustomItem(onClick = {
                                                        if (uiState.myRole > 0) {
                                                            viewModel.showTagDialog(tag)
                                                        }
                                                    }) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = try {
                                                                        Color(tag.color.toColorInt()) }
                                                                        catch (_: Exception) { MaterialTheme.colorScheme.primary },
                                                                    modifier = Modifier.size(12.dp)
                                                                ) {}
                                                                Spacer(modifier = Modifier.width(12.dp))
                                                                Text(tag.name, style = MaterialTheme.typography.bodyLarge)
                                                            }
                                                            if (uiState.myRole > 0) {
                                                                Icon(
                                                                    Icons.Default.Delete,
                                                                    contentDescription = "删除标签",
                                                                    modifier = Modifier
                                                                        .size(20.dp)
                                                                        .clickable { viewModel.deleteTag(tag.id) },
                                                                    tint = MaterialTheme.colorScheme.error
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (uiState.myRole > 0) {
                                            add {
                                                SettingsCustomItem(onClick = { viewModel.showTagDialog() }) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(16.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Add,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Text("添加标签", color = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            if (!uiState.isJoined) {
                                Button(
                                    onClick = { viewModel.joinGroup() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
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

                        item {
                            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
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
