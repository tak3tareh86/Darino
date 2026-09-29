package com.example.reminder.domain

import com.example.reminder.presentation.TodayNotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationStore {
    private val initialList = listOf(
        TodayNotificationItem(
            id = "notif_1",
            title = "سررسید قسط بانک مهر",
            subtitle = "مبلغ: ۳,۰۰۰,۰۰۰ تومان - مهلت پرداخت تا ساعت ۲۴ امروز",
            time = "۰۹:۰۰",
            type = ReminderType.INSTALLMENT,
            isUrgent = true
        ),
        TodayNotificationItem(
            id = "notif_2",
            title = "تمدید بیمه خودرو",
            subtitle = "مهلت تمدید بیمه‌نامه شخص ثالث پژو ۲۰۷ فرا رسیده است",
            time = "۱۰:۰۰",
            type = ReminderType.VEHICLE,
            isUrgent = true
        ),
        TodayNotificationItem(
            id = "notif_3",
            title = "پرداخت قبوض خدماتی",
            subtitle = "قبض برق و گاز شهری - مبلغ ۴۸۰,۰۰۰ تومان",
            time = "۱۱:۰۰",
            type = ReminderType.FINANCE
        )
    )

    private val _notifications = MutableStateFlow<List<TodayNotificationItem>>(initialList)
    val notifications: StateFlow<List<TodayNotificationItem>> = _notifications.asStateFlow()

    private val _hasBeenViewed = MutableStateFlow(false)
    val hasBeenViewed: StateFlow<Boolean> = _hasBeenViewed.asStateFlow()

    fun markAsViewed() {
        _hasBeenViewed.value = true
    }

    fun deleteNotification(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    fun deleteAll() {
        _notifications.value = emptyList()
    }

    fun getUnreadCount(): Int {
        if (_hasBeenViewed.value) return 0
        return _notifications.value.size
    }
}
