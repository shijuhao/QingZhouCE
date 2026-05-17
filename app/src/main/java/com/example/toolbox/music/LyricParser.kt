package com.example.toolbox.music

data class LyricLine(
    val timestamp: Long,
    val text: String
)

object LyricParser {
    fun parseLrc(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        
        val timeRegex = Regex("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{2,3}))?]")
        
        lrcContent.lines().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEach
            
            val matches = timeRegex.findAll(trimmedLine).toList()
            if (matches.isEmpty()) return@forEach
            
            var text = trimmedLine
            var lastTimestamp = 0L
            
            matches.forEach { match ->
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val millisecondsStr = match.groupValues[3]
                
                val milliseconds = when (millisecondsStr.length) {
                    2 -> millisecondsStr.toLong() * 10
                    3 -> millisecondsStr.toLong()
                    else -> 0
                }
                
                lastTimestamp = minutes * 60 * 1000 + seconds * 1000 + milliseconds
                
                val matchStart = match.range.first
                val matchEnd = match.range.last + 1
                text = text.substring(0, matchStart) + text.substring(matchEnd)
            }
            
            text = text.trim()
            
            if (text.isNotEmpty() && !text.startsWith("[") && !text.contains(":")) {
                lines.add(LyricLine(lastTimestamp, text))  // 使用 lastTimestamp
            }
        }
        
        return lines.sortedBy { it.timestamp }
    }
    
    fun getCurrentLyricIndex(lyrics: List<LyricLine>, currentPosition: Int): Int {
        if (lyrics.isEmpty()) return -1
        
        val currentMs = currentPosition.toLong()
        var resultIndex = -1
        
        for (i in lyrics.indices) {
            if (currentMs >= lyrics[i].timestamp) {
                resultIndex = i
            } else {
                break
            }
        }
        
        return resultIndex
    }
}