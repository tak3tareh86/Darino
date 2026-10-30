package com.example.vehicle.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.IranianPhoneUtils
import com.example.util.PersianCalendarHelper
import com.example.vehicle.data.VehicleExpenseCategory
import com.example.vehicle.data.VehicleExpenseEntity
import com.example.util.MoneyFormatter
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private val expenseMonthNames = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)
private data class ExpenseDateParts(val year: Int, val month: Int, val day: Int)

@Composable
fun VehicleExpenseInsightsSection(
    expenses: List<VehicleExpenseEntity>,
    modifier: Modifier = Modifier
) {
    val today = remember { PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()) }
    val currentYear = today.year
    val currentMonth = today.month
    val currentDay = today.day
    val datedExpenses = remember(expenses) {
        expenses.mapNotNull { expense -> parseExpenseDate(expense.date)?.let { expense to it } }
    }
    val invalidCount = expenses.size - datedExpenses.size
    val yearOptions = remember(datedExpenses, currentYear) {
        val years = datedExpenses.map { it.second.year }
        val start = minOf(years.minOrNull() ?: currentYear, currentYear - 10)
        val end = maxOf(years.maxOrNull() ?: currentYear, currentYear)
        (start..end).filter { it in 1300..1500 }.toList().ifEmpty { listOf(currentYear) }
    }

    var annualYear by rememberSaveable { mutableIntStateOf(currentYear) }
    var firstMonth by rememberSaveable { mutableIntStateOf(currentMonth) }
    var firstYear by rememberSaveable { mutableIntStateOf(currentYear) }
    var secondMonth by rememberSaveable { mutableIntStateOf(if (currentMonth == 1) 12 else currentMonth - 1) }
    var secondYear by rememberSaveable { mutableIntStateOf(if (currentMonth == 1) currentYear - 1 else currentYear) }

    val previousMonth = if (currentMonth == 1) 12 else currentMonth - 1
    val previousYear = if (currentMonth == 1) currentYear - 1 else currentYear
    val previousDayLimit = minOf(currentDay, jalaliDaysInMonth(previousYear, previousMonth))
    val currentToDate = datedExpenses.filter { it.second.year == currentYear && it.second.month == currentMonth && it.second.day <= currentDay }.sumOf { it.first.amount }
    val previousToDate = datedExpenses.filter { it.second.year == previousYear && it.second.month == previousMonth && it.second.day <= previousDayLimit }.sumOf { it.first.amount }
    val currentCount = datedExpenses.count { it.second.year == currentYear && it.second.month == currentMonth && it.second.day <= currentDay }
    val previousCount = datedExpenses.count { it.second.year == previousYear && it.second.month == previousMonth && it.second.day <= previousDayLimit }

    val annualTotals = remember(datedExpenses, annualYear) {
        (1..12).map { month ->
            month to datedExpenses.filter { it.second.year == annualYear && it.second.month == month }.sumOf { it.first.amount }
        }
    }
    val annualTotal = annualTotals.sumOf { it.second }
    val annualMax = annualTotals.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    val annualCount = datedExpenses.count { it.second.year == annualYear }

    val firstTotal = datedExpenses.filter { it.second.year == firstYear && it.second.month == firstMonth }.sumOf { it.first.amount }
    val secondTotal = datedExpenses.filter { it.second.year == secondYear && it.second.month == secondMonth }.sumOf { it.first.amount }
    val firstCount = datedExpenses.count { it.second.year == firstYear && it.second.month == firstMonth }
    val secondCount = datedExpenses.count { it.second.year == secondYear && it.second.month == secondMonth }
    val categoryComparison = remember(datedExpenses, firstYear, firstMonth, secondYear, secondMonth) {
        VehicleExpenseCategory.values().map { category ->
            val firstAmount = datedExpenses.filter {
                it.first.category == category && it.second.year == firstYear && it.second.month == firstMonth
            }.sumOf { it.first.amount }
            val secondAmount = datedExpenses.filter {
                it.first.category == category && it.second.year == secondYear && it.second.month == secondMonth
            }.sumOf { it.first.amount }
            Triple(category, firstAmount, secondAmount)
        }.filter { it.second > 0L || it.third > 0L }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("مقایسه با ماه قبل", "هزینه‌ها تا روز متناظر هر ماه", Icons.Rounded.CompareArrows)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AmountSummary(expenseMonthNames[currentMonth - 1] + " " + fa(currentYear), currentToDate, currentCount, Modifier.weight(1f))
                    AmountSummary(expenseMonthNames[previousMonth - 1] + " " + fa(previousYear), previousToDate, previousCount, Modifier.weight(1f))
                }
                DifferenceSummary(currentToDate, previousToDate, currentCount, previousCount)
                Text(
                    "برای جلوگیری از مقایسه گمراه‌کننده، هزینه ماه جاری تا امروز با همین تعداد روز از ماه قبل سنجیده شده است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("مقایسه دلخواه ماه‌ها", "انتخاب مستقل ماه و سال برای هر بازه", Icons.Rounded.CompareArrows)
                PeriodPicker("بازه اول", firstMonth, firstYear, yearOptions, { firstMonth = it }, { firstYear = it })
                PeriodPicker("بازه دوم", secondMonth, secondYear, yearOptions, { secondMonth = it }, { secondYear = it })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AmountSummary(expenseMonthNames[firstMonth - 1] + " " + fa(firstYear), firstTotal, firstCount, Modifier.weight(1f))
                    AmountSummary(expenseMonthNames[secondMonth - 1] + " " + fa(secondYear), secondTotal, secondCount, Modifier.weight(1f))
                }
                DifferenceSummary(firstTotal, secondTotal, firstCount, secondCount)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text("مقایسه به تفکیک دسته‌بندی (مبالغ به ${MoneyFormatter.getUnitLabel()})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (categoryComparison.isEmpty()) {
                    EmptyExpenseMessage("در هیچ‌یک از دو بازه، هزینه دسته‌بندی‌شده‌ای ثبت نشده است.")
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "دسته‌بندی",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "بازه اول",
                            Modifier.width(94.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            "بازه دوم",
                            Modifier.width(94.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    categoryComparison.forEach { (category, firstAmount, secondAmount) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(category.title, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                amountNumber(firstAmount),
                                Modifier.width(94.dp),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                amountNumber(secondAmount),
                                Modifier.width(94.dp),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        if (invalidCount > 0) {
            Text(
                fa(invalidCount) + " رکورد به‌دلیل تاریخ نامعتبر از تحلیل زمانی کنار گذاشته شد؛ تاریخ این هزینه‌ها را بررسی کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PeriodPicker(
    title: String,
    month: Int,
    year: Int,
    years: List<Int>,
    onMonthSelected: (Int) -> Unit,
    onYearSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactSelector(expenseMonthNames[month - 1], expenseMonthNames.mapIndexed { index, name -> index + 1 to name }, onMonthSelected, Modifier.weight(1f))
            CompactSelector(fa(year), years.map { it to fa(it) }, onYearSelected, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CompactSelector(
    selectedLabel: String,
    options: List<Pair<Int, String>>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(selectedLabel, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.second, maxLines = 1) },
                    onClick = { onSelected(option.first); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun AmountSummary(title: String, amount: Long, count: Int, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                title,
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                money(amount),
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                fa(count) + " هزینه ثبت‌شده",
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun DifferenceSummary(first: Long, second: Long, firstCount: Int, secondCount: Int) {
    val difference = first - second
    val bothEmpty = firstCount == 0 && secondCount == 0
    val percent = if (second != 0L) difference.toDouble() / second.toDouble() * 100.0 else null
    val color = when { difference > 0L -> MaterialTheme.colorScheme.error; difference < 0L -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.onSurfaceVariant }
    val icon = when { difference > 0L -> Icons.Rounded.TrendingUp; difference < 0L -> Icons.Rounded.TrendingDown; else -> Icons.Rounded.CompareArrows }
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.08f)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                when {
                    bothEmpty -> Text("در هیچ‌یک از دو بازه هزینه‌ای ثبت نشده است.", color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    second == 0L && first == 0L -> Text("مبلغ هر دو بازه صفر است؛ درصد تغییر قابل محاسبه نیست.", color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    second == 0L -> Text("در بازه دوم هزینه‌ای ثبت نشده؛ درصد تغییر قابل محاسبه نیست.", color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    difference == 0L -> Text("مجموع هزینه دو بازه برابر است.", color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    else -> {
                        Text((if (difference > 0L) "افزایش " else "کاهش ") + money(abs(difference)), color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(fa(String.format(Locale.US, "%.1f", abs(percent ?: 0.0))) + "٪ " + (if (difference > 0L) "افزایش" else "کاهش") + " نسبت به بازه دوم", color = color, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyExpenseMessage(message: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
        Text(message, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun parseExpenseDate(raw: String): ExpenseDateParts? {
    val clean = IranianPhoneUtils.convertDigitsToEnglish(raw.trim())
    val parts = clean.split('/', '-')
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    val maxDay = runCatching { PersianCalendarHelper.getDaysInMonth(year, month) }.getOrNull() ?: return null
    if (year !in 1300..1500 || month !in 1..12 || day !in 1..maxDay) return null
    return ExpenseDateParts(year, month, day)
}

private fun jalaliDaysInMonth(year: Int, month: Int): Int =
    runCatching { PersianCalendarHelper.getDaysInMonth(year, month) }.getOrDefault(if (month <= 6) 31 else 30)

private fun amountNumber(amount: Long): String = MoneyFormatter.formatToman(amount, includeUnit = false)

private fun money(amount: Long): String = MoneyFormatter.formatToman(amount)

private fun fa(value: Int): String = IranianPhoneUtils.convertDigitsToPersian(value.toString())
private fun fa(value: String): String = IranianPhoneUtils.convertDigitsToPersian(value)
