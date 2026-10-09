package com.example.ui.screens.home.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.TransactionEntity
import com.example.data.security.SessionManager
import com.example.data.security.SessionState
import com.example.data.sms.BankSmsRepository
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

    private val smsProcessingMutex = Mutex()

    private fun findMatchingAccount(
        accounts: List<com.example.data.database.AccountEntity>,
        targetNameOrId: String?,
        bankNameHint: String?
    ): com.example.data.database.AccountEntity? {
        if (accounts.isEmpty()) return null

        // 1. If targetNameOrId is specified (from user selection or card/account number)
        if (!targetNameOrId.isNullOrBlank()) {
            val trimmed = targetNameOrId.trim()

            // Exact match on account stringId
            val idMatches = accounts.filter { it.stringId == trimmed }
            if (idMatches.size == 1) return idMatches.first()

            // Exact match on account name (case-insensitive)
            val nameMatches = accounts.filter { it.name.equals(trimmed, ignoreCase = true) }
            if (nameMatches.size == 1) return nameMatches.first()

            // Safe masked account / card number matching without loose contains
            val targetDigits = trimmed.filter { it.isDigit() }
            if (targetDigits.length >= 4) {
                val cardMatches = accounts.filter { acc ->
                    val accDigits = acc.accountNumberMasked?.filter { it.isDigit() } ?: ""
                    when {
                        accDigits.length >= 16 && targetDigits.length >= 16 -> accDigits == targetDigits
                        accDigits.length >= 4 -> accDigits.takeLast(4) == targetDigits.takeLast(4)
                        else -> false
                    }
                }
                // Allowed ONLY if the match is strictly unique!
                if (cardMatches.size == 1) return cardMatches.first()
            }

            // Exact match on account bankName
            val bankMatches = accounts.filter {
                !it.bankName.isNullOrBlank() && it.bankName.equals(trimmed, ignoreCase = true)
            }
            if (bankMatches.size == 1) return bankMatches.first()
        }

        // 2. If bankNameHint is provided (from SMS bank detection), match normalized bankName ONLY if exact & UNIQUE
        if (!bankNameHint.isNullOrBlank()) {
            val cleanHint = bankNameHint.replace("بانک", "").trim()
            if (cleanHint.isNotBlank()) {
                val matchedByBank = accounts.filter { acc ->
                    val accCleanBank = acc.bankName?.replace("بانک", "")?.trim() ?: ""
                    accCleanBank.equals(cleanHint, ignoreCase = true)
                }
                // Allowed ONLY if the match is exact, valid, and UNIQUE!
                if (matchedByBank.size == 1) {
                    return matchedByBank.first()
                }
            }
        }

        // Under no circumstances auto-select first result, loose partial substring, or single remaining account!
        // Return null so user is prompted to explicitly select the account.
        return null
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
            _isScanningSms.value = true
            _smsErrorMessage.value = null
            try {
                val realSmsList = smsRepository.readInboxBankSms(userId)
                val currentPending = _pendingSmsQueue.value
                val existingIds = currentPending.map { it.id }.toSet()
                val newItems = realSmsList.filterNot { existingIds.contains(it.id) }

                _pendingSmsQueue.value = currentPending + newItems
            } catch (e: Exception) {
                _smsErrorMessage.value = "خطا در خواندن پیامک‌ها: ${e.localizedMessage}"
            } finally {
                _isScanningSms.value = false
            }
        }
    }

    fun updateSmsTransactionType(id: String, newType: TransactionType) {
        _pendingSmsQueue.update { list ->
            list.map { item ->
                if (item.id == id) {
                    val formatted = when {
                        !item.isAmountValid || item.amount <= 0L -> "مبلغ نامشخص"
                        newType == TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(item.amount, isExpense = true)
                        newType == TransactionType.INCOME -> MoneyFormatter.formatSignedToman(item.amount, isExpense = false)
                        newType == TransactionType.TRANSFER -> MoneyFormatter.formatToman(item.amount)
                        else -> MoneyFormatter.formatToman(item.amount)
                    }
                    val isCustomCategory = item.category != "سایر" &&
                            item.category != "خرید روزمره" &&
                            item.category != "درآمد و واریز" &&
                            item.category != "انتقال بین‌بانکی"

                    val finalCat = if (isCustomCategory) {
                        item.category
                    } else {
                        when (newType) {
                            TransactionType.INCOME -> "درآمد و واریز"
                            TransactionType.TRANSFER -> "انتقال بین‌بانکی"
                            TransactionType.EXPENSE -> "خرید روزمره"
                        }
                    }
                    item.copy(
                        type = newType,
                        isTypeUncertain = false,
                        category = finalCat,
                        formattedAmount = formatted
                    )
                } else {
                    item
                }
            }
        }
    }

    /**
     * Preserves user's edited details in the pending SMS queue so that in case of error or
     * re-opening, the user does not lose their typed/selected input.
     */
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
                if (item.id == id) {
                    val finalAmount = amount ?: item.amount
                    val finalType = type ?: item.type
                    val isValid = amount?.let { it > 0L } ?: item.isAmountValid
                    val formatted = if (isValid && finalAmount > 0L) {
                        when (finalType) {
                            TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(finalAmount, isExpense = true)
                            TransactionType.INCOME -> MoneyFormatter.formatSignedToman(finalAmount, isExpense = false)
                            else -> MoneyFormatter.formatToman(finalAmount)
                        }
                    } else "مبلغ نامشخص"

                    item.copy(
                        amount = finalAmount,
                        isAmountValid = isValid,
                        type = finalType,
                        isTypeUncertain = (finalType == null),
                        category = category?.trim()?.ifEmpty { null } ?: item.category,
                        sourceAccount = account?.trim()?.ifEmpty { null } ?: item.sourceAccount,
                        destinationAccount = destAccount?.trim()?.ifEmpty { null } ?: item.destinationAccount,
                        formattedAmount = formatted
                    )
                } else {
                    item
                }
            }
        }
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
                    _pendingSmsQueue.update { list -> list.filter { it.id != id } }
                    onResult(SmsAcceptResult.AlreadyExists)
                    return@withLock
                }

                val item = _pendingSmsQueue.value.find { id == it.id }
                if (item == null) {
                    onResult(SmsAcceptResult.Failed)
                    return@withLock
                }

                // 2. Amount validation: must be strictly positive and valid
                val finalAmount = customAmount ?: item.amount
                if (finalAmount <= 0L || (customAmount == null && !item.isAmountValid)) {
                    onResult(SmsAcceptResult.InvalidAmount)
                    return@withLock
                }

                // 3. Type validation
                val finalType = customType ?: item.type
                if (finalType == null || (customType == null && item.isTypeUncertain)) {
                    onResult(SmsAcceptResult.TypeNotSelected)
                    return@withLock
                }

                val txId = "tx_sms_$id"
                val db = AppDatabase.getDatabase(getApplication())

                // 4. Idempotency check: check if transaction with this stable ID already exists in Room
                val existing = db.transactionDao().getTransactionIncludingDeleted(userId, txId)
                if (existing != null) {
                    // Do NOT revive soft-deleted transactions. Mark SMS processed and ignore duplicate.
                    smsRepository.markSmsProcessed(id, userId)
                    _pendingSmsQueue.update { list -> list.filter { it.id != id } }
                    loadDashboardData()
                    onResult(SmsAcceptResult.AlreadyExists)
                    return@withLock
                }

                val finalCategory = customCategory?.trim()?.ifEmpty { null } ?: item.category
                val finalAccount = customAccount?.trim()?.ifEmpty { null } ?: item.bankName
                val finalDesc = customDescription?.trim()?.ifEmpty { null } ?: buildString {
                    append("ثبت هوشمند از پیامک ")
                    append(finalAccount)
                    if (finalType == TransactionType.TRANSFER && !destAccount.isNullOrBlank()) {
                        append(" به $destAccount")
                    }
                }

                // 5. Resolve accounts strictly from user's active, valid accounts
                val activeAccounts = db.accountDao().getAllAccountsList(userId).filter { it.isActive && it.deletedAt == null }
                val sourceAcc = findMatchingAccount(activeAccounts, customAccount ?: finalAccount, item.bankName)
                if (sourceAcc == null) {
                    // Do NOT auto-select first account and do NOT auto-create account without user action!
                    onResult(SmsAcceptResult.AccountRequired)
                    return@withLock
                }

                val destAcc = if (finalType == TransactionType.TRANSFER) {
                    val matchedDest = findMatchingAccount(
                        activeAccounts.filter { it.stringId != sourceAcc.stringId },
                        destAccount,
                        null
                    )
                    if (matchedDest == null || matchedDest.stringId == sourceAcc.stringId) {
                        // Transfer cannot be registered without a valid distinct destination account!
                        onResult(SmsAcceptResult.DestinationAccountRequired)
                        return@withLock
                    }
                    matchedDest
                } else null

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
                    amount = finalAmount,
                    type = finalType.name,
                    category = finalCategory,
                    accountName = sourceAcc.name,
                    description = finalDesc,
                    timestamp = item.timestampMillis,
                    timeFormatted = item.timeText,
                    datePersian = item.dateText,
                    paymentMethod = "BANK_CARD",
                    sourceType = "BANK_SMS",
                    sourceId = id,
                    accountId = if (finalType != TransactionType.TRANSFER) sourceAcc.stringId else null,
                    transferSourceAccountId = if (finalType == TransactionType.TRANSFER) sourceAcc.stringId else null,
                    transferDestinationAccountId = if (finalType == TransactionType.TRANSFER) destAcc?.stringId else null
                )

                try {
                    db.transactionDao().insertTransaction(entity)

                    // Mark SMS as processed persistently ONLY after successful DB insert
                    smsRepository.markSmsProcessed(id, userId)

                    // Remove from local queue
                    _pendingSmsQueue.update { list -> list.filter { it.id != id } }

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
                        _pendingSmsQueue.update { list -> list.filter { it.id != id } }
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
        _pendingSmsQueue.update { list -> list.filter { it.id != id } }
    }

    /**
     * Real payment / completion action for an obligation.
     * Updates the underlying Room database & repositories, and records financial transaction.
     */
    fun markObligationDone(id: String) {
        viewModelScope.launch {
            try {
                val obligation = _uiState.value.upcomingObligations.find { it.id == id } ?: return@launch
                val currentPdt = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
                val todayJalali = currentPdt.toFormattedDate()
                val nowTime = currentPdt.toFormattedTime()

                when (obligation.type) {
                    ObligationType.INSTALLMENT -> {
                        val installmentId = obligation.rawId
                        // Real payment in Installment repository
                        LocalInstallmentRepository.instance.payInstallment(
                            installmentId = installmentId,
                            paymentDate = todayJalali,
                            context = getApplication()
                        )

                        // Record real expense transaction
                        val finTx = TransactionItemData(
                            id = "tx_inst_pay_${System.currentTimeMillis()}",
                            title = "پرداخت قسط: ${obligation.title}",
                            amount = obligation.amount,
                            type = TransactionType.EXPENSE,
                            category = TransactionCategory(
                                id = "cat_loan",
                                title = "اقساط و وام",
                                iconEmoji = "🏦",
                                accentColor = Color(0xFF2563EB),
                                type = TransactionType.EXPENSE
                            ),
                            datePersian = todayJalali,
                            timePersian = nowTime,
                            accountName = "کارت بانکی",
                            description = "پرداخت قسط از داشبورد خانه"
                        )
                        LocalFinanceRepository.instance.addTransaction(finTx)
                    }

                    ObligationType.VEHICLE_INSURANCE -> {
                        val insId = obligation.rawId
                        val insurance = VehicleRepository.instance.insurances.value.find { it.id == insId }
                        val vehicleId = insurance?.vehicleId ?: VehicleRepository.instance.vehicles.value.firstOrNull()?.id ?: ""

                        // Update underlying insurance policy to next year so it is renewed
                        val nextYearDate = IranianDateUtils.createFormattedDate(currentPdt.year + 1, currentPdt.month, currentPdt.day)
                        VehicleRepository.instance.renewInsurance(insId, nextYearDate)

                        // Record vehicle expense
                        VehicleRepository.instance.addExpense(
                            vehicleId = vehicleId,
                            title = obligation.title,
                            category = VehicleExpenseCategory.INSURANCE,
                            amount = obligation.amount,
                            date = todayJalali,
                            description = "پرداخت و تمدید بیمه از داشبورد خانه"
                        )

                        // Record finance transaction
                        val finTx = TransactionItemData(
                            id = "tx_ins_pay_${System.currentTimeMillis()}",
                            title = obligation.title,
                            amount = obligation.amount,
                            type = TransactionType.EXPENSE,
                            category = TransactionCategory(
                                id = "cat_insurance",
                                title = "بیمه خودرو",
                                iconEmoji = "🛡️",
                                accentColor = Color(0xFFF59E0B),
                                type = TransactionType.EXPENSE
                            ),
                            datePersian = todayJalali,
                            timePersian = nowTime,
                            accountName = "کارت بانکی",
                            description = "پرداخت بیمه از داشبورد خانه"
                        )
                        LocalFinanceRepository.instance.addTransaction(finTx)
                    }

                    else -> {
                        // Periodic vehicle service or other obligations
                        val serviceId = obligation.rawId
                        VehicleRepository.instance.completeService(serviceId, todayJalali)

                        val vId = VehicleRepository.instance.services.value.find { it.id == serviceId }?.vehicleId
                            ?: VehicleRepository.instance.vehicles.value.firstOrNull()?.id ?: ""

                        VehicleRepository.instance.addExpense(
                            vehicleId = vId,
                            title = obligation.title,
                            category = VehicleExpenseCategory.SERVICE,
                            amount = obligation.amount,
                            date = todayJalali,
                            description = "انجام سرویس دوره‌ای از داشبورد خانه"
                        )

                        val finTx = TransactionItemData(
                            id = "tx_ob_pay_${System.currentTimeMillis()}",
                            title = obligation.title,
                            amount = obligation.amount,
                            type = TransactionType.EXPENSE,
                            category = TransactionCategory(
                                id = "cat_other",
                                title = "سایر هزینه‌ها",
                                iconEmoji = "📝",
                                accentColor = Color(0xFF10B981),
                                type = TransactionType.EXPENSE
                            ),
                            datePersian = todayJalali,
                            timePersian = nowTime,
                            accountName = "کارت بانکی",
                            description = "تسویه تعهد از داشبورد خانه"
                        )
                        LocalFinanceRepository.instance.addTransaction(finTx)
                    }
                }

                // Refresh dashboard to reflect updated state from real database
                loadDashboardData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Real completion action for a reminder.
     * Updates the ReminderEntity status in Room database to 'COMPLETED'.
     * The reactive flow then updates Home automatically.
     */
    fun markReminderCompleted(id: String) {
        viewModelScope.launch {
            try {
                val userId = SessionManager.userId
                if (userId != null) {
                    val db = AppDatabase.getDatabase(getApplication())
                    db.smartReminderDao().updateStatus(
                        userId = userId,
                        id = id,
                        status = "COMPLETED",
                        updatedAt = System.currentTimeMillis(),
                        completedAt = System.currentTimeMillis()
                    )
                }
                loadDashboardData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
