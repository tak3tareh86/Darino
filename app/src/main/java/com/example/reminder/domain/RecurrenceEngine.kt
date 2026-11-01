package com.example.reminder.domain

import com.example.reminder.data.ReminderScheduleEntity
import com.example.util.PersianCalendarHelper
import java.util.Calendar
import java.util.UUID

object RecurrenceEngine {

    /**
     * Calculates the next trigger timestamp based on current trigger time, repeat type, and repeat interval.
     */
    fun calculateNextOccurrence(
        currentTriggerMillis: Long,
        repeatType: RepeatType,
        interval: Int = 1
    ): Long {
        if (repeatType == RepeatType.NONE || interval <= 0) {
            return currentTriggerMillis
        }

        // Match the timezone used by PersianCalendarHelper for date/time conversion.
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTriggerMillis
        }

        when (repeatType) {
            RepeatType.NONE -> {
                return currentTriggerMillis
            }
            RepeatType.DAILY -> {
                cal.add(Calendar.DAY_OF_YEAR, interval)
            }
            RepeatType.WEEKLY -> {
                cal.add(Calendar.WEEK_OF_YEAR, interval)
            }
            RepeatType.MONTHLY -> {
                // In Persian calendar, add 1 Persian month
                val pDate = PersianCalendarHelper.fromEpochMillis(currentTriggerMillis)
                var nextMonth = pDate.month + interval
                var nextYear = pDate.year
                while (nextMonth > 12) {
                    nextMonth -= 12
                    nextYear += 1
                }
                val maxDaysInNextMonth = PersianCalendarHelper.getDaysInMonth(nextYear, nextMonth)
                val nextDay = pDate.day.coerceAtMost(maxDaysInNextMonth)
                return PersianCalendarHelper.jalaliToEpochMillis(
                    year = nextYear,
                    month = nextMonth,
                    day = nextDay,
                    hour = pDate.hour,
                    minute = pDate.minute
                )
            }
            RepeatType.YEARLY -> {
                val pDate = PersianCalendarHelper.fromEpochMillis(currentTriggerMillis)
                val nextYear = pDate.year + interval
                val maxDays = PersianCalendarHelper.getDaysInMonth(nextYear, pDate.month)
                val nextDay = pDate.day.coerceAtMost(maxDays)
                return PersianCalendarHelper.jalaliToEpochMillis(
                    year = nextYear,
                    month = pDate.month,
                    day = nextDay,
                    hour = pDate.hour,
                    minute = pDate.minute
                )
            }
        }

        return cal.timeInMillis
    }

    /**
     * Generates the next schedule entity for a recurring schedule.
     */
    fun generateNextSchedule(
        currentSchedule: ReminderScheduleEntity,
        nextTriggerMillis: Long
    ): ReminderScheduleEntity {
        return currentSchedule.copy(
            id = UUID.randomUUID().toString(),
            triggerDateTime = nextTriggerMillis,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Validates whether a recurrence specification is logically correct.
     */
    fun validateRecurrence(repeatType: RepeatType, interval: Int): Boolean {
        if (interval < 1 || interval > 365) return false
        return repeatType != RepeatType.NONE
    }
}
