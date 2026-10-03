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
import com.example.vehicle.data.VehicleExpenseEntity
import com.example.vehicle.data.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SmsPermissionState {
    NOT_REQUESTED,
    DENIED,
    PERMANENTLY_DENIED,
    GRANTED
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

    init {
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

    fun scanInboxSms() {
        if (!smsRepository.hasSmsPermission()) {
            _smsPermissionState.value = SmsPermissionState.DENIED
            return
        }

        viewModelScope.launch {
            _isScanningSms.value = true
            _smsErrorMessage.value = null
            try {
                val realSmsList = smsRepository.readInboxBankSms()
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
                    val isExpense = newType == TransactionType.EXPENSE
                    val defaultCat = when (newType) {
                        TransactionType.INCOME -> "درآمد و واریز"
                        TransactionType.TRANSFER -> "انتقال بین‌بانکی"
                        TransactionType.EXPENSE -> if (item.category == "درآمد و واریز" || item.category == "انتقال بین‌بانکی") "خرید روزمره" else item.category
                    }
                    item.copy(
                        type = newType,
                        category = defaultCat,
                        formattedAmount = MoneyFormatter.formatSignedToman(item.amount, isExpense = isExpense)
                    )
                } else {
                    item
                }
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val aggregated = aggregator.aggregate()
                _uiState.value = aggregated
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "خطا در بارگذاری اطلاعات داشبورد: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun acceptSmsSuggestion(
        id: String,
        customType: TransactionType? = null,
        customCategory: String? = null,
        customAccount: String? = null,
        customDescription: String? = null,
        destAccount: String? = null
    ) {
        viewModelScope.launch {
            val item = _pendingSmsQueue.value.find { it.id == id } ?: return@launch
            try {
                val db = AppDatabase.getDatabase(getApplication())
                val userId = SessionManager.userId ?: "user_default"

                val finalType = customType ?: item.type
                val finalCategory = customCategory?.trim()?.ifEmpty { null } ?: item.category
                val finalAccount = customAccount?.trim()?.ifEmpty { null } ?: item.bankName
                val finalDesc = customDescription?.trim()?.ifEmpty { null } ?: buildString {
                    append("ثبت هوشمند از پیامک ")
                    append(finalAccount)
                    if (finalType == TransactionType.TRANSFER && !destAccount.isNullOrBlank()) {
                        append(" به $destAccount")
                    }
                }

                // Add to Room transactions database
                db.transactionDao().insertTransaction(
                    TransactionEntity(
                        userId = userId,
                        amount = item.amount,
                        type = finalType.name,
                        category = finalCategory,
                        accountName = finalAccount,
                        description = finalDesc,
                        timestamp = System.currentTimeMillis(),
                        timeFormatted = item.timeText
                    )
                )

                // Add to LocalFinanceRepository so FinancialScreen and TransactionsScreen update immediately
                val catObj = com.example.ui.screens.finance.model.FinanceDefaultCategories.allDefaultCategories.find {
                    it.title.contains(finalCategory, ignoreCase = true)
                } ?: TransactionCategory(
                    id = "cat_gen_${System.currentTimeMillis()}",
                    title = finalCategory,
                    iconEmoji = when (finalType) {
                        TransactionType.EXPENSE -> "🛍️"
                        TransactionType.INCOME -> "💰"
                        TransactionType.TRANSFER -> "🔄"
                    },
                    accentColor = when (finalType) {
                        TransactionType.EXPENSE -> Color(0xFFEF4444)
                        TransactionType.INCOME -> Color(0xFF10B981)
                        TransactionType.TRANSFER -> Color(0xFF3B82F6)
                    },
                    type = finalType
                )

                val finTx = TransactionItemData(
                    id = "tx_sms_${System.currentTimeMillis()}",
                    title = when (finalType) {
                        TransactionType.EXPENSE -> "برداشت: $finalCategory"
                        TransactionType.INCOME -> "واریز: $finalCategory"
                        TransactionType.TRANSFER -> "انتقال: $finalAccount"
                    },
                    amount = item.amount,
                    type = finalType,
                    category = catObj,
                    datePersian = item.dateText,
                    timePersian = item.timeText,
                    accountName = finalAccount,
                    description = finalDesc
                )

                LocalFinanceRepository.instance.addTransaction(finTx)

                // Mark SMS as processed persistently so it is never re-imported
                smsRepository.markSmsProcessed(id)

                // Remove from local queue
                _pendingSmsQueue.update { list -> list.filter { it.id != id } }

                // Refresh dashboard to reflect new balance & totals immediately
                loadDashboardData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismissSmsSuggestion(id: String) {
        // Mark SMS as dismissed persistently so it is never re-imported
        smsRepository.markSmsDismissed(id)
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

    fun dismissOverdueAlert(id: String) {
        _uiState.update { current ->
            val updated = current.overdueItems.filter { it.id != id }
            current.copy(
                overdueItems = updated,
                isAllClear = updated.isEmpty() && current.upcomingObligations.none { it.relativeDaysText == "امروز" }
            )
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
