package com.example.loan.domain

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Mathematical estimates for common repayment models.
 * These calculations are not a substitute for a bank's contract or official schedule.
 */
class LoanCalculatorEngine {

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    fun validateInputs(
        amount: Long?,
        interestRate: Double?,
        duration: Int?,
        interestPeriod: InterestPeriod = InterestPeriod.ANNUAL,
        durationType: DurationType = DurationType.MONTHS
    ): ValidationResult {
        if (amount == null) return ValidationResult.Invalid("لطفاً مبلغ وام را وارد کنید")
        if (amount <= 0L || amount > MAX_PRINCIPAL) {
            return ValidationResult.Invalid("مبلغ وام باید بین ۱ تومان تا ۱٬۰۰۰٬۰۰۰٬۰۰۰٬۰۰۰٬۰۰۰ تومان باشد")
        }
        if (interestRate == null || !interestRate.isFinite()) {
            return ValidationResult.Invalid("لطفاً نرخ سود معتبر وارد کنید")
        }
        if (interestRate < 0.0) return ValidationResult.Invalid("نرخ سود نمی‌تواند منفی باشد")
        val annualRate = if (interestPeriod == InterestPeriod.MONTHLY) interestRate * 12.0 else interestRate
        if (!annualRate.isFinite() || annualRate > MAX_ANNUAL_RATE) {
            return ValidationResult.Invalid("نرخ سالانه‌شده نباید بیشتر از ۱۰۰۰ درصد باشد")
        }
        if (duration == null) return ValidationResult.Invalid("لطفاً مدت زمان بازپرداخت را مشخص کنید")
        if (duration <= 0) return ValidationResult.Invalid("مدت بازپرداخت باید بیشتر از صفر باشد")
        val months = if (durationType == DurationType.YEARS) duration.toLong() * 12L else duration.toLong()
        if (months > MAX_DURATION_MONTHS) {
            return ValidationResult.Invalid("مدت بازپرداخت نمی‌تواند بیشتر از ۶۰۰ ماه باشد")
        }
        return ValidationResult.Valid
    }

    fun calculate(
        amount: Long,
        interestRate: Double,
        interestPeriod: InterestPeriod = InterestPeriod.ANNUAL,
        duration: Int,
        durationType: DurationType = DurationType.MONTHS,
        method: CalculationMethod = CalculationMethod.ANNUITY
    ): CalculationResult {
        require(validateInputs(amount, interestRate, duration, interestPeriod, durationType) is ValidationResult.Valid) {
            "ورودی‌های محاسبه تسهیلات معتبر نیستند"
        }
        val totalMonths = if (durationType == DurationType.YEARS) duration * 12 else duration
        val annualRate = if (interestPeriod == InterestPeriod.MONTHLY) interestRate * 12.0 else interestRate
        return when (method) {
            CalculationMethod.ANNUITY -> calculateAnnuity(amount, annualRate, totalMonths)
            CalculationMethod.SIMPLE_INTEREST -> calculateSimpleInterest(amount, annualRate, totalMonths)
            CalculationMethod.CUSTOM -> calculateCustomFlatFee(amount, annualRate, totalMonths)
        }
    }

    /**
     * Equal-payment amortization estimate: monthly rate = nominal annual rate / 12.
     * The displayed installment is rounded to whole tomans; totals are derived from it
     * so total payment always reconciles with the displayed monthly installment.
     */
    private fun calculateAnnuity(principal: Long, annualRate: Double, months: Int): CalculationResult {
        val monthlyRate = annualRate / 1200.0
        val payment = if (monthlyRate == 0.0) {
            (principal.toDouble() / months).roundToLong()
        } else {
            val discountFactor = (1.0 + monthlyRate).pow(-months.toDouble())
            (principal.toDouble() * monthlyRate / (1.0 - discountFactor)).roundToLong()
        }.coerceAtLeast(1L)
        val totalPayment = Math.multiplyExact(payment, months.toLong())
        val totalInterest = (totalPayment - principal).coerceAtLeast(0L)
        return result(principal, annualRate, months, payment, totalPayment, totalInterest, CalculationMethod.ANNUITY)
    }

    /**
     * Flat/simple-interest estimate. Interest is charged on the original principal
     * for the full term; this is deliberately NOT represented as standard bank amortization.
     */
    private fun calculateSimpleInterest(principal: Long, annualRate: Double, months: Int): CalculationResult {
        val interest = (principal.toDouble() * annualRate / 1200.0 * months).roundToLong()
        val total = Math.addExact(principal, interest)
        val payment = (total.toDouble() / months).roundToLong()
        val reconciledTotal = Math.multiplyExact(payment, months.toLong())
        val reconciledInterest = (reconciledTotal - principal).coerceAtLeast(0L)
        return result(principal, annualRate, months, payment, reconciledTotal, reconciledInterest, CalculationMethod.SIMPLE_INTEREST)
    }

    /**
     * Pro-rated flat fee estimate. No artificial one-year minimum is applied:
     * fee = principal × annual fee rate × actual months / 12.
     * The applicable fee must still be taken from the specific contract/regulation.
     */
    private fun calculateCustomFlatFee(principal: Long, annualRate: Double, months: Int): CalculationResult {
        val fee = (principal.toDouble() * annualRate / 1200.0 * months).roundToLong()
        val total = Math.addExact(principal, fee)
        val payment = (total.toDouble() / months).roundToLong()
        val reconciledTotal = Math.multiplyExact(payment, months.toLong())
        val reconciledFee = (reconciledTotal - principal).coerceAtLeast(0L)
        return result(principal, annualRate, months, payment, reconciledTotal, reconciledFee, CalculationMethod.CUSTOM)
    }

    private fun result(
        principal: Long,
        annualRate: Double,
        months: Int,
        monthlyPayment: Long,
        totalPayment: Long,
        totalInterest: Long,
        method: CalculationMethod
    ) = CalculationResult(
        principalAmount = principal,
        durationMonths = months,
        annualInterestRate = annualRate,
        monthlyPayment = monthlyPayment,
        totalPayment = totalPayment,
        totalInterest = totalInterest,
        realCostPercentage = totalInterest.toDouble() / principal.toDouble() * 100.0,
        method = method
    )

    fun compareLoans(loan1Input: ComparisonLoanInput, loan2Input: ComparisonLoanInput): ComparisonResult {
        val result1 = calculate(loan1Input.amount, loan1Input.interestRate, duration = loan1Input.durationMonths, method = loan1Input.method)
        val result2 = calculate(loan2Input.amount, loan2Input.interestRate, duration = loan2Input.durationMonths, method = loan2Input.method)
        return ComparisonResult(
            loan1 = result1,
            loan2 = result2,
            monthlyPaymentDiff = result2.monthlyPayment - result1.monthlyPayment,
            totalInterestDiff = result2.totalInterest - result1.totalInterest,
            totalPaymentDiff = result2.totalPayment - result1.totalPayment
        )
    }

    companion object {
        private const val MAX_PRINCIPAL = 1_000_000_000_000_000L
        private const val MAX_ANNUAL_RATE = 1000.0
        private const val MAX_DURATION_MONTHS = 600L
    }
}
