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
import com.example.ui.screens.finance.viewmodel.FinancialViewModel
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun setUp() {
        runBlocking {
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
            val db = AppDatabase.getDatabase(context)
            db.transactionDao().clearAllTransactions("test_user_stage2")
            db.accountDao().clearAllAccounts("test_user_stage2")
            repository.addAccountResult(
                com.example.ui.screens.finance.model.Account(
                    id = "stage2_acc",
                    userId = "test_user_stage2",
                    name = "حساب مرحله ۲",
                    type = com.example.ui.screens.finance.model.AccountType.CARD,
                    initialBalance = 10_000_000L,
                    isActive = true
                )
            )
            val prefs = context.getSharedPreferences("darino_general_preferences", Context.MODE_PRIVATE)
            prefs.edit().remove("pref_persisted_budgets_test_user_stage2").commit()
            repository.refreshMetadataForCurrentUser()
        }
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

        val isInside = { timestamp: Long ->
            timestamp >= range.startMillis && timestamp < range.endMillis
        }

        // Boundary checks
        assertEquals(exactStartOfDay, range.startMillis)
        assertEquals(nextDayStart, range.endMillis)
        assertTrue(isInside(insideToday))
        assertFalse(isInside(exactJustBeforeStart))
        assertTrue(isInside(exactStartOfDay))
        assertTrue(isInside(exactJustBeforeEnd))
        assertFalse(isInside(nextDayStart))
    }

    // THIS_WEEK deterministic test verifying Saturday 00:00 -> next Saturday 00:00 business rule
    @Test
    fun `THIS_WEEK verified from Saturday 00_00 to next Saturday 00_00 with deterministic boundaries`() {
        val tz = TimeZone.getDefault()

        // 1. Create a deterministic Wednesday: Oct 2, 2024 14:30:00.000
        val wednesdayCal = Calendar.getInstance(tz).apply {
            set(Calendar.YEAR, 2024)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 2)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("Must verify fixedNow is Wednesday", Calendar.WEDNESDAY, wednesdayCal.get(Calendar.DAY_OF_WEEK))
        val fixedWednesdayNow = wednesdayCal.timeInMillis

        // 2. Expected Saturday start: Sep 28, 2024 00:00:00.000
        val satCal = Calendar.getInstance(tz).apply {
            set(Calendar.YEAR, 2024)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 28)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("Must verify satCal is Saturday", Calendar.SATURDAY, satCal.get(Calendar.DAY_OF_WEEK))
        val expectedSaturdayStart = satCal.timeInMillis

        // 3. Expected Next Saturday start: Oct 5, 2024 00:00:00.000
        val nextSatCal = Calendar.getInstance(tz).apply {
            set(Calendar.YEAR, 2024)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 5)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("Must verify nextSatCal is next Saturday", Calendar.SATURDAY, nextSatCal.get(Calendar.DAY_OF_WEEK))
        val expectedNextSaturdayStart = nextSatCal.timeInMillis

        // Execute FinanceTimeUtils
        val range = FinanceTimeUtils.getTimeRangeForPeriod(FinanceFilterPeriod.THIS_WEEK, fixedWednesdayNow)

        // Strict verification of the business rule: Saturday 00:00 -> next Saturday 00:00
        assertEquals("startMillis must equal Saturday 00:00:00.000", expectedSaturdayStart, range.startMillis)
        assertEquals("endMillis must equal next Saturday 00:00:00.000", expectedNextSaturdayStart, range.endMillis)

        // Boundary helper function for [startMillis, endMillis)
        val isInside = { timestamp: Long ->
            timestamp >= range.startMillis && timestamp < range.endMillis
        }

        // Boundary checks [startMillis, endMillis)
        // 1. Transaction exactly at startMillis -> included
        assertTrue("startMillis must be included", isInside(range.startMillis))

        // 2. Transaction at startMillis - 1 (Friday 23:59:59.999 of preceding week) -> excluded
        assertFalse("startMillis - 1 must be excluded", isInside(range.startMillis - 1))

        // 3. Transaction in middle of week (Wednesday) -> included
        assertTrue("midweek timestamp must be included", isInside(fixedWednesdayNow))

        // 4. Transaction at endMillis - 1 (Friday 23:59:59.999 of current week) -> included
        assertTrue("endMillis - 1 must be included", isInside(range.endMillis - 1))

        // 5. Transaction exactly at endMillis (Saturday 00:00:00.000 of next week) -> excluded
        assertFalse("endMillis must be excluded", isInside(range.endMillis))
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

    // LAST_3_MONTHS deterministic tests (both standard and crossing Persian year)
    @Test
    fun `LAST_3_MONTHS standard 3 calendar months boundaries`() {
        // Fixed date: Khordad 15, 1403 (Month 3 of 1403)
        // 3 calendar months: Farvardin (1), Ordibehesht (2), Khordad (3)
        val fixedKhordad = PersianCalendarHelper.jalaliToEpochMillis(1403, 3, 15, 12, 0)
        val range = FinanceTimeUtils.getTimeRangeForPeriod(FinanceFilterPeriod.LAST_3_MONTHS, fixedKhordad)

        val startOfFarvardin = PersianCalendarHelper.jalaliToEpochMillis(1403, 1, 1, 0, 0)
        val endOfKhordad = PersianCalendarHelper.jalaliToEpochMillis(1403, 4, 1, 0, 0) // Tir 1
        val insideFarvardin = PersianCalendarHelper.jalaliToEpochMillis(1403, 1, 15, 12, 0)
        val startOfEsfand1402 = PersianCalendarHelper.jalaliToEpochMillis(1402, 12, 1, 0, 0)

        assertEquals(startOfFarvardin, range.startMillis)
        assertEquals(endOfKhordad, range.endMillis)

        // 1. Start of 1st month -> included
        assertTrue("Start of Farvardin must be included", startOfFarvardin >= range.startMillis && startOfFarvardin < range.endMillis)
        // 2. Timestamp inside 1st month -> included
        assertTrue("Inside Farvardin must be included", insideFarvardin >= range.startMillis && insideFarvardin < range.endMillis)
        // 3. Month before that (Esfand 1402) -> excluded
        assertFalse("Esfand 1402 must be excluded", startOfEsfand1402 >= range.startMillis)
        // 4. Exactly at endMillis -> excluded
        assertFalse("Tir 1 (endMillis) must be excluded", endOfKhordad < range.endMillis)
    }

    @Test
    fun `LAST_3_MONTHS crossing Persian year boundary`() {
        // Fixed date: Farvardin 10, 1404 (Month 1 of 1404)
        // 3 calendar months: Bahman 1403 (11), Esfand 1403 (12), Farvardin 1404 (1)
        val fixedFarvardin = PersianCalendarHelper.jalaliToEpochMillis(1404, 1, 10, 10, 0)
        val range = FinanceTimeUtils.getTimeRangeForPeriod(FinanceFilterPeriod.LAST_3_MONTHS, fixedFarvardin)

        val startOfBahman1403 = PersianCalendarHelper.jalaliToEpochMillis(1403, 11, 1, 0, 0)
        val endOfFarvardin1404 = PersianCalendarHelper.jalaliToEpochMillis(1404, 2, 1, 0, 0) // Ordibehesht 1
        val insideBahman1403 = PersianCalendarHelper.jalaliToEpochMillis(1403, 11, 20, 14, 0)
        val startOfDey1403 = PersianCalendarHelper.jalaliToEpochMillis(1403, 10, 1, 0, 0)

        assertEquals(startOfBahman1403, range.startMillis)
        assertEquals(endOfFarvardin1404, range.endMillis)

        // 1. Start of Bahman 1403 -> included
        assertTrue("Start of Bahman 1403 must be included", startOfBahman1403 >= range.startMillis && startOfBahman1403 < range.endMillis)
        // 2. Inside Bahman 1403 -> included
        assertTrue("Inside Bahman 1403 must be included", insideBahman1403 >= range.startMillis && insideBahman1403 < range.endMillis)
        // 3. Dey 1403 (month before the 3-month window) -> excluded
        assertFalse("Dey 1403 must be excluded", startOfDey1403 >= range.startMillis)
        // 4. Exactly at endMillis -> excluded
        assertFalse("Ordibehesht 1 (endMillis) must be excluded", endOfFarvardin1404 < range.endMillis)
    }

    // 5 & 6: THIS_YEAR filtering & complete boundary checks
    @Test
    fun `5 & 6 THIS_YEAR boundary checks - startOfYear, before, endOfYear-1, endOfYear`() {
        // Date: 1403/05/10 (Mordad 10, 1403)
        val fixedNow = PersianCalendarHelper.jalaliToEpochMillis(1403, 5, 10, 12, 0)
        val range = FinanceTimeUtils.getTimeRangeForPeriod(FinanceFilterPeriod.THIS_YEAR, fixedNow)

        val startOfYear1403 = PersianCalendarHelper.jalaliToEpochMillis(1403, 1, 1, 0, 0)
        val startOfYear1404 = PersianCalendarHelper.jalaliToEpochMillis(1404, 1, 1, 0, 0)
        val endOfYear1402 = startOfYear1403 - 1
        val lastMomentOf1403 = startOfYear1404 - 1

        assertEquals(startOfYear1403, range.startMillis)
        assertEquals(startOfYear1404, range.endMillis)

        // startOfYear -> included
        assertTrue("startOfYear must be included", startOfYear1403 >= range.startMillis && startOfYear1403 < range.endMillis)
        // startOfYear - 1 -> excluded
        assertFalse("startOfYear - 1 must be excluded", endOfYear1402 >= range.startMillis)
        // endOfYear - 1 -> included
        assertTrue("endOfYear - 1 must be included", lastMomentOf1403 >= range.startMillis && lastMomentOf1403 < range.endMillis)
        // endOfYear -> excluded
        assertFalse("endOfYear must be excluded", startOfYear1404 < range.endMillis)
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
        val tx = TransactionItemData(id = "tx_to_delete", title = "To Delete", amount = 2_000_000, type = TransactionType.EXPENSE, category = cat, datePersian = "", timePersian = "", accountId = "stage2_acc")

        val addRes = repository.addTransactionResult(tx)
        assertEquals(TransactionOperationResult.SUCCESS, addRes)

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

        val tx = TransactionItemData(id = "tx_food_del", title = "Food Tx", amount = 1_200_000, type = TransactionType.EXPENSE, category = catFood, datePersian = "", timePersian = "", accountId = "stage2_acc")
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

    // 17: Genuine Reload Persistence Test (proving state is loaded from disk, not in-memory flow)
    @Test
    fun `17 True Reload Persistence Test for Budget Toggle`() = runBlocking {
        val budget = Budget(id = "toggle_persist_test", title = "Toggle Persist Test", amount = 3_000_000, isEnabled = true)
        repository.addBudget(budget)

        // 1. Toggle disabled and assert result
        val disableResult = repository.setBudgetEnabled("toggle_persist_test", false)
        assertTrue("setBudgetEnabled(false) must return true", disableResult)

        // 2. Clear in-memory state completely via session logout
        SessionManager.logout()
        repository.refreshMetadataForCurrentUser()
        val emptyState = repository.getBudgets().first()
        assertTrue("In-memory budgets must be empty after logout", emptyState.isEmpty())

        // 3. Restore session and reload metadata from disk
        SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(id = "test_user_stage2", fullName = "Stage 2 User", email = null, phoneNumber = "09120000000", phoneVerified = true)
        )
        repository.refreshMetadataForCurrentUser()

        // 4. Assert that disabled status was loaded from disk
        val reloadedDisabled = repository.getBudgets().first().find { it.id == "toggle_persist_test" }
        assertNotNull("Budget must exist in persisted disk store", reloadedDisabled)
        assertFalse("Budget isEnabled must be false after reloading from disk", reloadedDisabled!!.isEnabled)

        // 5. Toggle enabled and assert result
        val enableResult = repository.setBudgetEnabled("toggle_persist_test", true)
        assertTrue("setBudgetEnabled(true) must return true", enableResult)

        // 6. Clear in-memory state again
        SessionManager.logout()
        repository.refreshMetadataForCurrentUser()
        assertTrue(repository.getBudgets().first().isEmpty())

        // 7. Restore session and reload metadata from disk again
        SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(id = "test_user_stage2", fullName = "Stage 2 User", email = null, phoneNumber = "09120000000", phoneVerified = true)
        )
        repository.refreshMetadataForCurrentUser()

        // 8. Assert that enabled status was loaded from disk
        val reloadedEnabled = repository.getBudgets().first().find { it.id == "toggle_persist_test" }
        assertNotNull("Budget must exist in persisted disk store", reloadedEnabled)
        assertTrue("Budget isEnabled must be true after reloading from disk", reloadedEnabled!!.isEnabled)
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

    // End-to-End ViewModel Period Test
    @Test
    fun `FinancialViewModel setPeriod dynamically filters transactions and budget summary`() = runBlocking {
        val catFood = FinanceDefaultCategories.defaultExpenseCategories.find { it.id == "food" }!!
        val now = System.currentTimeMillis()
        val monthRange = FinanceTimeUtils.getTimeRangeForPeriod(FinanceFilterPeriod.THIS_MONTH, now)

        val currentMonthTx = TransactionItemData(
            id = "tx_cur_month",
            title = "Food Current Month",
            amount = 1_000_000L,
            type = TransactionType.EXPENSE,
            category = catFood,
            datePersian = "این ماه",
            timePersian = "12:00",
            accountId = "stage2_acc",
            dateMillis = monthRange.startMillis + 3600_000L // definitely inside current month
        )

        val previousMonthTx = TransactionItemData(
            id = "tx_prev_month",
            title = "Food Previous Month",
            amount = 2_000_000L,
            type = TransactionType.EXPENSE,
            category = catFood,
            datePersian = "ماه قبل",
            timePersian = "12:00",
            accountId = "stage2_acc",
            dateMillis = monthRange.startMillis - 3600_000L // definitely in previous month
        )

        val budget = Budget(
            id = "b_test_vm",
            title = "Food Budget VM",
            amount = 5_000_000L,
            categoryId = "food",
            isEnabled = true
        )

        repository.addTransactionResult(currentMonthTx)
        repository.addTransactionResult(previousMonthTx)
        repository.addBudget(budget)

        val viewModel = FinancialViewModel(repository)

        // 1. With THIS_MONTH, only current month transaction (1,000,000) should be included
        viewModel.setPeriod(FinanceFilterPeriod.THIS_MONTH)
        // Wait for flow combine to emit
        var state = viewModel.uiState.first { it.monthlyExpense > 0 }
        assertEquals(1_000_000L, state.monthlyExpense)
        assertEquals(1_000_000L, state.budgetUsed)

        // 2. With ALL, both transactions (1,000,000 + 2,000,000 = 3,000,000) should be included
        viewModel.setPeriod(FinanceFilterPeriod.ALL)
        state = viewModel.uiState.first { it.monthlyExpense == 3_000_000L }
        assertEquals(3_000_000L, state.monthlyExpense)
        assertEquals(3_000_000L, state.budgetUsed)
    }
}
