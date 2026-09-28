package com.example.ui.screens.finance.data

import com.example.ui.screens.finance.model.Budget
import com.example.ui.screens.finance.model.RecurringTransaction
import com.example.ui.screens.finance.model.SavingsGoal
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionItemData
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    fun getTransactions(): Flow<List<TransactionItemData>>
    fun getCategories(): Flow<List<TransactionCategory>>
    fun getBudgets(): Flow<List<Budget>>
    fun getSavingsGoals(): Flow<List<SavingsGoal>>
    fun getRecurringTransactions(): Flow<List<RecurringTransaction>>

    fun addTransaction(transaction: TransactionItemData)
    fun updateTransaction(transaction: TransactionItemData)
    fun deleteTransaction(id: String)
    fun duplicateTransaction(id: String): TransactionItemData?

    fun addCategory(category: TransactionCategory)
    fun updateCategory(category: TransactionCategory)
    fun deleteCategory(id: String)
    fun toggleCategoryActive(id: String)

    fun addBudget(budget: Budget)
    fun updateBudget(budget: Budget)
    fun deleteBudget(id: String)

    fun addSavingsGoal(goal: SavingsGoal)
    fun updateSavingsGoal(goal: SavingsGoal)
    fun deleteSavingsGoal(id: String)
    fun depositToSavingsGoal(goalId: String, amount: Long)
    fun completeSavingsGoal(goalId: String)

    fun addRecurringTransaction(recurring: RecurringTransaction)
    fun updateRecurringTransaction(recurring: RecurringTransaction)
    fun deleteRecurringTransaction(id: String)
    fun toggleRecurringEnabled(id: String)
}
