@file:Suppress("PropertyName", "AssignedValueIsNeverRead")

package com.example.toolbox.community

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.TokenManager
import com.example.toolbox.data.community.LocalApiResponse
import com.example.toolbox.data.community.MessageInfo
import com.example.toolbox.data.community.ReferencedMessageInfo
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import androidx.core.graphics.createBitmap
import coil3.compose.rememberAsyncImagePainter
import coil3.request.bitmapConfig
import coil3.request.crossfade
import com.example.toolbox.AppJson
import com.example.toolbox.R
import com.example.toolbox.data.community.LocalEditRecord
import com.example.toolbox.utils.MarkdownRenderer
import com.example.toolbox.utils.MultiImageViewer
import java.io.File
import androidx.compose.ui.viewinterop.AndroidView
import coil3.request.allowHardware

object AppImageLoaders {
    private var _coil3Loader: coil3.ImageLoader? = null

    fun getCoil3Loader(context: Context): coil3.ImageLoader {
        return _coil3Loader ?: synchronized(this) {
            _coil3Loader ?: coil3.ImageLoader.Builder(context)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .crossfade(true)
                .allowHardware(false)
                .build().also { _coil3Loader = it }
        }
    }
}

@Composable
fun MarkdownText(content: String, modifier: Modifier = Modifier) {
    MarkdownRenderer.Render(
        modifier = modifier,
        content = content
    )
}

