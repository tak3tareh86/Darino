package com.example.ui.screens.finance.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinancialReportCalculationsTest {

    @Test
    fun sumReportAmountsExact_returnsExactSumWithinLongRange() {
        assertEquals(600L, sumReportAmountsExact(listOf(100L, 200L, 300L)))
    }

    @Test
    fun sumReportAmountsExact_supportsLongBoundariesWhenResultIsValid() {
        assertEquals(Long.MAX_VALUE, sumReportAmountsExact(listOf(Long.MAX_VALUE - 10L, 10L)))
        assertEquals(Long.MIN_VALUE, sumReportAmountsExact(listOf(Long.MIN_VALUE + 10L, -10L)))
    }

    @Test
    fun sumReportAmountsExact_returnsNullInsteadOfOverflowing() {
        assertNull(sumReportAmountsExact(listOf(Long.MAX_VALUE, 1L)))
        assertNull(sumReportAmountsExact(listOf(Long.MIN_VALUE, -1L)))
    }

    @Test
    fun sumReportAmountsExact_handlesEmptyAndNegativeValues() {
        assertEquals(0L, sumReportAmountsExact(emptyList()))
        assertEquals(-25L, sumReportAmountsExact(listOf(-40L, 15L)))
    }

    @Test
    fun reportTrendDayRanges_coverEveryDayExactlyOnceForJalaliMonthLengths() {
        listOf(29, 30, 31).forEach { daysInMonth ->
            val ranges = reportTrendDayRanges(daysInMonth)
            assertEquals(6, ranges.size)
            assertEquals((1..daysInMonth).toList(), ranges.flatMap { it.toList() })
        }
    }

    @Test
    fun reportTrendDayRanges_rejectsInvalidArguments() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            reportTrendDayRanges(31, 0)
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            reportTrendDayRanges(5, 6)
        }
    }
}
