package com.example.toolbox.function.daily

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.FloatingActionButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.delay
import java.util.Locale

class StopWatchActivity : ComponentActivity() {
    private val viewModel: StopWatchViewModel by lazy {
        StopWatchViewModel.getInstance(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    viewModel: StopWatchViewModel
) {
    val context = LocalContext.current
    val stopwatches = viewModel.stopwatches.collectAsState().value
    val countdowns = viewModel.countdowns.collectAsState().value
    
    // 请求通知权限（Android 13+）
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.widget.Toast.makeText(context, "已授予通知权限", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    // Android 13+ 需要请求通知权限
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("秒表 & 倒计时") },
            navigationIcon = {
                FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }
        )

        SecondaryTabRow(selectedTabIndex = viewModel.selectedTab) {
            Tab(
                selected = viewModel.selectedTab == 0,
                onClick = { viewModel.selectTab(0) },
                text = { Text("秒表 (${stopwatches.size})") }
            )
            Tab(
                selected = viewModel.selectedTab == 1,
                onClick = { viewModel.selectTab(1) },
                text = { Text("倒计时 (${countdowns.size})") }
            )
        }

        when (viewModel.selectedTab) {
            0 -> StopwatchTab(stopwatches, viewModel)
            1 -> CountdownTab(countdowns, viewModel)
        }
    }
}

@Composable
fun StopwatchTab(
    stopwatches: List<TimerInstance>,
    viewModel: StopWatchViewModel
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(stopwatches, key = { stopwatch -> stopwatch.id }) { stopwatch ->
                StopwatchCard(stopwatch, viewModel)
            }
        }

        FloatingActionButton(
            onClick = { viewModel.addStopwatch() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加秒表")
        }
    }
}

@Suppress("AssignedValueIsNeverRead")
@Composable
fun StopwatchCard(
    stopwatch: TimerInstance,
    viewModel: StopWatchViewModel
) {
    val context = LocalContext.current
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(stopwatch.name) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        onClick = {
            val intent = android.content.Intent(context, TimerDetailActivity::class.java)
            intent.putExtra("timer_id", stopwatch.id)
            intent.putExtra("timer_type", "stopwatch")
            context.startActivity(intent)
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stopwatch.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
                
                IconButton(onClick = { 
                    newName = stopwatch.name
                    showRenameDialog = true 
                }) {
                    Icon(Icons.Default.Edit, "重命名", modifier = Modifier.size(20.dp))
                }
                
                IconButton(onClick = { viewModel.removeStopwatch(stopwatch.id) }) {
                    Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                }
            }

            Text(
                text = formatTime(stopwatch.elapsedTime),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!stopwatch.isRunning) {
                    FilledIconButton(
                        onClick = { viewModel.startStopwatch(stopwatch.id) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = if (stopwatch.elapsedTime == 0L) "开始" else "继续",
                            modifier = Modifier.size(28.dp)
                        )
                    }
            
                    FilledTonalIconButton(
                        onClick = { viewModel.resetStopwatch(stopwatch.id) },
                        enabled = stopwatch.elapsedTime > 0,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.RotateLeft,
                            contentDescription = "重置",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    FilledIconButton(
                        onClick = { viewModel.pauseStopwatch(stopwatch.id) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "暂停",
                            modifier = Modifier.size(28.dp)
                        )
                    }
            
                    FilledTonalIconButton(
                        onClick = { viewModel.recordLap(stopwatch.id) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "计次",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            if (stopwatch.lapTimes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.height(150.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(stopwatch.lapTimes) { index, lapTime ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("计次 ${stopwatch.lapTimes.size - index}", fontSize = 14.sp)
                            Text(formatTime(lapTime), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名秒表") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.renameTimer(stopwatch.id, newName, TimerType.STOPWATCH)
                    }
                    showRenameDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                Button(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun CountdownTab(
    countdowns: List<TimerInstance>,
    viewModel: StopWatchViewModel
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(countdowns, key = { countdown -> countdown.id }) { countdown ->
                CountdownCard(countdown, viewModel)
            }
        }

        FloatingActionButton(
            onClick = { viewModel.addCountdown() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加倒计时")
        }
    }
}

@Suppress("AssignedValueIsNeverRead")
@Composable
fun CountdownCard(
    countdown: TimerInstance,
    viewModel: StopWatchViewModel
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(countdown.name) }

    val totalSeconds = (countdown.targetTime / 1000).toInt()
    var hours by remember { mutableIntStateOf(totalSeconds / 3600) }
    var minutes by remember { mutableIntStateOf((totalSeconds % 3600) / 60) }
    var seconds by remember { mutableIntStateOf(totalSeconds % 60) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        onClick = {
            val intent = android.content.Intent(context, TimerDetailActivity::class.java)
            intent.putExtra("timer_id", countdown.id)
            intent.putExtra("timer_type", "countdown")
            context.startActivity(intent)
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = countdown.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
                
                IconButton(onClick = { 
                    newName = countdown.name
                    showRenameDialog = true 
                }) {
                    Icon(Icons.Default.Edit, "重命名", modifier = Modifier.size(20.dp))
                }
                
                IconButton(onClick = { viewModel.removeCountdown(countdown.id) }) {
                    Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                }
            }

            val remainingColor = if (countdown.elapsedTime <= 0 && !countdown.isRunning) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }

            Text(
                text = if (countdown.elapsedTime <= 0 && !countdown.isRunning) "时间到！" else formatTime(countdown.elapsedTime, false),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = remainingColor,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!countdown.isRunning) {
                    FilledTonalIconButton(
                        onClick = { viewModel.resetCountdown(countdown.id) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.RotateLeft,
                            contentDescription = "重置",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    if (countdown.elapsedTime > 0) {
                        FilledIconButton(
                            onClick = { viewModel.startCountdown(countdown.id) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "开始",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    FilledTonalIconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = "设置时长",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    FilledIconButton(
                        onClick = { viewModel.pauseCountdown(countdown.id) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "暂停",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("设置倒计时时长") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("时", style = MaterialTheme.typography.bodySmall)
                            NumberPicker(
                                value = hours,
                                onValueChange = { hours = it },
                                range = 0..23
                            )
                        }
                        
                        Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("分", style = MaterialTheme.typography.bodySmall)
                            NumberPicker(
                                value = minutes,
                                onValueChange = { minutes = it },
                                range = 0..59
                            )
                        }
                        
                        Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("秒", style = MaterialTheme.typography.bodySmall)
                            NumberPicker(
                                value = seconds,
                                onValueChange = { seconds = it },
                                range = 0..59
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val totalMillis = ((hours * 3600 + minutes * 60 + seconds) * 1000).toLong()
                    if (totalMillis > 0) {
                        viewModel.setCountdownDuration(countdown.id, totalMillis)
                    }
                    showEditDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 重命名对话框
    if (showRenameDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名倒计时") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.renameTimer(countdown.id, newName, TimerType.COUNTDOWN)
                    }
                    showRenameDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                Button(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    val locale = remember { Locale.getDefault() }
    
    Column(
        modifier = Modifier
            .width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = {
                if (value < range.last) {
                    onValueChange(value + 1)
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "增加",
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = String.format(locale, "%02d", value),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        IconButton(
            onClick = {
                if (value > range.first) {
                    onValueChange(value - 1)
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "减少",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun formatTime(millis: Long, showMs: Boolean = true): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val base = if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
    return if (showMs) {
        val ms = (millis % 1000) / 10
        "$base.${String.format(Locale.getDefault(), "%02d", ms)}"
    } else {
        base
    }
}