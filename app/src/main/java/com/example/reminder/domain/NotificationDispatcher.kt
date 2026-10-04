package com.example.reminder.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.database.NotificationLogEntity
import com.example.data.preferences.AppPreferencesRepository
import com.example.reminder.data.ReminderDeliveryLogEntity
import com.example.reminder.data.ReminderEntity
import com.example.reminder.data.ReminderScheduleEntity
import com.example.reminder.receiver.ReminderBroadcastReceiver
import com.example.ui.screens.settings.model.AlertDeliveryPreference
import com.example.util.MoneyFormatter

class NotificationDispatcher(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * Initializes all standard notification channels with Persian labels and importance.
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    "یادآورهای عمومی و شخصی",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "اعلان‌های مربوط به یادآوری‌های شخصی، کاری و عمومی"
                    enableVibration(true)
                    setSound(defaultSoundUri, audioAttributes)
                },
                NotificationChannel(
                    CHANNEL_INSTALLMENTS,
                    "اقساط و سررسیدهای مالی",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "اعلان‌های سررسید اقساط بانکی و چک‌ها"
                    enableVibration(true)
                    setSound(defaultSoundUri, audioAttributes)
                },
                NotificationChannel(
                    CHANNEL_VEHICLES,
                    "سرویس و بیمه خودرو",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "یادآوری تمدید بیمه، تعویض روغن و سرویس‌های خودرو"
                    enableVibration(true)
                    setSound(defaultSoundUri, audioAttributes)
                },
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "اعلان‌های عمومی سیستم",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "گزارشات و اعلان‌های کلی برنامه"
                }
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Builds and displays the notification for a reminder schedule.
     */
    suspend fun dispatchNotification(
        reminder: ReminderEntity,
        schedule: ReminderScheduleEntity? = null
    ): Int {
        val triggerTime = schedule?.triggerDateTime ?: System.currentTimeMillis()
        val notifId = NotificationIdGenerator.generateNotificationId(
            reminderId = reminder.id,
            scheduleId = schedule?.id,
            triggerTime = triggerTime
        )

        val channelId = when (reminder.type.uppercase()) {
            "INSTALLMENT" -> CHANNEL_INSTALLMENTS
            "VEHICLE", "INSURANCE", "MAINTENANCE", "FUEL" -> CHANNEL_VEHICLES
            else -> CHANNEL_REMINDERS
        }

        // 1. Content Intent (Open App + Deep Link)
        val deepLinkIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("DEEP_LINK_TARGET", reminder.type)
            putExtra("DEEP_LINK_ID", reminder.id)
            putExtra("DEEP_LINK_SOURCE_TYPE", reminder.sourceType)
            putExtra("DEEP_LINK_SOURCE_ID", reminder.sourceId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action: Complete Reminder
        val completeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_COMPLETE_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 1000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action: Snooze 15 Minutes
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SNOOZE_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(ReminderBroadcastReceiver.EXTRA_SNOOZE_MINUTES, 15)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 2000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification body
        val amountText = reminder.amount?.let { " • مبلغ: ${MoneyFormatter.formatToman(it)}" }.orEmpty()
        val bodyText = if (reminder.description.isNotBlank()) {
            "${reminder.description}$amountText"
        } else {
            "سررسید یادآوری: ${reminder.date} ساعت ${reminder.time}$amountText"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.img_3d_bell_notification)
            .setContentTitle(reminder.title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(
                if (reminder.priority.equals("HIGH", ignoreCase = true))
                    NotificationCompat.PRIORITY_MAX
                else NotificationCompat.PRIORITY_HIGH
            )
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.img_3d_installment, "انجام شد", completePendingIntent)
            .addAction(R.drawable.img_3d_calendar, "۱۵ دقیقه بعد", snoozePendingIntent)
            .build()

        val deliveryPref = try {
            AppPreferencesRepository.getInstance(context).alertDelivery.value
        } catch (e: Exception) {
            AlertDeliveryPreference.BOTH
        }

        if (deliveryPref == AlertDeliveryPreference.DISABLED) {
            Log.i("NotificationDispatcher", "Alert delivery disabled by user setting. Skipping.")
            return notifId
        }

        val showNotification = deliveryPref == AlertDeliveryPreference.BOTH ||
                deliveryPref == AlertDeliveryPreference.NOTIFICATION_ONLY

        if (showNotification) {
            notificationManager.notify(notifId, notification)
            Log.i("NotificationDispatcher", "Dispatched notification id $notifId for reminder ${reminder.title}")
        } else {
            Log.i("NotificationDispatcher", "Notification banner skipped due to delivery preference: $deliveryPref (SMS Only active)")
        }

        // Save delivery logs in database
        try {
            val db = AppDatabase.getDatabase(context)
            db.smartReminderDao().insertDeliveryLog(
                ReminderDeliveryLogEntity(
                    reminderId = reminder.id,
                    scheduleId = schedule?.id,
                    channel = if (showNotification) "NOTIFICATION" else "SMS",
                    scheduledAt = triggerTime,
                    triggeredAt = System.currentTimeMillis(),
                    status = "DELIVERED"
                )
            )

            db.notificationLogDao().insertNotification(
                NotificationLogEntity(
                    userId = reminder.userId,
                    reminderId = notifId,
                    title = reminder.title,
                    message = bodyText,
                    type = reminder.type,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    source = "SMART_REMINDER",
                    status = "DELIVERED"
                )
            )
        } catch (e: Exception) {
            Log.e("NotificationDispatcher", "Error logging notification delivery", e)
        }

        return notifId
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    companion object {
        const val CHANNEL_REMINDERS = "channel_reminders"
        const val CHANNEL_INSTALLMENTS = "channel_installments"
        const val CHANNEL_VEHICLES = "channel_vehicles"
        const val CHANNEL_GENERAL = "channel_general"
    }
}
