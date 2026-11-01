package com.example.ui.screens.check

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckReportFilterTest {
    @Test fun monthNamesContainTwelveMonths() {
        assertEquals(12, CheckReportFilter.monthNames.size)
    }
}
