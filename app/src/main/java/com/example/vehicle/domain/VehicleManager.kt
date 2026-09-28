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

        // Simple Persian date increment for mock (add defaultIntervalMonths)
        val parts = currentShamsiDate.split("/")
        val nextDate = if (parts.size == 3) {
            val year = parts[0].toIntOrNull() ?: 1405
            var month = parts[1].toIntOrNull() ?: 6
            val day = parts[2]

            month += serviceType.defaultIntervalMonths
            var finalYear = year
            while (month > 12) {
                month -= 12
                finalYear += 1
            }
            val monthStr = if (month < 10) "0$month" else "$month"
            "$finalYear/$monthStr/$day"
        } else {
            "1405/12/10"
        }

        return Pair(nextKm, nextDate)
    }

    /**
     * Formats car plate to standard Iranian visual format
     */
    fun formatPlateNumber(part1: String, letter: String, part2: String, iranCode: String): String {
        return "ایران $iranCode - $part1 $letter $part2"
    }
}
