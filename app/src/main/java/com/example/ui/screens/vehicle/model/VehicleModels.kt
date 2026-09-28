package com.example.ui.screens.vehicle.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.WarningAmberLight

enum class VehicleHealthStatus(
    val title: String,
    val iconEmoji: String,
    val color: Color,
    val description: String
) {
    GOOD("وضعیت مناسب", "🟢", EmeraldPrimaryLight, "تمامی سرویس‌ها به موقع انجام شده است"),
    NEEDS_SERVICE("نیاز به سرویس", "🟡", WarningAmberLight, "موعد تعویض روغن و فیلترها نزدیک است"),
    OVERDUE("سرویس عقب افتاده", "🔴", ExpenseRoseLight, "معاینه فنی و سرویس دوره‌ای عقب افتاده است")
}

enum class ServiceReminderType(val label: String) {
    BY_DATE("بر اساس تاریخ"),
    BY_MILEAGE("بر اساس کیلومتر")
}

data class UpcomingServiceItem(
    val id: String,
    val title: String,
    val reminderType: ServiceReminderType,
    val dueText: String,
    val isUrgent: Boolean = false,
    @DrawableRes val iconRes: Int,
    val accentColor: Color
)

data class VehicleExpenseItemData(
    val id: String,
    val title: String,
    val categoryTitle: String,
    val amountFormatted: String,
    val datePersian: String,
    val odometerKmFormatted: String,
    @DrawableRes val iconRes: Int,
    val accentColor: Color
)

data class MaintenanceTimelineRecord(
    val id: String,
    val title: String,
    val datePersian: String,
    val odometerKmFormatted: String,
    val costFormatted: String,
    val serviceCenter: String,
    val partsChanged: List<String>,
    @DrawableRes val iconRes: Int
)

data class VehicleInsuranceData(
    val title: String = "بیمه شخص ثالث",
    val provider: String = "بیمه ایران",
    val policyNumber: String = "IR-982310-A",
    val daysRemaining: Int = 25,
    val isExpiringSoon: Boolean = true,
    val expiryDatePersian: String = "۱۴۰۵/۰۷/۲۴",
    val statusText: String = "فعال"
)

data class VehicleStatsData(
    val monthlyExpenseFormatted: String,
    val yearlyExpenseFormatted: String,
    val lastServiceAgo: String,
    val currentOdometerFormatted: String,
    val fuelEfficiencyAvg: String = "۷.۲ لیتر / ۱۰۰ کیلومتر"
)

data class VehicleCategoryData(
    val id: String,
    val title: String,
    val iconEmoji: String,
    @DrawableRes val iconRes: Int? = null,
    val accentColor: Color
)

data class VehicleData(
    val id: String,
    val name: String,
    val brand: String,
    val modelYear: String,
    val odometerKm: Long,
    val odometerFormatted: String,
    val licensePlate: String,
    val colorName: String,
    @DrawableRes val carImageRes: Int,
    val healthStatus: VehicleHealthStatus,
    val stats: VehicleStatsData,
    val insurance: VehicleInsuranceData,
    val upcomingServices: List<UpcomingServiceItem>,
    val recentExpenses: List<VehicleExpenseItemData>,
    val timelineRecords: List<MaintenanceTimelineRecord>
)

object VehicleMockDataSource {

    val categories = listOf(
        VehicleCategoryData("fuel", "سوخت", "⛽", R.drawable.img_3d_fuel, Color(0xFF0D9488)),
        VehicleCategoryData("repair", "تعمیرات", "🔧", null, Color(0xFFEF4444)),
        VehicleCategoryData("service", "سرویس دوره‌ای", "🛠", R.drawable.img_3d_oil, Color(0xFFF59E0B)),
        VehicleCategoryData("tire", "لاستیک", "🛞", R.drawable.img_3d_tire, Color(0xFF6366F1)),
        VehicleCategoryData("battery", "باتری", "🔋", R.drawable.img_3d_battery, Color(0xFF10B981)),
        VehicleCategoryData("insurance", "بیمه", "📄", R.drawable.img_3d_insurance, Color(0xFF3B82F6)),
        VehicleCategoryData("inspection", "معاینه فنی", "🔍", null, Color(0xFF8B5CF6)),
        VehicleCategoryData("carwash", "کارواش", "🧽", null, Color(0xFF06B6D4)),
        VehicleCategoryData("misc", "سایر هزینه‌ها", "📦", null, Color(0xFF64748B))
    )

