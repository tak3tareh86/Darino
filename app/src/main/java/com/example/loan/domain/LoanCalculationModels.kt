package com.example.loan.domain

import androidx.annotation.DrawableRes
import com.example.R
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency
import java.text.DecimalFormat

enum class LoanType(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int
) {
    BANK_LOAN(
        id = "bank_loan",
        title = "وام بانکی",
        subtitle = "تسهیلات بانکی و اعتباری",
        iconRes = R.drawable.img_3d_bank
    ),
    INSTALLMENT_PURCHASE(
        id = "purchase",
        title = "خرید اقساطی",
        subtitle = "کالا، لوازم خانگی و دیجی‌پی",
        iconRes = R.drawable.img_3d_shopping
    ),
    PERSONAL_LOAN(
        id = "personal",
        title = "وام شخصی",
        subtitle = "وام شخصی و قرض‌الحسنه؛ نرخ تابع قرارداد",
        iconRes = R.drawable.img_3d_home
    ),
    CAR_LOAN(
        id = "car_loan",
        title = "وام خودرو",
        subtitle = "تسهیلات خرید و لیزینگ خودرو",
        iconRes = R.drawable.img_3d_car
    ),
    CUSTOM(
        id = "custom",
        title = "سفارشی",
        subtitle = "تنظیم دلخواه کلیه پارامترها",
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
    ANNUITY("اقساط ثابت ماهانه (Annuity)", "مدل ریاضی اقساط مساوی؛ برآورد غیرقراردادی"),
    SIMPLE_INTEREST("سود ساده", "سود بر مبنای اصل وام و مدت زمان"),
    CUSTOM("کارمزد / قرض‌الحسنه", "کارمزد ساده سالانه متناسب با مدت؛ تابع قرارداد")
}

data class CalculationResult(
    val principalAmount: Long,
    val durationMonths: Int,
    val annualInterestRate: Double,
    val monthlyPayment: Long,
    val totalPayment: Long,
    val totalInterest: Long,
    val realCostPercentage: Double,
    val method: CalculationMethod,
    val amountUnit: AppCurrency = AppCurrency.TOMAN
) {
    val interestRatio: Float
        get() = if (totalPayment > 0) (totalInterest.toFloat() / totalPayment.toFloat()).coerceIn(0f, 1f) else 0f

    val principalRatio: Float
        get() = if (totalPayment > 0) (principalAmount.toFloat() / totalPayment.toFloat()).coerceIn(0f, 1f) else 1f

    val monthlyPaymentFormatted: String
        get() = formatMoney(monthlyPayment, amountUnit)

    val totalPaymentFormatted: String
        get() = formatMoney(totalPayment, amountUnit)

    val totalInterestFormatted: String
        get() = formatMoney(totalInterest, amountUnit)

    val realCostFormatted: String
        get() = "${IranianPhoneUtils.convertDigitsToPersian(String.format("%.1f", realCostPercentage))}٪"

    companion object {
        fun formatMoney(amount: Long, unit: AppCurrency = AppCurrency.TOMAN): String = MoneyFormatter.formatExact(MoneyAmount.of(amount, unit))
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
    val createdAt: Long = System.currentTimeMillis(),
    val amountUnit: com.example.ui.screens.settings.model.AppCurrency = com.example.ui.screens.settings.model.AppCurrency.TOMAN
)

data class ComparisonLoanInput(
    val name: String,
    val amount: Long,
    val amountUnit: AppCurrency = AppCurrency.TOMAN,
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
