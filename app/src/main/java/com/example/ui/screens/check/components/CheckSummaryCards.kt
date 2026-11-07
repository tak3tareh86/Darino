package com.example.ui.screens.check.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.domain.CalendarDateUtils
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType
import com.example.util.IranianPhoneUtils
import com.example.util.PersianCalendarHelper
import com.example.util.MoneyFormatter
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency

@Composable
fun CheckSummaryCards(checks: List<Check>, modifier: Modifier = Modifier) {
    // Keep the existing business calculations unchanged; this component only changes presentation.
    val totalCount = checks.size
    val totalAmount = checks.fold(0L) { total, check -> Math.addExact(total, MoneyAmount.of(check.amount, check.amountUnit).asRialsLongExact()) }
    val (curYear, curMonth, _) = CalendarDateUtils.getCurrentJalaliDate()
    val startMonthMillis = PersianCalendarHelper.jalaliToEpochMillis(curYear, curMonth, 1, 0, 0)
    val (nextYear, nextMonth) = if (curMonth < 12) Pair(curYear, curMonth + 1) else Pair(curYear + 1, 1)
    val endMonthMillis = PersianCalendarHelper.jalaliToEpochMillis(nextYear, nextMonth, 1, 0, 0)
    val monthChecks = checks.filter {
        it.dueDate in startMonthMillis until endMonthMillis &&
            (it.status == CheckStatus.REGISTERED || it.status == CheckStatus.PENDING || it.status == CheckStatus.BOUNCED)
    }
    val receivedMonthAmount = monthChecks.filter { it.type == CheckType.RECEIVED }.fold(0L) { total, check -> Math.addExact(total, MoneyAmount.of(check.amount, check.amountUnit).asRialsLongExact()) }
    val receivedMonthCount = monthChecks.count { it.type == CheckType.RECEIVED }
    val paidMonthAmount = monthChecks.filter { it.type == CheckType.PAID }.fold(0L) { total, check -> Math.addExact(total, MoneyAmount.of(check.amount, check.amountUnit).asRialsLongExact()) }
    val paidMonthCount = monthChecks.count { it.type == CheckType.PAID }
    val bouncedMonthAmount = monthChecks.filter { it.status == CheckStatus.BOUNCED }.fold(0L) { total, check -> Math.addExact(total, MoneyAmount.of(check.amount, check.amountUnit).asRialsLongExact()) }
    val monthName = PersianCalendarHelper.PERSIAN_MONTH_NAMES.getOrElse(curMonth - 1) { "" }
    val countLabel = remember(totalCount) { IranianPhoneUtils.convertDigitsToPersian(totalCount.toString()) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // In RTL layout this first card appears on the right, matching the reference composition.
        MonthChecksCard(
            modifier = Modifier.weight(1f),
            monthName = monthName,
            monthCount = monthChecks.size,
            receivedAmount = receivedMonthAmount,
            receivedCount = receivedMonthCount,
            paidAmount = paidMonthAmount,
            paidCount = paidMonthCount,
            bouncedAmount = bouncedMonthAmount
        )
        TotalChecksCard(
            modifier = Modifier.weight(1f),
            totalCount = totalCount,
            countLabel = countLabel,
            totalAmount = totalAmount
        )
    }
}

@Composable
private fun TotalChecksCard(
    modifier: Modifier,
    totalCount: Int,
    countLabel: String,
    totalAmount: Long
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 9.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "تعداد کل چک‌ها",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        countLabel,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "مجموع مبلغ ثبت‌شده",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                CompactCheckAmountText(totalAmount, MaterialTheme.colorScheme.onSurface, Modifier.fillMaxWidth(), 15f)
            }
        }
    }
}

@Composable
private fun MonthChecksCard(
    modifier: Modifier,
    monthName: String,
    monthCount: Int,
    receivedAmount: Long,
    receivedCount: Int,
    paidAmount: Long,
    paidCount: Int,
    bouncedAmount: Long
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Compact title row keeps the month and heading on one line.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "چک‌های ماه جاری",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    monthName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            MonthAmountRow(
                title = "دریافتی",
                amount = receivedAmount,
                count = receivedCount,
                amountColor = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            MonthAmountRow(
                title = "پرداختی",
                amount = paidAmount,
                count = paidCount,
                amountColor = MaterialTheme.colorScheme.error
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            if (bouncedAmount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "برگشتی",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    CompactCheckAmountText(
                        bouncedAmount,
                        MaterialTheme.colorScheme.error,
                        Modifier.weight(1.7f),
                        11f
                    )
                }
            } else {
                Text(
                    "${IranianPhoneUtils.convertDigitsToPersian(monthCount.toString())} چک فعال",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MonthAmountRow(
    title: String,
    amount: Long,
    count: Int,
    amountColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = amountColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier.weight(2.2f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            CompactCheckAmountText(
                amount,
                amountColor,
                Modifier.fillMaxWidth(),
                12f
            )
            Text(
                "${IranianPhoneUtils.convertDigitsToPersian(count.toString())} فقره",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CompactCheckAmountText(
    amount: Long,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    maximumFontSize: Float
) {
    val formatted = MoneyFormatter.formatExact(MoneyAmount.of(amount, AppCurrency.RIAL))
    BoxWithConstraints(modifier = modifier) {
        val adaptiveFontSize = (maxWidth.value / (formatted.length.coerceAtLeast(1) * 0.58f))
            .coerceIn(8f, maximumFontSize)
        Text(
            text = formatted,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = adaptiveFontSize.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
    }
}
