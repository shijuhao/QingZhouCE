package com.example.toolbox.function.daily

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ui.theme.ToolBoxTheme

class TimerDetailActivity : ComponentActivity() {
    private val viewModel: StopWatchViewModel by lazy {
        StopWatchViewModel.getInstance(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val timerId = intent.getStringExtra("timer_id") ?: return
        val timerType = intent.getStringExtra("timer_type") ?: return

        setContent {
            ToolBoxTheme {
                TimerDetailScreen(
                    timerId = timerId,
                    timerType = timerType,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerDetailScreen(
    timerId: String,
    timerType: String,
    viewModel: StopWatchViewModel
) {
    val context = LocalContext.current
    val stopwatches = viewModel.stopwatches.collectAsState().value
    val countdowns = viewModel.countdowns.collectAsState().value

    val timer = if (timerType == "stopwatch") {
        stopwatches.find { it.id == timerId }
    } else {
        countdowns.find { it.id == timerId }
    }

    if (timer == null) {
        LaunchedEffect(Unit) {
            (context as Activity).finish()
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(timer.name) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.8f to MaterialTheme.colorScheme.surface,
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧 - 重置
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (!timer.isRunning) {
                            IconButton(
                                onClick = {
                                    if (timerType == "stopwatch") {
                                        viewModel.resetStopwatch(timer.id)
                                    } else {
                                        viewModel.resetCountdown(timer.id)
                                    }
                                },
                                enabled = if (timerType == "stopwatch") timer.elapsedTime > 0 else true,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.RotateLeft,
                                    contentDescription = "重置",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                
                    // 中间 - 开始/暂停
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (timer.isRunning) {
                            FilledIconButton(
                                onClick = {
                                    if (timerType == "stopwatch") {
                                        viewModel.pauseStopwatch(timer.id)
                                    } else {
                                        viewModel.pauseCountdown(timer.id)
                                    }
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Default.Stop, "暂停", modifier = Modifier.size(32.dp))
                            }
                        } else if (timerType == "stopwatch" || timer.elapsedTime > 0) {
                            FilledIconButton(
                                onClick = {
                                    if (timerType == "stopwatch") {
                                        viewModel.startStopwatch(timer.id)
                                    } else {
                                        viewModel.startCountdown(timer.id)
                                    }
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, "开始", modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                
                    // 右侧 - 计次 / +1分钟
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (timer.isRunning && timerType == "stopwatch") {
                            IconButton(
                                onClick = { viewModel.recordLap(timer.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "计次",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else if (timerType == "countdown") {
                            TextButton(
                                onClick = { viewModel.addTimeToCountdown(timer.id, 60_000L) }
                            ) {
                                Text(
                                    "+1分钟",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 时间显示
            item {
                Spacer(modifier = Modifier.height(12.dp))
                
                if (timerType == "countdown") {
                    val remainingColor = if (timer.elapsedTime <= 0 && !timer.isRunning) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }

                    Text(
                        text = if (timer.elapsedTime <= 0 && !timer.isRunning) "时间到！"
                        else formatTime(timer.elapsedTime, false),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = remainingColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = formatTime(timer.elapsedTime),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 计次记录
            if (timerType == "stopwatch" && timer.lapTimes.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "计次记录",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }

                itemsIndexed(timer.lapTimes) { index, lapTime ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "计次 ${timer.lapTimes.size - index}",
                            fontSize = 16.sp
                        )
                        Text(
                            text = formatTime(lapTime),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }
    }
}