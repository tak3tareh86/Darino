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
enum class ServiceType(val title: String, val defaultIntervalKm: Int, val defaultIntervalMonths: Int, @DrawableRes val iconRes: Int) {
    OIL_CHANGE("تعویض روغن موتور", 5000, 6, R.drawable.img_3d_oil),
    FILTERS("تعویض فیلترها (هوا و روغن)", 5000, 6, R.drawable.img_3d_oil),
    BRAKE_PADS("تعویض لنت ترمز", 20000, 12, R.drawable.img_3d_tire),
    ENGINE_TUNE("تنظیم موتور و شمع‌ها", 25000, 12, R.drawable.img_3d_settings_gear),
    TIRE_CHANGE("تعویض یا جابجایی لاستیک", 40000, 24, R.drawable.img_3d_tire),
    BATTERY("سرویس یا تعویض باتری", 40000, 24, R.drawable.img_3d_battery),
    CUSTOM("سرویس سفارشی", 10000, 6, R.drawable.img_3d_car)
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
