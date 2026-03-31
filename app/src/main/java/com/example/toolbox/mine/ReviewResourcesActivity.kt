@file:Suppress("PropertyName")

package com.example.toolbox.mine

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.TokenManager
import com.example.toolbox.data.mine.LocalResourceItem
import com.example.toolbox.data.mine.PendingResourceResponse
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ReviewResourcesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ToolBoxTheme {
                val context = LocalContext.current
                val token = TokenManager.get(context)

                // 状态管理
                var reports by remember { mutableStateOf(listOf<LocalResourceItem>()) }
                var isRefreshing by remember { mutableStateOf(false) }
                var selectedItem by remember { mutableStateOf<LocalResourceItem?>(null) }
                var showSheet by remember { mutableStateOf(false) }
                var showRejectDialog by remember { mutableStateOf(false) }

                // 封装刷新逻辑
                val refreshData = {
                    isRefreshing = true
                    token?.let {
                        fetchPendingResources(it) { list ->
                            reports = list ?: listOf()
                            isRefreshing = false
                            if (list == null) Toast.makeText(this, "加载失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // 初始加载
                LaunchedEffect(Unit) { refreshData() }

                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("待审核资源") },
                            navigationIcon = {
                                FilledTonalIconButton(onClick = { finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                            }
                        )
                    }
                ) { innerPadding ->
                    @OptIn(ExperimentalMaterial3Api::class)
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { refreshData() },
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    ) {
                        if (reports.isEmpty() && !isRefreshing) {
                            // --- 空状态显示大图标 ---
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Inbox,
                                        contentDescription = null,
                                        modifier = Modifier.size(120.dp),
                                        tint = Color.LightGray
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text("暂无待审核资源")
                                }
                            }
                        } else {
                            LazyColumn(Modifier.fillMaxSize()) {
                                items(reports) { item ->
                                    ResourceCard(item) {
                                        selectedItem = item
                                        showSheet = true
                                    }
                                }
                            }
                        }
                    }

                    // 底部操作菜单
                    if (showSheet && selectedItem != null) {
                        @OptIn(ExperimentalMaterial3Api::class)
                        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                            Column(Modifier.padding(16.dp).padding(bottom = 16.dp).fillMaxWidth()) {
                                Text(selectedItem!!.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        showSheet = false
                                        token?.let { approveResource(this@ReviewResourcesActivity, it, selectedItem!!.id, 1, null) { refreshData() } }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("通过审核") }

                                OutlinedButton(
                                    onClick = {
                                        showSheet = false
                                        showRejectDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) { Text("驳回资源", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }

                    // 驳回理由输入框
                    if (showRejectDialog && selectedItem != null) {
                        var reason by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showRejectDialog = false },
                            title = { Text("驳回理由") },
                            text = {
                                OutlinedTextField(
                                    value = reason,
                                    onValueChange = { reason = it },
                                    placeholder = { Text("请输入驳回原因...") }
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    showRejectDialog = false
                                    token?.let { approveResource(this@ReviewResourcesActivity, it, selectedItem!!.id, 2, reason) { refreshData() } }
                                }) { Text("确定") }
                            },
                            dismissButton = { TextButton(onClick = { showRejectDialog = false }) { Text("取消") } }
                        )
                    }
                }
            }
        }
    }
}

private val client = OkHttpClient()

fun fetchPendingResources(token: String, onResult: (List<LocalResourceItem>?) -> Unit) {
    val request = Request.Builder()
        .url(ApiAddress + "pending_resources")
        .addHeader("x-access-token", token)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Handler(Looper.getMainLooper()).post { onResult(null) }
        }
        override fun onResponse(call: Call, response: Response) {
            val body = response.body.string()
            val data = try {
                AppJson.json.decodeFromString<PendingResourceResponse>(body)
            } catch (_: Exception) { null }
            Handler(Looper.getMainLooper()).post { onResult(data?.pending_resources) }
        }
    })
}

fun approveResource(context: Context, token: String, id: Int, status: Int, reason: String?, onSuccess: () -> Unit) {
    val jsonObject = buildJsonObject {
        put("resource_id", id)
        put("status", status)
        reason?.let { put("reject_reason", it) }
    }
    val body = jsonObject.toString().toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url(ApiAddress + "approve_resource")
        .post(body)
        .addHeader("x-access-token", token)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
        override fun onResponse(call: Call, response: Response) {
            Handler(Looper.getMainLooper()).post {
                if (response.isSuccessful) {
                    Toast.makeText(context, if (status == 1) "已通过" else "已驳回", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "操作失败: ${response.code}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    })
}

@Composable
fun ResourceCard(item: LocalResourceItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(13.dp, 6.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0x157d7d7d)),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            // 图标加载 (需要 Coil 库)
            AsyncImage(
                model = item.icon_url,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(start = 9.dp).weight(1f)) {
                Text(item.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("上传者：${item.developer_name}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("分区：${item.category.name} 版本：${item.version}", fontSize = 13.sp)
                Text(item.package_name, fontSize = 12.sp, color = Color.Gray)
            }
            val statusText = when(item.status) {
                0 -> "待审核" to MaterialTheme.colorScheme.primary
                1 -> "已通过" to Color.Green
                else -> "已驳回" to MaterialTheme.colorScheme.error
            }
            Text(statusText.first, color = statusText.second, fontWeight = FontWeight.Bold)
        }
    }
}