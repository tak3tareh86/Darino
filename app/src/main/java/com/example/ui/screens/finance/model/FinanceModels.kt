package com.example.ui.screens.finance.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.WarningAmberLight
import com.example.util.MoneyFormatter

enum class TransactionType(val title: String) {
    EXPENSE("هزینه"),
    INCOME("درآمد"),
    TRANSFER("انتقال")
}

enum class TransactionSourceType(val title: String) {
    MANUAL("دستی"),
    INSTALLMENT("پرداخت قسط"),
    VEHICLE("خودرو"),
    INSURANCE("بیمه"),
    MAINTENANCE("سرویس و نگهداری"),
    FUEL("سوخت و بنزین"),
    OTHER("سایر")
}

enum class PaymentMethod(val title: String, val iconEmoji: String) {
    BANK_CARD("کارت بانکی", "💳"),
    CASH("وجه نقد", "💵"),
    BANK_TRANSFER("انتقال بانکی", "🏦"),
    WALLET("کیف پول دیجیتال", "👛"),
    OTHER("سایر روش‌ها", "📝")
}

enum class RecurringFrequency(val title: String) {
    DAILY("روزانه"),
    WEEKLY("هفتگی"),
    MONTHLY("ماهانه"),
    YEARLY("سالانه")
}

enum class BudgetStatus(val title: String, val color: Color) {
    SAFE("مصرف مطلوب", Color(0xFF10B981)),
    WARNING("نزدیک به سقف", Color(0xFFF59E0B)),
    EXCEEDED("فراتر از بودجه", Color(0xFFEF4444))
}

enum class SavingsGoalStatus(val title: String) {
    IN_PROGRESS("در حال پس‌انداز"),
    COMPLETED("تکمیل‌شده"),
    PAUSED("متوقف")
}

enum class FinanceFilterPeriod(val title: String) {
    TODAY("امروز"),
    THIS_WEEK("این هفته"),
    THIS_MONTH("این ماه"),
    LAST_3_MONTHS("۳ ماه اخیر"),
    THIS_YEAR("امسال"),
    ALL("همه")
}

data class TransactionCategory(
    val id: String,
    val title: String,
    val iconEmoji: String,
    @DrawableRes val iconRes: Int? = null,
    val accentColor: Color,
    val type: TransactionType = TransactionType.EXPENSE,
    val parentId: String? = null,
    val isDefault: Boolean = true,
    val isCustom: Boolean = !isDefault,
    val isActive: Boolean = true,
    val subCategories: List<String> = emptyList()
)

enum class AccountType(val title: String) {
    BANK("بانک"),
    CASH("نقدی"),
    CARD("کارت"),
    OTHER("سایر")
}

