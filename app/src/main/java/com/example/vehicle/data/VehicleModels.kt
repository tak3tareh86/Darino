package com.example.vehicle.data

import androidx.annotation.DrawableRes
import com.example.R

/**
 * Main Vehicle Entity
 */
data class VehicleEntity(
    val id: String,
    val brand: String,
    val model: String,
    val year: String, // e.g. "1402"
    val color: String,
    val plate: String, // e.g. "ایران ۲۲ - ۵۶۴ ج ۱۸"
    val vin: String = "",
    val currentMileage: Int, // e.g. 45000
    val estimatedValue: Long = 0L, // e.g. 750_000_000L
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Service Type Enum
 */
enum class ServiceType(
    val title: String,
    val defaultIntervalKm: Int,
    val defaultIntervalMonths: Int,
    @DrawableRes val iconRes: Int,
    val intervalSource: String
) {
    OIL_CHANGE("تعویض روغن موتور", 5000, 6, R.drawable.img_3d_oil, "راهنمای محصول و گارانتی تارا، جدول سرویس‌های دوره‌ای؛ دفترچه مدل خودروی شما اولویت دارد."),
    FILTERS("تعویض فیلترها (هوا و روغن)", 0, 0, R.drawable.img_3d_oil, "راهنمای محصول و گارانتی تارا، جدول سرویس‌های دوره‌ای؛ نوع فیلتر و مدل خودرو مؤثر است."),
    BRAKE_PADS("بازدید لنت ترمز", 10000, 6, R.drawable.img_3d_tire, "بازدید در سرویس‌های دوره‌ای طبق راهنمای محصول تارا؛ تعویض بر اساس میزان فرسودگی است."),
    ENGINE_TUNE("بازدید تنظیم موتور و شمع‌ها", 0, 0, R.drawable.img_3d_settings_gear, "جدول سرویس دوره‌ای دناکار ایرانیان برای برخی محصولات ایران‌خودرو؛ دفترچه خودرو ملاک نهایی است."),
    TIRE_CHANGE("بازدید وضعیت لاستیک‌ها", 10000, 6, R.drawable.img_3d_tire, "یادآور بازدید دوره‌ای است، نه عمر قطعی لاستیک؛ تعویض بر اساس وضعیت و دستور سازنده."),
    BATTERY("بازدید سلامت باتری", 10000, 6, R.drawable.img_3d_battery, "یادآور بازدید دوره‌ای است؛ زمان تعویض به تست باتری و توصیه سازنده وابسته است."),
    TRANSMISSION_OIL_CHANGE("تعویض روغن گیربکس", 0, 0, R.drawable.img_3d_oil, "راهنمای محصول و گارانتی تارا، گیربکس اتوماتیک DAE شش‌سرعته: هر ۶۰٬۰۰۰ کیلومتر یا ۵ سال؛ برای گیربکس دستی یا مدل دیگر، دفترچهٔ اختصاصی ملاک است."),
    HYDRAULIC_STEERING_OIL_CHANGE("تعویض روغن هیدرولیک فرمان", 0, 0, R.drawable.img_3d_settings_gear, "خودرو (car.ir): توصیهٔ تعویض پس از ۱۲۰٬۰۰۰ کیلومتر در صورت نداشتن دستور متفاوت سازنده؛ خودروهای فرمان برقی روغن هیدرولیک ندارند."),
    BRAKE_FLUID_CHANGE("تعویض روغن ترمز", 0, 0, R.drawable.img_3d_settings_gear, "راهنمای محصول و گارانتی تارا: هر ۴۰٬۰۰۰ کیلومتر یا ۲ سال؛ هرکدام زودتر برسد."),
    ALTERNATOR_BELT_CHANGE("تعویض تسمه دینام", 0, 0, R.drawable.img_3d_settings_gear, "راهنمای محصول و گارانتی تارا: هر ۸۰٬۰۰۰ کیلومتر یا ۳ سال؛ هرکدام زودتر برسد."),
    TIMING_BELT_CHANGE("تعویض تسمه تایم", 0, 0, R.drawable.img_3d_settings_gear, "راهنمای محصول و گارانتی تارا: هر ۸۰٬۰۰۰ کیلومتر یا ۳ سال؛ بازه در موتورهای دیگر متفاوت است."),
    COOLANT_CHANGE("تعویض آب رادیاتور و ضدیخ (مایع خنک‌کننده)", 0, 0, R.drawable.img_3d_oil, "راهنمای محصول و گارانتی تارا: هر ۲ سال؛ در این مرجع فاصلهٔ کیلومتری جداگانه تعیین نشده است."),
    CUSTOM("سرویس سفارشی", 0, 0, R.drawable.img_3d_car, "برای سرویس سفارشی فاصلهٔ ثابت و قابل تعمیم وجود ندارد؛ فاصله را طبق دفترچه یا نظر متخصص وارد کنید.")
}

/**
 * Periodic Service Record Entity
 */
data class VehicleServiceEntity(
    val id: String,
    val vehicleId: String,
    val title: String,
    val serviceType: ServiceType = ServiceType.OIL_CHANGE,
    val date: String, // e.g. "1405/06/10"
    val mileage: Int, // e.g. 45000
    val cost: Long, // e.g. 850_000L
    val description: String = "",
    val nextReminderDate: String? = null, // e.g. "1405/12/10"
    val nextReminderMileage: Int? = null, // e.g. 50000
    val isReminderEnabled: Boolean = true
)

/**
 * Expense Categories for Vehicle
 */
enum class VehicleExpenseCategory(val title: String, val colorHex: Long, @DrawableRes val iconRes: Int) {
    FUEL("سوخت", 0xFF3B82F6, R.drawable.img_3d_fuel),
    REPAIRS("تعمیرات", 0xFFEF4444, R.drawable.img_3d_tire),
    SERVICE("سرویس", 0xFF10B981, R.drawable.img_3d_oil),
    INSURANCE("بیمه", 0xFFF59E0B, R.drawable.img_3d_insurance),
    PARTS("قطعات", 0xFF8B5CF6, R.drawable.img_3d_settings_gear),
    WASH("شستشو و کارواش", 0xFF06B6D4, R.drawable.img_3d_car),
    PARKING("پارکینگ و عوارض", 0xFF64748B, R.drawable.img_3d_card),
    INSPECTION("معاینه فنی", 0xFF0D9488, R.drawable.img_3d_settings_gear),
    OTHER("سایر", 0xFF6B7280, R.drawable.img_3d_wallet)
}

/**
 * Vehicle Expense Entry Entity
 */
data class VehicleExpenseEntity(
    val id: String,
    val vehicleId: String,
    val title: String,
    val category: VehicleExpenseCategory,
    val amount: Long,
    val date: String,
    val description: String = "",
    val receiptImageUri: String? = null
)

/**
 * Vehicle Insurance Entity
 */
data class VehicleInsuranceEntity(
    val id: String,
    val vehicleId: String,
    val company: String, // e.g. "بیمه ایران"
    val type: String, // e.g. "شخص ثالث" or "بیمه بدنه"
    val startDate: String,
    val endDate: String,
    val amount: Long,
    val policyNumber: String = "",
    val reminderDays: List<Int> = listOf(30, 15, 7)
)

/**
 * Technical Inspection (معاینه فنی) Entity
 */
data class VehicleInspectionEntity(
    val id: String,
    val vehicleId: String,
    val lastInspectionDate: String,
    val expiryDate: String,
    val cost: Long,
    val status: String = "معتبر",
    val centerName: String = "مرکز معاینه فنی نیایش"
)

/**
 * Unified Timeline Event for Vehicle History
 */
sealed class VehicleTimelineEvent(
    open val id: String,
    open val date: String,
    open val title: String,
    open val amount: Long,
    open val subtitle: String,
    @DrawableRes open val iconRes: Int,
    open val colorHex: Long
) {
    data class ServiceEvent(
        override val id: String,
        override val date: String,
        override val title: String,
        override val amount: Long,
        override val subtitle: String,
        @DrawableRes override val iconRes: Int,
        override val colorHex: Long,
        val mileage: Int
    ) : VehicleTimelineEvent(id, date, title, amount, subtitle, iconRes, colorHex)

    data class ExpenseEvent(
        override val id: String,
        override val date: String,
        override val title: String,
        override val amount: Long,
        override val subtitle: String,
        @DrawableRes override val iconRes: Int,
        override val colorHex: Long,
        val category: VehicleExpenseCategory
    ) : VehicleTimelineEvent(id, date, title, amount, subtitle, iconRes, colorHex)

    data class InsuranceEvent(
        override val id: String,
        override val date: String,
        override val title: String,
        override val amount: Long,
        override val subtitle: String,
        @DrawableRes override val iconRes: Int,
        override val colorHex: Long,
        val company: String
    ) : VehicleTimelineEvent(id, date, title, amount, subtitle, iconRes, colorHex)
}
