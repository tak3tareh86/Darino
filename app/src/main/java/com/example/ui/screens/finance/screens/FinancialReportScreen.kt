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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.util.MoneyAmount
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper
import com.example.ui.screens.settings.model.AppCurrency

private enum class ReportPeriod { MONTH, YEAR }
private data class ReportCategoryTotal(val category: TransactionCategory, val amount: Long)

private fun formatReportSharePercentage(amount: Long, total: Long): String {
    if (total <= 0L || amount <= 0L) return "۰٪"
    val percentage = amount.toDouble() / total.toDouble() * 100.0
    if (percentage < 0.1) return "<۰٫۱٪"
    val formatted = String.format(Locale.US, "%.1f", percentage).replace('.', '٫')
    return "${MoneyFormatter.toPersianDigits(formatted)}٪"
}

internal fun sumReportAmountsExact(values: Iterable<Long>): Long? = runCatching {
    values.fold(0L) { total, value -> Math.addExact(total, value) }
}.getOrNull()

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
    LaunchedEffect(selectedType, period, selectedYear, selectedMonth) {
        showAllCategories = false
    }

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
    // Give duplicate legacy category colors distinct, stable colors in the report.
    val reportCategoryColors = buildMap<String, Color> {
        val usedColors = mutableSetOf<Long>()
        val distinctPalette = listOf(
            Color(0xFFF97316), Color(0xFF3B82F6), Color(0xFF8B5CF6),
            Color(0xFF10B981), Color(0xFFEC4899), Color(0xFF06B6D4),
            Color(0xFFF59E0B), Color(0xFF14B8A6), Color(0xFFEF4444),
            Color(0xFF0D9488), Color(0xFF64748B)
        )
        computedTotals.sortedBy { it.category.id }.forEach { total ->
            val originalColor = total.category.accentColor
            val displayColor = if (usedColors.add(originalColor.value.toLong())) {
                originalColor
            } else {
                distinctPalette.firstOrNull { candidate ->
                    usedColors.add(candidate.value.toLong())
                } ?: originalColor
            }
            put(total.category.id, displayColor)
        }
    }
    val totals = if (hasAmountOverflow) emptyList() else computedTotals
    val monthNames = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
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
                        ReportSegment("هزینه‌ها", selectedType == TransactionType.EXPENSE, Modifier.weight(1f), icon = Icons.Default.TrendingDown, selectedColor = MaterialTheme.colorScheme.errorContainer, selectedContentColor = MaterialTheme.colorScheme.onErrorContainer) {
                            selectedType = TransactionType.EXPENSE
                        }
                        ReportSegment("درآمدها", selectedType == TransactionType.INCOME, Modifier.weight(1f), icon = Icons.Default.TrendingUp, selectedColor = MaterialTheme.colorScheme.primaryContainer, selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
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
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            if (hasAmountOverflow) {
                                Text("جمع دسته‌بندی‌ها قابل محاسبه نیست.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            } else if (totals.isEmpty()) {
                                Text("در این بازه تراکنشی ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            } else {
                                totals.take(if (showAllCategories) totals.size else 5).forEach { item ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = (reportCategoryColors[item.category.id] ?: item.category.accentColor).copy(alpha = 0.18f)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(Modifier.size(8.dp).clip(CircleShape).background(reportCategoryColors[item.category.id] ?: item.category.accentColor))
                                            Text(
                                                item.category.title,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                MoneyFormatter.formatExact(MoneyAmount.of(item.amount, AppCurrency.RIAL), includeUnit = false),
                                                modifier = Modifier.weight(1.2f),
                                                maxLines = 1,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                formatReportSharePercentage(item.amount, totalAmount),
                                                maxLines = 1,
                                                softWrap = false,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (totals.size > 5) {
                                    Text(
                                        if (showAllCategories) "نمایش کمتر" else "دسته‌های بیشتر",
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            showAllCategories = !showAllCategories
                                        }.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
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
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                    "نمودار توزیع",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize()) {
                                val stroke = 28.dp.toPx()
                                var startAngle = -90f
                                if (!hasAmountOverflow && totalAmount > 0L) {
                                    totals.forEach { item ->
                                        val sweep = (item.amount.toDouble() / totalAmount.toDouble() * 360.0).toFloat()
                                        drawArc(
                                            reportCategoryColors[item.category.id] ?: item.category.accentColor,
                                            startAngle,
                                            sweep,
                                            false,
                                            style = Stroke(stroke, cap = StrokeCap.Butt)
                                        )
                                        startAngle += sweep
                                    }
                                } else {
                                    drawArc(chartTrackColor, -90f, 360f, false, style = Stroke(stroke))
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("جمع کل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(amountText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text(unitLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (hasAmountOverflow) {
                            Text(
                                "به‌دلیل محدودیت محاسبات عددی، نمودار این بازه نمایش داده نمی‌شود.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        } else if (totals.isEmpty()) {
                            Text(
                                "در این بازه تراکنشی ثبت نشده است.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

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
