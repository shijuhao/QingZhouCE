package com.example.toolbox.function.text

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ui.theme.ToolBoxTheme
import java.text.SimpleDateFormat
import java.util.*

class TimestampActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TimestampScreen(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimestampScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var timestampInput by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf("") }
    var convertedResult by remember { mutableStateOf("") }
    var currentTimestamp by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTimestamp = System.currentTimeMillis() / 1000
        }
    }

    fun timestampToDate() {
        if (timestampInput.isEmpty()) {
            convertedResult = ""
            return
        }

        try {
            val ts = timestampInput.toLongOrNull() ?: run {
                convertedResult = "请输入有效的时间戳"
                return
            }
            
            val isSeconds = ts.toString().length <= 10
            val millis = if (isSeconds) ts * 1000 else ts
            
            val date = Date(millis)
            val formatted = dateFormat.format(date)
            
            convertedResult = buildString {
                appendLine("日期时间: $formatted")
                appendLine("时区: ${TimeZone.getDefault().displayName}")
                if (isSeconds) {
                    appendLine("毫秒时间戳: $millis")
                } else {
                    appendLine("秒时间戳: ${millis / 1000}")
                }
            }
        } catch (e: Exception) {
            convertedResult = "转换失败: ${e.message}"
        }
    }

    fun dateToTimestamp() {
        if (dateInput.isEmpty()) {
            convertedResult = ""
            return
        }

        try {
            val formats = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy-MM-dd",
                "yyyy/MM/dd"
            )
            
            var date: Date? = null
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    date = sdf.parse(dateInput)
                    if (date != null) break
                } catch (_: Exception) {
                }
            }
            
            if (date == null) {
                convertedResult = "无法解析日期，请使用格式：yyyy-MM-dd HH:mm:ss"
                return
            }
            
            val seconds = date.time / 1000
            val millis = date.time
            
            convertedResult = buildString {
                appendLine("秒时间戳: $seconds")
                appendLine("毫秒时间戳: $millis")
            }
        } catch (e: Exception) {
            convertedResult = "转换失败: ${e.message}"
        }
    }

    fun copyToClipboard() {
        if (convertedResult.isEmpty()) {
            Toast.makeText(context, "没有可复制的内容", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("时间戳结果", convertedResult)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("时间戳转换") },
            navigationIcon = {
                FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "当前时间戳",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "$currentTimestamp",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dateFormat.format(Date()),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            if (convertedResult.isNotEmpty()) {
                Card(elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "转换结果",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = convertedResult,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { copyToClipboard() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text("复制结果")
                        }
                    }
                }
            }

            Card(elevation = CardDefaults.cardElevation(0.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "时间戳 → 日期",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = timestampInput,
                        onValueChange = { timestampInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入时间戳（秒或毫秒）") },
                        singleLine = true
                    )
                    Button(
                        onClick = { timestampToDate() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("转换为日期")
                    }
                }
            }

            Card(elevation = CardDefaults.cardElevation(0.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "日期 → 时间戳",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("例如: 2024-01-01 12:00:00") },
                        singleLine = true
                    )
                    Button(
                        onClick = { dateToTimestamp() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("转换为时间戳")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
