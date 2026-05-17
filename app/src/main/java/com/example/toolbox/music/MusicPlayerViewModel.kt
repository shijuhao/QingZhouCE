package com.example.toolbox.music

import android.graphics.BitmapFactory
import android.util.LruCache
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import androidx.core.content.edit

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
    val scanMode: ScanMode = ScanMode.AUTO,
    val customFolders: List<String> = emptyList(),
    val needsPermission: Boolean = false,
    val scanProgress: String = "",
    val lyrics: List<LyricLine> = emptyList(),
    val showLyricsInMiniPlayer: Boolean = false
)

enum class ScanMode {
    AUTO,       // 自动：先MediaStore，失败后文件扫描
    MEDIASTORE, // 仅MediaStore
    FILE        // 仅文件扫描
}

class MusicPlayerViewModel(private val application: android.app.Application) : ViewModel() {
    private var mediaPlayer: MediaPlayer? = null
    private var progressUpdateJob: kotlinx.coroutines.Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isPreparing = false // 防止重复准备
    private var hasStarted = false // 防止重复启动
    private var mediaSession: MediaSession? = null
    private val audioManager: AudioManager by lazy {
        application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private val notificationManager: NotificationManager by lazy {
        application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val musicCache = mutableMapOf<String, List<MusicItem>>()
    private val cacheTimestamp = mutableMapOf<String, Long>()
    private val cacheValidDuration = 24 * 60 * 60 * 1000L
    private val coverCache = LruCache<String, Bitmap>(50)
    
    private val _state = MutableStateFlow(MusicPlayerState())
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()
    
    init {
        loadScanModeFromPrefs()
        loadCustomFoldersFromPrefs()
        loadLyricsSettingsFromPrefs()
        initializeMediaSession()
    }
    
    private fun initializeMediaSession() {
        try {
            val sessionActivity = PendingIntent.getActivity(
                application,
                0,
                Intent(application, com.example.toolbox.MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            
            mediaSession = MediaSession(application, "MusicPlayerSession").apply {
                setSessionActivity(sessionActivity)
                setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
                
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        if (!_state.value.isPlaying) {
                            togglePlayPause()
                        }
                    }
                    
                    override fun onPause() {
                        if (_state.value.isPlaying) {
                            togglePlayPause()
                        }
                    }
                    
                    override fun onSkipToNext() {
                        playNext()
                    }
                    
                    override fun onSkipToPrevious() {
                        playPrevious()
                    }
                    
                    override fun onStop() {
                        stopMusic()
                    }
                })
                
                isActive = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadCustomFoldersFromPrefs() {
        try {
            val prefs = application.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
            val foldersJson = prefs.getString("custom_folders", "[]") ?: "[]"
            val folders = if (foldersJson == "[]") emptyList() else {
                foldersJson.trim('[', ']').split(",").map { it.trim().trim('"') }.filter { it.isNotEmpty() }
            }
            _state.update { it.copy(customFolders = folders) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun saveCustomFoldersToPrefs(folders: List<String>) {
        try {
            val prefs = application.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
            val foldersJson = if (folders.isEmpty()) "[]" else {
                "[${folders.joinToString(",") { "\"$it\"" }}]"
            }
            prefs.edit {putString("custom_folders", foldersJson)}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun addCustomFolder(folderPath: String) {
        val currentFolders = _state.value.customFolders.toMutableList()
        if (!currentFolders.contains(folderPath)) {
            currentFolders.add(folderPath)
            _state.update { it.copy(customFolders = currentFolders) }
            saveCustomFoldersToPrefs(currentFolders)
        }
    }
    
    fun removeCustomFolder(folderPath: String) {
        val currentFolders = _state.value.customFolders.toMutableList()
        currentFolders.remove(folderPath)
        _state.update { it.copy(customFolders = currentFolders) }
        saveCustomFoldersToPrefs(currentFolders)
    }
    
    private fun loadScanModeFromPrefs() {
        try {
            val prefs = application.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
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
            val prefs = application.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
            val modeString = when (mode) {
                ScanMode.AUTO -> "AUTO"
                ScanMode.MEDIASTORE -> "MEDIASTORE"
                ScanMode.FILE -> "FILE"
            }
            prefs.edit {putString("scan_mode", modeString)}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getAlbumArt(context: Context, musicItem: MusicItem): Bitmap? {
        val key = musicItem.id.toString()
        val cached = coverCache.get(key)
        if (cached != null) {
            return cached
        }
        
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, musicItem.uri)
            val artwork = retriever.embeddedPicture
            retriever.release()
            
            if (artwork != null) {
                val bitmap = BitmapFactory.decodeByteArray(artwork, 0, artwork.size)
                bitmap?.let { coverCache.put(key, it) }
                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun loadLyricsSettingsFromPrefs() {
        try {
            val prefs = application.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
            val showLyrics = prefs.getBoolean("show_lyrics_mini_player", false)
            _state.update { it.copy(showLyricsInMiniPlayer = showLyrics) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun setShowLyricsInMiniPlayer(show: Boolean) {
        _state.update { it.copy(showLyricsInMiniPlayer = show) }
        try {
            val prefs = application.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
            prefs.edit { putBoolean("show_lyrics_mini_player", show) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "music_player_channel",
                "音乐播放器",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "音乐播放控制通知"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun getAlbumArtBitmap(musicItem: MusicItem): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(application, musicItem.uri)
            val artwork = retriever.embeddedPicture
            retriever.release()
            
            if (artwork != null) {
                android.graphics.BitmapFactory.decodeByteArray(artwork, 0, artwork.size)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun updateNotification() {
        val currentMusic = _state.value.currentMusic ?: return
        
        createNotificationChannel()
        
        val albumArt = getAlbumArtBitmap(currentMusic)
        
        // 根据设置决定显示歌词还是歌手
        val subtitle = if (_state.value.showLyricsInMiniPlayer) {
            val currentLyricIndex = LyricParser.getCurrentLyricIndex(
                _state.value.lyrics,
                _state.value.currentPosition
            )
            if (currentLyricIndex >= 0 && currentLyricIndex < _state.value.lyrics.size) {
                _state.value.lyrics[currentLyricIndex].text
            } else {
                currentMusic.artist
            }
        } else {
            currentMusic.artist
        }
        
        val playPauseAction = if (_state.value.isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "暂停",
                createPendingIntent("pause")
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "播放",
                createPendingIntent("play")
            )
        }
        
        val notification = NotificationCompat.Builder(application, "music_player_channel")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentMusic.title)
            .setContentText(subtitle)
            .setLargeIcon(albumArt)
            .addAction(
                android.R.drawable.ic_media_previous,
                "上一首",
                createPendingIntent("previous")
            )
            .addAction(playPauseAction)
            .addAction(
                android.R.drawable.ic_media_next,
                "下一首",
                createPendingIntent("next")
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
        
        notificationManager.notify(1, notification)
        
        // 更新MediaSession状态
        updateMediaSessionState()
    }
    
    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(application, com.example.toolbox.MainActivity::class.java).apply {
            putExtra("notification_action", action)
        }
        return PendingIntent.getActivity(
            application,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun updateMediaSessionState() {
        mediaSession?.let { session ->
            val state = if (_state.value.isPlaying) {
                PlaybackState.STATE_PLAYING
            } else {
                PlaybackState.STATE_PAUSED
            }
            
            session.setPlaybackState(
                PlaybackState.Builder()
                    .setState(
                        state,
                        _state.value.currentPosition.toLong(),
                        1.0f
                    )
                    .setActions(
                        PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_STOP
                    )
                    .build()
            )
            
            _state.value.currentMusic?.let { music ->
                session.setMetadata(
                    android.media.MediaMetadata.Builder()
                        .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, music.title)
                        .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, music.artist)
                        .putLong(android.media.MediaMetadata.METADATA_KEY_DURATION, music.duration)
                        .build()
                )
            }
        }
    }
    
    fun clearNotification() {
        notificationManager.cancel(1)
        mediaSession?.isActive = false
    }
    
    fun loadLyrics(musicItem: MusicItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val lyrics = if (musicItem.lyricPath != null) {
                loadLyricsFromFile(musicItem.lyricPath)
            } else if (musicItem.filePath.isNotEmpty()) {
                loadLyricsFromAudioFile(musicItem.filePath)
            } else {
                emptyList()
            }
            _state.update { it.copy(lyrics = lyrics) }
        }
    }
    
    private fun loadLyricsFromAudioFile(audioFilePath: String): List<LyricLine> {
        return try {
            val lyricsText = ID3LyricExtractor.extractLyrics(audioFilePath)
            
            if (!lyricsText.isNullOrEmpty()) {
                android.util.Log.d("LyricExtractor", "Found embedded lyrics, length: ${lyricsText.length}")
                LyricParser.parseLrc(lyricsText)
            } else {
                android.util.Log.d("LyricExtractor", "No embedded lyrics found")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("LyricExtractor", "Failed to extract lyrics", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun loadLyricsFromFile(lyricPath: String): List<LyricLine> {
        return try {
            val file = File(lyricPath)
            if (file.exists()) {
                val content = file.readText()
                LyricParser.parseLrc(content)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun findLyricFile(musicFilePath: String): String? {
        if (musicFilePath.isEmpty()) return null
        
        val musicFile = File(musicFilePath)
        val musicDir = musicFile.parent
        val musicName = musicFile.nameWithoutExtension
        
        val possibleNames = listOf(
            "$musicName.lrc",
            "$musicName.LRC",
            "${musicName}.lrc"
        )
        
        for (name in possibleNames) {
            val lyricFile = File(musicDir, name)
            if (lyricFile.exists()) {
                android.util.Log.d("LyricFinder", "Found lyric file: ${lyricFile.absolutePath}")
                return lyricFile.absolutePath
            }
        }
        
        return null
    }
    
    fun loadMusicList(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, scanProgress = "正在检查权限...") }
            try {
                if (!hasReadPermission(context)) {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            needsPermission = true,
                            error = "需要存储权限才能扫描音乐文件"
                        )
                    }
                    return@launch
                }

                _state.update { it.copy(needsPermission = false) }
                
                val musicList = when (_state.value.scanMode) {
                    ScanMode.AUTO -> {
                        android.util.Log.d("MusicScanner", "Starting AUTO scan mode")
                        _state.update { it.copy(scanProgress = "正在使用 MediaStore 扫描...") }
                        val mediaStoreList = scanMusicFilesMediaStore(context)
                        if (mediaStoreList.isEmpty()) {
                            val customFolders = _state.value.customFolders
                            if (customFolders.isNotEmpty()) {
                                android.util.Log.d("MusicScanner", "Scanning ${customFolders.size} custom folders")
                                scanCustomFoldersParallel(customFolders)
                            } else {
                                android.util.Log.d("MusicScanner", "Fallback to direct file scan")
                                _state.update { it.copy(scanProgress = "正在扫描整个存储...") }
                                scanMusicFilesDirect(context)
                            }
                        } else {
                            android.util.Log.d("MusicScanner", "MediaStore found ${mediaStoreList.size} songs")
                            mediaStoreList
                        }
                    }
                    ScanMode.MEDIASTORE -> {
                        android.util.Log.d("MusicScanner", "Starting MEDIASTORE scan mode")
                        _state.update { it.copy(scanProgress = "正在使用 MediaStore 扫描...") }
                        scanMusicFilesMediaStore(context)
                    }
                    ScanMode.FILE -> {
                        android.util.Log.d("MusicScanner", "Starting FILE scan mode")
                        val customFolders = _state.value.customFolders
                        if (customFolders.isNotEmpty()) {
                            android.util.Log.d("MusicScanner", "Scanning ${customFolders.size} custom folders")
                            scanCustomFoldersParallel(customFolders)
                        } else {
                            android.util.Log.d("MusicScanner", "Scanning entire external storage")
                            _state.update { it.copy(scanProgress = "正在扫描整个存储...") }
                            scanMusicFilesDirect(context)
                        }
                    }
                }
                
                android.util.Log.d("MusicScanner", "Total songs found: ${musicList.size}")
                _state.update { 
                    it.copy(
                        musicList = musicList,
                        isLoading = false,
                        scanProgress = "",
                        error = if (musicList.isEmpty()) "未找到音乐文件，请检查权限和文件位置" else null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicScanner", "Scan failed with exception", e)
                _state.update { 
                    it.copy(
                        isLoading = false,
                        scanProgress = "",
                        error = "加载音乐失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            android.util.Log.d("MusicScanner", "Permission granted, reloading music list")
            _state.update { it.copy(needsPermission = false) }
            loadMusicList(application)
        } else {
            android.util.Log.w("MusicScanner", "Permission denied")
            _state.update { 
                it.copy(
                    needsPermission = true,
                    error = "权限被拒绝，无法扫描音乐文件"
                )
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

                    if (duration > 0) {
                        val lyricPath = findLyricFile(filePath)
                        musicList.add(
                            MusicItem(
                                id = id,
                                title = title,
                                artist = artist,
                                duration = duration,
                                uri = uri,
                                filePath = filePath,
                                lyricPath = lyricPath
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
                android.util.Log.d("MusicScanner", "Android 10+, using MediaStore enhanced")
                return scanMusicFilesMediaStoreEnhanced(context)
            } else {
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

    private suspend fun scanCustomFoldersParallel(folders: List<String>): List<MusicItem> {
        android.util.Log.d("MusicScanner", "Starting parallel scan for ${folders.size} folders")

        return kotlinx.coroutines.coroutineScope {
            val deferredResults = folders.mapIndexed { index, folderPath ->
                async(Dispatchers.IO) {
                    try {
                        val folder = File(folderPath)
                        if (folder.exists() && folder.isDirectory) {
                            val cached = getCachedMusic(folderPath)
                            if (cached != null) {
                                android.util.Log.d("MusicScanner", "Using cache for: $folderPath (${cached.size} songs)")
                                _state.update { it.copy(scanProgress = "正在扫描... ($index/${folders.size}) [缓存]") }
                                cached
                            } else {
                                android.util.Log.d("MusicScanner", "Scanning folder: $folderPath")
                                _state.update { it.copy(scanProgress = "正在扫描... ($index/${folders.size})") }
                                val result = scanSingleFolder(folder)
                                saveCache(folderPath, result)
                                result
                            }
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MusicScanner", "Failed to scan folder: $folderPath", e)
                        emptyList()
                    }
                }
            }

            val results = deferredResults.awaitAll()
            val musicList = results.flatten().sortedBy { it.title.lowercase() }
            
            android.util.Log.d("MusicScanner", "Parallel scan found ${musicList.size} songs")
            musicList
        }
    }

    private fun getCachedMusic(folderPath: String): List<MusicItem>? {
        val timestamp = cacheTimestamp[folderPath] ?: return null
        val now = System.currentTimeMillis()

        if (now - timestamp > cacheValidDuration) {
            cacheTimestamp.remove(folderPath)
            return musicCache.remove(folderPath)
        }
        
        return musicCache[folderPath]
    }
    
    private fun saveCache(folderPath: String, musicList: List<MusicItem>) {
        musicCache[folderPath] = musicList
        cacheTimestamp[folderPath] = System.currentTimeMillis()
    }
    
    fun clearCache() {
        musicCache.clear()
        cacheTimestamp.clear()
    }

    private fun scanSingleFolder(folder: File): List<MusicItem> {
        val musicList = mutableListOf<MusicItem>()
        val musicExtensions = listOf(".mp3", ".wav", ".flac", ".m4a", ".aac", ".ogg")
        scanDirectoryRecursive(folder, musicExtensions, musicList, maxDepth = 5)
        return musicList
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
            val audioFiles = mutableListOf<File>()
            
            for (file in files) {
                if (file.isDirectory && !file.name.startsWith(".")) {
                    scanDirectoryRecursive(file, extensions, musicList, maxDepth, currentDepth + 1)
                } else if (file.isFile) {
                    val fileName = file.name.lowercase()
                    if (extensions.any { fileName.endsWith(it) }) {
                        audioFiles.add(file)
                    }
                }
            }

            val retriever = MediaMetadataRetriever()
            for (file in audioFiles) {
                try {
                    retriever.setDataSource(file.absolutePath)
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val duration = durationStr?.toLongOrNull() ?: 0
                    
                    if (duration > 0) {
                        val title = file.nameWithoutExtension
                        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?.takeIf { it.isNotBlank() } ?: "未知艺术家"
                        val lyricPath = findLyricFile(file.absolutePath)
                        
                        musicList.add(
                            MusicItem(
                                id = System.currentTimeMillis() + musicList.size,
                                title = title,
                                artist = artist,
                                duration = duration,
                                uri = Uri.fromFile(file),
                                filePath = file.absolutePath,
                                lyricPath = lyricPath
                            )
                        )
                    }
                } catch (_: Exception) {
                    // 跳过无法读取的文件
                }
            }
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun playMusic(musicItem: MusicItem) {
        // 如果正在播放同一首歌，不做任何操作
        if (_state.value.currentMusic?.id == musicItem.id && _state.value.isPlaying) {
            return
        }
        
        // 防止重复准备
        if (isPreparing) {
            android.util.Log.w("MusicPlayer", "Already preparing, ignore this request")
            return
        }
        
        stopMusic()
        isPreparing = true
        
        // 请求音频焦点
        requestAudioFocus()
        
        try {
            mediaPlayer = MediaPlayer().apply {
                // 设置音频属性
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                setAudioAttributes(audioAttributes)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val fd = application.contentResolver.openFileDescriptor(musicItem.uri, "r")
                    fd?.let {
                        setDataSource(it.fileDescriptor)
                        it.close()
                    }
                } else {
                    setDataSource(application, musicItem.uri)
                }
                
                prepareAsync()
                setOnPreparedListener { mp ->
                    isPreparing = false
                    if (hasStarted) {
                        android.util.Log.w("MusicPlayer", "Already started, ignore")
                        return@setOnPreparedListener
                    }
                    hasStarted = true
                    
                    val currentVolume = _state.value.volume
                    mp.setVolume(currentVolume, currentVolume)
                    
                    _state.update { 
                        it.copy(
                            currentMusic = musicItem,
                            isPlaying = true,
                            duration = mp.duration,
                            currentPosition = 0,
                            error = null
                        )
                    }
                    loadLyrics(musicItem)
                    mp.start()
                    startProgressUpdate()
                    updateNotification()
                }
                setOnErrorListener { _, what, extra ->
                    isPreparing = false
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
            isPreparing = false
            _state.update { 
                it.copy(
                    error = "无法播放音乐: ${e.message}",
                    isPlaying = false
                )
            }
        }
    }
    
    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setOnAudioFocusChangeListener { focusChange ->
                        when (focusChange) {
                            AudioManager.AUDIOFOCUS_LOSS -> {
                                // 永久失去焦点，停止播放
                                stopMusic()
                            }
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                                // 暂时失去焦点，暂停播放
                                mediaPlayer?.pause()
                                _state.update { it.copy(isPlaying = false) }
                            }
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                                // 可以降低音量
                                mediaPlayer?.setVolume(0.3f, 0.3f)
                            }
                            AudioManager.AUDIOFOCUS_GAIN -> {
                                // 重新获得焦点，恢复音量
                                mediaPlayer?.setVolume(_state.value.volume, _state.value.volume)
                            }
                        }
                    }
                    build()
                }
                audioFocusRequest?.let {
                    audioManager.requestAudioFocus(it)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        
        if (currentList.isEmpty()) return
        
        if (currentMusic == null) {
            playMusic(currentList.first())
            return
        }
        
        val currentIndex = currentList.indexOf(currentMusic)
        if (currentIndex == -1) {
            playMusic(currentList.first())
            return
        }
        
        val nextIndex = if (_state.value.isShuffle) {
            if (currentList.size == 1) {
                0
            } else {
                var newIndex = (currentList.indices).random()
                while (newIndex == currentIndex && currentList.size > 1) {
                    newIndex = (currentList.indices).random()
                }
                newIndex
            }
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
            if (currentList.size == 1) {
                0
            } else {
                var newIndex = (currentList.indices).random()
                while (newIndex == currentIndex && currentList.size > 1) {
                    newIndex = (currentList.indices).random()
                }
                newIndex
            }
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
            updateNotification()
        }
    }
    
    fun stopMusic() {
        isPreparing = false
        hasStarted = false
        stopProgressUpdate()
        abandonAudioFocus()
        clearNotification()
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
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
            var lastNotificationUpdate = 0L
            while (isActive && _state.value.isPlaying && mediaPlayer != null) {
                delay(100)
                try {
                    val currentPosition = mediaPlayer?.currentPosition ?: 0
                    if (currentPosition != _state.value.currentPosition) {
                        _state.update { it.copy(currentPosition = currentPosition) }
                        
                        val now = System.currentTimeMillis()
                        if (now - lastNotificationUpdate > 5000) {
                            updateNotification()
                            lastNotificationUpdate = now
                        }
                    }
                } catch (_: Exception) {
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