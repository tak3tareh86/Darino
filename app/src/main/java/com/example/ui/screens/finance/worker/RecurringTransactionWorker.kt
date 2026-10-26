package com.example.ui.screens.finance.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.ui.screens.finance.data.LocalFinanceRepository
import java.util.concurrent.TimeUnit

class RecurringTransactionWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "RecurringTransactionWorker execution started...")
        return try {
            val repository = LocalFinanceRepository.instance
            repository.processDueRecurringTransactions(applicationContext)
            Log.i(TAG, "RecurringTransactionWorker executed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in RecurringTransactionWorker execution", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "RecurringTxWorker"
        private const val PERIODIC_WORK_NAME = "darino_recurring_tx_periodic_work"
        private const val ONETIME_WORK_NAME = "darino_recurring_tx_onetime_work"

        fun enqueuePeriodic(context: Context) {
            try {
                val periodicRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(12, TimeUnit.HOURS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
                Log.i(TAG, "Enqueued periodic recurring transaction worker (12h interval)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enqueue periodic WorkManager task: ${e.message}")
            }
        }

        fun enqueueOneTime(context: Context) {
            try {
                val oneTimeRequest = OneTimeWorkRequestBuilder<RecurringTransactionWorker>()
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    ONETIME_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    oneTimeRequest
                )
                Log.i(TAG, "Enqueued one-time recurring transaction worker")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enqueue one-time WorkManager task: ${e.message}")
            }
        }
    }
}
