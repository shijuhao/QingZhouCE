package com.example.toolbox.function.game

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random
import androidx.core.content.edit

// ==================== 数据模型 ====================

data class GameState(
    val grid: Array<IntArray> = Array(4) { IntArray(4) },
    val score: Int = 0,
    val bestScore: Int = 0,
    val gameOver: Boolean = false,
    val gameWon: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        if (!grid.contentDeepEquals(other.grid)) return false
        if (score != other.score) return false
        if (bestScore != other.bestScore) return false
        if (gameOver != other.gameOver) return false
        if (gameWon != other.gameWon) return false
        return true
    }

    override fun hashCode(): Int {
        var result = grid.contentDeepHashCode()
        result = 31 * result + score
        result = 31 * result + bestScore
        result = 31 * result + gameOver.hashCode()
        result = 31 * result + gameWon.hashCode()
        return result
    }
}

data class MoveInfo(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val merged: Boolean = false
)

data class GameSnapshot(
    val grid: List<List<Int>>,
    val score: Int
)

// ==================== 游戏逻辑 ====================

class Game2048Logic {
    companion object {
        const val SIZE = 4
        const val WIN_VALUE = 2048

        fun newGrid() = Array(SIZE) { IntArray(SIZE) }

        fun copyGrid(grid: Array<IntArray>) = Array(SIZE) { r -> grid[r].clone() }

        fun emptyTiles(grid: Array<IntArray>) = buildList {
            for (r in 0 until SIZE)
                for (c in 0 until SIZE)
                    if (grid[r][c] == 0) add(Pair(r, c))
        }

        fun spawnTile(grid: Array<IntArray>): Pair<Int, Int>? {
            val empty = emptyTiles(grid)
            if (empty.isEmpty()) return null
            val pos = empty[Random.nextInt(empty.size)]
            grid[pos.first][pos.second] = if (Random.nextFloat() < 0.9f) 2 else 4
            return pos
        }

        fun hasMovesLeft(grid: Array<IntArray>): Boolean {
            for (r in 0 until SIZE) {
                for (c in 0 until SIZE) {
                    if (grid[r][c] == 0) return true
                    if (c < SIZE - 1 && grid[r][c] == grid[r][c + 1]) return true
                    if (r < SIZE - 1 && grid[r][c] == grid[r + 1][c]) return true
                }
            }
            return false
        }

        fun move(grid: Array<IntArray>, direction: Direction): Triple<Boolean, Int, List<MoveInfo>> {
            val before = copyGrid(grid)
            val moves = mutableListOf<MoveInfo>()
            var gain = 0

            for (i in 0 until SIZE) {
                val line = IntArray(SIZE)
                val origins = Array(SIZE) { Pair(0, 0) }

                for (j in 0 until SIZE) {
                    val (r, c) = direction.toRC(i, j)
                    line[j] = grid[r][c]
                    origins[j] = Pair(r, c)
                }

                // 压缩并合并
                val packed = mutableListOf<Int>()
                val packedOrig = mutableListOf<Pair<Int, Int>>()
                for (j in 0 until SIZE) {
                    if (line[j] != 0) {
                        packed.add(line[j])
                        packedOrig.add(origins[j])
                    }
                }

                val out = IntArray(SIZE)
                val outSecs = mutableListOf<MutableList<Pair<Int, Int>>>()
                var k = 0
                while (k < packed.size) {
                    if (k + 1 < packed.size && packed[k] == packed[k + 1]) {
                        val value = packed[k] * 2
                        out[outSecs.size] = value
                        outSecs.add(mutableListOf(packedOrig[k], packedOrig[k + 1]))
                        gain += value
                        k += 2
                    } else {
                        out[outSecs.size] = packed[k]
                        outSecs.add(mutableListOf(packedOrig[k]))
                        k += 1
                    }
                }

                for (j in 0 until SIZE) {
                    val (r, c) = direction.toRC(i, j)
                    grid[r][c] = out[j]

                    for (src in outSecs.getOrNull(j) ?: emptyList()) {
                        if (src != Pair(r, c)) {
                            moves.add(MoveInfo(src.first, src.second, r, c, outSecs[j].size > 1))
                        }
                    }
                }
            }

            val changed = !grid.contentDeepEquals(before)
            return Triple(changed, gain, moves)
        }
    }
}

enum class Direction {
    LEFT, RIGHT, UP, DOWN;

