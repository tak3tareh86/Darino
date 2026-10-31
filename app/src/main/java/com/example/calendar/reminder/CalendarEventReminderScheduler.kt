package com.example.calendar.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.calendar.domain.model.FinancialEvent

class CalendarEventReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(event: FinancialEvent) {
        cancel(event.id)
        val triggerAt = event.customReminderAt ?: return
        if (triggerAt <= System.currentTimeMillis()) return

        val pendingIntent = pendingIntent(event.id, event.title, event.description)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (error: SecurityException) {
            Log.e(TAG, "Unable to schedule calendar reminder for ${event.id}", error)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancel(eventId: String) {
        val intent = baseIntent(eventId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun pendingIntent(eventId: String, title: String, description: String): PendingIntent {
        val intent = baseIntent(eventId).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DESCRIPTION, description)
        }
        return PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun baseIntent(eventId: String) = Intent(context, CalendarEventReminderReceiver::class.java).apply {
        action = ACTION_TRIGGER
        data = Uri.parse("darino://calendar-reminder/$eventId")
        putExtra(EXTRA_EVENT_ID, eventId)
    }

    companion object {
        const val ACTION_TRIGGER = "com.example.action.TRIGGER_CALENDAR_EVENT_REMINDER"
        const val EXTRA_EVENT_ID = "calendar_event_id"
        const val EXTRA_TITLE = "calendar_event_title"
        const val EXTRA_DESCRIPTION = "calendar_event_description"
        private const val TAG = "CalendarEventReminder"
    }
}
