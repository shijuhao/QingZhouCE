package com.example.toolbox.function.daily

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // 根据类型查找对应的计时器
    val timer = if (timerType == "stopwatch") {
        stopwatches.find { it.id == timerId }
    } else {
        countdowns.find { it.id == timerId }
    }

    if (timer == null) {
        // 找不到则关闭
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = formatTime(timer.elapsedTime),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!timer.isRunning) {
                    if (timerType == "stopwatch" || timer.elapsedTime > 0) {
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
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = if (timer.elapsedTime == 0L) "开始" else "继续",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
            
                    FilledTonalIconButton(
                        onClick = {
                            if (timerType == "stopwatch") {
                                viewModel.resetStopwatch(timer.id)
                            } else {
                                viewModel.resetCountdown(timer.id)
                            }
                        },
                        enabled = if (timerType == "stopwatch") timer.elapsedTime > 0 else true,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.RotateLeft,
                            contentDescription = "重置",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
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
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "暂停",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    if (timerType == "stopwatch") {
                        FilledTonalIconButton(
                            onClick = { viewModel.recordLap(timer.id) },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "计次",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
            
            // 计次记录
            if (timerType == "stopwatch" && timer.lapTimes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "计次记录",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(timer.lapTimes) { index, lapTime ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}