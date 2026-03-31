package com.example.toolbox.function.math

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ui.theme.ToolBoxTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CalculatorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CalculatorScreen(
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }
            }
        }
    }
}

fun getVisualText(text: String): String {
    if (text == "错误" || text.contains("E")) return text

    return try {
        val parts = text.split(".")
        val intPart = parts[0].toBigDecimal()
        // 格式化整数部分，加入千分位
        val df = DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US))
        val formattedInt = df.format(intPart)

        // 如果有小数部分，拼回去
        if (parts.size > 1) {
            "$formattedInt.${parts[1]}"
        } else if (text.endsWith(".")) {
            "$formattedInt."
        } else {
            formattedInt
        }
    } catch (_: Exception) {
        text
    }
}

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    baseFontSize: TextUnit = 64.sp,
    minFontSize: TextUnit = 28.sp,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val visualText = remember(text) { getVisualText(text) }

    val density = LocalDensity.current
    val context = LocalContext.current

    var fontSizeValue by remember { mutableStateOf(baseFontSize) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxWidthPx = with(density) { maxWidth.toPx() }

        // 辅助函数：测量当前字号下的文字宽度
        fun measureWidth(size: TextUnit): Float {
            val textPainter = androidx.compose.ui.text.ParagraphIntrinsics(
                text = visualText,
                style = TextStyle(fontSize = size, fontWeight = FontWeight.Bold),
                density = density,
                fontFamilyResolver = createFontFamilyResolver(context)
            )
            return textPainter.maxIntrinsicWidth
        }

        // 每一帧渲染前检查逻辑
        SideEffect {
            val currentWidth = measureWidth(fontSizeValue)

            if (currentWidth > maxWidthPx) {
                // 情况1：文字太长了，继续缩小
                if (fontSizeValue > minFontSize) {
                    fontSizeValue = (fontSizeValue.value - 2f).sp
                }
            } else if (fontSizeValue < baseFontSize) {
                // 情况2：文字变短了，尝试恢复变大
                val nextSize = (fontSizeValue.value + 2f).sp
                if (measureWidth(nextSize) <= maxWidthPx) {
                    fontSizeValue = nextSize
                }
            }
        }

        Text(
            text = visualText,
            color = color,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(
                fontSize = fontSizeValue,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var displayValue by remember { mutableStateOf("0") }
    var previousValue by remember { mutableStateOf("") }
    var currentOperator by remember { mutableStateOf("") }
    var waitingForNewValue by remember { mutableStateOf(false) }
    var hasResult by remember { mutableStateOf(false) }
    var lastOperand by remember { mutableStateOf("") }

    fun clear() {
        displayValue = "0"
        previousValue = ""
        currentOperator = ""
        lastOperand = "" // 清空
        waitingForNewValue = false
        hasResult = false
    }

    fun delete() {
        if (hasResult) {
            displayValue = "0"
            hasResult = false
        } else {
            displayValue = if (displayValue.length > 1) displayValue.dropLast(1) else "0"
        }
    }


    fun appendNumber(number: String) {
        if (hasResult || displayValue == "错误") {
            displayValue = number
            hasResult = false
            waitingForNewValue = false
            currentOperator = ""
            previousValue = ""
            lastOperand = ""
        }
        else if (waitingForNewValue) {
            displayValue = number
            waitingForNewValue = false
        }
        else {
            if (displayValue.length < 15) {
                displayValue = if (displayValue == "0") {
                    number
                } else {
                    displayValue + number
                }
            }
        }
    }

    fun appendDecimal() {
        if (hasResult) {
            displayValue = "0."
            previousValue = ""
            currentOperator = ""
            hasResult = false
            waitingForNewValue = false
        } else if (waitingForNewValue) {
            displayValue = "0."
            waitingForNewValue = false
        } else if (!displayValue.contains(".")) {
            displayValue += "."
        }
    }

    fun formatBigDecimal(value: BigDecimal): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
        val absValue = value.abs()

        // 科学计数法阈值
        val useScientific = absValue >= BigDecimal("1000000000000") ||
                (absValue < BigDecimal("0.000001") && absValue > BigDecimal.ZERO)

        return if (useScientific) {
            val df = DecimalFormat("0.######E0", DecimalFormatSymbols.getInstance(Locale.US))
            df.format(value)
        } else {
            // 关键：去掉这里的逗号 #,### -> #
            val df = DecimalFormat("#.########", DecimalFormatSymbols.getInstance(Locale.US))
            val result = df.format(value)
            if (result.length > 15) {
                val dfForce = DecimalFormat("0.#####E0", DecimalFormatSymbols.getInstance(Locale.US))
                dfForce.format(value)
            } else {
                result
            }
        }
    }

    fun calculate() {
        val first: BigDecimal
        val second: BigDecimal

        if (hasResult && lastOperand.isNotEmpty()) {
            // --- 情况 A: 连续点击“=” ---
            // 比如 8 (display) + 1 (lastOperand)
            first = try { BigDecimal(displayValue) } catch (_: Exception) { BigDecimal.ZERO }
            second = try { BigDecimal(lastOperand) } catch (_: Exception) { BigDecimal.ZERO }
        } else if (previousValue.isNotEmpty() && currentOperator.isNotEmpty()) {
            // --- 情况 B: 第一次点击“=” ---
            // 比如 7 (previous) + 1 (display)
            first = try { BigDecimal(previousValue) } catch (_: Exception) { BigDecimal.ZERO }
            second = try { BigDecimal(displayValue) } catch (_: Exception) { BigDecimal.ZERO }
            // 关键：记录下这个 1，供下次连续点击使用
            lastOperand = displayValue
        } else {
            return // 没有足够的条件进行计算
        }

        val result = when (currentOperator) {
            "+" -> first + second
            "-" -> first - second
            "×" -> first.multiply(second)
            "÷" -> {
                if (second.compareTo(BigDecimal.ZERO) == 0) null
                else first.divide(second, 15, RoundingMode.HALF_UP)
            }
            else -> first
        }

        if (result == null) {
            displayValue = "错误"
            previousValue = ""
            lastOperand = ""
        } else {
            displayValue = formatBigDecimal(result)
            // 注意：这里不要清空 currentOperator 和 lastOperand，以便连续计算
            previousValue = ""
            hasResult = true
            waitingForNewValue = true
        }
    }

    fun setOperator(operator: String) {
        lastOperand = "" // 重置连续计算操作数
        if (hasResult) {
            previousValue = displayValue
            currentOperator = operator
            waitingForNewValue = true
            hasResult = false
        } else if (currentOperator.isNotEmpty() && !waitingForNewValue) {
            calculate()
            previousValue = displayValue
            currentOperator = operator
            waitingForNewValue = true
            hasResult = false
        } else {
            previousValue = displayValue
            currentOperator = operator
            waitingForNewValue = true
        }
    }

    fun percentage() {
        val value = try { BigDecimal(displayValue) } catch (_: Exception) { BigDecimal.ZERO }
        val result = value.divide(BigDecimal(100), 15, RoundingMode.HALF_UP)
        displayValue = formatBigDecimal(result)
        hasResult = false
    }

    fun toggleSign() {
        if (displayValue != "0") {
            displayValue = if (displayValue.startsWith("-")) {
                displayValue.substring(1)
            } else {
                "-$displayValue"
            }
        }
        hasResult = false
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("计算器") },
            navigationIcon = {
                FilledTonalIconButton(onClick = { (context as ComponentActivity).finish() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }
        )

        // 计算器显示区域
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .heightIn(max = 150.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (previousValue.isNotEmpty()) {
                    Text(
                        text = "$previousValue $currentOperator",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    AutoSizeText(
                        text = displayValue,
                        baseFontSize = 60.sp,
                        minFontSize = 30.sp
                    )
                }
            }
        }

        // 按钮区域 - 使用垂直滚动
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // 使用权重占用剩余空间
                .verticalScroll(rememberScrollState()) // 添加垂直滚动
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 第一行：清除、正负、百分比、除号
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorButton(
                    text = "C",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { clear() }
                )
                CalculatorButton(
                    text = "±",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { toggleSign() }
                )
                CalculatorButton(
                    text = "%",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { percentage() }
                )
                CalculatorButton(
                    text = "÷",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { setOperator("÷") }
                )
            }

            // 第二行：7、8、9、乘号
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorButton(
                    text = "7",
                    modifier = Modifier.weight(1f),
                    onClick = { appendNumber("7") }
                )
                CalculatorButton(
                    text = "8",
                    modifier = Modifier.weight(1f),
                    onClick = { appendNumber("8") }
                )
                CalculatorButton(
                    text = "9",
                    modifier = Modifier.weight(1f),
                    onClick = { appendNumber("9") }
                )
                CalculatorButton(
                    text = "×",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { setOperator("×") }
                )
            }

            // 第三行：4、5、6、减号
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorButton(
                    text = "4",
                    modifier = Modifier.weight(1f),
                    onClick = { appendNumber("4") }
                )
                CalculatorButton(
                    text = "5",
                    modifier = Modifier.weight(1f),
                    onClick = { appendNumber("5") }
                )
                CalculatorButton(
                    text = "6",
                    modifier = Modifier.weight(1f),
                    onClick = { appendNumber("6") }
                )
                CalculatorButton(
                    text = "-",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { setOperator("-") }
                )
            }

            // 第四行：1、2、3、加号
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorButton(
                    text = "1",
                    modifier = Modifier.weight(1f),
                    onClick = { appendNumber("1") }
                )
                CalculatorButton(
                    text = "2",
                    modifier = Modifier.weight(1f),
                    onClick = { appendNumber("2") }
                )
                CalculatorButton(
                    text = "3",
                    modifier = Modifier.weight(1f),
                    onClick = { appendNumber("3") }
                )
                CalculatorButton(
                    text = "+",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { setOperator("+") }
                )
            }

            // 第五行：0、小数点、删除、等于
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(2f)
                        .aspectRatio(2f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    onClick = { appendNumber("0") }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "0",
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                CalculatorButton(
                    text = ".",
                    modifier = Modifier.weight(1f),
                    onClick = { appendDecimal() }
                )
                CalculatorButton(
                    icon = Icons.AutoMirrored.Filled.Backspace,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { delete() }
                )
                CalculatorButton(
                    text = "=",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary,
                    textColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = { calculate() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorButton(
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // 触发轻微震动
            onClick()
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (text != null) {
                Text(
                    text = text,
                    fontSize = 24.sp,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    CalculatorButton(
        text = text,
        icon = null,
        modifier = modifier,
        color = color,
        textColor = textColor,
        onClick = onClick
    )
}

@Preview(showBackground = true)
@Composable
fun CalculatorScreenPreview() {
    ToolBoxTheme {
        CalculatorScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun CalculatorButtonPreview() {
    ToolBoxTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorButton(
                text = "7",
                onClick = {}
            )
            CalculatorButton(
                text = "+",
                color = MaterialTheme.colorScheme.primaryContainer,
                onClick = {}
            )
            CalculatorButton(
                text = "=",
                color = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onPrimary,
                onClick = {}
            )
        }
    }
}