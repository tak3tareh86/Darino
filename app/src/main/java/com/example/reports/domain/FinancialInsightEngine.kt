package com.example.reports.domain

import com.example.R

/**
 * Rule Engine for Darino Financial Analysis.
 * Evaluates financial health, budget trends, installment weights, and vehicle maintenance loads.
 */
class FinancialInsightEngine {

    /**
     * Analyzes expenses and compares with previous periods to produce actionable insights.
     */
    fun analyzeExpenses(
        income: Long,
        expense: Long,
        previousExpense: Long,
        categories: List<CategoryExpenseItem>
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()

        // 1. Overall expense change vs last month
        if (previousExpense > 0) {
            val diff = expense - previousExpense
            val changePercent = ((diff.toDouble() / previousExpense) * 100).toInt()
            if (changePercent > 10) {
                insights.add(
                    FinancialInsight(
                        id = "exp_increase",
                        title = "افزایش هزینه‌های ماه جاری",
                        description = "هزینه‌های شما نسبت به ماه قبل ${changePercent}٪ افزایش داشته است.",
                        recommendation = "بررسی دسته‌بندی‌های با رشد بالا به خصوص اقلام غیرضروری توصیه می‌شود.",
                        severity = InsightSeverity.WARNING,
                        type = InsightType.EXPENSE_ALERT,
                        iconRes = R.drawable.img_3d_analytics,
                        changePercent = changePercent
                    )
                )
            } else if (changePercent < -5) {
                insights.add(
                    FinancialInsight(
                        id = "exp_controlled",
                        title = "کنترل و بهینه‌سازی هزینه‌ها",
                        description = "هزینه‌های شما نسبت به ماه قبل ${kotlin.math.abs(changePercent)}٪ کاهش یافته و مدیریت شده است.",
                        recommendation = "روند مناسب خود را حفظ کنید و مازاد بودجه را به پس‌انداز منتقل نمایید.",
                        severity = InsightSeverity.POSITIVE,
                        type = InsightType.BUDGET_CONTROL,
                        iconRes = R.drawable.img_3d_wallet,
                        changePercent = changePercent
                    )
                )
            }
        }

        // 2. High single-category concentration (> 35% of total expenses)
        val highestCategory = categories.maxByOrNull { it.amount }
        if (highestCategory != null && expense > 0) {
            val ratio = (highestCategory.amount.toDouble() / expense) * 100
            if (ratio >= 35.0) {
                insights.add(
                    FinancialInsight(
                        id = "cat_dominance_${highestCategory.categoryName}",
                        title = "تمرکز بالا در دسته ${highestCategory.categoryName}",
                        description = "حدود ${ratio.toInt()}٪ از کل مخارج شما به دسته «${highestCategory.categoryName}» اختصاص یافته است.",
                        recommendation = "توصیه می‌شود سقف بودجه مشخصی برای این بخش در ماه آینده تعیین کنید.",
                        severity = InsightSeverity.INFO,
                        type = InsightType.EXPENSE_ALERT,
                        iconRes = highestCategory.iconRes
                    )
                )
            }
        }

        // 3. Saving rate analysis
        if (income > 0) {
            val saving = income - expense
            val savingRate = ((saving.toDouble() / income) * 100).toInt()
            if (savingRate >= 30) {
                insights.add(
                    FinancialInsight(
                        id = "saving_healthy",
                        title = "نرخ پس‌انداز ایده‌آل",
                        description = "شما موفق شدید ${savingRate}٪ از کل درآمد این دوره را ذخیره و پس‌انداز کنید.",
                        recommendation = "می‌توانید بخشی از پس‌انداز را در صندوق‌های با درآمد ثابت یا طلا سرمایه‌گذاری کنید.",
                        severity = InsightSeverity.POSITIVE,
                        type = InsightType.SAVING_OPPORTUNITY,
                        iconRes = R.drawable.img_3d_bank
                    )
                )
            } else if (savingRate < 10 && saving >= 0) {
                insights.add(
                    FinancialInsight(
                        id = "saving_low",
                        title = "حاشیه امن پس‌انداز پایین",
                        description = "تنها ${savingRate}٪ از درآمد شما در این دوره پس‌انداز شده است.",
                        recommendation = "کاهش ۵٪ مخارج متفرقه می‌تواند توان مالی شما در شرایط اضطراری را دو برابر کند.",
                        severity = InsightSeverity.WARNING,
                        type = InsightType.SAVING_OPPORTUNITY,
                        iconRes = R.drawable.img_3d_shield_security
                    )
                )
            }
        }

        return insights
    }

