package com.example.reports.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reports.domain.CategoryExpenseItem
import com.example.reports.domain.ExpenseTrendPoint
import com.example.reports.domain.MonthlyExpenseBar
import com.example.util.IranianPhoneUtils
import java.text.NumberFormat
import java.util.Locale

/**
 * Modern animated Donut Chart with Category breakdown.
 */
@Composable
fun ModernDonutChart(
    categories: List<CategoryExpenseItem>,
    totalAmount: Long,
    modifier: Modifier = Modifier,
    chartSize: Dp = 190.dp,
    strokeWidth: Dp = 26.dp
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(categories) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(chartSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = strokeWidth.toPx()
                val radius = (size.minDimension - strokePx) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)
                val arcSize = Size(radius * 2, radius * 2)
                val topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)

                // Background track
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                var startAngle = -90f
                val total = categories.sumOf { it.amount }.coerceAtLeast(1L).toFloat()

                for (item in categories) {
                    val sweep = (item.amount / total) * 360f * progress.value
                    if (sweep > 0.5f) {
                        drawArc(
                            color = Color(item.colorHex),
                            startAngle = startAngle,
                            sweepAngle = sweep - 2f, // minor gap for aesthetics
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                        startAngle += sweep
                    }
                }
            }

            // Center Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "مجموع هزینه‌ها",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = IranianPhoneUtils.convertDigitsToPersian(
                        NumberFormat.getNumberInstance(Locale.US).format(totalAmount)
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "تومان",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legends Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(item.colorHex))
                                )
                                Text(
                                    text = item.categoryName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${IranianPhoneUtils.convertDigitsToPersian(item.percentage.toString())}٪",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(item.colorHex)
                            )
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Modern Bar Chart comparing expenses across months.
 */
@Composable
fun ModernMonthlyBarChart(
    bars: List<MonthlyExpenseBar>,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(bars) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val maxVal = (bars.maxOfOrNull { it.expenseAmount } ?: 1L).coerceAtLeast(1L).toFloat()
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val primaryColor = MaterialTheme.colorScheme.primary
    val barInactiveColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bar ->
                val ratio = (bar.expenseAmount / maxVal) * progress.value

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    // Value tooltip on top of current month bar
                    if (bar.isCurrentMonth) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primaryColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "${IranianPhoneUtils.convertDigitsToPersian((bar.expenseAmount / 1_000_000L).toString())} م",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // Bar
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .fillMaxHeight(fraction = ratio.coerceIn(0.12f, 1f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                if (bar.isCurrentMonth) {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            primaryColor,
                                            primaryColor.copy(alpha = 0.7f)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            barInactiveColor,
                                            barInactiveColor.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Month Name Label
                    Text(
                        text = bar.monthName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (bar.isCurrentMonth) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (bar.isCurrentMonth) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Modern Line Chart showing expenses trend with glowing gradient fill.
 */
@Composable
fun ModernTrendLineChart(
    points: List<ExpenseTrendPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val gridColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    val maxAmount = (points.maxOfOrNull { it.amount } ?: 1L).coerceAtLeast(1L).toFloat()
    val minAmount = (points.minOfOrNull { it.amount } ?: 0L).toFloat()
    val range = (maxAmount - minAmount).coerceAtLeast(1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val canvasHeight = size.height
                val paddingH = 32f
                val paddingV = 24f
                val chartWidth = width - (paddingH * 2)
                val chartHeight = canvasHeight - (paddingV * 2)

                // Grid lines
                for (i in 0..3) {
                    val y = paddingV + (chartHeight / 3) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingH, y),
                        end = Offset(width - paddingH, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (points.size >= 2) {
                    val stepX = chartWidth / (points.size - 1)
                    val coords = points.mapIndexed { index, point ->
                        val normalizedY = ((point.amount - minAmount) / range) * progress.value
                        val x = paddingH + (index * stepX)
                        val y = canvasHeight - paddingV - (normalizedY * chartHeight)
                        Offset(x, y)
                    }

                    // Draw Gradient Path underneath curve
                    val fillPath = Path().apply {
                        moveTo(coords.first().x, canvasHeight - paddingV)
                        coords.forEachIndexed { i, pt ->
                            if (i == 0) {
                                lineTo(pt.x, pt.y)
                            } else {
                                val prev = coords[i - 1]
                                val cx1 = prev.x + (pt.x - prev.x) / 2
                                val cy1 = prev.y
                                val cx2 = prev.x + (pt.x - prev.x) / 2
                                val cy2 = pt.y
                                cubicTo(cx1, cy1, cx2, cy2, pt.x, pt.y)
                            }
                        }
                        lineTo(coords.last().x, canvasHeight - paddingV)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.35f),
                                primaryColor.copy(alpha = 0.02f)
                            ),
                            startY = paddingV,
                            endY = canvasHeight - paddingV
                        )
                    )

                    // Draw smooth curve stroke
                    val strokePath = Path().apply {
                        coords.forEachIndexed { i, pt ->
                            if (i == 0) {
                                moveTo(pt.x, pt.y)
                            } else {
                                val prev = coords[i - 1]
                                val cx1 = prev.x + (pt.x - prev.x) / 2
                                val cy1 = prev.y
                                val cx2 = prev.x + (pt.x - prev.x) / 2
                                val cy2 = pt.y
                                cubicTo(cx1, cy1, cx2, cy2, pt.x, pt.y)
                            }
                        }
                    }

                    drawPath(
                        path = strokePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Data Points
                    coords.forEach { pt ->
                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 3.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }

        // Labels under trend points
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
