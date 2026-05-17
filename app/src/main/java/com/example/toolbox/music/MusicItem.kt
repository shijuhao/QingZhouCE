package com.example.toolbox.music

import android.net.Uri
import java.util.Locale

data class MusicItem(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: Uri,
    val filePath: String = "",
    val lyricPath: String? = null
) {
    fun formatDuration(): String {
        val minutes = duration / 1000 / 60
        val seconds = (duration / 1000) % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}