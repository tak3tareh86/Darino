package com.example.finance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.NetworkUserDto
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.domain.RecurringDateCalculator
import com.example.ui.screens.finance.model.PaymentMethod
import com.example.ui.screens.finance.model.RecurringFrequency
import com.example.ui.screens.finance.model.RecurringTransaction
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.finance.worker.RecurringTransactionNotificationManager
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
        context.getSharedPreferences("darino_general_preferences", Context.MODE_PRIVATE)
            .edit()
            .remove("pref_persisted_recurring_txs_$testUserId")
            .commit()
        context.getSharedPreferences("darino_recurring_pending_notifications", Context.MODE_PRIVATE)
            .edit()
            .remove("pending_$testUserId")
            .commit()

        SessionManager.init(context)
        SessionManager.setAuthenticatedUser(
            NetworkUserDto(
                id = testUserId,
                fullName = "recurring_tester",
                email = "recurring@example.com",
                phoneNumber = "09123456789",
                phoneVerified = true
            )
        )

        repository = LocalFinanceRepository.instance
        repository.init(context)

        val field = LocalFinanceRepository::class.java.getDeclaredField("_recurringTransactions")
        field.isAccessible = true
        (field.get(repository) as MutableStateFlow<List<RecurringTransaction>>).value = emptyList()

        db = AppDatabase.getDatabase(context)
        db.transactionDao().clearAllTransactions(testUserId)
        repository.ensureDefaultAccounts(db, testUserId)
    }

    private fun today(): String =
        PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()

    private suspend fun defaultAccountId(): String =
        db.accountDao().getAllAccountsList(testUserId)
            .first { it.name == "کارت بانکی" && it.isActive && it.deletedAt == null }
            .stringId

    private fun recurring(
        id: String = "rec_${UUID.randomUUID()}",
        title: String = "تراکنش دوره‌ای آزمایشی",
        type: TransactionType = TransactionType.EXPENSE,
        accountId: String? = null,
        accountName: String = "کارت بانکی",
        startDate: String = today(),
        nextDate: String = today(),
        endDate: String? = null,
        enabled: Boolean = true,
        frequency: RecurringFrequency = RecurringFrequency.MONTHLY
    ) = RecurringTransaction(
        id = id,
        title = title,
        amount = 1_500_000L,
        type = type,
        categoryId = if (type == TransactionType.INCOME) "salary" else "home",
        categoryTitle = if (type == TransactionType.INCOME) "حقوق و دستمزد" else "خانه و اجاره",
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        nextExecutionDate = nextDate,
        enabled = enabled,
        paymentMethod = PaymentMethod.BANK_CARD,
        accountName = accountName,
        accountId = accountId
    )

    private fun occurrenceId(recurringId: String, date: String): String {
        val digits = com.example.calendar.domain.CalendarDateUtils.normalizeDate(date)
            .replace("/", "")
            .replace("-", "")
        return "rec_exec_${recurringId}_$digits"
    }

    @Test
    fun dueExpenseCreatesPendingOccurrenceAndOnlyConfirmationWritesTransaction() = runBlocking {
        val date = today()
        val item = recurring(id = "rec_expense_confirm", title = "اجاره خانه", accountId = defaultAccountId())
        repository.addRecurringTransaction(item)

        repository.processDueRecurringTransactions(context)

        val id = occurrenceId(item.id, date)
        assertTrue(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))
        assertTrue(db.transactionDao().getAllTransactionsList(testUserId).none { it.title == item.title })

        // Re-running the worker reuses the same pending occurrence, not a second one.
        repository.processDueRecurringTransactions(context)
        assertTrue(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))
        assertTrue(db.transactionDao().getAllTransactionsList(testUserId).none { it.title == item.title })

        assertTrue(repository.confirmPendingRecurringTransaction(context, id))
        val transactions = db.transactionDao().getAllTransactionsList(testUserId)
        val created = transactions.single { it.stringId == id }
        assertEquals("EXPENSE", created.type)
        assertEquals(item.amount, created.amount)
        assertTrue(created.isRecurring)
        assertFalse(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))
        assertFalse(repository.confirmPendingRecurringTransaction(context, id))
        assertEquals(1, db.transactionDao().getAllTransactionsList(testUserId).count { it.stringId == id })
    }

    @Test
    fun dueIncomeRequiresConfirmationBeforeInsertion() = runBlocking {
        val date = today()
        val item = recurring(
            id = "rec_income_confirm",
            title = "حقوق ماهانه پروژه",
            type = TransactionType.INCOME,
            accountId = defaultAccountId()
        )
        repository.addRecurringTransaction(item)

        repository.processDueRecurringTransactions(context)

        val id = occurrenceId(item.id, date)
        assertTrue(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))
        assertTrue(db.transactionDao().getAllTransactionsList(testUserId).none { it.stringId == id })

        assertTrue(repository.confirmPendingRecurringTransaction(context, id))
        val created = db.transactionDao().getAllTransactionsList(testUserId).single { it.stringId == id }
        assertEquals("INCOME", created.type)
    }

    @Test
    fun disabledRecurringTemplateCancelsPendingOccurrenceAndRejectsConfirmation() = runBlocking {
        val date = today()
        val item = recurring(id = "rec_disable_pending", accountId = defaultAccountId())
        repository.addRecurringTransaction(item)
        repository.processDueRecurringTransactions(context)

        val id = occurrenceId(item.id, date)
        assertTrue(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))

        repository.setRecurringEnabled(item.id, false)

        assertFalse(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))
        assertFalse(repository.confirmPendingRecurringTransaction(context, id))
        assertTrue(db.transactionDao().getAllTransactionsList(testUserId).none { it.stringId == id })
    }

    @Test
    fun deletingRecurringTemplateCancelsPendingOccurrence() = runBlocking {
        val date = today()
        val item = recurring(id = "rec_delete_pending", accountId = defaultAccountId())
        repository.addRecurringTransaction(item)
        repository.processDueRecurringTransactions(context)

        val id = occurrenceId(item.id, date)
        assertTrue(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))

        repository.deleteRecurringTransaction(item.id)

        assertFalse(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))
        assertFalse(repository.confirmPendingRecurringTransaction(context, id))
        assertTrue(db.transactionDao().getAllTransactionsList(testUserId).none { it.stringId == id })
    }

    @Test
    fun invalidOrDeletedAccountDoesNotCreatePendingOccurrenceOrAdvanceDate() = runBlocking {
        val date = today()
        val item = recurring(
            id = "rec_invalid_account",
            accountId = "deleted_or_missing_account_id",
            accountName = "حساب حذف‌شده"
        )
        repository.addRecurringTransaction(item)

        repository.processDueRecurringTransactions(context)

        val id = occurrenceId(item.id, date)
        assertFalse(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))
        assertTrue(db.transactionDao().getAllTransactionsList(testUserId).none { it.stringId == id })
        val stored = repository.getRecurringTransactions().first().single { it.id == item.id }
        assertEquals(date, stored.nextExecutionDate)
    }

    @Test
    fun anotherUserWorkerRunCannotCreateOrShowFirstUsersRecurringOccurrence() = runBlocking {
        val date = today()
        val item = recurring(id = "rec_user_a", accountId = defaultAccountId())
        repository.addRecurringTransaction(item)

        SessionManager.setAuthenticatedUser(
            NetworkUserDto(
                id = "user_B_999",
                fullName = "User B",
                email = "userB@example.com",
                phoneNumber = "09999999999",
                phoneVerified = true
            )
        )
        repository.processDueRecurringTransactions(context)

        val id = occurrenceId(item.id, date)
        assertFalse(RecurringTransactionNotificationManager.hasPending(context, "user_B_999", id))
        assertTrue(db.transactionDao().getAllTransactionsList("user_B_999").none { it.title == item.title })
    }

    @Test
    fun failedDatabaseConfirmationKeepsPendingOccurrenceForRetry() = runBlocking {
        val date = today()
        val item = recurring(id = "rec_retry_confirmation", accountId = defaultAccountId())
        repository.addRecurringTransaction(item)
        repository.processDueRecurringTransactions(context)

        val id = occurrenceId(item.id, date)
        assertTrue(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))

        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_insert BEFORE INSERT ON transactions BEGIN SELECT RAISE(ABORT, 'Simulated DB Error'); END;"
        )
        try {
            assertFalse(repository.confirmPendingRecurringTransaction(context, id))
            assertTrue(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))
            assertTrue(db.transactionDao().getAllTransactionsList(testUserId).none { it.stringId == id })
        } finally {
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS fail_insert;")
        }

        assertTrue(repository.confirmPendingRecurringTransaction(context, id))
        assertFalse(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))
        assertEquals(1, db.transactionDao().getAllTransactionsList(testUserId).count { it.stringId == id })
    }

    @Test
    fun recurringTemplateWithUnknownAccountNameDoesNotCreatePendingOccurrence() = runBlocking {
        val date = today()
        val item = recurring(
            id = "rec_unknown_account_name",
            accountId = null,
            accountName = "حساب ناموجود"
        )
        repository.addRecurringTransaction(item)

        repository.processDueRecurringTransactions(context)

        val id = occurrenceId(item.id, date)
        assertFalse(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))
        val stored = repository.getRecurringTransactions().first().single { it.id == item.id }
        assertEquals(date, stored.nextExecutionDate)
    }

    @Test
    fun repositoryRejectsInvalidJalaliRecurringDates() = runBlocking {
        val invalid = recurring(
            id = "rec_invalid_date",
            accountId = defaultAccountId(),
            startDate = "۱۴۰۵/۱۲/۳۰",
            nextDate = "۱۴۰۵/۱۲/۳۰"
        )
        repository.addRecurringTransaction(invalid)

        assertFalse(repository.getRecurringTransactions().first().any { it.id == invalid.id })
    }

    @Test
    fun monthlyRecurrenceRestoresOriginalDayAfterShortMonth() {
        val afterEsfand = RecurringDateCalculator.nextDate(
            currentDate = "۱۴۰۵/۱۲/۲۹",
            frequency = RecurringFrequency.MONTHLY,
            anchorDayOfMonth = 31,
            anchorMonthOfYear = 12
        )
        assertEquals("۱۴۰۶/۰۱/۳۱", afterEsfand)
    }

    @Test
    fun yearlyRecurrenceRestoresLeapDayAnchorInNextJalaliLeapYear() {
        var date = "۱۴۰۳/۱۲/۳۰"
        repeat(5) {
            date = RecurringDateCalculator.nextDate(
                currentDate = date,
                frequency = RecurringFrequency.YEARLY,
                anchorDayOfMonth = 30,
                anchorMonthOfYear = 12
            ) ?: fail("Expected a valid next Jalali date")
        }
        assertEquals("۱۴۰۸/۱۲/۳۰", date)
    }

    @Test
    fun editingRecurringTemplateCancelsOldPendingPayload() = runBlocking {
        val date = today()
        val item = recurring(id = "rec_edit_pending", accountId = defaultAccountId())
        repository.addRecurringTransaction(item)
        repository.processDueRecurringTransactions(context)

        val id = occurrenceId(item.id, date)
        assertTrue(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))

        repository.updateRecurringTransaction(item.copy(amount = item.amount + 100_000L))

        assertFalse(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))
        assertFalse(repository.confirmPendingRecurringTransaction(context, id))
        assertTrue(db.transactionDao().getAllTransactionsList(testUserId).none { it.stringId == id })
    }

    @Test
    fun pendingOccurrenceOnEndDateRemainsAvailableForConfirmation() = runBlocking {
        val date = today()
        val item = recurring(
            id = "rec_end_date_pending",
            accountId = defaultAccountId(),
            endDate = date
        )
        repository.addRecurringTransaction(item)
        repository.processDueRecurringTransactions(context)

        val id = occurrenceId(item.id, date)
        assertTrue(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))

        // A later worker run sees that there are no future scheduled dates, but must
        // not discard the occurrence that was already due on the inclusive end date.
        repository.processDueRecurringTransactions(context)

        assertTrue(RecurringTransactionNotificationManager.hasPending(context, testUserId, id))
        assertTrue(repository.confirmPendingRecurringTransaction(context, id))
        assertEquals(1, db.transactionDao().getAllTransactionsList(testUserId).count { it.stringId == id })
    }

}
