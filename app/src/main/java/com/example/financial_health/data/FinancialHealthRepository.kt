package com.example.financial_health.data

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.TransactionEntity
import com.example.data.database.VehicleServiceEntity
import com.example.data.security.SessionManager
import com.example.data.security.SessionState
import com.example.financial_health.domain.FinancialAnalyzerEngine
import com.example.financial_health.domain.FinancialHealthProfile
import com.example.financial_health.domain.FinancialHealthStatus
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FinancialHealthRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.financialHealthDao()

    companion object { const val DEFAULT_PROFILE_ID = "default_user_profile" }

    private fun currentMonthRange(): Pair<Long, Long> {
        val today = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val start = PersianCalendarHelper.jalaliToEpochMillis(today.year, today.month, 1, 0, 0)
        val nextYear = if (today.month == 12) today.year + 1 else today.year
        val nextMonth = if (today.month == 12) 1 else today.month + 1
        val end = PersianCalendarHelper.jalaliToEpochMillis(nextYear, nextMonth, 1, 0, 0)
        return start to end
    }

    val transactionsFlow: Flow<List<TransactionEntity>> =
        SessionManager.sessionState.flatMapLatest { state ->
            val userId = when (state) {
                is SessionState.Authenticated -> state.user.id
                is SessionState.PhoneVerificationRequired -> state.user.id
                else -> null
            }
            if (userId == null) flowOf(emptyList())
            else db.transactionDao().getAllTransactions(userId).map { rows -> rows.filter { it.deletedAt == null } }
        }

    val vehicleServicesFlow: Flow<List<VehicleServiceEntity>> =
        SessionManager.sessionState.flatMapLatest { state ->
            val userId = when (state) {
                is SessionState.Authenticated -> state.user.id
                is SessionState.PhoneVerificationRequired -> state.user.id
                else -> null
            }
            if (userId == null) flowOf(emptyList())
            else db.vehicleDao().getAllServices(userId).map { rows -> rows.filter { it.deletedAt == null } }
        }

    val profileFlow: Flow<FinancialHealthProfile> =
        SessionManager.sessionState.flatMapLatest { state ->
            val userId = when (state) {
                is SessionState.Authenticated -> state.user.id
                is SessionState.PhoneVerificationRequired -> state.user.id
                else -> null
            }
            if (userId == null) flowOf(emptyProfile())
            else dao.getProfile(userId, DEFAULT_PROFILE_ID).combine(db.transactionDao().getAllTransactions(userId)) { entity, transactions ->
                val validProfile = entity != null &&
                    entity.monthlyIncome >= 0L && entity.monthlyInstallments >= 0L && entity.fixedExpenses >= 0L &&
                    entity.financialPressure.isFinite() && entity.financialPressure >= 0f && entity.updatedAt > 0L
                if (!validProfile) calculateProfile(userId, transactions)
                else {
                    val saved = requireNotNull(entity)
                    val status = runCatching { FinancialHealthStatus.valueOf(saved.healthStatus) }
                        .getOrElse { FinancialAnalyzerEngine.calculateHealthStatus(saved.financialPressure) }
                    FinancialHealthProfile(
                        id = saved.id,
                        monthlyIncome = saved.monthlyIncome,
                        monthlyInstallments = saved.monthlyInstallments,
                        fixedExpenses = saved.fixedExpenses,
                        financialPressure = saved.financialPressure,
                        healthStatus = status, isConfigured = true, updatedAt = saved.updatedAt
                    )
                }
            }
        }

    private fun calculateProfile(userId: String, sourceTransactions: List<TransactionEntity>): FinancialHealthProfile {
        val transactions = sourceTransactions.filter { it.deletedAt == null }
        val (start, end) = currentMonthRange()
        val income = transactions.filter {
            it.type == "INCOME" && it.timestamp >= start && it.timestamp < end && it.amount > 0L
        }.sumOf { it.amount }
        // Fixed expenses are user-entered. Never invent a value when they are unknown.
        val pressure = FinancialAnalyzerEngine.calculateFinancialPressure(0L, income) ?: 0f
        return FinancialHealthProfile(
            id = DEFAULT_PROFILE_ID, monthlyIncome = income, monthlyInstallments = 0L,
            fixedExpenses = 0L, financialPressure = pressure,
            healthStatus = FinancialAnalyzerEngine.calculateHealthStatus(pressure)
        )
    }

    private fun emptyProfile() = FinancialHealthProfile(
        id = DEFAULT_PROFILE_ID, monthlyIncome = 0L, monthlyInstallments = 0L,
        fixedExpenses = 0L, financialPressure = 0f, healthStatus = FinancialHealthStatus.MEDIUM
    )

    private fun requireUserId(): String =
        SessionManager.userId ?: throw IllegalStateException("برای ذخیره اطلاعات مالی باید وارد حساب کاربری شوید.")

    suspend fun saveProfile(monthlyIncome: Long, monthlyInstallments: Long, fixedExpenses: Long) = withContext(Dispatchers.IO) {
        require(monthlyIncome >= 0L) { "درآمد نمی‌تواند منفی باشد." }
        require(monthlyInstallments >= 0L) { "مبلغ اقساط نمی‌تواند منفی باشد." }
        require(fixedExpenses >= 0L) { "هزینه ثابت نمی‌تواند منفی باشد." }
        val pressure = FinancialAnalyzerEngine.calculateFinancialPressure(monthlyInstallments + fixedExpenses, monthlyIncome) ?: 0f
        val status = FinancialAnalyzerEngine.calculateHealthStatus(pressure)
        dao.saveProfile(FinancialHealthEntity(
            id = DEFAULT_PROFILE_ID, userId = requireUserId(), monthlyIncome = monthlyIncome,
            monthlyInstallments = monthlyInstallments, fixedExpenses = fixedExpenses,
            financialPressure = pressure, healthStatus = status.name, updatedAt = System.currentTimeMillis()
        ))
    }

    suspend fun updateIncome(newIncome: Long) = withContext(Dispatchers.IO) {
        require(newIncome >= 0L)
        val existing = dao.getProfileSync(requireUserId(), DEFAULT_PROFILE_ID)
            ?: throw IllegalStateException("ابتدا درآمد و هزینه‌های ثابت را هم‌زمان از فرم سلامت مالی ثبت کنید.")
        saveProfile(newIncome, existing.monthlyInstallments, existing.fixedExpenses)
    }

    suspend fun updateFixedExpenses(newFixed: Long) = withContext(Dispatchers.IO) {
        require(newFixed >= 0L)
        val existing = dao.getProfileSync(requireUserId(), DEFAULT_PROFILE_ID)
            ?: throw IllegalStateException("ابتدا درآمد و هزینه‌های ثابت را هم‌زمان از فرم سلامت مالی ثبت کنید.")
        saveProfile(existing.monthlyIncome, existing.monthlyInstallments, newFixed)
    }
}