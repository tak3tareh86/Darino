package com.example.reports

import com.example.ui.screens.reports.ReportDateRangeCalculator
import com.example.ui.screens.reports.ReportPeriod
import com.example.ui.screens.reports.ShamsiDate
import com.example.util.PersianCalendarHelper
import com.example.vehicle.data.VehicleExpenseCategory
import com.example.vehicle.data.VehicleExpenseEntity
import com.example.vehicle.data.VehicleInspectionEntity
import com.example.vehicle.data.VehicleInsuranceEntity
import com.example.vehicle.data.VehicleServiceEntity
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
        assertEquals(current.second - current.first + 1L, previous.second - previous.first + 1L)
    }

    @Test
    fun yearToDateComparisonPreservesTheSameClockOffsetWithoutOverlap() {
        val now = PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 20, 12, 34) + 25_123L
        val current = ReportDateRangeCalculator.getPeriodBounds(
            ReportPeriod.YEAR, ShamsiDate(1405, 1, 1), ShamsiDate(1405, 7, 20), now
        )
        val previous = ReportDateRangeCalculator.getPreviousPeriodBounds(
            ReportPeriod.YEAR, current.first, current.second
        )
        val expectedPreviousEnd = PersianCalendarHelper.jalaliToEpochMillis(1404, 7, 20, 12, 34) + 25_123L

        assertTrue(previous.second < current.first)
        assertEquals(expectedPreviousEnd, previous.second)
        assertEquals(current.second - current.first + 1L, previous.second - previous.first + 1L)
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
    fun vehicleReportDoesNotDoubleCountRepositoryMirroredServiceExpenses() {
        val day = "1405/07/12"
        val start = PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 1, 0, 0)
        val end = PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 30, 23, 59) + 59_999L
        val expenses = listOf(
            VehicleExpenseEntity(
                id = "exp_svc_service-1",
                vehicleId = "vehicle-1",
                title = "تعویض روغن",
                category = VehicleExpenseCategory.SERVICE,
                amount = 250_000L,
                date = day
            ),
            VehicleExpenseEntity(
                id = "ordinary-expense",
                vehicleId = "vehicle-1",
                title = "سوخت",
                category = VehicleExpenseCategory.FUEL,
                amount = 100_000L,
                date = day
            )
        )
        val services = listOf(
            VehicleServiceEntity(
                id = "service-1",
                vehicleId = "vehicle-1",
                title = "تعویض روغن",
                date = day,
                mileage = 20_000,
                cost = 250_000L
            ),
            VehicleServiceEntity(
                id = "legacy-service",
                vehicleId = "vehicle-1",
                title = "سرویس قدیمی",
                date = day,
                mileage = 15_000,
                cost = 75_000L
            )
        )

        val total = ReportVehicleExpenseCalculator.totalForRange(
            startTime = start,
            endTime = end,
            expenses = expenses,
            services = services,
            insurances = emptyList<VehicleInsuranceEntity>(),
            inspections = emptyList<VehicleInspectionEntity>()
        )

        assertEquals(425_000L, total)
    }

    @Test
    fun dateParserAcceptsDashSeparatedPersianDatesAndRejectsInvalidDates() {
        val expected = PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 12, 9, 0)
        assertEquals(expected, ReportDateRangeCalculator.parsePersianDateToMillis("۱۴۰۵-۰۷-۱۲"))
        assertEquals(0L, ReportDateRangeCalculator.parsePersianDateToMillis("1405/13/40"))
    }
}
