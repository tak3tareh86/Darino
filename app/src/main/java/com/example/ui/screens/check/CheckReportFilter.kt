package com.example.ui.screens.check

import com.example.ui.screens.check.model.Check
import com.example.util.PersianCalendarHelper
import java.util.Locale

/** Filters real, non-deleted check records by due date in the device's local timezone. */
object CheckReportFilter {
    val monthNames: List<String> = PersianCalendarHelper.PERSIAN_MONTH_NAMES
    fun availableYears(checks: List<Check>): List<Int> =
        checks.map { PersianCalendarHelper.fromEpochMillis(it.dueDate).year }.distinct().sortedDescending()

    fun filter(checks: List<Check>, year: Int?, month: Int?, query: String): List<Check> {
        val normalizedQuery = normalize(query)
        return checks.asSequence()
            .filter { check ->
                val due = PersianCalendarHelper.fromEpochMillis(check.dueDate)
                (year == null || due.year == year) && (year == null || month == null || due.month == month)
            }
            .filter { check -> normalizedQuery.isBlank() || searchableFields(check).any { normalize(it).contains(normalizedQuery) } }
            .sortedWith(compareByDescending<Check> { it.dueDate }.thenByDescending { it.id })
            .toList()
    }

    private fun searchableFields(check: Check) = listOfNotNull(
        check.checkNumber, check.sayadId, check.bankName, check.branch,
        check.holderName, check.receiverName, check.notes, check.type.title, check.status.title
    )

    private fun normalize(value: String): String = buildString(value.length) {
        value.trim().lowercase(Locale.ROOT).forEach { char ->
            append(when (char) {
                in '۰'..'۹' -> ('0'.code + char.code - '۰'.code).toChar()
                in '٠'..'٩' -> ('0'.code + char.code - '٠'.code).toChar()
                'ي' -> 'ی'
                'ك' -> 'ک'
                else -> char
            })
        }
    }
}
