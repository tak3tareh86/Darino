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
    val activeCount: Int = 6,
    val paidAmount: Long = 24_500_000L,
    val remainingAmount: Long = 18_200_000L,
    val paidAmountFormatted: String = "",
    val remainingAmountFormatted: String = "",
    val nextDueText: String = "۳ روز دیگر",
    val nextDueTitle: String = "وام بانک ملت"
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

    fun getSampleInitialInstallments(): List<InstallmentItem> {
        return initialBankLoans + initialHomeLoans + initialCarInsurance + initialMiscInstallments
    }

    private val initialBankLoans = listOf(
        InstallmentItem(
            id = "bank_1",
            title = "وام مسکن بانک ملت",
            category = InstallmentCategory.BANK_LOANS,
            providerOrPerson = "بانک ملت",
            totalAmount = 80_000_000,
            totalAmountFormatted = "۸۰,۰۰۰,۰۰۰ تومان",
            paidAmount = 42_000_000,
            paidAmountFormatted = "۴۲,۰۰۰,۰۰۰ تومان",
            remainingAmount = 38_000_000,
            remainingAmountFormatted = "۳۸,۰۰۰,۰۰۰ تومان",
            monthlyPaymentFormatted = "۴,۵۰۰,۰۰۰ تومان",
            totalInstallments = 18,
            remainingInstallments = 9,
            nextPaymentDate = "۱۴۰۴/۰۷/۱۵",
            nextDueDaysText = "۳ روز دیگر",
            startDate = "۱۴۰۳/۰۱/۱۵",
            endDate = "۱۴۰۴/۰۶/۱۵",
            status = InstallmentStatus.DUE_SOON,
            notes = "برداشت مستقیم از حساب جاری ملت کد ۶۷۲",
            paymentHistory = listOf(
                PaymentHistoryItem("p1_1", 1, "۱۴۰۴/۰۴/۱۵", "۱۴۰۴/۰۴/۱۴", "۴,۵۰۰,۰۰۰ تومان", InstallmentStatus.PAID, "پرداخت خودکار ساتنا"),
                PaymentHistoryItem("p1_2", 2, "۱۴۰۴/۰۵/۱۵", "۱۴۰۴/۰۵/۱۵", "۴,۵۰۰,۰۰۰ تومان", InstallmentStatus.PAID, "واریز از همراه بانک"),
                PaymentHistoryItem("p1_3", 3, "۱۴۰۴/۰۶/۱۵", "۱۴۰۴/۰۶/۱۵", "۴,۵۰۰,۰۰۰ تومان", InstallmentStatus.PAID, "تایید شعبه مرکزی"),
                PaymentHistoryItem("p1_4", 4, "۱۴۰۴/۰۷/۱۵", null, "۴,۵۰۰,۰۰۰ تومان", InstallmentStatus.DUE_SOON, "سررسید ماه جاری"),
                PaymentHistoryItem("p1_5", 5, "۱۴۰۴/۰۸/۱۵", null, "۴,۵۰۰,۰۰۰ تومان", InstallmentStatus.PENDING, "قسط آینده")
            )
        ),
        InstallmentItem(
            id = "bank_2",
            title = "وام قرض‌الحسنه بانک ملی",
            category = InstallmentCategory.BANK_LOANS,
            providerOrPerson = "بانک ملی",
            totalAmount = 50_000_000,
            totalAmountFormatted = "۵۰,۰۰۰,۰۰۰ تومان",
            paidAmount = 32_000_000,
            paidAmountFormatted = "۳۲,۰۰۰,۰۰۰ تومان",
            remainingAmount = 18_000_000,
            remainingAmountFormatted = "۱۸,۰۰۰,۰۰۰ تومان",
            monthlyPaymentFormatted = "۲,۰۰۰,۰۰۰ تومان",
            totalInstallments = 25,
            remainingInstallments = 9,
            nextPaymentDate = "۱۴۰۴/۰۷/۰۸",
            nextDueDaysText = "۵ روز گذشته",
            startDate = "۱۴۰۲/۰۶/۰۸",
            endDate = "۱۴۰۴/۰۷/۰۸",
            status = InstallmentStatus.OVERDUE,
            notes = "شعبه فردوسی، ضامن: آقای احمدی",
            paymentHistory = listOf(
                PaymentHistoryItem("p2_1", 14, "۱۴۰۴/۰۵/۰۸", "۱۴۰۴/۰۵/۰۸", "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("p2_2", 15, "۱۴۰۴/۰۶/۰۸", "۱۴۰۴/۰۶/۰۸", "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("p2_3", 16, "۱۴۰۴/۰۷/۰۸", null, "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.OVERDUE, "نیاز به پرداخت فوری جهت عدم تاخیر")
            )
        ),
        InstallmentItem(
            id = "bank_3",
            title = "وام خرید کالا بانک سامان",
            category = InstallmentCategory.BANK_LOANS,
            providerOrPerson = "بانک سامان",
            totalAmount = 30_000_000,
            totalAmountFormatted = "۳۰,۰۰۰,۰۰۰ تومان",
            paidAmount = 20_000_000,
            paidAmountFormatted = "۲۰,۰۰۰,۰۰۰ تومان",
            remainingAmount = 10_000_000,
            remainingAmountFormatted = "۱۰,۰۰۰,۰۰۰ تومان",
            monthlyPaymentFormatted = "۲,۵۰۰,۰۰۰ تومان",
            totalInstallments = 12,
            remainingInstallments = 4,
            nextPaymentDate = "۱۴۰۴/۰۷/۲۸",
            nextDueDaysText = "۱۶ روز دیگر",
            startDate = "۱۴۰۳/۰۸/۲۸",
            endDate = "۱۴۰۴/۰۸/۲۸",
            status = InstallmentStatus.PENDING,
            notes = "کارت اعتباری سامان",
            paymentHistory = listOf(
                PaymentHistoryItem("p3_1", 7, "۱۴۰۴/۰۵/۲۸", "۱۴۰۴/۰۵/۲۸", "۲,۵۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("p3_2", 8, "۱۴۰۴/۰۶/۲۸", "۱۴۰۴/۰۶/۲۸", "۲,۵۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("p3_3", 9, "۱۴۰۴/۰۷/۲۸", null, "۲,۵۰۰,۰۰۰ تومان", InstallmentStatus.PENDING)
            )
        )
    )

    private val initialHomeLoans = listOf(
        InstallmentItem(
            id = "home_1",
            title = "صندوق قرض‌الحسنه خانوادگی مهر",
            category = InstallmentCategory.HOME_LOANS,
            providerOrPerson = "عمو رضا (مدیر صندوق)",
            totalAmount = 14_000_000,
            totalAmountFormatted = "۱۴,۰۰۰,۰۰۰ تومان",
            paidAmount = 9_800_000,
            paidAmountFormatted = "۹,۸۰۰,۰۰۰ تومان",
            remainingAmount = 4_200_000,
            remainingAmountFormatted = "۴,۲۰۰,۰۰۰ تومان",
            monthlyPaymentFormatted = "۱,۴۰۰,۰۰۰ تومان",
            totalInstallments = 10,
            remainingInstallments = 3,
            nextPaymentDate = "۱۴۰۴/۰۷/۲۰",
            nextDueDaysText = "۸ روز دیگر",
            startDate = "۱۴۰۳/۱۰/۲۰",
            endDate = "۱۴۰۴/۰۹/۲۰",
            status = InstallmentStatus.PENDING,
            notes = "قرعه‌کشی ماهانه فامیلی",
            paymentHistory = listOf(
                PaymentHistoryItem("h1_1", 5, "۱۴۰۴/۰۵/۲۰", "۱۴۰۴/۰۵/۲۰", "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("h1_2", 6, "۱۴۰۴/۰۶/۲۰", "۱۴۰۴/۰۶/۲۰", "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("h1_3", 7, "۱۴۰۴/۰۷/۲۰", null, "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PENDING)
            )
        )
    )

    private val initialCarInsurance = listOf(
        InstallmentItem(
            id = "car_1",
            title = "بیمه شخص ثالث ایران (خودرو)",
            category = InstallmentCategory.CAR_INSURANCE,
            providerOrPerson = "بیمه ایران نمایندگی حسینی",
            vehicleName = "پژو ۲۰۶ تیپ ۵",
            insuranceType = "شخص ثالث ۱۲ ماهه",
            totalAmount = 12_000_000,
            totalAmountFormatted = "۱۲,۰۰۰,۰۰۰ تومان",
            paidAmount = 8_000_000,
            paidAmountFormatted = "۸,۰۰۰,۰۰۰ تومان",
            remainingAmount = 4_000_000,
            remainingAmountFormatted = "۴,۰۰۰,۰۰۰ تومان",
            monthlyPaymentFormatted = "۲,۰۰۰,۰۰۰ تومان",
            totalInstallments = 6,
            remainingInstallments = 2,
            nextPaymentDate = "۱۴۰۴/۰۷/۱۴",
            nextDueDaysText = "۲ روز دیگر",
            startDate = "۱۴۰۴/۰۱/۱۴",
            endDate = "۱۴۰۴/۰۹/۱۴",
            status = InstallmentStatus.DUE_SOON,
            notes = "شماره بیمه‌نامه: ۱۲/۹۹/۴۸۱۷",
            paymentHistory = listOf(
                PaymentHistoryItem("c1_1", 3, "۱۴۰۴/۰۵/۱۴", "۱۴۰۴/۰۵/۱۴", "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("c1_2", 4, "۱۴۰۴/۰۶/۱۴", "۱۴۰۴/۰۶/۱۴", "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("c1_3", 5, "۱۴۰۴/۰۷/۱۴", null, "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.DUE_SOON)
            )
        )
    )

    private val initialMiscInstallments = listOf(
        InstallmentItem(
            id = "misc_1",
            title = "خرید لپ‌تاپ ایسوس از دیجی‌کالا",
            category = InstallmentCategory.MISC,
            providerOrPerson = "دیجی‌پی / ازکی‌وام",
            totalAmount = 24_000_000,
            totalAmountFormatted = "۲۴,۰۰۰,۰۰۰ تومان",
            paidAmount = 18_000_000,
            paidAmountFormatted = "۱۸,۰۰۰,۰۰۰ تومان",
            remainingAmount = 6_000_000,
            remainingAmountFormatted = "۶,۰۰۰,۰۰۰ تومان",
            monthlyPaymentFormatted = "۲,۰۰۰,۰۰۰ تومان",
            totalInstallments = 12,
            remainingInstallments = 3,
            nextPaymentDate = "۱۴۰۴/۰۷/۲۵",
            nextDueDaysText = "۱۳ روز دیگر",
            startDate = "۱۴۰۳/۰۸/۲۵",
            endDate = "۱۴۰۴/۰۸/۲۵",
            status = InstallmentStatus.PENDING,
            notes = "کسر مستقیم از کیف پول دیجی‌پی",
            paymentHistory = listOf(
                PaymentHistoryItem("m1_1", 8, "۱۴۰۴/۰۵/۲۵", "۱۴۰۴/۰۵/۲۵", "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("m1_2", 9, "۱۴۰۴/۰۶/۲۵", "۱۴۰۴/۰۶/۲۵", "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("m1_3", 10, "۱۴۰۴/۰۷/۲۵", null, "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PENDING)
            )
        )
    )

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
                paidAmountFormatted = s.paidAmountFormatted,
                remainingAmountFormatted = s.remainingAmountFormatted,
                nextDueText = s.nextDueText,
                nextDueTitle = s.nextDueTitle
            )
        }

    val categorySummaries: List<CategorySummaryStat>
        get() {
            return repository.categorySummaries.value.map {
                CategorySummaryStat(
                    category = it.category,
                    countText = it.countText,
                    remainingFormatted = it.remainingFormatted
                )
            }
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

    fun restoreSampleInstallments(context: Context? = null) {
        repository.restoreSampleInstallments(context)
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
        return repository.updateScheduleItem(installmentId, scheduleItemId, newStatus, newNote ?: "", context)
    }
}
