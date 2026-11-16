package com.example.finance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.data.TransactionOperationResult
import com.example.ui.screens.finance.model.*
import com.example.ui.screens.settings.model.AppCurrency
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
@Config(sdk = [36])
class AccountTransferTest {

    private lateinit var context: Context
    private lateinit var repository: LocalFinanceRepository
    private lateinit var db: AppDatabase

    private val userA = "user_A"
    private val userB = "user_B"

    @Before
    fun setUp() {
        runBlocking {
            context = ApplicationProvider.getApplicationContext()
            SessionManager.setAuthenticatedUser(
                com.example.data.api.NetworkUserDto(
                    id = userA,
                    fullName = "کاربر آ",
                    email = null,
                    phoneNumber = "09120000001",
                    phoneVerified = true
                )
            )
            repository = LocalFinanceRepository.instance
            repository.init(context)
            db = AppDatabase.getDatabase(context)
            db.transactionDao().clearAllTransactions(userA)
            db.transactionDao().clearAllTransactions(userB)
            db.accountDao().clearAllAccounts(userA)
            db.accountDao().clearAllAccounts(userB)
        }
    }

    @Test
    fun `Account - create, read, update, user isolation, duplicate identity, inactive policy`() = runBlocking {
        val acc = Account(
            id = "acc_1",
            userId = userA,
            name = "حساب پاسارگاد",
            type = AccountType.CARD,
            initialBalance = 1_000_000L,
            isActive = true
        )

        // 1. Create account
        val createRes = repository.addAccountResult(acc)
        assertEquals(TransactionOperationResult.SUCCESS, createRes)

        // 2. Read account from DB directly (fully synchronous under Robolectric)
        val list = db.accountDao().getAllAccountsList(userA)
        assertTrue(list.any { it.stringId == "acc_1" && it.name == "حساب پاسارگاد" })

        // 3. Update account
        val updated = acc.copy(name = "حساب پاسارگاد جدید", initialBalance = 2_000_000L)
        val updateRes = repository.updateAccountResult(updated)
        assertEquals(TransactionOperationResult.SUCCESS, updateRes)
        
        val listAfterUpdate = db.accountDao().getAllAccountsList(userA)
        val reloaded = listAfterUpdate.find { it.stringId == "acc_1" }
        assertNotNull(reloaded)
        assertEquals("حساب پاسارگاد جدید", reloaded!!.name)

        // 4. Duplicate identity within user scope
        // Inserting another account with same ID "acc_1" for same user should trigger constraint violation (PERSISTENCE_ERROR)
        val duplicate = Account(
            id = "acc_1",
            userId = userA,
            name = "حساب همزاد",
            type = AccountType.BANK,
            initialBalance = 500L
        )
        val dupRes = repository.addAccountResult(duplicate)
        assertEquals(TransactionOperationResult.PERSISTENCE_ERROR, dupRes)

        // 5. User isolation
        // Log in as User B
        SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(
                id = userB,
                fullName = "کاربر ب",
                email = null,
                phoneNumber = "09120000002",
                phoneVerified = true
            )
        )
        // Refresh metadata/repository scope
        repository.refreshMetadataForCurrentUser()
        
        // User B list of accounts should be empty
        val listUserB = db.accountDao().getAllAccountsList(userB)
        assertFalse(listUserB.any { it.stringId == "acc_1" })

