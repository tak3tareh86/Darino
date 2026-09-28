package com.example.ui.screens.reports.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.WarningAmberLight

enum class ReportPeriod(val title: String, val subtitle: String) {
    THIS_WEEK("این هفته", "۲۴ تا ۳۰ مهر ۱۴۰۴"),
    THIS_MONTH("این ماه", "مهر ۱۴۰۴"),
    THIS_SEASON("این فصل", "پاییز ۱۴۰۴"),
    THIS_YEAR("امسال", "سال ۱۴۰۴"),
    CUSTOM("سفارشی", "بازه انتخابی")
}

data class ReportsSummaryData(
    val totalIncomeFormatted: String,
    val totalExpenseFormatted: String,
    val remainingBalanceFormatted: String,
    val savingsRatePercentage: String,
    val incomeChangePercent: String,
    val isIncomeGrowth: Boolean,
    val expenseChangePercent: String,
    val isExpenseIncreased: Boolean,
    val periodLabel: String
)

data class TrendPoint(
    val label: String,
    val incomeAmount: Long,
    val expenseAmount: Long,
    val incomeFormatted: String,
    val expenseFormatted: String,
    val balanceFormatted: String
)

data class ExpenseCategoryReportItem(
    val id: String,
    val title: String,
    val amountFormatted: String,
    val amountRaw: Long,
    val percentage: Float,
    val percentageFormatted: String,
    val transactionCount: Int,
    @DrawableRes val iconRes: Int,
    val accentColor: Color
)

data class TopExpenseReportItem(
    val id: String,
    val title: String,
    val categoryTitle: String,
    val datePersian: String,
    val amountFormatted: String,
    @DrawableRes val iconRes: Int,
    val accentColor: Color,
    val percentageOfTotal: String
)

data class InstallmentCategoryItem(
    val id: String,
    val title: String,
    val totalAmountFormatted: String,
    val paidAmountFormatted: String,
    val remainingAmountFormatted: String,
    val progress: Float,
    @DrawableRes val iconRes: Int,
    val accentColor: Color
)

data class InstallmentReportData(
    val totalAmountFormatted: String,
    val paidAmountFormatted: String,
    val remainingAmountFormatted: String,
    val overallProgress: Float,
    val activeInstallmentsCount: Int,
    val upcomingDueCount: Int,
    val categories: List<InstallmentCategoryItem>
)

data class VehicleExpenseBreakdownItem(
    val title: String,
    val amountFormatted: String,
    val percentage: Float,
    val percentageFormatted: String,
    @DrawableRes val iconRes: Int,
    val color: Color
)

data class VehicleComparisonData(
    val vehicleId: String,
    val vehicleName: String,
    val modelYear: String,
    val totalCostFormatted: String,
    val fuelCostFormatted: String,
    val maintenanceCostFormatted: String,
    val insuranceCostFormatted: String,
    val otherCostFormatted: String,
    val totalKmDriven: String,
    @DrawableRes val imageRes: Int
)

data class VehicleExpenseReportData(
    val totalVehicleExpenseFormatted: String,
    val vehicleExpenseSharePercent: String,
    val breakdown: List<VehicleExpenseBreakdownItem>,
    val vehicleComparisons: List<VehicleComparisonData>
)

enum class InsightType {
    POSITIVE,
    WARNING,
    INFO
}

data class FinancialInsightItem(
    val id: String,
    val title: String,
    val description: String,
    val tagText: String,
    val type: InsightType,
    val percentageBadge: String?,
    @DrawableRes val iconRes: Int
)

data class PeriodComparisonMetric(
    val id: String,
    val title: String,
    val currentValue: String,
    val previousValue: String,
    val differenceFormatted: String,
    val percentageFormatted: String,
    val isIncreased: Boolean,
    val isPositiveForUser: Boolean,
    @DrawableRes val iconRes: Int
)

data class ReportTransactionItemData(
    val id: String,
    val title: String,
    val category: String,
    val account: String,
    val datePersian: String,
    val amountFormatted: String,
    val isExpense: Boolean,
    @DrawableRes val iconRes: Int,
    val accentColor: Color
)

