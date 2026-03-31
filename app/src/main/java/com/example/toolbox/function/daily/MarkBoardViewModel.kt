package com.example.toolbox.function.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

data class MarkBoardState(
    val teamAScore: Int = 0,
    val teamBScore: Int = 0,
    val timerRunning: Boolean = false,
    val startTime: Long = 0L,
    val accumulatedTime: Long = 0L,
    val displayTime: Long = 0L,
    val lapTimes: List<Long> = emptyList()
)

class MarkBoardViewModel : ViewModel() {

    private val _state = MutableStateFlow(MarkBoardState())
    val state: StateFlow<MarkBoardState> = _state.asStateFlow()

    private var timerJob: Job? = null

    // 不再在 init 中启动，改为按需启动
    // 启动计时器更新循环
    private fun startTimerUpdates() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (true) {
                if (_state.value.timerRunning) {
                    val currentTime = calculateCurrentTime()
                    _state.update { it.copy(displayTime = currentTime) }
                }
                delay(16) // 约60fps
            }
        }
    }

    private fun stopTimerUpdates() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimerUpdates()
    }

    // 计分操作
    fun incrementTeamA() {
        _state.update { it.copy(teamAScore = it.teamAScore + 1) }
    }

    fun decrementTeamA() {
        _state.update { if (it.teamAScore > 0) it.copy(teamAScore = it.teamAScore - 1) else it }
    }

    fun incrementTeamB() {
        _state.update { it.copy(teamBScore = it.teamBScore + 1) }
    }

    fun decrementTeamB() {
        _state.update { if (it.teamBScore > 0) it.copy(teamBScore = it.teamBScore - 1) else it }
    }

    fun resetScores() {
        _state.update { it.copy(teamAScore = 0, teamBScore = 0) }
    }

    // 计时器操作
    fun startTimer() {
        if (!_state.value.timerRunning) {
            _state.update {
                it.copy(
                    timerRunning = true,
                    startTime = System.currentTimeMillis()
                )
            }
            startTimerUpdates()
        }
    }

    fun pauseTimer() {
        if (_state.value.timerRunning) {
            val currentTime = calculateCurrentTime()
            _state.update {
                it.copy(
                    timerRunning = false,
                    accumulatedTime = currentTime,
                    displayTime = currentTime
                )
            }
            stopTimerUpdates()
        }
    }

    fun resetTimer() {
        _state.update {
            it.copy(
                timerRunning = false,
                startTime = 0L,
                accumulatedTime = 0L,
                displayTime = 0L,
                lapTimes = emptyList()
            )
        }
        stopTimerUpdates()
    }

    fun recordLap() {
        if (_state.value.timerRunning) {
            val lapTime = calculateCurrentTime()
            _state.update {
                it.copy(lapTimes = it.lapTimes + lapTime)
            }
        }
    }

    // 辅助方法
    private fun calculateCurrentTime(): Long {
        val s = _state.value
        return if (s.timerRunning) {
            s.accumulatedTime + (System.currentTimeMillis() - s.startTime)
        } else {
            s.accumulatedTime
        }
    }

    companion object {
        fun formatTime(millis: Long): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
            val millisFormatted = (millis % 1000) / 10
            return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, millisFormatted)
        }
    }
}