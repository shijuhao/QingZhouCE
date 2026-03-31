package com.example.toolbox.function.text

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Base64
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ui.theme.ToolBoxTheme
import java.security.MessageDigest
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AESActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AesCryptoScreen(
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }
            }
        }
    }
}
// 更准确的 OpenSSL 兼容 AES 实现
object OpenSslCompatibleAes {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val SALT_PREFIX = "Salted__"
    private const val SALT_LENGTH = 8

    // OpenSSL 兼容的 EVP_BytesToKey 实现
    private fun evpBytesToKey(password: String, salt: ByteArray, keySize: Int, ivSize: Int): Pair<ByteArray, ByteArray> {
        var data = ByteArray(0)
        val keyAndIv = ByteArray(keySize + ivSize)
        var offset = 0

        while (offset < keyAndIv.size) {
            val md = MessageDigest.getInstance("MD5")
            if (data.isNotEmpty()) {
                md.update(data)
            }
            md.update(password.toByteArray(Charsets.UTF_8))
            md.update(salt)
            data = md.digest()

            val length = minOf(data.size, keyAndIv.size - offset)
            System.arraycopy(data, 0, keyAndIv, offset, length)
            offset += length
        }

        val key = keyAndIv.copyOfRange(0, keySize)
        val iv = keyAndIv.copyOfRange(keySize, keySize + ivSize)

        return Pair(key, iv)
    }

