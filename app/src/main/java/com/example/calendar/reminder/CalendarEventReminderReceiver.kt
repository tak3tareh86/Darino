package com.example.calendar.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalendarEventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
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
                        dao.getEventsWithCustomReminders(userId).forEach { entity ->
                            scheduler.schedule(entity.toDomain())
                        }
                    }
                } else if (intent.action == CalendarEventReminderScheduler.ACTION_TRIGGER) {
                    val eventId = intent.getStringExtra(CalendarEventReminderScheduler.EXTRA_EVENT_ID)
                        ?: return@launch
                    if (userId == null) return@launch
                    val event = dao.getEventById(userId, eventId)?.toDomain() ?: return@launch
                    if (event.status.name == "PAID") return@launch
                    val triggerAt = event.customReminderAt ?: return@launch
                    if (triggerAt > System.currentTimeMillis()) {
                        CalendarEventReminderScheduler(context).schedule(event)
                        return@launch
                    }
                    showNotification(context, event.id, event.title, event.description)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to process calendar event reminder", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, eventId: String, title: String, description: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "یادآوری رویدادهای مالی", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val openApp = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("DEEP_LINK_TARGET", "FINANCIAL_CALENDAR")
                putExtra("DEEP_LINK_ID", eventId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.img_3d_bell_notification)
            .setContentTitle(title)
            .setContentText(description.ifBlank { "زمان یادآوری رویداد مالی فرا رسیده است." })
            .setStyle(NotificationCompat.BigTextStyle().bigText(description.ifBlank { "زمان یادآوری رویداد مالی فرا رسیده است." }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        manager.notify(eventId.hashCode(), notification)
    }

    companion object {
        private const val TAG = "CalendarEventReminder"
        private const val CHANNEL_ID = "calendar_custom_event_reminders"
    }
}
