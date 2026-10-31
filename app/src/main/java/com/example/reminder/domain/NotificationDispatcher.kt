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

    /**
     * Cancels only notifications posted for the given reminder. Reminder-tagged
     * notifications are isolated from calendar and other application notifications.
     */
    fun cancelReminderNotifications(
        reminderId: String,
        schedules: List<ReminderScheduleEntity> = emptyList()
    ) {
        val reminderTag = "reminder:$reminderId"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications
                .filter { it.tag == reminderTag }
                .forEach { notificationManager.cancel(it.tag, it.id) }
        }

        // Legacy notifications were posted without tags. Rebuild both generated IDs
        // from schedule data captured before schedule rows are updated or deleted.
        schedules.forEach { schedule ->
            val notificationId = NotificationIdGenerator.generateNotificationId(
                reminderId = reminderId,
                scheduleId = schedule.id,
                triggerTime = schedule.triggerDateTime
            )
            val standardNotificationId = NotificationIdGenerator.generateNotificationId(
                reminderId = reminderId,
                scheduleId = "standard:${schedule.id}",
                triggerTime = schedule.triggerDateTime
            )
            notificationManager.cancel(notificationId)
            notificationManager.cancel(standardNotificationId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.cancel(reminderTag, notificationId)
                notificationManager.cancel(reminderTag, standardNotificationId)
            }
        }
    }

    /**
     * Cancels only this app's reminder notifications, leaving calendar and other
     * notification categories untouched.
     */
    fun cancelAllReminderNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications
                .filter { it.tag?.startsWith("reminder:") == true }
                .forEach { notificationManager.cancel(it.tag, it.id) }
        }
    }

    private fun configuredChannelId(baseId: String): String {
        val preferences = AppPreferencesRepository.getInstance(context)
        val sound = if (preferences.notificationSoundEnabled.value) 1 else 0
        val vibration = if (preferences.notificationVibrationEnabled.value) 1 else 0
        return "${baseId}_s${sound}_v${vibration}"
    }

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
            val preferences = AppPreferencesRepository.getInstance(context)
            val soundEnabled = preferences.notificationSoundEnabled.value
            val vibrationEnabled = preferences.notificationVibrationEnabled.value
            val channels = listOf(
                NotificationChannel(
                    configuredChannelId(CHANNEL_REMINDERS),
                    "یادآورهای عمومی و شخصی",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "اعلان‌های مربوط به یادآوری‌های شخصی، کاری و عمومی"
                    enableVibration(vibrationEnabled)
                    setSound(if (soundEnabled) defaultSoundUri else null, if (soundEnabled) audioAttributes else null)
                },
                NotificationChannel(
                    configuredChannelId(CHANNEL_INSTALLMENTS),
                    "اقساط و سررسیدهای مالی",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "اعلان‌های سررسید اقساط بانکی و چک‌ها"
                    enableVibration(vibrationEnabled)
                    setSound(if (soundEnabled) defaultSoundUri else null, if (soundEnabled) audioAttributes else null)
                },
                NotificationChannel(
                    configuredChannelId(CHANNEL_VEHICLES),
                    "سرویس و بیمه خودرو",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "یادآوری تمدید بیمه، تعویض روغن و سرویس‌های خودرو"
                    enableVibration(vibrationEnabled)
                    setSound(if (soundEnabled) defaultSoundUri else null, if (soundEnabled) audioAttributes else null)
                },
                NotificationChannel(
                    configuredChannelId(CHANNEL_GENERAL),
                    "اعلان‌های عمومی سیستم",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "گزارشات و اعلان‌های کلی برنامه"
                    enableVibration(vibrationEnabled)
                    setSound(if (soundEnabled) defaultSoundUri else null, if (soundEnabled) audioAttributes else null)
                },
                NotificationChannel(
                    configuredChannelId(CHANNEL_FULL_SCREEN_ALARMS),
                    "آلارم‌های تمام‌صفحه دارینو",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "هشدارهای زمان‌بندی‌شده هنگام قفل بودن صفحه؛ صدای ممتد در صفحه آلارم پخش می‌شود"
                    enableVibration(vibrationEnabled)
                    // The alarm screen owns continuous ringtone playback while the device is locked.
                    setSound(null, null)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    configuredChannelId(CHANNEL_ALARM_HEADS_UP),
                    "هشدار آلارم هنگام استفاده از گوشی",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "هشدار صوتی و برجسته هنگام روشن و فعال بودن صفحه گوشی"
                    enableVibration(vibrationEnabled)
                    setSound(if (soundEnabled) defaultSoundUri else null, if (soundEnabled) audioAttributes else null)
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

        val preferences = AppPreferencesRepository.getInstance(context)
        val deliveryPref = preferences.alertDelivery.value
        if (!preferences.notificationsEnabled.value) {
            Log.i("NotificationDispatcher", "Global Android notifications are disabled; suppressing reminder ${reminder.id}.")
            return -1
        }
        // Delivery is strictly opt-in at BOTH levels: the reminder must explicitly
        // enable a channel, and the global preference must permit that channel.
        // A global preference alone must never create an alert for an unconfigured reminder.
        val useFullScreenAlarm = reminder.fullScreenAlarmEnabled &&
            (deliveryPref == AlertDeliveryPreference.FULL_SCREEN_ALARM ||
                deliveryPref == AlertDeliveryPreference.BOTH)
        val showStandardNotification = reminder.notificationEnabled &&
            (deliveryPref == AlertDeliveryPreference.NOTIFICATION_ONLY ||
                deliveryPref == AlertDeliveryPreference.BOTH)
        if (!useFullScreenAlarm && !showStandardNotification) {
            Log.i("NotificationDispatcher", "No opted-in Android delivery channel for reminder ${reminder.id}; suppressing notification.")
            return -1
        }
        // BOTH keeps a separate standard notification only when this reminder also uses FSI.
        val standardNotificationId = if (useFullScreenAlarm && showStandardNotification &&
            deliveryPref == AlertDeliveryPreference.BOTH) {
            NotificationIdGenerator.generateNotificationId(
                reminderId = reminder.id,
                scheduleId = "standard:${schedule?.id.orEmpty()}",
                triggerTime = triggerTime
            )
        } else -1
        val fullScreenAccessGranted = Build.VERSION.SDK_INT < 34 ||
            notificationManager.canUseFullScreenIntent()
        val isDeviceInteractive = (context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager)
            .isInteractive
        // Android normally presents a heads-up notification instead of launching an
        // activity while the user is interacting with the device. Use an audible,
        // high-importance channel in that case. Keep the lock-screen FSI channel silent
        // because ReminderAlarmActivity plays the continuous alarm ringtone itself.
        // If Darino itself is visible, launch the dedicated alarm screen directly.
        // Android restricts background activity launches, so when another app is in
        // front we keep the system-managed heads-up / full-screen notification path.
        val launchAlarmActivityDirectly = useFullScreenAlarm &&
            isDeviceInteractive && MainActivity.isAppVisible
        val useAudibleAlarmFallback = useFullScreenAlarm &&
            !launchAlarmActivityDirectly && (isDeviceInteractive || !fullScreenAccessGranted)
        val channelId = when {
            launchAlarmActivityDirectly -> configuredChannelId(CHANNEL_FULL_SCREEN_ALARMS)
            useAudibleAlarmFallback -> configuredChannelId(CHANNEL_ALARM_HEADS_UP)
            useFullScreenAlarm -> configuredChannelId(CHANNEL_FULL_SCREEN_ALARMS)
            else -> when (reminder.type.uppercase()) {
                "INSTALLMENT" -> configuredChannelId(CHANNEL_INSTALLMENTS)
                "VEHICLE", "INSURANCE", "MAINTENANCE", "FUEL" -> configuredChannelId(CHANNEL_VEHICLES)
                else -> configuredChannelId(CHANNEL_REMINDERS)
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

        // 4. Action: Snooze 5 Minutes (independent PendingIntent from the 15-minute action)
        val snoozeFiveIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SNOOZE_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(ReminderBroadcastReceiver.EXTRA_SECONDARY_NOTIFICATION_ID, standardNotificationId)
            putExtra(ReminderBroadcastReceiver.EXTRA_SNOOZE_MINUTES, 5)
        }
        val snoozeFivePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 4000,
            snoozeFiveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification body
        val amountText = reminder.amount?.takeIf { it > 0L }?.let { " • مبلغ: ${MoneyFormatter.formatToman(it)}" }.orEmpty()
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
            // Keep all three concise actions in a single system-managed action row.
            .addAction(R.drawable.img_3d_calendar, "۵ دقیقه بعد", snoozeFivePendingIntent)
            .addAction(R.drawable.img_3d_calendar, "۱۵ دقیقه بعد", snoozePendingIntent)
            .addAction(R.drawable.img_3d_installment, "انجام شد", completePendingIntent)

        val alarmActivityIntent = if (useFullScreenAlarm) {
            Intent(context, ReminderAlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ReminderAlarmActivity.EXTRA_REMINDER_ID, reminder.id)
                putExtra(ReminderAlarmActivity.EXTRA_NOTIFICATION_ID, notifId)
                putExtra(ReminderAlarmActivity.EXTRA_SECONDARY_NOTIFICATION_ID, standardNotificationId)
                putExtra(ReminderAlarmActivity.EXTRA_TITLE, reminder.title)
                putExtra(ReminderAlarmActivity.EXTRA_BODY, bodyText)
            }.also { alarmIntent ->
                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    notifId + 3000,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                if (!launchAlarmActivityDirectly) {
                    notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
                }
            }
        } else null
        val notification = notificationBuilder.build()
        val showNotification = useFullScreenAlarm || showStandardNotification

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
                        notificationManager.notify("reminder:${reminder.id}", notifId, notification)
                        notificationPosted = true
                        if (useFullScreenAlarm && showStandardNotification && deliveryPref == AlertDeliveryPreference.BOTH) {
                            val standardChannelId = when (reminder.type.uppercase()) {
                                "INSTALLMENT" -> configuredChannelId(CHANNEL_INSTALLMENTS)
                                "VEHICLE", "INSURANCE", "MAINTENANCE", "FUEL" -> configuredChannelId(CHANNEL_VEHICLES)
                                else -> configuredChannelId(CHANNEL_REMINDERS)
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
                                notificationManager.notify("reminder:${reminder.id}", standardNotificationId, standardNotification)
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

        // When Darino is already on screen, open the alarm UI immediately instead
        // of relying on a heads-up notification to act like a full-screen alarm.
        if (launchAlarmActivityDirectly && alarmActivityIntent != null) {
            try {
                context.startActivity(alarmActivityIntent)
                Log.i("NotificationDispatcher", "Launched full-screen alarm directly while Darino was visible: reminderId=${reminder.id}")
            } catch (e: Exception) {
                Log.e("NotificationDispatcher", "Could not launch alarm activity directly; keeping notification fallback.", e)
            }
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
        // Keep the lock-screen channel silent: the alarm activity plays its continuous ringtone.
        const val CHANNEL_FULL_SCREEN_ALARMS = "channel_full_screen_alarms_v2"
        // Separate audible channel for heads-up delivery while the device is interactive.
        const val CHANNEL_ALARM_HEADS_UP = "channel_alarm_heads_up_v1"
    }
}
