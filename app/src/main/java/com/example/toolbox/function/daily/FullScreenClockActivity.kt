package com.example.toolbox.function.daily

import android.Manifest
import android.content.Context.MODE_PRIVATE
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FullScreenClockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        enableEdgeToEdge()

        setContent {
            ToolBoxTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    ClockScreen()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
fun ClockScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val sharedPrefs = remember { context.getSharedPreferences("clock_prefs", MODE_PRIVATE) }

    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var currentDate by remember { mutableStateOf(getCurrentDate()) }

    val (permissionGranted, setPermissionGranted) = remember { mutableStateOf(false) }
    val (showRationale, setShowRationale) = remember { mutableStateOf(false) }

    val screenWidth = context.resources.displayMetrics.widthPixels
    val screenHeight = context.resources.displayMetrics.heightPixels

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        setPermissionGranted(allGranted)
        if (!allGranted) {
            val shouldShowRationale = getStoragePermissions().any { permission ->
                activity?.shouldShowRequestPermissionRationale(permission) == true
            }
            setShowRationale(shouldShowRationale)
            if (!shouldShowRationale) {
                Toast.makeText(context, "请在设置中授予存储权限", Toast.LENGTH_LONG).show()
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                inputStream?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                        BitmapFactory.decodeStream(stream, null, this)
                        val scale = maxOf(outWidth / screenWidth, outHeight / screenHeight)
                        inSampleSize = if (scale > 1) scale else 1
                        inJustDecodeBounds = false
                    }
                    context.contentResolver.openInputStream(it)?.use { newStream ->
                        val bitmap = BitmapFactory.decodeStream(newStream, null, options)
                        if (bitmap != null) {
                            val savedPath = saveBitmapToPrivateStorage(context, bitmap)
                            if (savedPath != null) {
                                sharedPrefs.edit {
                                    putString("FullScreenClock_Background", savedPath)
                                }
                                backgroundBitmap = bitmap
                                Toast.makeText(context, "背景已更换", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "保存背景失败", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "无法读取图片", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "选择图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val savedPath = sharedPrefs.getString("FullScreenClock_Background", null)
        if (savedPath != null) {
            val file = File(savedPath)
            if (file.exists()) {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeFile(savedPath, this)
                    val scale = maxOf(outWidth / screenWidth, outHeight / screenHeight)
                    inSampleSize = if (scale > 1) scale else 1
                    inJustDecodeBounds = false
                }
                val bitmap = BitmapFactory.decodeFile(savedPath, options)
                if (bitmap != null) {
                    backgroundBitmap = bitmap
                } else {
                    sharedPrefs.edit { remove("FullScreenClock_Background") }
                    Toast.makeText(context, "背景文件已损坏", Toast.LENGTH_SHORT).show()
                }
            } else {
                sharedPrefs.edit { remove("FullScreenClock_Background") }
            }
        } else {
            Toast.makeText(context, "长按选择背景", Toast.LENGTH_SHORT).show()
        }

        setPermissionGranted(checkPermissionStatus(context))

        while (true) {
            currentTime = getCurrentTime()
            currentDate = getCurrentDate()
            delay(1000)
        }
    }

    // 使用 BoxWithConstraints 获取屏幕约束，动态计算字体大小
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        if (permissionGranted) {
                            imagePickerLauncher.launch("image/*")
                        } else {
                            if (showRationale) {
                                setShowRationale(true)
                            } else {
                                permissionLauncher.launch(getStoragePermissions())
                            }
                        }
                    }
                )
            }
    ) {
        val timeFontSize = (maxWidth.value * 0.15).sp   // 系数 0.15 可调整
        val dateFontSize = (timeFontSize.value * 0.3).sp

        // 背景图片
        backgroundBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.4f),
                contentScale = ContentScale.Crop
            )
        }

        // 时间日期显示
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BasicText(
                text = currentTime,
                style = TextStyle(
                    fontSize = timeFontSize,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            BasicText(
                text = currentDate,
                style = TextStyle(
                    fontSize = dateFontSize,
                    color = Color.White,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // 权限解释对话框（与之前相同）
        if (showRationale) {
            AlertDialog(
                onDismissRequest = { setShowRationale(false) },
                title = { Text("需要存储权限") },
                text = { Text("需要存储权限来选择背景图片。请授予权限以继续。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            setShowRationale(false)
                            permissionLauncher.launch(getStoragePermissions())
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { setShowRationale(false) }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

// 获取存储权限列表
private fun getStoragePermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

// 检查权限状态
private fun checkPermissionStatus(context: android.content.Context): Boolean {
    return getStoragePermissions().all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

// 保存 Bitmap 到私有存储
private fun saveBitmapToPrivateStorage(context: android.content.Context, bitmap: Bitmap): String? {
    return try {
        val imagesDir = File(context.filesDir, "backgrounds").apply { mkdirs() }
        val file = File(imagesDir, "background_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// 获取当前时间 HH:mm:ss
private fun getCurrentTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}

// 获取当前日期和星期
private fun getCurrentDate(): String {
    val date = Date()
    val dateStr = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(date)
    val calendar = Calendar.getInstance().apply { time = date }
    val weekDay = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "星期日"
        Calendar.MONDAY -> "星期一"
        Calendar.TUESDAY -> "星期二"
        Calendar.WEDNESDAY -> "星期三"
        Calendar.THURSDAY -> "星期四"
        Calendar.FRIDAY -> "星期五"
        Calendar.SATURDAY -> "星期六"
        else -> ""
    }
    return "$dateStr    $weekDay"
}

@Preview(showBackground = true)
@Composable
fun ClockScreenPreview() {
    ToolBoxTheme {
        ClockScreen()
    }
}