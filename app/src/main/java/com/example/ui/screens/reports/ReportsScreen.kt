package com.example.ui.screens.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * دسته‌بندی‌های گزارشات
 */
enum class ReportCategory(val title: String) {
    FINANCIAL("گزارش مالی"),
    INSTALLMENTS("گزارش اقساط"),
    VEHICLE("گزارش خودرو"),
    SMART_ANALYSIS("تحلیل هوشمند")
}

/**
 * فیلترهای بازه زمانی
 */
enum class ReportPeriod(val title: String) {
    WEEK("هفته"),
    MONTH("ماه"),
    YEAR("سال"),
    CUSTOM("بازه دلخواه")
}

/**
 * مدل تاریخ شمسی
 */
data class ShamsiDate(
    val year: Int = 1400,
    val month: Int = 1,
    val day: Int = 1
) {
    companion object {
        fun now(): ShamsiDate {
            val n = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
            return ShamsiDate(n.year, n.month, n.day)
        }
    }
    fun formatDisplay(): String {
        val m = if (month < 10) "0$month" else "$month"
        val d = if (day < 10) "0$day" else "$day"
        return "$year/$m/$d"
    }

    fun monthName(): String {
        return when (month) {
            1 -> "فروردین"
            2 -> "اردیبهشت"
            3 -> "خرداد"
            4 -> "تیر"
            5 -> "مرداد"
            6 -> "شهریور"
            7 -> "مهر"
            8 -> "آبان"
            9 -> "آذر"
            10 -> "دی"
            11 -> "بهمن"
            12 -> "اسفند"
            else -> "ماه $month"
        }
    }
}

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.reports.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToFinancial: () -> Unit = {},
    onNavigateToInstallments: () -> Unit = {},
    onNavigateToVehicles: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // وضعیت بازه تاریخی دلخواه
    val startDate = uiState.customStartDate ?: remember {
        val now = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        ShamsiDate(now.year, now.month, 1)
    }
    val endDate = uiState.customEndDate ?: remember {
        val now = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        ShamsiDate(now.year, now.month, PersianCalendarHelper.getDaysInMonth(now.year, now.month))
    }
    
    var showCustomDateSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            bottomBar = bottomBar,
            contentWindowInsets = WindowInsets(0),
            containerColor = Color(0xFF090E17),
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // ۱. نوار بالای صفحه با دکمه خروجی (فریز شده)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    ReportsHeader(
                        onExportClick = { showExportSheet = true },
                        onFilterClick = { showCustomDateSheet = true }
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ۲. کارت خلاصه وضعیت مالی
                    item {
                        FinancialOverviewCard(
                            selectedPeriod = uiState.selectedPeriod,
                            startDate = startDate,
                            endDate = endDate,
                            totalIncome = uiState.totalIncome,
                            totalExpense = uiState.totalExpense,
                            savings = uiState.savings,
                            savingsPercent = uiState.savingsPercent,
                            onEditCustomRange = { showCustomDateSheet = true },
                            selectedCategory = uiState.selectedCategory
                        )
                    }

                    // ۳. تب‌های دسته‌بندی گزارشات
                    item {
                        CategoryFilterRow(
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = { viewModel.setCategory(it) }
                        )
                    }

                    // ۴. تب‌های بازه زمانی
                    item {
                        PeriodFilterRow(
                            selectedPeriod = uiState.selectedPeriod,
                            onPeriodSelected = { period ->
                                if (period == ReportPeriod.CUSTOM) {
                                    showCustomDateSheet = true
                                } else {
                                    viewModel.setPeriod(period)
                                }
                            }
                        )
                    }

                    // ۵. نمایش نوار بازه انتخابی در صورت انتخاب بازه دلخواه
                    if (uiState.selectedPeriod == ReportPeriod.CUSTOM) {
                        item {
                            ActiveCustomRangeBanner(
                                startDate = startDate,
                                endDate = endDate,
                                onClick = { showCustomDateSheet = true }
                            )
                        }
                    }

                    // ۶. کارت مقایسه با دوره قبل
                    item {
                        MonthComparisonCard(
                            currentValue = uiState.currentPeriodExpense,
                            previousValue = uiState.previousPeriodExpense,
                            percent = uiState.expenseChangePercent,
                            isIncrease = uiState.isExpenseIncreased,
                            label = when(uiState.selectedPeriod) {
                                ReportPeriod.WEEK -> "مقایسه با هفته قبل"
                                ReportPeriod.MONTH -> "مقایسه با ماه قبل"
                                ReportPeriod.YEAR -> "مقایسه با سال قبل"
                                ReportPeriod.CUSTOM -> "مقایسه با دوره معادل قبل"
                            }
                        )
                    }

                    // ۷. محتوای اختصاصی هر دسته
                    when (uiState.selectedCategory) {
                        ReportCategory.FINANCIAL, ReportCategory.SMART_ANALYSIS -> {
                            item {
                                ExpenseCategoryCard(
                                    totalExpense = uiState.totalExpense,
                                    topCategory = uiState.topExpenseCategory,
                                    topCategoryAmount = uiState.topExpenseCategoryAmount
                                )
                            }
                        }
                        ReportCategory.INSTALLMENTS -> {
                            item {
                                InstallmentSummaryCard(
                                    totalAmount = uiState.totalInstallmentAmount,
                                    paidAmount = uiState.paidInstallmentAmount,
                                    activeCount = uiState.activeInstallmentsCount
                                )
                            }
                        }
                        ReportCategory.VEHICLE -> {
                            item {
                                VehicleExpenseCard(
                                    totalVehicleExpense = uiState.vehicleExpenseTotal
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
        }
    }

        // باتم‌شیت انتخاب تاریخ شمسی دلخواه
        if (showCustomDateSheet) {
            CustomDateRangeBottomSheet(
                initialStartDate = startDate,
                initialEndDate = endDate,
                onDismiss = { showCustomDateSheet = false },
                onApply = { start, end ->
                    viewModel.setCustomRange(start, end)
                    showCustomDateSheet = false
                }
            )
        }

        // باتم‌شیت خروجی و اشتراک‌گذاری گزارش (PDF واقعی و ذخیره در گالری)
        if (showExportSheet) {
            ExportReportBottomSheet(
                startDate = startDate,
                endDate = endDate,
                onDismiss = { showExportSheet = false },
                onExportPdf = {
                    ReportExportUtils.exportReportAsPdf(
                        context = context,
                        startDate = startDate,
                        endDate = endDate,
                        totalIncome = uiState.totalIncome,
                        totalExpense = uiState.totalExpense,
                        savings = uiState.savings,
                        savingsPercent = "${uiState.savingsPercent}٪",
                        topExpenseCategory = uiState.topExpenseCategory,
                        expenseRatio = "${(uiState.expenseRatio * 100).toInt()}٪",
                        previousPeriodExpense = uiState.previousPeriodExpense,
                        expenseChangePercent = String.format("%.1f٪", uiState.expenseChangePercent),
                        isExpenseIncreased = uiState.isExpenseIncreased
                    )
                    showExportSheet = false
                },
                onSaveImage = {
                    ReportExportUtils.saveReportImageToGallery(
                        context = context,
                        startDate = startDate,
                        endDate = endDate,
                        totalIncome = uiState.totalIncome,
                        totalExpense = uiState.totalExpense,
                        savings = uiState.savings,
                        savingsPercent = "${uiState.savingsPercent}٪",
                        topExpenseCategory = uiState.topExpenseCategory,
                        topCategoryAmount = MoneyFormatter.formatToman(uiState.topExpenseCategoryAmount),
                        expenseRatio = "${(uiState.expenseRatio * 100).toInt()}٪",
                        previousPeriodExpense = uiState.previousPeriodExpense,
                        expenseChangePercent = String.format("%.1f٪", uiState.expenseChangePercent),
                        isExpenseIncreased = uiState.isExpenseIncreased
                    )
                    showExportSheet = false
                }
            )
        }
    }
}

