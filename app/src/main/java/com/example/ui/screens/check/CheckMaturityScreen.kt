package com.example.ui.screens.check

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
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
import java.util.TimeZone
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
    val context = LocalContext.current
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
    var searchQuery by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(type, eligibleChecks.map { it.id }) {
        selectedIds = selectedIds.intersect(eligibleChecks.map { it.id }.toSet())
    }

    val todayMillis = remember {
        val now = Calendar.getInstance()
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        }.timeInMillis
    }
    var baselineMillis by rememberSaveable { mutableLongStateOf(todayMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = baselineMillis)
    val selectedChecks = remember(eligibleChecks, selectedIds) { eligibleChecks.filter { it.id in selectedIds } }
    val visibleChecks = remember(eligibleChecks, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) eligibleChecks else eligibleChecks.filter { check ->
            check.bankName.contains(query, ignoreCase = true) ||
                check.checkNumber.contains(query, ignoreCase = true) ||
                check.formattedAmount.contains(query, ignoreCase = true) ||
                check.formattedDueDate.contains(query, ignoreCase = true)
        }
    }
    val baselineGregorian = remember(baselineMillis) {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = baselineMillis }
    }
    val baselineJalali = remember(baselineMillis) {
        PersianCalendarHelper.gregorianToJalali(
            baselineGregorian.get(Calendar.YEAR),
            baselineGregorian.get(Calendar.MONTH) + 1,
            baselineGregorian.get(Calendar.DAY_OF_MONTH)
        )
    }
    val baselineGregorianDate = Triple(
        baselineGregorian.get(Calendar.YEAR),
        baselineGregorian.get(Calendar.MONTH) + 1,
        baselineGregorian.get(Calendar.DAY_OF_MONTH)
    )
    var calculationRevision by rememberSaveable { mutableIntStateOf(0) }
    val calculation = remember(selectedChecks, baselineGregorianDate, calculationRevision) {
        calculateMaturity(selectedChecks, baselineGregorianDate)
    }
    val allSelected = eligibleChecks.isNotEmpty() && selectedIds.size == eligibleChecks.size
    val formattedDays = calculation.roundedDays?.let {
        val dayText = IranianPhoneUtils.convertDigitsToPersian(kotlin.math.abs(it).toString())
        when {
            it > 0 -> "$dayText روز بعد"
            it < 0 -> "$dayText روز قبل"
            else -> "همان روز"
        }
    } ?: "—"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("راس‌گیری هوشمند چک‌ها", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Text("محاسبه تاریخ میانگین موزون سررسید چک‌ها",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }

        item {
            MaturityOverviewCard(
                count = selectedChecks.size,
                total = if (calculation.invalidAmount) "—" else formatRials(calculation.totalRials)
            )
        }

        item {
            MaturityResultCard(
                targetDate = calculation.targetDate,
                days = formattedDays,
                total = if (calculation.invalidAmount) "—" else formatRials(calculation.totalRials),
                selectedCount = selectedChecks.size,
                invalidAmount = calculation.invalidAmount
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("تاریخ مبنا", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis = baselineMillis
                            showDatePicker = true
                        }) {
                            Text("تغییر تاریخ")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(17.dp))
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Event, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                PersianCalendarHelper.PersianDateTime(
                                    baselineJalali.first, baselineJalali.second, baselineJalali.third
                                ).toFormattedDate(),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Rounded.ChevronLeft, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("نوع چک‌ها", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = type == CheckType.RECEIVED,
                            onClick = { selectedType = CheckType.RECEIVED.name },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {}
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("چک‌های دریافتی")
                        }
                        SegmentedButton(
                            selected = type == CheckType.PAID,
                            onClick = { selectedType = CheckType.PAID.name },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {}
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("چک‌های پرداختی")
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("انتخاب چک‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${IranianPhoneUtils.convertDigitsToPersian(selectedIds.size.toString())} انتخاب از ${IranianPhoneUtils.convertDigitsToPersian(eligibleChecks.size.toString())} چک",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { selectedIds = if (allSelected) emptySet() else eligibleChecks.map { it.id }.toSet() },
                    enabled = eligibleChecks.isNotEmpty()
                ) {
                    Icon(
                        if (allSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (allSelected) "حذف انتخاب" else "انتخاب همه")
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("جست‌وجوی بانک، شماره چک، مبلغ یا تاریخ…", maxLines = 1) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Rounded.Close, "پاک‌کردن جست‌وجو") } }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                )
            )
        }

        if (eligibleChecks.isEmpty()) {
            item {
                EmptyMaturityState(
                    title = "چک واجد شرایطی پیدا نشد",
                    message = "چک‌های تسویه‌شده، لغوشده و برگشتی در راس‌گیری وارد نمی‌شوند."
                )
            }
        } else if (visibleChecks.isEmpty()) {
            item {
                EmptyMaturityState(
                    title = "نتیجه‌ای پیدا نشد",
                    message = "عبارت جست‌وجو را تغییر بده یا آن را پاک کن."
                )
            }
        } else {
            items(visibleChecks, key = { it.id }) { check ->
                val selected = check.id in selectedIds
                MaturityCheckCard(
                    check = check,
                    selected = selected,
                    onSelectedChange = { checked ->
                        selectedIds = if (checked) selectedIds + check.id else selectedIds - check.id
                    }
                )
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(11.dp),
                            color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(Icons.Rounded.Calculate, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp).size(19.dp))
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("خلاصه محاسبات", fontWeight = FontWeight.Bold)
                            Text("بر اساس میانگین موزون مبلغ و روزهای سررسید",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    SummaryLine("مجموع مبلغ انتخاب‌شده",
                        if (calculation.invalidAmount) "—" else formatRials(calculation.totalRials))
                    SummaryLine("تعداد چک‌های انتخاب‌شده",
                        IranianPhoneUtils.convertDigitsToPersian(selectedChecks.size.toString()))
                    SummaryLine("فاصله تاریخ راس از مبنا", formattedDays)
                    if (calculation.invalidAmount) {
                        Text("مبلغ یکی از چک‌ها به‌دلیل محدودیت واحد پول قابل تبدیل دقیق نیست؛ محاسبه انجام نشد.",
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    } else if (selectedChecks.isEmpty()) {
                        Text("برای شروع، حداقل یک چک را انتخاب کن.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("روز اعشاری حاصل از میانگین موزون به نزدیک‌ترین روز گرد می‌شود.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { calculationRevision++ },
                            shape = RoundedCornerShape(14.dp),
                            enabled = selectedChecks.isNotEmpty() && !calculation.invalidAmount
                        ) {
                            Icon(Icons.Rounded.Calculate, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("محاسبه مجدد")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val shareText = buildString {
                                    append("نتیجه راس‌گیری چک‌ها\n")
                                    append("تاریخ راس: ${calculation.targetDate ?: "محاسبه نشده"}\n")
                                    append("مجموع مبلغ: ${if (calculation.invalidAmount) "نامعتبر" else formatRials(calculation.totalRials)}\n")
                                    append("تعداد چک: ${selectedChecks.size}\n")
                                    append("فاصله از مبنا: $formattedDays")
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "اشتراک نتیجه راس‌گیری"))
                            },
                            shape = RoundedCornerShape(14.dp),
                            enabled = selectedChecks.isNotEmpty() && !calculation.invalidAmount
                        ) {
                            Icon(Icons.Rounded.Share, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("اشتراک نتیجه")
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
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
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("انصراف") } }
        ) { DatePicker(state = datePickerState, showModeToggle = false) }
    }
}

