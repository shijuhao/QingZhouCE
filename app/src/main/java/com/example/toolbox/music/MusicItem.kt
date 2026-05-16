package com.example.toolbox.music

import android.net.Uri

data class MusicItem(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: Uri,
    val albumArt: Uri? = null,
    val filePath: String = ""
) {
    fun formatDuration(): String {
        val minutes = duration / 1000 / 60
        val seconds = (duration / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