    fun toRC(i: Int, j: Int): Pair<Int, Int> {
        return when (this) {
            LEFT -> Pair(i, j)
            RIGHT -> Pair(i, Game2048Logic.SIZE - 1 - j)
            UP -> Pair(j, i)
            DOWN -> Pair(Game2048Logic.SIZE - 1 - j, i)
        }
    }
}

// ==================== ViewModel ====================

class GameViewModel(private val prefs: SharedPreferences) : ViewModel() {
    private val _gameState = mutableStateOf(loadGameState())
    val gameState: State<GameState> = _gameState

    // 临时动画数据（移动动画中显示的临时瓦片）
    private val _pendingMoves = mutableStateOf<List<MoveInfo>>(emptyList())
    val pendingMoves: State<List<MoveInfo>> = _pendingMoves

    private var undoSnapshot: GameSnapshot? = null
    private var isAnimating = false

    init {
        if (_gameState.value.grid.contentDeepEquals(Array(4) { IntArray(4) })) {
            startNewGame()
        }
    }

    private fun loadGameState(): GameState {
        val grid = Array(4) { r ->
            IntArray(4) { c ->
                prefs.getInt("grid_${r}_${c}", 0)
            }
        }
        val hasSavedGame = grid.any { row -> row.any { it != 0 } }
        return if (hasSavedGame) {
            GameState(
                grid = grid,
                score = prefs.getInt("current_score", 0),
                bestScore = prefs.getInt("best_score", 0),
                gameOver = prefs.getBoolean("game_over", false),
                gameWon = prefs.getBoolean("game_won", false)
            )
        } else {
            GameState(bestScore = prefs.getInt("best_score", 0))
        }
    }

    private fun saveGameState() {
        val state = _gameState.value
        prefs.edit {
            for (r in 0 until 4) {
                for (c in 0 until 4) {
                    putInt("grid_${r}_${c}", state.grid[r][c])
                }
            }
            putInt("current_score", state.score)
            putInt("best_score", state.bestScore)
            putBoolean("game_over", state.gameOver)
            putBoolean("game_won", state.gameWon)
        }
    }

    fun startNewGame() {
        if (isAnimating) return
        val newGrid = Game2048Logic.newGrid()
        Game2048Logic.spawnTile(newGrid)
        Game2048Logic.spawnTile(newGrid)
        _gameState.value = GameState(
            grid = newGrid,
            score = 0,
            bestScore = _gameState.value.bestScore,
            gameOver = false,
            gameWon = false
        )
        undoSnapshot = null
        saveGameState()
    }

    suspend fun move(direction: Direction) {
        if (isAnimating || _gameState.value.gameOver) return

        val current = _gameState.value
        val gridCopy = Game2048Logic.copyGrid(current.grid)
        val (changed, gain, moves) = Game2048Logic.move(gridCopy, direction)

        if (!changed) return

        // 保存撤销快照
        undoSnapshot = GameSnapshot(
            grid = current.grid.map { it.toList() },
            score = current.score
        )

        val newScore = current.score + gain
        val newBest = max(newScore, current.bestScore)

        // 检查胜利（只有在尚未胜利时）
        var won = current.gameWon
        if (!won) {
            for (r in 0 until 4) {
                for (c in 0 until 4) {
                    if (gridCopy[r][c] == Game2048Logic.WIN_VALUE) {
                        won = true
                        break
                    }
                }
            }
        }

        // 生成新瓦片
        val spawnPos = Game2048Logic.spawnTile(gridCopy)
        val gameOver = !Game2048Logic.hasMovesLeft(gridCopy)

        // 开始动画：显示移动效果
        isAnimating = true
        _pendingMoves.value = moves

        // 等待移动动画（约150ms）
        delay(150)

        // 应用新状态
        _gameState.value = GameState(
            grid = gridCopy,
            score = newScore,
            bestScore = newBest,
            gameOver = gameOver,
            gameWon = won
        )
        _pendingMoves.value = emptyList()

        // 播放生成动画（新瓦片会出现，已经在Grid中显示，但我们可以通过一个标志来增加特效，这里简单延时）
        if (spawnPos != null) {
            // 触发一个短暂的生成效果，可以通过状态控制，但为了简洁，直接略过额外动画
            delay(100)
        }

        isAnimating = false
        saveGameState()

        // 更新最佳分数到SharedPreferences
        if (newBest > current.bestScore) {
            prefs.edit { putInt("best_score", newBest) }
        }
    }

