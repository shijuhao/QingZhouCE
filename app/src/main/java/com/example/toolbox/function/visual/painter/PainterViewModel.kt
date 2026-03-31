package com.example.toolbox.function.visual.painter

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Color as AndroidColor

data class DrawPoint(val x: Float, val y: Float)

data class DrawPath(
    val points: List<DrawPoint> = emptyList(),
    val strokeWidth: Float = 5f,
    val color: Color = Color.Black,
    val alpha: Float = 1f
)

data class PainterState(
    val paths: List<DrawPath> = emptyList(),
    val currentPath: DrawPath? = null,
    val currentColor: Color = Color.Black,
    val strokeWidth: Float = 5f,
    val isDrawing: Boolean = false,
    val undoStack: List<DrawPath> = emptyList(), // 已撤销的路径（用于重做）
    val redoStack: List<DrawPath> = emptyList()  // 已重做的路径（用于撤销）
)

class PainterViewModel : ViewModel() {
    private val _state = MutableStateFlow(PainterState())
    val state: StateFlow<PainterState> = _state.asStateFlow()

    private val _currentColor = mutableStateOf(Color.Black)
    val currentColor: State<Color> = _currentColor

    private val _currentStrokeWidth = mutableFloatStateOf(5f)
    val currentStrokeWidth: State<Float> = _currentStrokeWidth

    fun startNewPath(x: Float, y: Float) {
        viewModelScope.launch {
            val newPath = DrawPath(
                points = listOf(DrawPoint(x, y)),
                strokeWidth = _currentStrokeWidth.floatValue,
                color = _currentColor.value
            )
            _state.value = _state.value.copy(
                currentPath = newPath,
                isDrawing = true
            )
        }
    }

    fun addPointToCurrentPath(x: Float, y: Float) {
        if (!_state.value.isDrawing || _state.value.currentPath == null) return
        viewModelScope.launch {
            val currentState = _state.value
            val currentPath = currentState.currentPath!!
            val updatedPath = currentPath.copy(points = currentPath.points + DrawPoint(x, y))
            _state.value = currentState.copy(currentPath = updatedPath)
        }
    }

    fun finishPath() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.isDrawing && currentState.currentPath != null) {
                val newPaths = currentState.paths + currentState.currentPath
                _state.value = currentState.copy(
                    paths = newPaths,
                    currentPath = null,
                    isDrawing = false,
                    redoStack = emptyList() // 新操作清空重做栈
                )
            }
        }
    }

    fun setColor(color: Color) {
        viewModelScope.launch {
            _currentColor.value = color
            _state.value = _state.value.copy(currentColor = color)
        }
    }

    fun setStrokeWidth(width: Float) {
        viewModelScope.launch {
            _currentStrokeWidth.floatValue = width
            _state.value = _state.value.copy(strokeWidth = width)
        }
    }

    fun clearCanvas() {
        viewModelScope.launch {
            val currentPaths = _state.value.paths
            _state.value = PainterState(
                currentColor = _currentColor.value,
                strokeWidth = _currentStrokeWidth.floatValue,
                undoStack = currentPaths,      // 将当前所有路径存入撤销栈（可重做）
                redoStack = emptyList()
            )
        }
    }

    fun undo() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.paths.isNotEmpty()) {
                val lastPath = currentState.paths.last()
                val newPaths = currentState.paths.dropLast(1)
                val newRedoStack = currentState.redoStack + lastPath
                _state.value = currentState.copy(
                    paths = newPaths,
                    redoStack = newRedoStack
                )
            }
        }
    }

    fun redo() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.redoStack.isNotEmpty()) {
                val lastRedo = currentState.redoStack.last()
                val newPaths = currentState.paths + lastRedo
                val newRedoStack = currentState.redoStack.dropLast(1)
                _state.value = currentState.copy(
                    paths = newPaths,
                    redoStack = newRedoStack
                )
            }
        }
    }

    fun saveImageToGallery(
        context: Context,
        width: Int,
        height: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            saveImage(context, width, height, onSuccess, onError)
        }
    }

    private suspend fun saveImage(
        context: Context,
        width: Int,
        height: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val bitmap = createBitmapFromPaths(width, height)
                val savedUri = saveBitmapToGallery(context, bitmap)
                withContext(Dispatchers.Main) {
                    onSuccess("图片已保存到相册")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("保存失败: ${e.message}")
                }
            }
        }
    }

    private fun createBitmapFromPaths(width: Int, height: Int): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(AndroidColor.WHITE)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
        val currentState = _state.value
        currentState.paths.forEach { drawPath ->
            drawPathToAndroidCanvas(drawPath, canvas, paint)
        }
        currentState.currentPath?.let { drawPath ->
            drawPathToAndroidCanvas(drawPath, canvas, paint)
        }
        return bitmap
    }

    private fun drawPathToAndroidCanvas(
        drawPath: DrawPath,
        canvas: Canvas,
        paint: android.graphics.Paint
    ) {
        if (drawPath.points.size < 2) return
        paint.color = drawPath.color.toArgb()
        paint.strokeWidth = drawPath.strokeWidth
        paint.alpha = (drawPath.alpha * 255).toInt()
        val androidPath = android.graphics.Path()
        androidPath.moveTo(drawPath.points[0].x, drawPath.points[0].y)
        for (i in 1 until drawPath.points.size) {
            androidPath.lineTo(drawPath.points[i].x, drawPath.points[i].y)
        }
        canvas.drawPath(androidPath, paint)
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): String {
        val filename = "Painter_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.png"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ToolBox")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                "保存成功: $filename"
            } ?: "保存失败"
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val toolBoxDir = File(picturesDir, "ToolBox")
            if (!toolBoxDir.exists()) toolBoxDir.mkdirs()
            val imageFile = File(toolBoxDir, filename)
            FileOutputStream(imageFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            context.sendBroadcast(android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, android.net.Uri.fromFile(imageFile)))
            "保存成功: ${imageFile.absolutePath}"
        }
    }
}