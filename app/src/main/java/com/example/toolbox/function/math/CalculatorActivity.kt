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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

fun evaluateRecursive(expr: String, pos: Int): Pair<Double, Int> {
    var (result, currentPos) = parseTerm(expr, pos)
    var position = currentPos
    
    while (position < expr.length && (expr[position] == '+' || expr[position] == '-')) {
        val op = expr[position]
        position++
        val (right, newPos) = parseTerm(expr, position)
        result = if (op == '+') result + right else result - right
        position = newPos
    }
    
    return Pair(result, position)
}

fun parseTerm(expr: String, pos: Int): Pair<Double, Int> {
    var (result, currentPos) = parsePower(expr, pos)
    var position = currentPos
    
    while (position < expr.length && (expr[position] == '*' || expr[position] == '/')) {
        val op = expr[position]
        position++
        val (right, newPos) = parsePower(expr, position)
        result = if (op == '*') result * right else result / right
        position = newPos
    }
    
    return Pair(result, position)
}

fun parsePower(expr: String, pos: Int): Pair<Double, Int> {
    val (base, currentPos) = parseFactor(expr, pos)
    var position = currentPos
    
    if (position < expr.length && expr[position] == '^') {
        position++
        val (exponent, newPos) = parsePower(expr, position)
        return Pair(base.pow(exponent), newPos)
    }
    
    return Pair(base, position)
}

fun parseFactor(expr: String, pos: Int): Pair<Double, Int> {
    if (pos >= expr.length) return Pair(0.0, pos)
    
    // 处理括号表达式
    if (expr[pos] == '(') {
        val (result, newPos) = evaluateRecursive(expr, pos + 1)
        // 跳过右括号
        val finalPos = if (newPos < expr.length && expr[newPos] == ')') newPos + 1 else newPos
        return Pair(result, finalPos)
    }
    
    // 处理负号
    if (expr[pos] == '-') {
        val (result, newPos) = parseFactor(expr, pos + 1)
        return Pair(-result, newPos)
    }
    
    // 处理正号
    if (expr[pos] == '+') {
        return parseFactor(expr, pos + 1)
    }
    
    // 处理数字
    return parseNumber(expr, pos)
}

fun parseNumber(expr: String, pos: Int): Pair<Double, Int> {
    if (pos >= expr.length) return Pair(0.0, pos)

    var currentPos = pos
    
    while (currentPos < expr.length && (expr[currentPos].isDigit() || expr[currentPos] == '.')) {
        currentPos++
    }
    
    if (currentPos == pos) return Pair(0.0, currentPos)
    
    val number = expr.substring(pos, currentPos).toDoubleOrNull() ?: 0.0
    return Pair(number, currentPos)
}

