package com.example.loan.data

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.loan.domain.LoanCalculation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LoanRepository(private val context: Context? = null) {

    private val inMemoryList = MutableStateFlow<List<LoanCalculation>>(emptyList())
    val savedCalculations: Flow<List<LoanCalculation>> = inMemoryList.asStateFlow()

    suspend fun loadSavedCalculations() = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext
        val userId = SessionManager.userId ?: return@withContext
        runCatching {
            val dao = AppDatabase.getDatabase(ctx).loanCalculationDao()
            dao.claimLegacyCalculations(userId)
            inMemoryList.value = dao.getAllCalculationsList(userId).map { it.toDomainModel() }
        }
    }

    suspend fun saveCalculation(calculation: LoanCalculation) = withContext(Dispatchers.IO) {
        val updated = listOf(calculation) + inMemoryList.value.filterNot { it.id == calculation.id }
        inMemoryList.value = updated

        context?.let { ctx ->
            val userId = SessionManager.userId ?: return@let
            try {
                val dao = AppDatabase.getDatabase(ctx).loanCalculationDao()
                dao.claimLegacyCalculations(userId)
                dao.insertCalculation(LoanCalculationEntity.fromDomainModel(calculation).copy(userId = userId))
            } catch (e: Exception) {
                // Keep safe in memory; a later backup will report only durable database records.
            }
        }
    }

    suspend fun deleteCalculation(id: String) = withContext(Dispatchers.IO) {
        val updated = inMemoryList.value.filterNot { it.id == id }
        inMemoryList.value = updated

        context?.let { ctx ->
            val userId = SessionManager.userId ?: return@let
            try {
                AppDatabase.getDatabase(ctx).loanCalculationDao().deleteCalculation(userId, id)
            } catch (e: Exception) {
                // Ignored
            }
        }
    }
}