data class FilterState(
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val selectedAccount: String = "همه حساب‌ها",
    val selectedCategory: String = "همه دسته‌ها",
    val selectedVehicle: String = "همه خودروها",
    val transactionType: String = "همه تراکنش‌ها",
    val minAmount: String = "",
    val maxAmount: String = ""
)

object ReportsMockDataSource {

    fun getSummaryForPeriod(period: ReportPeriod): ReportsSummaryData {
        return when (period) {
            ReportPeriod.THIS_WEEK -> ReportsSummaryData(
                totalIncomeFormatted = "۴,۵۰۰,۰۰۰",
                totalExpenseFormatted = "۲,۱۵۰,۰۰۰",
                remainingBalanceFormatted = "۲,۳۵۰,۰۰۰",
                savingsRatePercentage = "۵۲٪",
                incomeChangePercent = "+۸.۲٪",
                isIncomeGrowth = true,
                expenseChangePercent = "-۴.۵٪",
                isExpenseIncreased = false,
                periodLabel = "هفته جاری (۲۴ تا ۳۰ مهر)"
            )
            ReportPeriod.THIS_MONTH -> ReportsSummaryData(
                totalIncomeFormatted = "۱۸,۰۰۰,۰۰۰",
                totalExpenseFormatted = "۹,۵۰۰,۰۰۰",
                remainingBalanceFormatted = "۸,۵۰۰,۰۰۰",
                savingsRatePercentage = "۴۷٪",
                incomeChangePercent = "+۱۲.۴٪",
                isIncomeGrowth = true,
                expenseChangePercent = "+۳.۱٪",
                isExpenseIncreased = true,
                periodLabel = "ماه جاری (مهر ۱۴۰۴)"
            )
            ReportPeriod.THIS_SEASON -> ReportsSummaryData(
                totalIncomeFormatted = "۵۲,۰۰۰,۰۰۰",
                totalExpenseFormatted = "۲۹,۸۰۰,۰۰۰",
                remainingBalanceFormatted = "۲۲,۲۰۰,۰۰۰",
                savingsRatePercentage = "۴۳٪",
                incomeChangePercent = "+۱۵.۰٪",
                isIncomeGrowth = true,
                expenseChangePercent = "+۶.۴٪",
                isExpenseIncreased = true,
                periodLabel = "فصل پاییز ۱۴۰۴"
            )
            ReportPeriod.THIS_YEAR -> ReportsSummaryData(
                totalIncomeFormatted = "۲۱۰,۰۰۰,۰۰۰",
                totalExpenseFormatted = "۱۱۸,۵۰۰,۰۰۰",
                remainingBalanceFormatted = "۹۱,۵۰۰,۰۰۰",
                savingsRatePercentage = "۴۴٪",
                incomeChangePercent = "+۲۲.۵٪",
                isIncomeGrowth = true,
                expenseChangePercent = "+۱۰.۲٪",
                isExpenseIncreased = true,
                periodLabel = "سال ۱۴۰۴"
            )
            ReportPeriod.CUSTOM -> ReportsSummaryData(
                totalIncomeFormatted = "۱۲,۲۰۰,۰۰۰",
                totalExpenseFormatted = "۶,۸۰۰,۰۰۰",
                remainingBalanceFormatted = "۵,۴۰۰,۰۰۰",
                savingsRatePercentage = "۴۴٪",
                incomeChangePercent = "+۵.۰٪",
                isIncomeGrowth = true,
                expenseChangePercent = "-۲.۰٪",
                isExpenseIncreased = false,
                periodLabel = "بازه انتخابی"
            )
        }
    }

