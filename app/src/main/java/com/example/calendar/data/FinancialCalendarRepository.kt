package com.example.calendar.data

import android.content.Context
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventStatus
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinancialCalendarRepository(context: Context) {

    private val dao: FinancialEventDao = AppDatabase.getDatabase(context).financialEventDao()

    private fun requireUserId(): String =
        SessionManager.userId ?: throw IllegalStateException("Authenticated user is required")

    fun getAllEvents(): Flow<List<FinancialEvent>> {
        val userId = SessionManager.userId
        if (userId == null) return kotlinx.coroutines.flow.flowOf(emptyList())
        return dao.getAllEvents(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getEventsByDate(date: String): Flow<List<FinancialEvent>> {
        return dao.getEventsByDate(requireUserId(), date).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun insertEvent(event: FinancialEvent) {
        dao.insertEvent(FinancialEventEntity.fromDomain(event, requireUserId()))
    }

    suspend fun insertEvents(events: List<FinancialEvent>) {
        dao.insertEvents(events.map { FinancialEventEntity.fromDomain(it, requireUserId()) })
    }

    suspend fun updateEvent(event: FinancialEvent) {
        dao.updateEvent(FinancialEventEntity.fromDomain(event, requireUserId()))
    }

    suspend fun updateEventStatus(id: String, status: FinancialEventStatus) {
        dao.updateEventStatus(requireUserId(), id, status.name)
    }

    suspend fun deleteEventById(id: String) {
        dao.deleteEventById(requireUserId(), id)
    }

    suspend fun getCount(): Int {
        return dao.getCount(requireUserId())
    }
}
