package com.example.data.api

import android.content.Context
import com.example.data.database.AppDatabase

/**
 * Offline compatibility adapter retained for repositories that historically
 * requested a background server sync after local database mutations.
 *
 * Local Room data is authoritative. These calls intentionally perform no
 * network work and never mutate or clear local records.
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    OFFLINE,
    ERROR
}

class SyncManager(
    @Suppress("UNUSED_PARAMETER") context: Context,
    @Suppress("UNUSED_PARAMETER") database: AppDatabase
) {
    suspend fun syncAll(): SyncStatus = SyncStatus.IDLE
    suspend fun syncTransactions(): SyncStatus = SyncStatus.IDLE
    suspend fun syncReminders(): SyncStatus = SyncStatus.IDLE
    suspend fun syncNotifications(): SyncStatus = SyncStatus.IDLE
    suspend fun syncPhoneStatus() = Unit
}
