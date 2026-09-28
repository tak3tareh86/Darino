package com.example.ui.screens.lock.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import kotlin.math.sqrt

/**
 * Interactive 3x3 Pattern Lock View.
 * Tracks touch gestures connecting 9 points (0..8).
 */
@Composable
fun PatternGridLock(
    selectedPoints: List<Int>,
    onPatternChange: (List<Int>) -> Unit,
    onPatternComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
    normalColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    activeColor: Color = EmeraldPrimaryLight,
    errorColor: Color = ExpenseRoseLight
) {
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    val internalPoints = remember { mutableStateListOf<Int>() }

    // Sync from external state
    LaunchedEffect(selectedPoints) {
        if (selectedPoints.isEmpty() && internalPoints.isNotEmpty()) {
            internalPoints.clear()
            currentTouchPoint = null
        }
    }

    val lineColor = when {
        isError -> errorColor
        else -> activeColor
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput

                    detectDragGestures(
                        onDragStart = { offset ->
                            internalPoints.clear()
                            val hit = findHitPoint(offset, size.width.toFloat(), size.height.toFloat())
                            if (hit != null) {
                                internalPoints.add(hit)
                                onPatternChange(internalPoints.toList())
                            }
                            currentTouchPoint = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val hit = findHitPoint(change.position, size.width.toFloat(), size.height.toFloat())
                            if (hit != null && !internalPoints.contains(hit)) {
                                internalPoints.add(hit)
                                onPatternChange(internalPoints.toList())
                            }
                            currentTouchPoint = change.position
                        },
                        onDragEnd = {
                            currentTouchPoint = null
                            if (internalPoints.isNotEmpty()) {
                                onPatternComplete(internalPoints.toList())
                            }
                        },
                        onDragCancel = {
                            currentTouchPoint = null
                            if (internalPoints.isNotEmpty()) {
                                onPatternComplete(internalPoints.toList())
                            }
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val cellWidth = width / 3f
            val cellHeight = height / 3f

            // Calculate exact center of each 3x3 dot
            fun getCenterOfDot(index: Int): Offset {
                val row = index / 3
                val col = index % 3
                val x = col * cellWidth + cellWidth / 2f
                val y = row * cellHeight + cellHeight / 2f
                return Offset(x, y)
            }

            // 1. Draw connected path lines
            if (internalPoints.isNotEmpty()) {
                for (i in 0 until internalPoints.size - 1) {
                    val start = getCenterOfDot(internalPoints[i])
                    val end = getCenterOfDot(internalPoints[i + 1])
                    drawLine(
                        color = lineColor,
                        start = start,
                        end = end,
                        strokeWidth = 10.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Drag line to current fingertip
                currentTouchPoint?.let { touchPos ->
                    val lastPoint = getCenterOfDot(internalPoints.last())
                    drawLine(
                        color = lineColor.copy(alpha = 0.7f),
                        start = lastPoint,
                        end = touchPos,
                        strokeWidth = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. Draw 9 circular node dots
            for (index in 0 until 9) {
                val center = getCenterOfDot(index)
                val isSelected = internalPoints.contains(index)

                val dotRadius = if (isSelected) 18.dp.toPx() else 10.dp.toPx()
                val outerRingRadius = if (isSelected) 28.dp.toPx() else 14.dp.toPx()

                // Outer halo/ring for selected dots
                if (isSelected) {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.22f),
                        radius = outerRingRadius,
                        center = center
                    )
                }

                // Inner core dot
                drawCircle(
                    color = if (isSelected) lineColor else normalColor,
                    radius = dotRadius,
                    center = center
                )
            }
        }
    }
}

private fun findHitPoint(offset: Offset, width: Float, height: Float): Int? {
    val cellWidth = width / 3f
    val cellHeight = height / 3f
    val hitThreshold = (cellWidth.coerceAtMost(cellHeight) * 0.42f)

    for (index in 0 until 9) {
        val row = index / 3
        val col = index % 3
        val centerX = col * cellWidth + cellWidth / 2f
        val centerY = row * cellHeight + cellHeight / 2f

        val dx = offset.x - centerX
        val dy = offset.y - centerY
        val distance = sqrt(dx * dx + dy * dy)

        if (distance <= hitThreshold) {
            return index
        }
    }
    return null
}
