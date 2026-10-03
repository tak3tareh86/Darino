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
    val accentColor: Color,
    val isOverdue: Boolean = false,
    val rawId: String = id
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

data class HomeDashboardState(
    val userName: String = "",
    val todayDate: String = "",
    val monthlyIncome: Long = 0L,
    val formattedIncome: String = "۰ تومان",
    val monthlyExpense: Long = 0L,
    val formattedExpense: String = "۰ تومان",
    val monthlyBalance: Long = 0L,
    val formattedBalance: String = "۰ تومان",
    val savingsRate: Int = 0,
    val upcomingObligations: List<UpcomingObligationItem> = emptyList(),
    val upcomingReminders: List<UpcomingReminderItem> = emptyList(),
    val notificationCount: Int = 0,
    val isAllClear: Boolean = false,
    val isEmptyState: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
