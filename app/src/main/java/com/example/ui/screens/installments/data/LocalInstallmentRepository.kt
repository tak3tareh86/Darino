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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Canonical Room-backed repository for the Installments domain.
 * Manages installments, payment schedules, and reactive state flows.
 */
class LocalInstallmentRepository private constructor() {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saveMutex = Mutex()

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
                        throw IllegalStateException("Invalid installment startDate in DB")
                    }

                    val nextDueMillis = if (entity.nextDueDate > 0L) {
                        entity.nextDueDate
                    } else {
                        entity.startDate
                    }

                    val nextDueJalali = PersianCalendarHelper.fromEpochMillis(nextDueMillis).toFormattedDate()
                    val endJalali = PersianCalendarHelper.addMonthsToPersianDate(startJalali, entity.totalInstallments)

                    val payments = dbPayments.filter { it.installmentId == entity.id }.map { p ->
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
                            id = p.paymentReference ?: "p_${p.id}",
                            installmentNumber = p.installmentNumber,
                            dueDate = dueStr,
                            paidDate = paidStr,
                            amountFormatted = MoneyFormatter.formatToman(p.amount),
                            status = pStatus,
                            note = p.notes,
                            amount = p.amount
                        )
                    }.sortedBy { it.installmentNumber }

                    val paidAmount = payments.filter { it.status == InstallmentStatus.PAID || it.status == InstallmentStatus.COMPLETED }.sumOf { it.amount }
                    val paidInstallmentsCount = payments.count { it.status == InstallmentStatus.PAID || it.status == InstallmentStatus.COMPLETED }
                    val remainingInstallmentsCount = (entity.totalInstallments - paidInstallmentsCount).coerceAtLeast(0)
                    val remainingAmount = (entity.amount - paidAmount).coerceAtLeast(0L)

                    val actualStatus = if (remainingInstallmentsCount == 0) InstallmentStatus.COMPLETED else status
                    val dueDaysText = computeDueDaysText(nextDueMillis, actualStatus, remainingInstallmentsCount)

                    val firstUnpaid = payments.firstOrNull { it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED }
                    val effectiveNextPaymentDate = firstUnpaid?.dueDate ?: nextDueJalali

                    val monthly = if (payments.isNotEmpty()) {
                        payments.first().amount
                    } else {
                        entity.amount / entity.totalInstallments.coerceAtLeast(1)
                    }

                    InstallmentItem(
                        id = entity.serverId ?: entity.id.toString(),
                        title = entity.title,
                        category = cat,
                        providerOrPerson = entity.providerName,
                        totalAmount = entity.amount,
                        totalAmountFormatted = MoneyFormatter.formatToman(entity.amount),
                        paidAmount = paidAmount,
                        paidAmountFormatted = MoneyFormatter.formatToman(paidAmount),
                        remainingAmount = remainingAmount,
                        remainingAmountFormatted = MoneyFormatter.formatToman(remainingAmount),
                        monthlyPaymentFormatted = MoneyFormatter.formatToman(monthly),
                        totalInstallments = entity.totalInstallments,
                        remainingInstallments = remainingInstallmentsCount,
                        nextPaymentDate = effectiveNextPaymentDate,
                        nextDueDaysText = dueDaysText,
                        startDate = startJalali,
                        endDate = endJalali,
                        status = actualStatus,
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
            throw e
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
            val userId = SessionManager.userId
            if (userId != null) {
                reloadForUser(targetContext, userId)
            }
        }
    }

    fun updateInstallment(updated: InstallmentItem, context: Context? = null) {
        val current = _installments.value.map { if (it.id == updated.id) updated else it }
        updateInternalState(current)

        val targetContext = context?.applicationContext ?: appContext ?: return
        repositoryScope.launch {
            saveToRoom(targetContext, current)
            val userId = SessionManager.userId
            if (userId != null) {
                reloadForUser(targetContext, userId)
            }
        }
    }

    fun deleteInstallment(id: String, context: Context? = null) {
        val current = _installments.value.filter { it.id != id }
        updateInternalState(current)

        val targetContext = context?.applicationContext ?: appContext ?: return
        repositoryScope.launch {
            saveMutex.withLock {
                try {
                    val db = AppDatabase.getDatabase(targetContext)
                    val userId = SessionManager.userId ?: return@withLock
                    val numericId = id.toIntOrNull() ?: 0
                    db.installmentDao().deleteInstallmentById(userId, numericId, id)
                    db.installmentDao().deletePaymentsForInstallment(userId, numericId, id)
                    reloadForUser(targetContext, userId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun markOverdueAsPaid(installmentId: String, paymentDate: String, context: Context? = null) {
        payInstallment(installmentId, paymentDate, context)
    }

    fun payInstallment(installmentId: String, paymentDate: String, context: Context? = null) {
        val todayJalali = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
        val effectivePaymentDate = paymentDate.ifBlank { todayJalali }

        val current = _installments.value.toMutableList()
        val index = current.indexOfFirst { it.id == installmentId }
        if (index == -1) return

        val item = current[index]
        val history = item.paymentHistory.toMutableList()

        val targetIndex = history.indexOfFirst { it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED }

        val updatedHistory = if (targetIndex != -1) {
            val target = history[targetIndex]
            if (target.status == InstallmentStatus.PAID) {
                history
            } else {
                history[targetIndex] = target.copy(
                    status = InstallmentStatus.PAID,
                    paidDate = effectivePaymentDate
                )
                history
            }
        } else {
            val nextNum = history.size + 1
            val monthlyAmount = item.totalAmount / item.totalInstallments.coerceAtLeast(1)
            history.add(
                PaymentHistoryItem(
                    id = "p_auto_${System.currentTimeMillis()}",
                    installmentNumber = nextNum,
                    dueDate = item.nextPaymentDate,
                    paidDate = effectivePaymentDate,
                    amountFormatted = MoneyFormatter.formatToman(monthlyAmount),
                    status = InstallmentStatus.PAID,
                    note = "پرداخت شده",
                    amount = monthlyAmount
                )
            )
            history
        }

        val paidCount = updatedHistory.count { it.status == InstallmentStatus.PAID }
        val remainingCount = (item.totalInstallments - paidCount).coerceAtLeast(0)

        val actualPaidAmount = updatedHistory.filter { it.status == InstallmentStatus.PAID }.sumOf { it.amount }
        val actualRemainingAmount = (item.totalAmount - actualPaidAmount).coerceAtLeast(0L)

        val isCompleted = remainingCount == 0
        val newStatus = if (isCompleted) InstallmentStatus.COMPLETED else InstallmentStatus.PENDING

        val nextPending = updatedHistory.firstOrNull { it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED }
        val nextPaymentDateStr = if (nextPending != null) {
            nextPending.dueDate
        } else if (remainingCount > 0) {
            PersianCalendarHelper.addMonthsToPersianDate(item.nextPaymentDate, 1)
        } else {
            item.nextPaymentDate
        }

        val nextDueMillis = parseJalaliStringToMillis(nextPaymentDateStr) ?: System.currentTimeMillis()
        val newDueDaysText = computeDueDaysText(nextDueMillis, newStatus, remainingCount)

        val updatedItem = item.copy(
            paidAmount = actualPaidAmount,
            paidAmountFormatted = MoneyFormatter.formatToman(actualPaidAmount),
            remainingAmount = actualRemainingAmount,
            remainingAmountFormatted = MoneyFormatter.formatToman(actualRemainingAmount),
            remainingInstallments = remainingCount,
            status = newStatus,
            nextPaymentDate = nextPaymentDateStr,
            nextDueDaysText = newDueDaysText,
            paymentHistory = updatedHistory
        )

        current[index] = updatedItem
        updateInternalState(current)

        val targetContext = context?.applicationContext ?: appContext ?: return
        repositoryScope.launch {
            saveToRoom(targetContext, current)
            val userId = SessionManager.userId
            if (userId != null) {
                reloadForUser(targetContext, userId)
            }
        }
    }

    fun updateScheduleItem(
        installmentId: String,
        scheduleItemId: String,
        newAmountLong: Long? = null,
        newDueDate: String? = null,
        newStatus: InstallmentStatus? = null,
        note: String? = null,
        context: Context? = null
    ): Boolean {
        var found = false
        val current = _installments.value.map { item ->
            if (item.id == installmentId) {
                val updatedHistory = item.paymentHistory.map { hist ->
                    if (hist.id == scheduleItemId) {
                        found = true
                        val targetStatus = newStatus ?: hist.status
                        val targetAmount = newAmountLong ?: hist.amount
                        val targetDueDate = newDueDate ?: hist.dueDate
                        hist.copy(
                            amount = targetAmount,
                            amountFormatted = MoneyFormatter.formatToman(targetAmount),
                            dueDate = targetDueDate,
                            status = targetStatus,
                            note = note ?: hist.note,
                            paidDate = if (targetStatus == InstallmentStatus.PAID && hist.paidDate.isNullOrBlank()) {
                                PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
                            } else hist.paidDate
                        )
                    } else {
                        hist
                    }
                }
                val paidCount = updatedHistory.count { it.status == InstallmentStatus.PAID }
                val remainingCount = (item.totalInstallments - paidCount).coerceAtLeast(0)
                val paidSum = updatedHistory.filter { it.status == InstallmentStatus.PAID }.sumOf { it.amount }
                val totalSum = updatedHistory.sumOf { it.amount }.coerceAtLeast(item.totalAmount)
                val remainingSum = (totalSum - paidSum).coerceAtLeast(0L)
                val status = if (remainingCount == 0) InstallmentStatus.COMPLETED else InstallmentStatus.PENDING

                item.copy(
                    totalAmount = totalSum,
                    totalAmountFormatted = MoneyFormatter.formatToman(totalSum),
                    paidAmount = paidSum,
                    paidAmountFormatted = MoneyFormatter.formatToman(paidSum),
                    remainingAmount = remainingSum,
                    remainingAmountFormatted = MoneyFormatter.formatToman(remainingSum),
                    remainingInstallments = remainingCount,
                    status = status,
                    paymentHistory = updatedHistory
                )
            } else {
                item
            }
        }
        if (found) {
            updateInternalState(current)
            val targetContext = context?.applicationContext ?: appContext ?: return true
            repositoryScope.launch {
                saveToRoom(targetContext, current)
                val userId = SessionManager.userId
                if (userId != null) {
                    reloadForUser(targetContext, userId)
                }
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

    private suspend fun saveToRoom(context: Context, items: List<InstallmentItem>) = saveMutex.withLock {
        try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.installmentDao()
            val userId = SessionManager.userId ?: return@withLock
            dao.clearAllInstallments(userId)
            dao.clearAllPayments(userId)

            items.forEach { item ->
                val startMillis = parseJalaliStringToMillis(item.startDate) ?: throw IllegalArgumentException("Invalid start date: ${item.startDate}")
                val nextDueMillis = parseJalaliStringToMillis(item.nextPaymentDate) ?: startMillis
                val dueDayNum = CalendarDateUtils.parseJalali(item.nextPaymentDate)?.third
                    ?: CalendarDateUtils.parseJalali(item.startDate)?.third
                    ?: throw IllegalArgumentException("Invalid due day in dates")

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
                    paidInstallments = item.paymentHistory.count { it.status == InstallmentStatus.PAID || it.status == InstallmentStatus.COMPLETED },
                    remainingInstallments = (item.totalInstallments - item.paymentHistory.count { it.status == InstallmentStatus.PAID || it.status == InstallmentStatus.COMPLETED }).coerceAtLeast(0),
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
                        installmentNumber = p.installmentNumber,
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
            throw e
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
