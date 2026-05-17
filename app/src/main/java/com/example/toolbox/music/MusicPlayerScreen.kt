@file:Suppress("AssignedValueIsNeverRead", "AssignedValueIsNeverRead")

package com.example.toolbox.music

import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    onMenuClick: () -> Unit,
    viewModel: MusicPlayerViewModel,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val state by viewModel.state.collectAsState()
    val currentRoute by navController.currentBackStackEntryAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.onPermissionResult(isGranted)
        }
    )
    
    LaunchedEffect(Unit) {
        viewModel.loadMusicList(context)
    }
    
    Scaffold(
        topBar = {
            when (currentRoute?.destination?.route) {
                "settings" -> {
                    TopAppBar(
                        title = { Text("播放器设置") },
                        navigationIcon = {
                            FilledTonalIconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                    )
                }
                "player" -> {
                    TopAppBar(
                        title = { Text("正在播放") },
                        navigationIcon = {
                            FilledTonalIconButton(onClick = { navController.popBackStack() }) {
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
                else -> {
                    TopAppBar(
                        title = { Text("音乐") },
                        navigationIcon = {
                            FilledTonalIconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = "菜单")
                            }
                        },
                        actions = {
                            IconButton(onClick = { navController.navigate("settings") }) {
                                Icon(Icons.Default.Settings, contentDescription = "设置")
                            }
                        }
                    )
                }
            }
        },
        bottomBar = {
            if (currentRoute?.destination?.route != "player" && state.musicList.isNotEmpty()) {
                MiniPlayer(
                    musicItem = state.currentMusic,
                    isPlaying = state.isPlaying,
                    currentPosition = state.currentPosition,
                    onPlayPauseClick = { 
                        if (state.currentMusic != null) {
                            viewModel.togglePlayPause() 
                        } else if (state.musicList.isNotEmpty()) {
                            viewModel.playMusic(state.musicList.first())
                        }
                    },
                    onClick = { navController.navigate("player") },
                    viewModel = viewModel
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
                    onMusicClick = { music ->
                        if (state.currentMusic?.id == music.id) {
                            navController.navigate("player")
                        } else {
                            viewModel.playMusic(music)
                        }
                    },
                    isLoading = state.isLoading,
                    needsPermission = state.needsPermission,
                    scanProgress = state.scanProgress,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_AUDIO)
                        }
                        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val hasReadPermission = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                                )
                            
                            if (!hasReadPermission) {
                                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            } else if (!Environment.isExternalStorageManager()) {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                                ).apply {
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        }
                        else {
                            permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    },
                    onScanClick = { viewModel.loadMusicList(context) },
                    viewModel = viewModel
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
    needsPermission: Boolean,
    scanProgress: String,
    onRequestPermission: () -> Unit,
    onScanClick: () -> Unit,
    viewModel: MusicPlayerViewModel
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                if (scanProgress.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = scanProgress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    } else if (needsPermission) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "需要存储权限",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请授予权限以扫描音乐文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onRequestPermission) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("授予权限")
                }
            }
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
                    isCurrentPlaying = currentMusic?.id == music.id,
                    isActuallyPlaying = currentMusic?.id == music.id && isPlaying,
                    onClick = { onMusicClick(music) },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun MusicListItem(
    musicItem: MusicItem,
    isCurrentPlaying: Boolean,
    isActuallyPlaying: Boolean,
    onClick: () -> Unit,
    viewModel: MusicPlayerViewModel
) {
    val context = LocalContext.current
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(musicItem.id) {
        withContext(Dispatchers.IO) {
            coverBitmap = viewModel.getAlbumArt(context, musicItem)
        }
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (coverBitmap != null) {
                Image(
                    bitmap = coverBitmap!!.asImageBitmap(),
                    contentDescription = "专辑封面",
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
            
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

            if (isCurrentPlaying) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = if (isActuallyPlaying) "播放中" else "已暂停",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun MiniPlayer(
    musicItem: MusicItem?,
    isPlaying: Boolean,
    currentPosition: Int,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit,
    viewModel: MusicPlayerViewModel
) {
    val state by viewModel.state.collectAsState()
    val currentLyricIndex = LyricParser.getCurrentLyricIndex(state.lyrics, currentPosition)
    val currentLyricText = if (state.showLyricsInMiniPlayer && currentLyricIndex >= 0 && currentLyricIndex < state.lyrics.size) {
        state.lyrics[currentLyricIndex].text
    } else null
    
    val context = LocalContext.current
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(musicItem?.id) {
        if (musicItem != null) {
            withContext(Dispatchers.IO) {
                coverBitmap = viewModel.getAlbumArt(context, musicItem)
            }
        }
    }
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (coverBitmap != null) {
                Image(
                    bitmap = coverBitmap!!.asImageBitmap(),
                    contentDescription = "专辑封面",
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = musicItem?.title ?: "未播放",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (state.showLyricsInMiniPlayer && currentLyricIndex >= 0 && currentLyricIndex < state.lyrics.size) {
                        state.lyrics[currentLyricIndex].text
                    } else {
                        musicItem?.artist ?: "点击播放音乐"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.showLyricsInMiniPlayer && currentLyricIndex >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
    viewModel: MusicPlayerViewModel
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> PlayerControllerPage(
                    musicItem = musicItem,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onPlayPauseClick = onPlayPauseClick,
                    onSeekTo = onSeekTo,
                    viewModel = viewModel,
                    state = state
                )
                1 -> LyricsPage(
                    lyrics = state.lyrics,
                    currentPosition = currentPosition
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(2) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 12.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isSelected) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                )
            }
        }
    }
}

@Composable
fun FolderPickerDialog(
    onDismiss: () -> Unit,
    onFolderSelected: (String) -> Unit,
    customFolders: List<String>,
    onRemoveFolder: (String) -> Unit
) {
    var showDirectoryChooser by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义扫描文件夹") },
        text = {
            Column {
                if (customFolders.isNotEmpty()) {
                    Text(
                        text = "已选择的文件夹：",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(customFolders) { folder ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = folder,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(onClick = { onRemoveFolder(folder) }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Text(
                    text = "点击按钮选择要扫描的文件夹",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { showDirectoryChooser = true }) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("选择文件夹")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )

    if (showDirectoryChooser) {
        DirectoryChooserDialog(
            onDismiss = { showDirectoryChooser = false },
            onDirectorySelected = { directory ->
                onFolderSelected(directory.absolutePath)
                showDirectoryChooser = false
            }
        )
    }
}

@Composable
fun DirectoryChooserDialog(
    onDismiss: () -> Unit,
    onDirectorySelected: (File) -> Unit
) {
    var currentDirectory by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择文件夹") },
        text = {
            Column {
                // 当前路径
                Text(
                    text = currentDirectory.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // 父目录按钮
                if (currentDirectory.parentFile != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentDirectory = currentDirectory.parentFile!! },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("..", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // 子目录列表
                val subDirs = currentDirectory.listFiles { file ->
                    file.isDirectory && !file.name.startsWith(".")
                }?.toList()?.sortedBy { it.name.lowercase() } ?: emptyList()
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(subDirs) { dir ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (dir.canRead()) {
                                        currentDirectory = dir
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dir.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDirectorySelected(currentDirectory) }) {
                Text("选择此文件夹")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
