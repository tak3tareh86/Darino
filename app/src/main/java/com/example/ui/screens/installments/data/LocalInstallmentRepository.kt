package com.example.ui.screens.installments.data

import android.content.Context
import com.example.calendar.domain.CalendarDateUtils
import com.example.data.database.AppDatabase
import com.example.data.database.InstallmentEntity
import com.example.data.database.InstallmentPaymentEntity
import com.example.data.security.SessionManager
import com.example.data.security.SessionState
import com.example.ui.screens.installments.model.*
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper
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
                    val cat = try { InstallmentCategory.valueOf(entity.category) } catch (e: Exception) { InstallmentCategory.BANK_LOANS }
                    val status = try { InstallmentStatus.valueOf(entity.status) } catch (e: Exception) { InstallmentStatus.PENDING }

                    val startJalali = if (entity.startDate > 0L) {
                        PersianCalendarHelper.fromEpochMillis(entity.startDate).toFormattedDate()
                    } else {
                        PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
                    }

                    val nextDueMillis = if (entity.nextDueDate > 0L) {
                        entity.nextDueDate
                    } else if (entity.startDate > 0L) {
                        entity.startDate
                    } else {
                        System.currentTimeMillis()
                    }

                    val nextDueJalali = PersianCalendarHelper.fromEpochMillis(nextDueMillis).toFormattedDate()
                    val endJalali = PersianCalendarHelper.addMonthsToPersianDate(startJalali, entity.totalInstallments)
                    val dueDaysText = computeDueDaysText(nextDueMillis, status, entity.remainingInstallments)

                    val payments = dbPayments.filter { it.installmentId == entity.id }.mapIndexed { idx, p ->
                        val dueStr = if (p.dueDate > 0L) {
                            PersianCalendarHelper.fromEpochMillis(p.dueDate).toFormattedDate()
                        } else {
                            nextDueJalali
                        }
                        val paidStr = p.paidDate?.takeIf { it > 0L }?.let {
                            PersianCalendarHelper.fromEpochMillis(it).toFormattedDate()
                        }
                        val pStatus = try { InstallmentStatus.valueOf(p.status) } catch (e: Exception) { InstallmentStatus.PENDING }

                        PaymentHistoryItem(
                            id = p.paymentReference ?: p.id.toString(),
                            installmentNumber = idx + 1,
                            dueDate = dueStr,
                            paidDate = paidStr,
                            amountFormatted = MoneyFormatter.formatToman(p.amount),
                            status = pStatus,
                            note = p.notes,
                            amount = p.amount
                        )
                    }

                    val paidAmount = entity.amount - (entity.amount * entity.remainingInstallments / entity.totalInstallments.coerceAtLeast(1))
                    val monthly = entity.amount / entity.totalInstallments.coerceAtLeast(1)

                    InstallmentItem(
                        id = entity.serverId ?: entity.id.toString(),
                        title = entity.title,
                        category = cat,
                        providerOrPerson = entity.providerName,
                        totalAmount = entity.amount,
                        totalAmountFormatted = MoneyFormatter.formatToman(entity.amount),
                        paidAmount = paidAmount,
                        paidAmountFormatted = MoneyFormatter.formatToman(paidAmount),
                        remainingAmount = entity.amount - paidAmount,
                        remainingAmountFormatted = MoneyFormatter.formatToman(entity.amount - paidAmount),
                        monthlyPaymentFormatted = MoneyFormatter.formatToman(monthly),
                        totalInstallments = entity.totalInstallments,
                        remainingInstallments = entity.remainingInstallments,
                        nextPaymentDate = nextDueJalali,
                        nextDueDaysText = dueDaysText,
                        startDate = startJalali,
                        endDate = endJalali,
                        status = status,
                        notes = entity.notes ?: "",
                        paymentHistory = payments
                    )
                }
                updateInternalState(loaded)
            } else {
                updateInternalState(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reloadFromDatabase(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        repositoryScope.launch {
            val userId = SessionManager.userId ?: return@launch
            reloadForUser(appCtx, userId)
        }
    }

    private fun updateInternalState(items: List<InstallmentItem>) {
        _installments.value = items
        _bankLoans.value = items.filter { it.category == InstallmentCategory.BANK_LOANS }
        _homeLoans.value = items.filter { it.category == InstallmentCategory.HOME_LOANS }
        _carInsurance.value = items.filter { it.category == InstallmentCategory.CAR_INSURANCE }
        _miscInstallments.value = items.filter { it.category == InstallmentCategory.MISC }
        _overdueInstallments.value = items.filter { it.status == InstallmentStatus.OVERDUE }

        val activeItems = items.filter { it.remainingInstallments > 0 }
        val nearestItem = activeItems.minByOrNull { parseJalaliStringToMillis(it.nextPaymentDate) ?: Long.MAX_VALUE }

        val totalPaid = items.sumOf { it.paidAmount }
        val totalRemaining = items.sumOf { it.remainingAmount }

        _summary.value = InstallmentSummaryData(
            activeCount = activeItems.size,
            paidAmount = totalPaid,
            remainingAmount = totalRemaining,
            paidAmountFormatted = MoneyFormatter.formatToman(totalPaid),
            remainingAmountFormatted = MoneyFormatter.formatToman(totalRemaining),
            nextDueText = nearestItem?.nextDueDaysText ?: "-",
            nextDueTitle = nearestItem?.title ?: "-"
        )

        _categorySummaries.value = listOf(
            CategorySummaryStat(
                InstallmentCategory.BANK_LOANS,
                "${_bankLoans.value.count { it.remainingInstallments > 0 }} مورد",
                MoneyFormatter.formatToman(_bankLoans.value.sumOf { it.remainingAmount })
            ),
            CategorySummaryStat(
                InstallmentCategory.HOME_LOANS,
                "${_homeLoans.value.count { it.remainingInstallments > 0 }} مورد",
                MoneyFormatter.formatToman(_homeLoans.value.sumOf { it.remainingAmount })
            ),
            CategorySummaryStat(
                InstallmentCategory.CAR_INSURANCE,
                "${_carInsurance.value.count { it.remainingInstallments > 0 }} مورد",
                MoneyFormatter.formatToman(_carInsurance.value.sumOf { it.remainingAmount })
            ),
            CategorySummaryStat(
                InstallmentCategory.MISC,
                "${_miscInstallments.value.count { it.remainingInstallments > 0 }} مورد",
                MoneyFormatter.formatToman(_miscInstallments.value.sumOf { it.remainingAmount })
            )
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
        payInstallment(installmentId, paymentDate, context)
    }

    fun payInstallment(installmentId: String, paymentDate: String, context: Context? = null) {
        val todayJalali = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
        val effectivePaymentDate = paymentDate.ifBlank { todayJalali }

        val current = _installments.value.map { item ->
            if (item.id == installmentId) {
                val updatedRemaining = (item.remainingInstallments - 1).coerceAtLeast(0)
                val monthly = item.totalAmount / item.totalInstallments.coerceAtLeast(1)
                val newPaidAmount = item.paidAmount + monthly
                val newRemainingAmount = (item.remainingAmount - monthly).coerceAtLeast(0)
                val newStatus = if (updatedRemaining == 0) InstallmentStatus.COMPLETED else InstallmentStatus.PENDING

                val nextDueDateStr = if (updatedRemaining > 0) {
                    PersianCalendarHelper.addMonthsToPersianDate(item.nextPaymentDate, 1)
                } else {
                    item.nextPaymentDate
                }
                val nextDueMillis = parseJalaliStringToMillis(nextDueDateStr) ?: System.currentTimeMillis()
                val newDueDaysText = computeDueDaysText(nextDueMillis, newStatus, updatedRemaining)

                val newHistory = item.paymentHistory.toMutableList()
                newHistory.add(
                    PaymentHistoryItem(
                        id = "p_auto_${System.currentTimeMillis()}",
                        installmentNumber = item.totalInstallments - updatedRemaining,
                        dueDate = item.nextPaymentDate,
                        paidDate = effectivePaymentDate,
                        amountFormatted = MoneyFormatter.formatToman(monthly),
                        status = InstallmentStatus.PAID,
                        note = "پرداخت شده",
                        amount = monthly
                    )
                )

                item.copy(
                    remainingInstallments = updatedRemaining,
                    paidAmount = newPaidAmount,
                    paidAmountFormatted = MoneyFormatter.formatToman(newPaidAmount),
                    remainingAmount = newRemainingAmount,
                    remainingAmountFormatted = MoneyFormatter.formatToman(newRemainingAmount),
                    status = newStatus,
                    nextPaymentDate = nextDueDateStr,
                    nextDueDaysText = newDueDaysText,
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
                val startMillis = parseJalaliStringToMillis(item.startDate) ?: System.currentTimeMillis()
                val nextDueMillis = parseJalaliStringToMillis(item.nextPaymentDate) ?: startMillis
                val dueDayNum = CalendarDateUtils.parseJalali(item.nextPaymentDate)?.third
                    ?: CalendarDateUtils.parseJalali(item.startDate)?.third
                    ?: 15

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
                    startDate = startMillis,
                    dueDay = dueDayNum,
                    nextDueDate = nextDueMillis,
                    status = item.status.name,
                    notes = item.notes
                )
                val instId = dao.insertInstallment(entity).toInt()

                item.paymentHistory.forEach { p ->
                    val pDueMillis = parseJalaliStringToMillis(p.dueDate) ?: nextDueMillis
                    val pPaidMillis = p.paidDate?.let { parseJalaliStringToMillis(it) }

                    val paymentEntity = InstallmentPaymentEntity(
                        installmentId = instId,
                        amount = p.amount,
                        dueDate = pDueMillis,
                        paidDate = pPaidMillis,
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

    private fun parseJalaliStringToMillis(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        val triple = CalendarDateUtils.parseJalali(dateStr) ?: return null
        return runCatching {
            PersianCalendarHelper.jalaliToEpochMillis(triple.first, triple.second, triple.third, 9, 0)
        }.getOrNull()
    }

    private fun computeDueDaysText(targetMillis: Long, status: InstallmentStatus, remainingInstallments: Int): String {
        if (status == InstallmentStatus.COMPLETED || remainingInstallments == 0) {
            return "تکمیل شده"
        }
        val nowMillis = System.currentTimeMillis()
        val diffMillis = targetMillis - nowMillis
        val diffDays = (diffMillis / (24 * 3600 * 1000L)).toInt()
        return when {
            diffDays == 0 -> "امروز"
            diffDays > 0 -> "${IranianPhoneUtils.convertDigitsToPersian(diffDays.toString())} روز دیگر"
            else -> "${IranianPhoneUtils.convertDigitsToPersian(kotlin.math.abs(diffDays).toString())} روز گذشته"
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
