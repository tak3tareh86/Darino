package com.example.vehicle.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils
import com.example.vehicle.domain.VehicleStatistics
import com.example.vehicle.data.VehicleExpenseEntity
import com.example.vehicle.data.VehicleExpenseCategory
import com.example.util.PersianCalendarHelper
import com.example.vehicle.presentation.components.VehicleCostChart
import com.example.vehicle.presentation.components.VehicleExpenseInsightsSection
import com.example.util.MoneyFormatter
import java.text.NumberFormat
import java.util.Locale

/**
 * Screen 12 & 13: Vehicle Financial Report & Cost Charts (گزارش و نمودار هزینه خودرو)
 */
@Composable
fun VehicleReportTab(
    statistics: VehicleStatistics?,
    expenses: List<VehicleExpenseEntity>,
    modifier: Modifier = Modifier
) {
    if (statistics == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val monthCostFormatted = MoneyFormatter.formatToman(statistics.currentMonthCost)
    val yearCostFormatted = MoneyFormatter.formatToman(statistics.yearlyCost)
    val topCostAmountFormatted = MoneyFormatter.formatToman(statistics.topCostAmount)
    val today = remember { PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()) }
    val currentYear = today.year
    val currentMonth = today.month
    val shortMonthNames = listOf("فرو", "ارد", "خرد", "تیر", "مرد", "شهر", "مهر", "آبا", "آذر", "دی", "بهم", "اسف")
    val parsedExpenses = remember(expenses) {
        expenses.mapNotNull { expense ->
            val parts = IranianPhoneUtils.convertDigitsToEnglish(expense.date).trim().split("/")
            val year = parts.getOrNull(0)?.toIntOrNull()
            val month = parts.getOrNull(1)?.toIntOrNull()
            if (year != null && month != null && year in 1300..1500 && month in 1..12) Triple(expense, year, month) else null
        }
    }
    val availableYears = remember(parsedExpenses, currentYear) {
        (parsedExpenses.map { it.second } + currentYear).distinct().filter { it in 1300..1500 }.sorted()
    }
    var selectedChartYear by rememberSaveable { mutableIntStateOf(currentYear) }
    val chartYear = selectedChartYear.takeIf { it in availableYears } ?: currentYear
    val annualChartBars = remember(parsedExpenses, chartYear, currentYear, currentMonth) {
        (1..12).map { month ->
            val items = parsedExpenses.filter { it.second == chartYear && it.third == month }.map { it.first }
            val fuel = items.filter { it.category == VehicleExpenseCategory.FUEL }.sumOf { it.amount }
            val repair = items.filter { it.category == VehicleExpenseCategory.REPAIRS || it.category == VehicleExpenseCategory.PARTS }.sumOf { it.amount }
            val service = items.filter { it.category == VehicleExpenseCategory.SERVICE }.sumOf { it.amount }
            val insurance = items.filter { it.category == VehicleExpenseCategory.INSURANCE }.sumOf { it.amount }
            val other = items.filter { it.category !in setOf(VehicleExpenseCategory.FUEL, VehicleExpenseCategory.REPAIRS, VehicleExpenseCategory.PARTS, VehicleExpenseCategory.SERVICE, VehicleExpenseCategory.INSURANCE) }.sumOf { it.amount }
            com.example.vehicle.domain.VehicleMonthlyExpenseBar(
                monthName = shortMonthNames[month - 1], fuelAmount = fuel, repairAmount = repair,
                serviceAmount = service, insuranceAmount = insurance, totalAmount = items.sumOf { it.amount },
                isCurrentMonth = chartYear == currentYear && month == currentMonth, otherAmount = other
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("vehicle_report_tab"),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. KPI Summary Cards Grid
        item {
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "خلاصه شاخص‌های مالی خودرو",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "هزینه این ماه",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = monthCostFormatted,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color(0xFF3B82F6)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "هزینه امسال",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = yearCostFormatted,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "تعداد سرویس‌ها",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${IranianPhoneUtils.convertDigitsToPersian(statistics.totalServicesCount.toString())} سرویس انجام شده",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "بیشترین هزینه",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${statistics.topCostCategoryTitle} ($topCostAmountFormatted)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    // Last Repair Info
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "آخرین تعمیر ثبت شده: ${statistics.lastRepairTitle}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = IranianPhoneUtils.convertDigitsToPersian(statistics.lastRepairDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Stacked Monthly Cost Chart
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
                                contentDescription = "نمودار ماهانه هزینه خودرو",
                                size = 30.dp,
                                accentColor = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "نمودار ماهانه هزینه خودرو",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        var yearMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            Surface(
                                onClick = { yearMenuExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                            ) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text(IranianPhoneUtils.convertDigitsToPersian(chartYear.toString()), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Icon(Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(expanded = yearMenuExpanded, onDismissRequest = { yearMenuExpanded = false }) {
                                availableYears.asReversed().forEach { year ->
                                    DropdownMenuItem(text = { Text(IranianPhoneUtils.convertDigitsToPersian(year.toString())) }, onClick = { selectedChartYear = year; yearMenuExpanded = false })
                                }
                            }
                        }
                    }

                    Text("هزینه‌های ثبت‌شده در ۱۲ ماه سال ${IranianPhoneUtils.convertDigitsToPersian(chartYear.toString())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    VehicleCostChart(bars = annualChartBars)
                }
            }
        }

        // 3. Real database-backed expense comparison and annual trends
        item {
            VehicleExpenseInsightsSection(expenses = expenses)
        }

        // 4. Category Breakdown List
        item {
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "سهم هزینه‌ها به تفکیک دسته‌بندی",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    statistics.categoryBreakdown.forEach { item ->
                        val amountStr = MoneyFormatter.formatToman(item.amount, includeUnit = false)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Soft3DIcon(
                                    imageRes = item.category.iconRes,
                                    contentDescription = item.category.title,
                                    size = 24.dp,
                                    accentColor = Color(item.category.colorHex)
                                )
                                Text(
                                    text = item.category.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = amountStr,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(item.category.colorHex).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${IranianPhoneUtils.convertDigitsToPersian(item.percentage.toString())}٪",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = Color(item.category.colorHex),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