    fun canUndo(): Boolean = undoSnapshot != null

    fun undo() {
        if (isAnimating) return
        val snapshot = undoSnapshot ?: return
        _gameState.value = GameState(
            grid = snapshot.grid.map { it.toIntArray() }.toTypedArray(),
            score = snapshot.score,
            bestScore = _gameState.value.bestScore,
            gameOver = false,
            gameWon = checkHas2048(snapshot.grid.map { it.toIntArray() }.toTypedArray())
        )
        undoSnapshot = null
        saveGameState()
    }

    private fun checkHas2048(grid: Array<IntArray>): Boolean {
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if (grid[r][c] == Game2048Logic.WIN_VALUE) return true
            }
        }
        return false
    }
}

// ==================== UI 样式 ====================

val TileColors = mapOf(
    0 to Color(0x1AFFFFFF),
    2 to Color(0xFFEEE4DA),
    4 to Color(0xFFEDE0C8),
    8 to Color(0xFFF2B179),
    16 to Color(0xFFF59563),
    32 to Color(0xFFF67C5F),
    64 to Color(0xFFF65E3B),
    128 to Color(0xFFEDCF72),
    256 to Color(0xFFEDCC61),
    512 to Color(0xFFEDC850),
    1024 to Color(0xFFEDC53F),
    2048 to Color(0xFFEDC22E)
)

fun tileColor(value: Int) = TileColors[value] ?: Color(0xFF3C3A32)
fun tileTextColor(value: Int) = if (value in listOf(2, 4)) Color(0xFF776E65) else Color.White

fun tileTextSize(value: Int): Float = when {
    value < 100 -> 42f
    value < 1000 -> 36f
    value < 10000 -> 30f
    else -> 24f
}

