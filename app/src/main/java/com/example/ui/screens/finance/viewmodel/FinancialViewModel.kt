package com.example.ui.screens.finance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.screens.finance.data.FinanceRepository
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.data.TransactionOperationResult
import com.example.ui.screens.finance.domain.BudgetEngine
import com.example.ui.screens.finance.domain.FinanceEngine
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.finance.model.Budget
import com.example.ui.screens.finance.model.FinanceFilterPeriod
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

    private val _selectedPeriod = MutableStateFlow(FinanceFilterPeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<FinanceFilterPeriod> = _selectedPeriod

    val uiState: StateFlow<FinancialState> = combine(
        listOf(
            repository.getTransactions(),
            repository.getBudgets(),
            repository.getSavingsGoals(),
            repository.getRecurringTransactions(),
            repository.getCategories(),
            repository.getAccounts(),
            _selectedPeriod
        )
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val transactions = args[0] as List<TransactionItemData>
        @Suppress("UNCHECKED_CAST")
        val budgets = args[1] as List<Budget>
        @Suppress("UNCHECKED_CAST")
        val savingsGoals = args[2] as List<SavingsGoal>
        @Suppress("UNCHECKED_CAST")
        val recurring = args[3] as List<RecurringTransaction>
        @Suppress("UNCHECKED_CAST")
        val categories = args[4] as List<TransactionCategory>
        @Suppress("UNCHECKED_CAST")
        val accounts = args[5] as List<com.example.ui.screens.finance.model.Account>
        val period = args[6] as FinanceFilterPeriod

        val range = com.example.ui.screens.finance.domain.FinanceTimeUtils.getTimeRangeForPeriod(period)
        
        val filteredTransactions = transactions.filter { 
            it.dateMillis >= range.startMillis && it.dateMillis < range.endMillis 
        }

        // Calculate real balances for all accounts (unrestricted by period)
        val accountBalances = accounts.associate { acc ->
            var balance = acc.initialBalance
            transactions.forEach { tx ->
                // Note: transactions from repository are already filtered by user and not deleted
                when (tx.type) {
                    TransactionType.INCOME -> if (tx.accountId == acc.id) balance += tx.amount
                    TransactionType.EXPENSE -> if (tx.accountId == acc.id) balance -= tx.amount
                    TransactionType.TRANSFER -> {
                        if (tx.transferSourceAccountId == acc.id) balance -= tx.amount
                        if (tx.transferDestinationAccountId == acc.id) balance += tx.amount
                    }
                }
            }
            acc.id to balance
        }

        val totalRealBalance = accountBalances.values.sum()

        val income = FinanceEngine.calculateMonthlyIncome(filteredTransactions)
        val expense = FinanceEngine.calculateMonthlyExpense(filteredTransactions)
        val balance = FinanceEngine.calculateMonthlyBalance(income, expense)
        val savingsRate = FinanceEngine.calculateSavingsRate(income, expense)

        // Recalculate each budget's usage based on the same filtered transactions
        val updatedBudgets = budgets.map { budget ->
            BudgetEngine.calculateBudgetUsage(budget, filteredTransactions)
        }

        val budgetSummary = BudgetEngine.calculateTotalMonthlyBudgetUsage(updatedBudgets, filteredTransactions)

        FinancialState(
            monthlyIncome = income,
            monthlyExpense = expense,
            monthlyBalance = balance,
            savingsRate = savingsRate,
            formattedIncome = MoneyFormatter.formatSignedToman(income, isExpense = false),
            formattedExpense = MoneyFormatter.formatSignedToman(expense, isExpense = true),
            formattedBalance = MoneyFormatter.formatToman(totalRealBalance), // Show total real balance in summary card? User said current balance shouldn't be restricted.
            monthlyBudget = budgetSummary.totalBudget,
            budgetUsed = budgetSummary.totalSpent,
            budgetRemaining = budgetSummary.remainingBudget,
            budgetProgress = budgetSummary.usagePercentage,
            budgetStatus = budgetSummary.status,
            formattedMonthlyBudget = MoneyFormatter.formatToman(budgetSummary.totalBudget),
            formattedBudgetUsed = MoneyFormatter.formatToman(budgetSummary.totalSpent),
            formattedBudgetRemaining = MoneyFormatter.formatToman(budgetSummary.remainingBudget),
            recentTransactions = filteredTransactions.take(4),
            allTransactions = filteredTransactions,
            activeBudgets = updatedBudgets,
            savingsGoals = savingsGoals,
            recurringTransactions = recurring,
            categories = categories,
            accounts = accounts,
            isLoading = false,
            error = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialState(isLoading = true)
    )

    fun setPeriod(period: FinanceFilterPeriod) {
        _selectedPeriod.value = period
    }

    fun addTransaction(transaction: TransactionItemData, onResult: (TransactionOperationResult) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.addTransactionResult(transaction)
            onResult(res)
        }
    }

    fun updateTransaction(transaction: TransactionItemData, onResult: (TransactionOperationResult) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.updateTransactionResult(transaction)
            onResult(res)
        }
    }

    fun deleteTransaction(id: String, onResult: (TransactionOperationResult) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.deleteTransactionResult(id)
            onResult(res)
        }
    }

    fun duplicateTransaction(id: String, onResult: (TransactionItemData?) -> Unit = {}) {
        viewModelScope.launch {
            val dup = repository.duplicateTransactionResult(id)
            onResult(dup)
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

    fun toggleRecurringTransaction(id: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setRecurringEnabled(id, enabled)
        }
    }

    fun toggleBudget(id: String, enabled: Boolean, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.setBudgetEnabled(id, enabled)
            onResult(success)
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

    fun toggleCategoryActive(id: String, active: Boolean) {
        viewModelScope.launch {
            repository.setCategoryActive(id, active)
        }
    }

    fun addAccount(account: com.example.ui.screens.finance.model.Account, onResult: (TransactionOperationResult) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.addAccountResult(account)
            onResult(res)
        }
    }

    fun updateAccount(account: com.example.ui.screens.finance.model.Account, onResult: (TransactionOperationResult) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.updateAccountResult(account)
            onResult(res)
        }
    }

    fun deleteAccount(id: String, onResult: (TransactionOperationResult) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.deleteAccountResult(id)
            onResult(res)
        }
    }
}
