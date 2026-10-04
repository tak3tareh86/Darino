package com.example.reminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "smart_reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String,
    val description: String = "",
    val type: String = "GENERAL",
    val sourceType: String = "MANUAL",
    val sourceId: String? = null,
    val priority: String = "NORMAL",
    val status: String = "ACTIVE",
    val date: String,
    val time: String = "۰۹:۰۰",
    val timezone: String = "Asia/Tehran",
    val notificationEnabled: Boolean = true,
    val smsEnabled: Boolean = false,
    val phoneNumber: String? = null,
    val amount: Long? = null,
    val targetKilometer: Long? = null,
    val currentKilometer: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val cancelledAt: Long? = null
)
