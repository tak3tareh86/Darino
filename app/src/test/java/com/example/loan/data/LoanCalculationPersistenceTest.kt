package com.example.loan.data

import com.example.loan.domain.CalculationMethod
import com.example.loan.domain.DurationType
import com.example.loan.domain.InterestPeriod
import com.example.loan.domain.LoanCalculation
import com.example.loan.domain.LoanType
import com.example.ui.screens.settings.model.AppCurrency
import org.junit.Assert.assertEquals
import org.junit.Test

class LoanCalculationPersistenceTest {

    @Test
    fun preservesCalculationMethodAndInterestPeriodAcrossEntityMapping() {
        listOf(CalculationMethod.ANNUITY, CalculationMethod.SIMPLE_INTEREST, CalculationMethod.CUSTOM)
            .forEach { method ->
                listOf(InterestPeriod.ANNUAL, InterestPeriod.MONTHLY).forEach { period ->
                    val original = LoanCalculation(
                        id = "saved-loan",
                        title = "وام آزمایشی",
                        loanType = LoanType.BANK_LOAN,
                        amount = 25_000_000L,
                        interestRate = 2.5,
                        duration = 18,
                        durationType = DurationType.MONTHS,
                        monthlyPayment = 1_500_000L,
                        totalPayment = 27_000_000L,
                        totalInterest = 2_000_000L,
                        realCostPercentage = 8.0,
                        createdAt = 1_800_000_000_000L,
                        amountUnit = AppCurrency.RIAL,
                        method = method,
                        interestPeriod = period
                    )

                    val restored = LoanCalculationEntity.fromDomainModel(original).toDomainModel()

                    assertEquals(method, restored.method)
                    assertEquals(period, restored.interestPeriod)
                    assertEquals(original, restored)
                }
            }
    }

    @Test
    fun legacyRowsUseExplicitFallbackDefaults() {
        val restored = LoanCalculationEntity(
            id = "legacy",
            title = "محاسبه قدیمی",
            loanType = "BANK_LOAN",
            amount = 10_000L,
            interestRate = 0.0,
            duration = 12,
            durationType = "MONTHS",
            monthlyPayment = 1_000L,
            totalPayment = 12_000L,
            totalInterest = 2_000L,
            realCostPercentage = 20.0
        ).toDomainModel()

        assertEquals(CalculationMethod.ANNUITY, restored.method)
        assertEquals(InterestPeriod.ANNUAL, restored.interestPeriod)
    }
}
