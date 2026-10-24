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
    fun testSmartReminderCalculation() {
        val (nextKm, nextDate) = VehicleManager.calculateRecommendedReminder(
            serviceType = ServiceType.OIL_CHANGE,
            currentMileage = 45000,
            currentShamsiDate = "1405/06/10"
        )

        assertEquals(50000, nextKm)
        assertEquals("۱۴۰۵/۱۲/۱۰", nextDate)
    }

    @Test
    fun testComputeStatisticsYearlyCostNormalizationAndFallback() {
        val currentYear = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).year
        val currentYearStrPersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian(currentYear.toString())
        val pastYear = currentYear - 1
        val pastYearStrPersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian(pastYear.toString())

        val expenses = listOf(
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "1",
                vehicleId = "car1",
                title = "هزینه سال قبل",
                category = VehicleExpenseCategory.FUEL,
                amount = 5_000_000L,
                date = "$pastYearStrPersian/05/10",
                description = ""
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "2",
                vehicleId = "car1",
                title = "هزینه سال جاری فارسی",
                category = VehicleExpenseCategory.FUEL,
                amount = 2_000_000L,
                date = "$currentYearStrPersian/02/10",
                description = ""
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "3",
                vehicleId = "car1",
                title = "هزینه سال جاری انگلیسی",
                category = VehicleExpenseCategory.FUEL,
                amount = 3_000_000L,
                date = "$currentYear/03/10",
                description = ""
            )
        )

        val stats = VehicleAnalyzer.computeStatistics(
            vehicleId = "car1",
            expenses = expenses,
            services = emptyList()
        )

        assertEquals(5_000_000L, stats.yearlyCost)

        val pastOnlyExpenses = listOf(
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "1",
                vehicleId = "car1",
                title = "هزینه سال قبل",
                category = VehicleExpenseCategory.FUEL,
                amount = 5_000_000L,
                date = "$pastYearStrPersian/05/10",
                description = ""
            )
        )
        val statsPastOnly = VehicleAnalyzer.computeStatistics(
            vehicleId = "car1",
            expenses = pastOnlyExpenses,
            services = emptyList()
        )
        assertEquals(0L, statsPastOnly.yearlyCost)
    }
}
