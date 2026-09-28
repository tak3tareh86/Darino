package com.example.data.repository

import android.content.Context
import com.example.data.api.ApiClient
import com.example.data.api.SyncManager
import com.example.data.database.AppDatabase
import com.example.data.database.NotificationLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val notificationLogDao = database.notificationLogDao()
    private val syncManager = SyncManager(context, database)
    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    fun getAllNotifications(): Flow<List<NotificationLogEntity>> =
        notificationLogDao.getAllNotifications()

    fun getUnreadCount(): Flow<Int> =
        notificationLogDao.getUnreadCount()

    suspend fun markAsRead(id: Int) = withContext(Dispatchers.IO) {
        notificationLogDao.markAsRead(id)
        backgroundScope.launch {
            syncManager.syncNotifications()
        }
    }

    suspend fun markAllAsRead() = withContext(Dispatchers.IO) {
        notificationLogDao.markAllAsRead()
        backgroundScope.launch {
            try {
                ApiClient.notificationApi.markAllAsRead()
            } catch (e: Exception) {
                // Handled in sync
            }
        }
    }

    suspend fun refreshNotifications() = withContext(Dispatchers.IO) {
        syncManager.syncNotifications()
    }
}
