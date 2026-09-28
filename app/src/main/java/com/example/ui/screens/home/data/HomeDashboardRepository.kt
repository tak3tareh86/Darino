package com.example.ui.screens.home.data

import android.content.Context
import com.example.R
import com.example.data.security.SessionManager
import com.example.ui.screens.home.components.BottomNavItem
import com.example.ui.screens.home.domain.ObligationType
import com.example.ui.screens.home.domain.OverdueItem
import com.example.ui.screens.home.domain.OverdueType
import com.example.ui.screens.home.domain.UpcomingObligationItem
import com.example.ui.screens.home.domain.UpcomingReminderItem
import com.example.ui.screens.home.domain.VehicleSummaryData
import com.example.ui.screens.installments.model.InstallmentMockDataSource
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils
import com.example.vehicle.data.VehicleRepository

/**
 * Data repository for Home Dashboard.
 * Accesses underlying modules cleanly.
 */
class HomeDashboardRepository(
    private val context: Context? = null,
    private val vehicleRepository: VehicleRepository = VehicleRepository()
) {
    private val reminderRepo = context?.let { com.example.reminder.data.LocalReminderRepository(it) }

    fun getUserFullName(): String {
        val user = SessionManager.currentUser
        return when {
            !user?.fullName.isNullOrBlank() -> user?.fullName ?: "علی"
            !user?.phoneNumber.isNullOrBlank() -> "کاربر ${user?.phoneNumber?.takeLast(4)}"
            else -> "علی"
        }
    }

    fun getMonthlyFinancials(): Triple<Long, Long, Long> {
        val income = 18_000_000L
        val expense = 9_500_000L
        val balance = income - expense
        return Triple(income, expense, balance)
    }

    fun getSavingsRate(income: Long, expense: Long): Int {
        if (income <= 0) return 0
        val saved = (income - expense).coerceAtLeast(0)
        return ((saved.toDouble() / income.toDouble()) * 100).toInt()
    }

    fun getUpcomingObligations(): List<UpcomingObligationItem> {
        // Collect top 3 obligations from installments & vehicle insurance
        val list = mutableListOf<UpcomingObligationItem>()

        // 1. Installment: Bank Loan
        list.add(
            UpcomingObligationItem(
                id = "ob_loan_1",
                title = "قسط بانک مهر",
                amount = 3_000_000L,
                formattedAmount = IranianPhoneUtils.convertDigitsToPersian("۳,۰۰۰,۰۰۰") + " تومان",
                relativeDaysText = "۳ روز دیگر",
                dueDatePersian = "۱۴۰۵/۰۷/۰۳",
                type = ObligationType.INSTALLMENT,
                destinationTab = BottomNavItem.INSTALLMENTS,
                iconRes = R.drawable.img_3d_bank,
                accentColor = ColorUtils.blueAccent
            )
        )

        // 2. Vehicle Insurance: Car Insurance
        list.add(
            UpcomingObligationItem(
                id = "ob_insurance_1",
                title = "بیمه پژو ۲۰۷",
                amount = 2_500_000L,
                formattedAmount = IranianPhoneUtils.convertDigitsToPersian("۲,۵۰۰,۰۰۰") + " تومان",
                relativeDaysText = "۷ روز دیگر",
                dueDatePersian = "۱۴۰۵/۰۷/۰۷",
                type = ObligationType.VEHICLE_INSURANCE,
                destinationTab = BottomNavItem.VEHICLE,
                iconRes = R.drawable.img_3d_insurance,
                accentColor = InfoIndigoLight
            )
        )

        // 3. Bill Payment
        list.add(
            UpcomingObligationItem(
                id = "ob_bill_1",
                title = "پرداخت قبض برق و گاز",
                amount = 850_000L,
                formattedAmount = IranianPhoneUtils.convertDigitsToPersian("۸۵۰,۰۰۰") + " تومان",
                relativeDaysText = "فردا",
                dueDatePersian = "۱۴۰۵/۰۷/۰۱",
                type = ObligationType.BILL,
                destinationTab = BottomNavItem.CALENDAR,
                iconRes = R.drawable.img_3d_card,
                accentColor = WarningAmberLight
            )
        )

        return list.take(3)
    }

    fun getUpcomingReminders(): List<UpcomingReminderItem> {
        return listOf(
            UpcomingReminderItem(
                id = "rem_1",
                title = "تماس با تعمیرگاه و سرویس دوره‌ای",
                relativeDaysText = "فردا",
                timePersian = "۱۰:۰۰",
                typeTitle = "سرویس خودرو",
                iconRes = R.drawable.img_3d_oil,
                accentColor = ExpenseRoseLight
            ),
            UpcomingReminderItem(
                id = "rem_2",
                title = "پرداخت قسط وام قرض‌الحسنه",
                relativeDaysText = "۳ روز دیگر",
                timePersian = "۰۹:۰۰",
                typeTitle = "قسط بانک",
                iconRes = R.drawable.img_3d_bank,
                accentColor = ColorUtils.blueAccent
            ),
            UpcomingReminderItem(
                id = "rem_3",
                title = "تمدید بیمه‌نامه شخص ثالث",
                relativeDaysText = "۷ روز دیگر",
                timePersian = "۱۱:۳۰",
                typeTitle = "بیمه خودرو",
                iconRes = R.drawable.img_3d_insurance,
                accentColor = InfoIndigoLight
            )
        )
    }

    fun getOverdueItems(): List<OverdueItem> {
        val overdueInstallments = InstallmentMockDataSource.overdueInstallments
        return overdueInstallments.map { item ->
            OverdueItem(
                id = item.id,
                title = item.title,
                amount = item.totalAmount,
                formattedAmount = item.monthlyPaymentFormatted,
                overdueDaysText = item.nextDueDaysText,
                type = OverdueType.INSTALLMENT,
                destinationTab = BottomNavItem.INSTALLMENTS
            )
        }
    }

    fun getPrimaryVehicleSummary(): VehicleSummaryData? {
        val list = vehicleRepository.vehicles.value
        if (list.isEmpty()) return null
        val primary = list.first()

        val isServiceDue = primary.currentMileage >= 44500
        val kmLeft = (50000 - primary.currentMileage).coerceAtLeast(0)
        val statusText = if (kmLeft in 1..1000) {
            "نیاز به سرویس در ${IranianPhoneUtils.convertDigitsToPersian(kmLeft.toString())} کیلومتر آینده"
        } else {
            "همه چیز مرتب است."
        }

        return VehicleSummaryData(
            id = primary.id,
            name = "${primary.brand} ${primary.model}",
            modelYear = "مدل ${primary.year}",
            currentMileage = primary.currentMileage,
            formattedMileage = IranianPhoneUtils.convertDigitsToPersian(
                String.format("%,d", primary.currentMileage)
            ) + " کیلومتر",
            statusText = statusText,
            isNeedsService = kmLeft in 1..1000,
            plate = primary.plate
        )
    }

    fun getUnreadNotificationCount(): Int {
        return 3
    }
}

object ColorUtils {
    val blueAccent = androidx.compose.ui.graphics.Color(0xFF2563EB)
}
