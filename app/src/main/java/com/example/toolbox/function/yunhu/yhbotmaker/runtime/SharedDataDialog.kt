package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedDataDialog(
    botIndex: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var dataMap by remember { mutableStateOf<MutableMap<String, String>>(mutableMapOf()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<String?>(null) }
    var editingValue by remember { mutableStateOf("") }
    
    fun refreshData() {
        val all = BotSharedData.getAll()
        val newMap = mutableMapOf<String, String>()
        all.forEach { (k, v) ->
            newMap[k] = v.toString()
        }
        dataMap = newMap
    }
    
    LaunchedEffect(Unit) {
        refreshData()
    }
    
    // 添加/编辑对话框
    if (showAddDialog || editingKey != null) {
        var key by remember { mutableStateOf(editingKey ?: "") }
        var value by remember { mutableStateOf(editingValue) }
        val isEdit = editingKey != null
        
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingKey = null
            },
            title = { Text(if (isEdit) "编辑数据" else "添加数据") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("键名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isEdit
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("值") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (key.isBlank()) {
                            toast(context, "键名不能为空")
                            return@TextButton
                        }
                        BotSharedData.set(key, value)
                        refreshData()
                        showAddDialog = false
                        editingKey = null
                        toast(context, if (isEdit) "修改成功" else "添加成功")
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        editingKey = null
                    }
                ) {
                    Text("取消")
                }
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SharedData 管理",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "添加")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "存储的数据重启后保留，每个机器人独立",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (dataMap.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无数据，点击 + 添加")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(dataMap.entries.toList()) { entry ->
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.key,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = entry.value,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingKey = entry.key
                                            editingValue = entry.value
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, "编辑")
                                    }
                                    IconButton(
                                        onClick = {
                                            BotSharedData.remove(entry.key)
                                            refreshData()
                                            toast(context, "已删除")
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, "删除")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        BotSharedData.clear()
                        refreshData()
                        toast(context, "已清空所有数据")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清空全部")
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}