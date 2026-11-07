package com.example.calendar.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventStatus
import com.example.calendar.domain.model.FinancialEventType
import com.example.calendar.domain.model.ReminderBeforeOption
import com.example.util.PersianCalendarHelper
import com.example.ui.screens.settings.model.AppCurrency

@Entity(
    tableName = "financial_events",
    primaryKeys = ["id", "userId"]
)
data class FinancialEventEntity(
    val id: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val type: String, // "INSTALLMENT", "REMINDER", "VEHICLE", "EXPENSE"
    val amount: Long? = null,
    val date: String, // "1405/06/15"
    val time: String? = null,
    val repeatType: String = "NONE",
    val reminderBefore: String = "NONE",
    val sourceId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    val customReminderAt: Long? = null,
    @ColumnInfo(defaultValue = "'TOMAN'") val amountUnit: String = "TOMAN"
) {
    fun toDomain(): FinancialEvent {
        val parsedAmountUnit = when (amountUnit) {
            AppCurrency.TOMAN.name -> AppCurrency.TOMAN
            AppCurrency.RIAL.name -> AppCurrency.RIAL
            else -> null
        }
        return FinancialEvent(
            id = id,
            title = title,
            description = description,
            type = FinancialEventType.fromString(type),
            // Never reinterpret an amount with an unknown persisted unit as Toman.
            // Keep the event visible, but omit its monetary value until the data is repaired.
            amount = amount.takeIf { parsedAmountUnit != null },
            amountUnit = parsedAmountUnit ?: AppCurrency.TOMAN,
            date = date,
            time = time,
            repeatType = repeatType,
            reminderBefore = ReminderBeforeOption.fromCode(reminderBefore),
            sourceId = sourceId,
            createdAt = createdAt,
            status = FinancialEventStatus.fromString(status),
            customReminderAt = customReminderAt,
            reminderLabel = if (customReminderAt != null) {
                val customDateTime = PersianCalendarHelper.fromEpochMillis(customReminderAt)
                "${customDateTime.toFormattedDate()}، ساعت ${customDateTime.toFormattedTime()}"
            } else ReminderBeforeOption.fromCode(reminderBefore).takeUnless { it == ReminderBeforeOption.NONE || it == ReminderBeforeOption.CUSTOM }?.label
        )
    }

    companion object {
        fun fromDomain(event: FinancialEvent, userId: String): FinancialEventEntity {
            return FinancialEventEntity(
                id = event.id,
                userId = userId,
                title = event.title,
                description = event.description,
                type = event.type.name,
                amount = event.amount,
                amountUnit = event.amountUnit.name,
                date = event.date,
                time = event.time,
                repeatType = event.repeatType,
                reminderBefore = event.reminderBefore.code,
                sourceId = event.sourceId,
                createdAt = event.createdAt,
                status = event.status.name,
                customReminderAt = event.customReminderAt
            )
        }
    }
}
