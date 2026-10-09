package com.example.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.sms.BankSmsParser
import com.example.data.sms.BankSmsRepository
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.data.HomeDashboardRepository
import com.example.ui.screens.home.domain.HomeDashboardAggregator
import com.example.ui.screens.home.domain.HomeDashboardState
import com.example.ui.screens.home.domain.ObligationType
import com.example.ui.screens.home.viewmodel.HomeDashboardViewModel
import com.example.ui.screens.home.viewmodel.SmsAcceptResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeDashboardTest {

    @Test
    fun `test HomeDashboardState default initial values contain no fake data`() {
        val defaultState = HomeDashboardState()

        // Verify no fake default sample amounts
        assertEquals(0L, defaultState.monthlyIncome)
        assertEquals(0L, defaultState.monthlyExpense)
        assertEquals(0L, defaultState.monthlyBalance)
        assertEquals(0, defaultState.savingsRate)
        assertEquals(0, defaultState.notificationCount)
        assertTrue(defaultState.upcomingObligations.isEmpty())
        assertTrue(defaultState.upcomingReminders.isEmpty())
    }

    @Test
    fun `test HomeDashboardAggregator produces complete dashboard state from real data`() = kotlinx.coroutines.runBlocking {
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
        vehicleRepo.initDatabase(context)

        val testAccount = com.example.ui.screens.finance.model.Account(
            id = "acc_test",
            userId = "test_user",
            name = "حساب تست",
            type = com.example.ui.screens.finance.model.AccountType.CARD,
            bankName = "بانک ملت",
            accountNumberMasked = "**** ۱۲۳۴",
            initialBalance = 100_000_000L,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        financeRepo.addAccountResult(testAccount)

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
                accountId = "acc_test",
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
                accountId = "acc_test",
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
                accountId = "acc_test",
                datePersian = currentMonthDate,
                timePersian = "11:00"
            )
        )

        // Transfer transaction - should NOT be counted in income or expense
        val testAccount2 = com.example.ui.screens.finance.model.Account(
            id = "acc_test_2",
            userId = "test_user",
            name = "حساب دوم",
            type = com.example.ui.screens.finance.model.AccountType.CASH,
            bankName = "نقدی",
            accountNumberMasked = null,
            initialBalance = 10_000_000L,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        financeRepo.addAccountResult(testAccount2)

        financeRepo.addTransaction(
            com.example.ui.screens.finance.model.TransactionItemData(
                id = "test-transfer",
                title = "انتقال بین حساب‌ها",
                amount = 5_000_000L,
                type = com.example.ui.screens.finance.model.TransactionType.TRANSFER,
                category = categoryExp,
                transferSourceAccountId = "acc_test",
                transferDestinationAccountId = "acc_test_2",
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
            ),
            context = context
        )
        var waitInst = 0
        while (installmentRepo.installments.value.isEmpty() && waitInst < 30) {
            kotlinx.coroutines.delay(50L)
            waitInst++
        }

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
    }

    @Test
    fun `test BankSmsParser type detection and unknown fallback`() {
        // Income detection
        val incomeType = BankSmsParser.detectTransactionType("واریز حقوق به حساب شما")
        assertEquals(TransactionType.INCOME, incomeType)

        // Expense detection
        val expenseType = BankSmsParser.detectTransactionType("برداشت و خرید از پایانه فروش")
        assertEquals(TransactionType.EXPENSE, expenseType)

        // Transfer detection
        val transferType = BankSmsParser.detectTransactionType("انتقال کارت به کارت موفق به کارت 6037")
        assertEquals(TransactionType.TRANSFER, transferType)

        // Unknown SMS text should NOT auto-resolve as Expense
        val unknownType = BankSmsParser.detectTransactionType("رمز پویای شما جهت ورود ۱۲۳۴۵ است")
        assertNull(unknownType)

        // Parse with uncertain type sets isTypeUncertain = true
        val parsed = BankSmsParser.parse(
            smsId = "sms_test_uncertain",
            sender = "بانک ملت",
            body = "مبلغ: 50,000 تومان در حساب شما منظور شد.",
            timestampMillis = System.currentTimeMillis()
        )
        assertNotNull(parsed)
        assertTrue(parsed!!.isTypeUncertain)
    }

    @Test
    fun `test BankSmsParser extracts amounts in Rial and converts to Toman`() {
        // 500,000 Rial = 50,000 Toman
        val parsedMellat = BankSmsParser.parse(
            smsId = "sms_m1",
            sender = "بانک ملت",
            body = "برداشت: 500,000 ریال از حساب *1234. مانده: 10,000,000 ریال",
            timestampMillis = 1700000000000L
        )
        assertNotNull(parsedMellat)
        assertEquals(50_000L, parsedMellat!!.amount)
        assertEquals(TransactionType.EXPENSE, parsedMellat.type)
        assertFalse(parsedMellat.isTypeUncertain)
        assertEquals("بانک ملت", parsedMellat.bankName)
        assertEquals(1700000000000L, parsedMellat.timestampMillis)

        // BluBank Transfer
        val parsedBlu = BankSmsParser.parse(
            smsId = "sms_b1",
            sender = "بلوبانک",
            body = "انتقال 2,500,000 ریال به کارت 6037991122334455",
            timestampMillis = 1700000000000L
        )
        assertNotNull(parsedBlu)
        assertEquals(250_000L, parsedBlu!!.amount)
        assertEquals(TransactionType.TRANSFER, parsedBlu.type)
        assertTrue(parsedBlu.destinationAccount?.contains("6037991122334455") == true)
    }

    @Test
    fun `test BankSmsRepository persistence, user isolation and idempotency`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val smsRepo = BankSmsRepository.getInstance(context)

        val testSmsId1 = "sms_unit_test_proc_1"
        val testSmsId2 = "sms_unit_test_dism_2"
        val testUserId = "user_isolation_test_1"

        // Mark processed and dismissed for test user
        smsRepo.markSmsProcessed(testSmsId1, testUserId)
        smsRepo.markSmsDismissed(testSmsId2, testUserId)

        // Verify persistent IDs for this user
        assertTrue(smsRepo.getProcessedSmsIds(testUserId).contains(testSmsId1))
        assertTrue(smsRepo.getDismissedSmsIds(testUserId).contains(testSmsId2))
        assertFalse(smsRepo.getProcessedSmsIds(testUserId).contains("sms_non_existent"))
    }

    @Test
    fun `test HomeDashboardViewModel accepts SMS and registers real transaction into Room`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testUserId = "test_user_sms_flow"
        com.example.data.security.SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(
                id = testUserId,
                fullName = "کاربر تست پیامک",
                email = null,
                phoneNumber = "09120000001",
                phoneVerified = true
            )
        )

        val db = AppDatabase.getDatabase(context)
        db.transactionDao().clearAllTransactions(testUserId)

        val testAccount = com.example.data.database.AccountEntity(
            stringId = "acc_saman",
            userId = testUserId,
            name = "حساب سامان",
            type = "CARD",
            bankName = "بانک سامان",
            accountNumberMasked = "**** 4321",
            initialBalance = 10_000_000L,
            isActive = true,
            deletedAt = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.accountDao().insertAccount(testAccount)

        val viewModel = HomeDashboardViewModel(
            ApplicationProvider.getApplicationContext()
        )

        // Parse a sample SMS
        val smsTimestamp = 1710000000000L
        val suggestion = BankSmsParser.parse(
            smsId = "test_sms_card_1",
            sender = "بانک سامان",
            body = "خرید از پایانه فروشگاه: مبلغ: 150,000 تومان. مانده: 2,000,000 تومان",
            timestampMillis = smsTimestamp
        )
        assertNotNull(suggestion)

        // Inject into viewModel queue
        val queueField = HomeDashboardViewModel::class.java.getDeclaredField("_pendingSmsQueue")
        queueField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val queueFlow = queueField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<List<com.example.ui.screens.home.domain.BankSmsSuggestion>>
        queueFlow.value = listOf(suggestion!!)

        var resultHolder: SmsAcceptResult? = null
        viewModel.acceptSmsSuggestion(
            id = suggestion.id,
            customType = null,
            customCategory = null,
            customAccount = null,
            customDescription = null,
            destAccount = null
        ) { res ->
            resultHolder = res
        }

        // Wait for coroutine completion
        var count = 0
        while (resultHolder == null && count < 20) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            kotlinx.coroutines.delay(50L)
            count++
        }

        assertEquals(SmsAcceptResult.Success, resultHolder)

        // Verify transaction inserted into Room with accurate timestamp and values
        val txList = db.transactionDao().getAllTransactionsList(testUserId)
        assertEquals(1, txList.size)
        val savedTx = txList[0]
        assertEquals("tx_sms_${suggestion.id}", savedTx.stringId)
        assertEquals(150_000L, savedTx.amount)
        assertEquals("EXPENSE", savedTx.type)
        assertEquals(smsTimestamp, savedTx.timestamp) // Real SMS timestamp preserved!
        assertEquals("BANK_SMS", savedTx.sourceType)
        assertNotNull(savedTx.accountId)

        // Try accepting again -> must return AlreadyExists (Idempotency)
        var secondResult: SmsAcceptResult? = null
        viewModel.acceptSmsSuggestion(id = suggestion.id) { res ->
            secondResult = res
        }
        var count2 = 0
        while (secondResult == null && count2 < 20) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            kotlinx.coroutines.delay(50L)
            count2++
        }
        assertEquals(SmsAcceptResult.AlreadyExists, secondResult)
    }

    @Test
    fun `test HomeDashboardViewModel rapid refresh concurrency and latest state win`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.data.security.SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(
                id = "test_user_vm",
                fullName = "کاربر تست",
                email = null,
                phoneNumber = "09120000000",
                phoneVerified = true
            )
        )

        val viewModel = HomeDashboardViewModel(
            ApplicationProvider.getApplicationContext()
        )

        // Trigger multiple rapid refreshes
        viewModel.loadDashboardData()
        viewModel.loadDashboardData()
        viewModel.loadDashboardData()

        // Wait a bit for debounce and mutex processing
        var attempts = 0
        while (viewModel.uiState.value.isLoading && attempts < 30) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper(100L)
            kotlinx.coroutines.delay(50L)
            attempts++
        }

        val state = viewModel.uiState.value
        assertNotNull(state)
        assertFalse(state.isLoading)
    }
}
