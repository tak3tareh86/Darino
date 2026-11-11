package com.example.data.backup

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.calendar.domain.CalendarDateUtils
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
        val isEnabled = prefs.getBoolean(preferenceKey, false)

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

            val jsonString = BackupFileCodec.encode(backup)
            val jsonPayload = jsonString.toByteArray(Charsets.UTF_8)
            require(jsonPayload.size.toLong() <= MAX_BACKUP_JSON_BYTES) {
                "حجم داده‌های پشتیبان خودکار از حد مجاز بیشتر است."
            }
            val encryptedPayload = DeviceBackupEncryption.encrypt(jsonPayload, applicationContext)
            require(encryptedPayload.size.toLong() <= MAX_LOCAL_BACKUP_FILE_BYTES) {
                "حجم فایل پشتیبان خودکار رمزگذاری‌شده از حد مجاز بیشتر است."
            }

            val userTag = BackupUserScope.tag(userId)
            val (y, m, d) = CalendarDateUtils.getCurrentJalaliDate()
            val timestamp = java.text.SimpleDateFormat("HHmmss", java.util.Locale.ROOT)
                .format(java.util.Date())
            val retentionPrefix = "${frequency}_auto_backup_${userTag}_"
            val filename = "${retentionPrefix}${y}_${m.toString().padStart(2, '0')}_${d.toString().padStart(2, '0')}_$timestamp.darino"

            // All user-visible backup types share the folder explicitly selected in settings.
            // A missing/revoked folder is an error; never silently write to private app storage.
            val keepCount = if (frequency == FREQUENCY_DAILY) DAILY_RETENTION_COUNT else WEEKLY_RETENTION_COUNT
            val savedUri = BackupFolderStorage.saveBackup(
                applicationContext,
                filename,
                encryptedPayload,
                retentionPrefix,
                keepCount
            )

            Log.i(TAG, "$frequency auto-backup file created successfully: $savedUri")

            // Update last backup time
            prefs.edit()
                .putLong("last_backup_time", System.currentTimeMillis())
                .putLong("last_backup_time_${userTag}", System.currentTimeMillis())
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
        private const val MAX_BACKUP_JSON_BYTES = 64L * 1024 * 1024
        // Keep device-encrypted automatic backups within the same ceiling used by local restore.
        private const val MAX_LOCAL_BACKUP_FILE_BYTES = MAX_BACKUP_JSON_BYTES + 1024L
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
