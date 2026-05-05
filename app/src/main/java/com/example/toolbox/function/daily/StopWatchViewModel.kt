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
            // 如果是首次启动，从0开始；如果是继续，从暂停时的累计时间开始
            val baseTime = if (stopwatch.pausedAt > 0) {
                // 计算暂停期间经过的时间
                now - stopwatch.elapsedTime - (now - stopwatch.pausedAt)
            } else {
                now - stopwatch.elapsedTime
            }
            list[index] = stopwatch.copy(
                isRunning = true,
                startTime = baseTime,
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
            val now = System.currentTimeMillis()
            list[index] = list[index].copy(
                isRunning = false,
                elapsedTime = now - list[index].startTime,
                pausedAt = now // 记录暂停时的系统时间
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

    // 更新秒表时间
    fun updateStopwatchTime(id: String, currentTime: Long) {
        val list = _stopwatches.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1 && list[index].isRunning) {
            list[index] = list[index].copy(
                elapsedTime = currentTime - list[index].startTime
            )
            _stopwatches.value = list
        }
    }

    // === 倒计时功能 ===

    // 添加新倒计时
    fun addCountdown(name: String = "倒计时 ${_countdowns.value.size + 1}", duration: Long = 60000) {
        val newCountdown = TimerInstance(
            name = name,
            targetTime = duration,
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
            // 计算剩余时间
            val remainingTime = if (countdown.pausedAt > 0) {
                // 从暂停时继续，计算已经经过的时间
                val elapsedDuringPause = now - countdown.pausedAt
                countdown.elapsedTime - elapsedDuringPause
            } else {
                countdown.elapsedTime
            }
            
            list[index] = countdown.copy(
                isRunning = true,
                elapsedTime = maxOf(0L, remainingTime),
                startTime = now,
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
            val now = System.currentTimeMillis()
            val countdown = list[index]
            val elapsed = now - countdown.startTime
            val remaining = maxOf(0L, countdown.elapsedTime - elapsed)
            
            list[index] = countdown.copy(
                isRunning = false,
                elapsedTime = remaining,
                pausedAt = now // 记录暂停时的系统时间
            )
            _countdowns.value = list
        }
    }

    // 重置倒计时
    fun resetCountdown(id: String) {
        val list = _countdowns.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            list[index] = list[index].copy(
                isRunning = false,
                elapsedTime = list[index].targetTime,
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
    
    // 启动后台监控
    private fun startMonitor() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            while (true) {
                delay(10) // 每10ms检查一次，保证毫秒级精度
                val currentTime = System.currentTimeMillis()
                
                // 更新秒表
                _stopwatches.value.filter { it.isRunning }.forEach { stopwatch ->
                    updateStopwatchTime(stopwatch.id, currentTime)
                }
                
                // 更新倒计时并检查是否结束
                val countdowns = _countdowns.value
                countdowns.filter { it.isRunning }.forEach { countdown ->
                    val elapsed = currentTime - countdown.startTime
                    val remaining = countdown.targetTime - elapsed
                    
                    if (remaining <= 0) {
                        // 倒计时结束，发送通知
                        sendNotification(countdown.name)
                        // 停止倒计时
                        pauseCountdown(countdown.id)
                        // 重置为0
                        val list = _countdowns.value.toMutableList()
                        val index = list.indexOfFirst { it.id == countdown.id }
                        if (index != -1) {
                            list[index] = list[index].copy(elapsedTime = 0L)
                            _countdowns.value = list
                        }
                    } else {
                        // 更新剩余时间
                        val list = _countdowns.value.toMutableList()
                        val index = list.indexOfFirst { it.id == countdown.id }
                        if (index != -1) {
                            list[index] = list[index].copy(elapsedTime = remaining)
                            _countdowns.value = list
                        }
                    }
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