class PostDetailActivity : ComponentActivity() {
    private var refreshTrigger by mutableIntStateOf(0)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val msgId = parseMessageId(intent)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                if (msgId == 0) {
                    LaunchedEffect(Unit) {
                        Toast.makeText(this@PostDetailActivity, "无效的消息ID", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }
                } else {
                    PostDetailScreen(
                        messageId = msgId,
                        refreshTrigger = refreshTrigger,
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    private fun parseMessageId(intent: Intent): Int {
        intent.getIntExtra("msgid", 0).takeIf { it != 0 }?.let { return it }
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data!!
            uri.getQueryParameter("id")?.toIntOrNull()?.let { return it }
        }
        return 0
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == RESULT_OK) {
            Toast.makeText(this, "发布成功", Toast.LENGTH_SHORT).show()
            refreshTrigger++
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    messageId: Int,
    refreshTrigger: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val token = TokenManager.get(context)
    var uiState by remember { mutableStateOf<MessageInfo?>(null) }
    var errorState by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val viewModel: CommunityViewModel = viewModel()
    var messageToDelete by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    var showShareSheet by remember { mutableStateOf(false) }

    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                pendingBitmap?.let { bitmap ->
                    scope.launch {
                        saveBitmapToGallery(context, bitmap)
                        pendingBitmap = null
                    }
                }
            } else {
                Toast.makeText(context, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show()
                pendingBitmap = null
            }
        }
    )

    var showEditDialog by remember { mutableStateOf(false) }
    var editRecords by remember { mutableStateOf<List<LocalEditRecord>>(emptyList()) }

    // 图片查看器状态
    var showImageViewer by remember { mutableStateOf(false) }
    var imageViewerUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var imageViewerInitialPage by remember { mutableIntStateOf(0) }

    fun load() {
        val client = OkHttpClient()
        val headers = token?.let {
            Headers.Builder()
                .add("Content-Type", "application/json")
                .add("x-access-token", it)
        }?.build()
        val request = headers?.let {
            Request.Builder()
                .url("${ApiAddress}get_message_info")
                .post(
                    JSONObject().apply { put("message_id", messageId) }.toString()
                        .toRequestBody("application/json".toMediaType())
                )
                .headers(it)
        }?.build()

        request?.let { client.newCall(it) }?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                errorState = "网络错误: ${e.message}"
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        errorState = "服务器错误: ${it.code}"; return
                    }
                    val res = it.body.string()
                        .let { string -> AppJson.json.decodeFromString<LocalApiResponse>(string) }
                    if (res.success) uiState = res.message_info else errorState = res.message
                }
            }
        })
    }

    LaunchedEffect(messageId, refreshTrigger) { load() }

    fun editMessage() {
        val intent = Intent(context, PostArticleActivity::class.java).apply {
            uiState?.let {
                putExtra("edit_message_id", it.message_id)
                putExtra("userId", TokenManager.getUserID(context))
                putExtra("old_title", it.content.title ?: "")
                putExtra("old_content", it.content.text ?: it.content.content ?: "")
                putStringArrayListExtra("old_images", ArrayList(it.content.images ?: emptyList()))
                putIntegerArrayListExtra("old_private", ArrayList(it.visible_to ?: emptyList()))
                putExtra("old_is_markdown", it.is_markdown)
            }
        }
        (context as Activity).startActivityForResult(intent, 123)
    }

    if (messageToDelete) {
        AlertDialog(
            onDismissRequest = { messageToDelete = false },
            title = { Text("删除确认") },
            text = { Text("确定要删除这条留言吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        token?.let { tk ->
                            uiState?.let { info ->
                                viewModel.deleteMessage(
                                    token = tk,
                                    messageId = info.message_id,
                                    onError = { err ->
                                        Toast.makeText(
                                            context,
                                            "删除失败: $err",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    })
                            }
                        }
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                        (context as Activity).finish()
                    }) { Text("确定删除") }
            },
            dismissButton = { TextButton(onClick = { messageToDelete = false }) { Text("取消") } }
        )
    }

    if (showEditDialog) {
        EditRecordsDialog(
            editRecords = editRecords,
            onDismiss = { showEditDialog = false }
        )
    }

    if (showImageViewer) {
        val painters = imageViewerUrls.map { url ->
            rememberAsyncImagePainter(
                model = url
            )
        }
        MultiImageViewer(
            images = painters,
            initialPage = imageViewerInitialPage,
            isVisible = showImageViewer,
            onDismiss = { showImageViewer = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (uiState?.is_private == true) "私密帖子详情" else "帖子详情",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (uiState != null) Text(
                            text = "ID: ${uiState?.message_id}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        uiState?.let { it1 ->
                            if (it1.content.type != "image") {
                                DropdownMenuItem(
                                    text = { Text("复制文本") },
                                    onClick = {
                                        uiState?.let {
                                            clipboard.nativeClipboard.setPrimaryClip(
                                                ClipData.newPlainText(
                                                    "text",
                                                    it.content.content ?: it.content.text ?: ""
                                                )
                                            )
                                        }
                                        showMenu = false
                                        Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            null,
                                            Modifier.size(18.dp)
                                        )
                                    })
                            }
                            DropdownMenuItem(
                                text = { Text("分享帖子") },
                                onClick = {
                                    showShareSheet = true; showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Share,
                                        null,
                                        Modifier.size(18.dp)
                                    )
                                })
                                if (it1.sender_info.id == TokenManager.getUserID(context)) {
                                    DropdownMenuItem(
                                        text = { Text("编辑留言") },
                                        onClick = { showMenu = false; editMessage() },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Edit,
                                                null,
                                                Modifier.size(18.dp)
                                            )
                                        })
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "删除留言",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = { showMenu = false; messageToDelete = true },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                null,
                                                Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        })
                                }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
        ) {
            if (errorState != null) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(errorState!!, style = MaterialTheme.typography.bodyLarge)
                }
            } else if (uiState == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                MessageMainLayout(
                    msg = uiState!!,
                    onShowEditRecords = { records ->
                        editRecords = records
                        showEditDialog = true
                    },
                    onImageClick = { urls, index ->
                        imageViewerUrls = urls
                        imageViewerInitialPage = index
                        showImageViewer = true
                    }
                )
            }
        }

        if (showShareSheet) {
            uiState?.let { messageInfo ->
                ShareBottomSheet(
                    messageInfo = messageInfo,
                    onDismiss = { showShareSheet = false },
                    onCopyDeepLink = {
                        clipboard.nativeClipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "deep_link",
                                "qz://post-detail?id=${messageInfo.message_id}"
                            )
                        )
                        Toast.makeText(context, "应用内链已复制", Toast.LENGTH_SHORT).show()
                        showShareSheet = false
                    },
                    onCopyWebLink = {
                        clipboard.nativeClipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "web_link",
                                "https://qztool.dpdns.org/message?id=${messageInfo.message_id}"
                            )
                        )
                        Toast.makeText(context, "网页链接已复制", Toast.LENGTH_SHORT).show()
                        showShareSheet = false
                    },

                    onSaveImage = { bitmap ->
                        scope.launch {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        permission
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    saveBitmapToGallery(context, bitmap)
                                } else {
                                    pendingBitmap = bitmap
                                    permissionLauncher.launch(permission)
                                }
                            } else {
                                saveBitmapToGallery(context, bitmap)
                            }
                            showShareSheet = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EditRecordsDialog(
    editRecords: List<LocalEditRecord>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑记录") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                editRecords.forEach { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = "${record.editor_username} 于 ${record.edit_time} 修改",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = record.old_content,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun PostWithWatermark(
    messageInfo: MessageInfo,
    imageLoader: coil3.ImageLoader
) {
    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            MessageContent(messageInfo, screenShotMode = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.7f)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Terrain,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "轻昼CE",
                    fontSize = 12.sp
                )
                Text("•", modifier = Modifier.padding(horizontal = 8.dp), fontSize = 12.sp)
                AsyncImage(
                    model = messageInfo.category.avatar_url,
                    contentDescription = null,
                    imageLoader = imageLoader,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.user)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(messageInfo.category.name, fontSize = 12.sp)
            }
        }
    }
}

suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
    val filename = "screenshot_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        } else {
            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (!picturesDir.exists()) picturesDir.mkdirs()
            val file = File(picturesDir, filename)
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        resolver.openOutputStream(it)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "截图已保存到相册", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "保存失败：无法打开输出流", Toast.LENGTH_SHORT).show()
            }
        }
    } ?: withContext(Dispatchers.Main) {
        Toast.makeText(context, "保存失败：无法创建媒体记录", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MessageContent(
    msg: MessageInfo,
    onShowEditRecords: (List<LocalEditRecord>) -> Unit = {},
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    screenShotMode: Boolean = false,
) {
    val context = LocalContext.current
    val softwareImageLoader = remember { AppImageLoaders.getCoil3Loader(context) }

    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
        if (msg.is_private) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(" 私密消息", fontSize = 12.sp)
                    if (msg.visible_to != null) Text(" (指定用户可见)", fontSize = 11.sp)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(context, UserInfoActivity::class.java)
                    intent.putExtra("userId", msg.sender_info.id)
                    context.startActivity(intent)
                }
        ) {
            AsyncImage(
                model = msg.avatar_url,
                imageLoader = softwareImageLoader,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.user)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    msg.username,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                )
                msg.timestamp_user?.let {
                    Text(
                        it,
                        fontSize = 10.sp,
                        modifier = Modifier.alpha(0.8f)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        ContentRenderer(msg, softwareImageLoader, onImageClick)

        Spacer(Modifier.height(16.dp))

        msg.referenced_message?.let { ref -> ReferencedMessageCard(ref, softwareImageLoader) }

        if (!screenShotMode) {
            Column(
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = msg.category.avatar_url,
                            imageLoader = softwareImageLoader,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.user)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(msg.category.name, fontSize = 12.sp)
                    }
                }
            }
        }

        if (msg.is_edited == 1 && !msg.edit_records.isNullOrEmpty() && !screenShotMode) {
            TextButton(
                onClick = { onShowEditRecords(msg.edit_records) },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("查看编辑记录 (${msg.edit_records.size})")
            }
        }

        if (!screenShotMode) {
            Spacer(
                modifier = Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    messageInfo: MessageInfo,
    onDismiss: () -> Unit,
    onCopyDeepLink: () -> Unit,
    onCopyWebLink: () -> Unit,
    onSaveImage: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val softwareImageLoader = remember { AppImageLoaders.getCoil3Loader(context) }

    var screenshotView by remember { mutableStateOf<View?>(null) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 16.dp)
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AndroidView(
                        factory = { _ ->
                            androidx.compose.ui.platform.ComposeView(activity!!)
                                .apply {
                                    setContent {
                                        ToolBoxTheme {
                                            PostWithWatermark(
                                                messageInfo = messageInfo,
                                                imageLoader = softwareImageLoader
                                            )
                                        }
                                    }
                                    screenshotView = this
                                }
                        },
                        modifier = Modifier
                            .width(380.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(
                    20.dp,
                    Alignment.CenterHorizontally
                )
            ) {
                ShareActionCard(
                    icon = Icons.Default.Link,
                    label = "应用内链",
                    onClick = onCopyDeepLink
                )
                ShareActionCard(
                    icon = Icons.Default.Link,
                    label = "网页链接",
                    onClick = onCopyWebLink
                )
                ShareActionCard(
                    icon = Icons.Default.Save,
                    label = "保存图片",
                    onClick = {
                        val view = screenshotView
                        if (view == null) {
                            Toast.makeText(context, "视图未准备好", Toast.LENGTH_SHORT).show()
                            return@ShareActionCard
                        }

                        scope.launch {
                            try {
                                Toast.makeText(context, "正在生成图片...", Toast.LENGTH_SHORT)
                                    .show()

                                withContext(Dispatchers.Main) {
                                    suspendCancellableCoroutine { continuation ->
                                        view.post {
                                            continuation.resume(Unit)
                                        }
                                    }

                                    val bitmap = createBitmap(view.width, view.height)
                                    val canvas = Canvas(bitmap)
                                    view.draw(canvas)

                                    onSaveImage(bitmap)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    context,
                                    "生成图片失败: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ShareActionCard(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.requiredSize(26.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MessageMainLayout(
    msg: MessageInfo,
    onShowEditRecords: (List<LocalEditRecord>) -> Unit,
    onImageClick: (List<String>, Int) -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        MessageContent(msg, onShowEditRecords, onImageClick)
    }
}

@Composable
fun ContentRenderer(
    msg: MessageInfo,
    imageLoader: coil3.ImageLoader,
    onImageClick: (List<String>, Int) -> Unit
) {
    val content = msg.content
    when (content.type) {
        "image" -> {}
        "post" -> {
            Text("[帖子] ${content.title}", fontWeight = FontWeight.Bold)
            if (msg.is_markdown) MarkdownText(content.content ?: "") else Text(
                content.content ?: "", style = MaterialTheme.typography.bodyLarge
            )
        }

        else -> {
            if (msg.is_markdown) MarkdownText(content.text ?: "") else Text(
                content.text ?: "",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    content.images?.let { images ->
        images.forEachIndexed { index, img ->
            Spacer(Modifier.height(8.dp))
            AsyncImage(
                model = img,
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onImageClick(images, index) },
                contentScale = ContentScale.Fit,
                error = painterResource(id = R.drawable.resource)
            )
        }
    }
}

@Composable
fun ReferencedMessageCard(ref: ReferencedMessageInfo, imageLoader: coil3.ImageLoader) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.5f), RoundedCornerShape(12.dp))
    ) {
        Column(Modifier.padding(12.dp)) {
            val authorName = ref.sender_info?.username ?: ref.username ?: "未知"
            Text(" @$authorName", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(ref.content, fontSize = 13.sp)
            ref.referenced_message?.let {
                Box(Modifier.padding(top = 8.dp)) { ReferencedMessageCard(it, imageLoader) }
            }
        }
    }
}