@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.function.visual.painter

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainterScreen(
    viewModel: PainterViewModel = viewModel()
) {
    val context = LocalContext.current
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val state by viewModel.state.collectAsState()
    val currentColor by viewModel.currentColor
    val strokeWidth by viewModel.currentStrokeWidth

    val canUndo = state.paths.isNotEmpty()            // 有内容可撤销
    val canRedo = state.redoStack.isNotEmpty()        // 有内容可重做

    var showColorPicker by remember { mutableStateOf(false) }
    var showBrushSizePicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveResultMessage by remember { mutableStateOf<String?>(null) }

    // 权限处理（仅 Android 9 及以下需要 WRITE_EXTERNAL_STORAGE）
    val requestWritePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 再次尝试保存
            showSaveDialog = true
        } else {
            Toast.makeText(context, "需要存储权限才能保存图片", Toast.LENGTH_LONG).show()
        }
    }

    // 检查是否有内容可保存
    val hasContent = state.paths.isNotEmpty() || state.currentPath != null

    // 保存结果对话框
    if (saveResultMessage != null) {
        AlertDialog(
            onDismissRequest = { saveResultMessage = null },
            title = { Text("保存结果") },
            text = { Text(saveResultMessage!!) },
            confirmButton = {
                Button(onClick = { saveResultMessage = null }) {
                    Text("确定")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("画板") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (hasContent && canvasSize.width > 0 && canvasSize.height > 0) {
                                // 检查权限（仅 Android 9 及以下）
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                    val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                                        requestWritePermission.launch(permission)
                                        return@IconButton
                                    }
                                }
                                showSaveDialog = true
                            } else {
                                saveResultMessage = "没有内容可保存"
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }

                    MenuButton(
                        onSaveClick = {
                            if (hasContent && canvasSize.width > 0 && canvasSize.height > 0) {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                    val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                                        requestWritePermission.launch(permission)
                                        return@MenuButton
                                    }
                                }
                                showSaveDialog = true
                            } else {
                                saveResultMessage = "没有内容可保存"
                            }
                        }
                    )
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "撤销")
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "重做")
                    }
                    IconButton(onClick = { showColorPicker = !showColorPicker; showBrushSizePicker = false }) {
                        Icon(Icons.Default.Palette, contentDescription = "颜色")
                    }
                    IconButton(onClick = { showBrushSizePicker = !showBrushSizePicker; showColorPicker = false }) {
                        Icon(Icons.Default.Brush, contentDescription = "画笔大小")
                    }
                    IconButton(onClick = { viewModel.clearCanvas() }) {
                        Icon(Icons.Default.Clear, contentDescription = "清空")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${strokeWidth.toInt()}px",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            DrawingCanvas(
                state = state,
                onDrawStart = { x, y -> viewModel.startNewPath(x, y) },
                onDrawMove = { x, y -> viewModel.addPointToCurrentPath(x, y) },
                onDrawEnd = { viewModel.finishPath() },
                modifier = Modifier.fillMaxSize(),
                onSizeChanged = { size -> canvasSize = size }
            )

            if (showColorPicker) {
                ColorPickerDialog(
                    currentColor = currentColor,
                    onColorSelected = { color ->
                        viewModel.setColor(color)
                        showColorPicker = false
                    },
                    onDismiss = { showColorPicker = false },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            if (showBrushSizePicker) {
                BrushSizeDialog(
                    currentSize = strokeWidth,
                    onSizeChanged = { size -> viewModel.setStrokeWidth(size) },
                    onDismiss = { showBrushSizePicker = false },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            if (showSaveDialog) {
                SaveImageDialog(
                    onConfirm = {
                        showSaveDialog = false
                        viewModel.saveImageToGallery(
                            context = context,
                            width = canvasSize.width.toInt(),
                            height = canvasSize.height.toInt(),
                            onSuccess = { message -> saveResultMessage = message },
                            onError = { message -> saveResultMessage = message }
                        )
                    },
                    onDismiss = { showSaveDialog = false }
                )
            }
        }
    }
}

@Composable
fun SaveImageDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存图片") },
        text = { Text("是否保存当前画板内容到相册？") },
        confirmButton = {
            Button(onClick = onConfirm) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun MenuButton(onSaveClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("保存图片") },
                onClick = { expanded = false; onSaveClick() }
            )
            DropdownMenuItem(
                text = { Text("分享图片") },
                onClick = { expanded = false /* TODO 分享 */ }
            )
        }
    }
}

