package com.example.finance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.data.TransactionOperationResult
import com.example.ui.screens.finance.domain.BudgetEngine
import com.example.ui.screens.finance.domain.FinanceEngine
import com.example.ui.screens.finance.domain.FinanceTimeUtils
import com.example.ui.screens.finance.model.*
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinanceStage2Test {

    private lateinit var context: Context
    private lateinit var repository: LocalFinanceRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(
                id = "test_user_stage2",
                fullName = "Stage 2 User",
                email = null,
                phoneNumber = "09120000000",
                phoneVerified = true
            )
        )
        repository = LocalFinanceRepository.instance
        repository.init(context)
        repository.clearAllTransactionsData()
    }

    // 1 & 2: TODAY filtering & boundary checks
    @Test
    fun `1 & 2 TODAY transaction inside range included and yesterday excluded`() {
        val now = System.currentTimeMillis()
        val range = FinanceTimeUtils.getTimeRangeForPeriod(FinanceFilterPeriod.TODAY, now)

        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val exactStartOfDay = cal.timeInMillis
        val exactJustBeforeStart = exactStartOfDay - 1
        val insideToday = exactStartOfDay + 3600_000L
        val nextDayStart = exactStartOfDay + 24 * 3600_000L
        val exactJustBeforeEnd = nextDayStart - 1

        // Boundary checks
        assertEquals(exactStartOfDay, range.startMillis)
        assertEquals(nextDayStart, range.endMillis)
        assertTrue(insideToday >= range.startMillis && insideToday < range.endMillis)
        assertFalse(exactJustBeforeStart >= range.startMillis)
        assertTrue(exactStartOfDay >= range.startMillis && exactStartOfDay < range.endMillis)
        assertTrue(exactJustBeforeEnd >= range.startMillis && exactJustBeforeEnd < range.endMillis)
        assertFalse(nextDayStart < range.endMillis)
    }

    // 3 & 4: THIS_MONTH filtering & boundary checks
    @Test
    fun `3 & 4 THIS_MONTH transaction included and previous month excluded`() {
        // Date: 1403/07/15 (Mehr 15, 1403)
        val fixedNow = PersianCalendarHelper.jalaliToEpochMillis(1403, 7, 15, 10, 0)
        val range = FinanceTimeUtils.getTimeRangeForPeriod(FinanceFilterPeriod.THIS_MONTH, fixedNow)

        val startOfMehr = PersianCalendarHelper.jalaliToEpochMillis(1403, 7, 1, 0, 0)
        val endOfMehr = PersianCalendarHelper.jalaliToEpochMillis(1403, 8, 1, 0, 0)
        val lastMomentOfShahrivar = startOfMehr - 1

        assertEquals(startOfMehr, range.startMillis)
        assertEquals(endOfMehr, range.endMillis)

        assertTrue(startOfMehr >= range.startMillis && startOfMehr < range.endMillis)
        assertFalse(lastMomentOfShahrivar >= range.startMillis)
    }

    // 5 & 6: THIS_YEAR filtering & boundary checks
    @Test
    fun `5 & 6 THIS_YEAR transaction included and previous year excluded`() {
        // Date: 1403/05/10 (Mordad 10, 1403)
        val fixedNow = PersianCalendarHelper.jalaliToEpochMillis(1403, 5, 10, 12, 0)
        val range = FinanceTimeUtils.getTimeRangeForPeriod(FinanceFilterPeriod.THIS_YEAR, fixedNow)

        val startOfYear1403 = PersianCalendarHelper.jalaliToEpochMillis(1403, 1, 1, 0, 0)
        val startOfYear1404 = PersianCalendarHelper.jalaliToEpochMillis(1404, 1, 1, 0, 0)
        val endOfYear1402 = startOfYear1403 - 1

        assertEquals(startOfYear1403, range.startMillis)
        assertEquals(startOfYear1404, range.endMillis)

        assertTrue(fixedNow >= range.startMillis && fixedNow < range.endMillis)
        assertFalse(endOfYear1402 >= range.startMillis)
    }

    // 7: ALL period
    @Test
    fun `7 ALL includes all active transactions without timestamp restriction`() {
        val range = FinanceTimeUtils.getTimeRangeForPeriod(FinanceFilterPeriod.ALL)
        assertEquals(0L, range.startMillis)
        assertEquals(Long.MAX_VALUE, range.endMillis)
    }

    // 8: Transfer excluded from Income and Expense calculations
    @Test
    fun `8 Transfer excluded from Income and Expense calculations`() {
        val cat = FinanceDefaultCategories.defaultExpenseCategories.first()
        val transactions = listOf(
            TransactionItemData(id = "1", title = "Income 1", amount = 10_000_000, type = TransactionType.INCOME, category = cat, datePersian = "", timePersian = ""),
            TransactionItemData(id = "2", title = "Expense 1", amount = 4_000_000, type = TransactionType.EXPENSE, category = cat, datePersian = "", timePersian = ""),
            TransactionItemData(id = "3", title = "Transfer Card to Card", amount = 50_000_000, type = TransactionType.TRANSFER, category = cat, datePersian = "", timePersian = "")
        )

        val income = FinanceEngine.calculateMonthlyIncome(transactions)
        val expense = FinanceEngine.calculateMonthlyExpense(transactions)
        val net = FinanceEngine.calculateMonthlyBalance(income, expense)

        assertEquals(10_000_000L, income)
        assertEquals(4_000_000L, expense)
        assertEquals(6_000_000L, net)
    }

    // 9: Deleted transaction excluded from summary
    @Test
    fun `9 Deleted transaction excluded from summary`() = runBlocking {
        val cat = FinanceDefaultCategories.defaultExpenseCategories.first()
        val tx = TransactionItemData(id = "tx_to_delete", title = "To Delete", amount = 2_000_000, type = TransactionType.EXPENSE, category = cat, datePersian = "", timePersian = "")

        val addRes = repository.addTransactionResult(tx)
        assertEquals(TransactionOperationResult.SUCCESS, addRes)

        // Verify it was added
        val txListBefore = repository.getTransactions().first()
        assertTrue(txListBefore.any { it.id == "tx_to_delete" })

        // Soft delete
        val delRes = repository.deleteTransactionResult("tx_to_delete")
        assertEquals(TransactionOperationResult.SUCCESS, delRes)

        // Verify excluded from repository output flow
        val txListAfter = repository.getTransactions().first()
        assertFalse(txListAfter.any { it.id == "tx_to_delete" })
        assertEquals(0L, FinanceEngine.calculateMonthlyExpense(txListAfter))
    }

    // 10 & 11: Budget matching category increases usage, other category does not
    @Test
    fun `10 & 11 Expense of matching category increases usage, other category does not`() {
        val catFood = FinanceDefaultCategories.defaultExpenseCategories.find { it.id == "food" }!!
        val catTransport = FinanceDefaultCategories.defaultExpenseCategories.find { it.id == "transport" }!!
        val budgetFood = Budget(id = "b_food", title = "Food Budget", amount = 5_000_000, categoryId = "food")

        val txs = listOf(
            TransactionItemData(id = "1", title = "Restaurant", amount = 1_500_000, type = TransactionType.EXPENSE, category = catFood, datePersian = "", timePersian = ""),
            TransactionItemData(id = "2", title = "Taxi", amount = 800_000, type = TransactionType.EXPENSE, category = catTransport, datePersian = "", timePersian = "")
        )

        val updated = BudgetEngine.calculateBudgetUsage(budgetFood, txs)
        assertEquals(1_500_000L, updated.spentAmount)
        assertEquals(3_500_000L, updated.remainingAmount)
        assertEquals(30, updated.usagePercentage)
        assertEquals(BudgetStatus.SAFE, updated.status)
    }

    // 12 & 13: Income & Transfer do not increase budget usage
    @Test
    fun `12 & 13 Income and Transfer do not increase budget usage`() {
        val catFood = FinanceDefaultCategories.defaultExpenseCategories.find { it.id == "food" }!!
        val budgetFood = Budget(id = "b_food", title = "Food Budget", amount = 5_000_000, categoryId = "food")

        val txs = listOf(
            TransactionItemData(id = "1", title = "Restaurant", amount = 1_000_000, type = TransactionType.EXPENSE, category = catFood, datePersian = "", timePersian = ""),
            TransactionItemData(id = "2", title = "Food Cashback Income", amount = 500_000, type = TransactionType.INCOME, category = catFood, datePersian = "", timePersian = ""),
            TransactionItemData(id = "3", title = "Food Transfer", amount = 2_000_000, type = TransactionType.TRANSFER, category = catFood, datePersian = "", timePersian = "")
        )

        val updated = BudgetEngine.calculateBudgetUsage(budgetFood, txs)
        assertEquals(1_000_000L, updated.spentAmount)
    }

    // 14: Transaction outside period does not increase budget usage
    @Test
    fun `14 Transaction outside period does not increase budget usage`() {
        val catFood = FinanceDefaultCategories.defaultExpenseCategories.find { it.id == "food" }!!
        val fixedNow = PersianCalendarHelper.jalaliToEpochMillis(1403, 7, 15, 12, 0)
        val range = FinanceTimeUtils.getTimeRangeForPeriod(FinanceFilterPeriod.THIS_MONTH, fixedNow)

        val inMonthTx = TransactionItemData(id = "1", title = "Inside Mehr", amount = 1_000_000, type = TransactionType.EXPENSE, category = catFood, datePersian = "", timePersian = "", dateMillis = fixedNow)
        val outMonthTx = TransactionItemData(id = "2", title = "Inside Mordad", amount = 3_000_000, type = TransactionType.EXPENSE, category = catFood, datePersian = "", timePersian = "", dateMillis = PersianCalendarHelper.jalaliToEpochMillis(1403, 5, 10, 12, 0))

        val allTxs = listOf(inMonthTx, outMonthTx)
        val filtered = allTxs.filter { it.dateMillis >= range.startMillis && it.dateMillis < range.endMillis }

        val budget = Budget(id = "b_food", title = "Food Budget", amount = 5_000_000, categoryId = "food")
        val updated = BudgetEngine.calculateBudgetUsage(budget, filtered)

        assertEquals(1_000_000L, updated.spentAmount)
    }

    // 15: Deleted transaction does not increase budget usage
    @Test
    fun `15 Deleted transaction does not increase budget usage`() = runBlocking {
        val catFood = FinanceDefaultCategories.defaultExpenseCategories.find { it.id == "food" }!!
        val budget = Budget(id = "b_food", title = "Food Budget", amount = 5_000_000, categoryId = "food")
        repository.addBudget(budget)

        val tx = TransactionItemData(id = "tx_food_del", title = "Food Tx", amount = 1_200_000, type = TransactionType.EXPENSE, category = catFood, datePersian = "", timePersian = "")
        repository.addTransactionResult(tx)

        // Delete tx
        repository.deleteTransactionResult("tx_food_del")

        val txs = repository.getTransactions().first()
        val updatedBudget = BudgetEngine.calculateBudgetUsage(budget, txs)

        assertEquals(0L, updatedBudget.spentAmount)
    }

    // 16: Disabled Budget excluded from active budget summary
    @Test
    fun `16 Disabled Budget excluded from active budget summary`() {
        val catFood = FinanceDefaultCategories.defaultExpenseCategories.find { it.id == "food" }!!
        val catBill = FinanceDefaultCategories.defaultExpenseCategories.find { it.id == "bill" }!!

        val budgets = listOf(
            Budget(id = "b_food", title = "Food", amount = 4_000_000, categoryId = "food", isEnabled = true),
            Budget(id = "b_bill", title = "Bill", amount = 2_000_000, categoryId = "bill", isEnabled = false)
        )

        val txs = listOf(
            TransactionItemData(id = "1", title = "Restaurant", amount = 1_000_000, type = TransactionType.EXPENSE, category = catFood, datePersian = "", timePersian = ""),
            TransactionItemData(id = "2", title = "Electricity", amount = 500_000, type = TransactionType.EXPENSE, category = catBill, datePersian = "", timePersian = "")
        )

        val summary = BudgetEngine.calculateTotalMonthlyBudgetUsage(budgets, txs)

        assertEquals(4_000_000L, summary.totalBudget)
        assertEquals(1_000_000L, summary.totalSpent)
        assertEquals(3_000_000L, summary.remainingBudget)
        assertEquals(25, summary.usagePercentage)
        assertEquals(BudgetStatus.SAFE, summary.status)
    }

    // 17: Toggle enable disable persists after reload
    @Test
    fun `17 Toggle enable disable persists after reload`() = runBlocking {
        val budget = Budget(id = "toggle_test", title = "Toggleable", amount = 3_000_000, isEnabled = true)
        repository.addBudget(budget)

        var list = repository.getBudgets().first()
        assertTrue(list.first { it.id == "toggle_test" }.isEnabled)

        // Toggle OFF
        repository.toggleBudget("toggle_test", false)
        list = repository.getBudgets().first()
        assertFalse(list.first { it.id == "toggle_test" }.isEnabled)

        // Toggle ON
        repository.toggleBudget("toggle_test", true)
        list = repository.getBudgets().first()
        assertTrue(list.first { it.id == "toggle_test" }.isEnabled)
    }

    // 18: Budget user A not visible to user B
    @Test
    fun `18 Budget user A not visible to user B`() = runBlocking {
        val budgetUserA = Budget(id = "b_user_a", title = "User A Budget", amount = 7_000_000, isEnabled = true)
        repository.addBudget(budgetUserA)

        val listUserA = repository.getBudgets().first()
        assertTrue(listUserA.any { it.id == "b_user_a" })

        // Switch to User B
        SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(id = "user_b_test", fullName = "User B", email = null, phoneNumber = "09130000000", phoneVerified = true)
        )
        repository.refreshMetadataForCurrentUser()

        val listUserB = repository.getBudgets().first()
        assertFalse("User B must not see User A's budget", listUserB.any { it.id == "b_user_a" })
    }

    // 19: Duplicate identity for Budget does not create unwanted duplicate
    @Test
    fun `19 Duplicate identity for Budget updates existing instead of creating duplicate`() = runBlocking {
        val budget1 = Budget(id = "unique_b_id", title = "Initial Title", amount = 1_000_000)
        repository.addBudget(budget1)

        val budgetUpdated = Budget(id = "unique_b_id", title = "Updated Title", amount = 2_000_000)
        repository.updateBudget(budgetUpdated)

        val list = repository.getBudgets().first()
        val matching = list.filter { it.id == "unique_b_id" }
        assertEquals(1, matching.size)
        assertEquals("Updated Title", matching.first().title)
        assertEquals(2_000_000L, matching.first().amount)
    }
}
