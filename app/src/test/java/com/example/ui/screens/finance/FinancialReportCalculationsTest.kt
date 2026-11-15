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
}
