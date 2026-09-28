package com.example.ui.screens.reports.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.reports.model.TrendPoint
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import kotlin.math.max

@Composable
fun IncomeExpenseTrendChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reports_trend_chart_card"),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 3.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Title & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ShowChart,
                            contentDescription = null,
                            tint = EmeraldPrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "روند درآمد و هزینه",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "مقایسه عملکرد مالی در طول زمان",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LegendItem(title = "درآمد", color = EmeraldPrimaryLight)
                    LegendItem(title = "هزینه", color = ExpenseRoseLight)
                }
            }

            // Interactive Tooltip if a point is selected
            AnimatedVisibility(
                visible = selectedPointIndex != null && selectedPointIndex in points.indices,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                val point = points[selectedPointIndex ?: 0]
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(RadiusMD)),
                    shape = RoundedCornerShape(RadiusMD),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بازه: ${point.label}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "درآمد: ${point.incomeFormatted}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldPrimaryLight
                            )
                            Text(
                                text = "هزینه: ${point.expenseFormatted}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ExpenseRoseLight
                            )
                            Text(
                                text = "مانده: ${point.balanceFormatted}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Canvas Chart
            if (points.isNotEmpty()) {
                val maxVal = points.maxOf { max(it.incomeAmount, it.expenseAmount) }.toFloat().coerceAtLeast(1f)
                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.25f)
                val textColor = MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .pointerInput(points) {
                                detectTapGestures { offset ->
                                    val count = points.size
                                    if (count > 1) {
                                        val stepX = size.width / (count - 1)
                                        val clickedIndex = ((offset.x + stepX / 2) / stepX).toInt().coerceIn(0, count - 1)
                                        selectedPointIndex = if (selectedPointIndex == clickedIndex) null else clickedIndex
                                    }
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height - 30.dp.toPx()
                        val bottomY = height
                        val stepX = if (points.size > 1) width / (points.size - 1) else width

                        // Draw Grid Lines (3 horizontal lines)
                        val gridLines = 3
                        for (i in 0..gridLines) {
                            val y = height * (i.toFloat() / gridLines)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        // Build Paths for Income & Expense
                        val incomePath = Path()
                        val incomeAreaPath = Path()
                        val expensePath = Path()
                        val expenseAreaPath = Path()

                        val incomePoints = mutableListOf<Offset>()
                        val expensePoints = mutableListOf<Offset>()

                        points.forEachIndexed { index, point ->
                            val x = index * stepX
                            val incomeY = bottomY - ((point.incomeAmount.toFloat() / maxVal) * (height * 0.85f))
                            val expenseY = bottomY - ((point.expenseAmount.toFloat() / maxVal) * (height * 0.85f))

                            val incPt = Offset(x, incomeY)
                            val expPt = Offset(x, expenseY)
                            incomePoints.add(incPt)
                            expensePoints.add(expPt)

                            if (index == 0) {
                                incomePath.moveTo(x, incomeY)
                                incomeAreaPath.moveTo(x, bottomY)
                                incomeAreaPath.lineTo(x, incomeY)

                                expensePath.moveTo(x, expenseY)
                                expenseAreaPath.moveTo(x, bottomY)
                                expenseAreaPath.lineTo(x, expenseY)
                            } else {
                                val prevInc = incomePoints[index - 1]
                                val prevExp = expensePoints[index - 1]
                                val ctrlX1 = prevInc.x + (x - prevInc.x) / 2
                                val ctrlX2 = prevExp.x + (x - prevExp.x) / 2

                                incomePath.cubicTo(ctrlX1, prevInc.y, ctrlX1, incomeY, x, incomeY)
                                incomeAreaPath.cubicTo(ctrlX1, prevInc.y, ctrlX1, incomeY, x, incomeY)

                                expensePath.cubicTo(ctrlX2, prevExp.y, ctrlX2, expenseY, x, expenseY)
                                expenseAreaPath.cubicTo(ctrlX2, prevExp.y, ctrlX2, expenseY, x, expenseY)
                            }
                        }

                        // Close area paths
                        incomeAreaPath.lineTo(width, bottomY)
                        incomeAreaPath.close()

                        expenseAreaPath.lineTo(width, bottomY)
                        expenseAreaPath.close()

                        // Draw Gradient Areas
                        drawPath(
                            path = incomeAreaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    EmeraldPrimaryLight.copy(alpha = if (isDark) 0.25f else 0.15f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = bottomY
                            )
                        )

                        drawPath(
                            path = expenseAreaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    ExpenseRoseLight.copy(alpha = if (isDark) 0.22f else 0.12f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = bottomY
                            )
                        )

                        // Draw Main Trend Curves
                        drawPath(
                            path = incomePath,
                            color = EmeraldPrimaryLight,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        drawPath(
                            path = expensePath,
                            color = ExpenseRoseLight,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Draw Dots & Selected Indicator
                        incomePoints.forEachIndexed { index, pt ->
                            val isSelected = selectedPointIndex == index
                            drawCircle(
                                color = if (isSelected) Color.White else EmeraldPrimaryLight,
                                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                center = pt
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = EmeraldPrimaryLight,
                                    radius = 6.dp.toPx(),
                                    center = pt,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )
                            }
                        }

                        expensePoints.forEachIndexed { index, pt ->
                            val isSelected = selectedPointIndex == index
                            drawCircle(
                                color = if (isSelected) Color.White else ExpenseRoseLight,
                                radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                                center = pt
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = ExpenseRoseLight,
                                    radius = 5.dp.toPx(),
                                    center = pt,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }

                    // Bottom Label Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        points.forEachIndexed { index, point ->
                            val isSelected = selectedPointIndex == index
                            Text(
                                text = point.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                color = if (isSelected) EmeraldPrimaryLight else textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    title: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
