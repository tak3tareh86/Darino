package com.example.reminder.data

import android.content.Context
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.data.security.SessionState
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import com.example.reminder.domain.ReminderScheduler
import com.example.reminder.domain.ReminderManager
import com.example.reminder.domain.NotificationIdGenerator
import com.example.reminder.domain.ReminderPolicyEngine
import com.example.reminder.domain.RepeatType

interface ReminderRepository {
    fun getAllReminders(): Flow<List<ReminderEntity>>
    fun getAllSchedules(): Flow<List<ReminderScheduleEntity>>
    fun getActiveReminders(): Flow<List<ReminderEntity>>
    fun getTodayReminders(): Flow<List<ReminderEntity>>
    fun getUpcomingReminders(): Flow<List<ReminderEntity>>
    fun getCompletedReminders(): Flow<List<ReminderEntity>>
    fun getMissedReminders(): Flow<List<ReminderEntity>>
    fun getRemindersByType(type: String): Flow<List<ReminderEntity>>
    suspend fun getReminderById(id: String): ReminderEntity?
    suspend fun getReminderBySourceId(sourceId: String): ReminderEntity?
    suspend fun getReminderBySource(sourceType: String, sourceId: String): ReminderEntity?
    suspend fun insertReminder(reminder: ReminderEntity)
    suspend fun insertReminders(reminders: List<ReminderEntity>)
    suspend fun insertReminderWithSchedules(reminder: ReminderEntity, schedules: List<ReminderScheduleEntity>)
    suspend fun updateReminder(reminder: ReminderEntity)
    suspend fun updateReminderWithSchedules(reminder: ReminderEntity, schedules: List<ReminderScheduleEntity>)
    suspend fun deleteReminder(reminder: ReminderEntity)
    suspend fun deleteReminderById(id: String)
    suspend fun deleteReminderWithSchedules(reminderId: String)
    suspend fun deleteCheckReminderWithSchedulesAndLogs(checkId: String)
    suspend fun updateStatus(id: String, status: String)

    fun getSchedules(reminderId: String): Flow<List<ReminderScheduleEntity>>
    suspend fun getSchedulesSync(reminderId: String): List<ReminderScheduleEntity>
    suspend fun getCheckSchedulesSync(reminderId: String): List<ReminderScheduleEntity>
    suspend fun insertSchedule(schedule: ReminderScheduleEntity)
    suspend fun insertSchedules(schedules: List<ReminderScheduleEntity>)
    suspend fun updateSchedule(schedule: ReminderScheduleEntity)
    suspend fun deleteSchedule(schedule: ReminderScheduleEntity)
    suspend fun deleteSchedulesByReminderId(reminderId: String): Int

    fun getDeliveryLogs(reminderId: String): Flow<List<ReminderDeliveryLogEntity>>
    fun getAllDeliveryLogs(): Flow<List<ReminderDeliveryLogEntity>>
    suspend fun insertDeliveryLog(log: ReminderDeliveryLogEntity)
}

private fun SessionState.userIdOrNull(): String? = when (this) {
    is SessionState.Authenticated -> user.id
    is SessionState.PhoneVerificationRequired -> user.id
    else -> null
}

class LocalReminderRepository(context: Context) : ReminderRepository {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
    private val dao = db.smartReminderDao()

    private fun requireUserId(): String =
        SessionManager.userId ?: throw IllegalStateException("Authenticated user is required for reminder access")

    init {
        // Session may not be restored when this repository is first constructed.
        // Observe it so a safely owner-scoped legacy migration is retried after login.
        migrationScope.launch {
            SessionManager.sessionState.collect { state ->
                val userId = state.userIdOrNull() ?: return@collect
                runCatching { retryPendingCheckReminderCleanup(userId) }
                    .onFailure {
                        android.util.Log.e("LocalReminderRepository", "Pending check reminder cleanup recovery failed for the authenticated user.", it)
                    }
                runCatching { migrateLegacyReminders(userId) }
                    .onFailure {
                        android.util.Log.e("LocalReminderRepository", "Legacy reminder migration failed for the authenticated user.", it)
                    }
            }
        }
    }

