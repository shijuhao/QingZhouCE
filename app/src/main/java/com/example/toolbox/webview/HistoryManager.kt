package com.example.toolbox.webview

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.toolbox.AppJson
import kotlinx.serialization.SerializationException

class HistoryManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("webview_history", Context.MODE_PRIVATE)

    companion object {
        private const val MAX_HISTORY_SIZE = 100
        private const val KEY_HISTORY = "history_list"
    }

    fun addToHistory(title: String, url: String) {
        if (url.startsWith("about:") || url.isEmpty()) return

        val history = getHistory().toMutableList()

        // 如果已存在相同URL，移除旧记录
        history.removeAll { it.url == url }

        // 添加新记录
        val newEntry = Bookmark(
            title = title.ifEmpty { url },
            url = url,
            timeAdded = System.currentTimeMillis()
        )
        history.add(0, newEntry)

        // 限制历史记录数量
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }

        saveHistory(history)
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