@file:Suppress("PropertyName")

package com.example.toolbox.mine

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.TokenManager
import com.example.toolbox.data.mine.ReportItem
import com.example.toolbox.data.mine.ReportResponse
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.serialization.decodeFromString
import okhttp3.*
import java.io.IOException

class ViewReportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val token = TokenManager.get(this) ?: ""

        setContent {
            ToolBoxTheme {
                // 状态管理
                var reports by remember { mutableStateOf(listOf<ReportItem>()) }
                var isRefreshing by remember { mutableStateOf(false) }

                // 初始加载
                LaunchedEffect(Unit) {
                    isRefreshing = true
                    fetchReports(token) { data ->
                        if (data != null) reports = data
                        isRefreshing = false
                    }
                }

                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("所有举报") },
                            navigationIcon = {
                                FilledTonalIconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    // 使用 PullToRefreshBox 包裹列表
                    @OptIn(ExperimentalMaterial3Api::class)
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            fetchReports(token) { data ->
                                if (data != null) reports = data
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        if (reports.isEmpty() && !isRefreshing) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("暂无数据")
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(reports) { report ->
                                    ReportItemCard(report)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // OkHttp 网络请求
    private fun fetchReports(token: String, onResult: (List<ReportItem>?) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(ApiAddress + "admin/reports")
            .addHeader("x-access-token", token)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ViewReportActivity, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()
                if (response.isSuccessful && bodyString != null) {
                    try {
                        val data = AppJson.json.decodeFromString<ReportResponse>(bodyString)
                        runOnUiThread { onResult(data.reports) }
                    } catch (_: Exception) {
                        runOnUiThread { onResult(null) }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ViewReportActivity, "错误: ${response.code}", Toast.LENGTH_SHORT).show()
                        onResult(null)
                    }
                }
            }
        })
    }
}

@Composable
fun ReportItemCard(report: ReportItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 13.dp, vertical = 6.dp),
        shape = RoundedCornerShape(15.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(15.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = report.content ?: "无内容",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "类型：${report.report_type} | 举报者：${report.reporter.username}",
                fontSize = 13.sp
            )
            Text(
                text = "被举报：${report.reported_user.username}",
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "时间：${report.report_time}",
                fontSize = 12.sp
            )
        }
    }
}