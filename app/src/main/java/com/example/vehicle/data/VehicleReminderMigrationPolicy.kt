package com.example.vehicle.data

import com.example.reminder.data.ReminderEntity
import com.example.reminder.domain.PredefinedOffset

/** Pure migration/recovery rules for legacy vehicle reminders. */
internal object VehicleReminderMigrationPolicy {

    fun offsetsForDays(days: List<Int>): List<PredefinedOffset> =
        days.distinct().sortedDescending().mapNotNull { day ->
            when (day) {
                0 -> PredefinedOffset.AT_TIME
                1 -> PredefinedOffset.BEFORE_1_DAY
                2 -> PredefinedOffset.BEFORE_2_DAYS
                3 -> PredefinedOffset.BEFORE_3_DAYS
                7 -> PredefinedOffset.BEFORE_7_DAYS
                14 -> PredefinedOffset.BEFORE_14_DAYS
                15 -> PredefinedOffset.BEFORE_15_DAYS
                30 -> PredefinedOffset.BEFORE_30_DAYS
                else -> null
            }
        }

    fun shouldRestore(reminder: ReminderEntity): Boolean =
        reminder.sourceType.equals("VEHICLE", ignoreCase = true) &&
            reminder.status.equals("ACTIVE", ignoreCase = true)
}
