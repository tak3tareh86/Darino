package com.example.calendar.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.reminder.data.ReminderEntity
import com.example.reminder.domain.NotificationDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalendarEventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SessionManager.init(context.applicationContext)
                val dao = AppDatabase.getDatabase(context).financialEventDao()
                val userId = SessionManager.userId
                if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
                    intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                    intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
                    intent.action == Intent.ACTION_TIME_CHANGED ||
                    intent.action == Intent.ACTION_TIMEZONE_CHANGED
                ) {
                    if (userId != null) {
                        val scheduler = CalendarEventReminderScheduler(context)
                        dao.getEventsWithReminders(userId).forEach { entity ->
                            scheduler.schedule(entity.toDomain())
                        }
                    }
                } else if (
                    intent.action == CalendarEventReminderScheduler.ACTION_TRIGGER ||
                    intent.action == CalendarEventReminderScheduler.ACTION_SNOOZE_TRIGGER
                ) {
                    val eventId = intent.getStringExtra(CalendarEventReminderScheduler.EXTRA_EVENT_ID)
                        ?: return@launch
                    if (userId == null) return@launch
                    val event = dao.getEventById(userId, eventId)?.toDomain() ?: return@launch
                    if (event.status.name == "PAID") return@launch
                    val scheduler = CalendarEventReminderScheduler(context)
                    if (intent.action == CalendarEventReminderScheduler.ACTION_TRIGGER) {
                        val triggerAt = scheduler.triggerAtMillis(event) ?: return@launch
                        if (triggerAt > System.currentTimeMillis()) {
                            scheduler.schedule(event)
                            return@launch
                        }
                    }
                    val reminder = ReminderEntity(
                        id = "calendar_${event.id}",
                        userId = userId,
                        title = event.title,
                        description = event.description,
                        type = event.type.name,
                        sourceType = "CALENDAR",
                        sourceId = event.id,
                        priority = "NORMAL",
                        status = "ACTIVE",
                        date = event.date,
                        time = event.time ?: "۰۹:۰۰",
                        notificationEnabled = true,
                        fullScreenAlarmEnabled = false
                    )
                    val reminderDao = AppDatabase.getDatabase(context).smartReminderDao()
                    val storedReminder = reminderDao.getReminderById(userId, reminder.id)
                    if (storedReminder == null) {
                        // Adopt legacy calendar entries into the canonical reminder store.
                        // Empty schedules avoid creating a second alarm; this receiver remains
                        // the only trigger for the legacy financial_events row.
                        com.example.reminder.domain.ReminderManager(context).createReminder(reminder, emptyList())
                    } else if (!storedReminder.status.equals("ACTIVE", ignoreCase = true)) {
                        return@launch
                    }
                    NotificationDispatcher(context).dispatchNotification(storedReminder ?: reminder)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to process calendar event reminder", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "CalendarEventReminder"
    }
}
