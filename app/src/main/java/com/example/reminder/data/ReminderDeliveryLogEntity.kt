package com.example.reminder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "smart_reminder_delivery_logs",
    indices = [
        Index(value = ["reminderId"]),
        Index(value = ["scheduledAt"])
    ]
)
data class ReminderDeliveryLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val reminderId: String,
    val scheduleId: String? = null,
    val channel: String, // NOTIFICATION, SMS
    val scheduledAt: Long,
    val triggeredAt: Long? = null,
    val status: String = "SCHEDULED", // SCHEDULED, TRIGGERED, SENT, DELIVERED, FAILED, CANCELLED
    val errorMessage: String? = null,
    val providerMessageId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
