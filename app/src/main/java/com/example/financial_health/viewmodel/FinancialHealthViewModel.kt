package com.example.financial_health.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.domain.CalendarDateUtils
import com.example.data.database.TransactionEntity
import com.example.data.database.VehicleServiceEntity
import com.example.financial_health.data.FinancialHealthRepository
import com.example.financial_health.domain.FinancialAnalyzerEngine
import com.example.financial_health.domain.FinancialHealthStatus
import com.example.financial_health.domain.MonthlyPressureData
import com.example.financial_health.domain.NextMonthPrediction
import com.example.ui.screens.installments.data.LocalInstallmentRepository
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.screens.installments.model.InstallmentStatus
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class FinancialHealthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinancialHealthRepository(application)
    private val installmentRepository = LocalInstallmentRepository.instance
    private val operationError = MutableStateFlow<String?>(null)

    private data class MonthRange(val start: Long, val end: Long, val label: String)

    private fun monthRange(offset: Int): MonthRange {
        val today = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        var year = today.year
        var month = today.month + offset
        while (month < 1) { month += 12; year -= 1 }
        while (month > 12) { month -= 12; year += 1 }
        val start = PersianCalendarHelper.jalaliToEpochMillis(year, month, 1, 0, 0)
        var nextYear = year
        var nextMonth = month + 1
        if (nextMonth > 12) { nextMonth = 1; nextYear += 1 }
        val end = PersianCalendarHelper.jalaliToEpochMillis(nextYear, nextMonth, 1, 0, 0)
        val names = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
        return MonthRange(start, end, names[month - 1])
    }

    private fun dueMillis(date: String): Long? {
        val parsed = CalendarDateUtils.parseJalali(date) ?: return null
        return runCatching { PersianCalendarHelper.jalaliToEpochMillis(parsed.first, parsed.second, parsed.third, 9, 0) }.getOrNull()
    }

    private fun isUnpaid(status: InstallmentStatus) = status != InstallmentStatus.PAID && status != InstallmentStatus.COMPLETED

    private fun hasCompleteSchedule(item: InstallmentItem): Boolean {
        if (item.remainingInstallments <= 0 || item.status == InstallmentStatus.PAID || item.status == InstallmentStatus.COMPLETED) return true
        val expectedCount = item.totalInstallments
        if (expectedCount <= 0 || item.paymentHistory.size < expectedCount) return false
        val validScheduleNumbers = item.paymentHistory
            .filter { payment -> payment.amount > 0L && dueMillis(payment.dueDate) != null }
            .map { it.installmentNumber }
            .filter { it > 0 }
            .distinct()
        return validScheduleNumbers.size >= expectedCount
    }

    private fun dayStartMillis(epochMillis: Long): Long {
        val date = PersianCalendarHelper.fromEpochMillis(epochMillis)
        return PersianCalendarHelper.jalaliToEpochMillis(date.year, date.month, date.day, 0, 0)
    }

    private fun scheduledAmount(items: List<InstallmentItem>, range: MonthRange, includePaid: Boolean = false): Long = items.sumOf { item ->
        item.paymentHistory.filter { payment ->
            (includePaid || isUnpaid(payment.status)) && payment.amount > 0L &&
                dueMillis(payment.dueDate)?.let { it >= range.start && it < range.end } == true
        }.sumOf { it.amount }
    }

    private fun scheduledVehicleExpenses(services: List<VehicleServiceEntity>, range: MonthRange): Long = services.sumOf { service ->
        val due = service.dueDate
        if (service.status != "COMPLETED" && service.cost > 0L && due != null && due >= range.start && due < range.end) service.cost else 0L
    }

    private fun activeItems(items: List<InstallmentItem>) =
        items.filter { it.remainingInstallments > 0 && isUnpaid(it.status) }

    private fun historicalData(transactions: List<TransactionEntity>, installments: List<InstallmentItem>): List<MonthlyPressureData> {
        if (activeItems(installments).any { !hasCompleteSchedule(it) }) return emptyList()
        val result = mutableListOf<MonthlyPressureData>()
        for (offset in -5..0) {
            val range = monthRange(offset)
            val income = transactions.filter {
                it.type == "INCOME" && it.timestamp >= range.start && it.timestamp < range.end && it.amount > 0L
            }.sumOf { it.amount }
            if (income <= 0L) continue
            val installmentDue = scheduledAmount(installments, range, includePaid = true)
            val pressure = FinancialAnalyzerEngine.calculateFinancialPressure(installmentDue, income) ?: continue
            result += MonthlyPressureData(range.label, pressure, installmentDue, income)
        }
        return result
    }

    val uiState: StateFlow<FinancialHealthState> = combine(
        repository.profileFlow, repository.transactionsFlow, installmentRepository.installments, repository.vehicleServicesFlow, operationError
    ) { profile, transactions, allInstallments, vehicleServices, currentError ->
        val currentMonth = monthRange(0)
        val previousMonth = monthRange(-1)
        val active = activeItems(allInstallments)
        // An empty active list is a valid zero-commitment state. Any active installment must have a complete, dated, positive-amount schedule.
        val hasCompleteInstallmentSchedule = active.all(::hasCompleteSchedule)
        val scheduledCurrentMonth = if (hasCompleteInstallmentSchedule) {
            scheduledAmount(allInstallments, currentMonth, includePaid = true)
        } else 0L
        val nextMonthRange = monthRange(1)
        val scheduledNextMonth = if (hasCompleteInstallmentSchedule) scheduledAmount(active, nextMonthRange) else 0L
        val nextMonthVehicleExpenses = scheduledVehicleExpenses(vehicleServices, nextMonthRange)
        val hasNextMonthVehicleExpenseAmount = vehicleServices.any { service ->
            service.status != "COMPLETED" && service.cost > 0L && service.dueDate?.let { it >= nextMonthRange.start && it < nextMonthRange.end } == true
        }
        val income = profile.monthlyIncome.coerceAtLeast(0L)
        val fixedExpenses = profile.fixedExpenses.coerceAtLeast(0L)
        val monthlyInstallments = scheduledCurrentMonth
        val totalCommitments = (monthlyInstallments + fixedExpenses).coerceAtLeast(0L)
        // Do not publish a definitive pressure score when required user data or any active schedule is missing.
        val pressure = if (income > 0L && profile.isConfigured && hasCompleteInstallmentSchedule) {
            FinancialAnalyzerEngine.calculateFinancialPressure(totalCommitments, income)
        } else null
        val history = historicalData(transactions, allInstallments)
        val currentHistoryPressure = history.lastOrNull { it.monthName == currentMonth.label }?.pressurePercentage
        val previousPressure = history.lastOrNull { it.monthName == previousMonth.label }?.pressurePercentage
        val status = pressure?.let { FinancialAnalyzerEngine.calculateHealthStatus(it) } ?: FinancialHealthStatus.MEDIUM

        val unpaidPayments = active.flatMap { item ->
            item.paymentHistory.filter { isUnpaid(it.status) && it.amount > 0L }.mapNotNull { payment ->
                dueMillis(payment.dueDate)?.let { Triple(item, payment, it) }
            }
        }
        val now = System.currentTimeMillis()
        val nearest = unpaidPayments.filter { it.third >= dayStartMillis(now) }.minByOrNull { it.third }
            ?: unpaidPayments.filter { it.third < dayStartMillis(now) }.maxByOrNull { it.third }
        val itemDueDates = active.mapNotNull { item ->
            dueMillis(item.nextPaymentDate)?.let { item to it }
        }
        val todayStart = dayStartMillis(now)
        val nearestItemFallback = itemDueDates.filter { it.second >= todayStart }.minByOrNull { it.second }
            ?: itemDueDates.filter { it.second < todayStart }.maxByOrNull { it.second }
        val nearestDueMillis = nearest?.third ?: nearestItemFallback?.second
        val nearestTitle = nearest?.first?.title ?: nearestItemFallback?.first?.title
        val daysToNearest = nearestDueMillis?.let { ((dayStartMillis(it) - todayStart) / 86_400_000L).toInt() } ?: 0
        val largest = active.flatMap { item ->
            item.paymentHistory.filter { isUnpaid(it.status) && it.amount > 0L }.map { item to it }
        }.maxByOrNull { it.second.amount }

        val nextMonth = NextMonthPrediction(
            installments = scheduledNextMonth, fixedExpenses = fixedExpenses, reminders = 0L, vehicleExpenses = nextMonthVehicleExpenses,
            installmentsAmountRecorded = hasCompleteInstallmentSchedule, fixedExpensesAmountRecorded = profile.isConfigured, remindersAmountRecorded = false,
            vehicleExpensesAmountRecorded = hasNextMonthVehicleExpenseAmount
        )
        val insights = FinancialAnalyzerEngine.generateInsights(
            income = income, installments = monthlyInstallments, fixedExpenses = fixedExpenses,
            currentPressure = currentHistoryPressure, previousMonthPressure = previousPressure, nextMonthPrediction = nextMonth,
            installmentDataComplete = hasCompleteInstallmentSchedule
        )
        val formatter = DecimalFormat("#,###")
        FinancialHealthState(
            monthlyIncome = income, totalInstallments = monthlyInstallments, fixedExpenses = fixedExpenses,
            totalCommitments = totalCommitments, pressurePercentage = pressure ?: 0f, healthStatus = status,
            hasIncomeData = income > 0L, hasFixedExpensesData = profile.isConfigured,
            incomeFromTransactions = !profile.isConfigured && income > 0L,
            hasInstallmentScheduleData = hasCompleteInstallmentSchedule,
            hasPreviousMonthData = previousPressure != null && currentHistoryPressure != null,
            currentMonthComparisonPressure = currentHistoryPressure, insights = insights,
            isLoading = false, errorMessage = currentError, activeInstallmentsCount = active.size,
            totalInstallmentsMonthlyFormatted = if (hasCompleteInstallmentSchedule) formatter.format(scheduledCurrentMonth) + " تومان" else "برنامه بعضی اقساط ناقص است",
            nearestDueDateDays = daysToNearest, nearestDueDateTitle = nearestTitle ?: "سررسیدی ثبت نشده",
            hasNearestDueDate = nearestDueMillis != null, largestInstallmentTitle = largest?.first?.title ?: "قسطی ثبت نشده",
            largestInstallmentAmount = largest?.second?.amount ?: 0L, hasLargestInstallment = largest != null,
            nextMonthPrediction = nextMonth, previousMonthPressure = previousPressure ?: 0f, historicalPressureList = history
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialHealthState())

    fun updateMonthlyIncome(newIncome: Long) {
        viewModelScope.launch {
            runCatching { repository.updateIncome(newIncome) }
                .onSuccess { operationError.value = null }
                .onFailure { operationError.value = it.message ?: "ذخیره درآمد انجام نشد." }
        }
    }

    fun updateFixedExpenses(newFixed: Long) {
        viewModelScope.launch {
            runCatching { repository.updateFixedExpenses(newFixed) }
                .onSuccess { operationError.value = null }
                .onFailure { operationError.value = it.message ?: "ذخیره هزینه ثابت انجام نشد." }
        }
    }

    fun saveProfile(income: Long, installments: Long, fixed: Long) {
        viewModelScope.launch {
            runCatching { repository.saveProfile(income, installments, fixed) }
                .onSuccess { operationError.value = null }
                .onFailure { operationError.value = it.message ?: "ذخیره اطلاعات مالی انجام نشد." }
        }
    }
}