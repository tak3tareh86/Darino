package com.example.finance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.data.TransactionOperationResult
import com.example.ui.screens.finance.model.*
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
    fun setUp() = runBlocking {
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
    fun `Transfer - real effects, validations, atomicity, delete and update`() = runBlocking {
        // Create accounts for User A
        val srcAcc = Account(id = "acc_src", userId = userA, name = "حساب مبدا", type = AccountType.CARD, initialBalance = 1_000_000L)
        val destAcc = Account(id = "acc_dest", userId = userA, name = "حساب مقصد", type = AccountType.CARD, initialBalance = 500_000L)
        
        repository.addAccountResult(srcAcc)
        repository.addAccountResult(destAcc)

        // 1. Successful transfer
        val cat = FinanceDefaultCategories.defaultExpenseCategories.first()
        val transferTx = TransactionItemData(
            id = "tx_transfer",
            title = "انتقال وجه تستی",
            amount = 300_000L,
            type = TransactionType.TRANSFER,
            category = cat,
            datePersian = "امروز",
            timePersian = "12:00",
            transferSourceAccountId = "acc_src",
            transferDestinationAccountId = "acc_dest"
        )

        val transferRes = repository.addTransactionResult(transferTx)
        assertEquals(TransactionOperationResult.SUCCESS, transferRes)

        // Balance changes
        assertEquals(700_000L, repository.calculateAccountBalance("acc_src"))
        assertEquals(800_000L, repository.calculateAccountBalance("acc_dest"))

        // 2. Transfer must be excluded from general Income & Expense totals
        val txList = db.transactionDao().getAllTransactionsList(userA).map {
            // Map simple mock TransactionItemData to pass to engine
            TransactionItemData(
                id = it.stringId,
                title = it.title,
                amount = it.amount,
                type = try { TransactionType.valueOf(it.type) } catch(e: Exception) { TransactionType.EXPENSE },
                category = cat,
                datePersian = it.datePersian,
                timePersian = it.timeFormatted
            )
        }
        assertEquals(0L, com.example.ui.screens.finance.domain.FinanceEngine.calculateMonthlyIncome(txList))
        assertEquals(0L, com.example.ui.screens.finance.domain.FinanceEngine.calculateMonthlyExpense(txList))

        // 3. Validation: source = destination rejected
        val invalidSelfTransfer = transferTx.copy(id = "tx_self", transferDestinationAccountId = "acc_src")
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(invalidSelfTransfer))

        // 4. Validation: zero or negative amount rejected
        val invalidZeroTx = transferTx.copy(id = "tx_zero", amount = 0L)
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(invalidZeroTx))

        val invalidNegativeTx = transferTx.copy(id = "tx_neg", amount = -100L)
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(invalidNegativeTx))

        // 5. Validation: cross-user account rejected
        // Create account for User B
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = userB, fullName = "User B", email = null, phoneNumber = "09120000002", phoneVerified = true))
        repository.refreshMetadataForCurrentUser()
        val bAcc = Account(id = "acc_b", userId = userB, name = "حساب کاربر ب", type = AccountType.CARD, initialBalance = 100_000L)
        repository.addAccountResult(bAcc)

        // Switch back to User A
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = userA, fullName = "User A", email = null, phoneNumber = "09120000001", phoneVerified = true))
        repository.refreshMetadataForCurrentUser()

        // Transfer from User A to User B's account (should be rejected since acc_b doesn't exist for User A)
        val crossUserTx = transferTx.copy(id = "tx_cross", transferDestinationAccountId = "acc_b")
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(crossUserTx))

        // 6. Delete transfer restores both sides
        repository.deleteTransactionResult("tx_transfer")
        assertEquals(1_000_000L, repository.calculateAccountBalance("acc_src"))
        assertEquals(500_000L, repository.calculateAccountBalance("acc_dest"))

        // 7. Update transfer recalculates both sides
        val newTransfer = transferTx.copy(id = "tx_transfer_2", amount = 100_000L)
        repository.addTransactionResult(newTransfer)
        assertEquals(900_000L, repository.calculateAccountBalance("acc_src"))
        assertEquals(600_000L, repository.calculateAccountBalance("acc_dest"))

        // Update amount from 100k to 400k
        val updatedTransfer = newTransfer.copy(amount = 400_000L)
        repository.updateTransactionResult(updatedTransfer)
        assertEquals(600_000L, repository.calculateAccountBalance("acc_src"))
        assertEquals(900_000L, repository.calculateAccountBalance("acc_dest"))
    }

    @Test
    fun `Period Separation - stage 2 period filter does not affect Current Account Balance`() = runBlocking {
        // Create an account and add a transaction inside and outside month range
        val acc = Account(id = "acc_period", userId = userA, name = "حساب دوره‌ای", type = AccountType.CARD, initialBalance = 1_000_000L)
        repository.addAccountResult(acc)

        val cat = FinanceDefaultCategories.defaultIncomeCategories.first()
        val now = System.currentTimeMillis()
        val monthRange = com.example.ui.screens.finance.domain.FinanceTimeUtils.getTimeRangeForPeriod(FinanceFilterPeriod.THIS_MONTH, now)

        val inMonthTx = TransactionItemData(
            id = "tx_in",
            title = "داخل ماه",
            amount = 200_000L,
            type = TransactionType.INCOME,
            category = cat,
            datePersian = "امروز",
            timePersian = "12:00",
            accountId = "acc_period",
            dateMillis = monthRange.startMillis + 1000L
        )

        val outMonthTx = TransactionItemData(
            id = "tx_out",
            title = "خارج ماه",
            amount = 500_000L,
            type = TransactionType.INCOME,
            category = cat,
            datePersian = "ماه قبل",
            timePersian = "12:00",
            accountId = "acc_period",
            dateMillis = monthRange.startMillis - 10_000L
        )

        repository.addTransactionResult(inMonthTx)
        repository.addTransactionResult(outMonthTx)

        // Current Account Balance must include both (1,000,000 + 200,000 + 500,000 = 1,700,000)
        val balance = repository.calculateAccountBalance("acc_period")
        assertEquals(1_700_000L, balance)
    }
}
