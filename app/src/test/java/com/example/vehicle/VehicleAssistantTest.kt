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
    fun vehicleStatisticsNormalizePersianAndAsciiDatesAndDoNotUseLifetimeTotalAsYearlyTotal() {
        val currentDate = com.example.util.PersianCalendarHelper
            .fromEpochMillis(System.currentTimeMillis())
            .toFormattedDate()
        val asciiDate = com.example.util.IranianPhoneUtils.convertDigitsToEnglish(currentDate)
        val vehicleId = "stats-car"
        val expenses = listOf(
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "current-persian",
                vehicleId = vehicleId,
                title = "هزینه با ارقام فارسی",
                category = VehicleExpenseCategory.FUEL,
                amount = 100L,
                date = currentDate
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "current-ascii",
                vehicleId = vehicleId,
                title = "هزینه با ارقام لاتین",
                category = VehicleExpenseCategory.REPAIRS,
                amount = 200L,
                date = asciiDate
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "historical",
                vehicleId = vehicleId,
                title = "هزینه سال‌های قبل",
                category = VehicleExpenseCategory.OTHER,
                amount = 900L,
                date = "1400/01/01"
            )
        )

        val stats = VehicleAnalyzer.computeStatistics(
            vehicleId = vehicleId,
            expenses = expenses,
            services = emptyList()
        )

        assertEquals(300L, stats.currentMonthCost)
        assertEquals(300L, stats.yearlyCost)
    }

    @Test
    fun vehicleHealthUsesLatestServiceAndNormalizesExpiryDates() {
        val vehicle = com.example.vehicle.data.VehicleEntity(
            id = "health-car",
            brand = "برند",
            model = "مدل",
            year = "1402",
            color = "سفید",
            plate = "",
            currentMileage = 57000
        )
        val services = listOf(
            com.example.vehicle.data.VehicleServiceEntity(
                id = "older-oil",
                vehicleId = vehicle.id,
                title = "سرویس قدیمی",
                serviceType = ServiceType.OIL_CHANGE,
                date = "1405/01/01",
                mileage = 45000,
                cost = 100L,
                nextReminderMileage = 50000
            ),
            com.example.vehicle.data.VehicleServiceEntity(
                id = "latest-oil",
                vehicleId = vehicle.id,
                title = "سرویس جدید",
                serviceType = ServiceType.OIL_CHANGE,
                date = "۱۴۰۵/۰۶/۰۱",
                mileage = 55000,
                cost = 100L,
                nextReminderMileage = 60000
            )
        )
        val insurance = com.example.vehicle.data.VehicleInsuranceEntity(
            id = "expired-insurance",
            vehicleId = vehicle.id,
            company = "بیمه",
            type = "شخص ثالث",
            startDate = "۱۳۹۹/۰۱/۰۱",
            endDate = "۱۴۰۰/۰۱/۰۱",
            amount = 1000L
        )
        val inspection = com.example.vehicle.data.VehicleInspectionEntity(
            id = "expired-inspection",
            vehicleId = vehicle.id,
            lastInspectionDate = "1399/01/01",
            expiryDate = "1400/01/01",
            cost = 100L
        )

        val report = VehicleAnalyzer.analyzeHealth(
            vehicle = vehicle,
            services = services,
            insurances = listOf(insurance),
            inspections = listOf(inspection)
        )

        val oilCheck = report.checklist.first { it.title == "روغن و فیلتر موتور" }
        assertTrue(oilCheck.statusText.contains("3000"))
        assertFalse(oilCheck.statusText.contains("معوق"))
        assertTrue(report.checklist.first { it.title == "بیمه‌نامه خودرو" }.statusText.contains("منقضی شده"))
        assertTrue(report.checklist.first { it.title == "معاینه فنی و آلایندگی" }.statusText.contains("منقضی شده"))
    }

    @Test
    fun vehicleHealthDoesNotTreatInvalidExpiryDatesAsValid() {
        val vehicle = com.example.vehicle.data.VehicleEntity(
            id = "invalid-dates-car",
            brand = "برند",
            model = "مدل",
            year = "1402",
            color = "سفید",
            plate = "",
            currentMileage = 10000
        )
        val insurance = com.example.vehicle.data.VehicleInsuranceEntity(
            id = "invalid-insurance",
            vehicleId = vehicle.id,
            company = "بیمه",
            type = "شخص ثالث",
            startDate = "۱۴۰۵/۰۱/۰۱",
            endDate = "تاریخ نامشخص",
            amount = 1000L
        )
        val inspection = com.example.vehicle.data.VehicleInspectionEntity(
            id = "invalid-inspection",
            vehicleId = vehicle.id,
            lastInspectionDate = "1405/01/01",
            expiryDate = "نامشخص",
            cost = 100L
        )

        val report = VehicleAnalyzer.analyzeHealth(
            vehicle = vehicle,
            services = emptyList(),
            insurances = listOf(insurance),
            inspections = listOf(inspection)
        )

        assertTrue(report.checklist.first { it.title == "بیمه‌نامه خودرو" }.statusText.contains("نامعتبر"))
        assertTrue(report.checklist.first { it.title == "معاینه فنی و آلایندگی" }.statusText.contains("نامعتبر"))
    }

    @Test
    fun vehicleStatisticsReturnZeroForYearWithoutExpenses() {
        val currentYear = com.example.util.PersianCalendarHelper
            .fromEpochMillis(System.currentTimeMillis())
            .year
        val expenses = listOf(
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "old-expense",
                vehicleId = "stats-car",
                title = "هزینه قدیمی",
                category = VehicleExpenseCategory.FUEL,
                amount = 900L,
                date = "${currentYear - 1}/01/01"
            )
        )

        val stats = VehicleAnalyzer.computeStatistics(
            vehicleId = "stats-car",
            expenses = expenses,
            services = emptyList()
        )

        assertEquals(0L, stats.yearlyCost)
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
}
