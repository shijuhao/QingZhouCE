@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.ui.theme.ColorTheme
import com.example.toolbox.ui.theme.ToolBoxTheme
import com.example.toolbox.ui.theme.darkColorSchemeForTheme
import com.example.toolbox.ui.theme.lightColorSchemeForTheme
import kotlin.system.exitProcess

class ThemeActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ThemeViewModel = viewModel()

            viewModel.loadSavedTheme(this)

            val currentTheme by viewModel.currentTheme.collectAsState()
            val monetEnabled by viewModel.monetEnabled.collectAsState()
            val iconColorEnabled by viewModel.iconColorEnabled.collectAsState()
            val colorTheme by viewModel.colorTheme.collectAsState()

            val scrollBehavior =
                TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

            ToolBoxTheme {
                var openDialog by remember { mutableStateOf(false) }
                val context = LocalContext.current

                if (openDialog) {
                    AlertDialog(
                        onDismissRequest = { openDialog = false },
                        title = { Text(text = "你确定要重启吗？") },
                        text = { Text("点击确定重启应用。") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val intent = context.packageManager
                                        .getLaunchIntentForPackage(context.packageName)
                                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                    (context as? ComponentActivity)?.finish()
                                    exitProcess(0)
                                }
                            ) {
                                Text("确定")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { openDialog = false }
                            ) {
                                Text("取消")
                            }
                        }
                    )
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        LargeTopAppBar(
                            title = {
                                Text("主题设置")
                            },
                            navigationIcon = {
                                FilledTonalIconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回"
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { openDialog = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                                        contentDescription = "重启"
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
                    ThemeSwitchScreen(
                        currentTheme = currentTheme,
                        monetEnabled = monetEnabled,
                        iconColorEnabled = iconColorEnabled,
                        colorTheme = colorTheme,
                        innerPadding = innerPadding,
                        onThemeChange = { theme ->
                            viewModel.changeTheme(theme, this)
                        },
                        onMonetToggle = {
                            viewModel.toggleMonetEnabled(this)
                        },
                        onColorThemeChange = { theme ->
                            viewModel.changeColorTheme(theme, this)
                        },
                        onIconColorToggle = { viewModel.toggleIconColorEnabled(context) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemePreviewSelector(
    currentTheme: ThemeData,
    colorTheme: ColorTheme,
    monetEnabled: Boolean,
    onThemeChange: (ThemeData) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val modes = listOf(
            Triple(ThemeData.Light, "浅色", false),
            Triple(ThemeData.Dark, "深色", true),
            Triple(ThemeData.Auto, "系统", androidx.compose.foundation.isSystemInDarkTheme())
        )

        modes.forEach { (data, label, isDarkPreview) ->
            val isSelected = currentTheme.themeName == data.themeName

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .selectable(
                        selected = isSelected,
                        onClick = { onThemeChange(data) },
                        role = Role.RadioButton
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ThemePreviewCard(
                    isDark = isDarkPreview,
                    isSelected = isSelected,
                    colorTheme = colorTheme,
                    monetEnabled = monetEnabled
                )

                Spacer(modifier = Modifier.size(8.dp))

                Row (
                    verticalAlignment = Alignment.CenterVertically
                ){
                    RadioButton(
                        selected = isSelected,
                        onClick = null
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ThemePreviewCard(
    isDark: Boolean,
    isSelected: Boolean,
    colorTheme: ColorTheme,
    monetEnabled: Boolean
) {
    val context = LocalContext.current
    val isMonetSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

    val previewScheme = when {
        monetEnabled && isMonetSupported -> {
            if (isDark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        isDark -> {
            darkColorSchemeForTheme(colorTheme)
        }
        else -> {
            lightColorSchemeForTheme(colorTheme)
        }
    }

    val bgColor = previewScheme.surface
    val contentColor = previewScheme.surfaceVariant
    val primaryColor = previewScheme.primary
    val onSurfaceVariant = previewScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(10.dp)
                    )
                } else Modifier
            )
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(modifier = Modifier
                    .size(12.dp, 4.dp)
                    .clip(CircleShape)
                    .background(onSurfaceVariant.copy(0.3f)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier
                    .size(8.dp, 4.dp)
                    .clip(CircleShape)
                    .background(onSurfaceVariant.copy(0.3f)))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(primaryColor)
            )

            repeat(3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(contentColor),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(onSurfaceVariant.copy(0.4f))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSwitchScreen(
    currentTheme: ThemeData,
    monetEnabled: Boolean,
    innerPadding: PaddingValues,
    colorTheme: ColorTheme,
    onThemeChange: (ThemeData) -> Unit,
    onMonetToggle: () -> Unit,
    onColorThemeChange: (ColorTheme) -> Unit,
    iconColorEnabled: Boolean,
    onIconColorToggle: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding()),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            val isMonetSupported =
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

            SettingsGroup(
                title = "外观",
                items = listOf(
                    {
                        SettingsSwitchItem(
                            icon = Icons.Default.Palette,
                            title = "开启莫奈取色",
                            subtitle = if (isMonetSupported) {
                                "使用系统动态配色方案（开启后将禁用颜色主题选择）"
                            } else {
                                "当前安卓版本过低，不支持莫奈取色"
                            },
                            checked = monetEnabled,
                            onCheckedChange = { onMonetToggle() },
                            isEnabled = isMonetSupported
                        )
                    },
                    {
                        SettingsSwitchItem(
                            icon = Icons.Default.FormatColorFill,
                            title = "启用功能图标多彩颜色",
                            subtitle = "关闭后将统一使用主题色",
                            checked = iconColorEnabled,
                            onCheckedChange = { onIconColorToggle() },
                            isEnabled = true
                        )
                    }
                )
            )
        }

        item {
            SettingsGroup(
                title = "主题模式",
                items = listOf(
                    {
                        ThemePreviewSelector(
                            currentTheme = currentTheme,
                            colorTheme = colorTheme,
                            monetEnabled = monetEnabled,
                            onThemeChange = onThemeChange
                        )
                    }
                )
            )
        }

        if (monetEnabled) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp)) // 稍微圆角
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "提示",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "莫奈取色已开启，颜色主题选择已禁用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            ColorThemeRadioGroup(
                currentTheme = colorTheme,
                monetEnabled = monetEnabled,
                onThemeSelected = onColorThemeChange
            )
        }

        item {
            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
fun ColorThemeRadioGroup(
    currentTheme: ColorTheme,
    monetEnabled: Boolean,
    onThemeSelected: (ColorTheme) -> Unit,
) {
    val colorThemes = listOf(
        ColorTheme.PURPLE to "紫色主题",
        ColorTheme.BLUE to "蓝色主题",
        ColorTheme.GREEN to "绿色主题",
        ColorTheme.ORANGE to "橙色主题",
        ColorTheme.RED to "红色主题",
        ColorTheme.DEEP_PURPLE to "深紫主题",
        ColorTheme.CYAN to "青色主题",
        ColorTheme.BROWN to "棕色主题"
    )

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    SettingsGroup(
        title = "颜色主题",
        items = colorThemes.map { (theme, themeName) ->
            {
                // 动态获取对应主题在当前模式下的主色调
                val previewColor = if (isDarkTheme) {
                    darkColorSchemeForTheme(theme).primary
                } else {
                    lightColorSchemeForTheme(theme).primary
                }

                ColorThemeRadioItem(
                    theme = theme,
                    themeName = themeName,
                    isSelected = currentTheme == theme,
                    primaryColor = previewColor,
                    monetEnabled = monetEnabled,
                    onThemeSelected = onThemeSelected
                )
            }
        }
    )
}

@Composable
fun ColorThemeRadioItem(
    theme: ColorTheme,
    themeName: String,
    isSelected: Boolean,
    primaryColor: Color,
    monetEnabled: Boolean,
    onThemeSelected: (ColorTheme) -> Unit
) {
    SettingsCustomItem {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .selectable(
                    selected = isSelected && !monetEnabled,
                    enabled = !monetEnabled,
                    onClick = {
                        if (!monetEnabled) {
                            onThemeSelected(theme)
                        }
                    },
                    role = Role.RadioButton
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (monetEnabled) primaryColor.copy(alpha = 0.38f) else primaryColor
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = themeName,
                    style = MaterialTheme.typography.titleMedium, // 使用 titleMedium 匹配 SettingsItemCell
                    color = if (monetEnabled) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // 禁用时使用较浅的颜色
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            RadioButton(
                selected = isSelected,
                enabled = !monetEnabled, // 莫奈取色开启时禁用
                onClick = null // 点击事件已在父容器处理
            )
        }
    }
}