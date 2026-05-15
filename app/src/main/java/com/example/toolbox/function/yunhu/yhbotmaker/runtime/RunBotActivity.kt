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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.toolbox.function.yunhu.yhbotmaker.toast
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit
import com.example.toolbox.AppJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private fun jsonObjectToMap(jsonObject: JsonObject): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    jsonObject.forEach { (key, element) ->
        result[key] = jsonElementToAny(element)
    }
    return result
}

private fun jsonElementToAny(element: JsonElement): Any {
    return when {
        element is JsonObject -> jsonObjectToMap(element)
        element is JsonArray -> element.map { jsonElementToAny(it) }
        element.jsonPrimitive.isString -> element.jsonPrimitive.content
        element.jsonPrimitive.booleanOrNull != null -> element.jsonPrimitive.boolean
        element.jsonPrimitive.intOrNull != null -> element.jsonPrimitive.int
        element.jsonPrimitive.longOrNull != null -> element.jsonPrimitive.long
        element.jsonPrimitive.doubleOrNull != null -> element.jsonPrimitive.double
        else -> element.toString()
    }
}

class RunBotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val token = intent.getStringExtra("token") ?: ""
        val botName = intent.getStringExtra("name") ?: "Bot"
        val index = intent.getIntExtra("index", 0)

        setContent {
            ToolBoxTheme {
                BotRuntimeScreen(
                    token = token,
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
    botName: String,
    index: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("bot_prefs", Context.MODE_PRIVATE)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    var isBlackout by remember { mutableStateOf(false) }

    var showQuickCommandManager by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSendDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    
    var isWsConnected by remember { mutableStateOf(false) }
    var currentLoopCode by remember { mutableStateOf(prefs.getString("code$index", "") ?: "") }

    val messages = remember {
        mutableStateListOf<ChatMessage>().apply {
            add(
                ChatMessage(
                    type = 1,
                    text = "🤖 机器人 [$botName] 已启动\n等待 WebSocket 连接...",
                    time = timeFormat.format(Date()),
                    iconColor = Color.Cyan
                )
            )
        }
    }
    
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(50)
            listState.animateScrollToItem(messages.size - 1)
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

    fun saveCode(type: String, code: String) {
        val key = if (type == "start") "code-start$index" else "code$index"
        prefs.edit { putString(key, code) }
        if (type == "loop") {
            currentLoopCode = code
        }
        toast(context, "已保存")
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
    
    val luaEngine = remember {
        LuaEngine(
            token = token,
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
    
    val wsManager = remember {
        BotWebSocketManager(
            token = token,
            onEvent = { eventJson ->
                val eventType = eventJson["header"]?.jsonObject
                    ?.get("eventType")?.jsonPrimitive?.contentOrNull ?: "unknown"
                
                messages.add(
                    ChatMessage(
                        type = 1,
                        text = "📨 [$eventType]\n${eventJson.toString().take(300)}",
                        time = timeFormat.format(Date()),
                        iconColor = Color.Cyan
                    )
                )
                
                if (currentLoopCode.isNotBlank()) {
                    val eventMap = jsonObjectToMap(eventJson)
                    luaEngine.runEventCode(currentLoopCode, eventMap)
                }
            },
            onStatusChanged = { connected ->
                isWsConnected = connected
                messages.add(
                    ChatMessage(
                        type = 1,
                        text = if (connected) "🔌 WebSocket 已连接" else "⚠️ WebSocket 已断开",
                        time = timeFormat.format(Date()),
                        iconColor = if (connected) Color.Green else Color.Red
                    )
                )
            },
            onError = { error ->
                messages.add(
                    ChatMessage(
                        type = 1,
                        text = "❌ WebSocket 错误: $error",
                        time = timeFormat.format(Date()),
                        iconColor = Color.Red
                    )
                )
            }
        )
    }
    
    LaunchedEffect(isWsConnected) {
        if (isWsConnected) {
            val startupCode = prefs.getString("code-start$index", "") ?: ""
            if (startupCode.isNotBlank()) {
                messages.add(
                    ChatMessage(
                        type = 1,
                        text = "📝 正在执行启动代码...",
                        time = timeFormat.format(Date()),
                        iconColor = Color.Yellow
                    )
                )
                luaEngine.runStartupCode(startupCode)
            }
        }
    }

    LaunchedEffect(Unit) {
        wsManager.connect()
    }

    BackHandler {
        wsManager.disconnect()
        onBack()
    }

    fun performSend(recvId: String, recvType: String, content: String, contentType: String) {
        val tempId = System.currentTimeMillis()
        messages.add(
            ChatMessage(
                id = tempId,
                type = 1,
                text = "发送中 → $recvId ($recvType): $content",
                time = timeFormat.format(Date()),
                iconColor = Color.Gray
            )
        )
    
        val api = YunHuApiService(token)
        api.sendMessage(
            recvId = recvId,
            recvType = recvType,
            contentType = contentType,
            content = content,
            onSuccess = { _, _ ->
                scope.launch(Dispatchers.Main) {
                    messages.removeAll { it.id == tempId }
                    messages.add(
                        ChatMessage(
                            type = 1,
                            text = "✅ → $recvId: $content",
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
                            text = "❌ 发送失败: $errorMsg",
                            time = timeFormat.format(Date()),
                            iconColor = Color.Red
                        )
                    )
                    toast(context, "发送失败: $errorMsg")
                }
            }
        )
    }

    if (showSendDialog) {
        SendMessageDialog(
            onDismiss = { showSendDialog = false },
            onSend = { recvId, recvType, content, contentType ->
                performSend(recvId, recvType, content, contentType)
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
                        Text("事件处理代码 (WebSocket事件触发)")  // 改这里
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
                Text(
                    text = if (currentCodeType == "start") "编辑功能代码" else "编辑事件处理代码",
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
                            saveCode(currentCodeType, codeContent.text)
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
                    
                    Text(
                        if (isWsConnected) "● WebSocket 已连接" else "○ WebSocket 未连接",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isWsConnected) Color.Green else Color.Red
                    )

                    HorizontalDivider()

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
                    subtitle = {
                        Text(
                            text = if (isWsConnected) "● 在线" else "○ 离线",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isWsConnected) Color.Green else Color.Red
                        )
                    },
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
                        if (isWsConnected) {
                            wsManager.disconnect()
                        } else {
                            wsManager.connect()
                        }
                    },
                    containerColor = if (isWsConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        if (isWsConnected) Icons.Default.Close else Icons.Default.Sync,
                        contentDescription = if (isWsConnected) "断开" else "连接"
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

                BottomActionBar(
                    onSendClick = { showSendDialog = true },
                    onClearClick = { messages.clear() },
                    onFullscreenClick = { isBlackout = true }
                )
            }
        }
        
        if (isBlackout) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { isBlackout = false },
                    modifier = Modifier.size(120.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "关闭黑屏",
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
    onClearClick: () -> Unit,
    onFullscreenClick: () -> Unit
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
            IconButton(onClick = onSendClick) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
            }
            IconButton(onClick = onFullscreenClick) {
                Icon(Icons.Default.Lightbulb, contentDescription = "黑屏模式")
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
