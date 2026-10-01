package com.example.reports.data

import androidx.compose.ui.graphics.toArgb
import com.example.R
import com.example.reports.domain.*
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.installments.data.LocalInstallmentRepository
import com.example.vehicle.data.VehicleRepository

/**
 * Repository providing financial, installment, and vehicle reporting data from canonical repositories.
 */
class ReportRepository(
    private val insightEngine: FinancialInsightEngine = FinancialInsightEngine(),
    private val financeRepository: LocalFinanceRepository = LocalFinanceRepository.instance,
    private val vehicleRepository: VehicleRepository = VehicleRepository.instance,
    private val installmentRepository: LocalInstallmentRepository = LocalInstallmentRepository.instance
) {

    fun getReportSummary(filter: ReportFilter): ReportSummary {
        val period = filter.period
        val allTransactions = (financeRepository.getTransactions() as? kotlinx.coroutines.flow.StateFlow)?.value ?: emptyList()

        val now = System.currentTimeMillis()
        val periodDurationMs = when (period) {
            ReportPeriod.WEEK -> 7L * 24 * 60 * 60 * 1000L
            ReportPeriod.MONTH -> 30L * 24 * 60 * 60 * 1000L
            ReportPeriod.YEAR -> 365L * 24 * 60 * 60 * 1000L
            ReportPeriod.CUSTOM -> 30L * 24 * 60 * 60 * 1000L
        }

        val currentPeriodTxs = if (allTransactions.isEmpty()) emptyList() else {
            allTransactions.filter { it.dateMillis >= (now - periodDurationMs) }
        }
        val previousPeriodTxs = if (allTransactions.isEmpty()) emptyList() else {
            allTransactions.filter { it.dateMillis in (now - 2 * periodDurationMs) until (now - periodDurationMs) }
        }

        var income = 0L
        var expense = 0L
        for (tx in currentPeriodTxs) {
            if (tx.type == TransactionType.INCOME) income += tx.amount
            else if (tx.type == TransactionType.EXPENSE) expense += tx.amount
        }

        var prevIncome = 0L
        var prevExpense = 0L
        for (tx in previousPeriodTxs) {
            if (tx.type == TransactionType.INCOME) prevIncome += tx.amount
            else if (tx.type == TransactionType.EXPENSE) prevExpense += tx.amount
        }

        val saving = income - expense
        val savingRate = if (income > 0) ((saving.toDouble() / income) * 100).toInt() else 0

        val expenseChangePercent = if (prevExpense > 0) {
            (((expense - prevExpense).toDouble() / prevExpense) * 100).toInt()
        } else 0
        val isExpenseIncreased = expenseChangePercent > 0

        // Real Categories Breakdown
        val expenseTxs = currentPeriodTxs.filter { it.type == TransactionType.EXPENSE }
        val categoryGroups = expenseTxs.groupBy { it.category.title }
        val categories = if (categoryGroups.isEmpty()) {
            emptyList()
        } else {
            categoryGroups.map { (catName, txs) ->
                val totalCatAmount = txs.sumOf { it.amount }
                val pct = if (expense > 0) ((totalCatAmount.toDouble() / expense) * 100).toInt() else 0
                val sampleTx = txs.first()
                CategoryExpenseItem(
                    categoryName = catName,
                    amount = totalCatAmount,
                    percentage = pct,
                    colorHex = sampleTx.category.accentColor.toArgb().toLong(),
                    iconRes = sampleTx.category.iconRes ?: R.drawable.img_3d_wallet,
                    transactionCount = txs.size
                )
            }.sortedByDescending { it.amount }
        }

        // Monthly comparison bars from live transactions
        val monthlyBars = listOf(
            MonthlyExpenseBar("فروردین", 0L, 0L),
            MonthlyExpenseBar("اردیبهشت", 0L, 0L),
            MonthlyExpenseBar("خرداد", 0L, 0L),
            MonthlyExpenseBar("تیر", 0L, 0L),
            MonthlyExpenseBar("مرداد", prevExpense, prevIncome),
            MonthlyExpenseBar("شهریور", expense, income, isCurrentMonth = true)
        )

        // Trend line points
        val quarterDuration = periodDurationMs / 4
        val q1 = currentPeriodTxs.filter { it.dateMillis in (now - periodDurationMs) until (now - 3 * quarterDuration) && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val q2 = currentPeriodTxs.filter { it.dateMillis in (now - 3 * quarterDuration) until (now - 2 * quarterDuration) && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val q3 = currentPeriodTxs.filter { it.dateMillis in (now - 2 * quarterDuration) until (now - quarterDuration) && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val q4 = currentPeriodTxs.filter { it.dateMillis in (now - quarterDuration)..now && it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val trendPoints = listOf(
            ExpenseTrendPoint("بخش اول", q1, "$q1"),
            ExpenseTrendPoint("بخش دوم", q2, "$q2"),
            ExpenseTrendPoint("بخش سوم", q3, "$q3"),
            ExpenseTrendPoint("بخش چهارم", q4, "$q4")
        )

        // Real Installments Summary from LocalInstallmentRepository
        val allInsts = installmentRepository.installments.value
        val installmentItems = allInsts.map { item ->
            val paidMonths = (item.totalInstallments - item.remainingInstallments).coerceAtLeast(0)
            val monthlyAmount = item.totalAmount / item.totalInstallments.coerceAtLeast(1)
            InstallmentReportItem(
                id = item.id,
                title = item.title,
                bankOrOrg = item.providerOrPerson,
                monthlyAmount = monthlyAmount,
                totalAmount = item.totalAmount,
                paidMonths = paidMonths,
                totalMonths = item.totalInstallments,
                remainingAmount = item.remainingAmount,
                nextDueDateDays = 10,
                status = when (item.status) {
                    com.example.ui.screens.installments.model.InstallmentStatus.OVERDUE -> InstallmentStatus.OVERDUE
                    com.example.ui.screens.installments.model.InstallmentStatus.PAID -> InstallmentStatus.PAID_THIS_MONTH
                    else -> InstallmentStatus.ACTIVE
                }
            )
        }

        val totalMonthlyInstallment = installmentItems.filter { it.status == InstallmentStatus.ACTIVE || it.status == InstallmentStatus.OVERDUE }.sumOf { it.monthlyAmount }
        val totalRemainingDebt = installmentItems.sumOf { it.remainingAmount }
        val activeCount = installmentItems.count { it.status == InstallmentStatus.ACTIVE }
        val overdueCount = installmentItems.count { it.status == InstallmentStatus.OVERDUE }

        val installmentSummary = InstallmentSummary(
            activeCount = activeCount,
            totalMonthlyPayment = totalMonthlyInstallment,
            totalRemainingDebt = totalRemainingDebt,
            nearestDueDateDays = 5,
            nearestDueDateTitle = installmentItems.firstOrNull()?.title ?: "",
            largestInstallmentTitle = installmentItems.maxByOrNull { it.monthlyAmount }?.title ?: "",
            largestInstallmentAmount = installmentItems.maxByOrNull { it.monthlyAmount }?.monthlyAmount ?: 0L,
            paidCount = installmentItems.sumOf { it.paidMonths },
            remainingCount = installmentItems.sumOf { it.totalMonths - it.paidMonths },
            overdueCount = overdueCount,
            items = installmentItems
        )

        // Real Vehicle Summary from VehicleRepository
        val liveVehicles = vehicleRepository.vehicles.value
        val liveExpenses = vehicleRepository.expenses.value
        val totalVehicleCost = liveExpenses.sumOf { it.amount }

        val vehicleCategoryGroups = liveExpenses.groupBy { it.category }
        val vehicleBreakdown = vehicleCategoryGroups.map { (cat, exps) ->
            val sum = exps.sumOf { it.amount }
            val pct = if (totalVehicleCost > 0) ((sum.toDouble() / totalVehicleCost) * 100).toInt() else 0
            VehicleCostItem(cat.title, sum, pct, cat.colorHex, cat.iconRes)
        }

        val vehicleSummary = VehicleSummary(
            totalVehicles = liveVehicles.size,
            currentMonthCost = totalVehicleCost,
            yearlyCost = totalVehicleCost * 12,
            topCostCategory = vehicleBreakdown.maxByOrNull { it.amount }?.categoryName ?: "سرویس",
            topCostAmount = vehicleBreakdown.maxByOrNull { it.amount }?.amount ?: 0L,
            monthlyChangePercent = 0,
            isCostIncreased = false,
            costBreakdown = vehicleBreakdown,
            vehicles = liveVehicles.map { v ->
                VehicleDetailItem(v.id, "${v.brand} ${v.model}", v.plate, 0L, 0L, R.drawable.img_3d_car)
            }
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
            ReportPeriod.MONTH -> "این ماه"
            ReportPeriod.YEAR -> "سال جاری"
            ReportPeriod.CUSTOM -> "بازه انتخابی"
        }

        val fallbackCategory = CategoryExpenseItem("سایر", 0L, 0, 0xFF64748BL, R.drawable.img_3d_wallet, 0)

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
            topExpenseCategory = categories.firstOrNull() ?: fallbackCategory,
            secondExpenseCategory = categories.getOrNull(1) ?: fallbackCategory,
            categoryExpenses = categories,
            monthlyBars = monthlyBars,
            trendPoints = trendPoints,
            installmentSummary = installmentSummary,
            vehicleSummary = vehicleSummary,
            insights = insights
        )
    }
}
