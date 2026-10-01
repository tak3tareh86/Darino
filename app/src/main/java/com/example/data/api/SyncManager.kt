package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.NotificationLogEntity
import com.example.data.database.ReminderEntity
import com.example.data.database.SmsLogEntity
import com.example.data.receiver.ReminderScheduler
import com.example.data.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    OFFLINE,
    ERROR
}

class SyncManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val smartReminderDao = database.smartReminderDao()
    private val notificationLogDao = database.notificationLogDao()
    private val smsLogDao = database.smsLogDao()
    private val reminderScheduler = ReminderScheduler(context)

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun syncAll(): SyncStatus = withContext(Dispatchers.IO) {
        if (SessionManager.accessToken == null) {
            Log.i(TAG, "Skipping remote sync: user is not authenticated")
            return@withContext SyncStatus.IDLE
        }

        try {
            Log.i(TAG, "Starting full two-way synchronization...")
            val remStatus = syncReminders()
            val notifStatus = syncNotifications()
            syncPhoneStatus()

            if (remStatus == SyncStatus.SUCCESS || notifStatus == SyncStatus.SUCCESS) {
                SyncStatus.SUCCESS
            } else {
                remStatus
            }
        } catch (e: Exception) {
            Log.w(TAG, "Synchronization encountered error: ${e.message}")
            SyncStatus.OFFLINE
        }
    }

    suspend fun syncReminders(): SyncStatus = withContext(Dispatchers.IO) {
        if (SessionManager.accessToken == null) return@withContext SyncStatus.IDLE

        try {
            // 1. Fetch Remote Reminders and Merge into canonical smart_reminders
            val listResponse = ApiClient.reminderApi.listReminders()
            if (listResponse.isSuccessful && listResponse.body()?.success == true) {
                val serverList = listResponse.body()?.data ?: emptyList()
                Log.i(TAG, "Fetched ${serverList.size} reminders from backend into canonical smart_reminders")

                for (serverItem in serverList) {
                    val scheduledMs = try {
                        isoDateFormat.parse(serverItem.dueAt)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    val smartEntity = com.example.reminder.data.ReminderEntity(
                        id = serverItem.id,
                        title = serverItem.title,
                        description = serverItem.description ?: "",
                        type = serverItem.type,
                        sourceType = "REMOTE_SYNC",
                        sourceId = null,
                        status = if (serverItem.status == "COMPLETED") "COMPLETED" else if (serverItem.status == "DISABLED") "DISABLED" else "ACTIVE",
                        date = com.example.util.PersianCalendarHelper.fromEpochMillis(scheduledMs).toFormattedDate(),
                        time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(scheduledMs)),
                        notificationEnabled = serverItem.notificationEnabled,
                        smsEnabled = serverItem.smsEnabled,
                        phoneNumber = serverItem.phoneNumber
                    )
                    smartReminderDao.insertReminder(smartEntity)
                }

                // 2. Query SMS Status for active SMS reminders
                syncSmsStatuses()

                SyncStatus.SUCCESS
            } else {
                Log.w(TAG, "Failed to list reminders: HTTP ${listResponse.code()}")
                SyncStatus.OFFLINE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during reminder sync", e)
            SyncStatus.ERROR
        }
    }

    private suspend fun syncSmsStatuses() {
        val activeSmsReminders = smartReminderDao.getActiveRemindersList().filter { it.smsEnabled }
        for (rem in activeSmsReminders) {
            try {
                val statusRes = ApiClient.reminderApi.getReminderSmsStatus(rem.id)
                if (statusRes.isSuccessful && statusRes.body()?.success == true) {
                    val smsData = statusRes.body()?.data
                    if (smsData != null) {
                        val intId = rem.id.hashCode()
                        val existingLog = smsLogDao.getLatestLogForReminder(intId)
                        if (existingLog == null) {
                            smsLogDao.insertSmsLog(
                                SmsLogEntity(
                                    reminderId = intId,
                                    providerMessageId = smsData.providerMessageId,
                                    phoneNumber = rem.phoneNumber ?: "",
                                    phoneNumberMasked = smsData.phoneNumberMasked,
                                    message = "یادآوری: ${rem.title}",
                                    status = smsData.status,
                                    sentAt = System.currentTimeMillis(),
                                    deliveredAt = if (smsData.status == "DELIVERED") System.currentTimeMillis() else null,
                                    failedAt = if (smsData.status == "FAILED") System.currentTimeMillis() else null,
                                    failureReason = smsData.failureReason
                                )
                            )
                        } else {
                            smsLogDao.updateSmsLog(
                                id = existingLog.id,
                                status = smsData.status,
                                providerId = smsData.providerMessageId ?: existingLog.providerMessageId,
                                sentAt = existingLog.sentAt ?: System.currentTimeMillis(),
                                deliveredAt = if (smsData.status == "DELIVERED") System.currentTimeMillis() else existingLog.deliveredAt,
                                failedAt = if (smsData.status == "FAILED") System.currentTimeMillis() else existingLog.failedAt,
                                reason = smsData.failureReason,
                                retryCount = existingLog.retryCount
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore SMS status query failure
            }
        }
    }

    suspend fun syncNotifications(): SyncStatus = withContext(Dispatchers.IO) {
        if (SessionManager.accessToken == null) return@withContext SyncStatus.IDLE

        try {
            val pendingNotifs = notificationLogDao.getPendingSyncNotifications()
            for (notif in pendingNotifs) {
                if (notif.isRead && notif.serverId != null) {
                    try {
                        ApiClient.notificationApi.markAsRead(notif.serverId)
                        notificationLogDao.updateSyncStatus(notif.id, notif.serverId, "SYNCED")
                    } catch (e: Exception) {
                        // Keep pending
                    }
                }
            }

            val listResponse = ApiClient.notificationApi.listNotifications(page = 0, size = 50)
            if (listResponse.isSuccessful && listResponse.body()?.success == true) {
                val serverNotifs = listResponse.body()?.data ?: emptyList()
                for (sNotif in serverNotifs) {
                    val local = notificationLogDao.getNotificationByServerId(sNotif.id)
                    if (local == null) {
                        notificationLogDao.insertNotification(
                            NotificationLogEntity(
                                serverId = sNotif.id,
                                syncState = "SYNCED",
                                reminderId = sNotif.reminderId?.toIntOrNull(),
                                title = sNotif.title,
                                message = sNotif.message,
                                type = sNotif.type,
                                timestamp = System.currentTimeMillis(),
                                isRead = sNotif.readAt != null,
                                source = "BACKEND_SYNC",
                                status = sNotif.status
                            )
                        )
                    }
                }
                SyncStatus.SUCCESS
            } else {
                SyncStatus.OFFLINE
            }
        } catch (e: Exception) {
            SyncStatus.OFFLINE
        }
    }

    suspend fun syncPhoneStatus() = withContext(Dispatchers.IO) {
        if (SessionManager.accessToken == null) return@withContext
        try {
            val response = ApiClient.phoneApi.getStatus()
            if (response.isSuccessful && response.body()?.success == true) {
                val phoneStatus = response.body()?.data
                val currentUser = SessionManager.currentUser
                if (phoneStatus != null && currentUser != null) {
                    val updatedUser = currentUser.copy(
                        phoneNumber = phoneStatus.phoneNumber ?: currentUser.phoneNumber,
                        phoneNumberMasked = phoneStatus.phoneNumberMasked ?: currentUser.phoneNumberMasked,
                        phoneVerified = phoneStatus.isVerified
                    )
                    if (phoneStatus.isVerified && !currentUser.phoneVerified) {
                        SessionManager.onPhoneVerified(updatedUser)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    companion object {
        private const val TAG = "SyncManager"
    }
}
