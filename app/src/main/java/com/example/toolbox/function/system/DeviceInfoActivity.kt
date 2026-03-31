package com.example.toolbox.function.system

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.toolbox.ui.theme.ToolBoxTheme
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.os.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

// --- 数据模型 ---
data class BatteryInfo(
    val level: String, val health: String, val status: String,
    val voltage: String, val temperature: String, val technology: String
)

data class NetworkInfo(
    val type: String, val ipAddress: String, val wifiSsid: String, val macAddress: String
)

// --- 主 Activity ---
class DeviceInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("设备信息") },
                            navigationIcon = {
                                FilledTonalIconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    DeviceInfoTabScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// --- 分页 UI 主屏幕 ---
@Composable
fun DeviceInfoTabScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tabs = listOf("概览", "硬件", "系统", "网络/电池", "传感器")
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    // 数据采集
    val deviceInfo = remember { collectDeviceInfo(context) }
    val batteryInfo = rememberBatteryInfo(context)
    val networkInfo = remember { collectNetworkInfo(context) }
    val sensors = remember { (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getSensorList(Sensor.TYPE_ALL) }

    Column(modifier = modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { page ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (page) {
                    0 -> { // 概览
                        item {
                            InfoSection("核心摘要", Icons.Default.Info, Color(0xFF4285F4)) {
                                InfoRow("品牌型号", "${deviceInfo.brand} ${deviceInfo.model}")
                                InfoRow("Android版本", deviceInfo.androidVersion)
                                InfoRow("处理器", deviceInfo.cpuModel)
                                InfoRow("运行内存", "${deviceInfo.availableRam} / ${deviceInfo.totalRam}")
                            }
                        }
                    }
                    1 -> { // 硬件
                        item {
                            InfoSection("处理器与存储", Icons.Default.Memory, Color(0xFFEA4335)) {
                                InfoRow("架构", deviceInfo.cpuArchitecture)
                                InfoRow("核心数", deviceInfo.cpuCores)
                                InfoRow("内部存储", "${deviceInfo.availableInternalStorage} / ${deviceInfo.totalInternalStorage}")
                            }
                        }
                        item {
                            InfoSection("屏幕参数", Icons.Default.DisplaySettings, Color(0xFF8E44AD)) {
                                InfoRow("分辨率", deviceInfo.screenResolution)
                                InfoRow("刷新率", deviceInfo.refreshRate)
                                InfoRow("屏幕尺寸", deviceInfo.screenSize)
                                InfoRow("像素密度", deviceInfo.screenDensity)
                            }
                        }
                    }
                    2 -> { // 系统
                        item {
                            InfoSection("软件信息", Icons.Default.Android, Color(0xFF34A853)) {
                                InfoRow("API级别", deviceInfo.apiLevel)
                                InfoRow("构建ID", deviceInfo.buildId)
                                InfoRow("安全补丁", deviceInfo.securityPatch)
                                InfoRow("Root状态", deviceInfo.isRooted)
                            }
                        }
                    }
                    3 -> { // 网络与电池
                        item {
                            InfoSection("电池状态", Icons.Default.BatteryChargingFull, Color(0xFFF4B400)) {
                                InfoRow("当前电量", batteryInfo.level)
                                InfoRow("健康度", batteryInfo.health)
                                InfoRow("状态", batteryInfo.status)
                                InfoRow("温度", batteryInfo.temperature)
                                InfoRow("电压", batteryInfo.voltage)
                            }
                        }
                        item {
                            InfoSection("网络连接", Icons.Default.Wifi, Color(0xFF00ACC1)) {
                                InfoRow("网络类型", networkInfo.type)
                                InfoRow("IP地址", networkInfo.ipAddress)
                                InfoRow("WiFi SSID", networkInfo.wifiSsid)
                            }
                        }
                    }
                    4 -> { // 传感器
                        item { Text("检测到 ${sensors.size} 个传感器", style = MaterialTheme.typography.labelLarge) }
                        items(sensors) { sensor ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                ListItem(
                                    headlineContent = { Text(sensor.name) },
                                    supportingContent = { Text(sensor.vendor) },
                                    trailingContent = { Text("Type: ${sensor.type}") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 增强的数据采集逻辑 ---

@Composable
fun rememberBatteryInfo(context: Context): BatteryInfo {
    var batteryInfo by remember { mutableStateOf(BatteryInfo("加载中...", "-", "-", "-", "-", "-")) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0
                val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
                    BatteryManager.BATTERY_STATUS_FULL -> "已充满"
                    else -> "未知"
                }
                val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "损坏"
                    else -> "一般"
                }

                batteryInfo = BatteryInfo(
                    level = "$level%",
                    health = health,
                    status = status,
                    voltage = "$voltage mV",
                    temperature = "$temp ℃",
                    technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "未知"
                )
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return batteryInfo
}

private fun collectNetworkInfo(context: Context): NetworkInfo {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetworkInfo
    val type = activeNetwork?.typeName ?: "无连接"

    var ip = "未知"
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            val addrs = iface.inetAddresses
            while (addrs.hasMoreElements()) {
                val addr = addrs.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    ip = addr.hostAddress ?: "未知"
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }

    return NetworkInfo(type, ip, "需位置权限", "已隐藏")
}

// --- 复用之前的辅助函数 (getCpuName, checkRoot 等) ---
// [此处保留您原代码中的 getCpuName, getRefreshRate, safeGetSerial, checkRoot, checkEmulator 和 collectDeviceInfo 函数]
// 注意：collectDeviceInfo 内部逻辑保持不变，仅作为基础数据源。

// 1. 数据模型定义
data class DeviceInfo(
    val brand: String,
    val model: String,
    val manufacturer: String,
    val deviceId: String,
    val androidVersion: String,
    val apiLevel: String,
    val securityPatch: String,
    val buildId: String,
    val cpuArchitecture: String,
    val cpuCores: String,
    val cpuModel: String,
    val board: String,
    val totalRam: String,
    val availableRam: String,
    val totalInternalStorage: String,
    val availableInternalStorage: String,
    val screenResolution: String,
    val screenDensity: String,
    val screenSize: String,
    val refreshRate: String,
    val serialNumber: String,
    val isRooted: String,
    val isEmulator: String,
    val bootTime: String,
    val timezone: String
)

@Composable
fun InfoSection(title: String, icon: ImageVector, color: Color, content: @Composable () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color.copy(alpha = 0.1f), MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
            content()
        }
    }
}

@Composable
fun InfoRow(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = title,
            modifier = Modifier.width(130.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// 5. 数据采集核心逻辑
@SuppressLint("HardwareIds")
private fun collectDeviceInfo(context: Context): DeviceInfo {
    val metrics = context.resources.displayMetrics

    // 内存
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    am.getMemoryInfo(memInfo)

    // 存储
    val internalStat = StatFs(Environment.getDataDirectory().path)
    val totalInternal = (internalStat.blockCountLong * internalStat.blockSizeLong) / (1024 * 1024 * 1024)
    val availInternal = (internalStat.availableBlocksLong * internalStat.blockSizeLong) / (1024 * 1024 * 1024)

    // 屏幕尺寸
    val x = (metrics.widthPixels.toDouble() / metrics.xdpi).pow(2.0)
    val y = (metrics.heightPixels.toDouble() / metrics.ydpi).pow(2.0)
    val screenInches = String.format(Locale.US, "%.1f\"", sqrt(x + y))

    // 启动时间
    val uptime = SystemClock.elapsedRealtime()
    val hours = uptime / (1000 * 3600)
    val minutes = (uptime / (1000 * 60)) % 60
    val bootTimeStr = "${hours}小时 ${minutes}分"

    return DeviceInfo(
        brand = Build.BRAND,
        model = Build.MODEL,
        manufacturer = Build.MANUFACTURER,
        deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "未知",
        androidVersion = Build.VERSION.RELEASE,
        apiLevel = Build.VERSION.SDK_INT.toString(),
        securityPatch = Build.VERSION.SECURITY_PATCH,
        buildId = Build.ID,
        cpuArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "未知",
        cpuCores = Runtime.getRuntime().availableProcessors().toString(),
        cpuModel = getCpuName(),
        board = Build.BOARD,
        totalRam = "${memInfo.totalMem / (1024 * 1024)} MB",
        availableRam = "${memInfo.availMem / (1024 * 1024)} MB",
        totalInternalStorage = "$totalInternal GB",
        availableInternalStorage = "$availInternal GB",
        screenResolution = "${metrics.widthPixels} x ${metrics.heightPixels}",
        screenDensity = "${metrics.densityDpi} dpi",
        screenSize = screenInches,
        refreshRate = getRefreshRate(context),
        serialNumber = safeGetSerial(),
        isRooted = if (checkRoot()) "已Root" else "未Root",
        isEmulator = checkEmulator().toString(),
        bootTime = bootTimeStr,
        timezone = TimeZone.getDefault().id
    )
}

// 6. 辅助工具函数
private fun getCpuName(): String {
    return try {
        val reader = BufferedReader(FileReader("/proc/cpuinfo"))
        var line: String?
        var name = ""
        while (reader.readLine().also { line = it } != null) {
            if (line!!.contains("Hardware") || line.contains("model name")) {
                name = line.split(":")[1].trim()
                break
            }
        }
        reader.close()
        name.ifEmpty { Build.HARDWARE }
    } catch (_: Exception) {
        Build.HARDWARE
    }
}

private fun getRefreshRate(context: Context): String {
    return try {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION") wm.defaultDisplay
        }
        "${display?.refreshRate?.toInt() ?: 60} Hz"
    } catch (_: Exception) {
        "60 Hz"
    }
}

@SuppressLint("HardwareIds")
private fun safeGetSerial(): String {
    // 核心修复：Android 10+ (API 29) 绝对不能调用 Build.getSerial()
    if (Build.VERSION.SDK_INT >= 29) {
        return "系统已限制访问 (Android 10+)"
    }
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Build.getSerial()
        } else {
            @Suppress("DEPRECATION") Build.SERIAL
        }
    } catch (_: SecurityException) {
        "无权限"
    } catch (_: Exception) {
        "未知"
    }
}

private fun checkRoot(): Boolean {
    val paths = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su"
    )
    return try {
        paths.any { File(it).exists() }
    } catch (_: Exception) {
        false
    }
}

private fun checkEmulator(): Boolean {
    return (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || "google_sdk" == Build.PRODUCT
            || Build.HARDWARE == "goldfish"
            || Build.HARDWARE == "ranchu")
}