package com.example.calendar.viewmodel

import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventType

data class FinancialCalendarState(
    val selectedDate: String = "", // "YYYY/MM/DD" in ASCII
    val currentMonth: Int = 6, // 1..12
    val currentYear: Int = 1405,
    val events: List<FinancialEvent> = emptyList(),
    val monthlyTotal: Long = 0L,
    val eventCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedFilterType: FinancialEventType? = null
) {
    val monthName: String
        get() = com.example.calendar.domain.CalendarDateUtils.getMonthName(currentMonth)

    val currentMonthFormatted: String
        get() = "$monthName ${com.example.util.IranianPhoneUtils.convertDigitsToPersian(currentYear.toString())}"

    val formattedMonthlyTotal: String
        get() {
            val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(monthlyTotal)
            return com.example.util.IranianPhoneUtils.convertDigitsToPersian(formatted) + " تومان"
        }

    val formattedEventCount: String
        get() = "${com.example.util.IranianPhoneUtils.convertDigitsToPersian(eventCount.toString())} مورد"
}
