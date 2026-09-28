package com.example.data.receiver

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
import com.example.data.database.NotificationLogEntity
import com.example.data.database.ReminderEntity
import com.example.data.database.SmsLogEntity
import com.example.data.sms.BackendSecureSmsAdapter
import com.example.data.sms.SmsDeliveryStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i("ReminderReceiver", "Received broadcast with action: $action")

        if (Intent.ACTION_BOOT_COMPLETED == action || "android.intent.action.QUICKBOOT_POWERON" == action) {
            // Handle Device Reboot Rescheduling
            Log.i("ReminderReceiver", "Device rebooted. Rescheduling all active reminders.")
            rescheduleAllActiveReminders(context)
        } else if (ACTION_TRIGGER_REMINDER == action) {
            val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
            Log.i("ReminderReceiver", "Triggering alarm for reminder ID: $reminderId")
            if (reminderId != -1) {
                dispatchReminder(context, reminderId)
            }
        }
    }

    private fun rescheduleAllActiveReminders(context: Context) {
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val activeReminders = db.reminderDao().getActiveReminders().first()
                val scheduler = ReminderScheduler(context)
                
                activeReminders.forEach { reminder ->
                    if (reminder.scheduledDateTime > System.currentTimeMillis()) {
                        scheduler.schedule(reminder)
                        Log.i("ReminderReceiver", "Rescheduled reminder: ${reminder.title} at ${reminder.scheduledDateTime}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Failed to reschedule reminders after reboot", e)
            }
        }
    }

    private fun dispatchReminder(context: Context, reminderId: Int) {
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val reminder = db.reminderDao().getReminderById(reminderId)
                if (reminder == null) {
                    Log.w("ReminderReceiver", "Reminder with ID $reminderId not found in database")
                    return@launch
                }

                if (!reminder.enabled) {
                    Log.i("ReminderReceiver", "Reminder $reminderId is disabled. Skipping trigger.")
                    return@launch
                }

                // 1. Android Notification Channel & Display
                if (reminder.notificationEnabled) {
                    showAndroidNotification(context, reminder)
                }

                // 2. SMS Dispatch (Secure Client-Server architecture)
                if (reminder.smsEnabled && !reminder.phoneNumber.isNullOrBlank()) {
                    dispatchSms(db, reminder)
                }

                // Mark schedules related to this as SENT/DELIVERED
                val schedules = db.reminderDao().getSchedulesForReminder(reminderId)
                schedules.forEach { sched ->
                    db.reminderDao().updateScheduleStatus(
                        sched.id,
                        status = "SENT",
                        notifStatus = if (reminder.notificationEnabled) "SENT" else "DISABLED",
                        smsStatus = if (reminder.smsEnabled) "SENT" else "DISABLED"
                    )
                }

            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error dispatching reminder ID: $reminderId", e)
            }
        }
    }

    private fun showAndroidNotification(context: Context, reminder: ReminderEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Define channels based on reminder category
        val channelId = when (reminder.type) {
            "INSTALLMENT" -> CHANNEL_INSTALLMENTS
            "VEHICLE", "INSURANCE", "MAINTENANCE", "FUEL" -> CHANNEL_VEHICLES
            else -> CHANNEL_GENERAL
        }

        val channelName = when (reminder.type) {
            "INSTALLMENT" -> "اقساط و سررسیدها"
            "VEHICLE", "INSURANCE", "MAINTENANCE", "FUEL" -> "خودرو و یادآوریهای سرویس"
            else -> "یادآورهای عمومی"
        }

        // Create Channel on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "کانال اعلانهای مربوط به $channelName"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Target Action Deep Link into specific sections
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("DEEP_LINK_TARGET", reminder.type)
            putExtra("DEEP_LINK_ID", reminder.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.id,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Generate dynamic notification message
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.img_3d_bell_notification) // Placeholder or system icon
            .setContentTitle(reminder.title)
            .setContentText(reminder.description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(reminder.id, notificationBuilder.build())
        Log.i("ReminderReceiver", "Displayed local notification for: ${reminder.title}")

        // Log to NotificationCenter database
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.notificationLogDao().insertNotification(
                    NotificationLogEntity(
                        reminderId = reminder.id,
                        title = reminder.title,
                        message = reminder.description,
                        type = reminder.type,
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        source = "SYSTEM_ALARM",
                        status = "DELIVERED"
                    )
                )
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Failed to log notification to database", e)
            }
        }
    }

    private suspend fun dispatchSms(db: AppDatabase, reminder: ReminderEntity) {
        val phoneNumber = reminder.phoneNumber ?: return
        val smsAdapter = BackendSecureSmsAdapter()

        // Log SMS initially as QUEUED
        val smsLogId = db.smsLogDao().insertSmsLog(
            SmsLogEntity(
                reminderId = reminder.id,
                providerMessageId = null,
                phoneNumber = phoneNumber,
                message = reminder.description,
                status = "QUEUED",
                sentAt = null,
                deliveredAt = null,
                failedAt = null,
                failureReason = null,
                retryCount = 0
            )
        ).toInt()

        // Process request to the secure backend SMS gateway
        val result = smsAdapter.sendSms(phoneNumber, reminder.description, reminder.type)
        
        if (result.success) {
            db.smsLogDao().updateSmsLog(
                id = smsLogId,
                status = "SENT",
                providerId = result.providerMessageId,
                sentAt = System.currentTimeMillis(),
                deliveredAt = System.currentTimeMillis(), // Simulating success delivery callback
                failedAt = null,
                reason = null,
                retryCount = 1
            )
            Log.i("ReminderReceiver", "SMS dispatched successfully via secure API to $phoneNumber")
        } else {
            db.smsLogDao().updateSmsLog(
                id = smsLogId,
                status = "FAILED",
                providerId = null,
                sentAt = null,
                deliveredAt = null,
                failedAt = System.currentTimeMillis(),
                reason = result.errorReason ?: "API_ERROR",
                retryCount = 1
            )
            Log.w("ReminderReceiver", "SMS dispatch failed: ${result.errorReason}")
        }
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.action.TRIGGER_REMINDER"
        const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"

        // Channels
        const val CHANNEL_GENERAL = "channel_general_reminders"
        const val CHANNEL_INSTALLMENTS = "channel_installments"
        const val CHANNEL_VEHICLES = "channel_vehicles"
    }
}
