package com.example.toolbox.function.text

import android.app.Activity
import android.os.Bundle
import android.util.Base64
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class JWTParseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    JWTParseScreen(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JWTParseScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var jwtInput by remember { mutableStateOf("") }
    var headerResult by remember { mutableStateOf("") }
    var payloadResult by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    fun parseJWT() {
        errorMessage = ""
        headerResult = ""
        payloadResult = ""

        if (jwtInput.isEmpty()) {
            return
        }

        try {
            val parts = jwtInput.trim().split(".")
            if (parts.size != 3) {
                errorMessage = "无效的JWT格式，应包含3个部分"
                return
            }

            val headerJson = String(Base64.decode(parts[0], Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)
            val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)

            val headerObj = JSONObject(headerJson)
            val payloadObj = JSONObject(payloadJson)

            headerResult = headerObj.toString(2)
            payloadResult = payloadObj.toString(2)
        } catch (e: Exception) {
            errorMessage = "解析失败: ${e.message}"
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("JWT解析") },
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
                        text = "输入JWT Token",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = jwtInput,
                        onValueChange = { jwtInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("粘贴JWT token（eyJ...）") },
                        singleLine = false,
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { parseJWT() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("解析JWT")
                    }
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

            if (headerResult.isNotEmpty()) {
                Card(elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Header（头部）",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = headerResult,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            if (payloadResult.isNotEmpty()) {
                Card(elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Payload（载荷）",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = payloadResult,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            )
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
                        text = "说明",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "• JWT由三部分组成：Header.Payload.Signature\n" +
                                "• 此工具仅解析Header和Payload部分\n" +
                                "• ⚠️ 不验证签名，仅用于查看内容\n" +
                                "• Header包含算法和token类型\n" +
                                "• Payload包含声明（claims）数据",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
