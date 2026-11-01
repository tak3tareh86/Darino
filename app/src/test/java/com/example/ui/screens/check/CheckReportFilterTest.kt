package com.example.ui.screens.check

import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType
import com.example.util.PersianCalendarHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckReportFilterTest {
    private fun sample(
        id: Int,
        year: Int,
        month: Int,
        day: Int,
        number: String,
        status: CheckStatus = CheckStatus.REGISTERED,
        type: CheckType = CheckType.RECEIVED,
        sayadId: String? = "SAYAD-$id",
        bankName: String = "بانک نمونه"
    ): Check {
        val due = PersianCalendarHelper.jalaliToEpochMillis(year, month, day, 9, 0)
        return Check(
            id = id,
            userId = "test-user",
            type = type,
            amount = 100L * id,
            checkNumber = number,
            sayadId = sayadId,
            bankName = bankName,
            branch = "شعبه مرکزی",
            holderName = "علی",
            receiverName = "رضا",
            issueDate = due,
            dueDate = due,
            status = status
        )
    }

    @Test
    fun availableYearsAreDistinctAndNewestFirst() {
        val checks = listOf(
            sample(1, 1403, 1, 5, "1"),
            sample(2, 1405, 2, 5, "2"),
            sample(3, 1404, 3, 5, "3")
        )
        assertEquals(listOf(1405, 1404, 1403), CheckReportFilter.availableYears(checks))
    }

    @Test
    fun selectedYearAndMonthFilterByJalaliDueDate() {
        val checks = listOf(
            sample(1, 1405, 1, 10, "101"),
            sample(2, 1405, 2, 10, "202"),
            sample(3, 1404, 1, 10, "303")
        )
        assertEquals(listOf(1), CheckReportFilter.filter(checks, 1405, 1, "").map { it.id })
        assertEquals(listOf(1, 2), CheckReportFilter.filter(checks, 1405, null, "").map { it.id })
    }

    @Test
    fun monthWithoutYearReturnsNoResultsInsteadOfMixingDifferentYears() {
        val checks = listOf(
            sample(1, 1405, 1, 10, "101"),
            sample(2, 1404, 1, 10, "202")
        )
        assertTrue(CheckReportFilter.filter(checks, null, 1, "").isEmpty())
    }

    @Test
    fun invalidMonthReturnsNoResults() {
        val checks = listOf(sample(1, 1405, 1, 10, "101"))
        assertTrue(CheckReportFilter.filter(checks, 1405, 13, "").isEmpty())
    }

    @Test
    fun searchNormalizesPersianAndArabicDigits() {
        val checks = listOf(
            sample(1, 1405, 1, 10, "12345"),
            sample(2, 1405, 1, 11, "98765")
        )
        assertEquals(listOf(1), CheckReportFilter.filter(checks, null, null, "۱۲۳۴۵").map { it.id })
        assertEquals(listOf(1), CheckReportFilter.filter(checks, null, null, "١٢٣٤٥").map { it.id })
    }

    @Test
    fun searchNormalizesArabicKafAndPersianKaf() {
        val checks = listOf(sample(1, 1405, 1, 10, "1", bankName = "بانک نمونه"))
        assertEquals(listOf(1), CheckReportFilter.filter(checks, null, null, "بانك").map { it.id })
    }

    @Test
    fun searchCanBeCombinedWithYearAndMonth() {
        val checks = listOf(
            sample(1, 1405, 1, 10, "12345"),
            sample(2, 1405, 2, 10, "12345"),
            sample(3, 1404, 1, 10, "12345")
        )
        assertEquals(listOf(1), CheckReportFilter.filter(checks, 1405, 1, "۱۲۳۴۵").map { it.id })
    }

    @Test
    fun reportIncludesRecordsFromBothSidesOfJalaliNewYear() {
        val checks = listOf(
            sample(1, 1404, 12, 29, "old-year"),
            sample(2, 1405, 1, 1, "new-year")
        )
        assertEquals(listOf(2), CheckReportFilter.filter(checks, 1405, 1, "").map { it.id })
        assertEquals(listOf(1), CheckReportFilter.filter(checks, 1404, 12, "").map { it.id })
    }

    @Test
    fun historicalStatusesRemainInReportAndAreSeparatedInSummary() {
        val checks = listOf(
            sample(1, 1405, 1, 10, "1", CheckStatus.CLEARED),
            sample(2, 1405, 1, 11, "2", CheckStatus.CANCELED),
            sample(3, 1405, 1, 12, "3", CheckStatus.BOUNCED, CheckType.PAID),
            sample(4, 1405, 1, 13, "4", CheckStatus.PENDING, CheckType.PAID)
        )
        val results = CheckReportFilter.filter(checks, 1405, 1, "")
        val summary = CheckReportFilter.summarize(results)

        assertEquals(setOf(1, 2, 3, 4), results.map { it.id }.toSet())
        assertEquals(4, summary.totalCount)
        assertEquals(1_000L, summary.totalNominalAmount)
        assertEquals(4, summary.statusTotals.sumOf { it.count })
        assertEquals(1, summary.statusTotals.single { it.status == CheckStatus.CLEARED }.count)
        assertEquals(1, summary.statusTotals.single { it.type == CheckType.PAID && it.status == CheckStatus.BOUNCED }.count)
        assertEquals(700L, summary.statusTotals.filter { it.type == CheckType.PAID }.sumOf { it.amount })
    }

    @Test
    fun directionFilterSeparatesReceivedAndPaidChecks() {
        val checks = listOf(
            sample(1, 1405, 1, 10, "received", type = CheckType.RECEIVED),
            sample(2, 1405, 1, 11, "paid", type = CheckType.PAID),
            sample(3, 1404, 2, 12, "older-paid", type = CheckType.PAID)
        )

        assertEquals(
            listOf(1),
            CheckReportFilter.filter(checks, null, null, "", CheckType.RECEIVED).map { it.id }
        )
        assertEquals(
            listOf(2, 3),
            CheckReportFilter.filter(checks, null, null, "", CheckType.PAID).map { it.id }
        )
        assertEquals(3, CheckReportFilter.filter(checks, null, null, "").size)
    }

    @Test
    fun availableYearsDependOnSelectedCheckDirection() {
        val checks = listOf(
            sample(1, 1405, 1, 10, "received", type = CheckType.RECEIVED),
            sample(2, 1403, 1, 11, "paid", type = CheckType.PAID),
            sample(3, 1404, 1, 12, "paid-2", type = CheckType.PAID)
        )

        assertEquals(listOf(1405), CheckReportFilter.availableYears(checks, CheckType.RECEIVED))
        assertEquals(listOf(1404, 1403), CheckReportFilter.availableYears(checks, CheckType.PAID))
        assertEquals(listOf(1405, 1404, 1403), CheckReportFilter.availableYears(checks))
    }

    @Test
    fun directionFilterCombinesWithYearMonthAndSearch() {
        val checks = listOf(
            sample(1, 1405, 1, 10, "12345", type = CheckType.RECEIVED),
            sample(2, 1405, 1, 11, "12345", type = CheckType.PAID),
            sample(3, 1405, 2, 12, "12345", type = CheckType.PAID)
        )

        assertEquals(
            listOf(2),
            CheckReportFilter.filter(checks, 1405, 1, "۱۲۳۴۵", CheckType.PAID).map { it.id }
        )
    }

    @Test
    fun resultsAreSortedByDueDateDescending() {
        val checks = listOf(
            sample(1, 1405, 1, 1, "1"),
            sample(2, 1405, 1, 20, "2")
        )
        assertEquals(listOf(2, 1), CheckReportFilter.filter(checks, null, null, "").map { it.id })
    }
}
