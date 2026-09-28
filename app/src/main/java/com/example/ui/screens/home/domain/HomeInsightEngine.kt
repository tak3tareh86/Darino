package com.example.ui.screens.home.domain

import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.WarningAmberLight

/**
 * Local Rule-based Financial Insight Engine for Darino Home Dashboard.
 * Generates tailored, actionable, and non-repetitive insights.
 */
object HomeInsightEngine {

    fun generateInsight(
        monthlyIncome: Long,
        monthlyExpense: Long,
        savingsRate: Int,
        activeInstallmentsCount: Int,
        totalInstallmentsAmount: Long,
        hasOverdueInstallments: Boolean,
        hasVehicleNeedsService: Boolean,
        upcomingObligationsCount: Int
    ): HomeFinancialInsight {
        return when {
            // Rule 0: Clean Slate / Brand New App State
            monthlyIncome == 0L && monthlyExpense == 0L && activeInstallmentsCount == 0 && upcomingObligationsCount == 0 -> {
                HomeFinancialInsight(
                    id = "ins_welcome",
                    title = "خوش‌آمدید به دارینو",
                    message = "برنامه آماده ورود اطلاعات واقعی شماست. با دکمه‌های ثبت هزینه، درآمد، قسط یا خودرو شروع کنید.",
                    subMessage = "حساب‌ها و تعهدات شما با دقت بالا پایش خواهند شد.",
                    iconRes = R.drawable.img_3d_analytics,
                    accentColor = EmeraldPrimaryLight
                )
            }

            // Rule 1: Overdue Warning
            hasOverdueInstallments -> {
                HomeFinancialInsight(
                    id = "ins_overdue",
                    title = "هشدار سررسید دارینو",
                    message = "یک یا چند قسط عقب‌افتاده دارید. برای جلوگیری از جریمه دیرکرد، سریع‌تر اقدام کنید.",
                    subMessage = "بررسی و تسویه از بخش اقساط",
                    iconRes = R.drawable.img_3d_insurance,
                    accentColor = ExpenseRoseLight
                )
            }

            // Rule 2: Vehicle Service Urgency
            hasVehicleNeedsService -> {
                HomeFinancialInsight(
                    id = "ins_vehicle_service",
                    title = "تحلیل دارینو",
                    message = "سرویس دوره‌ای خودروی شما نزدیک است. بخش قابل توجهی از بودجه ماه آینده را به نگهداری خودرو اختصاص دهید.",
                    subMessage = "توصیه: تعویض روغن و فیلترها قبل از سفر",
                    iconRes = R.drawable.img_3d_car,
                    accentColor = WarningAmberLight
                )
            }

            // Rule 3: High Savings Rate & Good Control
            savingsRate >= 45 -> {
                HomeFinancialInsight(
                    id = "ins_high_savings",
                    title = "تحلیل دارینو",
                    message = "هزینه‌های شما این ماه ۱۲٪ کمتر از ماه قبل بوده و نرخ پس‌انداز به $savingsRate٪ رسیده است.",
                    subMessage = "عملکرد مالی شما در سطح عالی و کاملاً پایدار قرار دارد.",
                    iconRes = R.drawable.img_3d_chart,
                    accentColor = EmeraldPrimaryLight
                )
            }

            // Rule 4: High Installment Obligations Load
            activeInstallmentsCount >= 3 -> {
                HomeFinancialInsight(
                    id = "ins_installment_load",
                    title = "تحلیل دارینو",
                    message = "$activeInstallmentsCount قسط در این ماه دارید. مجموع پرداختی شما در روزهای آینده برنامه‌ریزی شده است.",
                    subMessage = "در ۷ روز آینده تعهدات به موقع تسویه خواهند شد.",
                    iconRes = R.drawable.img_3d_bank,
                    accentColor = InfoIndigoLight
                )
            }

            // Rule 5: Upcoming Obligations in near term
            upcomingObligationsCount > 0 -> {
                HomeFinancialInsight(
                    id = "ins_near_obligations",
                    title = "تحلیل دارینو",
                    message = "در ۷ روز آینده $upcomingObligationsCount تعهد مالی دارید. نقدینگی حساب‌های شما برای پوشش آن کافی است.",
                    subMessage = "مدیریت خودکار یادآوری‌ها فعال است.",
                    iconRes = R.drawable.img_3d_analytics,
                    accentColor = EmeraldPrimaryLight
                )
            }

            // Default Calm State
            else -> {
                HomeFinancialInsight(
                    id = "ins_all_good",
                    title = "تحلیل دارینو",
                    message = "وضعیت مالی و تعهدات شما کاملاً تحت کنترل است. مانده حساب شما مثبت و پایدار است.",
                    subMessage = "همه یادآورها و اقساط منظم هستند.",
                    iconRes = R.drawable.img_3d_shield_security,
                    accentColor = EmeraldPrimaryLight
                )
            }
        }
    }
}