    fun getTrendPointsForPeriod(period: ReportPeriod): List<TrendPoint> {
        return when (period) {
            ReportPeriod.THIS_WEEK -> listOf(
                TrendPoint("ش", 800_000, 320_000, "۸۰۰,۰۰۰", "۳۲۰,۰۰۰", "+۴۸۰,۰۰۰"),
                TrendPoint("ی", 1_200_000, 450_000, "۱,۲۰۰,۰۰۰", "۴۵۰,۰۰۰", "+۷۵۰,۰۰۰"),
                TrendPoint("د", 400_000, 280_000, "۴۰۰,۰۰۰", "۲۸۰,۰۰۰", "+۱۲۰,۰۰۰"),
                TrendPoint("س", 950_000, 520_000, "۹۵۰,۰۰۰", "۵۲۰,۰۰۰", "+۴۳۰,۰۰۰"),
                TrendPoint("چ", 600_000, 310_000, "۶۰۰,۰۰۰", "۳۱۰,۰۰۰", "+۲۹۰,۰۰۰"),
                TrendPoint("پ", 350_000, 180_000, "۳۵۰,۰۰۰", "۱۸۰,۰۰۰", "+۱۷۰,۰۰۰"),
                TrendPoint("ج", 200_000, 90_000, "۲۰۰,۰۰۰", "۹۰,۰۰۰", "+۱۱۰,۰۰۰")
            )
            ReportPeriod.THIS_MONTH -> listOf(
                TrendPoint("هفته ۱", 4_200_000, 2_100_000, "۴,۲۰۰,۰۰۰", "۲,۱۰۰,۰۰۰", "+۲,۱۰۰,۰۰۰"),
                TrendPoint("هفته ۲", 5_100_000, 2_850_000, "۵,۱۰۰,۰۰۰", "۲,۸۵۰,۰۰۰", "+۲,۲۵۰,۰۰۰"),
                TrendPoint("هفته ۳", 4_800_000, 2_350_000, "۴,۸۰۰,۰۰۰", "۲,۳۵۰,۰۰۰", "+۲,۴۵۰,۰۰۰"),
                TrendPoint("هفته ۴", 3_900_000, 2_200_000, "۳,۹۰۰,۰۰۰", "۲,۲۰۰,۰۰۰", "+۱,۷۰۰,۰۰۰")
            )
            ReportPeriod.THIS_SEASON -> listOf(
                TrendPoint("مهر", 18_000_000, 9_500_000, "۱۸,۰۰۰,۰۰۰", "۹,۵۰۰,۰۰۰", "+۸,۵۰۰,۰۰۰"),
                TrendPoint("آبان", 16_500_000, 10_200_000, "۱۶,۵۰۰,۰۰۰", "۱۰,۲۰۰,۰۰۰", "+۶,۳۰۰,۰۰۰"),
                TrendPoint("آذر", 17_500_000, 10_100_000, "۱۷,۵۰۰,۰۰۰", "۱۰,۱۰۰,۰۰۰", "+۷,۴۰۰,۰۰۰")
            )
            ReportPeriod.THIS_YEAR -> listOf(
                TrendPoint("فروردین", 15_000_000, 8_500_000, "۱۵,۰۰۰,۰۰۰", "۸,۵۰۰,۰۰۰", "+۶,۵۰۰,۰۰۰"),
                TrendPoint("اردیبهشت", 16_500_000, 9_200_000, "۱۶,۵۰۰,۰۰۰", "۹,۲۰۰,۰۰۰", "+۷,۳۰۰,۰۰۰"),
                TrendPoint("خرداد", 17_000_000, 9_800_000, "۱۷,۰۰۰,۰۰۰", "۹,۸۰۰,۰۰۰", "+۷,۲۰۰,۰۰۰"),
                TrendPoint("تیر", 18_200_000, 10_400_000, "۱۸,۲۰۰,۰۰۰", "۱۰,۴۰۰,۰۰۰", "+۷,۸۰۰,۰۰۰"),
                TrendPoint("مرداد", 17_800_000, 10_100_000, "۱۷,۸۰۰,۰۰۰", "۱۰,۱۰۰,۰۰۰", "+۷,۷۰۰,۰۰۰"),
                TrendPoint("شهریور", 19_000_000, 11_000_000, "۱۹,۰۰۰,۰۰۰", "۱۱,۰۰۰,۰۰۰", "+۸,۰۰۰,۰۰۰"),
                TrendPoint("مهر", 18_000_000, 9_500_000, "۱۸,۰۰۰,۰۰۰", "۹,۵۰۰,۰۰۰", "+۸,۵۰۰,۰۰۰")
            )
            ReportPeriod.CUSTOM -> listOf(
                TrendPoint("بخش ۱", 3_000_000, 1_700_000, "۳,۰۰۰,۰۰۰", "۱,۷۰۰,۰۰۰", "+۱,۳۰۰,۰۰۰"),
                TrendPoint("بخش ۲", 4_500_000, 2_400_000, "۴,۵۰۰,۰۰۰", "۲,۴۰۰,۰۰۰", "+۲,۱۰۰,۰۰۰"),
                TrendPoint("بخش ۳", 4_700_000, 2_700_000, "۴,۷۰۰,۰۰۰", "۲,۷۰۰,۰۰۰", "+۲,۰۰۰,۰۰۰")
            )
        }
    }