@Composable
fun DrawingCanvas(
    state: PainterState,
    onDrawStart: (Float, Float) -> Unit,
    onDrawMove: (Float, Float) -> Unit,
    onDrawEnd: () -> Unit,
    modifier: Modifier = Modifier,
    onSizeChanged: (Size) -> Unit = {}
) {
    val currentPath = remember { Path() }

    Box(
        modifier = modifier
            .background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPath.reset()
                        currentPath.moveTo(offset.x, offset.y)
                        onDrawStart(offset.x, offset.y)
                    },
                    onDrag = { change, _ ->
                        val newX = change.position.x
                        val newY = change.position.y
                        val prevX = change.previousPosition.x
                        val prevY = change.previousPosition.y
                        val midX = (prevX + newX) / 2
                        val midY = (prevY + newY) / 2
                        currentPath.quadraticTo(prevX, prevY, midX, midY)
                        onDrawMove(newX, newY)
                    },
                    onDragEnd = {
                        currentPath.reset()
                        onDrawEnd()
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size -> onSizeChanged(Size(size.width.toFloat(), size.height.toFloat())) }
        ) {
            // 绘制所有已完成的路径
            state.paths.forEach { drawPath ->
                drawPathOnCanvas(drawPath)
            }
            // 绘制当前正在绘制的路径
            state.currentPath?.let { drawPath ->
                drawPathOnCanvas(drawPath)
            }
        }
    }
}

