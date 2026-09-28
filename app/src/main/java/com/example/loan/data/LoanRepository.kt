package com.example.loan.data

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.loan.domain.CalculationMethod
import com.example.loan.domain.DurationType
import com.example.loan.domain.LoanCalculation
import com.example.loan.domain.LoanCalculatorEngine
import com.example.loan.domain.LoanType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class LoanRepository(private val context: Context? = null) {

    private val engine = LoanCalculatorEngine()

    // Pre-seeded initial sample calculations for instant UI feedback
    private val inMemoryList = MutableStateFlow<List<LoanCalculation>>(createInitialSeedList())
    val savedCalculations: Flow<List<LoanCalculation>> = inMemoryList.asStateFlow()

    private fun createInitialSeedList(): List<LoanCalculation> {
        val calc1 = engine.calculate(
            amount = 100_000_000L,
            interestRate = 23.0,
            duration = 36,
            durationType = DurationType.MONTHS,
            method = CalculationMethod.ANNUITY
        )
        val calc2 = engine.calculate(
            amount = 50_000_000L,
            interestRate = 4.0,
            duration = 20,
            durationType = DurationType.MONTHS,
            method = CalculationMethod.CUSTOM
        )

        return listOf(
            LoanCalculation(
                id = "seed_loan_1",
                title = "تسهیلات طرح مهر بانک ملت",
                loanType = LoanType.BANK_LOAN,
                amount = 100_000_000L,
                interestRate = 23.0,
                duration = 36,
                durationType = DurationType.MONTHS,
                monthlyPayment = calc1.monthlyPayment,
                totalPayment = calc1.totalPayment,
                totalInterest = calc1.totalInterest,
                realCostPercentage = calc1.realCostPercentage,
                createdAt = System.currentTimeMillis() - 86400000L * 3
            ),
            LoanCalculation(
                id = "seed_loan_2",
                title = "وام قرض‌الحسنه خانواده",
                loanType = LoanType.PERSONAL_LOAN,
                amount = 50_000_000L,
                interestRate = 4.0,
                duration = 20,
                durationType = DurationType.MONTHS,
                monthlyPayment = calc2.monthlyPayment,
                totalPayment = calc2.totalPayment,
                totalInterest = calc2.totalInterest,
                realCostPercentage = calc2.realCostPercentage,
                createdAt = System.currentTimeMillis() - 86400000L * 7
            )
        )
    }

    suspend fun saveCalculation(calculation: LoanCalculation) = withContext(Dispatchers.IO) {
        val updated = listOf(calculation) + inMemoryList.value.filterNot { it.id == calculation.id }
        inMemoryList.value = updated

        context?.let { ctx ->
            try {
                val db = AppDatabase.getDatabase(ctx)
                db.loanCalculationDao().insertCalculation(LoanCalculationEntity.fromDomainModel(calculation))
            } catch (e: Exception) {
                // Keep safe in memory
            }
        }
    }

    suspend fun deleteCalculation(id: String) = withContext(Dispatchers.IO) {
        val updated = inMemoryList.value.filterNot { it.id == id }
        inMemoryList.value = updated

        context?.let { ctx ->
            try {
                val db = AppDatabase.getDatabase(ctx)
                db.loanCalculationDao().deleteCalculation(id)
            } catch (e: Exception) {
                // Ignored
            }
        }
    }
}
