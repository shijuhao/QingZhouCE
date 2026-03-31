package com.example.toolbox.function.text

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ui.theme.ToolBoxTheme
import java.math.BigInteger

class NumberBaseActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("进制转换器") },
                            navigationIcon = {
                                FilledTonalIconButton(onClick = {
                                    this.finish()
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回"
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    // 将内容放在 innerPadding 中以避免被 Toolbar 遮挡
                    NumberBaseConverterScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun NumberBaseConverterScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // 状态变量
    var inputText by remember { mutableStateOf("") }
    var inputRadix by remember { mutableIntStateOf(10) }
    var outputRadix by remember { mutableIntStateOf(2) }
    var resultText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 统一管理支持的进制列表
    val radixOptions = listOf(2, 3, 8, 10, 12, 16, 32, 36)

    // 转换逻辑
    fun performConversion() {
        if (inputText.isBlank()) {
            resultText = ""
            errorMessage = null
            return
        }
        try {
            // 使用 BigInteger 支持超大数字转换
            val decimalValue = BigInteger(inputText.trim(), inputRadix)
            resultText = decimalValue.toString(outputRadix).uppercase()
            errorMessage = null
        } catch (_: Exception) {
            resultText = ""
            errorMessage = "无效输入 (不符合 $inputRadix 进制)"
        }
    }

    // 监听输入或进制变化自动转换
    LaunchedEffect(inputText, inputRadix, outputRadix) {
        performConversion()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 输入区域
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("请输入数值") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            singleLine = true
        )

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        // 进制选择区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 源进制选择
            Box(modifier = Modifier.weight(1f)) {
                RadixDropdown(
                    label = "从",
                    selectedRadix = inputRadix,
                    options = radixOptions
                ) { inputRadix = it }
            }

            // 目标进制选择
            Box(modifier = Modifier.weight(1f)) {
                RadixDropdown(
                    label = "转换为",
                    selectedRadix = outputRadix,
                    options = radixOptions
                ) { outputRadix = it }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 结果显示区域
        Text(text = "转换结果 (点击可复制):", fontWeight = FontWeight.SemiBold)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (resultText.isNotEmpty()) {
                        copyToClipboard(context, resultText)
                    }
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text(
                text = resultText.ifEmpty { "等待有效输入..." },
                modifier = Modifier.padding(16.dp),
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "提示：支持 2 到 36 进制。大写字母代表 10 以上的数值。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadixDropdown(
    label: String,
    selectedRadix: Int,
    options: List<Int>,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = "$selectedRadix 进制",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { radix ->
                DropdownMenuItem(
                    text = { Text("$radix 进制") },
                    onClick = {
                        onSelected(radix)
                        expanded = false
                    }
                )
            }
        }
    }
}

// 复制到剪贴板的辅助函数
fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("进制转换结果", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
}