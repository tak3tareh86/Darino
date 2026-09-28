package com.example.reports.domain

import androidx.annotation.DrawableRes
import com.example.R

/**
 * Report Period enum for time-based filtering
 */
enum class ReportPeriod(val title: String) {
    WEEK("هفته"),
    MONTH("ماه"),
    YEAR("سال"),
    CUSTOM("بازه دلخواه")
}

/**
 * Main Reports Navigation Tabs
 */
enum class ReportTab(val title: String, val iconRes: Int) {
    FINANCIAL("گزارش مالی", R.drawable.img_3d_chart),
    INSTALLMENTS("گزارش اقساط", R.drawable.img_3d_installment),
    VEHICLE("گزارش خودرو", R.drawable.img_3d_car),
    INSIGHTS("تحلیل هوشمند", R.drawable.img_3d_analytics)
}

/**
 * Category breakdown item for expenses
 */
data class CategoryExpenseItem(
    val categoryName: String,
    val amount: Long,
    val percentage: Int,
    val colorHex: Long,
    @DrawableRes val iconRes: Int,
    val transactionCount: Int = 1
)

/**
 * Monthly comparison bar data
 */
data class MonthlyExpenseBar(
    val monthName: String,
    val expenseAmount: Long,
    val incomeAmount: Long,
    val isCurrentMonth: Boolean = false
)

/**
 * Trend point for expense curve
 */
data class ExpenseTrendPoint(
    val label: String,
    val amount: Long,
    val formattedAmount: String
)

/**
 * Status of individual installment
 */
enum class InstallmentStatus(val label: String) {
    ACTIVE("فعال"),
    OVERDUE("عقب افتاده"),
    PAID_THIS_MONTH("پرداخت شده")
}

/**
 * Single installment detail item
 */
data class InstallmentReportItem(
    val id: String,
    val title: String,
    val bankOrOrg: String,
    val monthlyAmount: Long,
    val totalAmount: Long,
    val paidMonths: Int,
    val totalMonths: Int,
    val remainingAmount: Long,
    val nextDueDateDays: Int,
    val status: InstallmentStatus
)

/**
 * Installment Summary metrics
 */
data class InstallmentSummary(
    val activeCount: Int,
    val totalMonthlyPayment: Long,
    val totalRemainingDebt: Long,
    val nearestDueDateDays: Int,
    val nearestDueDateTitle: String,
    val largestInstallmentTitle: String,
    val largestInstallmentAmount: Long,
    val paidCount: Int,
    val remainingCount: Int,
    val overdueCount: Int,
    val items: List<InstallmentReportItem>
)

/**
 * Vehicle cost breakdown category
 */
data class VehicleCostItem(
    val categoryName: String,
    val amount: Long,
    val percentage: Int,
    val colorHex: Long,
    @DrawableRes val iconRes: Int
)

/**
 * Single vehicle item
 */
data class VehicleDetailItem(
    val id: String,
    val name: String,
    val plateNumber: String,
    val monthlyCost: Long,
    val yearlyCost: Long,
    @DrawableRes val iconRes: Int
)

/**
 * Vehicle Summary metrics
 */
data class VehicleSummary(
    val totalVehicles: Int,
    val currentMonthCost: Long,
    val yearlyCost: Long,
    val topCostCategory: String,
    val topCostAmount: Long,
    val monthlyChangePercent: Int, // e.g. +15
    val isCostIncreased: Boolean,
    val costBreakdown: List<VehicleCostItem>,
    val vehicles: List<VehicleDetailItem>
)

/**
 * Insight severity and types
 */
enum class InsightSeverity {
    POSITIVE,
    WARNING,
    CRITICAL,
    INFO
}

enum class InsightType {
    EXPENSE_ALERT,
    INSTALLMENT_BURDEN,
    SAVING_OPPORTUNITY,
    VEHICLE_HEALTH,
    BUDGET_CONTROL
}

/**
 * Smart insight item for Darino Insights
 */
data class FinancialInsight(
    val id: String,
    val title: String,
    val description: String,
    val recommendation: String,
    val severity: InsightSeverity,
    val type: InsightType,
    @DrawableRes val iconRes: Int,
    val changePercent: Int? = null
)

/**
 * Complete Report Summary model
 */
data class ReportSummary(
    val period: ReportPeriod,
    val periodLabel: String,
    val totalIncome: Long,
    val totalExpense: Long,
    val totalSaving: Long,
    val savingRatePercent: Int,
    val previousMonthExpense: Long,
    val previousMonthIncome: Long,
    val expenseChangePercent: Int,
    val isExpenseIncreased: Boolean,
    val topExpenseCategory: CategoryExpenseItem,
    val secondExpenseCategory: CategoryExpenseItem,
    val categoryExpenses: List<CategoryExpenseItem>,
    val monthlyBars: List<MonthlyExpenseBar>,
    val trendPoints: List<ExpenseTrendPoint>,
    val installmentSummary: InstallmentSummary,
    val vehicleSummary: VehicleSummary,
    val insights: List<FinancialInsight>
)

/**
 * Filter configuration
 */
data class ReportFilter(
    val period: ReportPeriod = ReportPeriod.MONTH,
    val category: String = "همه",
    val transactionType: String = "همه",
    val costType: String = "همه",
    val customDateStart: String? = null,
    val customDateEnd: String? = null
)
