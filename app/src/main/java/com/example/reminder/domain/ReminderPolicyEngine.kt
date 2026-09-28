package com.example.reminder.domain

import com.example.reminder.data.ReminderEntity
import java.util.Calendar
import java.util.TimeZone

data class QuietHoursConfig(
    val enabled: Boolean = false,
    val startHour: Int = 23,
    val startMinute: Int = 0,
    val endHour: Int = 7,
    val endMinute: Int = 0,
    val allowHighPriority: Boolean = true
)

sealed class DispatchDecision {
    data object DeliverNow : DispatchDecision()
    data class PostponeTo(val postponeTimeMillis: Long) : DispatchDecision()
    data object SuppressQuietHours : DispatchDecision()
}

object ReminderPolicyEngine {

    /**
     * Evaluates whether a reminder should be delivered now or postponed based on Quiet Hours and Priority.
     */
    fun evaluateDispatch(
        reminder: ReminderEntity,
        quietHours: QuietHoursConfig,
        currentTimestampMillis: Long = System.currentTimeMillis()
    ): DispatchDecision {
        if (!quietHours.enabled) {
            return DispatchDecision.DeliverNow
        }

        val priority = Priority.fromKey(reminder.priority)
        if (priority == Priority.HIGH && quietHours.allowHighPriority) {
            return DispatchDecision.DeliverNow
        }

        val tz = TimeZone.getTimeZone("Asia/Tehran")
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = currentTimestampMillis
        }

        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val currentMinuteOfDay = currentHour * 60 + currentMinute

        val startMinuteOfDay = quietHours.startHour * 60 + quietHours.startMinute
        val endMinuteOfDay = quietHours.endHour * 60 + quietHours.endMinute

        val isInQuietHours = if (startMinuteOfDay > endMinuteOfDay) {
            // Overnight quiet hours (e.g. 23:00 to 07:00)
            currentMinuteOfDay >= startMinuteOfDay || currentMinuteOfDay < endMinuteOfDay
        } else {
            // Same day quiet hours (e.g. 13:00 to 15:00)
            currentMinuteOfDay in startMinuteOfDay until endMinuteOfDay
        }

        if (isInQuietHours) {
            // Calculate next active hour (end of quiet hours)
            val postponeCal = Calendar.getInstance(tz).apply {
                timeInMillis = currentTimestampMillis
                set(Calendar.HOUR_OF_DAY, quietHours.endHour)
                set(Calendar.MINUTE, quietHours.endMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= currentTimestampMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return DispatchDecision.PostponeTo(postponeCal.timeInMillis)
        }

        return DispatchDecision.DeliverNow
    }
}
