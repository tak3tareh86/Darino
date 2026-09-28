package com.example.data.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.database.ReminderEntity

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: ReminderEntity) {
        if (!reminder.enabled) return

        val triggerTime = reminder.scheduledDateTime
        if (triggerTime <= System.currentTimeMillis()) {
            Log.i("ReminderScheduler", "Skipping reminder ${reminder.id} as its scheduled date/time is in the past.")
            return
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.i("ReminderScheduler", "Scheduled exact alarm for reminder: ${reminder.title} at $triggerTime")
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.i("ReminderScheduler", "Exact alarm permission missing. Fallback to inexact alarm for: ${reminder.title}")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.i("ReminderScheduler", "Scheduled exact alarm for reminder: ${reminder.title} on API < 31")
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule alarm for reminder: ${reminder.title}", e)
        }
    }

    fun cancel(reminderId: Int) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i("ReminderScheduler", "Cancelled alarm schedule for reminder ID: $reminderId")
        }
    }
}
