package com.example.loan.data

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.data.security.SessionState
import com.example.loan.domain.LoanCalculation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LoanRepository(private val context: Context? = null) {

    // Room is the source of truth. The DAO Flow keeps this list synchronized with
    // backup restores and with writes performed by any other repository.
    val savedCalculations: Flow<List<LoanCalculation>> = context?.applicationContext?.let { appContext ->
        SessionManager.sessionState.flatMapLatest { state ->
            val userId = when (state) {
                is SessionState.Authenticated -> state.user.id
                is SessionState.PhoneVerificationRequired -> state.user.id
                else -> null
            }
            if (userId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                AppDatabase.getDatabase(appContext)
                    .loanCalculationDao()
                    .getAllCalculations(userId)
                    .map { rows -> rows.map { it.toDomainModel() } }
            }
        }
    } ?: flowOf(emptyList())

    suspend fun loadSavedCalculations() = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext
        val userId = SessionManager.userId?.takeIf { it.isNotBlank() } ?: return@withContext
        try {
            // Preserve the one-time ownership migration for legacy calculation rows.
            // The DAO Flow emits again when Room commits this update or a later restore.
            AppDatabase.getDatabase(ctx).loanCalculationDao().claimLegacyCalculations(userId)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e("LoanRepository", "Unable to claim legacy calculations for the active user", error)
        }
    }

    suspend fun saveCalculation(calculation: LoanCalculation): Boolean = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext false
        val userId = SessionManager.userId?.takeIf { it.isNotBlank() } ?: return@withContext false
        try {
            val dao = AppDatabase.getDatabase(ctx).loanCalculationDao()
            dao.claimLegacyCalculations(userId)
            dao.insertCalculation(LoanCalculationEntity.fromDomainModel(calculation).copy(userId = userId))
            true
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e("LoanRepository", "Unable to save calculation for the active user", error)
            false
        }
    }

    suspend fun deleteCalculation(id: String): Boolean = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext false
        val userId = SessionManager.userId?.takeIf { it.isNotBlank() } ?: return@withContext false
        try {
            val dao = AppDatabase.getDatabase(ctx).loanCalculationDao()
            dao.deleteCalculation(userId, id) > 0
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e("LoanRepository", "Unable to delete calculation for the active user", error)
            false
        }
    }
}
