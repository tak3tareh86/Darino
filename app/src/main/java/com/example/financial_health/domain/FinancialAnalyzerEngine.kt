package com.example.financial_health.domain

import kotlin.math.roundToInt

/**
 * Core business engine for evaluating financial pressure, health status, and generating actionable insights.
 * Completely decoupled from UI logic.
 */
object FinancialAnalyzerEngine {

    /**
     * Calculates financial pressure percentage: (Total Commitments / Monthly Income) * 100
     */
    fun calculateFinancialPressure(commitments: Long, income: Long): Float {
        if (income <= 0L) return 100.0f
        if (commitments <= 0L) return 0.0f
        val ratio = (commitments.toDouble() / income.toDouble()) * 100.0
        // Round to 1 decimal place
        val rounded = (ratio * 10.0).roundToInt() / 10.0f
        return rounded.coerceIn(0.0f, 100.0f)
    }

    /**
     * Categorizes financial pressure percentage into predefined health tiers:
     * - 0% to 30%: SAFE (مناسب)
     * - 30% to 50%: MEDIUM (متوسط)
     * - 50% and above: HIGH (بالا)
     */
    fun calculateHealthStatus(pressurePercentage: Float): FinancialHealthStatus {
        return when {
            pressurePercentage <= 30.0f -> FinancialHealthStatus.SAFE
            pressurePercentage <= 50.0f -> FinancialHealthStatus.MEDIUM
            else -> FinancialHealthStatus.HIGH
        }
    }

    /**
     * Generates analytical insights and smart recommendations based on user commitment metrics.
     */
    fun generateInsights(
        income: Long,
        installments: Long,
        fixedExpenses: Long,
        currentPressure: Float,
        previousMonthPressure: Float,
        nextMonthPrediction: NextMonthPrediction
    ): List<FinancialHealthInsight> {
        val insights = mutableListOf<FinancialHealthInsight>()

        // 1. Month-over-month comparison alert
        val diff = currentPressure - previousMonthPressure
        val diffAbs = (Math.abs(diff) * 10).roundToInt() / 10.0f
        when {
            diff > 2.0f -> {
                insights.add(
                    FinancialHealthInsight(
                        id = "trend_alert",
                        title = "تحلیل دارینو: افزایش فشار مالی",
                        description = "فشار مالی شما نسبت به ماه قبل $diffAbs٪ افزایش داشته است (از $previousMonthPressure٪ به $currentPressure٪). تعهدات جدید یا نوسان مخارج نیازمند پایش هستند.",
                        type = InsightType.WARNING,
                        recommendation = "بررسی و اولویت‌بندی تسویه زودتر اقساط خرد برای کاهش بار ماهانه"
                    )
                )
            }
            diff < -2.0f -> {
                insights.add(
                    FinancialHealthInsight(
                        id = "trend_alert",
                        title = "تحلیل دارینو: بهبود وضعیت تعهدات",
                        description = "فشار مالی شما نسبت به ماه قبل $diffAbs٪ کاهش یافته و تعادل درآمد به تعهدات بهتر شده است.",
                        type = InsightType.POSITIVE,
                        recommendation = "تخصیص مازاد آزادشده به صندوق پس‌انداز اضطراری"
                    )
                )
            }
            else -> {
                insights.add(
                    FinancialHealthInsight(
                        id = "trend_alert",
                        title = "تحلیل دارینو: ثبات تعهدات ماهانه",
                        description = "بار تعهدات مالی شما نسبت به ماه قبل در وضعیتی باثبات و قابل پیش‌بینی قرار دارد.",
                        type = InsightType.INFO
                    )
                )
            }
        }

        // 2. Installments Weight Insight
        if (income > 0L) {
            val installmentRatio = ((installments.toDouble() / income.toDouble()) * 100.0).roundToInt()
            if (installmentRatio > 25) {
                insights.add(
                    FinancialHealthInsight(
                        id = "installments_weight",
                        title = "سهم اقساط از درآمد",
                        description = "اقساط فعال به تنهایی $installmentRatio٪ از کل درآمد ماهانه شما را به خود اختصاص داده‌اند.",
                        type = if (installmentRatio > 35) InsightType.ALERT else InsightType.WARNING,
                        recommendation = "از ایجاد تسهیلات و خریدهای اقساطی جدید تا تسویه حداقل یک مورد خودداری کنید."
                    )
                )
            }
        }

        // 3. Safe Zone recommendation
        if (currentPressure > 30.0f && income > 0L) {
            val maxSafeCommitment = (income * 0.30).toLong()
            val excessCommitment = (installments + fixedExpenses) - maxSafeCommitment
            if (excessCommitment > 0L) {
                val excessInMillions = (excessCommitment / 1_000_000.0 * 10).roundToInt() / 10.0
                insights.add(
                    FinancialHealthInsight(
                        id = "safe_zone_target",
                        title = "هدف‌گذاری منطقه امن (زیر ۳۰٪)",
                        description = "برای بازگشت به محدوده سبز و امن، کاهش حدود $excessInMillions میلیون تومان از تعهدات ماهانه لازم است.",
                        type = InsightType.INFO,
                        recommendation = "کاهش برخی هزینه‌های ثابت اشتراکی یا بازپرداخت زودتر یک وام کم‌مبلغ"
                    )
                )
            }
        }

        // 4. Next month outlook
        val nextMonthPressure = calculateFinancialPressure(nextMonthPrediction.total, income)
        insights.add(
            FinancialHealthInsight(
                id = "next_month_outlook",
                title = "چشم‌انداز تعهدات ماه آینده",
                description = "پیش‌بینی تعهدات ماه بعد مجموعاً ${(nextMonthPrediction.total / 1_000_000.0 * 10).roundToInt() / 10.0} میلیون تومان (فشار معادل $nextMonthPressure٪) خواهد بود.",
                type = if (nextMonthPressure > 45f) InsightType.WARNING else InsightType.INFO
            )
        )

        return insights
    }
}
