@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge 
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.function.yunhu.yhbotmaker.toast
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit
import com.example.toolbox.AppJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

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

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val type: Int,  // 1=收到事件, 2=发送消息, 3=系统消息, 4=自动回复, 5=快捷命令, 6=错误
    val text: String,
    val time: String,
    val iconRes: Int? = null,
    val iconColor: Color = Color.White
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    
    val viewModel = BotRuntimeViewModel.getInstance(index)
    val messagesState by viewModel.messages.collectAsState()
    
    val isWsConnected by viewModel.isWsConnected.collectAsState()
    val currentLoopCode by viewModel.currentLoopCode.collectAsState()
    
    var showRestartDialog by remember { mutableStateOf(false) }
    var pendingConnect by remember { mutableStateOf(false) }
    
    var isBlackout by remember { mutableStateOf(false) }

    var showFastBotDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSendDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showSharedDataDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    
    // 电池优化弹窗
    val batteryDialogShown = remember { mutableStateOf(prefs.getBoolean("battery_dialog_shown", false)) }
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
    val isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(context.packageName)
    
    if (!isIgnoringBattery && !batteryDialogShown.value) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("电池优化") },
            text = { Text("为了让机器人在后台稳定运行，请允许忽略电池优化") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("去设置")
                    }
                    TextButton(onClick = { }) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            batteryDialogShown.value = true
                            prefs.edit { putBoolean("battery_dialog_shown", true) }
                        }
                    ) {
                        Text("不再提醒")
                    }
                }
            }
        )
    }
    
    LaunchedEffect(Unit) {
        if (messagesState.isEmpty()) {
            viewModel.addMessage(
                ChatMessage(
                    type = 3,
                    text = "🤖 机器人 [$botName] 已启动\n等待 WebSocket 连接...",
                    time = timeFormat.format(Date()),
                    iconColor = Color.Cyan
                )
            )
        }
    }
    
    val listState = rememberLazyListState()

    LaunchedEffect(messagesState.size) {
        if (messagesState.isNotEmpty()) {
            delay(50)
            listState.animateScrollToItem(messagesState.size - 1)
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
            viewModel.setCurrentLoopCode(code)
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
    
    BotSharedData.init(context, index)
    
    val luaEngine = remember {
        viewModel.luaEngine ?: LuaEngine(
            token = token,
            onPrint = { msg, type ->
                viewModel.addMessage(
                    ChatMessage(
                        type = type,
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
        ).also {
            viewModel.luaEngine = it
        }
    }
    
    val onEventCallback: (JsonObject) -> Unit = { eventJson ->
        val eventType = eventJson["header"]?.jsonObject
            ?.get("eventType")?.jsonPrimitive?.contentOrNull ?: "unknown"
        
        viewModel.addMessage(
            ChatMessage(
                type = 1,
                text = "收到消息，类型为：$eventType",
                time = timeFormat.format(Date()),
                iconColor = Color.Cyan
            )
        )
        
        // ========== 黑名单和违禁词过滤 ==========
        if (eventType == "message.receive.normal") {
            val eventObj = eventJson["event"]?.jsonObject
            val messageObj = eventObj?.get("message")?.jsonObject
            val contentObj = messageObj?.get("content")?.jsonObject
            val text = contentObj?.get("text")?.jsonPrimitive?.content ?: ""
            val senderObj = eventObj?.get("sender")?.jsonObject
            val senderId = senderObj?.get("senderId")?.jsonPrimitive?.content ?: ""
            val chatObj = eventObj?.get("chat")?.jsonObject
            val chatType = chatObj?.get("chatType")?.jsonPrimitive?.content ?: ""
            val msgId = messageObj?.get("msgId")?.jsonPrimitive?.content ?: ""
            val chatId = chatObj?.get("chatId")?.jsonPrimitive?.content ?: ""
            
            val helperKey = "chelper$index"
            val helperJson = prefs.getString(helperKey, "") ?: ""
            
            if (helperJson.isNotBlank()) {
                try {
                    val commandData = AppJson.json.decodeFromString<CommandData>(helperJson)
                    
                    if (chatType == "group") {
                        val isBlocked = commandData.blockedUsers.any { it.userId == senderId }
                        if (isBlocked) {
                            val api = YunHuApiService(token)
                            api.recallMessage(
                                chatId = chatId,
                                chatType = chatType,
                                msgId = msgId,
                                onSuccess = { _, _ ->
                                    viewModel.addMessage(
                                        ChatMessage(
                                            type = 4,
                                            text = "🚫 已屏蔽黑名单用户发言: $senderId",
                                            time = timeFormat.format(Date()),
                                            iconColor = Color.Red
                                        )
                                    )
                                },
                                onError = { err ->
                                    viewModel.addMessage(
                                        ChatMessage(
                                            type = 4,
                                            text = "撤回失败: $err",
                                            time = timeFormat.format(Date()),
                                            iconColor = Color.Red
                                        )
                                    )
                                }
                            )
                            return@onEventCallback
                        }
                    }
                    
                    for (bannedWord in commandData.bannedWords) {
                        if (text.contains(bannedWord.word)) {
                            when (bannedWord.action) {
                                "delete" -> {
                                    val api = YunHuApiService(token)
                                    api.recallMessage(
                                        chatId = if (chatType == "group") chatId else senderId,
                                        chatType = chatType,
                                        msgId = msgId,
                                        onSuccess = { _, _ ->
                                            viewModel.addMessage(
                                                ChatMessage(
                                                    type = 4,
                                                    text = "⚠️ 包含违禁词「${bannedWord.word}」，已删除",
                                                    time = timeFormat.format(Date()),
                                                    iconColor = Color.Red
                                                )
                                            )
                                        },
                                        onError = { err ->
                                            viewModel.addMessage(
                                                ChatMessage(
                                                    type = 4,
                                                    text = "撤回失败: $err",
                                                    time = timeFormat.format(Date()),
                                                    iconColor = Color.Red
                                                )
                                            )
                                        }
                                    )
                                    return@onEventCallback
                                }
                                "warn" -> {
                                    val api = YunHuApiService(token)
                                    val recvId = if (chatType == "group") chatId else senderId
                                    api.sendMessage(
                                        recvId = recvId,
                                        recvType = chatType,
                                        contentType = "text",
                                        content = "⚠️ 您的消息包含违禁词「${bannedWord.word}」，请注意言行",
                                        onSuccess = { _, _ ->
                                            viewModel.addMessage(
                                                ChatMessage(
                                                    type = 4,
                                                    text = "⚠️ 已警告包含违禁词「${bannedWord.word}」的用户",
                                                    time = timeFormat.format(Date()),
                                                    iconColor = Color.Yellow
                                                )
                                            )
                                        },
                                        onError = { err ->
                                            viewModel.addMessage(
                                                ChatMessage(
                                                    type = 4,
                                                    text = "发送警告失败: $err",
                                                    time = timeFormat.format(Date()),
                                                    iconColor = Color.Red
                                                )
                                            )
                                        }
                                    )
                                    return@onEventCallback
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // 解析失败，忽略
                }
            }
        }
        // ========== 黑名单和违禁词过滤结束 ==========
        
        // ========== 自动回复和快捷命令处理 ==========
        val helperKey = "chelper$index"
        val helperJson = prefs.getString(helperKey, "") ?: ""
        
        if (helperJson.isNotBlank()) {
            try {
                val commandData = AppJson.json.decodeFromString<CommandData>(helperJson)
                
                if (eventType == "message.receive.normal") {
                    val eventObj = eventJson["event"]?.jsonObject
                    val messageObj = eventObj?.get("message")?.jsonObject
                    val contentObj = messageObj?.get("content")?.jsonObject
                    val text = contentObj?.get("text")?.jsonPrimitive?.content ?: ""
                    val senderObj = eventObj?.get("sender")?.jsonObject
                    val senderId = senderObj?.get("senderId")?.jsonPrimitive?.content ?: ""
                    val chatObj = eventObj?.get("chat")?.jsonObject
                    val chatType = chatObj?.get("chatType")?.jsonPrimitive?.content ?: ""
                    val chatId = chatObj?.get("chatId")?.jsonPrimitive?.content ?: ""
                    
                    for (autoReply in commandData.autoReplies) {
                        if (text.contains(autoReply.key)) {
                            val api = YunHuApiService(token)
                            
                            // 根据聊天类型决定回复给谁
                            val (recvId, recvType) = when (chatType) {
                                "group" -> Pair(chatId, "group")      // 群聊：回复到群
                                "bot" -> Pair(senderId, "user")       // 私聊：回复给用户
                                else -> Pair(senderId, "user")
                            }
                            
                            api.sendMessage(
                                recvId = recvId,
                                recvType = recvType,
                                contentType = autoReply.type,
                                content = autoReply.reply,
                                onSuccess = { _, _ ->
                                    viewModel.addMessage(
                                        ChatMessage(
                                            type = 2,
                                            text = "自动回复成功: ${autoReply.reply}",
                                            time = timeFormat.format(Date()),
                                            iconColor = Color.Green
                                        )
                                    )
                                },
                                onError = { err ->
                                    viewModel.addMessage(
                                        ChatMessage(
                                            type = 4,
                                            text = "自动回复失败: $err",
                                            time = timeFormat.format(Date()),
                                            iconColor = Color.Red
                                        )
                                    )
                                }
                            )
                            break
                        }
                    }
                }
                
                // 处理指令消息（快捷命令）
                if (eventType == "message.receive.instruction") {
                    val eventObj = eventJson["event"]?.jsonObject
                    val messageObj = eventObj?.get("message")?.jsonObject
                    val commandId = messageObj?.get("commandId")?.jsonPrimitive?.intOrNull ?: 0
                    
                    for (quickCmd in commandData.quickCommands) {
                        if (quickCmd.id == commandId) {
                            // 执行快捷命令的 Lua 代码
                            try {
                                val eventMap = jsonObjectToMap(eventJson)
                                luaEngine.runEventCode(quickCmd.code, eventMap)
                                viewModel.addMessage(
                                    ChatMessage(
                                        type = 2,
                                        text = "执行快捷命令成功: $commandId",
                                        time = timeFormat.format(Date()),
                                        iconColor = Color.Cyan
                                    )
                                )
                            } catch (e: Exception) {
                                viewModel.addMessage(
                                    ChatMessage(
                                        type = 4,
                                        text = "快捷命令执行失败: ${e.message}",
                                        time = timeFormat.format(Date()),
                                        iconColor = Color.Red
                                    )
                                )
                            }
                            break
                        }
                    }
                }
            } catch (_: Exception) {
                // 解析失败，忽略
            }
        }
        // ========== 自动回复和快捷命令处理结束 ==========
        
        if (currentLoopCode.isNotBlank()) {
            val eventMap = jsonObjectToMap(eventJson)
            luaEngine.runEventCode(currentLoopCode, eventMap)
        }
    }
    
    val onStatusChangedCallback: (Boolean) -> Unit = { connected ->
        viewModel.setWsConnected(connected)
        prefs.edit { putBoolean("stop_${index + 1}", !connected) }
    
        if (connected) {
            viewModel.addMessage(
                ChatMessage(
                    type = 3,
                    text = "WebSocket 已连接",
                    time = timeFormat.format(Date()),
                    iconColor = Color.Green
                )
            )
        }
    }
    
    val onErrorCallback: (String) -> Unit = { error ->
        viewModel.addMessage(
            ChatMessage(
                type = 4,
                text = "WebSocket 错误: $error",
                time = timeFormat.format(Date()),
                iconColor = Color.Red
            )
        )
    }
    
    LaunchedEffect(Unit) {
        val savedCode = prefs.getString("code$index", "") ?: ""
        if (viewModel.currentLoopCode.value.isEmpty() && savedCode.isNotEmpty()) {
            viewModel.setCurrentLoopCode(savedCode)
        }
        BotWebSocketManagerSingleton.getInstance(
            botIndex = index,
            token = token,
            onEvent = onEventCallback,
            onStatusChanged = onStatusChangedCallback,
            onError = onErrorCallback
        )
    }
    
    val startupExecuted by viewModel.startupExecuted.collectAsState()

    LaunchedEffect(isWsConnected) {
        if (isWsConnected && !startupExecuted) {
            viewModel.setStartupExecuted(true)
            val startupCode = prefs.getString("code-start$index", "") ?: ""
            if (startupCode.isNotBlank()) {
                viewModel.addMessage(
                    ChatMessage(
                        type = 3,
                        text = "📝 正在执行启动代码...",
                        time = timeFormat.format(Date()),
                        iconColor = Color.Yellow
                    )
                )
                luaEngine.runStartupCode(startupCode)
            }
        }
    }

    BackHandler {
        onBack()
    }

    fun performSend(recvId: String, recvType: String, content: String, contentType: String) {
        val tempId = System.currentTimeMillis()
        viewModel.addMessage(
            ChatMessage(
                id = tempId,
                type = 3,
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
                    val newMessages = viewModel.messages.value.filter { it.id != tempId }
                    viewModel.clearMessages()
                    newMessages.forEach { viewModel.addMessage(it) }
                    
                    viewModel.addMessage(
                        ChatMessage(
                            type = 2,
                            text = "成功向 $recvId 发送: $content",
                            time = timeFormat.format(Date()),
                            iconColor = Color.Green
                        )
                    )
                    toast(context, "发送成功")
                }
            },
            onError = { errorMsg ->
                scope.launch(Dispatchers.Main) {
                    val newMessages = viewModel.messages.value.filter { it.id != tempId }
                    viewModel.clearMessages()
                    newMessages.forEach { viewModel.addMessage(it) }
                    
                    viewModel.addMessage(
                        ChatMessage(
                            type = 4,
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
    
    if (showSharedDataDialog) {
        SharedDataDialog(
            botIndex = index,
            onDismiss = { showSharedDataDialog = false }
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
                        Text("事件处理代码 (WebSocket事件触发)")
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
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { newValue ->
                newValue != SheetValue.Hidden
            }
        )
        ModalBottomSheet(
            onDismissRequest = { },
            dragHandle = { },
            sheetState = sheetState,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showCodeEditor = false }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentCodeType == "start") "编辑功能代码" else "编辑事件处理代码",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            saveCode(currentCodeType, codeContent.text)
                            showCodeEditor = false
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    OutlinedTextField(
                        value = codeContent,
                        onValueChange = { codeContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 8.dp),
                        label = { Text("Lua 代码") },
                        maxLines = Int.MAX_VALUE,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        items(symbols) { symbol ->
                            FilledTonalButton(
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
            }
        }
    }

    if (showFastBotDialog) {
        FastBotDialog(
            botIndex = index,
            onDismiss = { showFastBotDialog = false }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            botIndex = index,
            botName = botName,
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
    
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("启动代码已修改") },
            text = { Text("检测到启动代码已变更，是否重新运行初始化代码？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestartDialog = false
                        viewModel.setStartupExecuted(false)
                        BotWebSocketManagerSingleton.connect(index)
                    }
                ) {
                    Text("重新运行")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestartDialog = false
                        BotWebSocketManagerSingleton.connect(index)
                    }
                ) {
                    Text("直接连接")
                }
            }
        )
    }
    
    fun getAvatarBitmap(): Bitmap? {
        val avatarPath = prefs.getString("avatar_$index", null)
        return avatarPath?.takeIf { File(it).exists() }?.let { BitmapFactory.decodeFile(it) }
    }
    
    var avatarBitmap by remember { mutableStateOf(getAvatarBitmap()) }

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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "YHBot 控制台",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (isWsConnected) "● WebSocket 已连接" else "○ WebSocket 未连接",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isWsConnected) Color.Green else MaterialTheme.colorScheme.error
                        )
                    }

                    HorizontalDivider()

                    NavigationDrawerItem(
                        label = { 
                            Text(
                                if (isWsConnected) "断开连接" else "连接",
                                color = if (isWsConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ) 
                        },
                        selected = false,
                        onClick = {
                            if (isWsConnected) {
                                BotWebSocketManagerSingleton.disconnect(index)
                            } else {
                                val currentStartupCode = prefs.getString("code-start$index", "") ?: ""
                                
                                if (startupExecuted && luaEngine.startupCodeExecuted != currentStartupCode) {
                                    showRestartDialog = true
                                } else {
                                    BotWebSocketManagerSingleton.connect(index)
                                }
                            }
                        },
                        icon = { 
                            Icon(
                                if (isWsConnected) Icons.Default.Close else Icons.Default.Sync, 
                                null,
                                tint = if (isWsConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ) 
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("编辑代码") },
                        selected = false,
                        onClick = { showCodeTypeSelector = true },
                        icon = { Icon(Icons.Default.Code, null) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("FastBot") },
                        selected = false,
                        onClick = { showFastBotDialog = true },
                        icon = { Icon(Icons.Default.Bolt, null) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("SharedData") },
                        selected = false,
                        onClick = { showSharedDataDialog = true },
                        icon = { Icon(Icons.Default.Storage, null) },
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
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (avatarBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = avatarBitmap!!.asImageBitmap(),
                                    contentDescription = "头像",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "默认头像",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Column {
                                Text("YHBot - $botName")
                                Text(
                                    text = if (isWsConnected) "● 在线" else "○ 离线",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isWsConnected) Color.Green else MaterialTheme.colorScheme.error
                                )
                            }
                        }
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
            bottomBar = {
                BottomActionBar(
                    onSendClick = { showSendDialog = true },
                    onClearClick = { viewModel.clearMessages() },
                    onFullscreenClick = { isBlackout = true },
                    onConnectClick = {
                        if (isWsConnected) {
                            BotWebSocketManagerSingleton.disconnect(index)
                        } else {
                            val currentStartupCode = prefs.getString("code-start$index", "") ?: ""
                            
                            if (startupExecuted && luaEngine.startupCodeExecuted != currentStartupCode) {
                                showRestartDialog = true
                            } else {
                                BotWebSocketManagerSingleton.connect(index)
                            }
                        }
                    },
                    isWsConnected = isWsConnected
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messagesState) { msg ->
                        MessageItem(msg)
                    }
                }
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
    val (icon, iconTint) = when (message.type) {
        1 -> Icons.AutoMirrored.Filled.CallReceived to Color.Cyan           // 收到消息
        2 -> Icons.Default.Check to Color.Green                       // 操作成功
        3 -> Icons.Default.Info to Color.White                              // 系统消息
        4 -> Icons.Default.Close to Color.Red                               // 报错
        else -> Icons.Default.Android to MaterialTheme.colorScheme.primary  // 其他
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = message.text, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = message.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BottomActionBar(
    onSendClick: () -> Unit,
    onClearClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onConnectClick: () -> Unit,
    isWsConnected: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .navigationBarsPadding(),
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
            IconButton(onClick = onConnectClick) {
                Icon(
                    if (isWsConnected) Icons.Default.Close else Icons.Default.Sync,
                    contentDescription = if (isWsConnected) "断开" else "连接",
                    tint = if (isWsConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
