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
    fun genericFilterRecordRemainsCompatibleWithBothFilterChecks() {
        val generic = service(
            id = "filters",
            type = ServiceType.FILTERS,
            title = "تعویض فیلترها",
            date = "۱۴۰۵/۰۸/۱۰"
        )

        assertEquals(
            "filters",
            VehicleAnalyzer.findLatestServiceForType(
                listOf(generic),
                ServiceType.OIL_FILTER_CHANGE
            )?.id
        )
        assertEquals(
            "filters",
            VehicleAnalyzer.findLatestServiceForType(
                listOf(generic),
                ServiceType.AIR_FILTER_CHANGE
            )?.id
        )
    }
}