    val expenseCategories = listOf(
        ExpenseCategoryReportItem(
            id = "cat_home",
            title = "مسکن و اجاره",
            amountFormatted = "۱,۸۰۰,۰۰۰ تومان",
            amountRaw = 1_800_000,
            percentage = 0.28f,
            percentageFormatted = "۲۸٪",
            transactionCount = 2,
            iconRes = R.drawable.img_3d_home,
            accentColor = InfoIndigoLight
        ),
        ExpenseCategoryReportItem(
            id = "cat_car",
            title = "خودرو و ترابری",
            amountFormatted = "۱,۴۵۰,۰۰۰ تومان",
            amountRaw = 1_450_000,
            percentage = 0.22f,
            percentageFormatted = "۲۲٪",
            transactionCount = 5,
            iconRes = R.drawable.img_3d_car,
            accentColor = WarningAmberLight
        ),
        ExpenseCategoryReportItem(
            id = "cat_food",
            title = "خوراک و سوپرمارکت",
            amountFormatted = "۱,۲۰۰,۰۰۰ تومان",
            amountRaw = 1_200_000,
            percentage = 0.18f,
            percentageFormatted = "۱۸٪",
            transactionCount = 14,
            iconRes = R.drawable.img_3d_food,
            accentColor = SuccessGreenLight
        ),
        ExpenseCategoryReportItem(
            id = "cat_shopping",
            title = "خرید و پوشاک",
            amountFormatted = "۹۰۰,۰۰۰ تومان",
            amountRaw = 900_000,
            percentage = 0.14f,
            percentageFormatted = "۱۴٪",
            transactionCount = 6,
            iconRes = R.drawable.img_3d_shopping,
            accentColor = EmeraldPrimaryLight
        ),
        ExpenseCategoryReportItem(
            id = "cat_bills",
            title = "قبوض و اشتراک‌ها",
            amountFormatted = "۶۵۰,۰۰۰ تومان",
            amountRaw = 650_000,
            percentage = 0.10f,
            percentageFormatted = "۱۰٪",
            transactionCount = 3,
            iconRes = R.drawable.img_3d_card,
            accentColor = ExpenseRoseLight
        ),
        ExpenseCategoryReportItem(
            id = "cat_other",
            title = "سایر هزینه‌ها",
            amountFormatted = "۵۰۰,۰۰۰ تومان",
            amountRaw = 500_000,
            percentage = 0.08f,
            percentageFormatted = "۸٪",
            transactionCount = 4,
            iconRes = R.drawable.img_3d_wallet,
            accentColor = Color(0xFF8B5CF6)
        )
    )

    val topExpenses = listOf(
        TopExpenseReportItem(
            id = "top_1",
            title = "تعمیر جلوبندی خودرو",
            categoryTitle = "تعمیرات خودرو",
            datePersian = "۳ روز پیش",
            amountFormatted = "۱,۲۵۰,۰۰۰ تومان",
            iconRes = R.drawable.img_3d_oil,
            accentColor = WarningAmberLight,
            percentageOfTotal = "۱۳.۱٪"
        ),
        TopExpenseReportItem(
            id = "top_2",
            title = "خرید مایحتاج منزل",
            categoryTitle = "خوراک و هایپرمارکت",
            datePersian = "دیروز",
            amountFormatted = "۸۲۰,۰۰۰ تومان",
            iconRes = R.drawable.img_3d_shopping,
            accentColor = EmeraldPrimaryLight,
            percentageOfTotal = "۸.۶٪"
        ),
        TopExpenseReportItem(
            id = "top_3",
            title = "سوخت و بنزین سوپر",
            categoryTitle = "سوخت خودرو",
            datePersian = "امروز",
            amountFormatted = "۴۵۰,۰۰۰ تومان",
            iconRes = R.drawable.img_3d_fuel,
            accentColor = SuccessGreenLight,
            percentageOfTotal = "۴.۷٪"
        )
    )

