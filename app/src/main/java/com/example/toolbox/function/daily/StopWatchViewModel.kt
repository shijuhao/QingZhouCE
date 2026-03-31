package com.example.toolbox.function.daily

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StopWatchViewModel : ViewModel() {
    // 秒表状态
    var isRunning by mutableStateOf(false)
        private set

    var elapsedTime by mutableLongStateOf(0L)
        private set

    private var startTime by mutableLongStateOf(0L)

    private val _lapTimes = MutableStateFlow<List<Long>>(emptyList())
    val lapTimes: StateFlow<List<Long>> = _lapTimes.asStateFlow()

    // 开始计时
    fun start() {
        if (!isRunning) {
            isRunning = true
            startTime = System.currentTimeMillis() - elapsedTime
        }
    }

    // 暂停计时
    fun pause() {
        if (isRunning) {
            isRunning = false
            elapsedTime = System.currentTimeMillis() - startTime
        }
    }

    // 重置计时器
    fun reset() {
        isRunning = false
        elapsedTime = 0L
        startTime = 0L
        _lapTimes.value = emptyList()
    }

    // 记录单圈时间
    fun recordLap() {
        if (isRunning) {
            val currentLaps = _lapTimes.value.toMutableList()
            currentLaps.add(0, elapsedTime)
            _lapTimes.value = currentLaps
        }
    }

    // 更新经过的时间（由 Activity 调用）
    fun updateElapsedTime(currentTime: Long) {
        if (isRunning) {
            elapsedTime = currentTime - startTime
        }
    }
}