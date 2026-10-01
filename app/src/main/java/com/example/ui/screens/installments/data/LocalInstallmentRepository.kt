package com.example.ui.screens.installments.data

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.InstallmentEntity
import com.example.data.database.InstallmentPaymentEntity
import com.example.data.security.SessionManager
import com.example.data.security.SessionState
import com.example.ui.screens.installments.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * Canonical Room-backed repository for the Installments domain.
 * Manages installments, payment schedules, and reactive state flows.
 */
class LocalInstallmentRepository private constructor() {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _installments = MutableStateFlow<List<InstallmentItem>>(emptyList())
    val installments: StateFlow<List<InstallmentItem>> = _installments.asStateFlow()

    private val _bankLoans = MutableStateFlow<List<InstallmentItem>>(emptyList())
    val bankLoans: StateFlow<List<InstallmentItem>> = _bankLoans.asStateFlow()

    private val _homeLoans = MutableStateFlow<List<InstallmentItem>>(emptyList())
    val homeLoans: StateFlow<List<InstallmentItem>> = _homeLoans.asStateFlow()

    private val _carInsurance = MutableStateFlow<List<InstallmentItem>>(emptyList())
    val carInsurance: StateFlow<List<InstallmentItem>> = _carInsurance.asStateFlow()

    private val _miscInstallments = MutableStateFlow<List<InstallmentItem>>(emptyList())
    val miscInstallments: StateFlow<List<InstallmentItem>> = _miscInstallments.asStateFlow()

    private val _overdueInstallments = MutableStateFlow<List<InstallmentItem>>(emptyList())
    val overdueInstallments: StateFlow<List<InstallmentItem>> = _overdueInstallments.asStateFlow()

    private val _summary = MutableStateFlow(InstallmentSummaryData())
    val summary: StateFlow<InstallmentSummaryData> = _summary.asStateFlow()

    private val _categorySummaries = MutableStateFlow<List<CategorySummaryStat>>(emptyList())
    val categorySummaries: StateFlow<List<CategorySummaryStat>> = _categorySummaries.asStateFlow()

    private var appContext: Context? = null
    private var initialized = false
    private var sessionObserverStarted = false

    init {
        // Room is the sole source of truth. Production state must never be seeded from mock data.
        updateInternalState(emptyList())
    }

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val appCtx = context.applicationContext
        appContext = appCtx

