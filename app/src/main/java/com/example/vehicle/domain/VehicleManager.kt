package com.example.vehicle.domain

import com.example.vehicle.data.ServiceType

/**
 * Manager responsible for vehicle business logic, smart reminders, and rules
 */
object VehicleManager {

    /**
     * Compute recommended next service reminder based on service type and current mileage
     */
    fun calculateRecommendedReminder(
        serviceType: ServiceType,
        currentMileage: Int,
        currentShamsiDate: String = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
    ): Pair<Int, String> {
        val nextKm = if (serviceType.defaultIntervalKm > 0) {
            (currentMileage.coerceAtLeast(0) + serviceType.defaultIntervalKm)
        } else {
            0 // This service type is time-based; do not fabricate a mileage target.
        }
        val nextDate = if (serviceType.defaultIntervalMonths > 0) {
            com.example.util.PersianCalendarHelper.addMonthsToPersianDate(
                currentShamsiDate,
                serviceType.defaultIntervalMonths
            )
        } else {
            "" // This service type has no sourced time interval.
        }
        return Pair(nextKm, nextDate)
    }

    /**
     * Formats car plate to standard Iranian visual format
     */
    fun formatPlateNumber(part1: String, letter: String, part2: String, iranCode: String): String {
        return "$part1 $letter $part2 ایران $iranCode"
    }
}
