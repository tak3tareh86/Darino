package com.example.ui.screens.finance.data

import com.example.ui.screens.finance.model.Budget
import com.example.ui.screens.finance.model.RecurringTransaction
import com.example.ui.screens.finance.model.SavingsGoal
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionItemData
import kotlinx.coroutines.flow.Flow

enum class TransactionOperationResult {
    SUCCESS,
    VALIDATION_ERROR,
    NOT_FOUND,
    PERSISTENCE_ERROR,
    NO_AUTHENTICATED_USER
}

interface FinanceRepository {
    fun getTransactions(): Flow<List<TransactionItemData>>
    fun getCategories(): Flow<List<TransactionCategory>>
    fun getBudgets(): Flow<List<Budget>>
    fun getSavingsGoals(): Flow<List<SavingsGoal>>
    fun getRecurringTransactions(): Flow<List<RecurringTransaction>>
    fun getAccounts(): Flow<List<com.example.ui.screens.finance.model.Account>>

    fun addTransaction(transaction: TransactionItemData)
    fun updateTransaction(transaction: TransactionItemData)
    fun deleteTransaction(id: String)
    fun duplicateTransaction(id: String): TransactionItemData?

    suspend fun addTransactionResult(transaction: TransactionItemData): TransactionOperationResult
    suspend fun updateTransactionResult(transaction: TransactionItemData): TransactionOperationResult
    suspend fun deleteTransactionResult(id: String): TransactionOperationResult
    suspend fun duplicateTransactionResult(id: String): TransactionItemData?

    suspend fun addAccountResult(account: com.example.ui.screens.finance.model.Account): TransactionOperationResult
    suspend fun updateAccountResult(account: com.example.ui.screens.finance.model.Account): TransactionOperationResult
    suspend fun deleteAccountResult(id: String): TransactionOperationResult
    suspend fun calculateAccountBalance(accountId: String): Long
    suspend fun ensureDefaultAccounts(userId: String)

    fun addCategory(category: TransactionCategory)
    fun updateCategory(category: TransactionCategory)
    fun deleteCategory(id: String)
    fun setCategoryActive(id: String, active: Boolean)

    fun addBudget(budget: Budget)
    fun updateBudget(budget: Budget)
    fun deleteBudget(id: String)
    fun setBudgetEnabled(id: String, enabled: Boolean): Boolean

    fun addSavingsGoal(goal: SavingsGoal)
    fun updateSavingsGoal(goal: SavingsGoal)
    fun deleteSavingsGoal(id: String)
    fun depositToSavingsGoal(goalId: String, amount: Long)
    fun completeSavingsGoal(goalId: String)

    fun addRecurringTransaction(recurring: RecurringTransaction)
    fun updateRecurringTransaction(recurring: RecurringTransaction)
    fun deleteRecurringTransaction(id: String)
    fun setRecurringEnabled(id: String, enabled: Boolean)

    fun clearAllTransactionsData()
    fun restoreSampleTransactions()
}
