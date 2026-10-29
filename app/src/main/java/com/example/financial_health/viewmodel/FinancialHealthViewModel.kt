package com.example.financial_health.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.domain.CalendarDateUtils
import com.example.data.database.TransactionEntity
import com.example.financial_health.data.FinancialHealthRepository
import com.example.financial_health.domain.FinancialAnalyzerEngine
import com.example.financial_health.domain.FinancialHealthStatus
import com.example.financial_health.domain.MonthlyPressureData
import com.example.financial_health.domain.NextMonthPrediction
import com.example.ui.screens.installments.data.LocalInstallmentRepository
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.screens.installments.model.InstallmentStatus
import com.example.util.PersianCalendarHelper
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class FinancialHealthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinancialHealthRepository(application)
    private val installmentRepository = LocalInstallmentRepository.instance

    private data class MonthRange(val start: Long, val end: Long, val label: String)

    private fun monthRange(offset: Int): MonthRange {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MONTH, offset)
        val start = calendar.timeInMillis
        val jalali = PersianCalendarHelper.fromEpochMillis(start)
        val names = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
        val label = names.getOrNull(jalali.month - 1) ?: "${jalali.year}/${jalali.month}"
        calendar.add(Calendar.MONTH, 1)
        return MonthRange(start, calendar.timeInMillis, label)
    }

    private fun dueMillis(date: String): Long? {
        val parsed = CalendarDateUtils.parseJalali(date) ?: return null
        return runCatching { PersianCalendarHelper.jalaliToEpochMillis(parsed.first, parsed.second, parsed.third, 9, 0) }.getOrNull()
    }

    private fun isUnpaid(status: InstallmentStatus) = status != InstallmentStatus.PAID && status != InstallmentStatus.COMPLETED

    private fun scheduledAmount(items: List<InstallmentItem>, range: MonthRange): Long = items.sumOf { item ->
        item.paymentHistory.filter { payment ->
            isUnpaid(payment.status) && payment.amount > 0L &&
                dueMillis(payment.dueDate)?.let { it >= range.start && it < range.end } == true
        }.sumOf { it.amount }
    }

    private fun activeItems(items: List<InstallmentItem>) =
        items.filter { it.remainingInstallments > 0 && it.status != InstallmentStatus.COMPLETED }

    private fun historicalData(transactions: List<TransactionEntity>, installments: List<InstallmentItem>): List<MonthlyPressureData> {
        val result = mutableListOf<MonthlyPressureData>()
        for (offset in -5..0) {
            val range = monthRange(offset)
            val income = transactions.filter {
                it.type == "INCOME" && it.timestamp >= range.start && it.timestamp < range.end && it.amount > 0L
            }.sumOf { it.amount }
            if (income <= 0L) continue
            val installmentDue = scheduledAmount(installments, range)
            val pressure = FinancialAnalyzerEngine.calculateFinancialPressure(installmentDue, income) ?: continue
            result += MonthlyPressureData(range.label, pressure, installmentDue, income)
        }
        return result
    }

    val uiState: StateFlow<FinancialHealthState> = combine(
        repository.profileFlow, repository.transactionsFlow, installmentRepository.installments
    ) { profile, transactions, allInstallments ->
        val currentMonth = monthRange(0)
        val previousMonth = monthRange(-1)
        val active = activeItems(allInstallments)
        val scheduledCurrentMonth = scheduledAmount(active, currentMonth)
        val scheduledNextMonth = scheduledAmount(active, monthRange(1))
        val income = profile.monthlyIncome.coerceAtLeast(0L)
        val fixedExpenses = profile.fixedExpenses.coerceAtLeast(0L)
        val monthlyInstallments = scheduledCurrentMonth
        val totalCommitments = monthlyInstallments + fixedExpenses
        val pressure = FinancialAnalyzerEngine.calculateFinancialPressure(totalCommitments, income)
        val history = historicalData(transactions, allInstallments)
        val previousPressure = history.lastOrNull { it.monthName == previousMonth.label }?.pressurePercentage
        val status = pressure?.let { FinancialAnalyzerEngine.calculateHealthStatus(it) } ?: FinancialHealthStatus.MEDIUM

        val unpaidPayments = active.flatMap { item ->
            item.paymentHistory.filter { isUnpaid(it.status) && it.amount > 0L }.mapNotNull { payment ->
                dueMillis(payment.dueDate)?.let { Triple(item, payment, it) }
            }
        }
        val now = System.currentTimeMillis()
        val nearest = unpaidPayments.filter { it.third >= currentMonth.start }.minByOrNull { it.third }
            ?: unpaidPayments.filter { it.third < currentMonth.start }.maxByOrNull { it.third }
        val daysToNearest = nearest?.third?.let { ((it - now) / 86_400_000L).toInt() } ?: 0
        val largest = active.flatMap { item ->
            item.paymentHistory.filter { isUnpaid(it.status) && it.amount > 0L }.map { item to it }
        }.maxByOrNull { it.second.amount }

        val nextMonth = NextMonthPrediction(
            installments = scheduledNextMonth, reminders = 0L, vehicleExpenses = 0L,
            remindersAmountRecorded = false, vehicleExpensesAmountRecorded = false
        )
        val insights = FinancialAnalyzerEngine.generateInsights(
            income = income, installments = monthlyInstallments, fixedExpenses = fixedExpenses,
            currentPressure = history.lastOrNull()?.pressurePercentage, previousMonthPressure = previousPressure, nextMonthPrediction = nextMonth
        )
        val formatter = DecimalFormat("#,###")
        FinancialHealthState(
            monthlyIncome = income, totalInstallments = monthlyInstallments, fixedExpenses = fixedExpenses,
            totalCommitments = totalCommitments, pressurePercentage = pressure ?: 0f, healthStatus = status,
            hasIncomeData = income > 0L, hasPreviousMonthData = previousPressure != null, insights = insights,
            isLoading = false, errorMessage = null, activeInstallmentsCount = active.size,
            totalInstallmentsMonthlyFormatted = if (scheduledCurrentMonth > 0L) formatter.format(scheduledCurrentMonth) + " تومان" else "سررسید مبلغ‌دار ثبت نشده",
            nearestDueDateDays = daysToNearest, nearestDueDateTitle = nearest?.first?.title ?: "سررسیدی ثبت نشده",
            hasNearestDueDate = nearest != null, largestInstallmentTitle = largest?.first?.title ?: "قسطی ثبت نشده",
            largestInstallmentAmount = largest?.second?.amount ?: 0L, hasLargestInstallment = largest != null,
            nextMonthPrediction = nextMonth, previousMonthPressure = previousPressure ?: 0f, historicalPressureList = history
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialHealthState())

    fun updateMonthlyIncome(newIncome: Long) { viewModelScope.launch { repository.updateIncome(newIncome) } }
    fun updateFixedExpenses(newFixed: Long) { viewModelScope.launch { repository.updateFixedExpenses(newFixed) } }
    fun saveProfile(income: Long, installments: Long, fixed: Long) {
        viewModelScope.launch { repository.saveProfile(income, installments, fixed) }
    }
}