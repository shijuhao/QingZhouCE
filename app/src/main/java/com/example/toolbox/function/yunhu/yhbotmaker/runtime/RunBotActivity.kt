@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.toolbox.function.yunhu.yhbotmaker.toast
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit
import com.example.toolbox.AppJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class RunBotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 获取传入的机器人参数
        val token = intent.getStringExtra("token") ?: ""
        val chatId = intent.getStringExtra("id") ?: ""
        val chatType = intent.getStringExtra("type") ?: "user"
        val botName = intent.getStringExtra("name") ?: "Bot"
        val index = intent.getIntExtra("index", 0)

        setContent {
            ToolBoxTheme {
                BotRuntimeScreen(
                    token = token,
                    chatId = chatId,
                    chatType = chatType,
                    botName = botName,
                    index = index,
                    onBack = { finish() }
                )
            }
        }
    }
}

/**
 * 消息数据类
 */
data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val type: Int,          // 1=普通消息, 2=带按钮消息
    val text: String,
    val time: String,
    val iconRes: Int? = null,
    val iconColor: Color = Color.White,
    val buttonText: String? = null,
    val buttonAction: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotRuntimeScreen(
    token: String,
    chatId: String,
    chatType: String,
    botName: String,
    index: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showQuickCommandManager by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSendDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    // 示例消息数据
    val messages = remember {
        mutableStateListOf<ChatMessage>().apply {
            add(
                ChatMessage(
                    type = 2,
                    text = "点击按钮测试",
                    time = "12:01",
                    buttonText = "说明文档",
                    iconColor = Color.Cyan
                )
            )
        }
    }

    // 在 messages 定义后添加
    val listState = rememberLazyListState()

// 监听消息数量变化自动滚动
    LaunchedEffect(messages.size) {
        val lastIndex = listState.layoutInfo.totalItemsCount - 1
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (lastVisibleIndex == lastIndex && lastIndex >= 0) {
            delay(100) // 等待布局稳定
            listState.animateScrollToItem(lastIndex)
        }
    }

    val prefs = context.getSharedPreferences("bot_prefs", Context.MODE_PRIVATE)

    var requestInterval by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        requestInterval = prefs.getString("shilv$index", "2000") ?: "2000"
    }

    fun saveRequestInterval() {
        val value = requestInterval.toIntOrNull()
        if (value != null) {
            prefs.edit { putString("shilv$index", value.toString()) }
            toast(context, "已保存")
        } else {
            toast(context, "请输入数字")
            // 恢复原值
            requestInterval = prefs.getString("shilv$index", "2000") ?: "2000"
        }
    }

    // 编辑代码相关
    var showCodeTypeSelector by remember { mutableStateOf(false) }
    var showCodeEditor by remember { mutableStateOf(false) }
    var currentCodeType by remember { mutableStateOf("") }
    var codeContent by remember { mutableStateOf(TextFieldValue("")) }

    fun loadCode(type: String) {
        val key = if (type == "start") "code-start$index" else "code$index"
        codeContent = TextFieldValue(prefs.getString(key, "") ?: "")
    }

    fun saveCode(type: String) {
        val key = if (type == "start") "code-start$index" else "code$index"
        prefs.edit { putString(key, codeContent.text) }
        toast(context, "已保存")
    }

    // 在 BotRuntimeScreen 组合函数内添加状态
    var isRunning by remember { mutableStateOf(false) }
    var pollingJob by remember { mutableStateOf<Job?>(null) }


    // 使用 SharedPreferences 持久化存储已处理消息ID
    val processedMsgIds = remember {
        mutableStateOf(
            prefs.getStringSet("processed_msgs_$index", emptySet())?.toMutableSet() ?: mutableSetOf()
        )
    }

    // 添加保存函数
    fun saveProcessedIds() {
        prefs.edit { putStringSet("processed_msgs_$index", processedMsgIds.value) }
    }

    val symbols = listOf(
        ",",
        ".",
        "(",
        ")",
        "<",
        ">",
        "-",
        "_",
        "?",
        "!",
        "#",
        "@",
        "%",
        "/",
        "[",
        "]",
        "{",
        "}",
        "\"",
        "=",
        ":",
        ";"
    )

    // 时间格式化
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val luaEngine = remember {
        LuaEngine(
            token = token,
            chatId = chatId,
            chatType = chatType,
            onPrint = { msg, type ->
                messages.add(
                    ChatMessage(
                        type = 1,
                        text = msg,
                        time = timeFormat.format(Date()),
                        iconColor = when (type) {
                            1 -> Color.Green
                            2 -> Color.Red
                            3 -> Color.Yellow
                            4 -> Color.Cyan
                            5 -> Color.Magenta
                            else -> Color.White
                        }
                    )
                )
            }
        )
    }

    @SuppressLint("UseKtx")
    fun startPolling() {
        if (isRunning) return
        isRunning = true
        prefs.edit { putBoolean("stop_${index + 1}", false) }

        pollingJob = scope.launch(Dispatchers.IO) {
            val interval = requestInterval.toIntOrNull() ?: 2000
            val client = OkHttpClient()

            val startupCode = prefs.getString("code-start$index", "") ?: ""
            withContext(Dispatchers.Main) {
                luaEngine.runStartupCode(startupCode)
            }

            while (isRunning) {
                // 检查是否被外部停止
                if (prefs.getBoolean("stop_${index + 1}", false)) {
                    withContext(Dispatchers.Main) { isRunning = false }
                    break
                }

                // 每次循环都从 prefs 读取最新的循环代码
                val currentLoopCode = prefs.getString("code$index", "") ?: ""

                try {
                    val url = "https://chat-go.jwzhd.com/open-apis/v1/bot/messages?token=$token&chat-id=$chatId&chat-type=$chatType&before=10"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body.string()
                            val json = try {
                                AppJson.json.parseToJsonElement(body).jsonObject
                            } catch (_: Exception) {
                                return@launch
                            }

                            if (json["code"]?.jsonPrimitive?.double == 1.0) {
                                val data = json["data"]?.jsonObject
                                val list = data?.get("list")?.jsonArray?.map { it.jsonObject } ?: emptyList()
                                val recentMsgs = list.take(5)
                                for (msg in recentMsgs.reversed()) {
                                    val msgId = msg["msgId"]?.jsonPrimitive?.contentOrNull ?: continue
                                    if (processedMsgIds.value.contains(msgId)) continue

                                    processedMsgIds.value.add(msgId)
                                    saveProcessedIds()

                                    withContext(Dispatchers.Main) {
                                        luaEngine.runLoopCode(currentLoopCode, msg)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        messages.add(
                            ChatMessage(
                                type = 1,
                                text = "轮询错误: ${e.message}",
                                time = timeFormat.format(Date()),
                                iconColor = Color.Red
                            )
                        )
                    }
                }
                delay(interval.toLong())
            }
        }
    }

    fun stopPolling() {
        isRunning = false
        pollingJob?.cancel()
        pollingJob = null
        prefs.edit { putBoolean("stop_${index + 1}", true) }
        messages.add(
            ChatMessage(
                type = 1,
                text = "机器人已停止",
                time = timeFormat.format(Date()),
                iconColor = Color.Yellow
            )
        )
    }

// 在界面退出时停止轮询
    BackHandler {
        stopPolling()
        onBack()
    }

    fun performSend(content: String, contentType: String) {
        val tempId = System.currentTimeMillis()
        messages.add(
            ChatMessage(
                id = tempId,
                type = 1,
                text = "发送中: $content",
                time = timeFormat.format(Date()),
                iconColor = Color.Gray
            )
        )

        val api = YunHuApiService(token)
        api.sendMessage(
            recvId = chatId,
            recvType = chatType,
            contentType = contentType,
            content = content,
            onSuccess = { _, _ ->
                scope.launch(Dispatchers.Main) {
                    messages.removeAll { it.id == tempId }
                    messages.add(
                        ChatMessage(
                            type = 1,
                            text = content,
                            time = timeFormat.format(Date()),
                            iconColor = Color.Green
                        )
                    )
                    toast(context, "发送成功")
                }
            },
            onError = { errorMsg ->
                scope.launch(Dispatchers.Main) {
                    messages.removeAll { it.id == tempId }
                    messages.add(
                        ChatMessage(
                            type = 1,
                            text = "发送失败: $errorMsg",
                            time = timeFormat.format(Date()),
                            iconColor = Color.Red
                        )
                    )
                    toast(context, "发送失败: $errorMsg")
                }
            }
        )
    }

    // 发送对话框
    if (showSendDialog) {
        SendMessageDialog(
            onDismiss = { showSendDialog = false },
            onSend = { content, contentType ->
                performSend(content, contentType)
            }
        )
    }

    // 代码类型选择对话框
    if (showCodeTypeSelector) {
        AlertDialog(
            onDismissRequest = { showCodeTypeSelector = false },
            title = { Text("选择要编辑的代码") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            currentCodeType = "start"
                            loadCode("start")
                            showCodeTypeSelector = false
                            showCodeEditor = true
                        }
                    ) {
                        Text("功能代码 (启动前运行)")
                    }
                    TextButton(
                        onClick = {
                            currentCodeType = "loop"
                            loadCode("loop")
                            showCodeTypeSelector = false
                            showCodeEditor = true
                        }
                    ) {
                        Text("循环监听代码")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCodeTypeSelector = false
                }) { Text("取消") }
            }
        )
    }

    if (showCodeEditor) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCodeEditor = false },
            sheetState = sheetState,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 标题
                Text(
                    text = if (currentCodeType == "start") "编辑功能代码" else "编辑循环监听代码",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 可滚动的编辑器区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 编辑器主体
                    OutlinedTextField(
                        value = codeContent,
                        onValueChange = { codeContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .padding(bottom = 8.dp),
                        maxLines = 20,
                        label = { Text("Lua 代码") }
                    )

                    // 快捷符号栏 - 横向滑动单行
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        items(symbols) { symbol ->
                            Button(
                                onClick = {
                                    codeContent = codeContent.copy(
                                        text = codeContent.text + symbol
                                    )
                                },
                                modifier = Modifier.wrapContentWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(symbol)
                            }
                        }
                    }
                }

                // 底部按钮（固定在底部）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showCodeEditor = false }
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            saveCode(currentCodeType)
                            showCodeEditor = false
                        }
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }

    if (showQuickCommandManager) {
        QuickCommandManagerDialog(
            botIndex = index,
            onDismiss = { showQuickCommandManager = false }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            botIndex = index,
            onDismiss = { showImportDialog = false }
        )
    }

    if (showBackupDialog) {
        BackupDialog(
            botIndex = index,
            botName = botName,
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showHelpDialog) {
        HelpDocumentDialog(
            onDismiss = { showHelpDialog = false }
        )
    }

    val configuration = LocalConfiguration.current
    val drawerWidth = (configuration.screenWidthDp * 0.75f).dp
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(drawerWidth)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            "YHBot 控制台",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider()

                    // 循环间隔设置卡片
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "请求间隔 (ms/次)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = requestInterval,
                                    onValueChange = { requestInterval = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = MaterialTheme.shapes.small,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { saveRequestInterval() },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("保存")
                                }
                            }
                        }
                    }

                    // 菜单项列表
                    NavigationDrawerItem(
                        label = { Text("编辑代码") },
                        selected = false,
                        onClick = { showCodeTypeSelector = true },
                        icon = { Icon(Icons.Default.Code, null) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("快捷指令") },
                        selected = false,
                        onClick = { showQuickCommandManager = true },
                        icon = { Icon(Icons.Default.Bolt, null) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("导入配置") },
                        selected = false,
                        onClick = { showImportDialog = true },
                        icon = { Icon(Icons.Default.Download, null) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("备份代码") },
                        selected = false,
                        onClick = { showBackupDialog = true },
                        icon = { Icon(Icons.Default.Upload, null) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("帮助文档") },
                        selected = false,
                        onClick = { showHelpDialog = true },
                        icon = { Icon(Icons.AutoMirrored.Filled.Help, null) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("YHBot - $botName") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (isRunning) {
                            stopPolling()
                        } else {
                            startPolling()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "停止" else "启动"
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 消息列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        when (msg.type) {
                            1 -> MessageItem(msg)
                            2 -> MessageItemWithButton(msg)
                        }
                    }
                }

                // 底部操作栏
                BottomActionBar(
                    onSendClick = { showSendDialog = true },
                    onClearClick = {
                        messages.clear()
                    }
                )
            }
        }
    }
}

@Composable
fun MessageItem(message: ChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = message.text, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = message.time,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun MessageItemWithButton(message: ChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = message.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (message.buttonText != null) {
                Button(
                    onClick = { message.buttonAction?.invoke() ?: Unit },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(message.buttonText)
                }
            }
            Text(
                text = message.time,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun BottomActionBar(
    onSendClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* 打开设置 */ }) {
                Icon(Icons.Default.Settings, contentDescription = "设置")
            }
            IconButton(onClick = onSendClick) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
            }
            IconButton(onClick = { /* 切换全屏模式 */ }) {
                Icon(Icons.Default.Fullscreen, contentDescription = "全屏")
            }
            IconButton(onClick = onClearClick) {
                Icon(Icons.Default.Delete, "清空日志")
            }
            IconButton(onClick = {  }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "开始")
            }
        }
    }
}
