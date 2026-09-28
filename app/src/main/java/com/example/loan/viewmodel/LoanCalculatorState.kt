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
    val loanType: LoanType = LoanType.BANK_LOAN,
    val amountInput: String = "100000000",
    val amount: Long = 100_000_000L,
    val interestRateInput: String = "23",
    val interestRate: Double = 23.0,
    val interestPeriod: InterestPeriod = InterestPeriod.ANNUAL,
    val durationInput: String = "36",
    val duration: Int = 36,
    val durationType: DurationType = DurationType.MONTHS,
    val calculationType: CalculationMethod = CalculationMethod.ANNUITY,
    
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
        name = "وام طرح ۱",
        amount = 100_000_000L,
        interestRate = 18.0,
        durationMonths = 18,
        method = CalculationMethod.ANNUITY
    ),
    val compLoan2Input: ComparisonLoanInput = ComparisonLoanInput(
        name = "وام طرح ۲",
        amount = 100_000_000L,
        interestRate = 23.0,
        durationMonths = 36,
        method = CalculationMethod.ANNUITY
    ),
    val comparisonResult: ComparisonResult? = null
)
