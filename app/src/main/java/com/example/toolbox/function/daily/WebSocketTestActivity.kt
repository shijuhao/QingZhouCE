package com.example.toolbox.function.daily

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.toolbox.ui.theme.ToolBoxTheme

class WebSocketTestActivity : ComponentActivity() {
    private val viewModel: WebSocketTestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                WebSocketTestScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSocketTestScreen(
    viewModel: WebSocketTestViewModel
) {
    val context = LocalContext.current
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    
    var url by remember { mutableStateOf("ws://echo.websocket.org") }
    var messageInput by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebSocket 测试") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.clearMessages()
                        Toast.makeText(context, "已清空消息", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Delete, "清空")
                    }
                }
            )
        },
        bottomBar = {
            if (isConnected) {
                Surface(
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            label = { Text("消息内容") },
                            placeholder = { Text("输入要发送的消息") },
                            modifier = Modifier.weight(1f),
                            maxLines = 3
                        )
                        
                        IconButton(
                            onClick = {
                                if (messageInput.isNotBlank()) {
                                    viewModel.sendMessage(messageInput)
                                    messageInput = ""
                                }
                            },
                            enabled = messageInput.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 连接状态卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "状态: $connectionStatus",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isConnected) "● 在线" else "○ 离线",
                            color = if (isConnected) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    if (!isConnected) {
                        Button(onClick = {
                            if (url.isNotBlank()) {
                                viewModel.connect(url)
                            } else {
                                Toast.makeText(context, "请输入WebSocket地址", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("连接")
                        }
                    } else {
                        Button(onClick = { viewModel.disconnect() }) {
                            Text("断开")
                        }
                    }
                }
            }
            
            // URL输入框（未连接时显示）
            if (!isConnected) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("WebSocket 地址") },
                    placeholder = { Text("ws://example.com/ws") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 消息列表
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无消息",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(messages) { message ->
                            MessageItem(message = message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: WebSocketMessage) {
    val context = LocalContext.current
    
    // 根据消息类型选择图标
    val (icon, iconColor) = when (message.type) {
        MessageType.SUCCESS -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        MessageType.RECEIVED -> Icons.Default.CallReceived to Color(0xFF2196F3)
        MessageType.ERROR -> Icons.Default.Error to Color(0xFFF44336)
        MessageType.WARNING -> Icons.Default.Warning to Color(0xFFFF9800)
        MessageType.SENT -> Icons.Default.CallMade to Color(0xFF4CAF50)
        MessageType.INFO -> Icons.Default.Info to Color(0xFF9E9E9E)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // 图标
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 1.dp),
                tint = iconColor
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 消息内容
            Text(
                text = message.content,
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
            
            // 复制按钮
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("WebSocket消息", message.content)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    "复制",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}