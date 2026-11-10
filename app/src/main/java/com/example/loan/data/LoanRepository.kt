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
        val ctx = context ?: run {
            inMemoryList.value = emptyList()
            return@withContext
        }
        val userId = SessionManager.userId
        if (userId.isNullOrBlank()) {
            inMemoryList.value = emptyList()
            return@withContext
        }
        try {
            val dao = AppDatabase.getDatabase(ctx).loanCalculationDao()
            dao.claimLegacyCalculations(userId)
            inMemoryList.value = dao.getAllCalculationsList(userId).map { it.toDomainModel() }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // Never leave another user's cached calculations visible after a failed account switch.
            inMemoryList.value = emptyList()
            android.util.Log.e("LoanRepository", "Unable to load saved calculations for the active user", error)
        }
    }

    suspend fun saveCalculation(calculation: LoanCalculation) = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext
        val userId = SessionManager.userId?.takeIf { it.isNotBlank() } ?: run {
            inMemoryList.value = emptyList()
            return@withContext
        }
        try {
            val dao = AppDatabase.getDatabase(ctx).loanCalculationDao()
            dao.claimLegacyCalculations(userId)
            dao.insertCalculation(LoanCalculationEntity.fromDomainModel(calculation).copy(userId = userId))
            inMemoryList.value = dao.getAllCalculationsList(userId).map { it.toDomainModel() }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e("LoanRepository", "Unable to save calculation for the active user", error)
        }
    }

    suspend fun deleteCalculation(id: String) = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext
        val userId = SessionManager.userId?.takeIf { it.isNotBlank() } ?: run {
            inMemoryList.value = emptyList()
            return@withContext
        }
        try {
            val dao = AppDatabase.getDatabase(ctx).loanCalculationDao()
            dao.deleteCalculation(userId, id)
            inMemoryList.value = dao.getAllCalculationsList(userId).map { it.toDomainModel() }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e("LoanRepository", "Unable to delete calculation for the active user", error)
        }
    }
}
