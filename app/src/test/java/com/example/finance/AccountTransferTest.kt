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
    fun `Transfer - external recipient reduces source only, validates, edits and deletes`() = runBlocking {
        val srcAcc = Account(id = "acc_src", userId = userA, name = "کارت بانکی من", type = AccountType.CARD, initialBalance = 1_000_000L)
        val unrelatedAcc = Account(id = "acc_other", userId = userA, name = "حساب دیگر من", type = AccountType.CARD, initialBalance = 500_000L)
        repository.addAccountResult(srcAcc)
        repository.addAccountResult(unrelatedAcc)

        val transferTx = TransactionItemData(
            id = "tx_transfer",
            title = "انتقال وجه به گیرنده آزمایشی",
            amount = 300_000L,
            type = TransactionType.TRANSFER,
            category = FinanceDefaultCategories.transferCategory,
            datePersian = "امروز",
            timePersian = "12:00",
            accountName = srcAcc.name,
            transferSourceAccountId = srcAcc.id,
            transferDestinationAccountId = null,
            transferRecipientName = "گیرنده آزمایشی"
        )

        assertEquals(TransactionOperationResult.SUCCESS, repository.addTransactionResult(transferTx))
        val storedTransfer = db.transactionDao().getTransactionByStringId(userA, "tx_transfer")
        assertEquals("گیرنده آزمایشی", storedTransfer?.transferRecipientName)
        assertEquals(null, storedTransfer?.transferDestinationAccountId)

        val renamedTransfer = transferTx.copy(transferRecipientName = "گیرنده ویرایش‌شده")
        assertEquals(TransactionOperationResult.SUCCESS, repository.updateTransactionResult(renamedTransfer))
        assertEquals(
            "گیرنده ویرایش‌شده",
            db.transactionDao().getTransactionByStringId(userA, "tx_transfer")?.transferRecipientName
        )

        // An outgoing transfer decreases only its source; it never credits another local account.
        assertEquals(700_000L, repository.calculateAccountBalance(srcAcc.id))
        assertEquals(500_000L, repository.calculateAccountBalance(unrelatedAcc.id))

        val cat = FinanceDefaultCategories.transferCategory
        val txList = db.transactionDao().getAllTransactionsList(userA).map {
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

        assertEquals(
            TransactionOperationResult.VALIDATION_ERROR,
            repository.addTransactionResult(transferTx.copy(id = "tx_no_source", transferSourceAccountId = null))
        )
        assertEquals(
            TransactionOperationResult.VALIDATION_ERROR,
            repository.addTransactionResult(transferTx.copy(id = "tx_zero", amount = 0L))
        )
        assertEquals(
            TransactionOperationResult.VALIDATION_ERROR,
            repository.addTransactionResult(transferTx.copy(id = "tx_negative", amount = -100_000L))
        )

        repository.deleteTransactionResult("tx_transfer")
        val deletedTransfer = db.transactionDao().getTransactionIncludingDeleted(userA, "tx_transfer")
        assertTrue(deletedTransfer?.deletedAt != null)
        assertEquals("گیرنده ویرایش‌شده", deletedTransfer?.transferRecipientName)
        assertEquals(1_000_000L, repository.calculateAccountBalance(srcAcc.id))
        assertEquals(500_000L, repository.calculateAccountBalance(unrelatedAcc.id))

        val newTransfer = transferTx.copy(id = "tx_transfer_2", amount = 100_000L)
        assertEquals(TransactionOperationResult.SUCCESS, repository.addTransactionResult(newTransfer))
        assertEquals(900_000L, repository.calculateAccountBalance(srcAcc.id))
        assertEquals(500_000L, repository.calculateAccountBalance(unrelatedAcc.id))

        val updatedTransfer = newTransfer.copy(amount = 400_000L)
        assertEquals(TransactionOperationResult.SUCCESS, repository.updateTransactionResult(updatedTransfer))
        assertEquals(600_000L, repository.calculateAccountBalance(srcAcc.id))
        assertEquals(500_000L, repository.calculateAccountBalance(unrelatedAcc.id))
    }

@Test
    fun `Income and Expense - validation rejects missing, invalid, foreign, deleted, inactive accounts`() = runBlocking {
        val activeAcc = Account(id = "acc_valid", userId = userA, name = "حساب معتبر", type = AccountType.CARD, initialBalance = 1_000_000L, isActive = true)
        val inactiveAcc = Account(id = "acc_inactive", userId = userA, name = "حساب غیرفعال", type = AccountType.CARD, initialBalance = 500_000L, isActive = false)
        val deletedAcc = Account(id = "acc_del", userId = userA, name = "حساب حذفی", type = AccountType.CARD, initialBalance = 200_000L, isActive = true)

        repository.addAccountResult(activeAcc)
        repository.addAccountResult(inactiveAcc)
        repository.addAccountResult(deletedAcc)
        repository.deleteAccountResult("acc_del")

        // User B account
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = userB, fullName = "User B", email = null, phoneNumber = "09120000002", phoneVerified = true))
        repository.refreshMetadataForCurrentUser()
        val userBAcc = Account(id = "acc_user_b", userId = userB, name = "حساب B", type = AccountType.CARD, initialBalance = 100_000L, isActive = true)
        repository.addAccountResult(userBAcc)

        // Switch back to User A
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = userA, fullName = "User A", email = null, phoneNumber = "09120000001", phoneVerified = true))
        repository.refreshMetadataForCurrentUser()

        val catInc = FinanceDefaultCategories.defaultIncomeCategories.first()
        val catExp = FinanceDefaultCategories.defaultExpenseCategories.first()

        // 1. Missing accountId -> FAIL
        val incNoAcc = TransactionItemData(id = "inc_no_acc", title = "درآمد بدون حساب", amount = 100_000L, type = TransactionType.INCOME, category = catInc, datePersian = "امروز", timePersian = "12:00", accountId = null)
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(incNoAcc))

        val expNoAcc = TransactionItemData(id = "exp_no_acc", title = "هزینه بدون حساب", amount = 50_000L, type = TransactionType.EXPENSE, category = catExp, datePersian = "امروز", timePersian = "12:00", accountId = "")
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(expNoAcc))

        // 2. Non-existent accountId -> FAIL
        val incInvalid = incNoAcc.copy(id = "inc_inv", accountId = "non_existent_acc")
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(incInvalid))

        val expInvalid = expNoAcc.copy(id = "exp_inv", accountId = "non_existent_acc")
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(expInvalid))

        // 3. Foreign user accountId -> FAIL
        val incForeign = incNoAcc.copy(id = "inc_foreign", accountId = "acc_user_b")
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(incForeign))

        val expForeign = expNoAcc.copy(id = "exp_foreign", accountId = "acc_user_b")
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(expForeign))

        // 4. Inactive accountId -> FAIL
        val incInactive = incNoAcc.copy(id = "inc_inact", accountId = "acc_inactive")
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(incInactive))

        val expInactive = expNoAcc.copy(id = "exp_inact", accountId = "acc_inactive")
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(expInactive))

        // 5. Deleted accountId -> FAIL
        val incDeleted = incNoAcc.copy(id = "inc_del", accountId = "acc_del")
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(incDeleted))

        val expDeleted = expNoAcc.copy(id = "exp_del", accountId = "acc_del")
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(expDeleted))

        // 6. Valid account -> SUCCESS & balance correctly adjusted
        val incValid = incNoAcc.copy(id = "inc_ok", accountId = "acc_valid", amount = 300_000L)
        assertEquals(TransactionOperationResult.SUCCESS, repository.addTransactionResult(incValid))
        assertEquals(1_300_000L, repository.calculateAccountBalance("acc_valid"))

        val expValid = expNoAcc.copy(id = "exp_ok", accountId = "acc_valid", amount = 100_000L)
        assertEquals(TransactionOperationResult.SUCCESS, repository.addTransactionResult(expValid))
        assertEquals(1_200_000L, repository.calculateAccountBalance("acc_valid"))
    }

    @Test
    fun `Transfer - validation rejects missing, invalid, foreign, deleted, inactive, same account, negative amount`() = runBlocking {
        val validSrc = Account(id = "t_src", userId = userA, name = "مبدأ", type = AccountType.CARD, initialBalance = 1_000_000L, isActive = true)
        val validDest = Account(id = "t_dest", userId = userA, name = "مقصد", type = AccountType.CARD, initialBalance = 500_000L, isActive = true)
        val inactDest = Account(id = "t_inact", userId = userA, name = "مقصد غیرفعال", type = AccountType.CARD, initialBalance = 0L, isActive = false)
        val delDest = Account(id = "t_del", userId = userA, name = "مقصد حذفی", type = AccountType.CARD, initialBalance = 0L, isActive = true)

        repository.addAccountResult(validSrc)
        repository.addAccountResult(validDest)
        repository.addAccountResult(inactDest)
        repository.addAccountResult(delDest)
        repository.deleteAccountResult("t_del")

        val cat = FinanceDefaultCategories.defaultExpenseCategories.first()
        val base = TransactionItemData(
            id = "tx_t_check",
            title = "انتقال",
            amount = 100_000L,
            type = TransactionType.TRANSFER,
            category = cat,
            datePersian = "امروز",
             @Test
    fun `Transfer - source account validation rejects missing, foreign, deleted and inactive accounts`() = runBlocking {
        val validSource = Account(id = "t_src", userId = userA, name = "کارت مبدأ", type = AccountType.CARD, initialBalance = 1_000_000L, isActive = true)
        val inactiveSource = Account(id = "t_inactive", userId = userA, name = "کارت غیرفعال", type = AccountType.CARD, initialBalance = 500_000L, isActive = false)
        val deletedSource = Account(id = "t_deleted", userId = userA, name = "کارت حذف‌شده", type = AccountType.CARD, initialBalance = 200_000L, isActive = true)
        repository.addAccountResult(validSource)
        repository.addAccountResult(inactiveSource)
        repository.addAccountResult(deletedSource)
        repository.deleteAccountResult(deletedSource.id)

        val base = TransactionItemData(
            id = "tx_external_validation",
            title = "انتقال وجه",
            amount = 100_000L,
            type = TransactionType.TRANSFER,
            category = FinanceDefaultCategories.transferCategory,
            datePersian = "امروز",
            timePersian = "12:00",
            accountName = validSource.name,
            transferSourceAccountId = validSource.id,
            transferDestinationAccountId = null,
            transferRecipientName = "شخص دیگر"
        )

        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "tx_missing_source", transferSourceAccountId = null)))
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "tx_blank_source", transferSourceAccountId = "")))
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "tx_fake_source", transferSourceAccountId = "not_a_local_account")))
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "tx_inactive_source", transferSourceAccountId = inactiveSource.id)))
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "tx_deleted_source", transferSourceAccountId = deletedSource.id)))
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "tx_zero_amount", amount = 0L)))
        assertEquals(TransactionOperationResult.VALIDATION_ERROR, repository.addTransactionResult(base.copy(id = "tx_negative_amount", amount = -50_000L)))

        // No destination account in the user's account list is required.
        assertEquals(TransactionOperationResult.SUCCESS, repository.addTransactionResult(base))
        assertEquals(900_000L, repository.calculateAccountBalance(validSource.id))
    }

