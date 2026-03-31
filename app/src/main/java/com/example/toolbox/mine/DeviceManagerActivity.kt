@file:Suppress("PropertyName")

package com.example.toolbox.mine

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.TokenManager
import com.example.toolbox.data.mine.Device
import com.example.toolbox.data.mine.DeviceResponse
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.serialization.decodeFromString
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class DeviceManagerActivity : ComponentActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                DeviceManagerScreen(
                    onBack = { finish() },
                    fetchDevices = ::fetchDevices,
                    revokeDevice = ::revokeDevice
                )
            }
        }
    }

    // 获取设备列表
    private fun fetchDevices(token: String, callback: (List<Device>?, String?) -> Unit) {
        val request = Request.Builder()
            .url("${ApiAddress}get_devices")
            .addHeader("x-access-token", token)
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body.string()
                if (response.isSuccessful) {
                    val data = AppJson.json.decodeFromString<DeviceResponse>(body)
                    callback(data.devices, null)
                } else {
                    callback(null, "错误码: ${response.code}")
                }
            }
        })
    }

    // 踢出设备
    private fun revokeDevice(token: String, deviceId: String, callback: (Boolean, String?) -> Unit) {
        val json = AppJson.json.encodeToString(mapOf("device_id" to deviceId))
        val request = Request.Builder()
            .url("${ApiAddress}revoke_device")
            .addHeader("x-access-token", token)
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful, if (response.isSuccessful) null else "失败")
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagerScreen(
    onBack: () -> Unit,
    fetchDevices: (String, (List<Device>?, String?) -> Unit) -> Unit,
    revokeDevice: (String, String, (Boolean, String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val token = TokenManager.get(context)

    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<Device?>(null) }

    // 定义刷新逻辑
    val onRefresh: () -> Unit = {
        isRefreshing = true
        token?.let {
            fetchDevices(it) { list, err ->
                isRefreshing = false
                if (list != null) {
                    devices = list
                } else {
                    Toast.makeText(context, "刷新失败: $err", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 首次进入自动触发刷新
    LaunchedEffect(Unit) {
        onRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录设备管理") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        // 使用 PullToRefreshBox 包裹列表
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
        ) {
            if (devices.isEmpty() && !isRefreshing) {
                // 空状态展示
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无登录设备", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(devices) { device ->
                        DeviceItem(
                            device = device,
                            onClick = { selectedDevice = device },
                            onKick = {
                                // 踢出设备逻辑
                                token?.let {
                                    revokeDevice(it, device.device_id) { success, err ->
                                        if (success) {
                                            // 踢出成功后刷新列表
                                            onRefresh()
                                        } else {
                                            // 切换回主线程显示 Toast (OkHttp回调在子线程)
                                            (context as? ComponentActivity)?.runOnUiThread {
                                                Toast.makeText(context, "操作失败: $err", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
                    }
                }
            }
        }

        // 详情弹窗 (保持不变)
        selectedDevice?.let { device ->
            AlertDialog(
                onDismissRequest = { selectedDevice = null },
                title = { Text("设备详情") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow("📱 型号", device.device_model)
                        DetailRow("🆔 ID", device.device_id)
                        DetailRow("🌐 IP地址", device.ip_address)
                        DetailRow("📍 IP属地", device.ip_location)
                        DetailRow("🕐 登录时间", device.login_time)
                        DetailRow("🕒 最后活动", device.last_activity)
                        DetailRow("🔋 状态", if (device.is_active) "在线" else "离线")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedDevice = null }) { Text("关闭") }
                }
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(value, fontSize = 14.sp)
    }
}

@Composable
fun DeviceItem(device: Device, onClick: () -> Unit, onKick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            // 对应 Lua 的 主题色-0xde000000 背景效果
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 第一行：图标 + 型号 + 当前设备标签
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp).padding(4.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (device.device_model == "unknown") "未知设备" else device.device_model,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1
                        )
                        if (device.is_current) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "当前设备",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "📱 ${device.device_id}\n📍 ${device.ip_location}",
                        fontSize = 13.sp,
                        modifier = Modifier.alpha(0.8f).padding(top = 4.dp)
                    )
                }
            }

            // 第二行：在线状态 + 踢出按钮
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = if (device.is_active) Color(0xFF9E9E9E) else Color(0xFF4CAF50)
                Text(
                    text = if (device.is_active) "🟢 在线" else "🔴 离线",
                    color = statusColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(6.dp)
                )

                if (!device.is_current) {
                    Surface(
                        modifier = Modifier.clickable { onKick() },
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "踢出设备",
                            color = MaterialTheme.colorScheme.onError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // 第三行：时间信息 (对应 Lua 的 time_info)
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "🕐 登录: ${formatTime(device.login_time)}\n🕒 最后活动: ${formatTime(device.last_activity)}",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.alpha(0.8f)
                )
            }
        }
    }
}

// 对应 Lua 的 formatTime 函数
fun formatTime(timeStr: String?): String {
    if (timeStr == null) return "未知时间"
    return timeStr.replace("-", "年", ignoreCase = false).replaceFirst("-", "月").replace(" ", "日 ")
}