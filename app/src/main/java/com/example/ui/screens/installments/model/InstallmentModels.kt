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

    private val todayJalaliStr: String
        get() = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()

    private fun daysOffsetJalali(days: Int): String {
        val millis = System.currentTimeMillis() + (days * 86400000L)
        return com.example.util.PersianCalendarHelper.fromEpochMillis(millis).toFormattedDate()
    }

    private fun relativeDaysText(days: Int): String {
        return when {
            days == 0 -> "امروز"
            days > 0 -> "${com.example.util.IranianPhoneUtils.convertDigitsToPersian(days.toString())} روز دیگر"
            else -> "${com.example.util.IranianPhoneUtils.convertDigitsToPersian(kotlin.math.abs(days).toString())} روز گذشته"
        }
    }

    fun getSampleInitialInstallments(): List<InstallmentItem> {
        return initialBankLoans + initialHomeLoans + initialCarInsurance + initialMiscInstallments
    }

    private val initialBankLoans: List<InstallmentItem>
        get() = listOf(
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
                nextPaymentDate = daysOffsetJalali(3),
                nextDueDaysText = relativeDaysText(3),
                startDate = daysOffsetJalali(-270),
                endDate = daysOffsetJalali(270),
                status = InstallmentStatus.DUE_SOON,
                notes = "برداشت مستقیم از حساب جاری ملت کد ۶۷۲",
                paymentHistory = listOf(
                    PaymentHistoryItem("p1_1", 1, daysOffsetJalali(-90), daysOffsetJalali(-91), "۴,۵۰۰,۰۰۰ تومان", InstallmentStatus.PAID, "پرداخت خودکار ساتنا"),
                    PaymentHistoryItem("p1_2", 2, daysOffsetJalali(-60), daysOffsetJalali(-60), "۴,۵۰۰,۰۰۰ تومان", InstallmentStatus.PAID, "واریز از همراه بانک"),
                    PaymentHistoryItem("p1_3", 3, daysOffsetJalali(-30), daysOffsetJalali(-30), "۴,۵۰۰,۰۰۰ تومان", InstallmentStatus.PAID, "تایید شعبه مرکزی"),
                    PaymentHistoryItem("p1_4", 4, daysOffsetJalali(3), null, "۴,۵۰۰,۰۰۰ تومان", InstallmentStatus.DUE_SOON, "سررسید ماه جاری"),
                    PaymentHistoryItem("p1_5", 5, daysOffsetJalali(33), null, "۴,۵۰۰,۰۰۰ تومان", InstallmentStatus.PENDING, "قسط آینده")
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
                nextPaymentDate = daysOffsetJalali(-5),
                nextDueDaysText = relativeDaysText(-5),
                startDate = daysOffsetJalali(-400),
                endDate = daysOffsetJalali(200),
                status = InstallmentStatus.OVERDUE,
                notes = "شعبه فردوسی، ضامن: آقای احمدی",
                paymentHistory = listOf(
                    PaymentHistoryItem("p2_1", 14, daysOffsetJalali(-65), daysOffsetJalali(-65), "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                    PaymentHistoryItem("p2_2", 15, daysOffsetJalali(-35), daysOffsetJalali(-35), "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                    PaymentHistoryItem("p2_3", 16, daysOffsetJalali(-5), null, "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.OVERDUE, "نیاز به پرداخت فوری جهت عدم تاخیر")
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
                nextPaymentDate = daysOffsetJalali(16),
                nextDueDaysText = relativeDaysText(16),
                startDate = daysOffsetJalali(-240),
                endDate = daysOffsetJalali(120),
                status = InstallmentStatus.PENDING,
                notes = "کارت اعتباری سامان",
                paymentHistory = listOf(
                    PaymentHistoryItem("p3_1", 7, daysOffsetJalali(-44), daysOffsetJalali(-44), "۲,۵۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                    PaymentHistoryItem("p3_2", 8, daysOffsetJalali(-14), daysOffsetJalali(-14), "۲,۵۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                    PaymentHistoryItem("p3_3", 9, daysOffsetJalali(16), null, "۲,۵۰۰,۰۰۰ تومان", InstallmentStatus.PENDING)
                )
            )
        )

    private val initialHomeLoans: List<InstallmentItem>
        get() = listOf(
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
                nextPaymentDate = daysOffsetJalali(8),
                nextDueDaysText = relativeDaysText(8),
                startDate = daysOffsetJalali(-210),
                endDate = daysOffsetJalali(90),
                status = InstallmentStatus.PENDING,
                notes = "قرعه‌کشی ماهانه فامیلی",
                paymentHistory = listOf(
                    PaymentHistoryItem("h1_1", 5, daysOffsetJalali(-52), daysOffsetJalali(-52), "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                    PaymentHistoryItem("h1_2", 6, daysOffsetJalali(-22), daysOffsetJalali(-22), "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                    PaymentHistoryItem("h1_3", 7, daysOffsetJalali(8), null, "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PENDING)
                )
            )
        )

    private val initialCarInsurance: List<InstallmentItem>
        get() = listOf(
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
                nextPaymentDate = daysOffsetJalali(2),
                nextDueDaysText = relativeDaysText(2),
                startDate = daysOffsetJalali(-120),
                endDate = daysOffsetJalali(60),
                status = InstallmentStatus.DUE_SOON,
                notes = "شماره بیمه‌نامه: ۱۲/۹۹/۴۸۱۷",
                paymentHistory = listOf(
                    PaymentHistoryItem("c1_1", 3, daysOffsetJalali(-58), daysOffsetJalali(-58), "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                    PaymentHistoryItem("c1_2", 4, daysOffsetJalali(-28), daysOffsetJalali(-28), "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                    PaymentHistoryItem("c1_3", 5, daysOffsetJalali(2), null, "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.DUE_SOON)
                )
            )
        )

    private val initialMiscInstallments: List<InstallmentItem>
        get() = listOf(
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
                nextPaymentDate = daysOffsetJalali(13),
                nextDueDaysText = relativeDaysText(13),
                startDate = daysOffsetJalali(-270),
                endDate = daysOffsetJalali(90),
                status = InstallmentStatus.PENDING,
                notes = "کسر مستقیم از کیف پول دیجی‌پی",
                paymentHistory = listOf(
                    PaymentHistoryItem("m1_1", 8, daysOffsetJalali(-47), daysOffsetJalali(-47), "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                    PaymentHistoryItem("m1_2", 9, daysOffsetJalali(-17), daysOffsetJalali(-17), "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                    PaymentHistoryItem("m1_3", 10, daysOffsetJalali(13), null, "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PENDING)
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
            val repoInstallments = repository.installments.value
            if (repoInstallments.isEmpty()) {
                val samples = getSampleInitialInstallments()
                return InstallmentSummaryData(
                    activeCount = samples.size,
                    paidAmount = samples.sumOf { it.paidAmount },
                    remainingAmount = samples.sumOf { it.remainingAmount },
                    paidAmountFormatted = "۱۲۹,۲۰۰,۰۰۰ تومان",
                    remainingAmountFormatted = "۶۱,۸۰۰,۰۰۰ تومان",
                    nextDueText = "۳ روز دیگر",
                    nextDueTitle = "وام مسکن بانک ملت"
                )
            }
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
            if (repository.installments.value.isEmpty()) {
                return listOf(
                    CategorySummaryStat(InstallmentCategory.BANK_LOANS, "${initialBankLoans.size} مورد", "${initialBankLoans.sumOf { it.remainingAmount }} تومان"),
                    CategorySummaryStat(InstallmentCategory.HOME_LOANS, "${initialHomeLoans.size} مورد", "${initialHomeLoans.sumOf { it.remainingAmount }} تومان"),
                    CategorySummaryStat(InstallmentCategory.CAR_INSURANCE, "${initialCarInsurance.size} مورد", "${initialCarInsurance.sumOf { it.remainingAmount }} تومان"),
                    CategorySummaryStat(InstallmentCategory.MISC, "${initialMiscInstallments.size} مورد", "${initialMiscInstallments.sumOf { it.remainingAmount }} تومان")
                )
            }
            return repository.categorySummaries.value.map {
                CategorySummaryStat(
                    category = it.category,
                    countText = it.countText,
                    remainingFormatted = it.remainingFormatted
                )
            }
        }

    val bankLoans: List<InstallmentItem> get() = repository.bankLoans.value.ifEmpty { initialBankLoans }
    val homeLoans: List<InstallmentItem> get() = repository.homeLoans.value.ifEmpty { initialHomeLoans }
    val carInsurance: List<InstallmentItem> get() = repository.carInsurance.value.ifEmpty { initialCarInsurance }
    val miscInstallments: List<InstallmentItem> get() = repository.miscInstallments.value.ifEmpty { initialMiscInstallments }
    val allInstallments: List<InstallmentItem> get() = repository.installments.value.ifEmpty { getSampleInitialInstallments() }
    val overdueInstallments: List<InstallmentItem> get() = repository.overdueInstallments.value.ifEmpty {
        getSampleInitialInstallments().filter { it.status == InstallmentStatus.OVERDUE }
    }
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
