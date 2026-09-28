package com.example.reports.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.reports.domain.ReportSummary
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils
import java.text.NumberFormat
import java.util.Locale

/**
 * Premium 3D Financial Summary Card displayed at the top of Reports Dashboard.
 */
@Composable
fun ReportSummaryHeaderCard(
    summary: ReportSummary,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val incomeFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(summary.totalIncome)
    )
    val expenseFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(summary.totalExpense)
    )
    val savingFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(summary.totalSaving)
    )

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reports_summary_card"),
        elevation = 6.dp,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        backgroundColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Period Title + Saving Rate Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Soft3DIcon(
                        imageRes = R.drawable.img_3d_analytics,
                        contentDescription = "خلاصه وضعیت مالی",
                        size = 34.dp,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "خلاصه وضعیت مالی",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = summary.periodLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Saving rate pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "پس‌انداز: ${IranianPhoneUtils.convertDigitsToPersian(summary.savingRatePercent.toString())}٪",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            // 3 Main Metric Pillars (Income, Expense, Saving)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Income
                MetricPillar(
                    title = "مجموع درآمد",
                    amount = incomeFormatted,
                    unit = "تومان",
                    accentColor = Color(0xFF10B981),
                    icon = Icons.Rounded.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )

                // Expense
                MetricPillar(
                    title = "مجموع هزینه",
                    amount = expenseFormatted,
                    unit = "تومان",
                    accentColor = Color(0xFFEF4444),
                    icon = Icons.Rounded.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )

                // Saving
                MetricPillar(
                    title = "میزان پس‌انداز",
                    amount = savingFormatted,
                    unit = "تومان",
                    accentColor = Color(0xFF3B82F6),
                    icon = null,
                    modifier = Modifier.weight(1f)
                )
            }

            // Progress bar showing expenses vs income ratio
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "نسبت مصارف به درآمد کل",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val expenseRatio = if (summary.totalIncome > 0) {
                        ((summary.totalExpense.toDouble() / summary.totalIncome) * 100).toInt()
                    } else 0
                    Text(
                        text = "${IranianPhoneUtils.convertDigitsToPersian(expenseRatio.toString())}٪",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (expenseRatio > 80) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                    )
                }

                val progressFraction = if (summary.totalIncome > 0) {
                    (summary.totalExpense.toFloat() / summary.totalIncome).coerceIn(0f, 1f)
                } else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6),
                                        if (progressFraction > 0.8f) Color(0xFFEF4444) else Color(0xFF10B981)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPillar(
    title: String,
    amount: String,
    unit: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = accentColor
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
