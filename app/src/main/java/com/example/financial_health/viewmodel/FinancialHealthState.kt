package com.example.financial_health.viewmodel

import com.example.financial_health.domain.FinancialHealthInsight
import com.example.financial_health.domain.FinancialHealthStatus
import com.example.financial_health.domain.MonthlyPressureData
import com.example.financial_health.domain.NextMonthPrediction

data class FinancialHealthState(
    val monthlyIncome: Long = 30_000_000L,
    val totalInstallments: Long = 8_000_000L,
    val fixedExpenses: Long = 4_000_000L,
    val totalCommitments: Long = 12_000_000L,
    val pressurePercentage: Float = 40.0f,
    val healthStatus: FinancialHealthStatus = FinancialHealthStatus.MEDIUM,
    val insights: List<FinancialHealthInsight> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // Installment Detailed Analysis
    val activeInstallmentsCount: Int = 5,
    val totalInstallmentsMonthlyFormatted: String = "۸,۵۰۰,۰۰۰ تومان",
    val nearestDueDateDays: Int = 5,
    val nearestDueDateTitle: String = "وام مسکن",
    val largestInstallmentTitle: String = "وام خودرو",
    val largestInstallmentAmount: Long = 3_500_000L,

    // Next Month Prediction
    val nextMonthPrediction: NextMonthPrediction = NextMonthPrediction(
        installments = 8_500_000L,
        reminders = 2_000_000L,
        vehicleExpenses = 1_500_000L
    ),

    // Comparison & Historical Trends
    val previousMonthPressure: Float = 32.0f,
    val historicalPressureList: List<MonthlyPressureData> = listOf(
        MonthlyPressureData("فروردین", 30.0f, 9_000_000L, 30_000_000L),
        MonthlyPressureData("اردیبهشت", 35.0f, 10_500_000L, 30_000_000L),
        MonthlyPressureData("خرداد", 42.0f, 12_600_000L, 30_000_000L),
        MonthlyPressureData("تیر", 38.0f, 11_400_000L, 30_000_000L),
        MonthlyPressureData("مرداد", 36.0f, 10_800_000L, 30_000_000L),
        MonthlyPressureData("شهریور", 40.0f, 12_000_000L, 30_000_000L)
    )
)
