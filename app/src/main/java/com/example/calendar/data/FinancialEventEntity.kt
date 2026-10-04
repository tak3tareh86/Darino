package com.example.calendar.data

import androidx.room.Entity
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventStatus
import com.example.calendar.domain.model.FinancialEventType
import com.example.calendar.domain.model.ReminderBeforeOption

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
    val reminderBefore: String = "1_DAY",
    val sourceId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING"
) {
    fun toDomain(): FinancialEvent {
        return FinancialEvent(
            id = id,
            title = title,
            description = description,
            type = FinancialEventType.fromString(type),
            amount = amount,
            date = date,
            time = time,
            repeatType = repeatType,
            reminderBefore = ReminderBeforeOption.fromCode(reminderBefore),
            sourceId = sourceId,
            createdAt = createdAt,
            status = FinancialEventStatus.fromString(status)
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
                date = event.date,
                time = event.time,
                repeatType = event.repeatType,
                reminderBefore = event.reminderBefore.code,
                sourceId = event.sourceId,
                createdAt = event.createdAt,
                status = event.status.name
            )
        }
    }
}
