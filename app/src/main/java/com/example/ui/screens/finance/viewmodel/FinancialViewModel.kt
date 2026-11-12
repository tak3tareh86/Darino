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
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinancialViewModel(
    private val repository: FinanceRepository = LocalFinanceRepository.instance
) : ViewModel() {

    init {
        viewModelScope.launch {
            com.example.data.security.SessionManager.userId?.let { uid ->
                repository.ensureDefaultAccounts(uid)
            }
        }
    }

    private val _selectedPeriod = MutableStateFlow(FinanceFilterPeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<FinanceFilterPeriod> = _selectedPeriod

    // Independent clock signal for the current Jalali-month summary card.
    private val _monthRefreshTime = MutableStateFlow(System.currentTimeMillis())

    fun refreshMonthSummary() {
        _monthRefreshTime.value = System.currentTimeMillis()
    }

    val uiState: StateFlow<FinancialState> = combine(
        listOf(
            repository.getTransactions(),
            repository.getBudgets(),
            repository.getSavingsGoals(),
            repository.getRecurringTransactions(),
            repository.getCategories(),
            repository.getAccounts(),
            _selectedPeriod,
            _monthRefreshTime,
            MoneyFormatter.activeCurrencyFlow
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
        val monthNow = args[7] as Long
        val currency = args[8] as com.example.ui.screens.settings.model.AppCurrency

        val range = com.example.ui.screens.finance.domain.FinanceTimeUtils.getTimeRangeForPeriod(period)
        
        val filteredTransactions = transactions.filter { 
            it.dateMillis >= range.startMillis && it.dateMillis < range.endMillis 
        }

        // The summary card always represents the current Jalali month,
        // regardless of the page-level transaction filter selected by the user.
        val currentMonthRange = com.example.ui.screens.finance.domain.FinanceTimeUtils
            .getTimeRangeForPeriod(FinanceFilterPeriod.THIS_MONTH, monthNow)
        val currentMonthTransactions = transactions.filter {
            it.dateMillis >= currentMonthRange.startMillis && it.dateMillis < currentMonthRange.endMillis
        }
        val currentMonthIncome = FinanceEngine.calculateMonthlyIncome(currentMonthTransactions)
        val currentMonthExpense = FinanceEngine.calculateMonthlyExpense(currentMonthTransactions)
        val currentMonthBalance = FinanceEngine.calculateMonthlyBalance(currentMonthIncome, currentMonthExpense)
        val currentMonthSavingsRate = FinanceEngine.calculateSavingsRate(currentMonthIncome, currentMonthExpense)
        val currentMonthName = com.example.util.PersianCalendarHelper.fromEpochMillis(monthNow).monthName

        // Calculate real balances for all accounts (unrestricted by period)
        val accountBalances = accounts.associate { acc ->
            var balance = MoneyAmount.of(acc.initialBalance, acc.initialBalanceUnit).asRialsLongExact()
            transactions.forEach { tx ->
                // Note: transactions from repository are already filtered by user and not deleted
                when (tx.type) {
                    TransactionType.INCOME -> if (tx.accountId == acc.id) balance = Math.addExact(balance, MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact())
                    TransactionType.EXPENSE -> if (tx.accountId == acc.id) balance = Math.subtractExact(balance, MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact())
                    TransactionType.TRANSFER -> {
                        val transferAmountRials = MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact()
                        if (tx.transferSourceAccountId == acc.id) balance = Math.subtractExact(balance, transferAmountRials)
                        if (tx.transferDestinationAccountId == acc.id) balance = Math.addExact(balance, transferAmountRials)
                    }
                }
            }
            acc.id to balance
        }

        val totalRealBalance = accountBalances.values.fold(0L) { total, value -> Math.addExact(total, value) }

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
            formattedIncome = MoneyFormatter.formatExact(MoneyAmount.of(income, AppCurrency.RIAL), targetCurrency = currency).let { if (it.startsWith("−")) it else "+$it" },
            formattedExpense = MoneyFormatter.formatExact(MoneyAmount.of(expense, AppCurrency.RIAL), targetCurrency = currency).let { if (it.startsWith("−")) it else "−$it" },
            formattedBalance = MoneyFormatter.formatExact(MoneyAmount.of(totalRealBalance, AppCurrency.RIAL), targetCurrency = currency),
            currentMonthName = currentMonthName,
            currentMonthFormattedIncome = MoneyFormatter.formatExact(
                MoneyAmount.of(currentMonthIncome, AppCurrency.RIAL), targetCurrency = currency
            ).let { if (it.startsWith("−")) it else "+$it" },
            currentMonthFormattedExpense = MoneyFormatter.formatExact(
                MoneyAmount.of(currentMonthExpense, AppCurrency.RIAL), targetCurrency = currency
            ).let { if (it.startsWith("−")) it else "−$it" },
            currentMonthFormattedBalance = MoneyFormatter.formatExact(
                MoneyAmount.of(currentMonthBalance, AppCurrency.RIAL), targetCurrency = currency
            ).let { if (currentMonthBalance > 0L && !it.startsWith("+")) "+$it" else it },
            currentMonthSavingsRate = currentMonthSavingsRate,
            monthlyBudget = budgetSummary.totalBudget,
            budgetUsed = budgetSummary.totalSpent,
            budgetRemaining = budgetSummary.remainingBudget,
            budgetProgress = budgetSummary.usagePercentage,
            budgetStatus = budgetSummary.status,
            formattedMonthlyBudget = MoneyFormatter.formatExact(MoneyAmount.of(budgetSummary.totalBudget, AppCurrency.RIAL), targetCurrency = currency),
            formattedBudgetUsed = MoneyFormatter.formatExact(MoneyAmount.of(budgetSummary.totalSpent, AppCurrency.RIAL), targetCurrency = currency),
            formattedBudgetRemaining = MoneyFormatter.formatExact(MoneyAmount.of(budgetSummary.remainingBudget, AppCurrency.RIAL), targetCurrency = currency),
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
