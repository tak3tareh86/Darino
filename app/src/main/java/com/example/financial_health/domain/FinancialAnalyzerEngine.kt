package com.example.financial_health.domain

import kotlin.math.roundToInt
import java.math.BigDecimal
import java.math.RoundingMode

/** Deterministic rules for the commitment-to-income ratio. */
object FinancialAnalyzerEngine {
    fun calculateFinancialPressure(commitments: Long, income: Long): Float? {
        if (income <= 0L || commitments < 0L) return null
        return BigDecimal.valueOf(commitments)
            .multiply(BigDecimal("100"))
            .divide(BigDecimal.valueOf(income), 1, RoundingMode.HALF_UP)
            .toFloat()
    }

    /** A schedule is financially complete only when its positive rows add up to the contract total. */
    fun isPaymentScheduleTotalConsistent(contractTotal: Long, scheduledAmounts: List<Long>): Boolean {
        if (contractTotal <= 0L || scheduledAmounts.isEmpty() || scheduledAmounts.any { it <= 0L }) return false
        var total = 0L
        for (amount in scheduledAmounts) {
            if (total > Long.MAX_VALUE - amount) return false
            total += amount
        }
        return total == contractTotal
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
        installmentDataComplete: Boolean = true,
        fixedExpensesRecorded: Boolean = true
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
                    description = "سهم اقساط برنامه‌ریزی‌شده (شامل پرداخت‌شده و پرداخت‌نشده) از درآمد از ${previousMonthPressure}٪ به ${currentPressure}٪ رسیده است؛ این شاخص فقط اقساط را می‌سنجد، نه کل فشار مالی.",
                    type = InsightType.WARNING,
                    recommendation = "اقساط سررسیدشونده و درآمد ثبت‌شده هر دو ماه را بررسی کنید."
                )
                diff < -2.0f -> insights += FinancialHealthInsight(
                    id = "trend_alert", title = "کاهش سهم اقساط از درآمد",
                    description = "سهم اقساط برنامه‌ریزی‌شده (شامل پرداخت‌شده و پرداخت‌نشده) از درآمد ${diffAbs} واحد درصد کاهش یافته است؛ این شاخص فقط اقساط را می‌سنجد، نه کل فشار مالی.",
                    type = InsightType.POSITIVE,
                    recommendation = "در صورت وجود مازاد واقعی، برای هزینه‌های پیش‌بینی‌نشده ذخیره در نظر بگیرید."
                )
                else -> insights += FinancialHealthInsight(
                    id = "trend_alert", title = "تغییر چشمگیری در سهم اقساط دیده نمی‌شود",
                    description = "مقایسه سهم اقساط برنامه‌ریزی‌شده (پرداخت‌شده و پرداخت‌نشده) از درآمد در دو ماه است و هزینه ثابت را شامل نمی‌شود.",
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

        // All input amounts are canonical Rials. Use decimal arithmetic for ratios and
        // convert Rials to millions of Toman with the exact 10:1 scale.
        val installmentRatio = BigDecimal.valueOf(installments)
            .multiply(BigDecimal("100"))
            .divide(BigDecimal.valueOf(income), 1, RoundingMode.HALF_UP)
        if (installmentDataComplete && installmentRatio > BigDecimal("25.0")) {
            insights += FinancialHealthInsight(
                id = "installments_weight", title = "سهم بالای اقساط از درآمد",
                description = "اقساط برنامه‌ریزی‌شده این ماه، شامل پرداخت‌شده و پرداخت‌نشده، حدود ${installmentRatio.stripTrailingZeros().toPlainString()}٪ از درآمد ماهانه ثبت‌شده را تشکیل می‌دهند؛ این نسبت فقط سهم اقساط است.",
                type = if (installmentRatio > BigDecimal("35.0")) InsightType.ALERT else InsightType.WARNING,
                recommendation = "پیش از ایجاد تعهد جدید، توان پرداخت پس از کسر هزینه‌های ثابت را بررسی کنید."
            )
        }

        val excessRials = BigDecimal.valueOf(installments)
            .add(BigDecimal.valueOf(fixedExpenses))
            .subtract(BigDecimal.valueOf(income).multiply(BigDecimal("0.30")))
        if (installmentDataComplete && fixedExpensesRecorded && excessRials.signum() > 0) {
            val excessMillionToman = excessRials
                .divide(BigDecimal("10000000"), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
            insights += FinancialHealthInsight(
                id = "safe_zone_target", title = "مقایسه با محدوده هدف ۳۰٪",
                description = "مجموع اقساط برنامه‌ریزی‌شده این ماه (پرداخت‌شده و پرداخت‌نشده) و هزینه ثابت ثبت‌شده $excessMillionToman میلیون تومان بالاتر از ۳۰٪ درآمد ثبت‌شده است؛ این یک معیار راهنماست، نه ارزیابی قطعی.",
                type = InsightType.INFO,
                recommendation = "درآمد و هزینه‌های ثابت ثبت‌شده را بازبینی کنید و از ایجاد تعهد غیرضروری بپرهیزید."
            )
        }

        if (nextMonthPrediction.total > 0L || nextMonthPrediction.fixedExpensesAmountRecorded) {
            val nextMonthPressure = calculateFinancialPressure(nextMonthPrediction.total, income)
            val ratioText = nextMonthPressure?.let { " (معادل ${it}٪ درآمد ماهانه ثبت‌شده)" } ?: ""
            val nextMonthMillionToman = BigDecimal.valueOf(nextMonthPrediction.total)
                .divide(BigDecimal("10000000"), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
            insights += FinancialHealthInsight(
                id = "next_month_outlook", title = "چشم‌انداز مبالغ ثبت‌شده ماه آینده",
                description = if (installmentDataComplete) {
                    "بر اساس اقساط سررسیدشونده، هزینه ثابت ثبت‌شده با فرض تکرار در ماه آینده و هزینه‌های خودروی دارای مبلغ و سررسید معتبر، مجموع مبالغ قابل محاسبه ${nextMonthMillionToman} میلیون تومان است${ratioText}. هزینه ثابت برآوردی است و مبلغ قطعی آینده نیست؛ اگر واحد پول یکی از هزینه‌های خودروی واجد شرایط نامعتبر باشد، مبلغ خودرو در جمع منظور نمی‌شود. یادآورها نیز هنوز متصل نیستند و در جمع لحاظ نشده‌اند."
                } else {
                    "به دلیل ناقص بودن برنامه بعضی اقساط، مبلغ اقساط در این برآورد لحاظ نشده است. هزینه ثابت ثبت‌شده با فرض تکرار، صرفاً برآورد ماه آینده است؛ مجموع مبالغ قابل محاسبه برای هزینه ثابت و هزینه‌های خودروی دارای واحد پول معتبر ${nextMonthMillionToman} میلیون تومان است${ratioText}. اگر واحد پول یکی از هزینه‌های خودروی واجد شرایط نامعتبر باشد، مبلغ خودرو در جمع منظور نمی‌شود؛ هزینه‌های ثبت‌نشده و یادآورها نیز در جمع نیستند."
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