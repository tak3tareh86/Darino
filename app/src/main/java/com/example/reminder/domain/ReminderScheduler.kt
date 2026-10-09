package com.example.reminder.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.reminder.data.ReminderEntity
import com.example.reminder.data.ReminderScheduleEntity
import com.example.reminder.receiver.ReminderBroadcastReceiver
import java.util.UUID

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    /**
     * Schedules all enabled triggers for a reminder.
     * Propagates errors to caller and cancels newly registered alarms from this batch if any trigger fails.
     */
    fun schedule(reminder: ReminderEntity, schedules: List<ReminderScheduleEntity>) {
        if (!reminder.notificationEnabled && !reminder.smsEnabled) {
            Log.i("ReminderScheduler", "Skipping schedule for reminder ${reminder.id} because all channels are disabled.")
            return
        }

        val newlyScheduled = mutableListOf<ReminderScheduleEntity>()
        val now = System.currentTimeMillis()
        for (schedule in schedules) {
            if (schedule.enabled && schedule.triggerDateTime > now) {
                try {
                    scheduleSingle(reminder, schedule)
                    newlyScheduled.add(schedule)
                } catch (e: Exception) {
                    Log.e("ReminderScheduler", "Failed to schedule trigger for reminder ${reminder.title}, schedule ${schedule.id}. Rolling back newly registered alarms in this batch.", e)
                    newlyScheduled.forEach { scheduledItem ->
                        cancelSchedule(scheduledItem.id)
                    }
                    throw e
                }
            }
        }
    }

    /**
     * Schedules a single schedule trigger using AlarmManager exact alarm (with fallback).
     * Throws an exception on failure so caller can detect failure and perform compensation if needed.
     */
    fun scheduleSingle(reminder: ReminderEntity, schedule: ReminderScheduleEntity) {
        val triggerTime = schedule.triggerDateTime
        val now = System.currentTimeMillis()

        if (triggerTime <= now) {
            Log.w("ReminderScheduler", "Trigger time $triggerTime is in the past for schedule ${schedule.id}. Cannot schedule.")
            throw IllegalArgumentException("زمان اجرای هشدار در گذشته است ($triggerTime <= $now).")
        }

        val manager = alarmManager
            ?: throw IllegalStateException("سرویس AlarmManager در سیستم در دسترس نیست.")

        val requestCode = NotificationIdGenerator.generateAlarmRequestCode(schedule.id)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_SCHEDULE_ID, schedule.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (manager.canScheduleExactAlarms()) {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.i("ReminderScheduler", "Set exact alarm for ${reminder.title} (Schedule ${schedule.id}) at $triggerTime")
                } else {
                    manager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.i("ReminderScheduler", "Fallback inexact alarm for ${reminder.title} (Schedule ${schedule.id}) at $triggerTime")
                }
            } else {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.i("ReminderScheduler", "Set exact alarm on pre-API 31 for ${reminder.title} at $triggerTime")
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule alarm for reminder ${reminder.title}", e)
            throw e
        }
    }

    /**
     * Cancels all scheduled alarms for a reminder.
     */
    fun cancel(reminderId: String, schedules: List<ReminderScheduleEntity>) {
        schedules.forEach { schedule ->
            cancelSchedule(schedule.id)
        }
    }

    /**
     * Cancels a specific schedule.
     */
    fun cancelSchedule(scheduleId: String) {
        val requestCode = NotificationIdGenerator.generateAlarmRequestCode(scheduleId)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i("ReminderScheduler", "Cancelled alarm for schedule ID: $scheduleId")
        }
    }

    /**
     * Snooze a reminder by creating a temporary independent trigger without corrupting original schedules.
     */
    fun snooze(reminder: ReminderEntity, snoozeMinutes: Int = 15): ReminderScheduleEntity {
        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        return snoozeToMillis(reminder, snoozeTime, snoozeMinutes)
    }

    /**
     * Schedules an independent snooze trigger at an exact future epoch millisecond timestamp.
     */
    fun snoozeToMillis(reminder: ReminderEntity, triggerMillis: Long, offsetMinutes: Int = 0): ReminderScheduleEntity {
        val snoozeSchedule = ReminderScheduleEntity(
            id = "snooze_${UUID.randomUUID()}",
            reminderId = reminder.id,
            triggerType = "EXACT",
            offsetValue = offsetMinutes,
            offsetUnit = "MINUTE",
            triggerDateTime = triggerMillis,
            repeatType = "NONE",
            enabled = true,
            createdAt = System.currentTimeMillis()
        )

        scheduleSingle(reminder, snoozeSchedule)
        Log.i("ReminderScheduler", "Created independent snooze trigger for ${reminder.title} at $triggerMillis.")
        return snoozeSchedule
    }

    /**
     * Rebuilds and re-arms all active schedules from the Room database.
     * Called after device boot, app update, timezone change, or process recovery.
     */
    suspend fun rebuildAllSchedules() {
        try {
            val db = AppDatabase.getDatabase(context)
            val userId = com.example.data.security.SessionManager.userId ?: run {
                Log.i("ReminderScheduler", "No authenticated user; skipping schedule rebuild.")
                return
            }
            val activeReminders = db.smartReminderDao().getActiveRemindersList(userId)
            var count = 0

            activeReminders.forEach { reminder ->
                val schedules = db.smartReminderDao().getSchedulesForReminderSync(reminder.userId, reminder.id)
                schedules.forEach { schedule ->
                    if (schedule.enabled && schedule.triggerDateTime > System.currentTimeMillis()) {
                        try {
                            scheduleSingle(reminder, schedule)
                            count++
                        } catch (e: Exception) {
                            Log.e("ReminderScheduler", "Failed to re-arm schedule ${schedule.id} during rebuild", e)
                        }
                    }
                }
            }
            Log.i("ReminderScheduler", "Rebuilt $count persistent active schedules after boot / reset.")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Error rebuilding schedules from database", e)
        }
    }
}
