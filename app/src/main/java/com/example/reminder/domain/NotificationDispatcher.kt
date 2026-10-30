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
import com.example.reminder.presentation.ReminderAlarmActivity
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

            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val alarmAudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
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
                },
                NotificationChannel(
                    CHANNEL_FULL_SCREEN_ALARMS,
                    "آلارم‌های تمام‌صفحه دارینو",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "هشدارهای زمان‌بندی‌شده‌ای که کاربر برای نمایش تمام‌صفحه انتخاب کرده است"
                    enableVibration(true)
                    setSound(alarmSoundUri, alarmAudioAttributes)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
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

        val deliveryPref = try {
            AppPreferencesRepository.getInstance(context).alertDelivery.value
        } catch (e: Exception) {
            AlertDeliveryPreference.NOTIFICATION_ONLY
        }
        val useFullScreenAlarm = deliveryPref == AlertDeliveryPreference.FULL_SCREEN_ALARM ||
            deliveryPref == AlertDeliveryPreference.BOTH
        // BOTH combines a full-screen alarm with a separately retained standard notification.
        val standardNotificationId = if (deliveryPref == AlertDeliveryPreference.BOTH) {
            NotificationIdGenerator.generateNotificationId(
                reminderId = reminder.id,
                scheduleId = "standard:${schedule?.id.orEmpty()}",
                triggerTime = triggerTime
            )
        } else -1
        val channelId = if (useFullScreenAlarm) {
            CHANNEL_FULL_SCREEN_ALARMS
        } else {
            when (reminder.type.uppercase()) {
                "INSTALLMENT" -> CHANNEL_INSTALLMENTS
                "VEHICLE", "INSURANCE", "MAINTENANCE", "FUEL" -> CHANNEL_VEHICLES
                else -> CHANNEL_REMINDERS
            }
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
            putExtra(ReminderBroadcastReceiver.EXTRA_SECONDARY_NOTIFICATION_ID, standardNotificationId)
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
            putExtra(ReminderBroadcastReceiver.EXTRA_SECONDARY_NOTIFICATION_ID, standardNotificationId)
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

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.img_3d_bell_notification)
            .setContentTitle(reminder.title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(
                if (useFullScreenAlarm || reminder.priority.equals("HIGH", ignoreCase = true))
                    NotificationCompat.PRIORITY_MAX
                else NotificationCompat.PRIORITY_HIGH
            )
            .setCategory(if (useFullScreenAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(!useFullScreenAlarm)
            .setOngoing(useFullScreenAlarm)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.img_3d_installment, "انجام شد", completePendingIntent)
            .addAction(R.drawable.img_3d_calendar, "۱۵ دقیقه بعد", snoozePendingIntent)

        if (useFullScreenAlarm) {
            val alarmActivityIntent = Intent(context, ReminderAlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ReminderAlarmActivity.EXTRA_REMINDER_ID, reminder.id)
                putExtra(ReminderAlarmActivity.EXTRA_NOTIFICATION_ID, notifId)
                putExtra(ReminderAlarmActivity.EXTRA_SECONDARY_NOTIFICATION_ID, standardNotificationId)
                putExtra(ReminderAlarmActivity.EXTRA_TITLE, reminder.title)
                putExtra(ReminderAlarmActivity.EXTRA_BODY, bodyText)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                notifId + 3000,
                alarmActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
        }
        val notification = notificationBuilder.build()
        val showNotification = true

        var notificationPosted = false
        var deliveryFailure: String? = null
        if (showNotification) {
            val hasRuntimePermission = notificationManager.areNotificationsEnabled() &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED)
            val channelEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                notificationManager.getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE
            val fullScreenAccessGranted = Build.VERSION.SDK_INT < 34 ||
                notificationManager.canUseFullScreenIntent()

            if (useFullScreenAlarm && !fullScreenAccessGranted) {
                Log.w("NotificationDispatcher", "Full-screen intent access is not granted. Android will show the high-priority notification fallback for reminderId=${reminder.id}.")
            }

            when {
                !hasRuntimePermission -> {
                    deliveryFailure = "POST_NOTIFICATIONS permission is not granted"
                    Log.w("NotificationDispatcher", "Cannot post notification $notifId: notification permission denied.")
                }
                !channelEnabled -> {
                    deliveryFailure = "Notification channel $channelId is disabled"
                    Log.w("NotificationDispatcher", "Cannot post notification $notifId: channel $channelId is disabled.")
                }
                else -> {
                    try {
                        notificationManager.notify(notifId, notification)
                        notificationPosted = true
                        if (deliveryPref == AlertDeliveryPreference.BOTH) {
                            val standardChannelId = when (reminder.type.uppercase()) {
                                "INSTALLMENT" -> CHANNEL_INSTALLMENTS
                                "VEHICLE", "INSURANCE", "MAINTENANCE", "FUEL" -> CHANNEL_VEHICLES
                                else -> CHANNEL_REMINDERS
                            }
                            val standardChannelEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                                notificationManager.getNotificationChannel(standardChannelId)?.importance != NotificationManager.IMPORTANCE_NONE
                            if (standardChannelEnabled) {
                                val standardNotification = NotificationCompat.Builder(context, standardChannelId)
                                    .setSmallIcon(R.drawable.img_3d_bell_notification)
                                    .setContentTitle(reminder.title)
                                    .setContentText(bodyText)
                                    .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                                    .setSilent(true)
                                    .setAutoCancel(true)
                                    .setContentIntent(contentPendingIntent)
                                    .addAction(R.drawable.img_3d_installment, "انجام شد", completePendingIntent)
                                    .addAction(R.drawable.img_3d_calendar, "۱۵ دقیقه بعد", snoozePendingIntent)
                                    .build()
                                notificationManager.notify(standardNotificationId, standardNotification)
                            } else {
                                Log.w("NotificationDispatcher", "The additional standard notification channel is disabled for reminderId=${reminder.id}.")
                            }
                        }
                        Log.i("NotificationDispatcher", "Notification posted to Android notification manager: id=$notifId, reminderId=${reminder.id}")
                    } catch (e: SecurityException) {
                        deliveryFailure = "Notification permission/security exception: ${e.message}"
                        Log.e("NotificationDispatcher", "Android rejected notification $notifId", e)
                    } catch (e: Exception) {
                        deliveryFailure = "Notification post failed: ${e.message}"
                        Log.e("NotificationDispatcher", "Failed to post notification $notifId", e)
                    }
                }
            }
        } else {
            Log.i("NotificationDispatcher", "Notification skipped due to delivery preference: $deliveryPref")
        }

        // "POSTED" means accepted by Android's notification manager, not proof that
        // the notification was visibly shown to the user.
        val deliveryStatus = when {
            notificationPosted -> "POSTED"
            deliveryFailure != null -> "FAILED"
            else -> "SKIPPED"
        }

        try {
            val db = AppDatabase.getDatabase(context)
            db.smartReminderDao().insertDeliveryLog(
                ReminderDeliveryLogEntity(
                    reminderId = reminder.id,
                    scheduleId = schedule?.id,
                    channel = "NOTIFICATION",
                    scheduledAt = triggerTime,
                    triggeredAt = System.currentTimeMillis(),
                    status = deliveryStatus,
                    errorMessage = deliveryFailure
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
                    status = deliveryStatus
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
        const val CHANNEL_FULL_SCREEN_ALARMS = "channel_full_screen_alarms"
    }
}
