package com.example.toolbox.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val DEFAULT_INDICATOR_HEIGHT = 3.dp

@Composable
fun TabIndicatorScope.RoundedCornerTabIndicator(
    index: Int,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    indicatorHeight: Dp = DEFAULT_INDICATOR_HEIGHT
) {
    var startAnimatable by remember { mutableStateOf<Animatable<Dp, AnimationVector1D>?>(null) }
    var endAnimatable by remember { mutableStateOf<Animatable<Dp, AnimationVector1D>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        Modifier
            .tabIndicatorLayout { measurable: Measurable,
                                  constraints: Constraints,
                                  tabPositions: List<TabPosition> ->
                val newStart = tabPositions[index].left
                val newEnd = tabPositions[index].right

                val startAnim =
                    startAnimatable
                        ?: Animatable(newStart, Dp.VectorConverter).also { startAnimatable = it }

                val endAnim =
                    endAnimatable
                        ?: Animatable(newEnd, Dp.VectorConverter).also { endAnimatable = it }

                if (endAnim.targetValue != newEnd) {
                    coroutineScope.launch {
                        endAnim.animateTo(
                            newEnd,
                            animationSpec =
                                if (endAnim.value < newEnd) {
                                    spring(stiffness = Spring.StiffnessHigh)
                                } else {
                                    spring(stiffness = Spring.StiffnessMedium)
                                }
                        )
                    }
                }

                if (startAnim.targetValue != newStart) {
                    coroutineScope.launch {
                        startAnim.animateTo(
                            newStart,
                            animationSpec =
                                if (startAnim.value < newStart) {
                                    spring(stiffness = Spring.StiffnessMedium)
                                } else {
                                    spring(stiffness = Spring.StiffnessHigh)
                                }
                        )
                    }
                }

                val indicatorEnd = endAnim.value.roundToPx()
                val indicatorStart = startAnim.value.roundToPx()

                val indicatorWidth = indicatorEnd - indicatorStart
                val indicatorHeightPx = indicatorHeight.roundToPx()
                val horizontalPadding =
                    (tabPositions[index].width - tabPositions[index].contentWidth).times(0.5f)
                        .roundToPx()

                val placeable =
                    measurable.measure(
                        Constraints.fixed(
                            width = (indicatorWidth - horizontalPadding * 2).coerceIn(
                                0,
                                indicatorWidth
                            ),
                            height = indicatorHeightPx
                        )
                    )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(
                        x = indicatorStart + horizontalPadding,
                        y = constraints.maxHeight - indicatorHeightPx
                    )
                }
            }
            .fillMaxSize()
            .drawWithContent {
                val path = Path().apply {
                    val cornerRadius = CornerRadius(size.height, size.height)
                    addRoundRect(
                        RoundRect(
                            rect = Rect(offset = Offset.Zero, size),
                            topLeft = cornerRadius,
                            topRight = cornerRadius,
                            bottomLeft = CornerRadius.Zero,
                            bottomRight = CornerRadius.Zero
                        )
                    )
                }
                drawPath(path, indicatorColor)
            }
    )
}