    // 加密
    fun encrypt(text: String, password: String): String {
        if (password.isEmpty()) throw IllegalArgumentException("密码不能为空")

        // 生成随机盐
        val salt = ByteArray(SALT_LENGTH)
        java.security.SecureRandom().nextBytes(salt)

        // 使用 EVP_BytesToKey 生成密钥和 IV (AES-256: 32字节密钥, 16字节IV)
        val (key, iv) = evpBytesToKey(password, salt, 32, 16)
        val secretKey = SecretKeySpec(key, ALGORITHM)
        val ivSpec: AlgorithmParameterSpec = IvParameterSpec(iv)

        // 加密
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))

        // 组合结果：Salted__ + salt + encryptedBytes
        val result = ByteArray(SALT_PREFIX.length + salt.size + encryptedBytes.size)
        System.arraycopy(SALT_PREFIX.toByteArray(), 0, result, 0, SALT_PREFIX.length)
        System.arraycopy(salt, 0, result, SALT_PREFIX.length, salt.size)
        System.arraycopy(encryptedBytes, 0, result, SALT_PREFIX.length + salt.size, encryptedBytes.size)

        return Base64.encodeToString(result, Base64.NO_WRAP)
    }

    // 解密 - 尝试多种可能的配置
    fun decrypt(encryptedText: String, password: String): String {
        if (password.isEmpty()) throw IllegalArgumentException("密码不能为空")

        try {
            // 清理输入，移除换行和空格
            val cleanInput = encryptedText.replace("\n", "").replace(" ", "")

            // Base64 解码
            val encryptedData = Base64.decode(cleanInput, Base64.NO_WRAP)

            // 检查是否是 OpenSSL 格式
            if (encryptedData.size < SALT_PREFIX.length + SALT_LENGTH) {
                throw IllegalArgumentException("无效的 OpenSSL 格式密文")
            }

            val prefix = String(encryptedData.copyOfRange(0, SALT_PREFIX.length), Charsets.UTF_8)
            if (prefix != SALT_PREFIX) {
                throw IllegalArgumentException("不是 OpenSSL 格式的密文")
            }

            // 提取盐和加密数据
            val salt = encryptedData.copyOfRange(SALT_PREFIX.length, SALT_PREFIX.length + SALT_LENGTH)
            val encryptedBytes = encryptedData.copyOfRange(SALT_PREFIX.length + SALT_LENGTH, encryptedData.size)

            // 尝试不同的密钥长度
            val keyLengths = listOf(32, 24, 16) // AES-256, AES-192, AES-128
            val ivLength = 16 // AES 总是使用 16 字节 IV

            for (keyLength in keyLengths) {
                try {
                    val (key, iv) = evpBytesToKey(password, salt, keyLength, ivLength)
                    val secretKey = SecretKeySpec(key, ALGORITHM)
                    val ivSpec: AlgorithmParameterSpec = IvParameterSpec(iv)

                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
                    val decryptedBytes = cipher.doFinal(encryptedBytes)

                    return String(decryptedBytes, Charsets.UTF_8)
                } catch (_: Exception) {
                    // 尝试下一个密钥长度
                    continue
                }
            }

            throw IllegalArgumentException("无法解密：请检查密钥是否正确")
        } catch (e: Exception) {
            throw IllegalArgumentException("解密失败: ${e.message}")
        }
    }

    // 简单 AES/ECB 加密（非 OpenSSL 兼容）
    object SimpleAes {
        private const val SIMPLE_TRANSFORMATION = "AES/ECB/PKCS5Padding"

        private fun generateKey(key: String): ByteArray {
            var keyBytes = key.toByteArray(Charsets.UTF_8)

            when {
                keyBytes.size < 16 -> keyBytes = keyBytes.copyOf(16)
                keyBytes.size < 24 -> keyBytes = keyBytes.copyOf(24)
                keyBytes.size < 32 -> keyBytes = keyBytes.copyOf(32)
                keyBytes.size > 32 -> keyBytes = keyBytes.copyOf(32)
            }

            return keyBytes
        }

        fun encrypt(text: String, key: String): String {
            if (key.isEmpty()) throw IllegalArgumentException("密钥不能为空")

            val secretKey = SecretKeySpec(generateKey(key), ALGORITHM)
            val cipher = Cipher.getInstance(SIMPLE_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        }

        fun decrypt(text: String, key: String): String {
            if (key.isEmpty()) throw IllegalArgumentException("密钥不能为空")

            val secretKey = SecretKeySpec(generateKey(key), ALGORITHM)
            val cipher = Cipher.getInstance(SIMPLE_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            val encryptedBytes = Base64.decode(text, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AesCryptoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }
    var keyText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("加密/解密后的文本显示在此处") }
    var useOpenSslFormat by remember { mutableStateOf(true) }

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AES结果", resultText)
        clipboard.setPrimaryClip(clip)
        // 可以添加 Toast 提示复制成功
    }

    fun encryptText() {
        if (keyText.isEmpty()) {
            resultText = "请输入密钥"
            return
        }
        resultText = try {
            if (useOpenSslFormat) {
                OpenSslCompatibleAes.encrypt(inputText, keyText)
            } else {
                OpenSslCompatibleAes.SimpleAes.encrypt(inputText, keyText)
            }
        } catch (e: Exception) {
            "加密失败: ${e.message}"
        }
    }

    // 检查是否为有效的Base64字符串
    fun isValidBase64(text: String): Boolean {
        return try {
            Base64.decode(text, Base64.NO_WRAP)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun decryptText() {
        if (keyText.isEmpty()) {
            resultText = "请输入密钥"
            return
        }
        try {
            if (inputText.isEmpty()) {
                resultText = "请输入要解密的内容"
                return
            }

            resultText = if (useOpenSslFormat) {
                OpenSslCompatibleAes.decrypt(inputText, keyText)
            } else {
                if (!isValidBase64(inputText)) {
                    return
                }
                OpenSslCompatibleAes.SimpleAes.decrypt(inputText, keyText)
            }
        } catch (e: Exception) {
            resultText = "解密失败: ${e.message}"
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("AES加解密") },
            navigationIcon = {
                FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }
        )

        // 使用垂直滚动容器包裹内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 输入区域
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        placeholder = { Text("请输入文本或Base64密文") },
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // 加密格式选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OpenSSL兼容格式:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = useOpenSslFormat,
                            onCheckedChange = { useOpenSslFormat = it },
                            thumbContent = {
                                Icon(
                                    imageVector = if (useOpenSslFormat) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    tint = if (useOpenSslFormat) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    }
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (useOpenSslFormat) "开启" else "关闭",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 密钥和操作区域
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                        // 解密按钮
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

                        // 加密按钮
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

            // 输出区域
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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

            // 使用说明
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "• OpenSSL格式（推荐）：与OpenSSL工具兼容，密文以\"U2FsdGVkX1\"开头\n" +
                                "• 简单AES格式：使用AES/ECB模式，密文为纯Base64编码\n" +
                                "• 加密：输入文本和密钥，点击\"加密\"按钮\n" +
                                "• 解密：输入Base64密文和密钥，点击\"解密\"按钮\n" +
                                "• 请确保加密和解密使用相同的密钥和格式\n" +
                                "• 对于OpenSSL格式，会自动尝试AES-128、AES-192和AES-256",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AesCryptoScreenPreview() {
    ToolBoxTheme {
        AesCryptoScreen()
    }
}