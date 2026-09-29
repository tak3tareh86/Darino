package com.example.ui.screens.finance.data

import com.example.ui.screens.finance.domain.BudgetEngine
import com.example.ui.screens.finance.domain.FinanceEngine
import com.example.ui.screens.finance.model.Budget
import com.example.ui.screens.finance.model.FinanceDefaultCategories
import com.example.ui.screens.finance.model.FinanceMockDataSource
import com.example.ui.screens.finance.model.RecurringTransaction
import com.example.ui.screens.finance.model.SavingsGoal
import com.example.ui.screens.finance.model.SavingsGoalStatus
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionItemData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class LocalFinanceRepository : FinanceRepository {

    private val _transactions = MutableStateFlow<List<TransactionItemData>>(FinanceMockDataSource.initialTransactions)
    private val _categories = MutableStateFlow<List<TransactionCategory>>(FinanceDefaultCategories.allDefaultCategories)
    private val _budgets = MutableStateFlow<List<Budget>>(FinanceMockDataSource.initialBudgets)
    private val _savingsGoals = MutableStateFlow<List<SavingsGoal>>(FinanceMockDataSource.initialSavingsGoals)
    private val _recurringTransactions = MutableStateFlow<List<RecurringTransaction>>(FinanceMockDataSource.initialRecurring)

    init {
        recalculateBudgets()
    }

    override fun getTransactions(): Flow<List<TransactionItemData>> = _transactions.asStateFlow()
    override fun getCategories(): Flow<List<TransactionCategory>> = _categories.asStateFlow()
    override fun getBudgets(): Flow<List<Budget>> = _budgets.asStateFlow()
    override fun getSavingsGoals(): Flow<List<SavingsGoal>> = _savingsGoals.asStateFlow()
    override fun getRecurringTransactions(): Flow<List<RecurringTransaction>> = _recurringTransactions.asStateFlow()

    override fun addTransaction(transaction: TransactionItemData) {
        val current = _transactions.value.toMutableList()
        current.add(0, transaction)
        _transactions.value = current
        recalculateBudgets()
    }

    override fun updateTransaction(transaction: TransactionItemData) {
        val current = _transactions.value.toMutableList()
        val index = current.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            current[index] = transaction
            _transactions.value = current
            recalculateBudgets()
        }
    }

    override fun deleteTransaction(id: String) {
        val current = _transactions.value.toMutableList()
        current.removeAll { it.id == id }
        _transactions.value = current
        recalculateBudgets()
    }

    override fun duplicateTransaction(id: String): TransactionItemData? {
        val original = _transactions.value.find { it.id == id } ?: return null
        val duplicate = original.copy(
            id = UUID.randomUUID().toString(),
            title = "${original.title} (کپی)",
            datePersian = "امروز",
            dateMillis = System.currentTimeMillis()
        )
        addTransaction(duplicate)
        return duplicate
    }

    override fun addCategory(category: TransactionCategory) {
        val current = _categories.value.toMutableList()
        current.add(category)
        _categories.value = current
    }

    override fun updateCategory(category: TransactionCategory) {
        val current = _categories.value.toMutableList()
        val index = current.indexOfFirst { it.id == category.id }
        if (index != -1) {
            current[index] = category
        } else {
            current.add(category)
        }
        _categories.value = current
    }

    override fun deleteCategory(id: String) {
        val current = _categories.value.toMutableList()
        current.removeAll { it.id == id && !it.isDefault }
        _categories.value = current
    }

    override fun toggleCategoryActive(id: String) {
        val current = _categories.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(isActive = !item.isActive)
            _categories.value = current
        }
    }

    override fun addBudget(budget: Budget) {
        val recalculated = BudgetEngine.calculateBudgetUsage(budget, _transactions.value)
        val current = _budgets.value.toMutableList()
        current.add(recalculated)
        _budgets.value = current
    }

    override fun updateBudget(budget: Budget) {
        val recalculated = BudgetEngine.calculateBudgetUsage(budget, _transactions.value)
        val current = _budgets.value.toMutableList()
        val index = current.indexOfFirst { it.id == budget.id }
        if (index != -1) {
            current[index] = recalculated
            _budgets.value = current
        }
    }

    override fun deleteBudget(id: String) {
        val current = _budgets.value.toMutableList()
        current.removeAll { it.id == id }
        _budgets.value = current
    }

    override fun addSavingsGoal(goal: SavingsGoal) {
        val progress = FinanceEngine.calculateSavingsGoalProgress(goal.currentAmount, goal.targetAmount)
        val current = _savingsGoals.value.toMutableList()
        current.add(goal.copy(progressPercentage = progress))
        _savingsGoals.value = current
    }

    override fun updateSavingsGoal(goal: SavingsGoal) {
        val progress = FinanceEngine.calculateSavingsGoalProgress(goal.currentAmount, goal.targetAmount)
        val current = _savingsGoals.value.toMutableList()
        val index = current.indexOfFirst { it.id == goal.id }
        if (index != -1) {
            current[index] = goal.copy(progressPercentage = progress)
            _savingsGoals.value = current
        }
    }

    override fun deleteSavingsGoal(id: String) {
        val current = _savingsGoals.value.toMutableList()
        current.removeAll { it.id == id }
        _savingsGoals.value = current
    }

    override fun depositToSavingsGoal(goalId: String, amount: Long) {
        val current = _savingsGoals.value.toMutableList()
        val index = current.indexOfFirst { it.id == goalId }
        if (index != -1) {
            val goal = current[index]
            val newAmount = goal.currentAmount + amount
            val progress = FinanceEngine.calculateSavingsGoalProgress(newAmount, goal.targetAmount)
            val status = if (newAmount >= goal.targetAmount) SavingsGoalStatus.COMPLETED else goal.status
            current[index] = goal.copy(
                currentAmount = newAmount,
                progressPercentage = progress,
                status = status
            )
            _savingsGoals.value = current
        }
    }

    override fun completeSavingsGoal(goalId: String) {
        val current = _savingsGoals.value.toMutableList()
        val index = current.indexOfFirst { it.id == goalId }
        if (index != -1) {
            val goal = current[index]
            current[index] = goal.copy(status = SavingsGoalStatus.COMPLETED)
            _savingsGoals.value = current
        }
    }

    override fun addRecurringTransaction(recurring: RecurringTransaction) {
        val current = _recurringTransactions.value.toMutableList()
        current.add(recurring)
        _recurringTransactions.value = current
    }

    override fun updateRecurringTransaction(recurring: RecurringTransaction) {
        val current = _recurringTransactions.value.toMutableList()
        val index = current.indexOfFirst { it.id == recurring.id }
        if (index != -1) {
            current[index] = recurring
            _recurringTransactions.value = current
        }
    }

    override fun deleteRecurringTransaction(id: String) {
        val current = _recurringTransactions.value.toMutableList()
        current.removeAll { it.id == id }
        _recurringTransactions.value = current
    }

    override fun toggleRecurringEnabled(id: String) {
        val current = _recurringTransactions.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(enabled = !item.enabled)
            _recurringTransactions.value = current
        }
    }

    override fun clearAllTransactionsData() {
        _transactions.value = emptyList()
        _recurringTransactions.value = emptyList()
        _budgets.value = emptyList()
        _savingsGoals.value = emptyList()
    }

    override fun restoreSampleTransactions() {
        _transactions.value = FinanceMockDataSource.initialTransactions
        _recurringTransactions.value = FinanceMockDataSource.initialRecurring
        _budgets.value = FinanceMockDataSource.initialBudgets
        _savingsGoals.value = FinanceMockDataSource.initialSavingsGoals
        recalculateBudgets()
    }

    private fun recalculateBudgets() {
        val txs = _transactions.value
        val updatedBudgets = _budgets.value.map { budget ->
            BudgetEngine.calculateBudgetUsage(budget, txs)
        }
        _budgets.value = updatedBudgets
    }

    companion object {
        val instance: LocalFinanceRepository by lazy { LocalFinanceRepository() }
    }
}
