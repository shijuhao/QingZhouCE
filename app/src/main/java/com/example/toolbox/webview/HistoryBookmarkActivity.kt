package com.example.toolbox.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.toolbox.ui.theme.ToolBoxTheme
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.platform.LocalLocale

data class HistoryGroup(
    val label: String,
    val items: List<Bookmark>
)

class HistoryBookmarkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                HistoryBookmarkScreen(onBackClick = { finish() })
            }
        }
    }
}

fun groupHistoryByTime(history: List<Bookmark>): List<HistoryGroup> {
    val now = System.currentTimeMillis()
    val calendar = java.util.Calendar.getInstance()

    // 今天 0 点
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    val todayStart = calendar.timeInMillis

    // 昨天 0 点
    val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L
    // 7 天前 0 点
    val weekStart = todayStart - 7 * 24 * 60 * 60 * 1000L
    // 30 天前 0 点
    val monthStart = todayStart - 30L * 24 * 60 * 60 * 1000L

    val grouped = LinkedHashMap<String, MutableList<Bookmark>>()

    for (item in history) {
        val label = when {
            item.timeAdded >= todayStart -> "今天"
            item.timeAdded >= yesterdayStart -> "昨天"
            item.timeAdded >= weekStart -> "7 天内"
            item.timeAdded >= monthStart -> "30 天内"
            else -> "更早"
        }
        grouped.getOrPut(label) { mutableListOf() }.add(item)
    }

    return grouped.map { (label, items) -> HistoryGroup(label, items) }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SimpleDateFormat")
@Composable
fun HistoryBookmarkScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val historyManager = remember { HistoryManager(context) }
    val bookmarkManager = remember { BookmarkManager(context) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var historyList by remember { mutableStateOf(historyManager.getHistory()) }
    var bookmarksList by remember { mutableStateOf(bookmarkManager.getBookmarks()) }

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("浏览记录与书签") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        if (selectedTab == 0 && historyList.isNotEmpty()) {
                            IconButton(onClick = {
                                historyManager.clearHistory()
                                historyList = emptyList()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "清空历史")
                            }
                        }
                    }
                )
                
                SecondaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("历史记录") },
                        icon = { Icon(Icons.Default.History, null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("书签") },
                        icon = { Icon(Icons.Default.Bookmark, null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedTab) {
                0 -> HistoryTab(
                    historyList = historyList,
                    onItemClick = { url ->
                        val intent = Intent(context, WebViewActivity::class.java).apply {
                            putExtra("url", url)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        context.startActivity(intent)
                        (context as? ComponentActivity)?.finish()
                    },
                    onDeleteItem = { item ->
                        historyManager.deleteHistoryItem(item)
                        historyList = historyManager.getHistory()
                    },
                    isBookmarked = { url -> bookmarkManager.isBookmarked(url) },
                    onToggleBookmark = { title, url ->
                        if (bookmarkManager.isBookmarked(url)) {
                            bookmarkManager.removeBookmarkByUrl(url)
                        } else {
                            bookmarkManager.addBookmark(title, url)
                        }
                        bookmarksList = bookmarkManager.getBookmarks()
                        historyList = historyManager.getHistory()
                    }
                )
                1 -> BookmarksTab(
                    bookmarksList = bookmarksList,
                    onItemClick = { url ->
                        val intent = Intent(context, WebViewActivity::class.java).apply {
                            putExtra("url", url)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        context.startActivity(intent)
                        (context as? ComponentActivity)?.finish()
                    },
                    onDeleteItem = { item ->
                        bookmarkManager.removeBookmark(item)
                        bookmarksList = bookmarkManager.getBookmarks()
                    }
                )
            }
        }
    }
}

@Composable
fun HistoryTab(
    historyList: List<Bookmark>,
    onItemClick: (String) -> Unit,
    onDeleteItem: (Bookmark) -> Unit,
    isBookmarked: (String) -> Boolean,
    onToggleBookmark: (String, String) -> Unit
) {
    if (historyList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无浏览历史", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        val groups = remember(historyList) { groupHistoryByTime(historyList) }

        LazyColumn {
            groups.forEach { group ->
                item(key = "header_${group.label}") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = group.label,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                items(group.items, key = { it.id }) { item ->
                    val bookmarked = isBookmarked(item.url)
                    HistoryItem(
                        bookmark = item,
                        onClick = { onItemClick(item.url) },
                        onDelete = { onDeleteItem(item) },
                        isBookmarked = bookmarked,
                        onToggleBookmark = { onToggleBookmark(item.title, item.url) }
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarksTab(
    bookmarksList: List<Bookmark>,
    onItemClick: (String) -> Unit,
    onDeleteItem: (Bookmark) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (bookmarksList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无书签", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn {
                items(bookmarksList) { item ->
                    BookmarkItem(
                        bookmark = item,
                        onClick = { onItemClick(item.url) },
                        onDelete = { onDeleteItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit
) {
    val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val todayStart = calendar.timeInMillis
    val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L
    
    val timeText = when {
        bookmark.timeAdded >= todayStart -> "今天 " + SimpleDateFormat("HH:mm", LocalLocale.current.platformLocale).format(Date(bookmark.timeAdded))
        bookmark.timeAdded >= yesterdayStart -> "昨天 " + SimpleDateFormat("HH:mm", LocalLocale.current.platformLocale).format(Date(bookmark.timeAdded))
        else -> SimpleDateFormat("yyyy-MM-dd HH:mm", LocalLocale.current.platformLocale).format(Date(bookmark.timeAdded))
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bookmark.url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = onToggleBookmark) {
                Icon(
                    if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isBookmarked) "取消书签" else "添加书签",
                    modifier = Modifier.size(20.dp),
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun BookmarkItem(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Bookmark,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bookmark.url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}