        // User B cannot see/update User A's account
        val userBUpdateRes = repository.updateAccountResult(updated)
        assertEquals(TransactionOperationResult.NOT_FOUND, userBUpdateRes)
    }

    @Test
    fun `Balance - initial, income, expense, soft deleted transactions, account isolation`() = runBlocking {
        // Create two accounts for User A
        val acc1 = Account(id = "acc_x", userId = userA, name = "حساب ایکس", type = AccountType.CARD, initialBalance = 500_000L)
        val acc2 = Account(id = "acc_y", userId = userA, name = "حساب وای", type = AccountType.CARD, initialBalance = 200_000L)
        
        repository.addAccountResult(acc1)
        repository.addAccountResult(acc2)

        // Verify initial balance
        assertEquals(500_000L, repository.calculateAccountBalance("acc_x"))
        assertEquals(200_000L, repository.calculateAccountBalance("acc_y"))

        // Add Income to acc_x
        val cat = FinanceDefaultCategories.defaultIncomeCategories.first()
        val txIncome = TransactionItemData(
            id = "tx_i",
            title = "حقوق",
            amount = 1_000_000L,
            type = TransactionType.INCOME,
            category = cat,
            datePersian = "امروز",
            timePersian = "12:00",
            accountId = "acc_x"
        )
        repository.addTransactionResult(txIncome)

        // acc_x should increase, acc_y should be unchanged
        assertEquals(1_500_000L, repository.calculateAccountBalance("acc_x"))
        assertEquals(200_000L, repository.calculateAccountBalance("acc_y"))

        // Add Expense to acc_x
        val catExp = FinanceDefaultCategories.defaultExpenseCategories.first()
        val txExpense = TransactionItemData(
            id = "tx_e",
            title = "خرید",
            amount = 300_000L,
            type = TransactionType.EXPENSE,
            category = catExp,
            datePersian = "امروز",
            timePersian = "12:00",
            accountId = "acc_x"
        )
        repository.addTransactionResult(txExpense)

        // acc_x should decrease, acc_y should be unchanged
        assertEquals(1_200_000L, repository.calculateAccountBalance("acc_x"))
        assertEquals(200_000L, repository.calculateAccountBalance("acc_y"))

        // Soft delete Income
        repository.deleteTransactionResult("tx_i")
        // balance should revert to initial balance - expense (500,000 - 300,000 = 200,000)
        assertEquals(200_000L, repository.calculateAccountBalance("acc_x"))

        // Soft delete Expense
        repository.deleteTransactionResult("tx_e")
        // balance should revert to initial balance (500,000)
        assertEquals(500_000L, repository.calculateAccountBalance("acc_x"))
    }

        
    @Test
    fun `Transfer - record only and never changes any account balance`() = runBlocking {
        val source = Account(id = "source", userId = userA, name = "کارت بانکی من", type = AccountType.CARD, initialBalance = 1_000_000L)
        val unrelated = Account(id = "unrelated", userId = userA, name = "حساب دیگر من", type = AccountType.CARD, initialBalance = 500_000L)
        assertEquals(TransactionOperationResult.SUCCESS, repository.addAccountResult(source))
        assertEquals(TransactionOperationResult.SUCCESS, repository.addAccountResult(unrelated))

        val transfer = TransactionItemData(
            id = "external_transfer",
            title = "انتقال وجه به شخص دیگر",
            amount = 300_000L,
            type = TransactionType.TRANSFER,
            category = FinanceDefaultCategories.transferCategory,
            datePersian = "امروز",
            timePersian = "12:00",
            accountName = source.name,
            transferSourceAccountId = source.id,
            transferDestinationAccountId = null,
            transferRecipientName = "گیرنده آزمایشی"
        )
        assertEquals(TransactionOperationResult.SUCCESS, repository.addTransactionResult(transfer))
        val stored = db.transactionDao().getTransactionByStringId(userA, transfer.id)
        assertEquals("گیرنده آزمایشی", stored?.transferRecipientName)
        assertNull(stored?.transferDestinationAccountId)
        assertEquals(1_000_000L, repository.calculateAccountBalance(source.id))
        assertEquals(500_000L, repository.calculateAccountBalance(unrelated.id))

        val transferCategory = FinanceDefaultCategories.transferCategory
        val persistedTransactions = db.transactionDao().getAllTransactionsList(userA).map { entity ->
            TransactionItemData(
                id = entity.stringId,
                title = entity.title,
                amount = entity.amount,
                type = TransactionType.valueOf(entity.type),
                category = transferCategory,
                datePersian = entity.datePersian,
                timePersian = entity.timeFormatted,
                dateMillis = entity.timestamp
            )
        }
        assertEquals(0L, com.example.ui.screens.finance.domain.FinanceEngine.calculateMonthlyIncome(persistedTransactions))
        assertEquals(0L, com.example.ui.screens.finance.domain.FinanceEngine.calculateMonthlyExpense(persistedTransactions))

        val edited = transfer.copy(amount = 400_000L, transferRecipientName = "گیرنده ویرایش‌شده")
        assertEquals(TransactionOperationResult.SUCCESS, repository.updateTransactionResult(edited))
        assertEquals(1_000_000L, repository.calculateAccountBalance(source.id))
        assertEquals(500_000L, repository.calculateAccountBalance(unrelated.id))

        assertEquals(TransactionOperationResult.SUCCESS, repository.deleteTransactionResult(transfer.id))
        assertEquals(1_000_000L, repository.calculateAccountBalance(source.id))
        assertEquals(500_000L, repository.calculateAccountBalance(unrelated.id))
    }

    @Test
    fun `Transfer - only active current-user source account is required`() = runBlocking {
        val source = Account(id = "source_valid", userId = userA, name = "کارت مبدأ", type = AccountType.CARD)
        val inactive = Account(id = "source_inactive", userId = userA, name = "کارت غیرفعال", type = AccountType.CARD, isActive = false)
        val deleted = Account(id = "source_deleted", userId = userA, name = "کارت حذف‌شده", type = AccountType.CARD)
        repository.addAccountResult(source)
        repository.addAccountResult(inactive)
        repository.addAccountResult(deleted)
        repository.deleteAccountResult(deleted.id)

        val base = TransactionItemData(
            id = "source_validation",
            title = "انتقال وجه",
            amount = 100_000L,
            type = TransactionType.TRANSFER,
            category = FinanceDefaultCategories.transferCategory,
            datePersian = "امروز",
            timePersian = "12:00",
            accountName = source.name,
            transferSourceAccountId = source.id,
            transferDestinationAccountId = null,
            transferRecipientName = "شخص دیگر"
        )
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "missing_source", transferSourceAccountId = null)))
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "blank_source", transferSourceAccountId = "")))
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "unknown_source", transferSourceAccountId = "not_local")))

        SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(
                id = userB,
                fullName = "کاربر ب",
                email = null,
                phoneNumber = "09120000002",
                phoneVerified = true
            )
        )
        repository.refreshMetadataForCurrentUser()
        val foreignSource = Account(id = "foreign_source", userId = userB, name = "کارت کاربر دیگر", type = AccountType.CARD)
        assertEquals(TransactionOperationResult.SUCCESS, repository.addAccountResult(foreignSource))
        SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(
                id = userA,
                fullName = "کاربر آ",
                email = null,
                phoneNumber = "09120000001",
                phoneVerified = true
            )
        )
        repository.refreshMetadataForCurrentUser()
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "foreign_source_transfer", transferSourceAccountId = foreignSource.id)))

        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "inactive_source", transferSourceAccountId = inactive.id)))
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "deleted_source", transferSourceAccountId = deleted.id)))
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "zero_amount", amount = 0L)))
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "negative_amount", amount = -1L)))

        // The recipient is external; no second account belonging to this user is required.
        assertEquals(TransactionOperationResult.SUCCESS, repository.addTransactionResult(base))
    }

    @Test
    fun `Transfer - mixed Rial and Toman values are stored without changing balances`() = runBlocking {
        val source = Account(
            id = "mixed_source",
            userId = userA,
            name = "کارت مبدأ",
            type = AccountType.CARD,
            initialBalance = 1_000_000L,
            initialBalanceUnit = AppCurrency.RIAL
        )
        assertEquals(TransactionOperationResult.SUCCESS, repository.addAccountResult(source))

        val transfer = TransactionItemData(
            id = "mixed_external_transfer",
            title = "انتقال به شخص دیگر",
            amount = 30_000L,
            amountUnit = AppCurrency.TOMAN,
            type = TransactionType.TRANSFER,
            category = FinanceDefaultCategories.transferCategory,
            datePersian = "امروز",
            timePersian = "12:00",
            accountName = source.name,
            transferSourceAccountId = source.id,
            transferDestinationAccountId = null,
            transferRecipientName = "گیرنده"
        )
        assertEquals(TransactionOperationResult.SUCCESS, repository.addTransactionResult(transfer))
        assertEquals(1_000_000L, repository.calculateAccountBalance(source.id))
    }

    @Test
    fun `Period Separation - stage 2 period filter does not affect Current Account Balance`() = runBlocking {
        val account = Account(id = "period_account", userId = userA, name = "حساب دوره‌ای", type = AccountType.CARD, initialBalance = 1_000_000L)
        repository.addAccountResult(account)
        val category = FinanceDefaultCategories.defaultIncomeCategories.first()
        val now = System.currentTimeMillis()
        val range = com.example.ui.screens.finance.domain.FinanceTimeUtils.getTimeRangeForPeriod(FinanceFilterPeriod.THIS_MONTH, now)

        val inPeriod = TransactionItemData(
            id = "period_inside",
            title = "داخل ماه",
            amount = 200_000L,
            type = TransactionType.INCOME,
            category = category,
            datePersian = "امروز",
            timePersian = "12:00",
            accountId = account.id,
            dateMillis = range.startMillis + 1_000L
        )
        val outsidePeriod = inPeriod.copy(
            id = "period_outside",
            title = "خارج ماه",
            amount = 500_000L,
            dateMillis = range.startMillis - 10_000L
        )
        repository.addTransactionResult(inPeriod)
        repository.addTransactionResult(outsidePeriod)
        assertEquals(1_700_000L, repository.calculateAccountBalance(account.id))
    }
}
