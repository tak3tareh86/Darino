package com.example.ui.screens.finance.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.security.SessionManager
import com.example.ui.screens.finance.data.LocalFinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RecurringTransactionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: return
        val occurrenceId = intent.getStringExtra(EXTRA_OCCURRENCE_ID) ?: return
        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                SessionManager.init(appContext)
                if (SessionManager.userId != userId) return@launch

                when (action) {
                    ACTION_CONFIRM -> {
                        val repository = LocalFinanceRepository.instance
                        repository.init(appContext)
                        val confirmed = repository.confirmPendingRecurringTransaction(appContext, occurrenceId)
                        if (!confirmed) {
                            // Keep the pending occurrence visible if validation or persistence failed.
                            RecurringTransactionNotificationManager.showPending(appContext, userId, occurrenceId)
                        }
                    }

                    ACTION_REMIND_LATER -> {
                        if (!RecurringTransactionNotificationManager.hasPending(appContext, userId, occurrenceId)) {
                            return@launch
                        }
                        val request = OneTimeWorkRequestBuilder<RecurringTransactionReminderWorker>()
                            .setInitialDelay(24, TimeUnit.HOURS)
                            .setInputData(
                                workDataOf(
                                    RecurringTransactionReminderWorker.KEY_USER_ID to userId,
                                    RecurringTransactionReminderWorker.KEY_OCCURRENCE_ID to occurrenceId
                                )
                            )
                            .build()
                        WorkManager.getInstance(appContext).enqueueUniqueWork(
                            "recurring_reminder_$occurrenceId",
                            ExistingWorkPolicy.REPLACE,
                            request
                        )
                        RecurringTransactionNotificationManager.cancel(appContext, occurrenceId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recurring transaction notification action failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "RecurringTxAction"
        const val ACTION_CONFIRM = "com.example.action.RECURRING_TX_CONFIRM"
        const val ACTION_REMIND_LATER = "com.example.action.RECURRING_TX_REMIND_LATER"
        const val EXTRA_USER_ID = "extra_recurring_user_id"
        const val EXTRA_OCCURRENCE_ID = "extra_recurring_occurrence_id"
    }
}
