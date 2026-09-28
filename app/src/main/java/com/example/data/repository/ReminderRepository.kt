package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.ApiClient
import com.example.data.api.SyncManager
import com.example.data.database.AppDatabase
import com.example.data.database.ReminderEntity
import com.example.data.receiver.ReminderScheduler
import com.example.data.security.SessionManager
import com.example.util.IranianPhoneUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val reminderDao = database.reminderDao()
    private val reminderScheduler = ReminderScheduler(context)
    private val syncManager = SyncManager(context, database)
    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    fun getAllReminders(): Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    fun getActiveReminders(): Flow<List<ReminderEntity>> = reminderDao.getActiveReminders()

    suspend fun getReminderById(id: Int): ReminderEntity? = withContext(Dispatchers.IO) {
        reminderDao.getReminderById(id)
    }

    suspend fun createReminder(
        type: String,
        title: String,
        description: String,
        scheduledDateTime: Long,
        repeatRule: String = "NONE",
        notificationEnabled: Boolean = true,
        smsEnabled: Boolean = false,
        phoneNumber: String? = null,
        sourceType: String? = null,
        sourceId: Int? = null
    ): Long = withContext(Dispatchers.IO) {
        val normalizedPhone = IranianPhoneUtils.normalizeIranianPhoneNumber(phoneNumber)
        val maskedPhone = if (normalizedPhone != null) IranianPhoneUtils.maskPhoneNumber(normalizedPhone) else null

        val entity = ReminderEntity(
            id = 0,
            serverId = null,
            syncState = "PENDING_INSERT",
            userId = SessionManager.userId ?: "1",
            type = type,
            title = title,
            description = description,
            sourceId = sourceId,
            sourceType = sourceType,
            scheduledDateTime = scheduledDateTime,
            timezone = "Asia/Tehran",
            repeatRule = repeatRule,
            enabled = true,
            notificationEnabled = notificationEnabled,
            smsEnabled = smsEnabled && !normalizedPhone.isNullOrBlank(),
            phoneNumber = normalizedPhone,
            phoneNumberMasked = maskedPhone,
            smsDeliveryStatus = if (smsEnabled && !normalizedPhone.isNullOrBlank()) "QUEUED" else null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            completedAt = null,
            deletedAt = null,
            lastSyncedAt = null
        )

        val insertedId = reminderDao.insertReminder(entity)
        val createdEntity = entity.copy(id = insertedId.toInt())

        // 1. Immediately schedule local notification if enabled
        if (createdEntity.notificationEnabled && createdEntity.scheduledDateTime > System.currentTimeMillis()) {
            reminderScheduler.schedule(createdEntity)
            Log.i(TAG, "Locally scheduled alarm for reminder #${createdEntity.id}")
        }

        // 2. Trigger asynchronous background push to backend
        backgroundScope.launch {
            syncManager.syncReminders()
        }

        insertedId
    }

    suspend fun updateReminder(reminder: ReminderEntity) = withContext(Dispatchers.IO) {
        val updated = reminder.copy(
            updatedAt = System.currentTimeMillis(),
            syncState = "PENDING_UPDATE"
        )
        reminderDao.updateReminder(updated)

        if (updated.enabled && updated.notificationEnabled && updated.scheduledDateTime > System.currentTimeMillis()) {
            reminderScheduler.schedule(updated)
        } else {
            reminderScheduler.cancel(updated.id)
        }

        backgroundScope.launch {
            syncManager.syncReminders()
        }
    }

    suspend fun toggleEnabled(id: Int, enabled: Boolean) = withContext(Dispatchers.IO) {
        reminderDao.toggleEnabled(id, enabled)
        val rem = reminderDao.getReminderById(id)
        if (rem != null) {
            if (enabled && rem.scheduledDateTime > System.currentTimeMillis()) {
                reminderScheduler.schedule(rem)
            } else {
                reminderScheduler.cancel(id)
            }
        }
        backgroundScope.launch {
            syncManager.syncReminders()
        }
    }

    suspend fun markCompleted(id: Int, completed: Boolean) = withContext(Dispatchers.IO) {
        val completedAt = if (completed) System.currentTimeMillis() else null
        reminderDao.markCompleted(id, completedAt)
        if (completed) {
            reminderScheduler.cancel(id)
        }
        backgroundScope.launch {
            syncManager.syncReminders()
        }
    }

    suspend fun deleteReminder(id: Int) = withContext(Dispatchers.IO) {
        reminderScheduler.cancel(id)
        reminderDao.softDeleteReminder(id)
        backgroundScope.launch {
            syncManager.syncReminders()
        }
    }

    suspend fun snoozeReminder(id: Int, snoozeMinutes: Long = 15) = withContext(Dispatchers.IO) {
        val existing = reminderDao.getReminderById(id) ?: return@withContext
        val newTrigger = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L
        val updated = existing.copy(
            scheduledDateTime = newTrigger,
            updatedAt = System.currentTimeMillis(),
            syncState = "PENDING_UPDATE"
        )
        reminderDao.updateReminder(updated)
        reminderScheduler.schedule(updated)

        if (existing.serverId != null) {
            backgroundScope.launch {
                try {
                    ApiClient.reminderApi.snoozeReminder(existing.serverId, mapOf("durationMinutes" to snoozeMinutes))
                } catch (e: Exception) {
                    // Handled during normal sync
                }
            }
        }
    }

    suspend fun refreshSmsStatus(reminderId: Int): String? = withContext(Dispatchers.IO) {
        val reminder = reminderDao.getReminderById(reminderId) ?: return@withContext null
        if (reminder.serverId == null || !reminder.smsEnabled) return@withContext null

        try {
            val response = ApiClient.reminderApi.getReminderSmsStatus(reminder.serverId)
            if (response.isSuccessful && response.body()?.success == true) {
                val status = response.body()?.data?.status
                if (status != null) {
                    reminderDao.updateSmsStatus(reminderId, status)
                    return@withContext status
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh SMS status: ${e.message}")
        }
        reminder.smsDeliveryStatus
    }

    companion object {
        private const val TAG = "ReminderRepository"
    }
}
