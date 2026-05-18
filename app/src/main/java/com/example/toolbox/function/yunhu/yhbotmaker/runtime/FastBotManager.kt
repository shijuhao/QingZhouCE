@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.toolbox.function.yunhu.yhbotmaker.toast
import androidx.core.content.edit
import com.example.toolbox.AppJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.example.toolbox.settings.SettingsGroup
import com.example.toolbox.settings.SettingsItemCell

@Serializable
data class AutoReply(
    val key: String,
    val reply: String,
    val type: String  // text, markdown, html
)

@Serializable
data class QuickCommand(
    val id: Int,
    val code: String  // Lua 代码片段
)

@Serializable
data class CommandData(
    @SerialName("autoReplies")
    val autoReplies: List<AutoReply> = emptyList(),
    @SerialName("quickCommands")
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
fun FastBotDialog(
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
    
    var selectedType by remember { mutableStateOf<String?>(null) }
    
    if (showAutoReplyDialog) {
        AutoReplyDialog(
            initial = editingAutoReply,
            onDismiss = { 
                showAutoReplyDialog = false
                editingAutoReply = null
                autoReplyIndex = -1
            },
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
                editingAutoReply = null
                autoReplyIndex = -1
            }
        )
    }
    
    if (showQuickCommandDialog) {
        QuickCommandDialog(
            initial = editingQuickCommand,
            onDismiss = { 
                showQuickCommandDialog = false
                editingQuickCommand = null
                quickCommandIndex = -1
            },
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
                editingQuickCommand = null
                quickCommandIndex = -1
            }
        )
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "FastBot",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    SettingsGroup(
                        items = listOf(
                            {
                                SettingsItemCell(
                                    icon = Icons.Default.Chat,
                                    title = "自动回复",
                                    subtitle = "收到关键词自动回复消息",
                                    onClick = { 
                                        selectedType = "autoReply"
                                    }
                                )
                            },
                            {
                                SettingsItemCell(
                                    icon = Icons.Default.Code,
                                    title = "快捷命令",
                                    subtitle = "收到机器人指令自动执行执行Lua代码",
                                    onClick = { 
                                        selectedType = "quickCommand"
                                    }
                                )
                            }
                        )
                    )
                }
            }
        }
    }
    
    if (selectedType == "autoReply") {
        AutoReplyManageDialog(
            data = data.autoReplies,
            onDismiss = { selectedType = null },
            onAdd = {
                editingAutoReply = null
                autoReplyIndex = -1
                showAutoReplyDialog = true
            },
            onEdit = { index, item ->
                editingAutoReply = item
                autoReplyIndex = index
                showAutoReplyDialog = true
            },
            onDelete = { index ->
                data = data.copy(autoReplies = data.autoReplies.toMutableList().apply { removeAt(index) })
                manager.save(data)
            }
        )
    }
    
    if (selectedType == "quickCommand") {
        QuickCommandManageDialog(
            data = data.quickCommands,
            onDismiss = { selectedType = null },
            onAdd = {
                editingQuickCommand = null
                quickCommandIndex = -1
                showQuickCommandDialog = true
            },
            onEdit = { index, item ->
                editingQuickCommand = item
                quickCommandIndex = index
                showQuickCommandDialog = true
            },
            onDelete = { index ->
                data = data.copy(quickCommands = data.quickCommands.toMutableList().apply { removeAt(index) })
                manager.save(data)
            }
        )
    }
}

@Composable
fun AutoReplyCard(
    item: AutoReply,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null)
            }
        }
    }
}

@Composable
fun QuickCommandCard(
    item: QuickCommand,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                Text("代码: ${item.code.take(30)}${if (item.code.length > 30) "..." else ""}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null)
            }
        }
    }
}

@Composable
fun AutoReplyManageDialog(
    data: List<AutoReply>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Int, AutoReply) -> Unit,
    onDelete: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自动回复管理") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (data.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无自动回复")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(data) { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("关键词: ${item.key}", style = MaterialTheme.typography.bodyMedium)
                                        Text("回复: ${item.reply} (${item.type})", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { onEdit(index, item) }) {
                                        Icon(Icons.Default.Edit, null)
                                    }
                                    IconButton(onClick = { onDelete(index) }) {
                                        Icon(Icons.Default.Delete, null)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FloatingActionButton(
                        onClick = onAdd,
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, "添加")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("返回")
            }
        }
    )
}

@Composable
fun QuickCommandManageDialog(
    data: List<QuickCommand>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Int, QuickCommand) -> Unit,
    onDelete: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快捷命令管理") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (data.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无快捷命令")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(data) { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("命令ID: ${item.id}", style = MaterialTheme.typography.bodyMedium)
                                        Text("代码: ${item.code.take(30)}${if (item.code.length > 30) "..." else ""}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { onEdit(index, item) }) {
                                        Icon(Icons.Default.Edit, null)
                                    }
                                    IconButton(onClick = { onDelete(index) }) {
                                        Icon(Icons.Default.Delete, null)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FloatingActionButton(
                        onClick = onAdd,
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, "添加")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("返回")
            }
        }
    )
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
    var code by remember { mutableStateOf(initial?.code ?: "") }
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
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("执行代码 (Lua)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                )
                Text(
                    text = "提示：代码中可以使用 event 变量获取事件数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    if (code.isBlank()) {
                        toast(context, "代码不能为空")
                        return@TextButton
                    }
                    onConfirm(QuickCommand(idInt, code))
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