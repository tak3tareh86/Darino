package com.example.loan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanCalculatorEngineTest {
    private val engine = LoanCalculatorEngine()

    @Test
    fun flatFeeIsProratedForTermsShorterThanOneYear() {
        val result = engine.calculate(
            amount = 12_000_000L,
            interestRate = 4.0,
            duration = 6,
            method = CalculationMethod.CUSTOM
        )
        assertEquals(240_000L, result.totalInterest)
        assertEquals(result.monthlyPayment * result.durationMonths, result.totalPayment)
        assertEquals(result.principalAmount + result.totalInterest, result.totalPayment)
    }

    @Test
    fun roundedMonthlyInstallmentsReconcileWithTotals() {
        listOf(CalculationMethod.ANNUITY, CalculationMethod.SIMPLE_INTEREST, CalculationMethod.CUSTOM)
            .forEach { method ->
                val result = engine.calculate(
                    amount = 100_000_000L,
                    interestRate = 23.0,
                    duration = 37,
                    method = method
                )
                assertEquals(result.monthlyPayment * result.durationMonths, result.totalPayment)
                assertEquals(result.totalPayment - result.principalAmount, result.totalInterest)
            }
    }

    @Test
    fun invalidAndExtremeInputsAreRejected() {
        assertTrue(engine.validateInputs(0L, 20.0, 12) is LoanCalculatorEngine.ValidationResult.Invalid)
        assertTrue(engine.validateInputs(1_000_000L, Double.NaN, 12) is LoanCalculatorEngine.ValidationResult.Invalid)
        assertTrue(engine.validateInputs(1_000_000L, 20.0, 601) is LoanCalculatorEngine.ValidationResult.Invalid)
        assertTrue(engine.validateInputs(1_000_000L, 100.0, 51, durationType = DurationType.YEARS) is LoanCalculatorEngine.ValidationResult.Invalid)
    }

    @Test
    fun monthlyRateIsAnnualizedAndProducesFiniteResult() {
        val result = engine.calculate(
            amount = 10_000_000L,
            interestRate = 2.0,
            interestPeriod = InterestPeriod.MONTHLY,
            duration = 24
        )
        assertEquals(24.0, result.annualInterestRate, 0.0001)
        assertTrue(result.monthlyPayment > 0L)
        assertTrue(result.totalPayment > result.principalAmount)
    }
}
