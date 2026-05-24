package com.example.toolbox.function.mouse

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.core.content.edit

class MouseViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("mouse_prefs", android.content.Context.MODE_PRIVATE)

    var mouseSize by mutableIntStateOf(prefs.getInt("mouse_size", 35))
        private set
    
    var mouseSpeed by mutableFloatStateOf(prefs.getFloat("mouse_speed", 1.2f))
        private set
    
    var mouseAlpha by mutableIntStateOf(prefs.getInt("mouse_alpha", 100))
        private set
    
    var mouseStyle by mutableIntStateOf(prefs.getInt("mouse_style", 1))
        private set
    
    var showClock by mutableStateOf(prefs.getBoolean("show_clock", false))
        private set
    
    var showBattery by mutableStateOf(prefs.getBoolean("show_battery", false))
        private set

    fun updateMouseSize(size: Int) {
        mouseSize = size
        prefs.edit { putInt("mouse_size", size) }
    }

    fun updateMouseSpeed(speed: Float) {
        mouseSpeed = speed
        prefs.edit { putFloat("mouse_speed", speed) }
    }

    fun updateMouseAlpha(alpha: Int) {
        mouseAlpha = alpha
        prefs.edit { putInt("mouse_alpha", alpha) }
    }

    fun updateMouseStyle(style: Int) {
        mouseStyle = style
        prefs.edit { putInt("mouse_style", style) }
    }

    fun toggleShowClock(show: Boolean) {
        showClock = show
        prefs.edit { putBoolean("show_clock", show) }
    }

    fun toggleShowBattery(show: Boolean) {
        showBattery = show
        prefs.edit { putBoolean("show_battery", show) }
    }
}