    // Vehicle 1: پژو ۲۰۶ (مدل ۱۳۹۹، ۱۲۵,۸۰۰ کیلومتر)
    val peugeot206 = VehicleData(
        id = "veh_peugeot_206",
        name = "پژو ۲۰۶ تیپ ۵",
        brand = "ایران خودرو",
        modelYear = "۱۳۹۹",
        odometerKm = 125800,
        odometerFormatted = "۱۲۵,۸۰۰ کیلومتر",
        licensePlate = "ایران ۱۱ - ۷۸۹ ج ۲۳",
        colorName = "خاکستری متالیک",
        carImageRes = R.drawable.img_3d_car_peugeot,
        healthStatus = VehicleHealthStatus.NEEDS_SERVICE,
        stats = VehicleStatsData(
            monthlyExpenseFormatted = "۱,۸۵۰,۰۰۰ تومان",
            yearlyExpenseFormatted = "۱۸,۵۰۰,۰۰۰ تومان",
            lastServiceAgo = "۱۴ روز پیش",
            currentOdometerFormatted = "۱۲۵,۸۰۰ کیلومتر",
            fuelEfficiencyAvg = "۷.۴ لیتر / ۱۰۰ کیلومتر"
        ),
        insurance = VehicleInsuranceData(
            title = "بیمه شخص ثالث (پژو ۲۰۶)",
            provider = "بیمه ایران",
            policyNumber = "IR-982310-A",
            daysRemaining = 25,
            isExpiringSoon = true,
            expiryDatePersian = "۱۴۰۵/۰۷/۲۴",
            statusText = "فعال (۲۵ روز باقی‌مانده)"
        ),
        upcomingServices = listOf(
            UpcomingServiceItem(
                id = "serv_1",
                title = "تعویض روغن موتور و فیلترها",
                reminderType = ServiceReminderType.BY_MILEAGE,
                dueText = "۱,۲۰۰ کیلومتر دیگر",
                isUrgent = false,
                iconRes = R.drawable.img_3d_oil,
                accentColor = WarningAmberLight
            ),
            UpcomingServiceItem(
                id = "serv_2",
                title = "معاینه فنی سالانه",
                reminderType = ServiceReminderType.BY_DATE,
                dueText = "۱۵ روز دیگر",
                isUrgent = true,
                iconRes = R.drawable.img_3d_car,
                accentColor = ExpenseRoseLight
            ),
            UpcomingServiceItem(
                id = "serv_3",
                title = "بررسی و جابجایی لاستیک‌ها",
                reminderType = ServiceReminderType.BY_DATE,
                dueText = "۲۵ روز دیگر",
                isUrgent = false,
                iconRes = R.drawable.img_3d_tire,
                accentColor = InfoIndigoLight
            )
        ),
        recentExpenses = listOf(
            VehicleExpenseItemData(
                id = "exp_1",
                title = "بنزین سوپر (جایگاه آزادی)",
                categoryTitle = "سوخت",
                amountFormatted = "−۸۵۰,۰۰۰ تومان",
                datePersian = "امروز",
                odometerKmFormatted = "۱۲۵,۸۰۰ کیلومتر",
                iconRes = R.drawable.img_3d_fuel,
                accentColor = EmeraldPrimaryLight
            ),
            VehicleExpenseItemData(
                id = "exp_2",
                title = "سرویس روغن موتور و فیلتر هوا",
                categoryTitle = "سرویس دوره‌ای",
                amountFormatted = "−۱,۵۰۰,۰۰۰ تومان",
                datePersian = "۳ روز پیش",
                odometerKmFormatted = "۱۲۵,۳۵۰ کیلومتر",
                iconRes = R.drawable.img_3d_oil,
                accentColor = WarningAmberLight
            ),
            VehicleExpenseItemData(
                id = "exp_3",
                title = "خرید دو حلقه لاستیک بارز",
                categoryTitle = "لاستیک",
                amountFormatted = "−۶,۸۰۰,۰۰۰ تومان",
                datePersian = "۱۲ روز پیش",
                odometerKmFormatted = "۱۲۴,۵۰۰ کیلومتر",
                iconRes = R.drawable.img_3d_tire,
                accentColor = InfoIndigoLight
            )
        ),
        timelineRecords = listOf(
            MaintenanceTimelineRecord(
                id = "hist_1",
                title = "تعویض روغن موتور و فیلتر روغن",
                datePersian = "۱۴۰۵/۰۶/۱۰",
                odometerKmFormatted = "۱۲۴,۸۰۰ کیلومتر",
                costFormatted = "۱,۵۰۰,۰۰۰ تومان",
                serviceCenter = "تعمیرگاه تخصصی کارن",
                partsChanged = listOf("روغن بهران رانا 10W-40", "فیلتر روغن سرکان", "فیلتر هوا"),
                iconRes = R.drawable.img_3d_oil
            ),
            MaintenanceTimelineRecord(
                id = "hist_2",
                title = "تعویض باتری ۶۰ آمپر",
                datePersian = "۱۴۰۵/۰۴/۲۵",
                odometerKmFormatted = "۱۲۰,۰۰۰ کیلومتر",
                costFormatted = "۳,۲۰۰,۰۰۰ تومان",
                serviceCenter = "فروشگاه امداد باتری",
                partsChanged = listOf("باتری سوزوکی ۶۰ آمپر با ۱۸ ماه گارانتی"),
                iconRes = R.drawable.img_3d_battery
            ),
            MaintenanceTimelineRecord(
                id = "hist_3",
                title = "سرویس دوره‌ای و تعویض تسمه تایم",
                datePersian = "۱۴۰۵/۰۲/۱۵",
                odometerKmFormatted = "۱۱۵,۰۰۰ کیلومتر",
                costFormatted = "۲,۸۰۰,۰۰۰ تومان",
                serviceCenter = "نمایندگی مجاز ایران خودرو",
                partsChanged = listOf("کیت تسمه تایم ایساکو", "هرزگردها", "تسمه دینام"),
                iconRes = R.drawable.img_3d_car
            )
        )
    )

