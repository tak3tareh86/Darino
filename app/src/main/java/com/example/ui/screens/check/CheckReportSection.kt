package com.example.ui.screens.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.check.components.CheckCard
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckType
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency

/**
 * Adds report controls and results directly to the screen's parent LazyColumn.
 * This keeps every check card lazy instead of composing the entire history in one item.
 */
@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.checkReportSection(
    results: List<Check>,
    availableYears: List<Int>,
    summary: CheckReportSummary,
    hasAnyChecks: Boolean,
    isLoading: Boolean,
    selectedYear: Int,
    selectedMonth: Int,
    selectedType: CheckType?,
    query: String,
    onQueryChange: (String) -> Unit,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onTypeChange: (CheckType?) -> Unit,
    onCheckClick: (Check) -> Unit
) {
    item(key = "check-report-direction-filter") {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            )
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("نوع چک برای گزارش", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { onTypeChange(null) },
                        label = { Text("همه چک‌ها") }
                    )
                    FilterChip(
                        selected = selectedType == CheckType.RECEIVED,
                        onClick = { onTypeChange(CheckType.RECEIVED) },
                        label = { Text("چک‌های دریافتی") }
                    )
                    FilterChip(
                        selected = selectedType == CheckType.PAID,
                        onClick = { onTypeChange(CheckType.PAID) },
                        label = { Text("چک‌های پرداختی") }
                    )
                }
                Text(
                    when (selectedType) {
                        CheckType.RECEIVED -> "جست‌وجو، فیلتر تاریخ و جمع مبالغ فقط روی چک‌های دریافتی اعمال می‌شود."
                        CheckType.PAID -> "جست‌وجو، فیلتر تاریخ و جمع مبالغ فقط روی چک‌های پرداختی اعمال می‌شود."
                        null -> "گزارش همه چک‌ها؛ دریافتی و پرداختی در خلاصه به تفکیک وضعیت نمایش داده می‌شوند."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    item(key = "check-report-search") {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("جست‌وجو در شماره، بانک، اشخاص، شناسه صیادی...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "پاک کردن جست‌وجو")
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        )
    }

    item(key = "check-report-date-filters") {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            )
        ) {
            var yearMenuExpanded by remember { mutableStateOf(false) }
            var monthMenuExpanded by remember { mutableStateOf(false) }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { yearMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Text("سال سررسید", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    if (selectedYear == 0) "همه سال‌ها"
                                    else IranianPhoneUtils.convertDigitsToPersian(selectedYear.toString()),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "انتخاب سال")
                        }
                        DropdownMenu(
                            expanded = yearMenuExpanded,
                            onDismissRequest = { yearMenuExpanded = false }
                        ) {
                            Column(
                                Modifier.width(280.dp).heightIn(max = 280.dp)
                                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                    .padding(8.dp)
                            ) {
                                FlowRow(
                                    maxItemsInEachRow = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    (listOf(0) + availableYears).forEach { year ->
                                        val label = if (year == 0) "همه سال‌ها"
                                            else IranianPhoneUtils.convertDigitsToPersian(year.toString())
                                        TextButton(
                                            onClick = { onYearChange(year); yearMenuExpanded = false },
                                            modifier = Modifier.width(86.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                label,
                                                maxLines = 1,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (selectedYear == year) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedYear == year) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { if (selectedYear != 0) monthMenuExpanded = true },
                            enabled = selectedYear != 0,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Text("ماه سررسید", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    if (selectedMonth == 0) "همه ماه‌ها" else CheckReportFilter.monthNames[selectedMonth - 1],
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "انتخاب ماه")
                        }
                        DropdownMenu(
                            expanded = monthMenuExpanded,
                            onDismissRequest = { monthMenuExpanded = false }
                        ) {
                            Column(Modifier.width(300.dp).padding(8.dp)) {
                                TextButton(
                                    onClick = { onMonthChange(0); monthMenuExpanded = false },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text(
                                        "همه ماه‌ها",
                                        color = if (selectedMonth == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                FlowRow(
                                    maxItemsInEachRow = 4,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CheckReportFilter.monthNames.forEachIndexed { index, name ->
                                        TextButton(
                                            onClick = { onMonthChange(index + 1); monthMenuExpanded = false },
                                            modifier = Modifier.width(68.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                name,
                                                maxLines = 1,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (selectedMonth == index + 1) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedMonth == index + 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (selectedYear == 0) {
                    Text(
                        "برای انتخاب ماه، ابتدا یک سال را انتخاب کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (!isLoading && results.isNotEmpty()) {
        item(key = "check-report-summary") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "خلاصه مبالغ اسمی چک‌ها",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("تعداد کل: ${IranianPhoneUtils.convertDigitsToPersian(summary.totalCount.toString())} چک")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("جمع مبلغ اسمی همه وضعیت‌ها", fontWeight = FontWeight.SemiBold)
                        Text(MoneyFormatter.formatExact(MoneyAmount.of(summary.totalNominalAmount, AppCurrency.RIAL)), fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                    Text("تفکیک بر اساس نوع و وضعیت", style = MaterialTheme.typography.labelLarge)
                    summary.statusTotals.forEach { total ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "${total.type.title} · ${total.status.title} (${IranianPhoneUtils.convertDigitsToPersian(total.count.toString())})",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (total.type == CheckType.RECEIVED) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                            Text(MoneyFormatter.formatExact(MoneyAmount.of(total.amount, AppCurrency.RIAL)), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        "این ارقام جمع مبلغ اسمی چک‌های ثبت‌شده‌اند و وضعیت هر چک جداگانه مشخص شده است؛ این مبالغ به معنی مانده حساب، وجه وصول‌شده یا تعهد فعال نیستند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    item(key = "check-report-results-heading") {
        Text(
            text = when {
                selectedYear == 0 -> "همه سوابق بر اساس تاریخ سررسید"
                selectedMonth == 0 -> "سررسیدهای ${IranianPhoneUtils.convertDigitsToPersian(selectedYear.toString())}"
                else -> "سررسیدهای ${CheckReportFilter.monthNames[selectedMonth - 1]} ${IranianPhoneUtils.convertDigitsToPersian(selectedYear.toString())}"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }

    when {
        isLoading -> item(key = "check-report-loading") {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) { CircularProgressIndicator() }
        }
        results.isEmpty() -> item(key = "check-report-empty") {
            Text(
                if (!hasAnyChecks) "هنوز چکی برای گزارش ثبت نشده است."
                else "با این سال، ماه یا عبارت جست‌وجو چکی پیدا نشد.",
                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> items(results, key = { "check-report-${it.id}" }) { check ->
            CheckCard(check = check, onClick = { onCheckClick(check) })
        }
    }

    item(key = "check-report-bottom-space") {
        Spacer(Modifier.height(72.dp))
    }
}
