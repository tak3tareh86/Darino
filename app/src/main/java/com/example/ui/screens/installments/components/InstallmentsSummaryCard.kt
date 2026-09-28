package com.example.ui.screens.installments.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.TrendingDown
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.installments.model.InstallmentSummaryData
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.WarningAmberLight

@Composable
fun InstallmentsSummaryCard(
    summary: InstallmentSummaryData,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val cardGradient = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color(0xFF1E293B),
                Color(0xFF0F172A)
            )
        } else {
            listOf(
                Color(0xFFF8FAFC),
                Color(0xFFF1F5F9)
            )
        }
    )

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 3.dp,
        contentPadding = PaddingValues(16.dp),
        testTag = "installments_summary_card",
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Active Count Badge + Next Due Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimaryLight)
                    )
                    Text(
                        text = "اقساط فعال",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimaryLight.copy(alpha = if (isDark) 0.2f else 0.12f)
                    ) {
                        Text(
                            text = "${summary.activeCount} مورد",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = EmeraldPrimaryLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Next Due Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = WarningAmberLight.copy(alpha = if (isDark) 0.2f else 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccessTime,
                            contentDescription = null,
                            tint = WarningAmberLight,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "قسط بعدی: ${summary.nextDueText}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = WarningAmberLight
                        )
                    }
                }
            }

            // Divider Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            )

            // Bottom Metrics: Paid vs Remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Paid Metric
                SummaryMetricItem(
                    title = "مبلغ پرداخت‌شده",
                    amount = summary.paidAmountFormatted,
                    unit = "تومان",
                    color = EmeraldPrimaryLight,
                    icon = Icons.Rounded.CheckCircle,
                    modifier = Modifier.weight(1f)
                )

                // Divider
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .padding(horizontal = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 1.dp, height = 38.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )
                }

                // Remaining Metric
                SummaryMetricItem(
                    title = "مبلغ باقیمانده",
                    amount = summary.remainingAmountFormatted,
                    unit = "تومان",
                    color = Color(0xFF3B82F6),
                    icon = Icons.Rounded.Payments,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricItem(
    title: String,
    amount: String,
    unit: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
