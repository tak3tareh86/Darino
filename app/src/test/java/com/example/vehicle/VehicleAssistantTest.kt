package com.example.vehicle

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.SessionManager
import com.example.vehicle.data.ServiceType
import com.example.vehicle.data.VehicleExpenseCategory
import com.example.vehicle.data.VehicleRepository
import com.example.vehicle.domain.VehicleAnalyzer
import com.example.vehicle.domain.VehicleHealthLevel
import com.example.vehicle.domain.VehicleManager
import com.example.vehicle.domain.VehicleMaintenanceCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VehicleAssistantTest {

    private lateinit var context: Context
    private lateinit var repository: VehicleRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SessionManager.init(context)
        SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(
                id = "test_user_vehicle",
                fullName = "کاربر تست",
                email = null,
                phoneNumber = "09120000000",
                phoneVerified = true
            )
        )
        repository = VehicleRepository()
        repository.initDatabase(context)
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

        // Allow Room flow collectors to emit updated entities to StateFlow
        withTimeoutOrNull(2000) {
            repository.expenses.first { list -> list.any { it.vehicleId == car.id } }
        }
        withTimeoutOrNull(2000) {
            repository.services.first { list -> list.any { it.id == service.id } }
        }

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

    @Test
    fun testComputeStatisticsMonthlyChartDataDigitNormalization() {
        val currentPdt = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val currentYear = currentPdt.year
        val currentMonth = currentPdt.month
        val currentYearPersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian(currentYear.toString())
        val currentMonthPersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian("%02d".format(currentMonth))

        var prevMonth = currentMonth - 1
        var prevYear = currentYear
        if (prevMonth <= 0) {
            prevMonth = 12
            prevYear -= 1
        }
        val prevYearPersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian(prevYear.toString())
        val prevMonthPersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian("%02d".format(prevMonth))

        val expenses = listOf(
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "m1",
                vehicleId = "car1",
                title = "بنزین ماه جاری فارسی",
                category = VehicleExpenseCategory.FUEL,
                amount = 1_000_000L,
                date = "$currentYearPersian/$currentMonthPersian/05",
                description = ""
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "m2",
                vehicleId = "car1",
                title = "بنزین ماه جاری انگلیسی",
                category = VehicleExpenseCategory.FUEL,
                amount = 2_000_000L,
                date = "%04d/%02d/12".format(currentYear, currentMonth),
                description = ""
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "m3",
                vehicleId = "car1",
                title = "سرویس ماه قبل فارسی",
                category = VehicleExpenseCategory.SERVICE,
                amount = 3_000_000L,
                date = "$prevYearPersian/$prevMonthPersian/15",
                description = ""
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "m4",
                vehicleId = "car1",
                title = "سرویس ماه قبل انگلیسی",
                category = VehicleExpenseCategory.SERVICE,
                amount = 4_000_000L,
                date = "%04d/%02d/20".format(prevYear, prevMonth),
                description = ""
            )
        )

        val stats = VehicleAnalyzer.computeStatistics(
            vehicleId = "car1",
            expenses = expenses,
            services = emptyList()
        )

        val chartData = stats.monthlyChartData
        assertEquals(6, chartData.size)

        // The last item in monthlyChartData (index 5) is the current month
        val currentMonthBar = chartData.last()
        assertTrue(currentMonthBar.isCurrentMonth)
        assertEquals(3_000_000L, currentMonthBar.fuelAmount) // 1M Persian + 2M English
        assertEquals(0L, currentMonthBar.serviceAmount)
        assertEquals(3_000_000L, currentMonthBar.totalAmount)

        // The second to last item (index 4) is the previous month
        val prevMonthBar = chartData[4]
        assertFalse(prevMonthBar.isCurrentMonth)
        assertEquals(0L, prevMonthBar.fuelAmount)
        assertEquals(7_000_000L, prevMonthBar.serviceAmount) // 3M Persian + 4M English
        assertEquals(7_000_000L, prevMonthBar.totalAmount)
    }

    @Test
    fun testInsuranceHealthCheckVariations() {
        val currentPdt = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val todayEnglish = "%04d/%02d/%02d".format(currentPdt.year, currentPdt.month, currentPdt.day)
        val todayPersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian(todayEnglish)

        val pastEnglish = "%04d/%02d/%02d".format(currentPdt.year - 1, currentPdt.month, currentPdt.day)
        val pastPersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian(pastEnglish)

        val futureEnglish = "%04d/%02d/%02d".format(currentPdt.year + 1, currentPdt.month, currentPdt.day)
        val futurePersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian(futureEnglish)

        val car = com.example.vehicle.data.VehicleEntity(
            id = "car_ins_test",
            brand = "پژو",
            model = "207",
            year = "1402",
            color = "سفید",
            plate = "۱۲ الف ۳۴۵ ایران ۶۸",
            currentMileage = 40000
        )

        // 1. Future date with English digits
        val futureInsEnglish = listOf(
            com.example.vehicle.data.VehicleInsuranceEntity(
                id = "ins1",
                vehicleId = car.id,
                company = "بیمه ایران",
                type = "شخص ثالث",
                startDate = pastEnglish,
                endDate = futureEnglish,
                amount = 5_000_000L
            )
        )
        val healthFutureEng = VehicleAnalyzer.analyzeHealth(car, emptyList(), futureInsEnglish, emptyList())
        val itemFutureEng = healthFutureEng.checklist.first { it.title == "بیمه‌نامه خودرو" }
        assertTrue(itemFutureEng.isOk)
        assertFalse(itemFutureEng.isWarning)
        assertEquals("دارای اعتبار (بیمه ایران)", itemFutureEng.statusText)

        // 2. Future date with Persian digits (must match English result)
        val futureInsPersian = listOf(
            futureInsEnglish[0].copy(endDate = futurePersian)
        )
        val healthFuturePer = VehicleAnalyzer.analyzeHealth(car, emptyList(), futureInsPersian, emptyList())
        val itemFuturePer = healthFuturePer.checklist.first { it.title == "بیمه‌نامه خودرو" }
        assertTrue(itemFuturePer.isOk)
        assertFalse(itemFuturePer.isWarning)
        assertEquals(itemFutureEng.isOk, itemFuturePer.isOk)
        assertEquals(healthFutureEng.healthScorePercent, healthFuturePer.healthScorePercent)

        // 3. Past date with Persian digits
        val pastInsPersian = listOf(
            futureInsEnglish[0].copy(endDate = pastPersian)
        )
        val healthPastPer = VehicleAnalyzer.analyzeHealth(car, emptyList(), pastInsPersian, emptyList())
        val itemPastPer = healthPastPer.checklist.first { it.title == "بیمه‌نامه خودرو" }
        assertFalse(itemPastPer.isOk)
        assertTrue(itemPastPer.isWarning)
        assertTrue(itemPastPer.statusText.contains("منقضی شده"))

        // 4. Past date with English digits (must match Persian result)
        val pastInsEnglish = listOf(
            futureInsEnglish[0].copy(endDate = pastEnglish)
        )
        val healthPastEng = VehicleAnalyzer.analyzeHealth(car, emptyList(), pastInsEnglish, emptyList())
        val itemPastEng = healthPastEng.checklist.first { it.title == "بیمه‌نامه خودرو" }
        assertFalse(itemPastEng.isOk)
        assertTrue(itemPastEng.isWarning)
        assertEquals(itemPastPer.isOk, itemPastEng.isOk)
        assertEquals(healthPastPer.healthScorePercent, healthPastEng.healthScorePercent)

        // 5. Date equal to today (valid until end of day)
        val todayIns = listOf(
            futureInsEnglish[0].copy(endDate = todayPersian)
        )
        val healthToday = VehicleAnalyzer.analyzeHealth(car, emptyList(), todayIns, emptyList())
        val itemToday = healthToday.checklist.first { it.title == "بیمه‌نامه خودرو" }
        assertTrue(itemToday.isOk)
        assertFalse(itemToday.isWarning)

        // 6. Blank / empty date
        val emptyDateIns = listOf(
            futureInsEnglish[0].copy(endDate = "")
        )
        val healthEmpty = VehicleAnalyzer.analyzeHealth(car, emptyList(), emptyDateIns, emptyList())
        val itemEmpty = healthEmpty.checklist.first { it.title == "بیمه‌نامه خودرو" }
        assertFalse(itemEmpty.isOk)
        assertTrue(itemEmpty.isWarning)
        assertEquals("تاریخ انقضا نامعتبر یا ثبت نشده (بیمه ایران)", itemEmpty.statusText)

        // 7. Incomplete or invalid date format
        val invalidDateIns = listOf(
            futureInsEnglish[0].copy(endDate = "1405/99/99")
        )
        val healthInvalid = VehicleAnalyzer.analyzeHealth(car, emptyList(), invalidDateIns, emptyList())
        val itemInvalid = healthInvalid.checklist.first { it.title == "بیمه‌نامه خودرو" }
        assertFalse(itemInvalid.isOk)
        assertTrue(itemInvalid.isWarning)
        assertEquals("تاریخ انقضا نامعتبر یا ثبت نشده (بیمه ایران)", itemInvalid.statusText)
        assertEquals(45, healthInvalid.healthScorePercent) // 100 - 30 (services) - 25 (invalid insurance)

        // 8. Impossible date: Month 7 with 31 days (Mehr has max 30 days)
        val futureYear = currentPdt.year + 1
        val impossibleDateStr = "%04d/07/31".format(futureYear)
        val impossibleDateIns = listOf(
            futureInsEnglish[0].copy(endDate = impossibleDateStr)
        )
        val healthImpossible = VehicleAnalyzer.analyzeHealth(car, emptyList(), impossibleDateIns, emptyList())
        val itemImpossible = healthImpossible.checklist.first { it.title == "بیمه‌نامه خودرو" }
        assertFalse("$impossibleDateStr must NOT be recognized as valid", itemImpossible.isOk)
        assertTrue(itemImpossible.isWarning)
        assertEquals("تاریخ انقضا نامعتبر یا ثبت نشده (بیمه ایران)", itemImpossible.statusText)
        assertEquals(45, healthImpossible.healthScorePercent)

        // 9. Valid day 31 date: Month 2 with 31 days (Ordibehesht has 31 days) in future year
        val futureDay31 = "%04d/02/31".format(futureYear)
        val validDay31Ins = listOf(
            futureInsEnglish[0].copy(endDate = futureDay31)
        )
        val healthValidDay31 = VehicleAnalyzer.analyzeHealth(car, emptyList(), validDay31Ins, emptyList())
        val itemValidDay31 = healthValidDay31.checklist.first { it.title == "بیمه‌نامه خودرو" }
        assertTrue("$futureDay31 must be recognized as valid", itemValidDay31.isOk)
        assertFalse(itemValidDay31.isWarning)
        assertEquals(70, healthValidDay31.healthScorePercent) // 100 - 30 (services)

        // 10. Non-leap year Esfand 30 (1405 is not leap, max 29 days)
        val nonLeapEsfand30Ins = listOf(
            futureInsEnglish[0].copy(endDate = "1405/12/30")
        )
        val healthNonLeap = VehicleAnalyzer.analyzeHealth(car, emptyList(), nonLeapEsfand30Ins, emptyList())
        val itemNonLeap = healthNonLeap.checklist.first { it.title == "بیمه‌نامه خودرو" }
        assertFalse("1405/12/30 is not a leap year, must be invalid", itemNonLeap.isOk)
        assertTrue(itemNonLeap.isWarning)
        assertEquals(45, healthNonLeap.healthScorePercent)
    }

    @Test
    fun testInspectionHealthCheckVariations() {
        val currentPdt = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val todayEnglish = "%04d/%02d/%02d".format(currentPdt.year, currentPdt.month, currentPdt.day)
        val todayPersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian(todayEnglish)

        val pastEnglish = "%04d/%02d/%02d".format(currentPdt.year - 1, currentPdt.month, currentPdt.day)
        val pastPersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian(pastEnglish)

        val futureYear = currentPdt.year + 1
        val futureEnglish = "%04d/%02d/%02d".format(futureYear, currentPdt.month, currentPdt.day)
        val futurePersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian(futureEnglish)
        val futureDay31 = "%04d/02/31".format(futureYear)
        val impossibleDateStr = "%04d/07/31".format(futureYear)

        val car = com.example.vehicle.data.VehicleEntity(
            id = "car_insp_test",
            brand = "پژو",
            model = "207",
            year = "1402",
            color = "سفید",
            plate = "۱۲ الف ۳۴۵ ایران ۶۸",
            currentMileage = 40000
        )

        val validIns = listOf(
            com.example.vehicle.data.VehicleInsuranceEntity(
                id = "ins_mock",
                vehicleId = car.id,
                company = "بیمه ایران",
                type = "شخص ثالث",
                startDate = pastEnglish,
                endDate = futureEnglish,
                amount = 5_000_000L
            )
        )

        // 1. Future date with English digits
        val futureInspEnglish = listOf(
            com.example.vehicle.data.VehicleInspectionEntity(
                id = "insp1",
                vehicleId = car.id,
                lastInspectionDate = pastEnglish,
                expiryDate = futureEnglish,
                cost = 200_000L
            )
        )
        val healthFutureEng = VehicleAnalyzer.analyzeHealth(car, emptyList(), validIns, futureInspEnglish)
        val itemFutureEng = healthFutureEng.checklist.first { it.title == "معاینه فنی و آلایندگی" }
        assertTrue(itemFutureEng.isOk)
        assertFalse(itemFutureEng.isWarning)
        assertEquals(70, healthFutureEng.healthScorePercent)

        // 2. Future date with Persian digits (must match English result)
        val futureInspPersian = listOf(
            futureInspEnglish[0].copy(expiryDate = futurePersian)
        )
        val healthFuturePer = VehicleAnalyzer.analyzeHealth(car, emptyList(), validIns, futureInspPersian)
        val itemFuturePer = healthFuturePer.checklist.first { it.title == "معاینه فنی و آلایندگی" }
        assertTrue(itemFuturePer.isOk)
        assertFalse(itemFuturePer.isWarning)
        assertEquals(itemFutureEng.isOk, itemFuturePer.isOk)
        assertEquals(healthFutureEng.healthScorePercent, healthFuturePer.healthScorePercent)

        // 3. Past date with Persian digits
        val pastInspPersian = listOf(
            futureInspEnglish[0].copy(expiryDate = pastPersian)
        )
        val healthPastPer = VehicleAnalyzer.analyzeHealth(car, emptyList(), validIns, pastInspPersian)
        val itemPastPer = healthPastPer.checklist.first { it.title == "معاینه فنی و آلایندگی" }
        assertFalse(itemPastPer.isOk)
        assertTrue(itemPastPer.isWarning)
        assertTrue(itemPastPer.statusText.contains("معاینه فنی منقضی شده"))
        assertEquals(50, healthPastPer.healthScorePercent) // 100 - 30 (services) - 20 (expired inspection)

        // 4. Past date with English digits (must match Persian result)
        val pastInspEnglish = listOf(
            futureInspEnglish[0].copy(expiryDate = pastEnglish)
        )
        val healthPastEng = VehicleAnalyzer.analyzeHealth(car, emptyList(), validIns, pastInspEnglish)
        val itemPastEng = healthPastEng.checklist.first { it.title == "معاینه فنی و آلایندگی" }
        assertFalse(itemPastEng.isOk)
        assertTrue(itemPastEng.isWarning)
        assertEquals(itemPastPer.isOk, itemPastEng.isOk)
        assertEquals(healthPastPer.healthScorePercent, healthPastEng.healthScorePercent)

        // 5. Date equal to today (valid until end of day)
        val todayInsp = listOf(
            futureInspEnglish[0].copy(expiryDate = todayPersian)
        )
        val healthToday = VehicleAnalyzer.analyzeHealth(car, emptyList(), validIns, todayInsp)
        val itemToday = healthToday.checklist.first { it.title == "معاینه فنی و آلایندگی" }
        assertTrue(itemToday.isOk)
        assertFalse(itemToday.isWarning)
        assertEquals(70, healthToday.healthScorePercent)

        // 6. Blank / empty date
        val emptyDateInsp = listOf(
            futureInspEnglish[0].copy(expiryDate = "")
        )
        val healthEmpty = VehicleAnalyzer.analyzeHealth(car, emptyList(), validIns, emptyDateInsp)
        val itemEmpty = healthEmpty.checklist.first { it.title == "معاینه فنی و آلایندگی" }
        assertFalse(itemEmpty.isOk)
        assertTrue(itemEmpty.isWarning)
        assertEquals("تاریخ انقضا نامعتبر یا ثبت نشده", itemEmpty.statusText)
        assertEquals(55, healthEmpty.healthScorePercent) // 100 - 30 (services) - 15 (invalid inspection)

        // 7. Incomplete or invalid date format
        val invalidDateInsp = listOf(
            futureInspEnglish[0].copy(expiryDate = "invalid-date")
        )
        val healthInvalid = VehicleAnalyzer.analyzeHealth(car, emptyList(), validIns, invalidDateInsp)
        val itemInvalid = healthInvalid.checklist.first { it.title == "معاینه فنی و آلایندگی" }
        assertFalse(itemInvalid.isOk)
        assertTrue(itemInvalid.isWarning)
        assertEquals("تاریخ انقضا نامعتبر یا ثبت نشده", itemInvalid.statusText)
        assertEquals(55, healthInvalid.healthScorePercent)

        // 8. Impossible date: Month 7 with 31 days
        val impossibleDateInsp = listOf(
            futureInspEnglish[0].copy(expiryDate = impossibleDateStr)
        )
        val healthImpossible = VehicleAnalyzer.analyzeHealth(car, emptyList(), validIns, impossibleDateInsp)
        val itemImpossible = healthImpossible.checklist.first { it.title == "معاینه فنی و آلایندگی" }
        assertFalse("$impossibleDateStr must NOT be recognized as valid", itemImpossible.isOk)
        assertTrue(itemImpossible.isWarning)
        assertEquals("تاریخ انقضا نامعتبر یا ثبت نشده", itemImpossible.statusText)
        assertEquals(55, healthImpossible.healthScorePercent)

        // 9. Valid day 31 date: Month 2 with 31 days
        val validDay31Insp = listOf(
            futureInspEnglish[0].copy(expiryDate = futureDay31)
        )
        val healthValidDay31 = VehicleAnalyzer.analyzeHealth(car, emptyList(), validIns, validDay31Insp)
        val itemValidDay31 = healthValidDay31.checklist.first { it.title == "معاینه فنی و آلایندگی" }
        assertTrue("$futureDay31 must be recognized as valid", itemValidDay31.isOk)
        assertFalse(itemValidDay31.isWarning)
        assertEquals(70, healthValidDay31.healthScorePercent)
    }

    @Test
    fun testJalaliDateValidationEdgeCasesAndLeapYears() {
        // 1. Persian and English digits equivalence
        assertEquals("1405/02/31", VehicleAnalyzer.parseAndValidateJalaliDate("۱۴۰۵/۰۲/۳۱"))
        assertEquals("1405/02/31", VehicleAnalyzer.parseAndValidateJalaliDate("1405/02/31"))

        // 2. Delimiter support: '/' and '-'
        assertEquals("1403/12/30", VehicleAnalyzer.parseAndValidateJalaliDate("1403-12-30"))
        assertEquals("1403/12/30", VehicleAnalyzer.parseAndValidateJalaliDate("1403/12/30"))

        // 3. Single-digit month and day padding
        assertEquals("1404/01/05", VehicleAnalyzer.parseAndValidateJalaliDate("1404/1/5"))
        assertEquals("1404/01/05", VehicleAnalyzer.parseAndValidateJalaliDate("۱۴۰۴/۱/۵"))

        // 4. Month lengths: 1..6 have 31 days
        assertEquals("1405/01/31", VehicleAnalyzer.parseAndValidateJalaliDate("1405/01/31"))
        assertEquals("1405/06/31", VehicleAnalyzer.parseAndValidateJalaliDate("1405/06/31"))

        // 5. Month lengths: 7..11 have 30 days (day 31 is impossible)
        assertNull("Month 7 cannot have 31 days", VehicleAnalyzer.parseAndValidateJalaliDate("1405/07/31"))
        assertEquals("1405/07/30", VehicleAnalyzer.parseAndValidateJalaliDate("1405/07/30"))
        assertNull("Month 11 cannot have 31 days", VehicleAnalyzer.parseAndValidateJalaliDate("1405/11/31"))
        assertEquals("1405/11/30", VehicleAnalyzer.parseAndValidateJalaliDate("1405/11/30"))

        // 6. Esfand (Month 12) Leap Year vs Non-Leap Year
        // 1403 is a leap Jalali year (1403 % 33 == 17)
        assertTrue(com.example.util.PersianCalendarHelper.isLeapJalaliYear(1403))
        assertEquals("1403/12/30", VehicleAnalyzer.parseAndValidateJalaliDate("1403/12/30"))
        assertEquals("1403/12/29", VehicleAnalyzer.parseAndValidateJalaliDate("1403/12/29"))

        // 1404 and 1405 are NOT leap Jalali years
        assertFalse(com.example.util.PersianCalendarHelper.isLeapJalaliYear(1404))
        assertFalse(com.example.util.PersianCalendarHelper.isLeapJalaliYear(1405))
        assertNull("1404 is not a leap year, Esfand 30 is invalid", VehicleAnalyzer.parseAndValidateJalaliDate("1404/12/30"))
        assertEquals("1404/12/29", VehicleAnalyzer.parseAndValidateJalaliDate("1404/12/29"))
        assertNull("1405 is not a leap year, Esfand 30 is invalid", VehicleAnalyzer.parseAndValidateJalaliDate("1405/12/30"))

        // 7. Invalid month bounds
        assertNull(VehicleAnalyzer.parseAndValidateJalaliDate("1405/00/10"))
        assertNull(VehicleAnalyzer.parseAndValidateJalaliDate("1405/13/10"))

        // 8. Invalid day bounds (0, 32)
        assertNull(VehicleAnalyzer.parseAndValidateJalaliDate("1405/05/00"))
        assertNull(VehicleAnalyzer.parseAndValidateJalaliDate("1405/05/32"))

        // 9. Blank, empty, null, and non-numeric formats
        assertNull(VehicleAnalyzer.parseAndValidateJalaliDate(null))
        assertNull(VehicleAnalyzer.parseAndValidateJalaliDate(""))
        assertNull(VehicleAnalyzer.parseAndValidateJalaliDate("   "))
        assertNull(VehicleAnalyzer.parseAndValidateJalaliDate("invalid"))
        assertNull(VehicleAnalyzer.parseAndValidateJalaliDate("1405/05"))
        assertNull(VehicleAnalyzer.parseAndValidateJalaliDate("1405/05/05/05"))
    }
}
