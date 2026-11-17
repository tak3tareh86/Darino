package com.example.vehicle.domain

import com.example.vehicle.data.ServiceType
import com.example.vehicle.data.VehicleServiceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun healthAnalysisChecklistAlwaysContainsOnlyTheNineFixedItems() {
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

        // Additional service records must never expand the fixed nine-item checklist.
        assertEquals(9, report.checklist.size)
        assertNull(report.checklist.firstOrNull { it.title == ServiceType.BATTERY.title })
        assertNull(report.checklist.firstOrNull { it.title == ServiceType.BRAKE_PADS.title })
        assertEquals(0, report.healthScorePercent)
    }

    @Test
    fun threeCompletedChecklistServicesProduceOneThirdHealthScore() {
        val vehicle = com.example.vehicle.data.VehicleEntity(
            id = "vehicle-1",
            brand = "Test",
            model = "Car",
            year = "1400",
            color = "",
            plate = "",
            currentMileage = 50000
        )
        val completedServices = listOf(
            service(
                id = "oil",
                type = ServiceType.OIL_CHANGE,
                title = ServiceType.OIL_CHANGE.title,
                date = "۱۴۰۵/۰۸/۱۰"
            ),
            service(
                id = "oil-filter",
                type = ServiceType.OIL_FILTER_CHANGE,
                title = ServiceType.OIL_FILTER_CHANGE.title,
                date = "۱۴۰۵/۰۸/۱۰"
            ),
            service(
                id = "air-filter",
                type = ServiceType.AIR_FILTER_CHANGE,
                title = ServiceType.AIR_FILTER_CHANGE.title,
                date = "۱۴۰۵/۰۸/۱۰"
            )
        )

        val report = VehicleAnalyzer.analyzeHealth(
            vehicle = vehicle,
            services = completedServices,
            insurances = emptyList(),
            inspections = emptyList()
        )

        assertEquals(33, report.healthScorePercent)
        assertEquals(true, report.checklist.first { it.title == "روغن موتور" }.isOk)
        assertEquals(true, report.checklist.first { it.title == "فیلتر روغن" }.isOk)
        assertEquals(true, report.checklist.first { it.title == "فیلتر هوا" }.isOk)
        assertEquals(9, report.checklist.size)
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
    @Test
    fun timelineHidesOnlyLinkedExpensesAndKeepsIndependentExpensesInSameCategories() {
        val service = VehicleServiceEntity(
            id = "service-1",
            vehicleId = "vehicle-1",
            title = "تعویض روغن",
            serviceType = ServiceType.OIL_CHANGE,
            date = "1405/08/10",
            mileage = 50000,
            cost = 100L
        )
        val insurance = com.example.vehicle.data.VehicleInsuranceEntity(
            id = "insurance-1",
            vehicleId = "vehicle-1",
            company = "بیمه تست",
            type = "شخص ثالث",
            startDate = "1405/08/10",
            endDate = "1406/08/10",
            amount = 200L
        )
        val inspection = com.example.vehicle.data.VehicleInspectionEntity(
            id = "inspection-1",
            vehicleId = "vehicle-1",
            lastInspectionDate = "1405/08/10",
            expiryDate = "1406/08/10",
            cost = 300L
        )
        val expenses = listOf(
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "exp_svc_service-1", vehicleId = "vehicle-1", title = "هزینه سرویس",
                category = com.example.vehicle.data.VehicleExpenseCategory.SERVICE,
                amount = 100L, date = "1405/08/10"
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "exp_ins_insurance-1", vehicleId = "vehicle-1", title = "حق بیمه",
                category = com.example.vehicle.data.VehicleExpenseCategory.INSURANCE,
                amount = 200L, date = "1405/08/10"
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "exp_insp_inspection-1", vehicleId = "vehicle-1", title = "هزینه معاینه",
                category = com.example.vehicle.data.VehicleExpenseCategory.INSPECTION,
                amount = 300L, date = "1405/08/10"
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "manual-service", vehicleId = "vehicle-1", title = "هزینه مستقل سرویس",
                category = com.example.vehicle.data.VehicleExpenseCategory.SERVICE,
                amount = 40L, date = "1405/08/11"
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "manual-insurance", vehicleId = "vehicle-1", title = "هزینه مستقل بیمه",
                category = com.example.vehicle.data.VehicleExpenseCategory.INSURANCE,
                amount = 50L, date = "1405/08/11"
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "manual-inspection", vehicleId = "vehicle-1", title = "هزینه مستقل معاینه",
                category = com.example.vehicle.data.VehicleExpenseCategory.INSPECTION,
                amount = 60L, date = "1405/08/11"
            )
        )

        val timeline = VehicleAnalyzer.buildTimeline(
            vehicleId = "vehicle-1",
            services = listOf(service),
            expenses = expenses,
            insurances = listOf(insurance),
            inspections = listOf(inspection)
        )

        val expenseEventIds = timeline
            .filterIsInstance<com.example.vehicle.data.VehicleTimelineEvent.ExpenseEvent>()
            .map { it.id }

        assertEquals(
            setOf("manual-service", "manual-insurance", "manual-inspection"),
            expenseEventIds.toSet()
        )
        assertEquals(3, expenseEventIds.size)
    }

}
