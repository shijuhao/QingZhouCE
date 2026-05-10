package com.example.toolbox.function.daily

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolbox.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class TimerInstance(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var isRunning: Boolean = false,
    var elapsedTime: Long = 0L,
    var targetTime: Long = 0L,
    var originalTargetTime: Long = 0L,
    var startTime: Long = 0L,
    var pausedAt: Long = 0L, // 暂停时的系统时间戳
    val lapTimes: MutableList<Long> = mutableListOf(),
    val type: TimerType = TimerType.STOPWATCH
)

enum class TimerType {
    STOPWATCH,   // 秒表
    COUNTDOWN    // 倒计时
}

class StopWatchViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private var instance: StopWatchViewModel? = null
        
        fun getInstance(application: Application): StopWatchViewModel {
            return instance ?: synchronized(this) {
                instance ?: StopWatchViewModel(application).also { instance = it }
            }
        }
    }
    
    // 后台监控任务
    private var monitorJob: Job? = null
    
    init {
        startMonitor()
    }
    // Tab状态
    var selectedTab by mutableIntStateOf(0)
        private set

    // 秒表列表
    private val _stopwatches = MutableStateFlow<List<TimerInstance>>(emptyList())
    val stopwatches: StateFlow<List<TimerInstance>> = _stopwatches.asStateFlow()

    // 倒计时列表
    private val _countdowns = MutableStateFlow<List<TimerInstance>>(emptyList())
    val countdowns: StateFlow<List<TimerInstance>> = _countdowns.asStateFlow()

    // 切换Tab
    fun selectTab(index: Int) {
        selectedTab = index
    }

    // 添加新秒表
    fun addStopwatch(name: String = "秒表 ${_stopwatches.value.size + 1}") {
        val newStopwatch = TimerInstance(name = name, type = TimerType.STOPWATCH)
        _stopwatches.value += newStopwatch
    }

    // 开始秒表
    fun startStopwatch(id: String) {
        val list = _stopwatches.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1 && !list[index].isRunning) {
            val stopwatch = list[index]
            val now = System.currentTimeMillis()
            
            val newStartTime = if (stopwatch.pausedAt > 0) {
                now - stopwatch.elapsedTime
            } else {
                now
            }
            
            list[index] = stopwatch.copy(
                isRunning = true,
                startTime = newStartTime,
                pausedAt = 0L
            )
            _stopwatches.value = list
        }
    }

    // 暂停秒表
    fun pauseStopwatch(id: String) {
        val list = _stopwatches.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1 && list[index].isRunning) {
            val stopwatch = list[index]
            val now = System.currentTimeMillis()
            val elapsed = now - stopwatch.startTime
            
            list[index] = stopwatch.copy(
                isRunning = false,
                elapsedTime = elapsed,
                pausedAt = now
            )
            _stopwatches.value = list
        }
    }

    // 重置秒表
    fun resetStopwatch(id: String) {
        val list = _stopwatches.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            list[index] = list[index].copy(
                isRunning = false,
                elapsedTime = 0L,
                startTime = 0L,
                pausedAt = 0L,
                lapTimes = mutableListOf()
            )
            _stopwatches.value = list
        }
    }

    // 删除秒表
    fun removeStopwatch(id: String) {
        _stopwatches.value = _stopwatches.value.filter { it.id != id }
    }

    // 记录计次
    fun recordLap(id: String) {
        val list = _stopwatches.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1 && list[index].isRunning) {
            val stopwatch = list[index]
            val currentLaps = stopwatch.lapTimes.toMutableList()
            currentLaps.add(0, stopwatch.elapsedTime)
            list[index] = stopwatch.copy(lapTimes = currentLaps)
            _stopwatches.value = list
        }
    }

    // 添加新倒计时
    fun addCountdown(name: String = "倒计时 ${_countdowns.value.size + 1}", duration: Long = 60000) {
        val newCountdown = TimerInstance(
            name = name,
            targetTime = duration,
            originalTargetTime = duration,
            elapsedTime = duration,
            type = TimerType.COUNTDOWN
        )
        _countdowns.value += newCountdown
    }

    // 开始倒计时
    fun startCountdown(id: String) {
        val list = _countdowns.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1 && !list[index].isRunning && list[index].elapsedTime > 0) {
            val countdown = list[index]
            val now = System.currentTimeMillis()
            
            val newStartTime = if (countdown.pausedAt > 0) {
                val alreadyElapsed = countdown.targetTime - countdown.elapsedTime
                now - alreadyElapsed
            } else {
                now
            }
            
            list[index] = countdown.copy(
                isRunning = true,
                startTime = newStartTime,
                pausedAt = 0L
            )
            _countdowns.value = list
        }
    }
    
    // 暂停倒计时
    fun pauseCountdown(id: String) {
        val list = _countdowns.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1 && list[index].isRunning) {
            val countdown = list[index]
            val now = System.currentTimeMillis()
            val elapsed = now - countdown.startTime
            val remaining = (countdown.targetTime - elapsed).coerceAtLeast(0)
            
            list[index] = countdown.copy(
                isRunning = false,
                elapsedTime = remaining,
                pausedAt = now
            )
            _countdowns.value = list
        }
    }
    
    // 重置倒计时
    fun resetCountdown(id: String) {
        val list = _countdowns.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            val original = list[index].originalTargetTime
            list[index] = list[index].copy(
                isRunning = false,
                targetTime = original,
                elapsedTime = original,
                startTime = 0L,
                pausedAt = 0L
            )
            _countdowns.value = list
        }
    }
    
    // 删除倒计时
    fun removeCountdown(id: String) {
        _countdowns.value = _countdowns.value.filter { it.id != id }
    }

    // 设置倒计时时长
    fun setCountdownDuration(id: String, duration: Long) {
        val list = _countdowns.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1 && !list[index].isRunning) {
            list[index] = list[index].copy(
                targetTime = duration,
                originalTargetTime = duration,
                elapsedTime = duration
            )
            _countdowns.value = list
        }
    }

    // 重命名计时器
    fun renameTimer(id: String, newName: String, type: TimerType) {
        if (type == TimerType.STOPWATCH) {
            val list = _stopwatches.value.toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) {
                list[index] = list[index].copy(name = newName)
                _stopwatches.value = list
            }
        } else {
            val list = _countdowns.value.toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) {
                list[index] = list[index].copy(name = newName)
                _countdowns.value = list
            }
        }
    }
    
    // 给倒计时增加时间
    fun addTimeToCountdown(id: String, additionalMs: Long) {
        val list = _countdowns.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            val countdown = list[index]
            if (countdown.isRunning) {
                // 运行中：增加 targetTime，同时调整 startTime 保证剩余时间正确
                val newTargetTime = countdown.targetTime + additionalMs
                val now = System.currentTimeMillis()
                val elapsed = now - countdown.startTime
                val newRemaining = (newTargetTime - elapsed).coerceAtLeast(0)
                list[index] = countdown.copy(
                    targetTime = newTargetTime,
                    elapsedTime = newRemaining
                )
            } else {
                // 停止中：直接增加 targetTime 和 elapsedTime
                val newTargetTime = countdown.targetTime + additionalMs
                val newRemaining = (countdown.elapsedTime + additionalMs).coerceAtLeast(0)
                list[index] = countdown.copy(
                    targetTime = newTargetTime,
                    elapsedTime = newRemaining
                )
            }
            _countdowns.value = list
        }
    }
    
    // 启动后台监控
    private fun startMonitor() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            while (true) {
                delay(10)
                
                val currentTime = System.currentTimeMillis()
                
                // === 更新秒表 ===
                val currentStopwatches = _stopwatches.value.toMutableList()
                var stopwatchesChanged = false
                
                currentStopwatches.forEachIndexed { index, stopwatch ->
                    if (stopwatch.isRunning) {
                        val elapsed = currentTime - stopwatch.startTime
                        if (currentStopwatches[index].elapsedTime != elapsed) {
                            currentStopwatches[index] = stopwatch.copy(elapsedTime = elapsed)
                            stopwatchesChanged = true
                        }
                    }
                }
                if (stopwatchesChanged) {
                    _stopwatches.value = currentStopwatches
                }
                
                // === 更新倒计时 ===
                val currentCountdowns = _countdowns.value.toMutableList()
                var countdownsChanged = false
                
                currentCountdowns.forEachIndexed { index, countdown ->
                    if (countdown.isRunning) {
                        val elapsed = currentTime - countdown.startTime
                        val remaining = (countdown.targetTime - elapsed).coerceAtLeast(0)
                        
                        if (remaining <= 0) {
                            if (countdown.isRunning) {
                                currentCountdowns[index] = countdown.copy(
                                    isRunning = false,
                                    elapsedTime = 0L
                                )
                                countdownsChanged = true
                                sendNotification(countdown.name)
                            }
                        } else if (currentCountdowns[index].elapsedTime != remaining) {
                            currentCountdowns[index] = countdown.copy(elapsedTime = remaining)
                            countdownsChanged = true
                        }
                    }
                }
                if (countdownsChanged) {
                    _countdowns.value = currentCountdowns
                }
            }
        }
    }
    
    // 发送通知
    private fun sendNotification(timerName: String) {
        val context = getApplication<Application>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "countdown_timer",
                "倒计时提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "倒计时结束时的通知"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent().apply {
            setClassName(context, "com.example.toolbox.function.daily.StopWatchActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationId = System.currentTimeMillis().toInt()
        val notification = NotificationCompat.Builder(context, "countdown_timer")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("倒计时结束")
            .setContentText("$timerName 已结束！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
}