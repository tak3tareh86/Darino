package com.example.ui.screens.home.domain

import com.example.ui.screens.home.data.HomeDashboardRepository
import com.example.util.IranianPhoneUtils
import com.example.util.PersianCalendarHelper

/**
 * Aggregator layer to compile a clean, unified HomeDashboardState without coupling presentation to multiple sources.
 */
class HomeDashboardAggregator(
    private val repository: HomeDashboardRepository
) {

    fun aggregate(): HomeDashboardState {
        val (income, expense, balance) = repository.getMonthlyFinancials()
        val savingsRate = repository.getSavingsRate(income, expense)
        val userName = repository.getUserFullName()
        val upcomingObligations = repository.getUpcomingObligations()
        val upcomingReminders = repository.getUpcomingReminders()
        val overdueItems = repository.getOverdueItems()
        val vehicleSummary = repository.getPrimaryVehicleSummary()
        val notificationCount = repository.getUnreadNotificationCount()

        val todayPersian = getTodayPersianDateString()

        // Generate dynamic rule-based insight
        val insight = HomeInsightEngine.generateInsight(
            monthlyIncome = income,
            monthlyExpense = expense,
            savingsRate = savingsRate,
            activeInstallmentsCount = 3,
            totalInstallmentsAmount = 8_500_000L,
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
            formattedIncome = IranianPhoneUtils.convertDigitsToPersian(String.format("%,d", income)) + " تومان",
            monthlyExpense = expense,
            formattedExpense = IranianPhoneUtils.convertDigitsToPersian(String.format("%,d", expense)) + " تومان",
            monthlyBalance = balance,
            formattedBalance = IranianPhoneUtils.convertDigitsToPersian(String.format("%,d", balance)) + " تومان",
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
        return "$dayName، $dayNumber $monthName $year"
    }
}
