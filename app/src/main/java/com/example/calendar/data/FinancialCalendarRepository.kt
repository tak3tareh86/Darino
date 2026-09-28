package com.example.calendar.data

import android.content.Context
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventStatus
import com.example.data.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinancialCalendarRepository(context: Context) {

    private val dao: FinancialEventDao = AppDatabase.getDatabase(context).financialEventDao()

    fun getAllEvents(): Flow<List<FinancialEvent>> {
        return dao.getAllEvents().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getEventsByDate(date: String): Flow<List<FinancialEvent>> {
        return dao.getEventsByDate(date).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun insertEvent(event: FinancialEvent) {
        dao.insertEvent(FinancialEventEntity.fromDomain(event))
    }

    suspend fun insertEvents(events: List<FinancialEvent>) {
        dao.insertEvents(events.map { FinancialEventEntity.fromDomain(it) })
    }

    suspend fun updateEvent(event: FinancialEvent) {
        dao.updateEvent(FinancialEventEntity.fromDomain(event))
    }

    suspend fun updateEventStatus(id: String, status: FinancialEventStatus) {
        dao.updateEventStatus(id, status.name)
    }

    suspend fun deleteEventById(id: String) {
        dao.deleteEventById(id)
    }

    suspend fun getCount(): Int {
        return dao.getCount()
    }
}
