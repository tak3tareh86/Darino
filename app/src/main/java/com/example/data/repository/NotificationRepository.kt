package com.example.data.repository

import android.content.Context
import com.example.data.api.ApiClient
import com.example.data.api.SyncManager
import com.example.data.database.AppDatabase
import com.example.data.database.NotificationLogEntity
import com.example.data.security.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
        notificationLogDao.getAllNotifications(SessionManager.userId ?: return flowOf(emptyList()))

    fun getUnreadCount(): Flow<Int> =
        notificationLogDao.getUnreadCount(SessionManager.userId ?: return flowOf(0))

    suspend fun markAsRead(id: Int) = withContext(Dispatchers.IO) {
        notificationLogDao.markAsRead(SessionManager.userId ?: return@withContext, id)
        backgroundScope.launch {
            syncManager.syncNotifications()
        }
    }

    suspend fun markAllAsRead() = withContext(Dispatchers.IO) {
        notificationLogDao.markAllAsRead(SessionManager.userId ?: return@withContext)
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
