package com.example.financial_health.viewmodel

import com.example.financial_health.domain.FinancialHealthInsight
import com.example.financial_health.domain.FinancialHealthStatus
import com.example.financial_health.domain.MonthlyPressureData
import com.example.financial_health.domain.NextMonthPrediction

data class FinancialHealthState(
    val monthlyIncome: Long = 0L,
    val totalInstallments: Long = 0L,
    val fixedExpenses: Long = 0L,
    val totalCommitments: Long = 0L,
    val pressurePercentage: Float = 0.0f,
    val healthStatus: FinancialHealthStatus = FinancialHealthStatus.MEDIUM,
    val hasIncomeData: Boolean = false,
    val incomeFromTransactions: Boolean = false,
    val profileUpdatedAt: Long = 0L,
    val hasFixedExpensesData: Boolean = false,
    val hasInstallmentScheduleData: Boolean = false,
    val hasPreviousMonthData: Boolean = false,
    val currentMonthComparisonPressure: Float? = null,
    val insights: List<FinancialHealthInsight> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val activeInstallmentsCount: Int = 0,
    val totalInstallmentsMonthlyFormatted: String = "اطلاعات ثبت نشده",
    val nearestDueDateDays: Int = 0,
    val nearestDueDateTitle: String = "سررسیدی ثبت نشده",
    val hasNearestDueDate: Boolean = false,
    val largestInstallmentTitle: String = "قسطی ثبت نشده",
    val largestInstallmentAmount: Long = 0L,
    val hasLargestInstallment: Boolean = false,
    val nextMonthPrediction: NextMonthPrediction = NextMonthPrediction(
        installments = 0L, fixedExpenses = 0L, reminders = 0L, vehicleExpenses = 0L,
        installmentsAmountRecorded = false, fixedExpensesAmountRecorded = false, remindersAmountRecorded = false, vehicleExpensesAmountRecorded = false
    ),
    val previousMonthPressure: Float = 0.0f,
    val historicalPressureList: List<MonthlyPressureData> = emptyList()
)