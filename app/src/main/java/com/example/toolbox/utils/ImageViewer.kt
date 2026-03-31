package com.example.toolbox.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class ImageState(
    var scale: Float = 1f,
    var offset: Offset = Offset.Zero
)

/**
 * 可复用的多图全屏查看器
 *
 * @param images 要显示的图片 Painter 列表
 * @param initialPage 初始显示的页码
 * @param isVisible 是否显示
 * @param onDismiss 关闭回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiImageViewer(
    modifier: Modifier = Modifier,
    images: List<Painter>,
    initialPage: Int = 0,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (!isVisible || images.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = initialPage) { images.size }

    val imageStates = remember(images.size) {
        List(images.size) { mutableStateOf(ImageState()) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDismiss() }
                    )
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val state = imageStates[page].value
                var scale by remember { mutableFloatStateOf(state.scale) }
                var offset by remember { mutableStateOf(state.offset) }

                fun updateState() {
                    imageStates[page].value = ImageState(scale, offset)
                }

                val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 3f)
                    offset += panChange
                    updateState()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    scale = if (scale < 2f) 2f else 1f
                                    if (scale == 1f) offset = Offset.Zero
                                    updateState()
                                },
                                onTap = {
                                    onDismiss()
                                }
                            )
                        }
                ) {
                    androidx.compose.foundation.Image(
                        painter = images[page],
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize()
                            .clipToBounds()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .transformable(transformableState)
                    )
                }
            }
        }
    }
}