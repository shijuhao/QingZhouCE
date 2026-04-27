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
import com.example.toolbox.functionPage.IconColorPrefs

sealed class ThemeData(val themeName: String, val isDarkTheme: Boolean) {
    object Light : ThemeData("浅色模式", false)
    object Dark : ThemeData("深色模式", true)
    object Auto : ThemeData("跟随系统", false)
}

class ThemeViewModel : ViewModel() {

    private val isMonetSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    private val _currentTheme = MutableStateFlow<ThemeData>(ThemeData.Auto)
    val currentTheme: StateFlow<ThemeData> = _currentTheme.asStateFlow()

    private val _colorTheme = MutableStateFlow(ColorTheme.PURPLE)
    val colorTheme: StateFlow<ColorTheme> = _colorTheme.asStateFlow()

    private val _monetEnabled = MutableStateFlow(isMonetSupported)
    val monetEnabled: StateFlow<Boolean> = _monetEnabled.asStateFlow()

    private val _iconColorEnabled = MutableStateFlow(true)
    val iconColorEnabled: StateFlow<Boolean> = _iconColorEnabled.asStateFlow()

    private val iconColorKey = "icon_color_enabled"
    private val prefsName = "theme_preferences"
    private val themeKey = "saved_theme"
    private val colorThemeKey = "color_theme"
    private val monetKey = "monet_enabled"

    fun loadSavedTheme(context: Context) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

            val savedIconColorEnabled = prefs.getBoolean(iconColorKey, true)
            _iconColorEnabled.value = savedIconColorEnabled

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
            if (_monetEnabled.value && isMonetSupported) {
                return@launch
            }

            _colorTheme.value = colorTheme
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit { putString(colorThemeKey, colorTheme.name) }
        }
    }

    fun toggleMonetEnabled(context: Context) {
        if (!isMonetSupported) return

        viewModelScope.launch {
            val newValue = !_monetEnabled.value
            _monetEnabled.value = newValue
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit { putBoolean(monetKey, newValue) }
        }
    }

    fun toggleIconColorEnabled(context: Context) {
        viewModelScope.launch {
            val newValue = !_iconColorEnabled.value
            _iconColorEnabled.value = newValue
            IconColorPrefs.setEnabled(context, newValue)
        }
    }
}