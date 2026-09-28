package com.example.reminder.viewmodel

import com.example.reminder.data.ReminderEntity
import com.example.reminder.domain.ReminderType
import com.example.reminder.domain.SmartSuggestion

enum class ReminderTab(val title: String) {
    ALL("همه"),
    TODAY("امروز"),
    FINANCE("مالی"),
    INSTALLMENT("اقساط"),
    VEHICLE("خودرو"),
    PERSONAL("شخصی"),
    COMPLETED("انجام‌شده")
}

enum class ReminderFilterChip(val title: String, val typeKey: String?) {
    ALL("همه", null),
    PERSONAL("شخصی", "PERSONAL"),
    INSTALLMENT("اقساط", "INSTALLMENT"),
    VEHICLE("خودرو", "VEHICLE"),
    INSURANCE("بیمه", "INSURANCE"),
    MAINTENANCE("سرویس", "MAINTENANCE"),
    FUEL("سوخت", "FUEL")
}

data class ReminderState(
    val reminders: List<ReminderEntity> = emptyList(),
    val todayReminders: List<ReminderEntity> = emptyList(),
    val upcomingReminders: List<ReminderEntity> = emptyList(),
    val completedReminders: List<ReminderEntity> = emptyList(),
    val missedReminders: List<ReminderEntity> = emptyList(),
    val selectedTab: ReminderTab = ReminderTab.ALL,
    val selectedFilterChip: ReminderFilterChip = ReminderFilterChip.ALL,
    val searchQuery: String = "",
    val notificationCount: Int = 0,
    val todayCount: Int = 0,
    val thisWeekCount: Int = 0,
    val nearestReminder: ReminderEntity? = null,
    val smartSuggestions: List<SmartSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
