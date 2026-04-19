@file:Suppress("PropertyName")

package com.example.toolbox.mine

import android.content.Intent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.toolbox.data.mine.ResourceItem
import com.example.toolbox.data.mine.ResourceResponse
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.serialization.decodeFromString
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MyResourceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ToolBoxTheme {
                MyResourceScreen(onBack = { finish() })
            }
        }
    }
}

// --- UI Screen 函数 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyResourceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val token = TokenManager.get(context)

    val resourceList = remember { mutableStateListOf<ResourceItem>() }
    var isRefreshing by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ResourceItem?>(null) }

    val client = OkHttpClient()

    val refreshData = {
        isRefreshing = true
        val request = token?.let {
            Request.Builder()
                .url(ApiAddress + "user_resources")
                .addHeader("x-access-token", it)
        }
            ?.build()

        request?.let { client.newCall(it) }?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                isRefreshing = false
            }
            override fun onResponse(call: Call, response: Response) {
                val json = response.body.string()
                (context as? ComponentActivity)?.runOnUiThread {
                    if (response.isSuccessful) {
                        val data = AppJson.json.decodeFromString<ResourceResponse>(json)
                        resourceList.clear()
                        resourceList.addAll(data.user_resources)
                    }
                    isRefreshing = false
                }
            }
        })
    }

    // 定义删除逻辑
    val deleteItem: (Int) -> Unit = { id ->
        val jsonBody = AppJson.json.encodeToString(mapOf("resource_id" to id, "action" to 1))
        val body = jsonBody.toRequestBody("application/json".toMediaType())
        val request = token?.let {
            Request.Builder()
                .url(ApiAddress + "delete_resource")
                .addHeader("x-access-token", it)
        }
            ?.post(body)
            ?.build()

        request?.let { client.newCall(it) }?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                (context as? ComponentActivity)?.runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                        refreshData()
                    }
                }
            }
        })
    }

    // 初始加载
    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的资源") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FloatingActionButton(onClick = {
                    val intent = Intent(context, ResourceSourceActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.AppShortcut, contentDescription = null)
                }
                FloatingActionButton(onClick = {
                    val intent = Intent(context, ResourceUploadActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { innerPadding ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refreshData() },
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()).fillMaxSize()
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(resourceList) { item ->
                    ResourceItemCard(item) {
                        selectedItem = item
                        showSheet = true
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
                }
            }
        }

        if (showSheet && selectedItem != null) {
            ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                    ListItem(
                        headlineContent = { Text("编辑资源") },
                        leadingContent = { Icon(Icons.Default.Edit, null) },
                        modifier = Modifier.clickable {
                            showSheet = false
                            selectedItem?.let { item ->
                                val intent = Intent(context, ResourceEditActivity::class.java).apply {
                                    putExtra("token", token)
                                    putExtra("resource_id", item.id.toString())
                                    putExtra("name", item.name)
                                    putExtra("description", item.description)
                                    putExtra("bio", item.description)
                                    putExtra("ver", item.version)
                                    putExtra("d_url", item.downloadUrl)
                                    putExtra("size", item.size)
                                    putExtra("icon", item.iconUrl)
                                    putExtra("fqid", item.category.id.toString())
                                }
                                context.startActivity(intent)
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("删除资源", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            showSheet = false
                            deleteItem(selectedItem!!.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ResourceItemCard(item: ResourceItem, onClick: () -> Unit) {
    val (statusText, statusColor) = when (item.status) {
        0 -> "审核中" to Color(0xFF4D75FF)
        1 -> "已通过" to Color(0xFF00FF3F)
        else -> "被驳回" to Color(0xFFFF0000)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 13.dp, vertical = 6.dp),
        shape = RoundedCornerShape(15.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.iconUrl,
                contentDescription = null,
                modifier = Modifier.size(45.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(text = item.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(text = "上传时间：${item.releaseDate}", fontSize = 12.sp, color = Color.Gray)
            }

            Text(text = statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
