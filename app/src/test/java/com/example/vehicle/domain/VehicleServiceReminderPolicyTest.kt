package com.example.vehicle.domain

import com.example.reminder.domain.PredefinedOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class VehicleServiceReminderPolicyTest {

    @Test
    fun onlyExplicitlySelectedFutureOffsetsAreReturned() {
        val base = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = Calendar.getInstance().apply {
            timeInMillis = base
            add(Calendar.DAY_OF_YEAR, -2)
        }.timeInMillis

        val selected = listOf(
            PredefinedOffset.BEFORE_7_DAYS,
            PredefinedOffset.BEFORE_3_DAYS,
            PredefinedOffset.BEFORE_1_DAY,
            PredefinedOffset.AT_TIME
        )

        val future = VehicleServiceReminderPolicy.futureOffsets(base, selected, now)

        assertEquals(
            listOf(
                PredefinedOffset.BEFORE_1_DAY,
                PredefinedOffset.AT_TIME
            ),
            future
        )
    }

    @Test
    fun duplicateOffsetsAreScheduledOnlyOnce() {
        val base = System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L
        val selected = listOf(
            PredefinedOffset.BEFORE_1_DAY,
            PredefinedOffset.BEFORE_1_DAY,
            PredefinedOffset.AT_TIME
        )

        assertEquals(
            listOf(PredefinedOffset.BEFORE_1_DAY, PredefinedOffset.AT_TIME),
            VehicleServiceReminderPolicy.futureOffsets(base, selected, System.currentTimeMillis())
        )
    }

    @Test
    fun dayOffsetPreservesLocalTimeAcrossDaylightSavingTransition() {
        val previousZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val due = Calendar.getInstance().apply {
                set(2024, Calendar.MARCH, 10, 9, 30, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val trigger = Calendar.getInstance().apply {
                timeInMillis = VehicleServiceReminderPolicy.triggerTimeMillis(
                    due,
                    PredefinedOffset.BEFORE_1_DAY
                )
            }

            assertEquals(9, trigger.get(Calendar.HOUR_OF_DAY))
            assertEquals(30, trigger.get(Calendar.MINUTE))
            assertEquals(Calendar.MARCH, trigger.get(Calendar.MONTH))
            assertEquals(9, trigger.get(Calendar.DAY_OF_MONTH))
            assertFalse("A calendar day across DST is not always 24 elapsed hours", due - trigger.timeInMillis == 24 * 60 * 60 * 1000L)
        } finally {
            TimeZone.setDefault(previousZone)
        }
    }

    @Test
    fun dueDateOffsetUsesExactSelectedDateAndTime() {
        val base = 1_800_000_000_000L
        assertEquals(
            base,
            VehicleServiceReminderPolicy.triggerTimeMillis(base, PredefinedOffset.AT_TIME)
        )
        assertTrue(
            VehicleServiceReminderPolicy.futureOffsets(
                base,
                listOf(PredefinedOffset.AT_TIME),
                base - 1
            ).contains(PredefinedOffset.AT_TIME)
        )
    }
}
