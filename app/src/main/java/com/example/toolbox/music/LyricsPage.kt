package com.example.toolbox.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LyricsPage(
    lyrics: List<LyricLine>,
    currentPosition: Int
) {
    val currentLyricIndex = LyricParser.getCurrentLyricIndex(lyrics, currentPosition)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    var autoScrollEnabled by remember { mutableStateOf(true) }
    var pendingScrollIndex by remember { mutableStateOf(-1) }
    var scrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            scrollJob?.cancel()
            autoScrollEnabled = false
        } else {
            scrollJob = scope.launch {
                delay(3000)
                autoScrollEnabled = true
                if (pendingScrollIndex >= 0) {
                    listState.animateScrollToItem(pendingScrollIndex, -100)
                    pendingScrollIndex = -1
                } else if (currentLyricIndex >= 0) {
                    listState.animateScrollToItem(currentLyricIndex, -100)
                }
            }
        }
    }
    
    LaunchedEffect(currentLyricIndex) {
        if (currentLyricIndex >= 0 && lyrics.isNotEmpty()) {
            if (autoScrollEnabled) {
                listState.animateScrollToItem(currentLyricIndex, -100)
                pendingScrollIndex = -1
            } else {
                pendingScrollIndex = currentLyricIndex
            }
        }
    }
    
    if (lyrics.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Subtitles,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无歌词",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 100.dp, horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lyrics.size) { index ->
                    val isCurrentLine = index == currentLyricIndex
                    Text(
                        text = lyrics[index].text,
                        style = if (isCurrentLine) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = if (isCurrentLine) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                            )
                        )
                    )
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
        }
    }
}