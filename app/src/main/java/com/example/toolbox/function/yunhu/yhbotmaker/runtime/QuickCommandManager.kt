@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.toolbox.function.yunhu.yhbotmaker.toast
import androidx.core.content.edit
import com.example.toolbox.AppJson
import kotlinx.serialization.SerialName

// 数据类
data class AutoReply(
    val key: String,
    val reply: String,
    val type: String
)

data class QuickCommand(
    val id: Int,
    val did: String
)

data class CommandData(
    @SerialName("自动回复")
    val autoReplies: List<AutoReply> = emptyList(),
    @SerialName("快捷命令")
    val quickCommands: List<QuickCommand> = emptyList()
)

class QuickCommandManager(context: Context, botIndex: Int) {
    private val prefs = context.getSharedPreferences("bot_prefs", Context.MODE_PRIVATE)
    private val key = "chelper$botIndex"

    fun load(): CommandData {
        val json = prefs.getString(key, "") ?: ""
        if (json.isBlank()) return CommandData()
        return try {
            AppJson.json.decodeFromString<CommandData>(json)
        } catch (_: Exception) {
            CommandData()
        }
    }

    fun save(data: CommandData) {
        prefs.edit { putString(key, AppJson.json.encodeToString(data)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCommandManagerDialog(
    botIndex: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { QuickCommandManager(context, botIndex) }
    var data by remember { mutableStateOf(manager.load()) }

    var showAutoReplyDialog by remember { mutableStateOf(false) }
    var editingAutoReply by remember { mutableStateOf<AutoReply?>(null) }
    var autoReplyIndex by remember { mutableIntStateOf(-1) }

    var showQuickCommandDialog by remember { mutableStateOf(false) }
    var editingQuickCommand by remember { mutableStateOf<QuickCommand?>(null) }
    var quickCommandIndex by remember { mutableIntStateOf(-1) }

    var selectedTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快捷指令管理") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // 选项卡
                SecondaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("自动回复 (${data.autoReplies.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("快捷命令 (${data.quickCommands.size})") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 内容区域
                when (selectedTab) {
                    0 -> {
                        // 自动回复列表
                        if (data.autoReplies.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无自动回复，点击下方 + 添加")
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                itemsIndexed(data.autoReplies) { index, item ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("关键词: ${item.key}", style = MaterialTheme.typography.bodyMedium)
                                                Text("回复: ${item.reply} (${item.type})", style = MaterialTheme.typography.bodySmall)
                                            }
                                            IconButton(onClick = {
                                                editingAutoReply = item
                                                autoReplyIndex = index
                                                showAutoReplyDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, null)
                                            }
                                            IconButton(onClick = {
                                                data = data.copy(autoReplies = data.autoReplies.toMutableList().apply { removeAt(index) })
                                                manager.save(data)
                                            }) {
                                                Icon(Icons.Default.Delete, null)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // 快捷命令列表
                        if (data.quickCommands.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无快捷命令，点击下方 + 添加")
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                itemsIndexed(data.quickCommands) { index, item ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("命令ID: ${item.id}", style = MaterialTheme.typography.bodyMedium)
                                                Text("代码: ${item.did.take(20)}${if (item.did.length > 20) "..." else ""}", style = MaterialTheme.typography.bodySmall)
                                            }
                                            IconButton(onClick = {
                                                editingQuickCommand = item
                                                quickCommandIndex = index
                                                showQuickCommandDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, null)
                                            }
                                            IconButton(onClick = {
                                                data = data.copy(quickCommands = data.quickCommands.toMutableList().apply { removeAt(index) })
                                                manager.save(data)
                                            }) {
                                                Icon(Icons.Default.Delete, null)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 底部添加按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FloatingActionButton(
                        onClick = {
                            when (selectedTab) {
                                0 -> {
                                    editingAutoReply = null
                                    autoReplyIndex = -1
                                    showAutoReplyDialog = true
                                }
                                1 -> {
                                    editingQuickCommand = null
                                    quickCommandIndex = -1
                                    showQuickCommandDialog = true
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )

    // 自动回复编辑对话框
    if (showAutoReplyDialog) {
        AutoReplyDialog(
            initial = editingAutoReply,
            onDismiss = { showAutoReplyDialog = false },
            onConfirm = { newItem ->
                val newList = data.autoReplies.toMutableList()
                if (autoReplyIndex >= 0) {
                    newList[autoReplyIndex] = newItem
                } else {
                    newList.add(newItem)
                }
                data = data.copy(autoReplies = newList)
                manager.save(data)
                showAutoReplyDialog = false
            }
        )
    }

    // 快捷命令编辑对话框
    if (showQuickCommandDialog) {
        QuickCommandDialog(
            initial = editingQuickCommand,
            onDismiss = { showQuickCommandDialog = false },
            onConfirm = { newItem ->
                val newList = data.quickCommands.toMutableList()
                if (quickCommandIndex >= 0) {
                    newList[quickCommandIndex] = newItem
                } else {
                    newList.add(newItem)
                }
                data = data.copy(quickCommands = newList)
                manager.save(data)
                showQuickCommandDialog = false
            }
        )
    }
}

@Composable
fun AutoReplyDialog(
    initial: AutoReply?,
    onDismiss: () -> Unit,
    onConfirm: (AutoReply) -> Unit
) {
    var key by remember { mutableStateOf(initial?.key ?: "") }
    var reply by remember { mutableStateOf(initial?.reply ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "text") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加自动回复" else "编辑自动回复") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("关键词") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text("回复内容") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("消息类型: ")
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = type == "text",
                        onClick = { type = "text" },
                        label = { Text("text") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = type == "markdown",
                        onClick = { type = "markdown" },
                        label = { Text("markdown") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = type == "html",
                        onClick = { type = "html" },
                        label = { Text("html") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (key.isBlank() || reply.isBlank()) {
                        toast(context, "请填写完整")
                        return@TextButton
                    }
                    onConfirm(AutoReply(key, reply, type))
                }
            ) {
                Text("确定")
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
fun QuickCommandDialog(
    initial: QuickCommand?,
    onDismiss: () -> Unit,
    onConfirm: (QuickCommand) -> Unit
) {
    var id by remember { mutableStateOf(initial?.id?.toString() ?: "") }
    var did by remember { mutableStateOf(initial?.did ?: "") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加快捷命令" else "编辑快捷命令") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("命令ID (数字)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = did,
                    onValueChange = { did = it },
                    label = { Text("执行代码") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val idInt = id.toIntOrNull()
                    if (idInt == null) {
                        toast(context, "命令ID必须为数字")
                        return@TextButton
                    }
                    if (did.isBlank()) {
                        toast(context, "代码不能为空")
                        return@TextButton
                    }
                    onConfirm(QuickCommand(idInt, did))
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}