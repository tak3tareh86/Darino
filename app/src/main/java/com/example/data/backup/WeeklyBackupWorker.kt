package com.example.data.backup

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.calendar.domain.CalendarDateUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class WeeklyBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "WeeklyBackupWorker started...")
        val prefs = applicationContext.getSharedPreferences("darino_backup_prefs", Context.MODE_PRIVATE)
        val frequency = inputData.getString(KEY_FREQUENCY) ?: FREQUENCY_WEEKLY
        val preferenceKey = if (frequency == FREQUENCY_DAILY) "auto_backup_daily" else "auto_backup_weekly"
        val isEnabled = prefs.getBoolean(preferenceKey, frequency == FREQUENCY_WEEKLY)

        if (!isEnabled) {
            Log.i(TAG, "$frequency auto-backup is disabled in settings. Skipping.")
            return Result.success()
        }

        val userId = com.example.data.security.SessionManager.userId
            ?: com.example.data.security.AuthSessionManager(applicationContext).getActiveSession()?.userId

        if (userId.isNullOrBlank()) {
            Log.i(TAG, "Weekly auto-backup skipped: no authenticated user session.")
            return Result.success()
        }

        return try {
            val backupRepo = LocalBackupRepository(applicationContext)
            val backup = backupRepo.createBackup()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(DarinoBackup::class.java)
            val jsonString = adapter.toJson(backup)

            val (y, m, d) = CalendarDateUtils.getCurrentJalaliDate()
            val filename = "${frequency}_auto_backup_${y}_${m.toString().padStart(2, '0')}_${d.toString().padStart(2, '0')}.json"

            val autoBackupsDir = File(applicationContext.filesDir, "auto_backups")
            if (!autoBackupsDir.exists()) {
                autoBackupsDir.mkdirs()
            }

            val backupFile = File(autoBackupsDir, filename)
            backupFile.writeText(jsonString, Charsets.UTF_8)

            // Keep bounded local history so daily backups cannot grow without limit.
            val keepCount = if (frequency == FREQUENCY_DAILY) DAILY_RETENTION_COUNT else WEEKLY_RETENTION_COUNT
            autoBackupsDir.listFiles { file ->
                file.isFile && file.name.startsWith("${frequency}_auto_backup_") && file.extension == "json"
            }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(keepCount)
                ?.forEach { oldFile ->
                    if (!oldFile.delete()) {
                        Log.w(TAG, "Could not delete expired automatic backup: ${oldFile.name}")
                    }
                }

            Log.i(TAG, "$frequency auto-backup file created successfully: ${backupFile.absolutePath}")

            // Update last backup time
            prefs.edit()
                .putLong("last_backup_time", System.currentTimeMillis())
                .apply()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing $frequency auto-backup", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WeeklyBackupWorker"
        private const val KEY_FREQUENCY = "backup_frequency"
        private const val FREQUENCY_WEEKLY = "weekly"
        private const val FREQUENCY_DAILY = "daily"
        private const val DAILY_RETENTION_COUNT = 30
        private const val WEEKLY_RETENTION_COUNT = 12
        const val UNIQUE_WORK_NAME = "weekly_auto_backup_work"
        const val DAILY_UNIQUE_WORK_NAME = "daily_auto_backup_work"

        fun enqueueWeekly(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            // Run every 7 days
            val request = PeriodicWorkRequestBuilder<WeeklyBackupWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "Enqueued weekly auto-backup WorkManager request")
        }

        fun cancelWeekly(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            Log.i(TAG, "Cancelled weekly auto-backup WorkManager request")
        }

        fun enqueueDaily(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<WeeklyBackupWorker>(1, TimeUnit.DAYS)
                .setInputData(workDataOf(KEY_FREQUENCY to FREQUENCY_DAILY))
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "Enqueued daily auto-backup WorkManager request")
        }

        fun cancelDaily(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(DAILY_UNIQUE_WORK_NAME)
            Log.i(TAG, "Cancelled daily auto-backup WorkManager request")
        }
    }
}