    /**
     * Replays only check-owned platform cancellations saved in the durable outbox.
     * Failures keep the row for the next authenticated session; no reminder is rescheduled or edited.
     */
    private suspend fun retryPendingCheckReminderCleanup(userId: String) {
        val queueDao = db.checkReminderCleanupQueueDao()
        val legacyScheduler = com.example.data.receiver.ReminderScheduler(appContext)
        val smartScheduler = com.example.reminder.domain.ReminderScheduler(appContext)
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        queueDao.getPendingForUser(userId).forEach { pending ->
            val legacyIds = try {
                org.json.JSONArray(pending.legacyReminderIdsJson).let { json ->
                    (0 until json.length()).map { json.getInt(it) }
                }
            } catch (error: Exception) {
                android.util.Log.e(
                    "LocalReminderRepository",
                    "Invalid legacy cancellation snapshot for deleted check ${pending.checkId}; keeping it for recovery.",
                    error
                )
                return@forEach
            }

            val smartSchedules = try {
                org.json.JSONArray(pending.smartSchedulesJson).let { json ->
                    (0 until json.length()).map { index ->
                        val item = json.getJSONObject(index)
                        ReminderScheduleEntity(
                            id = item.getString("id"),
                            reminderId = pending.smartReminderId
                                ?: throw IllegalStateException("Missing smart reminder ID in cleanup snapshot"),
                            triggerDateTime = item.getLong("triggerDateTime")
                        )
                    }
                }
            } catch (error: Exception) {
                if (pending.smartReminderId != null) {
                    android.util.Log.e(
                        "LocalReminderRepository",
                        "Invalid smart cancellation snapshot for deleted check ${pending.checkId}; keeping it for recovery.",
                        error
                    )
                    return@forEach
                } else {
                    emptyList()
                }
            }

            val errors = mutableListOf<Throwable>()
            legacyIds.forEach { reminderId ->
                runCatching { legacyScheduler.cancel(reminderId) }
                    .onFailure { errors.add(it) }
                runCatching { notificationManager.cancel(reminderId) }
                    .onFailure { errors.add(it) }
            }

            pending.smartReminderId?.let { reminderId ->
                smartSchedules.forEach { schedule ->
                    runCatching { smartScheduler.cancelSchedule(schedule.id) }
                        .onFailure { errors.add(it) }
                }
                runCatching {
                    com.example.reminder.domain.NotificationDispatcher(appContext)
                        .cancelReminderNotifications(reminderId, smartSchedules)
                }.onFailure { errors.add(it) }
            }

            if (errors.isEmpty()) {
                queueDao.delete(pending.userId, pending.checkId)
            } else {
                android.util.Log.w(
                    "LocalReminderRepository",
                    "Pending cleanup for deleted check ${pending.checkId} remains queued for retry.",
                    errors.first()
                )
            }
        }
    }

    private suspend fun migrateLegacyReminders(userId: String) = migrationMutex.withLock {
        val legacyList = db.reminderDao().getAllRemindersForBackup(userId)
            .filter { it.deletedAt == null }
        if (legacyList.isEmpty()) return@withLock

        val existing = dao.getAllRemindersList(userId)
        val existingIds = existing.mapTo(mutableSetOf()) { it.id }
        val existingSources = existing.mapNotNull { reminder ->
            reminder.sourceId?.let { reminder.sourceType.uppercase() to it }
        }.toMutableSet()
        val legacySchedules = db.reminderDao().getAllSchedulesForBackup(userId)
            .groupBy { it.reminderId }
        val now = System.currentTimeMillis()

        val migratedReminders = mutableListOf<ReminderEntity>()
        val migratedSchedules = mutableListOf<ReminderScheduleEntity>()

        legacyList.forEach { legacy ->
            val id = "legacy_reminder_${userId}_${legacy.serverId?.takeIf { it.isNotBlank() } ?: legacy.id}"
            val sourceType = when (legacy.sourceType?.uppercase()) {
                "VEHICLE_SERVICE" -> "VEHICLE"
                null, "" -> "MANUAL"
                else -> legacy.sourceType.uppercase()
            }
            val sourceId = legacy.sourceId?.toString()
            val sourceKey = sourceId?.let { sourceType to it }
            if (id in existingIds || (sourceKey != null && sourceKey in existingSources)) return@forEach

            val status = when {
                legacy.completedAt != null -> "COMPLETED"
                !legacy.enabled -> "DISABLED"
                else -> "ACTIVE"
            }
            val hasPersistedPendingDelivery = legacySchedules[legacy.id].orEmpty().any {
                (it.status.equals("PENDING", true) || it.status.equals("SCHEDULED", true)) &&
                    it.status != "CANCELLED"
            }
            val reminder = ReminderEntity(
                id = id,
                userId = userId,
                title = legacy.title,
                description = legacy.description,
                type = legacy.type,
                sourceType = sourceType,
                sourceId = sourceId,
                status = status,
                date = PersianCalendarHelper.fromEpochMillis(legacy.scheduledDateTime).toFormattedDate(),
                time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(legacy.scheduledDateTime)),
                notificationEnabled = legacy.notificationEnabled && hasPersistedPendingDelivery,
                fullScreenAlarmEnabled = false,
                smsEnabled = false,
                phoneNumber = null,
                createdAt = legacy.createdAt,
                updatedAt = legacy.updatedAt,
                completedAt = legacy.completedAt
            )
            migratedReminders += reminder
            if (sourceKey != null) existingSources += sourceKey

            val oldSchedules = legacySchedules[legacy.id].orEmpty()
                .filterNot { it.status.equals("CANCELLED", true) }
            val repeatType = RepeatType.entries.firstOrNull {
                it.name.equals(legacy.repeatRule ?: "NONE", ignoreCase = true)
            } ?: RepeatType.NONE
            val schedulesForReminder = if (oldSchedules.isNotEmpty()) {
                oldSchedules.map { old ->
                    ReminderScheduleEntity(
                        id = "legacy_schedule_${old.id}",
                        reminderId = id,
                        triggerType = "EXACT",
                        offsetValue = 0,
                        offsetUnit = "MINUTE",
                        triggerDateTime = old.triggerDateTime,
                        repeatType = repeatType.name,
                        enabled = status == "ACTIVE" && legacy.notificationEnabled &&
                            old.triggerDateTime > now &&
                            (old.status.equals("PENDING", true) || old.status.equals("SCHEDULED", true)),
                        createdAt = legacy.createdAt
                    )
                }
            } else {
                // Never invent a delivery time from the old reminder row alone.
                // Only persisted PENDING/SCHEDULED legacy triggers prove opt-in.
                emptyList()
            }
            migratedSchedules += schedulesForReminder
        }

