package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.api.SyncManager
import com.example.data.api.SyncStatus
import com.example.data.database.AppDatabase
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "SyncWorker started in background...")
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val syncManager = SyncManager(applicationContext, database)
            val status = syncManager.syncAll()
            Log.i(TAG, "SyncWorker completed with status: $status")
            if (status == SyncStatus.SUCCESS || status == SyncStatus.IDLE) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker error", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val UNIQUE_PERIODIC_WORK_NAME = "finance_periodic_sync_work"
        private const val UNIQUE_ONE_TIME_WORK_NAME = "finance_onetime_sync_work"

        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Log.i(TAG, "Enqueued periodic sync worker (15 min interval)")
        }

        fun enqueueOneTime(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.i(TAG, "Enqueued one-time sync worker")
        }
    }
}
