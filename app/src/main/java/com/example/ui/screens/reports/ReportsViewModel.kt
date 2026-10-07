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
import kotlinx.coroutines.launch

data class ReportsState(
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
    val savings: Long = 0,
    val savingsPercent: Int = 0,
    val topExpenseCategory: String = ""
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ReportsState())
    val uiState: StateFlow<ReportsState> = _uiState.asStateFlow()

    private val financeRepo = LocalFinanceRepository.instance
    private val installmentRepo = LocalInstallmentRepository.instance
    private val vehicleRepo = VehicleRepository.instance

    init {
        loadReportData()
    }

    fun loadReportData() {
        viewModelScope.launch {
            val transactions = financeRepo.getTransactions().value
            val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val savings = (income - expense)
            val savingsPercent = if (income > 0) ((savings.toDouble() / income.toDouble()) * 100).toInt() else 0
            
            // Real top category
            val topCategory = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category.title }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .maxByOrNull { it.value }?.key ?: ""
            
            _uiState.value = ReportsState(
                totalIncome = income,
                totalExpense = expense,
                savings = savings,
                savingsPercent = savingsPercent,
                topExpenseCategory = topCategory
            )
        }
    }
}
