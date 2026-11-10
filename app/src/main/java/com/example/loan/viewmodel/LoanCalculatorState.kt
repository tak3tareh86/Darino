package com.example.loan.viewmodel

import com.example.loan.domain.CalculationMethod
import com.example.loan.domain.CalculationResult
import com.example.loan.domain.ComparisonLoanInput
import com.example.loan.domain.ComparisonResult
import com.example.loan.domain.DurationType
import com.example.loan.domain.InterestPeriod
import com.example.loan.domain.LoanCalculation
import com.example.loan.domain.LoanType

enum class LoanScreenTab(val title: String) {
    CALCULATOR("محاسبه‌گر"),
    COMPARISON("مقایسه وام‌ها"),
    SAVED("محاسبات ذخیره شده")
}

data class LoanCalculatorState(
    val currentTab: LoanScreenTab = LoanScreenTab.CALCULATOR,
    val loanType: LoanType? = null,
    val amountInput: String = "",
    val amount: Long = 0L,
    val interestRateInput: String = "",
    val interestRate: Double = 0.0,
    val interestPeriod: InterestPeriod? = null,
    val durationInput: String = "",
    val duration: Int = 0,
    val durationType: DurationType? = null,
    val calculationType: CalculationMethod? = null,
    
    // Outputs
    val monthlyPayment: Long = 0L,
    val totalPayment: Long = 0L,
    val totalInterest: Long = 0L,
    val realCostPercentage: Double = 0.0,
    val result: CalculationResult? = null,
    
    // Status
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successFeedbackMessage: String? = null,
    
    // History / Saved
    val savedCalculations: List<LoanCalculation> = emptyList(),

    // Loan comparison inputs & result
    val compLoan1Input: ComparisonLoanInput = ComparisonLoanInput(
        name = "",
        amount = 0L,
        interestRate = 0.0,
        durationMonths = 0,
        method = CalculationMethod.ANNUITY
    ),
    val compLoan2Input: ComparisonLoanInput = ComparisonLoanInput(
        name = "",
        amount = 0L,
        interestRate = 0.0,
        durationMonths = 0,
        method = CalculationMethod.ANNUITY
    ),
    val comparisonResult: ComparisonResult? = null
)
