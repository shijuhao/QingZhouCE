package com.example.toolbox.music

data class LyricLine(
    val timestamp: Long,
    val text: String
)

object LyricParser {
    fun parseLrc(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)")
        
        lrcContent.lines().forEach { line ->
            val matchResult = regex.find(line.trim())
            if (matchResult != null) {
                val minutes = matchResult.groupValues[1].toLong()
                val seconds = matchResult.groupValues[2].toLong()
                val milliseconds = matchResult.groupValues[3].padEnd(3, '0').toLong()
                val text = matchResult.groupValues[4].trim()
                
                val timestamp = minutes * 60 * 1000 + seconds * 1000 + milliseconds
                if (text.isNotEmpty()) {
                    lines.add(LyricLine(timestamp, text))
                }
            }
        }
        
        return lines.sortedBy { it.timestamp }
    }
    
    fun getCurrentLyricIndex(lyrics: List<LyricLine>, currentPosition: Int): Int {
        if (lyrics.isEmpty()) return -1
        
        val currentMs = currentPosition.toLong()
        for (i in lyrics.indices.reversed()) {
            if (currentMs >= lyrics[i].timestamp) {
                return i
            }
        }
        return -1
    }
}
