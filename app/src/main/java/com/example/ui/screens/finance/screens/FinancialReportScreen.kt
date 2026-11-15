package com.example.ui.screens.finance.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.util.MoneyAmount
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper
import com.example.ui.screens.settings.model.AppCurrency

private enum class ReportPeriod { MONTH, YEAR }
private data class ReportCategoryTotal(val category: TransactionCategory, val amount: Long)

internal fun sumReportAmountsExact(values: Iterable<Long>): Long? = runCatching {
    values.fold(0L) { total, value -> Math.addExact(total, value) }
}.getOrNull()

internal fun reportTrendDayRanges(daysInMonth: Int, bucketCount: Int = 6): List<IntRange> {
    require(daysInMonth > 0 && bucketCount > 0 && bucketCount <= daysInMonth)
    return (0 until bucketCount).map { index ->
        val firstDay = 1 + daysInMonth * index / bucketCount
        val lastDay = daysInMonth * (index + 1) / bucketCount
        firstDay..lastDay
    }
}

@Composable
fun FinancialReportScreen(
    transactions: List<TransactionItemData>,
    categories: List<TransactionCategory>,
    onBackClick: () -> Unit
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val initialDate = remember { PersianCalendarHelper.fromEpochMillis(now) }
    var period by remember { mutableStateOf(ReportPeriod.MONTH) }
    var selectedYear by remember { mutableIntStateOf(initialDate.year) }
    var selectedMonth by remember { mutableIntStateOf(initialDate.month) }
    LaunchedEffect(Unit) {
        var lastJalaliDate = PersianCalendarHelper.fromEpochMillis(now)
        while (true) {
            delay(60_000L)
            val updatedNow = System.currentTimeMillis()
            val updatedJalaliDate = PersianCalendarHelper.fromEpochMillis(updatedNow)
            // Keep following the current period across month/year boundaries,
            // but do not override a period the user navigated to manually.
            if (selectedYear == lastJalaliDate.year && selectedMonth == lastJalaliDate.month) {
                selectedYear = updatedJalaliDate.year
                selectedMonth = updatedJalaliDate.month
            }
            now = updatedNow
            lastJalaliDate = updatedJalaliDate
        }
    }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showAllCategories by remember { mutableStateOf(false) }
    var selectedTrendIndex by remember(period, selectedYear, selectedMonth, selectedType) { mutableIntStateOf(-1) }

    val rangeStart = if (period == ReportPeriod.MONTH) {
        PersianCalendarHelper.jalaliToEpochMillis(selectedYear, selectedMonth, 1, 0, 0)
    } else {
        PersianCalendarHelper.jalaliToEpochMillis(selectedYear, 1, 1, 0, 0)
    }
    val rangeEnd = if (period == ReportPeriod.MONTH) {
        val nextMonth = if (selectedMonth == 12) 1 else selectedMonth + 1
        val nextYear = if (selectedMonth == 12) selectedYear + 1 else selectedYear
        PersianCalendarHelper.jalaliToEpochMillis(nextYear, nextMonth, 1, 0, 0)
    } else {
        PersianCalendarHelper.jalaliToEpochMillis(selectedYear + 1, 1, 1, 0, 0)
    }

    val currentJalaliDate = PersianCalendarHelper.fromEpochMillis(now)
    val isCurrentPeriod = if (period == ReportPeriod.MONTH) {
        selectedYear == currentJalaliDate.year && selectedMonth == currentJalaliDate.month
    } else {
        selectedYear == currentJalaliDate.year
    }
    val effectiveRangeEnd = if (isCurrentPeriod) minOf(rangeEnd, now + 1L) else rangeEnd
    val periodTransactions = transactions.filter {
        it.dateMillis >= rangeStart && it.dateMillis < effectiveRangeEnd && it.type == selectedType
    }
    var hasAmountOverflow = false
    val computedTotals = periodTransactions.groupBy { it.categoryId }.mapNotNull { (categoryId, items) ->
        val category = categories.firstOrNull { it.id == categoryId } ?: items.firstOrNull()?.category
        category?.let { resolvedCategory ->
            val amount = runCatching {
                sumReportAmountsExact(items.map { tx -> MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact() })
            }.getOrNull()
            if (amount == null) {
                hasAmountOverflow = true
                null
            } else {
                ReportCategoryTotal(category = resolvedCategory, amount = amount)
            }
        }
    }.sortedByDescending { it.amount }
    val calculatedTotal = sumReportAmountsExact(computedTotals.map { it.amount })
    if (calculatedTotal == null) hasAmountOverflow = true
    val totalAmount = calculatedTotal ?: 0L
    val totals = if (hasAmountOverflow) emptyList() else computedTotals
    val trendBuckets = if (period == ReportPeriod.YEAR) 12 else 6
    val monthlyTrendRanges = if (period == ReportPeriod.MONTH) {
        reportTrendDayRanges(PersianCalendarHelper.getDaysInMonth(selectedYear, selectedMonth), trendBuckets)
    } else {
        emptyList()
    }
    val visibleTrendBuckets = when {
        isCurrentPeriod && period == ReportPeriod.YEAR -> currentJalaliDate.month
        isCurrentPeriod && period == ReportPeriod.MONTH ->
            monthlyTrendRanges.count { it.first <= currentJalaliDate.day }.coerceAtLeast(1)
        else -> trendBuckets
    }
    val trendAmounts = (0 until visibleTrendBuckets).map { index ->
        val bucketStart: Long
        val bucketEnd: Long
        if (period == ReportPeriod.YEAR) {
            bucketStart = PersianCalendarHelper.jalaliToEpochMillis(selectedYear, index + 1, 1, 0, 0)
            bucketEnd = PersianCalendarHelper.jalaliToEpochMillis(
                if (index == 11) selectedYear + 1 else selectedYear,
                if (index == 11) 1 else index + 2, 1, 0, 0
            )
        } else {
            val bucketDays = monthlyTrendRanges[index]
            bucketStart = PersianCalendarHelper.jalaliToEpochMillis(
                selectedYear, selectedMonth, bucketDays.first, 0, 0
            )
            bucketEnd = if (index == trendBuckets - 1) rangeEnd else {
                PersianCalendarHelper.jalaliToEpochMillis(
                    selectedYear, selectedMonth, bucketDays.last + 1, 0, 0
                )
            }
        }
        val boundedBucketEnd = minOf(bucketEnd, effectiveRangeEnd)
        val bucketTransactions = periodTransactions.filter {
            it.dateMillis >= bucketStart && it.dateMillis < boundedBucketEnd
        }
        val bucketTotal = runCatching {
            sumReportAmountsExact(bucketTransactions.map {
                MoneyAmount.of(it.amount, it.amountUnit).asRialsLongExact()
            })
        }.getOrNull()
        if (bucketTotal == null) {
            hasAmountOverflow = true
            0L
        } else bucketTotal
    }

    val monthNames = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    val allTrendLabels = if (period == ReportPeriod.YEAR) {
        listOf("فر", "ارد", "خرد", "تیر", "مرد", "شهر", "مهر", "آبا", "آذر", "دی", "بهم", "اسف")
    } else {
        monthlyTrendRanges.mapIndexed { index, range ->
            val displayedLastDay = if (
                isCurrentPeriod && index == visibleTrendBuckets - 1
            ) minOf(range.last, currentJalaliDate.day) else range.last
            "${MoneyFormatter.toPersianDigits(range.first.toString())}-${MoneyFormatter.toPersianDigits(displayedLastDay.toString())}"
        }
    }
    val trendLabels = allTrendLabels.take(visibleTrendBuckets)
    val periodTitle = if (period == ReportPeriod.MONTH) "${monthNames[selectedMonth - 1]} ${MoneyFormatter.toPersianDigits(selectedYear.toString())}" else "سال ${MoneyFormatter.toPersianDigits(selectedYear.toString())}"
    val amountText = if (hasAmountOverflow) "—" else MoneyFormatter.formatExact(MoneyAmount.of(totalAmount, AppCurrency.RIAL), includeUnit = false)
    val unitLabel = MoneyFormatter.getUnitLabel()
    val chartPrimaryColor = MaterialTheme.colorScheme.primary
    val chartTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val trendAverageColor = MaterialTheme.colorScheme.tertiary
    val trendPointSurfaceColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Text("گزارش مالی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
                    Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ReportSegment("هزینه‌ها", selectedType == TransactionType.EXPENSE, Modifier.weight(1f), icon = Icons.Default.TrendingDown, selectedColor = MaterialTheme.colorScheme.error, selectedContentColor = MaterialTheme.colorScheme.onError) {
                            selectedType = TransactionType.EXPENSE
                        }
                        ReportSegment("درآمدها", selectedType == TransactionType.INCOME, Modifier.weight(1f), icon = Icons.Default.TrendingUp, selectedColor = MaterialTheme.colorScheme.tertiary, selectedContentColor = MaterialTheme.colorScheme.onTertiary) {
                            selectedType = TransactionType.INCOME
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        ReportSegment("ماهانه", period == ReportPeriod.MONTH, Modifier.weight(0.9f), selectedColor = MaterialTheme.colorScheme.primary, selectedContentColor = MaterialTheme.colorScheme.onPrimary) {
                            period = ReportPeriod.MONTH
                        }
                        ReportSegment("سالانه", period == ReportPeriod.YEAR, Modifier.weight(0.9f), selectedColor = MaterialTheme.colorScheme.secondary, selectedContentColor = MaterialTheme.colorScheme.onSecondary) {
                            period = ReportPeriod.YEAR
                        }
                        Row(
                            modifier = Modifier.weight(1.35f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    if (period == ReportPeriod.MONTH) {
                                        if (selectedMonth == 1) { selectedMonth = 12; selectedYear-- } else selectedMonth--
                                    } else selectedYear--
                                },
                                modifier = Modifier.size(32.dp)
                            ) { Icon(Icons.Default.ChevronRight, contentDescription = "بازه قبل", modifier = Modifier.size(19.dp)) }
                            Text(
                                periodTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(
                                onClick = {
                                    if (period == ReportPeriod.MONTH) {
                                        if (selectedMonth == 12) { selectedMonth = 1; selectedYear++ } else selectedMonth++
                                    } else selectedYear++
                                },
                                modifier = Modifier.size(32.dp)
                            ) { Icon(Icons.Default.ChevronLeft, contentDescription = "بازه بعد", modifier = Modifier.size(19.dp)) }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(11.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(38.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (selectedType == TransactionType.EXPENSE) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        if (selectedType == TransactionType.EXPENSE) "گزارش هزینه‌ها" else "گزارش درآمدها",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        if (selectedType == TransactionType.EXPENSE) "نمای کلی هزینه‌های ثبت‌شده" else "نمای کلی درآمدهای ثبت‌شده",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                totals.take(5).forEach { item ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = item.category.accentColor.copy(alpha = 0.13f)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(Modifier.size(10.dp).clip(CircleShape).background(item.category.accentColor))
                                            Spacer(Modifier.width(6.dp))
                                            Text(item.category.title, modifier = Modifier.weight(1f), maxLines = 1, fontSize = 11.sp)
                                            Text(
                                                "${if (totalAmount > 0) (item.amount.toDouble() / totalAmount.toDouble() * 100.0).toInt() else 0}٪",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                if (hasAmountOverflow) {
                                    Text("جمع دسته‌بندی‌ها قابل محاسبه نیست.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                } else if (totals.isEmpty()) {
                                    Text("در این بازه تراکنشی ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                            Box(Modifier.size(142.dp), contentAlignment = Alignment.Center) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val stroke = 22.dp.toPx()
                                    var startAngle = -90f
                                    if (!hasAmountOverflow && totalAmount > 0L) {
                                        totals.forEach { item ->
                                            val sweep = (item.amount.toDouble() / totalAmount.toDouble() * 360.0).toFloat()
                                            drawArc(item.category.accentColor, startAngle, sweep, false, style = Stroke(stroke, cap = StrokeCap.Butt))
                                            startAngle += sweep
                                        }
                                    } else {
                                        drawArc(chartTrackColor, -90f, 360f, false, style = Stroke(stroke))
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("جمع کل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(amountText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(unitLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (hasAmountOverflow) {
                            Text(
                                "مجموع مبالغ این بازه از محدوده قابل محاسبه خارج است؛ برای جلوگیری از نمایش عدد نادرست، جمع و نمودار پنهان شده‌اند.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("جمع کل ${if (selectedType == TransactionType.EXPENSE) "هزینه‌ها" else "درآمدها"}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                Text("$amountText $unitLabel", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Text(
                                    "روند تغییرات",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        if (hasAmountOverflow) {
                            Text("نمودار به دلیل خارج بودن مجموع مبالغ از محدوده محاسبه نمایش داده نمی‌شود.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        } else {
                            // Exclude the in-progress final bucket from the average.
                            val averageBuckets = if (isCurrentPeriod && trendAmounts.size > 1) {
                                trendAmounts.dropLast(1)
                            } else {
                                trendAmounts
                            }
                            val trendSumResult = sumReportAmountsExact(averageBuckets)
                            val trendSum = trendSumResult ?: 0L
                            val averageTrend = if (averageBuckets.isNotEmpty()) trendSum / averageBuckets.size else 0L
                            val maxTrend = trendAmounts.maxOrNull() ?: 0L
                            val minTrend = trendAmounts.minOrNull() ?: 0L
                            val previousYear = when {
                                period == ReportPeriod.MONTH && selectedMonth > 1 -> selectedYear
                                else -> selectedYear - 1
                            }
                            val previousMonth = if (period == ReportPeriod.MONTH) {
                                if (selectedMonth == 1) 12 else selectedMonth - 1
                            } else {
                                currentJalaliDate.month
                            }
                            val previousRangeStart = if (period == ReportPeriod.MONTH) {
                                PersianCalendarHelper.jalaliToEpochMillis(previousYear, previousMonth, 1, 0, 0)
                            } else {
                                PersianCalendarHelper.jalaliToEpochMillis(selectedYear - 1, 1, 1, 0, 0)
                            }
                            val previousComparableEnd = if (isCurrentPeriod) {
                                val previousDay = currentJalaliDate.day.coerceAtMost(
                                    PersianCalendarHelper.getDaysInMonth(previousYear, previousMonth)
                                )
                                minOf(
                                    rangeStart,
                                    PersianCalendarHelper.jalaliToEpochMillis(
                                        previousYear, previousMonth, previousDay,
                                        currentJalaliDate.hour, currentJalaliDate.minute
                                    ) + 60_000L
                                )
                            } else {
                                rangeStart
                            }
                            val currentComparableTotal = runCatching {
                                sumReportAmountsExact(periodTransactions.map {
                                    MoneyAmount.of(it.amount, it.amountUnit).asRialsLongExact()
                                })
                            }.getOrNull()
                            val previousTotalResult = runCatching {
                                sumReportAmountsExact(transactions.filter {
                                    it.dateMillis >= previousRangeStart &&
                                        it.dateMillis < previousComparableEnd &&
                                        it.type == selectedType
                                }.map {
                                    MoneyAmount.of(it.amount, it.amountUnit).asRialsLongExact()
                                })
                            }.getOrNull()
                            val changeText = when {
                                currentComparableTotal == null || previousTotalResult == null -> "—"
                                previousTotalResult == 0L && currentComparableTotal == 0L -> "—"
                                previousTotalResult == 0L -> "جدید"
                                else -> {
                                    val change = (currentComparableTotal.toDouble() - previousTotalResult.toDouble()) /
                                        previousTotalResult.toDouble() * 100.0
                                    val rounded = kotlin.math.round(change).toLong()
                                    val sign = if (rounded > 0) "+" else ""
                                    "$sign${MoneyFormatter.toPersianDigits(rounded.toString())}٪"
                                }
                            }
                            val changeColor = when {
                                currentComparableTotal == null || previousTotalResult == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                currentComparableTotal == previousTotalResult -> MaterialTheme.colorScheme.onSurfaceVariant
                                (currentComparableTotal > previousTotalResult) == (selectedType == TransactionType.INCOME) -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TrendMetricCard(
                                    if (isCurrentPeriod && trendAmounts.size == 1) "میانگین بازه جاری" else if (isCurrentPeriod) "میانگین بازه‌های کامل" else "میانگین هر بازه",
                                    MoneyFormatter.formatExact(MoneyAmount.of(averageTrend, AppCurrency.RIAL), includeUnit = false),
                                    Modifier.weight(1f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                )
                                TrendMetricCard("بیشترین مقدار", MoneyFormatter.formatExact(MoneyAmount.of(maxTrend, AppCurrency.RIAL), includeUnit = false), Modifier.weight(1f), MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TrendMetricCard("کمترین مقدار", MoneyFormatter.formatExact(MoneyAmount.of(minTrend, AppCurrency.RIAL), includeUnit = false), Modifier.weight(1f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                TrendMetricCard(if (isCurrentPeriod) "تغییر تا امروز" else "تغییر نسبت به قبل", changeText, Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), valueColor = changeColor)
                            }
                            Canvas(
                                Modifier.fillMaxWidth().height(148.dp).pointerInput(trendAmounts) {
                                    detectTapGestures { tap ->
                                        val horizontalInset = 5.dp.toPx()
                                        val usableWidth = (size.width - horizontalInset * 2).coerceAtLeast(1f)
                                        val visualIndex = (((tap.x - horizontalInset) / usableWidth) * trendAmounts.lastIndex)
                                            .roundToInt().coerceIn(0, trendAmounts.lastIndex)
                                        selectedTrendIndex = trendAmounts.lastIndex - visualIndex
                                    }
                                }
                            ) {
                                val maxValue = (trendAmounts.maxOrNull() ?: 0L).coerceAtLeast(1L).toDouble()
                                val horizontalInset = 5.dp.toPx()
                                val topInset = 12.dp.toPx()
                                val bottomInset = 12.dp.toPx()
                                val usableWidth = (size.width - horizontalInset * 2).coerceAtLeast(1f)
                                val usableHeight = (size.height - topInset - bottomInset).coerceAtLeast(1f)
                                val points = trendAmounts.mapIndexed { index, value ->
                                    Offset(
                                        horizontalInset + if (trendAmounts.size > 1) usableWidth * (trendAmounts.lastIndex - index) / trendAmounts.lastIndex else usableWidth / 2f,
                                        topInset + usableHeight * (1.0 - value.toDouble() / maxValue).toFloat()
                                    )
                                }
                                repeat(4) { index ->
                                    val y = topInset + usableHeight * index / 3f
                                    drawLine(
                                        color = chartTrackColor.copy(alpha = 0.65f),
                                        start = Offset(horizontalInset, y),
                                        end = Offset(size.width - horizontalInset, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                fun smoothPath(closeAtBottom: Boolean): Path {
                                    val path = Path()
                                    if (points.isEmpty()) return path
                                    path.moveTo(points.first().x, points.first().y)
                                    for (index in 0 until points.lastIndex) {
                                        val first = points[index]
                                        val second = points[index + 1]
                                        val middleX = (first.x + second.x) / 2f
                                        path.cubicTo(middleX, first.y, middleX, second.y, second.x, second.y)
                                    }
                                    if (closeAtBottom) {
                                        path.lineTo(points.last().x, size.height - bottomInset)
                                        path.lineTo(points.first().x, size.height - bottomInset)
                                        path.close()
                                    }
                                    return path
                                }
                                if (points.size > 1) {
                                    drawPath(
                                        smoothPath(true),
                                        brush = Brush.verticalGradient(
                                            colors = listOf(chartPrimaryColor.copy(alpha = 0.28f), chartPrimaryColor.copy(alpha = 0.015f)),
                                            startY = topInset,
                                            endY = size.height - bottomInset
                                        )
                                    )
                                    drawPath(
                                        smoothPath(false),
                                        color = chartPrimaryColor,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                val averageY = topInset + usableHeight * (1.0 - averageTrend.toDouble() / maxValue).toFloat()
                                drawLine(
                                    color = trendAverageColor,
                                    start = Offset(horizontalInset, averageY),
                                    end = Offset(size.width - horizontalInset, averageY),
                                    strokeWidth = 1.6.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))
                                )
                                if (selectedTrendIndex in points.indices) {
                                    val selectedPoint = points[selectedTrendIndex]
                                    drawLine(
                                        color = trendAverageColor.copy(alpha = 0.65f),
                                        start = Offset(selectedPoint.x, topInset),
                                        end = Offset(selectedPoint.x, size.height - bottomInset),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                                    )
                                }
                                points.forEachIndexed { index, point ->
                                    drawCircle(
                                        color = trendPointSurfaceColor,
                                        radius = if (index == selectedTrendIndex) 7.dp.toPx() else 5.dp.toPx(),
                                        center = point
                                    )
                                    drawCircle(
                                        color = chartPrimaryColor,
                                        radius = if (index == selectedTrendIndex) 5.dp.toPx() else 3.dp.toPx(),
                                        center = point
                                    )
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                trendLabels.forEachIndexed { index, label ->
                                    Text(
                                        label,
                                        color = if (index == selectedTrendIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (index == selectedTrendIndex) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = if (period == ReportPeriod.YEAR) 8.sp else 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            if (selectedTrendIndex in trendAmounts.indices) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            trendLabels[selectedTrendIndex],
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            MoneyFormatter.formatExact(MoneyAmount.of(trendAmounts[selectedTrendIndex], AppCurrency.RIAL), includeUnit = false) + " " + unitLabel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(chartPrimaryColor))
                                Text("مقدار هر بازه", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.width(15.dp).height(2.dp).background(trendAverageColor))
                                Text("میانگین", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                        Text(
                            if (period == ReportPeriod.YEAR) "برچسب‌ها به‌ترتیب ماه‌های سال هستند" else "بازه‌های روزانه ماه",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("دسته‌بندی‌ها", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${MoneyFormatter.toPersianDigits(totals.size.toString())} دسته", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        if (hasAmountOverflow) {
                            Text("به‌دلیل محدودیت محاسبات عددی، جزئیات جمع‌بندی این بازه نمایش داده نمی‌شود.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        } else if (totals.isEmpty()) {
                            Text("داده‌ای برای نمایش وجود ندارد.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        } else {
                            val displayedCategories = if (showAllCategories) totals else totals.take(5)
                            displayedCategories.forEach { item ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.category.iconEmoji, fontSize = 16.sp)
                                        Spacer(Modifier.width(7.dp))
                                        Text(item.category.title, Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1)
                                        Text(MoneyFormatter.formatExact(MoneyAmount.of(item.amount, AppCurrency.RIAL), includeUnit = false), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                        Spacer(Modifier.width(7.dp))
                                        Text("${if (totalAmount > 0) (item.amount.toDouble() / totalAmount.toDouble() * 100.0).toInt() else 0}٪", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                        Box(Modifier.fillMaxWidth(if (totalAmount > 0) (item.amount.toFloat() / totalAmount).coerceIn(0f, 1f) else 0f).height(4.dp).clip(CircleShape).background(item.category.accentColor))
                                    }
                                }
                            }
                            if (totals.size > 5) {
                                Text(
                                    if (showAllCategories) "نمایش کمتر" else "مشاهده همه دسته‌ها (${MoneyFormatter.toPersianDigits((totals.size - 5).toString())} مورد دیگر)",
                                    modifier = Modifier.fillMaxWidth().clickable { showAllCategories = !showAllCategories }.padding(vertical = 5.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun TrendMetricCard(
    label: String,
    value: String,
    modifier: Modifier,
    containerColor: Color,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = containerColor) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1)
            Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ReportSegment(label: String, selected: Boolean, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, selectedColor: Color = MaterialTheme.colorScheme.primary, selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        color = if (selected) selectedColor else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp),
                    tint = if (selected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
            }
            Text(label, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = if (selected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
        }
    }
}
