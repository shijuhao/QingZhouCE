package com.example.toolbox.function.visual

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlin.math.roundToInt
import androidx.core.graphics.get

class ImageGetColorActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("图片取色器") },
                            navigationIcon = {
                                FilledTonalIconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                            },
                        )
                    }
                ) { innerPadding ->
                    ColorPickerScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * 手动计算亮度，兼容 API 23
 * 返回值在 0.0 到 1.0 之间
 */
fun calculateLuminance(colorInt: Int): Float {
    val r = Color.red(colorInt)
    val g = Color.green(colorInt)
    val b = Color.blue(colorInt)
    return (0.299f * r + 0.587f * g + 0.114f * b) / 255f
}

@Composable
fun ColorPickerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pointerOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var containerSize by remember { mutableStateOf(IntSize(0, 0)) }
    var selectedColor by remember { mutableIntStateOf(Color.WHITE) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                pointerOffset = Offset(0f, 0f)
            } catch (_: Exception) {
                Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateColor(offset: Offset) {
        val bmp = bitmap ?: return
        if (containerSize.width <= 0 || containerSize.height <= 0) return

        val x = offset.x.coerceIn(0f, containerSize.width.toFloat())
        val y = offset.y.coerceIn(0f, containerSize.height.toFloat())

        val scaleX = bmp.width.toFloat() / containerSize.width
        val scaleY = bmp.height.toFloat() / containerSize.height

        val pixelX = (x * scaleX).toInt().coerceIn(0, bmp.width - 1)
        val pixelY = (y * scaleY).toInt().coerceIn(0, bmp.height - 1)

        selectedColor = bmp[pixelX, pixelY]
        pointerOffset = Offset(x, y)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 颜色预览卡片
        val hexColor = String.format("#%06X", 0xFFFFFF and selectedColor)
        val rgbText = "RGB: ${Color.red(selectedColor)}, ${Color.green(selectedColor)}, ${Color.blue(selectedColor)}"

        // 使用自定义亮度计算
        val isLightColor = calculateLuminance(selectedColor) > 0.5f
        val contentColor = if (isLightColor) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(bottom = 16.dp)
                .clickable {
                    clipboardManager.setText(AnnotatedString(hexColor))
                    Toast.makeText(context, "已复制: $hexColor", Toast.LENGTH_SHORT).show()
                },
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(selectedColor)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = contentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = hexColor,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(rgbText, color = contentColor, style = MaterialTheme.typography.bodyMedium)
                    Text("点击复制", fontSize = 11.sp, color = contentColor.copy(alpha = 0.7f))
                }
            }
        }

        // 图片操作区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Color(0xFFEEEEEE))
                .onGloballyPositioned { containerSize = it.size }
                .pointerInput(bitmap) {
                    detectDragGestures { change, _ ->
                        updateColor(change.position)
                    }
                }
                .pointerInput(bitmap) {
                    detectTapGestures { updateColor(it) }
                }
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )

                // 准星
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val crosshairColor = if (isLightColor) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
                    drawCircle(
                        color = crosshairColor,
                        radius = 12f,
                        center = pointerOffset,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                    drawLine(
                        color = crosshairColor,
                        start = Offset(pointerOffset.x - 30, pointerOffset.y),
                        end = Offset(pointerOffset.x + 30, pointerOffset.y),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = crosshairColor,
                        start = Offset(pointerOffset.x, pointerOffset.y - 30),
                        end = Offset(pointerOffset.x, pointerOffset.y + 30),
                        strokeWidth = 3f
                    )
                }
            } ?: Text("点击下方按钮选择图片", modifier = Modifier.align(Alignment.Center).padding(10.dp), color = androidx.compose.ui.graphics.Color.Gray)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 滑动条辅助
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("X: ${pointerOffset.x.roundToInt()}", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = pointerOffset.x,
                onValueChange = { updateColor(Offset(it, pointerOffset.y)) },
                valueRange = 0f..(containerSize.width.toFloat().coerceAtLeast(1f))
            )

            Text("Y: ${pointerOffset.y.roundToInt()}", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = pointerOffset.y,
                onValueChange = { updateColor(Offset(pointerOffset.x, it)) },
                valueRange = 0f..(containerSize.height.toFloat().coerceAtLeast(1f))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("选择本地图片")
        }
    }
}