@Composable
private fun MaturityOverviewCard(count: Int, total: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(shape = RoundedCornerShape(11.dp), color = colors.primaryContainer) {
                    Icon(
                        Icons.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
                Text(
                    "مجموع مبلغ چک‌ها",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                Text(
                    total,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
            Box(
                modifier = Modifier.width(1.dp).height(76.dp)
                    .background(colors.outlineVariant.copy(alpha = 0.7f))
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(shape = RoundedCornerShape(11.dp), color = colors.secondaryContainer) {
                    Icon(
                        Icons.Rounded.Numbers,
                        contentDescription = null,
                        tint = colors.secondary,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
                Text(
                    "تعداد چک‌ها",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                Text(
                    IranianPhoneUtils.convertDigitsToPersian(count.toString()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.primary
                )
            }
        }
    }
}

@Composable
private fun MaturityResultCard(
    targetDate: String?,
    days: String,
    total: String,
    selectedCount: Int,
    invalidAmount: Boolean
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colors.primaryContainer.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(10.dp), color = colors.primary.copy(alpha = 0.12f)) {
                    Icon(
                        Icons.Rounded.AutoGraph,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "تاریخ راس چک‌ها",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                    Text(
                        "میانگین موزون سررسید چک‌های انتخاب‌شده",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Rounded.EventAvailable,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                targetDate ?: if (invalidAmount) "محاسبه نامعتبر" else "—",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (invalidAmount) colors.error else colors.primary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResultMetric(Modifier.weight(1f), "فاصله از تاریخ مبنا", days)
                ResultMetric(
                    Modifier.weight(1f),
                    "تعداد چک مؤثر",
                    IranianPhoneUtils.convertDigitsToPersian(selectedCount.toString())
                )
            }
            Text(
                when {
                    invalidAmount -> "به‌دلیل محدودیت تبدیل مبلغ، تاریخ راس قابل محاسبه نیست."
                    selectedCount == 0 -> "با انتخاب چک‌ها، تاریخ راس به‌صورت خودکار محاسبه می‌شود."
                    else -> "مجموع مبلغ مبنای محاسبه: $total"
                },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ResultMetric(modifier: Modifier, label: String, value: String) {
    Surface(modifier, shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun MaturityCheckCard(
    check: Check,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onSelectedChange(!selected) },
        shape = RoundedCornerShape(19.dp),
        color = if (selected) colors.primaryContainer.copy(alpha = 0.48f) else colors.surface,
        border = BorderStroke(1.dp,
            if (selected) colors.primary.copy(alpha = 0.58f) else colors.outlineVariant.copy(alpha = 0.55f))
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Checkbox(checked = selected, onCheckedChange = onSelectedChange)
            Surface(shape = RoundedCornerShape(12.dp),
                color = if (selected) colors.primary.copy(alpha = 0.12f) else colors.surfaceVariant) {
                Icon(Icons.Rounded.AccountBalance, null, tint = colors.primary,
                    modifier = Modifier.padding(9.dp).size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(check.bankName, fontWeight = FontWeight.Bold, maxLines = 1,
                    color = colors.onSurface)
                Text("شماره ${check.checkNumber}", style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.CalendarToday, null, modifier = Modifier.size(13.dp),
                        tint = colors.onSurfaceVariant)
                    Text("سررسید ${check.formattedDueDate}", style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant, maxLines = 1)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(check.formattedAmount, fontWeight = FontWeight.ExtraBold,
                    color = if (selected) colors.primary else colors.onSurface,
                    style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(check.amountUnit.title, style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant)
                Icon(if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    null, tint = if (selected) colors.primary else colors.outlineVariant,
                    modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun EmptyMaturityState(title: String, message: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)) {
        Column(Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.EventBusy, null, modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.Bold)
            Text(message, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatRials(value: BigInteger): String =
    MoneyFormatter.formatExact(MoneyAmount.of(BigDecimal(value), AppCurrency.RIAL))

/**
 * Converts a valid Gregorian civil date to an absolute day number.
 * This avoids depending on Jalali conversion/leap-year arithmetic for day differences.
 */
private fun gregorianEpochDay(year: Int, month: Int, day: Int): Long {
    val adjustedYear = year - if (month <= 2) 1 else 0
    val era = Math.floorDiv(adjustedYear, 400)
    val yearOfEra = adjustedYear - era * 400
    val monthPrime = month + if (month > 2) -3 else 9
    val dayOfYear = (153L * monthPrime + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365L + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146097L + dayOfEra
}

private fun calculateMaturity(
    checks: List<Check>,
    baselineGregorian: Triple<Int, Int, Int>
): MaturityCalculation {
    if (checks.isEmpty()) return MaturityCalculation(BigInteger.ZERO, null, null)

    val baseDay = gregorianEpochDay(
        baselineGregorian.first, baselineGregorian.second, baselineGregorian.third
    )
    var total = BigInteger.ZERO
    var weighted = BigInteger.ZERO

    for (check in checks) {
        val rials = try {
            MoneyAmount.of(check.amount, check.amountUnit).asRialsLongExact()
        } catch (_: ArithmeticException) {
            // Never expose a partial total when any selected check cannot be represented exactly.
            return MaturityCalculation(BigInteger.ZERO, null, null, invalidAmount = true)
        }
        if (rials <= 0L) continue

        // dueDate is a timestamp; interpret its civil date in the same local timezone
        // used by the rest of the app to display check due dates.
        val dueDate = Calendar.getInstance().apply { timeInMillis = check.dueDate }
        val dueDay = gregorianEpochDay(
            dueDate.get(Calendar.YEAR),
            dueDate.get(Calendar.MONTH) + 1,
            dueDate.get(Calendar.DAY_OF_MONTH)
        )
        val days = dueDay - baseDay
        val amount = BigInteger.valueOf(rials)
        total = total.add(amount)
        weighted = weighted.add(amount.multiply(BigInteger.valueOf(days)))
    }

    if (total.signum() <= 0) return MaturityCalculation(total, null, null)

    val division = weighted.divideAndRemainder(total)
    var rounded = division[0]
    // Round half away from zero, consistently for dates before/after the baseline.
    if (division[1].abs().shiftLeft(1) >= total) {
        rounded = rounded.add(BigInteger.valueOf(weighted.signum().toLong()))
    }

    val offset = try {
        rounded.longValueExact()
    } catch (_: ArithmeticException) {
        return MaturityCalculation(BigInteger.ZERO, null, null, invalidAmount = true)
    }

    val target = try {
        val dayOffset = Math.toIntExact(offset)
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                baselineGregorian.first,
                baselineGregorian.second - 1,
                baselineGregorian.third,
                12, 0, 0
            )
            add(Calendar.DAY_OF_MONTH, dayOffset)
        }
    } catch (_: Exception) {
        return MaturityCalculation(BigInteger.ZERO, null, null, invalidAmount = true)
    }

    // Convert only the final Gregorian result for Persian-calendar presentation.
    val jalali = PersianCalendarHelper.gregorianToJalali(
        target.get(Calendar.YEAR),
        target.get(Calendar.MONTH) + 1,
        target.get(Calendar.DAY_OF_MONTH)
    )
    val formatted = PersianCalendarHelper.PersianDateTime(
        jalali.first, jalali.second, jalali.third
    ).toFormattedDate()

    return MaturityCalculation(total, offset, formatted)
}
