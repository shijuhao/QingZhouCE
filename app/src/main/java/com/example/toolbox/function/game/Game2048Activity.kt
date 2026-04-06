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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    private val _pendingMoves = mutableStateOf<List<MoveInfo>>(emptyList())
    val pendingMoves: State<List<MoveInfo>> = _pendingMoves

    private val _pendingMerges = mutableStateOf<Set<Pair<Int, Int>>>(emptySet())
    val pendingMerges: State<Set<Pair<Int, Int>>> = _pendingMerges

    private val _spawningTiles = mutableStateOf<Set<Pair<Int, Int>>>(emptySet())
    val spawningTiles: State<Set<Pair<Int, Int>>> = _spawningTiles

    private val _animatedGrid = mutableStateOf<Array<IntArray>?>(null)
    val animatedGrid: State<Array<IntArray>?> = _animatedGrid

    private var undoSave: GameSnapshot? = null
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
        undoSave = null
        saveGameState()
    }

    suspend fun move(direction: Direction) {
        if (isAnimating || _gameState.value.gameOver) return

        val current = _gameState.value
        val gridAfterMove = Game2048Logic.copyGrid(current.grid)
        val (changed, gain, moves) = Game2048Logic.move(gridAfterMove, direction)

        if (!changed) return

        // 保存撤销快照
        undoSave = GameSnapshot(
            grid = current.grid.map { it.toList() },
            score = current.score
        )

        val newScore = current.score + gain
        val newBest = max(newScore, current.bestScore)

        // 检查胜利
        var won = current.gameWon
        if (!won) {
            for (r in 0 until 4) {
                for (c in 0 until 4) {
                    if (gridAfterMove[r][c] == Game2048Logic.WIN_VALUE) {
                        won = true
                        break
                    }
                }
            }
        }

        isAnimating = true

        _pendingMoves.value = moves
        _pendingMerges.value = moves.filter { it.merged }.map { Pair(it.toRow, it.toCol) }.toSet()

        _animatedGrid.value = current.grid

        delay(180)

        _pendingMoves.value = emptyList()
        _pendingMerges.value = emptySet()
        _animatedGrid.value = null

        val spawnPos = Game2048Logic.spawnTile(gridAfterMove)
        val gameOver = !Game2048Logic.hasMovesLeft(gridAfterMove)

        if (spawnPos != null) {
            _spawningTiles.value = setOf(spawnPos)
        }

        _gameState.value = GameState(
            grid = gridAfterMove,
            score = newScore,
            bestScore = newBest,
            gameOver = gameOver,
            gameWon = won
        )

        if (spawnPos != null) {
            delay(150)
            _spawningTiles.value = emptySet()
        }

        isAnimating = false
        saveGameState()

        if (newBest > current.bestScore) {
            prefs.edit { putInt("best_score", newBest) }
        }
    }

    fun canUndo(): Boolean = undoSave != null

    fun undo() {
        if (isAnimating) return
        val snapshot = undoSave ?: return
        _gameState.value = GameState(
            grid = snapshot.grid.map { it.toIntArray() }.toTypedArray(),
            score = snapshot.score,
            bestScore = _gameState.value.bestScore,
            gameOver = false,
            gameWon = checkHas2048(snapshot.grid.map { it.toIntArray() }.toTypedArray())
        )
        undoSave = null
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
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedScoreCard("分数", gameState.score, scoreGain, Modifier.weight(1f))
                    ScoreCard("最佳", gameState.bestScore, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 控制按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.undo() },
                        enabled = viewModel.canUndo() && !gameState.gameOver,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("撤回")
                    }
                    Button(
                        onClick = { viewModel.startNewGame() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("新游戏")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 棋盘 - 使用动态计算的最大尺寸
                Box(
                    modifier = Modifier
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
                        animatedGrid = viewModel.animatedGrid.value,
                        pendingMoves = pendingMoves,
                        pendingMerges = viewModel.pendingMerges.value,
                        spawningTiles = viewModel.spawningTiles.value,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 游戏说明 - 小屏幕时自动滚动可见
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "💡 滑动屏幕移动方块",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "相同数字的方块会合并",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                score.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                Text(
                    score.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (gain > 0) {
                    @Suppress("RemoveRedundantQualifierName")
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }) + scaleIn(),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }) + scaleOut()
                    ) {
                        Text(
                            "+$gain",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.offset(x = 32.dp, y = (-6).dp)
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
    animatedGrid: Array<IntArray>?,
    pendingMoves: List<MoveInfo>,
    pendingMerges: Set<Pair<Int, Int>>,
    spawningTiles: Set<Pair<Int, Int>>,
    modifier: Modifier = Modifier
) {
    var cellSizePx by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    val moveAnimations = remember { mutableStateMapOf<String, Animatable<Offset, AnimationVector2D>>() }
    val mergeAnimations = remember { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }

    LaunchedEffect(pendingMoves) {
        if (pendingMoves.isEmpty()) return@LaunchedEffect

        while (cellSizePx == 0f) {
            delay(10)
        }

        pendingMoves.forEach { move ->
            val fromKey = "${move.fromRow},${move.fromCol}"
            val deltaX = (move.toCol - move.fromCol).toFloat() * cellSizePx
            val deltaY = (move.toRow - move.fromRow).toFloat() * cellSizePx

            val animatable = Animatable(Offset.Zero, Offset.VectorConverter)
            moveAnimations[fromKey] = animatable
            scope.launch {
                animatable.animateTo(
                    Offset(deltaX, deltaY),
                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                )
                moveAnimations.remove(fromKey)
            }
        }
    }

    LaunchedEffect(pendingMerges) {
        pendingMerges.forEach { target ->
            val key = "${target.first},${target.second}"
            val animatable = mergeAnimations.getOrPut(key) {
                Animatable(1f, Float.VectorConverter)
            }
            scope.launch {
                animatable.animateTo(1.25f, animationSpec = tween(80))
                animatable.animateTo(1f, animationSpec = tween(100))
                delay(50)
                mergeAnimations.remove(key)
            }
        }
    }

    val displayGrid = animatedGrid ?: grid

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                cellSizePx = coordinates.size.width.toFloat() / 4f
            }
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until 4) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0 until 4) {
                        val isSource = pendingMoves.any { it.fromRow == row && it.fromCol == col }
                        val isTarget = pendingMoves.any { it.toRow == row && it.toCol == col }
                        val mergeTarget = pendingMerges.contains(Pair(row, col))

                        val moveOffset = moveAnimations["$row,$col"]?.value

                        val mergeScale = if (mergeTarget) {
                            mergeAnimations["$row,$col"]?.value ?: 1f
                        } else 1f

                        var displayValue = displayGrid[row][col]

                        if (isSource && moveOffset != null) {
                            displayValue = displayGrid[row][col]
                        }

                        val isMergedTarget = pendingMoves.any { it.toRow == row && it.toCol == col && it.merged }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (!isTarget || moveOffset == null) {
                                Tile(
                                    value = displayValue,
                                    modifier = Modifier.fillMaxSize(),
                                    moveOffset = moveOffset,
                                    mergeScale = mergeScale,
                                    isSpawning = spawningTiles.contains(Pair(row, col)),
                                    isMerged = isMergedTarget
                                )
                            }

                            if (isSource && moveOffset != null && displayValue != 0) {
                                Tile(
                                    value = displayValue,
                                    modifier = Modifier.fillMaxSize(),
                                    moveOffset = moveOffset,
                                    mergeScale = 1f,
                                    isSpawning = false,
                                    isMoving = true
                                )
                            }
                        }
                    }
                }
                if (row < 3) Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun Tile(
    value: Int,
    modifier: Modifier = Modifier,
    moveOffset: Offset? = null,
    mergeScale: Float = 1f,
    isSpawning: Boolean = false,
    isMoving: Boolean = false,
    isMerged: Boolean = false
) {
    val backgroundColor = tileColor(value)
    val textColor = tileTextColor(value)
    val textSize = tileTextSize(value)

    // 生成动画
    val spawnScale = remember { Animatable(0f) }
    LaunchedEffect(isSpawning) {
        if (isSpawning) {
            spawnScale.snapTo(0f)
            spawnScale.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }
    val finalSpawnScale = if (isSpawning) spawnScale.value else 1f

    // 合并时的闪烁效果
    val mergedFlash by animateColorAsState(
        targetValue = if (isMerged) Color(0xFFFFD700) else backgroundColor,
        animationSpec = tween(150),
        label = "mergedFlash"
    )

    val finalColor = if (isMerged) mergedFlash else backgroundColor

    Card(
        modifier = modifier
            .graphicsLayer {
                translationX = moveOffset?.x ?: 0f
                translationY = moveOffset?.y ?: 0f
                scaleX = finalSpawnScale * mergeScale
                scaleY = finalSpawnScale * mergeScale
                // 移动中的瓦片增加透明度
                alpha = if (isMoving) 0.9f else 1f
            },
        colors = CardDefaults.cardColors(containerColor = finalColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isMoving) 6.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (value != 0) {
                Text(
                    text = value.toString(),
                    fontSize = textSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
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