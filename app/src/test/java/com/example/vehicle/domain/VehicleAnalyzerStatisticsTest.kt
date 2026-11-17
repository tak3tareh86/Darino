package com.example.vehicle.domain

import com.example.ui.screens.settings.model.AppCurrency
import com.example.vehicle.data.ServiceType
import com.example.vehicle.data.VehicleExpenseCategory
import com.example.vehicle.data.VehicleExpenseEntity
import com.example.vehicle.data.VehicleServiceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleAnalyzerStatisticsTest {

    private fun service(
        id: String,
        vehicleId: String = "vehicle-1",
        status: String = "PENDING"
    ) = VehicleServiceEntity(
        id = id,
        vehicleId = vehicleId,
        title = id,
        serviceType = ServiceType.OIL_CHANGE,
        date = "1405/08/10",
        mileage = 50000,
        cost = 0L,
        status = status
    )

    private fun repair(
        id: String,
        date: String,
        vehicleId: String = "vehicle-1"
    ) = VehicleExpenseEntity(
        id = id,
        vehicleId = vehicleId,
        title = id,
        category = VehicleExpenseCategory.REPAIRS,
        amount = 100L,
        date = date,
        amountUnit = AppCurrency.RIAL
    )

    @Test
    fun statisticsCountOnlyCompletedServicesForSelectedVehicle() {
        val statistics = VehicleAnalyzer.computeStatistics(
            vehicleId = "vehicle-1",
            expenses = emptyList(),
            services = listOf(
                service("completed", status = "COMPLETED"),
                service("pending"),
                service("completed-lowercase", status = "completed"),
                service("other-vehicle", vehicleId = "vehicle-2", status = "COMPLETED")
            )
        )

        assertEquals(2, statistics.totalServicesCount)
    }

    @Test
    fun latestRepairIsSelectedByValidatedDateNotInputOrder() {
        val statistics = VehicleAnalyzer.computeStatistics(
            vehicleId = "vehicle-1",
            expenses = listOf(
                repair("newer", "۱۴۰۵-۰۸-۱۰"),
                repair("older", "1405/07/20"),
                repair("invalid-day", "1405/07/31"),
                repair("other-vehicle", "1405/09/01", vehicleId = "vehicle-2")
            ),
            services = emptyList()
        )

        assertEquals("newer", statistics.lastRepairTitle)
        assertEquals("۱۴۰۵-۰۸-۱۰", statistics.lastRepairDate)
    }

    @Test
    fun jalaliDateParserAcceptsBothSeparatorsAndPersianDigits() {
        assertEquals(
            "1405/08/10",
            VehicleAnalyzer.parseAndValidateJalaliDate("۱۴۰۵-۰۸-۱۰")
        )
        assertEquals(
            "1405/08/10",
            VehicleAnalyzer.parseAndValidateJalaliDate("1405/08/10")
        )
    }

    @Test
    fun jalaliDateParserRejectsImpossibleDay() {
        assertNull(VehicleAnalyzer.parseAndValidateJalaliDate("1405/07/31"))
    }
}
