@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.mine

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.example.toolbox.ApiAddress
import com.example.toolbox.TokenManager
import com.example.toolbox.community.uploadImage
import com.example.toolbox.R
import com.example.toolbox.ui.theme.ToolBoxTheme
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ResourceUploadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                ResourceUploadScreen(onFinish = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceUploadScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val token = TokenManager.get(context)
    val client = OkHttpClient()
    val coroutineScope = rememberCoroutineScope()

    // 表单状态
    var name by remember { mutableStateOf("") }
    var ver by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var dUrl by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf("") }

    // 分类状态 (Spinner)
    val categories = listOf(
        "其他",
        "开源软件",
        "实用工具",
        "生活便利",
        "影音娱乐",
        "玩机工具",
        "社交",
        "金融理财",
        "网页"
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    // 弹窗控制
    var showExitDialog by remember { mutableStateOf(false) }
    var showIconSheet by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            coroutineScope.launch {
                val url = token?.let { tk -> uploadImage(context, it, tk, 3) { _ -> } }
                if (url != null) {
                    appIcon = url
                    Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "上传失败", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 拦截返回键
    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("退出") },
            text = { Text("确定退出吗？你当前的投稿将不会保存。") },
            confirmButton = { TextButton(onClick = onFinish) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("点错了") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("资源投稿") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { showExitDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (name.isEmpty() || bio.isEmpty() || ver.isEmpty() || dUrl.isEmpty() || size.isEmpty()) {
                        Toast.makeText(context, "请填写完整！", Toast.LENGTH_SHORT).show()
                    } else {
                        val jsonObject = buildJsonObject {
                            put("name", name)
                            put("package_name", bio)
                            put("version", ver)
                            put("category_id", selectedIndex + 1)
                            put("download_url", dUrl)
                            put("size", size)
                            put("icon_url", appIcon.ifEmpty { "drawable/archive_blue.png" })
                        }
                        val body = jsonObject.toString().toRequestBody("application/json".toMediaType())
                        val request = token?.let {
                            Request.Builder()
                                .url(ApiAddress + "upload_resource")
                                .addHeader("x-access-token", it)
                        }
                            ?.post(body)
                            ?.build()
                        request?.let { client.newCall(it) }?.enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {}
                            override fun onResponse(call: Call, response: Response) {
                                (context as? ComponentActivity)?.runOnUiThread {
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "投稿成功", Toast.LENGTH_SHORT)
                                            .show()
                                        onFinish()
                                    }
                                }
                            }
                        })
                    }
                },
                icon = { Icon(Icons.Default.CloudUpload, null) },
                text = { Text("确认投稿") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(15.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = if (appIcon.isNotEmpty())
                        rememberAsyncImagePainter(appIcon)
                    else painterResource(R.drawable.resource),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .clickable { showIconSheet = true },
                    contentScale = ContentScale.Crop,
                    colorFilter = if (appIcon.isNotEmpty()) null else ColorFilter.tint(
                        MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.width(15.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("资源名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // 分类选择
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = categories[selectedIndex],
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("资源类型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        )
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEachIndexed { index, s ->
                        DropdownMenuItem(
                            text = { Text(s) },
                            onClick = {
                                selectedIndex = index
                                expanded = false
                            }
                        )
                    }
                }
            }

            // 版本和大小
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ver,
                    onValueChange = { ver = it },
                    label = { Text("版本号") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = size,
                    onValueChange = { size = it },
                    label = { Text("资源大小") },
                    modifier = Modifier.weight(1f)
                )
            }

            // 链接
            OutlinedTextField(
                value = dUrl,
                onValueChange = { dUrl = it },
                label = { Text("资源链接") },
                modifier = Modifier.fillMaxWidth()
            )

            // 简介
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("资源简介") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
            )
        }
    }

    // --- 各种弹窗 ---
    if (showIconSheet) {
        ModalBottomSheet(onDismissRequest = { showIconSheet = false }) {
            Column(modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 20.dp)) {
                ListItem(
                    headlineContent = { Text("上传图片") },
                    leadingContent = { Icon(Icons.Default.Photo, null) },
                    modifier = Modifier.clickable {
                        showIconSheet = false; photoPicker.launch("image/*")
                    })
                ListItem(
                    headlineContent = { Text("修改直链") },
                    leadingContent = { Icon(Icons.Default.Link, null) },
                    modifier = Modifier.clickable { showIconSheet = false; showUrlDialog = true })
            }
        }
    }

    if (showUrlDialog) {
        var tempUrl by remember { mutableStateOf(appIcon) }
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("图床链接") },
            text = {
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = { tempUrl = it },
                    label = { Text("输入链接") })
            },
            confirmButton = {
                Button(onClick = { appIcon = tempUrl; showUrlDialog = false }) {
                    Text(
                        "确定"
                    )
                }
            }
        )
    }
}