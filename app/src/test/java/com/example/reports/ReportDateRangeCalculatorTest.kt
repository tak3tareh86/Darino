package com.example.reports

import com.example.ui.screens.reports.ReportDateRangeCalculator
import com.example.ui.screens.reports.ReportPeriod
import com.example.ui.screens.reports.ShamsiDate
import com.example.util.PersianCalendarHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportDateRangeCalculatorTest {
    @Test
    fun reversedCustomRangeIsNormalizedAndIncludesTheEntireEndDay() {
        val start = ShamsiDate(1405, 7, 4)
        val end = ShamsiDate(1405, 7, 9)

        val actual = ReportDateRangeCalculator.getPeriodBounds(
            ReportPeriod.CUSTOM,
            customStart = end,
            customEnd = start,
            nowMillis = PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 20, 12, 0)
        )

        assertEquals(
            PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 4, 0, 0),
            actual.first
        )
        assertEquals(
            PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 9, 23, 59) + 59_999L,
            actual.second
        )
    }

    @Test
    fun customPreviousPeriodEndsImmediatelyBeforeCurrentPeriod() {
        val current = ReportDateRangeCalculator.getPeriodBounds(
            ReportPeriod.CUSTOM,
            ShamsiDate(1405, 6, 1),
            ShamsiDate(1405, 6, 10),
            nowMillis = PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 20, 12, 0)
        )
        val previous = ReportDateRangeCalculator.getPreviousPeriodBounds(
            ReportPeriod.CUSTOM, current.first, current.second
        )

        assertEquals(current.first - 1L, previous.second)
        assertEquals(current.second - current.first + 1L, previous.second - previous.first + 1L)
    }

    @Test
    fun weekComparisonRangesDoNotOverlapAndHaveEqualDurations() {
        val now = PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 20, 12, 34)
        val current = ReportDateRangeCalculator.getPeriodBounds(
            ReportPeriod.WEEK, ShamsiDate(1405, 7, 1), ShamsiDate(1405, 7, 30), now
        )
        val previous = ReportDateRangeCalculator.getPreviousPeriodBounds(
            ReportPeriod.WEEK, current.first, current.second
        )

        assertEquals(current.first - 1L, previous.second)
        assertEquals(current.second - current.first, previous.second - previous.first)
    }

    @Test
    fun monthToDateComparisonDoesNotExtendBeyondPreviousMonth() {
        val now = PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 20, 12, 34)
        val current = ReportDateRangeCalculator.getPeriodBounds(
            ReportPeriod.MONTH, ShamsiDate(1405, 7, 1), ShamsiDate(1405, 7, 30), now
        )
        val previous = ReportDateRangeCalculator.getPreviousPeriodBounds(
            ReportPeriod.MONTH, current.first, current.second
        )
        val previousStartDate = PersianCalendarHelper.fromEpochMillis(previous.first)
        val previousEndDate = PersianCalendarHelper.fromEpochMillis(previous.second)

        assertTrue(previous.second < current.first)
        assertEquals(6, previousStartDate.month)
        assertEquals(1405, previousStartDate.year)
        assertEquals(6, previousEndDate.month)
        assertEquals(1405, previousEndDate.year)
    }

    @Test
    fun dateParserAcceptsDashSeparatedPersianDatesAndRejectsInvalidDates() {
        val expected = PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 12, 9, 0)
        assertEquals(expected, ReportDateRangeCalculator.parsePersianDateToMillis("۱۴۰۵-۰۷-۱۲"))
        assertEquals(0L, ReportDateRangeCalculator.parsePersianDateToMillis("1405/13/40"))
    }
}
