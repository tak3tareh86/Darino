package com.example.ui.screens.check.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    val bouncedChecks = monthChecks.filter { it.status == CheckStatus.BOUNCED }
    val bouncedMonthAmount = bouncedChecks.fold(0L) { total, check -> Math.addExact(total, MoneyAmount.of(check.amount, check.amountUnit).asRialsLongExact()) }
    val bouncedMonthCount = bouncedChecks.size
    val monthName = PersianCalendarHelper.PERSIAN_MONTH_NAMES.getOrElse(curMonth - 1) { "" }
    val countLabel = remember(totalCount) { IranianPhoneUtils.convertDigitsToPersian(totalCount.toString()) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MonthChecksCard(
            modifier = Modifier.fillMaxWidth(),
            monthName = monthName,
            monthCount = monthChecks.size,
            receivedAmount = receivedMonthAmount,
            receivedCount = receivedMonthCount,
            paidAmount = paidMonthAmount,
            paidCount = paidMonthCount,
            bouncedAmount = bouncedMonthAmount,
            bouncedCount = bouncedMonthCount
        )
        TotalChecksCard(
            modifier = Modifier.fillMaxWidth(),
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(
                Icons.Rounded.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                "تعداد کل چک‌ها",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                countLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.width(1.dp).height(26.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    "مجموع مبلغ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                CompactCheckAmountText(
                    totalAmount,
                    MaterialTheme.colorScheme.onSurface,
                    Modifier.widthIn(min = 70.dp, max = 132.dp),
                    13f
                )
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
    bouncedAmount: Long,
    bouncedCount: Int
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = scheme.primary,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = scheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "چک‌های ماه جاری",
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        monthName,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(8.dp))
                    HorizontalDivider(
                        modifier = Modifier.width(1.dp).height(30.dp),
                        color = scheme.outlineVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        androidx.compose.material.icons.Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
            MonthAmountRow(
                title = "دریافتی",
                amount = receivedAmount,
                count = receivedCount,
                amountColor = scheme.primary,
                rowColor = scheme.primaryContainer.copy(alpha = 0.42f)
            )
            MonthAmountRow(
                title = "پرداختی",
                amount = paidAmount,
                count = paidCount,
                amountColor = scheme.error,
                rowColor = scheme.errorContainer.copy(alpha = 0.48f)
            )
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.7f))
            if (bouncedCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${IranianPhoneUtils.convertDigitsToPersian(bouncedCount.toString())} فقره",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(10.dp))
                    CompactCheckAmountText(
                        bouncedAmount,
                        scheme.error,
                        Modifier.weight(1.25f),
                        12f
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "برگشتی / فعال",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            } else {
                Text(
                    "${IranianPhoneUtils.convertDigitsToPersian(monthCount.toString())} چک فعال",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
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
    amountColor: androidx.compose.ui.graphics.Color,
    rowColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = rowColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // RTL: count on the right, amount in the middle, category on the left.
            Text(
                "${IranianPhoneUtils.convertDigitsToPersian(count.toString())} فقره",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.width(8.dp))
            CompactCheckAmountText(
                amount,
                MaterialTheme.colorScheme.onSurface,
                Modifier.weight(1f),
                14f
            )
            Spacer(Modifier.width(8.dp))
            HorizontalDivider(
                modifier = Modifier.width(1.dp).height(34.dp),
                color = amountColor.copy(alpha = 0.42f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = amountColor,
                fontWeight = FontWeight.Bold,
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
