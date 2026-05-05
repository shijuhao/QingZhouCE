package com.example.toolbox.function.text

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ui.theme.ToolBoxTheme

class RegexTesterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RegexTesterScreen(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegexTesterScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var pattern by remember { mutableStateOf("") }
    var testText by remember { mutableStateOf("") }
    var matchResult by remember { mutableStateOf("") }

    fun testRegex() {
        if (pattern.isEmpty() || testText.isEmpty()) {
            matchResult = "请输入正则表达式和测试文本"
            return
        }
        try {
            val regex = Regex(pattern)
            val matches = regex.findAll(testText).toList()
            matchResult = if (matches.isEmpty()) {
                "未找到匹配"
            } else {
                buildString {
                    appendLine("找到 ${matches.size} 个匹配:\n")
                    matches.forEachIndexed { index, match ->
                        appendLine("${index + 1}. \"${match.value}\"")
                        appendLine("   位置: ${match.range.first}-${match.range.last}")
                        if (match.groups.size > 1) {
                            append("   分组: ")
                            match.groups.drop(1).forEachIndexed { i, group ->
                                if (group != null) append("[${i+1}]=${group.value} ")
                            }
                            appendLine()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            matchResult = "正则表达式错误: ${e.message}"
        }
    }

    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TopAppBar(
            title = { Text("正则测试") },
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
            Card(elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text("正则表达式") },
                        placeholder = { Text("例如: \\d+") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        label = { Text("测试文本") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                    Button(onClick = { testRegex() }, modifier = Modifier.fillMaxWidth()) {
                        Text("测试匹配")
                    }
                }
            }

            if (matchResult.isNotEmpty()) {
                Card(elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("匹配结果", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text(
                                text = matchResult,
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