data class Account(
    val id: String,
    val userId: String,
    val name: String,
    val type: AccountType,
    val initialBalance: Long = 0L,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class TransactionItemData(
    val id: String,
    val title: String,
    val amount: Long,
    val type: TransactionType,
    val category: TransactionCategory,
    val categoryId: String = category.id,
    val subCategory: String? = null,
    val datePersian: String,
    val timePersian: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val description: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.BANK_CARD,
    val accountName: String = "کارت بانکی",
    val sourceType: TransactionSourceType = TransactionSourceType.MANUAL,
    val sourceId: String? = null,
    val tags: List<String> = emptyList(),
    val isRecurring: Boolean = false,
    val recurringFrequency: RecurringFrequency? = null,
    val accountId: String? = null,
    val transferSourceAccountId: String? = null,
    val transferDestinationAccountId: String? = null
) {
    val amountFormatted: String
        get() = when (type) {
            TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(amount, isExpense = true)
            TransactionType.INCOME -> MoneyFormatter.formatSignedToman(amount, isExpense = false)
            TransactionType.TRANSFER -> MoneyFormatter.formatToman(amount)
        }
}

data class RecurringTransaction(
    val id: String,
    val title: String,
    val amount: Long,
    val type: TransactionType,
    val categoryId: String,
    val categoryTitle: String,
    val frequency: RecurringFrequency,
    val startDate: String,
    val endDate: String? = null,
    val nextExecutionDate: String,
    val enabled: Boolean = true,
    val paymentMethod: PaymentMethod = PaymentMethod.BANK_CARD,
    val accountName: String = "کارت بانکی",
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedAmount: String
        get() = MoneyFormatter.formatToman(amount)
    val amountFormatted: String
        get() = MoneyFormatter.formatToman(amount)
}

data class Budget(
    val id: String,
    val title: String,
    val categoryId: String? = null,
    val categoryTitle: String? = null,
    val amount: Long,
    val period: String = "این ماه",
    val startDate: String = "",
    val endDate: String = "",
    val spentAmount: Long = 0L,
    val remainingAmount: Long = 0L,
    val usagePercentage: Int = 0,
    val status: BudgetStatus = BudgetStatus.SAFE,
    val isEnabled: Boolean = true,
    val enabled: Boolean = isEnabled,
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedAmount: String
        get() = MoneyFormatter.formatToman(amount)
    val formattedSpent: String
        get() = MoneyFormatter.formatToman(spentAmount)
    val formattedRemaining: String
        get() = MoneyFormatter.formatToman(remainingAmount)
}

data class SavingsGoal(
    val id: String,
    val title: String,
    val targetAmount: Long,
    val currentAmount: Long,
    val targetDate: String,
    val description: String = "",
    val status: SavingsGoalStatus = SavingsGoalStatus.IN_PROGRESS,
    val progressPercentage: Int = 0,
    val iconEmoji: String = "🎯",
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedTarget: String
        get() = MoneyFormatter.formatToman(targetAmount)
    val formattedCurrent: String
        get() = MoneyFormatter.formatToman(currentAmount)
    val formattedRemaining: String
        get() = MoneyFormatter.formatToman(kotlin.math.max(0L, targetAmount - currentAmount))
}

data class FinancialState(
    val monthlyIncome: Long = 0L,
    val monthlyExpense: Long = 0L,
    val monthlyBalance: Long = 0L,
    val savingsRate: Int = 0,
    val formattedIncome: String = "",
    val formattedExpense: String = "",
    val formattedBalance: String = "",
    val monthlyBudget: Long = 0L,
    val budgetUsed: Long = 0L,
    val budgetRemaining: Long = 0L,
    val budgetProgress: Int = 0,
    val budgetStatus: BudgetStatus = BudgetStatus.SAFE,
    val formattedMonthlyBudget: String = "",
    val formattedBudgetUsed: String = "",
    val formattedBudgetRemaining: String = "",
    val recentTransactions: List<TransactionItemData> = emptyList(),
    val allTransactions: List<TransactionItemData> = emptyList(),
    val activeBudgets: List<Budget> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val categories: List<TransactionCategory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

object FinanceDefaultCategories {
    val defaultExpenseCategories = listOf(
        TransactionCategory("food", "خوراک و رستوران", "🍔", R.drawable.img_3d_food, Color(0xFFF97316),
            subCategories = listOf("سوپرمارکت", "رستوران", "کافه", "میوه و سبزیجات")),
        TransactionCategory("transport", "حمل‌ونقل", "🚕", R.drawable.img_3d_car, Color(0xFF3B82F6),
            subCategories = listOf("تاکسی", "مترو و اتوبوس", "اسنپ/تپسی", "کرایه")),
        TransactionCategory("fuel", "سوخت و بنزین", "⛽", R.drawable.img_3d_car, Color(0xFFEF4444),
            subCategories = listOf("بنزین", "گاز", "روغن موتور")),
        TransactionCategory("shopping", "خرید و پوشاک", "🛒", R.drawable.img_3d_shopping, Color(0xFF8B5CF6),
            subCategories = listOf("پوشاک", "لوازم دیجیتال", "لوازم خانگی", "آرایشی")),
        TransactionCategory("bill", "قبض و اشتراک", "🧾", R.drawable.img_3d_home, Color(0xFF10B981),
            subCategories = listOf("برق", "آب", "گاز", "اینترنت", "تلفن همراه")),
        TransactionCategory("home", "خانه و اجاره", "🏠", R.drawable.img_3d_home, Color(0xFF0D9488),
            subCategories = listOf("اجاره‌بها", "شارژ ساختمان", "تعمیرات خانه")),
        TransactionCategory("health", "درمان و دارو", "💊", null, Color(0xFFEC4899),
            subCategories = listOf("ویزیت پزشک", "داروخانه", "دندانپزشکی", "آزمایشگاه")),
        TransactionCategory("entertainment", "تفریح و سفر", "🎮", null, Color(0xFFF59E0B),
            subCategories = listOf("سینما", "سفر و هتل", "کتاب", "بازی")),
        TransactionCategory("education", "آموزش و رشد", "🎓", null, Color(0xFF06B6D4),
            subCategories = listOf("کلاس آموزشی", "کتاب و پکیج", "شهریه")),
        TransactionCategory("vehicle", "سرویس و نگهداری خودرو", "🔧", R.drawable.img_3d_car, Color(0xFF64748B),
            subCategories = listOf("تعویض روغن", "لنت و ترمز", "باطری", "کارواش")),
        TransactionCategory("insurance", "بیمه", "🛡️", null, Color(0xFF0284C7),
            subCategories = listOf("بیمه شخص ثالث", "بیمه بدنه", "بیمه تکمیلی")),
        TransactionCategory("installments", "اقساط و وام", "💳", R.drawable.img_3d_card, Color(0xFF6366F1),
            subCategories = listOf("وام بانکی", "قسط خودرو", "قسط مسکن", "خرید اقساطی")),
        TransactionCategory("misc", "سایر هزینه‌ها", "📦", null, Color(0xFF71717A),
            subCategories = listOf("متفرقه", "هدایا", "خیریه"))
    )

    val defaultIncomeCategories = listOf(
        TransactionCategory("salary", "حقوق و دستمزد", "💰", R.drawable.img_3d_wallet, EmeraldPrimaryLight,
            type = TransactionType.INCOME, subCategories = listOf("حقوق ماهانه", "پاداش", "اضافه‌کاری")),
        TransactionCategory("side_income", "درآمد جانبی و پروژه", "💻", null, InfoIndigoLight,
            type = TransactionType.INCOME, subCategories = listOf("فریلنسری", "مشاوره", "فروش اینترنتی")),
        TransactionCategory("investment", "سود و سرمایه‌گذاری", "📈", R.drawable.img_3d_chart, WarningAmberLight,
            type = TransactionType.INCOME, subCategories = listOf("سود بانکی", "بورس", "طلا/ارز")),
        TransactionCategory("gift", "هدیه و پاداش", "🎁", null, Color(0xFFEC4899),
            type = TransactionType.INCOME),
        TransactionCategory("other_income", "سایر درآمدها", "💵", null, Color(0xFF14B8A6),
            type = TransactionType.INCOME)
    )

    val allDefaultCategories: List<TransactionCategory>
        get() = defaultExpenseCategories + defaultIncomeCategories
}

object FinanceMockDataSource {
    val initialTransactions = listOf(
        TransactionItemData(
            id = "tx_1",
            title = "خرید روزمره سوپرمارکت",
            amount = 450000,
            type = TransactionType.EXPENSE,
            category = FinanceDefaultCategories.defaultExpenseCategories[0],
            subCategory = "سوپرمارکت",
            datePersian = "امروز",
            timePersian = "۱۱:۳۰",
            description = "خرید لبنیات و میوه تازه",
            paymentMethod = PaymentMethod.BANK_CARD,
            accountName = "کارت پاسارگاد",
            tags = listOf("روزمره", "خانه")
        ),
        TransactionItemData(
            id = "tx_2",
            title = "سوخت خودرو",
            amount = 850000,
            type = TransactionType.EXPENSE,
            category = FinanceDefaultCategories.defaultExpenseCategories[2],
            subCategory = "بنزین",
            datePersian = "دیروز",
            timePersian = "۱۶:۴۵",
            description = "بنزین سوپر جایگاه اتوبان همت",
            paymentMethod = PaymentMethod.BANK_CARD,
            accountName = "کارت پاسارگاد",
            sourceType = TransactionSourceType.FUEL,
            tags = listOf("خودرو", "سوخت")
        ),
        TransactionItemData(
            id = "tx_3",
            title = "رستوران سنتی",
            amount = 320000,
            type = TransactionType.EXPENSE,
            category = FinanceDefaultCategories.defaultExpenseCategories[0],
            subCategory = "رستوران",
            datePersian = "۲۸ شهریور",
            timePersian = "۲۱:۱۵",
            description = "شام خانوادگی",
            paymentMethod = PaymentMethod.BANK_CARD,
            accountName = "کارت سپه",
            tags = listOf("تفریح", "رستوران")
        ),
        TransactionItemData(
            id = "tx_4",
            title = "قبض برق و گاز",
            amount = 780000,
            type = TransactionType.EXPENSE,
            category = FinanceDefaultCategories.defaultExpenseCategories[4],
            subCategory = "برق",
            datePersian = "۲۶ شهریور",
            timePersian = "۱۰:۰۰",
            description = "تسویه قبوض شهریور ماه",
            paymentMethod = PaymentMethod.BANK_CARD,
            accountName = "کارت پاسارگاد",
            tags = listOf("قبوض")
        ),
        TransactionItemData(
            id = "tx_5",
            title = "خرید وسایل خانه",
            amount = 1200000,
            type = TransactionType.EXPENSE,
            category = FinanceDefaultCategories.defaultExpenseCategories[3],
            subCategory = "لوازم خانگی",
            datePersian = "۲۵ شهریور",
            timePersian = "۱۸:۲۰",
            description = "خرید وسایل بهداشتی و تمیزکننده",
            paymentMethod = PaymentMethod.BANK_CARD,
            accountName = "کارت پاسارگاد"
        ),
        TransactionItemData(
            id = "tx_6",
            title = "حقوق ماهانه شهریور",
            amount = 18000000,
            type = TransactionType.INCOME,
            category = FinanceDefaultCategories.defaultIncomeCategories[0],
            subCategory = "حقوق ماهانه",
            datePersian = "۰۱ شهریور",
            timePersian = "۰۸:۳۰",
            description = "واریز حقوق شرکت فناوران",
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            accountName = "حساب سپه (حقوق)",
            tags = listOf("حقوق", "اصلی")
        )
    )

    val initialBudgets = listOf(
        Budget(
            id = "b_food",
            title = "خوراک و رستوران",
            categoryId = "food",
            categoryTitle = "خوراک و رستوران",
            amount = 3000000,
            period = "این ماه",
            spentAmount = 770000,
            remainingAmount = 2230000,
            usagePercentage = 25,
            status = BudgetStatus.SAFE
        ),
        Budget(
            id = "b_fuel",
            title = "سوخت و بنزین",
            categoryId = "fuel",
            categoryTitle = "سوخت و بنزین",
            amount = 2000000,
            period = "این ماه",
            spentAmount = 850000,
            remainingAmount = 1150000,
            usagePercentage = 42,
            status = BudgetStatus.SAFE
        ),
        Budget(
            id = "b_entertainment",
            title = "تفریح و سرگرمی",
            categoryId = "entertainment",
            categoryTitle = "تفریح و سرگرمی",
            amount = 1500000,
            period = "این ماه",
            spentAmount = 320000,
            remainingAmount = 1180000,
            usagePercentage = 21,
            status = BudgetStatus.SAFE
        )
    )

    val initialSavingsGoals = listOf(
        SavingsGoal(
            id = "sg_1",
            title = "خرید لپ‌تاپ",
            targetAmount = 60000000,
            currentAmount = 18000000,
            targetDate = "۱۴۰۵/۱۲/۲۹",
            description = "پس‌انداز برای خرید مک‌بوک یا لپ‌تاپ برنامه‌نویسی",
            status = SavingsGoalStatus.IN_PROGRESS,
            progressPercentage = 30,
            iconEmoji = "💻"
        ),
        SavingsGoal(
            id = "sg_2",
            title = "صندوق اضطراری",
            targetAmount = 30000000,
            currentAmount = 22500000,
            targetDate = "۱۴۰۵/۰۹/۳۰",
            description = "ذخیره مالی برای ۳ ماه هزینه‌های ضروری",
            status = SavingsGoalStatus.IN_PROGRESS,
            progressPercentage = 75,
            iconEmoji = "🛡️"
        )
    )

    val initialRecurring = listOf(
        RecurringTransaction(
            id = "rec_1",
            title = "حقوق ماهانه شرکت",
            amount = 18000000,
            type = TransactionType.INCOME,
            categoryId = "salary",
            categoryTitle = "حقوق و دستمزد",
            frequency = RecurringFrequency.MONTHLY,
            startDate = "۱۴۰۵/۰۱/۰۱",
            nextExecutionDate = "۱۴۰۵/۰۷/۰۱",
            enabled = true,
            accountName = "حساب سپه (حقوق)"
        ),
        RecurringTransaction(
            id = "rec_2",
            title = "شارژ ساختمان و اینترنت",
            amount = 450000,
            type = TransactionType.EXPENSE,
            categoryId = "bill",
            categoryTitle = "قبض و اشتراک",
            frequency = RecurringFrequency.MONTHLY,
            startDate = "۱۴۰۵/۰۱/۱۵",
            nextExecutionDate = "۱۴۰۵/۰۷/۱۵",
            enabled = true,
            accountName = "کارت پاسارگاد"
        )
    )
}
