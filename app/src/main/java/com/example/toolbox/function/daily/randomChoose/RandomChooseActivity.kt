package com.example.toolbox.function.daily.randomChoose

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.widget.Toast
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.List
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.withSave
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.plus
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

@Serializable
data class RandomItem(
    val id: Int,
    val name: String,
    val weight: Int,
    var isSelected: Boolean = false
)

@Serializable
data class SavedConfig(
    val id: Long,
    val name: String,
    val items: List<RandomItem>,
    val timestamp: Long
)

class RandomChooseViewModel : ViewModel() {
    var items by mutableStateOf<List<RandomItem>>(emptyList())
        private set

    var newItemName by mutableStateOf("")
        private set

    var newItemWeight by mutableStateOf("1")
        private set

    var selectedResult by mutableStateOf("")
        private set

    var errorMessage by mutableStateOf("")
        private set

    var isSpinning by mutableStateOf(false)
        private set

    var showResult by mutableStateOf(false)
        private set

    var rotationAngle by mutableFloatStateOf(0f)
        private set

    var targetIndex by mutableIntStateOf(-1)
        private set

    private var nextId = 1

    val totalWeight: Int
        get() = items.sumOf { it.weight }
        
    private val json = Json { ignoreUnknownKeys = true }
    private val prefsKey = "random_wheel_prefs"
    
    var savedConfigs by mutableStateOf<List<SavedConfig>>(emptyList())
        private set
    
    var showSaveManagerDialog by mutableStateOf(false)
        private set

    fun updateNewItemName(name: String) {
        newItemName = name
    }

    fun updateNewItemWeight(weight: String) {
        newItemWeight = weight
    }

    fun addItem() {
        errorMessage = ""
        try {
            val name = newItemName.trim()
            val weightText = newItemWeight.trim()

            if (name.isEmpty()) {
                errorMessage = "请输入项目名称"
                return
            }
            val weight = weightText.toIntOrNull()
            if (weight == null || weight <= 0) {
                errorMessage = "权重值必须是大于0的整数"
                return
            }

            items = items + RandomItem(nextId++, name, weight)
            newItemName = ""
            newItemWeight = "1"
            selectedResult = ""
            showResult = false
            targetIndex = -1
        } catch (e: Exception) {
            errorMessage = "添加失败: ${e.message}"
        }
    }

    fun removeItem(id: Int) {
        items = items.filter { it.id != id }
        selectedResult = ""
        showResult = false
        targetIndex = -1
    }

    fun clearAllItems() {
        items = emptyList()
        selectedResult = ""
        showResult = false
        isSpinning = false
        rotationAngle = 0f
        targetIndex = -1
    }

    fun spin() {
        if (items.isEmpty() || isSpinning) return

        viewModelScope.launch {
            isSpinning = true
            showResult = false
            targetIndex = -1

            items = items.map { it.copy(isSelected = false) }

            val targetItem = selectItemByWeight()
            val index = items.indexOf(targetItem)
            val sliceAngle = 360f / items.size

            val safeMargin = sliceAngle * 0.2f
            val randomOffset = Random.nextFloat() * (sliceAngle - 2 * safeMargin) + safeMargin

            val targetAbsoluteAngle = index * sliceAngle + randomOffset

            val currentRotation = rotationAngle % 360f
            val normalizedCurrent = if (currentRotation < 0) currentRotation + 360f else currentRotation

            val targetWorldAngle = (normalizedCurrent + targetAbsoluteAngle) % 360f

            var deltaRotation = (270f - targetWorldAngle) % 360f
            if (deltaRotation < 0) deltaRotation += 360f

            val extraSpins = Random.nextInt(5, 8) * 360f
            val totalRotation = deltaRotation + extraSpins

            val startAngle = rotationAngle
            val endAngle = rotationAngle + totalRotation

            animateRotation(start = startAngle, end = endAngle, durationMs = 3500)

            items = items.mapIndexed { i, item ->
                if (i == index) item.copy(isSelected = true) else item.copy(isSelected = false)
            }
            targetIndex = index
            selectedResult = targetItem.name
            showResult = true
            isSpinning = false
        }
    }

    private fun selectItemByWeight(): RandomItem {
        val total = items.sumOf { it.weight }
        if (total == 0) return items.first()
        val random = Random.nextDouble(0.0, total.toDouble())
        var cumulative = 0.0
        for (item in items) {
            cumulative += item.weight
            if (random < cumulative) return item
        }
        return items.last()
    }

