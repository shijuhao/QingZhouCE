package com.example.toolbox.function.daily

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.delay

class RandomChooseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RandomChooseScreen(
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomChooseScreen(
    modifier: Modifier = Modifier,
    viewModel: RandomChooseViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val state = viewModel // 直接使用，或通过 derivedStateOf 优化

    // 无限旋转动画用于抽选中
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // 结果缩放动画
    val scaleAnim by animateFloatAsState(
        targetValue = if (state.showResult) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // 抽选完成后滚动到顶部（使用全局列表状态）
    val listState = rememberLazyListState()
    LaunchedEffect(state.showResult) {
        if (state.showResult && !state.isSelecting) {
            delay(50)
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("随机抽选") },
            navigationIcon = {
                FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }
        )

        // 使用单一 LazyColumn 替代嵌套滚动，避免滚动冲突
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 抽选结果区域（条件显示）
            if (state.selectedResult.isNotEmpty()) {
                item {
                    ResultCard(
                        selectedResult = state.selectedResult,
                        showResult = state.showResult,
                        scaleAnim = scaleAnim,
                        items = state.items,
                        isSelecting = state.isSelecting,
                        rotationAngle = rotationAngle,
                        onCopy = {
                            copyResultToClipboard(context, state.selectedResult)
                        },
                        onReselect = {
                            state.performSelection()
                        }
                    )
                }
            }

            // 添加项目区域
            item {
                AddItemCard(
                    newItemName = state.newItemName,
                    onNameChange = { state.updateNewItemName(it) },
                    newItemWeight = state.newItemWeight,
                    onWeightChange = { state.updateNewItemWeight(it) },
                    onAdd = { state.addItem() },
                    isSelecting = state.isSelecting
                )
            }

            // 错误提示
            if (state.errorMessage.isNotEmpty()) {
                item {
                    ErrorMessageCard(message = state.errorMessage)
                }
            }

            // 项目列表
            if (state.items.isNotEmpty()) {
                item {
                    ItemsListCard(
                        items = state.items,
                        totalWeight = state.totalWeight,
                        highlightedIndex = state.highlightedIndex,
                        isSelecting = state.isSelecting,
                        onRemove = { state.removeItem(it) },
                        onClearAll = { state.clearAllItems() },
                        onSelect = {
                            state.performSelection()
                        },
                        rotationAngle = rotationAngle
                    )
                }
            }

            // 底部留白
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun ResultCard(
    selectedResult: String,
    showResult: Boolean,
    scaleAnim: Float,
    items: List<RandomItem>,
    isSelecting: Boolean,
    rotationAngle: Float,
    onCopy: () -> Unit,
    onReselect: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎉 抽选结果 🎉",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            AnimatedContent(
                targetState = selectedResult,
                transitionSpec = {
                    (fadeIn() + slideInVertically { -20 }).togetherWith(fadeOut() + slideOutVertically { 20 })
                }
            ) { result ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = result,
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.scale(scaleAnim)
                    )
                }
            }

            if (showResult) {
                val selectedItem = items.find { it.isSelected }
                selectedItem?.let { item ->
                    val index = items.indexOf(item)
                    val probability = if (items.isNotEmpty() && index >= 0) {
                        (item.weight.toDouble() / items.sumOf { it.weight }) * 100
                    } else 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "权重", value = item.weight.toString())
                        StatItem(label = "概率", value = "${"%.1f".format(probability)}%", color = MaterialTheme.colorScheme.primary)
                        StatItem(label = "总项目", value = items.size.toString())
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    enabled = selectedResult.isNotEmpty()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("复制结果")
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                OutlinedButton(
                    onClick = onReselect,
                    modifier = Modifier.weight(1f),
                    enabled = items.isNotEmpty() && !isSelecting
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelecting) {
                            Icon(
                                Icons.Filled.Refresh,
                                null,
                                modifier = Modifier
                                    .size(ButtonDefaults.IconSize)
                                    .rotate(rotationAngle)
                            )
                        } else {
                            Icon(Icons.Filled.Casino, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        }
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(if (isSelecting) "抽选中..." else "重新抽选")
                    }
                }
            }
        }
    }
}

@Composable
fun AddItemCard(
    newItemName: String,
    onNameChange: (String) -> Unit,
    newItemWeight: String,
    onWeightChange: (String) -> Unit,
    onAdd: () -> Unit,
    isSelecting: Boolean
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("添加抽选项目", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = newItemName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("输入项目名称") },
                singleLine = true,
                label = { Text("项目名称") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = newItemWeight,
                    onValueChange = onWeightChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("权重") },
                    singleLine = true,
                    label = { Text("权重") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Button(
                    onClick = onAdd,
                    modifier = Modifier.height(50.dp),
                    enabled = !isSelecting
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("添加")
                    }
                }
            }

            Text(
                text = "📝 权重值越大，被抽中的概率越高",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorMessageCard(message: String) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
fun ItemsListCard(
    items: List<RandomItem>,
    totalWeight: Int,
    highlightedIndex: Int,
    isSelecting: Boolean,
    onRemove: (Int) -> Unit,
    onClearAll: () -> Unit,
    onSelect: () -> Unit,
    rotationAngle: Float
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "抽选项目列表 (${items.size}个)",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("总权重: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = totalWeight.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val index = items.indexOf(item)
                    val probability = if (items.isNotEmpty() && index >= 0) {
                        (item.weight.toDouble() / totalWeight) * 100
                    } else 0.0

                    val isHighlighted = index == highlightedIndex

                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Card(
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (item.isSelected) 8.dp else 2.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isHighlighted || item.isSelected)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = if (item.isSelected) CardDefaults.outlinedCardBorder() else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .padding(end = 8.dp)
                                            )
                                        }
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (item.isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (item.isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "权重: ${item.weight}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "${"%.1f".format(probability)}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "概率",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { onRemove(item.id) },
                                    enabled = !isSelecting
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onClearAll,
                    modifier = Modifier.weight(1f),
                    enabled = !isSelecting
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("清空所有")
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onSelect,
                    modifier = Modifier.weight(1f),
                    enabled = items.isNotEmpty() && !isSelecting
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelecting) {
                            Icon(
                                Icons.Filled.Casino,
                                null,
                                modifier = Modifier
                                    .size(ButtonDefaults.IconSize)
                                    .rotate(rotationAngle)
                            )
                        } else {
                            Icon(Icons.Filled.Casino, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        }
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(if (isSelecting) "抽选中..." else "开始抽选")
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun copyResultToClipboard(context: Context, result: String) {
    if (result.isEmpty()) {
        Toast.makeText(context, "请先进行抽选", Toast.LENGTH_SHORT).show()
        return
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("抽选结果", result)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "已复制: $result", Toast.LENGTH_SHORT).show()
}