package com.example.ui.screens.check.model

import com.example.util.PersianCalendarHelper
import java.util.Calendar

enum class CheckType(val title: String) {
    RECEIVED("دریافتی"),
    PAID("پرداختی")
}

enum class CheckStatus(val title: String, val color: Long) {
    REGISTERED("ثبت شده", 0xFF64748B),
    PENDING("در انتظار", 0xFFF59E0B),
    CLEARED("وصول شده", 0xFF10B981),
    BOUNCED("برگشتی", 0xFFEF4444),
    CANCELED("لغو شده", 0xFF94A3B8)
}

data class Check(
    val id: Int = 0,
    val userId: String,
    val type: CheckType,
    val amount: Long,
    val checkNumber: String,
    val sayadId: String? = null,
    val bankName: String,
    val branch: String? = null,
    val holderName: String,
    val receiverName: String,
    val issueDate: Long,
    val dueDate: Long,
    val status: CheckStatus,
    val notes: String? = null,
    val accountId: String? = null,
    val reminderDateTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val formattedDueDate: String
        get() = PersianCalendarHelper.fromEpochMillis(dueDate).toFormattedDate()

    val formattedIssueDate: String
        get() = PersianCalendarHelper.fromEpochMillis(issueDate).toFormattedDate()

    /**
     * Calendar-day difference in the device's local time zone.
     * Comparing date boundaries avoids off-by-one results caused by the current time
     * and daylight-saving transitions.
     */
    val daysToDueDate: Long
        get() {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val due = Calendar.getInstance().apply {
                timeInMillis = dueDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            fun Calendar.epochDay(): Long {
                val year = get(Calendar.YEAR)
                val month = get(Calendar.MONTH)
                val day = get(Calendar.DAY_OF_MONTH)
                val adjustedYear = year - if (month <= Calendar.FEBRUARY) 1 else 0
                val era = Math.floorDiv(adjustedYear, 400)
                val yearOfEra = adjustedYear - era * 400
                val monthPrime = month + if (month > Calendar.FEBRUARY) -2 else 10
                val dayOfYear = (153L * monthPrime + 2) / 5 + day - 1
                val dayOfEra = yearOfEra * 365L + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
                return era * 146097L + dayOfEra
            }
            return due.epochDay() - today.epochDay()
        }

    val isOverdue: Boolean
        get() = daysToDueDate < 0 && (status == CheckStatus.REGISTERED || status == CheckStatus.PENDING || status == CheckStatus.BOUNCED)
}
