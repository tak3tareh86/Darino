package com.example.loan.domain

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Pure domain calculation engine for loans and installments.
 * Fully decoupled from Android UI.
 */
class LoanCalculatorEngine {

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    /**
     * Validates user inputs before performing calculation.
     */
    fun validateInputs(
        amount: Long?,
        interestRate: Double?,
        duration: Int?
    ): ValidationResult {
        if (amount == null) {
            return ValidationResult.Invalid("لطفاً مبلغ وام را وارد کنید")
        }
        if (amount <= 0L) {
            return ValidationResult.Invalid("مبلغ وام باید بیشتر از صفر تومان باشد")
        }
        if (interestRate == null) {
            return ValidationResult.Invalid("لطفاً نرخ سود را وارد کنید")
        }
        if (interestRate < 0.0) {
            return ValidationResult.Invalid("نرخ سود نمی‌تواند منفی باشد")
        }
        if (duration == null) {
            return ValidationResult.Invalid("لطفاً مدت زمان بازپرداخت را مشخص کنید")
        }
        if (duration <= 0) {
            return ValidationResult.Invalid("مدت بازپرداخت باید حداقل ۱ ماه باشد")
        }
        return ValidationResult.Valid
    }

    /**
     * Calculates the complete loan details.
     */
    fun calculate(
        amount: Long,
        interestRate: Double,
        interestPeriod: InterestPeriod = InterestPeriod.ANNUAL,
        duration: Int,
        durationType: DurationType = DurationType.MONTHS,
        method: CalculationMethod = CalculationMethod.ANNUITY
    ): CalculationResult {
        // Normalize duration to months
        val totalMonths = when (durationType) {
            DurationType.MONTHS -> duration.coerceAtLeast(1)
            DurationType.YEARS -> (duration * 12).coerceAtLeast(1)
        }

        // Normalize interest to annual percentage
        val annualRate = when (interestPeriod) {
            InterestPeriod.ANNUAL -> interestRate.coerceAtLeast(0.0)
            InterestPeriod.MONTHLY -> (interestRate * 12.0).coerceAtLeast(0.0)
        }

        return when (method) {
            CalculationMethod.ANNUITY -> calculateAnnuity(amount, annualRate, totalMonths)
            CalculationMethod.SIMPLE_INTEREST -> calculateSimpleInterest(amount, annualRate, totalMonths)
            CalculationMethod.CUSTOM -> calculateCustomFlatFee(amount, annualRate, totalMonths)
        }
    }

    /**
     * 1. Standard Banking Equal Monthly Installments (Annuity / French Method).
     * Formula:
     *   r = monthly interest rate = (annualRate / 100) / 12
     *   Monthly Payment M = P * [ r * (1 + r)^n ] / [ (1 + r)^n - 1 ]
     * If r == 0 (Interest free):
     *   M = P / n
     */
    private fun calculateAnnuity(
        principal: Long,
        annualRate: Double,
        months: Int
    ): CalculationResult {
        if (annualRate <= 0.00001) {
            val monthly = (principal.toDouble() / months).roundToLong()
            val totalPayment = monthly * months
            return CalculationResult(
                principalAmount = principal,
                durationMonths = months,
                annualInterestRate = annualRate,
                monthlyPayment = monthly,
                totalPayment = totalPayment,
                totalInterest = 0L,
                realCostPercentage = 0.0,
                method = CalculationMethod.ANNUITY
            )
        }

        val monthlyRate = (annualRate / 100.0) / 12.0
        val compound = (1.0 + monthlyRate).pow(months.toDouble())
        val monthlyDouble = principal.toDouble() * ((monthlyRate * compound) / (compound - 1.0))
        val monthlyPayment = monthlyDouble.roundToLong()
        val totalPayment = monthlyPayment * months
        val totalInterest = (totalPayment - principal).coerceAtLeast(0L)
        val realCostPercentage = (totalInterest.toDouble() / principal.toDouble()) * 100.0

        return CalculationResult(
            principalAmount = principal,
            durationMonths = months,
            annualInterestRate = annualRate,
            monthlyPayment = monthlyPayment,
            totalPayment = totalPayment,
            totalInterest = totalInterest,
            realCostPercentage = realCostPercentage,
            method = CalculationMethod.ANNUITY
        )
    }

    /**
     * 2. Simple Interest (سود ساده):
     *   Total Interest = Principal * (annualRate / 100) * (months / 12)
     *   Total Payment = Principal + Total Interest
     *   Monthly Payment = Total Payment / months
     */
    private fun calculateSimpleInterest(
        principal: Long,
        annualRate: Double,
        months: Int
    ): CalculationResult {
        val years = months.toDouble() / 12.0
        val totalInterestDouble = principal.toDouble() * (annualRate / 100.0) * years
        val totalInterest = totalInterestDouble.roundToLong().coerceAtLeast(0L)
        val totalPayment = principal + totalInterest
        val monthlyPayment = (totalPayment.toDouble() / months).roundToLong()
        val realCostPercentage = (totalInterest.toDouble() / principal.toDouble()) * 100.0

        return CalculationResult(
            principalAmount = principal,
            durationMonths = months,
            annualInterestRate = annualRate,
            monthlyPayment = monthlyPayment,
            totalPayment = totalPayment,
            totalInterest = totalInterest,
            realCostPercentage = realCostPercentage,
            method = CalculationMethod.SIMPLE_INTEREST
        )
    }

    /**
     * 3. Custom Flat Fee / Qarz al-Hasana (کارمزد ۴٪ سالانه):
     */
    private fun calculateCustomFlatFee(
        principal: Long,
        annualRate: Double,
        months: Int
    ): CalculationResult {
        val years = (months.toDouble() / 12.0).coerceAtLeast(1.0)
        // Flat annual commission on loan
        val totalFee = (principal.toDouble() * (annualRate / 100.0) * years).roundToLong()
        val totalPayment = principal + totalFee
        val monthlyPayment = (totalPayment.toDouble() / months).roundToLong()
        val realCostPercentage = (totalFee.toDouble() / principal.toDouble()) * 100.0

        return CalculationResult(
            principalAmount = principal,
            durationMonths = months,
            annualInterestRate = annualRate,
            monthlyPayment = monthlyPayment,
            totalPayment = totalPayment,
            totalInterest = totalFee,
            realCostPercentage = realCostPercentage,
            method = CalculationMethod.CUSTOM
        )
    }

    /**
     * Compares two distinct loan structures and computes differences.
     */
    fun compareLoans(
        loan1Input: ComparisonLoanInput,
        loan2Input: ComparisonLoanInput
    ): ComparisonResult {
        val result1 = calculate(
            amount = loan1Input.amount,
            interestRate = loan1Input.interestRate,
            interestPeriod = InterestPeriod.ANNUAL,
            duration = loan1Input.durationMonths,
            durationType = DurationType.MONTHS,
            method = loan1Input.method
        )

        val result2 = calculate(
            amount = loan2Input.amount,
            interestRate = loan2Input.interestRate,
            interestPeriod = InterestPeriod.ANNUAL,
            duration = loan2Input.durationMonths,
            durationType = DurationType.MONTHS,
            method = loan2Input.method
        )

        return ComparisonResult(
            loan1 = result1,
            loan2 = result2,
            monthlyPaymentDiff = result2.monthlyPayment - result1.monthlyPayment,
            totalInterestDiff = result2.totalInterest - result1.totalInterest,
            totalPaymentDiff = result2.totalPayment - result1.totalPayment
        )
    }
}
