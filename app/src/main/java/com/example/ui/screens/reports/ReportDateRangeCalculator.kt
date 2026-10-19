package com.example.ui.screens.reports

import com.example.util.IranianPhoneUtils
import com.example.util.PersianCalendarHelper

/**
 * Pure date-range calculations shared by ReportsViewModel and unit tests.
 * Ranges are inclusive at both ends; adjacent comparison ranges never overlap.
 */
internal object ReportDateRangeCalculator {
    fun parsePersianDateToMillis(persianDate: String): Long {
        if (persianDate.isBlank()) return 0L
        val normalized = IranianPhoneUtils.convertDigitsToEnglish(persianDate.trim())
            .replace('-', '/')
            .replace('.', '/')
        val parts = normalized.split('/').filter { it.isNotBlank() }
        if (parts.size != 3) return 0L
        return try {
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            if (month !in 1..12 || day !in 1..PersianCalendarHelper.getDaysInMonth(year, month)) {
                return 0L
            }
            PersianCalendarHelper.jalaliToEpochMillis(year, month, day, 9, 0)
        } catch (_: Exception) {
            0L
        }
    }

    fun getPeriodBounds(
        period: ReportPeriod,
        customStart: ShamsiDate,
        customEnd: ShamsiDate,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<Long, Long> {
        val currentPersian = PersianCalendarHelper.fromEpochMillis(nowMillis)
        return when (period) {
            ReportPeriod.WEEK -> Pair(nowMillis - (7 * 24 * 60 * 60 * 1000L), nowMillis)
            ReportPeriod.MONTH -> {
                val start = PersianCalendarHelper.jalaliToEpochMillis(
                    currentPersian.year, currentPersian.month, 1, 0, 0
                )
                Pair(start, nowMillis)
            }
            ReportPeriod.YEAR -> {
                val start = PersianCalendarHelper.jalaliToEpochMillis(currentPersian.year, 1, 1, 0, 0)
                Pair(start, nowMillis)
            }
            ReportPeriod.CUSTOM -> {
                val startDate = if (compareShamsiDates(customStart, customEnd) <= 0) customStart else customEnd
                val endDate = if (compareShamsiDates(customStart, customEnd) <= 0) customEnd else customStart
                val start = PersianCalendarHelper.jalaliToEpochMillis(
                    startDate.year, startDate.month, startDate.day, 0, 0
                )
                val end = PersianCalendarHelper.jalaliToEpochMillis(
                    endDate.year, endDate.month, endDate.day, 23, 59
                ) + 60_000L - 1L
                Pair(start, end)
            }
        }
    }

    fun compareShamsiDates(first: ShamsiDate, second: ShamsiDate): Int =
        compareValuesBy(first, second, ShamsiDate::year, ShamsiDate::month, ShamsiDate::day)

    fun getPreviousPeriodBounds(
        period: ReportPeriod,
        startTime: Long,
        endTime: Long
    ): Pair<Long, Long> {
        return when (period) {
            ReportPeriod.WEEK -> {
                val duration = 7 * 24 * 60 * 60 * 1000L
                Pair(startTime - duration, startTime - 1L)
            }
            ReportPeriod.MONTH -> {
                val startPersian = PersianCalendarHelper.fromEpochMillis(startTime)
                var previousYear = startPersian.year
                var previousMonth = startPersian.month - 1
                if (previousMonth == 0) {
                    previousMonth = 12
                    previousYear -= 1
                }
                val previousStart = PersianCalendarHelper.jalaliToEpochMillis(
                    previousYear, previousMonth, 1, 0, 0
                )
                val previousMonthEnd = PersianCalendarHelper.jalaliToEpochMillis(
                    previousYear,
                    previousMonth,
                    PersianCalendarHelper.getDaysInMonth(previousYear, previousMonth),
                    23,
                    59
                ) + 60_000L - 1L
                val elapsed = (endTime - startTime).coerceAtLeast(0L)
                Pair(previousStart, minOf(previousStart + elapsed, previousMonthEnd))
            }
            ReportPeriod.YEAR -> {
                val startPersian = PersianCalendarHelper.fromEpochMillis(startTime)
                val endPersian = PersianCalendarHelper.fromEpochMillis(endTime)
                val previousYear = startPersian.year - 1
                val previousStart = PersianCalendarHelper.jalaliToEpochMillis(previousYear, 1, 1, 0, 0)
                val matchingDay = minOf(
                    endPersian.day,
                    PersianCalendarHelper.getDaysInMonth(previousYear, endPersian.month)
                )
                val previousEnd = PersianCalendarHelper.jalaliToEpochMillis(
                    previousYear, endPersian.month, matchingDay, endPersian.hour, endPersian.minute
                ) + 60_000L - 1L
                Pair(previousStart, maxOf(previousStart, previousEnd))
            }
            ReportPeriod.CUSTOM -> {
                val inclusiveDuration = (endTime - startTime + 1L).coerceAtLeast(1L)
                Pair(startTime - inclusiveDuration, startTime - 1L)
            }
        }
    }
}
