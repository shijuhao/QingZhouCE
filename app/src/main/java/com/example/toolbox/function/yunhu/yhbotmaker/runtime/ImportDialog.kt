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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun ImportDialog(
    botIndex: Int,
    botName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var importMode by remember { mutableIntStateOf(0) } // 0=全部, 1=仅快捷指令
    var showPreview by remember { mutableStateOf(false) }
    var previewData by remember { mutableStateOf("") }

    val prefs = context.getSharedPreferences("bot_prefs", Context.MODE_PRIVATE)

    fun validateAndPreview() {
        try {
            val json = AppJson.json.parseToJsonElement(importText).jsonObject
            
            when (importMode) {
                0 -> {
                    val items = mutableListOf<String>()
                    if (json.containsKey("startupCode")) items.add("启动代码")
                    if (json.containsKey("loopCode")) items.add("循环代码")
                    if (json.containsKey("quickCommands")) items.add("快捷指令")
                    if (items.isEmpty()) {
                        toast(context, "不是有效的备份文件")
                        return
                    }
                    previewData = "✅ 将导入: ${items.joinToString(", ")}"
                }
                1 -> {
                    if (!json.containsKey("quickCommands")) {
                        toast(context, "备份文件中没有快捷指令")
                        return
                    }
                    previewData = "✅ 将导入: 快捷指令"
                }
            }
            showPreview = true
        } catch (e: Exception) {
            toast(context, "JSON 格式错误: ${e.message}")
        }
    }

    fun doImport() {
        try {
            val json = AppJson.json.parseToJsonElement(importText).jsonObject
            
            when (importMode) {
                0 -> {
                    json["startupCode"]?.jsonPrimitive?.contentOrNull?.let {
                        prefs.edit { putString("code-start$botIndex", it) }
                    }
                    json["loopCode"]?.jsonPrimitive?.contentOrNull?.let {
                        prefs.edit { putString("code$botIndex", it) }
                        BotRuntimeViewModel.getInstance(botIndex).setCurrentLoopCode(it)
                    }
                    json["quickCommands"]?.jsonPrimitive?.contentOrNull?.let {
                        prefs.edit { putString("chelper$botIndex", it) }
                    }
                    toast(context, "导入成功")
                }
                1 -> {
                    json["quickCommands"]?.jsonPrimitive?.contentOrNull?.let {
                        prefs.edit { putString("chelper$botIndex", it) }
                        toast(context, "快捷指令导入成功")
                    } ?: toast(context, "未找到快捷指令")
                }
            }
            onDismiss()
        } catch (e: Exception) {
            toast(context, "导入失败: ${e.message}")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入配置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 导入选项
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = importMode == 0,
                        onClick = { importMode = 0; showPreview = false },
                        label = { Text("全部") }
                    )
                    FilterChip(
                        selected = importMode == 1,
                        onClick = { importMode = 1; showPreview = false },
                        label = { Text("仅导入FastBot") }
                    )
                }
                
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it; showPreview = false },
                    label = { Text("粘贴备份 JSON") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    maxLines = 10
                )
                
                Button(
                    onClick = { validateAndPreview() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = importText.isNotBlank()
                ) {
                    Text("预览")
                }
                
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
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}