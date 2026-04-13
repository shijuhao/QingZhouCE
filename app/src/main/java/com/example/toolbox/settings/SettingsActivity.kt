package com.example.toolbox.settings

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.lanzou.viewmodel.LanzouAuthViewModel
import com.example.toolbox.ui.theme.ToolBoxTheme
import com.example.toolbox.webview.WebViewActivity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import com.example.toolbox.utils.getAppVersionInfo

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                SettingsScreen()
            }
        }
    }
}

data class ActionData(
    var isOpenCancelTips: Boolean = false,
    var isDisabledNotice: Boolean = false,
    var isEnabledAutoCheckUpdate: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lanzouAuthViewModel: LanzouAuthViewModel = viewModel()
    val isLanzouLoggedIn by lanzouAuthViewModel.isLoggedIn.collectAsState()
    val lanzouLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        lanzouAuthViewModel.refresh(context)
        if (it.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "蓝奏云登录成功", Toast.LENGTH_SHORT).show()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val prefs = context.getSharedPreferences("app_preferences", MODE_PRIVATE)
    val actionData = ActionData()
    actionData.isOpenCancelTips = prefs.getBoolean("exit_confirmation", false)
    var isOpenCancelTips by remember { mutableStateOf(actionData.isOpenCancelTips) }
    actionData.isDisabledNotice = prefs.getBoolean("disabled_community_notices", false)
    var isDisabledNotice by remember { mutableStateOf(actionData.isDisabledNotice) }
    actionData.isEnabledAutoCheckUpdate = prefs.getBoolean("autoCheckUpdate", true)
    var isEnabledAutoCheckUpdate by remember { mutableStateOf(actionData.isEnabledAutoCheckUpdate) }

    LaunchedEffect(Unit) {
        lanzouAuthViewModel.refresh(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text("设置")
                },
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
                    title = "行为",
                    items = listOf(
                        {
                            SettingsSwitchItem(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                title = "返回二次确认",
                                subtitle = "在主页面按返回键退出时二次确认",
                                checked = isOpenCancelTips,
                                onCheckedChange = {
                                    actionData.isOpenCancelTips = !actionData.isOpenCancelTips
                                    isOpenCancelTips = actionData.isOpenCancelTips
                                    prefs.edit().apply {
                                        putBoolean("exit_confirmation", isOpenCancelTips)
                                        apply()
                                    }
                                }
                            )
                        },
                        {
                            SettingsSwitchItem(
                                icon = Icons.Default.Update,
                                title = "自动检查更新",
                                subtitle = "在应用启动时自动检查更新",
                                checked = isEnabledAutoCheckUpdate,
                                onCheckedChange = {
                                    actionData.isEnabledAutoCheckUpdate = !actionData.isEnabledAutoCheckUpdate
                                    isEnabledAutoCheckUpdate = actionData.isEnabledAutoCheckUpdate
                                    prefs.edit().apply {
                                        putBoolean("autoCheckUpdate", isEnabledAutoCheckUpdate)
                                        apply()
                                    }
                                }
                            )
                        },
                        {
                            SettingsSwitchItem(
                                icon = Icons.Default.Notifications,
                                title = "禁用社区通知",
                                subtitle = "不再显示通知按钮",
                                checked = isDisabledNotice,
                                onCheckedChange = {
                                    actionData.isDisabledNotice = !actionData.isDisabledNotice
                                    isDisabledNotice = actionData.isDisabledNotice
                                    prefs.edit().apply {
                                        putBoolean("disabled_community_notices", isDisabledNotice)
                                        apply()
                                    }
                                }
                            )
                        }
                    )
                )
            }

            // 外观设置组
            item {
                SettingsGroup(
                    title = "外观",
                    items = listOf(
                        {
                            SettingsItemCell(
                                icon = Icons.Default.Draw,
                                title = "主题设置",
                                subtitle = "设置应用主题",
                                onClick = {
                                    val intent = Intent(context, ThemeActivity::class.java)
                                    context.startActivity(intent)
                                }
                            )
                        }
                    )
                )
            }

            item {
                SettingsGroup(
                    title = "账号",
                    items = buildList {
                        add {
                            SettingsItemCell(
                                icon = Icons.Default.Person,
                                title = "账号设置",
                                subtitle = "设置你的轻昼账号",
                                onClick = {
                                    val intent = Intent(context, UserSettingsActivity::class.java)
                                    context.startActivity(intent)
                                }
                            )
                        }
                        add {
                            SettingsItemCell(
                                icon = Icons.Default.Cloud,
                                title = "蓝奏云账号登录",
                                subtitle = if (isLanzouLoggedIn) "已登录" else "未登录",
                                onClick = {
                                    val intent = Intent(context, WebViewActivity::class.java).apply {
                                        putExtra(WebViewActivity.EXTRA_URL, "https://pc.woozooo.com/account.php?action=login&ref=/mydisk.php")
                                        putExtra(WebViewActivity.EXTRA_LANZOU_LOGIN_MODE, !isLanzouLoggedIn)
                                    }
                                    lanzouLoginLauncher.launch(intent)
                                }
                            )
                        }
                        if (isLanzouLoggedIn) {
                            add {
                                SettingsItemCell(
                                    icon = Icons.Default.Logout,
                                    title = "退出蓝奏云账号",
                                    subtitle = "清除本地保存的蓝奏云登录状态",
                                    onClick = {
                                        lanzouAuthViewModel.logout(context)
                                        Toast.makeText(context, "已退出蓝奏云账号", Toast.LENGTH_SHORT).show()
                                    },
                                    isDestructive = true
                                )
                            }
                        }
                    }
                )
            }

            // 关于应用组
            item {
                SettingsGroup(
                    title = "关于",
                    items = listOf(
                        {
                            SettingsItemCell(
                                icon = Icons.Default.Info,
                                title = "关于应用",
                                subtitle = "版本号 ${context.getAppVersionInfo().versionName}",
                                onClick = {
                                    val intent = Intent(context, InfoActivity::class.java)
                                    context.startActivity(intent)
                                }
                            )
                        }
                    )
                )
            }

            item {
                SettingsGroup(
                    title = "其他",
                    items = listOf(
                        {
                            SettingsItemCell(
                                icon = Icons.Default.Delete,
                                title = "清空已完成引导页状态",
                                subtitle = "重启应用后将会重新前往引导页",
                                onClick = {
                                    val prefs = context.getSharedPreferences("app_preferences", MODE_PRIVATE)
                                    prefs.edit().apply {
                                        remove("guideFinished")
                                        apply()
                                    }
                                    Toast.makeText(context, "操作已完成，重启生效", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                )
            }

            item {
                Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }
    }
}

/**
 * 设置组容器 (SplicedColumnGroup 风格)
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    items: List<@Composable () -> Unit>,
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (!title.isNullOrEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }

        val cornerRadius = 24.dp
        val smallRadius = 4.dp

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEachIndexed { index, item ->
                val shape = when {
                    items.size == 1 -> RoundedCornerShape(cornerRadius)
                    index == 0 -> RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius, bottomStart = smallRadius, bottomEnd = smallRadius)
                    index == items.size - 1 -> RoundedCornerShape(topStart = smallRadius, topEnd = smallRadius, bottomStart = cornerRadius, bottomEnd = cornerRadius)
                    else -> RoundedCornerShape(smallRadius)
                }

                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item()
                }
            }
        }
    }
}

/**
 * 标准设置项
 */
@Composable
fun SettingsItemCell(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    SettingsCustomItem(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * 带 Switch 的设置项
 */
@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isError: Boolean = false,
    isEnabled: Boolean = true // 新增参数
) {
    SettingsCustomItem(onClick = { if (isEnabled) onCheckedChange(!checked) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = when {
                    isError -> MaterialTheme.colorScheme.error
                    !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isEnabled) 1f else 0.38f
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = isEnabled,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                        tint = if (checked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    )
                }
            )
        }
    }
}

/**
 * 自定义内容的设置项
 */
@Composable
fun SettingsCustomItem(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            content()
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ToolBoxTheme {
        SettingsScreen()
    }
}
