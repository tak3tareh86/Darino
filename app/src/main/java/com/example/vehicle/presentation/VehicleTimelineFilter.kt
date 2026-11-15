package com.example.vehicle.presentation

import com.example.vehicle.data.VehicleTimelineEvent
import com.example.vehicle.domain.VehicleAnalyzer

internal enum class TimelineFilterMode {
    ALL, MONTH, YEAR
}

/**
 * Filters already-built vehicle timeline events by their recorded Jalali event date.
 * Invalid dates remain visible in ALL mode but are never assigned to a guessed period.
 */
internal fun filterVehicleTimelineEvents(
    events: List<VehicleTimelineEvent>,
    mode: TimelineFilterMode,
    year: Int,
    month: Int
): List<VehicleTimelineEvent> {
    if (mode == TimelineFilterMode.ALL) return events

    val yearPrefix = year.toString().padStart(4, '0')
    val monthPrefix = "$yearPrefix/${month.toString().padStart(2, '0')}"

    return events.filter { event ->
        val normalizedDate = VehicleAnalyzer.parseAndValidateJalaliDate(event.date)
        when (mode) {
            TimelineFilterMode.ALL -> true
            TimelineFilterMode.MONTH -> normalizedDate?.startsWith(monthPrefix) == true
            TimelineFilterMode.YEAR -> normalizedDate?.startsWith("$yearPrefix/") == true
        }
    }
}
