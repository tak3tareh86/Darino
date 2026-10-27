package com.example.ui.screens.check.model

import com.example.util.PersianCalendarHelper

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

    val daysToDueDate: Long
        get() {
            val today = System.currentTimeMillis()
            return (dueDate - today) / (24 * 60 * 60 * 1000)
        }

    val isOverdue: Boolean
        get() = dueDate < System.currentTimeMillis() && (status == CheckStatus.REGISTERED || status == CheckStatus.PENDING || status == CheckStatus.BOUNCED)
}
