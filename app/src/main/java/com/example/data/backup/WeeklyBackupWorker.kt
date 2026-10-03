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
        val isEnabled = prefs.getBoolean("auto_backup_weekly", true)

        if (!isEnabled) {
            Log.i(TAG, "Weekly auto-backup is disabled in settings. Skipping.")
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
            val filename = "auto_backup_${y}_${m.toString().padStart(2, '0')}_${d.toString().padStart(2, '0')}.json"

            val autoBackupsDir = File(applicationContext.filesDir, "auto_backups")
            if (!autoBackupsDir.exists()) {
                autoBackupsDir.mkdirs()
            }

            val backupFile = File(autoBackupsDir, filename)
            backupFile.writeText(jsonString, Charsets.UTF_8)

            Log.i(TAG, "Weekly auto-backup file created successfully: ${backupFile.absolutePath}")

            // Update last backup time
            prefs.edit()
                .putLong("last_backup_time", System.currentTimeMillis())
                .apply()

            Result.success()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Weekly auto-backup skipped: ${e.message}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing weekly auto-backup", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WeeklyBackupWorker"
        const val UNIQUE_WORK_NAME = "weekly_auto_backup_work"

        fun enqueueWeekly(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
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
    }
}
