package com.example.vehicle

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.SessionManager
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlarmManager
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
        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            com.example.data.database.AppDatabase.getDatabase(context).clearAllTables()
        }
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

    @Test
    fun testVehicleCrudOperationsAndValidation() = kotlinx.coroutines.runBlocking {
        // 1. Validation error states on Vehicle Creation
        val blankBrand = repository.addVehicle("", "دنا", "1401", "سفید", "پلاک ۱")
        assertTrue(blankBrand.isFailure)
        assertTrue(blankBrand.exceptionOrNull() is IllegalArgumentException)

        val blankPlate = repository.addVehicle("ایران خودرو", "دنا", "1401", "سفید", "")
        assertTrue(blankPlate.isFailure)
        assertTrue(blankPlate.exceptionOrNull() is IllegalArgumentException)

        val negativeMileage = repository.addVehicle("ایران خودرو", "دنا", "1401", "سفید", "پلاک ۱", currentMileage = -100)
        assertTrue(negativeMileage.isFailure)
        assertTrue(negativeMileage.exceptionOrNull() is IllegalArgumentException)

        // 2. Successful creation of first vehicle
        val car1Result = repository.addVehicle(
            brand = "ایران خودرو",
            model = "دنا پلاس",
            year = "1401",
            color = "مشکی",
            plate = "۴۴ ج ۶۷۸ ایران ۱۱",
            currentMileage = 20000,
            estimatedValue = 700_000_000L
        )
        assertTrue(car1Result.isSuccess)
        val car1 = car1Result.getOrThrow()
        assertNotNull(car1.id)
        assertEquals("دنا پلاس", car1.model)

        // 3. Duplicate plate prevention
        val duplicatePlateResult = repository.addVehicle(
            brand = "سایپا",
            model = "شاهین",
            year = "1402",
            color = "سفید",
            plate = "۴۴ ج ۶۷۸ ایران ۱۱" // Same plate as car1
        )
        assertTrue("Duplicate plate must be rejected", duplicatePlateResult.isFailure)
        assertTrue(duplicatePlateResult.exceptionOrNull() is IllegalArgumentException)

        // 4. Successful creation of second vehicle
        val car2Result = repository.addVehicle(
            brand = "سایپا",
            model = "شاهین",
            year = "1402",
            color = "سفید",
            plate = "۵۵ د ۷۸۹ ایران ۲۲"
        )
        assertTrue(car2Result.isSuccess)
        val car2 = car2Result.getOrThrow()

        // 5. Update vehicle: preserving ID and entity properties
        val updatedCar1 = car1.copy(color = "آبی کاربنی", currentMileage = 22000)
        val updateResult = repository.updateVehicle(updatedCar1)
        assertTrue(updateResult.isSuccess)
        assertEquals(car1.id, updateResult.getOrThrow().id)
        assertEquals("آبی کاربنی", updateResult.getOrThrow().color)

        // 6. Update vehicle: preventing duplicate plate of another vehicle
        val invalidPlateUpdate = car2.copy(plate = "۴۴ ج ۶۷۸ ایران ۱۱") // car1's plate
        val invalidPlateResult = repository.updateVehicle(invalidPlateUpdate)
        assertTrue("Changing plate to existing plate of another car must fail", invalidPlateResult.isFailure)
        assertTrue(invalidPlateResult.exceptionOrNull() is IllegalArgumentException)

        // 7. Update non-existent vehicle
        val nonExistentCar = car1.copy(id = "non_existent_id")
        val updateNonExistent = repository.updateVehicle(nonExistentCar)
        assertTrue(updateNonExistent.isFailure)
        assertTrue(updateNonExistent.exceptionOrNull() is NoSuchElementException)

        // 8. Delete vehicle and verify clean removal
        val deleteResult = repository.deleteVehicle(car1.id)
        assertTrue(deleteResult.isSuccess)

        val deleteNonExistent = repository.deleteVehicle("non_existent_id")
        assertTrue(deleteNonExistent.isFailure)
    }

    @Test
    fun testExpenseCrudOperationsAndValidation() = kotlinx.coroutines.runBlocking {
        // 1. Setup vehicle
        val car = repository.addVehicle(
            brand = "پژو",
            model = "Pars LX",
            year = "1400",
            color = "خاکستری",
            plate = "۱۲ الف ۳۴۵ ایران ۹۹",
            currentMileage = 50000
        ).getOrThrow()

        // 2. Expense creation input validation
        val blankTitle = repository.addExpense(car.id, "", VehicleExpenseCategory.FUEL, 500_000L, "1405/05/10")
        assertTrue(blankTitle.isFailure)
        assertTrue(blankTitle.exceptionOrNull() is IllegalArgumentException)

        val zeroAmount = repository.addExpense(car.id, "بنزین", VehicleExpenseCategory.FUEL, 0L, "1405/05/10")
        assertTrue(zeroAmount.isFailure)
        assertTrue(zeroAmount.exceptionOrNull() is IllegalArgumentException)

        val invalidDate = repository.addExpense(car.id, "بنزین", VehicleExpenseCategory.FUEL, 500_000L, "1405/07/31") // Month 7 max 30 days
        assertTrue("Impossible date must be rejected", invalidDate.isFailure)
        assertTrue(invalidDate.exceptionOrNull() is IllegalArgumentException)

        val nonExistentCarExpense = repository.addExpense("fake_car_id", "بنزین", VehicleExpenseCategory.FUEL, 500_000L, "1405/05/10")
        assertTrue(nonExistentCarExpense.isFailure)
        assertTrue(nonExistentCarExpense.exceptionOrNull() is NoSuchElementException)

        // 3. Successful expense creation
        val exp1 = repository.addExpense(
            vehicleId = car.id,
            title = "سوخت‌گیری بنزین آزاد",
            category = VehicleExpenseCategory.FUEL,
            amount = 300_000L,
            date = "1405/05/10",
            description = "۶۰ لیتر"
        ).getOrThrow()

        assertNotNull(exp1.id)
        assertEquals(car.id, exp1.vehicleId)

        // 4. Update expense
        val updateExp = repository.updateExpense(
            expenseId = exp1.id,
            title = "سوخت‌گیری بنزین سهمیه‌ای",
            category = VehicleExpenseCategory.FUEL,
            amount = 150_000L,
            date = "1405/05/11",
            description = "۳۰ لیتر"
        )
        assertTrue(updateExp.isSuccess)
        val updated = updateExp.getOrThrow()
        assertEquals("سوخت‌گیری بنزین سهمیه‌ای", updated.title)
        assertEquals(150_000L, updated.amount)

        // 5. Delete expense
        val deleteExp = repository.deleteExpense(exp1.id)
        assertTrue(deleteExp.isSuccess)

        val deleteNonExistent = repository.deleteExpense("fake_exp_id")
        assertTrue(deleteNonExistent.isFailure)
        assertTrue(deleteNonExistent.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun testLicensePlateNormalizationAndDuplicates() = kotlinx.coroutines.runBlocking {
        // 1. Add vehicle with Persian plate
        val persianPlateCar = repository.addVehicle(
            brand = "ایران خودرو",
            model = "تارا",
            year = "1403",
            color = "مشکی",
            plate = "۴۴ ج ۶۷۸ ایران ۱۱"
        ).getOrThrow()

        // 2. Attempt adding another vehicle with English digits equivalent plate
        val englishDigitDuplicate = repository.addVehicle(
            brand = "سایپا",
            model = "کوئیک",
            year = "1402",
            color = "سفید",
            plate = "44 ج 678 ایران 11"
        )
        assertTrue("English digit equivalent plate must be rejected as duplicate", englishDigitDuplicate.isFailure)
        assertTrue(englishDigitDuplicate.exceptionOrNull() is IllegalArgumentException)

        // 3. Attempt adding another vehicle with extra internal spaces
        val extraSpacingDuplicate = repository.addVehicle(
            brand = "پارس خودرو",
            model = "سهند",
            year = "1403",
            color = "نقره‌ای",
            plate = "۴۴   ج   ۶۷۸   ایران   ۱۱"
        )
        assertTrue("Extra spacing plate must be rejected as duplicate", extraSpacingDuplicate.isFailure)
        assertTrue(extraSpacingDuplicate.exceptionOrNull() is IllegalArgumentException)

        // 4. Update the SAME vehicle with formatted/English plate (permitted)
        val selfPlateUpdate = repository.updateVehicle(
            persianPlateCar.copy(plate = "44 ج 678 ایران 11", color = "سرمه‌ای")
        )
        assertTrue("Editing same vehicle with its own plate equivalent must be allowed", selfPlateUpdate.isSuccess)
        assertEquals("سرمه‌ای", selfPlateUpdate.getOrThrow().color)

        // 5. Add second vehicle and attempt changing its plate to first vehicle's plate
        val car2 = repository.addVehicle(
            brand = "کرمان موتور",
            model = "J4",
            year = "1401",
            color = "سفید",
            plate = "۸۸ د ۹۹۹ ایران ۳۳"
        ).getOrThrow()

        val illegalPlateTransfer = repository.updateVehicle(
            car2.copy(plate = "۴۴  ج  ۶۷۸  ایران ۱۱")
        )
        assertTrue("Changing vehicle plate to another vehicle's plate must fail", illegalPlateTransfer.isFailure)
        assertTrue(illegalPlateTransfer.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testCascadingVehicleDeletionVerification() = kotlinx.coroutines.runBlocking {
        val db = com.example.data.database.AppDatabase.getDatabase(context)
        val userId = com.example.data.security.SessionManager.userId!!

        // 1. Add vehicle
        val car = repository.addVehicle(
            brand = "پژو",
            model = "206",
            year = "1399",
            color = "سفید",
            plate = "۷۷ ط ۴۵۶ ایران ۵۵",
            currentMileage = 60000
        ).getOrThrow()

        // 2. Add dependent entities: Service, Expense, Insurance, Inspection
        val service = repository.addService(
            vehicleId = car.id,
            title = "تعویض شمع و وایر",
            serviceType = ServiceType.CUSTOM,
            date = "1405/04/10",
            mileage = 60000,
            cost = 1_200_000L,
            description = "شمع انژکتوری نور",
            nextReminderDate = "1405/10/10",
            nextReminderMileage = 70000,
            isReminderEnabled = true
        ).getOrThrow()

        val standaloneExpense = repository.addExpense(
            vehicleId = car.id,
            title = "کارواش نانو",
            category = VehicleExpenseCategory.WASH,
            amount = 120_000L,
            date = "1405/04/11",
            description = "نظافت کامل داخل و خارج"
        ).getOrThrow()

        val insurance = repository.saveInsurance(
            vehicleId = car.id,
            company = "بیمه البرز",
            type = "شخص ثالث",
            startDate = "1404/05/01",
            endDate = "1405/05/01",
            amount = 4_500_000L,
            policyNumber = "ALB-998877"
        ).getOrThrow()

        val inspection = repository.saveInspection(
            vehicleId = car.id,
            lastInspectionDate = "1404/06/01",
            expiryDate = "1405/06/01",
            cost = 250_000L,
            status = "معتبر",
            centerName = "مرکز بیهقی"
        ).getOrThrow()

        // 3. Verify BEFORE deletion: DB records exist in Room
        assertNotNull(db.vehicleDao().getVehicleByServerId(userId, car.id))
        assertTrue(db.vehicleDao().getServicesForVehicle(userId, car.id).first().isNotEmpty())
        assertTrue(db.vehicleDao().getAllExpensesList(userId).any { it.vehicleId == car.id })
        assertTrue(db.vehicleDao().getAllInsurancesList(userId).any { it.vehicleId == car.id })
        assertTrue(db.vehicleDao().getAllInspectionsList(userId).any { it.vehicleId == car.id })

        assertNotNull(db.smartReminderDao().getReminderById(userId, service.id))
        assertNotNull(db.smartReminderDao().getReminderById(userId, insurance.id))
        assertNotNull(db.smartReminderDao().getReminderById(userId, inspection.id))

        // 4. Perform atomic cascading delete
        val deleteResult = repository.deleteVehicle(car.id)
        assertTrue(deleteResult.isSuccess)

        // 5. Verify AFTER deletion: DB records are completely wiped from Room
        assertNull(db.vehicleDao().getVehicleByServerId(userId, car.id))
        assertTrue(db.vehicleDao().getServicesForVehicle(userId, car.id).first().isEmpty())
        assertFalse(db.vehicleDao().getAllExpensesList(userId).any { it.vehicleId == car.id })
        assertFalse(db.vehicleDao().getAllInsurancesList(userId).any { it.vehicleId == car.id })
        assertFalse(db.vehicleDao().getAllInspectionsList(userId).any { it.vehicleId == car.id })

        assertNull(db.smartReminderDao().getReminderById(userId, service.id))
        assertNull(db.smartReminderDao().getReminderById(userId, insurance.id))
        assertNull(db.smartReminderDao().getReminderById(userId, inspection.id))

        assertTrue(db.smartReminderDao().getSchedulesForReminderSync(userId, service.id).isEmpty())
        assertTrue(db.smartReminderDao().getSchedulesForReminderSync(userId, insurance.id).isEmpty())
        assertTrue(db.smartReminderDao().getSchedulesForReminderSync(userId, inspection.id).isEmpty())
    }

    @Test
    @Config(sdk = [30])
    fun testReminderAndScheduleLifecycleOnServiceUpdateAndDelete() = kotlinx.coroutines.runBlocking {
        val db = com.example.data.database.AppDatabase.getDatabase(context)
        val userId = com.example.data.security.SessionManager.userId!!

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)

        // 1. Add vehicle
        val car = repository.addVehicle(
            brand = "ایران خودرو",
            model = "پژو پارس",
            year = "1401",
            color = "سفید",
            plate = "۵۵ ج ۳۲۱ ایران ۴۴",
            currentMileage = 40000
        ).getOrThrow()

        // 2. Add service with reminder enabled
        val serviceResult = repository.addService(
            vehicleId = car.id,
            title = "تعویض تسمه تایم",
            serviceType = ServiceType.TIMING_BELT_CHANGE,
            date = "1405/01/10",
            mileage = 40000,
            cost = 2_500_000L,
            description = "تسمه کنتیننتال اصلی",
            nextReminderDate = "1408/01/10",
            nextReminderMileage = 100000,
            isReminderEnabled = true
        )
        val service = serviceResult.getOrThrow()

        // 3. Verify reminder and schedule created in Room DB and scheduled in ShadowAlarmManager
        val reminderBefore = db.smartReminderDao().getReminderById(userId, service.id)
        assertNotNull("Reminder record must exist for service ID", reminderBefore)
        assertEquals(service.id, reminderBefore!!.id)
        assertEquals("ACTIVE", reminderBefore.status)

        val schedulesBefore = db.smartReminderDao().getSchedulesForReminderSync(userId, service.id)
        assertFalse("Schedule records must be generated for future date reminder", schedulesBefore.isEmpty())

        val scheduledAlarmsBefore = shadowAlarmManager.scheduledAlarms
        assertFalse("Alarm must be registered in AlarmManager for future reminder", scheduledAlarmsBefore.isEmpty())
        val initialScheduleId = schedulesBefore.first().id
        assertTrue("Registered alarm must correspond to initial schedule ID", scheduledAlarmsBefore.any { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.getStringExtra(com.example.reminder.receiver.ReminderBroadcastReceiver.EXTRA_SCHEDULE_ID) == initialScheduleId
        })

        // 4. Update service with new reminder date
        val updatedServiceResult = repository.updateService(
            serviceId = service.id,
            title = "تعویض تسمه تایم و بلبرینگ",
            serviceType = ServiceType.TIMING_BELT_CHANGE,
            date = "1405/01/10",
            mileage = 40000,
            cost = 3_000_000L,
            description = "تسمه کیت کامل",
            nextReminderDate = "1408/06/10", // Updated date
            nextReminderMileage = 100000,
            isReminderEnabled = true
        )
        assertTrue(updatedServiceResult.isSuccess)

        // 5. Verify old schedule replaced cleanly in Room without orphan duplicate schedules
        val schedulesAfterUpdate = db.smartReminderDao().getSchedulesForReminderSync(userId, service.id)
        assertFalse("Updated schedules must exist", schedulesAfterUpdate.isEmpty())
        val oldScheduleIds = schedulesBefore.map { it.id }.toSet()
        val newScheduleIds = schedulesAfterUpdate.map { it.id }.toSet()
        assertTrue("Old schedule IDs must be purged from Room and replaced by new schedule records", oldScheduleIds.intersect(newScheduleIds).isEmpty())

        // Verify ShadowAlarmManager has cancelled the old alarm ID and scheduled the new alarm ID
        val newScheduleId = schedulesAfterUpdate.first().id
        val scheduledAlarmsAfterUpdate = shadowAlarmManager.scheduledAlarms
        assertFalse("Old schedule ID must no longer be present in scheduled alarms", scheduledAlarmsAfterUpdate.any { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.getStringExtra(com.example.reminder.receiver.ReminderBroadcastReceiver.EXTRA_SCHEDULE_ID) == initialScheduleId
        })
        assertTrue("New schedule ID must be present in scheduled alarms", scheduledAlarmsAfterUpdate.any { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.getStringExtra(com.example.reminder.receiver.ReminderBroadcastReceiver.EXTRA_SCHEDULE_ID) == newScheduleId
        })

        // 6. Delete service and verify reminder + schedule cleanup in Room DB and ShadowAlarmManager
        val deleteServiceResult = repository.deleteService(service.id)
        assertTrue(deleteServiceResult.isSuccess)

        assertNull("Reminder must be deleted from Room", db.smartReminderDao().getReminderById(userId, service.id))
        assertTrue("Schedules must be deleted from Room", db.smartReminderDao().getSchedulesForReminderSync(userId, service.id).isEmpty())

        val scheduledAlarmsAfterDelete = shadowAlarmManager.scheduledAlarms
        assertFalse("No scheduled alarms must remain for service ID", scheduledAlarmsAfterDelete.any { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.getStringExtra(com.example.reminder.receiver.ReminderBroadcastReceiver.EXTRA_REMINDER_ID) == service.id
        })
    }

    @Test
    @Config(sdk = [30])
    fun testRebuildAllSchedulesBootRecovery() = kotlinx.coroutines.runBlocking {
        val db = com.example.data.database.AppDatabase.getDatabase(context)
        val userId = com.example.data.security.SessionManager.userId!!

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)

        val now = System.currentTimeMillis()
        val futureTrigger = now + 86400000L // 1 day in future
        val pastTrigger = now - 86400000L   // 1 day in past

        // 1. Insert Active Reminder #1 with 3 schedules:
        // - Active future schedule (Should be re-armed)
        // - Disabled future schedule (Should NOT be re-armed)
        // - Active past schedule (Should NOT be re-armed)
        val activeReminder = com.example.reminder.data.ReminderEntity(
            id = "rem_active_1",
            userId = userId,
            title = "یادآور فعال سرویس",
            description = "توضیحات سرویس",
            type = "VEHICLE",
            status = "ACTIVE",
            date = "1405/01/10",
            notificationEnabled = true
        )
        val activeFutureSchedule = com.example.reminder.data.ReminderScheduleEntity(
            id = "sched_active_future",
            reminderId = activeReminder.id,
            triggerType = "EXACT",
            triggerDateTime = futureTrigger,
            enabled = true
        )
        val disabledFutureSchedule = com.example.reminder.data.ReminderScheduleEntity(
            id = "sched_disabled_future",
            reminderId = activeReminder.id,
            triggerType = "EXACT",
            triggerDateTime = futureTrigger,
            enabled = false
        )
        val activePastSchedule = com.example.reminder.data.ReminderScheduleEntity(
            id = "sched_active_past",
            reminderId = activeReminder.id,
            triggerType = "EXACT",
            triggerDateTime = pastTrigger,
            enabled = true
        )

        db.smartReminderDao().insertReminder(activeReminder)
        db.smartReminderDao().insertSchedule(activeFutureSchedule)
        db.smartReminderDao().insertSchedule(disabledFutureSchedule)
        db.smartReminderDao().insertSchedule(activePastSchedule)

        // 2. Insert Inactive Reminder #2 (status = "DISABLED") with an active future schedule (Should NOT be re-armed)
        val disabledReminder = com.example.reminder.data.ReminderEntity(
            id = "rem_disabled_2",
            userId = userId,
            title = "یادآور غیرفعال سرویس",
            description = "توضیحات غیرفعال",
            type = "VEHICLE",
            status = "DISABLED",
            date = "1405/01/10",
            notificationEnabled = true
        )
        val disabledReminderSchedule = com.example.reminder.data.ReminderScheduleEntity(
            id = "sched_inactive_rem_future",
            reminderId = disabledReminder.id,
            triggerType = "EXACT",
            triggerDateTime = futureTrigger,
            enabled = true
        )

        db.smartReminderDao().insertReminder(disabledReminder)
        db.smartReminderDao().insertSchedule(disabledReminderSchedule)

        // Verify ShadowAlarmManager is empty initially
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)

        // 3. Execute rebuildAllSchedules
        val scheduler = com.example.reminder.domain.ReminderScheduler(context)
        scheduler.rebuildAllSchedules()

        // 4. Directly assert ShadowAlarmManager state
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Exactly one active future schedule must be re-armed in AlarmManager", 1, scheduledAlarms.size)

        val scheduledAlarm = scheduledAlarms.first()
        assertEquals(futureTrigger, scheduledAlarm.triggerAtTime)

        val shadowPendingIntent = Shadows.shadowOf(scheduledAlarm.operation)
        val intent = shadowPendingIntent.savedIntent
        assertNotNull(intent)
        assertEquals(activeReminder.id, intent.getStringExtra(com.example.reminder.receiver.ReminderBroadcastReceiver.EXTRA_REMINDER_ID))
        assertEquals(activeFutureSchedule.id, intent.getStringExtra(com.example.reminder.receiver.ReminderBroadcastReceiver.EXTRA_SCHEDULE_ID))
    }

    @Test
    @Config(sdk = [36])
    fun testServiceCreationWhenExactAlarmPermissionDenied_preservesServiceInRoomAndMarksReminderDisabled() = kotlinx.coroutines.runBlocking {
        val db = com.example.data.database.AppDatabase.getDatabase(context)
        val userId = com.example.data.security.SessionManager.userId!!

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)

        val car = repository.addVehicle(
            brand = "سایپا",
            model = "شاهین",
            year = "1402",
            color = "مشکی",
            plate = "۱۲ الف ۳۴۵ ایران ۲۲",
            currentMileage = 15000
        ).getOrThrow()

        // Attempt adding service with reminder enabled when exact alarm permission is denied
        val serviceResult = repository.addService(
            vehicleId = car.id,
            title = "تعویض شمع",
            serviceType = ServiceType.CUSTOM,
            date = "1405/02/10",
            mileage = 15000,
            cost = 800_000L,
            description = "شمع ایریدیوم",
            nextReminderDate = "1407/02/10",
            nextReminderMileage = 40000,
            isReminderEnabled = true
        )

        // 1. Verify service creation succeeded despite alarm permission error
        assertTrue("Service creation must succeed even if exact alarm permission is missing", serviceResult.isSuccess)
        val service = serviceResult.getOrThrow()

        // 2. Verify service record saved in Room DB
        val dbServices = db.vehicleDao().getServicesForVehicle(userId, car.id).first()
        assertTrue("Service record must be preserved in Room database", dbServices.any { it.serverId == service.id })

        // 3. Verify reminder record status in Room is rolled back to DISABLED
        val reminder = db.smartReminderDao().getReminderById(userId, service.id)
        assertNotNull("Reminder data record must be preserved in Room", reminder)
        assertEquals("DISABLED", reminder!!.status)

        // 4. Verify no alarm was registered in ShadowAlarmManager
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertFalse("No alarm should remain registered when permission was denied", scheduledAlarms.any { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.getStringExtra(com.example.reminder.receiver.ReminderBroadcastReceiver.EXTRA_REMINDER_ID) == service.id
        })
    }

    @Test
    fun testVehicleExpenseReportsStatisticsAndHealthScoreConsistency() {
        val currentPdt = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val currentYear = currentPdt.year
        val currentMonth = currentPdt.month
        val currentYearMonthStr = "%04d/%02d".format(currentYear, currentMonth)
        val currentYearStr = "%04d".format(currentYear)
        val pastYearStr = "%04d".format(currentYear - 1)

        val currentYearPersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian(currentYearStr)
        val currentMonthPersian = com.example.util.IranianPhoneUtils.convertDigitsToPersian("%02d".format(currentMonth))

        val car1Id = "car_stats_1"
        val car2Id = "car_stats_2"

        val expenses = listOf(
            // Car 1 - Current month expenses with different digit / delimiter formats
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "exp1",
                vehicleId = car1Id,
                title = "بنزین سوپر",
                category = VehicleExpenseCategory.FUEL,
                amount = 400_000L,
                date = "$currentYearPersian/$currentMonthPersian/05" // Persian digits
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "exp2",
                vehicleId = car1Id,
                title = "تعویض روغن",
                category = VehicleExpenseCategory.SERVICE,
                amount = 1_200_000L,
                date = "$currentYearStr-$currentMonth/10" // Hyphen delimiter
            ),
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "exp3",
                vehicleId = car1Id,
                title = "کارواش",
                category = VehicleExpenseCategory.WASH,
                amount = 150_000L,
                date = " $currentYearStr/$currentMonth/15 " // Trailing spaces
            ),
            // Car 1 - Past year expense
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "exp4",
                vehicleId = car1Id,
                title = "بیمه سال قبل",
                category = VehicleExpenseCategory.INSURANCE,
                amount = 4_000_000L,
                date = "$pastYearStr/05/20"
            ),
            // Car 1 - Invalid date format
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "exp5",
                vehicleId = car1Id,
                title = "هزینه با تاریخ نامعتبر",
                category = VehicleExpenseCategory.OTHER,
                amount = 500_000L,
                date = "invalid-date"
            ),
            // Car 2 - Expense (must NOT leak into Car 1 stats)
            com.example.vehicle.data.VehicleExpenseEntity(
                id = "exp6",
                vehicleId = car2Id,
                title = "سوخت خودرو دوم",
                category = VehicleExpenseCategory.FUEL,
                amount = 800_000L,
                date = "$currentYearStr/$currentMonth/01"
            )
        )

        // 1. Verify Car 1 Statistics
        val statsCar1 = VehicleAnalyzer.computeStatistics(
            vehicleId = car1Id,
            expenses = expenses,
            services = emptyList()
        )

        // Current month cost for Car 1 = 400k (exp1) + 1.2M (exp2) + 150k (exp3) = 1,750,000L
        assertEquals("Current month cost must accurately match all formatted current month expenses for Car 1", 1_750_000L, statsCar1.currentMonthCost)

        // Yearly cost for Car 1 = 1,750,000L (current year expenses)
        assertEquals("Yearly cost must include current year expenses for Car 1 and exclude past year", 1_750_000L, statsCar1.yearlyCost)

        // Grand total of all expenses for Car 1 = 400k + 1.2M + 150k + 4M + 500k = 6,250,000L
        val totalCategorySumCar1 = statsCar1.categoryBreakdown.sumOf { it.amount }
        assertEquals("Sum of categories must match total expenses of Car 1", 6_250_000L, totalCategorySumCar1)

        // Verify Car 2 Stats isolation
        val statsCar2 = VehicleAnalyzer.computeStatistics(
            vehicleId = car2Id,
            expenses = expenses,
            services = emptyList()
        )
        assertEquals("Car 2 expenses must be isolated from Car 1", 800_000L, statsCar2.currentMonthCost)
        assertEquals("Car 2 yearly cost must be isolated", 800_000L, statsCar2.yearlyCost)

        // 2. Empty Expenses List Handling
        val statsEmpty = VehicleAnalyzer.computeStatistics(
            vehicleId = car1Id,
            expenses = emptyList(),
            services = emptyList()
        )
        assertEquals(0L, statsEmpty.currentMonthCost)
        assertEquals(0L, statsEmpty.yearlyCost)
        assertEquals(0, statsEmpty.totalServicesCount)
        assertTrue(statsEmpty.categoryBreakdown.isEmpty())
    }
}




