package com.example.reminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "smart_reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val type: String = "GENERAL", // GENERAL, INSTALLMENT, VEHICLE, INSURANCE, MAINTENANCE, FUEL, CUSTOM, PERSONAL, FINANCE
    val sourceType: String = "MANUAL", // MANUAL, INSTALLMENT, VEHICLE, INSURANCE, MAINTENANCE, FUEL, OTHER
    val sourceId: String? = null,
    val priority: String = "NORMAL", // LOW, NORMAL, HIGH
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, MISSED, DISABLED, CANCELLED
    val date: String, // Shamsi date string, e.g. "۱۴۰۵/۰۷/۳۰"
    val time: String = "۰۹:۰۰", // Time string, e.g. "۰۹:۰۰"
    val timezone: String = "Asia/Tehran",
    val notificationEnabled: Boolean = true,
    val smsEnabled: Boolean = false,
    val phoneNumber: String? = null,
    val amount: Long? = null, // Amount in Toman
    val targetKilometer: Long? = null,
    val currentKilometer: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val cancelledAt: Long? = null
)