@Composable
private fun ReportsHeader(
    onExportClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // دکمه خروجی (اشتراک‌گذاری گزارش)
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onExportClick() },
                shape = CircleShape,
                color = Color(0xFF131D33),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.FileUpload,
                        contentDescription = "خروجی و اشتراک‌گذاری",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color(0xFF131D33),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "فیلترها",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "مرکز تحلیل مالی دارینو",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "گزارشات هوشمند، بودجه و تعهدات",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = Color(0xFF94A3B8)
                )
            }

            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color(0xFF0F766E).copy(alpha = 0.25f),
                border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.PieChart,
                        contentDescription = null,
                        tint = Color(0xFF2DD4BF),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialOverviewCard(
    selectedPeriod: ReportPeriod,
    startDate: ShamsiDate,
    endDate: ShamsiDate,
    totalIncome: Long,
    totalExpense: Long,
    savings: Long,
    savingsPercent: Int,
    onEditCustomRange: () -> Unit,
    selectedCategory: ReportCategory
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF10192D),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedCategory == ReportCategory.FINANCIAL) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF064E3B).copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "پس‌انداز: $savingsPercent٪",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = Color(0xFF34D399),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = selectedCategory.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "خلاصه وضعیت مالی",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                        val periodLabel = if (selectedPeriod == ReportPeriod.CUSTOM) {
                            "بازه: ${startDate.formatDisplay()} تا ${endDate.formatDisplay()}"
                        } else {
                            "بازه انتخابی (${selectedPeriod.title})"
                        }
                        Text(
                            text = periodLabel,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF94A3B8),
                            modifier = if (selectedPeriod == ReportPeriod.CUSTOM) {
                                Modifier.clickable { onEditCustomRange() }
                            } else Modifier
                        )
                    }

                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.MonetizationOn,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricBox(
                    modifier = Modifier.weight(1f),
                    title = "مجموع درآمد",
                    amount = MoneyFormatter.formatToman(totalIncome),
                    currency = "تومان",
                    accentColor = Color(0xFF10B981),
                    icon = Icons.Rounded.ArrowDownward
                )
                MetricBox(
                    modifier = Modifier.weight(1f),
                    title = "مجموع هزینه",
                    amount = MoneyFormatter.formatToman(totalExpense),
                    currency = "تومان",
                    accentColor = Color(0xFFEF4444),
                    icon = Icons.Rounded.ArrowUpward
                )
                MetricBox(
                    modifier = Modifier.weight(1f),
                    title = "میزان پس‌انداز",
                    amount = MoneyFormatter.formatToman(savings),
                    currency = "تومان",
                    accentColor = Color(0xFF38BDF8)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نسبت مصارف به درآمد کل",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${(uiState.expenseRatio * 100).toInt()}٪",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = Color(0xFF38BDF8)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(uiState.expenseRatio)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    modifier: Modifier = Modifier,
    title: String,
    amount: String,
    currency: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF131D33),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF94A3B8)
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                ),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = currency,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: ReportCategory,
    onCategorySelected: (ReportCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportCategory.values().forEach { category ->
            val isSelected = selectedCategory == category
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCategorySelected(category) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFF14B8A6) else Color(0xFF131D33),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFF2DD4BF) else Color(0xFF1E293B)
                )
            ) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.5.sp
                    ),
                    color = if (isSelected) Color(0xFF090E17) else Color(0xFFCBD5E1),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun PeriodFilterRow(
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF10192D))
            .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ReportPeriod.values().forEach { period ->
            val isSelected = selectedPeriod == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) Color(0xFF14B8A6) else Color.Transparent
                    )
                    .clickable { onPeriodSelected(period) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.5.sp
                    ),
                    color = if (isSelected) Color(0xFF090E17) else Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun ActiveCustomRangeBanner(
    startDate: ShamsiDate,
    endDate: ShamsiDate,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F766E).copy(alpha = 0.18f),
        border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = Color(0xFF2DD4BF),
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = "بازه انتخابی: از ${startDate.formatDisplay()} تا ${endDate.formatDisplay()}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xFF5EEAD4)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "تغییر تاریخ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF2DD4BF)
                )
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "ویرایش",
                    tint = Color(0xFF2DD4BF),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthComparisonCard(
    currentValue: Long,
    previousValue: Long,
    percent: Double,
    isIncrease: Boolean,
    label: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF10192D),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val semanticColor = if (isIncrease) Color(0xFFEF4444) else Color(0xFF10B981)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = semanticColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, semanticColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format("%.1f٪ %s", percent, if(isIncrease) "افزایش" else "کاهش"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (isIncrease) Color(0xFFF87171) else Color(0xFF34D399)
                        )
                        Icon(
                            imageVector = if (isIncrease) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = if (isIncrease) Color(0xFFF87171) else Color(0xFF34D399),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = Color.White
                    )
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Timeline,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "دوره فعلی",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${MoneyFormatter.formatToman(currentValue)} تومان",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = Color(0xFFF87171)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "دوره قبل",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${MoneyFormatter.formatToman(previousValue)} تومان",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