// 抽取的绘图辅助函数（在 Canvas 内部使用）
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPathOnCanvas(drawPath: DrawPath) {
    if (drawPath.points.size < 2) return
    val path = Path()
    path.moveTo(drawPath.points[0].x, drawPath.points[0].y)
    for (i in 1 until drawPath.points.size - 1) {
        val curr = drawPath.points[i]
        val next = drawPath.points[i + 1]
        val midX = (curr.x + next.x) / 2
        val midY = (curr.y + next.y) / 2
        path.quadraticTo(curr.x, curr.y, midX, midY)
    }
    val last = drawPath.points.last()
    val secondLast = drawPath.points[drawPath.points.size - 2]
    path.quadraticTo(secondLast.x, secondLast.y, last.x, last.y)
    drawPath(
        path = path,
        color = drawPath.color,
        style = Stroke(
            width = drawPath.strokeWidth,
            cap = StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
    )
}

@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customColorHex by remember(currentColor) { mutableStateOf(currentColor.toHex()) }
    var showColorError by remember { mutableStateOf(false) }

    // RGB 滑块状态（使用 currentColor 作为 key 确保切换预设颜色时同步）
    var r by remember(currentColor) { mutableFloatStateOf(currentColor.red * 255) }
    var g by remember(currentColor) { mutableFloatStateOf(currentColor.green * 255) }
    var b by remember(currentColor) { mutableFloatStateOf(currentColor.blue * 255) }

    fun hexToColor(hex: String): Color? {
        return try {
            var hexColor = hex.trim().uppercase(Locale.getDefault())
            if (hexColor.isEmpty()) return null
            if (hexColor.startsWith("#")) hexColor = hexColor.substring(1)
            // 处理3位简写
            if (hexColor.length == 3) {
                hexColor = hexColor.map { c -> "$c$c" }.joinToString("")
            }
            if (hexColor.length != 6 && hexColor.length != 8) return null
            val colorInt = hexColor.toLong(16)
            if (hexColor.length == 6) {
                Color(android.graphics.Color.rgb(
                    (colorInt shr 16).toInt(),
                    (colorInt shr 8).toInt(),
                    colorInt.toInt()
                ))
            } else {
                Color(android.graphics.Color.argb(
                    (colorInt shr 24).toInt(),
                    ((colorInt shr 16) and 0xFF).toInt(),
                    ((colorInt shr 8) and 0xFF).toInt(),
                    (colorInt and 0xFF).toInt()
                ))
            }
        } catch (_: Exception) {
            null
        }
    }

    Card(modifier = modifier.fillMaxWidth(0.9f)) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("选择颜色", style = MaterialTheme.typography.titleMedium)

            // 预设颜色网格
            val colors = listOf(
                Color.Black, Color.White, Color.Red, Color.Green, Color.Blue,
                Color.Magenta, Color.Yellow, Color.Cyan, Color(0xFFFFA500),
                Color(0xFFA52A2A), Color.Gray, Color(0xFF800080)
            )
            colors.chunked(4).forEach { rowColors ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    rowColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(if (color == currentColor) 3.dp else 0.dp, Color.Black, CircleShape)
                                .clickable {
                                    onColorSelected(color)
                                    customColorHex = color.toHex()
                                    r = color.red * 255
                                    g = color.green * 255
                                    b = color.blue * 255
                                }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("自定义颜色", style = MaterialTheme.typography.titleSmall)

            // 颜色预览与HEX输入
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                )
                Column(Modifier.weight(1f)) {
                    Text("HEX 值", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = customColorHex,
                        onValueChange = {
                            customColorHex = it
                            showColorError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("#FF5733 或 FF5733") },
                        isError = showColorError,
                        supportingText = {
                            if (showColorError) Text("无效的颜色格式") else Text("支持 6 位或 8 位 HEX")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val color = hexToColor(customColorHex)
                            if (color != null) {
                                onColorSelected(color)
                                r = color.red * 255
                                g = color.green * 255
                                b = color.blue * 255
                            } else {
                                showColorError = true
                            }
                        }),
                        shape = MaterialTheme.shapes.small
                    )
                }
                IconButton(
                    onClick = {
                        val color = hexToColor(customColorHex)
                        if (color != null) {
                            onColorSelected(color)
                            r = color.red * 255
                            g = color.green * 255
                            b = color.blue * 255
                        } else {
                            showColorError = true
                        }
                    },
                    enabled = customColorHex.isNotBlank()
                ) {
                    Icon(Icons.Default.Check, contentDescription = "应用颜色")
                }
            }

            Text("RGB 选择", style = MaterialTheme.typography.titleSmall)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SliderWithLabel("红: ${r.toInt()}", r, 0f..255f) { r = it }
                SliderWithLabel("绿: ${g.toInt()}", g, 0f..255f) { g = it }
                SliderWithLabel("蓝: ${b.toInt()}", b, 0f..255f) { b = it }
                Button(
                    onClick = {
                        val newColor = Color(r / 255f, g / 255f, b / 255f)
                        onColorSelected(newColor)
                        customColorHex = newColor.toHex()
                    }
                ) {
                    Text("应用 RGB 颜色")
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Button(
                    onClick = {
                        val color = hexToColor(customColorHex)
                        if (color != null) {
                            onColorSelected(color)
                            onDismiss()
                        } else {
                            showColorError = true
                        }
                    },
                    enabled = customColorHex.isNotBlank() && !showColorError
                ) {
                    Text("确定")
                }
            }
        }
    }
}

@Composable
fun SliderWithLabel(label: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = Modifier.fillMaxWidth())
    }
}

fun Color.toHex(includeAlpha: Boolean = false): String {
    val alpha = (alpha * 255).toInt()
    val red = (red * 255).toInt()
    val green = (green * 255).toInt()
    val blue = (blue * 255).toInt()
    return if (includeAlpha) {
        String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
    } else {
        String.format("#%02X%02X%02X", red, green, blue)
    }
}

@Composable
fun BrushSizeDialog(
    currentSize: Float,
    onSizeChanged: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tempSize by remember { mutableFloatStateOf(currentSize) }
    Card(modifier = modifier.fillMaxWidth(0.9f)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("画笔大小", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
            Slider(value = tempSize, onValueChange = { tempSize = it }, valueRange = 1f..50f, steps = 49, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("当前大小: ${tempSize.toInt()}px", style = MaterialTheme.typography.bodyMedium)
            // 预览（实心圆）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    drawCircle(
                        color = Color.Black,
                        radius = tempSize / 2,
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(
                    onClick = {
                        onSizeChanged(tempSize)
                        onDismiss()
                    }
                ) { Text("确定") }
            }
        }
    }
}