fun simpleEval(expr: String): Double {
    val cleaned = expr.replace(" ", "")
    if (cleaned.isEmpty()) return 0.0

    return try {
        evaluateRecursive(cleaned, 0).first
    } catch (_: Exception) {
        0.0
    }
}

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
        val df = DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US))
        val formattedInt = df.format(intPart)

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

        fun measureWidth(size: TextUnit): Float {
            val textPainter = androidx.compose.ui.text.ParagraphIntrinsics(
                text = visualText,
                style = TextStyle(fontSize = size, fontWeight = FontWeight.Bold),
                density = density,
                fontFamilyResolver = createFontFamilyResolver(context)
            )
            return textPainter.minIntrinsicWidth
        }

        SideEffect {
            val currentWidth = measureWidth(fontSizeValue)

            if (currentWidth > maxWidthPx) {
                if (fontSizeValue > minFontSize) {
                    fontSizeValue = (fontSizeValue.value - 2f).sp
                }
            } else if (fontSizeValue < baseFontSize) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var displayValue by remember { mutableStateOf("") }
    var expression by remember { mutableStateOf("") }
    var hasResult by remember { mutableStateOf(false) }
    var isScientificMode by remember { mutableStateOf(false) }

    fun evaluateExpression(expr: String): Double {
        var processed = expr.replace("×", "*").replace("÷", "/")
        processed = processed.replace("π", "${Math.PI}").replace("e", "${Math.E}")

        val sinPattern = Regex("sin\\(([^)]+)\\)")
        val cosPattern = Regex("cos\\(([^)]+)\\)")
        val tanPattern = Regex("tan\\(([^)]+)\\)")
        val log10Pattern = Regex("log\\(([^)]+)\\)")
        val lnPattern = Regex("ln\\(([^)]+)\\)")
        val sqrtPattern = Regex("sqrt\\(([^)]+)\\)")

        processed = sinPattern.replace(processed) { match ->
            sin(Math.toRadians(match.groupValues[1].toDouble())).toString()
        }
        processed = cosPattern.replace(processed) { match ->
            cos(Math.toRadians(match.groupValues[1].toDouble())).toString()
        }
        processed = tanPattern.replace(processed) { match ->
            tan(Math.toRadians(match.groupValues[1].toDouble())).toString()
        }
        processed = log10Pattern.replace(processed) { match ->
            log10(match.groupValues[1].toDouble()).toString()
        }
        processed = lnPattern.replace(processed) { match ->
            ln(match.groupValues[1].toDouble()).toString()
        }
        processed = sqrtPattern.replace(processed) { match ->
            sqrt(match.groupValues[1].toDouble()).toString()
        }

        return simpleEval(processed)
    }

    fun clear() {
        displayValue = ""
        expression = ""
        hasResult = false
    }

    fun delete() {
        if (hasResult) {
            clear()
        } else if (displayValue.isNotEmpty()) {
            displayValue = displayValue.dropLast(1)
        }
    }


    fun appendNumber(number: String) {
        if (hasResult) {
            displayValue = number
            expression = ""
            hasResult = false
        } else {
            displayValue += number
        }
    }

    fun appendDecimal() {
        if (hasResult) {
            displayValue = "0."
            expression = ""
            hasResult = false
        } else if (!displayValue.contains(".")) {
            if (displayValue.isEmpty()) {
                displayValue = "0."
            } else {
                displayValue += "."
            }
        }
    }

    fun formatBigDecimal(value: BigDecimal): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
        val absValue = value.abs()

        val useScientific = absValue >= BigDecimal("1000000000000") ||
                (absValue < BigDecimal("0.000001") && absValue > BigDecimal.ZERO)

        return if (useScientific) {
            val df = DecimalFormat("0.######E0", DecimalFormatSymbols.getInstance(Locale.US))
            df.format(value)
        } else {
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

    fun setOperator(operator: String) {
        if (hasResult) {
            expression = "$displayValue $operator "
            displayValue = ""
            hasResult = false
        } else if (displayValue.isNotEmpty()) {
            expression += "$displayValue $operator "
            displayValue = ""
        } else if (expression.isNotEmpty() && expression.trimEnd().last().isLetterOrDigit()) {
            expression = expression.trimEnd().dropLast(1) + " " + operator + " "
        }
    }

    fun appendFunction(funcName: String) {
        if (hasResult) {
            expression = "$funcName("
            displayValue = ""
            hasResult = false
        } else {
            if (displayValue.isNotEmpty()) {
                expression += "$displayValue * $funcName("
                displayValue = ""
            } else {
                expression += "$funcName("
            }
        }
    }

    fun appendConstant(value: String) {
        if (hasResult) {
            displayValue = value
            expression = ""
            hasResult = false
        } else {
            displayValue += value
        }
    }

    fun appendCloseParenthesis() {
        if (displayValue.isNotEmpty()) {
            expression += "$displayValue)"
            displayValue = ""
        } else if (expression.isNotEmpty()) {
            expression += ")"
        }
    }

    fun calculate() {
        val fullExpression = expression + displayValue
        if (fullExpression.isEmpty()) return
        
        try {
            val result = evaluateExpression(fullExpression)
            displayValue = formatBigDecimal(BigDecimal(result.toString()))
            expression = ""
            hasResult = true
        } catch (_: Exception) {
            displayValue = "错误"
            expression = ""
            hasResult = true
        }
    }

    fun percentage() {
        if (displayValue.isNotEmpty()) {
            expression += "($displayValue/100)"
            displayValue = ""
        }
    }

    fun toggleSign() {
        if (displayValue.isNotEmpty()) {
            displayValue = if (displayValue.startsWith("-")) {
                displayValue.substring(1)
            } else {
                "-$displayValue"
            }
        } else if (expression.isNotEmpty()) {
            expression += "(-"
        }
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
            },
            actions = {
                IconButton(onClick = { isScientificMode = !isScientificMode }) {
                    Icon(
                        if (isScientificMode) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        if (isScientificMode) "收起科学模式" else "展开科学模式"
                    )
                }
            }
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (expression.isNotEmpty()) {
                    Text(
                        text = expression,
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
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    AutoSizeText(
                        text = displayValue.ifEmpty { "0" },
                        baseFontSize = 60.sp,
                        minFontSize = 30.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val buttonSpacing = 8.dp
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                if (isScientificMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                    ) {
                        CalculatorButton(
                            text = "sin",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = { appendFunction("sin") }
                        )
                        CalculatorButton(
                            text = "cos",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = { appendFunction("cos") }
                        )
                        CalculatorButton(
                            text = "tan",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = { appendFunction("tan") }
                        )
                        CalculatorButton(
                            text = "log",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = { appendFunction("log") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                    ) {
                        CalculatorButton(
                            text = "ln",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = { appendFunction("ln") }
                        )
                        CalculatorButton(
                            text = "x²",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = { 
                                if (displayValue.isNotEmpty()) {
                                    expression += "($displayValue)^2"
                                    displayValue = ""
                                }
                            }
                        )
                        CalculatorButton(
                            text = "√",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = { appendFunction("sqrt") }
                        )
                        CalculatorButton(
                            text = "xʸ",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            onClick = { setOperator("^") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                    ) {
                        CalculatorButton(
                            text = "(",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = { 
                                if (displayValue.isNotEmpty()) {
                                    expression += "$displayValue * ("
                                    displayValue = ""
                                } else {
                                    expression += "("
                                }
                            }
                        )
                        CalculatorButton(
                            text = ")",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = { appendCloseParenthesis() }
                        )
                        CalculatorButton(
                            text = "π",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = { appendConstant(Math.PI.toString().take(10)) }
                        )
                        CalculatorButton(
                            text = "e",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = { appendConstant(Math.E.toString().take(10)) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
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
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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