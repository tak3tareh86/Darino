package com.example.finance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.NetworkUserDto
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.model.PaymentMethod
import com.example.ui.screens.finance.model.RecurringFrequency
import com.example.ui.screens.finance.model.RecurringTransaction
import com.example.ui.screens.finance.model.TransactionType
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecurringTransactionWorkerTest {

    private lateinit var context: Context
    private lateinit var repository: LocalFinanceRepository
    private lateinit var db: AppDatabase
    private val testUserId = "test_user_recurring_123"

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        SessionManager.init(context)

        val testUser = NetworkUserDto(
            id = testUserId,
            fullName = "recurring_tester",
            email = "recurring@example.com",
            phoneNumber = "09123456789",
            phoneVerified = true
        )
        SessionManager.setAuthenticatedUser(testUser)

        repository = LocalFinanceRepository.instance
        repository.init(context)

        // Clear SharedPreferences and in-memory list for test isolation
        val prefs = context.getSharedPreferences("finance_prefs_${testUserId}", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val field = LocalFinanceRepository::class.java.getDeclaredField("_recurringTransactions")
        field.isAccessible = true
        (field.get(repository) as MutableStateFlow<List<RecurringTransaction>>).value = emptyList()

        db = AppDatabase.getDatabase(context)
        db.transactionDao().clearAllTransactions(testUserId)
        repository.ensureDefaultAccounts(db, testUserId)
    }

    @Test
    fun testProcessDueRecurringTransaction_createsExpenseAndPreventsDuplicates() = runBlocking {
        val todayJalali = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()

        val recurringExpense = RecurringTransaction(
            id = "rec_exp_${UUID.randomUUID()}",
            title = "اجاره خانه",
            amount = 15000000L,
            type = TransactionType.EXPENSE,
            categoryId = "home",
            categoryTitle = "خانه و اجاره",
            frequency = RecurringFrequency.MONTHLY,
            startDate = todayJalali,
            nextExecutionDate = todayJalali,
            enabled = true,
            paymentMethod = PaymentMethod.BANK_CARD,
            accountName = "کارت بانکی"
        )

        repository.addRecurringTransaction(recurringExpense)

        // Run due processing first time
        repository.processDueRecurringTransactions(context)

        // Verify transaction was inserted directly into Room database
        val transactions = db.transactionDao().getAllTransactionsList(testUserId)
        val createdTx = transactions.find { it.title == "اجاره خانه" }
        assertNotNull("Recurring expense transaction should be created in database", createdTx)
        assertEquals(15000000L, createdTx?.amount)
        assertEquals("EXPENSE", createdTx?.type)
        assertEquals(true, createdTx?.isRecurring)

        val countFirstRun = transactions.count { it.title == "اجاره خانه" }
        assertEquals(1, countFirstRun)

        // Run due processing second time immediately -> Database-level deduplication check must prevent second insert!
        repository.processDueRecurringTransactions(context)

        val transactionsSecondRun = db.transactionDao().getAllTransactionsList(testUserId)
        val countSecondRun = transactionsSecondRun.count { it.title == "اجاره خانه" }
        assertEquals("Duplicate transaction should NOT be created on second worker run", 1, countSecondRun)
    }

    @Test
    fun testProcessDueRecurringTransaction_createsIncomeCorrectly() = runBlocking {
        val todayJalali = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()

        val recurringIncome = RecurringTransaction(
            id = "rec_inc_${UUID.randomUUID()}",
            title = "حقوق ماهانه پروژه",
            amount = 45000000L,
            type = TransactionType.INCOME,
            categoryId = "salary",
            categoryTitle = "حقوق و دستمزد",
            frequency = RecurringFrequency.MONTHLY,
            startDate = todayJalali,
            nextExecutionDate = todayJalali,
            enabled = true,
            paymentMethod = PaymentMethod.BANK_CARD,
            accountName = "کارت بانکی"
        )

        repository.addRecurringTransaction(recurringIncome)

        repository.processDueRecurringTransactions(context)

        val transactions = db.transactionDao().getAllTransactionsList(testUserId)
        val incomeTx = transactions.find { it.title == "حقوق ماهانه پروژه" }
        assertNotNull("Recurring income transaction should be created", incomeTx)
        assertEquals(45000000L, incomeTx?.amount)
        assertEquals("INCOME", incomeTx?.type)
    }

    @Test
    fun testDisabledRecurringTransaction_isNotProcessed() = runBlocking {
        val todayJalali = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()

        val disabledRecurring = RecurringTransaction(
            id = "rec_disabled_${UUID.randomUUID()}",
            title = "اشتراک غیرفعال",
            amount = 200000L,
            type = TransactionType.EXPENSE,
            categoryId = "bill",
            categoryTitle = "قبض و اشتراک",
            frequency = RecurringFrequency.MONTHLY,
            startDate = todayJalali,
            nextExecutionDate = todayJalali,
            enabled = false
        )

        repository.addRecurringTransaction(disabledRecurring)

        repository.processDueRecurringTransactions(context)

        val transactions = db.transactionDao().getAllTransactionsList(testUserId)
        val disabledTx = transactions.find { it.title == "اشتراک غیرفعال" }
        assertNull("Disabled recurring item should not create any transaction", disabledTx)
    }

    @Test
    fun testInvalidAccountId_preventsTransactionInsertionAndPreventsAdvancingDate() = runBlocking {
        val todayJalali = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()

        val invalidAccItem = RecurringTransaction(
            id = "rec_invalid_acc_${UUID.randomUUID()}",
            title = "پرداخت با حساب نامعتبر",
            amount = 500000L,
            type = TransactionType.EXPENSE,
            categoryId = "test",
            categoryTitle = "تست",
            frequency = RecurringFrequency.MONTHLY,
            startDate = todayJalali,
            nextExecutionDate = todayJalali,
            enabled = true,
            accountId = "non_existent_deleted_account_id_999"
        )

        repository.addRecurringTransaction(invalidAccItem)

        repository.processDueRecurringTransactions(context)

        val transactions = db.transactionDao().getAllTransactionsList(testUserId)
        val tx = transactions.find { it.title == "پرداخت با حساب نامعتبر" }
        assertNull("Transaction should NOT be inserted into invalid or deleted account", tx)

        // Verify next execution date was NOT advanced
        val itemInRepo = repository.getRecurringTransactions().first().find { it.id == invalidAccItem.id }
        assertEquals("Next execution date should remain unchanged when account is invalid", todayJalali, itemInRepo?.nextExecutionDate)
    }

    @Test
    fun testUserIsolation_userBCannotCreateTransactionsForUserA() = runBlocking {
        val todayJalali = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()

        val userARecurring = RecurringTransaction(
            id = "rec_userA_${UUID.randomUUID()}",
            title = "تراکنش کاربر الف",
            amount = 100000L,
            type = TransactionType.EXPENSE,
            categoryId = "test",
            categoryTitle = "تست",
            frequency = RecurringFrequency.MONTHLY,
            startDate = todayJalali,
            nextExecutionDate = todayJalali,
            enabled = true
        )
        repository.addRecurringTransaction(userARecurring)

        // Switch active user session to User B
        val userB = NetworkUserDto(
            id = "user_B_999",
            fullName = "User B",
            email = "userB@example.com",
            phoneNumber = "09999999999",
            phoneVerified = true
        )
        SessionManager.setAuthenticatedUser(userB)
        repository.ensureDefaultAccounts(db, "user_B_999")

        // Run process for User B
        repository.processDueRecurringTransactions(context)

        val userBTransactions = db.transactionDao().getAllTransactionsList("user_B_999")
        val userBTx = userBTransactions.find { it.title == "تراکنش کاربر الف" }
        assertNull("User B worker run must not process User A's recurring transactions", userBTx)
    }

    @Test
    fun testInsertionErrorInRoom_preventsAdvancingDate() = runBlocking {
        val todayJalali = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()

        val recurringExpense = RecurringTransaction(
            id = "rec_db_error_12345",
            title = "خطای دیتابیس",
            amount = 100000L,
            type = TransactionType.EXPENSE,
            categoryId = "home",
            categoryTitle = "خانه",
            frequency = RecurringFrequency.MONTHLY,
            startDate = todayJalali,
            nextExecutionDate = todayJalali,
            enabled = true,
            accountName = "کارت بانکی"
        )
        repository.addRecurringTransaction(recurringExpense)

        // Create a SQLite BEFORE INSERT trigger to force a DB exception on insertion
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_insert BEFORE INSERT ON transactions BEGIN SELECT RAISE(ABORT, 'Simulated DB Error'); END;"
        )

        try {
            // Processing should fail on DB insertion and NOT advance nextExecutionDate
            repository.processDueRecurringTransactions(context)

            val itemInRepo = repository.getRecurringTransactions().first().find { it.id == recurringExpense.id }
            assertEquals("Next execution date should NOT advance when Room insertion fails", todayJalali, itemInRepo?.nextExecutionDate)
        } finally {
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS fail_insert;")
        }
    }

    @Test
    fun testTemplateWithoutAccountIdAndNoMatchingAccountName_doesNotInsertOnOtherAccountAndKeepsDate() = runBlocking {
        val todayJalali = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()

        val itemWithoutAccount = RecurringTransaction(
            id = "rec_no_acc_name_${UUID.randomUUID()}",
            title = "پرداخت بدون حساب معتبر",
            amount = 750000L,
            type = TransactionType.EXPENSE,
            categoryId = "test",
            categoryTitle = "تست",
            frequency = RecurringFrequency.MONTHLY,
            startDate = todayJalali,
            nextExecutionDate = todayJalali,
            enabled = true,
            accountId = "",
            accountName = "حساب ناموجود و نامعتبر"
        )

        repository.addRecurringTransaction(itemWithoutAccount)

        repository.processDueRecurringTransactions(context)

        val transactions = db.transactionDao().getAllTransactionsList(testUserId)
        val tx = transactions.find { it.title == "پرداخت بدون حساب معتبر" }
        assertNull("Transaction should NOT be inserted into another fallback account", tx)

        val itemInRepo = repository.getRecurringTransactions().first().find { it.id == itemWithoutAccount.id }
        assertEquals("Next execution date should remain unchanged when no matching account found", todayJalali, itemInRepo?.nextExecutionDate)
    }
}
