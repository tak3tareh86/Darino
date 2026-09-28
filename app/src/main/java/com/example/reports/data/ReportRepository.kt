package com.example.reports.data

import com.example.R
import com.example.reports.domain.*

/**
 * Repository providing financial, installment, and vehicle reporting data.
 */
class ReportRepository(
    private val insightEngine: FinancialInsightEngine = FinancialInsightEngine()
) {

    fun getReportSummary(filter: ReportFilter): ReportSummary {
        val period = filter.period

        val (income, expense, prevExpense, prevIncome) = when (period) {
            ReportPeriod.WEEK -> Quadruple(7_500_000L, 3_800_000L, 4_100_000L, 7_000_000L)
            ReportPeriod.MONTH -> Quadruple(30_000_000L, 15_000_000L, 12_000_000L, 28_000_000L)
            ReportPeriod.YEAR -> Quadruple(360_000_000L, 180_000_000L, 160_000_000L, 320_000_000L)
            ReportPeriod.CUSTOM -> Quadruple(20_000_000L, 10_000_000L, 9_500_000L, 18_000_000L)
        }

        val saving = income - expense
        val savingRate = if (income > 0) ((saving.toDouble() / income) * 100).toInt() else 0

        val expenseChangePercent = if (prevExpense > 0) {
            (((expense - prevExpense).toDouble() / prevExpense) * 100).toInt()
        } else 0
        val isExpenseIncreased = expenseChangePercent > 0

        // Categories Breakdown
        val categories = listOf(
            CategoryExpenseItem(
                categoryName = "خودرو",
                amount = 5_500_000L,
                percentage = 37,
                colorHex = 0xFF3B82F6,
                iconRes = R.drawable.img_3d_car,
                transactionCount = 4
            ),
            CategoryExpenseItem(
                categoryName = "خوراک",
                amount = 4_000_000L,
                percentage = 27,
                colorHex = 0xFF10B981,
                iconRes = R.drawable.img_3d_food,
                transactionCount = 18
            ),
            CategoryExpenseItem(
                categoryName = "خرید",
                amount = 2_200_000L,
                percentage = 15,
                colorHex = 0xFFF59E0B,
                iconRes = R.drawable.img_3d_shopping,
                transactionCount = 7
            ),
            CategoryExpenseItem(
                categoryName = "حمل‌ونقل",
                amount = 1_500_000L,
                percentage = 10,
                colorHex = 0xFF8B5CF6,
                iconRes = R.drawable.img_3d_battery,
                transactionCount = 12
            ),
            CategoryExpenseItem(
                categoryName = "سایر",
                amount = 1_000_000L,
                percentage = 6,
                colorHex = 0xFF64748B,
                iconRes = R.drawable.img_3d_wallet,
                transactionCount = 5
            ),
            CategoryExpenseItem(
                categoryName = "قبوض",
                amount = 800_000L,
                percentage = 5,
                colorHex = 0xFFEC4899,
                iconRes = R.drawable.img_3d_analytics,
                transactionCount = 3
            )
        )

        // Monthly comparison bars
        val monthlyBars = listOf(
            MonthlyExpenseBar("فروردین", 10_500_000L, 25_000_000L),
            MonthlyExpenseBar("اردیبهشت", 11_800_000L, 26_000_000L),
            MonthlyExpenseBar("خرداد", 13_200_000L, 28_000_000L),
            MonthlyExpenseBar("تیر", 12_000_000L, 28_000_000L),
            MonthlyExpenseBar("مرداد", 12_000_000L, 29_000_000L),
            MonthlyExpenseBar("شهریور", 15_000_000L, 30_000_000L, isCurrentMonth = true)
        )

        // Trend line points
        val trendPoints = listOf(
            ExpenseTrendPoint("هفته اول", 3_200_000L, "۳,۲۰۰,۰۰۰"),
            ExpenseTrendPoint("هفته دوم", 4_100_000L, "۴,۱۰۰,۰۰۰"),
            ExpenseTrendPoint("هفته سوم", 3_800_000L, "۳,۸۰۰,۰۰۰"),
            ExpenseTrendPoint("هفته چهارم", 3_900_000L, "۳,۹۰۰,۰۰۰")
        )

        // Installments Summary
        val installmentItems = listOf(
            InstallmentReportItem(
                id = "inst_1",
                title = "وام خودرو",
                bankOrOrg = "بانک ملی",
                monthlyAmount = 3_500_000L,
                totalAmount = 84_000_000L,
                paidMonths = 10,
                totalMonths = 24,
                remainingAmount = 49_000_000L,
                nextDueDateDays = 12,
                status = InstallmentStatus.ACTIVE
            ),
            InstallmentReportItem(
                id = "inst_2",
                title = "تسهیلات مسکن",
                bankOrOrg = "بانک مسکن",
                monthlyAmount = 2_800_000L,
                totalAmount = 100_000_000L,
                paidMonths = 15,
                totalMonths = 36,
                remainingAmount = 58_800_000L,
                nextDueDateDays = 5,
                status = InstallmentStatus.ACTIVE
            ),
            InstallmentReportItem(
                id = "inst_3",
                title = "خرید لپ‌تاپ اقساطی",
                bankOrOrg = "دیجی‌پی",
                monthlyAmount = 1_200_000L,
                totalAmount = 14_400_000L,
                paidMonths = 7,
                totalMonths = 12,
                remainingAmount = 6_000_000L,
                nextDueDateDays = 18,
                status = InstallmentStatus.PAID_THIS_MONTH
            ),
            InstallmentReportItem(
                id = "inst_4",
                title = "وام قرض‌الحسنه خانوادگی",
                bankOrOrg = "صندوق رسالت",
                monthlyAmount = 600_000L,
                totalAmount = 7_200_000L,
                paidMonths = 5,
                totalMonths = 12,
                remainingAmount = 4_200_000L,
                nextDueDateDays = 22,
                status = InstallmentStatus.ACTIVE
            ),
            InstallmentReportItem(
                id = "inst_5",
                title = "خرید بیمه خودرو اقساطی",
                bankOrOrg = "بیمه ایران",
                monthlyAmount = 400_000L,
                totalAmount = 2_400_000L,
                paidMonths = 1,
                totalMonths = 6,
                remainingAmount = 2_000_000L,
                nextDueDateDays = 2,
                status = InstallmentStatus.OVERDUE
            )
        )

        val installmentSummary = InstallmentSummary(
            activeCount = 5,
            totalMonthlyPayment = 8_500_000L,
            totalRemainingDebt = 120_000_000L,
            nearestDueDateDays = 5,
            nearestDueDateTitle = "تسهیلات مسکن",
            largestInstallmentTitle = "وام خودرو",
            largestInstallmentAmount = 3_500_000L,
            paidCount = 38,
            remainingCount = 52,
            overdueCount = 1,
            items = installmentItems
        )

        // Vehicle Summary
        val vehicleBreakdown = listOf(
            VehicleCostItem("تعمیرات", 1_200_000L, 60, 0xFFEF4444, R.drawable.img_3d_tire),
            VehicleCostItem("سوخت", 600_000L, 30, 0xFF3B82F6, R.drawable.img_3d_fuel),
            VehicleCostItem("سرویس", 100_000L, 5, 0xFF10B981, R.drawable.img_3d_oil),
            VehicleCostItem("بیمه", 50_000L, 3, 0xFFF59E0B, R.drawable.img_3d_insurance),
            VehicleCostItem("قطعات", 50_000L, 2, 0xFF8B5CF6, R.drawable.img_3d_car)
        )

        val vehiclesList = listOf(
            VehicleDetailItem("veh_1", "پژو ۲۰۷i دنده‌ای", "ایران ۲۲ - ۵۶۴ ج ۱۸", 1_450_000L, 16_800_000L, R.drawable.img_3d_car),
            VehicleDetailItem("veh_2", "تارا اتوماتیک V4", "ایران ۵۵ - ۹۱۲ ب ۳۳", 550_000L, 7_200_000L, R.drawable.img_3d_car)
        )

        val vehicleSummary = VehicleSummary(
            totalVehicles = 2,
            currentMonthCost = 2_000_000L,
            yearlyCost = 24_000_000L,
            topCostCategory = "تعمیرات",
            topCostAmount = 1_200_000L,
            monthlyChangePercent = 15,
            isCostIncreased = true,
            costBreakdown = vehicleBreakdown,
            vehicles = vehiclesList
        )

        // Generate Smart Insights via Rule Engine
        val insights = insightEngine.generateInsights(
            income = income,
            expense = expense,
            previousExpense = prevExpense,
            categories = categories,
            installmentSummary = installmentSummary,
            vehicleSummary = vehicleSummary
        )

        val periodLabel = when (period) {
            ReportPeriod.WEEK -> "هفته جاری"
            ReportPeriod.MONTH -> "این ماه (شهریور ۱۴۰۵)"
            ReportPeriod.YEAR -> "سال ۱۴۰۵"
            ReportPeriod.CUSTOM -> "بازه انتخابی"
        }

        return ReportSummary(
            period = period,
            periodLabel = periodLabel,
            totalIncome = income,
            totalExpense = expense,
            totalSaving = saving,
            savingRatePercent = savingRate,
            previousMonthExpense = prevExpense,
            previousMonthIncome = prevIncome,
            expenseChangePercent = expenseChangePercent,
            isExpenseIncreased = isExpenseIncreased,
            topExpenseCategory = categories[0],
            secondExpenseCategory = categories[1],
            categoryExpenses = categories,
            monthlyBars = monthlyBars,
            trendPoints = trendPoints,
            installmentSummary = installmentSummary,
            vehicleSummary = vehicleSummary,
            insights = insights
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
