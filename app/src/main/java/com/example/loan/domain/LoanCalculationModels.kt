package com.example.loan.domain

import androidx.annotation.DrawableRes
import com.example.R
import com.example.util.IranianPhoneUtils
import java.text.DecimalFormat

enum class LoanType(
    val id: String,
    val title: String,
    val subtitle: String,
    val defaultInterest: Double,
    val defaultDuration: Int,
    @DrawableRes val iconRes: Int
) {
    BANK_LOAN(
        id = "bank_loan",
        title = "وام بانکی",
        subtitle = "تسهیلات بانکی و اعتباری",
        defaultInterest = 23.0,
        defaultDuration = 36,
        iconRes = R.drawable.img_3d_bank
    ),
    INSTALLMENT_PURCHASE(
        id = "purchase",
        title = "خرید اقساطی",
        subtitle = "کالا، لوازم خانگی و دیجی‌پی",
        defaultInterest = 24.0,
        defaultDuration = 12,
        iconRes = R.drawable.img_3d_shopping
    ),
    PERSONAL_LOAN(
        id = "personal",
        title = "وام شخصی",
        subtitle = "قرض‌الحسنه و صندوق خانگی",
        defaultInterest = 4.0,
        defaultDuration = 10,
        iconRes = R.drawable.img_3d_home
    ),
    CAR_LOAN(
        id = "car_loan",
        title = "وام خودرو",
        subtitle = "تسهیلات خرید و لیزینگ خودرو",
        defaultInterest = 21.0,
        defaultDuration = 24,
        iconRes = R.drawable.img_3d_car
    ),
    CUSTOM(
        id = "custom",
        title = "سفارشی",
        subtitle = "تنظیم دلخواه کلیه پارامترها",
        defaultInterest = 18.0,
        defaultDuration = 12,
        iconRes = R.drawable.img_3d_calculator
    )
}

enum class InterestPeriod(val title: String) {
    ANNUAL("سود سالانه"),
    MONTHLY("سود ماهانه")
}

enum class DurationType(val title: String, val shortTitle: String) {
    MONTHS("ماه", "ماه"),
    YEARS("سال", "سال")
}

enum class CalculationMethod(val title: String, val subtitle: String) {
    ANNUITY("اقساط ثابت ماهانه (Annuity)", "فرمول مصوب بانکی با اقساط ماهانه یکسان"),
    SIMPLE_INTEREST("سود ساده", "سود بر مبنای اصل وام و مدت زمان"),
    CUSTOM("کارمزد / قرض‌الحسنه", "کارمزد ثابت سالانه (مانند قرض‌الحسنه ۴٪)")
}

data class CalculationResult(
    val principalAmount: Long,
    val durationMonths: Int,
    val annualInterestRate: Double,
    val monthlyPayment: Long,
    val totalPayment: Long,
    val totalInterest: Long,
    val realCostPercentage: Double,
    val method: CalculationMethod
) {
    val interestRatio: Float
        get() = if (totalPayment > 0) (totalInterest.toFloat() / totalPayment.toFloat()).coerceIn(0f, 1f) else 0f

    val principalRatio: Float
        get() = if (totalPayment > 0) (principalAmount.toFloat() / totalPayment.toFloat()).coerceIn(0f, 1f) else 1f

    val monthlyPaymentFormatted: String
        get() = formatMoney(monthlyPayment)

    val totalPaymentFormatted: String
        get() = formatMoney(totalPayment)

    val totalInterestFormatted: String
        get() = formatMoney(totalInterest)

    val realCostFormatted: String
        get() = "${IranianPhoneUtils.convertDigitsToPersian(String.format("%.1f", realCostPercentage))}٪"

    companion object {
        fun formatMoney(amount: Long): String {
            val formatter = DecimalFormat("#,###")
            val formatted = formatter.format(amount)
            return "${IranianPhoneUtils.convertDigitsToPersian(formatted)} تومان"
        }
    }
}

data class LoanCalculation(
    val id: String,
    val title: String,
    val loanType: LoanType,
    val amount: Long,
    val interestRate: Double,
    val duration: Int,
    val durationType: DurationType,
    val monthlyPayment: Long,
    val totalPayment: Long,
    val totalInterest: Long,
    val realCostPercentage: Double,
    val createdAt: Long = System.currentTimeMillis()
)

data class ComparisonLoanInput(
    val name: String,
    val amount: Long,
    val interestRate: Double,
    val durationMonths: Int,
    val method: CalculationMethod = CalculationMethod.ANNUITY
)

data class ComparisonResult(
    val loan1: CalculationResult,
    val loan2: CalculationResult,
    val monthlyPaymentDiff: Long,
    val totalInterestDiff: Long,
    val totalPaymentDiff: Long
)
