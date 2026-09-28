package com.example.reports.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
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
import com.example.reports.domain.InstallmentReportItem
import com.example.reports.domain.InstallmentStatus
import com.example.reports.domain.InstallmentSummary
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils
import java.text.NumberFormat
import java.util.Locale

/**
 * Screen 2: Installment Report (گزارش اقساط)
 */
@Composable
fun InstallmentReportScreen(
    installmentSummary: InstallmentSummary,
    modifier: Modifier = Modifier
) {
    val totalMonthlyFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(installmentSummary.totalMonthlyPayment)
    )
    val totalDebtFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(installmentSummary.totalRemainingDebt)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("installment_report_screen"),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Key Metrics 5-Pillar Grid
        item {
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                imageRes = R.drawable.img_3d_installment,
                                contentDescription = "وضعیت تعهدات و اقساط",
                                size = 32.dp,
                                accentColor = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "وضعیت تعهدات و اقساط",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${IranianPhoneUtils.convertDigitsToPersian(installmentSummary.activeCount.toString())} قسط فعال",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // Row 1: Monthly Payment & Total Debt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCardItem(
                            title = "مجموع پرداخت ماهانه",
                            value = "$totalMonthlyFormatted تومان",
                            accentColor = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCardItem(
                            title = "کل بدهی باقیمانده",
                            value = "$totalDebtFormatted تومان",
                            accentColor = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Nearest Due Date & Largest Installment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCardItem(
                            title = "نزدیک‌ترین سررسید",
                            value = "${IranianPhoneUtils.convertDigitsToPersian(installmentSummary.nearestDueDateDays.toString())} روز دیگر (${installmentSummary.nearestDueDateTitle})",
                            accentColor = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCardItem(
                            title = "بیشترین قسط ماهانه",
                            value = "${installmentSummary.largestInstallmentTitle} (${IranianPhoneUtils.convertDigitsToPersian(NumberFormat.getNumberInstance(Locale.US).format(installmentSummary.largestInstallmentAmount))} تومان)",
                            accentColor = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Status Breakdown Progress Card (Paid, Remaining, Overdue)
        item {
            InstallmentStatusBreakdownCard(summary = installmentSummary)
        }

        // 3. Header for Installment Items
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "لیست اقساط و تسهیلات جاری",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${IranianPhoneUtils.convertDigitsToPersian(installmentSummary.items.size.toString())} مورد",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4. Installment Item List
        items(installmentSummary.items, key = { it.id }) { item ->
            InstallmentDetailCard(item = item)
        }
    }
}

@Composable
private fun MetricCardItem(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = accentColor,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Visual Progress Bar for Paid / Remaining / Overdue Installments
 */
@Composable
private fun InstallmentStatusBreakdownCard(
    summary: InstallmentSummary
) {
    val totalCount = (summary.paidCount + summary.remainingCount + summary.overdueCount).coerceAtLeast(1)
    val paidFraction = summary.paidCount.toFloat() / totalCount
    val remainingFraction = summary.remainingCount.toFloat() / totalCount
    val overdueFraction = summary.overdueCount.toFloat() / totalCount

    Layered3DCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "نمودار وضعیت بازپرداخت کل اقساط",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Segmented Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                if (paidFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(paidFraction)
                            .fillMaxHeight()
                            .background(Color(0xFF10B981))
                    )
                }
                if (remainingFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(remainingFraction)
                            .fillMaxHeight()
                            .background(Color(0xFF3B82F6))
                    )
                }
                if (overdueFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(overdueFraction)
                            .fillMaxHeight()
                            .background(Color(0xFFEF4444))
                    )
                }
            }

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusLegend(
                    title = "پرداخت شده",
                    count = summary.paidCount,
                    color = Color(0xFF10B981)
                )
                StatusLegend(
                    title = "باقیمانده",
                    count = summary.remainingCount,
                    color = Color(0xFF3B82F6)
                )
                StatusLegend(
                    title = "عقب افتاده",
                    count = summary.overdueCount,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
private fun StatusLegend(
    title: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$title (${IranianPhoneUtils.convertDigitsToPersian(count.toString())} قسط)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InstallmentDetailCard(
    item: InstallmentReportItem
) {
    val monthlyFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(item.monthlyAmount)
    )
    val remainingDebtFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(item.remainingAmount)
    )
    val progressFraction = (item.paidMonths.toFloat() / item.totalMonths.toFloat()).coerceIn(0f, 1f)

    Layered3DCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        imageRes = R.drawable.img_3d_card,
                        contentDescription = item.title,
                        size = 30.dp,
                        accentColor = if (item.status == InstallmentStatus.OVERDUE) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.bankOrOrg,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Tag
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when (item.status) {
                        InstallmentStatus.ACTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        InstallmentStatus.OVERDUE -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        InstallmentStatus.PAID_THIS_MONTH -> Color(0xFF10B981).copy(alpha = 0.12f)
                    }
                ) {
                    Text(
                        text = item.status.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when (item.status) {
                            InstallmentStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                            InstallmentStatus.OVERDUE -> Color(0xFFEF4444)
                            InstallmentStatus.PAID_THIS_MONTH -> Color(0xFF10B981)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Monthly vs Remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "قسط ماهانه: $monthlyFormatted تومان",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "مانده: $remainingDebtFormatted تومان",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (item.status == InstallmentStatus.OVERDUE) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "پرداخت شده: ${IranianPhoneUtils.convertDigitsToPersian(item.paidMonths.toString())} از ${IranianPhoneUtils.convertDigitsToPersian(item.totalMonths.toString())} ماه",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "سررسید: ${IranianPhoneUtils.convertDigitsToPersian(item.nextDueDateDays.toString())} روز دیگر",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = if (item.nextDueDateDays <= 3) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
