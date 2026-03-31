package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.toolbox.function.yunhu.yhbotmaker.toast
import androidx.core.content.edit
import com.example.toolbox.AppJson
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ImportDialog(
    botIndex: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var importType by remember { mutableIntStateOf(0) } // 0: 快捷指令, 1: 数据
    var showPreview by remember { mutableStateOf(false) }
    var previewData by remember { mutableStateOf("") }

    val prefs = context.getSharedPreferences("bot_prefs", Context.MODE_PRIVATE)

    fun validateAndPreview() {
        try {
            when (importType) {
                0 -> { // 快捷指令
                    val json = AppJson.json.parseToJsonElement(importText).jsonObject
                    if (json.containsKey("autoReplies") || json.containsKey("quickCommands") ||
                        json.containsKey("自动回复") || json.containsKey("快捷命令")) {
                        previewData = "✅ 快捷指令配置有效"
                        showPreview = true
                    } else {
                        toast(context, "不是有效的快捷指令格式")
                    }
                }
                1 -> { // 数据
                    val json = AppJson.json.parseToJsonElement(importText).jsonObject
                    previewData = "✅ 数据格式有效\n包含字段: ${json.keys}"
                    showPreview = true
                }
            }
        } catch (e: Exception) {
            toast(context, "JSON 格式错误: ${e.message}")
        }
    }

    fun doImport() {
        try {
            when (importType) {
                0 -> { // 导入快捷指令
                    prefs.edit { putString("chelper$botIndex", importText) }
                    toast(context, "快捷指令导入成功")
                }
                1 -> { // 导入数据
                    val json = try {
                        AppJson.json.parseToJsonElement(importText).jsonObject
                    } catch (_: Exception) {
                        toast(context, "JSON 格式错误")
                        return
                    }

                    json.forEach { (key, value) ->
                        val stringValue = value.jsonPrimitive.contentOrNull
                        if (stringValue != null) {
                            prefs.edit { putString(key, stringValue) }
                        }
                    }
                    toast(context, "数据导入成功")
                }
            }
            onDismiss()
        } catch (e: Exception) {
            toast(context, "导入失败: ${e.message}")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (importType == 0) "导入快捷指令" else "导入数据") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 类型选择
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = importType == 0,
                        onClick = { importType = 0; showPreview = false },
                        label = { Text("快捷指令") }
                    )
                    FilterChip(
                        selected = importType == 1,
                        onClick = { importType = 1; showPreview = false },
                        label = { Text("数据") }
                    )
                }

                // 输入框
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it; showPreview = false },
                    label = { Text("粘贴 JSON 内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    maxLines = 10
                )

                // 预览按钮
                Button(
                    onClick = { validateAndPreview() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = importText.isNotBlank()
                ) {
                    Text("预览")
                }

                // 预览结果
                if (showPreview) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = previewData,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { doImport() },
                enabled = importText.isNotBlank() && showPreview
            ) {
                Text("导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}