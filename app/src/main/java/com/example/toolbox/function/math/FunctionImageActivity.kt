package com.example.toolbox.function.math

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.toolbox.ui.theme.ToolBoxTheme
import net.objecthunter.exp4j.ExpressionBuilder

class FunctionImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                FunctionPlotterScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionPlotterScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val materialThemeColorScheme = MaterialTheme.colorScheme
    var functionText by remember { mutableStateOf("x^2") }
    var xMin by remember { mutableStateOf("-5") }
    var xMax by remember { mutableStateOf("5") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var plotPoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var yMin by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var yMax by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    val scrollState = rememberScrollState()

    fun updatePlot() {
        try {
            errorMessage = null

            val cleanXMin = xMin.trim()
            val cleanXMax = xMax.trim()
            val cleanFunction = functionText.trim()

            if (cleanXMin.isEmpty() || cleanXMax.isEmpty()) {
                errorMessage = "请输入 x 范围"
                plotPoints = emptyList()
                return
            }

            val min = cleanXMin.toFloatOrNull()
            val max = cleanXMax.toFloatOrNull()

            if (min == null || max == null) {
                errorMessage = "请输入有效的数字（例如：-5 或 10）"
                plotPoints = emptyList()
                return
            }

            if (min >= max) {
                errorMessage = "x 最小值必须小于最大值"
                plotPoints = emptyList()
                return
            }

            if (cleanFunction.isEmpty()) {
                errorMessage = "请输入函数表达式"
                plotPoints = emptyList()
                return
            }

            if (cleanFunction.matches(Regex("^[+\\-*/^().\\s]+$"))) {
                errorMessage = "函数表达式不能只有运算符"
                plotPoints = emptyList()
                return
            }

            val expression = ExpressionBuilder(cleanFunction)
                .variable("x")
                .build()

            val step = (max - min) / 500
            val points = mutableListOf<Pair<Float, Float>>()
            var currentYMin = Float.MAX_VALUE
            var currentYMax = -Float.MAX_VALUE

            var x = min
            while (x <= max) {
                try {
                    val y = expression.setVariable("x", x.toDouble()).evaluate().toFloat()
                    if (y.isFinite()) {
                        points.add(x to y)
                        if (y < currentYMin) currentYMin = y
                        if (y > currentYMax) currentYMax = y
                    }
                } catch (_: Exception) {
                }
                x += step
            }

            if (points.isEmpty()) {
                errorMessage = "函数在指定范围内无有效值"
                plotPoints = emptyList()
                return
            }

            val padding = if (currentYMax - currentYMin > 0) (currentYMax - currentYMin) * 0.1f else 1f
            yMin = if (currentYMin == currentYMax) currentYMin - 1f else currentYMin - padding
            yMax = if (currentYMin == currentYMax) currentYMax + 1f else currentYMax + padding
            plotPoints = points

        } catch (e: Exception) {
            errorMessage = "解析失败：${e.message}"
            plotPoints = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        updatePlot()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("函数图像生成器") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "输入函数表达式",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = functionText,
                        onValueChange = { functionText = it },
                        label = { Text("f(x) =") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("例: x^2, sin(x), 2*x+1") }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = xMin,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                                    xMin = newValue
                                }
                            },
                            label = { Text("x 最小值") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = xMax,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                                    xMax = newValue
                                }
                            },
                            label = { Text("x 最大值") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = { updatePlot() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("绘制图像")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "f(x) = $functionText",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }

                    // 绘图 Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        if (plotPoints.isNotEmpty()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height

                            val safeXMin = xMin.toFloatOrNull() ?: 0f
                            val safeXMax = xMax.toFloatOrNull() ?: 1f
                            val xRange = if (safeXMax - safeXMin != 0f) safeXMax - safeXMin else 1f
                            val yRange = if (yMax - yMin != 0f) yMax - yMin else 1f

                            fun mapToCanvas(x: Float, y: Float): Offset {
                                val px = ((x - safeXMin) / xRange) * canvasWidth
                                val py = canvasHeight - ((y - yMin) / yRange) * canvasHeight
                                return Offset(px, py)
                            }

                            val axisColor = Color.Gray
                            val axisStroke = Stroke(width = 1.5f)

                            if (0f in yMin..yMax) {
                                val yZero = mapToCanvas(safeXMin, 0f).y
                                drawLine(
                                    color = axisColor,
                                    start = Offset(0f, yZero),
                                    end = Offset(canvasWidth, yZero),
                                    strokeWidth = axisStroke.width
                                )
                            }

                            if (0f in safeXMin..safeXMax) {
                                val xZero = mapToCanvas(0f, yMin).x
                                drawLine(
                                    color = axisColor,
                                    start = Offset(xZero, 0f),
                                    end = Offset(xZero, canvasHeight),
                                    strokeWidth = axisStroke.width
                                )
                            }

                            val path = Path()
                            plotPoints.forEachIndexed { index, (x, y) ->
                                val point = mapToCanvas(x, y)
                                if (index == 0) {
                                    path.moveTo(point.x, point.y)
                                } else {
                                    path.lineTo(point.x, point.y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = materialThemeColorScheme.primary,
                                style = Stroke(width = 2.5f)
                            )
                        }
                    }

                    if (plotPoints.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "x: [${xMin.toFloatOrNull()}, ${xMax.toFloatOrNull()}]",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "y: [%.2f, %.2f]".format(yMin, yMax),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "支持的函数与运算符",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• 基本运算: + - * / ^ (幂)\n" +
                                "• 三角函数: sin(x), cos(x), tan(x)\n" +
                                "• 其他: sqrt(x), abs(x), log(x), exp(x)\n" +
                                "• 常量: pi, e",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}