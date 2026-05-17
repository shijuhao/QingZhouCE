package com.example.toolbox.music

import java.io.File
import java.io.RandomAccessFile

object ID3LyricExtractor {
    
    fun extractLyrics(audioFilePath: String): String? {
        return try {
            val file = File(audioFilePath)
            if (!file.exists()) return null
            
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(10)
                raf.read(header)
                
                val tag = String(header.copyOf(3))
                if (tag != "ID3") return null
                
                val version = header[3].toInt() and 0xFF
                val size = decodeSynchsafeInt(header.copyOfRange(6, 10))
                
                var offset = 10L
                val endOffset = 10L + size
                
                while (offset < endOffset) {
                    if (offset + 10 > file.length()) break
                    
                    raf.seek(offset)
                    val frameHeader = ByteArray(10)
                    raf.read(frameHeader)
                    
                    val frameId = String(frameHeader.copyOf(4))
                    val frameSize = when (version) {
                        3 -> decodeSynchsafeInt(frameHeader.copyOfRange(4, 8))
                        4 -> decodeSynchsafeInt(frameHeader.copyOfRange(4, 8))
                        else -> Integer.parseInt(String(formatBytes(frameHeader.copyOfRange(4, 8))), 16)
                    }
                    
                    if (frameSize <= 0 || offset + 10 + frameSize > file.length()) break
                    
                    if (frameId == "USLT" || frameId == "SYLT") {
                        raf.seek(offset + 10)
                        val frameData = ByteArray(frameSize)
                        raf.read(frameData)
                        
                        val lyrics = parseUSLTFrame(frameData, version)
                        if (lyrics != null) {
                            return lyrics
                        }
                    }
                    
                    offset += 10 + frameSize
                }
            }
            
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun parseUSLTFrame(data: ByteArray, @Suppress("UNUSED_PARAMETER") version: Int): String? {
        if (data.size < 7) return null
            
        var offset = 0
            
        val encoding = data[offset++].toInt() and 0xFF
            
        offset += 3

        val contentTypeEnd = indexOfNull(data, offset)
        if (contentTypeEnd == -1) return null
        offset = contentTypeEnd + 1
            
        val descriptionEnd = indexOfNull(data, offset)
        if (descriptionEnd == -1) return null
        offset = descriptionEnd + 1
            
        if (offset >= data.size) return null
            
        val lyricsBytes = data.copyOfRange(offset, data.size)
            
        return when (encoding) {
            0, 3 -> String(lyricsBytes, charset("ISO-8859-1"))
            1 -> {
                if (lyricsBytes.size >= 2) {
                    val bom = (lyricsBytes[0].toInt() and 0xFF shl 8) or (lyricsBytes[1].toInt() and 0xFF)
                    val charset = if (bom == 0xFEFF) "UTF-16BE" else "UTF-16LE"
                    String(lyricsBytes, charset(charset))
                } else {
                    String(lyricsBytes, charset("UTF-16"))
                }
            }
            2 -> String(lyricsBytes, charset("UTF-16BE"))
            else -> String(lyricsBytes, charset("ISO-8859-1"))
        }
    }
    
    private fun indexOfNull(data: ByteArray, start: Int): Int {
        for (i in start until data.size) {
            if (data[i] == 0.toByte()) return i
        }
        return -1
    }
    
    private fun decodeSynchsafeInt(bytes: ByteArray): Int {
        var result = 0
        for (byte in bytes) {
            result = (result shl 7) or (byte.toInt() and 0x7F)
        }
        return result
    }
    
    private fun formatBytes(bytes: ByteArray): ByteArray {
        return bytes
    }
}
