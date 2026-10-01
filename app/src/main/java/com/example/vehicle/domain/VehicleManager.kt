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
        currentShamsiDate: String = "1405/06/10"
    ): Pair<Int, String> {
        val nextKm = currentMileage + serviceType.defaultIntervalKm
        val nextDate = com.example.util.PersianCalendarHelper.addMonthsToPersianDate(
            currentShamsiDate,
            serviceType.defaultIntervalMonths
        )
        return Pair(nextKm, nextDate)
    }

    /**
     * Formats car plate to standard Iranian visual format
     */
    fun formatPlateNumber(part1: String, letter: String, part2: String, iranCode: String): String {
        return "$part1 $letter $part2 ایران $iranCode"
    }
}
