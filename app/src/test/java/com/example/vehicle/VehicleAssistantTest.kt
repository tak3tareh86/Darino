package com.example.vehicle

import com.example.vehicle.data.ServiceType
import com.example.vehicle.data.VehicleExpenseCategory
import com.example.vehicle.data.VehicleRepository
import com.example.vehicle.domain.VehicleAnalyzer
import com.example.vehicle.domain.VehicleHealthLevel
import com.example.vehicle.domain.VehicleManager
import com.example.vehicle.domain.VehicleMaintenanceCatalog
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VehicleAssistantTest {

    private lateinit var repository: VehicleRepository

    @Before
    fun setUp() {
        repository = VehicleRepository()
    }

    @Test
    fun testAddVehicleAndServiceWorkflow() = kotlinx.coroutines.runBlocking {
        // 1. Add Peugeot 207 (1402, 45,000 km)
        val carResult = repository.addVehicle(
            brand = "ایران‌خودرو",
            model = "پژو 207i",
            year = "1402",
            color = "سفید",
            plate = "ایران ۶۸ - ۳۴۵ ب ۱۲",
            vin = "IRAN-PEUG-207-88910",
            currentMileage = 45000,
            estimatedValue = 850_000_000L
        )
        val car = carResult.getOrThrow()

        assertNotNull(car)
        assertEquals("پژو 207i", car.model)
        assertEquals(45000, car.currentMileage)

        // 2. Add Oil Change (850,000 Toman)
        val serviceResult = repository.addService(
            vehicleId = car.id,
            title = "تعویض روغن موتور و فیلترها",
            serviceType = ServiceType.OIL_CHANGE,
            date = "1405/06/10",
            mileage = 45000,
            cost = 850_000L,
            description = "روغن ۱۰-۴۰ بهران سوپر رانا + فیلتر سرکان",
            nextReminderDate = "1405/12/10",
            nextReminderMileage = 50000,
            isReminderEnabled = true
        )
        val service = serviceResult.getOrThrow()

        assertNotNull(service)
        assertEquals(850_000L, service.cost)
        assertEquals(50000, service.nextReminderMileage)

        // Verify that adding service automatically logged an expense
        val expenses = repository.expenses.value.filter { it.vehicleId == car.id }
        assertTrue(expenses.any { it.category == VehicleExpenseCategory.SERVICE && it.amount == 850_000L })

        // 3. Test Health Analysis
        val health = VehicleAnalyzer.analyzeHealth(
            vehicle = car,
            services = repository.services.value,
            insurances = repository.insurances.value,
            inspections = repository.inspections.value
        )

        assertNotNull(health)
        assertTrue(health.healthScorePercent > 0)
        assertFalse(health.checklist.isEmpty())

        // 4. Test Statistics & Reports
        val stats = VehicleAnalyzer.computeStatistics(
            vehicleId = car.id,
            expenses = repository.expenses.value,
            services = repository.services.value
        )

        assertNotNull(stats)
        assertTrue(stats.totalServicesCount >= 1)

        // 5. Test Timeline
        val timeline = VehicleAnalyzer.buildTimeline(
            vehicleId = car.id,
            services = repository.services.value,
            expenses = repository.expenses.value,
            insurances = repository.insurances.value
        )

        assertTrue(timeline.isNotEmpty())
    }

    @Test
    fun serviceIntervalsUseVehicleProfileAndDoNotLeakTaraRules() {
        val tara = com.example.vehicle.data.VehicleEntity(
            id = "tara",
            brand = "ایران‌خودرو",
            model = "تارا",
            year = "1403",
            color = "سفید",
            plate = "",
            currentMileage = 30000
        )
        val peugeot = com.example.vehicle.data.VehicleEntity(
            id = "207",
            brand = "ایران‌خودرو",
            model = "پژو 207i",
            year = "1402",
            color = "سفید",
            plate = "",
            currentMileage = 45000
        )
        val taraBrakeFluid = VehicleMaintenanceCatalog.resolve(tara, ServiceType.BRAKE_FLUID_CHANGE)
        val peugeotBrakeFluid = VehicleMaintenanceCatalog.resolve(peugeot, ServiceType.BRAKE_FLUID_CHANGE)
        val taraManualTransmission = VehicleMaintenanceCatalog.resolve(tara, ServiceType.TRANSMISSION_OIL_CHANGE)
        val taraAutomatic = VehicleMaintenanceCatalog.resolve(
            tara.copy(transmission = "اتوماتیک DAE شش‌سرعته"),
            ServiceType.TRANSMISSION_OIL_CHANGE
        )
        val oil = VehicleMaintenanceCatalog.resolve(peugeot, ServiceType.OIL_CHANGE)

        assertEquals(40000, taraBrakeFluid.intervalKm)
        assertEquals(24, taraBrakeFluid.intervalMonths)
        assertEquals(0, peugeotBrakeFluid.intervalKm)
        assertEquals(0, peugeotBrakeFluid.intervalMonths)
        assertEquals(0, taraManualTransmission.intervalKm)
        assertEquals(60000, taraAutomatic.intervalKm)
        assertEquals(60, taraAutomatic.intervalMonths)
        assertEquals(5000, oil.intervalKm)
        assertEquals(6, oil.intervalMonths)
    }

    @Test
    fun healthAnalysisUsesLatestServicePolicyAndInspection() {
        val vehicle = com.example.vehicle.data.VehicleEntity(
            id = "health-car",
            brand = "Test",
            model = "Vehicle",
            year = "1402",
            color = "سفید",
            plate = "TEST",
            currentMileage = 45_000
        )
        val services = listOf(
            com.example.vehicle.data.VehicleServiceEntity(
                id = "old-oil",
                vehicleId = vehicle.id,
                title = "سرویس قدیمی",
                serviceType = ServiceType.OIL_CHANGE,
                date = "1404/01/01",
                mileage = 30_000,
                cost = 100_000,
                nextReminderMileage = 35_000
            ),
            com.example.vehicle.data.VehicleServiceEntity(
                id = "latest-oil",
                vehicleId = vehicle.id,
                title = "سرویس جدید",
                serviceType = ServiceType.OIL_CHANGE,
                date = "1405/06/01",
                mileage = 40_000,
                cost = 200_000,
                nextReminderMileage = 60_000
            )
        )
        val insurances = listOf(
            com.example.vehicle.data.VehicleInsuranceEntity(
                id = "expired-policy",
                vehicleId = vehicle.id,
                company = "بیمه قدیمی",
                type = "شخص ثالث",
                startDate = "1403/01/01",
                endDate = "1404/01/01",
                amount = 100_000
            ),
            com.example.vehicle.data.VehicleInsuranceEntity(
                id = "active-policy",
                vehicleId = vehicle.id,
                company = "بیمه معتبر",
                type = "شخص ثالث",
                startDate = "1405/01/01",
                endDate = "1406/01/01",
                amount = 200_000
            )
        )
        val inspections = listOf(
            com.example.vehicle.data.VehicleInspectionEntity(
                id = "old-inspection",
                vehicleId = vehicle.id,
                lastInspectionDate = "1404/01/01",
                expiryDate = "1404/12/29",
                cost = 50_000
            ),
            com.example.vehicle.data.VehicleInspectionEntity(
                id = "latest-inspection",
                vehicleId = vehicle.id,
                lastInspectionDate = "1405/05/01",
                expiryDate = "1406/05/01",
                cost = 60_000,
                status = "معتبر"
            )
        )

        val report = VehicleAnalyzer.analyzeHealth(vehicle, services, insurances, inspections)

        assertTrue(report.checklist.first { it.title == "روغن و فیلتر موتور" }.statusText.contains("15000"))
        assertTrue(report.checklist.first { it.title == "بیمه‌نامه خودرو" }.statusText.contains("بیمه معتبر"))
        assertTrue(report.checklist.first { it.title == "معاینه فنی و آلایندگی" }.isOk)
    }

    @Test
    fun missingInspectionAndInsuranceAreNotReportedAsCompliant() {
        val vehicle = com.example.vehicle.data.VehicleEntity(
            id = "unknown-docs-car",
            brand = "Test",
            model = "Vehicle",
            year = "1402",
            color = "سفید",
            plate = "TEST",
            currentMileage = 0
        )

        val report = VehicleAnalyzer.analyzeHealth(
            vehicle = vehicle,
            services = emptyList(),
            insurances = emptyList(),
            inspections = emptyList()
        )

        assertFalse(report.checklist.first { it.title == "بیمه‌نامه خودرو" }.isOk)
        assertFalse(report.checklist.first { it.title == "معاینه فنی و آلایندگی" }.isOk)
    }

    @Test
    fun yearlyCostDoesNotFallBackToAllTimeExpensesWhenCurrentYearIsZero() {
        val currentYear = com.example.util.PersianCalendarHelper
            .fromEpochMillis(System.currentTimeMillis()).year
        val vehicleId = "stats-car"
        val expenses = listOf(
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "old-expense",
                vehicleId = vehicleId,
                title = "هزینه سال قبل",
                category = VehicleExpenseCategory.FUEL,
                amount = 123_000L,
                date = "${currentYear - 1}/12/29"
            )
        )

        val stats = VehicleAnalyzer.computeStatistics(vehicleId, expenses, emptyList())

        assertEquals(0L, stats.yearlyCost)
    }

    @Test
    fun testSmartReminderCalculation() {
        val (nextKm, nextDate) = VehicleManager.calculateRecommendedReminder(
            serviceType = ServiceType.OIL_CHANGE,
            currentMileage = 45000,
            currentShamsiDate = "1405/06/10"
        )

        assertEquals(50000, nextKm)
        assertEquals("۱۴۰۵/۱۲/۱۰", nextDate)
    }
}
