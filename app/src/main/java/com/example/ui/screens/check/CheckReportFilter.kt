package com.example.ui.screens.check

import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType
import com.example.util.PersianCalendarHelper
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency
import java.util.Locale

/** Aggregated nominal values; these are not settlement or account-balance figures. */
data class CheckReportStatusTotal(
    val type: CheckType,
    val status: CheckStatus,
    val count: Int,
    val amount: Long
)

data class CheckReportSummary(
    val totalCount: Int,
    val totalNominalAmount: Long,
    val statusTotals: List<CheckReportStatusTotal>
)

/** Filters the user's persisted, non-deleted check records by their Jalali due date. */
object CheckReportFilter {
    val monthNames: List<String> = PersianCalendarHelper.PERSIAN_MONTH_NAMES

    fun availableYears(checks: List<Check>, type: CheckType? = null): List<Int> =
        checks.asSequence()
            .filter { type == null || it.type == type }
            .map { PersianCalendarHelper.fromEpochMillis(it.dueDate).year }
            .plus(PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).year)
            .distinct()
            .sortedDescending()
            .toList()

    /**
     * A month is meaningful only within a selected Jalali year. Invalid or incomplete
     * date filters return no records rather than silently broadening the report.
     */
    fun filter(
        checks: List<Check>,
        year: Int?,
        month: Int?,
        query: String,
        type: CheckType? = null
    ): List<Check> {
        if (month != null && (year == null || month !in 1..12)) return emptyList()
        if (year != null && year <= 0) return emptyList()

        val normalizedQuery = normalize(query)
        return checks.asSequence()
            .filter { check -> type == null || check.type == type }
            .filter { check ->
                val due = PersianCalendarHelper.fromEpochMillis(check.dueDate)
                (year == null || due.year == year) &&
                    (month == null || due.month == month)
            }
            .filter { check ->
                normalizedQuery.isBlank() ||
                    searchableFields(check).any { normalize(it).contains(normalizedQuery) }
            }
            .sortedWith(compareByDescending<Check> { it.dueDate }.thenByDescending { it.id })
            .toList()
    }

    /** Breaks nominal totals down by both direction and persisted check status. */
    fun summarize(checks: List<Check>): CheckReportSummary {
        // Keep replaced records visible in the report list for audit, but never count
        // the old instrument again in nominal totals after its replacement is registered.
        val countedChecks = checks.filter { it.status != CheckStatus.REPLACED }
        val statusTotals = CheckType.values().flatMap { type ->
            CheckStatus.values().mapNotNull { status ->
                val matching = countedChecks.filter { it.type == type && it.status == status }
                if (matching.isEmpty()) null
                else CheckReportStatusTotal(
                    type = type,
                    status = status,
                    count = matching.size,
                    amount = matching.fold(0L) { total, check -> Math.addExact(total, MoneyAmount.of(check.amount, check.amountUnit).asRialsLongExact()) }
                )
            }
        }
        return CheckReportSummary(
            totalCount = countedChecks.size,
            totalNominalAmount = countedChecks.fold(0L) { total, check -> Math.addExact(total, MoneyAmount.of(check.amount, check.amountUnit).asRialsLongExact()) },
            statusTotals = statusTotals
        )
    }

    private fun searchableFields(check: Check): List<String> = listOfNotNull(
        check.checkNumber,
        check.sayadId,
        check.bankName,
        check.branch,
        check.holderName,
        check.receiverName,
        check.notes,
        check.type.title,
        check.status.title
    )

    private fun normalize(value: String): String = buildString(value.length) {
        value.trim().lowercase(Locale.ROOT).forEach { char ->
            append(
                when (char) {
                    in '۰'..'۹' -> ('0'.code + char.code - '۰'.code).toChar()
                    in '٠'..'٩' -> ('0'.code + char.code - '٠'.code).toChar()
                    'ي', 'ى' -> 'ی'
                    'ك' -> 'ک'
                    else -> char
                }
            )
        }
    }
}
