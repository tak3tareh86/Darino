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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val now = remember { System.currentTimeMillis() }
    val initialDate = remember { PersianCalendarHelper.fromEpochMillis(now) }
    var period by remember { mutableStateOf(ReportPeriod.MONTH) }
    var selectedYear by remember { mutableIntStateOf(initialDate.year) }
    var selectedMonth by remember { mutableIntStateOf(initialDate.month) }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showAllCategories by remember { mutableStateOf(false) }

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

    val periodTransactions = transactions.filter {
        it.dateMillis >= rangeStart && it.dateMillis < rangeEnd && it.type == selectedType
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
    val trendAmounts = (0 until trendBuckets).map { index ->
        val bucketStart: Long
        val bucketEnd: Long
        if (period == ReportPeriod.YEAR) {
            bucketStart = PersianCalendarHelper.jalaliToEpochMillis(selectedYear, index + 1, 1, 0, 0)
            bucketEnd = PersianCalendarHelper.jalaliToEpochMillis(
                if (index == 11) selectedYear + 1 else selectedYear,
                if (index == 11) 1 else index + 2, 1, 0, 0
            )
        } else {
            val daysInMonth = when {
                selectedMonth <= 6 -> 31
                selectedMonth <= 11 -> 30
                else -> kotlin.math.round(
                    (PersianCalendarHelper.jalaliToEpochMillis(selectedYear + 1, 1, 1, 0, 0) -
                        PersianCalendarHelper.jalaliToEpochMillis(selectedYear, 12, 1, 0, 0)) / 86_400_000.0
                ).toInt().coerceIn(29, 30)
            }
            val firstDay = 1 + (daysInMonth * index / trendBuckets)
            val lastDay = 1 + (daysInMonth * (index + 1) / trendBuckets)
            bucketStart = PersianCalendarHelper.jalaliToEpochMillis(selectedYear, selectedMonth, firstDay, 0, 0)
            bucketEnd = if (index == trendBuckets - 1) rangeEnd else {
                PersianCalendarHelper.jalaliToEpochMillis(selectedYear, selectedMonth, lastDay, 0, 0)
            }
        }
        val bucketTransactions = periodTransactions.filter { it.dateMillis >= bucketStart && it.dateMillis < bucketEnd }
        val bucketTotal = runCatching {
            sumReportAmountsExact(bucketTransactions.map { MoneyAmount.of(it.amount, it.amountUnit).asRialsLongExact() })
        }.getOrNull()
        if (bucketTotal == null) {
            hasAmountOverflow = true
            0L
        } else bucketTotal
    }

    val monthNames = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    val trendLabels = if (period == ReportPeriod.YEAR) {
        listOf("فر", "ارد", "خرد", "تیر", "مرد", "شهر", "مهر", "آبا", "آذر", "دی", "بهم", "اسف")
    } else {
        val daysInMonth = when {
            selectedMonth <= 6 -> 31
            selectedMonth <= 11 -> 30
            else -> kotlin.math.round(
                (PersianCalendarHelper.jalaliToEpochMillis(selectedYear + 1, 1, 1, 0, 0) -
                    PersianCalendarHelper.jalaliToEpochMillis(selectedYear, 12, 1, 0, 0)) / 86_400_000.0
            ).toInt().coerceIn(29, 30)
        }
        reportTrendDayRanges(daysInMonth, trendBuckets).map { range ->
            "${MoneyFormatter.toPersianDigits(range.first.toString())}-${MoneyFormatter.toPersianDigits(range.last.toString())}"
        }
    }
    if (trendAmounts.any { it == 0L } && hasAmountOverflow) {
        // Keep the report in an explicit error state rather than displaying partial totals.
    }
    val periodTitle = if (period == ReportPeriod.MONTH) "${monthNames[selectedMonth - 1]} ${MoneyFormatter.toPersianDigits(selectedYear.toString())}" else "سال ${MoneyFormatter.toPersianDigits(selectedYear.toString())}"
    val amountText = if (hasAmountOverflow) "—" else MoneyFormatter.formatExact(MoneyAmount.of(totalAmount, AppCurrency.RIAL), includeUnit = false)
    val unitLabel = MoneyFormatter.getUnitLabel()
    val chartPrimaryColor = MaterialTheme.colorScheme.primary
    val chartTrackColor = MaterialTheme.colorScheme.surfaceVariant

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
                        ReportSegment("هزینه‌ها", selectedType == TransactionType.EXPENSE, Modifier.weight(1f)) {
                            selectedType = TransactionType.EXPENSE
                        }
                        ReportSegment("درآمدها", selectedType == TransactionType.INCOME, Modifier.weight(1f)) {
                            selectedType = TransactionType.INCOME
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ReportSegment("ماهانه", period == ReportPeriod.MONTH, Modifier.weight(1f)) { period = ReportPeriod.MONTH }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = {
                                if (period == ReportPeriod.MONTH) {
                                    if (selectedMonth == 1) { selectedMonth = 12; selectedYear-- } else selectedMonth--
                                } else selectedYear--
                            }) { Icon(Icons.Default.ChevronRight, contentDescription = "بازه قبل") }
                            Text(periodTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = {
                                if (period == ReportPeriod.MONTH) {
                                    if (selectedMonth == 12) { selectedMonth = 1; selectedYear++ } else selectedMonth++
                                } else selectedYear++
                            }) { Icon(Icons.Default.ChevronLeft, contentDescription = "بازه بعد") }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(if (selectedType == TransactionType.EXPENSE) "گزارش هزینه‌ها" else "گزارش درآمدها", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                totals.take(5).forEach { item ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(9.dp).clip(CircleShape).background(item.category.accentColor))
                                        Spacer(Modifier.width(6.dp))
                                        Text(item.category.title, modifier = Modifier.weight(1f), maxLines = 1, fontSize = 11.sp)
                                        Text("${if (totalAmount > 0) (item.amount.toDouble() / totalAmount.toDouble() * 100.0).toInt() else 0}٪", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                        Text("روند تغییرات", fontWeight = FontWeight.Bold)
                        if (hasAmountOverflow) {
                            Text("نمودار به دلیل خارج بودن مجموع مبالغ از محدوده محاسبه نمایش داده نمی‌شود.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        } else {
                        Canvas(Modifier.fillMaxWidth().height(112.dp)) {
                            val maxValue = (trendAmounts.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
                            val stepX = if (trendAmounts.size > 1) size.width / (trendAmounts.size - 1) else size.width
                            val points = trendAmounts.mapIndexed { index, value ->
                                Offset(index * stepX, size.height - (value.toFloat() / maxValue * (size.height - 12.dp.toPx())) - 6.dp.toPx())
                            }
                            if (points.size > 1) {
                                val path = Path().apply {
                                    moveTo(points.first().x, points.first().y)
                                    points.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(path, chartPrimaryColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                            }
                            points.forEach { point ->
                                drawCircle(chartPrimaryColor, radius = 4.dp.toPx(), center = point)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            trendLabels.forEach { label ->
                                Text(
                                    label,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = if (period == ReportPeriod.YEAR) 8.sp else 9.sp,
                                    maxLines = 1
                                )
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
private fun ReportSegment(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
    }
}
