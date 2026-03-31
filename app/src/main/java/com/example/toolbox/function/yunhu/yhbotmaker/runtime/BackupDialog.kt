package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.toolbox.AppJson
import com.example.toolbox.function.yunhu.yhbotmaker.toast
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.FileWriter

@Composable
fun BackupDialog(
    botIndex: Int,
    botName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("bot_prefs", Context.MODE_PRIVATE)

    // 加载所有相关数据
    val startupCode = prefs.getString("code-start$botIndex", "") ?: ""
    val loopCode = prefs.getString("code$botIndex", "") ?: ""
    val quickCommands = prefs.getString("chelper$botIndex", "{}") ?: "{}"
    val requestInterval = prefs.getString("shilv$botIndex", "2000") ?: "2000"

    val backupData = buildJsonObject {
        put("botName", botName)
        put("startupCode", startupCode)
        put("loopCode", loopCode)
        put("quickCommands", quickCommands)
        put("requestInterval", requestInterval)
        put("backupTime", System.currentTimeMillis())
    }

    val backupJson = remember { AppJson.json.encodeToString(backupData) }

    fun shareBackup() {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, backupJson)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "分享备份"))
        } catch (e: Exception) {
            toast(context, "分享失败: ${e.message}")
        }
    }

    fun copyToClipboard() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("机器人备份", backupJson)
            clipboard.setPrimaryClip(clip)
            toast(context, "已复制到剪贴板")
        } catch (e: Exception) {
            toast(context, "复制失败: ${e.message}")
        }
    }

    fun saveToFile() {
        try {
            val fileName = "bot_backup_${botName}_${System.currentTimeMillis()}.json"
            val file = File(context.getExternalFilesDir(null), fileName)
            FileWriter(file).use { writer ->
                writer.write(backupJson)
            }
            toast(context, "已保存到: ${file.absolutePath}")

            // 提示文件位置
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/json")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "打开文件"))
        } catch (e: Exception) {
            toast(context, "保存失败: ${e.message}")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("备份代码 - $botName") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // 备份内容预览
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        item {
                            Text(
                                text = "启动代码: ${startupCode.take(50)}${if (startupCode.length > 50) "..." else ""}",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        item {
                            Text(
                                text = "循环代码: ${loopCode.take(50)}${if (loopCode.length > 50) "..." else ""}",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        item {
                            Text(
                                text = "请求间隔: $requestInterval ms",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // 操作按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { copyToClipboard() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("复制")
                    }
                    Button(
                        onClick = { shareBackup() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("分享")
                    }
                }

                OutlinedButton(
                    onClick = { saveToFile() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("保存到文件")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}