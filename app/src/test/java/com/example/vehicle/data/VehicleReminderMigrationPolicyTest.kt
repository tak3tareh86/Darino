package com.example.vehicle.data

import com.example.reminder.data.ReminderEntity
import com.example.reminder.domain.PredefinedOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleReminderMigrationPolicyTest {

    @Test
    fun legacyDayOffsetsAreMappedDeduplicatedAndSorted() {
        assertEquals(
            listOf(
                PredefinedOffset.BEFORE_30_DAYS,
                PredefinedOffset.BEFORE_7_DAYS,
                PredefinedOffset.BEFORE_1_DAY,
                PredefinedOffset.AT_TIME
            ),
            VehicleReminderMigrationPolicy.offsetsForDays(listOf(7, 30, 1, 0, 7, 99, -1))
        )
    }

    @Test
    fun onlyActiveVehicleRemindersAreEligibleForRecovery() {
        val activeVehicle = ReminderEntity(
            title = "Insurance expiry",
            date = "۱۴۰۶/۰۱/۰۱",
            sourceType = "VEHICLE",
            status = "ACTIVE"
        )
        val disabledVehicle = activeVehicle.copy(status = "DISABLED")
        val activeManual = activeVehicle.copy(sourceType = "MANUAL")

        assertTrue(VehicleReminderMigrationPolicy.shouldRestore(activeVehicle))
        assertFalse(VehicleReminderMigrationPolicy.shouldRestore(disabledVehicle))
        assertFalse(VehicleReminderMigrationPolicy.shouldRestore(activeManual))
    }
}
