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
    private val reminderDao = database.reminderDao()
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
            val pendingReminders = reminderDao.getPendingSyncReminders()
            Log.i(TAG, "Found ${pendingReminders.size} local reminders pending sync")

            // 1. Process PENDING_DELETE
            for (reminder in pendingReminders.filter { it.syncState == "PENDING_DELETE" }) {
                if (reminder.serverId != null) {
                    try {
                        val response = ApiClient.reminderApi.deleteReminder(reminder.serverId)
                        if (response.isSuccessful) {
                            reminderDao.hardDeleteReminder(reminder.id)
                            reminderScheduler.cancel(reminder.id)
                            Log.i(TAG, "Successfully deleted remote reminder: ${reminder.serverId}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete remote reminder ${reminder.serverId}: ${e.message}")
                    }
                } else {
                    reminderDao.hardDeleteReminder(reminder.id)
                    reminderScheduler.cancel(reminder.id)
                }
            }

            // 2. Process PENDING_INSERT
            for (reminder in pendingReminders.filter { it.syncState == "PENDING_INSERT" }) {
                try {
                    val idempotencyKey = "client_rem_${reminder.id}_${reminder.createdAt}"
                    val request = NetworkCreateReminderRequest(
                        type = reminder.type,
                        title = reminder.title,
                        description = reminder.description,
                        sourceType = reminder.sourceType,
                        sourceId = reminder.sourceId?.toString(),
                        dueAt = isoDateFormat.format(Date(reminder.scheduledDateTime)),
                        timezone = reminder.timezone,
                        repeatRule = reminder.repeatRule ?: "NONE",
                        notificationEnabled = reminder.notificationEnabled,
                        smsEnabled = reminder.smsEnabled,
                        phoneNumber = reminder.phoneNumber
                    )
                    val response = ApiClient.reminderApi.createReminder(idempotencyKey, request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val serverDto = response.body()?.data
                        if (serverDto != null) {
                            reminderDao.updateSyncStatus(
                                id = reminder.id,
                                serverId = serverDto.id,
                                syncState = "SYNCED",
                                lastSyncedAt = System.currentTimeMillis()
                            )
                            Log.i(TAG, "Successfully pushed local reminder #${reminder.id} -> Server #${serverDto.id}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to push reminder #${reminder.id} to server: ${e.message}")
                }
            }

            // 3. Process PENDING_UPDATE
            for (reminder in pendingReminders.filter { it.syncState == "PENDING_UPDATE" }) {
                if (reminder.serverId != null) {
                    try {
                        val updateReq = NetworkUpdateReminderRequest(
                            type = reminder.type,
                            title = reminder.title,
                            description = reminder.description,
                            dueAt = isoDateFormat.format(Date(reminder.scheduledDateTime)),
                            status = if (reminder.completedAt != null) "COMPLETED" else if (reminder.enabled) "ACTIVE" else "DISABLED",
                            repeatRule = reminder.repeatRule,
                            notificationEnabled = reminder.notificationEnabled,
                            smsEnabled = reminder.smsEnabled,
                            phoneNumber = reminder.phoneNumber
                        )
                        val response = ApiClient.reminderApi.updateReminder(reminder.serverId, updateReq)
                        if (response.isSuccessful && response.body()?.success == true) {
                            reminderDao.updateSyncStatus(
                                id = reminder.id,
                                serverId = reminder.serverId,
                                syncState = "SYNCED",
                                lastSyncedAt = System.currentTimeMillis()
                            )
                            Log.i(TAG, "Successfully updated remote reminder: ${reminder.serverId}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to update reminder ${reminder.serverId}: ${e.message}")
                    }
                }
            }

            // 4. Fetch Remote Reminders and Merge
            val listResponse = ApiClient.reminderApi.listReminders()
            if (listResponse.isSuccessful && listResponse.body()?.success == true) {
                val serverList = listResponse.body()?.data ?: emptyList()
                Log.i(TAG, "Fetched ${serverList.size} reminders from backend")

                for (serverItem in serverList) {
                    val local = reminderDao.getReminderByServerId(serverItem.id)
                    val scheduledMs = try {
                        isoDateFormat.parse(serverItem.dueAt)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    if (local == null) {
                        // Insert new reminder into Room
                        val newEntity = ReminderEntity(
                            id = 0,
                            serverId = serverItem.id,
                            syncState = "SYNCED",
                            userId = SessionManager.userId ?: "1",
                            type = serverItem.type,
                            title = serverItem.title,
                            description = serverItem.description ?: "",
                            scheduledDateTime = scheduledMs,
                            timezone = serverItem.timezone,
                            repeatRule = serverItem.repeatRule,
                            enabled = serverItem.status == "ACTIVE",
                            notificationEnabled = serverItem.notificationEnabled,
                            smsEnabled = serverItem.smsEnabled,
                            phoneNumber = serverItem.phoneNumber,
                            phoneNumberMasked = serverItem.phoneNumberMasked,
                            smsDeliveryStatus = serverItem.smsDeliveryStatus,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            completedAt = if (serverItem.status == "COMPLETED") System.currentTimeMillis() else null,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                        val newId = reminderDao.insertReminder(newEntity).toInt()
                        if (newEntity.enabled && newEntity.scheduledDateTime > System.currentTimeMillis()) {
                            reminderScheduler.schedule(newEntity.copy(id = newId))
                        }
                    } else if (local.syncState == "SYNCED") {
                        // Safe to update with latest remote state
                        val updated = local.copy(
                            type = serverItem.type,
                            title = serverItem.title,
                            description = serverItem.description ?: local.description,
                            scheduledDateTime = scheduledMs,
                            enabled = serverItem.status == "ACTIVE",
                            notificationEnabled = serverItem.notificationEnabled,
                            smsEnabled = serverItem.smsEnabled,
                            phoneNumber = serverItem.phoneNumber,
                            phoneNumberMasked = serverItem.phoneNumberMasked,
                            smsDeliveryStatus = serverItem.smsDeliveryStatus ?: local.smsDeliveryStatus,
                            completedAt = if (serverItem.status == "COMPLETED") local.completedAt ?: System.currentTimeMillis() else null,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                        reminderDao.updateReminder(updated)
                        if (updated.enabled && updated.scheduledDateTime > System.currentTimeMillis()) {
                            reminderScheduler.schedule(updated)
                        } else {
                            reminderScheduler.cancel(updated.id)
                        }
                    }
                }

                // 5. Query SMS Status for active SMS reminders
                syncSmsStatuses()

                SyncStatus.SUCCESS
            } else {
                Log.w(TAG, "Failed to list reminders: HTTP ${listResponse.code()}")
                SyncStatus.OFFLINE
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network exception during reminder sync: ${e.message}")
            SyncStatus.OFFLINE
        }
    }

    private suspend fun syncSmsStatuses() {
        val activeSmsReminders = reminderDao.getActiveReminders()
        // Check SMS delivery status for reminders with serverId & smsEnabled
        for (rem in activeRemindersList()) {
            if (rem.smsEnabled && rem.serverId != null && rem.smsDeliveryStatus != "DELIVERED") {
                try {
                    val statusRes = ApiClient.reminderApi.getReminderSmsStatus(rem.serverId)
                    if (statusRes.isSuccessful && statusRes.body()?.success == true) {
                        val smsData = statusRes.body()?.data
                        if (smsData != null) {
                            reminderDao.updateSmsStatus(rem.id, smsData.status)

                            // Also save or update SMS log
                            val existingLog = smsLogDao.getLatestLogForReminder(rem.id)
                            if (existingLog == null) {
                                smsLogDao.insertSmsLog(
                                    SmsLogEntity(
                                        reminderId = rem.id,
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
    }

    private suspend fun activeRemindersList(): List<ReminderEntity> {
        return reminderDao.getPendingSyncReminders() + (reminderDao.getActiveRemindersSnapshot())
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
