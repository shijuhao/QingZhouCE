package com.example.toolbox.function.yunhu.yhbotmaker

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.function.yunhu.yhbotmaker.runtime.RunBotActivity
import com.example.toolbox.function.yunhu.yhbotmaker.runtime.BotWebSocketManagerSingleton
import com.example.toolbox.ui.theme.ToolBoxTheme
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.edit
import com.example.toolbox.AppJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class Bot(
    val token: String,
    val name: String,
    var index: Int = 0
)

class BotModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("bot_prefs", Context.MODE_PRIVATE)

    var bots by mutableStateOf<List<Bot>>(emptyList())
        private set

    init { reload() }

    private fun reload() {
        val json = prefs.getString("botlist", "[]") ?: "[]"
        bots = try {
            AppJson.json.decodeFromString<List<Bot>>(json)
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    private fun save() {
        prefs.edit { putString("botlist", AppJson.json.encodeToString(bots)) }
    }

    fun avatarPath(index: Int) = prefs.getString("avatar_$index", null)
    fun setAvatar(index: Int, path: String?) {
        prefs.edit {
            if (path == null) remove("avatar_$index") else putString("avatar_$index", path)
        }
    }

    fun add(bot: Bot): Boolean {
        // 检查是否已存在同名机器人
        if (bots.any { it.name == bot.name }) {
            return false
        }
        bots = bots + bot.copy(index = bots.size + 1)
        save()
        return true
    }
    
    fun update(pos: Int, bot: Bot): Boolean {
        // 检查是否已存在同名机器人（排除当前编辑的）
        if (bots.any { it.index != pos + 1 && it.name == bot.name }) {
            return false
        }
        bots = bots.toMutableList().apply { this[pos] = bot.copy(index = pos + 1) }
        save()
        return true
    }

    fun delete(pos: Int) {
        bots = bots.toMutableList().apply { removeAt(pos) }
        save()
        (pos + 1).let { idx ->
            listOf("avatar_$idx", "chelper_$idx", "code_$idx", "code-start_$idx")
               .forEach { key -> prefs.edit { remove(key) } }
        }
    }

    var lastIndex: Int
        get() = prefs.getInt("last_index", -1)
        set(value) = prefs.edit { putInt("last_index", value) }

    fun clearLast() {
        prefs.edit { remove("last_index") }
    }
}

// ==================== 主屏幕 ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotManagerScreen(
    isMain: Boolean = false,
    onMenuClick: () -> Unit = {},
    onSettings: () -> Unit = {},
    onBotDetail: ((Bot) -> Unit)? = null
) {
    val context = LocalContext.current
    val model: BotModel = viewModel()
    var showCreate by remember { mutableStateOf(false) }
    var editPos by remember { mutableIntStateOf(-1) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val navigateToBotDetail: (Bot) -> Unit = remember {
        onBotDetail ?: { bot ->
            context.startActivity(Intent(context, RunBotActivity::class.java).apply {
                putExtra("token", bot.token)
                putExtra("name", bot.name)
                putExtra("index", bot.index)
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YHBotMaker") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = {
                        if (isMain) onMenuClick() else (context as Activity).finish()
                    }) {
                        Icon(if (isMain) Icons.Default.Menu else Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.MoreVert, null) } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, null) } }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (model.bots.isEmpty()) {
                Text(
                    "机器人列表是空的，点击右下角按钮创建",
                    Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    itemsIndexed(
                        items = model.bots,
                        key = { index, bot -> "${index}_${bot.index}_$refreshKey" }
                    ) { pos, bot ->
                        BotCard(
                            bot = bot,
                            avatar = model.avatarPath(pos + 1),
                            onClick = { model.lastIndex = pos; navigateToBotDetail(bot) },
                            onEdit = { editPos = pos },
                            onDelete = { model.delete(pos) }
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        EditDialog(
            initial = null,
            currentAvatar = null,
            onDismiss = { showCreate = false },
            onConfirm = { bot, avatarUri ->
                val success = model.add(bot)
                if (success) {
                    if (avatarUri != null) {
                        saveImage(context, avatarUri)?.let { path ->
                            model.setAvatar(model.bots.size, path)
                        }
                    }
                    showCreate = false
                    refreshKey++
                } else {
                    toast(context, "机器人名称已存在")
                }
            }
        )
    }

    if (editPos >= 0) {
        val bot = model.bots[editPos]
        EditDialog(
            initial = bot,
            currentAvatar = model.avatarPath(editPos + 1),
            onDismiss = { editPos = -1 },
            onConfirm = { newBot, avatarUri ->
                val success = model.update(editPos, newBot)
                if (success) {
                    if (avatarUri != null) {
                        saveImage(context, avatarUri)?.let { path ->
                            model.setAvatar(editPos + 1, path)
                        }
                    } else if (model.avatarPath(editPos + 1) != null) {
                        model.setAvatar(editPos + 1, null)
                    }
                    editPos = -1
                    refreshKey++
                } else {
                    toast(context, "机器人名称已存在")
                }
            }
        )
    }
}

@Composable
fun BotCard(
    bot: Bot,
    avatar: String?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val connectionState = BotWebSocketManagerSingleton.getConnectionState(bot.token)
    val isConnected = connectionState?.collectAsState()?.value ?: false

    val bitmap = remember(avatar) {
        avatar?.takeIf { File(it).exists() }?.let { BitmapFactory.decodeFile(it) }
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp)) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .background(
                            color = if (isConnected) Color.Green else Color.Gray,
                            shape = CircleShape
                        )
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(bot.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Token: ${bot.token.take(10)}...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                listOfNotNull(
                    "编辑" to onEdit,
                    "删除" to onDelete
                ).forEach { (text, action) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = { action(); showMenu = false }
                    )
                }
            }
        }
    }
}

@Composable
fun EditDialog(
    initial: Bot?,
    currentAvatar: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (Bot, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var token by remember { mutableStateOf(initial?.token ?: "") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val bitmap = remember(avatarUri, currentAvatar) {
        when {
            avatarUri != null -> {
                try {
                    context.contentResolver.openInputStream(avatarUri!!)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                } catch (_: Exception) { null }
            }
            currentAvatar != null -> {
                File(currentAvatar).takeIf { it.exists() }?.let {
                    BitmapFactory.decodeFile(it.absolutePath)
                }
            }
            else -> null
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { avatarUri = it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "创建机器人" else "编辑机器人") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { launcher.launch("image/*") }) {
                        Text("选择头像")
                    }
                    if (avatarUri != null || currentAvatar != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = { avatarUri = null }) {
                            Text("清除")
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        name.isBlank() || token.isBlank() -> toast(context, "请填写完整")
                        else -> {
                            val bot = Bot(token, name)
                            onConfirm(bot, avatarUri)
                        }
                    }
                }
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onDismiss) { Text("取消") } }
    )
}

fun saveImage(context: Context, uri: Uri): String? = try {
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input)?.let { bmp ->
            File(context.filesDir, "avatar_${System.currentTimeMillis()}.png").apply {
                FileOutputStream(this).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
            }.absolutePath
        }
    }
} catch (_: Exception) { null }

fun toast(context: Context, msg: String) {
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}

class BotMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                BotManagerScreen(
                    onSettings = { /* 设置按钮点击事件 */ }
                )
            }
        }
    }
}