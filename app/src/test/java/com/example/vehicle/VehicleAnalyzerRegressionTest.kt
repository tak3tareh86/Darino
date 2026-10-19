package com.example.vehicle

import com.example.vehicle.data.ServiceType
import com.example.vehicle.data.VehicleEntity
import com.example.vehicle.data.VehicleExpenseCategory
import com.example.vehicle.data.VehicleExpenseEntity
import com.example.vehicle.data.VehicleServiceEntity
import com.example.vehicle.domain.VehicleAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleAnalyzerRegressionTest {

    @Test
    fun healthUsesMostRecentOilServiceByPerformedDate() {
        val vehicle = VehicleEntity(
            id = "health-car",
            brand = "سایپا",
            model = "تیبا",
            year = "1402",
            color = "سفید",
            plate = "",
            currentMileage = 25000
        )
        val olderService = VehicleServiceEntity(
            id = "old-oil",
            vehicleId = vehicle.id,
            title = "تعویض روغن قدیمی",
            serviceType = ServiceType.OIL_CHANGE,
            date = "۱۴۰۴/۰۱/۰۱",
            mileage = 10000,
            cost = 0L,
            nextReminderMileage = 20000
        )
        val newerService = VehicleServiceEntity(
            id = "new-oil",
            vehicleId = vehicle.id,
            title = "تعویض روغن جدید",
            serviceType = ServiceType.OIL_CHANGE,
            date = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate(),
            mileage = 20000,
            cost = 0L,
            nextReminderMileage = 60000
        )

        val report = VehicleAnalyzer.analyzeHealth(
            vehicle = vehicle,
            services = listOf(olderService, newerService),
            insurances = emptyList(),
            inspections = emptyList()
        )
        val oilCheck = report.checklist.first { it.title == "روغن و فیلتر موتور" }

        assertTrue("باید سرویس جدیدتر مبنا قرار بگیرد", oilCheck.statusText.contains("35000"))
    }

    @Test
    fun healthFlagsAnOverdueCalendarReminderEvenWhenMileageTargetIsFarAway() {
        val vehicle = VehicleEntity(
            id = "date-health-car",
            brand = "سایپا",
            model = "پراید",
            year = "1400",
            color = "سفید",
            plate = "",
            currentMileage = 10000
        )
        val service = VehicleServiceEntity(
            id = "date-oil",
            vehicleId = vehicle.id,
            title = "تعویض روغن",
            serviceType = ServiceType.OIL_CHANGE,
            date = "1405/01/01",
            mileage = 9000,
            cost = 0L,
            nextReminderDate = "1400/01/01",
            nextReminderMileage = 90000
        )
        val report = VehicleAnalyzer.analyzeHealth(
            vehicle = vehicle,
            services = listOf(service),
            insurances = emptyList(),
            inspections = emptyList()
        )
        val oilCheck = report.checklist.first { it.title == "روغن و فیلتر موتور" }

        assertFalse(oilCheck.isOk)
        assertTrue(oilCheck.statusText.contains("زمانی"))
    }

    @Test
    fun yearlyExpenseDoesNotFallBackToLifetimeTotalWhenCurrentYearIsZero() {
        val currentYear = com.example.util.PersianCalendarHelper
            .fromEpochMillis(System.currentTimeMillis()).year
        val oldExpense = VehicleExpenseEntity(
            id = "old-expense",
            vehicleId = "stats-car",
            title = "تعمیر سال قبل",
            category = VehicleExpenseCategory.REPAIRS,
            amount = 80_000_000L,
            date = "${currentYear - 1}/01/01"
        )

        val stats = VehicleAnalyzer.computeStatistics(
            vehicleId = "stats-car",
            expenses = listOf(oldExpense),
            services = emptyList()
        )

        assertEquals(0L, stats.yearlyCost)
    }
}
