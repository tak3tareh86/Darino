package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.NotificationLogEntity
import com.example.data.database.SmsLogEntity
import com.example.data.receiver.ReminderScheduler
import com.example.data.security.SessionManager
import com.example.reminder.data.ReminderEntity
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
            Log.i(TAG, "Starting authenticated synchronization...")
            val txStatus = syncTransactions()
            val remStatus = syncReminders()
            val notifStatus = syncNotifications()
            syncPhoneStatus()

            if (txStatus == SyncStatus.SUCCESS || remStatus == SyncStatus.SUCCESS || notifStatus == SyncStatus.SUCCESS) {
                SyncStatus.SUCCESS
            } else {
                remStatus
            }
        } catch (e: Exception) {
            Log.w(TAG, "Synchronization encountered error: ${e.message}")
            SyncStatus.OFFLINE
        }
    }

    suspend fun syncTransactions(): SyncStatus = withContext(Dispatchers.IO) {
        val userId = SessionManager.userId ?: return@withContext SyncStatus.IDLE
        try {
            val dao = database.transactionDao()
            val remoteResponse = ApiClient.transactionApi.listTransactions()
            if (!remoteResponse.isSuccessful || remoteResponse.body()?.success != true) {
                return@withContext SyncStatus.OFFLINE
            }

            val remoteByClientId = remoteResponse.body()?.data.orEmpty().associateBy { it.clientId }

            fun remoteUpdatedAt(remote: NetworkTransactionDto): Long =
                remote.updatedAt?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
                    ?: runCatching { java.time.Instant.parse(remote.occurredAt).toEpochMilli() }.getOrDefault(0L)

            fun remoteTimestamp(remote: NetworkTransactionDto): Long =
                runCatching { java.time.Instant.parse(remote.occurredAt).toEpochMilli() }.getOrDefault(System.currentTimeMillis())

            suspend fun applyRemote(remote: NetworkTransactionDto, existing: TransactionEntity?) {
                val timestamp = remoteTimestamp(remote)
                val updatedAt = remoteUpdatedAt(remote)
                dao.insertTransaction(
                    TransactionEntity(
                        id = existing?.id ?: 0,
                        userId = userId,
                        amount = remote.amount,
                        type = remote.type,
                        category = remote.category,
                        accountName = remote.accountName,
                        description = remote.description,
                        timestamp = timestamp,
                        timeFormatted = remote.timeFormatted,
                        stringId = remote.clientId,
                        title = remote.title,
                        subCategory = remote.subCategory,
                        datePersian = remote.datePersian,
                        paymentMethod = remote.paymentMethod,
                        sourceType = remote.sourceType,
                        sourceId = remote.sourceId,
                        isRecurring = remote.isRecurring,
                        syncState = "SYNCED",
                        updatedAt = updatedAt,
                        deletedAt = null
                    )
                )
            }

            for (original in dao.getPendingSyncTransactions(userId)) {
                val clientId = original.stringId.ifBlank { "android-local-" + original.id }
                val local = if (original.stringId.isBlank()) {
                    val normalized = original.copy(stringId = clientId)
                    dao.updateTransaction(normalized)
                    normalized
                } else {
                    original
                }
                val remote = remoteByClientId[clientId]
                val remoteUpdated = remote?.let(::remoteUpdatedAt) ?: Long.MIN_VALUE

                if (local.syncState == "PENDING_DELETE") {
                    if (remote == null) {
                        dao.markTransactionSynced(userId, clientId, local.updatedAt, local.deletedAt)
                    } else if (local.updatedAt >= remoteUpdated) {
                        val response = ApiClient.transactionApi.deleteTransaction(remote.id)
                        if (response.isSuccessful && response.body()?.success == true) {
                            dao.markTransactionSynced(userId, clientId, local.updatedAt, local.deletedAt)
                        }
                    } else {
                        applyRemote(remote, local)
                    }
                } else if (remote == null || local.updatedAt >= remoteUpdated) {
                    val request = NetworkTransactionRequest(
                        clientId, local.amount, local.type, local.category, local.accountName,
                        local.description, java.time.Instant.ofEpochMilli(local.timestamp).toString(),
                        local.timeFormatted, local.title, local.subCategory, local.datePersian,
                        local.paymentMethod, local.sourceType, local.sourceId, local.isRecurring
                    )
                    val response = if (remote == null) {
                        ApiClient.transactionApi.upsertTransaction(request)
                    } else {
                        ApiClient.transactionApi.updateTransaction(remote.id, request)
                    }
                    if (response.isSuccessful && response.body()?.success == true) {
                        val server = response.body()?.data
                        val serverUpdated = server?.let(::remoteUpdatedAt) ?: local.updatedAt
                        dao.markTransactionSynced(userId, clientId, serverUpdated, null)
                    }
                } else {
                    applyRemote(remote, local)
                }
            }

            for (remote in remoteByClientId.values) {
                val existing = dao.getTransactionIncludingDeleted(userId, remote.clientId)
                val remoteUpdated = remoteUpdatedAt(remote)
                if (existing == null || (existing.syncState == "SYNCED" && remoteUpdated >= existing.updatedAt)) {
                    applyRemote(remote, existing)
                }
            }

            SyncStatus.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "Transaction sync failed", e)
            SyncStatus.ERROR
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

                    val smartEntity = ReminderEntity(
                        id = serverItem.id,
                        userId = SessionManager.userId ?: continue,
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
        val userId = SessionManager.userId ?: return
        val activeSmsReminders = smartReminderDao.getActiveRemindersList(userId).filter { it.smsEnabled }
        for (rem in activeSmsReminders) {
            try {
                val statusRes = ApiClient.reminderApi.getReminderSmsStatus(rem.id)
                if (statusRes.isSuccessful && statusRes.body()?.success == true) {
                    val smsData = statusRes.body()?.data
                    if (smsData != null) {
                        val intId = rem.id.hashCode()
                        val existingLog = smsLogDao.getLatestLogForReminder(userId, intId)
                        if (existingLog == null) {
                            smsLogDao.insertSmsLog(
                                SmsLogEntity(
                                    userId = userId,
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
                                userId = userId,
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
            val userId = SessionManager.userId ?: return@withContext SyncStatus.IDLE
            val pendingNotifs = notificationLogDao.getPendingSyncNotifications(userId)
            for (notif in pendingNotifs) {
                if (notif.isRead && notif.serverId != null) {
                    try {
                        ApiClient.notificationApi.markAsRead(notif.serverId)
                        notificationLogDao.updateSyncStatus(userId, notif.id, notif.serverId, "SYNCED")
                    } catch (e: Exception) {
                        // Keep pending
                    }
                }
            }

            val listResponse = ApiClient.notificationApi.listNotifications(page = 0, size = 50)
            if (listResponse.isSuccessful && listResponse.body()?.success == true) {
                val serverNotifs = listResponse.body()?.data ?: emptyList()
                for (sNotif in serverNotifs) {
                    val local = notificationLogDao.getNotificationByServerId(userId, sNotif.id)
                    if (local == null) {
                        notificationLogDao.insertNotification(
                            NotificationLogEntity(
                                userId = userId,
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
