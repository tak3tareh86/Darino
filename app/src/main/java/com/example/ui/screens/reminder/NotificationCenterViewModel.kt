package com.example.ui.screens.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.SyncStatus
import com.example.data.database.NotificationLogEntity
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotificationCenterUiState(
    val isRefreshing: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.IDLE
)

class NotificationCenterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotificationRepository(application)

    private val _uiState = MutableStateFlow(NotificationCenterUiState())
    val uiState: StateFlow<NotificationCenterUiState> = _uiState.asStateFlow()

    val notifications: StateFlow<List<NotificationLogEntity>> = repository.getAllNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unreadCount: StateFlow<Int> = repository.getUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    init {
        refresh()
    }

    fun markAsRead(id: Int) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, syncStatus = SyncStatus.SYNCING)
            repository.refreshNotifications()
            _uiState.value = _uiState.value.copy(isRefreshing = false, syncStatus = SyncStatus.SUCCESS)
        }
    }
}
