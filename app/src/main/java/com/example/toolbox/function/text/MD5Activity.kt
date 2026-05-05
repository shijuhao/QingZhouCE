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
import java.security.MessageDigest

class MD5Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MD5Screen(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MD5Screen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }
    var md5Result by remember { mutableStateOf("") }
    var md5Upper by remember { mutableStateOf("") }

    fun calculateMD5() {
        if (inputText.isEmpty()) {
            md5Result = ""
            md5Upper = ""
            return
        }

        try {
            val bytes = MessageDigest.getInstance("MD5").digest(inputText.toByteArray(Charsets.UTF_8))
            val hexString = StringBuilder()
            bytes.forEach {
                val hex = Integer.toHexString(0xff and it.toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            md5Result = hexString.toString()
            md5Upper = md5Result.uppercase()
        } catch (e: Exception) {
            md5Result = "计算失败: ${e.message}"
            md5Upper = ""
        }
    }

    fun copyToClipboard(text: String, label: String) {
        if (text.isEmpty()) {
            Toast.makeText(context, "没有可复制的内容", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "已复制$label", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("MD5哈希") },
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
                        onValueChange = { 
                            inputText = it
                            calculateMD5()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("请输入要计算MD5的文本") },
                        singleLine = false,
                        maxLines = 5
                    )
                }
            }

            if (md5Result.isNotEmpty()) {
                Card(elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "MD5结果（小写）",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = md5Result,
                                    modifier = Modifier.weight(1f),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { copyToClipboard(md5Result, "MD5小写") }) {
                                    Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                }
                            }
                        }

                        Text(
                            text = "MD5结果（大写）",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = md5Upper,
                                    modifier = Modifier.weight(1f),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { copyToClipboard(md5Upper, "MD5大写") }) {
                                    Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
