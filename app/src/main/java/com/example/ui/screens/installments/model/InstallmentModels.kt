package com.example.ui.screens.installments.model

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.database.InstallmentEntity
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.WarningAmberLight
import com.example.util.MoneyFormatter

enum class InstallmentCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int,
    val accentColor: Color
) {
    BANK_LOANS(
        id = "bank_loans",
        title = "وام‌های بانکی",
        subtitle = "تسهیلات و اقساط بانکی",
        iconRes = R.drawable.img_3d_bank,
        accentColor = Color(0xFF2563EB)
    ),
    HOME_LOANS(
        id = "home_loans",
        title = "وام‌های خانگی",
        subtitle = "صندوق‌های فامیلی و تعهدات خانوادگی",
        iconRes = R.drawable.img_3d_home,
        accentColor = WarningAmberLight
    ),
    CAR_INSURANCE(
        id = "car_insurance",
        title = "اقساط بیمه خودرو",
        subtitle = "بیمه شخص ثالث و بدنه خودروها",
        iconRes = R.drawable.img_3d_insurance,
        accentColor = InfoIndigoLight
    ),
    MISC(
        id = "misc_installments",
        title = "اقساط متفرقه",
        subtitle = "خرید کالا، لوازم و تعهدات شخصی",
        iconRes = R.drawable.img_3d_installment,
        accentColor = ExpenseRoseLight
    )
}

enum class InstallmentStatus(
    val title: String,
    val color: Color,
    val icon: ImageVector
) {
    PAID("پرداخت شده", EmeraldPrimaryLight, Icons.Rounded.CheckCircle),
    PENDING("در انتظار پرداخت", Color(0xFF64748B), Icons.Rounded.Schedule),
    DUE_SOON("نزدیک به سررسید", WarningAmberLight, Icons.Rounded.AccessTime),
    OVERDUE("عقب افتاده", ExpenseRoseLight, Icons.Rounded.Warning),
    COMPLETED("تکمیل شده", Color(0xFF8B5CF6), Icons.Rounded.TaskAlt)
}

data class PaymentHistoryItem(
    val id: String,
    val installmentNumber: Int,
    val dueDate: String,
    val paidDate: String? = null,
    val amountFormatted: String = "",
    val status: InstallmentStatus,
    val note: String? = null,
    val amount: Long = 0L,
    val isPaidLate: Boolean = false
) {
    val displayAmount: String
        get() = if (amount > 0L) MoneyFormatter.formatToman(amount) else if (amountFormatted.isNotBlank()) amountFormatted else MoneyFormatter.formatToman(0L)
}

data class InstallmentItem(
    val id: String,
    val title: String,
    val category: InstallmentCategory,
    val providerOrPerson: String,
    val vehicleName: String? = null,
    val insuranceType: String? = null,
    val totalAmount: Long,
    val totalAmountFormatted: String = "",
    val paidAmount: Long,
    val paidAmountFormatted: String = "",
    val remainingAmount: Long,
    val remainingAmountFormatted: String = "",
    val monthlyPaymentFormatted: String = "",
    val totalInstallments: Int,
    val remainingInstallments: Int,
    val nextPaymentDate: String,
    val nextDueDaysText: String,
    val startDate: String,
    val endDate: String,
    val status: InstallmentStatus,
    val notes: String = "",
    val paymentHistory: List<PaymentHistoryItem> = emptyList()
) {
    val progressPercentage: Float
        get() = if (totalAmount > 0) (paidAmount.toFloat() / totalAmount.toFloat()).coerceIn(0f, 1f) else 0f

    val displayTotal: String
        get() = MoneyFormatter.formatToman(totalAmount)

    val displayPaid: String
        get() = MoneyFormatter.formatToman(paidAmount)

    val displayRemaining: String
        get() = MoneyFormatter.formatToman(remainingAmount)

    val displayMonthlyPayment: String
        get() = MoneyFormatter.formatToman(totalAmount / totalInstallments.coerceAtLeast(1))
}

data class InstallmentSummaryData(
    val activeCount: Int = 0,
    val paidAmount: Long = 0L,
    val remainingAmount: Long = 0L,
    val paidAmountFormatted: String = "۰ تومان",
    val remainingAmountFormatted: String = "۰ تومان",
    val nextDueText: String = "-",
    val nextDueTitle: String = "-"
) {
    val displayPaid: String
        get() = MoneyFormatter.formatToman(paidAmount)

    val displayRemaining: String
        get() = MoneyFormatter.formatToman(remainingAmount)
}

data class CategorySummaryStat(
    val category: InstallmentCategory,
    val countText: String,
    val remainingFormatted: String
)

/**
 * Facade delegating directly to [com.example.ui.screens.installments.data.LocalInstallmentRepository]
 */
object InstallmentMockDataSource {

    private val repository: com.example.ui.screens.installments.data.LocalInstallmentRepository
        get() = com.example.ui.screens.installments.data.LocalInstallmentRepository.instance

    fun init(context: Context) {
        repository.init(context)
    }

    val summary: InstallmentSummaryData
        get() {
            val s = repository.summary.value
            return InstallmentSummaryData(
                activeCount = s.activeCount,
                paidAmount = s.paidAmount,
                remainingAmount = s.remainingAmount,
                paidAmountFormatted = s.paidAmountFormatted,
                remainingAmountFormatted = s.remainingAmountFormatted,
                nextDueText = s.nextDueText,
                nextDueTitle = s.nextDueTitle
            )
        }

    val categorySummaries: List<CategorySummaryStat>
        get() = repository.categorySummaries.value.map {
            CategorySummaryStat(
                category = it.category,
                countText = it.countText,
                remainingFormatted = it.remainingFormatted
            )
        }

    val bankLoans: List<InstallmentItem> get() = repository.bankLoans.value
    val homeLoans: List<InstallmentItem> get() = repository.homeLoans.value
    val carInsurance: List<InstallmentItem> get() = repository.carInsurance.value
    val miscInstallments: List<InstallmentItem> get() = repository.miscInstallments.value
    val allInstallments: List<InstallmentItem> get() = repository.installments.value
    val overdueInstallments: List<InstallmentItem> get() = repository.overdueInstallments.value
    val upcomingInstallments: List<InstallmentItem>
        get() = allInstallments
            .filter { it.status == InstallmentStatus.DUE_SOON || it.status == InstallmentStatus.PENDING }
            .sortedBy { it.remainingInstallments }
            .take(3)

    fun addInstallment(item: InstallmentItem, context: Context? = null) {
        repository.addInstallment(item, context)
    }

    fun clearAllInstallments(context: Context? = null) {
        repository.clearAllInstallments(context)
    }

    fun markOverdueAsPaid(installmentId: String, paymentDate: String, context: Context? = null): Boolean {
        repository.markOverdueAsPaid(installmentId, paymentDate, context)
        return true
    }

    fun updateScheduleItem(
        installmentId: String,
        scheduleItemId: String,
        newAmountLong: Long,
        newDueDate: String,
        newNote: String?,
        context: Context? = null
    ): Boolean {
        val current = repository.installments.value.find { it.id == installmentId } ?: return false
        val targetHistory = current.paymentHistory.find { it.id == scheduleItemId } ?: return false
        val newStatus = targetHistory.status
        return repository.updateScheduleItem(
            installmentId = installmentId,
            scheduleItemId = scheduleItemId,
            newAmountLong = newAmountLong,
            newDueDate = newDueDate,
            newStatus = newStatus,
            note = newNote,
            context = context
        )
    }
}
