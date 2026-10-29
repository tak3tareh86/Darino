package com.example.financial_health.domain

import kotlin.math.roundToInt

/** Deterministic rules for the commitment-to-income ratio. */
object FinancialAnalyzerEngine {
    fun calculateFinancialPressure(commitments: Long, income: Long): Float? {
        if (income <= 0L || commitments < 0L) return null
        return ((commitments.toDouble() / income.toDouble()) * 100.0)
            .let { (it * 10.0).roundToInt() / 10.0f }
    }

    fun calculateHealthStatus(pressurePercentage: Float): FinancialHealthStatus = when {
        pressurePercentage <= 30.0f -> FinancialHealthStatus.SAFE
        pressurePercentage <= 50.0f -> FinancialHealthStatus.MEDIUM
        else -> FinancialHealthStatus.HIGH
    }

    fun generateInsights(
        income: Long, installments: Long, fixedExpenses: Long,
        currentPressure: Float?, previousMonthPressure: Float?,
        nextMonthPrediction: NextMonthPrediction
    ): List<FinancialHealthInsight> {
        val insights = mutableListOf<FinancialHealthInsight>()
        if (income <= 0L || currentPressure == null) {
            insights += FinancialHealthInsight(
                id = "income_required", title = "برای تحلیل، درآمد ماهانه را ثبت کنید",
                description = "تا زمانی که درآمد ماهانه معتبر ثبت نشود، درصد فشار مالی و مقایسه ماهانه قابل محاسبه نیست.",
                type = InsightType.INFO
            )
            return insights
        }

        if (previousMonthPressure != null) {
            val diff = currentPressure - previousMonthPressure
            val diffAbs = (kotlin.math.abs(diff) * 10).roundToInt() / 10.0f
            when {
                diff > 2.0f -> insights += FinancialHealthInsight(
                    id = "trend_alert", title = "افزایش فشار تعهدات نسبت به ماه قبل",
                    description = "سهم اقساط ثبت‌شده از درآمد از ${previousMonthPressure}٪ به ${currentPressure}٪ رسیده است.",
                    type = InsightType.WARNING,
                    recommendation = "اقساط سررسیدشونده و درآمد ثبت‌شده هر دو ماه را بررسی کنید."
                )
                diff < -2.0f -> insights += FinancialHealthInsight(
                    id = "trend_alert", title = "کاهش فشار تعهدات نسبت به ماه قبل",
                    description = "سهم اقساط ثبت‌شده از درآمد ${diffAbs} واحد درصد کاهش یافته است.",
                    type = InsightType.POSITIVE,
                    recommendation = "در صورت وجود مازاد واقعی، برای هزینه‌های پیش‌بینی‌نشده ذخیره در نظر بگیرید."
                )
                else -> insights += FinancialHealthInsight(
                    id = "trend_alert", title = "تغییر چشمگیری در سهم اقساط دیده نمی‌شود",
                    description = "مقایسه بر اساس درآمد و سررسیدهای ثبت‌شده در دو ماه انجام شده است.",
                    type = InsightType.INFO
                )
            }
        } else {
            insights += FinancialHealthInsight(
                id = "trend_unavailable", title = "مقایسه ماهانه هنوز در دسترس نیست",
                description = "برای مقایسه معتبر، اطلاعات درآمد و سررسیدهای ماه قبل کافی نیست.",
                type = InsightType.INFO
            )
        }

        val installmentRatio = installments.toDouble() / income.toDouble() * 100.0
        if (installmentRatio > 25.0) {
            insights += FinancialHealthInsight(
                id = "installments_weight", title = "سهم بالای اقساط از درآمد",
                description = "اقساط سررسیدشونده ثبت‌شده حدود ${(installmentRatio * 10).roundToInt() / 10.0}٪ از درآمد ماهانه ثبت‌شده را تشکیل می‌دهند.",
                type = if (installmentRatio > 35.0) InsightType.ALERT else InsightType.WARNING,
                recommendation = "پیش از ایجاد تعهد جدید، توان پرداخت پس از کسر هزینه‌های ثابت را بررسی کنید."
            )
        }

        val excess = installments + fixedExpenses - (income * 0.30).toLong()
        if (excess > 0L) {
            insights += FinancialHealthInsight(
                id = "safe_zone_target", title = "مقایسه با محدوده هدف ۳۰٪",
                description = "تعهدات ثبت‌شده ${excess / 1_000_000.0} میلیون تومان بالاتر از ۳۰٪ درآمد هستند؛ این یک معیار راهنماست، نه ارزیابی قطعی.",
                type = InsightType.INFO,
                recommendation = "درآمد و هزینه‌های ثابت ثبت‌شده را بازبینی کنید و از ایجاد تعهد غیرضروری بپرهیزید."
            )
        }

        if (nextMonthPrediction.installments > 0L) {
            val nextMonthPressure = calculateFinancialPressure(nextMonthPrediction.installments, income)
            val ratioText = nextMonthPressure?.let { " (معادل ${it}٪ درآمد ماهانه ثبت‌شده)" } ?: ""
            insights += FinancialHealthInsight(
                id = "next_month_outlook", title = "اقساط سررسیدشونده ماه آینده",
                description = "بر اساس برنامه اقساط ثبت‌شده، مبلغ اقساط ماه آینده ${nextMonthPrediction.installments / 1_000_000.0} میلیون تومان است${ratioText}. یادآورها و هزینه‌های خودرو در این جمع نیستند، چون مبلغ قابل محاسبه‌ای برای آن‌ها ثبت نشده است.",
                type = if ((nextMonthPressure ?: 0f) > 45f) InsightType.WARNING else InsightType.INFO
            )
        } else {
            insights += FinancialHealthInsight(
                id = "next_month_outlook", title = "اطلاعات کافی برای پیش‌بینی ماه آینده وجود ندارد",
                description = "برای ماه آینده سررسید قسط دارای مبلغ و تاریخ معتبر پیدا نشد؛ این به معنی صفر بودن هزینه‌های آینده نیست.",
                type = InsightType.INFO
            )
        }
        return insights
    }
}