    private suspend fun animateRotation(start: Float, end: Float, durationMs: Long) {
        val startTime = System.currentTimeMillis()

        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)

            val eased = 1 - (1 - progress).pow(3)

            rotationAngle = start + (end - start) * eased

            if (progress >= 1f) {
                rotationAngle = end
                break
            }

            delay(16)
        }
    }
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(prefsKey, Context.MODE_PRIVATE)
    }
    
    private fun loadSavedConfigs(context: Context) {
        val jsonStr = getPrefs(context).getString("saved_configs", null)
        savedConfigs = try {
            jsonStr?.let { json.decodeFromString(it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun openSaveManagerDialog(context: Context) {
        loadSavedConfigs(context)
        showSaveManagerDialog = true
    }
    
    fun closeSaveManagerDialog() {
        showSaveManagerDialog = false
    }
    
    fun saveCurrentList(context: Context, name: String = "") {
        if (items.isEmpty()) {
            errorMessage = "没有可保存的项目"
            return
        }
        val configs = savedConfigs.toMutableList()
        val timestamp = System.currentTimeMillis()
        val newConfig = SavedConfig(
            id = timestamp,
            name = name.ifEmpty { "配置 ${configs.size + 1}" },
            items = items.map { it.copy() },
            timestamp = timestamp
        )
        configs.add(newConfig)
        getPrefs(context).edit().putString("saved_configs", json.encodeToString(configs)).apply()
        savedConfigs = configs
        errorMessage = ""
        Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
    }
    
    fun loadConfig(context: Context, config: SavedConfig) {
        items = config.items.map { it.copy(id = ++nextId) }
        selectedResult = ""
        showResult = false
        isSpinning = false
        rotationAngle = 0f
        targetIndex = -1
        errorMessage = ""
        Toast.makeText(context, "已加载: ${config.name}", Toast.LENGTH_SHORT).show()
    }
    
    fun deleteConfig(context: Context, id: Long) {
        val configs = savedConfigs.filter { it.id != id }
        getPrefs(context).edit().putString("saved_configs", json.encodeToString(configs)).apply()
        savedConfigs = configs
        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
    }
    
    fun overwriteConfig(context: Context, id: Long) {
        val index = savedConfigs.indexOfFirst { it.id == id }
        if (index == -1) return
        val configs = savedConfigs.toMutableList()
        configs[index] = configs[index].copy(
            items = items.map { it.copy() },
            timestamp = System.currentTimeMillis()
        )
        getPrefs(context).edit().putString("saved_configs", json.encodeToString(configs)).apply()
        savedConfigs = configs
        Toast.makeText(context, "已覆盖", Toast.LENGTH_SHORT).show()
    }
}

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
    modifier: Modifier = Modifier
) {
    val viewModel: RandomChooseViewModel = viewModel()
    val context = LocalContext.current
    
    val showSaveDialog = remember { mutableStateOf(false) }
    var saveConfigName by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    LaunchedEffect(viewModel.showResult) {
        if (viewModel.showResult && !viewModel.isSpinning) {
            delay(100)
            listState.animateScrollToItem(0)
        }
    }
    
    if (showSaveDialog.value) {
        AlertDialog(
            onDismissRequest = { showSaveDialog.value = false },
            title = { Text("保存配置") },
            text = {
                OutlinedTextField(
                    value = saveConfigName,
                    onValueChange = { saveConfigName = it },
                    label = { Text("配置名称（可选）") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveCurrentList(context, saveConfigName)
                    saveConfigName = ""
                    showSaveDialog.value = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog.value = false }) { Text("取消") }
            }
        )
    }
    
    if (viewModel.showSaveManagerDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeSaveManagerDialog() },
            title = { Text("已保存的配置") },
            text = {
                if (viewModel.savedConfigs.isEmpty()) {
                    Text("还没有保存的配置")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.savedConfigs.sortedByDescending { it.timestamp }) { config ->
                            Card {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(config.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${config.items.size}项  ${
                                                SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                                                    .format(Date(config.timestamp))
                                            }",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            viewModel.loadConfig(context, config)
                                            viewModel.closeSaveManagerDialog()
                                        }) {
                                            Icon(Icons.Default.PlayArrow, "加载")
                                        }
                                        IconButton(
                                            onClick = { viewModel.overwriteConfig(context, config.id) },
                                            enabled = viewModel.items.isNotEmpty()
                                        ) {
                                            Icon(Icons.Default.Save, "覆盖")
                                        }
                                        IconButton(onClick = { viewModel.deleteConfig(context, config.id) }) {
                                            Icon(Icons.Default.Delete, "删除")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.closeSaveManagerDialog() }) { Text("关闭") }
            }
        )
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
            },
            actions = {
                IconButton(
                    onClick = { showSaveDialog.value = true },
                    enabled = viewModel.items.isNotEmpty() && !viewModel.isSpinning
                ) {
                    Icon(Icons.Default.Save, "保存")
                }
                IconButton(
                    onClick = { viewModel.openSaveManagerDialog(context) },
                    enabled = !viewModel.isSpinning
                ) {
                    Icon(Icons.Default.List, "管理")
                }
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (viewModel.selectedResult.isNotEmpty()) {
                item {
                    ResultCard(
                        selectedResult = viewModel.selectedResult,
                        showResult = viewModel.showResult,
                        items = viewModel.items,
                        isSpinning = viewModel.isSpinning,
                        onCopy = { copyResultToClipboard(context, viewModel.selectedResult) },
                        onReselect = { viewModel.spin() }
                    )
                }
            }

            if (viewModel.items.isNotEmpty()) {
                item {
                    WheelCard(
                        items = viewModel.items,
                        rotationAngle = viewModel.rotationAngle,
                        isSpinning = viewModel.isSpinning,
                        targetIndex = viewModel.targetIndex,
                        onSpin = { viewModel.spin() }
                    )
                }
            }

            item {
                AddItemCard(
                    newItemName = viewModel.newItemName,
                    onNameChange = viewModel::updateNewItemName,
                    newItemWeight = viewModel.newItemWeight,
                    onWeightChange = viewModel::updateNewItemWeight,
                    onAdd = viewModel::addItem,
                    isSpinning = viewModel.isSpinning
                )
            }

            if (viewModel.errorMessage.isNotEmpty()) {
                item {
                    ErrorMessageCard(message = viewModel.errorMessage)
                }
            }

            if (viewModel.items.isNotEmpty()) {
                item {
                    ItemsListCard(
                        items = viewModel.items,
                        totalWeight = viewModel.totalWeight,
                        onRemove = viewModel::removeItem,
                        onClearAll = viewModel::clearAllItems,
                        isSpinning = viewModel.isSpinning
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun WheelCard(
    items: List<RandomItem>,
    rotationAngle: Float,
    isSpinning: Boolean,
    targetIndex: Int,
    onSpin: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (!isSpinning) onSpin()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Wheel(
                    items = items,
                    rotationAngle = rotationAngle,
                    targetIndex = targetIndex
                )

                // 顶部指针
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val wheelRadius = size.minDimension / 2 * 0.85f

                    val tipY = centerY - wheelRadius + 4.dp.toPx()
                    val baseY = centerY - wheelRadius - 18.dp.toPx()
                    val halfWidth = 12.dp.toPx()

                    drawCircle(
                        color = Color(0x20000000),
                        radius = wheelRadius + 8.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )

                    val path = Path().apply {
                        moveTo(centerX, tipY)
                        lineTo(centerX - halfWidth, baseY)
                        lineTo(centerX + halfWidth, baseY)
                        close()
                    }
                    drawPath(path = path, color = Color(0xFFE53935))
                    drawPath(
                        path = path,
                        color = Color(0xFFB71C1C),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 中心按钮
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .then(
                            if (!isSpinning) Modifier.pointerInput(Unit) {
                                detectTapGestures { onSpin() }
                            } else Modifier.Companion
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isSpinning) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSpin,
                enabled = items.isNotEmpty() && !isSpinning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (isSpinning) Icons.Default.Refresh else Icons.Default.Casino,
                    null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isSpinning) "转盘中..." else "开始转盘")
            }
        }
    }
}

@Composable
fun Wheel(
    items: List<RandomItem>,
    rotationAngle: Float,
    targetIndex: Int
) {
    val colors = listOf(
        Color(0xFF1E88E5),
        Color(0xFF43A047),
        Color(0xFFFB8C00),
        Color(0xFF8E24AA),
        Color(0xFFE53935),
        Color(0xFF00ACC1),
        Color(0xFF7CB342),
        Color(0xFFF4511E)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (items.isEmpty()) return@Canvas

        val sweepAngle = 360f / items.size
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * 0.85f

        val displayAngle = rotationAngle % 360f

        rotate(displayAngle) {
            items.forEachIndexed { index, item ->
                val startAngle = index * sweepAngle
                val color = colors[index % colors.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )

                drawArc(
                    color = Color.White.copy(alpha = 0.5f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 1.5f)
                )

                if (index == targetIndex) {
                    drawArc(
                        color = Color.White,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 4f)
                    )
                }

                val textAngle = startAngle + sweepAngle / 2
                val textRadius = radius * 0.65f
                val textX =
                    center.x + textRadius * cos(Math.toRadians(textAngle.toDouble())).toFloat()
                val textY =
                    center.y + textRadius * sin(Math.toRadians(textAngle.toDouble())).toFloat()

                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        textSize = if (items.size > 6) 12.sp.toPx() else 14.sp.toPx()
                        setColor(android.graphics.Color.WHITE)
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = true
                        isAntiAlias = true
                    }

                    val maxWidth = radius * 0.45f
                    val displayText = if (paint.measureText(item.name) > maxWidth) {
                        var truncated = item.name
                        while (paint.measureText("$truncated…") > maxWidth && truncated.isNotEmpty()) {
                            truncated = truncated.dropLast(1)
                        }
                        if (truncated.isEmpty()) item.name.take(1) else "$truncated…"
                    } else {
                        item.name
                    }

                    withSave {
                        val fm = paint.fontMetrics
                        val baselineOffset = (fm.ascent + fm.descent) / 2f
                        val rotationDegrees = if (textAngle > 90f && textAngle < 270f) {
                            textAngle + 180f
                        } else {
                            textAngle
                        }

                        rotate(rotationDegrees, textX, textY)
                        drawText(displayText, textX, textY - baselineOffset, paint)
                    }
                }
            }
        }

        drawArc(
            color = Color(0xFF37474F),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius - 3.dp.toPx(), center.y - radius - 3.dp.toPx()),
            size = Size((radius + 3.dp.toPx()) * 2, (radius + 3.dp.toPx()) * 2),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
