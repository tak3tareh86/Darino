package com.example.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.home.data.HomeDashboardRepository
import com.example.ui.screens.home.domain.HomeDashboardAggregator
import com.example.ui.screens.home.domain.HomeInsightEngine
import com.example.ui.screens.home.domain.ObligationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeDashboardTest {

    @Test
    fun `test HomeDashboardAggregator produces complete dashboard state`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.data.security.SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(
                id = "test_user",
                fullName = "کاربر تست",
                email = null,
                phoneNumber = "09120000000",
                phoneVerified = true
            )
        )

        // Initialize repositories
        val financeRepo = com.example.ui.screens.finance.data.LocalFinanceRepository.instance
        financeRepo.init(context)
        val installmentRepo = com.example.ui.screens.installments.data.LocalInstallmentRepository.instance
        installmentRepo.init(context)
        val vehicleRepo = com.example.vehicle.data.VehicleRepository.instance

        val categoryInc = com.example.ui.screens.finance.model.FinanceDefaultCategories.defaultIncomeCategories.first()
        val categoryExp = com.example.ui.screens.finance.model.FinanceDefaultCategories.defaultExpenseCategories.first()
        val currentPdt = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val currentMonthDate = com.example.util.IranianDateUtils.createFormattedDate(currentPdt.year, currentPdt.month, 1)
        val prevMonth = if (currentPdt.month > 1) currentPdt.month - 1 else 12
        val prevYear = if (currentPdt.month > 1) currentPdt.year else currentPdt.year - 1
        val prevMonthDate = com.example.util.IranianDateUtils.createFormattedDate(prevYear, prevMonth, 1)

        // Previous month transaction - should NOT be included in monthly financials
        financeRepo.addTransaction(
            com.example.ui.screens.finance.model.TransactionItemData(
                id = "test-old",
                title = "حقوق ماه قبل",
                amount = 25_000_000L,
                type = com.example.ui.screens.finance.model.TransactionType.INCOME,
                category = categoryInc,
                datePersian = prevMonthDate,
                timePersian = "09:00"
            )
        )

        // Current month income
        financeRepo.addTransaction(
            com.example.ui.screens.finance.model.TransactionItemData(
                id = "test-inc",
                title = "حقوق",
                amount = 18_000_000L,
                type = com.example.ui.screens.finance.model.TransactionType.INCOME,
                category = categoryInc,
                datePersian = currentMonthDate,
                timePersian = "10:00"
            )
        )

        // Current month expense
        financeRepo.addTransaction(
            com.example.ui.screens.finance.model.TransactionItemData(
                id = "test-exp",
                title = "هزینه جاری",
                amount = 9_500_000L,
                type = com.example.ui.screens.finance.model.TransactionType.EXPENSE,
                category = categoryExp,
                datePersian = currentMonthDate,
                timePersian = "11:00"
            )
        )

        // Transfer transaction - should NOT be counted in income or expense
        financeRepo.addTransaction(
            com.example.ui.screens.finance.model.TransactionItemData(
                id = "test-transfer",
                title = "انتقال بین حساب‌ها",
                amount = 5_000_000L,
                type = com.example.ui.screens.finance.model.TransactionType.TRANSFER,
                category = categoryExp,
                datePersian = currentMonthDate,
                timePersian = "12:00"
            )
        )

        // Add test installment obligation
        installmentRepo.addInstallment(
            com.example.ui.screens.installments.model.InstallmentItem(
                id = "loan_test",
                title = "وام مسکن بانک ملت",
                category = com.example.ui.screens.installments.model.InstallmentCategory.HOME_LOANS,
                providerOrPerson = "بانک مسکن",
                totalAmount = 240_000_000L,
                paidAmount = 100_000_000L,
                remainingAmount = 140_000_000L,
                totalInstallments = 24,
                remainingInstallments = 14,
                nextPaymentDate = currentMonthDate,
                nextDueDaysText = "امروز",
                startDate = "1402/01/01",
                endDate = "1404/01/01",
                status = com.example.ui.screens.installments.model.InstallmentStatus.PENDING,
                monthlyPaymentFormatted = "۱۰,۰۰۰,۰۰۰ تومان"
            )
        )

        // Add test reminder
        val db = com.example.data.database.AppDatabase.getDatabase(context)
        db.smartReminderDao().insertReminder(
            com.example.reminder.data.ReminderEntity(
                id = "rem_test",
                userId = "test_user",
                title = "تماس با تعمیرگاه و سرویس دوره‌ای",
                date = currentMonthDate,
                time = "10:00",
                type = "SERVICE",
                status = "ACTIVE"
            )
        )

        // Add test vehicle if none exists
        if (vehicleRepo.vehicles.value.isEmpty()) {
            vehicleRepo.addVehicle(
                brand = "ایران‌خودرو",
                model = "پژو ۲۰۶",
                year = "1400",
                color = "سفید",
                plate = "۱۲ب۳۴۵-۶۷",
                vin = "VIN-PEUG-206-TEST",
                currentMileage = 40000,
                estimatedValue = 300_000_000L
            )
        }

        val repository = HomeDashboardRepository(
            context = context,
            vehicleRepository = vehicleRepo,
            financeRepository = financeRepo,
            installmentRepository = installmentRepo
        )
        val aggregator = HomeDashboardAggregator(repository)

        val state = aggregator.aggregate()

        // 1. Verify Monthly Financials & Savings Rate
        assertEquals(18_000_000L, state.monthlyIncome)
        assertEquals(9_500_000L, state.monthlyExpense)
        assertEquals(8_500_000L, state.monthlyBalance)
        assertEquals(47, state.savingsRate)

        // 2. Verify Upcoming Obligations (max 3)
        assertTrue(state.upcomingObligations.size in 1..3)
        val loanObligation = state.upcomingObligations.find { it.type == ObligationType.INSTALLMENT }
        assertNotNull(loanObligation)
        assertEquals("وام مسکن بانک ملت", loanObligation?.title)

        // 3. Verify Upcoming Reminders (max 3)
        assertTrue(state.upcomingReminders.size in 1..3)
        assertEquals("تماس با تعمیرگاه و سرویس دوره‌ای", state.upcomingReminders[0].title)

        // 4. Verify Vehicle Summary
        assertNotNull(state.vehicleSummary)
        assertTrue(state.vehicleSummary?.name?.contains("پژو") == true)

        // 5. Verify Smart Insight Generated
        assertNotNull(state.financialInsight)
        assertEquals("هشدار سررسید دارینو", state.financialInsight?.title)
    }

    @Test
    fun `test HomeInsightEngine rule engine logic`() {
        // High savings rate test
        val highSavingsInsight = HomeInsightEngine.generateInsight(
            monthlyIncome = 20_000_000L,
            monthlyExpense = 8_000_000L,
            savingsRate = 60,
            activeInstallmentsCount = 1,
            totalInstallmentsAmount = 2_000_000L,
            hasOverdueInstallments = false,
            hasVehicleNeedsService = false,
            upcomingObligationsCount = 1
        )
        assertTrue(highSavingsInsight.message.contains("نرخ پس‌انداز"))

        // Overdue warning test
        val overdueInsight = HomeInsightEngine.generateInsight(
            monthlyIncome = 20_000_000L,
            monthlyExpense = 8_000_000L,
            savingsRate = 60,
            activeInstallmentsCount = 1,
            totalInstallmentsAmount = 2_000_000L,
            hasOverdueInstallments = true,
            hasVehicleNeedsService = false,
            upcomingObligationsCount = 1
        )
        assertTrue(overdueInsight.message.contains("عقب‌افتاده"))

        // Vehicle service test
        val vehicleInsight = HomeInsightEngine.generateInsight(
            monthlyIncome = 20_000_000L,
            monthlyExpense = 15_000_000L,
            savingsRate = 25,
            activeInstallmentsCount = 1,
            totalInstallmentsAmount = 2_000_000L,
            hasOverdueInstallments = false,
            hasVehicleNeedsService = true,
            upcomingObligationsCount = 1
        )
        assertTrue(vehicleInsight.message.contains("سرویس"))
    }
}
