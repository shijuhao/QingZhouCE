package com.example.toolbox.function.mouse

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.core.content.edit

class MouseAppViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("mouse_prefs", android.content.Context.MODE_PRIVATE)

    var isMouseRunning by mutableStateOf(prefs.getBoolean("is_mouse_running", false))
        private set

    fun updateMouseRunning(running: Boolean) {
        isMouseRunning = running
        prefs.edit { putBoolean("is_mouse_running", running) }
    }

    fun getMouseSize(): Int = prefs.getInt("mouse_size", 35)
    fun getMouseSpeed(): Float = prefs.getFloat("mouse_speed", 1.2f)
    fun getMouseAlpha(): Int = prefs.getInt("mouse_alpha", 100)
    fun getMouseStyle(): Int = prefs.getInt("mouse_style", 1)
    fun getShowClock(): Boolean = prefs.getBoolean("show_clock", false)
    fun getShowBattery(): Boolean = prefs.getBoolean("show_battery", false)

    fun updateMouseSize(size: Int) {
        prefs.edit { putInt("mouse_size", size) }
    }

    fun updateMouseSpeed(speed: Float) {
        prefs.edit { putFloat("mouse_speed", speed) }
    }

    fun updateMouseAlpha(alpha: Int) {
        prefs.edit { putInt("mouse_alpha", alpha) }
    }

    fun updateMouseStyle(style: Int) {
        prefs.edit { putInt("mouse_style", style) }
    }

    fun toggleShowClock(show: Boolean) {
        prefs.edit { putBoolean("show_clock", show) }
    }

    fun toggleShowBattery(show: Boolean) {
        prefs.edit { putBoolean("show_battery", show) }
    }
}
