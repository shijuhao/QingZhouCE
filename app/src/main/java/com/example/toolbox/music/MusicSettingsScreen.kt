@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.toolbox.settings.SettingsCustomItem
import com.example.toolbox.settings.SettingsGroup
import com.example.toolbox.settings.SettingsItemCell

@Composable
fun MusicSettingsScreen(
    onScanClick: () -> Unit,
    viewModel: MusicPlayerViewModel
) {
    val state by viewModel.state.collectAsState()
    var showQualityDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SettingsGroup(
                title = "音乐库",
                items = listOf(
                    {
                        SettingsItemCell(
                            icon = Icons.Default.Refresh,
                            title = "手动扫描音乐",
                            subtitle = "重新扫描设备上的音乐文件",
                            onClick = onScanClick
                        )
                    },
                    {
                        SettingsItemCell(
                            icon = Icons.Default.DeleteSweep,
                            title = "清除扫描缓存",
                            subtitle = "清除已缓存的文件夹扫描结果",
                            onClick = { viewModel.clearCache() }
                        )
                    },
                    {
                        SettingsItemCell(
                            icon = Icons.Default.FolderOpen,
                            title = "选择扫描文件夹",
                            subtitle = if (state.customFolders.isEmpty()) "未设置" else "已设置 ${state.customFolders.size} 个文件夹",
                            onClick = { showFolderPicker = true }
                        )
                    }
                )
            )
        }

        item {
            SettingsGroup(
                title = "扫描方式",
                items = listOf(
                    {
                        SettingsCustomItem(onClick = { viewModel.setScanMode(ScanMode.AUTO) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "自动扫描",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "优先使用MediaStore，失败后使用文件扫描",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = state.scanMode == ScanMode.AUTO,
                                    onClick = { viewModel.setScanMode(ScanMode.AUTO) }
                                )
                            }
                        }
                    },
                    {
                        SettingsCustomItem(onClick = { viewModel.setScanMode(ScanMode.MEDIASTORE) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "MediaStore扫描",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "使用系统媒体库，速度快",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = state.scanMode == ScanMode.MEDIASTORE,
                                    onClick = { viewModel.setScanMode(ScanMode.MEDIASTORE) }
                                )
                            }
                        }
                    },
                    {
                        SettingsCustomItem(onClick = { viewModel.setScanMode(ScanMode.FILE) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "文件扫描",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "直接扫描文件系统，更全面但较慢",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = state.scanMode == ScanMode.FILE,
                                    onClick = { viewModel.setScanMode(ScanMode.FILE) }
                                )
                            }
                        }
                    }
                )
            )
        }

        item {
            SettingsGroup(
                title = "音质设置",
                items = listOf(
                    {
                        SettingsItemCell(
                            icon = Icons.Default.MusicNote,
                            title = "音质选择",
                            subtitle = "标准音质",
                            onClick = { showQualityDialog = true }
                        )
                    }
                )
            )
        }

        item {
            SettingsGroup(
                title = "均衡器",
                items = listOf(
                    {
                        SettingsItemCell(
                            icon = Icons.Default.Equalizer,
                            title = "均衡器设置",
                            subtitle = "未启用",
                            onClick = { showEqualizerDialog = true }
                        )
                    }
                )
            )
        }

        item {
            SettingsGroup(
                title = "歌词设置",
                items = listOf(
                    {
                        SettingsCustomItem(onClick = { viewModel.setShowLyricsInMiniPlayer(!state.showLyricsInMiniPlayer) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Subtitles,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "迷你播放器显示歌词",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "在迷你播放器中显示当前播放的歌词",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = state.showLyricsInMiniPlayer,
                                    onCheckedChange = { viewModel.setShowLyricsInMiniPlayer(it) }
                                )
                            }
                        }
                    }
                )
            )
        }
    }

    if (showQualityDialog) {
        val qualities = listOf("标准音质", "高品质", "无损音质")
        var selectedQuality by remember { mutableIntStateOf(0) }
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("选择音质") },
            text = {
                Column {
                    qualities.forEachIndexed { index, quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedQuality = index },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedQuality == index,
                                onClick = { selectedQuality = index }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(quality)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showEqualizerDialog) {
        AlertDialog(
            onDismissRequest = { showEqualizerDialog = false },
            title = { Text("均衡器") },
            text = { Text("均衡器功能暂未实现") },
            confirmButton = {
                TextButton(onClick = { showEqualizerDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            onDismiss = { showFolderPicker = false },
            onFolderSelected = { folderPath ->
                viewModel.addCustomFolder(folderPath)
            },
            customFolders = state.customFolders,
            onRemoveFolder = { folderPath ->
                viewModel.removeCustomFolder(folderPath)
            }
        )
    }
}