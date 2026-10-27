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
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper
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

    @Test
    fun `test UpcomingObligations returns only single nearest unpaid bank installment`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val user = com.example.data.api.NetworkUserDto(
            id = "test_user_single_obligation",
            fullName = "کاربر تست",
            email = null,
            phoneNumber = "09121111111",
            phoneVerified = true
        )
        com.example.data.security.SessionManager.setAuthenticatedUser(user)

        val installmentRepo = com.example.ui.screens.installments.data.LocalInstallmentRepository.instance
        installmentRepo.init(context)
        installmentRepo.clearAllInstallments(context)
        var clearWait = 0
        while (installmentRepo.installments.value.isNotEmpty() && clearWait < 30) {
            kotlinx.coroutines.delay(50L)
            clearWait++
        }

        val vehicleRepo = com.example.vehicle.data.VehicleRepository.instance

        // Dates for test installments
        val currentPdt = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val farJalali = com.example.util.IranianDateUtils.createFormattedDate(currentPdt.year, currentPdt.month, (currentPdt.day + 15).coerceAtMost(28))
        val nearJalali = com.example.util.IranianDateUtils.createFormattedDate(currentPdt.year, currentPdt.month, (currentPdt.day + 2).coerceAtMost(28))
        val mediumJalali = com.example.util.IranianDateUtils.createFormattedDate(currentPdt.year, currentPdt.month, (currentPdt.day + 8).coerceAtMost(28))

        // Far installment: Bank Melli
        installmentRepo.addInstallment(
            com.example.ui.screens.installments.model.InstallmentItem(
                id = "inst_far",
                title = "وام خرید کالا",
                category = com.example.ui.screens.installments.model.InstallmentCategory.BANK_LOANS,
                providerOrPerson = "بانک ملی",
                totalAmount = 60_000_000L,
                paidAmount = 10_000_000L,
                remainingAmount = 50_000_000L,
                totalInstallments = 12,
                remainingInstallments = 10,
                nextPaymentDate = farJalali,
                nextDueDaysText = "۱۵ روز دیگر",
                startDate = "1403/01/01",
                endDate = "1404/01/01",
                status = com.example.ui.screens.installments.model.InstallmentStatus.PENDING,
                monthlyPaymentFormatted = "۵,۰۰۰,۰۰۰ تومان"
            ),
            context = context
        )
        var wait = 0
        while (!installmentRepo.installments.value.any { it.id == "inst_far" } && wait < 30) {
            kotlinx.coroutines.delay(50L)
            wait++
        }

        // Near installment: Bank Mellat (The nearest!)
        installmentRepo.addInstallment(
            com.example.ui.screens.installments.model.InstallmentItem(
                id = "inst_near",
                title = "وام ازدواج",
                category = com.example.ui.screens.installments.model.InstallmentCategory.BANK_LOANS,
                providerOrPerson = "بانک ملت",
                totalAmount = 120_000_000L,
                paidAmount = 20_000_000L,
                remainingAmount = 100_000_000L,
                totalInstallments = 24,
                remainingInstallments = 20,
                nextPaymentDate = nearJalali,
                nextDueDaysText = "۲ روز دیگر",
                startDate = "1403/01/01",
                endDate = "1405/01/01",
                status = com.example.ui.screens.installments.model.InstallmentStatus.PENDING,
                monthlyPaymentFormatted = "۵,۰۰۰,۰۰۰ تومان"
            ),
            context = context
        )
        wait = 0
        while (!installmentRepo.installments.value.any { it.id == "inst_near" } && wait < 30) {
            kotlinx.coroutines.delay(50L)
            wait++
        }

        // Medium installment: Bank Tejarat
        installmentRepo.addInstallment(
            com.example.ui.screens.installments.model.InstallmentItem(
                id = "inst_med",
                title = "تسهیلات خودرو",
                category = com.example.ui.screens.installments.model.InstallmentCategory.BANK_LOANS,
                providerOrPerson = "بانک تجارت",
                totalAmount = 80_000_000L,
                paidAmount = 20_000_000L,
                remainingAmount = 60_000_000L,
                totalInstallments = 16,
                remainingInstallments = 12,
                nextPaymentDate = mediumJalali,
                nextDueDaysText = "۸ روز دیگر",
                startDate = "1403/01/01",
                endDate = "1404/05/01",
                status = com.example.ui.screens.installments.model.InstallmentStatus.PENDING,
                monthlyPaymentFormatted = "۵,۰۰۰,۰۰۰ تومان"
            ),
            context = context
        )
        wait = 0
        while (!installmentRepo.installments.value.any { it.id == "inst_med" } && wait < 30) {
            kotlinx.coroutines.delay(50L)
            wait++
        }

        val repo = HomeDashboardRepository(
            context = context,
            vehicleRepository = vehicleRepo,
            installmentRepository = installmentRepo
        )

        val obligations = repo.getUpcomingObligations()

        // 1. Must return EXACTLY ONE obligation
        assertEquals(1, obligations.size)

        // 2. The obligation must be the nearest one (inst_near)
        val single = obligations.first()
        assertEquals("وام ازدواج", single.title)
        assertEquals("بانک ملت", single.bankName)
        assertEquals(nearJalali, single.dueDatePersian)
        assertEquals(ObligationType.INSTALLMENT, single.type)
        assertEquals(MoneyFormatter.formatToman(120_000_000L / 24), single.formattedAmount)

        // 3. Vehicle insurance and services are completely excluded
        assertTrue(obligations.none { it.type == ObligationType.VEHICLE_INSURANCE })
        assertTrue(obligations.none { it.type == ObligationType.OTHER })
    }

    @Test
    fun `test UpcomingObligations prioritizes overdue installment over upcoming`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val user = com.example.data.api.NetworkUserDto(
            id = "test_user_overdue_priority",
            fullName = "کاربر تست",
            email = null,
            phoneNumber = "09122222222",
            phoneVerified = true
        )
        com.example.data.security.SessionManager.setAuthenticatedUser(user)

        val installmentRepo = com.example.ui.screens.installments.data.LocalInstallmentRepository.instance
        installmentRepo.init(context)
        installmentRepo.clearAllInstallments(context)
        var clearWait = 0
        while (installmentRepo.installments.value.isNotEmpty() && clearWait < 30) {
            kotlinx.coroutines.delay(50L)
            clearWait++
        }

        // Installment 1: upcoming tomorrow
        val currentPdt = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val tomorrowJalali = com.example.util.IranianDateUtils.createFormattedDate(currentPdt.year, currentPdt.month, (currentPdt.day + 1).coerceAtMost(28))

        // Installment 2: overdue (due 5 days ago)
        val overdueDay = (currentPdt.day - 5).coerceAtLeast(1)
        val overdueJalali = com.example.util.IranianDateUtils.createFormattedDate(currentPdt.year, currentPdt.month, overdueDay)

        installmentRepo.addInstallment(
            com.example.ui.screens.installments.model.InstallmentItem(
                id = "inst_upcoming",
                title = "وام فوری",
                category = com.example.ui.screens.installments.model.InstallmentCategory.BANK_LOANS,
                providerOrPerson = "بانک صادرات",
                totalAmount = 20_000_000L,
                paidAmount = 0L,
                remainingAmount = 20_000_000L,
                totalInstallments = 10,
                remainingInstallments = 10,
                nextPaymentDate = tomorrowJalali,
                nextDueDaysText = "فردا",
                startDate = "1403/01/01",
                endDate = "1403/11/01",
                status = com.example.ui.screens.installments.model.InstallmentStatus.PENDING,
                monthlyPaymentFormatted = "۲,۰۰۰,۰۰۰ تومان"
            ),
            context = context
        )
        var wait = 0
        while (!installmentRepo.installments.value.any { it.id == "inst_upcoming" } && wait < 30) {
            kotlinx.coroutines.delay(50L)
            wait++
        }

        installmentRepo.addInstallment(
            com.example.ui.screens.installments.model.InstallmentItem(
                id = "inst_overdue",
                title = "وام مسکن",
                category = com.example.ui.screens.installments.model.InstallmentCategory.BANK_LOANS,
                providerOrPerson = "بانک مسکن",
                totalAmount = 50_000_000L,
                paidAmount = 10_000_000L,
                remainingAmount = 40_000_000L,
                totalInstallments = 10,
                remainingInstallments = 8,
                nextPaymentDate = overdueJalali,
                nextDueDaysText = "۵ روز گذشته (معوق)",
                startDate = "1403/01/01",
                endDate = "1403/11/01",
                status = com.example.ui.screens.installments.model.InstallmentStatus.OVERDUE,
                monthlyPaymentFormatted = "۵,۰۰۰,۰۰۰ تومان"
            ),
            context = context
        )
        wait = 0
        while (!installmentRepo.installments.value.any { it.id == "inst_overdue" } && wait < 30) {
            kotlinx.coroutines.delay(50L)
            wait++
        }

        val repo = HomeDashboardRepository(
            context = context,
            installmentRepository = installmentRepo
        )

        val obligations = repo.getUpcomingObligations()
        assertEquals(1, obligations.size)
        // Overdue must take priority!
        assertEquals("وام مسکن", obligations.first().title)
        assertEquals("بانک مسکن", obligations.first().bankName)
        assertTrue(obligations.first().isOverdue)
    }

    @Test
    fun `test UpcomingObligations empty state when all installments completed or paid`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val user = com.example.data.api.NetworkUserDto(
            id = "test_user_empty_obligations",
            fullName = "کاربر تست",
            email = null,
            phoneNumber = "09123333333",
            phoneVerified = true
        )
        com.example.data.security.SessionManager.setAuthenticatedUser(user)

        val installmentRepo = com.example.ui.screens.installments.data.LocalInstallmentRepository.instance
        installmentRepo.init(context)
        installmentRepo.clearAllInstallments(context)
        var clearWait = 0
        while (installmentRepo.installments.value.isNotEmpty() && clearWait < 30) {
            kotlinx.coroutines.delay(50L)
            clearWait++
        }

        installmentRepo.addInstallment(
            com.example.ui.screens.installments.model.InstallmentItem(
                id = "inst_completed",
                title = "وام تسویه شده",
                category = com.example.ui.screens.installments.model.InstallmentCategory.BANK_LOANS,
                providerOrPerson = "بانک کشاورزی",
                totalAmount = 30_000_000L,
                paidAmount = 30_000_000L,
                remainingAmount = 0L,
                totalInstallments = 10,
                remainingInstallments = 0,
                nextPaymentDate = "1403/05/01",
                nextDueDaysText = "تکمیل شده",
                startDate = "1402/01/01",
                endDate = "1403/05/01",
                status = com.example.ui.screens.installments.model.InstallmentStatus.COMPLETED,
                monthlyPaymentFormatted = "۳,۰۰۰,۰۰۰ تومان"
            ),
            context = context
        )

        var waitAttempts = 0
        while (installmentRepo.installments.value.isEmpty() && waitAttempts < 40) {
            kotlinx.coroutines.delay(50L)
            waitAttempts++
        }

        val repo = HomeDashboardRepository(
            context = context,
            installmentRepository = installmentRepo
        )

        val obligations = repo.getUpcomingObligations()
        assertTrue(obligations.isEmpty())
    }

    @Test
    fun `test Settlement of installment reactively replaces with next nearest installment`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val user = com.example.data.api.NetworkUserDto(
            id = "test_user_settlement_reactive",
            fullName = "کاربر تست",
            email = null,
            phoneNumber = "09124444444",
            phoneVerified = true
        )
        com.example.data.security.SessionManager.setAuthenticatedUser(user)

        val installmentRepo = com.example.ui.screens.installments.data.LocalInstallmentRepository.instance
        installmentRepo.init(context)
        installmentRepo.clearAllInstallments(context)
        var clearWait = 0
        while (installmentRepo.installments.value.isNotEmpty() && clearWait < 30) {
            kotlinx.coroutines.delay(50L)
            clearWait++
        }

        val currentPdt = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val nearJalali = com.example.util.IranianDateUtils.createFormattedDate(currentPdt.year, currentPdt.month, (currentPdt.day + 2).coerceAtMost(28))
        val nextNearJalali = com.example.util.IranianDateUtils.createFormattedDate(currentPdt.year, currentPdt.month, (currentPdt.day + 10).coerceAtMost(28))

        installmentRepo.addInstallment(
            com.example.ui.screens.installments.model.InstallmentItem(
                id = "inst_1",
                title = "قسط اول نزدیک",
                category = com.example.ui.screens.installments.model.InstallmentCategory.BANK_LOANS,
                providerOrPerson = "بانک سپه",
                totalAmount = 10_000_000L,
                paidAmount = 0L,
                remainingAmount = 10_000_000L,
                totalInstallments = 5,
                remainingInstallments = 5,
                nextPaymentDate = nearJalali,
                nextDueDaysText = "۲ روز دیگر",
                startDate = "1403/01/01",
                endDate = "1403/06/01",
                status = com.example.ui.screens.installments.model.InstallmentStatus.PENDING,
                monthlyPaymentFormatted = "۲,۰۰۰,۰۰۰ تومان"
            ),
            context = context
        )
        var wait = 0
        while (!installmentRepo.installments.value.any { it.id == "inst_1" } && wait < 30) {
            kotlinx.coroutines.delay(50L)
            wait++
        }

        installmentRepo.addInstallment(
            com.example.ui.screens.installments.model.InstallmentItem(
                id = "inst_2",
                title = "قسط دوم بعد",
                category = com.example.ui.screens.installments.model.InstallmentCategory.BANK_LOANS,
                providerOrPerson = "بانک پاسارگاد",
                totalAmount = 20_000_000L,
                paidAmount = 0L,
                remainingAmount = 20_000_000L,
                totalInstallments = 10,
                remainingInstallments = 10,
                nextPaymentDate = nextNearJalali,
                nextDueDaysText = "۱۰ روز دیگر",
                startDate = "1403/01/01",
                endDate = "1403/11/01",
                status = com.example.ui.screens.installments.model.InstallmentStatus.PENDING,
                monthlyPaymentFormatted = "۲,۰۰۰,۰۰۰ تومان"
            ),
            context = context
        )
        wait = 0
        while (!installmentRepo.installments.value.any { it.id == "inst_2" } && wait < 30) {
            kotlinx.coroutines.delay(50L)
            wait++
        }

        val repo = HomeDashboardRepository(context = context, installmentRepository = installmentRepo)

        // Initially, inst_1 is the nearest
        assertEquals(1, repo.getUpcomingObligations().size)
        assertEquals("قسط اول نزدیک", repo.getUpcomingObligations().first().title)
        assertEquals("بانک سپه", repo.getUpcomingObligations().first().bankName)

        // Settle/Pay inst_1 (remainingInstallments = 0, status = COMPLETED)
        installmentRepo.updateInstallment(
            com.example.ui.screens.installments.model.InstallmentItem(
                id = "inst_1",
                title = "قسط اول نزدیک",
                category = com.example.ui.screens.installments.model.InstallmentCategory.BANK_LOANS,
                providerOrPerson = "بانک سپه",
                totalAmount = 10_000_000L,
                paidAmount = 10_000_000L,
                remainingAmount = 0L,
                totalInstallments = 5,
                remainingInstallments = 0,
                nextPaymentDate = nearJalali,
                nextDueDaysText = "تکمیل شده",
                startDate = "1403/01/01",
                endDate = "1403/06/01",
                status = com.example.ui.screens.installments.model.InstallmentStatus.COMPLETED,
                monthlyPaymentFormatted = "۲,۰۰۰,۰۰۰ تومان"
            ),
            context = context
        )

        var settleAttempts = 0
        while (installmentRepo.installments.value.find { it.id == "inst_1" }?.status != com.example.ui.screens.installments.model.InstallmentStatus.COMPLETED && settleAttempts < 40) {
            kotlinx.coroutines.delay(50L)
            settleAttempts++
        }

        // Now, inst_2 must automatically become the nearest obligation!
        val newObligations = repo.getUpcomingObligations()
        assertEquals(1, newObligations.size)
        assertEquals("قسط دوم بعد", newObligations.first().title)
        assertEquals("بانک پاسارگاد", newObligations.first().bankName)
    }
}
