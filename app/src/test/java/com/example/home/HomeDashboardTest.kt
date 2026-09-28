package com.example.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.home.data.HomeDashboardRepository
import com.example.ui.screens.home.domain.HomeDashboardAggregator
import com.example.ui.screens.home.domain.HomeInsightEngine
import com.example.ui.screens.home.domain.ObligationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeDashboardTest {

    @Test
    fun `test HomeDashboardAggregator produces complete dashboard state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = HomeDashboardRepository(context)
        val aggregator = HomeDashboardAggregator(repository)

        val state = aggregator.aggregate()

        // 1. Verify Monthly Financials & Savings Rate
        assertEquals(18_000_000L, state.monthlyIncome)
        assertEquals(9_500_000L, state.monthlyExpense)
        assertEquals(8_500_000L, state.monthlyBalance)
        assertEquals(47, state.savingsRate)

        // 2. Verify Upcoming Obligations (max 3)
        assertTrue(state.upcomingObligations.size in 1..3)
        val loanObligation = state.upcomingObligations.find { it.type == ObligationType.INSTALLMENT }
        assertNotNull(loanObligation)
        assertEquals("قسط بانک مهر", loanObligation?.title)

        // 3. Verify Upcoming Reminders (max 3)
        assertTrue(state.upcomingReminders.size in 1..3)
        assertEquals("تماس با تعمیرگاه و سرویس دوره‌ای", state.upcomingReminders[0].title)

        // 4. Verify Vehicle Summary
        assertNotNull(state.vehicleSummary)
        assertTrue(state.vehicleSummary?.name?.contains("پژو") == true)

        // 5. Verify Smart Insight Generated
        assertNotNull(state.financialInsight)
        assertEquals("هشدار سررسید دارینو", state.financialInsight?.title)
    }

    @Test
    fun `test HomeInsightEngine rule engine logic`() {
        // High savings rate test
        val highSavingsInsight = HomeInsightEngine.generateInsight(
            monthlyIncome = 20_000_000L,
            monthlyExpense = 8_000_000L,
            savingsRate = 60,
            activeInstallmentsCount = 1,
            totalInstallmentsAmount = 2_000_000L,
            hasOverdueInstallments = false,
            hasVehicleNeedsService = false,
            upcomingObligationsCount = 1
        )
        assertTrue(highSavingsInsight.message.contains("نرخ پس‌انداز"))

        // Overdue warning test
        val overdueInsight = HomeInsightEngine.generateInsight(
            monthlyIncome = 20_000_000L,
            monthlyExpense = 8_000_000L,
            savingsRate = 60,
            activeInstallmentsCount = 1,
            totalInstallmentsAmount = 2_000_000L,
            hasOverdueInstallments = true,
            hasVehicleNeedsService = false,
            upcomingObligationsCount = 1
        )
        assertTrue(overdueInsight.message.contains("عقب‌افتاده"))

        // Vehicle service test
        val vehicleInsight = HomeInsightEngine.generateInsight(
            monthlyIncome = 20_000_000L,
            monthlyExpense = 15_000_000L,
            savingsRate = 25,
            activeInstallmentsCount = 1,
            totalInstallmentsAmount = 2_000_000L,
            hasOverdueInstallments = false,
            hasVehicleNeedsService = true,
            upcomingObligationsCount = 1
        )
        assertTrue(vehicleInsight.message.contains("سرویس"))
    }
}
