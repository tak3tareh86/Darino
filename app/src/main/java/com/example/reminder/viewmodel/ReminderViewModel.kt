package com.example.reminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NotificationRepository
import com.example.reminder.data.LocalReminderRepository
import com.example.reminder.data.ReminderEntity
import com.example.reminder.data.ReminderScheduleEntity
import com.example.reminder.data.ReminderRepository
import com.example.reminder.domain.PredefinedOffset
import com.example.reminder.domain.ReminderManager
import com.example.reminder.domain.ReminderPolicyEngine
import com.example.reminder.domain.RepeatType
import com.example.reminder.domain.SmartSuggestion
import com.example.reminder.domain.SnoozeOption
import com.example.ui.screens.installments.data.LocalInstallmentRepository
import com.example.ui.screens.installments.model.InstallmentStatus
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class ReminderEditConfiguration(
    val offsets: List<PredefinedOffset>,
    val repeatType: RepeatType,
    val repeatInterval: Int
)

private data class FilterParams(
    val tab: ReminderTab,
    val filterChip: ReminderFilterChip,
    val searchQuery: String
)

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReminderRepository = LocalReminderRepository(application)
    private val notificationRepository = NotificationRepository(application)
    private val manager: ReminderManager = ReminderManager(application, repository)

    private val _selectedTab = MutableStateFlow(ReminderTab.ALL)
    private val _selectedFilterChip = MutableStateFlow(ReminderFilterChip.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _activeSuggestions = MutableStateFlow(ReminderManager.getSmartSuggestions())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _userMessage = MutableStateFlow<String?>(null)

    private val filterParams = combine(_selectedTab, _selectedFilterChip, _searchQuery) { tab, chip, query ->
        FilterParams(tab, chip, query)
    }

    // Refresh time-sensitive due-date filtering even when the database does not emit.
    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000L)
        }
    }

    private val reminderData = combine(repository.getAllReminders(), repository.getAllSchedules(), clock) { reminders, schedules, now ->
        Triple(reminders, schedules, now)
    }

    val uiState: StateFlow<ReminderState> = combine(
        reminderData,
        filterParams,
        _activeSuggestions,
        _isLoading,
        combine(_errorMessage, _userMessage, notificationRepository.getUnreadCount()) { error, userMsg, unreadCount ->
            Triple(error, userMsg, unreadCount)
        }
    ) { reminderData: Triple<List<ReminderEntity>, List<ReminderScheduleEntity>, Long>, filters: FilterParams, suggestions: List<SmartSuggestion>, isLoading: Boolean, triple: Triple<String?, String?, Int> ->
        val allReminders = reminderData.first
        val allSchedules = reminderData.second
        val now = reminderData.third
        val error = triple.first
        val userMsg = triple.second
        val unreadCount = triple.third
        val todayCalendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val todayPersian = PersianCalendarHelper.fromEpochMillis(now).toFormattedDate()
        val tomorrowCalendar = (todayCalendar.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowPersian = PersianCalendarHelper.fromEpochMillis(tomorrowCalendar.timeInMillis).toFormattedDate()

        val activeList = allReminders.filter { it.status.equals("ACTIVE", ignoreCase = true) }
        val completedList = allReminders.filter { it.status.equals("COMPLETED", ignoreCase = true) }
        val missedList = allReminders.filter { it.status.equals("MISSED", ignoreCase = true) }

        // Installment schedules are stored per payment with stable IDs. Resolve the
        // next unpaid payment first, then read only that payment's saved trigger rows.
        // Never fall back to the loan reminder's date: that field is the due date.
        val installmentReminders = activeList.filter {
            it.sourceType.equals("INSTALLMENT", ignoreCase = true)
        }
        val installmentReminderIds = installmentReminders.map { it.id }.toSet()

        // Alert schedules determine the alert timestamp shown on a card, not whether
        // the underlying obligation belongs in the Upcoming tab. Installment rows are
        // excluded here because their schedules belong to individual payments.
        val nextTriggerByReminder = allSchedules.asSequence()
            .filter {
                it.reminderId !in installmentReminderIds &&
                    it.enabled &&
                    it.triggerDateTime > now
            }
            .groupBy { it.reminderId }
            .mapValues { (_, schedules) -> schedules.minOf { it.triggerDateTime } }

        val installmentRepository = LocalInstallmentRepository.instance
        val installmentItems = installmentRepository.installments.value
        val installmentReminderDisplayTimes = installmentReminders.mapNotNull reminderEntry@{ reminder ->
            val installmentId = reminder.sourceId ?: return@reminderEntry null
            val installment = installmentItems.firstOrNull { it.id == installmentId }
                ?: return@reminderEntry null
            val unpaidPayments = installment.paymentHistory.filter {
                it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED
            }
            val paymentCandidates = unpaidPayments.mapNotNull paymentEntry@{ payment ->
                val dueAt = runCatching {
                    ReminderManager.parsePersianDateTimeToMillis(payment.dueDate, reminder.time)
                }.getOrNull() ?: return@paymentEntry null
                payment to dueAt
            }
            val nextPayment = paymentCandidates
                .filter { it.second > now }
                .minByOrNull { it.second }
                ?.first
                ?: paymentCandidates.minByOrNull { it.second }?.first
                ?: return@reminderEntry null

            val matchingTrigger = allSchedules.asSequence()
                .filter { schedule ->
                    schedule.reminderId == reminder.id &&
                        !schedule.id.startsWith("snooze_") &&
                        schedule.id == java.util.UUID.nameUUIDFromBytes(
                            "installment:$installmentId:${nextPayment.id}:${schedule.offsetValue}:${schedule.offsetUnit}"
                                .toByteArray(Charsets.UTF_8)
                        ).toString()
                }
                .map { it.triggerDateTime }
                .maxOrNull()
            matchingTrigger?.let { reminder.id to it }
        }.toMap()
        val reminderDisplayTimes = nextTriggerByReminder + installmentReminderDisplayTimes

        fun dueTime(reminder: ReminderEntity): Long? =
            runCatching { ReminderManager.parsePersianDateTimeToMillis(reminder.date, reminder.time) }.getOrNull()

        // Upcoming means the original due date/time is still in the future. It must
        // not depend on whether a notification was configured or has already fired.
        val upcomingReminders = activeList.mapNotNull { reminder ->
            val dueAt = dueTime(reminder) ?: return@mapNotNull null
            if (dueAt > now) reminder to dueAt else null
        }.sortedWith(compareBy<Pair<ReminderEntity, Long>>({ it.second }, { it.first.id }))
            .map { it.first }
        val todayReminders = activeList.filter { it.date == todayPersian }
        val tomorrowReminders = activeList.filter { it.date == tomorrowPersian }

        val visibleReminders = allReminders.filterNot { it.status.equals("CANCELLED", ignoreCase = true) }

        // Filter by Tab. Cancelled reminders stay out of every normal list.
        val tabFiltered = when (filters.tab) {
            ReminderTab.ALL -> visibleReminders
            ReminderTab.TODAY -> todayReminders
            ReminderTab.UPCOMING -> upcomingReminders
            ReminderTab.FINANCE -> visibleReminders.filter { it.type.equals("FINANCE", ignoreCase = true) }
            ReminderTab.INSTALLMENT -> visibleReminders.filter { it.type.equals("INSTALLMENT", ignoreCase = true) }
            ReminderTab.VEHICLE -> visibleReminders.filter {
                it.type.equals("VEHICLE", ignoreCase = true) ||
                it.type.equals("MAINTENANCE", ignoreCase = true) ||
                it.type.equals("INSURANCE", ignoreCase = true) ||
                it.type.equals("FUEL", ignoreCase = true)
            }
            ReminderTab.PERSONAL -> visibleReminders.filter {
                it.type.equals("PERSONAL", ignoreCase = true) || it.type.equals("GENERAL", ignoreCase = true)
            }
            ReminderTab.COMPLETED -> completedList
        }

        // Filter by Chip
        val chipFiltered = if (filters.filterChip.typeKey != null) {
            tabFiltered.filter { it.type.equals(filters.filterChip.typeKey, ignoreCase = true) }
        } else {
            tabFiltered
        }

        // Filter by Search Query
        val finalFiltered = (if (filters.searchQuery.isNotBlank()) {
            chipFiltered.filter {
                it.title.contains(filters.searchQuery, ignoreCase = true) ||
                it.description.contains(filters.searchQuery, ignoreCase = true)
            }
        } else {
            chipFiltered
        }).sortedWith(compareBy<ReminderEntity>({ if (it.status.equals("ACTIVE", ignoreCase = true)) 0 else 1 }, { dueTime(it) ?: Long.MAX_VALUE }))

        // The nearest summary card is based on the reminder's actual due date/time,
        // never on notification schedule triggers. This keeps reminders visible after
        // their alerts have fired or been disabled, while excluding already-due items.
        val nearestDueReminder = activeList.mapNotNull { reminder ->
            val dueAt = runCatching {
                ReminderManager.parsePersianDateTimeToMillis(reminder.date, reminder.time)
            }.getOrNull() ?: return@mapNotNull null
            if (dueAt > now) reminder to dueAt else null
        }.sortedWith(
            compareBy<Pair<ReminderEntity, Long>>({ it.second }, { it.first.id })
        ).firstOrNull()

        ReminderState(
            reminders = finalFiltered,
            todayReminders = todayReminders,
            upcomingReminders = upcomingReminders,
            completedReminders = completedList,
            missedReminders = missedList,
            selectedTab = filters.tab,
            selectedFilterChip = filters.filterChip,
            searchQuery = filters.searchQuery,
            notificationCount = unreadCount,
            todayCount = todayReminders.size,
            tomorrowCount = tomorrowReminders.size,
            nearestReminder = nearestDueReminder?.first,
            nearestReminderDueAt = nearestDueReminder?.second,
            reminderDisplayTimes = reminderDisplayTimes,
            smartSuggestions = suggestions,
            isLoading = isLoading,
            errorMessage = error,
            userMessage = userMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReminderState(isLoading = true)
    )

    fun selectTab(tab: ReminderTab) {
        _selectedTab.value = tab
    }

    fun selectFilterChip(chip: ReminderFilterChip) {
        _selectedFilterChip.value = chip
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createReminder(
        reminder: ReminderEntity,
        offsets: List<PredefinedOffset> = emptyList(),
        repeatType: RepeatType = RepeatType.NONE,
        repeatInterval: Int = 1,
        onComplete: ((Result<Int>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val scheduledCount = manager.createReminder(reminder, offsets, repeatType, repeatInterval)
                _userMessage.value = if (scheduledCount > 0) {
                    "یادآور ذخیره شد و آلارم سیستم‌عامل زمان‌بندی شد؛ نمایش اعلان به مجوز اعلان اندروید و فعال بودن کانال اعلان بستگی دارد."
                } else {
                    "یادآور بدون اعلان ثبت شد."
                }
                onComplete?.invoke(Result.success(scheduledCount))
            } catch (e: Exception) {
                val message = e.localizedMessage ?: "خطای نامشخص"
                _errorMessage.value = "خطا در ثبت یادآور: $message"
                onComplete?.invoke(Result.failure(e))
            }
        }
    }

    fun updateReminder(
        reminder: ReminderEntity,
        offsets: List<PredefinedOffset> = emptyList(),
        repeatType: RepeatType = RepeatType.NONE,
        repeatInterval: Int = 1,
        onComplete: ((Result<ReminderEntity>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                manager.updateReminder(reminder, offsets, repeatType, repeatInterval)
                val updated = repository.getReminderById(reminder.id)
                    ?: throw IllegalStateException("یادآور ویرایش شد اما بازیابی اطلاعات آن ممکن نشد.")
                _userMessage.value = "یادآور با موفقیت ویرایش شد."
                onComplete?.invoke(Result.success(updated))
            } catch (e: Exception) {
                val error = e.localizedMessage ?: "خطای نامشخص"
                _errorMessage.value = "خطا در ویرایش یادآور: $error"
                onComplete?.invoke(Result.failure(e))
            }
        }
    }

    suspend fun getReminderEditConfiguration(reminderId: String): ReminderEditConfiguration {
        val schedules = repository.getSchedulesSync(reminderId).filterNot { it.id.startsWith("snooze_") }
        val offsets = schedules.mapNotNull { stored ->
            PredefinedOffset.entries.find {
                it.value == stored.offsetValue &&
                    it.unit.name.equals(stored.offsetUnit, ignoreCase = true)
            }
        }.distinct()
        val repeatType = schedules.firstOrNull()?.repeatType
            ?.let { value -> RepeatType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } }
            ?: RepeatType.NONE
        val repeatInterval = schedules.firstOrNull()?.repeatInterval?.coerceIn(1, 365) ?: 1
        return ReminderEditConfiguration(offsets, repeatType, repeatInterval)
    }

    fun updateInstallmentReminder(
        reminder: ReminderEntity,
        dueTimePersian: String,
        offsets: List<PredefinedOffset>,
        onComplete: ((Result<ReminderEntity>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                require(reminder.sourceType.equals("INSTALLMENT", ignoreCase = true) &&
                    !reminder.sourceId.isNullOrBlank()) {
                    "این یادآور به وام معتبری متصل نیست."
                }
                val installmentRepository = LocalInstallmentRepository.instance
                installmentRepository.init(getApplication())
                withTimeout(15_000L) {
                    installmentRepository.installmentDataAvailable.first { it }
                }
                val installment = installmentRepository.installments.value
                    .firstOrNull { it.id == reminder.sourceId }
                    ?: throw IllegalStateException("اطلاعات وام متصل به این یادآور پیدا نشد. ابتدا صفحه اقساط را باز و دوباره تلاش کنید.")

                val unpaidPayments = installment.paymentHistory.filter {
                    it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED
                }
                val now = System.currentTimeMillis()
                val nextPayment = unpaidPayments
                    .mapNotNull { payment ->
                        val dueAt = runCatching {
                            ReminderManager.parsePersianDateTimeToMillis(payment.dueDate, dueTimePersian)
                        }.getOrNull() ?: return@mapNotNull null
                        if (dueAt > now) payment to dueAt else null
                    }
                    .minByOrNull { it.second }
                    ?.first
                    ?: unpaidPayments.minByOrNull { payment ->
                        runCatching { ReminderManager.parsePersianDateTimeToMillis(payment.dueDate, dueTimePersian) }
                            .getOrNull() ?: Long.MAX_VALUE
                    }
                    ?: throw IllegalStateException("برای این وام قسط پرداخت‌نشده‌ای باقی نمانده است.")
                val linkedReminder = repository.getReminderBySource("INSTALLMENT", installment.id)
                    ?: throw IllegalStateException("یادآور متصل به این وام پیدا نشد؛ برای جلوگیری از ایجاد یادآور تکراری، عملیات متوقف شد.")
                if (linkedReminder.id != reminder.id) {
                    throw IllegalStateException("ارتباط این یادآور با وام تغییر کرده است؛ برای جلوگیری از تغییر رکورد اشتباه، عملیات متوقف شد.")
                }

                manager.syncInstallmentReminder(
                    installmentId = installment.id,
                    title = installment.title,
                    amount = nextPayment.amount,
                    dueDatePersian = nextPayment.dueDate,
                    dueTimePersian = dueTimePersian,
                    selectedOffsets = offsets,
                    paymentDueDates = unpaidPayments.map { it.id to it.dueDate },
                    amountUnit = installment.amountUnit,
                    useSelectedOffsetsExactly = true
                )

                val updated = repository.getReminderById(reminder.id)
                    ?: throw IllegalStateException("یادآور ویرایش شد اما بازیابی اطلاعات آن ممکن نشد.")
                _userMessage.value = if (offsets.isEmpty()) {
                    "تنظیمات یادآور وام ذخیره شد؛ هشدارهای زمان‌بندی‌شده غیرفعال شدند."
                } else {
                    "تنظیمات یادآور وام با موفقیت به‌روزرسانی شد."
                }
                onComplete?.invoke(Result.success(updated))
            } catch (e: Exception) {
                val error = e.localizedMessage ?: "خطای نامشخص"
                _errorMessage.value = "خطا در ویرایش یادآور وام: $error"
                onComplete?.invoke(Result.failure(e))
            }
        }
    }

    fun completeReminder(reminderId: String) {
        viewModelScope.launch {
            try {
                manager.completeReminder(reminderId)
            } catch (e: Exception) {
                _errorMessage.value = "خطا در تکمیل یادآور"
            }
        }
    }

    fun cancelReminder(reminderId: String) {
        viewModelScope.launch {
            try {
                manager.cancelReminder(reminderId)
            } catch (e: Exception) {
                _errorMessage.value = "خطا در لغو یادآور"
            }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            try {
                manager.deleteReminder(reminderId)
            } catch (e: Exception) {
                _errorMessage.value = "خطا در حذف یادآور"
            }
        }
    }

    fun deleteAllCompletedReminders() {
        viewModelScope.launch {
            try {
                val completed = repository.getCompletedReminders().first()
                completed.forEach { reminder -> manager.deleteReminder(reminder.id) }
                _userMessage.value = if (completed.isEmpty()) {
                    "یادآور تکمیل‌شده‌ای برای حذف وجود ندارد."
                } else {
                    completed.size.toString() + " یادآور تکمیل‌شده حذف شد."
                }
            } catch (e: Exception) {
                _errorMessage.value = "خطا در حذف یادآورهای تکمیل‌شده"
            }
        }
    }

    fun snoozeReminder(
        reminderId: String,
        option: SnoozeOption,
        customTargetMillis: Long? = null,
        onResult: ((Result<Long>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val result = manager.snoozeReminder(reminderId, option, customTargetMillis)
                result.onSuccess { triggerMillis ->
                    val relativeTime = PersianCalendarHelper.formatRelativeTimePersian(triggerMillis)
                    val pdt = PersianCalendarHelper.fromEpochMillis(triggerMillis)
                    _userMessage.value = "یادآور تا ${pdt.toFormattedDate()} ساعت ${pdt.toFormattedTime()} ($relativeTime) به تعویق افتاد."
                }.onFailure { err ->
                    _errorMessage.value = err.message ?: "خطا در ثبت تعویق یادآور"
                }
                onResult?.invoke(result)
            } catch (e: Exception) {
                val err = "خطا در تعویق یادآور: ${e.localizedMessage}"
                _errorMessage.value = err
                onResult?.invoke(Result.failure(e))
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun restoreReminder(reminderId: String) {
        viewModelScope.launch {
            try {
                manager.restoreReminder(reminderId)
                _userMessage.value = "یادآور دوباره فعال شد."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "فعال‌سازی دوباره یادآور انجام نشد."
            }
        }
    }

    fun toggleReminderEnabled(reminderId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) {
                    manager.enableReminder(reminderId)
                } else {
                    manager.disableReminder(reminderId)
                }
            } catch (e: Exception) {
                _errorMessage.value = "خطا در تغییر وضعیت یادآور"
            }
        }
    }

    fun acceptSuggestion(suggestion: SmartSuggestion) {
        viewModelScope.launch {
            val reminder = ReminderEntity(
                title = suggestion.title,
                description = suggestion.message,
                type = suggestion.type.name,
                sourceType = suggestion.sourceType ?: "MANUAL",
                sourceId = suggestion.sourceId,
                amount = suggestion.amount,
                amountUnit = suggestion.amountUnit.name,
                date = suggestion.defaultDate,
                time = suggestion.defaultTime,
                targetKilometer = suggestion.targetKilometer,
                status = "ACTIVE",
                notificationEnabled = false,
                fullScreenAlarmEnabled = false
            )
            // Suggestions never activate alert times without an explicit user choice.
            createReminder(reminder, emptyList())
            _activeSuggestions.value = _activeSuggestions.value.filter { it.id != suggestion.id }
        }
    }

    fun dismissSuggestion(suggestionId: String) {
        _activeSuggestions.value = _activeSuggestions.value.filter { it.id != suggestionId }
    }
}
