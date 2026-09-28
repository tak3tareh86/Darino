package com.example.reminder.data

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.reminder.domain.ReminderManager
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface ReminderRepository {
    fun getAllReminders(): Flow<List<ReminderEntity>>
    fun getActiveReminders(): Flow<List<ReminderEntity>>
    fun getTodayReminders(): Flow<List<ReminderEntity>>
    fun getUpcomingReminders(): Flow<List<ReminderEntity>>
    fun getCompletedReminders(): Flow<List<ReminderEntity>>
    fun getMissedReminders(): Flow<List<ReminderEntity>>
    fun getRemindersByType(type: String): Flow<List<ReminderEntity>>
    suspend fun getReminderById(id: String): ReminderEntity?
    suspend fun getReminderBySourceId(sourceId: String): ReminderEntity?
    suspend fun insertReminder(reminder: ReminderEntity)
    suspend fun insertReminders(reminders: List<ReminderEntity>)
    suspend fun updateReminder(reminder: ReminderEntity)
    suspend fun deleteReminder(reminder: ReminderEntity)
    suspend fun deleteReminderById(id: String)
    suspend fun updateStatus(id: String, status: String)

    fun getSchedules(reminderId: String): Flow<List<ReminderScheduleEntity>>
    suspend fun getSchedulesSync(reminderId: String): List<ReminderScheduleEntity>
    suspend fun insertSchedule(schedule: ReminderScheduleEntity)
    suspend fun insertSchedules(schedules: List<ReminderScheduleEntity>)
    suspend fun updateSchedule(schedule: ReminderScheduleEntity)
    suspend fun deleteSchedule(schedule: ReminderScheduleEntity)
    suspend fun deleteSchedulesByReminderId(reminderId: String)

    fun getDeliveryLogs(reminderId: String): Flow<List<ReminderDeliveryLogEntity>>
    fun getAllDeliveryLogs(): Flow<List<ReminderDeliveryLogEntity>>
    suspend fun insertDeliveryLog(log: ReminderDeliveryLogEntity)
}

class LocalReminderRepository(context: Context) : ReminderRepository {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.smartReminderDao()

    init {
        // Prepopulate with initial smart reminders if empty
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val current = dao.getAllReminders().first()
                if (current.isEmpty()) {
                    val initial = ReminderManager.getInitialSmartReminders()
                    dao.insertReminders(initial)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getAllReminders(): Flow<List<ReminderEntity>> = dao.getAllReminders()

    override fun getActiveReminders(): Flow<List<ReminderEntity>> = dao.getActiveReminders()

    override fun getTodayReminders(): Flow<List<ReminderEntity>> {
        val todayPersian = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
        return dao.getActiveReminders().map { list ->
            list.filter { it.date == todayPersian }
        }
    }

    override fun getUpcomingReminders(): Flow<List<ReminderEntity>> {
        val todayPersian = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
        return dao.getActiveReminders().map { list ->
            list.filter { it.date >= todayPersian }
        }
    }

    override fun getCompletedReminders(): Flow<List<ReminderEntity>> = dao.getCompletedReminders()

    override fun getMissedReminders(): Flow<List<ReminderEntity>> = dao.getMissedReminders()

    override fun getRemindersByType(type: String): Flow<List<ReminderEntity>> = dao.getRemindersByType(type)

    override suspend fun getReminderById(id: String): ReminderEntity? = dao.getReminderById(id)

    override suspend fun getReminderBySourceId(sourceId: String): ReminderEntity? = dao.getReminderBySourceId(sourceId)

    override suspend fun insertReminder(reminder: ReminderEntity) = dao.insertReminder(reminder)

    override suspend fun insertReminders(reminders: List<ReminderEntity>) = dao.insertReminders(reminders)

    override suspend fun updateReminder(reminder: ReminderEntity) = dao.updateReminder(reminder)

    override suspend fun deleteReminder(reminder: ReminderEntity) = dao.deleteReminder(reminder)

    override suspend fun deleteReminderById(id: String) = dao.deleteReminderById(id)

    override suspend fun updateStatus(id: String, status: String) {
        val completedAt = if (status == "COMPLETED") System.currentTimeMillis() else null
        val cancelledAt = if (status == "CANCELLED") System.currentTimeMillis() else null
        dao.updateStatus(id, status, System.currentTimeMillis(), completedAt, cancelledAt)
    }

    override fun getSchedules(reminderId: String): Flow<List<ReminderScheduleEntity>> =
        dao.getSchedulesForReminder(reminderId)

    override suspend fun getSchedulesSync(reminderId: String): List<ReminderScheduleEntity> =
        dao.getSchedulesForReminderSync(reminderId)

    override suspend fun insertSchedule(schedule: ReminderScheduleEntity) =
        dao.insertSchedule(schedule)

    override suspend fun insertSchedules(schedules: List<ReminderScheduleEntity>) =
        dao.insertSchedules(schedules)

    override suspend fun updateSchedule(schedule: ReminderScheduleEntity) =
        dao.updateSchedule(schedule)

    override suspend fun deleteSchedule(schedule: ReminderScheduleEntity) =
        dao.deleteSchedule(schedule)

    override suspend fun deleteSchedulesByReminderId(reminderId: String) =
        dao.deleteSchedulesByReminderId(reminderId)

    override fun getDeliveryLogs(reminderId: String): Flow<List<ReminderDeliveryLogEntity>> =
        dao.getDeliveryLogsForReminder(reminderId)

    override fun getAllDeliveryLogs(): Flow<List<ReminderDeliveryLogEntity>> =
        dao.getAllDeliveryLogs()

    override suspend fun insertDeliveryLog(log: ReminderDeliveryLogEntity) =
        dao.insertDeliveryLog(log)
}
