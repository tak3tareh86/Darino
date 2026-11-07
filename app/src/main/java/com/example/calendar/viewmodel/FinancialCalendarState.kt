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
            return com.example.util.MoneyFormatter.formatExact(
                com.example.util.MoneyAmount.of(
                    monthlyTotal,
                    com.example.ui.screens.settings.model.AppCurrency.RIAL
                )
            )
        }

    val formattedEventCount: String
        get() = "${com.example.util.IranianPhoneUtils.convertDigitsToPersian(eventCount.toString())} مورد"
}