// ==================== Composable 屏幕 ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Game2048Screen(viewModel: GameViewModel = viewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val gameState by viewModel.gameState
    val pendingMoves by viewModel.pendingMoves

    // 用于显示得分增加动画
    var lastScore by remember { mutableIntStateOf(gameState.score) }
    var scoreGain by remember { mutableIntStateOf(0) }

    // 当分数增加时记录增量并触发动画
    LaunchedEffect(gameState.score) {
        if (gameState.score > lastScore) {
            scoreGain = gameState.score - lastScore
            delay(200)
            scoreGain = 0
        }
        lastScore = gameState.score
    }

    // 游戏胜利/结束的弹窗显示
    var showWinDialog by remember { mutableStateOf(false) }
    var showGameOverDialog by remember { mutableStateOf(false) }

    LaunchedEffect(gameState.gameWon) {
        if (gameState.gameWon && !showWinDialog) {
            showWinDialog = true
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
    LaunchedEffect(gameState.gameOver) {
        if (gameState.gameOver && !showGameOverDialog && gameState.score > 0) {
            showGameOverDialog = true
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("2048") },
                subtitle = { Text("合并方块，达成 2048！") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ),
                navigationIcon = {
                    IconButton(onClick = { (context as Activity).finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 分数栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedScoreCard("分数", gameState.score, scoreGain, Modifier.weight(1f))
                ScoreCard("最佳", gameState.bestScore, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.undo() },
                    enabled = viewModel.canUndo() && !gameState.gameOver,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("撤回")
                }
                Button(
                    onClick = { viewModel.startNewGame() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("新游戏")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 棋盘
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {},
                            onDragEnd = {},
                            onDragCancel = {}
                        ) { change, dragAmount ->
                            change.consume()
                            val (dx, dy) = dragAmount
                            val direction = when {
                                abs(dx) > abs(dy) && abs(dx) > 20f -> if (dx > 0) Direction.RIGHT else Direction.LEFT
                                abs(dy) > abs(dx) && abs(dy) > 20f -> if (dy > 0) Direction.DOWN else Direction.UP
                                else -> null
                            }
                            direction?.let {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                scope.launch { viewModel.move(it) }
                            }
                        }
                    }
            ) {
                GameGrid(
                    grid = gameState.grid,
                    pendingMoves = pendingMoves,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 弹窗
        if (showWinDialog) {
            WinDialog(onDismiss = { showWinDialog = false })
        }
        if (showGameOverDialog) {
            GameOverDialog(
                score = gameState.score,
                onNewGame = {
                    viewModel.startNewGame()
                    showGameOverDialog = false
                },
                onDismiss = { showGameOverDialog = false }
            )
        }
    }
}

@Composable
fun ScoreCard(title: String, score: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(score.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AnimatedScoreCard(title: String, score: Int, gain: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                Text(score.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (gain > 0) {
                    @Suppress("RemoveRedundantQualifierName")
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }) + scaleIn(),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }) + scaleOut()
                    ) {
                        Text(
                            "+$gain",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.offset(x = 40.dp, y = (-8).dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameGrid(
    grid: Array<IntArray>,
    pendingMoves: List<MoveInfo>,
    modifier: Modifier = Modifier
) {
    var cellSizePx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val animatedOffsets = remember { mutableStateMapOf<Pair<Int, Int>, Animatable<Offset, AnimationVector2D>>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pendingMoves) {
        if (pendingMoves.isNotEmpty()) {
            // 清除旧的偏移
            animatedOffsets.clear()

            // 等待一帧，确保 cellSizePx 已经更新
            delay(16)

            // 创建新动画
            pendingMoves.forEach { move ->
                val key = Pair(move.fromRow, move.fromCol)
                val startOffset = Offset(0f, 0f)
                val deltaCol = (move.toCol - move.fromCol).toFloat() * cellSizePx
                val deltaRow = (move.toRow - move.fromRow).toFloat() * cellSizePx
                val targetOffset = Offset(deltaCol, deltaRow)

                val animatable = Animatable(startOffset, Offset.VectorConverter)
                animatedOffsets[key] = animatable
                scope.launch {
                    animatable.animateTo(
                        targetOffset,
                        animationSpec = tween(150, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    // 当移动动画结束时清除偏移
    LaunchedEffect(pendingMoves.isEmpty()) {
        if (pendingMoves.isEmpty()) {
            // 延迟清除，让动画完成
            delay(150)
            animatedOffsets.clear()
        }
    }

    Box(modifier = modifier) {
        // 使用 BoxWithConstraints 来获取实际尺寸
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            // 计算每个单元格的尺寸（减去内边距和间距）
            val sidePadding = 16.dp  // 左右内边距 8dp * 2
            val spacing = 8.dp       // 单元格间距
            val totalSpacing = spacing * 3 // 4列有3个间距
            val availableWidth = maxWidth - sidePadding - totalSpacing
            val cellSizeDp = availableWidth / 4

            // 更新像素尺寸
            LaunchedEffect(cellSizeDp) {
                cellSizePx = with(density) { cellSizeDp.toPx() }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0 until 4) {
                            val value = grid[row][col]
                            val isMoving = pendingMoves.any { it.fromRow == row && it.fromCol == col }
                            val offset = animatedOffsets[Pair(row, col)]?.value

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Tile(
                                    value = value,
                                    modifier = Modifier.fillMaxSize(),
                                    isMoving = isMoving,
                                    offset = offset
                                )
                            }
                        }
                    }
                    if (row < 3) Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun Tile(
    value: Int,
    modifier: Modifier = Modifier,
    isMoving: Boolean = false,
    offset: Offset? = null
) {
    val backgroundColor = tileColor(value)
    val textColor = tileTextColor(value)
    val textSize = tileTextSize(value)

    // 生成动画：当 value 从 0 变为非零时（新瓦片出现）
    val spawnScale by animateFloatAsState(
        targetValue = if (value != 0) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "spawnScale"
    )

    // 合并动画：当瓦片是合并目标时，放大闪烁
    val mergeScale by animateFloatAsState(
        targetValue = if (isMoving) 1.2f else 1f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "mergeScale"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = spawnScale * mergeScale
                scaleY = spawnScale * mergeScale
                offset?.let {
                    translationX = it.x
                    translationY = it.y
                }
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isMoving) 4.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (value != 0) {
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.8f)) togetherWith
                                (fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f))
                    },
                    label = "valueAnimation"
                ) { targetValue ->
                    Text(
                        text = targetValue.toString(),
                        fontSize = textSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(score: Int, onNewGame: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("游戏结束") },
        text = { Text("您的得分：$score") },
        confirmButton = {
            Button(onClick = onNewGame) {
                Text("新游戏")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun WinDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎉 胜利！") },
        text = { Text("您达到了 2048！\n可以继续游戏挑战更高分数。") },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("继续")
            }
        }
    )
}

// ==================== Activity ====================

class Game2048Activity : ComponentActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var viewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("game2048_prefs", MODE_PRIVATE)
        viewModel = GameViewModel(prefs)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Game2048Screen(viewModel = viewModel)
            }
        }
    }
}