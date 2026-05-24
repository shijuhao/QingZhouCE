package com.example.toolbox.function.mouse

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MouseSimulatorScreen(
    viewModel: MouseViewModel = viewModel(),
    appViewModel: MouseAppViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null,
    isMain: Boolean = false
) {
    val context = LocalContext.current
    var overlayManager by remember { mutableStateOf<MouseOverlayManager?>(null) }
    var showStartDialog by remember { mutableStateOf(false) }
    
    // 延迟初始化 overlayManager，确保在 Activity 完全创建后
    LaunchedEffect(Unit) {
        if (overlayManager == null) {
            overlayManager = MouseOverlayManager(context)
        }
    }
    
    LaunchedEffect(overlayManager) {
        overlayManager?.let { manager ->
            if (appViewModel.isMouseRunning && !manager.checkIsRunning()) {
                try {
                    manager.startOverlay(viewModel) {
                        appViewModel.updateMouseRunning(false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模拟鼠标") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = {
                        if (isMain && onMenuClick != null) {
                            onMenuClick()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            if (isMain) Icons.Default.Menu else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isMain) "菜单" else "返回"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (appViewModel.isMouseRunning) {
                        overlayManager?.stopOverlay()
                        appViewModel.updateMouseRunning(false)
                    } else {
                        showStartDialog = true
                    }
                },
                containerColor = if (appViewModel.isMouseRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    if (appViewModel.isMouseRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                    contentDescription = if (appViewModel.isMouseRunning) "停止" else "启动"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (appViewModel.isMouseRunning) 
                        MaterialTheme.colorScheme.errorContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (appViewModel.isMouseRunning) "运行中" else "状态",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = if (appViewModel.isMouseRunning) "模拟鼠标正在运行" else "模拟鼠标未启动",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (appViewModel.isMouseRunning) 
                            MaterialTheme.colorScheme.onErrorContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "鼠标设置",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("鼠标大小: ${viewModel.mouseSize}px")
                    Slider(
                        value = viewModel.mouseSize.toFloat(),
                        onValueChange = { viewModel.updateMouseSize(it.toInt()) },
                        valueRange = 20f..100f,
                        steps = 79
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("鼠标速度: ${String.format("%.1f", viewModel.mouseSpeed)}x")
                    Slider(
                        value = viewModel.mouseSpeed,
                        onValueChange = { viewModel.updateMouseSpeed(it) },
                        valueRange = 0.5f..5.0f,
                        steps = 44
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("鼠标透明度: ${viewModel.mouseAlpha}%")
                    Slider(
                        value = viewModel.mouseAlpha.toFloat(),
                        onValueChange = { viewModel.updateMouseAlpha(it.toInt()) },
                        valueRange = 20f..100f,
                        steps = 79
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "显示选项",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    SwitchItem(
                        title = "显示时钟",
                        checked = viewModel.showClock,
                        onCheckedChange = { viewModel.toggleShowClock(it) }
                    )
                    
                    SwitchItem(
                        title = "显示电池",
                        checked = viewModel.showBattery,
                        onCheckedChange = { viewModel.toggleShowBattery(it) }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "1. 点击右下角按钮启动模拟鼠标\n" +
                               "2. 使用方向控制面板移动鼠标\n" +
                               "3. 左键/右键进行点击操作\n" +
                               "4. 关闭按钮可停止模拟鼠标",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("启动模拟鼠标") },
            text = { 
                Column {
                    Text("即将启动模拟鼠标悬浮窗。")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (overlayManager?.hasOverlayPermission() == false) {
                        Text(
                            text = "• 需要悬浮窗权限",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (overlayManager?.hasAccessibilityPermission() == false) {
                        Text(
                            text = "• 需要无障碍权限（用于点击功能）",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when {
                            overlayManager?.hasOverlayPermission() == false -> {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    "package:${context.packageName}".toUri()
                                )
                                context.startActivity(intent)
                                showStartDialog = false
                            }
                            overlayManager?.hasAccessibilityPermission() == false -> {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                                showStartDialog = false
                            }
                            else -> {
                                try {
                                    overlayManager?.startOverlay(viewModel) {
                                        appViewModel.updateMouseRunning(false)
                                    }
                                    appViewModel.updateMouseRunning(true)
                                    showStartDialog = false
                                    Toast.makeText(context, "模拟鼠标已启动", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "启动失败: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        when {
                            overlayManager?.hasOverlayPermission() == false -> "去授权"
                            overlayManager?.hasAccessibilityPermission() == false -> "去开启"
                            else -> "启动"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
