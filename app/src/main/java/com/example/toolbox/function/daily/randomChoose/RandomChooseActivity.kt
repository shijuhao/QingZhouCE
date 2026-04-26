package com.example.toolbox.function.daily.randomChoose

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.widget.Toast
import androidx.compose.material.icons.filled.Save
import kotlinx.serialization.Serializable
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.withSave
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.delay
import kotlin.collections.plus
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.core.content.edit
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

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

    var editingItem by mutableStateOf<RandomItem?>(null)
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

    var configName by mutableStateOf("")
        private set

    fun updateConfigName(name: String) {
        configName = name
    }

    fun updateNewItemName(name: String) {
        newItemName = name
    }

    fun updateNewItemWeight(weight: String) {
        newItemWeight = weight
    }

    fun startEditItem(item: RandomItem) {
        editingItem = item
        newItemName = item.name
        newItemWeight = item.weight.toString()
        errorMessage = ""
    }

    fun cancelEdit() {
        editingItem = null
        newItemName = ""
        newItemWeight = "1"
        errorMessage = ""
    }

    fun saveEditItem() {
        val editItem = editingItem ?: return
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

            items = items.map { 
                if (it.id == editItem.id) it.copy(name = name, weight = weight) else it 
            }
            editingItem = null
            newItemName = ""
            newItemWeight = "1"
        } catch (e: Exception) {
            errorMessage = "保存失败: ${e.message}"
        }
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

            val totalWeight = items.sumOf { it.weight }.toFloat()
            val startAngle = items.take(index).sumOf { it.weight }.toFloat() / totalWeight * 360f
            val sweepAngle = targetItem.weight.toFloat() / totalWeight * 360f

            val safeMargin = sweepAngle * 0.2f
            val randomOffset = if (sweepAngle > 2 * safeMargin) {
                Random.nextFloat() * (sweepAngle - 2 * safeMargin) + safeMargin
            } else {
                sweepAngle / 2f
            }

            val targetSectorAngle = startAngle + randomOffset
            val pointerAngle = 270f
            val neededRotation = ((pointerAngle - targetSectorAngle) % 360f + 360f) % 360f
            val currentMod = ((rotationAngle % 360f) + 360f) % 360f

            var delta = neededRotation - currentMod
            if (delta < 0) delta += 360f

            val extraSpins = Random.nextInt(8, 13) * 360f
            val totalRotation = delta + extraSpins

            // 动画旋转
            animateRotation(rotationAngle, rotationAngle + totalRotation, 2500)

            items = items.mapIndexed { i, item ->
                if (i == index) item.copy(isSelected = true) else item.copy(isSelected = false)
            }

            targetIndex = index
            selectedResult = targetItem.name
            showResult = true
            isSpinning = false
        }
    }

    private suspend fun animateRotation(start: Float, end: Float, durationMs: Long) {
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
            val eased = 1 - (1 - progress) * (1 - progress) * (1 - progress) // ease out cubic
            rotationAngle = start + (end - start) * eased
            if (progress >= 1f) break
            delay(16)
        }
        rotationAngle = end
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

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(prefsKey, Context.MODE_PRIVATE)
    }

    private fun loadSavedConfigs(context: Context) {
        val jsonStr = getPrefs(context).getString("saved_configs", null) ?: ""
        savedConfigs = try {
            jsonStr.let { json.decodeFromString(it) } ?: emptyList()
        } catch (_: Exception) {
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

    fun saveCurrentList(context: Context) {
        if (items.isEmpty()) {
            errorMessage = "没有可保存的项目"
            return
        }
        val configs = savedConfigs.toMutableList()
        val timestamp = System.currentTimeMillis()
        val finalName = configName.ifEmpty { "配置 ${configs.size + 1}" }
        
        // 检查是否有同名配置，有则覆盖
        val existingIndex = configs.indexOfFirst { it.name == finalName }
        if (existingIndex != -1) {
            // 覆盖已有配置
            configs[existingIndex] = configs[existingIndex].copy(
                items = items.map { it.copy() },
                timestamp = timestamp
            )
            Toast.makeText(context, "已更新: $finalName", Toast.LENGTH_SHORT).show()
        } else {
            // 新建配置
            val newConfig = SavedConfig(
                id = timestamp,
                name = finalName,
                items = items.map { it.copy() },
                timestamp = timestamp
            )
            configs.add(newConfig)
            Toast.makeText(context, "已保存: $finalName", Toast.LENGTH_SHORT).show()
        }
        
        getPrefs(context).edit { putString("saved_configs", json.encodeToString(configs)) }
        savedConfigs = configs
        errorMessage = ""
    }

    fun loadConfig(context: Context, config: SavedConfig) {
        items = config.items.map { it.copy(id = ++nextId) }
        configName = config.name
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
        getPrefs(context).edit { putString("saved_configs", json.encodeToString(configs)) }
        savedConfigs = configs
        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
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

    val showAddItemDialog = remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(viewModel.showResult) {
        if (viewModel.showResult && !viewModel.isSpinning) {
            delay(100)
            listState.animateScrollToItem(0)
        }
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

    if (showAddItemDialog.value) {
        AlertDialog(
            onDismissRequest = { 
                if (viewModel.editingItem != null) viewModel.cancelEdit()
                showAddItemDialog.value = false 
            },
            title = { Text(if (viewModel.editingItem != null) "编辑项目" else "添加项目") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.newItemName,
                        onValueChange = viewModel::updateNewItemName,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("项目名称") },
                        placeholder = { Text("输入项目名称") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.newItemWeight,
                        onValueChange = viewModel::updateNewItemWeight,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("权重") },
                        placeholder = { Text("输入权重值") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    if (viewModel.errorMessage.isNotEmpty()) {
                        Text(
                            text = viewModel.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (viewModel.editingItem != null) {
                        viewModel.saveEditItem()
                    } else {
                        viewModel.addItem()
                    }
                    if (viewModel.errorMessage.isEmpty()) {
                        showAddItemDialog.value = false
                    }
                }) { Text(if (viewModel.editingItem != null) "保存" else "添加") }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (viewModel.editingItem != null) viewModel.cancelEdit()
                    showAddItemDialog.value = false
                    viewModel.updateNewItemName("")
                    viewModel.updateNewItemWeight("1")
                }) { Text("取消") }
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
                FilledTonalButton(
                    onClick = { showAddItemDialog.value = true },
                    enabled = !viewModel.isSpinning
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("新建")
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = { viewModel.saveCurrentList(context) },
                    enabled = viewModel.items.isNotEmpty() && !viewModel.isSpinning
                ) {
                    Icon(Icons.Default.Save, contentDescription = "保存")
                }

                IconButton(
                    onClick = { viewModel.openSaveManagerDialog(context) },
                    enabled = !viewModel.isSpinning
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "管理")
                }
            }
        )

        if (viewModel.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "还没有抽选项",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "添加一些选项，或者载入以前保存的配置来开始抽选吧",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Button(onClick = { showAddItemDialog.value = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("新建项目")
                        }

                        OutlinedButton(onClick = { viewModel.openSaveManagerDialog(context) }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("载入配置")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (viewModel.items.isNotEmpty()) {
                    item {
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = viewModel.configName,
                                onValueChange = viewModel::updateConfigName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                placeholder = { Text("输入抽选主题（可选）", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

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

                if (viewModel.items.isNotEmpty()) {
                    item {
                        ItemsListCard(
                            items = viewModel.items,
                            totalWeight = viewModel.totalWeight,
                            onRemove = viewModel::removeItem,
                            onClearAll = viewModel::clearAllItems,
                            isSpinning = viewModel.isSpinning,
                            onEdit = { item ->
                                viewModel.startEditItem(item)
                                showAddItemDialog.value = true
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
    val presetColors = listOf(
        Color(0xFFFF6B6B), // 珊瑚红
        Color(0xFF4ECDC4), // 青绿色
        Color(0xFF45B7D1), // 天蓝色
        Color(0xFFFFA07A), // 浅橙色
        Color(0xFF98D8C8), // 薄荷绿
        Color(0xFFF7DC6F), // 柠檬黄
        Color(0xFFBB8FCE), // 淡紫色
        Color(0xFF85C1E2), // 浅蓝色
        Color(0xFFF8B739), // 金黄色
        Color(0xFF6C5CE7), // 紫罗兰
        Color(0xFFA29BFE), // 淡紫蓝
        Color(0xFFFF7675), // 粉红色
        Color(0xFF74B9FF), // 湖蓝色
        Color(0xFF55EFC4), // 翠绿色
        Color(0xFFFFEAA7), // 奶油黄
        Color(0xFFDFE6E9), // 浅灰色
        Color(0xFFFD79A8), // 玫瑰粉
        Color(0xFFFDCB6E), // 琥珀色
        Color(0xFF6C5CE7), // 深紫色
        Color(0xFF00CEC9)  // 青蓝色
    )
    
    val itemColors = remember(items.map { it.id }) {
        items.withIndex().associate { (index, item) ->
            item.id to presetColors[index % presetColors.size]
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (items.isEmpty()) return@Canvas

        val totalWeight = items.sumOf { it.weight }.toFloat()
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * 0.85f

        val displayAngle = rotationAngle % 360f

        rotate(displayAngle) {
            items.forEachIndexed { index, item ->
                val startAngle = items.take(index).sumOf { it.weight }.toFloat() / totalWeight * 360f
                val sweepAngle = item.weight.toFloat() / totalWeight * 360f
                val color = itemColors[item.id] ?: Color.Gray

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

                        val absoluteAngle = ((textAngle + displayAngle) % 360f + 360f) % 360f

                        val nativeRotation = if (absoluteAngle > 90f && absoluteAngle < 270f) {
                            textAngle + 180f
                        } else {
                            textAngle
                        }

                        rotate(nativeRotation, textX, textY)
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
fun ItemsListCard(
    items: List<RandomItem>,
    totalWeight: Int,
    onRemove: (Int) -> Unit,
    onClearAll: () -> Unit,
    isSpinning: Boolean,
    onEdit: (RandomItem) -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
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

                            Row {
                                IconButton(
                                    onClick = { onEdit(item) },
                                    enabled = !isSpinning
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
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