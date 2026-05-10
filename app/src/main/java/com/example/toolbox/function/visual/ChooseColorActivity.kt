package com.example.toolbox.function.visual

import android.app.Activity
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.toolbox.ui.theme.ToolBoxTheme

class ChooseColorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChooseColorScreen(
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseColorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var red by remember { mutableIntStateOf(255) }
    var green by remember { mutableIntStateOf(255) }
    var blue by remember { mutableIntStateOf(255) }
    var alpha by remember { mutableIntStateOf(255) }

    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var value by remember { mutableFloatStateOf(1f) }

    var hexColorA by remember { mutableStateOf("#FFFFFF") }
    var hexColor by remember { mutableStateOf("#FFFFFF") }
    var rgbText by remember { mutableStateOf("RGB(255, 255, 255)") }
    var argbText by remember { mutableStateOf("ARGB(255, 255, 255, 255)") }
    var errorMessage by remember { mutableStateOf("") }

    val hsv = FloatArray(3)
    AndroidColor.RGBToHSV(255, 255, 255, hsv)
    LaunchedEffect(Unit) {
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    val selectedColor = ComposeColor(red, green, blue, alpha)

    fun updateFromRgb(r: Int, g: Int, b: Int, a: Int) {
        red = r.coerceIn(0, 255)
        green = g.coerceIn(0, 255)
        blue = b.coerceIn(0, 255)
        alpha = a.coerceIn(0, 255)

        val newHsv = FloatArray(3)
        AndroidColor.RGBToHSV(red, green, blue, newHsv)
        hue = newHsv[0]
        saturation = newHsv[1]
        value = newHsv[2]

        hexColorA = String.format("#%02X%02X%02X", red, green, blue)
        hexColor = if (alpha == 255) hexColorA else String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
        rgbText = "RGB($red, $green, $blue)"
        argbText = "ARGB($alpha, $red, $green, $blue)"
    }

    fun updateFromHsv(h: Float, s: Float, v: Float, a: Int) {
        hue = h.coerceIn(0f, 360f)
        saturation = s.coerceIn(0f, 1f)
        value = v.coerceIn(0f, 1f)
        alpha = a.coerceIn(0, 255)

        val rgb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
        red = AndroidColor.red(rgb)
        green = AndroidColor.green(rgb)
        blue = AndroidColor.blue(rgb)

        hexColorA = String.format("#%02X%02X%02X", red, green, blue)
        hexColor = if (alpha == 255) hexColorA else String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
        rgbText = "RGB($red, $green, $blue)"
        argbText = "ARGB($alpha, $red, $green, $blue)"
    }

    fun parseHexColor(hex: String) {
        errorMessage = ""
        try {
            var hexValue = hex.trim()
            if (!hexValue.startsWith("#")) hexValue = "#$hexValue"
            if (hexValue.length == 7 || hexValue.length == 9) {
                val color = hexValue.toColorInt()
                updateFromRgb(
                    AndroidColor.red(color),
                    AndroidColor.green(color),
                    AndroidColor.blue(color),
                    AndroidColor.alpha(color)
                )
            } else {
                errorMessage = "HEX颜色格式不正确，应为 #RRGGBB 或 #AARRGGBB"
            }
        } catch (e: Exception) {
            errorMessage = "HEX颜色解析失败: ${e.message}"
        }
    }

    fun copyToClipboard(text: String, label: String) {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
        Toast.makeText(context, "已复制 $label", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("选色器") },
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
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(selectedColor)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(16.dp)
                            )
                    )
                    Column {
                        Text("颜色预览", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = hexColor,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("HSV 圆盘选择", style = MaterialTheme.typography.titleMedium)
                    
                    // HSV 圆盘
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val size = this.size
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val offset = change.position - center
                                    
                                    // 计算角度和距离
                                    val angle = Math.toDegrees(
                                        kotlin.math.atan2(offset.y.toDouble(), offset.x.toDouble())
                                    ).toFloat()
                                    val distance = kotlin.math.sqrt(
                                        offset.x * offset.x + offset.y * offset.y
                                    )
                                    val maxRadius = kotlin.math.min(size.width, size.height) / 2f
                                    
                                    // 更新色相（角度）
                                    val newHue = (angle + 360) % 360
                                    
                                    // 更新饱和度（距离中心的距离）
                                    val newSaturation = (distance / maxRadius).coerceIn(0f, 1f)
                                    
                                    updateFromHsv(newHue, newSaturation, value, alpha)
                                    change.consume()
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val radius = kotlin.math.min(size.width, size.height) / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            
                            // 如果明度为0，直接绘制黑色背景
                            if (value <= 0.01f) {
                                drawCircle(
                                    color = ComposeColor.Black,
                                    center = center,
                                    radius = radius
                                )
                            } else {
                                // 使用径向渐变绘制HSV圆盘（高性能）
                                // 先绘制色相环作为背景
                                for (i in 0 until 360 step 2) {
                                    drawArc(
                                        color = ComposeColor(
                                            AndroidColor.HSVToColor(floatArrayOf(i.toFloat(), 1f, value))
                                        ),
                                        startAngle = i.toFloat(),
                                        sweepAngle = 4f,
                                        useCenter = true,
                                        topLeft = center - Offset(radius, radius),
                                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                                    )
                                }
                                
                                // 再绘制从白到透明的径向渐变（模拟饱和度）
                                drawCircle(
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(
                                            ComposeColor.White,
                                            ComposeColor.Transparent
                                        ),
                                        center = center,
                                        radius = radius
                                    ),
                                    center = center,
                                    radius = radius
                                )
                            }
                            
                            // 绘制饱和度指示器
                            val indicatorAngle = Math.toRadians(hue.toDouble()).toFloat()
                            val indicatorDistance = saturation * radius
                            val indicatorPos = center + Offset(
                                kotlin.math.cos(indicatorAngle) * indicatorDistance,
                                kotlin.math.sin(indicatorAngle) * indicatorDistance
                            )
                            
                            drawCircle(
                                color = ComposeColor.White,
                                radius = 10f,
                                center = indicatorPos
                            )
                            drawCircle(
                                color = ComposeColor.Black,
                                radius = 8f,
                                center = indicatorPos
                            )
                        }
                    }
                    
                    // 明度滑块
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("明度: ${(value * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = value,
                            onValueChange = { newValue ->
                                updateFromHsv(hue, saturation, newValue, alpha)
                            },
                            valueRange = 0f..1f
                        )
                    }
                }
            }

            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ComposeColor.Transparent)
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val gridSize = 10f
                            for (x in 0 until size.width.toInt() step (gridSize.toInt() * 2)) {
                                for (y in 0 until size.height.toInt() step (gridSize.toInt() * 2)) {
                                    drawRect(
                                        ComposeColor.LightGray,
                                        Offset(x.toFloat(), y.toFloat()),
                                        androidx.compose.ui.geometry.Size(gridSize, gridSize)
                                    )
                                    drawRect(
                                        ComposeColor.LightGray,
                                        Offset(x + gridSize, y + gridSize),
                                        androidx.compose.ui.geometry.Size(gridSize, gridSize)
                                    )
                                    drawRect(
                                        ComposeColor.White,
                                        Offset(x + gridSize, y.toFloat()),
                                        androidx.compose.ui.geometry.Size(gridSize, gridSize)
                                    )
                                    drawRect(
                                        ComposeColor.White,
                                        Offset(x.toFloat(), y + gridSize),
                                        androidx.compose.ui.geometry.Size(gridSize, gridSize)
                                    )
                                }
                            }
                        }

                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        selectedColor.copy(alpha = 0f),
                                        selectedColor.copy(alpha = 1f)
                                    )
                                )
                            )
                        }

                        Slider(
                            value = alpha.toFloat() / 255f,
                            onValueChange = { newAlpha ->
                                updateFromHsv(hue, saturation, value, (newAlpha * 255).toInt())
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.matchParentSize(),
                            track = { _ ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .background(ComposeColor.Transparent)
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        "$alpha",
                        modifier = Modifier.width(40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("HEX颜色输入", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = hexColor,
                        onValueChange = {
                            // 只更新文本，不立即验证
                            hexColor = it
                            errorMessage = "" // 清除错误提示
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("例如: #FF5733") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                // 按回车时才验证
                                parseHexColor(hexColor)
                            }
                        ),
                        singleLine = true,
                        isError = errorMessage.isNotEmpty(),
                        supportingText = {
                            if (errorMessage.isNotEmpty()) {
                                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Card(
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

            // 颜色值显示
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("颜色值", style = MaterialTheme.typography.titleMedium)
                    CopyRow("HEX", hexColor) { copyToClipboard(hexColor, "HEX颜色值") }
                    CopyRow("RGB", rgbText) { copyToClipboard(rgbText, "RGB颜色值") }
                    CopyRow("ARGB", argbText) { copyToClipboard(argbText, "ARGB颜色值") }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun CopyRow(label: String, value: String, onCopy: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp))
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, "复制")
            }
        }
    }
}