package com.example.toolbox.webview

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.example.toolbox.ui.theme.ToolBoxTheme

class WebViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val url = intent.getStringExtra("url") ?: "https://www.bing.com"
        setContent {
            ToolBoxTheme {
                WebViewScreen(initialUrl = url, onBackClick = { finish() })
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(initialUrl: String, onBackClick: () -> Unit) {
    val context = LocalContext.current

    // 使用数据类合并状态，减少重组
    data class WebViewUiState(
        val url: String = initialUrl,
        val title: String = "加载中...",
        val progress: Float = 0f,
        val canGoBack: Boolean = false,
        val canGoForward: Boolean = false
    )

    var uiState by remember { mutableStateOf(WebViewUiState()) }
    var webView: WebView? by remember { mutableStateOf(null) }
    var showMenu by remember { mutableStateOf(false) }

    // 处理物理返回键：如果 WebView 可后退则后退，否则关闭 Activity
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    // 在 Composable 销毁时释放 WebView 资源，避免内存泄漏
    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.loadUrl("about:blank")
            webView?.destroy()
            webView = null
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = uiState.url,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "关闭")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("添加到书签") },
                                onClick = {
                                    showMenu = false
                                    val bookmarkManager = BookmarkManager(context)
                                    if (bookmarkManager.addBookmark(uiState.title, uiState.url)) {
                                        Toast.makeText(context, "已添加到书签", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "该书签已存在", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.BookmarkBorder,
                                        null,
                                        Modifier.size(18.dp)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("查看历史/书签") },
                                onClick = {
                                    showMenu = false
                                    val intent = Intent(context, HistoryBookmarkActivity::class.java)
                                    context.startActivity(intent)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.History,
                                        null,
                                        Modifier.size(18.dp)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("用系统浏览器打开") },
                                onClick = {
                                    showMenu = false
                                    val intent = Intent(Intent.ACTION_VIEW, uiState.url.toUri())
                                    context.startActivity(intent)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.WebAsset,
                                        null,
                                        Modifier.size(18.dp)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("复制网址") },
                                onClick = {
                                    showMenu = false
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("URL", uiState.url)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT)
                                        .show()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        null,
                                        Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                // 进度条：仅在 progress < 1.0f 时显示（即加载未完成）
                if (uiState.progress < 1.0f) {
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        bottomBar = {
            BottomAppBar {
                IconButton(
                    onClick = { webView?.goBack() },
                    enabled = uiState.canGoBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "后退")
                }
                IconButton(
                    onClick = { webView?.goForward() },
                    enabled = uiState.canGoForward,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "前进")
                }
                IconButton(
                    onClick = {
                        if (uiState.progress < 1.0f) {
                            webView?.stopLoading()
                        } else {
                            webView?.reload()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (uiState.progress < 1.0f) Icons.Filled.Close else Icons.Filled.Refresh,
                        contentDescription = if (uiState.progress < 1.0f) "停止" else "刷新"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // WebView 基础设置
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                uiState = uiState.copy(progress = newProgress / 100f)
                            }

                            override fun onReceivedTitle(view: WebView?, newTitle: String?) {
                                newTitle?.let {
                                    uiState = uiState.copy(title = it)
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ) = false // 让 WebView 自行处理

                            override fun onPageStarted(
                                view: WebView?,
                                urlStr: String?,
                                favicon: Bitmap?
                            ) {
                                uiState = uiState.copy(
                                    url = urlStr ?: uiState.url,
                                    canGoBack = view?.canGoBack() ?: false,
                                    canGoForward = view?.canGoForward() ?: false
                                )
                                val historyManager = HistoryManager(context)
                                historyManager.addToHistory(view?.title ?: "", urlStr ?: "")
                            }

                            override fun onPageFinished(view: WebView?, urlStr: String?) {
                                uiState = uiState.copy(
                                    url = urlStr ?: uiState.url,
                                    title = view?.title ?: uiState.title,
                                    canGoBack = view?.canGoBack() ?: false,
                                    canGoForward = view?.canGoForward() ?: false,
                                    progress = 1.0f // 确保进度条隐藏
                                )
                            }
                        }

                        setDownloadListener { downloadUrl, _, contentDisposition, mimetype, _ ->
                            // 检查外部存储状态
                            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                                Toast.makeText(ctx, "外部存储不可用", Toast.LENGTH_LONG).show()
                                return@setDownloadListener
                            }
                            try {
                                val fileName =
                                    URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype)
                                val request = DownloadManager.Request(downloadUrl.toUri()).apply {
                                    setMimeType(mimetype)
                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    setDestinationInExternalPublicDir(
                                        Environment.DIRECTORY_DOWNLOADS,
                                        fileName
                                    )
                                    setTitle(fileName)
                                    setDescription("正在下载文件...")
                                    setAllowedOverRoaming(true)
                                    setAllowedOverMetered(true)
                                }
                                val dm =
                                    ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                dm.enqueue(request)
                                Toast.makeText(ctx, "开始下载: $fileName", Toast.LENGTH_SHORT)
                                    .show()
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "下载失败: ${e.message}", Toast.LENGTH_LONG)
                                    .show()
                            }
                        }

                        // 加载初始 URL
                        loadUrl(initialUrl)
                        webView = this
                    }
                },
                update = { /* 不需要额外更新 */ },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}