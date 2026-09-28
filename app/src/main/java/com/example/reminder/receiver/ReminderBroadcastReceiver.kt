package com.example.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.reminder.data.ReminderDeliveryLogEntity
import com.example.reminder.domain.DispatchDecision
import com.example.reminder.domain.MockSmsDispatcher
import com.example.reminder.domain.NotificationDispatcher
import com.example.reminder.domain.QuietHoursConfig
import com.example.reminder.domain.RecurrenceEngine
import com.example.reminder.domain.ReminderPolicyEngine
import com.example.reminder.domain.ReminderScheduler
import com.example.reminder.domain.RepeatType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i("ReminderReceiver", "Broadcast received with action: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.i("ReminderReceiver", "Device boot / timezone change detected. Rebuilding all schedules.")
                scope.launch {
                    val scheduler = ReminderScheduler(context)
                    scheduler.rebuildAllSchedules()
                }
            }

            ACTION_TRIGGER_REMINDER -> {
                val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
                val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID)
                handleTrigger(context, reminderId, scheduleId)
            }

            ACTION_COMPLETE_REMINDER -> {
                val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
                val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                handleComplete(context, reminderId, notifId)
            }

            ACTION_SNOOZE_REMINDER -> {
                val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
                val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 15)
                handleSnooze(context, reminderId, notifId, snoozeMinutes)
            }
        }
    }

    private fun handleTrigger(context: Context, reminderId: String, scheduleId: String?) {
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val reminder = db.smartReminderDao().getReminderById(reminderId)
                if (reminder == null) {
                    Log.w("ReminderReceiver", "Reminder $reminderId not found in database.")
                    return@launch
                }

                if (!reminder.status.equals("ACTIVE", ignoreCase = true)) {
                    Log.i("ReminderReceiver", "Reminder $reminderId is not ACTIVE (${reminder.status}). Skipping.")
                    return@launch
                }

                val schedule = if (scheduleId != null) {
                    db.smartReminderDao().getScheduleById(scheduleId)
                } else null

                // 1. Check Policy & Quiet Hours
                val quietHours = QuietHoursConfig(
                    enabled = false, // Configurable from settings
                    startHour = 23,
                    startMinute = 0,
                    endHour = 7,
                    endMinute = 0,
                    allowHighPriority = true
                )

                val decision = ReminderPolicyEngine.evaluateDispatch(reminder, quietHours)
                when (decision) {
                    is DispatchDecision.DeliverNow -> {
                        // Deliver Android Notification
                        if (reminder.notificationEnabled) {
                            val notifDispatcher = NotificationDispatcher(context)
                            notifDispatcher.dispatchNotification(reminder, schedule)
                        }

                        // Deliver Mock SMS
                        if (reminder.smsEnabled && !reminder.phoneNumber.isNullOrBlank()) {
                            val smsDispatcher = MockSmsDispatcher()
                            val smsResult = smsDispatcher.sendSms(
                                phoneNumber = reminder.phoneNumber,
                                message = reminder.title + "\n" + reminder.description,
                                reminderType = reminder.type
                            )
                            db.smartReminderDao().insertDeliveryLog(
                                ReminderDeliveryLogEntity(
                                    reminderId = reminder.id,
                                    scheduleId = scheduleId,
                                    channel = "SMS",
                                    scheduledAt = schedule?.triggerDateTime ?: System.currentTimeMillis(),
                                    triggeredAt = System.currentTimeMillis(),
                                    status = if (smsResult.success) "SENT" else "FAILED",
                                    errorMessage = smsResult.errorReason,
                                    providerMessageId = smsResult.providerMessageId
                                )
                            )
                        }

                        // Handle Recurring schedules if any
                        if (schedule != null && !schedule.repeatType.equals("NONE", ignoreCase = true)) {
                            val repeatType = RepeatType.fromKey(schedule.repeatType)
                            val nextTrigger = RecurrenceEngine.calculateNextOccurrence(
                                schedule.triggerDateTime,
                                repeatType,
                                schedule.repeatInterval
                            )
                            val nextSchedule = RecurrenceEngine.generateNextSchedule(schedule, nextTrigger)
                            db.smartReminderDao().insertSchedule(nextSchedule)
                            val scheduler = ReminderScheduler(context)
                            scheduler.scheduleSingle(reminder, nextSchedule)
                            Log.i("ReminderReceiver", "Scheduled next recurring occurrence for ${reminder.title} at $nextTrigger")
                        }
                    }

                    is DispatchDecision.PostponeTo -> {
                        Log.i("ReminderReceiver", "Quiet hours active. Postponing reminder ${reminder.title} to ${decision.postponeTimeMillis}")
                        val postponedSchedule = schedule?.copy(triggerDateTime = decision.postponeTimeMillis)
                        if (postponedSchedule != null) {
                            val scheduler = ReminderScheduler(context)
                            scheduler.scheduleSingle(reminder, postponedSchedule)
                        }
                    }

                    is DispatchDecision.SuppressQuietHours -> {
                        Log.i("ReminderReceiver", "Reminder ${reminder.title} suppressed due to quiet hours.")
                    }
                }

            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error processing reminder trigger", e)
            }
        }
    }

    private fun handleComplete(context: Context, reminderId: String, notifId: Int) {
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.smartReminderDao().updateStatus(
                    id = reminderId,
                    status = "COMPLETED",
                    updatedAt = System.currentTimeMillis(),
                    completedAt = System.currentTimeMillis()
                )
                if (notifId != -1) {
                    val notifDispatcher = NotificationDispatcher(context)
                    notifDispatcher.cancelNotification(notifId)
                }
                Log.i("ReminderReceiver", "Reminder $reminderId marked as COMPLETED from notification action.")
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error completing reminder $reminderId", e)
            }
        }
    }

    private fun handleSnooze(context: Context, reminderId: String, notifId: Int, snoozeMinutes: Int) {
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val reminder = db.smartReminderDao().getReminderById(reminderId) ?: return@launch
                val scheduler = ReminderScheduler(context)
                val snoozeSchedule = scheduler.snooze(reminder, snoozeMinutes)
                db.smartReminderDao().insertSchedule(snoozeSchedule)

                if (notifId != -1) {
                    val notifDispatcher = NotificationDispatcher(context)
                    notifDispatcher.cancelNotification(notifId)
                }
                Log.i("ReminderReceiver", "Reminder $reminderId snoozed for $snoozeMinutes minutes.")
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error snoozing reminder $reminderId", e)
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.reminder.ACTION_TRIGGER_REMINDER"
        const val ACTION_COMPLETE_REMINDER = "com.example.reminder.ACTION_COMPLETE_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.example.reminder.ACTION_SNOOZE_REMINDER"

        const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
        const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        const val EXTRA_SNOOZE_MINUTES = "EXTRA_SNOOZE_MINUTES"
    }
}