        if (migratedReminders.isNotEmpty()) {
            db.withTransaction {
                dao.insertReminders(migratedReminders)
                if (migratedSchedules.isNotEmpty()) dao.insertSchedules(migratedSchedules)
            }
            // Rebuild is user-scoped and respects both the global alert mode and each
            // migrated reminder's explicit notification opt-in.
            runCatching { ReminderScheduler(appContext).rebuildAllSchedules() }
                .onFailure {
                    android.util.Log.e("LocalReminderRepository", "Unable to arm migrated reminder schedules.", it)
                }
        }
    }

    companion object {
        private val migrationMutex = Mutex()
        private val migrationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Serializes check-specific legacy cleanup with the one-time legacy import.
         * This does not change migration behavior for any other reminder source.
         */
        suspend fun <T> withLegacyMigrationLock(block: suspend () -> T): T {
            migrationMutex.lock()
            try {
                return block()
            } finally {
                migrationMutex.unlock()
            }
        }
    }

    override fun getAllReminders(): Flow<List<ReminderEntity>> =
        SessionManager.sessionState.flatMapLatest { state -> state.userIdOrNull()?.let { dao.getAllReminders(it) } ?: flowOf(emptyList()) }

    override fun getAllSchedules(): Flow<List<ReminderScheduleEntity>> =
        SessionManager.sessionState.flatMapLatest { state -> state.userIdOrNull()?.let { dao.getAllSchedules(it) } ?: flowOf(emptyList()) }

    override fun getActiveReminders(): Flow<List<ReminderEntity>> =
        SessionManager.sessionState.flatMapLatest { state -> state.userIdOrNull()?.let { dao.getActiveReminders(it) } ?: flowOf(emptyList()) }

    override fun getTodayReminders(): Flow<List<ReminderEntity>> {
        val todayPersian = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
        return getActiveReminders().map { list -> list.filter { it.date == todayPersian } }
    }

    override fun getUpcomingReminders(): Flow<List<ReminderEntity>> =
        getActiveReminders().map { list ->
            ReminderPolicyEngine.upcomingByDateTime(list, System.currentTimeMillis()) { date, time ->
                ReminderManager.parsePersianDateTimeToMillis(date, time)
            }
        }

    override fun getCompletedReminders(): Flow<List<ReminderEntity>> =
        SessionManager.sessionState.flatMapLatest { state -> state.userIdOrNull()?.let { dao.getCompletedReminders(it) } ?: flowOf(emptyList()) }

    override fun getMissedReminders(): Flow<List<ReminderEntity>> =
        SessionManager.sessionState.flatMapLatest { state -> state.userIdOrNull()?.let { dao.getMissedReminders(it) } ?: flowOf(emptyList()) }

    override fun getRemindersByType(type: String): Flow<List<ReminderEntity>> =
        SessionManager.sessionState.flatMapLatest { state -> state.userIdOrNull()?.let { dao.getRemindersByType(it, type) } ?: flowOf(emptyList()) }

    override suspend fun getReminderById(id: String): ReminderEntity? =
        SessionManager.userId?.let { dao.getReminderById(it, id) }

    override suspend fun getReminderBySourceId(sourceId: String): ReminderEntity? =
        SessionManager.userId?.let { dao.getReminderBySourceId(it, sourceId) }

    override suspend fun getReminderBySource(sourceType: String, sourceId: String): ReminderEntity? =
        SessionManager.userId?.let { dao.getReminderBySource(it, sourceType, sourceId) }

    override suspend fun insertReminder(reminder: ReminderEntity) {
        dao.insertReminder(reminder.copy(userId = requireUserId()))
    }

    override suspend fun insertReminders(reminders: List<ReminderEntity>) {
        val userId = requireUserId()
        dao.insertReminders(reminders.map { it.copy(userId = userId) })
    }

    override suspend fun insertReminderWithSchedules(reminder: ReminderEntity, schedules: List<ReminderScheduleEntity>) {
        val userId = requireUserId()
        db.withTransaction {
            dao.insertReminder(reminder.copy(userId = userId))
            dao.insertSchedules(schedules.map { it.copy(reminderId = reminder.id) })
        }
    }

    override suspend fun updateReminder(reminder: ReminderEntity) {
        val userId = requireUserId()
        if (reminder.userId != userId) throw SecurityException("Reminder does not belong to current user")
        if (dao.updateReminder(reminder) != 1) {
            throw NoSuchElementException("یادآور موردنظر برای ویرایش پیدا نشد.")
        }
    }

    override suspend fun updateReminderWithSchedules(reminder: ReminderEntity, schedules: List<ReminderScheduleEntity>) {
        val userId = requireUserId()
        if (reminder.userId != userId) throw SecurityException("Reminder does not belong to current user")
        db.withTransaction {
            if (dao.updateReminder(reminder) != 1) {
                throw NoSuchElementException("یادآور موردنظر برای به‌روزرسانی پیدا نشد.")
            }
            dao.deleteSchedulesByReminderId(userId, reminder.id)
            dao.insertSchedules(schedules.map { it.copy(reminderId = reminder.id) })
        }
    }

    override suspend fun deleteReminder(reminder: ReminderEntity) {
        val userId = requireUserId()
        if (reminder.userId != userId) throw SecurityException("Reminder does not belong to current user")
        dao.deleteReminder(reminder)
    }

    override suspend fun deleteReminderById(id: String) {
        dao.deleteReminderById(requireUserId(), id)
    }

    override suspend fun deleteReminderWithSchedules(reminderId: String) {
        val userId = requireUserId()
        db.withTransaction {
            dao.deleteSchedulesByReminderId(userId, reminderId)
            dao.deleteReminderById(userId, reminderId)
        }
    }

    /**
     * Check-only cleanup: remove its notification-center entries, schedules,
     * delivery logs, and reminder atomically. Generic reminder deletion
     * intentionally keeps its existing behavior.
     */
    override suspend fun deleteCheckReminderWithSchedulesAndLogs(checkId: String) {
        val userId = requireUserId()
        val numericCheckId = checkId.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid check ID for reminder cleanup")
        val legacyReminders = db.reminderDao().getCheckRemindersForCheck(userId, numericCheckId)
        val smartReminder = dao.getCheckReminderBySource(userId, checkId)

        // Persist every OS cancellation identifier in the same transaction as check/reminder deletion.
        // If the process dies immediately after commit, session startup can replay this check-only outbox.
        db.withTransaction {
            val smartSchedules = smartReminder?.let {
                dao.getCheckSchedulesForReminderSync(userId, it.id)
            }.orEmpty()
            if (legacyReminders.isNotEmpty() || smartReminder != null) {
                db.checkReminderCleanupQueueDao().upsert(
                    com.example.data.database.CheckReminderCleanupQueueEntity(
                        userId = userId,
                        checkId = checkId,
                        smartReminderId = smartReminder?.id,
                        smartSchedulesJson = org.json.JSONArray().apply {
                            smartSchedules.forEach { schedule ->
                                put(
                                    org.json.JSONObject()
                                        .put("id", schedule.id)
                                        .put("triggerDateTime", schedule.triggerDateTime)
                                )
                            }
                        }.toString(),
                        legacyReminderIdsJson = org.json.JSONArray().apply {
                            legacyReminders.forEach { put(it.id) }
                        }.toString(),
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            legacyReminders.forEach { legacy ->
                db.reminderDao().softDeleteReminder(userId, legacy.id)
                db.reminderDao().deleteSchedulesForReminder(userId, legacy.id)
                db.notificationLogDao().deleteLegacyReminderNotifications(userId, legacy.id)
            }
            if (smartReminder != null) {
                val deliveryLogs = dao.getDeliveryLogsForReminderSync(userId, smartReminder.id)
                val notificationIds = deliveryLogs.map { log ->
                    NotificationIdGenerator.generateNotificationId(
                        reminderId = smartReminder.id,
                        scheduleId = log.scheduleId,
                        triggerTime = log.scheduledAt
                    )
                }.distinct()
                if (notificationIds.isNotEmpty()) {
                    db.notificationLogDao().deleteSmartReminderNotifications(userId, notificationIds)
                }
                dao.deleteCheckSchedulesByReminderId(userId, smartReminder.id)
                dao.deleteDeliveryLogsByReminderId(smartReminder.id)
                dao.deleteCheckReminderById(userId, smartReminder.id)
            }
        }
    }

    override suspend fun updateStatus(id: String, status: String) {
        val userId = requireUserId()
        val completedAt = if (status == "COMPLETED") System.currentTimeMillis() else null
        val cancelledAt = if (status == "CANCELLED") System.currentTimeMillis() else null
        dao.updateStatus(userId, id, status, System.currentTimeMillis(), completedAt, cancelledAt)
    }

    override fun getSchedules(reminderId: String): Flow<List<ReminderScheduleEntity>> =
        SessionManager.sessionState.flatMapLatest { state -> state.userIdOrNull()?.let { dao.getSchedulesForReminder(it, reminderId) } ?: flowOf(emptyList()) }

    override suspend fun getSchedulesSync(reminderId: String): List<ReminderScheduleEntity> =
        dao.getSchedulesForReminderSync(requireUserId(), reminderId)

    override suspend fun getCheckSchedulesSync(reminderId: String): List<ReminderScheduleEntity> =
        dao.getCheckSchedulesForReminderSync(requireUserId(), reminderId)

    override suspend fun insertSchedule(schedule: ReminderScheduleEntity) {
        val userId = requireUserId()
        dao.getReminderById(userId, schedule.reminderId)
            ?: throw SecurityException("Reminder does not belong to current user")
        dao.insertSchedule(schedule)
    }

    override suspend fun insertSchedules(schedules: List<ReminderScheduleEntity>) {
        val userId = requireUserId()
        schedules.forEach { schedule ->
            dao.getReminderById(userId, schedule.reminderId)
                ?: throw SecurityException("Reminder does not belong to current user")
        }
        dao.insertSchedules(schedules)
    }

    override suspend fun updateSchedule(schedule: ReminderScheduleEntity) {
        val userId = requireUserId()
        dao.getReminderById(userId, schedule.reminderId)
            ?: throw SecurityException("Reminder does not belong to current user")
        dao.updateSchedule(schedule)
    }

    override suspend fun deleteSchedule(schedule: ReminderScheduleEntity) {
        val userId = requireUserId()
        dao.getReminderById(userId, schedule.reminderId)
            ?: throw SecurityException("Reminder does not belong to current user")
        dao.deleteSchedule(schedule)
    }

    override suspend fun deleteSchedulesByReminderId(reminderId: String) =
        dao.deleteSchedulesByReminderId(requireUserId(), reminderId)

    override fun getDeliveryLogs(reminderId: String): Flow<List<ReminderDeliveryLogEntity>> =
        SessionManager.sessionState.flatMapLatest { state -> state.userIdOrNull()?.let { dao.getDeliveryLogsForReminder(it, reminderId) } ?: flowOf(emptyList()) }

    override fun getAllDeliveryLogs(): Flow<List<ReminderDeliveryLogEntity>> =
        SessionManager.sessionState.flatMapLatest { state -> state.userIdOrNull()?.let { dao.getAllDeliveryLogs(it) } ?: flowOf(emptyList()) }

    override suspend fun insertDeliveryLog(log: ReminderDeliveryLogEntity) {
        val userId = requireUserId()
        dao.getReminderById(userId, log.reminderId)
            ?: throw SecurityException("Reminder does not belong to current user")
        dao.insertDeliveryLog(log)
    }
}
