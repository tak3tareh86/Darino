package com.example.reminder.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "smart_reminder_schedules",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["reminderId"]),
        Index(value = ["triggerDateTime"])
    ]
)
data class ReminderScheduleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val reminderId: String,
    val triggerType: String = "EXACT", // EXACT, BEFORE_EVENT
    val offsetValue: Int = 0,
    val offsetUnit: String = "DAY", // MINUTE, HOUR, DAY, WEEK, MONTH
    val triggerDateTime: Long, // epoch millis
    val repeatType: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY, YEARLY
    val repeatInterval: Int = 1,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
