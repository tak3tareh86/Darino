package com.example.ui.screens.home.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.TransactionEntity
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.security.SessionManager
import com.example.data.security.SessionState
import com.example.data.sms.BankSmsRepository
import com.example.data.sms.BankSmsParser
import com.example.data.sms.SmsPatternMatcher
import com.example.data.sms.SmsPatternRepository
import com.example.data.sms.UserBankSmsRuleRepository
import com.example.data.sms.UserBankSmsRuleMatcher
import com.example.data.sms.UserRuleSmsAmountParser
import com.example.data.sms.SmsPatternChangeNotifier
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.data.HomeDashboardRepository
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.ui.screens.home.domain.HomeDashboardAggregator
import com.example.ui.screens.home.domain.HomeDashboardState
import com.example.ui.screens.home.domain.ObligationType
import com.example.ui.screens.installments.data.LocalInstallmentRepository
import com.example.util.IranianDateUtils
import com.example.util.MoneyFormatter
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency
import com.example.util.PersianCalendarHelper
import com.example.vehicle.data.VehicleExpenseCategory
import com.example.vehicle.data.VehicleRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SmsPermissionState {
    NOT_REQUESTED,
    DENIED,
    PERMANENTLY_DENIED,
    GRANTED
}

sealed class SmsAcceptResult {
    object Success : SmsAcceptResult()
    object AlreadyExists : SmsAcceptResult()
    object TypeNotSelected : SmsAcceptResult()
    object InvalidAmount : SmsAcceptResult()
    object AccountRequired : SmsAcceptResult()
    object DestinationAccountRequired : SmsAcceptResult()
    object Failed : SmsAcceptResult()
}

class HomeDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HomeDashboardRepository(application)
    private val aggregator = HomeDashboardAggregator(repository)
    private val smsRepository = BankSmsRepository.getInstance(application)
    private val smsPatternRepository = SmsPatternRepository(application)
    private val userBankSmsRuleRepository = UserBankSmsRuleRepository(application)

    private val _uiState = MutableStateFlow(HomeDashboardState(isLoading = true))
    val uiState: StateFlow<HomeDashboardState> = _uiState.asStateFlow()

    // SMS Permission & Scanning States
    private val _smsPermissionState = MutableStateFlow(
        if (smsRepository.hasSmsPermission()) {
            SmsPermissionState.GRANTED
        } else if (smsRepository.wasPermissionRequestedBefore()) {
            SmsPermissionState.DENIED
        } else {
            SmsPermissionState.NOT_REQUESTED
        }
    )
    val smsPermissionState: StateFlow<SmsPermissionState> = _smsPermissionState.asStateFlow()

    private val _isScanningSms = MutableStateFlow(false)
    val isScanningSms: StateFlow<Boolean> = _isScanningSms.asStateFlow()

    private val _smsErrorMessage = MutableStateFlow<String?>(null)
    val smsErrorMessage: StateFlow<String?> = _smsErrorMessage.asStateFlow()

    // SMS suggestions queue
    private val _pendingSmsQueue = MutableStateFlow<List<BankSmsSuggestion>>(emptyList())
    val pendingSmsQueue: StateFlow<List<BankSmsSuggestion>> = _pendingSmsQueue.asStateFlow()

    // Reactive user accounts for authenticated user
    private val _userAccounts = MutableStateFlow<List<com.example.data.database.AccountEntity>>(emptyList())
    val userAccounts: StateFlow<List<com.example.data.database.AccountEntity>> = _userAccounts.asStateFlow()

    // Controlled debounced refresh mechanism with Mutex serialization & versioned latest-wins state protection
    private val refreshTrigger = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val refreshMutex = Mutex()
    private val smsScanMutex = Mutex()
    private val smsProcessingMutex = Mutex()
    private val obligationProcessingMutex = Mutex()
    private var latestRequestId = 0L
    private var lastCommittedVersion = 0L

    init {
        // Robust refresh pipeline: debounced, mutex-serialized (at most one aggregate execution at a time), latest wins (version checked)
        viewModelScope.launch {
            refreshTrigger
                .debounce(50L)
                .collect {
                    val requestId = ++latestRequestId
                    refreshMutex.withLock {
                        if (requestId <= lastCommittedVersion) return@withLock
                        try {
                            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                            val aggregated = aggregator.aggregate()
                            if (requestId > lastCommittedVersion) {
                                lastCommittedVersion = requestId
                                _uiState.value = aggregated
                            }
                        } catch (e: Exception) {
                            if (requestId > lastCommittedVersion) {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = "خطا در بارگذاری اطلاعات داشبورد: ${e.localizedMessage}"
                                    )
                                }
                            }
                        }
                    }
                }
        }

        // Reactive observations across all data layers
        viewModelScope.launch {
            SessionManager.sessionState.collectLatest { state ->
                val userId = when (state) {
                    is SessionState.Authenticated -> state.user.id
                    is SessionState.PhoneVerificationRequired -> state.user.id
                    else -> null
                }
                // Restore only the authenticated user's encrypted review queue.
                _pendingSmsQueue.value = if (userId.isNullOrBlank()) {
                    emptyList()
                } else {
                    smsRepository.getPendingSmsSuggestions(userId)
                        .filterNot { it.id in smsRepository.getProcessedSmsIds(userId) || it.id in smsRepository.getDismissedSmsIds(userId) }
                }

                if (userId != null) {
                    launch {
                        try {
                            val db = AppDatabase.getDatabase(application)
                            db.smartReminderDao().getActiveReminders(userId).collect {
                                loadDashboardData()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    launch {
                        try {
                            val db = AppDatabase.getDatabase(application)
                            com.example.ui.screens.finance.data.LocalFinanceRepository.instance.ensureDefaultAccounts(db, userId)
                            db.accountDao().getAllAccountsFlow(userId).collectLatest { accList ->
                                _userAccounts.value = accList.filter { it.isActive && it.deletedAt == null }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    _userAccounts.value = emptyList()
                }
                loadDashboardData()
            }
        }

        viewModelScope.launch {
            LocalFinanceRepository.instance.getTransactions().collect {
                loadDashboardData()
            }
        }

        viewModelScope.launch {
            LocalInstallmentRepository.instance.installments.collect {
                loadDashboardData()
            }
        }

        viewModelScope.launch {
            VehicleRepository.instance.vehicles.collect {
                loadDashboardData()
            }
        }

        viewModelScope.launch {
            VehicleRepository.instance.insurances.collect {
                loadDashboardData()
            }
        }

        viewModelScope.launch {
            VehicleRepository.instance.services.collect {
                loadDashboardData()
            }
        }

        viewModelScope.launch {
            com.example.data.preferences.AppPreferencesRepository.getInstance(application).currency.collect {
                loadDashboardData()
            }
        }

        // Re-scan immediately when a template is added, edited, deleted,
        // or its per-template lookback window changes in SMS pattern management.
        viewModelScope.launch {
            SmsPatternChangeNotifier.changes.collect {
                scanInboxSms()
            }
        }

        // If permission is already granted, scan real SMS inbox
        if (smsRepository.hasSmsPermission()) {
            scanInboxSms()
        }
    }

    fun updatePermissionState(state: SmsPermissionState) {
        _smsPermissionState.value = state
        if (state == SmsPermissionState.GRANTED) {
            scanInboxSms()
        }
    }

    fun markPermissionRequested(granted: Boolean, permanentlyDenied: Boolean = false) {
        smsRepository.markPermissionRequested()
        _smsPermissionState.value = when {
            granted -> SmsPermissionState.GRANTED
            permanentlyDenied -> SmsPermissionState.PERMANENTLY_DENIED
            else -> SmsPermissionState.DENIED
        }
        if (granted) {
            scanInboxSms()
        }
    }

    fun scanInboxSms() {
        if (!smsRepository.hasSmsPermission()) {
            _smsPermissionState.value = SmsPermissionState.DENIED
            return
        }

        val userId = SessionManager.userId
        if (userId.isNullOrBlank()) {
            _pendingSmsQueue.value = emptyList()
            return
        }

        viewModelScope.launch {
            smsScanMutex.withLock {
                _isScanningSms.value = true
                _smsErrorMessage.value = null
                try {
                    val realSmsList = smsRepository.readInboxBankSms(userId)
                    val processedIds = smsRepository.getProcessedSmsIds(userId)
                    val dismissedIds = smsRepository.getDismissedSmsIds(userId)
                    // The persisted queue is already scoped to this user. Do not merge
                    // in-memory state here: it may belong to a previous account during a
                    // fast sign-out/sign-in transition.
                    val currentPending = smsRepository.getPendingSmsSuggestions(userId)
                        .filterNot { it.id in processedIds || it.id in dismissedIds }
                    val existingIds = currentPending.map { it.id }.toSet()
                    val newItems = realSmsList.filterNot { it.id in existingIds }

                    val mergedQueue = (currentPending + newItems).takeLast(50)
                    _pendingSmsQueue.value = mergedQueue
                    smsRepository.savePendingSmsSuggestions(userId, mergedQueue)
                } catch (e: Exception) {
                    _smsErrorMessage.value = "خطا در خواندن پیامک‌ها: ${e.localizedMessage}"
                } finally {
                    _isScanningSms.value = false
                }
            }
        }
    }

    // Transaction type is immutable after parsing; configured templates and the generic parser both determine it.
    fun updateSmsTransactionType(id: String, newType: TransactionType) = Unit

    /** Persist user-selected category and account links while keeping the original SMS evidence intact. */
    fun updateSmsCustomDetails(
        id: String,
        amount: Long? = null,
        type: TransactionType? = null,
        category: String? = null,
        account: String? = null,
        destAccount: String? = null
    ) {
        _pendingSmsQueue.update { list ->
            list.map { item ->
                if (item.id == id) item.copy(
                    category = category?.trim()?.ifEmpty { null } ?: item.category,
                    sourceAccount = account?.trim()?.ifEmpty { null } ?: item.sourceAccount,
                    destinationAccount = destAccount?.trim()?.ifEmpty { null } ?: item.destinationAccount
                ) else item
            }
        }
        persistPendingSmsQueue()
    }

    private fun persistPendingSmsQueue(userId: String? = SessionManager.userId) {
        if (!userId.isNullOrBlank()) {
            smsRepository.savePendingSmsSuggestions(userId, _pendingSmsQueue.value)
        }
    }

    private fun removePendingSmsSuggestion(id: String, userId: String? = SessionManager.userId) {
        _pendingSmsQueue.update { list -> list.filterNot { it.id == id } }
        if (!userId.isNullOrBlank()) {
            smsRepository.savePendingSmsSuggestions(userId, _pendingSmsQueue.value)
        }
    }

    fun refreshPendingSmsQueue() {
        val userId = SessionManager.userId
        if (userId.isNullOrBlank()) {
            _pendingSmsQueue.value = emptyList()
            return
        }
        val processed = smsRepository.getProcessedSmsIds(userId)
        val dismissed = smsRepository.getDismissedSmsIds(userId)
        _pendingSmsQueue.value = smsRepository.getPendingSmsSuggestions(userId)
            .filterNot { it.id in processed || it.id in dismissed }
    }

    fun loadDashboardData() {
        refreshTrigger.tryEmit(Unit)
    }

    fun acceptSmsSuggestion(
        id: String,
        customType: TransactionType? = null,
        customCategory: String? = null,
        customAccount: String? = null,
        customDescription: String? = null,
        destAccount: String? = null,
        customAmount: Long? = null,
        onResult: (SmsAcceptResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            val userId = SessionManager.userId
            // Strictly require authenticated user. No fake or default user fallback!
            if (userId.isNullOrBlank()) {
                onResult(SmsAcceptResult.Failed)
                return@launch
            }

            smsProcessingMutex.withLock {
                // 1. Check if SMS has already been processed or dismissed for this user
                if (smsRepository.getProcessedSmsIds(userId).contains(id) || smsRepository.getDismissedSmsIds(userId).contains(id)) {
                    removePendingSmsSuggestion(id, userId)
                    onResult(SmsAcceptResult.AlreadyExists)
                    return@withLock
                }

                val item = _pendingSmsQueue.value.find { id == it.id }
                if (item == null) {
                    onResult(SmsAcceptResult.Failed)
                    return@withLock
                }

                // Revalidate against the current settings at the moment of persistence.
                val activePatterns = smsPatternRepository.getPatterns(userId)
                val activeRules = userBankSmsRuleRepository.getAll(userId)
                val freshlyParsed = when {
                    item.patternId.startsWith("user-rule:") -> {
                        val ruleId = item.patternId.removePrefix("user-rule:")
                        val eligibleRules = activeRules.filter { item.receivedAtMillis >= System.currentTimeMillis() - it.lookbackMillis }
                        val match = UserBankSmsRuleMatcher.matchAll(item.smsText, item.rawSender.orEmpty(), eligibleRules)
                            .singleOrNull()?.takeIf { it.rule.id == ruleId }
                        match?.let {
                            UserRuleSmsAmountParser.parse(item.id, item.rawSender.orEmpty(), item.smsText, item.receivedAtMillis, it.rule, it.type)
                        }
                    }
                    item.patternId.isBlank() -> null
                    else -> {
                        val originPattern = activePatterns.singleOrNull { it.id == item.patternId }
                        val uniquePattern = SmsPatternMatcher.findUniqueMatch(item.smsText, activePatterns, item.rawSender)
                        if (originPattern != null && uniquePattern?.id == originPattern.id) {
                            BankSmsParser.parseMatchedPattern(item.id, item.rawSender.orEmpty(), item.smsText, item.receivedAtMillis, originPattern)
                        } else null
                    }
                }
                if (freshlyParsed == null ||
                    freshlyParsed.amount != item.amount ||
                    freshlyParsed.amountUnit != item.amountUnit ||
                    freshlyParsed.type != item.type ||
                    freshlyParsed.patternId != item.patternId ||
                    freshlyParsed.timestampMillis != item.timestampMillis ||
                    freshlyParsed.receivedAtMillis != item.receivedAtMillis ||
                    freshlyParsed.dateText != item.dateText ||
                    freshlyParsed.timeText != item.timeText ||
                    freshlyParsed.balanceAmount != item.balanceAmount ||
                    freshlyParsed.balanceAmountUnit != item.balanceAmountUnit ||
                    freshlyParsed.amountCandidates != item.amountCandidates ||
                    freshlyParsed.amountCandidateUnits != item.amountCandidateUnits ||
                    freshlyParsed.trackingCode != item.trackingCode ||
                    freshlyParsed.bankName != item.bankName
                ) {
                    removePendingSmsSuggestion(id, userId)
                    scanInboxSms()
                    onResult(SmsAcceptResult.Failed)
                    return@withLock
                }

                // 2. Amount validation: must be strictly positive and valid
                val finalAmountRials = customAmount ?: runCatching { MoneyAmount.of(item.amount, item.amountUnit).asRialsLongExact() }.getOrNull()
                if (finalAmountRials == null || finalAmountRials <= 0L) {
                    onResult(SmsAcceptResult.InvalidAmount)
                    return@withLock
                }

                // 3. Type validation
                val finalType = customType ?: item.type
                if (finalType == null || (item.isTypeUncertain && customType == null)) {
                    onResult(SmsAcceptResult.TypeNotSelected)
                    return@withLock
                }

                // Resolve account links against the authenticated user's active Room accounts.
                // Never trust IDs supplied by the UI without checking ownership and active state.
                val selectedSourceId = customAccount?.trim()?.takeIf { it.isNotEmpty() }
                val selectedDestinationId = destAccount?.trim()?.takeIf { it.isNotEmpty() }
                val accountDao = AppDatabase.getDatabase(getApplication()).accountDao()
                val sourceAccount = selectedSourceId?.let {
                    accountDao.getAccountByStringId(userId, it)?.takeIf { account -> account.isActive && account.deletedAt == null }
                }
                if (sourceAccount == null) {
                    onResult(SmsAcceptResult.AccountRequired)
                    return@withLock
                }

                val destinationAccount = if (finalType == TransactionType.TRANSFER) {
                    val destinationId = selectedDestinationId?.takeIf { it.isNotBlank() }
                    if (destinationId == null) {
                        onResult(SmsAcceptResult.DestinationAccountRequired)
                        return@withLock
                    }
                    val resolved = accountDao.getAccountByStringId(userId, destinationId)
                        ?.takeIf { account -> account.isActive && account.deletedAt == null }
                    if (resolved == null) {
                        onResult(SmsAcceptResult.DestinationAccountRequired)
                        return@withLock
                    }
                    resolved
                } else {
                    null
                }

                val txId = "tx_sms_$id"
                val db = AppDatabase.getDatabase(getApplication())

                // 4. Idempotency check: check if transaction with this stable ID already exists in Room
                val existing = db.transactionDao().getTransactionIncludingDeleted(userId, txId)
                if (existing != null) {
                    // Do NOT revive soft-deleted transactions. Mark SMS processed and ignore duplicate.
                    smsRepository.markSmsProcessed(id, userId)
                    removePendingSmsSuggestion(id, userId)
                    loadDashboardData()
                    onResult(SmsAcceptResult.AlreadyExists)
                    return@withLock
                }

                val finalCategory = customCategory?.trim()?.ifEmpty { null } ?: item.category
                val finalDesc = customDescription?.trim()?.ifEmpty { null }
                    ?: "ثبت هوشمند از پیامک ${item.bankName}"

                val txTitle = when (finalType) {
                    TransactionType.TRANSFER -> "انتقال وجه"
                    TransactionType.INCOME -> "واریز ${item.bankName}"
                    TransactionType.EXPENSE -> "برداشت ${item.bankName}"
                }

                // Single Write: Insert directly into Room database with accurate SMS timestamp and account links

                val entity = TransactionEntity(
                    stringId = txId,
                    userId = userId,
                    title = txTitle,
                    amount = finalAmountRials,
                    amountUnit = AppCurrency.RIAL.name,
                    type = finalType.name,
                    category = finalCategory,
                    accountName = sourceAccount.name.ifBlank { item.bankName.ifBlank { "بدون حساب مشخص" } },
                    description = finalDesc,
                    timestamp = item.timestampMillis,
                    timeFormatted = item.timeText,
                    datePersian = item.dateText,
                    paymentMethod = "OTHER",
                    sourceType = "BANK_SMS",
                    sourceId = id,
                    accountId = if (finalType == TransactionType.TRANSFER) null else sourceAccount.stringId,
                    transferSourceAccountId = if (finalType == TransactionType.TRANSFER) sourceAccount.stringId else null,
                    transferDestinationAccountId = destinationAccount?.stringId
                )

                try {
                    db.transactionDao().insertTransaction(entity)

                    // Mark SMS as processed persistently ONLY after successful DB insert
                    smsRepository.markSmsProcessed(id, userId)

                    // Remove from local queue
                    removePendingSmsSuggestion(id, userId)

                    // Synchronize LocalFinanceRepository caches
                    LocalFinanceRepository.instance.refreshMetadataForCurrentUser()

                    // Refresh dashboard
                    loadDashboardData()

                    onResult(SmsAcceptResult.Success)
                } catch (e: Exception) {
                    // In case of conflict, verify if transaction is present
                    val recheckExisting = db.transactionDao().getTransactionIncludingDeleted(userId, txId)
                    if (recheckExisting != null) {
                        smsRepository.markSmsProcessed(id, userId)
                        removePendingSmsSuggestion(id, userId)
                        loadDashboardData()
                        onResult(SmsAcceptResult.AlreadyExists)
                    } else {
                        // Real failure: leave SMS in queue for retry!
                        onResult(SmsAcceptResult.Failed)
                    }
                }
            }
        }
    }

    fun dismissSmsSuggestion(id: String) {
        val userId = SessionManager.userId
        if (!userId.isNullOrBlank()) {
            smsRepository.markSmsDismissed(id, userId)
        }
        removePendingSmsSuggestion(id, userId)
    }

    /**
     * Real payment / completion action for an obligation.
     * Updates the underlying Room database & repositories, and records financial transaction atomically and idempotently.
     */
    fun markObligationDone(id: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            var resultReported = false
            fun safeResult(success: Boolean) {
                if (!resultReported) {
                    resultReported = true
                    onResult(success)
                }
            }

            val userId = SessionManager.userId
            if (userId.isNullOrBlank()) {
                safeResult(false)
                return@launch
            }

            obligationProcessingMutex.withLock {
                try {
                    val obligation = _uiState.value.upcomingObligations.find { it.id == id }
                    if (obligation == null || obligation.amount <= 0L) {
                        safeResult(false)
                        return@withLock
                    }

                    val currentPdt = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
                    val todayJalali = currentPdt.toFormattedDate()
                    val nowTime = currentPdt.toFormattedTime()

                    // Home obligation completion must not depend on users entering bank/account details.

                    val success = when (obligation.type) {
                        ObligationType.INSTALLMENT -> {
                            val installmentId = obligation.rawId
                            val scheduleItemId = obligation.scheduleItemId
                            val stableSchedId = scheduleItemId?.takeIf { it.isNotBlank() } ?: "sched_${obligation.dueDatePersian.replace('/', '_')}"
                            val txId = "tx_inst_${installmentId}_${stableSchedId}"

                            val finTxEntity = TransactionEntity(
                                stringId = txId,
                                userId = userId,
                                title = "پرداخت قسط: ${obligation.title}",
                                amount = obligation.amount,
                                amountUnit = obligation.amountUnit.name,
                                type = "EXPENSE",
                                category = "اقساط و وام",
                                accountName = "بدون حساب مشخص",
                                description = "پرداخت قسط از داشبورد خانه",
                                timestamp = System.currentTimeMillis(),
                                timeFormatted = nowTime,
                                datePersian = todayJalali,
                                paymentMethod = "OTHER",
                                sourceType = "INSTALLMENT",
                                sourceId = installmentId,
                                accountId = null
                            )

                            val paidSuccessfully = LocalInstallmentRepository.instance.payInstallment(
                                installmentId = installmentId,
                                paymentDate = todayJalali,
                                context = getApplication(),
                                scheduleItemId = scheduleItemId,
                                financialTransaction = finTxEntity
                            )
                            paidSuccessfully
                        }

                        ObligationType.VEHICLE_INSURANCE -> {
                            val insId = obligation.rawId
                            val targetCycle = obligation.dueDatePersian.takeIf { it.isNotBlank() }?.replace('/', '_') ?: todayJalali.replace('/', '_')
                            val txId = "tx_ins_${insId}_${targetCycle}"
                            val finTxEntity = TransactionEntity(
                                stringId = txId,
                                userId = userId,
                                title = "تمدید ${obligation.title}",
                                amount = obligation.amount,
                                amountUnit = obligation.amountUnit.name,
                                type = "EXPENSE",
                                category = "بیمه",
                                accountName = "بدون حساب مشخص",
                                description = "پرداخت و تمدید بیمه خودرو از داشبورد خانه",
                                timestamp = System.currentTimeMillis(),
                                timeFormatted = nowTime,
                                datePersian = todayJalali,
                                paymentMethod = "OTHER",
                                sourceType = "INSURANCE",
                                sourceId = insId,
                                accountId = null
                            )

                            val nextYearDate = IranianDateUtils.createFormattedDate(currentPdt.year + 1, currentPdt.month, currentPdt.day)
                            val renewalResult = VehicleRepository.instance.renewInsurance(
                                insuranceId = insId,
                                newEndDate = nextYearDate,
                                financialTransaction = finTxEntity
                            )
                            renewalResult.isSuccess
                        }

                        else -> {
                            val serviceId = obligation.rawId
                            val txId = "tx_svc_${serviceId}"
                            val finTxEntity = TransactionEntity(
                                stringId = txId,
                                userId = userId,
                                title = "سرویس دوره‌ای: ${obligation.title}",
                                amount = obligation.amount,
                                amountUnit = obligation.amountUnit.name,
                                type = "EXPENSE",
                                category = "سرویس و نگهداری خودرو",
                                accountName = "بدون حساب مشخص",
                                description = "تسویه هزینه سرویس خودرو از داشبورد خانه",
                                timestamp = System.currentTimeMillis(),
                                timeFormatted = nowTime,
                                datePersian = todayJalali,
                                paymentMethod = "OTHER",
                                sourceType = "MAINTENANCE",
                                sourceId = serviceId,
                                accountId = null
                            )

                            val completionResult = VehicleRepository.instance.completeService(
                                serviceId = serviceId,
                                completedDate = todayJalali,
                                financialTransaction = finTxEntity
                            )
                            completionResult.isSuccess
                        }
                    }

                    if (success) {
                        LocalFinanceRepository.instance.refreshMetadataForCurrentUser()
                        loadDashboardData()
                    }
                    safeResult(success)
                } catch (e: Exception) {
                    e.printStackTrace()
                    safeResult(false)
                }
            }
        }
    }

    /**
     * Real completion action for a reminder.
     * Updates the ReminderEntity status in Room database to 'COMPLETED'.
     * The reactive flow then updates Home automatically.
     */
    fun markReminderCompleted(id: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val userId = SessionManager.userId
                var success = false
                if (userId != null) {
                    val db = AppDatabase.getDatabase(getApplication())
                    val updatedRows = db.smartReminderDao().updateStatus(
                        userId = userId,
                        id = id,
                        status = "COMPLETED",
                        updatedAt = System.currentTimeMillis(),
                        completedAt = System.currentTimeMillis()
                    )
                    success = updatedRows > 0
                }
                if (success) {
                    loadDashboardData()
                }
                onResult(success)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }


}
