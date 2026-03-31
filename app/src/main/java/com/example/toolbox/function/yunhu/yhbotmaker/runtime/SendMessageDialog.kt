package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.toolbox.function.yunhu.yhbotmaker.toast

@Composable
fun SendMessageDialog(
    onDismiss: () -> Unit,
    onSend: (content: String, contentType: String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var contentType by remember { mutableStateOf("text") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发送消息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("消息内容") },
                    singleLine = false,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                // 消息类型选择
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("消息类型：", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = contentType == "text",
                        onClick = { contentType = "text" },
                        label = { Text("text") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = contentType == "markdown",
                        onClick = { contentType = "markdown" },
                        label = { Text("markdown") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = contentType == "html",
                        onClick = { contentType = "html" },
                        label = { Text("html") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (content.isBlank()) {
                        toast(context, "内容不能为空")
                        return@TextButton
                    }
                    onSend(content, contentType)
                    onDismiss()
                }
            ) {
                Text("发送")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}