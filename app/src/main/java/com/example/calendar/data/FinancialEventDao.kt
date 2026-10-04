package com.example.calendar.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialEventDao {

    @Query("SELECT * FROM financial_events WHERE userId = :userId ORDER BY date ASC, time ASC")
    fun getAllEvents(userId: String): Flow<List<FinancialEventEntity>>

    @Query("SELECT * FROM financial_events WHERE userId = :userId")
    suspend fun getAllEventsList(userId: String): List<FinancialEventEntity>

    @Query("DELETE FROM financial_events WHERE userId = :userId")
    suspend fun clearAllEvents(userId: String)

    @Query("SELECT * FROM financial_events WHERE userId = :userId AND date = :date ORDER BY time ASC")
    fun getEventsByDate(userId: String, date: String): Flow<List<FinancialEventEntity>>

    @Query("SELECT * FROM financial_events WHERE userId = :userId AND date LIKE :monthPrefix || '%' ORDER BY date ASC")
    fun getEventsForMonth(userId: String, monthPrefix: String): Flow<List<FinancialEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: FinancialEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<FinancialEventEntity>)

    @Update
    suspend fun updateEvent(event: FinancialEventEntity)

    @Delete
    suspend fun deleteEvent(event: FinancialEventEntity)

    @Query("DELETE FROM financial_events WHERE userId = :userId AND id = :id")
    suspend fun deleteEventById(userId: String, id: String)

    @Query("UPDATE financial_events SET status = :status WHERE userId = :userId AND id = :id")
    suspend fun updateEventStatus(userId: String, id: String, status: String)

    @Query("SELECT COUNT(*) FROM financial_events WHERE userId = :userId")
    suspend fun getCount(userId: String): Int
}
