package com.example.toolbox.function.text

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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

class Rc4Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Rc4CryptoScreen(
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }
            }
        }
    }
}

object MiniCrypto {
    private fun initializeState(key: ByteArray): MutableList<Int> {
        val s = MutableList(256) { it }
        var j = 0
        for (i in 0 until 256) {
            j = (j + s[i] + key[i % key.size].toInt() and 0xFF) % 256
            s[i] = s[j].also { s[j] = s[i] }
        }
        return s
    }

    private fun encryptOne(state: MutableList<Int>, byte: Int): Int {
        var i = state[256]
        var j = state[257]
        i = (i + 1) % 256
        j = (j + state[i]) % 256
        state[i] = state[j].also { state[j] = state[i] }
        val k = state[(state[i] + state[j]) % 256]
        state[256] = i
        state[257] = j
        return k xor byte
    }

    fun encrypt(text: String, key: String): String {
        require(key.isNotEmpty()) { "密钥不能为空" }
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        val state = initializeState(keyBytes).apply { addAll(listOf(0, 0)) }.toMutableList()
        return text.toByteArray(Charsets.UTF_8).joinToString("") { byte ->
            "%02X".format(encryptOne(state, byte.toInt() and 0xFF))
        }
    }

    fun decrypt(hexText: String, key: String): String {
        require(key.isNotEmpty()) { "密钥不能为空" }
        require(hexText.length % 2 == 0) { "密文长度必须为偶数" }
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        val state = initializeState(keyBytes).apply { addAll(listOf(0, 0)) }.toMutableList()
        val bytes = hexText.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val decrypted = bytes.map { encryptOne(state, it.toInt() and 0xFF).toByte() }.toByteArray()
        return String(decrypted, Charsets.UTF_8)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Rc4CryptoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }
    var keyText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("加密/解密后的文本显示在此处") }

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("RC4结果", resultText)
        clipboard.setPrimaryClip(clip)
    }

    fun encryptText() {
        if (keyText.isEmpty()) {
            resultText = "请输入密钥"
            return
        }
        resultText = try {
            MiniCrypto.encrypt(inputText, keyText)
        } catch (e: Exception) {
            "加密失败: ${e.message}"
        }
    }

    fun decryptText() {
        if (keyText.isEmpty()) {
            resultText = "请输入密钥"
            return
        }
        try {
            if (inputText.length % 2 != 0 || !inputText.matches(Regex("[0-9A-Fa-f]+"))) {
                resultText = "请输入有效的十六进制密文"
                return
            }
            resultText = MiniCrypto.decrypt(inputText, keyText)
        } catch (e: Exception) {
            resultText = "解密失败: ${e.message}"
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("RC4加解密") },
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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "输入内容",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("请输入文本或十六进制密文") },
                        singleLine = false,
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = keyText,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "密钥"
                            )
                        },
                        onValueChange = { keyText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入密钥") },
                        singleLine = true
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { decryptText() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.LockOpen,
                                    contentDescription = "解密",
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text("解密")
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = { encryptText() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = "加密",
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text("加密")
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
                        text = "结果",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = resultText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        )
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { copyToClipboard() }
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

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}