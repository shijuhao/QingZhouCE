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

class TextDiffActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TextDiffScreen(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextDiffScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var text1 by remember { mutableStateOf("") }
    var text2 by remember { mutableStateOf("") }
    var diffResult by remember { mutableStateOf("") }

    fun compareText() {
        if (text1.isEmpty() && text2.isEmpty()) {
            diffResult = ""
            return
        }

        val lines1 = text1.lines()
        val lines2 = text2.lines()
        
        diffResult = buildString {
            appendLine("=== 文本对比结果 ===\n")
            
            if (text1 == text2) {
                appendLine("✓ 两段文本完全相同")
                return@buildString
            }
            
            appendLine("文本1行数: ${lines1.size}")
            appendLine("文本2行数: ${lines2.size}")
            appendLine("文本1字符数: ${text1.length}")
            appendLine("文本2字符数: ${text2.length}\n")
            
            val maxLines = maxOf(lines1.size, lines2.size)
            var diffCount = 0
            
            for (i in 0 until maxLines) {
                val line1 = if (i < lines1.size) lines1[i] else "<无>"
                val line2 = if (i < lines2.size) lines2[i] else "<无>"
                
                if (line1 != line2) {
                    diffCount++
                    appendLine("第 ${i + 1} 行不同:")
                    appendLine("  文本1: $line1")
                    appendLine("  文本2: $line2\n")
                }
            }
            
            if (diffCount == 0 && text1.length != text2.length) {
                appendLine("✓ 内容相同但格式可能不同（空格、换行等）")
            } else if (diffCount > 0) {
                appendLine("\n共发现 $diffCount 处差异")
            }
        }
    }

    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TopAppBar(
            title = { Text("文本对比") },
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
                    Text("文本1", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = text1,
                        onValueChange = { text1 = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入第一段文本") },
                        maxLines = 5
                    )
                    
                    Text("文本2", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = text2,
                        onValueChange = { text2 = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入第二段文本") },
                        maxLines = 5
                    )
                    
                    Button(onClick = { compareText() }, modifier = Modifier.fillMaxWidth()) {
                        Text("对比差异")
                    }
                }
            }

            if (diffResult.isNotEmpty()) {
                Card(elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("对比结果", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text(
                                text = diffResult,
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