    // Vehicle 2: دنا پلاس (مدل ۱۴۰۱، ۸۲,۴۰۰ کیلومتر)
    val denaPlus = VehicleData(
        id = "veh_dena_plus",
        name = "دنا پلاس توربو",
        brand = "ایران خودرو",
        modelYear = "۱۴۰۱",
        odometerKm = 82400,
        odometerFormatted = "۸۲,۴۰۰ کیلومتر",
        licensePlate = "ایران ۲۲ - ۴۵۶ ب ۶۷",
        colorName = "آبی کبود متالیک",
        carImageRes = R.drawable.img_3d_car_dena,
        healthStatus = VehicleHealthStatus.GOOD,
        stats = VehicleStatsData(
            monthlyExpenseFormatted = "۱,۱۲۰,۰۰۰ تومان",
            yearlyExpenseFormatted = "۱۴,۲۰۰,۰۰۰ تومان",
            lastServiceAgo = "۵ روز پیش",
            currentOdometerFormatted = "۸۲,۴۰۰ کیلومتر",
            fuelEfficiencyAvg = "۸.۱ لیتر / ۱۰۰ کیلومتر"
        ),
        insurance = VehicleInsuranceData(
            title = "بیمه شخص ثالث و بدنه (دنا پلاس)",
            provider = "بیمه آسیا",
            policyNumber = "AS-554109-D",
            daysRemaining = 120,
            isExpiringSoon = false,
            expiryDatePersian = "۱۴۰۵/۱۰/۱۵",
            statusText = "فعال (۱۲۰ روز باقی‌مانده)"
        ),
        upcomingServices = listOf(
            UpcomingServiceItem(
                id = "dena_serv_1",
                title = "تعویض شمع و وایر توربو",
                reminderType = ServiceReminderType.BY_MILEAGE,
                dueText = "۴,۵۰۰ کیلومتر دیگر",
                isUrgent = false,
                iconRes = R.drawable.img_3d_car,
                accentColor = InfoIndigoLight
            ),
            UpcomingServiceItem(
                id = "dena_serv_2",
                title = "بررسی لنت‌های ترمز جلو",
                reminderType = ServiceReminderType.BY_DATE,
                dueText = "۴۵ روز دیگر",
                isUrgent = false,
                iconRes = R.drawable.img_3d_tire,
                accentColor = WarningAmberLight
            )
        ),
        recentExpenses = listOf(
            VehicleExpenseItemData(
                id = "dena_exp_1",
                title = "سوخت‌گیری بنزین معمولی",
                categoryTitle = "سوخت",
                amountFormatted = "−۱۲۰,۰۰۰ تومان",
                datePersian = "دیروز",
                odometerKmFormatted = "۸۲,۴۰۰ کیلومتر",
                iconRes = R.drawable.img_3d_fuel,
                accentColor = EmeraldPrimaryLight
            ),
            VehicleExpenseItemData(
                id = "dena_exp_2",
                title = "کارواش نانو VIP",
                categoryTitle = "کارواش",
                amountFormatted = "−۲۵۰,۰۰۰ تومان",
                datePersian = "۴ روز پیش",
                odometerKmFormatted = "۸۲,۱۰۰ کیلومتر",
                iconRes = R.drawable.img_3d_car,
                accentColor = Color(0xFF06B6D4)
            )
        ),
        timelineRecords = listOf(
            MaintenanceTimelineRecord(
                id = "dena_hist_1",
                title = "سرویس ۸۰ هزار کیلومتر",
                datePersian = "۱۴۰۵/۰۵/۲۰",
                odometerKmFormatted = "۸۰,۰۰۰ کیلومتر",
                costFormatted = "۲,۱۰۰,۰۰۰ تومان",
                serviceCenter = "نمایندگی ۱۰۰۴ ایران خودرو",
                partsChanged = listOf("روغن کاسترول 5W-40", "فیلتر بنزین", "فیلتر کابین"),
                iconRes = R.drawable.img_3d_oil
            )
        )
    )

    val sampleVehicles = listOf(peugeot206, denaPlus)
}
