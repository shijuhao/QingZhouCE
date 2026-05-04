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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ui.theme.ToolBoxTheme

class UnicodeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UnicodeScreen(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnicodeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var displayMode by remember { mutableIntStateOf(0) }
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("编码/解码结果将显示在这里") }
    var errorMessage by remember { mutableStateOf("") }
    val structuredList = remember { mutableStateListOf<Pair<Char, String>>() }

    fun encodeToUnicode() {
        errorMessage = ""
        structuredList.clear()
        try {
            if (inputText.isNotEmpty()) {
                val chars = inputText.toCharArray()
                
                if (displayMode == 0) {
                    val encoded = chars.joinToString("") { char ->
                        "\\u%04x".format(char.code)
                    }
                    outputText = encoded
                } else {
                    chars.forEach { char ->
                        val unicode = "\\u%04x".format(char.code)
                        structuredList.add(Pair(char, unicode))
                    }
                    outputText = "结构化显示模式"
                }
            } else {
                errorMessage = "请输入要编码的文本"
            }
        } catch (e: Exception) {
            errorMessage = "编码失败: ${e.message}"
        }
    }

    fun decodeFromUnicode() {
        errorMessage = ""
        structuredList.clear()
        try {
            if (inputText.isNotEmpty()) {
                val regex = Regex("\\\\u([0-9a-fA-F]{4})")
                val matches = regex.findAll(inputText)
                
                if (displayMode == 1 && matches.any()) {
                    matches.forEach { matchResult ->
                        val hexValue = matchResult.groupValues[1]
                        val codePoint = hexValue.toInt(16)
                        val char = Char(codePoint)
                        structuredList.add(Pair(char, "\\u${hexValue.lowercase()}"))
                    }
                    outputText = "结构化显示模式"
                } else {
                    val decoded = regex.replace(inputText) { matchResult ->
                        val hexValue = matchResult.groupValues[1]
                        val codePoint = hexValue.toInt(16)
                        Char(codePoint).toString()
                    }
                    outputText = decoded
                }
            } else {
                errorMessage = "请输入要解码的Unicode文本"
            }
        } catch (_: Exception) {
            errorMessage = "解码失败: 请检查输入是否为有效的Unicode编码"
        }
    }

    fun copyToClipboard() {
        if (inputText.isEmpty() && outputText == "编码/解码结果将显示在这里") {
            Toast.makeText(context,"请输入文本",Toast.LENGTH_SHORT).show()
            return
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("文本", outputText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context,"已复制",Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("Unicode 编码/解码") },
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
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = displayMode == 0,
                            onClick = { displayMode = 0 },
                            label = { Text("紧凑模式") }
                        )
                        FilterChip(
                            selected = displayMode == 1,
                            onClick = { displayMode = 1 },
                            label = { Text("结构模式") }
                        )
                    }

                    Text(
                        text = "输入文本",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (displayMode == 0) "请输入文本或Unicode编码" else "请输入文本（结构模式）") },
                        singleLine = false,
                        maxLines = 5
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Card(
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Card(
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { encodeToUnicode() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.RotateLeft,
                                    contentDescription = "编码",
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text("编码")
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = { decodeFromUnicode() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.RotateRight,
                                    contentDescription = "解码",
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text("解码")
                            }
                        }
                    }
                }
            }

            Card(
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "输出结果",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (structuredList.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(300.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(structuredList) { (char, unicode) ->
                                Card(
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
                                        Text(
                                            text = "'$char'",
                                            style = TextStyle(
                                                fontSize = 18.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "U+${unicode.substring(2).uppercase()}",
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = unicode,
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 14.sp
                                            ),
                                            modifier = Modifier.weight(2f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = outputText.ifEmpty { "编码/解码结果将显示在这里" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            onClick = { copyToClipboard() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = "复制",
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text("复制")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UnicodeScreenPreview() {
    ToolBoxTheme {
        UnicodeScreen()
    }
}