lt("tx_inc_c")
        assertEquals(1_000_000L, repository.calculateAccountBalance("acc_b1"))

        // 2. Expense flow: add -> edit -> delete
        val txExp = TransactionItemData(id = "tx_exp_c", title = "هزینه", amount = 250_000L, type = TransactionType.EXPENSE,    @Test
    fun `Edit and Delete balance consistency across Income, Expense, and external Transfer`() = runBlocking {
        val acc1 = Account(id = "acc_b1", userId = userA, name = "کارت مبدأ", type = AccountType.CARD, initialBalance = 1_000_000L, isActive = true)
        val acc2 = Account(id = "acc_b2", userId = userA, name = "کارت دیگر", type = AccountType.CARD, initialBalance = 500_000L, isActive = true)
        repository.addAccountResult(acc1)
        repository.addAccountResult(acc2)

        val catInc = FinanceDefaultCategories.defaultIncomeCategories.first()
        val catExp = FinanceDefaultCategories.defaultExpenseCategories.first()

        val txInc = TransactionItemData(id = "tx_inc_c", title = "درآمد", amount = 400_000L, type = TransactionType.INCOME, category = catInc, datePersian = "امروز", timePersian = "12:00", accountId = "acc_b1")
        repository.addTransactionResult(txInc)
        assertEquals(1_400_000L, repository.calculateAccountBalance("acc_b1"))
        repository.updateTransactionResult(txInc.copy(amount = 600_000L))
        assertEquals(1_600_000L, repository.calculateAccountBalance("acc_b1"))
        repository.deleteTransactionResult("tx_inc_c")
        assertEquals(1_000_000L, repository.calculateAccountBalance("acc_b1"))

        val txExp = TransactionItemData(id = "tx_exp_c", title = "هزینه", amount = 250_000L, type = TransactionType.EXPENSE, category = catExp, datePersian = "امروز", timePersian = "12:00", accountId = "acc_b1")
        repository.addTransactionResult(txExp)
        assertEquals(750_000L, repository.calculateAccountBalance("acc_b1"))
        repository.updateTransactionResult(txExp.copy(amount = 350_000L))
        assertEquals(650_000L, repository.calculateAccountBalance("acc_b1"))
        repository.deleteTransactionResult("tx_exp_c")
        assertEquals(1_000_000L, repository.calculateAccountBalance("acc_b1"))

        val txTr = TransactionItemData(
            id = "tx_tr_c",
            title = "انتقال وجه به شخص دیگر",
            amount = 200_000L,
            type = TransactionType.TRANSFER,
            category = catExp,
            datePersian = "امروز",
            timePersian = "12:00",
            accountName = acc1.name,
            transferSourceAccountId = acc1.id,
            transferDestinationAccountId = null,
            transferRecipientName = "گیرنده"
        )
        repository.addTransactionResult(txTr)
        assertEquals(800_000L, repository.calculateAccountBalance("acc_b1"))
        assertEquals(500_000L, repository.calculateAccountBalance("acc_b2"))

        repository.updateTransactionResult(txTr.copy(amount = 500_000L))
        assertEquals(500_000L, repository.calculateAccountBalance("acc_b1"))
        assertEquals(500_000L, repository.calculateAccountBalance("acc_b2"))

        repository.deleteTransactionResult("tx_tr_c")
        assertEquals(1_000_000L, repository.calculateAccountBalance("acc_b1"))
        assertEquals(500_000L, repository.calculateAccountBalance("acc_b2"))
    }

oth (1,000,000 + 200,000 + 500,000 = 1,700,000)
        val balance = repository.calculateAccountBalance("acc_period")
        assertEquals(1_700_000L, balance)
    }
    @Test
    fun `Transfer - mixed Rial and Toman values produce exact account balances`() = runBlocking {
        val source = Account(
            id = "mixed_src",
            userId = userA,
            name = "مبدأ",
            type = AccountType.CARD,
            initialBalance = 1_000_000L,
            initialBalanceUnit = AppCurrency.RIAL
        )
        val destination = Account(
            id = "mixed_dest",
            userId = userA,
            name = "مقصد",
            type = AccountType.CARD,
            initialBalance = 500_000L,
            initialBalanceUnit = AppCurrency.RIAL
        )
        assertEquals(TransactionOperationResult.SUCCESS, repository.addAccountResult(source))
        assertEquals(TransactionOperationResult.SUCCESS, repository.addAccountResult(destination))

        val transfer = TransactionItemData(
            id = "mixed_transfer",
            title = "انتقال تومانی",
            amount = 30_000L,
            amountUnit = AppCurrency.TOMAN,
            type = TransactionType.TRANSFER,
            category = FinanceDefaultCategories.defaultExpenseCategories.first(),
            datePersian = "امروز",
            timePersian = "12:00",
            transferSourceAccountId = source.id,
            transferDestinationAccountId = destination.id
        )
        assertEquals(TransactionOperationResult.SUCCESS, repository.addTransactionResult(transfer))
        assertEquals(700_000L, repository.calculateAccountBalance(source.id))
        assertEquals(800_000L, repository.calculateAccountBalance(destination.id))
    }

}
    @Test
    fun `Transfer - mixed Rial and Toman values deduct from source only`() = runBlocking {
        val source = Account(
            id = "mixed_src",
            userId = userA,
            name = "کارت مبدأ",
            type = AccountType.CARD,
            initialBalance = 1_000_000L,
            initialBalanceUnit = AppCurrency.RIAL
        )
        assertEquals(TransactionOperationResult.SUCCESS, repository.addAccountResult(source))

        val transfer = TransactionItemData(
            id = "mixed_transfer",
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
        assertEquals(700_000L, repository.calculateAccountBalance(source.id))
    }

