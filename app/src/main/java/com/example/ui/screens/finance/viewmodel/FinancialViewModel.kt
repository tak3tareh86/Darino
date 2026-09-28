package com.example.ui.screens.finance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.screens.finance.data.FinanceRepository
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.domain.BudgetEngine
import com.example.ui.screens.finance.domain.FinanceEngine
import com.example.ui.screens.finance.model.Budget
import com.example.ui.screens.finance.model.FinancialState
import com.example.ui.screens.finance.model.RecurringTransaction
import com.example.ui.screens.finance.model.SavingsGoal
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.util.MoneyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinancialViewModel(
    private val repository: FinanceRepository = LocalFinanceRepository.instance
) : ViewModel() {

    val uiState: StateFlow<FinancialState> = combine(
        repository.getTransactions(),
        repository.getBudgets(),
        repository.getSavingsGoals(),
        repository.getRecurringTransactions(),
        repository.getCategories()
    ) { transactions, budgets, savingsGoals, recurring, categories ->
        val income = FinanceEngine.calculateMonthlyIncome(transactions)
        val expense = FinanceEngine.calculateMonthlyExpense(transactions)
        val balance = FinanceEngine.calculateMonthlyBalance(income, expense)
        val savingsRate = FinanceEngine.calculateSavingsRate(income, expense)

        val budgetSummary = BudgetEngine.calculateTotalMonthlyBudgetUsage(budgets, transactions)

        FinancialState(
            monthlyIncome = income,
            monthlyExpense = expense,
            monthlyBalance = balance,
            savingsRate = savingsRate,
            formattedIncome = MoneyFormatter.formatSignedToman(income, isExpense = false),
            formattedExpense = MoneyFormatter.formatSignedToman(expense, isExpense = true),
            formattedBalance = MoneyFormatter.formatToman(balance),
            monthlyBudget = budgetSummary.totalBudget,
            budgetUsed = budgetSummary.totalSpent,
            budgetRemaining = budgetSummary.remainingBudget,
            budgetProgress = budgetSummary.usagePercentage,
            budgetStatus = budgetSummary.status,
            formattedMonthlyBudget = MoneyFormatter.formatToman(budgetSummary.totalBudget),
            formattedBudgetUsed = MoneyFormatter.formatToman(budgetSummary.totalSpent),
            formattedBudgetRemaining = MoneyFormatter.formatToman(budgetSummary.remainingBudget),
            recentTransactions = transactions.take(4),
            allTransactions = transactions,
            activeBudgets = budgets,
            savingsGoals = savingsGoals,
            recurringTransactions = recurring,
            categories = categories,
            isLoading = false,
            error = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialState(isLoading = true)
    )

    fun addTransaction(transaction: TransactionItemData) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: TransactionItemData) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun duplicateTransaction(id: String) {
        viewModelScope.launch {
            repository.duplicateTransaction(id)
        }
    }

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            repository.addBudget(budget)
        }
    }

    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            repository.updateBudget(budget)
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    fun addSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.addSavingsGoal(goal)
        }
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal)
        }
    }

    fun deleteSavingsGoal(id: String) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(id)
        }
    }

    fun depositToSavingsGoal(goalId: String, amount: Long) {
        viewModelScope.launch {
            repository.depositToSavingsGoal(goalId, amount)
        }
    }

    fun completeSavingsGoal(goalId: String) {
        viewModelScope.launch {
            repository.completeSavingsGoal(goalId)
        }
    }

    fun addRecurringTransaction(recurring: RecurringTransaction) {
        viewModelScope.launch {
            repository.addRecurringTransaction(recurring)
        }
    }

    fun updateRecurringTransaction(recurring: RecurringTransaction) {
        viewModelScope.launch {
            repository.updateRecurringTransaction(recurring)
        }
    }

    fun deleteRecurringTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(id)
        }
    }

    fun toggleRecurringEnabled(id: String) {
        viewModelScope.launch {
            repository.toggleRecurringEnabled(id)
        }
    }

    fun toggleRecurringTransaction(id: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleRecurringEnabled(id)
        }
    }

    fun toggleBudget(id: String, enabled: Boolean) {
        viewModelScope.launch {
            val budgets = (repository as? LocalFinanceRepository)?.let {
                // repository handles it or toggle isEnabled
            }
        }
    }

    fun saveCategory(category: TransactionCategory) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun addCategory(category: TransactionCategory) {
        viewModelScope.launch {
            repository.addCategory(category)
        }
    }

    fun updateCategory(category: TransactionCategory) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    fun toggleCategoryActive(id: String, active: Boolean = true) {
        viewModelScope.launch {
            repository.toggleCategoryActive(id)
        }
    }
}
