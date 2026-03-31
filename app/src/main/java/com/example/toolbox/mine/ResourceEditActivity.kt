@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.mine

import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.example.toolbox.ApiAddress
import androidx.compose.ui.graphics.ColorFilter
import com.example.toolbox.AppJson
import com.example.toolbox.ui.theme.ToolBoxTheme
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.example.toolbox.R
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class ResourceEditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 接收来自列表页传递的参数 (对应 Lua 的 传递参数[1-9])
        val token = intent.getStringExtra("token") ?: ""
        val resourceId = intent.getStringExtra("resource_id") ?: ""
        val initialName = intent.getStringExtra("name") ?: ""
        val initialBio = intent.getStringExtra("bio") ?: ""
        val initialVer = intent.getStringExtra("ver") ?: ""
        val initialUrl = intent.getStringExtra("d_url") ?: ""
        val initialSize = intent.getStringExtra("size") ?: ""
        val initialIcon = intent.getStringExtra("icon") ?: ""
        val initialFqid = intent.getStringExtra("fqid") ?: "1" // 分区ID

        setContent {
            ToolBoxTheme {
                ResourceEditScreen(
                    token = token,
                    resourceId = resourceId,
                    initialName = initialName,
                    initialBio = initialBio,
                    initialVer = initialVer,
                    initialUrl = initialUrl,
                    initialSize = initialSize,
                    initialIcon = initialIcon,
                    initialFqid = initialFqid,
                    onFinish = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceEditScreen(
    token: String,
    resourceId: String,
    initialName: String,
    initialBio: String,
    initialVer: String,
    initialUrl: String,
    initialSize: String,
    initialIcon: String,
    initialFqid: String,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val client = OkHttpClient()

    // 初始化状态 (对应 Lua 的赋值逻辑)
    var name by remember { mutableStateOf(initialName) }
    var ver by remember { mutableStateOf(initialVer) }
    var size by remember { mutableStateOf(initialSize) }
    var dUrl by remember { mutableStateOf(initialUrl) }
    var bio by remember { mutableStateOf(initialBio) }
    var appIcon by remember { mutableStateOf(initialIcon) }

    // 分类状态
    val categories = listOf("其他", "开源软件", "实用工具", "生活便利", "影音娱乐", "玩机工具", "社交", "金融理财", "网页")
    var expanded by remember { mutableStateOf(false) }
    // 初始选择索引 (fqid - 1)
    var selectedIndex by remember { mutableIntStateOf((initialFqid.toIntOrNull() ?: 1) - 1) }

    // 弹窗控制
    var showExitDialog by remember { mutableStateOf(false) }
    var showIconSheet by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }

    // 图片上传逻辑
    val uploadImage: (Uri) -> Unit = { uri ->
        val file = File(context.cacheDir, "temp_edit_icon.png")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("status", "1")
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaType()))
            .build()
        client.newCall(Request.Builder().url(ApiAddress + "upload_image").post(requestBody).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body.string()
                (context as? ComponentActivity)?.runOnUiThread {
                    val map = try {
                        resStr.let { AppJson.json.parseToJsonElement(it) }.jsonObject
                    } catch (_: Exception) {
                        null
                    }
                    (map?.get("image_url")?.jsonPrimitive?.contentOrNull)?.let {
                        appIcon = it
                        Toast.makeText(context, "修改成功", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { uploadImage(it) } }

    // 返回键拦截
    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("退出") },
            text = { Text("确定退出吗？你所做的更改将不会保存。") },
            confirmButton = { TextButton(onClick = onFinish) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("点错了") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑资源") },
                navigationIcon = { FilledTonalIconButton(onClick = { showExitDialog = true }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (name.isEmpty() || bio.isEmpty() || ver.isEmpty() || dUrl.isEmpty() || size.isEmpty()) {
                        Toast.makeText(context, "请填写完整！", Toast.LENGTH_SHORT).show()
                    } else {
                        val jsonObject = buildJsonObject {
                            put("resource_id", resourceId)
                            put("name", name)
                            put("package_name", bio)
                            put("version", ver)
                            put("category_id", selectedIndex + 1)
                            put("download_url", dUrl)
                            put("size", size)
                            put("icon_url", appIcon.ifEmpty { "drawable/archive_blue.png" })
                        }
                        val body = jsonObject.toString().toRequestBody("application/json".toMediaType())
                        val request = Request.Builder()
                            .url(ApiAddress + "edit_resource")
                            .addHeader("x-access-token", token)
                            .post(body)
                            .build()
                        client.newCall(request).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {}
                            override fun onResponse(call: Call, response: Response) {
                                val resStr = response.body.string()
                                (context as? ComponentActivity)?.runOnUiThread {
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "修改成功", Toast.LENGTH_SHORT).show()
                                        onFinish()
                                    } else {
                                        Toast.makeText(context, "失败: $resStr", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })
                    }
                },
                icon = { Icon(Icons.Default.Check, null) },
                text = { Text("保存修改") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(15.dp).verticalScroll(rememberScrollState()).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // 图标和名称
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
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("资源名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
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
                        DropdownMenuItem(text = { Text(s) }, onClick = { selectedIndex = index; expanded = false })
                    }
                }
            }

            // 版本和大小
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = ver, onValueChange = { ver = it }, label = { Text("版本号") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = size, onValueChange = { size = it }, label = { Text("资源大小") }, modifier = Modifier.weight(1f))
            }

            // 链接
            OutlinedTextField(value = dUrl, onValueChange = { dUrl = it }, label = { Text("资源链接") }, modifier = Modifier.fillMaxWidth())

            // 简介
            OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("资源简介") }, modifier = Modifier.fillMaxWidth().height(150.dp))
        }
    }

    // 底部弹窗 (修改方式)
    if (showIconSheet) {
        ModalBottomSheet(onDismissRequest = { showIconSheet = false }) {
            Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 20.dp)) {
                ListItem(headlineContent = { Text("上传图片") }, leadingContent = { Icon(Icons.Default.Photo, null) },
                    modifier = Modifier.clickable { showIconSheet = false; photoPicker.launch("image/*") })
                ListItem(headlineContent = { Text("修改直链") }, leadingContent = { Icon(Icons.Default.Link, null) },
                    modifier = Modifier.clickable { showIconSheet = false; showUrlDialog = true })
            }
        }
    }

    // 直链输入弹窗
    if (showUrlDialog) {
        var tempUrl by remember { mutableStateOf(appIcon) }
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("图床链接") },
            text = {
                Column {
                    OutlinedTextField(value = tempUrl, onValueChange = { tempUrl = it }, label = { Text("输入链接") })
                    Text("请填写有效图床链接，否则审核不予通过。", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { Button(onClick = { appIcon = tempUrl; showUrlDialog = false }) { Text("确定") } }
        )
    }
}