package com.example.toolbox.function.system

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.toolbox.ui.theme.ToolBoxTheme
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import androidx.core.content.edit

class ShellRunnerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("Shell运行器") },
                            navigationIcon = {
                                FilledTonalIconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    ShellRunnerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

object ShellHistoryManager {
    private const val PREF_NAME = "shell_history"
    private const val HISTORY_KEY = "command_history"

    fun getHistory(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(HISTORY_KEY, emptySet())?.toList()?.reversed() ?: emptyList()
    }

    fun addHistory(context: Context, command: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val history = prefs.getStringSet(HISTORY_KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        
        // 如果已存在，先移除
        history.remove(command)
        // 添加到末尾（最新的在最后）
        history.add(command)
        
        // 限制最多20条
        if (history.size > 20) {
            val sortedHistory = history.toList().sortedByDescending { 
                history.indexOf(it) 
            }.take(20).toMutableSet()
            prefs.edit { putStringSet(HISTORY_KEY, sortedHistory)}
        } else {
            prefs.edit { putStringSet(HISTORY_KEY, history) }
        }
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit {remove(HISTORY_KEY)}
    }
}

@Composable
fun ShellRunnerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var command by remember { mutableStateOf("") }
    var output by remember { mutableStateOf<List<String>>(emptyList()) }
    var useRoot by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var commandHistory by remember { mutableStateOf(ShellHistoryManager.getHistory(context)) }
    var showHistory by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "使用Root权限",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Switch(
                        checked = useRoot,
                        onCheckedChange = { useRoot = it },
                        thumbContent = {
                            Icon(
                                imageVector = if (useRoot) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                tint = if (useRoot) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            )
                        }
                    )
                }
            }
        }

        // 命令输入框
        item {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                label = { Text("输入Shell命令") },
                placeholder = { Text("例如: ls -la /sdcard/") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                trailingIcon = {
                    if (command.isNotEmpty()) {
                        IconButton(onClick = { command = "" }) {
                            Icon(Icons.Default.Clear, "清除")
                        }
                    }
                }
            )
        }

        // 执行按钮和历史记录按钮
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (command.isNotBlank()) {
                            isRunning = true
                            // 添加到历史记录（持久化）
                            ShellHistoryManager.addHistory(context, command)
                            // 更新UI状态
                            commandHistory = ShellHistoryManager.getHistory(context)
                            executeCommand(command, useRoot) { result ->
                                output = result
                                isRunning = false
                            }
                        } else {
                            Toast.makeText(context, "请输入命令", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRunning) "执行中..." else "执行命令")
                }

                OutlinedButton(
                    onClick = { showHistory = !showHistory },
                    enabled = !isRunning && commandHistory.isNotEmpty()
                ) {
                    Text("历史")
                }
            }
        }

        item {
            AnimatedVisibility(visible = showHistory && commandHistory.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "命令历史",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            commandHistory.forEach { historyCommand ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = historyCommand,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = {
                                            command = historyCommand
                                            showHistory = false
                                        }
                                    ) {
                                        Text("使用")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "输出结果:",
                style = MaterialTheme.typography.titleMedium
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(12.dp)
            ) {
                if (output.isEmpty()) {
                    Text(
                        text = "等待执行命令...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    SelectionContainer {
                        Column {
                            output.forEach { line ->
                                Text(
                                    text = line,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun executeCommand(command: String, useRoot: Boolean, onResult: (List<String>) -> Unit) {
    Thread {
        try {
            val process = if (useRoot) {
                Runtime.getRuntime().exec("su")
            } else {
                Runtime.getRuntime().exec("sh")
            }

            val outputStream = DataOutputStream(process.outputStream)
            val inputStream = BufferedReader(InputStreamReader(process.inputStream))
            val errorStream = BufferedReader(InputStreamReader(process.errorStream))

            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val outputLines = mutableListOf<String>()
            var line: String?
            while (inputStream.readLine().also { line = it } != null) {
                outputLines.add(line!!)
            }

            val errorLines = mutableListOf<String>()
            while (errorStream.readLine().also { line = it } != null) {
                errorLines.add("ERROR: $line")
            }

            val allOutput = outputLines + errorLines

            process.waitFor()

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult(allOutput)
            }

        } catch (e: Exception) {
            val errorMessage = e.message ?: ""
            val resultMessage = if (errorMessage.contains("su") && errorMessage.contains("No such file or directory")) {
                listOf("错误: 请检查你的root权限是否正常")
            } else {
                listOf("执行失败: ${e.message}")
            }
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult(resultMessage)
            }
        }
    }.start()
}
