package com.example.ui.screens.check

import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType
import com.example.util.PersianCalendarHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckReportFilterTest {
    private fun sample(id: Int, year: Int, month: Int, day: Int, number: String, status: CheckStatus = CheckStatus.REGISTERED): Check {
        val due = PersianCalendarHelper.jalaliToEpochMillis(year, month, day, 9, 0)
        return Check(
            id = id,
            userId = "test-user",
            type = CheckType.RECEIVED,
            amount = 100L * id,
            checkNumber = number,
            bankName = "بانک نمونه",
            holderName = "علی",
            receiverName = "رضا",
            issueDate = due,
            dueDate = due,
            status = status
        )
    }

    @Test
    fun availableYearsAreDistinctAndNewestFirst() {
        val checks = listOf(sample(1, 1403, 1, 5, "1"), sample(2, 1405, 2, 5, "2"), sample(3, 1404, 3, 5, "3"))
        assertEquals(listOf(1405, 1404, 1403), CheckReportFilter.availableYears(checks))
    }

    @Test
    fun selectedYearAndMonthFilterByJalaliDueDate() {
        val checks = listOf(sample(1, 1405, 1, 10, "101"), sample(2, 1405, 2, 10, "202"), sample(3, 1404, 1, 10, "303"))
        assertEquals(listOf(1), CheckReportFilter.filter(checks, 1405, 1, "").map { it.id })
        assertEquals(listOf(1, 2), CheckReportFilter.filter(checks, 1405, null, "").map { it.id })
    }

    @Test
    fun searchAcceptsPersianDigits() {
        val checks = listOf(sample(1, 1405, 1, 10, "12345"), sample(2, 1405, 1, 11, "98765"))
        assertEquals(listOf(1), CheckReportFilter.filter(checks, null, null, "۱۲۳۴۵").map { it.id })
    }

    @Test
    fun historicalStatusesRemainInReport() {
        val checks = listOf(
            sample(1, 1405, 1, 10, "1", CheckStatus.CLEARED),
            sample(2, 1405, 1, 11, "2", CheckStatus.CANCELED),
            sample(3, 1405, 1, 12, "3", CheckStatus.BOUNCED)
        )
        assertEquals(setOf(1, 2, 3), CheckReportFilter.filter(checks, 1405, 1, "").map { it.id }.toSet())
    }
}