    /**
     * Evaluates installment load against income and overdue flags.
     */
    fun analyzeInstallments(
        installmentSummary: InstallmentSummary,
        income: Long
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()

        // 1. Installment to Income ratio (DTI - Debt to Income)
        if (income > 0) {
            val dtiRatio = ((installmentSummary.totalMonthlyPayment.toDouble() / income) * 100).toInt()
            if (dtiRatio >= 30) {
                insights.add(
                    FinancialInsight(
                        id = "inst_burden_high",
                        title = "فشار اقساط ماهانه",
                        description = "بخش قابل توجهی از درآمد شما (${dtiRatio}٪) صرف پرداخت اقساط می‌شود.",
                        recommendation = "از دریافت تسهیلات یا خریدهای قسطی جدید تا تسویه حداقل ۲ وام قبلی خودداری فرمایید.",
                        severity = InsightSeverity.WARNING,
                        type = InsightType.INSTALLMENT_BURDEN,
                        iconRes = R.drawable.img_3d_installment
                    )
                )
            } else if (dtiRatio in 1..15) {
                insights.add(
                    FinancialInsight(
                        id = "inst_safe",
                        title = "ظرفیت اعتباری مناسب",
                        description = "تنها ${dtiRatio}٪ از درآمد ماهانه صرف اقساط می‌شود و بار مالی تسهیلات سبک است.",
                        recommendation = "وضعیت تعهدات ماهانه شما در شرایط ایده‌آل و پایدار قرار دارد.",
                        severity = InsightSeverity.POSITIVE,
                        type = InsightType.INSTALLMENT_BURDEN,
                        iconRes = R.drawable.img_3d_card
                    )
                )
            }
        }

        // 2. Overdue Installments Check
        if (installmentSummary.overdueCount > 0) {
            insights.add(
                FinancialInsight(
                    id = "inst_overdue_alert",
                    title = "هشدار قسط معوقه",
                    description = "شما ${installmentSummary.overdueCount} قسط معوقه دارید که نیازمند پرداخت سریع است.",
                    recommendation = "جهت جلوگیری از جرایم دیرکرد بانکی و افت رتبه اعتباری در اولین فرصت تسویه نمایید.",
                    severity = InsightSeverity.CRITICAL,
                    type = InsightType.INSTALLMENT_BURDEN,
                    iconRes = R.drawable.img_3d_bell_notification
                )
            )
        }

        // 3. Nearest due date reminder
        if (installmentSummary.nearestDueDateDays in 1..5) {
            insights.add(
                FinancialInsight(
                    id = "inst_near_due",
                    title = "سررسید نزدیک قسط",
                    description = "سررسید «${installmentSummary.nearestDueDateTitle}» تا ${installmentSummary.nearestDueDateDays} روز دیگر است.",
                    recommendation = "موجودی کارت متصل به قسط را بررسی نمایید تا کسر وجه به درستی انجام شود.",
                    severity = InsightSeverity.INFO,
                    type = InsightType.INSTALLMENT_BURDEN,
                    iconRes = R.drawable.img_3d_calendar
                )
            )
        }

        return insights
    }

    /**
     * Evaluates vehicle costs, periodic maintenance load, and trends.
     */
    fun analyzeVehicleCosts(
        vehicleSummary: VehicleSummary,
        totalExpense: Long
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()

        // 1. Vehicle cost increase trend
        if (vehicleSummary.isCostIncreased && vehicleSummary.monthlyChangePercent >= 10) {
            insights.add(
                FinancialInsight(
                    id = "veh_cost_surge",
                    title = "افزایش هزینه خودرو",
                    description = "هزینه خودرو شما نسبت به ماه قبل ${vehicleSummary.monthlyChangePercent}٪ افزایش داشته است.",
                    recommendation = "بیشترین سهم متعلق به «${vehicleSummary.topCostCategory}» بوده است. کنترل مصرف و معاینه دوره‌ای را مدنظر قرار دهید.",
                    severity = InsightSeverity.WARNING,
                    type = InsightType.VEHICLE_HEALTH,
                    iconRes = R.drawable.img_3d_car,
                    changePercent = vehicleSummary.monthlyChangePercent
                )
            )
        }

        // 2. Vehicle cost portion of total expenses
        if (totalExpense > 0) {
            val vehicleRatio = ((vehicleSummary.currentMonthCost.toDouble() / totalExpense) * 100).toInt()
            if (vehicleRatio > 20) {
                insights.add(
                    FinancialInsight(
                        id = "veh_expense_weight",
                        title = "سهم قابل توجه خودرو از مخارج",
                        description = "${vehicleRatio}٪ از کل مخارج این ماه صرف هزینه‌های نگهداری و سوخت خودرو شده است.",
                        recommendation = "انجام به موقع سرویس‌های سبک می‌تواند از هزینه‌های سنگین تعمیر موتور پیشگیری کند.",
                        severity = InsightSeverity.INFO,
                        type = InsightType.VEHICLE_HEALTH,
                        iconRes = R.drawable.img_3d_oil
                    )
                )
            }
        }

        return insights
    }

    /**
     * Aggregates all analyses into an ordered list of prioritized insights.
     */
    fun generateInsights(
        income: Long,
        expense: Long,
        previousExpense: Long,
        categories: List<CategoryExpenseItem>,
        installmentSummary: InstallmentSummary,
        vehicleSummary: VehicleSummary
    ): List<FinancialInsight> {
        val all = mutableListOf<FinancialInsight>()
        all.addAll(analyzeExpenses(income, expense, previousExpense, categories))
        all.addAll(analyzeInstallments(installmentSummary, income))
        all.addAll(analyzeVehicleCosts(vehicleSummary, expense))

        // Sort by severity: CRITICAL first, then WARNING, then POSITIVE, then INFO
        return all.sortedBy { insight ->
            when (insight.severity) {
                InsightSeverity.CRITICAL -> 0
                InsightSeverity.WARNING -> 1
                InsightSeverity.POSITIVE -> 2
                InsightSeverity.INFO -> 3
            }
        }
    }
}
