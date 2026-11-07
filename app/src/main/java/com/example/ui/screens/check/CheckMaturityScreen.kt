package com.example.ui.screens.check

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType
import com.example.ui.screens.settings.model.AppCurrency
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyAmount
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Calendar

private data class MaturityCalculation(
    val totalRials: BigInteger,
    val roundedDays: Long?,
    val targetDate: String?,
    val invalidAmount: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckMaturityScreen(checks: List<Check>, onBack: () -> Unit) {
    var selectedType by rememberSaveable { mutableStateOf(CheckType.RECEIVED.name) }
    val type = if (selectedType == CheckType.RECEIVED.name) CheckType.RECEIVED else CheckType.PAID
    val eligibleChecks = remember(checks, type) {
        checks.filter {
            it.type == type && it.amount > 0 &&
                it.status != CheckStatus.CLEARED &&
                it.status != CheckStatus.CANCELED &&
                it.status != CheckStatus.BOUNCED
        }.sortedBy { it.dueDate }
    }
    var selectedIds by remember { mutableStateOf(emptySet<Int>()) }
    LaunchedEffect(type, eligibleChecks.map { it.id }) {
        selectedIds = selectedIds.intersect(eligibleChecks.map { it.id }.toSet())
    }

    val today = remember {
        val now = Calendar.getInstance()
        LocalDate.of(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH))
    }
    val todayMillis = remember(today) { today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
    var baselineMillis by rememberSaveable { mutableLongStateOf(todayMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = baselineMillis)
    val selectedChecks = remember(eligibleChecks, selectedIds) { eligibleChecks.filter { it.id in selectedIds } }
    val baselineGregorian = remember(baselineMillis) {
        Instant.ofEpochMilli(baselineMillis).atZone(ZoneOffset.UTC).toLocalDate()
    }
    val baselineJalali = remember(baselineGregorian) {
        PersianCalendarHelper.gregorianToJalali(baselineGregorian.year, baselineGregorian.monthValue, baselineGregorian.dayOfMonth)
    }
    val calculation = remember(selectedChecks, baselineJalali) { calculateMaturity(selectedChecks, baselineJalali) }
    val allSelected = eligibleChecks.isNotEmpty() && selectedIds.size == eligibleChecks.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("محاسبه سررسید موزون", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("چک‌ها را انتخاب کن تا تاریخ راس را ببینی", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(onClick = onBack, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Icon(Icons.Rounded.ArrowForward, contentDescription = "بازگشت", modifier = Modifier.padding(11.dp))
                }
            }
        }
        item {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), color = Color.Transparent) {
                Box(
                    Modifier.background(
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.82f), Color(0xFF0F766E)))
                    ).padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.16f)) {
                                Icon(Icons.Rounded.AutoGraph, null, tint = Color.White, modifier = Modifier.padding(9.dp))
                            }
                            Text("ابزار هوشمند راس‌گیری", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Rounded.SsidChart, null, tint = Color.White.copy(alpha = 0.8f))
                        }
                        Text(calculation.targetDate ?: "هنوز چکی انتخاب نشده", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (calculation.roundedDays == null) "با انتخاب چک‌ها، تاریخ میانگین موزون سررسید اینجا نمایش داده می‌شود"
                            else "تاریخ راس بر اساس " + selectedChecks.size + " چک انتخاب‌شده",
                            color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MaturityMetric(Modifier.weight(1f), "تعداد چک", IranianPhoneUtils.convertDigitsToPersian(selectedChecks.size.toString()))
                            MaturityMetric(
                                Modifier.weight(1f), "فاصله از مبنا",
                                calculation.roundedDays?.let {
                                    val dayText = IranianPhoneUtils.convertDigitsToPersian(kotlin.math.abs(it).toString())
                                    when { it > 0 -> dayText + " روز بعد"; it < 0 -> dayText + " روز قبل"; else -> "همان روز" }
                                } ?: "—"
                            )
                        }
                    }
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("نوع چک‌ها", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = type == CheckType.RECEIVED,
                            onClick = { selectedType = CheckType.RECEIVED.name },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Icon(Icons.Rounded.CallReceived, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp)); Text("دریافتی")
                        }
                        SegmentedButton(
                            selected = type == CheckType.PAID,
                            onClick = { selectedType = CheckType.PAID.name },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Icon(Icons.Rounded.CallMade, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp)); Text("پرداختی")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("تاریخ مبنا", fontWeight = FontWeight.SemiBold)
                            Text(
                                PersianCalendarHelper.PersianDateTime(baselineJalali.first, baselineJalali.second, baselineJalali.third).toFormattedDate(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
                            )
                        }
                        FilledTonalButton(onClick = {
                            datePickerState.selectedDateMillis = baselineMillis
                            showDatePicker = true
                        }) {
                            Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("تغییر تاریخ")
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("انتخاب چک‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(IranianPhoneUtils.convertDigitsToPersian(eligibleChecks.size.toString()) + " چک قابل محاسبه",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = {
                    selectedIds = if (allSelected) emptySet() else eligibleChecks.map { it.id }.toSet()
                }, enabled = eligibleChecks.isNotEmpty()) {
                    Text(if (allSelected) "پاک‌کردن انتخاب" else "انتخاب همه")
                }
            }
        }
        if (eligibleChecks.isEmpty()) {
            item {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.EventBusy, null, modifier = Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("چک واجد شرایطی پیدا نشد", fontWeight = FontWeight.Bold)
                        Text("چک‌های تسویه‌شده، لغوشده و برگشتی در راس‌گیری وارد نمی‌شوند.",
                            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(eligibleChecks, key = { it.id }) { check ->
                val selected = check.id in selectedIds
                Surface(
                    Modifier.fillMaxWidth().clickable { selectedIds = if (selected) selectedIds - check.id else selectedIds + check.id },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Checkbox(checked = selected, onCheckedChange = { checked ->
                            selectedIds = if (checked) selectedIds + check.id else selectedIds - check.id
                        })
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(check.bankName, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("شماره " + check.checkNumber + " • سررسید " + check.formattedDueDate,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(check.formattedAmount, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(check.amountUnit.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Calculate, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp)); Text("خلاصه محاسبه", fontWeight = FontWeight.Bold)
                    }
                    SummaryLine("مجموع مبلغ انتخاب‌شده", formatRials(calculation.totalRials))
                    SummaryLine("تعداد چک‌های انتخاب‌شده", IranianPhoneUtils.convertDigitsToPersian(selectedChecks.size.toString()))
                    if (calculation.invalidAmount) {
                        Text("مبلغ یکی از چک‌ها به‌دلیل محدودیت واحد پول قابل تبدیل دقیق نیست؛ محاسبه انجام نشد.",
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    } else if (selectedChecks.isEmpty()) {
                        Text("برای شروع، حداقل یک چک را انتخاب کن.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("راس‌گیری بر پایه میانگین موزون مبلغ و فاصله روزانه سررسید از تاریخ مبنا انجام می‌شود. روز اعشاری به نزدیک‌ترین روز گرد می‌شود.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { baselineMillis = it }
                    showDatePicker = false
                }) { Text("تأیید") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("انصراف") }
            }
        ) { DatePicker(state = datePickerState, showModeToggle = false) }
    }
}

@Composable
private fun MaturityMetric(modifier: Modifier, label: String, value: String) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.14f)) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatRials(value: BigInteger): String =
    MoneyFormatter.formatExact(MoneyAmount.of(BigDecimal(value), AppCurrency.RIAL))

private fun calculateMaturity(checks: List<Check>, baselineJalali: Triple<Int, Int, Int>): MaturityCalculation {
    if (checks.isEmpty()) return MaturityCalculation(BigInteger.ZERO, null, null)
    val baseDay = PersianCalendarHelper.getJalaliEpochDay(baselineJalali.first, baselineJalali.second, baselineJalali.third)
    var total = BigInteger.ZERO
    var weighted = BigInteger.ZERO
    for (check in checks) {
        val rials = try {
            MoneyAmount.of(check.amount, check.amountUnit).asRialsLongExact()
        } catch (_: ArithmeticException) {
            return MaturityCalculation(total, null, null, invalidAmount = true)
        }
        if (rials <= 0L) continue
        val due = PersianCalendarHelper.fromEpochMillis(check.dueDate)
        val days = PersianCalendarHelper.getJalaliEpochDay(due.year, due.month, due.day) - baseDay
        val amount = BigInteger.valueOf(rials)
        total = total.add(amount)
        weighted = weighted.add(amount.multiply(BigInteger.valueOf(days)))
    }
    if (total.signum() <= 0) return MaturityCalculation(total, null, null)
    val division = weighted.divideAndRemainder(total)
    var rounded = division[0]
    if (division[1].abs().shiftLeft(1) >= total) rounded = rounded.add(BigInteger.valueOf(weighted.signum().toLong()))
    val offset = try { rounded.longValueExact() } catch (_: ArithmeticException) {
        return MaturityCalculation(total, null, null, invalidAmount = true)
    }
    val baseGregorian = PersianCalendarHelper.jalaliToGregorian(baselineJalali.first, baselineJalali.second, baselineJalali.third)
    val target = try { LocalDate.of(baseGregorian.first, baseGregorian.second, baseGregorian.third).plusDays(offset) }
    catch (_: Exception) { return MaturityCalculation(total, null, null, invalidAmount = true) }
    val jalali = PersianCalendarHelper.gregorianToJalali(target.year, target.monthValue, target.dayOfMonth)
    val formatted = PersianCalendarHelper.PersianDateTime(jalali.first, jalali.second, jalali.third).toFormattedDate()
    return MaturityCalculation(total, offset, formatted)
}
