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
import androidx.compose.material3.BottomAppBar
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
import java.util.Locale

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
            TopAppBar(
                title = { Text("浏览记录与书签") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                SecondaryTabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
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
                        }
                        context.startActivity(intent)
                    },
                    onDeleteItem = { item ->
                        historyManager.deleteHistoryItem(item)
                        historyList = historyManager.getHistory()
                    },
                    onClearAll = {
                        historyManager.clearHistory()
                        historyList = emptyList()
                    },
                    isBookmarked = { url -> bookmarkManager.isBookmarked(url) },
                    onToggleBookmark = { title, url ->
                        if (bookmarkManager.isBookmarked(url)) {
                            bookmarkManager.removeBookmarkByUrl(url)
                        } else {
                            bookmarkManager.addBookmark(title, url)
                        }
                        bookmarksList = bookmarkManager.getBookmarks()
                    }
                )
                1 -> BookmarksTab(
                    bookmarksList = bookmarksList,
                    onItemClick = { url ->
                        val intent = Intent(context, WebViewActivity::class.java).apply {
                            putExtra("url", url)
                        }
                        context.startActivity(intent)
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
    onClearAll: () -> Unit,
    isBookmarked: (String) -> Boolean,
    onToggleBookmark: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onClearAll) {
                Icon(Icons.Default.Clear, contentDescription = "清空历史")
            }
        }

        if (historyList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无浏览历史", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn {
                items(historyList) { item ->
                    HistoryItem(
                        bookmark = item,
                        onClick = { onItemClick(item.url) },
                        onDelete = { onDeleteItem(item) },
                        isBookmarked = isBookmarked(item.url),
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
                    text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(bookmark.timeAdded)),
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