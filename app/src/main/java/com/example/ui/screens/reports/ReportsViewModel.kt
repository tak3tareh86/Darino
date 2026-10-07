package com.example.ui.screens.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NotificationRepository
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.installments.data.LocalInstallmentRepository
import com.example.vehicle.data.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.example.util.PersianCalendarHelper

data class ReportsState(
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
    val savings: Long = 0,
    val savingsPercent: Int = 0,
    
    // Comparison
    val currentPeriodExpense: Long = 0,
    val previousPeriodExpense: Long = 0,
    val expenseChangePercent: Double = 0.0,
    val isExpenseIncreased: Boolean = false,
    
    // Category Analysis
    val topExpenseCategory: String = "نامشخص",
    val topExpenseCategoryAmount: Long = 0,
    val categoryBreakdown: Map<String, Long> = emptyMap(),
    
    // Installments
    val totalInstallmentAmount: Long = 0,
    val paidInstallmentAmount: Long = 0,
    val activeInstallmentsCount: Int = 0,
    
    // Vehicle
    val vehicleExpenseTotal: Long = 0,
    val expenseRatio: Float = 0f,
    
    // Active Filters (Canonical)
    val selectedCategory: ReportCategory = ReportCategory.FINANCIAL,
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTH,
    val customStartDate: ShamsiDate? = null,
    val customEndDate: ShamsiDate? = null,
    
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ReportsState())
    val uiState: StateFlow<ReportsState> = _uiState.asStateFlow()

    private val financeRepo = LocalFinanceRepository.instance
    private val installmentRepo = LocalInstallmentRepository.instance
    private val vehicleRepo = VehicleRepository.instance

    private val _selectedCategory = MutableStateFlow(ReportCategory.FINANCIAL)
    private val _selectedPeriod = MutableStateFlow(ReportPeriod.MONTH)
    
    private val now = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
    private val _customStartDate = MutableStateFlow(ShamsiDate(now.year, now.month, 1))
    private val _customEndDate = MutableStateFlow(ShamsiDate(now.year, now.month, PersianCalendarHelper.getDaysInMonth(now.year, now.month)))

    init {
        // Initialize repos if needed
        financeRepo.init(application)
        
        viewModelScope.launch {
            combine(
                financeRepo.getTransactions(),
                installmentRepo.getAllInstallments(),
                vehicleRepo.getAllVehicles(),
                _selectedCategory,
                _selectedPeriod,
                _customStartDate,
                _customEndDate
            ) { transactions, installments, vehicles, category, period, start, end ->
                calculateState(transactions, installments, vehicles, category, period, start, end)
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun calculateState(
        allTransactions: List<com.example.ui.screens.finance.model.TransactionItemData>,
        allInstallments: List<com.example.ui.screens.installments.data.InstallmentEntity>,
        allVehicles: List<com.example.vehicle.data.VehicleEntity>,
        category: ReportCategory,
        period: ReportPeriod,
        customStart: ShamsiDate,
        customEnd: ShamsiDate
    ): ReportsState {
        val (startTime, endTime) = getPeriodBounds(period, customStart, customEnd)
        val (prevStartTime, prevEndTime) = getPreviousPeriodBounds(period, startTime, endTime)

        // Filter transactions by time and category if needed
        val periodTransactions = allTransactions.filter { it.dateMillis in startTime..endTime }
        val filteredTransactions = if (category == ReportCategory.FINANCIAL) {
            periodTransactions
        } else {
            // If specific categories are implemented in ReportCategory, filter here.
            // For now, ReportCategory seems to be the "Tab" selector.
            periodTransactions
        }

        val income = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val savings = income - expense
        val savingsPercent = if (income > 0) ((savings.toDouble() / income.toDouble()) * 100).toInt() else 0

        // Comparison
        val prevTransactions = allTransactions.filter { it.dateMillis in prevStartTime..prevEndTime }
        val prevExpense = prevTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val expenseDiff = expense - prevExpense
        val expenseChangePercent = if (prevExpense > 0) (Math.abs(expenseDiff).toDouble() / prevExpense.toDouble()) * 100 else 0.0

        // Category Analysis
        val categoryGroups = filteredTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category.title }
            .mapValues { it.value.sumOf { it.amount } }
        
        val topCategory = categoryGroups.maxByOrNull { it.value }
        
        // Installments (Mocking logic for now using repo data)
        val activeInstallments = allInstallments.filter { it.status == "ACTIVE" }
        val installmentTotal = activeInstallments.sumOf { it.totalAmount }
        val installmentPaid = activeInstallments.sumOf { it.paidAmount }

        // Vehicle (Filter transactions with vehicle source)
        val vehicleExpenses = periodTransactions.filter { 
            it.sourceType == com.example.ui.screens.finance.model.TransactionSourceType.VEHICLE ||
            it.category.id == "vehicle" || it.category.id == "fuel"
        }.sumOf { it.amount }

        val expenseRatio = if (income > 0) {
            (expense.toDouble() / income.toDouble()).coerceIn(0.0, 1.0).toFloat()
        } else if (expense > 0) 1.0f else 0.0f

        return ReportsState(
            totalIncome = income,
            totalExpense = expense,
            savings = savings,
            savingsPercent = savingsPercent,
            currentPeriodExpense = expense,
            previousPeriodExpense = prevExpense,
            expenseChangePercent = expenseChangePercent,
            isExpenseIncreased = expenseDiff > 0,
            topExpenseCategory = topCategory?.key ?: "نامشخص",
            topExpenseCategoryAmount = topCategory?.value ?: 0,
            categoryBreakdown = categoryGroups,
            totalInstallmentAmount = installmentTotal,
            paidInstallmentAmount = installmentPaid,
            activeInstallmentsCount = activeInstallments.size,
            vehicleExpenseTotal = vehicleExpenses,
            expenseRatio = expenseRatio,
            selectedCategory = category,
            selectedPeriod = period,
            customStartDate = customStart,
            customEndDate = customEnd
        )
    }

    private fun getPeriodBounds(period: ReportPeriod, customStart: ShamsiDate, customEnd: ShamsiDate): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance()
        val nowMillis = System.currentTimeMillis()
        val currentPersian = PersianCalendarHelper.fromEpochMillis(nowMillis)
        
        return when (period) {
            ReportPeriod.WEEK -> {
                val start = nowMillis - (7 * 24 * 60 * 60 * 1000L)
                Pair(start, nowMillis)
            }
            ReportPeriod.MONTH -> {
                val start = PersianCalendarHelper.jalaliToEpochMillis(currentPersian.year, currentPersian.month, 1, 0, 0)
                Pair(start, nowMillis)
            }
            ReportPeriod.YEAR -> {
                val start = PersianCalendarHelper.jalaliToEpochMillis(currentPersian.year, 1, 1, 0, 0)
                Pair(start, nowMillis)
            }
            ReportPeriod.CUSTOM -> {
                val start = PersianCalendarHelper.jalaliToEpochMillis(customStart.year, customStart.month, customStart.day, 0, 0)
                val end = PersianCalendarHelper.jalaliToEpochMillis(customEnd.year, customEnd.month, customEnd.day, 23, 59)
                Pair(start, end)
            }
        }
    }

    private fun getPreviousPeriodBounds(period: ReportPeriod, startTime: Long, endTime: Long): Pair<Long, Long> {
        val duration = endTime - startTime
        return Pair(startTime - duration, startTime)
    }

    fun setPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun setCategory(category: ReportCategory) {
        _selectedCategory.value = category
    }

    fun setCustomRange(start: ShamsiDate, end: ShamsiDate) {
        _customStartDate.value = start
        _customEndDate.value = end
        _selectedPeriod.value = ReportPeriod.CUSTOM
    }
}
