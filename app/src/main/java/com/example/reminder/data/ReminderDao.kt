package com.example.reminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    // === Reminders ===

    @Query("SELECT * FROM smart_reminders WHERE status != 'CANCELLED' ORDER BY date ASC, time ASC")
    fun getAllActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM smart_reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM smart_reminders")
    suspend fun getAllRemindersList(): List<ReminderEntity>

    @Query("SELECT * FROM smart_reminder_schedules")
    suspend fun getAllSchedulesList(): List<ReminderScheduleEntity>

    @Query("DELETE FROM smart_reminders")
    suspend fun clearAllReminders()

    @Query("SELECT * FROM smart_reminders WHERE status = 'ACTIVE' ORDER BY date ASC, time ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM smart_reminders WHERE status = 'ACTIVE' ORDER BY date ASC, time ASC")
    suspend fun getActiveRemindersList(): List<ReminderEntity>

    @Query("SELECT * FROM smart_reminders WHERE status = 'COMPLETED' ORDER BY updatedAt DESC")
    fun getCompletedReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM smart_reminders WHERE status = 'MISSED' ORDER BY date ASC")
    fun getMissedReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM smart_reminders WHERE type = :type ORDER BY createdAt DESC")
    fun getRemindersByType(type: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM smart_reminders WHERE sourceId = :sourceId LIMIT 1")
    suspend fun getReminderBySourceId(sourceId: String): ReminderEntity?

    @Query("SELECT * FROM smart_reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM smart_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    @Query("UPDATE smart_reminders SET status = :status, updatedAt = :updatedAt, completedAt = :completedAt, cancelledAt = :cancelledAt WHERE id = :id")
    suspend fun updateStatus(
        id: String,
        status: String,
        updatedAt: Long = System.currentTimeMillis(),
        completedAt: Long? = null,
        cancelledAt: Long? = null
    )

    @Query("UPDATE smart_reminders SET date = :date, time = :time, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDateTime(id: String, date: String, time: String, updatedAt: Long = System.currentTimeMillis())

    // === Schedules ===

    @Query("SELECT * FROM smart_reminder_schedules WHERE reminderId = :reminderId ORDER BY triggerDateTime ASC")
    fun getSchedulesForReminder(reminderId: String): Flow<List<ReminderScheduleEntity>>

    @Query("SELECT * FROM smart_reminder_schedules WHERE reminderId = :reminderId ORDER BY triggerDateTime ASC")
    suspend fun getSchedulesForReminderSync(reminderId: String): List<ReminderScheduleEntity>

    @Query("SELECT * FROM smart_reminder_schedules WHERE enabled = 1 AND triggerDateTime > :now ORDER BY triggerDateTime ASC")
    suspend fun getAllFutureActiveSchedules(now: Long = System.currentTimeMillis()): List<ReminderScheduleEntity>

    @Query("SELECT * FROM smart_reminder_schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: String): ReminderScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ReminderScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ReminderScheduleEntity>)

    @Update
    suspend fun updateSchedule(schedule: ReminderScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ReminderScheduleEntity)

    @Query("DELETE FROM smart_reminder_schedules WHERE reminderId = :reminderId")
    suspend fun deleteSchedulesByReminderId(reminderId: String)

    // === Delivery Logs ===

    @Query("SELECT * FROM smart_reminder_delivery_logs ORDER BY scheduledAt DESC")
    fun getAllDeliveryLogs(): Flow<List<ReminderDeliveryLogEntity>>

    @Query("SELECT * FROM smart_reminder_delivery_logs WHERE reminderId = :reminderId ORDER BY scheduledAt DESC")
    fun getDeliveryLogsForReminder(reminderId: String): Flow<List<ReminderDeliveryLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryLog(log: ReminderDeliveryLogEntity)

    @Query("UPDATE smart_reminder_delivery_logs SET status = :status, triggeredAt = :triggeredAt, errorMessage = :errorMessage, providerMessageId = :providerId WHERE id = :id")
    suspend fun updateDeliveryLog(
        id: String,
        status: String,
        triggeredAt: Long?,
        errorMessage: String?,
        providerId: String?
    )
}
