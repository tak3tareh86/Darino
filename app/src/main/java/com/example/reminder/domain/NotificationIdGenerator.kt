package com.example.reminder.domain

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.math.abs

object NotificationIdGenerator {

    /**
     * Generates a deterministic, positive 31-bit integer notification ID based on
     * reminderId, scheduleId, and triggerTime.
     * Guarantees identical ID across reboots, reschedules, and process kills.
     */
    fun generateNotificationId(
        reminderId: String,
        scheduleId: String?,
        triggerTime: Long
    ): Int {
        val rawKey = "$reminderId:${scheduleId.orEmpty()}:$triggerTime"
        return try {
            val md = MessageDigest.getInstance("MD5")
            val hash = md.digest(rawKey.toByteArray(StandardCharsets.UTF_8))
            // Take first 4 bytes and convert to positive integer
            var value = 0
            for (i in 0 until 4) {
                value = (value shl 8) or (hash[i].toInt() and 0xFF)
            }
            abs(value)
        } catch (e: Exception) {
            abs(rawKey.hashCode())
        }
    }

    /**
     * Generates an integer request code for AlarmManager PendingIntent for a specific schedule.
     */
    fun generateAlarmRequestCode(scheduleId: String): Int {
        return abs(scheduleId.hashCode())
    }
}
