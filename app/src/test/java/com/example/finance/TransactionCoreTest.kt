package com.example.finance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.data.TransactionOperationResult
import com.example.ui.screens.finance.domain.FinanceEngine
import com.example.ui.screens.finance.model.FinanceDefaultCategories
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionCoreTest {

    private lateinit var context: Context
    private lateinit var repository: LocalFinanceRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(
                id = "test_user_tx",
                fullName = "کاربر تست",
                email = null,
                phoneNumber = "09120000000",
                phoneVerified = true
            )
        )
        repository = LocalFinanceRepository.instance
        repository.init(context)
        val db = AppDatabase.getDatabase(context)
        db.transactionDao().clearAllTransactions("test_user_tx")
    }

    @Test
    fun `Test 1 - Add Transaction successful inserts exactly one transaction`() = runBlocking {
        val cat = FinanceDefaultCategories.defaultExpenseCategories.first()
        val tx = TransactionItemData(
            id = "tx_1",
            title = "خرید مواد غذایی",
            amount = 1_500_000L,
            type = TransactionType.EXPENSE,
            category = cat,
            datePersian = "۱۴۰۳/۰۷/۰۱",
            timePersian = "10:00"
        )
        val result = repository.addTransactionResult(tx)
        assertEquals(TransactionOperationResult.SUCCESS, result)

        val db = AppDatabase.getDatabase(context)
        val list = db.transactionDao().getAllTransactionsList("test_user_tx")
        assertEquals(1, list.size)
        assertEquals("tx_1", list[0].stringId)
    }

    @Test
    fun `Test 2 - Duplicate transaction creates new unique identity and count becomes 2`() = runBlocking {
        val cat = FinanceDefaultCategories.defaultIncomeCategories.first()
        val tx = TransactionItemData(
            id = "tx_orig",
            title = "حقوق ماهانه",
            amount = 20_000_000L,
            type = TransactionType.INCOME,
            category = cat,
            datePersian = "۱۴۰۳/۰۷/۰۱",
            timePersian = "09:00"
        )
        repository.addTransactionResult(tx)
        val duplicate = repository.duplicateTransactionResult("tx_orig")

        assertNotNull(duplicate)
        assertNotEquals("tx_orig", duplicate!!.id)

        val db = AppDatabase.getDatabase(context)
        val list = db.transactionDao().getAllTransactionsList("test_user_tx")
        assertEquals(2, list.size)
    }

    @Test
    fun `Test 3 - Update transaction modifies fields with same ID and exactly one record`() = runBlocking {
        val cat = FinanceDefaultCategories.defaultExpenseCategories.first()
        val tx = TransactionItemData(
            id = "tx_upd",
            title = "هزینه اولیه",
            amount = 500_000L,
            type = TransactionType.EXPENSE,
            category = cat,
            datePersian = "۱۴۰۳/۰۷/۰۱",
            timePersian = "11:00"
        )
        repository.addTransactionResult(tx)

        val updatedTx = tx.copy(amount = 750_000L, title = "هزینه اصلاح‌شده")
        val result = repository.updateTransactionResult(updatedTx)
        assertEquals(TransactionOperationResult.SUCCESS, result)

        val db = AppDatabase.getDatabase(context)
        val list = db.transactionDao().getAllTransactionsList("test_user_tx")
        assertEquals(1, list.size)
        assertEquals(750_000L, list[0].amount)
        assertEquals("هزینه اصلاح‌شده", list[0].title)
        assertEquals("tx_upd", list[0].stringId)
    }

    @Test
    fun `Test 4 - Delete transaction removes it from active financial calculations`() = runBlocking {
        val cat = FinanceDefaultCategories.defaultIncomeCategories.first()
        val tx = TransactionItemData(
            id = "tx_del",
            title = "پاداش",
            amount = 5_000_000L,
            type = TransactionType.INCOME,
            category = cat,
            datePersian = "۱۴۰۳/۰۷/۰۱",
            timePersian = "12:00"
        )
        repository.addTransactionResult(tx)

        val incomeBefore = FinanceEngine.calculateMonthlyIncome(listOf(tx))
        assertEquals(5_000_000L, incomeBefore)

        val result = repository.deleteTransactionResult("tx_del")
        assertEquals(TransactionOperationResult.SUCCESS, result)

        val db = AppDatabase.getDatabase(context)
        val list = db.transactionDao().getAllTransactionsList("test_user_tx")
        assertTrue(list.isEmpty())
    }

    @Test
    fun `Test 5 - Transfer transaction contributes zero to income and expense`() {
        val cat = FinanceDefaultCategories.defaultExpenseCategories.first()
        val tx = TransactionItemData(
            id = "tx_tr",
            title = "انتقال وجه",
            amount = 3_000_000L,
            type = TransactionType.TRANSFER,
            category = cat,
            datePersian = "۱۴۰۳/۰۷/۰۱",
            timePersian = "13:00"
        )
        val income = FinanceEngine.calculateMonthlyIncome(listOf(tx))
        val expense = FinanceEngine.calculateMonthlyExpense(listOf(tx))

        assertEquals(0L, income)
        assertEquals(0L, expense)
    }

    @Test
    fun `Test 6 - Income transaction included only in income`() {
        val cat = FinanceDefaultCategories.defaultIncomeCategories.first()
        val tx = TransactionItemData(
            id = "tx_inc",
            title = "فروش",
            amount = 10_000_000L,
            type = TransactionType.INCOME,
            category = cat,
            datePersian = "۱۴۰۳/۰۷/۰۱",
            timePersian = "14:00"
        )
        val income = FinanceEngine.calculateMonthlyIncome(listOf(tx))
        val expense = FinanceEngine.calculateMonthlyExpense(listOf(tx))

        assertEquals(10_000_000L, income)
        assertEquals(0L, expense)
    }

    @Test
    fun `Test 7 - Expense transaction included only in expense`() {
        val cat = FinanceDefaultCategories.defaultExpenseCategories.first()
        val tx = TransactionItemData(
            id = "tx_exp",
            title = "خرید",
            amount = 2_000_000L,
            type = TransactionType.EXPENSE,
            category = cat,
            datePersian = "۱۴۰۳/۰۷/۰۱",
            timePersian = "15:00"
        )
        val income = FinanceEngine.calculateMonthlyIncome(listOf(tx))
        val expense = FinanceEngine.calculateMonthlyExpense(listOf(tx))

        assertEquals(0L, income)
        assertEquals(2_000_000L, expense)
    }

    @Test
    fun `Test 9 - Unique Identity enforcement within user scope`() = runBlocking {
        val cat = FinanceDefaultCategories.defaultExpenseCategories.first()
        val tx = TransactionItemData(id = "tx_123", title = "تست", amount = 1000L, type = TransactionType.EXPENSE, category = cat, datePersian = "۱۴۰۳/۰۷/۰۱", timePersian = "10:00")
        
        assertEquals(TransactionOperationResult.SUCCESS, repository.addTransactionResult(tx))
        // Attempt duplicate identity for same user
        assertEquals(TransactionOperationResult.PERSISTENCE_ERROR, repository.addTransactionResult(tx))
    }

    @Test
    fun `Test 10 - User Isolation`() = runBlocking {
        val cat = FinanceDefaultCategories.defaultExpenseCategories.first()
        val txA = TransactionItemData(id = "tx_shared", title = "برای کاربر A", amount = 1000L, type = TransactionType.EXPENSE, category = cat, datePersian = "۱۴۰۳/۰۷/۰۱", timePersian = "10:00")
        
        // As User A
        assertEquals(TransactionOperationResult.SUCCESS, repository.addTransactionResult(txA))
        
        // Switch to User B
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "test_user_B", fullName = "User B", email = null, phoneNumber = "09120000001", phoneVerified = true))
        
        // User B cannot see/update/delete/duplicate User A's tx
        val db = AppDatabase.getDatabase(context)
        assertEquals(null, db.transactionDao().getTransactionByStringId("test_user_B", "tx_shared"))
        assertEquals(TransactionOperationResult.NOT_FOUND, repository.updateTransactionResult(txA))
        assertEquals(TransactionOperationResult.NOT_FOUND, repository.deleteTransactionResult("tx_shared"))
        assertEquals(null, repository.duplicateTransactionResult("tx_shared"))
    }

    @Test
    fun `Test 11 - Soft Delete Verification`() = runBlocking {
        val cat = FinanceDefaultCategories.defaultExpenseCategories.first()
        val tx = TransactionItemData(id = "tx_del_check", title = "تست حذف", amount = 1000L, type = TransactionType.EXPENSE, category = cat, datePersian = "۱۴۰۳/۰۷/۰۱", timePersian = "10:00")
        repository.addTransactionResult(tx)
        
        repository.deleteTransactionResult("tx_del_check")
        
        val db = AppDatabase.getDatabase(context)
        val entity = db.transactionDao().getTransactionIncludingDeleted("test_user_tx", "tx_del_check")
        
        assertNotNull(entity)
        assertNotNull(entity!!.deletedAt)
        assertEquals("PENDING_DELETE", entity.syncState)
    }

    @Test
    fun `Test 12 - Operation without authentication rejected`() = runBlocking {
        // Log out explicitly
        SessionManager.logout()
        
        val cat = FinanceDefaultCategories.defaultExpenseCategories.first()
        val tx = TransactionItemData(id = "tx_no_auth", title = "تست", amount = 1000L, type = TransactionType.EXPENSE, category = cat, datePersian = "۱۴۰۳/۰۷/۰۱", timePersian = "10:00")
        
        assertEquals(TransactionOperationResult.NO_AUTHENTICATED_USER, repository.addTransactionResult(tx))
    }
}
