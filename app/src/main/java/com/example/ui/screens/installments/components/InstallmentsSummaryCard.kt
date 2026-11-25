package com.example.ui.screens.installments.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.installments.model.InstallmentSummaryData
import com.example.ui.screens.settings.model.AppCurrency
import com.example.util.MoneyAmount
import com.example.util.MoneyFormatter
import com.example.util.AppNumberFormatter

private val FinanceTeal = Color(0xFF12DCC5)
private val FinanceMint = Color(0xFF25E0B0)
private val FinanceText = Color(0xFFF5FAFF)
private val FinanceMuted = Color(0xFFB9D0E5)

@Composable
fun InstallmentsSummaryCard(
    summary: InstallmentSummaryData,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val numberMode by AppNumberFormatter.modeFlow.collectAsState()
    val activeCurrency by MoneyFormatter.activeCurrencyFlow.collectAsState()
    val displayTotal = MoneyFormatter.formatExact(
        MoneyAmount.of(summary.totalRepaymentAmount, AppCurrency.RIAL),
        targetCurrency = activeCurrency
    )
    val displayPaid = MoneyFormatter.formatExact(
        MoneyAmount.of(summary.paidAmount, AppCurrency.RIAL),
        targetCurrency = activeCurrency
    )
    val displayRemaining = MoneyFormatter.formatExact(
        MoneyAmount.of(summary.remainingAmount, AppCurrency.RIAL),
        targetCurrency = activeCurrency
    )
    // Presentation-only ratio: all monetary values continue to come from the existing summary.
    val progress = if (summary.totalRepaymentAmount > 0L) {
        (summary.paidAmount.toDouble() / summary.totalRepaymentAmount.toDouble())
            .coerceIn(0.0, 1.0)
    } else {
        0.0
    }
    val progressPercent = (progress * 100.0).toInt()
    val progressText = AppNumberFormatter.formatDigits("$progressPercent٪", numberMode)
    val cardShape = RoundedCornerShape(26.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("installments_summary_card")
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .shadow(
                elevation = 9.dp,
                shape = cardShape,
                ambientColor = FinanceTeal.copy(alpha = 0.24f),
                spotColor = Color(0xFF020817).copy(alpha = 0.48f)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF4B91F5).copy(alpha = 0.88f),
                        Color(0xFF147D9B).copy(alpha = 0.62f),
                        FinanceTeal.copy(alpha = 0.95f)
                    )
                ),
                shape = cardShape
            ),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF071A38),
                            Color(0xFF062947),
                            Color(0xFF063B3C)
                        )
                    )
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                // Soft, flowing glass-like curves inspired by the supplied reference.
                val upperWave = Path().apply {
                    moveTo(w * 0.38f, 0f)
                    cubicTo(w * 0.55f, h * 0.08f, w * 0.64f, h * 0.18f, w * 0.79f, h * 0.08f)
                    cubicTo(w * 0.91f, h * 0.01f, w * 0.96f, h * 0.08f, w, h * 0.02f)
                    lineTo(w, h * 0.30f)
                    cubicTo(w * 0.82f, h * 0.37f, w * 0.68f, h * 0.25f, w * 0.54f, h * 0.32f)
                    cubicTo(w * 0.46f, h * 0.36f, w * 0.40f, h * 0.26f, w * 0.38f, 0f)
                    close()
                }
                drawPath(
                    path = upperWave,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF1A4D77).copy(alpha = 0.10f),
                            FinanceTeal.copy(alpha = 0.17f),
                            Color(0xFF16C9B2).copy(alpha = 0.035f)
                        )
                    )
                )

                val lowerWave = Path().apply {
                    moveTo(0f, h * 0.78f)
                    cubicTo(w * 0.18f, h * 0.67f, w * 0.30f, h * 0.94f, w * 0.48f, h * 0.86f)
                    cubicTo(w * 0.66f, h * 0.77f, w * 0.79f, h * 0.98f, w, h * 0.82f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = lowerWave,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1265B7).copy(alpha = 0.13f),
                            Color(0xFF12CDBB).copy(alpha = 0.11f)
                        )
                    )
                )

                val waveHighlight = Path().apply {
                    moveTo(w * 0.02f, h * 0.83f)
                    cubicTo(w * 0.22f, h * 0.72f, w * 0.31f, h * 0.96f, w * 0.49f, h * 0.88f)
                    cubicTo(w * 0.70f, h * 0.79f, w * 0.82f, h * 0.99f, w * 0.99f, h * 0.84f)
                }
                drawPath(
                    path = waveHighlight,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF2A83E8).copy(alpha = 0.16f),
                            FinanceTeal.copy(alpha = 0.30f)
                        )
                    ),
                    style = Stroke(width = 1.2.dp.toPx())
                )

                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF4C91F4).copy(alpha = 0.42f),
                            Color(0xFF1D829A).copy(alpha = 0.28f),
                            FinanceTeal.copy(alpha = 0.64f)
                        )
                    ),
                    style = Stroke(width = 1.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx())
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // In RTL layout the first child is placed at the right edge.
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = FinanceTeal.copy(alpha = 0.13f),
                        contentColor = FinanceTeal
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Wallet,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                                tint = FinanceTeal
                            )
                            Text(
                                text = "${AppNumberFormatter.formatDigits(summary.activeCount.toString(), numberMode)} مورد فعال",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                color = FinanceTeal
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF153D69).copy(alpha = 0.88f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Wallet,
                                contentDescription = null,
                                tint = FinanceTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "قسط بعدی",
                                color = FinanceText,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
                            )
                            Text(
                                text = AppNumberFormatter.formatDigits(summary.nextDueText, numberMode),
                                color = FinanceMuted.copy(alpha = 0.92f),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // RTL order: next due on the right, total in the center, progress ring on the left.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "مجموع اقساط فعال",
                            color = FinanceMuted,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp)
                        )
                        Text(
                            text = displayTotal,
                            color = FinanceText,
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    ProgressRing(
                        progress = progress.toFloat(),
                        progressText = progressText,
                        modifier = Modifier.size(76.dp)
                    )
                }

                // Bottom amount panels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // RTL: paid panel is first (right), remaining panel is second (left).
                    SummaryMetricPanel(
                        title = "مبلغ پرداخت‌شده",
                        amount = summary.displayPaid,
                        icon = Icons.Rounded.CheckCircle,
                        accent = FinanceMint,
                        background = Brush.linearGradient(
                            listOf(Color(0xFF087A68).copy(alpha = 0.94f), Color(0xFF064C4B).copy(alpha = 0.96f))
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricPanel(
                        title = "مبلغ باقی‌مانده",
                        amount = summary.displayRemaining,
                        icon = Icons.Rounded.Payments,
                        accent = Color(0xFF75A9FF),
                        background = Brush.linearGradient(
                            listOf(Color(0xFF123E87).copy(alpha = 0.94f), Color(0xFF092B60).copy(alpha = 0.96f))
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    progressText: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            drawArc(
                color = Color(0xFF2C5A7C).copy(alpha = 0.72f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color(0xFF19B8D1), Color(0xFF20E5A7), Color(0xFF19B8D1))
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = progressText,
                color = FinanceText,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp)
            )
            Text(
                text = "پرداخت‌شده",
                color = FinanceMuted,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
            )
        }
    }
}

@Composable
private fun SummaryMetricPanel(
    title: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    background: Brush,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(23.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.70f)),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .background(background)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = title,
                    color = FinanceMuted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = amount,
                    color = FinanceText,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
