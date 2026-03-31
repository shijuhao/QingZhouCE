package com.example.toolbox.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class ImageState(
    var scale: Float = 1f,
    var offset: Offset = Offset.Zero
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiImageViewer(
    images: List<Painter>,
    initialPage: Int = 0,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible || images.isEmpty()) return

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp.value
    val screenHeight = configuration.screenHeightDp.dp.value

    val pagerState = rememberPagerState(initialPage = initialPage) { images.size }

    val imageStates = remember(images.size) {
        List(images.size) { mutableStateOf(ImageState()) }
    }

    val currentScale = imageStates[pagerState.currentPage].value.scale

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
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = currentScale <= 1f
            ) { page ->
                val state = imageStates[page].value
                var scale by remember { mutableFloatStateOf(state.scale) }
                var offset by remember { mutableStateOf(state.offset) }

                fun clampOffset(scale: Float, offset: Offset): Offset {
                    val maxOffsetX = maxOf(0f, (screenWidth * scale - screenWidth) / 2)
                    val maxOffsetY = maxOf(0f, (screenHeight * scale - screenHeight) / 2)

                    val clampedX = when {
                        offset.x > maxOffsetX -> maxOffsetX
                        offset.x < -maxOffsetX -> -maxOffsetX
                        else -> offset.x
                    }
                    val clampedY = when {
                        offset.y > maxOffsetY -> maxOffsetY
                        offset.y < -maxOffsetY -> -maxOffsetY
                        else -> offset.y
                    }
                    return Offset(clampedX, clampedY)
                }

                fun updateState() {
                    val clamped = clampOffset(scale, offset)
                    if (clamped != offset) offset = clamped
                    imageStates[page].value = ImageState(scale, clamped)
                }

                val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                    val newScale = (scale * zoomChange).coerceIn(1f, 3f)
                    scale = newScale
                    offset = clampOffset(scale, offset + panChange)
                    updateState()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    scale = 1f
                                    offset = Offset.Zero
                                    updateState()
                                },
                                onTap = {
                                    onDismiss()
                                }
                            )
                        }
                ) {
                    Image(
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