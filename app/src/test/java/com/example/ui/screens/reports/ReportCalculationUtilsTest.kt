package com.example.ui.screens.reports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportCalculationUtilsTest {
    @Test
    fun customRangeAcceptsSameDayAndAscendingDates() {
        assertTrue(ReportCalculationUtils.isValidDateRange(ShamsiDate(1405, 7, 20), ShamsiDate(1405, 7, 20)))
        assertTrue(ReportCalculationUtils.isValidDateRange(ShamsiDate(1405, 6, 30), ShamsiDate(1405, 7, 1)))
    }

    @Test
    fun customRangeRejectsStartAfterEnd() {
        assertFalse(ReportCalculationUtils.isValidDateRange(ShamsiDate(1405, 7, 2), ShamsiDate(1405, 7, 1)))
    }

    @Test
    fun expenseChangeUsesAbsoluteDifferenceAgainstPreviousPeriod() {
        assertEquals(50.0, ReportCalculationUtils.calculateExpenseChangePercent(150L, 100L), 0.001)
        assertEquals(50.0, ReportCalculationUtils.calculateExpenseChangePercent(50L, 100L), 0.001)
    }

    @Test
    fun expenseChangeHasNoInventedPercentageWhenPreviousPeriodIsZero() {
        assertEquals(0.0, ReportCalculationUtils.calculateExpenseChangePercent(100L, 0L), 0.001)
        assertEquals(0.0, ReportCalculationUtils.calculateExpenseChangePercent(0L, 0L), 0.001)
    }
}
