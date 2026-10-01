package com.example.financial_health.data

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.financial_health.domain.FinancialAnalyzerEngine
import com.example.financial_health.domain.FinancialHealthProfile
import com.example.financial_health.domain.FinancialHealthStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FinancialHealthRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.financialHealthDao()

    companion object {
        const val DEFAULT_PROFILE_ID = "default_user_profile"
        const val DEFAULT_INCOME = 30_000_000L
        const val DEFAULT_INSTALLMENTS = 8_000_000L
        const val DEFAULT_FIXED_EXPENSES = 4_000_000L
    }

    val profileFlow: Flow<FinancialHealthProfile> = dao.getProfile(DEFAULT_PROFILE_ID).map { entity ->
        if (entity == null) {
            val transactions = try { db.transactionDao().getAllTransactionsList() } catch (e: Exception) { emptyList() }
            val installmentsList = try { db.installmentDao().getAllInstallmentsList() } catch (e: Exception) { emptyList() }

            val computedIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }.let { if (it > 0) it else DEFAULT_INCOME }
            val computedInstallments = installmentsList.filter { it.status == "ACTIVE" || it.status == "PENDING" || it.status == "OVERDUE" }
                .sumOf { it.amount / it.totalInstallments.coerceAtLeast(1) }.let { if (it > 0) it else DEFAULT_INSTALLMENTS }

            val commitments = computedInstallments + DEFAULT_FIXED_EXPENSES
            val pressure = FinancialAnalyzerEngine.calculateFinancialPressure(commitments, computedIncome)
            val status = FinancialAnalyzerEngine.calculateHealthStatus(pressure)
            FinancialHealthProfile(
                id = DEFAULT_PROFILE_ID,
                monthlyIncome = computedIncome,
                monthlyInstallments = computedInstallments,
                fixedExpenses = DEFAULT_FIXED_EXPENSES,
                financialPressure = pressure,
                healthStatus = status
            )
        } else {
            val status = try {
                FinancialHealthStatus.valueOf(entity.healthStatus)
            } catch (e: Exception) {
                FinancialAnalyzerEngine.calculateHealthStatus(entity.financialPressure)
            }
            FinancialHealthProfile(
                id = entity.id,
                monthlyIncome = entity.monthlyIncome,
                monthlyInstallments = entity.monthlyInstallments,
                fixedExpenses = entity.fixedExpenses,
                financialPressure = entity.financialPressure,
                healthStatus = status,
                updatedAt = entity.updatedAt
            )
        }
    }

    suspend fun saveProfile(
        monthlyIncome: Long,
        monthlyInstallments: Long,
        fixedExpenses: Long
    ) = withContext(Dispatchers.IO) {
        val totalCommitments = monthlyInstallments + fixedExpenses
        val pressure = FinancialAnalyzerEngine.calculateFinancialPressure(totalCommitments, monthlyIncome)
        val status = FinancialAnalyzerEngine.calculateHealthStatus(pressure)

        val entity = FinancialHealthEntity(
            id = DEFAULT_PROFILE_ID,
            monthlyIncome = monthlyIncome,
            monthlyInstallments = monthlyInstallments,
            fixedExpenses = fixedExpenses,
            financialPressure = pressure,
            healthStatus = status.name,
            updatedAt = System.currentTimeMillis()
        )
        dao.saveProfile(entity)
    }

    suspend fun updateIncome(newIncome: Long) = withContext(Dispatchers.IO) {
        val existing = dao.getProfileSync(DEFAULT_PROFILE_ID)
        val installments = existing?.monthlyInstallments ?: DEFAULT_INSTALLMENTS
        val fixed = existing?.fixedExpenses ?: DEFAULT_FIXED_EXPENSES
        saveProfile(newIncome, installments, fixed)
    }

    suspend fun updateFixedExpenses(newFixed: Long) = withContext(Dispatchers.IO) {
        val existing = dao.getProfileSync(DEFAULT_PROFILE_ID)
        val income = existing?.monthlyIncome ?: DEFAULT_INCOME
        val installments = existing?.monthlyInstallments ?: DEFAULT_INSTALLMENTS
        saveProfile(income, installments, newFixed)
    }
}
