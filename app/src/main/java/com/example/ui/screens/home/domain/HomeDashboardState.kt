package com.example.ui.screens.home.domain

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.home.components.BottomNavItem

enum class ObligationType {
    INSTALLMENT,
    VEHICLE_INSURANCE,
    BILL,
    OTHER
}

enum class OverdueType {
    INSTALLMENT,
    REMINDER
}

data class UpcomingObligationItem(
    val id: String,
    val title: String,
    val amount: Long,
    val formattedAmount: String,
    val relativeDaysText: String,
    val dueDatePersian: String,
    val type: ObligationType,
    val destinationTab: BottomNavItem,
    @DrawableRes val iconRes: Int,
    val accentColor: Color
)

data class UpcomingReminderItem(
    val id: String,
    val title: String,
    val relativeDaysText: String,
    val timePersian: String = "۰۹:۰۰",
    val typeTitle: String,
    @DrawableRes val iconRes: Int,
    val accentColor: Color
)

data class OverdueItem(
    val id: String,
    val title: String,
    val amount: Long?,
    val formattedAmount: String?,
    val overdueDaysText: String,
    val type: OverdueType,
    val destinationTab: BottomNavItem
)

data class VehicleSummaryData(
    val id: String,
    val name: String,
    val modelYear: String,
    val currentMileage: Int,
    val formattedMileage: String,
    val statusText: String,
    val isNeedsService: Boolean,
    val plate: String
)

data class HomeFinancialInsight(
    val id: String,
    val title: String = "تحلیل دارینو",
    val message: String,
    val subMessage: String = "",
    @DrawableRes val iconRes: Int,
    val accentColor: Color
)

data class HomeDashboardState(
    val userName: String = "کاربر دارینو",
    val todayDate: String = "دوشنبه، ۳۱ شهریور ۱۴۰۵",
    val monthlyIncome: Long = 18_000_000L,
    val formattedIncome: String = "۱۸,۰۰۰,۰۰۰ تومان",
    val monthlyExpense: Long = 9_500_000L,
    val formattedExpense: String = "۹,۵۰۰,۰۰۰ تومان",
    val monthlyBalance: Long = 8_500_000L,
    val formattedBalance: String = "۸,۵۰۰,۰۰۰ تومان",
    val savingsRate: Int = 47, // 47%
    val upcomingObligations: List<UpcomingObligationItem> = emptyList(),
    val upcomingReminders: List<UpcomingReminderItem> = emptyList(),
    val overdueItems: List<OverdueItem> = emptyList(),
    val vehicleSummary: VehicleSummaryData? = null,
    val financialInsight: HomeFinancialInsight? = null,
    val notificationCount: Int = 3,
    val isAllClear: Boolean = false,
    val isEmptyState: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
