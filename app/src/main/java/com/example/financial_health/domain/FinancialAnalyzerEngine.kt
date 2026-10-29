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
        nextMonthPrediction: NextMonthPrediction,
        installmentDataComplete: Boolean = true
    ): List<FinancialHealthInsight> {
        val insights = mutableListOf<FinancialHealthInsight>()
        if (income <= 0L) {
            insights += FinancialHealthInsight(
                id = "income_required", title = "برای تحلیل، درآمد ماهانه را ثبت کنید",
                description = "تا زمانی که درآمد ماهانه معتبر ثبت نشود، درصد فشار مالی و مقایسه ماهانه قابل محاسبه نیست.",
                type = InsightType.INFO
            )
            return insights
        }

        if (installmentDataComplete && currentPressure != null && previousMonthPressure != null) {
            val diff = currentPressure - previousMonthPressure
            val diffAbs = (kotlin.math.abs(diff) * 10).roundToInt() / 10.0f
            when {
                diff > 2.0f -> insights += FinancialHealthInsight(
                    id = "trend_alert", title = "افزایش سهم اقساط از درآمد",
                    description = "سهم اقساط ثبت‌شده از درآمد از ${previousMonthPressure}٪ به ${currentPressure}٪ رسیده است.",
                    type = InsightType.WARNING,
                    recommendation = "اقساط سررسیدشونده و درآمد ثبت‌شده هر دو ماه را بررسی کنید."
                )
                diff < -2.0f -> insights += FinancialHealthInsight(
                    id = "trend_alert", title = "کاهش سهم اقساط از درآمد",
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

        if (!installmentDataComplete) {
            insights += FinancialHealthInsight(
                id = "installment_schedule_incomplete",
                title = "برنامه بعضی اقساط ناقص است",
                description = "حداقل یک قسط فعال تاریخچه پرداخت یا زمان‌بندی کامل ندارد؛ بنابراین فشار مالی و روند اقساط به‌صورت کامل قابل محاسبه نیست.",
                type = InsightType.WARNING,
                recommendation = "برنامه پرداخت اقساط فعال را تکمیل کنید تا شاخص‌ها دقیق‌تر شوند."
            )
        }

        val installmentRatio = installments.toDouble() / income.toDouble() * 100.0
        if (installmentDataComplete && installmentRatio > 25.0) {
            insights += FinancialHealthInsight(
                id = "installments_weight", title = "سهم بالای اقساط از درآمد",
                description = "اقساط سررسیدشونده ثبت‌شده حدود ${(installmentRatio * 10).roundToInt() / 10.0}٪ از درآمد ماهانه ثبت‌شده را تشکیل می‌دهند.",
                type = if (installmentRatio > 35.0) InsightType.ALERT else InsightType.WARNING,
                recommendation = "پیش از ایجاد تعهد جدید، توان پرداخت پس از کسر هزینه‌های ثابت را بررسی کنید."
            )
        }

        val excess = installments + fixedExpenses - (income * 0.30).toLong()
        if (installmentDataComplete && excess > 0L) {
            insights += FinancialHealthInsight(
                id = "safe_zone_target", title = "مقایسه با محدوده هدف ۳۰٪",
                description = "تعهدات ثبت‌شده ${excess / 1_000_000.0} میلیون تومان بالاتر از ۳۰٪ درآمد هستند؛ این یک معیار راهنماست، نه ارزیابی قطعی.",
                type = InsightType.INFO,
                recommendation = "درآمد و هزینه‌های ثابت ثبت‌شده را بازبینی کنید و از ایجاد تعهد غیرضروری بپرهیزید."
            )
        }

        if (nextMonthPrediction.total > 0L || nextMonthPrediction.fixedExpensesAmountRecorded) {
            val nextMonthPressure = calculateFinancialPressure(nextMonthPrediction.total, income)
            val ratioText = nextMonthPressure?.let { " (معادل ${it}٪ درآمد ماهانه ثبت‌شده)" } ?: ""
            insights += FinancialHealthInsight(
                id = "next_month_outlook", title = "اقساط سررسیدشونده ماه آینده",
                                description = if (installmentDataComplete) {
                    "بر اساس اقساط سررسیدشونده، هزینه‌های ثابت ثبت‌شده و هزینه‌های خودروی دارای مبلغ و سررسید ثبت‌شده، مجموع مبالغ شناخته‌شده ماه آینده ${nextMonthPrediction.total / 1_000_000.0} میلیون تومان است${ratioText}. هزینه‌های خودرو بدون مبلغ یا تاریخ معتبر و یادآورها در جمع لحاظ نشده‌اند."
                } else {
                    "به دلیل ناقص بودن برنامه بعضی اقساط، مبلغ اقساط در این برآورد لحاظ نشده است. مجموع مبالغ ثبت‌شده برای هزینه‌های ثابت و خودرو ${nextMonthPrediction.total / 1_000_000.0} میلیون تومان است${ratioText}؛ هزینه‌های ثبت‌نشده و یادآورها در جمع نیستند."
                },
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