package com.example.calendar.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.WarningAmberLight

enum class FinancialEventType(
    val title: String,
    @DrawableRes val icon3dRes: Int,
    val iconVector: ImageVector,
    val primaryColor: Color
) {
    INSTALLMENT(
        title = "اقساط",
        icon3dRes = R.drawable.img_3d_installment,
        iconVector = Icons.Rounded.AccountBalance,
        primaryColor = Color(0xFF2563EB)
    ),
    REMINDER(
        title = "یادآور شخصی",
        icon3dRes = R.drawable.img_3d_bell_notification,
        iconVector = Icons.Rounded.Notifications,
        primaryColor = WarningAmberLight
    ),
    VEHICLE(
        title = "خدمات خودرو",
        icon3dRes = R.drawable.img_3d_car,
        iconVector = Icons.Rounded.DirectionsCar,
        primaryColor = InfoIndigoLight
    ),
    EXPENSE(
        title = "هزینه و مالی",
        icon3dRes = R.drawable.img_3d_wallet,
        iconVector = Icons.Rounded.AccountBalanceWallet,
        primaryColor = EmeraldPrimaryLight
    );

    companion object {
        fun fromString(value: String): FinancialEventType {
            return when (value.uppercase()) {
                "INSTALLMENT", "LOAN" -> INSTALLMENT
                "REMINDER" -> REMINDER
                "VEHICLE", "CAR" -> VEHICLE
                "EXPENSE", "FINANCE", "FINANCIAL" -> EXPENSE
                else -> REMINDER
            }
        }
    }
}

enum class FinancialEventStatus(
    val title: String,
    val color: Color
) {
    PENDING("در انتظار", Color(0xFF64748B)),
    PAID("پرداخت شده", EmeraldPrimaryLight),
    OVERDUE("عقب افتاده", ExpenseRoseLight);

    companion object {
        fun fromString(value: String): FinancialEventStatus {
            return when (value.uppercase()) {
                "PAID", "COMPLETED" -> PAID
                "OVERDUE" -> OVERDUE
                else -> PENDING
            }
        }
    }
}

enum class ReminderBeforeOption(
    val label: String,
    val code: String
) {
    ONE_DAY("یک روز قبل", "1_DAY"),
    THREE_DAYS("سه روز قبل", "3_DAYS"),
    SEVEN_DAYS("هفت روز قبل", "7_DAYS"),
    CUSTOM("تاریخ دلخواه", "CUSTOM");

    companion object {
        fun fromCode(code: String): ReminderBeforeOption {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ONE_DAY
        }
    }
}

data class FinancialEvent(
    val id: String,
    val title: String,
    val description: String = "",
    val type: FinancialEventType,
    val amount: Long? = null,
    val date: String, // YYYY/MM/DD in Persian or English digits, e.g. "1405/06/15"
    val time: String? = null, // e.g. "10:00"
    val repeatType: String = "NONE", // "NONE", "MONTHLY", "YEARLY"
    val reminderBefore: ReminderBeforeOption = ReminderBeforeOption.ONE_DAY,
    val sourceId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val status: FinancialEventStatus = FinancialEventStatus.PENDING,
    val isDueSoon: Boolean = false,
    val isOverdue: Boolean = false
) {
    val formattedAmount: String?
        get() = amount?.let {
            val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(it)
            com.example.util.IranianPhoneUtils.convertDigitsToPersian(formatted) + " تومان"
        }

    val displayDatePersian: String
        get() = com.example.util.IranianPhoneUtils.convertDigitsToPersian(date)

    val displayTimePersian: String?
        get() = time?.let { com.example.util.IranianPhoneUtils.convertDigitsToPersian(it) }
}
