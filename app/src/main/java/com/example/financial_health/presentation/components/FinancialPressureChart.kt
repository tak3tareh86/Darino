package com.example.financial_health.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financial_health.domain.MonthlyPressureData
import com.example.util.IranianPhoneUtils

@Composable
fun FinancialPressureChart(
    dataPoints: List<MonthlyPressureData>,
    modifier: Modifier = Modifier,
    highlightLastPoint: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(dataPoints) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val safeColor = Color(0xFF10B981)
    val warningColor = Color(0xFFF59E0B)
    val alertColor = Color(0xFFEF4444)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDark) Color(0xFF131D2E) else Color(0xFFF8FAFC)
            )
            .padding(16.dp)
            .testTag("financial_pressure_chart"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Chart Top Legend & Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "روند تغییر فشار مالی (۶ ماه اخیر)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "هدف بهینه: زیر ۳۰ درصد درآمد",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Legend indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(safeColor)
                    )
                    Text(
                        text = "مناسب (<۳۰٪)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = textColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(warningColor)
                    )
                    Text(
                        text = "متوسط (<۵۰٪)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = textColor
                    )
                }
            }
        }

        // Main Drawing Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (dataPoints.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height
                val topPadding = 24.dp.toPx()
                val bottomPadding = 28.dp.toPx()
                val usableHeight = height - topPadding - bottomPadding
                val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)

                // Max scale is 60% or highest point + 10%
                val maxPressure = 60.0f
                val minPressure = 0.0f

                // Draw reference lines at 30% and 50%
                val y30 = topPadding + usableHeight * (1f - (30f - minPressure) / (maxPressure - minPressure))
                val y50 = topPadding + usableHeight * (1f - (50f - minPressure) / (maxPressure - minPressure))

                // Line 30% (Safe limit)
                drawLine(
                    color = safeColor.copy(alpha = 0.4f),
                    start = Offset(0f, y30),
                    end = Offset(width, y30),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                // Line 50% (High alert limit)
                drawLine(
                    color = alertColor.copy(alpha = 0.35f),
                    start = Offset(0f, y50),
                    end = Offset(width, y50),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                // Calculate animated coordinates
                val points = dataPoints.mapIndexed { index, item ->
                    val normY = ((item.pressurePercentage - minPressure) / (maxPressure - minPressure)).coerceIn(0f, 1f)
                    val targetY = topPadding + usableHeight * (1f - normY)
                    val initialY = topPadding + usableHeight
                    val currentY = initialY + (targetY - initialY) * animationProgress.value
                    Offset(index * stepX, currentY)
                }

                // Construct area fill path
                val fillPath = Path().apply {
                    moveTo(points.first().x, topPadding + usableHeight)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, topPadding + usableHeight)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = if (isDark) 0.35f else 0.22f),
                            primaryColor.copy(alpha = 0.02f)
                        ),
                        startY = topPadding,
                        endY = topPadding + usableHeight
                    )
                )

                // Construct line path
                val strokePath = Path().apply {
                    points.forEachIndexed { i, pt ->
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }

                drawPath(
                    path = strokePath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw circles and values
                points.forEachIndexed { index, pt ->
                    val value = dataPoints[index].pressurePercentage
                    val ptColor = when {
                        value <= 30f -> safeColor
                        value <= 50f -> warningColor
                        else -> alertColor
                    }

                    // Outer halo for last point
                    if (index == points.lastIndex && highlightLastPoint) {
                        drawCircle(
                            color = ptColor.copy(alpha = 0.3f),
                            radius = 11.dp.toPx(),
                            center = pt
                        )
                    }

                    // Main dot
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = ptColor,
                        radius = 3.5.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        // X-Axis month labels & percentage badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dataPoints.forEachIndexed { index, item ->
                val isLast = index == dataPoints.lastIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${IranianPhoneUtils.convertDigitsToPersian(item.pressurePercentage.toInt().toString())}٪",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isLast) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 10.5.sp
                        ),
                        color = if (isLast) MaterialTheme.colorScheme.primary else textColor
                    )
                    Text(
                        text = item.monthName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        ),
                        color = if (isLast) MaterialTheme.colorScheme.onSurface else textColor
                    )
                }
            }
        }
    }
}
