package com.example.ui.screens.home.domain

import com.example.ui.screens.home.data.HomeDashboardRepository
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper

/**
 * Aggregator layer to compile a clean, unified HomeDashboardState without coupling presentation to multiple sources.
 */
class HomeDashboardAggregator(
    private val repository: HomeDashboardRepository
) {

    suspend fun aggregate(): HomeDashboardState {
        val (income, expense, balance) = repository.getMonthlyFinancials()
        val savingsRate = repository.getSavingsRate(income, expense)
        val userName = repository.getUserFullName()
        val upcomingObligations = repository.getUpcomingObligations()
        val upcomingReminders = repository.getUpcomingReminders()
        val overdueItems = repository.getOverdueItems()
        val vehicleSummary = repository.getPrimaryVehicleSummary()
        val notificationCount = repository.getUnreadNotificationCount()

        val todayPersian = getTodayPersianDateString()

        // Real counts for insight engine
        val activeInstCount = upcomingObligations.count { it.type == ObligationType.INSTALLMENT }
        val totalInstAmount = upcomingObligations.filter { it.type == ObligationType.INSTALLMENT }.sumOf { it.amount }

        // Generate dynamic rule-based insight
        val insight = HomeInsightEngine.generateInsight(
            monthlyIncome = income,
            monthlyExpense = expense,
            savingsRate = savingsRate,
            activeInstallmentsCount = activeInstCount,
            totalInstallmentsAmount = totalInstAmount,
            hasOverdueInstallments = overdueItems.isNotEmpty(),
            hasVehicleNeedsService = vehicleSummary?.isNeedsService == true,
            upcomingObligationsCount = upcomingObligations.size
        )

        val isAllClear = overdueItems.isEmpty() && upcomingObligations.none { it.relativeDaysText == "امروز" }
        val isEmpty = income == 0L && expense == 0L && upcomingObligations.isEmpty() && vehicleSummary == null

        return HomeDashboardState(
            userName = userName,
            todayDate = todayPersian,
            monthlyIncome = income,
            formattedIncome = MoneyFormatter.formatSignedToman(income, isExpense = false),
            monthlyExpense = expense,
            formattedExpense = MoneyFormatter.formatSignedToman(expense, isExpense = true),
            monthlyBalance = balance,
            formattedBalance = MoneyFormatter.formatToman(balance),
            savingsRate = savingsRate,
            upcomingObligations = upcomingObligations,
            upcomingReminders = upcomingReminders,
            overdueItems = overdueItems,
            vehicleSummary = vehicleSummary,
            financialInsight = insight,
            notificationCount = notificationCount,
            isAllClear = isAllClear,
            isEmptyState = isEmpty,
            isLoading = false
        )
    }

    private fun getTodayPersianDateString(): String {
        val now = System.currentTimeMillis()
        val pdt = PersianCalendarHelper.fromEpochMillis(now)
        val dayName = "امروز"
        val dayNumber = IranianPhoneUtils.convertDigitsToPersian(pdt.day.toString())
        val monthName = pdt.monthName
        val year = IranianPhoneUtils.convertDigitsToPersian(pdt.year.toString())
        return "$dayName $dayNumber $monthName $year"
    }
}
