package com.example.reports.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.reports.domain.CategoryExpenseItem
import com.example.reports.domain.ReportPeriod
import com.example.reports.domain.ReportSummary
import com.example.reports.presentation.components.ModernDonutChart
import com.example.reports.presentation.components.ModernMonthlyBarChart
import com.example.reports.presentation.components.ModernTrendLineChart
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils
import java.text.NumberFormat
import java.util.Locale

/**
 * Screen 1: Complete Financial Report (گزارش مالی)
 */
@Composable
fun FinancialReportScreen(
    summary: ReportSummary,
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("financial_report_screen"),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Period Selector Tabs
        item {
            PeriodSelectorRow(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = onPeriodSelected
            )
        }

        // 2. Month-over-Month Comparison Card
        item {
            MonthOverMonthComparisonCard(summary = summary)
        }

        // 3. Top Expense Categories Analysis Card
        item {
            TopExpenseCategoriesCard(
                topCategory = summary.topExpenseCategory,
                secondCategory = summary.secondExpenseCategory
            )
        }

        // 4. Donut Chart - Expense Category Breakdown
        item {
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 3.dp
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
                                imageRes = R.drawable.img_3d_shopping,
                                contentDescription = "سهم دسته‌بندی هزینه‌ها",
                                size = 32.dp,
                                accentColor = Color(0xFF3B82F6)
                            )
                            Text(
                                text = "سهم دسته‌بندی هزینه‌ها",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "نمودار تفکیکی",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ModernDonutChart(
                        categories = summary.categoryExpenses,
                        totalAmount = summary.totalExpense
                    )
                }
            }
        }

        // 5. Bar Chart - Monthly Comparison
        item {
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 3.dp
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
                                imageRes = R.drawable.img_3d_chart,
                                contentDescription = "مقایسه هزینه‌های ماه‌ها",
                                size = 32.dp,
                                accentColor = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "مقایسه هزینه‌های ماه‌ها",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "۶ ماه اخیر",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ModernMonthlyBarChart(
                        bars = summary.monthlyBars
                    )
                }
            }
        }

        // 6. Line Chart - Expense Trend Curve
        item {
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 3.dp
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
                                imageRes = R.drawable.img_3d_analytics,
                                contentDescription = "روند تغییرات هزینه",
                                size = 32.dp,
                                accentColor = Color(0xFF10B981)
                            )
                            Text(
                                text = "روند تغییرات هزینه",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "هفته به هفته",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ModernTrendLineChart(
                        points = summary.trendPoints
                    )
                }
            }
        }
    }
}

/**
 * Period Selector Tab Chips
 */
@Composable
private fun PeriodSelectorRow(
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ReportPeriod.values().forEach { period ->
                val isSelected = selectedPeriod == period
                val primaryColor = MaterialTheme.colorScheme.primary

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPeriodSelected(period) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) primaryColor else Color.Transparent,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Month over Month Comparison Card (Section 6)
 */
@Composable
private fun MonthOverMonthComparisonCard(
    summary: ReportSummary
) {
    val prevExpenseFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(summary.previousMonthExpense)
    )
    val currentExpenseFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(summary.totalExpense)
    )
    val changePercentFormatted = IranianPhoneUtils.convertDigitsToPersian(
        summary.expenseChangePercent.toString()
    )

    Layered3DCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 3.dp,
        borderColor = if (summary.isExpenseIncreased) Color(0xFFEF4444).copy(alpha = 0.3f) else Color(0xFF10B981).copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageRes = R.drawable.img_3d_wallet,
                        contentDescription = "مقایسه با ماه قبل",
                        size = 32.dp,
                        accentColor = if (summary.isExpenseIncreased) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                    Text(
                        text = "مقایسه با ماه قبل",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Change pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (summary.isExpenseIncreased) Color(0xFFEF4444).copy(alpha = 0.12f) else Color(0xFF10B981).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (summary.isExpenseIncreased) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = if (summary.isExpenseIncreased) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "$changePercentFormatted٪ ${if (summary.isExpenseIncreased) "افزایش" else "کاهش"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (summary.isExpenseIncreased) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "هزینه ماه قبل",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$prevExpenseFormatted تومان",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "هزینه ماه جاری",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currentExpenseFormatted تومان",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (summary.isExpenseIncreased) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Top Expenses Category Highlights Card (Section 5)
 */
@Composable
private fun TopExpenseCategoriesCard(
    topCategory: CategoryExpenseItem,
    secondCategory: CategoryExpenseItem
) {
    val topAmountFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(topCategory.amount)
    )
    val secondAmountFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(secondCategory.amount)
    )

    Layered3DCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Soft3DIcon(
                    imageRes = R.drawable.img_3d_calculator,
                    contentDescription = "تحلیل دسته‌بندی هزینه‌ها",
                    size = 32.dp,
                    accentColor = Color(0xFFF59E0B)
                )
                Text(
                    text = "تحلیل دسته‌بندی هزینه‌ها",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Top Item Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(topCategory.colorHex).copy(alpha = 0.08f))
                    .border(1.dp, Color(topCategory.colorHex).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Soft3DIcon(
                        imageRes = topCategory.iconRes,
                        contentDescription = topCategory.categoryName,
                        size = 32.dp,
                        accentColor = Color(topCategory.colorHex)
                    )
                    Column {
                        Text(
                            text = "بیشترین هزینه این ماه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = topCategory.categoryName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$topAmountFormatted تومان",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(topCategory.colorHex)
                        )
                    )
                    Text(
                        text = "${IranianPhoneUtils.convertDigitsToPersian(topCategory.percentage.toString())}٪ کل مخارج",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Second Item Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Soft3DIcon(
                        imageRes = secondCategory.iconRes,
                        contentDescription = secondCategory.categoryName,
                        size = 32.dp,
                        accentColor = Color(secondCategory.colorHex)
                    )
                    Column {
                        Text(
                            text = "دومین هزینه عمده",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = secondCategory.categoryName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$secondAmountFormatted تومان",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(secondCategory.colorHex)
                        )
                    )
                    Text(
                        text = "${IranianPhoneUtils.convertDigitsToPersian(secondCategory.percentage.toString())}٪ کل مخارج",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
