package com.example.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Durable, check-only outbox for cancelling platform alarms and notifications
 * after the check and its reminder rows have been deleted transactionally.
 */
@Entity(tableName = "check_reminder_cleanup_queue", primaryKeys = ["userId", "checkId"])
data class CheckReminderCleanupQueueEntity(
    val userId: String,
    val checkId: String,
    val smartReminderId: String?,
    val smartSchedulesJson: String,
    val legacyReminderIdsJson: String,
    val createdAt: Long
)

@Dao
interface CheckReminderCleanupQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CheckReminderCleanupQueueEntity)

    @Query("SELECT * FROM check_reminder_cleanup_queue WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getPendingForUser(userId: String): List<CheckReminderCleanupQueueEntity>

    @Query("DELETE FROM check_reminder_cleanup_queue WHERE userId = :userId AND checkId = :checkId")
    suspend fun delete(userId: String, checkId: String)
}
