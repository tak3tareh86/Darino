package com.example.ui.screens.finance.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.security.SessionManager

/**
 * Re-displays a still-pending occurrence after the user asks to be reminded tomorrow.
 */
class RecurringTransactionReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
        val occurrenceId = inputData.getString(KEY_OCCURRENCE_ID) ?: return Result.failure()

        return try {
            SessionManager.init(applicationContext)
            if (SessionManager.userId != userId) return Result.success()

            if (RecurringTransactionNotificationManager.hasPending(applicationContext, userId, occurrenceId)) {
                RecurringTransactionNotificationManager.showPending(applicationContext, userId, occurrenceId)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to re-notify pending recurring transaction $occurrenceId", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "RecurringTxReminder"
        const val KEY_USER_ID = "recurring_reminder_user_id"
        const val KEY_OCCURRENCE_ID = "recurring_reminder_occurrence_id"
    }
}
