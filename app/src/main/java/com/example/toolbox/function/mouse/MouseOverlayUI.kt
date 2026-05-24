package com.example.toolbox.function.mouse

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.toolbox.R
import kotlinx.coroutines.launch

@Composable
fun MinimizeButtonComposable(
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .size(56.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xE1000000)
        ),
        shape = RoundedCornerShape(28.dp),
        onClick = onRestore
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "恢复",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun MousePointerComposable(
    size: Int,
    alpha: Float,
    style: Int,
    showClock: Boolean,
    showBattery: Boolean
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            1 -> MouseImage(R.drawable.mouse1)
            2 -> MouseImage(R.drawable.mouse2)
            3 -> MouseImage(R.drawable.mouse3)
            4 -> MouseImage(R.drawable.mouse4)
            5 -> MouseImage(R.drawable.mouse5)
            6 -> MouseImage(R.drawable.mouse6)
            else -> MouseImage(R.drawable.mouse1)
        }
        
        if (showClock || showBattery) {
            Column(
                modifier = Modifier.offset(x = (size / 2).dp, y = (-size).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showClock) {
                    ClockDisplay()
                }
                if (showBattery) {
                    BatteryDisplay()
                }
            }
        }
    }
}

@Composable
private fun MouseImage(resourceId: Int) {
    Image(
        painter = painterResource(id = resourceId),
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ClockDisplay() {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Text(
        text = currentTime,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall
    )
}

@Composable
private fun BatteryDisplay() {
    val batteryLevel by remember { mutableIntStateOf(80) }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CircularProgressIndicator(
            progress = { batteryLevel / 100f },
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = Color.Green
        )
        Text(
            text = "$batteryLevel%",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun getCurrentTime(): String {
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    return String.format("%02d:%02d", hour, minute)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlPanelComposable(
    onMove: (Float, Float) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipe: (SwipeDirection) -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    speed: Float
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    val cardWidth = with(density) { 280.dp.toPx() }
    val cardHeight = with(density) { 250.dp.toPx() } // 估算高度
    
    Card(
        modifier = Modifier
            .padding(16.dp)
            .width(280.dp)
            .offset { 
                // 限制偏移量在屏幕范围内
                val clampedX = offsetX.coerceIn(-screenWidth / 2 + cardWidth / 2, screenWidth / 2 - cardWidth / 2)
                val clampedY = offsetY.coerceIn(-screenHeight / 2 + cardHeight / 2, screenHeight / 2 - cardHeight / 2)
                IntOffset(clampedX.toInt(), clampedY.toInt())
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xE1000000)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "左键点击",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { onSwipe(SwipeDirection.UP) }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "向上",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { onSwipe(SwipeDirection.DOWN) }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "向下",
                            tint = Color.White
                        )
                    }
                }
                
                IconButton(onClick = onLongClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "右键点击",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            DirectionPad(
                onMove = onMove,
                speed = speed
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onMinimize) {
                    Icon(
                        imageVector = Icons.Default.Minimize,
                        contentDescription = "最小化",
                        tint = Color.White
                    )
                }
                
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectionPad(
    onMove: (Float, Float) -> Unit,
    speed: Float
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    
    LaunchedEffect(offsetX, offsetY) {
        launch {
            animOffsetX.snapTo(offsetX)
        }
        launch {
            animOffsetY.snapTo(offsetY)
        }
    }
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val scaledDx = dragAmount.x * speed
                        val scaledDy = dragAmount.y * speed
                        
                        offsetX += scaledDx
                        offsetY += scaledDy
                        
                        onMove(scaledDx, scaledDy)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.5f))
                .offset {
                    IntOffset(
                        animOffsetX.value.toInt(),
                        animOffsetY.value.toInt()
                    )
                }
        )
    }
}
