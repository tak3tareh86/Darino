package com.example.vehicle.domain

/** Pure rules for advance odometer reminders. */
object VehicleMileageReminderPolicy {
    fun thresholdKilometer(targetMileage: Int, leadKilometers: Int): Int {
        val safeLead = leadKilometers.coerceAtLeast(0)
        return (targetMileage.toLong() - safeLead.toLong())
            .coerceAtLeast(0L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    fun isDue(currentMileage: Int, targetMileage: Int?, leadKilometers: Int): Boolean {
        if (currentMileage < 0 || targetMileage == null || targetMileage < 0) return false
        return currentMileage >= thresholdKilometer(targetMileage, leadKilometers)
    }

    fun isCrossed(previousMileage: Int, currentMileage: Int, targetMileage: Int?, leadKilometers: Int): Boolean {
        if (previousMileage < 0 || currentMileage <= previousMileage) return false
        val threshold = thresholdKilometer(targetMileage ?: return false, leadKilometers)
        return previousMileage < threshold && currentMileage >= threshold
    }
}
