package com.example.toolbox.ui.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.google.accompanist.systemuicontroller.rememberSystemUiController

// 定义颜色主题枚举
enum class ColorTheme {
    PURPLE,    // 紫色主题
    BLUE,      // 蓝色主题
    GREEN,     // 绿色主题
    ORANGE,    // 橙色主题
    RED,       // 红色主题
    DEEP_PURPLE, // 深紫色主题
    CYAN,      // 青色主题
    BROWN      // 棕色主题
}

@Suppress("DEPRECATION")
@Composable
fun ToolBoxTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val themePrefs = remember(context) {
        context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE)
    }

    var themeMode by remember(themePrefs) {
        mutableStateOf(themePrefs.getString("saved_theme", "Auto") ?: "Auto")
    }

    var colorTheme by remember(themePrefs) {
        mutableStateOf(
            ColorTheme.valueOf(themePrefs.getString("color_theme", "PURPLE") ?: "PURPLE")
        )
    }

    var monetEnabled by remember(themePrefs) {
        mutableStateOf(themePrefs.getBoolean("monet_enabled", true))
    }

    DisposableEffect(themePrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                "saved_theme" -> {
                    themeMode = sharedPreferences.getString("saved_theme", "Auto") ?: "Auto"
                }
                "color_theme" -> {
                    val themeStr = sharedPreferences.getString("color_theme", "PURPLE") ?: "PURPLE"
                    colorTheme = try {
                        ColorTheme.valueOf(themeStr)
                    } catch (_: IllegalArgumentException) {
                        ColorTheme.PURPLE
                    }
                }
                "monet_enabled" -> {
                    monetEnabled = sharedPreferences.getBoolean("monet_enabled", true)
                }
            }
        }

        themePrefs.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            themePrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val darkTheme = when (themeMode) {
        "浅色模式" -> false
        "深色模式" -> true
        else -> isSystemInDarkTheme()
    }

    val shouldUseDynamicColor = monetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        shouldUseDynamicColor -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            if (darkTheme) darkColorSchemeForTheme(colorTheme) else lightColorSchemeForTheme(colorTheme)
        }
    }

    val systemUiController = rememberSystemUiController()
    val useDarkIcons = !darkTheme
    val view = LocalView.current

    SideEffect {
        systemUiController.run {
            setStatusBarColor(color = Color.Transparent, darkIcons = useDarkIcons)
            setNavigationBarColor(
                color = Color.Transparent,
                darkIcons = useDarkIcons,
                navigationBarContrastEnforced = false
            )
        }
        
        if (!view.isInEditMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (context as Activity).window.isNavigationBarContrastEnforced = false
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}