package com.example.toolbox.function.daily

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ui.theme.ToolBoxTheme

class MarkBoardActivity : ComponentActivity() {
    private val viewModel: MarkBoardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MarkBoardScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkBoardScreen(
    viewModel: MarkBoardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("计分板") },
            navigationIcon = {
                FilledTonalIconButton(onClick = { (context as ComponentActivity).finish() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 计时器区域
            TimerDisplay(
                displayTime = state.displayTime,
                timerRunning = state.timerRunning,
                lapTimes = state.lapTimes
            )

            // 计分板区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ScoreCard(
                    teamName = "队伍 A",
                    score = state.teamAScore,
                    onIncrement = { viewModel.incrementTeamA() },
                    onDecrement = { viewModel.decrementTeamA() },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ScoreCard(
                    teamName = "队伍 B",
                    score = state.teamBScore,
                    onIncrement = { viewModel.incrementTeamB() },
                    onDecrement = { viewModel.decrementTeamB() },
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            // 计时器控制按钮
            TimerControls(
                timerRunning = state.timerRunning,
                onStart = { viewModel.startTimer() },
                onPause = { viewModel.pauseTimer() },
                onRecordLap = { viewModel.recordLap() }
            )

            // 重置按钮
            ResetControls(
                onResetScores = { viewModel.resetScores() },
                onResetTimer = { viewModel.resetTimer() }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun TimerDisplay(
    displayTime: Long,
    timerRunning: Boolean,
    lapTimes: List<Long>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 优化字体大小：使用稍小的字体，与卡片协调
            Text(
                text = if (timerRunning || displayTime > 0) MarkBoardViewModel.formatTime(displayTime) else "等待开始",
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = if (timerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (lapTimes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "分段记录",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                lapTimes.forEachIndexed { index, lapTime ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "分段 ${index + 1}")
                        Text(
                            text = MarkBoardViewModel.formatTime(lapTime),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreCard(
    teamName: String,
    score: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = teamName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDecrement,
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("-", fontSize = 24.sp)
                }
                Button(
                    onClick = onIncrement,
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("+", fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun TimerControls(
    timerRunning: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onRecordLap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!timerRunning) {
                ActionButton(
                    onClick = onStart,
                    icon = { Icon(Icons.Default.PlayArrow, "开始") },
                    text = "开始",
                    modifier = Modifier.weight(1f)
                )
            } else {
                ActionButton(
                    onClick = onPause,
                    icon = { Icon(Icons.Default.Pause, "暂停") },
                    text = "暂停",
                    modifier = Modifier.weight(1f)
                )
            }

            ActionButton(
                onClick = onRecordLap,
                icon = { Icon(Icons.Default.Flag, "分段") },
                text = "分段",
                containerColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ResetControls(
    onResetScores: () -> Unit,
    onResetTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                onClick = onResetScores,
                icon = { Icon(Icons.Default.Delete, "重置分数") },
                text = "重置所有分数",
                containerColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "此操作会将两队分数重置为0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            ActionButton(
                onClick = onResetTimer,
                icon = { Icon(Icons.Default.RestartAlt, "重置计时器") },
                text = "重置计时器",
                containerColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    }
}