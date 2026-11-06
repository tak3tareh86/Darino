package com.example.loan.data

import com.example.ui.screens.settings.model.AppCurrency
import org.junit.Assert.assertEquals
import org.junit.Test

class LoanCalculationCurrencyUnitTest {
    private fun entity(unit: String) = LoanCalculationEntity(
        id = "loan",
        title = "وام",
        loanType = "BANK_LOAN",
        amount = 1_234L,
        interestRate = 0.0,
        duration = 12,
        durationType = "MONTHS",
        monthlyPayment = 100L,
        totalPayment = 1_200L,
        totalInterest = 0L,
        realCostPercentage = 0.0,
        amountUnit = unit
    )

    @Test
    fun preservesRialUnit() {
        assertEquals(AppCurrency.RIAL, entity("RIAL").toDomainModel().amountUnit)
    }

    @Test
    fun rejectsUnknownUnitInsteadOfTreatingItAsToman() {
        var rejected = false
        try {
            entity("UNKNOWN").toDomainModel()
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertEquals(true, rejected)
    }
}