    val installmentReport = InstallmentReportData(
        totalAmountFormatted = "۷,۵۰۰,۰۰۰ تومان",
        paidAmountFormatted = "۴,۲۰۰,۰۰۰ تومان",
        remainingAmountFormatted = "۳,۳۰۰,۰۰۰ تومان",
        overallProgress = 0.56f,
        activeInstallmentsCount = 6,
        upcomingDueCount = 2,
        categories = listOf(
            InstallmentCategoryItem(
                id = "inst_bank",
                title = "وام بانکی (مسکن)",
                totalAmountFormatted = "۳,۵۰۰,۰۰۰ تومان",
                paidAmountFormatted = "۲,۱۰۰,۰۰۰ تومان",
                remainingAmountFormatted = "۱,۴۰۰,۰۰۰ تومان",
                progress = 0.60f,
                iconRes = R.drawable.img_3d_bank,
                accentColor = InfoIndigoLight
            ),
            InstallmentCategoryItem(
                id = "inst_home",
                title = "صندوق قرعه‌کشی خانگی",
                totalAmountFormatted = "۲,۰۰۰,۰۰۰ تومان",
                paidAmountFormatted = "۱,۲۰۰,۰۰۰ تومان",
                remainingAmountFormatted = "۸۰۰,۰۰۰ تومان",
                progress = 0.60f,
                iconRes = R.drawable.img_3d_installment,
                accentColor = EmeraldPrimaryLight
            ),
            InstallmentCategoryItem(
                id = "inst_ins",
                title = "اقساط بیمه بدنه",
                totalAmountFormatted = "۱,۲۰۰,۰۰۰ تومان",
                paidAmountFormatted = "۶۰۰,۰۰۰ تومان",
                remainingAmountFormatted = "۶۰۰,۰۰۰ تومان",
                progress = 0.50f,
                iconRes = R.drawable.img_3d_insurance,
                accentColor = WarningAmberLight
            ),
            InstallmentCategoryItem(
                id = "inst_misc",
                title = "اقساط متفرقه و کالا",
                totalAmountFormatted = "۸۰۰,۰۰۰ تومان",
                paidAmountFormatted = "۳۰۰,۰۰۰ تومان",
                remainingAmountFormatted = "۵۰۰,۰۰۰ تومان",
                progress = 0.37f,
                iconRes = R.drawable.img_3d_card,
                accentColor = Color(0xFFEC4899)
            )
        )
    )

