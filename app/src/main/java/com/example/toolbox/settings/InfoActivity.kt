@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.toolbox.R
import com.example.toolbox.webview.WebViewActivity
import com.example.toolbox.ui.theme.ToolBoxTheme
import com.example.toolbox.utils.AppIconViewer
import com.example.toolbox.utils.MarkdownRenderer
import com.example.toolbox.utils.UpdateInfo
import com.example.toolbox.utils.checkForUpdateWithDetails
import com.example.toolbox.utils.getAppVersionInfo
import kotlinx.coroutines.launch

class InfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                InfoScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleScope = (context as? ComponentActivity)?.lifecycleScope

    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    var showUpdateLogDialog by remember { mutableStateOf(false) }
    var showUserRulesDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val userRules = stringResource(R.string.user_rules)

    if (showUpdateDialog && updateInfo != null) {
    AlertDialog(
        onDismissRequest = { showUpdateDialog = false },
        title = {
            Text(
                text = if (updateInfo?.isPreRelease == true) {
                    "发现新预发布版 ${updateInfo?.version}"
                } else {
                    "发现新版本 ${updateInfo?.version}"
                }
            )
        },
        text = {
            Text("是否前往下载最新版本？")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, updateInfo?.releaseUrl?.toUri())
                    context.startActivity(intent)
                    showUpdateDialog = false
                }
            ) {
                Text("前往下载")
            }
        },
        dismissButton = {
            TextButton(onClick = { showUpdateDialog = false }) {
                Text("稍后")
            }
        }
    )
}

    if (showUserRulesDialog) {
        AlertDialog(
            onDismissRequest = { showUserRulesDialog = false },
            title = { Text("隐私政策") },
            text = {
                MarkdownRenderer.Render(
                    modifier = Modifier.fillMaxWidth(),
                    content = userRules
                )
            },
            confirmButton = {
                TextButton(onClick = { showUserRulesDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                SettingsGroup(
                    items = listOf(
                        {
                            SettingsCustomItem(onClick = null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AppIconViewer()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(id = R.string.app_name),
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        {
                            SettingsItemCell(
                                icon = Icons.Outlined.Info,
                                title = "版本号",
                                subtitle = context.getAppVersionInfo().versionName,
                                onClick = {}
                            )
                        },
                        {
                            SettingsItemCell(
                                icon = Icons.Default.Code,
                                title = "源代码仓库",
                                subtitle = "https://github.com/shijuhao/QingZhouCE",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/shijuhao/QingZhouCE".toUri())
                                    context.startActivity(intent)
                                }
                            )
                        },
                        {
                            SettingsItemCell(
                                icon = Icons.Default.Update,
                                title = "检查更新",
                                subtitle = "检测是否有新版本",
                                onClick = {
                                    lifecycleScope?.launch {
                                        val info = checkForUpdateWithDetails(
                                            context = context,
                                            includePreRelease = true
                                        )
                                        if (info != null) {
                                            updateInfo = info
                                            showUpdateDialog = true
                                        } else {
                                            Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    )
                )
            }

            item {
                SettingsGroup(
                    title = "应用简介",
                    items = listOf {
                        SettingsCustomItem(onClick = null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "${stringResource(id = R.string.app_name)}是一款集成了多种实用工具的工具箱，使用Material You风格，旨在为用户提供便捷的日常工具服务。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                )
            }

            item {
                SettingsGroup(
                    title = "开发者信息",
                    items = listOf(
                        {
                            SettingsItemCell(
                                icon = Icons.Default.Person,
                                title = "开发者",
                                subtitle = "JuHao",
                                onClick = {}
                            )
                        },
                        {
                            SettingsItemCell(
                                icon = Icons.Default.Email,
                                title = "联系邮箱",
                                subtitle = "juhaoluoye@outlook.com",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = "mailto:juhaoluoye@outlook.com".toUri()
                                        putExtra(Intent.EXTRA_SUBJECT, "工具箱应用反馈")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "无法打开邮件应用", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        {
                            SettingsItemCell(
                                icon = Icons.Default.Share,
                                title = "分享应用",
                                subtitle = "向好友推荐工具箱",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "推荐工具箱应用")
                                        putExtra(Intent.EXTRA_TEXT, "我正在使用一款很实用的工具箱应用，推荐给你")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "分享应用"))
                                }
                            )
                        }
                    )
                )
            }

            item {
                SettingsGroup(
                    title = null,
                    items = listOf(
                        {
                            SettingsItemCell(
                                icon = Icons.Default.Person,
                                title = "隐私政策",
                                subtitle = "查看隐私政策条款",
                                onClick = { showUserRulesDialog = true }
                            )
                        },
                        {
                            SettingsItemCell(
                                icon = Icons.AutoMirrored.Filled.TextSnippet,
                                title = "更多信息",
                                subtitle = "查看应用许可证、特别鸣谢等",
                                onClick = {
                                    val intent = Intent(context, MoreInfoActivity::class.java)
                                    context.startActivity(intent)
                                }
                            )
                        }
                    )
                )
            }

            item {
                SettingsGroup(
                    title = null,
                    items = listOf {
                        SettingsCustomItem(onClick = null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "© 2026 QingZhou Team",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "All Rights Reserved",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }

            item {
                Spacer(
                    modifier = Modifier.height(
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                )
            }
        }
    }
}