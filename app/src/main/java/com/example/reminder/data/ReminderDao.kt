package com.example.reminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM smart_reminders WHERE userId = :userId AND status != 'CANCELLED' ORDER BY date ASC, time ASC")
    fun getAllActiveReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM smart_reminders WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM smart_reminders WHERE userId = :userId")
    suspend fun getAllRemindersList(userId: String): List<ReminderEntity>

    @Query("DELETE FROM smart_reminders WHERE userId = :userId")
    suspend fun clearAllReminders(userId: String): Int

    @Query("SELECT * FROM smart_reminders WHERE userId = :userId AND status = 'ACTIVE' ORDER BY date ASC, time ASC")
    fun getActiveReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM smart_reminders WHERE userId = :userId AND status = 'ACTIVE' ORDER BY date ASC, time ASC")
    suspend fun getActiveRemindersList(userId: String): List<ReminderEntity>

    @Query("SELECT * FROM smart_reminders WHERE userId = :userId AND status = 'COMPLETED' ORDER BY updatedAt DESC")
    fun getCompletedReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM smart_reminders WHERE userId = :userId AND status = 'MISSED' ORDER BY date ASC")
    fun getMissedReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM smart_reminders WHERE userId = :userId AND type = :type ORDER BY createdAt DESC")
    fun getRemindersByType(userId: String, type: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM smart_reminders WHERE userId = :userId AND sourceId = :sourceId LIMIT 1")
    suspend fun getReminderBySourceId(userId: String, sourceId: String): ReminderEntity?

    @Query("SELECT * FROM smart_reminders WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getReminderById(userId: String, id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>): List<Long>

    @Update
    suspend fun updateReminder(reminder: ReminderEntity): Int

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity): Int

    @Query("DELETE FROM smart_reminders WHERE userId = :userId AND id = :id")
    suspend fun deleteReminderById(userId: String, id: String): Int

    @Query("UPDATE smart_reminders SET status = :status, updatedAt = :updatedAt, completedAt = :completedAt, cancelledAt = :cancelledAt WHERE userId = :userId AND id = :id")
    suspend fun updateStatus(userId: String, id: String, status: String, updatedAt: Long = System.currentTimeMillis(), completedAt: Long? = null, cancelledAt: Long? = null): Int

    @Query("UPDATE smart_reminders SET date = :date, time = :time, updatedAt = :updatedAt WHERE userId = :userId AND id = :id")
    suspend fun updateDateTime(userId: String, id: String, date: String, time: String, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("SELECT s.* FROM smart_reminder_schedules s INNER JOIN smart_reminders r ON r.id = s.reminderId WHERE r.userId = :userId AND s.reminderId = :reminderId ORDER BY s.triggerDateTime ASC")
    fun getSchedulesForReminder(userId: String, reminderId: String): Flow<List<ReminderScheduleEntity>>

    @Query("SELECT s.* FROM smart_reminder_schedules s INNER JOIN smart_reminders r ON r.id = s.reminderId WHERE r.userId = :userId AND s.reminderId = :reminderId ORDER BY s.triggerDateTime ASC")
    suspend fun getSchedulesForReminderSync(userId: String, reminderId: String): List<ReminderScheduleEntity>

    @Query("SELECT s.* FROM smart_reminder_schedules s INNER JOIN smart_reminders r ON r.id = s.reminderId WHERE r.userId = :userId AND s.enabled = 1 AND s.triggerDateTime > :now ORDER BY s.triggerDateTime ASC")
    suspend fun getAllFutureActiveSchedules(userId: String, now: Long = System.currentTimeMillis()): List<ReminderScheduleEntity>

    @Query("DELETE FROM smart_reminder_schedules WHERE reminderId IN (SELECT id FROM smart_reminders WHERE userId = :userId) AND reminderId = :reminderId")
    suspend fun deleteSchedulesByReminderId(userId: String, reminderId: String): Int

    @Query("""
        SELECT s.*
        FROM smart_reminder_schedules AS s
        INNER JOIN smart_reminders AS r ON r.id = s.reminderId
        WHERE r.userId = :userId
          AND s.id = :id
        LIMIT 1
    """)
    suspend fun getScheduleById(userId: String, id: String): ReminderScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ReminderScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ReminderScheduleEntity>)

    @Update
    suspend fun updateSchedule(schedule: ReminderScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ReminderScheduleEntity)

    @Query("SELECT l.* FROM smart_reminder_delivery_logs l INNER JOIN smart_reminders r ON r.id = l.reminderId WHERE r.userId = :userId AND l.reminderId = :reminderId ORDER BY l.scheduledAt DESC")
    fun getDeliveryLogsForReminder(userId: String, reminderId: String): Flow<List<ReminderDeliveryLogEntity>>

    @Query("SELECT l.* FROM smart_reminder_delivery_logs l INNER JOIN smart_reminders r ON r.id = l.reminderId WHERE r.userId = :userId ORDER BY l.scheduledAt DESC")
    fun getAllDeliveryLogs(userId: String): Flow<List<ReminderDeliveryLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryLog(log: ReminderDeliveryLogEntity)
}
