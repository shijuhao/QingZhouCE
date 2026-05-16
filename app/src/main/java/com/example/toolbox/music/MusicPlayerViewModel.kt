package com.example.toolbox.music

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

data class MusicPlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Int = 0,
    val duration: Int = 0,
    val currentMusic: MusicItem? = null,
    val musicList: List<MusicItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val volume: Float = 1.0f,
    val isLooping: Boolean = false,
    val isShuffle: Boolean = false,
    val scanMode: ScanMode = ScanMode.AUTO
)

enum class ScanMode {
    AUTO,       // 自动：先MediaStore，失败后文件扫描
    MEDIASTORE, // 仅MediaStore
    FILE        // 仅文件扫描
}

class MusicPlayerViewModel(private val context: Context) : ViewModel() {
    private var mediaPlayer: MediaPlayer? = null
    private var progressUpdateJob: kotlinx.coroutines.Job? = null
    
    private val _state = MutableStateFlow(MusicPlayerState())
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()
    
    init {
        loadScanModeFromPrefs()
    }
    
    private fun loadScanModeFromPrefs() {
        try {
            val prefs = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
            val savedMode = prefs.getString("scan_mode", "AUTO") ?: "AUTO"
            val mode = when (savedMode) {
                "MEDIASTORE" -> ScanMode.MEDIASTORE
                "FILE" -> ScanMode.FILE
                else -> ScanMode.AUTO
            }
            _state.update { it.copy(scanMode = mode) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun saveScanModeToPrefs(mode: ScanMode) {
        try {
            val prefs = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
            val modeString = when (mode) {
                ScanMode.AUTO -> "AUTO"
                ScanMode.MEDIASTORE -> "MEDIASTORE"
                ScanMode.FILE -> "FILE"
            }
            prefs.edit().putString("scan_mode", modeString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun loadMusicList(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // 检查权限
                if (!hasReadPermission(context)) {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = "缺少存储权限，请在设置中授予权限"
                        )
                    }
                    return@launch
                }
                
                val musicList = when (_state.value.scanMode) {
                    ScanMode.AUTO -> {
                        val mediaStoreList = scanMusicFilesMediaStore(context)
                        if (mediaStoreList.isEmpty()) {
                            scanMusicFilesDirect(context)
                        } else {
                            mediaStoreList
                        }
                    }
                    ScanMode.MEDIASTORE -> scanMusicFilesMediaStore(context)
                    ScanMode.FILE -> scanMusicFilesDirect(context)
                }
                
                _state.update { 
                    it.copy(
                        musicList = musicList,
                        isLoading = false,
                        error = if (musicList.isEmpty()) "未找到音乐文件，请检查权限和文件位置" else null
                    )
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = "加载音乐失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun hasReadPermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11, 12
                if (android.os.Environment.isExternalStorageManager()) {
                    true
                } else {
                    ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
            else -> {
                // Android 10 及以下
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    fun setScanMode(mode: ScanMode) {
        _state.update { it.copy(scanMode = mode) }
        saveScanModeToPrefs(mode)
    }
    
    private fun scanMusicFilesMediaStore(context: Context): List<MusicItem> {
        val musicList = mutableListOf<MusicItem>()
        
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Audio.Media.getContentUri(
                    android.provider.MediaStore.VOLUME_EXTERNAL
                )
            } else {
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            
            val projection = arrayOf(
                android.provider.MediaStore.Audio.Media._ID,
                android.provider.MediaStore.Audio.Media.TITLE,
                android.provider.MediaStore.Audio.Media.ARTIST,
                android.provider.MediaStore.Audio.Media.DURATION,
                android.provider.MediaStore.Audio.Media.DATA,
                android.provider.MediaStore.Audio.Media.ALBUM_ID,
                android.provider.MediaStore.Audio.Media.DISPLAY_NAME
            )
            
            val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${android.provider.MediaStore.Audio.Media.TITLE} ASC"
            
            android.util.Log.d("MusicScanner", "MediaStore query, Android SDK: ${Build.VERSION.SDK_INT}")
            
            val cursor = context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )
            
            android.util.Log.d("MusicScanner", "Cursor: ${cursor != null}, count: ${cursor?.count ?: 0}")
            
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                val durationColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                val dataColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                val albumIdColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM_ID)
                
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn) ?: "未知歌曲"
                    val artist = it.getString(artistColumn) ?: "未知艺术家"
                    val duration = it.getLong(durationColumn)
                    val filePath = it.getString(dataColumn) ?: ""
                    
                    val uri = collection.buildUpon()
                        .appendPath(id.toString())
                        .build()
                    
                    val albumArtUri = if (albumIdColumn >= 0) {
                        val albumId = it.getLong(albumIdColumn)
                        android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
                            .buildUpon()
                            .appendPath(albumId.toString())
                            .build()
                    } else null
                    
                    if (duration > 0) {
                        musicList.add(
                            MusicItem(
                                id = id,
                                title = title,
                                artist = artist,
                                duration = duration,
                                uri = uri,
                                albumArt = albumArtUri,
                                filePath = filePath
                            )
                        )
                    }
                }
            }
            
            android.util.Log.d("MusicScanner", "Found ${musicList.size} songs")
        } catch (e: Exception) {
            android.util.Log.e("MusicScanner", "Scan failed", e)
        }
        
        return musicList
    }
    
    private fun scanMusicFilesDirect(context: Context): List<MusicItem> {
        val musicList = mutableListOf<MusicItem>()
        val musicExtensions = listOf(".mp3", ".wav", ".flac", ".m4a", ".aac", ".ogg")
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+：只使用 MediaStore 查询
                android.util.Log.d("MusicScanner", "Android 10+, using MediaStore enhanced")
                return scanMusicFilesMediaStoreEnhanced(context)
            } else {
                // Android 9 及以下：直接文件扫描
                android.util.Log.d("MusicScanner", "Android 9-, using direct file scan")
                val externalDir = android.os.Environment.getExternalStorageDirectory()
                if (externalDir.exists() && externalDir.canRead()) {
                    scanDirectoryRecursive(externalDir, musicExtensions, musicList)
                } else {
                    android.util.Log.e("MusicScanner", "Cannot access external storage directory")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicScanner", "File scan failed", e)
            e.printStackTrace()
        }
        
        return musicList.sortedBy { it.title.lowercase() }
    }
    
    private fun scanMusicFilesMediaStoreEnhanced(context: Context): List<MusicItem> {
        android.util.Log.d("MusicScanner", "Starting enhanced MediaStore scan")
        
        try {
            val allMusic = scanMusicFilesMediaStore(context)
            
            if (allMusic.isNotEmpty()) {
                android.util.Log.d("MusicScanner", "Enhanced scan found ${allMusic.size} songs via general query")
                return allMusic
            }
            
            val musicList = mutableListOf<MusicItem>()
            val musicDirectories = listOf(
                "Music",
                "Download",
                "Downloads", 
                "Audio",
                "media/music",
                "media/audio",
                "DCIM",
                "Recordings",
                "Voice Recorder"
            )
            
            for (dir in musicDirectories) {
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    android.provider.MediaStore.Audio.Media.getContentUri(
                        android.provider.MediaStore.VOLUME_EXTERNAL
                    )
                } else {
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                
                val projection = arrayOf(
                    android.provider.MediaStore.Audio.Media._ID,
                    android.provider.MediaStore.Audio.Media.TITLE,
                    android.provider.MediaStore.Audio.Media.ARTIST,
                    android.provider.MediaStore.Audio.Media.DURATION,
                    android.provider.MediaStore.Audio.Media.ALBUM_ID,
                    android.provider.MediaStore.Audio.Media.RELATIVE_PATH,
                    android.provider.MediaStore.Audio.Media.DISPLAY_NAME
                )
                
                val selection = "${android.provider.MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? AND ${android.provider.MediaStore.Audio.Media.DURATION} > 0"
                val selectionArgs = arrayOf("%${dir}%")
                
                android.util.Log.d("MusicScanner", "Scanning directory: $dir")
                
                context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                    val durationColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                    
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: "未知歌曲"
                        val artist = cursor.getString(artistColumn) ?: "未知艺术家"
                        val duration = cursor.getLong(durationColumn)
                        
                        val uri = collection.buildUpon()
                            .appendPath(id.toString())
                            .build()
                        
                        if (duration > 0) {
                            musicList.add(
                                MusicItem(
                                    id = id,
                                    title = title,
                                    artist = artist,
                                    duration = duration,
                                    uri = uri
                                )
                            )
                        }
                    }
                }
            }
            
            android.util.Log.d("MusicScanner", "Directory-based scan found ${musicList.size} songs")
            return musicList
        } catch (e: Exception) {
            android.util.Log.e("MusicScanner", "Enhanced MediaStore scan failed", e)
            return emptyList()
        }
    }
    
