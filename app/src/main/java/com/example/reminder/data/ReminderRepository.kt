package com.example.reminder.data

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
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

    private fun requireUserId(): String =
        SessionManager.userId ?: throw IllegalStateException("Authenticated user is required for reminder access")

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = SessionManager.userId ?: return@launch
                val prefs = context.applicationContext.getSharedPreferences("darino_general_preferences", Context.MODE_PRIVATE)
                val isCleanSlate = prefs.getBoolean("pref_is_clean_slate", false)

                val current = dao.getAllReminders(userId).first()
                if (current.isEmpty()) {
                    val legacyList = db.reminderDao().getActiveRemindersSnapshot()
                        .filter { it.userId == userId }
                    if (legacyList.isNotEmpty()) {
                        val migrated = legacyList.map { leg ->
                            ReminderEntity(
                                id = if (leg.serverId.isNullOrBlank()) leg.id.toString() else leg.serverId,
                                userId = userId,
                                title = leg.title,
                                description = leg.description,
                                type = leg.type,
                                sourceType = leg.sourceType ?: "MANUAL",
                                sourceId = leg.sourceId?.toString(),
                                status = if (leg.completedAt != null) "COMPLETED" else if (leg.enabled) "ACTIVE" else "DISABLED",
                                date = PersianCalendarHelper.fromEpochMillis(leg.scheduledDateTime).toFormattedDate(),
                                time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(leg.scheduledDateTime)),
                                notificationEnabled = leg.notificationEnabled,
                                smsEnabled = leg.smsEnabled,
                                phoneNumber = leg.phoneNumber
                            )
                        }
                        dao.insertReminders(migrated)
                    } else if (!isCleanSlate) {
                        dao.insertReminders(ReminderManager.getInitialSmartReminders().map { it.copy(userId = userId) })
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getAllReminders(): Flow<List<ReminderEntity>> =
        SessionManager.userId?.let { dao.getAllReminders(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList())

    override fun getActiveReminders(): Flow<List<ReminderEntity>> =
        SessionManager.userId?.let { dao.getActiveReminders(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList())

    override fun getTodayReminders(): Flow<List<ReminderEntity>> {
        val todayPersian = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
        return getActiveReminders().map { list -> list.filter { it.date == todayPersian } }
    }

    override fun getUpcomingReminders(): Flow<List<ReminderEntity>> {
        val todayPersian = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
        return getActiveReminders().map { list -> list.filter { it.date >= todayPersian } }
    }

    override fun getCompletedReminders(): Flow<List<ReminderEntity>> =
        SessionManager.userId?.let { dao.getCompletedReminders(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList())

    override fun getMissedReminders(): Flow<List<ReminderEntity>> =
        SessionManager.userId?.let { dao.getMissedReminders(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList())

    override fun getRemindersByType(type: String): Flow<List<ReminderEntity>> =
        SessionManager.userId?.let { dao.getRemindersByType(it, type) } ?: kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun getReminderById(id: String): ReminderEntity? =
        SessionManager.userId?.let { dao.getReminderById(it, id) }

    override suspend fun getReminderBySourceId(sourceId: String): ReminderEntity? =
        SessionManager.userId?.let { dao.getReminderBySourceId(it, sourceId) }

    override suspend fun insertReminder(reminder: ReminderEntity) {
        dao.insertReminder(reminder.copy(userId = requireUserId()))
    }

    override suspend fun insertReminders(reminders: List<ReminderEntity>) {
        val userId = requireUserId()
        dao.insertReminders(reminders.map { it.copy(userId = userId) })
    }

    override suspend fun updateReminder(reminder: ReminderEntity) {
        val userId = requireUserId()
        if (reminder.userId != userId) throw SecurityException("Reminder does not belong to current user")
        dao.updateReminder(reminder)
    }

    override suspend fun deleteReminder(reminder: ReminderEntity) {
        val userId = requireUserId()
        if (reminder.userId != userId) throw SecurityException("Reminder does not belong to current user")
        dao.deleteReminder(reminder)
    }

    override suspend fun deleteReminderById(id: String) {
        dao.deleteReminderById(requireUserId(), id)
    }

    override suspend fun updateStatus(id: String, status: String) {
        val userId = requireUserId()
        val completedAt = if (status == "COMPLETED") System.currentTimeMillis() else null
        val cancelledAt = if (status == "CANCELLED") System.currentTimeMillis() else null
        dao.updateStatus(userId, id, status, System.currentTimeMillis(), completedAt, cancelledAt)
    }

    override fun getSchedules(reminderId: String): Flow<List<ReminderScheduleEntity>> =
        SessionManager.userId?.let { dao.getSchedulesForReminder(it, reminderId) } ?: kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun getSchedulesSync(reminderId: String): List<ReminderScheduleEntity> =
        dao.getSchedulesForReminderSync(requireUserId(), reminderId)

    override suspend fun insertSchedule(schedule: ReminderScheduleEntity) =
        dao.insertSchedule(schedule)

    override suspend fun insertSchedules(schedules: List<ReminderScheduleEntity>) =
        dao.insertSchedules(schedules)

    override suspend fun updateSchedule(schedule: ReminderScheduleEntity) =
        dao.updateSchedule(schedule)

    override suspend fun deleteSchedule(schedule: ReminderScheduleEntity) =
        dao.deleteSchedule(schedule)

    override suspend fun deleteSchedulesByReminderId(reminderId: String) =
        dao.deleteSchedulesByReminderId(requireUserId(), reminderId)

    override fun getDeliveryLogs(reminderId: String): Flow<List<ReminderDeliveryLogEntity>> =
        SessionManager.userId?.let { dao.getDeliveryLogsForReminder(it, reminderId) } ?: kotlinx.coroutines.flow.flowOf(emptyList())

    override fun getAllDeliveryLogs(): Flow<List<ReminderDeliveryLogEntity>> =
        SessionManager.userId?.let { dao.getAllDeliveryLogs(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun insertDeliveryLog(log: ReminderDeliveryLogEntity) =
        dao.insertDeliveryLog(log)
}