@Composable
private fun InstallmentSummaryCard(
    totalAmount: Long,
    paidAmount: Long,
    activeCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF10192D),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$activeCount قسط فعال",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    text = "خلاصه تعهدات اقساط",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricBox(
                    modifier = Modifier.weight(1f),
                    title = "کل تعهدات",
                    amount = MoneyFormatter.formatToman(totalAmount),
                    currency = "تومان",
                    accentColor = Color.White
                )
                MetricBox(
                    modifier = Modifier.weight(1f),
                    title = "پرداخت شده",
                    amount = MoneyFormatter.formatToman(paidAmount),
                    currency = "تومان",
                    accentColor = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
private fun VehicleExpenseCard(
    totalVehicleExpense: Long
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF10192D),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.DirectionsCar, null, tint = Color(0xFFF59E0B))
                Text(
                    text = "مخارج مدیریت خودرو",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            
            MetricBox(
                modifier = Modifier.fillMaxWidth(),
                title = "مجموع هزینه‌های خودرو در این دوره",
                amount = MoneyFormatter.formatToman(totalVehicleExpense),
                currency = "تومان",
                accentColor = Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
private fun ExpenseCategoryCard(
    totalExpense: Long,
    topCategory: String,
    topCategoryAmount: Long
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF10192D),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تحلیل دسته‌بندی هزینه‌ها",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = Color.White
                    )
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PieChart,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF131D33),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "${MoneyFormatter.formatToman(totalExpense)} تومان",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "مجموع هزینه‌های این دوره",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "بیشترین دسته هزینه",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = topCategory.ifBlank { "نامشخص" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = Color.White
                            )
                        }

                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PieChart,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * باتم‌شیت خروجی و اشتراک‌گذاری گزارش (PDF و گالری کاملاً فعال و واقعی)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportBottomSheet(
    startDate: ShamsiDate,
    endDate: ShamsiDate,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onSaveImage: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "بستن",
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "خروجی و اشتراک‌گذاری گزارش",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color.White
                        )
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color(0xFF0F766E).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.5f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PieChart,
                                    contentDescription = null,
                                    tint = Color(0xFF2DD4BF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Preview Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF131D33),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "پیش‌نمایش خروجی بازه انتخابی",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = Color.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "مجموع درآمد: ${MoneyFormatter.formatToman(uiState.totalIncome)} تومان",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = Color(0xFF34D399)
                            )
                            Text(
                                text = "مجموع هزینه: ${MoneyFormatter.formatToman(uiState.totalExpense)} تومان",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = Color(0xFFF87171)
                            )
                        }

                        Text(
                            text = "تحلیل شامل: نمودارهای دایره‌ای، میله‌ای، وضعیت اقساط و خودرو",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // دکمه ۱: دریافت فایل PDF تحلیلی (واقعی و فعال)
                Button(
                    onClick = onExportPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF14B8A6),
                        contentColor = Color(0xFF090E17)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = "PDF",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "دریافت فایل PDF تحلیلی",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                // دکمه ۲: ذخیره تصویر گرافیکی خلاصه در گالری (واقعی و فعال)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSaveImage() },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF131D33),
                    border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = "Gallery",
                            tint = Color(0xFF2DD4BF),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ذخیره تصویر گرافیکی در گالری",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF2DD4BF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * باتم‌شیت اختصاصی انتخاب بازه زمانی شمسی
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangeBottomSheet(
    initialStartDate: ShamsiDate,
    initialEndDate: ShamsiDate,
    onDismiss: () -> Unit,
    onApply: (ShamsiDate, ShamsiDate) -> Unit
) {
    var tempStart by remember { mutableStateOf(initialStartDate) }
    var tempEnd by remember { mutableStateOf(initialEndDate) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color(0xFF14B8A6).copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFF2DD4BF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "انتخاب بازه زمانی دلخواه",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text("انصراف", color = Color(0xFF94A3B8))
                    }
                }

                Text(
                    text = "انتخاب سریع بازه:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF94A3B8)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val now = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
                    
                    PresetChip("۷ روز اخیر") {
                        val startMillis = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                        val start = PersianCalendarHelper.fromEpochMillis(startMillis)
                        tempStart = ShamsiDate(start.year, start.month, start.day)
                        tempEnd = ShamsiDate(now.year, now.month, now.day)
                    }
                    PresetChip("۳۰ روز اخیر") {
                        val startMillis = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                        val start = PersianCalendarHelper.fromEpochMillis(startMillis)
                        tempStart = ShamsiDate(start.year, start.month, start.day)
                        tempEnd = ShamsiDate(now.year, now.month, now.day)
                    }
                    PresetChip("ماه جاری") {
                        tempStart = ShamsiDate(now.year, now.month, 1)
                        tempEnd = ShamsiDate(now.year, now.month, PersianCalendarHelper.getDaysInMonth(now.year, now.month))
                    }
                    PresetChip("۳ ماه اخیر") {
                        var startMonth = now.month - 2
                        var startYear = now.year
                        if (startMonth <= 0) {
                            startMonth += 12
                            startYear -= 1
                        }
                        tempStart = ShamsiDate(startYear, startMonth, 1)
                        tempEnd = ShamsiDate(now.year, now.month, now.day)
                    }
                    PresetChip("از اول سال") {
                        tempStart = ShamsiDate(now.year, 1, 1)
                        tempEnd = ShamsiDate(now.year, now.month, now.day)
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // انتخابگر تاریخ شروع
                PersianDateSelectorRow(
                    title = "از تاریخ (شروع بازه):",
                    date = tempStart,
                    onDateChanged = { tempStart = it }
                )

                // انتخابگر تاریخ پایان
                PersianDateSelectorRow(
                    title = "تا تاریخ (پایان بازه):",
                    date = tempEnd,
                    onDateChanged = { tempEnd = it }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // دکمه اعمال بازه
                Button(
                    onClick = { onApply(tempStart, tempEnd) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF14B8A6),
                        contentColor = Color(0xFF090E17)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "اعمال بازه تاریخی (${tempStart.formatDisplay()} تا ${tempEnd.formatDisplay()})",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PresetChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFFCBD5E1),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun PersianDateSelectorRow(
    title: String,
    date: ShamsiDate,
    onDateChanged: (ShamsiDate) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = Color(0xFFCBD5E1)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // روز
            Box(modifier = Modifier.weight(1f)) {
                var dayExpanded by remember { mutableStateOf(false) }
                DateDropdownPill(
                    label = "روز: ${date.day}",
                    onClick = { dayExpanded = true }
                )
                DropdownMenu(
                    expanded = dayExpanded,
                    onDismissRequest = { dayExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    val maxDays = if (date.month <= 6) 31 else if (date.month <= 11) 30 else 29
                    for (d in 1..maxDays) {
                        DropdownMenuItem(
                            text = { Text("$d", color = Color.White) },
                            onClick = {
                                onDateChanged(date.copy(day = d))
                                dayExpanded = false
                            }
                        )
                    }
                }
            }

            // ماه
            Box(modifier = Modifier.weight(1.3f)) {
                var monthExpanded by remember { mutableStateOf(false) }
                DateDropdownPill(
                    label = date.monthName(),
                    onClick = { monthExpanded = true }
                )
                DropdownMenu(
                    expanded = monthExpanded,
                    onDismissRequest = { monthExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    val months = listOf(
                        1 to "فروردین", 2 to "اردیبهشت", 3 to "خرداد",
                        4 to "تیر", 5 to "مرداد", 6 to "شهریور",
                        7 to "مهر", 8 to "آبان", 9 to "آذر",
                        10 to "دی", 11 to "بهمن", 12 to "اسفند"
                    )
                    months.forEach { (mNum, mName) ->
                        DropdownMenuItem(
                            text = { Text(mName, color = Color.White) },
                            onClick = {
                                val maxDays = if (mNum <= 6) 31 else if (mNum <= 11) 30 else 29
                                val safeDay = if (date.day > maxDays) maxDays else date.day
                                onDateChanged(date.copy(month = mNum, day = safeDay))
                                monthExpanded = false
                            }
                        )
                    }
                }
            }

            // سال
            Box(modifier = Modifier.weight(1f)) {
                var yearExpanded by remember { mutableStateOf(false) }
                DateDropdownPill(
                    label = "سال: ${date.year}",
                    onClick = { yearExpanded = true }
                )
                DropdownMenu(
                    expanded = yearExpanded,
                    onDismissRequest = { yearExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    for (y in 1400..1405) {
                        DropdownMenuItem(
                            text = { Text("$y", color = Color.White) },
                            onClick = {
                                onDateChanged(date.copy(year = y))
                                yearExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateDropdownPill(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF131D33),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
