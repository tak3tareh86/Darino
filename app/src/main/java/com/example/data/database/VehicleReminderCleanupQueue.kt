package com.example.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Durable vehicle-owned work items for external alarm/notification cancellation. */
@Entity(tableName = "vehicle_reminder_cleanup_queue", primaryKeys = ["userId", "reminderId"])
data class VehicleReminderCleanupQueueEntity(
    val userId: String,
    val reminderId: String,
    val schedulesJson: String,
    val createdAt: Long
)

@Dao
interface VehicleReminderCleanupQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: VehicleReminderCleanupQueueEntity)

    @Query("SELECT * FROM vehicle_reminder_cleanup_queue WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getPendingForUser(userId: String): List<VehicleReminderCleanupQueueEntity>

    @Query("DELETE FROM vehicle_reminder_cleanup_queue WHERE userId = :userId AND reminderId = :reminderId")
    suspend fun delete(userId: String, reminderId: String)
}
