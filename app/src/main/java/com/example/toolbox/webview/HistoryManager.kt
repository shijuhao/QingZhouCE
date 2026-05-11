package com.example.toolbox.webview

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.toolbox.AppJson
import kotlinx.serialization.SerializationException

class HistoryManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("webview_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HISTORY = "history_list"
    }

    fun addToHistory(title: String, url: String): Bookmark? {
        if (url.startsWith("about:") || url.isEmpty()) return null
    
        val history = getHistory().toMutableList()
    
        history.removeAll { it.url == url }
    
        val newEntry = Bookmark(
            title = title.ifEmpty { url },
            url = url,
            timeAdded = System.currentTimeMillis()
        )
        history.add(0, newEntry)
        
        saveHistory(history)
        return newEntry
    }

    fun getHistory(): List<Bookmark> {
        val json = prefs.getString(KEY_HISTORY, "")
        if (json.isNullOrEmpty()) return emptyList()

        return try {
            AppJson.json.decodeFromString<List<Bookmark>>(json)
        } catch (_: SerializationException) {
            emptyList()
        }
    }
    
    fun updateHistoryTitle(id: Long, title: String) {
        val history = getHistory().toMutableList()
        val index = history.indexOfFirst { it.id == id }
        if (index != -1 && title.isNotEmpty()) {
            history[index] = history[index].copy(title = title)
            saveHistory(history)
        }
    }

    fun clearHistory() {
        prefs.edit { remove(KEY_HISTORY) }
    }

    fun deleteHistoryItem(bookmark: Bookmark) {
        val history = getHistory().toMutableList()
        history.removeAll { it.id == bookmark.id }
        saveHistory(history)
    }

    private fun saveHistory(history: List<Bookmark>) {
        val json = AppJson.json.encodeToString(history)
        prefs.edit { putString(KEY_HISTORY, json) }
    }
}