package com.example.vehicle.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleMileageReminderPolicyTest {
    @Test
    fun selectedLeadDistanceIsSubtractedFromServiceTarget() {
        assertEquals(4_500, VehicleMileageReminderPolicy.thresholdKilometer(5_000, 500))
        assertEquals(4_800, VehicleMileageReminderPolicy.thresholdKilometer(5_000, 200))
        assertEquals(4_000, VehicleMileageReminderPolicy.thresholdKilometer(5_000, 1_000))
    }

    @Test
    fun thresholdIsClampedAtZero() {
        assertEquals(0, VehicleMileageReminderPolicy.thresholdKilometer(300, 500))
    }

    @Test
    fun notificationBecomesDueAtSelectedAdvanceThreshold() {
        assertFalse(VehicleMileageReminderPolicy.isDue(4_499, 5_000, 500))
        assertTrue(VehicleMileageReminderPolicy.isDue(4_500, 5_000, 500))
        assertTrue(VehicleMileageReminderPolicy.isDue(4_700, 5_000, 500))
    }

    @Test
    fun crossingRequiresMovingFromBelowToAtOrAboveThreshold() {
        assertFalse(VehicleMileageReminderPolicy.isCrossed(4_500, 4_600, 5_000, 500))
        assertTrue(VehicleMileageReminderPolicy.isCrossed(4_499, 4_500, 5_000, 500))
        assertTrue(VehicleMileageReminderPolicy.isCrossed(4_499, 4_700, 5_000, 500))
        assertFalse(VehicleMileageReminderPolicy.isCrossed(4_700, 4_800, 5_000, 500))
    }
}
