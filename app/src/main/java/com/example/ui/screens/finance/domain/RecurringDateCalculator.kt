package com.example.ui.screens.finance.domain

import com.example.calendar.domain.CalendarDateUtils
import com.example.ui.screens.finance.model.RecurringFrequency

/**
 * Jalali recurrence arithmetic that preserves the original day/month when a target
 * month is shorter (for example, day 31 returns to day 31 after a 30-day month).
 */
object RecurringDateCalculator {

    fun nextDate(
        currentDate: String,
        frequency: RecurringFrequency,
        anchorDayOfMonth: Int = 0,
        anchorMonthOfYear: Int = 0
    ): String? {
        if (!CalendarDateUtils.isValidJalaliDate(currentDate)) return null
        val (year, month, day) = CalendarDateUtils.parseJalali(currentDate) ?: return null
        val anchorDay = anchorDayOfMonth.takeIf { it in 1..31 } ?: day
        val anchorMonth = anchorMonthOfYear.takeIf { it in 1..12 } ?: month

        val next = when (frequency) {
            RecurringFrequency.DAILY -> addDays(year, month, day, 1)
            RecurringFrequency.WEEKLY -> addDays(year, month, day, 7)
            RecurringFrequency.MONTHLY -> {
                val nextYear = if (month == 12) year + 1 else year
                val nextMonth = if (month == 12) 1 else month + 1
                Triple(nextYear, nextMonth, anchorDay.coerceAtMost(CalendarDateUtils.getDaysInJalaliMonth(nextYear, nextMonth)))
            }
            RecurringFrequency.YEARLY -> {
                val nextYear = year + 1
                Triple(
                    nextYear,
                    anchorMonth,
                    anchorDay.coerceAtMost(CalendarDateUtils.getDaysInJalaliMonth(nextYear, anchorMonth))
                )
            }
        }

        return CalendarDateUtils.toPersianDisplay(
            CalendarDateUtils.formatJalali(next.first, next.second, next.third)
        )
    }

    private fun addDays(year: Int, month: Int, day: Int, daysToAdd: Int): Triple<Int, Int, Int> {
        var y = year
        var m = month
        var d = day
        repeat(daysToAdd) {
            d++
            if (d > CalendarDateUtils.getDaysInJalaliMonth(y, m)) {
                d = 1
                m++
                if (m > 12) {
                    m = 1
                    y++
                }
            }
        }
        return Triple(y, m, d)
    }
}
