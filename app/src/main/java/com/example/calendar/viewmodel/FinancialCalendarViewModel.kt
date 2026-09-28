package com.example.calendar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.domain.CalendarDateUtils
import com.example.calendar.domain.CalendarManager
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class CalendarFilterState(
    val year: Int,
    val month: Int,
    val selectedDate: String,
    val searchQuery: String = "",
    val selectedFilterType: FinancialEventType? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class FinancialCalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val calendarManager = CalendarManager(application)

    private val todayParts = CalendarDateUtils.getCurrentJalaliDate()
    private val todayYear = todayParts.first
    private val todayMonth = todayParts.second
    private val todayDay = todayParts.third
    private val todayDateFormatted = CalendarDateUtils.formatJalali(todayYear, todayMonth, todayDay)

    private val _filterState = MutableStateFlow(
        CalendarFilterState(
            year = todayYear,
            month = todayMonth,
            selectedDate = todayDateFormatted
        )
    )

    val uiState: StateFlow<FinancialCalendarState> = combine(
        calendarManager.getAllEvents(),
        _filterState
    ) { allEvents: List<FinancialEvent>, filter: CalendarFilterState ->
        val monthPrefix = "${filter.year.toString().padStart(4, '0')}/${filter.month.toString().padStart(2, '0')}"

        // Filter events for current month to compute monthly totals & count
        val monthEvents = allEvents.filter {
            CalendarDateUtils.normalizeDate(it.date).startsWith(monthPrefix)
        }

        val totalAmount = monthEvents.mapNotNull { it.amount }.sum()
        val count = monthEvents.size

        // Now filter allEvents for display according to search, filterType
        val filteredEvents = allEvents.filter { event ->
            // Search filter
            val matchesSearch = if (filter.searchQuery.isBlank()) {
                true
            } else {
                val q = filter.searchQuery.trim()
                event.title.contains(q, ignoreCase = true) ||
                        event.description.contains(q, ignoreCase = true) ||
                        (event.type == FinancialEventType.INSTALLMENT && ("قسط" in q || "وام" in q)) ||
                        (event.type == FinancialEventType.VEHICLE && ("خودرو" in q || "بیمه" in q || "روغن" in q)) ||
                        (event.type == FinancialEventType.REMINDER && ("یادآور" in q || "یادآوری" in q)) ||
                        (event.type == FinancialEventType.EXPENSE && ("هزینه" in q || "قبض" in q || "مالی" in q))
            }

            // Category filter
            val matchesType = filter.selectedFilterType == null || event.type == filter.selectedFilterType

            matchesSearch && matchesType
        }

        FinancialCalendarState(
            selectedDate = filter.selectedDate,
            currentMonth = filter.month,
            currentYear = filter.year,
            events = filteredEvents,
            monthlyTotal = totalAmount,
            eventCount = count,
            isLoading = filter.isLoading,
            errorMessage = filter.errorMessage,
            searchQuery = filter.searchQuery,
            selectedFilterType = filter.selectedFilterType
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialCalendarState(
            selectedDate = todayDateFormatted,
            currentMonth = todayMonth,
            currentYear = todayYear
        )
    )

    fun selectDate(date: String) {
        val normalized = CalendarDateUtils.normalizeDate(date)
        val parsed = CalendarDateUtils.parseJalali(date)
        _filterState.update { current ->
            if (parsed != null) {
                current.copy(
                    selectedDate = normalized,
                    year = parsed.first,
                    month = parsed.second
                )
            } else {
                current.copy(selectedDate = normalized)
            }
        }
    }

    fun previousMonth() {
        _filterState.update { current ->
            val (newYear, newMonth) = if (current.month == 1) {
                Pair(current.year - 1, 12)
            } else {
                Pair(current.year, current.month - 1)
            }
            val newSelectedDate = CalendarDateUtils.formatJalali(newYear, newMonth, 1)
            current.copy(
                year = newYear,
                month = newMonth,
                selectedDate = newSelectedDate
            )
        }
    }

    fun nextMonth() {
        _filterState.update { current ->
            val (newYear, newMonth) = if (current.month == 12) {
                Pair(current.year + 1, 1)
            } else {
                Pair(current.year, current.month + 1)
            }
            val newSelectedDate = CalendarDateUtils.formatJalali(newYear, newMonth, 1)
            current.copy(
                year = newYear,
                month = newMonth,
                selectedDate = newSelectedDate
            )
        }
    }

    fun jumpToToday() {
        val (y, m, d) = CalendarDateUtils.getCurrentJalaliDate()
        val todayStr = CalendarDateUtils.formatJalali(y, m, d)
        _filterState.update { current ->
            current.copy(
                year = y,
                month = m,
                selectedDate = todayStr
            )
        }
    }

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun setFilterType(type: FinancialEventType?) {
        _filterState.update { it.copy(selectedFilterType = type) }
    }

    fun addEvent(event: FinancialEvent) {
        viewModelScope.launch {
            calendarManager.addEvent(event)
        }
    }

    fun updateEvent(event: FinancialEvent) {
        viewModelScope.launch {
            calendarManager.updateEvent(event)
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            calendarManager.deleteEvent(id)
        }
    }

    fun toggleEventStatus(event: FinancialEvent) {
        viewModelScope.launch {
            calendarManager.toggleStatus(event)
        }
    }
}
