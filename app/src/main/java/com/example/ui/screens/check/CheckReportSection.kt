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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.check.components.CheckCard
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckType
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckReportSection(
    checks: List<Check>,
    isLoading: Boolean,
    query: String,
    selectedYear: Int,
    selectedMonth: Int,
    onQueryChange: (String) -> Unit,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onCheckClick: (Check) -> Unit
) {
    val years = CheckReportFilter.availableYears(checks)
    val yearFilter = selectedYear.takeIf { it != 0 }
    val monthFilter = selectedMonth.takeIf { it != 0 && yearFilter != null }
    val results = remember(checks, yearFilter, monthFilter, query) {
        CheckReportFilter.filter(checks, yearFilter, monthFilter, query)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth(),
            singleLine = true, shape = RoundedCornerShape(16.dp),
            placeholder = { Text("جست‌وجو در شماره، بانک، اشخاص، شناسه صیادی...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Rounded.Clear, contentDescription = "پاک کردن جست‌وجو") } }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        )
        Card(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("سال شمسیِ سررسید", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = selectedYear == 0, onClick = { onYearChange(0) }, label = { Text("همه سال‌ها") })
                    years.forEach { year ->
                        FilterChip(
                            selected = selectedYear == year,
                            onClick = { onYearChange(year) },
                            label = { Text(IranianPhoneUtils.convertDigitsToPersian(year.toString())) }
                        )
                    }
                }
                Text("ماه شمسی", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = selectedMonth == 0, onClick = { onMonthChange(0) }, label = { Text("همه ماه‌ها") })
                    CheckReportFilter.monthNames.forEachIndexed { index, name ->
                        FilterChip(selected = selectedMonth == index + 1, enabled = yearFilter != null,
                            onClick = { onMonthChange(index + 1) }, label = { Text(name) })
                    }
                }
                if (yearFilter == null) Text(
                    "برای انتخاب ماه، ابتدا یک سال را انتخاب کنید.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val received = results.filter { it.type == CheckType.RECEIVED }
        val paid = results.filter { it.type == CheckType.PAID }
        if (!isLoading) {
        Card(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("خلاصه نتایج فیلترشده", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("تعداد کل: ${IranianPhoneUtils.convertDigitsToPersian(results.size.toString())} چک")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("دریافتی (${received.size})", color = MaterialTheme.colorScheme.primary)
                    Text(MoneyFormatter.format(received.sumOf { it.amount }), fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("پرداختی (${paid.size})", color = MaterialTheme.colorScheme.error)
                    Text(MoneyFormatter.format(paid.sumOf { it.amount }), fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "مبالغ بالا جمع مبلغ اسمی چک‌های ثبت‌شده در نتایج است؛ به معنی مانده حساب، گردش قطعی یا مبلغ وصول‌شده نیست.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }
        Text(
            text = if (yearFilter == null) "همه سوابق بر اساس تاریخ سررسید" else
                "سررسیدهای ${IranianPhoneUtils.convertDigitsToPersian(yearFilter.toString())}" +
                    (monthFilter?.let { " - ${CheckReportFilter.monthNames[it - 1]}" } ?: ""),
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold
        )
        when {
            isLoading -> androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) { CircularProgressIndicator() }
            results.isEmpty() -> Text(
                if (checks.isEmpty()) "هنوز چکی برای گزارش ثبت نشده است."
                else "با این سال، ماه یا عبارت جست‌وجو چکی پیدا نشد.",
                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> results.forEach { check -> CheckCard(check = check, onClick = { onCheckClick(check) }) }
        }
        Spacer(Modifier.height(72.dp))
    }
}
