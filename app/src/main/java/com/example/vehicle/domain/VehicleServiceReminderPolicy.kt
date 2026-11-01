package com.example.vehicle.domain

import com.example.reminder.domain.OffsetUnit
import com.example.reminder.domain.PredefinedOffset
import java.util.Calendar

/**
 * Pure scheduling rules for vehicle-service date reminders.
 * Day offsets are calendar based so the chosen local wall-clock time is preserved
 * across daylight-saving transitions.
 */
object VehicleServiceReminderPolicy {

    fun triggerTimeMillis(baseMillis: Long, offset: PredefinedOffset): Long {
        return if (offset.unit == OffsetUnit.DAY) {
            Calendar.getInstance().apply {
                timeInMillis = baseMillis
                add(Calendar.DAY_OF_YEAR, -offset.value)
            }.timeInMillis
        } else {
            baseMillis - (offset.value.toLong() * offset.unit.millisMultiplier)
        }
    }

    fun futureOffsets(
        baseMillis: Long,
        selectedOffsets: List<PredefinedOffset>,
        nowMillis: Long
    ): List<PredefinedOffset> = selectedOffsets.distinct().filter { offset ->
        triggerTimeMillis(baseMillis, offset) > nowMillis
    }
}
