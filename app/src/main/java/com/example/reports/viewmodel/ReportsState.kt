package com.example.reports.viewmodel

import com.example.reports.domain.*

/**
 * UI State for Darino Financial Analysis Center
 */
data class ReportsState(
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTH,
    val activeTab: ReportTab = ReportTab.FINANCIAL,
    val filter: ReportFilter = ReportFilter(),
    val summary: ReportSummary? = null,
    val income: Long = 30_000_000L,
    val expense: Long = 15_000_000L,
    val saving: Long = 15_000_000L,
    val installmentData: InstallmentSummary? = null,
    val vehicleData: VehicleSummary? = null,
    val insights: List<FinancialInsight> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showFilterSheet: Boolean = false,
    val showExportSheet: Boolean = false,
    val exportSuccessMessage: String? = null
)
