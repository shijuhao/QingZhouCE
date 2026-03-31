package com.example.toolbox.function.text

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ui.theme.ToolBoxTheme

class SpecialTextActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SpecialTextScreen(
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialTextScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // 特殊文本样式数据
    val textStyles = listOf(
        "a'ゞ文本",
        "ζั͡文本ۣۖิ ั͡✾",
        "༺文本༻",
        "꧁文本꧂",
        "✎﹏ℳ文本ℳ₆₆₆",
        "文本✅已认证💯",
        "ℳℓ文本ℳℓ",
        "༄༅文本༅༉",
        "⃢文本⃢",
        "文本ۣۖิ",
        "ฏ๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎文本ฏ๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎",
        "ด้้้้้็็็็็้้้้้็็็็็้้้้้้้้็็็็็้้้้้็็็็็้้้้้้้้็็็็็้้้้้็็็็็้้้้้้้้็文本ด้้้้้็็็็็้้้้้็็็็็้้้้้้้้็็็็็้้้้้็็็็็้้้้้้้้็็็็็้้้้้็็็็็้้้้้้้้็",
        "ۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖ 文本 ۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖ"
    )

    // 状态变量
    var inputText by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(textStyles[0]) }
    var generatedText by remember { mutableStateOf("生成后的文字将会显示在此处") }
    var isExpanded by remember { mutableStateOf(false) }


    // 实现 Lua 代码中的 change 函数逻辑 - 遍历每个字符
    fun generateByTemplate(template: String, input: String): String {
        var result = ""
        var a = 0
        var b = 1

        for (i in input.indices) {
            val char = input.substring(a, b)
            val decorated = template.replace("文本", char)
            result += decorated
            a += 1
            b += 1
        }

        return result
    }

    fun generateSpecialText() {
        if (inputText.isEmpty()) {
            // 可以添加 Toast 提示
            return
        }

        val template = selectedStyle
        var result = ""

        // 根据 Lua 代码中的逻辑实现
        when (template) {
            "文本ۣۖิ" -> {
                result = generateByTemplate("文本ۣۖิ", inputText)
            }
            "ζั͡文本ۣۖิ ั͡✾" -> {
                val temp = generateByTemplate("文本ۣۖิ", inputText)
                result = "ζั͡$temp ั͡✾"
            }
            "ฏ๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎文本ฏ๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎" -> {
                result = generateByTemplate("ฏ๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎文本ฏ๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎๎", inputText)
            }
            "ด้้้้้็็็็็้้้้้็็็็็้้้้้้้้็็็็็้้้้้็็็็็้้้้้้้้็็็็็้้้้้็็็็็้้้้้้้้็文本ด้้้้้็็็็็้้้้้็็็็็้้้้้้้้็็็็็้้้้้็็็็็้้้้้้้้็็็็็้้้้้็็็็็้้้้้้้้็" -> {
                result = generateByTemplate("ด้้้้้็็็็็้้้้้็็็็็้้้้้้้้็็็็็้้้้้็็็็็้้้้้้้้็็็็็้้้้้็็็็็้้้้้้้้็文本ด้้้้้็็็็็้้้้้็็็็็้้้้้้้้็็็็็้้้้้็็็็็้้้้้้้้็็็็็้้้้้็็็็็้้้้้้้้็", inputText)
            }
            "ۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖ 文本 ۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖ" -> {
                result = generateByTemplate("ۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖ 文本 ۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۣۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖۖ", inputText)
            }
            "a'ゞ文本" -> {
                val temp = generateByTemplate("文本", inputText)
                result = "a'ゞ$temp"
            }
            "༺文本༻" -> {
                result = "༺$inputText༻"
            }
            "꧁文本꧂" -> {
                result = "꧁$inputText꧂"
            }
            "✎﹏ℳ文本ℳ₆₆₆" -> {
                result = "✎﹏ℳ${inputText}ℳ"
            }
            "文本✅已认证💯" -> {
                result = "$inputText✅已认证💯"
            }
            "ℳℓ文本ℳℓ" -> {
                val temp = generateByTemplate("文本ℳℓ", inputText)
                result = "ℳℓ$temp"
            }
            "༄༅文本༅༉" -> {
                result = "༄༅$inputText༅༉"
            }
            "⃢文本⃢" -> {
                result = generateByTemplate("文本⃢", inputText)
            }
            else -> {
                // 默认简单替换
                result = template.replace("文本", inputText)
            }
        }

        generatedText = result
    }

    // 复制文本到剪贴板
    fun copyToClipboard() {
        if (inputText.isEmpty() && generatedText == "生成后的文字将会显示在此处") {
            Toast.makeText(context,"请输入文本",Toast.LENGTH_SHORT).show()
            return
        }

        if (inputText.isNotEmpty() && generatedText == "生成后的文字将会显示在此处") {
            generateSpecialText()
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("特殊文本", generatedText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context,"已复制",Toast.LENGTH_SHORT).show()
    }

    // 分享文本
    fun shareText() {
        if (inputText.isNotEmpty() && generatedText == "生成后的文字将会显示在此处") {
            generateSpecialText()
        } else if (inputText.isEmpty() && generatedText == "生成后的文字将会显示在此处") {
            Toast.makeText(context,"请输入文本",Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, generatedText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享特殊文本"))
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("特殊文本生成") },
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
                        text = "请输入内容",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("请输入要转换的文本") },
                        singleLine = false,
                        maxLines = 3
                    )
                }
            }

            // 样式选择区域
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "选择样式",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 下拉选择框
                    ExposedDropdownMenuBox(
                        expanded = isExpanded,
                        onExpandedChange = { isExpanded = !isExpanded }
                    ) {
                        TextField(
                            value = selectedStyle,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(
                                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                )
                        )

                        ExposedDropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false }
                        ) {
                            textStyles.forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style) },
                                    onClick = {
                                        selectedStyle = style
                                        isExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { generateSpecialText() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "生成",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("生成")
                }
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
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

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { shareText() },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "分享",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("分享")
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
                        text = "生成结果",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = generatedText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = TextStyle(
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpecialTextScreenPreview() {
    ToolBoxTheme {
        SpecialTextScreen()
    }
}