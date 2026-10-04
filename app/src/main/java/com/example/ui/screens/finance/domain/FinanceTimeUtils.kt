package com.example.ui.screens.finance.domain

import com.example.ui.screens.finance.model.FinanceFilterPeriod
import com.example.util.PersianCalendarHelper
import java.util.Calendar
import java.util.TimeZone

data class TimeRange(val startMillis: Long, val endMillis: Long)

object FinanceTimeUtils {

    /**
     * Converts a FinanceFilterPeriod to a specific TimeRange (start inclusive, end exclusive).
     * Uses current system time and default timezone.
     */
    fun getTimeRangeForPeriod(period: FinanceFilterPeriod, now: Long = System.currentTimeMillis()): TimeRange {
        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = now
        }

        return when (period) {
            FinanceFilterPeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val end = calendar.timeInMillis
                TimeRange(start, end)
            }
            FinanceFilterPeriod.THIS_WEEK -> {
                // In Iran, week usually starts from Saturday (Calendar.SATURDAY)
                // However, standard Java Calendar might vary. Let's force start of week to Saturday for Persian locale context.
                // We find the most recent Saturday.
                while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 7)
                val end = calendar.timeInMillis
                TimeRange(start, end)
            }
            FinanceFilterPeriod.THIS_MONTH -> {
                val pdt = PersianCalendarHelper.fromEpochMillis(now)
                val start = PersianCalendarHelper.jalaliToEpochMillis(pdt.year, pdt.month, 1, 0, 0)
                
                var nextYear = pdt.year
                var nextMonth = pdt.month + 1
                if (nextMonth > 12) {
                    nextMonth = 1
                    nextYear++
                }
                val end = PersianCalendarHelper.jalaliToEpochMillis(nextYear, nextMonth, 1, 0, 0)
                TimeRange(start, end)
            }
            FinanceFilterPeriod.LAST_3_MONTHS -> {
                val pdt = PersianCalendarHelper.fromEpochMillis(now)
                // Start of current month
                // Then go back 2 months to include a total of 3 months
                var startYear = pdt.year
                var startMonth = pdt.month - 2
                while (startMonth < 1) {
                    startMonth += 12
                    startYear--
                }
                val start = PersianCalendarHelper.jalaliToEpochMillis(startYear, startMonth, 1, 0, 0)
                
                var nextYear = pdt.year
                var nextMonth = pdt.month + 1
                if (nextMonth > 12) {
                    nextMonth = 1
                    nextYear++
                }
                val end = PersianCalendarHelper.jalaliToEpochMillis(nextYear, nextMonth, 1, 0, 0)
                TimeRange(start, end)
            }
            FinanceFilterPeriod.THIS_YEAR -> {
                val pdt = PersianCalendarHelper.fromEpochMillis(now)
                val start = PersianCalendarHelper.jalaliToEpochMillis(pdt.year, 1, 1, 0, 0)
                val end = PersianCalendarHelper.jalaliToEpochMillis(pdt.year + 1, 1, 1, 0, 0)
                TimeRange(start, end)
            }
            FinanceFilterPeriod.ALL -> {
                TimeRange(0L, Long.MAX_VALUE)
            }
        }
    }
}
