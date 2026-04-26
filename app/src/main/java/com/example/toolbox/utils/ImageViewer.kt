package com.example.toolbox.utils

import android.content.ClipData
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.bitmapConfig
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private data class ImageState(
    var scale: Float = 1f,
    var offset: Offset = Offset.Zero
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MultiImageViewer(
    images: List<String>,
    initialPage: Int = 0,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (!isVisible || images.isEmpty()) return

    val context = LocalContext.current
    val windowInfo = LocalWindowInfo.current
    val screenWidth = windowInfo.containerSize.width.toFloat()
    val screenHeight = windowInfo.containerSize.height.toFloat()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    val pagerState = rememberPagerState(initialPage = initialPage) { images.size }

    val imageStates = remember(images.size) {
        MutableList(images.size) { mutableStateOf(ImageState()) }
    }

    val loadStates = remember(images.size) {
        MutableList(images.size) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    }

    var showMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = imageStates[pagerState.currentPage].value.scale <= 1.001f
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    AsyncImage(
                        model = images[page],
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize()
                            .clipToBounds()
                            .graphicsLayer(
                                scaleX = imageStates[page].value.scale,
                                scaleY = imageStates[page].value.scale,
                                translationX = imageStates[page].value.offset.x,
                                translationY = imageStates[page].value.offset.y
                            )
                            .transformable(
                                state = rememberTransformableState { zoomChange, panChange, _ ->
                                    val currentScale = imageStates[page].value.scale
                                    val currentOffset = imageStates[page].value.offset
                                    
                                    val newScale = (currentScale * zoomChange).coerceIn(1f, 5f)
                                    
                                    if (newScale > 1.001f || zoomChange != 1f) {
                                        val maxOffsetX = maxOf(0f, (screenWidth * newScale - screenWidth) / 2)
                                        val maxOffsetY = maxOf(0f, (screenHeight * newScale - screenHeight) / 2)
                                        
                                        var newX = currentOffset.x + panChange.x
                                        var newY = currentOffset.y + panChange.y
                                        
                                        newX = when {
                                            newX > maxOffsetX -> maxOffsetX
                                            newX < -maxOffsetX -> -maxOffsetX
                                            else -> newX
                                        }
                                        newY = when {
                                            newY > maxOffsetY -> maxOffsetY
                                            newY < -maxOffsetY -> -maxOffsetY
                                            else -> newY
                                        }
                                        
                                        imageStates[page].value = ImageState(newScale, Offset(newX, newY))
                                    }
                                },
                                canPan = { 
                                    imageStates[page].value.scale > 1.001f 
                                }
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        imageStates[page].value = ImageState()
                                    },
                                    onTap = {
                                        onDismiss()
                                    },
                                    onLongPress = {
                                        showMenu = true
                                    }
                                )
                            },
                        contentScale = ContentScale.Fit,
                        onState = { newState ->
                            loadStates[page].value = newState
                        }
                    )

                    when (loadStates[page].value) {
                        is AsyncImagePainter.State.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                        is AsyncImagePainter.State.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    "加载失败",
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                IconButton(
                                    onClick = {
                                        loadStates[page].value = AsyncImagePainter.State.Empty
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "重试",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("保存图片") },
                    onClick = {
                        showMenu = false
                        scope.launch {
                            saveCurrentImage(context, images[pagerState.currentPage])
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                    }
                )
                DropdownMenuItem(
                    text = { Text("复制链接") },
                    onClick = {
                        showMenu = false
                        clipboard.nativeClipboard.setPrimaryClip(
                            ClipData.newPlainText("Image URL", images[pagerState.currentPage])
                        )
                        Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    }
                )
            }
        }
    }
}

private suspend fun saveCurrentImage(context: android.content.Context, imageUrl: String) {
    try {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "开始保存...", Toast.LENGTH_SHORT).show()
        }
        
        val imageLoader = ImageLoader.Builder(context).build()
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .build()
        
        val result = imageLoader.execute(request)
        
        if (result is SuccessResult) {
            val bitmap = result.image.toBitmap()

            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val fileName = "IMG_${System.currentTimeMillis()}.jpg"
            val file = File(directory, fileName)

            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    out.flush()
                }
                
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("image/jpeg"),
                    null
                )
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "保存失败：无法下载图片", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
        e.printStackTrace()
    }
}