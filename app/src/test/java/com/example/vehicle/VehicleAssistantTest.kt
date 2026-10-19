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
    fun healthUsesMostRecentOilServiceAndChecksSavedDueDate() {
        val vehicle = com.example.vehicle.data.VehicleEntity(
            id = "health-car",
            brand = "سایپا",
            model = "تیبا",
            year = "1402",
            color = "سفید",
            plate = "",
            currentMileage = 25000
        )
        val olderService = com.example.vehicle.data.VehicleServiceEntity(
            id = "old-oil",
            vehicleId = vehicle.id,
            title = "تعویض روغن قدیمی",
            serviceType = ServiceType.OIL_CHANGE,
            date = "۱۴۰۴/۰۱/۰۱",
            mileage = 10000,
            cost = 0L,
            nextReminderMileage = 20000
        )
        val newerService = com.example.vehicle.data.VehicleServiceEntity(
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
    fun yearlyExpenseDoesNotFallBackToLifetimeTotalWhenCurrentYearIsZero() {
        val currentYear = com.example.util.PersianCalendarHelper
            .fromEpochMillis(System.currentTimeMillis()).year
        val oldExpense = com.example.vehicle.data.VehicleExpenseEntity(
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
