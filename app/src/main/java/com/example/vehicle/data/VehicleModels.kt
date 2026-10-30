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
    val createdAt: Long = System.currentTimeMillis(),
    val engine: String = "",
    val transmission: String = ""
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
    OIL_CHANGE("تعویض روغن موتور", 0, 0, R.drawable.img_3d_oil, "بازه را از دفترچهٔ راهنمای همین خودرو بررسی و وارد کنید."),
    FILTERS("تعویض فیلترها", 0, 0, R.drawable.img_3d_oil, "نوع فیلتر و بازهٔ تعویض را از دفترچهٔ همین خودرو بررسی کنید."),
    FUEL_FILTER_CHANGE("تعویض فیلتر بنزین", 0, 0, R.drawable.img_3d_fuel, "بازهٔ تعویض فیلتر بنزین به نوع سیستم سوخت‌رسانی و موتور وابسته است؛ دفترچهٔ همین خودرو ملاک است."),
    BRAKE_PADS("تعویض لنت ترمز", 0, 0, R.drawable.img_3d_tire, "وضعیت سایش و دستورالعمل سازندهٔ همین خودرو را ملاک قرار دهید."),
    CLUTCH_DISC_PLATE_CHANGE("تعویض دیسک و صفحه کلاچ", 0, 0, R.drawable.img_3d_settings_gear, "عمر دیسک و صفحه به نوع خودرو و شرایط رانندگی وابسته است؛ دفترچه و بررسی فنی ملاک است."),
    SPARK_PLUG_WIRE_CHANGE("تعویض شمع و وایر", 0, 0, R.drawable.img_3d_settings_gear, "نوع شمع، موتور و وجود وایر در سیستم جرقه‌زنی تعیین‌کننده است؛ دفترچهٔ همین خودرو را بررسی کنید."),
    ENGINE_TUNE("بازدید تنظیم موتور و شمع‌ها", 0, 0, R.drawable.img_3d_settings_gear, "بازهٔ بازدید را از دفترچهٔ راهنمای همین خودرو بررسی کنید."),
    TIRE_CHANGE("تعویض لاستیک ها", 0, 0, R.drawable.img_3d_tire, "وضعیت سایش، سن و دستورالعمل سازندهٔ همین خودرو ملاک است."),
    BATTERY("تعویض باتری", 0, 0, R.drawable.img_3d_battery, "سلامت باتری را بررسی و توصیهٔ سازندهٔ همین خودرو را ملاک قرار دهید."),
    TRANSMISSION_OIL_CHANGE("تعویض روغن گیربکس", 0, 0, R.drawable.img_3d_oil, "نوع گیربکس و روغن تعیین‌کننده‌اند؛ بازه را از دفترچهٔ همین خودرو بررسی کنید."),
    HYDRAULIC_STEERING_OIL_CHANGE("تعویض روغن هیدرولیک فرمان", 0, 0, R.drawable.img_3d_settings_gear, "این سرویس فقط برای فرمان هیدرولیک کاربرد دارد؛ ابتدا نوع سیستم فرمان را بررسی کنید."),
    BRAKE_FLUID_CHANGE("تعویض روغن ترمز", 0, 0, R.drawable.img_3d_settings_gear, "نوع روغن و بازهٔ تعویض را از دفترچهٔ همین خودرو بررسی کنید."),
    ALTERNATOR_BELT_CHANGE("تعویض تسمه دینام", 0, 0, R.drawable.img_3d_settings_gear, "بازه و وضعیت تسمه به موتور خودرو وابسته است؛ دفترچهٔ همین خودرو ملاک است."),
    TIMING_BELT_CHANGE("تعویض تسمه تایم", 0, 0, R.drawable.img_3d_settings_gear, "بازهٔ تعویض به کد موتور وابسته است؛ فقط دستورالعمل همان موتور را ملاک قرار دهید."),
    COOLANT_CHANGE("تعویض آب رادیاتور و ضدیخ (مایع خنک‌کننده)", 0, 0, R.drawable.img_3d_oil, "نوع مایع خنک‌کننده و بازهٔ تعویض را از دفترچهٔ همین خودرو بررسی کنید."),
    CUSTOM("سرویس سفارشی", 0, 0, R.drawable.img_3d_car, "فاصله را طبق دفترچهٔ خودرو یا نظر متخصص وارد کنید.")
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
    val isReminderEnabled: Boolean = true,
    val reminderAlarmFailed: Boolean = false,
    val reminderAlarmErrorMessage: String? = null
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
    val reminderDays: List<Int> = listOf(30, 15, 7),
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "۰۹:۰۰"
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
    val centerName: String = "مرکز معاینه فنی نیایش",
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "۰۹:۰۰",
    val reminderDays: List<Int> = listOf(30, 7, 1, 0)
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

    data class InspectionEvent(
        override val id: String,
        override val date: String,
        override val title: String,
        override val amount: Long,
        override val subtitle: String,
        @DrawableRes override val iconRes: Int,
        override val colorHex: Long,
        val expiryDate: String
    ) : VehicleTimelineEvent(id, date, title, amount, subtitle, iconRes, colorHex)
}