fun ResultCard(
    selectedResult: String,
    showResult: Boolean,
    items: List<RandomItem>,
    isSpinning: Boolean,
    onCopy: () -> Unit,
    onReselect: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
                fontWeight = FontWeight.Bold
            )

            Text(
                text = selectedResult,
                style = MaterialTheme.typography.displaySmall,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (showResult) {
                val selectedItem = items.find { it.isSelected }
                selectedItem?.let { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "权重", value = item.weight.toString())
                        StatItem(
                            label = "概率",
                            value = "${"%.1f".format(item.weight.toDouble() / items.sumOf { it.weight } * 100)}%",
                            color = MaterialTheme.colorScheme.primary
                        )
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
                    Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("复制")
                }

                Spacer(modifier = Modifier.width(16.dp))

                OutlinedButton(
                    onClick = onReselect,
                    modifier = Modifier.weight(1f),
                    enabled = items.isNotEmpty() && !isSpinning
                ) {
                    Icon(Icons.Filled.Casino, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("再转一次")
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
    isSpinning: Boolean
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("添加项目", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = newItemName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("项目名称") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = newItemWeight,
                    onValueChange = onWeightChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("权重") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Button(
                    onClick = onAdd,
                    modifier = Modifier.height(56.dp),
                    enabled = !isSpinning
                ) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("添加")
                }
            }
        }
    }
}

@Composable
fun ErrorMessageCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
fun ItemsListCard(
    items: List<RandomItem>,
    totalWeight: Int,
    onRemove: (Int) -> Unit,
    onClearAll: () -> Unit,
    isSpinning: Boolean
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("项目列表 (${items.size}个)", style = MaterialTheme.typography.titleMedium)
                Text(
                    "总权重: $totalWeight",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    val probability = (item.weight.toDouble() / totalWeight * 100)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isSelected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (item.isSelected) CardDefaults.outlinedCardBorder() else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = if (item.isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "权重: ${item.weight}  |  ${"%.1f".format(probability)}%",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            IconButton(
                                onClick = { onRemove(item.id) },
                                enabled = !isSpinning
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onClearAll,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSpinning
            ) {
                Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("清空所有")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun copyResultToClipboard(context: Context, result: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("抽选结果", result)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "已复制: $result", Toast.LENGTH_SHORT).show()
}