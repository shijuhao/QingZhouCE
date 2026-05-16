package com.example.toolbox.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.example.toolbox.settings.SettingsCustomItem
import com.example.toolbox.settings.SettingsGroup
import com.example.toolbox.settings.SettingsItemCell

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: MusicPlayerViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MusicPlayerViewModel(context) as T
            }
        }
    )
    val navController = rememberNavController()
    val state by viewModel.state.collectAsState()
    val currentRoute by navController.currentBackStackEntryAsState()
    val isSettingsPage = currentRoute?.destination?.route == "settings"
    
    LaunchedEffect(Unit) {
        viewModel.loadMusicList(context)
    }
    
    Scaffold(
        topBar = {
            if (!isSettingsPage) {
                TopAppBar(
                    title = { Text("音乐") },
                    navigationIcon = {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("播放器设置") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentRoute?.destination?.route != "player" && state.currentMusic != null) {
                MiniPlayer(
                    musicItem = state.currentMusic!!,
                    isPlaying = state.isPlaying,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onClick = { navController.navigate("player") }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "list",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("list") {
                MusicListScreen(
                    musicList = state.musicList,
                    currentMusic = state.currentMusic,
                    isPlaying = state.isPlaying,
                    onMusicClick = { viewModel.playMusic(it) },
                    isLoading = state.isLoading,
                    error = state.error,
                    onScanClick = { viewModel.loadMusicList(context) }
                )
            }
            composable("player") {
                FullScreenPlayer(
                    musicItem = state.currentMusic,
                    isPlaying = state.isPlaying,
                    currentPosition = state.currentPosition,
                    duration = state.duration,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onClose = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }
            composable("settings") {
                MusicSettingsScreen(
                    onScanClick = { viewModel.loadMusicList(context) },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun MusicListScreen(
    musicList: List<MusicItem>,
    currentMusic: MusicItem?,
    isPlaying: Boolean,
    onMusicClick: (MusicItem) -> Unit,
    isLoading: Boolean,
    error: String?,
    onScanClick: () -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (musicList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.MusicOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "没有找到音乐文件",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请检查存储权限或手动扫描",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onScanClick) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("手动扫描")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(musicList) { music ->
                MusicListItem(
                    musicItem = music,
                    isCurrentPlaying = currentMusic?.id == music.id && isPlaying,
                    onClick = { onMusicClick(music) }
                )
            }
        }
    }
}

@Composable
fun MusicListItem(
    musicItem: MusicItem,
    isCurrentPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlaying) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isCurrentPlaying) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isCurrentPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = musicItem.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = musicItem.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = musicItem.formatDuration(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MiniPlayer(
    musicItem: MusicItem,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = musicItem.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = musicItem.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayer(
    musicItem: MusicItem?,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onClose: () -> Unit,
    viewModel: MusicPlayerViewModel
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("正在播放") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 更多功能 */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // 专辑封面 - 圆角正方形
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .sizeIn(maxWidth = 320.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (musicItem?.albumArt != null) {
                        AsyncImage(
                            model = musicItem.albumArt,
                            contentDescription = "专辑封面",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 音乐信息
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = musicItem?.title ?: "未知歌曲",
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = musicItem?.artist ?: "未知艺术家",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 进度条区域
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 自定义进度条
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { newValue ->
                        if (duration > 0) {
                            onSeekTo((newValue * duration).toInt())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 播放模式按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 随机播放按钮
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "随机播放",
                        tint = if (state.isShuffle) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // 循环模式按钮（支持三种模式：单曲循环、列表循环、关闭）
                IconButton(
                    onClick = { viewModel.toggleLoop() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (state.isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "循环播放",
                        tint = if (state.isLooping) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 播放控制按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 上一首
                IconButton(
                    onClick = { viewModel.playPrevious() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "上一首",
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // 播放/暂停按钮
                FloatingActionButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // 下一首
                IconButton(
                    onClick = { viewModel.playNext() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 音量控制区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeDown,
                    contentDescription = "减小音量",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Slider(
                    value = state.volume,
                    onValueChange = { viewModel.setVolume(it) },
                    modifier = Modifier.weight(1f),
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
                
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "增大音量",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MusicSettingsScreen(
    onScanClick: () -> Unit,
    viewModel: MusicPlayerViewModel
) {
    val state by viewModel.state.collectAsState()
    var showQualityDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SettingsGroup(
                title = "音乐库",
                items = listOf(
                    {
                        SettingsItemCell(
                            icon = Icons.Default.Refresh,
                            title = "手动扫描音乐",
                            subtitle = "重新扫描设备上的音乐文件",
                            onClick = onScanClick
                        )
                    }
                )
            )
        }
        
        item {
            SettingsGroup(
                title = "扫描方式",
                items = listOf(
                    {
                        SettingsCustomItem(onClick = { viewModel.setScanMode(ScanMode.AUTO) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "自动扫描",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "优先使用MediaStore，失败后使用文件扫描",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = state.scanMode == ScanMode.AUTO,
                                    onClick = { viewModel.setScanMode(ScanMode.AUTO) }
                                )
                            }
                        }
                    },
                    {
                        SettingsCustomItem(onClick = { viewModel.setScanMode(ScanMode.MEDIASTORE) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "MediaStore扫描",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "使用系统媒体库，速度快",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = state.scanMode == ScanMode.MEDIASTORE,
                                    onClick = { viewModel.setScanMode(ScanMode.MEDIASTORE) }
                                )
                            }
                        }
                    },
                    {
                        SettingsCustomItem(onClick = { viewModel.setScanMode(ScanMode.FILE) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "文件扫描",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "直接扫描文件系统，更全面但较慢",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = state.scanMode == ScanMode.FILE,
                                    onClick = { viewModel.setScanMode(ScanMode.FILE) }
                                )
                            }
                        }
                    }
                )
            )
        }
        
        item {
            SettingsGroup(
                title = "音质设置",
                items = listOf(
                    {
                        SettingsItemCell(
                            icon = Icons.Default.MusicNote,
                            title = "音质选择",
                            subtitle = "标准音质",
                            onClick = { showQualityDialog = true }
                        )
                    }
                )
            )
        }
        
        item {
            SettingsGroup(
                title = "均衡器",
                items = listOf(
                    {
                        SettingsItemCell(
                            icon = Icons.Default.Equalizer,
                            title = "均衡器设置",
                            subtitle = "未启用",
                            onClick = { showEqualizerDialog = true }
                        )
                    }
                )
            )
        }
    }
    
    // 音质选择对话框
    if (showQualityDialog) {
        val qualities = listOf("标准音质", "高品质", "无损音质")
        var selectedQuality by remember { mutableIntStateOf(0) }
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("选择音质") },
            text = {
                Column {
                    qualities.forEachIndexed { index, quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedQuality = index },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedQuality == index,
                                onClick = { selectedQuality = index }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(quality)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 均衡器对话框
    if (showEqualizerDialog) {
        AlertDialog(
            onDismissRequest = { showEqualizerDialog = false },
            title = { Text("均衡器") },
            text = { Text("均衡器功能暂未实现") },
            confirmButton = {
                TextButton(onClick = { showEqualizerDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
