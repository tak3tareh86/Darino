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
import com.example.ui.components.SharedPersianDatePickerDialog
import com.example.ui.screens.check.components.CheckBankLogo
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
    val invalidAmount: Boolean = false,
    val targetWeekday: String? = null
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
                it.status != CheckStatus.REPLACED
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
    var showPersianDatePicker by remember { mutableStateOf(false) }
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            MaturityOverviewCard(
                count = selectedChecks.size,
                total = if (calculation.invalidAmount) "—" else formatRials(calculation.totalRials),
                targetDate = calculation.targetDate,
                days = formattedDays,
                weekday = calculation.targetWeekday
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MaturityTypePill(
                    modifier = Modifier.weight(1f),
                    title = "چک‌های پرداختی",
                    selected = type == CheckType.PAID,
                    icon = Icons.Rounded.ArrowUpward,
                    onClick = { selectedType = CheckType.PAID.name }
                )
                MaturityTypePill(
                    modifier = Modifier.weight(1f),
                    title = "چک‌های دریافتی",
                    selected = type == CheckType.RECEIVED,
                    icon = Icons.Rounded.ArrowDownward,
                    onClick = { selectedType = CheckType.RECEIVED.name }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "تاریخ مبنا:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                ) {
                    Text(
                        PersianCalendarHelper.PersianDateTime(
                            baselineJalali.first, baselineJalali.second, baselineJalali.third
                        ).toFormattedDate() + if (baselineMillis == todayMillis) " (امروز)" else "",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 9.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(
                    onClick = { showPersianDatePicker = true },
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.EditCalendar, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("تغییر تاریخ", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f).height(48.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = {
                        Text(
                            "جست و جو",
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                        )
                    },
                    trailingIcon = { Icon(Icons.Rounded.Search, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                )
                OutlinedButton(
                    modifier = Modifier.height(48.dp),
                    onClick = { selectedIds = if (allSelected) emptySet() else eligibleChecks.map { it.id }.toSet() },
                    enabled = eligibleChecks.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.SelectAll, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("انتخاب همه", maxLines = 1)
                }
                OutlinedButton(
                    modifier = Modifier.height(48.dp),
                    onClick = { selectedIds = emptySet() },
                    enabled = selectedIds.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("پاک کردن", maxLines = 1)
                }
            }
        }

        if (eligibleChecks.isEmpty()) {
            item {
                EmptyMaturityState(
                    title = "چک واجد شرایطی پیدا نشد",
                    message = "چک‌های وصول‌شده و لغوشده در راس‌گیری وارد نمی‌شوند؛ چک‌های برگشتیِ تسویه‌نشده محاسبه می‌شوند."
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
                MaturityCheckCard(
                    check = check,
                    selected = check.id in selectedIds,
                    onSelectedChange = { checked ->
                        selectedIds = if (checked) selectedIds + check.id else selectedIds - check.id
                    }
                )
            }
        }

        item {
            MaturitySummaryCard(
                total = if (calculation.invalidAmount) "—" else formatRials(calculation.totalRials),
                days = formattedDays,
                invalidAmount = calculation.invalidAmount
            )
        }

        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    modifier = Modifier.width(220.dp).height(58.dp),
                    onClick = { calculationRevision++ },
                    enabled = selectedChecks.isNotEmpty() && !calculation.invalidAmount,
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Rounded.Calculate, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("محاسبه مجدد", fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }

    if (showPersianDatePicker) {
        SharedPersianDatePickerDialog(
            initialYear = baselineJalali.first,
            initialMonth = baselineJalali.second,
            initialDay = baselineJalali.third,
            title = "انتخاب تاریخ مبنا (شمسی)",
            onDismiss = { showPersianDatePicker = false },
            onConfirm = { year, month, day ->
                val (gy, gm, gd) = PersianCalendarHelper.jalaliToGregorian(year, month, day)
                baselineMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(gy, gm - 1, gd, 0, 0, 0)
                }.timeInMillis
            }
        )
    }
}

@Composable
private fun MaturityOverviewCard(
    count: Int,
    total: String,
    targetDate: String?,
    days: String,
    weekday: String?
) {
    val colors = MaterialTheme.colorScheme
    val gradient = Brush.linearGradient(
        listOf(colors.primary, colors.tertiary, colors.secondary)
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .background(gradient)
                .padding(horizontal = 11.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right section: keep all calculation metrics right-aligned.
            Column(
                modifier = Modifier.weight(1.6f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OverviewMetricRow(
                    label = "تعداد چک‌ها",
                    value = IranianPhoneUtils.convertDigitsToPersian(count.toString())
                )
                OverviewMetricRow(label = "مجموع مبلغ", value = total)
                OverviewMetricRow(label = "میانگین فاصله", value = days)
            }

            Box(
                Modifier
                    .padding(horizontal = 9.dp)
                    .width(1.dp)
                    .height(76.dp)
                    .background(Color.White.copy(alpha = 0.42f))
            )

            // Center section: date and weekday remain centered.
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "تاریخ راس",
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    targetDate ?: "—",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    weekday ?: "—",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun OverviewMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.88f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Spacer(Modifier.width(5.dp))
        Text(
            value,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

@Composable
private fun MaturityTypePill(
    modifier: Modifier,
    title: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.height(48.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = if (selected) colors.primary else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant)
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = colors.onPrimary, modifier = Modifier.size(19.dp))
            Icon(icon, null, tint = if (selected) colors.onPrimary else colors.primary,
                modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(7.dp))
            Text(title, color = if (selected) colors.onPrimary else colors.onSurface,
                fontWeight = FontWeight.Bold, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MaturitySummaryCard(total: String, days: String, invalidAmount: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        color = colors.primaryContainer.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Center the section label on the card's top edge, with the border
            // continuing visibly on both sides of the label.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(Modifier.weight(1f).height(1.dp).background(colors.primary.copy(alpha = 0.45f)))
                Surface(
                    modifier = Modifier.padding(horizontal = 9.dp),
                    color = colors.primary,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "خلاصه محاسبات",
                        Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                        color = colors.onPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Box(Modifier.weight(1f).height(1.dp).background(colors.primary.copy(alpha = 0.45f)))
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryMetric(
                    Modifier.weight(1f),
                    "فاصله میانگین از مبنا",
                    days,
                    Icons.Rounded.Schedule
                )
                Box(
                    Modifier.width(1.dp).height(54.dp)
                        .background(colors.outlineVariant)
                )
                SummaryMetric(
                    Modifier.weight(1f),
                    "مجموع مبالغ انتخاب‌شده",
                    total,
                    Icons.Rounded.Paid
                )
            }
            if (invalidAmount) {
                Text(
                    "مبلغ یکی از چک‌ها قابل تبدیل دقیق نیست؛ محاسبه انجام نشد.",
                    Modifier.padding(horizontal = 14.dp),
                    color = colors.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    modifier: Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(modifier.padding(horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(2.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
        }
        Text(value, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 2)
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
private fun MaturityCheckCard(check: Check, selected: Boolean, onSelectedChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val amountFontSize = when {
        check.formattedAmount.length >= 18 -> 9.sp
        check.formattedAmount.length >= 15 -> 10.sp
        check.formattedAmount.length >= 12 -> 12.sp
        check.formattedAmount.length >= 10 -> 14.sp
        else -> MaterialTheme.typography.titleMedium.fontSize
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onSelectedChange(!selected) },
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.primary.copy(alpha = 0.55f)
            else colors.outlineVariant.copy(alpha = 0.65f))
    ) {
        Row(Modifier.heightIn(min = 76.dp), verticalAlignment = Alignment.CenterVertically) {
            // مستقل نگه‌داشتن فضای چک‌باکس از فضای اطلاعات چک.
            Checkbox(
                checked = selected,
                onCheckedChange = onSelectedChange,
                modifier = Modifier.padding(horizontal = 0.dp)
            )
            // خط جداکننده کمی به چک‌باکس نزدیک‌تر شده تا فضای اطلاعات بیشتر شود.
            Box(
                Modifier.offset(x = 3.dp).width(1.dp).height(58.dp)
                    .background(colors.outlineVariant)
            )

            // همه اطلاعات چک در یک بخش مشترک قرار دارند؛ مبلغ عرض موردنیازش
            // را می‌گیرد و ستون مشخصات، فضای باقی‌مانده را استفاده می‌کند.
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .padding(start = 6.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckBankLogo(
                    bankName = check.bankName,
                    modifier = Modifier.padding(end = 5.dp).size(42.dp)
                )
                Column(
                    Modifier.weight(1f).padding(horizontal = 3.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        check.bankName,
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Start,
                        maxLines = 1
                    )
                    Text(
                        "شماره: ${check.checkNumber}",
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start,
                        maxLines = 1
                    )
                    Text(
                        "سررسید: ${check.formattedDueDate}",
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start,
                        maxLines = 1
                    )
                }
                Column(
                    modifier = Modifier.wrapContentWidth().padding(start = 4.dp, end = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        MoneyFormatter.formatExact(
                            MoneyAmount.of(check.amount, check.amountUnit),
                            includeUnit = false
                        ),
                        color = if (selected) colors.primary else colors.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = amountFontSize),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        MoneyFormatter.getUnitLabel(),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
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

    val weekday = when (target.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SATURDAY -> "شنبه"
        Calendar.SUNDAY -> "یکشنبه"
        Calendar.MONDAY -> "دوشنبه"
        Calendar.TUESDAY -> "سه‌شنبه"
        Calendar.WEDNESDAY -> "چهارشنبه"
        Calendar.THURSDAY -> "پنجشنبه"
        Calendar.FRIDAY -> "جمعه"
        else -> null
    }

    return MaturityCalculation(total, offset, formatted, targetWeekday = weekday)
}
