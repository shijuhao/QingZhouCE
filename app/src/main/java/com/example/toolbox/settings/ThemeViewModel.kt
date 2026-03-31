package com.example.toolbox.settings

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.toolbox.ui.theme.ColorTheme
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

sealed class ThemeData(val themeName: String, val isDarkTheme: Boolean) {
    object Light : ThemeData("浅色模式", false)
    object Dark : ThemeData("深色模式", true)
    object Auto : ThemeData("跟随系统", false)
}

class ThemeViewModel : ViewModel() {

    // 检查系统是否支持莫奈取色 (Android 12+)
    private val isMonetSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    private val _currentTheme = MutableStateFlow<ThemeData>(ThemeData.Auto)
    val currentTheme: StateFlow<ThemeData> = _currentTheme.asStateFlow()

    private val _colorTheme = MutableStateFlow(ColorTheme.PURPLE)
    val colorTheme: StateFlow<ColorTheme> = _colorTheme.asStateFlow()

    // 莫奈取色状态
    private val _monetEnabled = MutableStateFlow(isMonetSupported) // 初始值根据支持情况定
    val monetEnabled: StateFlow<Boolean> = _monetEnabled.asStateFlow()

    private val prefsName = "theme_preferences"
    private val themeKey = "saved_theme"
    private val colorThemeKey = "color_theme"
    private val monetKey = "monet_enabled"

    fun loadSavedTheme(context: Context) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

            // 如果不支持莫奈取色，强制设为 false，否则读取保存值（默认 true）
            val savedMonetEnabled = if (isMonetSupported) {
                prefs.getBoolean(monetKey, true)
            } else {
                false
            }

            val savedThemeName = prefs.getString(themeKey, "Auto") ?: "Auto"
            val savedColorThemeStr = prefs.getString(colorThemeKey, "PURPLE") ?: "PURPLE"
            val savedColorTheme = try {
                ColorTheme.valueOf(savedColorThemeStr)
            } catch (_: IllegalArgumentException) {
                ColorTheme.PURPLE
            }

            val newTheme = when (savedThemeName) {
                "浅色模式" -> ThemeData.Light
                "深色模式" -> ThemeData.Dark
                else -> ThemeData.Auto
            }

            _currentTheme.value = newTheme
            _monetEnabled.value = savedMonetEnabled
            _colorTheme.value = savedColorTheme

            // 如果因为版本不支持而强制设为 false，同步到存储中
            if (!isMonetSupported && prefs.getBoolean(monetKey, true)) {
                prefs.edit { putBoolean(monetKey, false) }
            }
        }
    }

    fun changeTheme(themeData: ThemeData, context: Context) {
        viewModelScope.launch {
            _currentTheme.value = themeData
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit { putString(themeKey, themeData.themeName) }
        }
    }

    fun changeColorTheme(colorTheme: ColorTheme, context: Context) {
        viewModelScope.launch {
            // 如果莫奈取色开启且系统支持，不处理切换
            if (_monetEnabled.value && isMonetSupported) {
                return@launch
            }

            _colorTheme.value = colorTheme
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit { putString(colorThemeKey, colorTheme.name) }
        }
    }

    fun toggleMonetEnabled(context: Context) {
        // 如果系统不支持，直接返回，不允许切换
        if (!isMonetSupported) return

        viewModelScope.launch {
            val newValue = !_monetEnabled.value
            _monetEnabled.value = newValue
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit { putBoolean(monetKey, newValue) }
        }
    }
}