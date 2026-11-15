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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
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
import java.util.Calendar

private enum class ReportPeriod { MONTH, YEAR }
private data class ReportCategoryTotal(val category: TransactionCategory, val amount: Long, val count: Int)

@Composable
fun FinancialReportScreen(
    transactions: List<TransactionItemData>,
    categories: List<TransactionCategory>,
    onBackClick: () -> Unit,
    onTransactionClick: (TransactionItemData) -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    val initialDate = remember { PersianCalendarHelper.fromEpochMillis(now) }
    var period by remember { mutableStateOf(ReportPeriod.MONTH) }
    var selectedYear by remember { mutableIntStateOf(initialDate.year) }
    var selectedMonth by remember { mutableIntStateOf(initialDate.month) }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

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
    val totals = periodTransactions.groupBy { it.categoryId }.mapNotNull { (categoryId, items) ->
        val category = categories.firstOrNull { it.id == categoryId } ?: items.firstOrNull()?.category
        category?.let {
            ReportCategoryTotal(
                category = it,
                amount = items.sumOf { tx -> MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact() },
                count = items.size
            )
        }
    }.sortedByDescending { it.amount }
    val totalAmount = totals.sumOf { it.amount }
    val selectedCategory = selectedCategoryId?.let { id -> totals.firstOrNull { it.category.id == id } }
    val visibleTransactions = if (selectedCategoryId == null) periodTransactions else {
        periodTransactions.filter { it.categoryId == selectedCategoryId }
    }.sortedByDescending { it.dateMillis }

    val monthNames = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    val periodTitle = if (period == ReportPeriod.MONTH) "${monthNames[selectedMonth - 1]} ${MoneyFormatter.toPersianDigits(selectedYear.toString())}" else "سال ${MoneyFormatter.toPersianDigits(selectedYear.toString())}"
    val amountText = MoneyFormatter.formatExact(MoneyAmount.of(totalAmount, AppCurrency.RIAL), includeUnit = false)

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
                            selectedCategoryId = null
                        }
                        ReportSegment("درآمدها", selectedType == TransactionType.INCOME, Modifier.weight(1f)) {
                            selectedType = TransactionType.INCOME
                            selectedCategoryId = null
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ReportSegment("ماهانه", period == ReportPeriod.MONTH, Modifier.weight(1f)) { period = ReportPeriod.MONTH; selectedCategoryId = null }
                            ReportSegment("سالانه", period == ReportPeriod.YEAR, Modifier.weight(1f)) { period = ReportPeriod.YEAR; selectedCategoryId = null }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = {
                                if (period == ReportPeriod.MONTH) {
                                    if (selectedMonth == 1) { selectedMonth = 12; selectedYear-- } else selectedMonth--
                                } else selectedYear--
                                selectedCategoryId = null
                            }) { Icon(Icons.Default.ChevronRight, contentDescription = "بازه قبل") }
                            Text(periodTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = {
                                if (period == ReportPeriod.MONTH) {
                                    if (selectedMonth == 12) { selectedMonth = 1; selectedYear++ } else selectedMonth++
                                } else selectedYear++
                                selectedCategoryId = null
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
                            Box(Modifier.size(142.dp), contentAlignment = Alignment.Center) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val stroke = 22.dp.toPx()
                                    var startAngle = -90f
                                    if (totalAmount > 0L) {
                                        totals.forEach { item ->
                                            val sweep = (item.amount.toDouble() / totalAmount.toDouble() * 360.0).toFloat()
                                            drawArc(item.category.accentColor, startAngle, sweep, false, style = Stroke(stroke, cap = StrokeCap.Butt))
                                            startAngle += sweep
                                        }
                                    } else {
                                        drawArc(MaterialTheme.colorScheme.surfaceVariant, -90f, 360f, false, style = Stroke(stroke))
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("جمع کل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(amountText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("تومان", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                totals.take(5).forEach { item ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(9.dp).clip(CircleShape).background(item.category.accentColor))
                                        Spacer(Modifier.width(6.dp))
                                        Text(item.category.title, modifier = Modifier.weight(1f), maxLines = 1, fontSize = 11.sp)
                                        Text("${if (totalAmount > 0) (item.amount * 100 / totalAmount).toInt() else 0}٪", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (totals.isEmpty()) Text("در این بازه تراکنشی ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("جمع کل ${if (selectedType == TransactionType.EXPENSE) "هزینه‌ها" else "درآمدها"}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text("$amountText تومان", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("دسته‌بندی‌ها", fontWeight = FontWeight.Bold)
                        if (totals.isEmpty()) {
                            Text("داده‌ای برای نمایش وجود ندارد.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else totals.forEach { item ->
                            val selected = selectedCategoryId == item.category.id
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable { selectedCategoryId = if (selected) null else item.category.id }
                                    .padding(9.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.category.iconEmoji, fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(item.category.title, Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(MoneyFormatter.formatExact(MoneyAmount.of(item.amount, AppCurrency.RIAL), includeUnit = false), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                    Box(Modifier.fillMaxWidth(if (totalAmount > 0) (item.amount.toFloat() / totalAmount).coerceIn(0f, 1f) else 0f).height(6.dp).clip(CircleShape).background(item.category.accentColor))
                                }
                                Text("${item.count} تراکنش • ${if (totalAmount > 0) (item.amount * 100 / totalAmount).toInt() else 0}٪", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedCategory == null) "جزئیات تراکنش‌ها" else "تراکنش‌های ${selectedCategory.category.title}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    if (selectedCategoryId != null) Text("نمایش همه", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { selectedCategoryId = null }, fontSize = 12.sp)
                }
            }
            if (visibleTransactions.isEmpty()) {
                item { Text("تراکنشی در این بخش وجود ندارد.", modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(visibleTransactions, key = { it.id }) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onTransactionClick(tx) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(tx.category.iconEmoji, fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tx.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("${tx.datePersian} • ${tx.timePersian}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(MoneyFormatter.formatExact(MoneyAmount.of(tx.amount, tx.amountUnit)), fontWeight = FontWeight.Bold, color = if (selectedType == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontSize = 12.sp)
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
