package com.example.financial_health.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialAnalyzerEngineTest {

    @Test
    fun pressureIsNullWhenIncomeIsNotPositive() {
        assertNull(FinancialAnalyzerEngine.calculateFinancialPressure(1_000L, 0L))
        assertNull(FinancialAnalyzerEngine.calculateFinancialPressure(1_000L, -1L))
    }

    @Test
    fun pressureKeepsOneDecimalPlace() {
        assertEquals(30.9f, FinancialAnalyzerEngine.calculateFinancialPressure(309L, 1_000L)!!, 0.001f)
    }

    @Test
    fun paymentScheduleMustExactlyMatchContractTotal() {
        assertTrue(FinancialAnalyzerEngine.isPaymentScheduleTotalConsistent(1_000L, listOf(300L, 300L, 400L)))
        assertTrue(!FinancialAnalyzerEngine.isPaymentScheduleTotalConsistent(1_000L, listOf(300L, 300L, 399L)))
        assertTrue(!FinancialAnalyzerEngine.isPaymentScheduleTotalConsistent(1_000L, listOf(300L, 300L, 401L)))
        assertTrue(!FinancialAnalyzerEngine.isPaymentScheduleTotalConsistent(1_000L, listOf(500L, 0L, 500L)))
        assertTrue(!FinancialAnalyzerEngine.isPaymentScheduleTotalConsistent(1_000L, listOf(Long.MAX_VALUE, 1L)))
    }

    @Test
    fun statusThresholdsAreConsistentAtBoundaries() {
        assertEquals(FinancialHealthStatus.SAFE, FinancialAnalyzerEngine.calculateHealthStatus(30.0f))
        assertEquals(FinancialHealthStatus.MEDIUM, FinancialAnalyzerEngine.calculateHealthStatus(30.1f))
        assertEquals(FinancialHealthStatus.MEDIUM, FinancialAnalyzerEngine.calculateHealthStatus(50.0f))
        assertEquals(FinancialHealthStatus.HIGH, FinancialAnalyzerEngine.calculateHealthStatus(50.1f))
    }

    @Test
    fun missingIncomeProducesAnExplicitInsightInsteadOfAFalseScore() {
        val insights = FinancialAnalyzerEngine.generateInsights(
            income = 0L,
            installments = 0L,
            fixedExpenses = 0L,
            currentPressure = null,
            previousMonthPressure = null,
            nextMonthPrediction = NextMonthPrediction()
        )

        assertEquals(1, insights.size)
        assertEquals("income_required", insights.single().id)
        assertTrue(insights.single().description.contains("قابل محاسبه نیست"))
    }

    @Test
    fun incompleteInstallmentScheduleIsCalledOutExplicitly() {
        val insights = FinancialAnalyzerEngine.generateInsights(
            income = 10_000L,
            installments = 2_000L,
            fixedExpenses = 1_000L,
            currentPressure = null,
            previousMonthPressure = null,
            nextMonthPrediction = NextMonthPrediction(
                installments = 0L,
                fixedExpenses = 1_000L,
                installmentsAmountRecorded = false,
                fixedExpensesAmountRecorded = true
            ),
            installmentDataComplete = false
        )

        assertTrue(insights.any { it.id == "installment_schedule_incomplete" })
        assertTrue(insights.any { it.id == "trend_unavailable" })
    }
}
