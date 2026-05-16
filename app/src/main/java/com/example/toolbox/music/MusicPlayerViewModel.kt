package com.example.toolbox.music

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

class MusicPlayerViewModel(private val context: android.content.Context) : ViewModel() {
    private var mediaPlayer: MediaPlayer? = null
    
    private val _state = MutableStateFlow(MusicPlayerState())
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()
    
    init {
        // 从 SharedPreferences 加载保存的扫描模式
        loadScanModeFromPrefs()
    }
    
    private fun loadScanModeFromPrefs() {
        try {
            val prefs = context.getSharedPreferences("music_player_prefs", android.content.Context.MODE_PRIVATE)
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
            val prefs = context.getSharedPreferences("music_player_prefs", android.content.Context.MODE_PRIVATE)
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
            _state.update { it.copy(isLoading = true) }
            try {
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
    
    fun setScanMode(mode: ScanMode) {
        _state.update { it.copy(scanMode = mode) }
        saveScanModeToPrefs(mode)
    }
    
    private fun scanMusicFilesMediaStore(context: Context): List<MusicItem> {
        val musicList = mutableListOf<MusicItem>()
        val projection = arrayOf(
            android.provider.MediaStore.Audio.Media._ID,
            android.provider.MediaStore.Audio.Media.TITLE,
            android.provider.MediaStore.Audio.Media.ARTIST,
            android.provider.MediaStore.Audio.Media.DURATION,
            android.provider.MediaStore.Audio.Media.DATA
        )
        
        val sortOrder = "${android.provider.MediaStore.Audio.Media.TITLE} ASC"
        
        try {
            context.contentResolver.query(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "未知歌曲"
                    val artist = cursor.getString(artistColumn) ?: "未知艺术家"
                    val duration = cursor.getLong(durationColumn)
                    val filePath = cursor.getString(dataColumn) ?: ""
                    val uri = Uri.withAppendedPath(
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    
                    if (duration > 0) {
                        musicList.add(
                            MusicItem(
                                id = id,
                                title = title,
                                artist = artist,
                                duration = duration,
                                uri = uri,
                                filePath = filePath
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return musicList
    }
    
    private fun scanMusicFilesDirect(context: Context): List<MusicItem> {
        val musicList = mutableListOf<MusicItem>()
        val musicExtensions = listOf(".mp3", ".wav", ".flac", ".m4a", ".aac", ".ogg", ".wma")
        
        try {
            val externalDir = android.os.Environment.getExternalStorageDirectory()
            scanDirectoryRecursive(externalDir, musicExtensions, musicList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return musicList.sortedBy { it.title.lowercase() }
    }
    
    private fun scanDirectoryRecursive(
        directory: java.io.File,
        extensions: List<String>,
        musicList: MutableList<MusicItem>,
        maxDepth: Int = 10,
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
                        val uri = Uri.fromFile(file)
                        val mediaPlayer = MediaPlayer()
                        try {
                            mediaPlayer.setDataSource(file.absolutePath)
                            mediaPlayer.prepare()
                            val duration = mediaPlayer.duration.toLong()
                            mediaPlayer.release()
                            
                            if (duration > 0) {
                                val title = file.nameWithoutExtension
                                musicList.add(
                                    MusicItem(
                                        id = System.currentTimeMillis() + musicList.size,
                                        title = title,
                                        artist = "未知艺术家",
                                        duration = duration,
                                        uri = uri,
                                        filePath = file.absolutePath
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            mediaPlayer.release()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun playMusic(musicItem: MusicItem) {
        stopMusic()
        
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(musicItem.uri.toString())
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
                    start()
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
                    if (_state.value.isLooping) {
                        seekTo(0)
                        start()
                    } else {
                        _state.update { it.copy(isPlaying = false, currentPosition = 0) }
                    }
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
    
    fun togglePlayPause() {
        mediaPlayer?.let { mp ->
            if (_state.value.isPlaying) {
                mp.pause()
                _state.update { it.copy(isPlaying = false) }
            } else {
                mp.start()
                _state.update { it.copy(isPlaying = true) }
                startProgressUpdate()
            }
        }
    }
    
    fun stopMusic() {
        mediaPlayer?.let { mp ->
            mp.stop()
            mp.release()
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
        mediaPlayer?.seekTo(position)
        _state.update { it.copy(currentPosition = position) }
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
        viewModelScope.launch {
            while (_state.value.isPlaying && mediaPlayer != null) {
                kotlin.runCatching {
                    val currentPosition = mediaPlayer?.currentPosition ?: 0
                    _state.update { it.copy(currentPosition = currentPosition) }
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopMusic()
    }
}
