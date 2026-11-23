package com.example.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Remembers exactly which legacy check reminders were active before a bounced
 * check was replaced. One row per legacy reminder keeps restoration owner- and
 * check-scoped and survives process death.
 */
@Entity(
    tableName = "check_reminder_replacement_pauses",
    primaryKeys = ["userId", "checkId", "legacyReminderId"]
)
data class CheckReminderReplacementPauseEntity(
    val userId: String,
    val checkId: Int,
    val legacyReminderId: Int,
    val pausedAt: Long
)

@Dao
interface CheckReminderReplacementPauseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<CheckReminderReplacementPauseEntity>)

    @Query("""
        SELECT * FROM check_reminder_replacement_pauses
        WHERE userId = :userId AND checkId = :checkId
        ORDER BY legacyReminderId ASC
    """)
    suspend fun getForCheck(userId: String, checkId: Int): List<CheckReminderReplacementPauseEntity>

    @Query("""
        DELETE FROM check_reminder_replacement_pauses
        WHERE userId = :userId AND checkId = :checkId
    """)
    suspend fun deleteForCheck(userId: String, checkId: Int)
}
