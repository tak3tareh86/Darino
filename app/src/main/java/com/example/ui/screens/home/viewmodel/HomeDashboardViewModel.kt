package com.example.ui.screens.home.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.screens.home.data.HomeDashboardRepository
import com.example.ui.screens.home.domain.HomeDashboardAggregator
import com.example.ui.screens.home.domain.HomeDashboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HomeDashboardRepository(application)
    private val aggregator = HomeDashboardAggregator(repository)

    private val _uiState = MutableStateFlow(HomeDashboardState(isLoading = true))
    val uiState: StateFlow<HomeDashboardState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            com.example.data.security.SessionManager.sessionState.collect {
                loadDashboardData()
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

    fun markObligationDone(id: String) {
        _uiState.update { current ->
            val updatedList = current.upcomingObligations.filter { it.id != id }
            current.copy(
                upcomingObligations = updatedList,
                isAllClear = current.overdueItems.isEmpty() && updatedList.none { it.relativeDaysText == "امروز" }
            )
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

    fun markReminderCompleted(id: String) {
        _uiState.update { current ->
            val updated = current.upcomingReminders.filter { it.id != id }
            current.copy(upcomingReminders = updated)
        }
    }
}
