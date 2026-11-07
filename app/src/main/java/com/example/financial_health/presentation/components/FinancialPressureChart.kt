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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
    year: Int? = null,
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
        // Chart title and compact, single-line legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = year?.let {
                        "روند سهم اقساط از درآمد (${IranianPhoneUtils.convertDigitsToPersian(it.toString())} · ۱۲ ماه)"
                    } ?: "روند سهم اقساط از درآمد (۱۲ ماه سال جاری)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "هدف بهینه: زیر ۳۰ درصد درآمد",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Legend indicators stay beside the title and never wrap.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(safeColor)
                    )
                    Text(
                        text = "مناسب (<۳۰٪)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = textColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(warningColor)
                    )
                    Text(
                        text = "متوسط (<۵۰٪)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = textColor,
                        maxLines = 1,
                        softWrap = false
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
                val stepX = if (dataPoints.size <= 1) width / 2f else width / (dataPoints.size - 1)

                // Scale only from real observations; missing months keep their own x-axis slot.
                val validValues = dataPoints.filter { it.hasData }.map { it.pressurePercentage }
                val maxPressure = maxOf(60.0f, (validValues.maxOrNull() ?: 0f) + 10f)
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

                // Keep a fixed x-position for every calendar month, including missing data.
                val points = dataPoints.mapIndexed { index, item ->
                    val normY = if (item.hasData) {
                        ((item.pressurePercentage - minPressure) / (maxPressure - minPressure)).coerceIn(0f, 1f)
                    } else 0f
                    val targetY = topPadding + usableHeight * (1f - normY)
                    val initialY = topPadding + usableHeight
                    val currentY = initialY + (targetY - initialY) * animationProgress.value
                    Offset(index * stepX, currentY)
                }

                // Draw fill and line only between adjacent months that both have real data.
                for (index in 0 until points.lastIndex) {
                    if (!dataPoints[index].hasData || !dataPoints[index + 1].hasData) continue
                    val first = points[index]
                    val second = points[index + 1]
                    val segmentFill = Path().apply {
                        moveTo(first.x, topPadding + usableHeight)
                        lineTo(first.x, first.y)
                        lineTo(second.x, second.y)
                        lineTo(second.x, topPadding + usableHeight)
                        close()
                    }
                    drawPath(
                        path = segmentFill,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = if (isDark) 0.35f else 0.22f),
                                primaryColor.copy(alpha = 0.02f)
                            ),
                            startY = topPadding,
                            endY = topPadding + usableHeight
                        )
                    )
                    drawLine(
                        color = primaryColor,
                        start = first,
                        end = second,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Draw only observed values; missing months remain visibly blank.
                val lastValidIndex = dataPoints.indexOfLast { it.hasData }
                points.forEachIndexed { index, pt ->
                    if (!dataPoints[index].hasData) return@forEachIndexed
                    val value = dataPoints[index].pressurePercentage
                    val ptColor = when {
                        value <= 30f -> safeColor
                        value <= 50f -> warningColor
                        else -> alertColor
                    }

                    // Outer halo for the latest available observation
                    if (index == lastValidIndex && highlightLastPoint) {
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

            if (dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "برای نمایش روند واقعی، اطلاعات درآمد و برنامه کامل اقساط در ماه‌های گذشته لازم است.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        ),
                        color = textColor
                    )
                }
            }
        }

        // X-Axis month labels & percentage badges
        // Keep the chronological January-to-December axis aligned with the Canvas.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dataPoints.forEachIndexed { index, item ->
                    val isLast = index == dataPoints.lastIndex
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (item.hasData) "${IranianPhoneUtils.convertDigitsToPersian(java.text.DecimalFormat("0.#").format(item.pressurePercentage))}٪" else "—",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isLast) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 8.5.sp
                            ),
                            color = if (isLast) MaterialTheme.colorScheme.primary else textColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = item.monthName.take(3),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 8.5.sp
                            ),
                            color = if (isLast) MaterialTheme.colorScheme.onSurface else textColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
