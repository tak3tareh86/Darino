package com.example.ui.screens.installments.model

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

object InstallmentMockDataSource {

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
            nextPaymentDate = "۱۴۰۴/۰۸/۰۲",
            nextDueDaysText = "۲۱ روز دیگر",
            startDate = "۱۴۰۳/۱۰/۰۲",
            endDate = "۱۴۰۴/۰۸/۰۲",
            status = InstallmentStatus.PENDING,
            notes = "واریز به کارت صندوق فامیلی بانک سپه",
            paymentHistory = listOf(
                PaymentHistoryItem("h1_1", 5, "۱۴۰۴/۰۵/۰۲", "۱۴۰۴/۰۵/۰۲", "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("h1_2", 6, "۱۴۰۴/۰۶/۰۲", "۱۴۰۴/۰۶/۰۲", "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("h1_3", 7, "۱۴۰۴/۰۷/۰۲", "۱۴۰۴/۰۷/۰۲", "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("h1_4", 8, "۱۴۰۴/۰۸/۰۲", null, "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PENDING)
            )
        ),
        InstallmentItem(
            id = "home_2",
            title = "بدهی شخصی تجهیز کارگاه",
            category = InstallmentCategory.HOME_LOANS,
            providerOrPerson = "حاج احمد حسینی",
            totalAmount = 7_000_000,
            totalAmountFormatted = "۷,۰۰۰,۰۰۰ تومان",
            paidAmount = 4_200_000,
            paidAmountFormatted = "۴,۲۰۰,۰۰۰ تومان",
            remainingAmount = 2_800_000,
            remainingAmountFormatted = "۲,۸۰۰,۰۰۰ تومان",
            monthlyPaymentFormatted = "۱,۴۰۰,۰۰۰ تومان",
            totalInstallments = 5,
            remainingInstallments = 2,
            nextPaymentDate = "۱۴۰۴/۰۶/۳۰",
            nextDueDaysText = "۸ روز گذشته",
            startDate = "۱۴۰۴/۰۳/۱۰",
            endDate = "۱۴۰۴/۰۸/۱۰",
            status = InstallmentStatus.OVERDUE,
            notes = "تسويه اقساطی خرید ابزارآلات و ملزومات",
            paymentHistory = listOf(
                PaymentHistoryItem("h2_1", 2, "۱۴۰۴/۰۵/۱۰", "۱۴۰۴/۰۵/۱۰", "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("h2_2", 3, "۱۴۰۴/۰۶/۱۰", "۱۴۰۴/۰۶/۱۰", "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("h2_3", 4, "۱۴۰۴/۰۶/۳۰", null, "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.OVERDUE, "سررسید گذشته")
            )
        )
    )

    private val initialCarInsurance = listOf(
        InstallmentItem(
            id = "car_ins_1",
            title = "بیمه شخص ثالث پژو ۲۰۶",
            category = InstallmentCategory.CAR_INSURANCE,
            providerOrPerson = "بیمه دانا",
            vehicleName = "پژو ۲۰۶ تیپ ۵",
            insuranceType = "شخص ثالث با تخفیف ۷۰٪",
            totalAmount = 12_000_000,
            totalAmountFormatted = "۱۲,۰۰۰,۰۰۰ تومان",
            paidAmount = 6_000_000,
            paidAmountFormatted = "۶,۰۰۰,۰۰۰ تومان",
            remainingAmount = 6_000_000,
            remainingAmountFormatted = "۶,۰۰۰,۰۰۰ تومان",
            monthlyPaymentFormatted = "۲,۰۰۰,۰۰۰ تومان",
            totalInstallments = 6,
            remainingInstallments = 3,
            nextPaymentDate = "۱۴۰۴/۰۷/۲۴",
            nextDueDaysText = "۱۲ روز دیگر",
            startDate = "۱۴۰۴/۰۴/۲۴",
            endDate = "۱۴۰۴/۱۰/۲۴",
            status = InstallmentStatus.PENDING,
            notes = "نمایندگی کد ۵۴۳ دانا، صدور آنلاین",
            paymentHistory = listOf(
                PaymentHistoryItem("ci1_1", 1, "۱۴۰۴/۰۴/۲۴", "۱۴۰۴/۰۴/۲۴", "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID, "پیش پرداخت"),
                PaymentHistoryItem("ci1_2", 2, "۱۴۰۴/۰۵/۲۴", "۱۴۰۴/۰۵/۲۴", "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("ci1_3", 3, "۱۴۰۴/۰۶/۲۴", "۱۴۰۴/۰۶/۲۴", "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("ci1_4", 4, "۱۴۰۴/۰۷/۲۴", null, "۲,۰۰۰,۰۰۰ تومان", InstallmentStatus.PENDING)
            )
        ),
        InstallmentItem(
            id = "car_ins_2",
            title = "بیمه بدنه طلایی دنا پلاس",
            category = InstallmentCategory.CAR_INSURANCE,
            providerOrPerson = "بیمه ایران",
            vehicleName = "دنا پلاس توربو اتوماتیک",
            insuranceType = "بدنه کامل با پوشش سرقت درجا و بلایا",
            totalAmount = 7_600_000,
            totalAmountFormatted = "۷,۶۰۰,۰۰۰ تومان",
            paidAmount = 3_800_000,
            paidAmountFormatted = "۳,۸۰۰,۰۰۰ تومان",
            remainingAmount = 3_800_000,
            remainingAmountFormatted = "۳,۸۰۰,۰۰۰ تومان",
            monthlyPaymentFormatted = "۱,۹۰۰,۰۰۰ تومان",
            totalInstallments = 4,
            remainingInstallments = 2,
            nextPaymentDate = "۱۴۰۴/۰۶/۲۵",
            nextDueDaysText = "۱۳ روز گذشته",
            startDate = "۱۴۰۴/۰۶/۰۵",
            endDate = "۱۴۰۴/۰۹/۰۵",
            status = InstallmentStatus.OVERDUE,
            notes = "بیمه ایران شعبه مطهری",
            paymentHistory = listOf(
                PaymentHistoryItem("ci2_1", 1, "۱۴۰۴/۰۶/۰۵", "۱۴۰۴/۰۶/۰۵", "۱,۹۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("ci2_2", 2, "۱۴۰۴/۰۷/۰۵", "۱۴۰۴/۰۷/۰۵", "۱,۹۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("ci2_3", 3, "۱۴۰۴/۰۶/۲۵", null, "۱,۹۰۰,۰۰۰ تومان", InstallmentStatus.OVERDUE, "سررسید گذشته")
            )
        )
    )

    private val initialMiscInstallments = listOf(
        InstallmentItem(
            id = "misc_1",
            title = "خرید لپ‌تاپ ایسوس زن‌بوک",
            category = InstallmentCategory.MISC,
            providerOrPerson = "دیجی‌پی / فروشگاه پایتخت",
            totalAmount = 17_000_000,
            totalAmountFormatted = "۱۷,۰۰۰,۰۰۰ تومان",
            paidAmount = 8_500_000,
            paidAmountFormatted = "۸,۵۰۰,۰۰۰ تومان",
            remainingAmount = 8_500_000,
            remainingAmountFormatted = "۸,۵۰۰,۰۰۰ تومان",
            monthlyPaymentFormatted = "۱,۷۰۰,۰۰۰ تومان",
            totalInstallments = 10,
            remainingInstallments = 5,
            nextPaymentDate = "۱۴۰۴/۰۶/۲۲",
            nextDueDaysText = "۱۶ روز گذشته",
            startDate = "۱۴۰۳/۱۲/۲۰",
            endDate = "۱۴۰۴/۰۹/۲۰",
            status = InstallmentStatus.OVERDUE,
            notes = "پرداخت از طریق کیف پول دیجی‌پی با تضامین بانکی",
            paymentHistory = listOf(
                PaymentHistoryItem("m1_1", 3, "۱۴۰۴/۰۴/۲۰", "۱۴۰۴/۰۴/۲۰", "۱,۷۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("m1_2", 4, "۱۴۰۴/۰۵/۲۰", "۱۴۰۴/۰۵/۲۰", "۱,۷۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("m1_3", 5, "۱۴۰۴/۰۶/۲۰", "۱۴۰۴/۰۶/۲۰", "۱,۷۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("m1_4", 6, "۱۴۰۴/۰۶/۲۲", null, "۱,۷۰۰,۰۰۰ تومان", InstallmentStatus.OVERDUE, "سررسید گذشته")
            )
        ),
        InstallmentItem(
            id = "misc_2",
            title = "خرید لوازم منزل و آشپزخانه",
            category = InstallmentCategory.MISC,
            providerOrPerson = "اسنپ‌پی / شهروند",
            totalAmount = 8_400_000,
            totalAmountFormatted = "۸,۴۰۰,۰۰۰ تومان",
            paidAmount = 4_200_000,
            paidAmountFormatted = "۴,۲۰۰,۰۰۰ تومان",
            remainingAmount = 4_200_000,
            remainingAmountFormatted = "۴,۲۰۰,۰۰۰ تومان",
            monthlyPaymentFormatted = "۱,۴۰۰,۰۰۰ تومان",
            totalInstallments = 6,
            remainingInstallments = 3,
            nextPaymentDate = "۱۴۰۴/۰۸/۰۱",
            nextDueDaysText = "۲۰ روز دیگر",
            startDate = "۱۴۰۴/۰۴/۰۱",
            endDate = "۱۴۰۴/۰۹/۰۱",
            status = InstallmentStatus.PENDING,
            notes = "خرید اقساطی ۴ قسطه اسنپ‌پی بدون کارمزد",
            paymentHistory = listOf(
                PaymentHistoryItem("m2_1", 1, "۱۴۰۴/۰۵/۰۱", "۱۴۰۴/۰۵/۰۱", "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("m2_2", 2, "۱۴۰۴/۰۶/۰۱", "۱۴۰۴/۰۶/۰۱", "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("m2_3", 3, "۱۴۰۴/۰۷/۰۱", "۱۴۰۴/۰۷/۰۱", "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("m2_4", 4, "۱۴۰۴/۰۸/۰۱", null, "۱,۴۰۰,۰۰۰ تومان", InstallmentStatus.PENDING)
            )
        ),
        InstallmentItem(
            id = "misc_3",
            title = "بیمه درمان تکمیلی انفرادی",
            category = InstallmentCategory.MISC,
            providerOrPerson = "بیمه سامان",
            totalAmount = 4_800_000,
            totalAmountFormatted = "۴,۸۰۰,۰۰۰ تومان",
            paidAmount = 3_600_000,
            paidAmountFormatted = "۳,۶۰۰,۰۰۰ تومان",
            remainingAmount = 1_200_000,
            remainingAmountFormatted = "۱,۲۰۰,۰۰۰ تومان",
            monthlyPaymentFormatted = "۱,۲۰۰,۰۰۰ تومان",
            totalInstallments = 4,
            remainingInstallments = 1,
            nextPaymentDate = "۱۴۰۴/۰۸/۱۵",
            nextDueDaysText = "۳۴ روز دیگر",
            startDate = "۱۴۰۴/۰۴/۱۵",
            endDate = "۱۴۰۴/۰۸/۱۵",
            status = InstallmentStatus.PENDING,
            notes = "طرح آرامش خانواده سامان",
            paymentHistory = listOf(
                PaymentHistoryItem("m3_1", 1, "۱۴۰۴/۰۵/۱۵", "۱۴۰۴/۰۵/۱۵", "۱,۲۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("m3_2", 2, "۱۴۰۴/۰۶/۱۵", "۱۴۰۴/۰۶/۱۵", "۱,۲۰۰,۰۰۰ تومان", InstallmentStatus.PAID),
                PaymentHistoryItem("m3_3", 3, "۱۴۰۴/۰۷/۱۵", "۱۴۰۴/۰۷/۱۵", "۱,۲۰۰,۰۰۰ تومان", InstallmentStatus.PAID)
            )
        )
    )

    private var _bankLoans = initialBankLoans.toMutableList()
    private var _homeLoans = initialHomeLoans.toMutableList()
    private var _carInsurance = initialCarInsurance.toMutableList()
    private var _miscInstallments = initialMiscInstallments.toMutableList()

    val summary: InstallmentSummaryData
        get() {
            val all = allInstallments
            if (all.isEmpty()) {
                return InstallmentSummaryData(
                    activeCount = 0,
                    paidAmountFormatted = "۰",
                    remainingAmountFormatted = "۰",
                    nextDueText = "بدون سررسید",
                    nextDueTitle = "تعهد فعالی ثبت نشده است"
                )
            }
            return InstallmentSummaryData()
        }

    val categorySummaries: List<CategorySummaryStat>
        get() {
            val all = allInstallments
            if (all.isEmpty()) return emptyList()
            return listOf(
                CategorySummaryStat(
                    category = InstallmentCategory.BANK_LOANS,
                    countText = "${_bankLoans.size} مورد",
                    remainingFormatted = "${_bankLoans.sumOf { it.remainingAmount } / 1_000_000}M باقی"
                ),
                CategorySummaryStat(
                    category = InstallmentCategory.HOME_LOANS,
                    countText = "${_homeLoans.size} مورد",
                    remainingFormatted = "${_homeLoans.sumOf { it.remainingAmount } / 1_000_000}M باقی"
                ),
                CategorySummaryStat(
                    category = InstallmentCategory.CAR_INSURANCE,
                    countText = "${_carInsurance.size} مورد",
                    remainingFormatted = "${_carInsurance.sumOf { it.remainingAmount } / 1_000_000}M باقی"
                ),
                CategorySummaryStat(
                    category = InstallmentCategory.MISC,
                    countText = "${_miscInstallments.size} مورد",
                    remainingFormatted = "${_miscInstallments.sumOf { it.remainingAmount } / 1_000_000}M باقی"
                )
            )
        }

    val bankLoans: List<InstallmentItem> get() = _bankLoans
    val homeLoans: List<InstallmentItem> get() = _homeLoans
    val carInsurance: List<InstallmentItem> get() = _carInsurance
    val miscInstallments: List<InstallmentItem> get() = _miscInstallments

    fun clearAllInstallments() {
        _bankLoans = mutableListOf()
        _homeLoans = mutableListOf()
        _carInsurance = mutableListOf()
        _miscInstallments = mutableListOf()
    }

    fun restoreSampleInstallments() {
        _bankLoans = initialBankLoans.toMutableList()
        _homeLoans = initialHomeLoans.toMutableList()
        _carInsurance = initialCarInsurance.toMutableList()
        _miscInstallments = initialMiscInstallments.toMutableList()
    }

    fun markOverdueAsPaid(installmentId: String, paymentDate: String): Boolean {
        fun updateList(list: MutableList<InstallmentItem>): Boolean {
            val index = list.indexOfFirst { it.id == installmentId }
            if (index == -1) return false
            val item = list[index]

            // Find the overdue installment entry in paymentHistory
            val overdueHistoryIndex = item.paymentHistory.indexOfFirst { it.status == InstallmentStatus.OVERDUE }
            val updatedHistory = if (overdueHistoryIndex != -1) {
                item.paymentHistory.mapIndexed { i, hist ->
                    if (i == overdueHistoryIndex) {
                        hist.copy(
                            status = InstallmentStatus.PAID,
                            paidDate = paymentDate,
                            isPaidLate = true,
                            note = if (hist.note.isNullOrBlank()) "پرداخت بعد از موعد سررسید (معوق)" else "${hist.note} - پرداخت بعد از موعد"
                        )
                    } else hist
                }
            } else {
                item.paymentHistory + PaymentHistoryItem(
                    id = "paid_late_${System.currentTimeMillis()}",
                    installmentNumber = item.paymentHistory.size + 1,
                    dueDate = item.nextPaymentDate,
                    paidDate = paymentDate,
                    amountFormatted = item.monthlyPaymentFormatted,
                    status = InstallmentStatus.PAID,
                    note = "پرداخت بعد از موعد سررسید (معوق)",
                    amount = item.totalAmount / item.totalInstallments.coerceAtLeast(1),
                    isPaidLate = true
                )
            }

            val monthlyAmount = item.totalAmount / item.totalInstallments.coerceAtLeast(1)
            val newPaidAmount = item.paidAmount + monthlyAmount
            val newRemainingAmount = (item.remainingAmount - monthlyAmount).coerceAtLeast(0L)
            val newRemainingInstallments = (item.remainingInstallments - 1).coerceAtLeast(0)

            val hasMoreOverdue = updatedHistory.any { it.status == InstallmentStatus.OVERDUE }
            val newStatus = if (newRemainingInstallments == 0) {
                InstallmentStatus.COMPLETED
            } else if (hasMoreOverdue) {
                InstallmentStatus.OVERDUE
            } else {
                InstallmentStatus.PENDING
            }

            val updatedItem = item.copy(
                status = newStatus,
                paidAmount = newPaidAmount,
                paidAmountFormatted = MoneyFormatter.formatToman(newPaidAmount),
                remainingAmount = newRemainingAmount,
                remainingAmountFormatted = MoneyFormatter.formatToman(newRemainingAmount),
                remainingInstallments = newRemainingInstallments,
                paymentHistory = updatedHistory,
                nextDueDaysText = if (newStatus == InstallmentStatus.OVERDUE) item.nextDueDaysText else "قسط بعدی در انتظار سررسید"
            )

            list[index] = updatedItem
            return true
        }

        return updateList(_bankLoans) || updateList(_homeLoans) || updateList(_carInsurance) || updateList(_miscInstallments)
    }

    val allInstallments: List<InstallmentItem>
        get() = bankLoans + homeLoans + carInsurance + miscInstallments

    val overdueInstallments: List<InstallmentItem>
        get() = allInstallments.filter { it.status == InstallmentStatus.OVERDUE }

    val upcomingInstallments: List<InstallmentItem>
        get() = allInstallments
            .filter { it.status == InstallmentStatus.DUE_SOON || it.status == InstallmentStatus.PENDING }
            .sortedBy { it.remainingInstallments }
            .take(3)
}
