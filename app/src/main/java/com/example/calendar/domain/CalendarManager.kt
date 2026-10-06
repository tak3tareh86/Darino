package com.example.calendar.domain

import android.content.Context
import com.example.calendar.data.FinancialCalendarRepository
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventStatus
import com.example.calendar.domain.model.FinancialEventType
import com.example.calendar.domain.model.ReminderBeforeOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.combine

class CalendarManager(private val context: Context) {

    private val repository = FinancialCalendarRepository(context)
    /** Streams persisted calendar events together with user-owned smart reminders. */
    fun getAllEvents(): Flow<List<FinancialEvent>> {
        val database = com.example.data.database.AppDatabase.getDatabase(context)
        val userId = com.example.data.security.SessionManager.userId ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        val remindersFlow = database.smartReminderDao().getAllReminders(userId)

        return repository.getAllEvents().combine(remindersFlow) { dbEvents, reminders ->
            val allEvents = mutableListOf<FinancialEvent>()
            val seenIds = mutableSetOf<String>()

            // 1. Add DB events
            for (ev in dbEvents) {
                seenIds.add(ev.id)
                allEvents.add(ev)
            }

            // 2. Cross-integrate from Reminders Module
            reminders.forEach { rem ->
                val eventId = "auto_rem_${rem.id}"
                if (!seenIds.contains(eventId)) {
                    val normDate = CalendarDateUtils.normalizeDate(rem.date)
                    val normTime = com.example.util.IranianPhoneUtils.convertDigitsToEnglish(rem.time)

                    val status = when (rem.status) {
                        "COMPLETED" -> FinancialEventStatus.PAID
                        "CANCELLED" -> FinancialEventStatus.PAID
                        else -> FinancialEventStatus.PENDING
                    }

                    allEvents.add(
                        FinancialEvent(
                            id = eventId,
                            title = rem.title,
                            description = rem.description,
                            type = FinancialEventType.REMINDER,
                            amount = rem.amount,
                            date = normDate,
                            time = normTime,
                            repeatType = "NONE",
                            reminderBefore = ReminderBeforeOption.ONE_DAY,
                            sourceId = rem.id,
                            status = status
                        )
                    )
                    seenIds.add(eventId)
                }
            }

            allEvents.sortedWith(
                compareBy<FinancialEvent> { CalendarDateUtils.normalizeDate(it.date) }
                    .thenBy { it.time ?: "00:00" }
            )
        }.flowOn(Dispatchers.IO)
    }

    suspend fun addEvent(event: FinancialEvent) {
        repository.insertEvent(event)
    }

    suspend fun updateEvent(event: FinancialEvent) {
        repository.updateEvent(event)
    }

    suspend fun deleteEvent(id: String) {
        repository.deleteEventById(id)
    }

    suspend fun toggleStatus(event: FinancialEvent) {
        if (event.id.startsWith("auto_rem_")) {
            val reminderId = event.id.removePrefix("auto_rem_")
            val database = com.example.data.database.AppDatabase.getDatabase(context)
            val userId = com.example.data.security.SessionManager.userId ?: return
            
            // Check current status. In Calendar, PAID maps to COMPLETED/CANCELLED.
            // If it's currently PAID, we want to set it back to PENDING (ACTIVE).
            val newReminderStatus = if (event.status == FinancialEventStatus.PAID) {
                "ACTIVE"
            } else {
                "COMPLETED"
            }
            
            // Update the real reminder in DB.
            database.smartReminderDao().updateStatus(
                userId = userId,
                id = reminderId,
                status = newReminderStatus,
                completedAt = if (newReminderStatus == "COMPLETED") System.currentTimeMillis() else null,
                cancelledAt = null
            )
        } else {
            val newStatus = if (event.status == FinancialEventStatus.PAID) {
                FinancialEventStatus.PENDING
            } else {
                FinancialEventStatus.PAID
            }
            repository.updateEvent(event.copy(status = newStatus))
        }
    }
}
