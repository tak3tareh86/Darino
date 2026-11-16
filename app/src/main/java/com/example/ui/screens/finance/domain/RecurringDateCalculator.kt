package com.example.ui.screens.finance.domain

import com.example.calendar.domain.CalendarDateUtils
import com.example.ui.screens.finance.model.RecurringFrequency
import com.example.util.PersianCalendarHelper

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

    /**
     * Returns the first scheduled date strictly after referenceDate.
     * Missed periods are skipped in one calculation instead of generating a backlog
     * of old notifications across repeated worker runs.
     */
    fun firstDateAfter(
        currentDate: String,
        frequency: RecurringFrequency,
        anchorDayOfMonth: Int,
        anchorMonthOfYear: Int,
        referenceDate: String
    ): String? {
        if (!CalendarDateUtils.isValidJalaliDate(currentDate) ||
            !CalendarDateUtils.isValidJalaliDate(referenceDate)
        ) return null

        val current = CalendarDateUtils.parseJalali(currentDate) ?: return null
        val reference = CalendarDateUtils.parseJalali(referenceDate) ?: return null
        val currentEpoch = PersianCalendarHelper.getJalaliEpochDay(current.first, current.second, current.third)
        val referenceEpoch = PersianCalendarHelper.getJalaliEpochDay(reference.first, reference.second, reference.third)
        val anchorDay = anchorDayOfMonth.takeIf { it in 1..31 } ?: current.third
        val anchorMonth = anchorMonthOfYear.takeIf { it in 1..12 } ?: current.second

        val next = when (frequency) {
            RecurringFrequency.DAILY -> {
                val days = (referenceEpoch - currentEpoch + 1L).coerceAtLeast(1L)
                addDays(current.first, current.second, current.third, days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            }
            RecurringFrequency.WEEKLY -> {
                val missedDays = (referenceEpoch - currentEpoch).coerceAtLeast(0L)
                val intervals = (missedDays / 7L) + 1L
                val days = (intervals * 7L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                addDays(current.first, current.second, current.third, days)
            }
            RecurringFrequency.MONTHLY -> {
                var months = ((reference.first - current.first) * 12 + reference.second - current.second).coerceAtLeast(1)
                var candidate = monthAfter(current.first, current.second, months, anchorDay)
                while (PersianCalendarHelper.getJalaliEpochDay(candidate.first, candidate.second, candidate.third) <= referenceEpoch) {
                    months++
                    candidate = monthAfter(current.first, current.second, months, anchorDay)
                }
                candidate
            }
            RecurringFrequency.YEARLY -> {
                var years = (reference.first - current.first).coerceAtLeast(1)
                var candidate = yearAfter(current.first, years, anchorMonth, anchorDay)
                while (PersianCalendarHelper.getJalaliEpochDay(candidate.first, candidate.second, candidate.third) <= referenceEpoch) {
                    years++
                    candidate = yearAfter(current.first, years, anchorMonth, anchorDay)
                }
                candidate
            }
        }

        return CalendarDateUtils.toPersianDisplay(
            CalendarDateUtils.formatJalali(next.first, next.second, next.third)
        )
    }

    private fun monthAfter(
        year: Int,
        month: Int,
        monthsToAdd: Int,
        anchorDay: Int
    ): Triple<Int, Int, Int> {
        val absoluteMonth = year * 12 + (month - 1) + monthsToAdd
        val nextYear = absoluteMonth / 12
        val nextMonth = absoluteMonth % 12 + 1
        val day = anchorDay.coerceAtMost(CalendarDateUtils.getDaysInJalaliMonth(nextYear, nextMonth))
        return Triple(nextYear, nextMonth, day)
    }

    private fun yearAfter(
        year: Int,
        yearsToAdd: Int,
        anchorMonth: Int,
        anchorDay: Int
    ): Triple<Int, Int, Int> {
        val nextYear = year + yearsToAdd
        val day = anchorDay.coerceAtMost(CalendarDateUtils.getDaysInJalaliMonth(nextYear, anchorMonth))
        return Triple(nextYear, anchorMonth, day)
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
