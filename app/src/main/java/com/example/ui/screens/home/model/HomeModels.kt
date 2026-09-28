package com.example.ui.screens.home.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.WarningAmberLight

data class MonthlySummary(
    val totalMonthlyDuesFormatted: String = "۱۲,۵۰۰,۰۰۰",
    val paidAmountFormatted: String = "۸,۱۲۵,۰۰۰",
    val remainingAmountFormatted: String = "۴,۳۷۵,۰۰۰",
    val progressPercentage: Float = 0.65f,
    val activeDuesCount: Int = 4,
    val totalRemindersCount: Int = 3
)

data class CategoryCardItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val nextDueLabel: String,
    val nextDueValue: String,
    val statusText: String,
    @DrawableRes val iconRes: Int,
    val accentColor: Color
)

data class QuickActionItemData(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val accentColor: Color
)

data class UpcomingPaymentItemData(
    val id: String,
    val title: String,
    val categoryName: String,
    val dueDatePersian: String,
    val amountFormatted: String,
    val daysRemainingText: String,
    val isUrgent: Boolean,
    @DrawableRes val iconRes: Int,
    val accentColor: Color
)

data class ReminderBannerData(
    val activeCount: Int = 3,
    val closestDueDateText: String = "۲ روز دیگر",
    val title: String = "۳ یادآوری فعال دارید",
    val subtitle: String = "نزدیک‌ترین پرداخت شما ۲ روز دیگر است."
)

object HomeMockDataSource {
    val summary = MonthlySummary()

    val quickActions = listOf(
        QuickActionItemData("add_expense", "ثبت هزینه", "💸", ExpenseRoseLight),
        QuickActionItemData("add_income", "ثبت درآمد", "💰", EmeraldPrimaryLight),
        QuickActionItemData("add_installment", "افزودن قسط", "📋", WarningAmberLight),
        QuickActionItemData("add_vehicle", "افزودن خودرو", "🚗", InfoIndigoLight)
    )

    val categoryCards = listOf(
        CategoryCardItem(
            id = "bank_loans",
            title = "وام‌های بانکی",
            subtitle = "۳ وام فعال",
            nextDueLabel = "قسط بعدی",
            nextDueValue = "۴,۵۰۰,۰۰۰ تومان",
            statusText = "سررسید ۲۵ شهریور",
            iconRes = R.drawable.img_3d_bank,
            accentColor = EmeraldPrimaryLight
        ),
        CategoryCardItem(
            id = "home_loans",
            title = "وام‌های خانگی",
            subtitle = "۲ وام فعال",
            nextDueLabel = "قسط بعدی",
            nextDueValue = "۲,۰۰۰,۰۰۰ تومان",
            statusText = "سررسید ۲ مهر",
            iconRes = R.drawable.img_3d_home,
            accentColor = WarningAmberLight
        ),
        CategoryCardItem(
            id = "car_insurance",
            title = "بیمه خودرو",
            subtitle = "۲ خودرو",
            nextDueLabel = "سررسید بعدی",
            nextDueValue = "۱۴۰۳/۰۷/۱۵",
            statusText = "تمدید شخص ثالث",
            iconRes = R.drawable.img_3d_car,
            accentColor = InfoIndigoLight
        ),
        CategoryCardItem(
            id = "misc_installments",
            title = "اقساط متفرقه",
            subtitle = "۵ قسط فعال",
            nextDueLabel = "قسط بعدی",
            nextDueValue = "۱,۲۰۰,۰۰۰ تومان",
            statusText = "خرید اقساطی کالا",
            iconRes = R.drawable.img_3d_installment,
            accentColor = ExpenseRoseLight
        )
    )

    val upcomingPayments = listOf(
        UpcomingPaymentItemData(
            id = "up_1",
            title = "قسط وام مسکن ملی",
            categoryName = "وام بانکی",
            dueDatePersian = "۲۵ شهریور",
            amountFormatted = "۴,۵۰۰,۰۰۰ تومان",
            daysRemainingText = "۲ روز مانده",
            isUrgent = true,
            iconRes = R.drawable.img_3d_bank,
            accentColor = ExpenseRoseLight
        ),
        UpcomingPaymentItemData(
            id = "up_2",
            title = "بیمه شخص ثالث پژو ۲۰۷",
            categoryName = "بیمه خودرو",
            dueDatePersian = "۳۰ شهریور",
            amountFormatted = "۸,۰۰۰,۰۰۰ تومان",
            daysRemainingText = "۷ روز مانده",
            isUrgent = false,
            iconRes = R.drawable.img_3d_car,
            accentColor = InfoIndigoLight
        ),
        UpcomingPaymentItemData(
            id = "up_3",
            title = "قسط صندوق خانوادگی مهر",
            categoryName = "وام خانگی",
            dueDatePersian = "۲ مهر",
            amountFormatted = "۲,۰۰۰,۰۰۰ تومان",
            daysRemainingText = "۹ روز مانده",
            isUrgent = false,
            iconRes = R.drawable.img_3d_home,
            accentColor = WarningAmberLight
        )
    )

    val reminder = ReminderBannerData()
}
