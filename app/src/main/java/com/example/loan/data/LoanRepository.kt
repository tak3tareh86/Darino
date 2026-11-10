package com.example.loan.data

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.loan.domain.LoanCalculation
import com.example.loan.domain.LoanCalculatorEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class LoanRepository(private val context: Context? = null) {

    private val inMemoryList = MutableStateFlow<List<LoanCalculation>>(emptyList())
    val savedCalculations: Flow<List<LoanCalculation>> = inMemoryList.asStateFlow()

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