    val vehicleExpenseReport = VehicleExpenseReportData(
        totalVehicleExpenseFormatted = "۴,۳۰۰,۰۰۰ تومان",
        vehicleExpenseSharePercent = "۳۱٪ از کل هزینه‌ها",
        breakdown = listOf(
            VehicleExpenseBreakdownItem("سوخت", "۱,۴۵۰,۰۰۰ تومان", 0.34f, "۳۴٪", R.drawable.img_3d_fuel, EmeraldPrimaryLight),
            VehicleExpenseBreakdownItem("تعمیرات", "۱,۲۵۰,۰۰۰ تومان", 0.29f, "۲۹٪", R.drawable.img_3d_oil, WarningAmberLight),
            VehicleExpenseBreakdownItem("سرویس دوره‌ای", "۸۰۰,۰۰۰ تومان", 0.19f, "۱۹٪", R.drawable.img_3d_oil, InfoIndigoLight),
            VehicleExpenseBreakdownItem("بیمه شخص ثالث", "۵۰۰,۰۰۰ تومان", 0.12f, "۱۲٪", R.drawable.img_3d_insurance, Color(0xFF8B5CF6)),
            VehicleExpenseBreakdownItem("لاستیک و سایر", "۳۰۰,۰۰۰ تومان", 0.06f, "۶٪", R.drawable.img_3d_tire, Color(0xFFF43F5E))
        ),
        vehicleComparisons = listOf(
            VehicleComparisonData(
                vehicleId = "veh_peugeot",
                vehicleName = "پژو ۲۰۶ تیپ ۵",
                modelYear = "مدل ۱۳۹۹",
                totalCostFormatted = "۲,۸۰۰,۰۰۰ تومان",
                fuelCostFormatted = "۹۵۰,۰۰۰ تومان",
                maintenanceCostFormatted = "۱,۲۵۰,۰۰۰ تومان",
                insuranceCostFormatted = "۴۰۰,۰۰۰ تومان",
                otherCostFormatted = "۲۰۰,۰۰۰ تومان",
                totalKmDriven = "۱,۴۲۰ کیلومتر این ماه",
                imageRes = R.drawable.img_3d_car_peugeot
            ),
            VehicleComparisonData(
                vehicleId = "veh_dena",
                vehicleName = "دنا پلاس توربو",
                modelYear = "مدل ۱۴۰۱",
                totalCostFormatted = "۱,۵۰۰,۰۰۰ تومان",
                fuelCostFormatted = "۵۰۰,۰۰۰ تومان",
                maintenanceCostFormatted = "۸۰۰,۰۰۰ تومان",
                insuranceCostFormatted = "۱۰۰,۰۰۰ تومان",
                otherCostFormatted = "۱۰۰,۰۰۰ تومان",
                totalKmDriven = "۸۵۰ کیلومتر این ماه",
                imageRes = R.drawable.img_3d_car_dena
            )
        )
    )

    val financialInsights = listOf(
        FinancialInsightItem(
            id = "ins_1",
            title = "رشد چشمگیر پس‌انداز",
            description = "نرخ پس‌انداز شما در این دوره به ۴۷٪ رسیده است که نسبت به میانگین ۳ ماه گذشته ۱۲٪ بهبود یافته است.",
            tagText = "پیشنهاد طلایی",
            type = InsightType.POSITIVE,
            percentageBadge = "+۱۲.۴٪",
            iconRes = R.drawable.img_3d_chart
        ),
        FinancialInsightItem(
            id = "ins_2",
            title = "افزایش هزینه‌های نگهداری خودرو",
            description = "هزینه تعمیرات پژو ۲۰۶ نسبت به ماه قبل ۱۸٪ افزایش داشته است. بررسی دوره‌ای جلوبندی می‌تواند از هزینه‌های آتی بکاهد.",
            tagText = "هزینه خودرو",
            type = InsightType.WARNING,
            percentageBadge = "+۱۸٪",
            iconRes = R.drawable.img_3d_car
        ),
        FinancialInsightItem(
            id = "ins_3",
            title = "بهینه‌سازی مصرف سوخت",
            description = "هزینه سوخت مصرفی در این ماه به دلیل کاهش ترددهای درون‌شهری ۶٪ کاهش یافته است.",
            tagText = "مدیریت سوخت",
            type = InsightType.POSITIVE,
            percentageBadge = "-۶.۰٪",
            iconRes = R.drawable.img_3d_fuel
        ),
        FinancialInsightItem(
            id = "ins_4",
            title = "بیشترین سهم هزینه‌ها: مسکن",
            description = "هزینه مسکن و اجاره ۲۸٪ از کل مخارج ماه جاری را تشکیل داده و در رتبه نخست دسته‌بندی‌ها قرار دارد.",
            tagText = "تمرکز مخارج",
            type = InsightType.INFO,
            percentageBadge = "۲۸٪ سهم",
            iconRes = R.drawable.img_3d_home
        )
    )