    private fun scanDirectoryRecursive(
        directory: File,
        extensions: List<String>,
        musicList: MutableList<MusicItem>,
        maxDepth: Int = 8,
        currentDepth: Int = 0
    ) {
        if (currentDepth >= maxDepth) return
        
        try {
            val files = directory.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory && !file.name.startsWith(".")) {
                    scanDirectoryRecursive(file, extensions, musicList, maxDepth, currentDepth + 1)
                } else if (file.isFile) {
                    val fileName = file.name.lowercase()
                    if (extensions.any { fileName.endsWith(it) }) {
                        val duration = getAudioDuration(file.absolutePath)
                        if (duration > 0) {
                            val title = file.nameWithoutExtension
                            musicList.add(
                                MusicItem(
                                    id = System.currentTimeMillis() + musicList.size,
                                    title = title,
                                    artist = extractArtistFromMetadata(file.absolutePath) ?: "未知艺术家",
                                    duration = duration,
                                    uri = Uri.fromFile(file),
                                    filePath = file.absolutePath
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getAudioDuration(filePath: String): Long {
        return try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(filePath)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationStr?.toLongOrNull() ?: 0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    private fun extractArtistFromMetadata(filePath: String): String? {
        return try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(filePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun playMusic(musicItem: MusicItem) {
        stopMusic()
        
        try {
            mediaPlayer = MediaPlayer().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val fd = context.contentResolver.openFileDescriptor(musicItem.uri, "r")
                    fd?.let {
                        setDataSource(it.fileDescriptor)
                        it.close()
                    }
                } else {
                    setDataSource(context, musicItem.uri)
                }
                
                prepareAsync()
                setOnPreparedListener { mp ->
                    _state.update { 
                        it.copy(
                            currentMusic = musicItem,
                            isPlaying = true,
                            duration = mp.duration,
                            currentPosition = 0,
                            error = null
                        )
                    }
                    mp.start()
                    startProgressUpdate()
                }
                setOnErrorListener { _, what, extra ->
                    _state.update { 
                        it.copy(
                            error = "播放错误: $what, $extra",
                            isPlaying = false
                        )
                    }
                    true
                }
                setOnCompletionListener {
                    handleCompletion()
                }
            }
        } catch (e: IOException) {
            _state.update { 
                it.copy(
                    error = "无法播放音乐: ${e.message}",
                    isPlaying = false
                )
            }
        }
    }
    
    private fun handleCompletion() {
        if (_state.value.isLooping) {
            // 单曲循环
            mediaPlayer?.seekTo(0)
            mediaPlayer?.start()
        } else {
            // 播放下一首
            playNext()
        }
    }
    
    fun playNext() {
        val currentList = _state.value.musicList
        val currentMusic = _state.value.currentMusic
        
        // 添加空列表检查
        if (currentList.isEmpty()) return
        
        // 如果没有正在播放的歌曲，播放第一首
        if (currentMusic == null) {
            if (currentList.isNotEmpty()) {
                playMusic(currentList.first())
            }
            return
        }
        
        val currentIndex = currentList.indexOf(currentMusic)
        if (currentIndex == -1) {
            // 如果当前歌曲不在列表中，播放第一首
            playMusic(currentList.first())
            return
        }
        
        val nextIndex = if (_state.value.isShuffle) {
            var newIndex = (currentList.indices).random()
            while (newIndex == currentIndex && currentList.size > 1) {
                newIndex = (currentList.indices).random()
            }
            newIndex
        } else {
            (currentIndex + 1) % currentList.size
        }
        
        playMusic(currentList[nextIndex])
    }
    
    fun playPrevious() {
        val currentList = _state.value.musicList
        val currentMusic = _state.value.currentMusic
        
        if (currentList.isEmpty()) return
        if (currentMusic == null) {
            playMusic(currentList.first())
            return
        }
        
        val currentIndex = currentList.indexOf(currentMusic)
        if (currentIndex == -1) return
        
        val prevIndex = if (_state.value.isShuffle) {
            var newIndex = (currentList.indices).random()
            while (newIndex == currentIndex && currentList.size > 1) {
                newIndex = (currentList.indices).random()
            }
            newIndex
        } else {
            if (currentIndex - 1 < 0) currentList.size - 1 else currentIndex - 1
        }
        
        playMusic(currentList[prevIndex])
    }
    
    fun togglePlayPause() {
        mediaPlayer?.let { mp ->
            if (_state.value.isPlaying) {
                mp.pause()
                _state.update { it.copy(isPlaying = false) }
                stopProgressUpdate()
            } else {
                mp.start()
                _state.update { it.copy(isPlaying = true) }
                startProgressUpdate()
            }
        }
    }
    
    fun stopMusic() {
        stopProgressUpdate()
        mediaPlayer?.let { mp ->
            try {
                mp.stop()
                mp.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null
        _state.update { 
            it.copy(
                isPlaying = false,
                currentPosition = 0,
                duration = 0
            )
        }
    }
    
    fun seekTo(position: Int) {
        mediaPlayer?.let { mp ->
            mp.seekTo(position)
            _state.update { it.copy(currentPosition = position) }
        }
    }
    
    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(clampedVolume, clampedVolume)
        _state.update { it.copy(volume = clampedVolume) }
    }
    
    fun toggleLoop() {
        _state.update { it.copy(isLooping = !it.isLooping) }
    }
    
    fun toggleShuffle() {
        _state.update { it.copy(isShuffle = !it.isShuffle) }
    }
    
    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressUpdateJob = viewModelScope.launch {
            while (isActive && _state.value.isPlaying && mediaPlayer != null) {
                delay(500) // 更新频率 500ms
                try {
                    val currentPosition = mediaPlayer?.currentPosition ?: 0
                    if (currentPosition != _state.value.currentPosition) {
                        _state.update { it.copy(currentPosition = currentPosition) }
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
    }
    
    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopMusic()
    }
}