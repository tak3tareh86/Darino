package com.example.ui.screens.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.SyncManager
import com.example.data.api.SyncStatus
import com.example.data.database.AppDatabase
import com.example.data.database.ReminderEntity
import com.example.data.repository.ReminderRepository
import com.example.data.security.SessionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ReminderFilterType {
    ALL,
    GENERAL,
    INSTALLMENT,
    VEHICLE,
    INSURANCE
}

data class ReminderListUiState(
    val selectedFilter: ReminderFilterType = ReminderFilterType.ALL,
    val searchQuery: String = "",
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val isRefreshing: Boolean = false,
    val selectedReminderForDetail: ReminderEntity? = null,
    val showCreateDialog: Boolean = false
)

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReminderRepository(application)
    private val syncManager = SyncManager(application, AppDatabase.getDatabase(application))

    private val _uiState = MutableStateFlow(ReminderListUiState())
    val uiState: StateFlow<ReminderListUiState> = _uiState.asStateFlow()

    val sessionState = SessionManager.sessionState

    val reminders: StateFlow<List<ReminderEntity>> = combine(
        repository.getAllReminders(),
        _uiState
    ) { allReminders, state ->
        allReminders.filter { reminder ->
            val matchesFilter = when (state.selectedFilter) {
                ReminderFilterType.ALL -> true
                ReminderFilterType.GENERAL -> reminder.type.equals("GENERAL", ignoreCase = true) || reminder.type.equals("CUSTOM", ignoreCase = true)
                ReminderFilterType.INSTALLMENT -> reminder.type.equals("INSTALLMENT", ignoreCase = true)
                ReminderFilterType.VEHICLE -> reminder.type.equals("VEHICLE", ignoreCase = true) || reminder.type.equals("MAINTENANCE", ignoreCase = true) || reminder.type.equals("FUEL", ignoreCase = true)
                ReminderFilterType.INSURANCE -> reminder.type.equals("INSURANCE", ignoreCase = true)
            }

            val matchesSearch = if (state.searchQuery.isBlank()) {
                true
            } else {
                reminder.title.contains(state.searchQuery, ignoreCase = true) ||
                        reminder.description.contains(state.searchQuery, ignoreCase = true)
            }

            matchesFilter && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Automatically sync on start if user is logged in
        if (SessionManager.accessToken != null) {
            syncNow()
        }
    }

    fun setFilter(filter: ReminderFilterType) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun openDetail(reminder: ReminderEntity?) {
        _uiState.value = _uiState.value.copy(selectedReminderForDetail = reminder)
    }

    fun setCreateDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showCreateDialog = visible)
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, syncStatus = SyncStatus.SYNCING)
            val result = syncManager.syncReminders()
            _uiState.value = _uiState.value.copy(isRefreshing = false, syncStatus = result)
        }
    }

    fun createReminder(
        type: String,
        title: String,
        description: String,
        scheduledDateTime: Long,
        repeatRule: String = "NONE",
        notificationEnabled: Boolean = true,
        smsEnabled: Boolean = false,
        phoneNumber: String? = null,
        sourceType: String? = null,
        sourceId: Int? = null
    ) {
        viewModelScope.launch {
            repository.createReminder(
                type = type,
                title = title,
                description = description,
                scheduledDateTime = scheduledDateTime,
                repeatRule = repeatRule,
                notificationEnabled = notificationEnabled,
                smsEnabled = smsEnabled,
                phoneNumber = phoneNumber,
                sourceType = sourceType,
                sourceId = sourceId
            )
            _uiState.value = _uiState.value.copy(showCreateDialog = false)
        }
    }

    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            _uiState.value = _uiState.value.copy(selectedReminderForDetail = null)
        }
    }

    fun toggleEnabled(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.toggleEnabled(reminder.id, !reminder.enabled)
        }
    }

    fun markCompleted(reminder: ReminderEntity) {
        viewModelScope.launch {
            val isCurrentlyCompleted = reminder.completedAt != null
            repository.markCompleted(reminder.id, !isCurrentlyCompleted)
        }
    }

    fun deleteReminder(reminderId: Int) {
        viewModelScope.launch {
            repository.deleteReminder(reminderId)
            _uiState.value = _uiState.value.copy(selectedReminderForDetail = null)
        }
    }

    fun snoozeReminder(reminderId: Int, minutes: Long = 15) {
        viewModelScope.launch {
            repository.snoozeReminder(reminderId, minutes)
        }
    }

    fun refreshSmsStatus(reminderId: Int) {
        viewModelScope.launch {
            repository.refreshSmsStatus(reminderId)
        }
    }
}
