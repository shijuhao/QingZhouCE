package com.example.toolbox.function.visual

import android.app.Activity
import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.toolbox.ui.theme.ToolBoxTheme
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

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

    var rectSize by remember { mutableStateOf(IntSize(0, 0)) }

    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var value by remember { mutableFloatStateOf(1f) }

    var hexColorA by remember { mutableStateOf("#FFFFFF") }
    var hexColor = remember(red, green, blue, alpha) {
        if (alpha == 255) {
            hexColorA
        } else {
            "#%02X%02X%02X%02X".format(alpha, red, green, blue)
        }
    }
    var rgbText by remember { mutableStateOf("RGB(255, 255, 255)") }
    var argbText by remember { mutableStateOf("ARGB(255, 255, 255, 255)") }
    var errorMessage by remember { mutableStateOf("") }

    val gradientBitmap = remember(hue) {
        val width = 256
        val height = 256
        val bitmap = createBitmap(width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val s = x / (width - 1f)
                val v = 1f - y / (height - 1f)
                val color = AndroidColor.HSVToColor(floatArrayOf(hue, s, v))
                bitmap[x, y] = color
            }
        }
        bitmap
    }

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

            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            )
                            .onGloballyPositioned { coordinates ->
                                rectSize = coordinates.size
                            }
                            .pointerInput(rectSize) {
                                detectDragGestures(
                                    onDrag = { change, _ ->
                                        val pos = change.position
                                        if (rectSize.width > 0 && rectSize.height > 0) {
                                            val newSaturation =
                                                (pos.x / rectSize.width).coerceIn(0f, 1f)
                                            val newValue =
                                                1f - (pos.y / rectSize.height).coerceIn(0f, 1f)
                                            updateFromHsv(hue, newSaturation, newValue, alpha)
                                        }
                                        change.consume()
                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val w = size.width
                            val h = size.height
                            if (w <= 0 || h <= 0) return@Canvas

                            drawImage(
                                image = gradientBitmap.asImageBitmap(),
                                dstSize = IntSize(w.toInt(), h.toInt())
                            )

                            val indicatorX = saturation * w
                            val indicatorY = (1f - value) * h
                            drawCircle(
                                color = ComposeColor.White,
                                radius = 8f,
                                center = Offset(indicatorX, indicatorY),
                                style = Stroke(width = 2f)
                            )
                            drawCircle(
                                color = ComposeColor.Black,
                                radius = 6f,
                                center = Offset(indicatorX, indicatorY),
                                style = Stroke(width = 2f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            )
                            .pointerInput(Unit) { // key 设为 Unit，因为不需要重置手势
                                detectDragGestures(
                                    onDrag = { change, _ ->
                                        val pos = change.position
                                        val size = this.size
                                        // 色相 = 360 * (y / height)
                                        val newHue = (pos.y / size.height).coerceIn(0f, 1f) * 360f
                                        updateFromHsv(newHue, saturation, value, alpha)
                                        change.consume()
                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val w = size.width
                            val h = size.height
                            val colors = (0..360 step 10).map { angle ->
                                ComposeColor(
                                    AndroidColor.HSVToColor(
                                        floatArrayOf(
                                            angle.toFloat(),
                                            1f,
                                            1f
                                        )
                                    )
                                )
                            }
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = colors,
                                    startY = 0f,
                                    endY = h
                                ),
                                size = size
                            )
                            val indicatorY = (hue / 360f) * h
                            drawRect(
                                color = ComposeColor.White,
                                topLeft = Offset(0f, indicatorY - 4f),
                                size = androidx.compose.ui.geometry.Size(w, 8f),
                                style = Stroke(width = 2f)
                            )
                            drawRect(
                                color = ComposeColor.Black,
                                topLeft = Offset(0f, indicatorY - 3f),
                                size = androidx.compose.ui.geometry.Size(w, 6f),
                                style = Stroke(width = 2f)
                            )
                        }
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
                            hexColor = it
                            parseHexColor(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("例如: #FF5733") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        singleLine = true
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