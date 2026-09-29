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
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.model.TransactionType
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
    private val vehicleRepository: VehicleRepository = VehicleRepository.instance,
    private val financeRepository: LocalFinanceRepository = LocalFinanceRepository.instance
) {

    fun getUserFullName(): String {
        val user = SessionManager.currentUser
        return when {
            !user?.fullName.isNullOrBlank() -> user?.fullName ?: "کاربر گرامی"
            !user?.phoneNumber.isNullOrBlank() -> "کاربر ${user?.phoneNumber?.takeLast(4)}"
            else -> "کاربر گرامی"
        }
    }

    fun getMonthlyFinancials(): Triple<Long, Long, Long> {
        val transactions = financeRepository.getTransactions()
        // If repo has transactions, compute real sum, otherwise return 0 for clean state
        val currentTxs = transactions.let {
            if (it is kotlinx.coroutines.flow.StateFlow) it.value else emptyList()
        }
        
        var income = 0L
        var expense = 0L
        for (tx in currentTxs) {
            if (tx.type == TransactionType.INCOME) {
                income += tx.amount
            } else if (tx.type == TransactionType.EXPENSE) {
                expense += tx.amount
            }
        }
        val balance = income - expense
        return Triple(income, expense, balance)
    }

    fun getSavingsRate(income: Long, expense: Long): Int {
        if (income <= 0) return 0
        val saved = (income - expense).coerceAtLeast(0)
        return ((saved.toDouble() / income.toDouble()) * 100).toInt()
    }

    fun getUpcomingObligations(): List<UpcomingObligationItem> {
        val list = mutableListOf<UpcomingObligationItem>()

        val bankLoans = InstallmentMockDataSource.bankLoans
        val firstLoan = bankLoans.firstOrNull { it.remainingInstallments > 0 }
        if (firstLoan != null) {
            list.add(
                UpcomingObligationItem(
                    id = "ob_loan_${firstLoan.id}",
                    title = firstLoan.title,
                    amount = firstLoan.totalAmount / firstLoan.totalInstallments.coerceAtLeast(1),
                    formattedAmount = firstLoan.monthlyPaymentFormatted,
                    relativeDaysText = firstLoan.nextDueDaysText,
                    dueDatePersian = firstLoan.nextPaymentDate,
                    type = ObligationType.INSTALLMENT,
                    destinationTab = BottomNavItem.INSTALLMENTS,
                    iconRes = R.drawable.img_3d_bank,
                    accentColor = ColorUtils.blueAccent
                )
            )
        }

        val primaryVehicle = vehicleRepository.vehicles.value.firstOrNull()
        if (primaryVehicle != null) {
            list.add(
                UpcomingObligationItem(
                    id = "ob_ins_${primaryVehicle.id}",
                    title = "بیمه ${primaryVehicle.brand} ${primaryVehicle.model}",
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
        }

        return list.take(3)
    }

    fun getUpcomingReminders(): List<UpcomingReminderItem> {
        val list = mutableListOf<UpcomingReminderItem>()
        val hasVehicles = vehicleRepository.vehicles.value.isNotEmpty()
        val hasInstallments = InstallmentMockDataSource.allInstallments.isNotEmpty()

        if (hasVehicles) {
            list.add(
                UpcomingReminderItem(
                    id = "rem_1",
                    title = "تماس با تعمیرگاه و سرویس دوره‌ای",
                    relativeDaysText = "فردا",
                    timePersian = "۱۰:۰۰",
                    typeTitle = "سرویس خودرو",
                    iconRes = R.drawable.img_3d_oil,
                    accentColor = ExpenseRoseLight
                )
            )
        }
        if (hasInstallments) {
            list.add(
                UpcomingReminderItem(
                    id = "rem_2",
                    title = "پرداخت قسط وام قرض‌الحسنه",
                    relativeDaysText = "۳ روز دیگر",
                    timePersian = "۰۹:۰۰",
                    typeTitle = "قسط بانک",
                    iconRes = R.drawable.img_3d_bank,
                    accentColor = ColorUtils.blueAccent
                )
            )
        }
        return list
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
        return com.example.reminder.domain.NotificationStore.getUnreadCount()
    }
}

object ColorUtils {
    val blueAccent = androidx.compose.ui.graphics.Color(0xFF2563EB)
}
