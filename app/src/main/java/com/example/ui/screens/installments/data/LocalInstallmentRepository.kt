package com.example.ui.screens.installments.data

import android.content.Context
import androidx.room.withTransaction
import com.example.calendar.domain.CalendarDateUtils
import com.example.data.database.AppDatabase
import com.example.data.database.InstallmentEntity
import com.example.data.database.InstallmentPaymentEntity
import com.example.data.database.TransactionEntity
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

    // True only after the current session's DB snapshot was read and validated successfully.
    private val _installmentDataAvailable = MutableStateFlow(false)
    val installmentDataAvailable: StateFlow<Boolean> = _installmentDataAvailable.asStateFlow()

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
                        _installmentDataAvailable.value = false
                        updateInternalState(emptyList())
                        return@collectLatest
                    }
                    // Never expose the previous account's cached data while loading a new session.
                    updateInternalState(emptyList())
                    runCatching { reloadForUser(appCtx, userId) }
                        .onFailure { updateInternalState(emptyList()) }
                }
            }
        }
    }

    private suspend fun reloadForUser(appCtx: Context, userId: String) {
        // Fail closed: a failed/partial DB read must not leave stale or cross-account data visible.
        _installmentDataAvailable.value = false
        updateInternalState(emptyList())
        try {
            val db = AppDatabase.getDatabase(appCtx)
            val dao = db.installmentDao()
            val dbInsts = dao.getAllInstallmentsList(userId).filter { it.deletedAt == null }
            val dbPayments = dao.getAllPaymentsList(userId)

            if (dbInsts.isNotEmpty()) {
                val loaded = dbInsts.map { entity ->
                    val cat = runCatching { InstallmentCategory.valueOf(entity.category) }
                        .getOrElse { throw IllegalStateException("Invalid installment category in DB: ${entity.id}", it) }
                    val status = runCatching { InstallmentStatus.valueOf(entity.status) }
                        .getOrElse { throw IllegalStateException("Invalid installment status in DB: ${entity.id}", it) }
                    require(entity.totalInstallments > 0) { "Invalid totalInstallments in DB: ${entity.id}" }
                    require(entity.amount > 0L) { "Invalid installment amount in DB: ${entity.id}" }

                    val startJalali = if (entity.startDate > 0L) {
                        PersianCalendarHelper.fromEpochMillis(entity.startDate).toFormattedDate()
                    } else {
                        throw IllegalStateException("Invalid installment startDate in DB: ${entity.startDate}")
                    }

                    val nextDueMillis = if (entity.nextDueDate > 0L) {
                        entity.nextDueDate
                    } else {
                        throw IllegalStateException("Invalid installment nextDueDate in DB: ${entity.nextDueDate}")
                    }

                    val nextDueJalali = PersianCalendarHelper.fromEpochMillis(nextDueMillis).toFormattedDate()
                    val endJalali = PersianCalendarHelper.addMonthsToPersianDate(startJalali, entity.totalInstallments)

                    val payments = dbPayments.filter { it.installmentId == entity.id }.map { p ->
                        val dueStr = if (p.dueDate > 0L) {
                            PersianCalendarHelper.fromEpochMillis(p.dueDate).toFormattedDate()
                        } else {
                            throw IllegalStateException("Invalid payment dueDate in DB for payment ${p.id}: ${p.dueDate}")
                        }
                        val paidStr = p.paidDate?.takeIf { it > 0L }?.let {
                            PersianCalendarHelper.fromEpochMillis(it).toFormattedDate()
                        }
                        require(p.amount > 0L) { "Invalid payment amount in DB: ${p.id}" }
                        require(p.installmentNumber in 1..entity.totalInstallments) { "Invalid installment number in DB: ${p.id}" }
                        val pStatus = runCatching { InstallmentStatus.valueOf(p.status) }
                            .getOrElse { throw IllegalStateException("Invalid payment status in DB: ${p.id}", it) }

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

                    val duplicateNumbers = payments.groupingBy { it.installmentNumber }.eachCount().filterValues { it > 1 }
                    require(duplicateNumbers.isEmpty()) { "Duplicate installment schedule rows in DB: ${entity.id}" }
                    val paidPayments = payments.filter { it.status == InstallmentStatus.PAID || it.status == InstallmentStatus.COMPLETED }
                    val paidAmount = paidPayments.sumOf { it.amount }
                    require(paidAmount <= entity.amount) { "Paid amount exceeds contract amount in DB: ${entity.id}" }
                    val paidInstallmentsCount = paidPayments.map { it.installmentNumber }.toSet().size
                    val remainingInstallmentsCount = (entity.totalInstallments - paidInstallmentsCount).coerceAtLeast(0)
                    val remainingAmount = entity.amount - paidAmount

                    // Schedule/payment history is the source of truth for whether a contract is still active.
                    // Do not let a stale top-level PAID/COMPLETED flag hide unpaid scheduled payments.
                    val actualStatus = when {
                        remainingInstallmentsCount == 0 -> InstallmentStatus.COMPLETED
                        status == InstallmentStatus.PAID || status == InstallmentStatus.COMPLETED -> InstallmentStatus.PENDING
                        else -> status
                    }
                    val dueDaysText = computeDueDaysText(nextDueMillis, actualStatus, remainingInstallmentsCount)

                    val firstUnpaid = payments
                        .filter { it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED }
                        .minByOrNull { parseJalaliStringToMillis(it.dueDate) ?: Long.MAX_VALUE }
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
                _installmentDataAvailable.value = true
            } else {
                updateInternalState(emptyList())
                _installmentDataAvailable.value = true
            }
        } catch (e: Exception) {
            _installmentDataAvailable.value = false
            updateInternalState(emptyList())
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
        val targetContext = context?.applicationContext ?: appContext ?: return
        repositoryScope.launch {
            try {
                val current = _installments.value.toMutableList()
                current.add(0, item)
                saveToRoom(targetContext, current)
                val userId = SessionManager.userId
                if (userId != null) {
                    reloadForUser(targetContext, userId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateInstallment(updated: InstallmentItem, context: Context? = null) {
        val targetContext = context?.applicationContext ?: appContext ?: return
        repositoryScope.launch {
            try {
                val current = _installments.value.map { if (it.id == updated.id) updated else it }
                saveToRoom(targetContext, current)
                val userId = SessionManager.userId
                if (userId != null) {
                    reloadForUser(targetContext, userId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteInstallment(id: String, context: Context? = null) {
        val targetContext = context?.applicationContext ?: appContext ?: return
        repositoryScope.launch {
            saveMutex.withLock {
                try {
                    val db = AppDatabase.getDatabase(targetContext)
                    val userId = SessionManager.userId ?: return@withLock
                    val numericId = id.toIntOrNull() ?: 0
                    db.withTransaction {
                        db.installmentDao().deleteInstallmentById(userId, numericId, id)
                        db.installmentDao().deletePaymentsForInstallment(userId, numericId, id)
                    }
                    reloadForUser(targetContext, userId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun markOverdueAsPaid(installmentId: String, paymentDate: String, context: Context? = null, scheduleItemId: String? = null) {
        repositoryScope.launch {
            payInstallment(installmentId, paymentDate, context, scheduleItemId)
        }
    }

    suspend fun payInstallment(
        installmentId: String,
        paymentDate: String,
        context: Context? = null,
        scheduleItemId: String? = null,
        financialTransaction: TransactionEntity? = null
    ): Boolean {
        val targetContext = context?.applicationContext ?: appContext ?: return false
        return saveMutex.withLock {
            try {
                val todayJalali = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
                val effectivePaymentDate = paymentDate.ifBlank { todayJalali }
                val userId = SessionManager.userId ?: return@withLock false
                val db = AppDatabase.getDatabase(targetContext)

                if (_installments.value.none { it.id == installmentId }) {
                    reloadForUser(targetContext, userId)
                }

                val current = _installments.value.toMutableList()
                val index = current.indexOfFirst { it.id == installmentId }
                if (index == -1) return@withLock false

                val item = current[index]
                val history = item.paymentHistory.toMutableList()

                val finalTargetIndex = if (!scheduleItemId.isNullOrBlank()) {
                    val matchingIndices = history.indices.filter { idx ->
                        val h = history[idx]
                        h.id == scheduleItemId || h.installmentNumber.toString() == scheduleItemId
                    }
                    if (matchingIndices.size == 1) {
                        matchingIndices.single()
                    } else {
                        val matchByDate = history.indices.filter { idx ->
                            val h = history[idx]
                            scheduleItemId.contains(h.dueDate.replace('/', '_')) || h.dueDate == scheduleItemId
                        }
                        if (matchByDate.size == 1) {
                            matchByDate.single()
                        } else {
                            -1
                        }
                    }
                } else {
                    val unpaid = history.filter { it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED }
                    val nearest = unpaid.minByOrNull { p -> parseJalaliStringToMillis(p.dueDate) ?: Long.MAX_VALUE }
                    if (nearest != null) {
                        history.indexOfFirst { it.id == nearest.id }
                    } else -1
                }

                if (finalTargetIndex == -1) {
                    return@withLock false
                }

                val target = history[finalTargetIndex]
                if (financialTransaction != null) {
                    if (financialTransaction.userId != userId ||
                        financialTransaction.amount <= 0L ||
                        financialTransaction.amount != target.amount ||
                        financialTransaction.type != "EXPENSE" ||
                        financialTransaction.sourceType != "INSTALLMENT" ||
                        financialTransaction.sourceId != installmentId) {
                        return@withLock false
                    }
                }

                val isAlreadyPaid = (target.status == InstallmentStatus.PAID || target.status == InstallmentStatus.COMPLETED)
                if (isAlreadyPaid) {
                    try {
                        db.withTransaction {
                            val targetInstallment = current.firstOrNull { it.id == installmentId }
                                ?: throw IllegalStateException("Installment state is missing during paid-state recovery")
                            // Validate/recover the supplied transaction before mutating installment state.
                            if (financialTransaction != null) {
                                syncFinancialTransaction(db, userId, financialTransaction)
                            }
                            // Recover only this installment and its payment history; do not clear or rebuild unrelated user installments.
                            persistInstallmentItemToRoom(db, userId, targetInstallment)
                        }
                        val confirmedInstallment = db.installmentDao().getAllInstallmentsList(userId).any {
                            it.serverId == installmentId && it.deletedAt == null
                        }
                        if (!confirmedInstallment) {
                            return@withLock false
                        }
                        if (financialTransaction != null) {
                            com.example.ui.screens.finance.data.LocalFinanceRepository.instance.refreshMetadataForCurrentUser()
                        }
                        return@withLock true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@withLock false
                    }
                }

                history[finalTargetIndex] = target.copy(
                    status = InstallmentStatus.PAID,
                    paidDate = effectivePaymentDate
                )

                val paidCount = history.count { it.status == InstallmentStatus.PAID || it.status == InstallmentStatus.COMPLETED }
                val remainingCount = (item.totalInstallments - paidCount).coerceAtLeast(0)

                val actualPaidAmount = history.filter { it.status == InstallmentStatus.PAID || it.status == InstallmentStatus.COMPLETED }.sumOf { it.amount }
                val actualRemainingAmount = (item.totalAmount - actualPaidAmount).coerceAtLeast(0L)

                val isCompleted = remainingCount == 0
                val newStatus = if (isCompleted) InstallmentStatus.COMPLETED else InstallmentStatus.PENDING

                val nextPending = history.filter { it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED }
                    .minByOrNull { p -> parseJalaliStringToMillis(p.dueDate) ?: Long.MAX_VALUE }
                val nextPaymentDateStr = nextPending?.dueDate ?: item.nextPaymentDate

                val nextDueMillis = parseJalaliStringToMillis(nextPaymentDateStr) ?: (System.currentTimeMillis() + 86400000L)
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
                    paymentHistory = history
                )

                current[index] = updatedItem
                saveToRoomInternal(targetContext, current, financialTransaction)
                _installments.value = current
                reloadForUser(targetContext, userId)
                if (financialTransaction != null) {
                    com.example.ui.screens.finance.data.LocalFinanceRepository.instance.refreshMetadataForCurrentUser()
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun updateScheduleItem(
        installmentId: String,
        scheduleItemId: String,
        newAmountLong: Long? = null,
        newDueDate: String? = null,
        newStatus: InstallmentStatus? = null,
        note: String? = null,
        context: Context? = null
    ): Boolean {
        val targetContext = context?.applicationContext ?: appContext ?: return false
        if (newDueDate != null && parseJalaliStringToMillis(newDueDate) == null) return false
        if (newAmountLong != null && newAmountLong <= 0L) return false

        return saveMutex.withLock {
            val userId = SessionManager.userId ?: return@withLock false
            val currentItems = _installments.value
            val targetInstallment = currentItems.firstOrNull { it.id == installmentId }
                ?: return@withLock false
            val targetScheduleItem = targetInstallment.paymentHistory.firstOrNull { it.id == scheduleItemId }
                ?: return@withLock false

            val targetAmount = newAmountLong ?: targetScheduleItem.amount
            val updatedHistory = targetInstallment.paymentHistory.map { historyItem ->
                if (historyItem.id != scheduleItemId) {
                    historyItem
                } else {
                    val targetStatus = newStatus ?: historyItem.status
                    historyItem.copy(
                        amount = targetAmount,
                        amountFormatted = MoneyFormatter.formatToman(targetAmount),
                        dueDate = newDueDate ?: historyItem.dueDate,
                        status = targetStatus,
                        note = note ?: historyItem.note,
                        paidDate = if (targetStatus == InstallmentStatus.PAID && historyItem.paidDate.isNullOrBlank()) {
                            PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
                        } else historyItem.paidDate
                    )
                }
            }

            val paidItems = updatedHistory.filter {
                it.status == InstallmentStatus.PAID || it.status == InstallmentStatus.COMPLETED
            }
            val paidCount = paidItems.size
            val paidSum = paidItems.sumOf { it.amount }
            // Adjust the contract total by the edited installment's delta, allowing both increases and decreases.
            val totalSum = (targetInstallment.totalAmount - targetScheduleItem.amount + targetAmount).coerceAtLeast(0L)
            val remainingSum = (totalSum - paidSum).coerceAtLeast(0L)
            val remainingCount = (targetInstallment.totalInstallments - paidCount).coerceAtLeast(0)
            val nextPending = updatedHistory
                .filter { it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED }
                .minByOrNull { parseJalaliStringToMillis(it.dueDate) ?: Long.MAX_VALUE }
            val nextPaymentDate = nextPending?.dueDate ?: targetInstallment.nextPaymentDate
            val nextDueMillis = parseJalaliStringToMillis(nextPaymentDate) ?: return@withLock false
            val nextStatus = if (remainingCount == 0) InstallmentStatus.COMPLETED else InstallmentStatus.PENDING
            val updatedInstallment = targetInstallment.copy(
                totalAmount = totalSum,
                totalAmountFormatted = MoneyFormatter.formatToman(totalSum),
                paidAmount = paidSum,
                paidAmountFormatted = MoneyFormatter.formatToman(paidSum),
                remainingAmount = remainingSum,
                remainingAmountFormatted = MoneyFormatter.formatToman(remainingSum),
                remainingInstallments = remainingCount,
                status = nextStatus,
                nextPaymentDate = nextPaymentDate,
                nextDueDaysText = computeDueDaysText(nextDueMillis, nextStatus, remainingCount),
                paymentHistory = updatedHistory
            )
            val updatedItems = currentItems.map {
                if (it.id == installmentId) updatedInstallment else it
            }

            try {
                val db = AppDatabase.getDatabase(targetContext)
                db.withTransaction {
                    persistInstallmentStateToRoom(db, userId, updatedItems)
                }
                // Refresh the repository state only after Room has committed the update.
                reloadForUser(targetContext, userId)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun clearAllInstallments(context: Context? = null) {
        val targetContext = context?.applicationContext ?: appContext ?: return
        repositoryScope.launch {
            saveMutex.withLock {
                try {
                    val db = AppDatabase.getDatabase(targetContext)
                    val userId = SessionManager.userId ?: return@withLock
                    db.withTransaction {
                        db.installmentDao().clearAllInstallments(userId)
                        db.installmentDao().clearAllPayments(userId)
                    }
                    reloadForUser(targetContext, userId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun saveToRoom(context: Context, items: List<InstallmentItem>) = saveMutex.withLock {
        val db = AppDatabase.getDatabase(context)
        db.withTransaction {
            val userId = SessionManager.userId ?: throw IllegalStateException("No authenticated user ID")
            persistInstallmentStateToRoom(db, userId, items)
        }
    }

    private suspend fun saveToRoomInternal(
        context: Context,
        items: List<InstallmentItem>,
        financialTransaction: TransactionEntity? = null
    ) {
        val db = AppDatabase.getDatabase(context)
        db.withTransaction {
            val userId = SessionManager.userId ?: throw IllegalStateException("No authenticated user ID")

            if (financialTransaction != null) {
                syncFinancialTransaction(db, userId, financialTransaction)
            }

            persistInstallmentStateToRoom(db, userId, items)
        }
    }

    private suspend fun persistInstallmentItemToRoom(
        db: AppDatabase,
        userId: String,
        item: InstallmentItem
    ) {
        val dao = db.installmentDao()
        val existingRows = dao.getAllInstallmentsList(userId).filter { it.serverId == item.id }
        // Prefer an active row; only restore a soft-deleted row when no active copy exists.
        val existing = existingRows.firstOrNull { it.deletedAt == null } ?: existingRows.firstOrNull()
        val now = System.currentTimeMillis()
        val startMillis = parseJalaliStringToMillis(item.startDate)
            ?: throw IllegalArgumentException("Invalid start date: ${item.startDate}")
        val nextDueMillis = parseJalaliStringToMillis(item.nextPaymentDate)
            ?: throw IllegalArgumentException("Invalid next payment date: ${item.nextPaymentDate}")
        val dueDayNum = CalendarDateUtils.parseJalali(item.nextPaymentDate)?.third
            ?: throw IllegalArgumentException("Invalid due day in nextPaymentDate: ${item.nextPaymentDate}")
        val paidCount = item.paymentHistory.count {
            it.status == InstallmentStatus.PAID || it.status == InstallmentStatus.COMPLETED
        }
        val entity = InstallmentEntity(
            id = existing?.id ?: 0,
            serverId = item.id,
            syncState = "SYNCED",
            userId = userId,
            category = item.category.name,
            providerName = item.providerOrPerson,
            title = item.title,
            amount = item.totalAmount,
            totalInstallments = item.totalInstallments,
            paidInstallments = paidCount,
            remainingInstallments = (item.totalInstallments - paidCount).coerceAtLeast(0),
            startDate = startMillis,
            dueDay = dueDayNum,
            nextDueDate = nextDueMillis,
            status = item.status.name,
            notes = item.notes,
            updatedAt = now,
            deletedAt = null
        )

        val internalId = if (existing == null) {
            dao.insertInstallment(entity).toInt()
        } else {
            dao.updateInstallment(entity)
            existing.id
        }

        dao.deletePaymentsForInstallment(userId, internalId, item.id)
        item.paymentHistory.forEach { payment ->
            val dueMillis = parseJalaliStringToMillis(payment.dueDate)
                ?: throw IllegalArgumentException("Invalid payment due date: ${payment.dueDate}")
            val paidMillis = payment.paidDate?.takeIf { it.isNotBlank() }?.let {
                parseJalaliStringToMillis(it)
                    ?: throw IllegalArgumentException("Invalid payment paid date: $it")
            }
            dao.insertPayment(
                InstallmentPaymentEntity(
                    installmentId = internalId,
                    installmentNumber = payment.installmentNumber,
                    amount = payment.amount,
                    dueDate = dueMillis,
                    paidDate = paidMillis,
                    status = payment.status.name,
                    paymentReference = payment.id,
                    notes = payment.note
                )
            )
        }

        val confirmedInstallment = dao.getAllInstallmentsList(userId).firstOrNull {
            it.serverId == item.id && it.deletedAt == null
        }
        if (confirmedInstallment == null || confirmedInstallment.id != internalId) {
            throw IllegalStateException("Failed to restore or verify installment ${item.id}")
        }
        val confirmedPayments = dao.getAllPaymentsList(userId).count { it.installmentId == internalId }
        if (confirmedPayments != item.paymentHistory.size) {
            throw IllegalStateException("Failed to restore installment payment history for ${item.id}")
        }
    }

    private suspend fun persistInstallmentStateToRoom(
        db: AppDatabase,
        userId: String,
        items: List<InstallmentItem>
    ) {
        val dao = db.installmentDao()
        dao.clearAllInstallments(userId)
        dao.clearAllPayments(userId)

        items.forEach { item ->
            val startMillis = parseJalaliStringToMillis(item.startDate) 
                ?: throw IllegalArgumentException("Invalid start date: ${item.startDate}")
            val nextDueMillis = parseJalaliStringToMillis(item.nextPaymentDate) 
                ?: throw IllegalArgumentException("Invalid next payment date: ${item.nextPaymentDate}")
            val dueDayNum = CalendarDateUtils.parseJalali(item.nextPaymentDate)?.third
                ?: throw IllegalArgumentException("Invalid due day in nextPaymentDate: ${item.nextPaymentDate}")

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
                val pDueMillis = parseJalaliStringToMillis(p.dueDate) 
                    ?: throw IllegalArgumentException("Invalid payment due date: ${p.dueDate}")
                val pPaidMillis = p.paidDate?.takeIf { it.isNotBlank() }?.let {
                    parseJalaliStringToMillis(it) ?: throw IllegalArgumentException("Invalid payment paid date: $it")
                }

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
        val targetDt = PersianCalendarHelper.fromEpochMillis(targetMillis)
        val nowDt = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())

        val targetEpochDay = PersianCalendarHelper.getJalaliEpochDay(targetDt.year, targetDt.month, targetDt.day)
        val nowEpochDay = PersianCalendarHelper.getJalaliEpochDay(nowDt.year, nowDt.month, nowDt.day)

        val diffDays = (targetEpochDay - nowEpochDay).toInt()
        return when {
            diffDays == 0 -> "امروز"
            diffDays > 0 -> "${IranianPhoneUtils.convertDigitsToPersian(diffDays.toString())} روز دیگر"
            else -> "${IranianPhoneUtils.convertDigitsToPersian(kotlin.math.abs(diffDays).toString())} روز گذشته"
        }
    }

    private suspend fun syncFinancialTransaction(
        db: AppDatabase,
        userId: String,
        financialTransaction: TransactionEntity
    ): TransactionEntity {
        if (financialTransaction.stringId.isBlank() || financialTransaction.amount <= 0L || financialTransaction.userId != userId) {
            throw IllegalArgumentException("Invalid financial transaction input for installment payment")
        }

        // A supplied account must belong to this user and remain active before any transaction or installment mutation.
        val suppliedAccountId = financialTransaction.accountId
        if (suppliedAccountId != null) {
            if (suppliedAccountId.isBlank()) {
                throw IllegalArgumentException("Blank account ID for installment payment")
            }
            val account = db.accountDao().getAccountByStringId(userId, suppliedAccountId)
            if (account == null || !account.isActive || account.deletedAt != null) {
                throw IllegalArgumentException("Invalid or inactive account for installment payment")
            }
        }

        val existingTx = db.transactionDao().getTransactionIncludingDeleted(userId, financialTransaction.stringId)
        if (existingTx == null) {
            db.transactionDao().insertTransaction(financialTransaction)
            val confirmed = db.transactionDao().getTransactionByStringId(userId, financialTransaction.stringId)
            if (confirmed == null || confirmed.deletedAt != null) {
                throw IllegalStateException("Failed to insert or verify financial transaction: ${financialTransaction.stringId}")
            }
            return confirmed
        }

        // Verify business identity
        val matchesBusinessIdentity = existingTx.userId == userId &&
                existingTx.amount == financialTransaction.amount &&
                existingTx.type == financialTransaction.type &&
                existingTx.sourceType == financialTransaction.sourceType &&
                existingTx.sourceId == financialTransaction.sourceId &&
                (financialTransaction.accountId.isNullOrBlank() || existingTx.accountId == financialTransaction.accountId)

        if (!matchesBusinessIdentity) {
            throw IllegalStateException("Transaction ID collision with mismatched financial data: id=${financialTransaction.stringId}")
        }

        if (existingTx.deletedAt != null) {
            // Restore soft-deleted transaction safely
            val restored = existingTx.copy(
                deletedAt = null,
                updatedAt = System.currentTimeMillis(),
                syncState = "PENDING_UPSERT",
                title = financialTransaction.title.ifBlank { existingTx.title },
                description = financialTransaction.description.ifBlank { existingTx.description }
            )
            db.transactionDao().updateTransaction(restored)
            val confirmed = db.transactionDao().getTransactionByStringId(userId, financialTransaction.stringId)
            if (confirmed == null || confirmed.deletedAt != null) {
                throw IllegalStateException("Failed to restore soft-deleted financial transaction: ${financialTransaction.stringId}")
            }
            return confirmed
        }

        return existingTx
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
