package com.example.reminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminder.data.LocalReminderRepository
import com.example.reminder.data.ReminderEntity
import com.example.reminder.data.ReminderRepository
import com.example.reminder.domain.PredefinedOffset
import com.example.reminder.domain.ReminderManager
import com.example.reminder.domain.RepeatType
import com.example.reminder.domain.SmartSuggestion
import com.example.reminder.domain.SnoozeOption
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class FilterParams(
    val tab: ReminderTab,
    val filterChip: ReminderFilterChip,
    val searchQuery: String
)

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReminderRepository = LocalReminderRepository(application)
    private val manager: ReminderManager = ReminderManager(application, repository)
    private val notificationRepository = com.example.data.repository.NotificationRepository(application)

    private val _selectedTab = MutableStateFlow(ReminderTab.ALL)
    private val _selectedFilterChip = MutableStateFlow(ReminderFilterChip.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _activeSuggestions = MutableStateFlow(ReminderManager.getSmartSuggestions())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val filterParams = combine(_selectedTab, _selectedFilterChip, _searchQuery) { tab, chip, query ->
        FilterParams(tab, chip, query)
    }

    val uiState: StateFlow<ReminderState> = combine(
        repository.getAllReminders(),
        filterParams,
        _activeSuggestions,
        notificationRepository.getUnreadCount(),
        combine(_isLoading, _errorMessage) { loading, err -> Pair(loading, err) }
    ) { allReminders: List<ReminderEntity>, filters: FilterParams, suggestions: List<SmartSuggestion>, notifCount: Int, (isLoading, error) ->

        val todayPersian = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()

        val activeList = allReminders.filter { it.status.equals("ACTIVE", ignoreCase = true) }
        val completedList = allReminders.filter { it.status.equals("COMPLETED", ignoreCase = true) }
        val missedList = allReminders.filter { it.status.equals("MISSED", ignoreCase = true) }

        val todayReminders = activeList.filter { it.date == todayPersian || it.date.contains(todayPersian.takeLast(5)) }
        val upcomingReminders = activeList.filter { it.date >= todayPersian }

        // Filter by Tab
        val tabFiltered = when (filters.tab) {
            ReminderTab.ALL -> allReminders.filter { !it.status.equals("CANCELLED", ignoreCase = true) }
            ReminderTab.TODAY -> todayReminders
            ReminderTab.FINANCE -> allReminders.filter { it.type.equals("FINANCE", ignoreCase = true) }
            ReminderTab.INSTALLMENT -> allReminders.filter { it.type.equals("INSTALLMENT", ignoreCase = true) }
            ReminderTab.VEHICLE -> allReminders.filter {
                it.type.equals("VEHICLE", ignoreCase = true) ||
                it.type.equals("MAINTENANCE", ignoreCase = true) ||
                it.type.equals("INSURANCE", ignoreCase = true) ||
                it.type.equals("FUEL", ignoreCase = true)
            }
            ReminderTab.PERSONAL -> allReminders.filter {
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
        val finalFiltered = if (filters.searchQuery.isNotBlank()) {
            chipFiltered.filter {
                it.title.contains(filters.searchQuery, ignoreCase = true) ||
                it.description.contains(filters.searchQuery, ignoreCase = true)
            }
        } else {
            chipFiltered
        }

        val nearest = activeList.sortedBy { it.date + it.time }.firstOrNull()

        ReminderState(
            reminders = finalFiltered,
            todayReminders = todayReminders,
            upcomingReminders = upcomingReminders,
            completedReminders = completedList,
            missedReminders = missedList,
            selectedTab = filters.tab,
            selectedFilterChip = filters.filterChip,
            searchQuery = filters.searchQuery,
            notificationCount = notifCount,
            todayCount = todayReminders.size.coerceAtLeast(1),
            thisWeekCount = activeList.size.coerceAtLeast(3),
            nearestReminder = nearest,
            smartSuggestions = suggestions,
            isLoading = isLoading,
            errorMessage = error
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
        offsets: List<PredefinedOffset> = listOf(PredefinedOffset.AT_TIME),
        repeatType: RepeatType = RepeatType.NONE,
        repeatInterval: Int = 1
    ) {
        viewModelScope.launch {
            try {
                manager.createReminder(reminder, offsets, repeatType, repeatInterval)
            } catch (e: Exception) {
                _errorMessage.value = "خطا در ثبت یادآور: ${e.localizedMessage}"
            }
        }
    }

    fun updateReminder(
        reminder: ReminderEntity,
        offsets: List<PredefinedOffset> = listOf(PredefinedOffset.AT_TIME),
        repeatType: RepeatType = RepeatType.NONE,
        repeatInterval: Int = 1
    ) {
        viewModelScope.launch {
            try {
                manager.updateReminder(reminder, offsets, repeatType, repeatInterval)
            } catch (e: Exception) {
                _errorMessage.value = "خطا در ویرایش یادآور: ${e.localizedMessage}"
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

    fun snoozeReminder(reminderId: String, option: SnoozeOption, customMinutes: Int? = null) {
        viewModelScope.launch {
            try {
                manager.snoozeReminder(reminderId, option, customMinutes)
            } catch (e: Exception) {
                _errorMessage.value = "خطا در تعویق یادآور"
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
                date = suggestion.defaultDate,
                time = suggestion.defaultTime,
                targetKilometer = suggestion.targetKilometer,
                status = "ACTIVE",
                notificationEnabled = true
            )
            val offsets = if (suggestion.type.name == "INSURANCE") {
                listOf(PredefinedOffset.BEFORE_7_DAYS, PredefinedOffset.BEFORE_1_DAY, PredefinedOffset.AT_TIME)
            } else {
                listOf(PredefinedOffset.BEFORE_1_DAY, PredefinedOffset.AT_TIME)
            }
            createReminder(reminder, offsets)
            _activeSuggestions.value = _activeSuggestions.value.filter { it.id != suggestion.id }
        }
    }

    fun dismissSuggestion(suggestionId: String) {
        _activeSuggestions.value = _activeSuggestions.value.filter { it.id != suggestionId }
    }
}
