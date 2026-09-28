package com.example.financial_health.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financial_health.data.FinancialHealthRepository
import com.example.financial_health.domain.FinancialAnalyzerEngine
import com.example.financial_health.domain.NextMonthPrediction
import com.example.ui.screens.installments.model.InstallmentMockDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinancialHealthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FinancialHealthRepository(application)

    // Optional override state for local UI manipulations or temporary overrides
    private val _customFixedExpenses = MutableStateFlow<Long?>(null)
    private val _customInstallments = MutableStateFlow<Long?>(null)

    // Read real installment info from InstallmentMockDataSource
    private val allInstallments = InstallmentMockDataSource.allInstallments
    private val activeInstallments = allInstallments.filter { it.remainingInstallments > 0 }
    private val activeCount = activeInstallments.size.coerceAtLeast(5)
    private val largestInstallment = activeInstallments.maxByOrNull {
        if (it.remainingInstallments > 0) it.remainingAmount / it.remainingInstallments else 0L
    }
    private val largestTitle = largestInstallment?.title ?: "وام خودرو"
    private val largestAmount = largestInstallment?.let {
        if (it.remainingInstallments > 0) it.remainingAmount / it.remainingInstallments else 3_500_000L
    } ?: 3_500_000L

    val uiState: StateFlow<FinancialHealthState> = combine(
        repository.profileFlow,
        _customFixedExpenses,
        _customInstallments
    ) { profile, customFixed, customInst ->
        val income = profile.monthlyIncome
        val installments = customInst ?: profile.monthlyInstallments
        val fixedExpenses = customFixed ?: profile.fixedExpenses
        val totalCommitments = installments + fixedExpenses

        val pressure = FinancialAnalyzerEngine.calculateFinancialPressure(totalCommitments, income)
        val healthStatus = FinancialAnalyzerEngine.calculateHealthStatus(pressure)

        val nextMonth = NextMonthPrediction(
            installments = 8_500_000L,
            reminders = 2_000_000L,
            vehicleExpenses = 1_500_000L
        )

        val insights = FinancialAnalyzerEngine.generateInsights(
            income = income,
            installments = installments,
            fixedExpenses = fixedExpenses,
            currentPressure = pressure,
            previousMonthPressure = 32.0f,
            nextMonthPrediction = nextMonth
        )

        FinancialHealthState(
            monthlyIncome = income,
            totalInstallments = installments,
            fixedExpenses = fixedExpenses,
            totalCommitments = totalCommitments,
            pressurePercentage = pressure,
            healthStatus = healthStatus,
            insights = insights,
            isLoading = false,
            errorMessage = null,
            activeInstallmentsCount = activeCount,
            nearestDueDateDays = 5,
            nearestDueDateTitle = "وام مسکن",
            largestInstallmentTitle = largestTitle,
            largestInstallmentAmount = largestAmount,
            nextMonthPrediction = nextMonth,
            previousMonthPressure = 32.0f
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialHealthState()
    )

    fun updateMonthlyIncome(newIncome: Long) {
        viewModelScope.launch {
            repository.updateIncome(newIncome)
        }
    }

    fun updateFixedExpenses(newFixed: Long) {
        viewModelScope.launch {
            repository.updateFixedExpenses(newFixed)
        }
    }

    fun saveProfile(income: Long, installments: Long, fixed: Long) {
        viewModelScope.launch {
            repository.saveProfile(income, installments, fixed)
        }
    }
}
