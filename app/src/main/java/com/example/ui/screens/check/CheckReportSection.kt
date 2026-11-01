package com.example.ui.screens.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.check.components.CheckCard
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckType
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter

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
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("سال شمسیِ سررسید", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedYear == 0,
                        onClick = { onYearChange(0) },
                        label = { Text("همه سال‌ها") }
                    )
                    availableYears.forEach { year ->
                        FilterChip(
                            selected = selectedYear == year,
                            onClick = { onYearChange(year) },
                            label = { Text(IranianPhoneUtils.convertDigitsToPersian(year.toString())) }
                        )
                    }
                }

                Text("ماه شمسی", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedMonth == 0,
                        onClick = { onMonthChange(0) },
                        label = { Text("همه ماه‌ها") }
                    )
                    CheckReportFilter.monthNames.forEachIndexed { index, name ->
                        FilterChip(
                            selected = selectedMonth == index + 1,
                            enabled = selectedYear != 0,
                            onClick = { onMonthChange(index + 1) },
                            label = { Text(name) }
                        )
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
                        Text(MoneyFormatter.format(summary.totalNominalAmount), fontWeight = FontWeight.Bold)
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
                            Text(MoneyFormatter.format(total.amount), style = MaterialTheme.typography.bodySmall)
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
