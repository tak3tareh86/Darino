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
import com.example.util.MoneyFormatter
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class FinancialHealthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinancialHealthRepository(application)
    private val installmentRepository = LocalInstallmentRepository.instance
    private val operationError = MutableStateFlow<String?>(null)

    // Refresh the current Jalali year at its boundary even when the underlying
    // database flows remain idle while this screen is open.
    private val calendarRefresh = flow {
        while (true) {
            emit(Unit)
            val now = System.currentTimeMillis()
            val jalaliYear = PersianCalendarHelper.fromEpochMillis(now).year
            val nextJalaliYearStart = PersianCalendarHelper.jalaliToEpochMillis(
                jalaliYear + 1, 1, 1, 0, 0
            )
            delay((nextJalaliYearStart - now).coerceIn(1L, 24L * 60L * 60L * 1000L))
        }
    }
    private val refreshedOperationError = combine(operationError, calendarRefresh) { error, _ -> error }

    private data class MonthRange(
        val year: Int,
        val month: Int,
        val start: Long,
        val end: Long,
        val label: String
    )

    private fun monthRange(year: Int, month: Int): MonthRange {
        require(month in 1..12) { "Persian month must be between 1 and 12." }
        val start = PersianCalendarHelper.jalaliToEpochMillis(year, month, 1, 0, 0)
        val nextYear = if (month == 12) year + 1 else year
        val nextMonth = if (month == 12) 1 else month + 1
        val end = PersianCalendarHelper.jalaliToEpochMillis(nextYear, nextMonth, 1, 0, 0)
        return MonthRange(year, month, start, end, PersianCalendarHelper.PERSIAN_MONTH_NAMES[month - 1])
    }

    private fun monthRange(offset: Int): MonthRange {
        val today = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val absoluteMonth = today.month - 1 + offset
        val year = today.year + Math.floorDiv(absoluteMonth, 12)
        val month = Math.floorMod(absoluteMonth, 12) + 1
        return monthRange(year, month)
    }

    private fun dueMillis(date: String): Long? {
        val parsed = CalendarDateUtils.parseJalali(date) ?: return null
        return runCatching { PersianCalendarHelper.jalaliToEpochMillis(parsed.first, parsed.second, parsed.third, 9, 0) }.getOrNull()
    }

    private fun isUnpaid(status: InstallmentStatus) = status != InstallmentStatus.PAID && status != InstallmentStatus.COMPLETED

    private fun parseCurrencyUnit(value: String): AppCurrency? = when (value) {
        AppCurrency.TOMAN.name -> AppCurrency.TOMAN
        AppCurrency.RIAL.name -> AppCurrency.RIAL
        else -> null
    }

    private fun hasCompletePaymentHistory(item: InstallmentItem): Boolean {
        val expectedCount = item.totalInstallments
        if (expectedCount <= 0) return false

        // Every expected installment must have exactly one valid dated, positive-amount schedule row.
        // Counting distinct numbers alone can incorrectly accept out-of-range or duplicate rows.
        val validPayments = item.paymentHistory.filter { payment ->
            payment.amount > 0L &&
                payment.installmentNumber in 1..expectedCount &&
                dueMillis(payment.dueDate) != null &&
                (payment.status != InstallmentStatus.PAID && payment.status != InstallmentStatus.COMPLETED ||
                    payment.paidDate?.let { dueMillis(it) != null } == true)
        }
        val scheduleNumbers = validPayments.map { it.installmentNumber }.toSet()
        return validPayments.size == expectedCount &&
            scheduleNumbers.size == expectedCount &&
            scheduleNumbers.containsAll((1..expectedCount).toSet()) &&
            FinancialAnalyzerEngine.isPaymentScheduleTotalConsistent(
                item.totalAmount,
                validPayments.map { it.amount }
            )
    }

    private fun hasCompleteSchedule(item: InstallmentItem): Boolean {
        // Top-level status can be stale. Only zero remaining installments make a contract inactive.
        if (item.remainingInstallments <= 0) return true
        return hasCompletePaymentHistory(item)
    }

    private fun dayStartMillis(epochMillis: Long): Long {
        val date = PersianCalendarHelper.fromEpochMillis(epochMillis)
        return PersianCalendarHelper.jalaliToEpochMillis(date.year, date.month, date.day, 0, 0)
    }

    private fun scheduledAmount(items: List<InstallmentItem>, range: MonthRange, includePaid: Boolean = false): Long = items.fold(0L) { itemTotal, item ->
        val paymentTotal = item.paymentHistory.filter { payment ->
            (includePaid || isUnpaid(payment.status)) && payment.amount > 0L &&
                dueMillis(payment.dueDate)?.let { it >= range.start && it < range.end } == true
        }.fold(0L) { total, payment -> Math.addExact(total, MoneyAmount.of(payment.amount, payment.amountUnit).asRialsLongExact()) }
        Math.addExact(itemTotal, paymentTotal)
    }

    private fun nextMonthVehicleServices(services: List<VehicleServiceEntity>, range: MonthRange) =
        services.filter { service ->
            service.status != "COMPLETED" && service.cost > 0L &&
                service.dueDate?.let { it >= range.start && it < range.end } == true
        }

    private fun validatedVehicleExpenseAmount(services: List<VehicleServiceEntity>): Long? {
        if (services.isEmpty() || services.any { parseCurrencyUnit(it.costUnit) == null }) return null
        return runCatching {
            services.fold(0L) { total, service ->
                val unit = parseCurrencyUnit(service.costUnit) ?: error("Unknown vehicle expense currency unit")
                Math.addExact(total, MoneyAmount.of(service.cost, unit).asRialsLongExact())
            }
        }.getOrNull()
    }

    private fun activeItems(items: List<InstallmentItem>) =
        // Remaining schedule is authoritative; a stale PAID/COMPLETED contract flag must not hide dues.
        items.filter { it.remainingInstallments > 0 }

    private fun historicalData(
        transactions: List<TransactionEntity>,
        installments: List<InstallmentItem>,
        configuredCurrentMonthIncome: Long?
    ): List<MonthlyPressureData> {
        val result = mutableListOf<MonthlyPressureData>()
        val currentDate = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        // Keep a stable Farvardin-to-Esfand Jalali year axis; future or unavailable
        // months remain explicit empty slots instead of shifting the chart window.
        for (month in 1..12) {
            val range = monthRange(currentDate.year, month)
            val isCurrentMonth = month == currentDate.month
            // Preserve all twelve calendar slots. Missing or incomplete source data is represented
            // explicitly, so the chart never connects non-consecutive months as if data existed.
            val hasIncompleteRelevantSchedule = installments.any { item ->
                val start = dueMillis(item.startDate)
                val end = dueMillis(item.endDate)
                val overlapsMonth = start != null && end != null && start < range.end && end >= range.start
                val contractRangeUnknown = item.remainingInstallments > 0 && (start == null || end == null)
                (overlapsMonth || contractRangeUnknown) && !hasCompletePaymentHistory(item)
            }
            if (hasIncompleteRelevantSchedule) {
                result += MonthlyPressureData(range.label, 0f, 0L, 0L, hasData = false)
                continue
            }

            val income: Long? = if (isCurrentMonth && configuredCurrentMonthIncome != null) {
                configuredCurrentMonthIncome
            } else {
                val monthIncomeTransactions = transactions.filter {
                    it.type == "INCOME" && it.deletedAt == null &&
                        it.timestamp >= range.start && it.timestamp < range.end && it.amount > 0L
                }
                if (monthIncomeTransactions.any { parseCurrencyUnit(it.amountUnit) == null }) {
                    null
                } else {
                    runCatching {
                        monthIncomeTransactions.fold(0L) { total, tx ->
                            val unit = parseCurrencyUnit(tx.amountUnit) ?: error("Unknown income currency unit")
                            Math.addExact(total, MoneyAmount.of(tx.amount, unit).asRialsLongExact())
                        }
                    }.getOrNull()
                }
            }
            if (income == null || income <= 0L) {
                result += MonthlyPressureData(range.label, 0f, 0L, 0L, hasData = false)
                continue
            }

            val installmentDue = runCatching {
                scheduledAmount(installments, range, includePaid = true)
            }.getOrNull()
            val pressure = installmentDue?.let {
                FinancialAnalyzerEngine.calculateFinancialPressure(it, income)
            }
            if (installmentDue == null || pressure == null) {
                result += MonthlyPressureData(range.label, 0f, 0L, 0L, hasData = false)
            } else {
                result += MonthlyPressureData(range.label, pressure, installmentDue, income, hasData = true)
            }
        }
        return result
    }

    private val installmentSnapshotFlow = combine(
        installmentRepository.installments, installmentRepository.installmentDataAvailable
    ) { items, available -> items to available }

    val uiState: StateFlow<FinancialHealthState> = combine(
        repository.profileFlow, repository.transactionsFlow, installmentSnapshotFlow,
        repository.vehicleServicesFlow, refreshedOperationError
    ) { profile, transactions, installmentSnapshot, vehicleServices, currentError ->
        val allInstallments = installmentSnapshot.first
        val installmentDataAvailable = installmentSnapshot.second
        val currentMonth = monthRange(0)
        val previousMonth = monthRange(-1)
        val active = activeItems(allInstallments)
        // An empty active list is a valid zero-commitment state. Any active installment must have a complete, dated, positive-amount schedule.
        val hasCompleteInstallmentSchedule = installmentDataAvailable && active.all(::hasCompleteSchedule)
        // Monthly pressure measures the planned burden of the month, including installments
        // already paid during that month. This keeps the current month comparable to history.
        val scheduledCurrentMonth = if (hasCompleteInstallmentSchedule) {
            scheduledAmount(allInstallments, currentMonth, includePaid = true)
        } else 0L
        // The analysis tile shows only this month's unpaid scheduled installments;
        // the pressure score above continues to include paid rows for month-to-month comparability.
        val currentMonthUnpaidInstallments = if (hasCompleteInstallmentSchedule) {
            scheduledAmount(active, currentMonth)
        } else 0L
        val nextMonthRange = monthRange(1)
        val scheduledNextMonth = if (hasCompleteInstallmentSchedule) scheduledAmount(active, nextMonthRange) else 0L
        val nextMonthVehicleServices = nextMonthVehicleServices(vehicleServices, nextMonthRange)
        // Do not publish a partial vehicle total if any eligible row has an unknown unit
        // or if an exact conversion/aggregation overflows.
        val validatedNextMonthVehicleExpenses = validatedVehicleExpenseAmount(nextMonthVehicleServices)
        val nextMonthVehicleExpenses = validatedNextMonthVehicleExpenses ?: 0L
        val hasNextMonthVehicleExpenseAmount = validatedNextMonthVehicleExpenses != null
        val income = profile.monthlyIncome.coerceAtLeast(0L)
        val fixedExpenses = profile.fixedExpenses.coerceAtLeast(0L)
        val monthlyInstallments = scheduledCurrentMonth
        val totalCommitments = Math.addExact(monthlyInstallments, fixedExpenses).coerceAtLeast(0L)
        // Do not publish a definitive pressure score when required user data or any active schedule is missing.
        val pressure = if (income > 0L && profile.isConfigured && hasCompleteInstallmentSchedule) {
            FinancialAnalyzerEngine.calculateFinancialPressure(totalCommitments, income)
        } else null
        val history = historicalData(
            transactions = transactions,
            installments = allInstallments,
            configuredCurrentMonthIncome = income.takeIf { profile.isConfigured }
        )
        val currentHistoryPressure = history.firstOrNull { it.monthName == currentMonth.label && it.hasData }?.pressurePercentage
        // January has no previous-month point in the current-year chart; do not
        // accidentally compare it with an older year's January/December data.
        val previousPressure = history.firstOrNull { it.monthName == previousMonth.label && it.hasData }?.pressurePercentage
        val status = pressure?.let { FinancialAnalyzerEngine.calculateHealthStatus(it) } ?: FinancialHealthStatus.MEDIUM

        val unpaidPayments = active.flatMap { item ->
            item.paymentHistory.filter { isUnpaid(it.status) && it.amount > 0L }.mapNotNull { payment ->
                dueMillis(payment.dueDate)?.let { Triple(item, payment, it) }
            }
        }
        val now = System.currentTimeMillis()
        val nearest = unpaidPayments.filter { it.third >= dayStartMillis(now) }.minByOrNull { it.third }
            ?: unpaidPayments.filter { it.third < dayStartMillis(now) }.maxByOrNull { it.third }
        // Only a validated, unpaid payment-schedule row is a real due date.
        // Contract-level nextPaymentDate is not a substitute for a missing schedule row.
        val nearestDueMillis = nearest?.third
        val nearestTitle = nearest?.first?.title
        // Compare Jalali calendar days directly instead of dividing epoch milliseconds,
        // which can be off by one around time-zone/DST transitions.
        val todayJalali = PersianCalendarHelper.fromEpochMillis(now)
        val todayEpochDay = PersianCalendarHelper.getJalaliEpochDay(
            todayJalali.year, todayJalali.month, todayJalali.day
        )
        val daysToNearest = nearestDueMillis?.let { due ->
            val dueJalali = PersianCalendarHelper.fromEpochMillis(due)
            (PersianCalendarHelper.getJalaliEpochDay(dueJalali.year, dueJalali.month, dueJalali.day) - todayEpochDay).toInt()
        } ?: 0
        val largest = active.flatMap { item ->
            item.paymentHistory.filter { isUnpaid(it.status) && it.amount > 0L && parseCurrencyUnit(it.amountUnit.name) != null }.map { item to it }
        }.maxByOrNull { (_, payment) -> MoneyAmount.of(payment.amount, payment.amountUnit).asRialsLongExact() }

        val nextMonth = NextMonthPrediction(
            installments = scheduledNextMonth, fixedExpenses = fixedExpenses, reminders = 0L, vehicleExpenses = nextMonthVehicleExpenses,
            installmentsAmountRecorded = hasCompleteInstallmentSchedule, fixedExpensesAmountRecorded = profile.isConfigured, remindersAmountRecorded = false,
            vehicleExpensesAmountRecorded = hasNextMonthVehicleExpenseAmount
        )
        val insights = FinancialAnalyzerEngine.generateInsights(
            income = income, installments = monthlyInstallments, fixedExpenses = fixedExpenses,
            currentPressure = currentHistoryPressure, previousMonthPressure = previousPressure, nextMonthPrediction = nextMonth,
            installmentDataComplete = hasCompleteInstallmentSchedule,
            fixedExpensesRecorded = profile.isConfigured
        )
        val formatter = DecimalFormat("#,###")
        FinancialHealthState(
            monthlyIncome = income, totalInstallments = monthlyInstallments, fixedExpenses = fixedExpenses,
            totalCommitments = totalCommitments, pressurePercentage = pressure ?: 0f, healthStatus = status,
            hasIncomeData = income > 0L, hasFixedExpensesData = profile.isConfigured,
            incomeFromTransactions = !profile.isConfigured && income > 0L,
            profileUpdatedAt = profile.updatedAt.takeIf { profile.isConfigured } ?: 0L,
            hasInstallmentScheduleData = hasCompleteInstallmentSchedule,
            hasPreviousMonthData = previousPressure != null && currentHistoryPressure != null,
            currentMonthComparisonPressure = currentHistoryPressure, insights = insights,
            isLoading = false, errorMessage = currentError, activeInstallmentsCount = active.size,
            totalInstallmentsMonthlyFormatted = if (hasCompleteInstallmentSchedule) MoneyFormatter.formatExact(MoneyAmount.of(scheduledCurrentMonth, AppCurrency.RIAL)) else "برنامه بعضی اقساط ناقص است",
            currentMonthUnpaidInstallments = currentMonthUnpaidInstallments,
            nearestDueDateDays = daysToNearest, nearestDueDateTitle = nearestTitle ?: "سررسیدی ثبت نشده",
            hasNearestDueDate = nearestDueMillis != null, largestInstallmentTitle = largest?.first?.title ?: "قسطی ثبت نشده",
            largestInstallmentAmount = largest?.let { MoneyAmount.of(it.second.amount, it.second.amountUnit).asRialsLongExact() } ?: 0L, hasLargestInstallment = largest != null,
            nextMonthPrediction = nextMonth, previousMonthPressure = previousPressure ?: 0f,
            historicalPressureYear = currentMonth.year, historicalPressureList = history
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

    fun saveProfile(
        income: Long,
        installments: Long,
        fixed: Long,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            operationError.value = null
            val result = runCatching { repository.saveProfile(income, installments, fixed) }
            result.onSuccess {
                operationError.value = null
                onResult(true)
            }.onFailure {
                operationError.value = it.message ?: "ذخیره اطلاعات مالی انجام نشد."
                onResult(false)
            }
        }
    }
}