    val periodComparisonMetrics = listOf(
        PeriodComparisonMetric(
            id = "comp_inc",
            title = "مجموع درآمد",
            currentValue = "۱۸.۰ میلیون",
            previousValue = "۱۶.۰ میلیون",
            differenceFormatted = "+۲.۰ م",
            percentageFormatted = "+۱۲.۵٪",
            isIncreased = true,
            isPositiveForUser = true,
            iconRes = R.drawable.img_3d_wallet
        ),
        PeriodComparisonMetric(
            id = "comp_exp",
            title = "مجموع هزینه‌ها",
            currentValue = "۹.۵ میلیون",
            previousValue = "۱۰.۸ میلیون",
            differenceFormatted = "-۱.۳ م",
            percentageFormatted = "-۱۲.۰٪",
            isIncreased = false,
            isPositiveForUser = true,
            iconRes = R.drawable.img_3d_card
        ),
        PeriodComparisonMetric(
            id = "comp_sav",
            title = "پس‌انداز و مانده",
            currentValue = "۸.۵ میلیون",
            previousValue = "۵.۲ میلیون",
            differenceFormatted = "+۳.۳ م",
            percentageFormatted = "+۶۳.۵٪",
            isIncreased = true,
            isPositiveForUser = true,
            iconRes = R.drawable.img_3d_bank
        ),
        PeriodComparisonMetric(
            id = "comp_veh",
            title = "هزینه‌های خودرو",
            currentValue = "۴.۳ میلیون",
            previousValue = "۳.۶ میلیون",
            differenceFormatted = "+۰.۷ م",
            percentageFormatted = "+۱۹.۴٪",
            isIncreased = true,
            isPositiveForUser = false,
            iconRes = R.drawable.img_3d_car
        ),
        PeriodComparisonMetric(
            id = "comp_inst",
            title = "پرداخت اقساط",
            currentValue = "۴.۲ میلیون",
            previousValue = "۴.۸ میلیون",
            differenceFormatted = "-۰.۶ م",
            percentageFormatted = "-۱۲.۵٪",
            isIncreased = false,
            isPositiveForUser = true,
            iconRes = R.drawable.img_3d_installment
        )
    )

    val sampleReportTransactions = listOf(
        ReportTransactionItemData(
            id = "tx_1",
            title = "تعمیر و تعویض لنت ترمز",
            category = "خودرو",
            account = "کارت ملی",
            datePersian = "۲۸ مهر ۱۴۰۴",
            amountFormatted = "−۱,۲۵۰,۰۰۰ تومان",
            isExpense = true,
            iconRes = R.drawable.img_3d_oil,
            accentColor = WarningAmberLight
        ),
        ReportTransactionItemData(
            id = "tx_2",
            title = "واریز حقوق ماهانه",
            category = "درآمد",
            account = "حساب پاسارگاد",
            datePersian = "۲۷ مهر ۱۴۰۴",
            amountFormatted = "+۱۸,۰۰۰,۰۰۰ تومان",
            isExpense = false,
            iconRes = R.drawable.img_3d_bank,
            accentColor = EmeraldPrimaryLight
        ),
        ReportTransactionItemData(
            id = "tx_3",
            title = "خرید سوپرمارکت و میوه",
            category = "خوراک",
            account = "کارت سامان",
            datePersian = "۲۶ مهر ۱۴۰۴",
            amountFormatted = "−۸۲۰,۰۰۰ تومان",
            isExpense = true,
            iconRes = R.drawable.img_3d_food,
            accentColor = SuccessGreenLight
        ),
        ReportTransactionItemData(
            id = "tx_4",
            title = "بنزین سوپر جایگاه ۳۲",
            category = "خودرو",
            account = "کارت ملی",
            datePersian = "۲۵ مهر ۱۴۰۴",
            amountFormatted = "−۴۵۰,۰۰۰ تومان",
            isExpense = true,
            iconRes = R.drawable.img_3d_fuel,
            accentColor = EmeraldPrimaryLight
        ),
        ReportTransactionItemData(
            id = "tx_5",
            title = "قسط وام مسکن",
            category = "اقساط",
            account = "حساب مسکن",
            datePersian = "۲۴ مهر ۱۴۰۴",
            amountFormatted = "−۲,۱۰۰,۰۰۰ تومان",
            isExpense = true,
            iconRes = R.drawable.img_3d_installment,
            accentColor = InfoIndigoLight
        )
    )
}
