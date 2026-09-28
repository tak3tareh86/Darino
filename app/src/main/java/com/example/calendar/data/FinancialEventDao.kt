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

    @Query("SELECT * FROM financial_events ORDER BY date ASC, time ASC")
    fun getAllEvents(): Flow<List<FinancialEventEntity>>

    @Query("SELECT * FROM financial_events")
    suspend fun getAllEventsList(): List<FinancialEventEntity>

    @Query("DELETE FROM financial_events")
    suspend fun clearAllEvents()

    @Query("SELECT * FROM financial_events WHERE date = :date ORDER BY time ASC")
    fun getEventsByDate(date: String): Flow<List<FinancialEventEntity>>

    @Query("SELECT * FROM financial_events WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    fun getEventsForMonth(monthPrefix: String): Flow<List<FinancialEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: FinancialEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<FinancialEventEntity>)

    @Update
    suspend fun updateEvent(event: FinancialEventEntity)

    @Delete
    suspend fun deleteEvent(event: FinancialEventEntity)

    @Query("DELETE FROM financial_events WHERE id = :id")
    suspend fun deleteEventById(id: String)

    @Query("UPDATE financial_events SET status = :status WHERE id = :id")
    suspend fun updateEventStatus(id: String, status: String)

    @Query("SELECT COUNT(*) FROM financial_events")
    suspend fun getCount(): Int
}
