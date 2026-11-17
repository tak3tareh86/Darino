package com.example.vehicle.presentation

import com.example.R
import com.example.vehicle.data.VehicleTimelineEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleTimelineFilterTest {

    private fun event(id: String, date: String) = VehicleTimelineEvent.ServiceEvent(
        id = id,
        date = date,
        title = id,
        amount = 100L,
        subtitle = "test",
        iconRes = R.drawable.img_3d_oil,
        colorHex = 0xFF10B981,
        mileage = 1000
    )

    @Test
    fun monthFilterIncludesOnlyMatchingJalaliMonthAndAcceptsPersianDigits() {
        val events = listOf(
            event("first", "۱۴۰۵/۰۸/۰۱"),
            event("last", "1405-08-30"),
            event("previous", "1405/07/30"),
            event("next", "1405/09/01")
        )

        val result = filterVehicleTimelineEvents(events, TimelineFilterMode.MONTH, 1405, 8)

        assertEquals(listOf("first", "last"), result.map { it.id })
    }

    @Test
    fun yearFilterIncludesEveryValidMonthInSelectedYearOnly() {
        val events = listOf(
            event("spring", "1405/01/01"),
            event("winter", "1405/12/29"),
            event("previous-year", "1404/12/29"),
            event("next-year", "1406/01/01")
        )

        val result = filterVehicleTimelineEvents(events, TimelineFilterMode.YEAR, 1405, 1)

        assertEquals(listOf("spring", "winter"), result.map { it.id })
    }

    @Test
    fun allModeKeepsRecordsWithInvalidDatesButPeriodFiltersDoNotGuessTheirDate() {
        val events = listOf(
            event("valid", "1405/08/15"),
            event("invalid", "تاریخ نامشخص")
        )

        assertEquals(
            listOf("valid", "invalid"),
            filterVehicleTimelineEvents(events, TimelineFilterMode.ALL, 1405, 8).map { it.id }
        )
        assertEquals(
            listOf("valid"),
            filterVehicleTimelineEvents(events, TimelineFilterMode.MONTH, 1405, 8).map { it.id }
        )
    }
    @Test
    fun timelineRowKeysRemainUniqueWhenDifferentTablesReuseTheSameId() {
        val sameId = "shared-id"
        val service = event(sameId, "1405/08/10")
        val expense = VehicleTimelineEvent.ExpenseEvent(
            id = sameId,
            date = "1405/08/10",
            title = "هزینه مستقل",
            amount = 100L,
            subtitle = "هزینه",
            iconRes = R.drawable.img_3d_oil,
            colorHex = 0xFF10B981,
            category = com.example.vehicle.data.VehicleExpenseCategory.SERVICE
        )
        val insurance = VehicleTimelineEvent.InsuranceEvent(
            id = sameId,
            date = "1405/08/10",
            title = "بیمه",
            amount = 100L,
            subtitle = "بیمه",
            iconRes = R.drawable.img_3d_oil,
            colorHex = 0xFF10B981,
            company = "تست"
        )

        val inspection = VehicleTimelineEvent.InspectionEvent(
            id = sameId,
            date = "1405/08/10",
            title = "معاینه فنی",
            amount = 100L,
            subtitle = "معاینه",
            iconRes = R.drawable.img_3d_oil,
            colorHex = 0xFF10B981,
            expiryDate = "1406/08/10"
        )

        val keys = listOf(service, expense, insurance, inspection).map(::vehicleTimelineEventKey)

        assertEquals(4, keys.toSet().size)
    }

}
