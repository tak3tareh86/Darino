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
     */
    fun schedule(reminder: ReminderEntity, schedules: List<ReminderScheduleEntity>) {
        if (!reminder.notificationEnabled && !reminder.smsEnabled) {
            Log.i("ReminderScheduler", "Skipping schedule for reminder ${reminder.id} because all channels are disabled.")
            return
        }

        schedules.forEach { schedule ->
            if (schedule.enabled) {
                scheduleSingle(reminder, schedule)
            }
        }
    }

    /**
     * Schedules a single schedule trigger using AlarmManager exact alarm (with fallback).
     */
    fun scheduleSingle(reminder: ReminderEntity, schedule: ReminderScheduleEntity) {
        val triggerTime = schedule.triggerDateTime
        val now = System.currentTimeMillis()

        if (triggerTime <= now) {
            Log.i("ReminderScheduler", "Trigger time $triggerTime is in the past for schedule ${schedule.id}. Skipping.")
            return
        }

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
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                        Log.i("ReminderScheduler", "Set exact alarm for ${reminder.title} (Schedule ${schedule.id}) at $triggerTime")
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                        Log.i("ReminderScheduler", "Fallback inexact alarm for ${reminder.title} (Schedule ${schedule.id}) at $triggerTime")
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.i("ReminderScheduler", "Set exact alarm on pre-API 31 for ${reminder.title} at $triggerTime")
                }
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule alarm for reminder ${reminder.title}", e)
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
        val snoozeSchedule = ReminderScheduleEntity(
            id = "snooze_${UUID.randomUUID()}",
            reminderId = reminder.id,
            triggerType = "EXACT",
            offsetValue = snoozeMinutes,
            offsetUnit = "MINUTE",
            triggerDateTime = snoozeTime,
            repeatType = "NONE",
            enabled = true,
            createdAt = System.currentTimeMillis()
        )

        scheduleSingle(reminder, snoozeSchedule)
        Log.i("ReminderScheduler", "Created independent snooze trigger for ${reminder.title} in $snoozeMinutes minutes.")
        return snoozeSchedule
    }

    /**
     * Rebuilds and re-arms all active schedules from the Room database.
     * Called after device boot, app update, timezone change, or process recovery.
     */
    suspend fun rebuildAllSchedules() {
        try {
            val db = AppDatabase.getDatabase(context)
            val activeReminders = db.smartReminderDao().getActiveRemindersList()
            var count = 0

            activeReminders.forEach { reminder ->
                val schedules = db.smartReminderDao().getSchedulesForReminderSync(reminder.id)
                schedules.forEach { schedule ->
                    if (schedule.enabled && schedule.triggerDateTime > System.currentTimeMillis()) {
                        scheduleSingle(reminder, schedule)
                        count++
                    }
                }
            }
            Log.i("ReminderScheduler", "Rebuilt $count persistent active schedules after boot / reset.")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Error rebuilding schedules from database", e)
        }
    }
}
