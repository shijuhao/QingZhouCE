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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
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
import androidx.compose.runtime.getValue
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

class HTMLEntityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HTMLEntityScreen(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HTMLEntityScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val htmlEntities = mapOf(
        "&lt;" to "<",
        "&gt;" to ">",
        "&amp;" to "&",
        "&quot;" to "\"",
        "&#39;" to "'",
        "&nbsp;" to " ",
        "&copy;" to "©",
        "&reg;" to "®",
        "&trade;" to "™",
        "&euro;" to "€",
        "&pound;" to "£",
        "&yen;" to "¥",
        "&cent;" to "¢"
    )

    fun encodeHTML() {
        errorMessage = ""
        if (inputText.isEmpty()) {
            outputText = ""
            return
        }

        try {
            var result = inputText
            result = result.replace("&", "&amp;")
            result = result.replace("<", "&lt;")
            result = result.replace(">", "&gt;")
            result = result.replace("\"", "&quot;")
            result = result.replace("'", "&#39;")
            result = result.replace(" ", "&nbsp;")
            
            outputText = result
        } catch (e: Exception) {
            errorMessage = "编码失败: ${e.message}"
        }
    }

    fun decodeHTML() {
        errorMessage = ""
        if (inputText.isEmpty()) {
            outputText = ""
            return
        }

        try {
            var result = inputText
            htmlEntities.forEach { (entity, char) ->
                result = result.replace(entity, char)
            }
            outputText = result
        } catch (e: Exception) {
            errorMessage = "解码失败: ${e.message}"
        }
    }

    fun copyToClipboard() {
        if (outputText.isEmpty()) {
            Toast.makeText(context, "没有可复制的内容", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("HTML实体", outputText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("HTML实体编解码") },
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "输入文本",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("请输入文本或HTML实体") },
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
                            onClick = { encodeHTML() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.RotateLeft,
                                    null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text("编码")
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = { decodeHTML() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.RotateRight,
                                    null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text("解码")
                            }
                        }
                    }
                }
            }

            if (outputText.isNotEmpty()) {
                Card(elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "结果",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = outputText,
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

            Card(
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "常用HTML实体",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = htmlEntities.entries.joinToString("\n") { (entity, char) ->
                            "$char → $entity"
                        },
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
