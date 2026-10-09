package com.example.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                goAsyncWork {
                    if (SessionManager.userId == null) {
                        Log.i(TAG, "No authenticated user; skipping reminder rebuild.")
                        return@goAsyncWork
                    }
                    ReminderScheduler(context.applicationContext).rebuildAllSchedules()
                }
            }

            ACTION_TRIGGER_REMINDER -> {
                val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
                val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: return
                goAsyncWork { handleTrigger(context.applicationContext, reminderId, scheduleId) }
            }

            ACTION_COMPLETE_REMINDER -> {
                val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                goAsyncWork { handleComplete(context.applicationContext, reminderId, notificationId) }
            }

            ACTION_SNOOZE_REMINDER -> {
                val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 15).coerceIn(1, 24 * 60)
                goAsyncWork { handleSnooze(context.applicationContext, reminderId, notificationId, snoozeMinutes) }
            }
        }
    }

    private fun goAsyncWork(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, "Reminder broadcast processing failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleTrigger(context: Context, reminderId: String, scheduleId: String) {
        val userId = SessionManager.userId ?: return
        val db = AppDatabase.getDatabase(context)
        val dao = db.smartReminderDao()

        val reminder = dao.getReminderById(userId, reminderId) ?: run {
            Log.w(TAG, "Reminder $reminderId not found for current user.")
            return
        }
        if (!reminder.status.equals("ACTIVE", ignoreCase = true)) return

        val schedule = dao.getScheduleById(userId, scheduleId) ?: run {
            Log.w(TAG, "Schedule $scheduleId not found for current user.")
            return
        }
        if (schedule.reminderId != reminder.id || !schedule.enabled) return

        val decision = ReminderPolicyEngine.evaluateDispatch(
            reminder,
            QuietHoursConfig(enabled = false)
        )

        when (decision) {
            DispatchDecision.DeliverNow -> {
                if (reminder.notificationEnabled) {
                    NotificationDispatcher(context).dispatchNotification(reminder, schedule)
                }

                if (reminder.smsEnabled && !reminder.phoneNumber.isNullOrBlank()) {
                    val result = MockSmsDispatcher().sendSms(
                        phoneNumber = reminder.phoneNumber,
                        message = buildString {
                            append(reminder.title)
                            if (reminder.description.isNotBlank()) {
                                append("\n")
                                append(reminder.description)
                            }
                        },
                        reminderType = reminder.type
                    )

                    dao.insertDeliveryLog(
                        com.example.reminder.data.ReminderDeliveryLogEntity(
                            reminderId = reminder.id,
                            scheduleId = schedule.id,
                            channel = "SMS",
                            scheduledAt = schedule.triggerDateTime,
                            triggeredAt = System.currentTimeMillis(),
                            status = if (result.success) "SENT" else "FAILED",
                            errorMessage = result.errorReason,
                            providerMessageId = result.providerMessageId
                        )
                    )
                }

                val repeatType = RepeatType.fromKey(schedule.repeatType)
                if (repeatType != RepeatType.NONE) {
                    val nextTrigger = RecurrenceEngine.calculateNextOccurrence(
                        schedule.triggerDateTime,
                        repeatType,
                        schedule.repeatInterval
                    )
                    if (nextTrigger > System.currentTimeMillis()) {
                        val nextSchedule = RecurrenceEngine.generateNextSchedule(schedule, nextTrigger)
                        dao.insertSchedule(nextSchedule)
                        ReminderScheduler(context).scheduleSingle(reminder, nextSchedule)
                    }
                }
            }

            is DispatchDecision.PostponeTo -> {
                val postponed = schedule.copy(triggerDateTime = decision.postponeTimeMillis)
                dao.updateSchedule(postponed)
                ReminderScheduler(context).scheduleSingle(reminder, postponed)
            }

            DispatchDecision.SuppressQuietHours -> Unit
        }
    }

    private suspend fun handleComplete(context: Context, reminderId: String, notificationId: Int) {
        val userId = SessionManager.userId ?: return
        val now = System.currentTimeMillis()
        AppDatabase.getDatabase(context).smartReminderDao().updateStatus(
            userId = userId,
            id = reminderId,
            status = "COMPLETED",
            updatedAt = now,
            completedAt = now,
            cancelledAt = null
        )
        if (notificationId != -1) NotificationDispatcher(context).cancelNotification(notificationId)
    }

    private suspend fun handleSnooze(
        context: Context,
        reminderId: String,
        notificationId: Int,
        snoozeMinutes: Int
    ) {
        val userId = SessionManager.userId ?: return
        val db = AppDatabase.getDatabase(context)
        val dao = db.smartReminderDao()
        val reminder = dao.getReminderById(userId, reminderId) ?: return
        if (!reminder.status.equals("ACTIVE", ignoreCase = true)) return

        val triggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val schedule = com.example.reminder.data.ReminderScheduleEntity(
            id = "snooze_${java.util.UUID.randomUUID()}",
            reminderId = reminder.id,
            triggerType = "EXACT",
            offsetValue = snoozeMinutes,
            offsetUnit = "MINUTE",
            triggerDateTime = triggerTime,
            repeatType = "NONE",
            enabled = true,
            createdAt = System.currentTimeMillis()
        )
        // 1. Insert into database FIRST so it is guaranteed available when alarm triggers
        dao.insertSchedule(schedule)
        // 2. Schedule alarm SECOND
        ReminderScheduler(context).scheduleSingle(reminder, schedule)
        if (notificationId != -1) NotificationDispatcher(context).cancelNotification(notificationId)
    }

    companion object {
        private const val TAG = "ReminderReceiver"

        const val ACTION_TRIGGER_REMINDER = "com.example.reminder.ACTION_TRIGGER_REMINDER"
        const val ACTION_COMPLETE_REMINDER = "com.example.reminder.ACTION_COMPLETE_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.example.reminder.ACTION_SNOOZE_REMINDER"

        const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
        const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        const val EXTRA_SNOOZE_MINUTES = "EXTRA_SNOOZE_MINUTES"
    }
}