        if (!sessionObserverStarted) {
            sessionObserverStarted = true
            repositoryScope.launch {
                SessionManager.sessionState.collectLatest { state ->
                    val userId = when (state) {
                        is SessionState.Authenticated -> state.user.id
                        is SessionState.PhoneVerificationRequired -> state.user.id
                        else -> null
                    }
                    if (userId == null) {
                        updateInternalState(emptyList())
                        return@collectLatest
                    }
                    reloadForUser(appCtx, userId)
                }
            }
        }
    }

    private suspend fun reloadForUser(appCtx: Context, userId: String) {
        try {
            val db = AppDatabase.getDatabase(appCtx)
            val dao = db.installmentDao()
            val dbInsts = dao.getAllInstallmentsList(userId)
            val dbPayments = dao.getAllPaymentsList(userId)

            if (dbInsts.isNotEmpty()) {
                val loaded = dbInsts.map { entity ->
                        val payments = dbPayments.filter { it.installmentId == entity.id }.map { p ->
                            PaymentHistoryItem(
                                id = p.paymentReference ?: p.id.toString(),
                                installmentNumber = 1,
                                dueDate = "۱۴۰۴/۰۷/۱۵",
                                paidDate = if (p.paidDate != null) "۱۴۰۴/۰۷/۱۵" else null,
                                amountFormatted = "${p.amount} تومان",
                                status = try { InstallmentStatus.valueOf(p.status) } catch (e: Exception) { InstallmentStatus.PENDING },
                                note = p.notes,
                                amount = p.amount
                            )
                        }
                        val cat = try { InstallmentCategory.valueOf(entity.category) } catch (e: Exception) { InstallmentCategory.BANK_LOANS }
                        val status = try { InstallmentStatus.valueOf(entity.status) } catch (e: Exception) { InstallmentStatus.PENDING }
                        val paidAmount = entity.amount - (entity.amount * entity.remainingInstallments / entity.totalInstallments.coerceAtLeast(1))
                        val monthly = entity.amount / entity.totalInstallments.coerceAtLeast(1)

                        InstallmentItem(
                            id = entity.serverId ?: entity.id.toString(),
                            title = entity.title,
                            category = cat,
                            providerOrPerson = entity.providerName,
                            totalAmount = entity.amount,
                            totalAmountFormatted = "${entity.amount} تومان",
                            paidAmount = paidAmount,
                            paidAmountFormatted = "$paidAmount تومان",
                            remainingAmount = entity.amount - paidAmount,
                            remainingAmountFormatted = "${entity.amount - paidAmount} تومان",
                            monthlyPaymentFormatted = "$monthly تومان",
                            totalInstallments = entity.totalInstallments,
                            remainingInstallments = entity.remainingInstallments,
                            nextPaymentDate = "۱۴۰۴/۰۷/۱۵",
                            nextDueDaysText = "۱۰ روز دیگر",
                            startDate = "۱۴۰۳/۰۱/۰۱",
                            endDate = "۱۴۰۵/۰۱/۰۱",
                            status = status,
                            notes = entity.notes ?: "",
                            paymentHistory = payments
                        )
                    }
                    updateInternalState(loaded)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reloadFromDatabase(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        repositoryScope.launch {
            try {
                val db = AppDatabase.getDatabase(appCtx)
                val dao = db.installmentDao()
                val userId = SessionManager.userId ?: return@launch
                val dbInsts = dao.getAllInstallmentsList(userId)
                val dbPayments = dao.getAllPaymentsList(userId)

                val loaded = dbInsts.map { entity ->
                    val payments = dbPayments.filter { it.installmentId == entity.id }.map { p ->
                        PaymentHistoryItem(
                            id = p.paymentReference ?: p.id.toString(),
                            installmentNumber = 1,
                            dueDate = "۱۴۰۴/۰۷/۱۵",
                            paidDate = if (p.paidDate != null) "۱۴۰۴/۰۷/۱۵" else null,
                            amountFormatted = "${p.amount} تومان",
                            status = try { InstallmentStatus.valueOf(p.status) } catch (e: Exception) { InstallmentStatus.PENDING },
                            note = p.notes,
                            amount = p.amount
                        )
                    }
                    val cat = try { InstallmentCategory.valueOf(entity.category) } catch (e: Exception) { InstallmentCategory.BANK_LOANS }
                    val status = try { InstallmentStatus.valueOf(entity.status) } catch (e: Exception) { InstallmentStatus.PENDING }
                    val paidAmount = entity.amount - (entity.amount * entity.remainingInstallments / entity.totalInstallments.coerceAtLeast(1))
                    val monthly = entity.amount / entity.totalInstallments.coerceAtLeast(1)

                    InstallmentItem(
                        id = entity.serverId ?: entity.id.toString(),
                        title = entity.title,
                        category = cat,
                        providerOrPerson = entity.providerName,
                        totalAmount = entity.amount,
                        totalAmountFormatted = "${entity.amount} تومان",
                        paidAmount = paidAmount,
                        paidAmountFormatted = "$paidAmount تومان",
                        remainingAmount = entity.amount - paidAmount,
                        remainingAmountFormatted = "${entity.amount - paidAmount} تومان",
                        monthlyPaymentFormatted = "$monthly تومان",
                        totalInstallments = entity.totalInstallments,
                        remainingInstallments = entity.remainingInstallments,
                        nextPaymentDate = "۱۴۰۴/۰۷/۱۵",
                        nextDueDaysText = "۱۰ روز دیگر",
                        startDate = "۱۴۰۳/۰۱/۰۱",
                        endDate = "۱۴۰۵/۰۱/۰۱",
                        status = status,
                        notes = entity.notes ?: "",
                        paymentHistory = payments
                    )
                }
                updateInternalState(loaded)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateInternalState(items: List<InstallmentItem>) {
        _installments.value = items
        _bankLoans.value = items.filter { it.category == InstallmentCategory.BANK_LOANS }
        _homeLoans.value = items.filter { it.category == InstallmentCategory.HOME_LOANS }
        _carInsurance.value = items.filter { it.category == InstallmentCategory.CAR_INSURANCE }
        _miscInstallments.value = items.filter { it.category == InstallmentCategory.MISC }
        _overdueInstallments.value = items.filter { it.status == InstallmentStatus.OVERDUE }
        _summary.value = InstallmentSummaryData(
            activeCount = items.size,
            paidAmount = items.sumOf { it.paidAmount },
            remainingAmount = items.sumOf { it.remainingAmount }
        )
        _categorySummaries.value = listOf(
            CategorySummaryStat(InstallmentCategory.BANK_LOANS, "${_bankLoans.value.size} مورد", "${_bankLoans.value.sumOf { it.remainingAmount }} تومان"),
            CategorySummaryStat(InstallmentCategory.HOME_LOANS, "${_homeLoans.value.size} مورد", "${_homeLoans.value.sumOf { it.remainingAmount }} تومان"),
            CategorySummaryStat(InstallmentCategory.CAR_INSURANCE, "${_carInsurance.value.size} مورد", "${_carInsurance.value.sumOf { it.remainingAmount }} تومان"),
            CategorySummaryStat(InstallmentCategory.MISC, "${_miscInstallments.value.size} مورد", "${_miscInstallments.value.sumOf { it.remainingAmount }} تومان")
        )
    }

    fun addInstallment(item: InstallmentItem, context: Context? = null) {
        val current = _installments.value.toMutableList()
        current.add(0, item)
        updateInternalState(current)

        val targetContext = context?.applicationContext ?: appContext ?: return
        repositoryScope.launch {
            saveToRoom(targetContext, current)
        }
    }

    fun updateInstallment(updated: InstallmentItem, context: Context? = null) {
        val current = _installments.value.map { if (it.id == updated.id) updated else it }
        updateInternalState(current)

        val targetContext = context?.applicationContext ?: appContext ?: return
        repositoryScope.launch {
            saveToRoom(targetContext, current)
        }
    }

    fun deleteInstallment(id: String, context: Context? = null) {
        val current = _installments.value.filter { it.id != id }
        updateInternalState(current)

        val targetContext = context?.applicationContext ?: appContext ?: return
        repositoryScope.launch {
            saveToRoom(targetContext, current)
        }
    }

    fun markOverdueAsPaid(installmentId: String, paymentDate: String, context: Context? = null) {
        val current = _installments.value.map { item ->
            if (item.id == installmentId) {
                val updatedRemaining = (item.remainingInstallments - 1).coerceAtLeast(0)
                val monthly = item.totalAmount / item.totalInstallments.coerceAtLeast(1)
                val newPaidAmount = item.paidAmount + monthly
                val newRemainingAmount = (item.remainingAmount - monthly).coerceAtLeast(0)
                val newStatus = if (updatedRemaining == 0) InstallmentStatus.COMPLETED else InstallmentStatus.PENDING

                val newHistory = item.paymentHistory.toMutableList()
                newHistory.add(
                    PaymentHistoryItem(
                        id = "p_auto_${System.currentTimeMillis()}",
                        installmentNumber = item.totalInstallments - updatedRemaining,
                        dueDate = paymentDate,
                        paidDate = paymentDate,
                        amountFormatted = "${monthly} تومان",
                        status = InstallmentStatus.PAID,
                        note = "پرداخت شده",
                        amount = monthly
                    )
                )

                item.copy(
                    remainingInstallments = updatedRemaining,
                    paidAmount = newPaidAmount,
                    paidAmountFormatted = "$newPaidAmount تومان",
                    remainingAmount = newRemainingAmount,
                    remainingAmountFormatted = "$newRemainingAmount تومان",
                    status = newStatus,
                    nextDueDaysText = if (updatedRemaining > 0) "۳۰ روز دیگر" else "تکمیل شده",
                    paymentHistory = newHistory
                )
            } else {
                item
            }
        }
        updateInternalState(current)

        val targetContext = context?.applicationContext ?: appContext ?: return
        repositoryScope.launch {
            saveToRoom(targetContext, current)
        }
    }

    fun updateScheduleItem(
        installmentId: String,
        scheduleItemId: String,
        newStatus: InstallmentStatus,
        note: String,
        context: Context? = null
    ): Boolean {
        var found = false
        val current = _installments.value.map { item ->
            if (item.id == installmentId) {
                val updatedHistory = item.paymentHistory.map { hist ->
                    if (hist.id == scheduleItemId) {
                        found = true
                        hist.copy(status = newStatus, note = note)
                    } else {
                        hist
                    }
                }
                item.copy(paymentHistory = updatedHistory)
            } else {
                item
            }
        }
        if (found) {
            updateInternalState(current)
            val targetContext = context?.applicationContext ?: appContext ?: return true
            repositoryScope.launch {
                saveToRoom(targetContext, current)
            }
        }
        return found
    }

    fun clearAllInstallments(context: Context? = null) {
        updateInternalState(emptyList())
        val targetContext = context?.applicationContext ?: appContext ?: return
        repositoryScope.launch {
            try {
                val db = AppDatabase.getDatabase(targetContext)
                val userId = SessionManager.userId ?: return@launch
                db.installmentDao().clearAllInstallments(userId)
                db.installmentDao().clearAllPayments(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreSampleInstallments(context: Context? = null) {
        val sample = InstallmentMockDataSource.getSampleInitialInstallments()
        updateInternalState(sample)
        val targetContext = context?.applicationContext ?: appContext ?: return
        repositoryScope.launch {
            saveToRoom(targetContext, sample)
        }
    }

    private suspend fun saveToRoom(context: Context, items: List<InstallmentItem>) {
        try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.installmentDao()
            val userId = SessionManager.userId ?: return
            dao.clearAllInstallments(userId)
            dao.clearAllPayments(userId)

            items.forEach { item ->
                val entity = InstallmentEntity(
                    id = 0,
                    serverId = item.id,
                    syncState = "SYNCED",
                    userId = userId,
                    category = item.category.name,
                    providerName = item.providerOrPerson,
                    title = item.title,
                    amount = item.totalAmount,
                    totalInstallments = item.totalInstallments,
                    paidInstallments = (item.totalInstallments - item.remainingInstallments).coerceAtLeast(0),
                    remainingInstallments = item.remainingInstallments,
                    startDate = System.currentTimeMillis(),
                    dueDay = 15,
                    nextDueDate = System.currentTimeMillis(),
                    status = item.status.name,
                    notes = item.notes
                )
                val instId = dao.insertInstallment(entity).toInt()

                item.paymentHistory.forEach { p ->
                    val paymentEntity = InstallmentPaymentEntity(
                        installmentId = instId,
                        amount = p.amount,
                        dueDate = System.currentTimeMillis(),
                        paidDate = if (p.paidDate != null) System.currentTimeMillis() else null,
                        status = p.status.name,
                        paymentReference = p.id,
                        notes = p.note
                    )
                    dao.insertPayment(paymentEntity)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LocalInstallmentRepository? = null

        val instance: LocalInstallmentRepository
            get() = INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalInstallmentRepository().also { INSTANCE = it }
            }
    }
}
