package com.example.reminder.domain

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.WarningAmberLight

enum class ReminderType(
    val title: String,
    val key: String,
    val accentColor: Color,
    @DrawableRes val iconRes: Int
) {
    GENERAL("عمومی", "GENERAL", Color(0xFF6366F1), R.drawable.img_3d_bell_notification),
    INSTALLMENT("قسط و وام", "INSTALLMENT", Color(0xFF2563EB), R.drawable.img_3d_bank),
    VEHICLE("خودرو", "VEHICLE", WarningAmberLight, R.drawable.img_3d_car),
    INSURANCE("بیمه", "INSURANCE", EmeraldPrimaryLight, R.drawable.img_3d_shield_security),
    MAINTENANCE("سرویس و نگهداری", "MAINTENANCE", Color(0xFFF59E0B), R.drawable.img_3d_oil),
    FUEL("سوخت", "FUEL", Color(0xFFEC4899), R.drawable.img_3d_fuel),
    FINANCE("مالی", "FINANCE", EmeraldPrimaryLight, R.drawable.img_3d_analytics),
    PERSONAL("شخصی", "PERSONAL", Color(0xFF8B5CF6), R.drawable.img_3d_calendar),
    CUSTOM("سفارشی", "CUSTOM", Color(0xFF14B8A6), R.drawable.img_3d_card);

    companion object {
        fun fromKey(key: String): ReminderType {
            return entries.find { it.name.equals(key, ignoreCase = true) || it.key.equals(key, ignoreCase = true) } ?: GENERAL
        }
    }
}

enum class ReminderSourceType(val title: String) {
    MANUAL("دستی"),
    INSTALLMENT("قسط"),
    VEHICLE("خودرو"),
    INSURANCE("بیمه"),
    MAINTENANCE("سرویس"),
    FUEL("سوخت"),
    OTHER("سایر");

    companion object {
        fun fromKey(key: String?): ReminderSourceType {
            if (key == null) return MANUAL
            return entries.find { it.name.equals(key, ignoreCase = true) } ?: OTHER
        }
    }
}

enum class ReminderStatus(
    val title: String,
    val color: Color,
    val darkContainerColor: Color,
    val lightContainerColor: Color
) {
    ACTIVE("فعال", Color(0xFF10B981), Color(0xFF064E3B), Color(0xFFD1FAE5)),
    COMPLETED("انجام شده", Color(0xFF6B7280), Color(0xFF1F2937), Color(0xFFF3F4F6)),
    MISSED("از دست رفته", ExpenseRoseLight, Color(0xFF7F1D1D), Color(0xFFFFE4E6)),
    DISABLED("غیرفعال", Color(0xFF9CA3AF), Color(0xFF374151), Color(0xFFF3F4F6)),
    CANCELLED("لغو شده", Color(0xFF9CA3AF), Color(0xFF374151), Color(0xFFF3F4F6));

    companion object {
        fun fromKey(key: String): ReminderStatus {
            return entries.find { it.name.equals(key, ignoreCase = true) } ?: ACTIVE
        }
    }
}

enum class Priority(val title: String, val level: Int, val color: Color) {
    LOW("کم", 1, Color(0xFF10B981)),
    NORMAL("عادی", 2, Color(0xFF3B82F6)),
    HIGH("مهم و فوری", 3, Color(0xFFEF4444));

    companion object {
        fun fromKey(key: String?): Priority {
            return entries.find { it.name.equals(key, ignoreCase = true) } ?: NORMAL
        }
    }
}

enum class TriggerType {
    EXACT,
    BEFORE_EVENT
}

enum class OffsetUnit(val title: String, val millisMultiplier: Long) {
    MINUTE("دقیقه", 60 * 1000L),
    HOUR("ساعت", 60 * 60 * 1000L),
    DAY("روز", 24 * 60 * 60 * 1000L),
    WEEK("هفته", 7 * 24 * 60 * 60 * 1000L),
    MONTH("ماه", 30L * 24 * 60 * 60 * 1000L);

    companion object {
        fun fromKey(key: String?): OffsetUnit {
            return entries.find { it.name.equals(key, ignoreCase = true) } ?: DAY
        }
    }
}

enum class RepeatType(val title: String) {
    NONE("یکبار (بدون تکرار)"),
    DAILY("روزانه"),
    WEEKLY("هفتگی"),
    MONTHLY("ماهانه"),
    YEARLY("سالانه");

    companion object {
        fun fromKey(key: String?): RepeatType {
            return entries.find { it.name.equals(key, ignoreCase = true) } ?: NONE
        }
    }
}

enum class DeliveryChannel {
    NOTIFICATION,
    SMS
}

enum class DeliveryStatus(val title: String) {
    SCHEDULED("زمان‌بندی شده"),
    TRIGGERED("اجرا شده"),
    SENT("ارسال شده"),
    DELIVERED("تحویل شده"),
    FAILED("ناموفق"),
    CANCELLED("لغو شده");

    companion object {
        fun fromKey(key: String?): DeliveryStatus {
            return entries.find { it.name.equals(key, ignoreCase = true) } ?: SCHEDULED
        }
    }
}

enum class SnoozeOption(val title: String, val minutes: Int) {
    MINUTES_15("۱۵ دقیقه دیگر", 15),
    HOUR_1("۱ ساعت دیگر", 60),
    TOMORROW("فردا همین موقع", 24 * 60),
    THREE_DAYS("۳ روز دیگر", 3 * 24 * 60),
    CUSTOM("زمان دلخواه", -1)
}

enum class PredefinedOffset(val title: String, val value: Int, val unit: OffsetUnit) {
    AT_TIME("در زمان سررسید", 0, OffsetUnit.MINUTE),
    BEFORE_1_DAY("۱ روز قبل", 1, OffsetUnit.DAY),
    BEFORE_3_DAYS("۳ روز قبل", 3, OffsetUnit.DAY),
    BEFORE_7_DAYS("۷ روز قبل (۱ هفته)", 7, OffsetUnit.DAY),
    BEFORE_14_DAYS("۱۴ روز قبل (۲ هفته)", 14, OffsetUnit.DAY),
    BEFORE_30_DAYS("۳۰ روز قبل (۱ ماه)", 30, OffsetUnit.DAY)
}

data class SmartSuggestion(
    val id: String,
    val title: String,
    val message: String,
    val type: ReminderType,
    val defaultDate: String,
    val defaultTime: String = "۰۹:۰۰",
    val amount: Long? = null,
    val sourceType: String? = null,
    val sourceId: String? = null,
    val targetKilometer: Long? = null
)

data class SmsDispatchResult(
    val success: Boolean,
    val providerMessageId: String? = null,
    val errorReason: String? = null
)
