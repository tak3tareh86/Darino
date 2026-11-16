package com.example.vehicle.domain

import com.example.vehicle.data.ServiceType
import com.example.vehicle.data.VehicleServiceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VehicleAnalyzerServiceHistoryTest {

    private fun service(
        id: String,
        type: ServiceType,
        title: String,
        date: String,
        description: String = ""
    ) = VehicleServiceEntity(
        id = id,
        vehicleId = "vehicle-1",
        title = title,
        serviceType = type,
        date = date,
        mileage = 50000,
        cost = 0L,
        description = description
    )

    @Test
    fun legacyCombinedEntryIsRecognizedForOilAndAirFilters() {
        val legacy = service(
            id = "legacy",
            type = ServiceType.OIL_CHANGE,
            title = "تعویض روغن موتور",
            date = "۱۴۰۵/۰۸/۱۰",
            description = "تعویض فیلتر روغن و تعویض فیلتر هوا"
        )

        assertNotNull(
            VehicleAnalyzer.findLatestServiceForType(
                listOf(legacy),
                ServiceType.OIL_FILTER_CHANGE
            )
        )
        assertNotNull(
            VehicleAnalyzer.findLatestServiceForType(
                listOf(legacy),
                ServiceType.AIR_FILTER_CHANGE
            )
        )
    }

    @Test
    fun newestLegacyCombinedEntryWinsOverOlderExactTypeEntry() {
        val olderExact = service(
            id = "exact-old",
            type = ServiceType.OIL_FILTER_CHANGE,
            title = "تعویض فیلتر روغن",
            date = "۱۴۰۵/۰۷/۰۱"
        )
        val newerCombined = service(
            id = "combined-new",
            type = ServiceType.OIL_CHANGE,
            title = "تعویض روغن موتور، تعویض فیلتر روغن، تعویض فیلتر هوا",
            date = "۱۴۰۵/۰۸/۱۰"
        )

        val latest = VehicleAnalyzer.findLatestServiceForType(
            listOf(olderExact, newerCombined),
            ServiceType.OIL_FILTER_CHANGE
        )

        assertEquals("combined-new", latest?.id)
    }

    @Test
    fun healthAnalysisIncludesEveryAdditionalRecordedServiceType() {
        val vehicle = com.example.vehicle.data.VehicleEntity(
            id = "vehicle-1",
            brand = "Test",
            model = "Car",
            year = "1400",
            color = "",
            plate = "",
            currentMileage = 50000
        )
        val battery = service(
            id = "battery",
            type = ServiceType.BATTERY,
            title = ServiceType.BATTERY.title,
            date = "۱۴۰۵/۰۸/۱۰"
        ).copy(nextReminderDate = "۱۴۹۹/۱۲/۲۹", isReminderEnabled = true)
        val brakes = service(
            id = "brakes",
            type = ServiceType.BRAKE_PADS,
            title = ServiceType.BRAKE_PADS.title,
            date = "۱۴۰۵/۰۸/۱۰"
        ).copy(nextReminderDate = "۱۴۹۹/۱۲/۲۹", isReminderEnabled = true)

        val report = VehicleAnalyzer.analyzeHealth(
            vehicle = vehicle,
            services = listOf(battery, brakes),
            insurances = emptyList(),
            inspections = emptyList()
        )

        assertNotNull(report.checklist.firstOrNull { it.title == ServiceType.BATTERY.title })
        assertNotNull(report.checklist.firstOrNull { it.title == ServiceType.BRAKE_PADS.title })
    }

    @Test
    fun genericFilterRecordDoesNotConfirmEitherSpecificFilter() {
        val generic = service(
            id = "filters",
            type = ServiceType.FILTERS,
            title = "تعویض فیلترها",
            date = "۱۴۰۵/۰۸/۱۰"
        )

        assertEquals(
            null,
            VehicleAnalyzer.findLatestServiceForType(
                listOf(generic),
                ServiceType.OIL_FILTER_CHANGE
            )
        )
        assertEquals(
            null,
            VehicleAnalyzer.findLatestServiceForType(
                listOf(generic),
                ServiceType.AIR_FILTER_CHANGE
            )
        )
    }
}
