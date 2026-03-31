package com.example.toolbox.webview

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.toolbox.AppJson
import kotlinx.serialization.SerializationException

class BookmarkManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("webview_bookmarks", Context.MODE_PRIVATE)
    private val KEY_BOOKMARKS = "bookmarks_list"

    fun addBookmark(title: String, url: String): Boolean {
        if (url.startsWith("about:") || url.isEmpty()) return false

        val bookmarks = getBookmarks().toMutableList()

        // 检查是否已存在
        if (bookmarks.any { it.url == url }) {
            return false
        }

        // 添加新书签
        val newBookmark = Bookmark(
            title = title.ifEmpty { url },
            url = url,
            timeAdded = System.currentTimeMillis()
        )
        bookmarks.add(0, newBookmark)
        saveBookmarks(bookmarks)
        return true
    }

    fun removeBookmark(bookmark: Bookmark) {
        val bookmarks = getBookmarks().toMutableList()
        bookmarks.removeAll { it.id == bookmark.id }
        saveBookmarks(bookmarks)
    }

    fun removeBookmarkByUrl(url: String) {
        val bookmarks = getBookmarks().toMutableList()
        bookmarks.removeAll { it.url == url }
        saveBookmarks(bookmarks)
    }

    fun isBookmarked(url: String): Boolean {
        return getBookmarks().any { it.url == url }
    }

    fun getBookmarks(): List<Bookmark> {
        val json = prefs.getString(KEY_BOOKMARKS, "")
        if (json.isNullOrEmpty()) return emptyList()

        return try {
            AppJson.json.decodeFromString<List<Bookmark>>(json)
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    private fun saveBookmarks(bookmarks: List<Bookmark>) {
        val json = AppJson.json.encodeToString(bookmarks)
        prefs.edit { putString(KEY_BOOKMARKS, json) }
    }
}