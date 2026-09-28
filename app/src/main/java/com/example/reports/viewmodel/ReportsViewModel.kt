package com.example.reports.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reports.data.ReportRepository
import com.example.reports.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing state and user actions for Darino Financial Analysis Center.
 */
class ReportsViewModel(
    private val repository: ReportRepository = ReportRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsState(isLoading = true))
    val uiState: StateFlow<ReportsState> = _uiState.asStateFlow()

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Simulate fast local calculation
                delay(150)
                val summary = repository.getReportSummary(_uiState.value.filter)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        summary = summary,
                        income = summary.totalIncome,
                        expense = summary.totalExpense,
                        saving = summary.totalSaving,
                        installmentData = summary.installmentSummary,
                        vehicleData = summary.vehicleSummary,
                        insights = summary.insights
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "خطا در بارگذاری گزارشات مالی: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun selectPeriod(period: ReportPeriod) {
        val newFilter = _uiState.value.filter.copy(period = period)
        _uiState.update { it.copy(selectedPeriod = period, filter = newFilter) }
        loadReports()
    }

    fun selectTab(tab: ReportTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun openFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = true) }
    }

    fun closeFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = false) }
    }

    fun applyFilter(filter: ReportFilter) {
        _uiState.update {
            it.copy(
                filter = filter,
                selectedPeriod = filter.period,
                showFilterSheet = false
            )
        }
        loadReports()
    }

    fun openExportSheet() {
        _uiState.update { it.copy(showExportSheet = true) }
    }

    fun closeExportSheet() {
        _uiState.update { it.copy(showExportSheet = false) }
    }

    fun exportPdfMock() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showExportSheet = false,
                    exportSuccessMessage = "فایل PDF گزارش تحلیل مالی با موفقیت صادر و ذخیره شد."
                )
            }
            delay(3500)
            _uiState.update { it.copy(exportSuccessMessage = null) }
        }
    }

    fun exportImageMock() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showExportSheet = false,
                    exportSuccessMessage = "تصویر گزارش تحلیلی در گالری ذخیره و آماده اشتراک‌گذاری شد."
                )
            }
            delay(3500)
            _uiState.update { it.copy(exportSuccessMessage = null) }
        }
    }

    fun clearExportMessage() {
        _uiState.update { it.copy(exportSuccessMessage = null) }
    }
}
