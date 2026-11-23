package com.example.reminder.domain

import android.content.Context
import android.app.NotificationManager
import androidx.room.withTransaction
import android.util.Log
import com.example.reminder.data.LocalReminderRepository
import com.example.reminder.data.ReminderEntity
import com.example.reminder.data.ReminderRepository
import com.example.reminder.data.ReminderScheduleEntity
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.util.MoneyFormatter
import com.example.util.MoneyAmount
import com.example.ui.screens.installments.data.LocalInstallmentRepository
import com.example.ui.screens.installments.model.InstallmentStatus
import com.example.ui.screens.settings.model.AppCurrency
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap
import java.util.Calendar
import java.util.UUID

private val reminderSnoozeLocks = ConcurrentHashMap<String, Mutex>()

class ReminderManager(
    private val context: Context,
    private val repository: ReminderRepository = LocalReminderRepository(context),
    private val scheduler: ReminderScheduler = ReminderScheduler(context)
) {

    private fun defaultReminderTime(): String =
        AppPreferencesRepository.getInstance(context).defaultReminderTime()

    /**
     * Cancels temporary snooze PendingIntents before removing their database rows.
     * The original reminder schedules are returned unchanged.
     */
    private suspend fun removeSnoozeSchedules(
        reminder: ReminderEntity,
        schedules: List<ReminderScheduleEntity>
    ): List<ReminderScheduleEntity> {
        val snoozeSchedules = schedules.filter { it.id.startsWith("snooze_") }
        if (snoozeSchedules.isEmpty()) return schedules

        snoozeSchedules.forEach { scheduler.cancelSchedule(it.id) }
        val baseSchedules = schedules.filterNot { it.id.startsWith("snooze_") }
        repository.updateReminderWithSchedules(reminder, baseSchedules)
        return baseSchedules
    }

    /**
     * Calculates installment offsets as calendar units in the device timezone.
     * This preserves the selected wall-clock time across daylight-saving changes.
     * Other reminder types keep their existing elapsed-millis behavior.
     */
    private fun calculateOffsetTriggerMillis(
        baseMillis: Long,
        offset: PredefinedOffset,
        calendarBasedOffsets: Boolean
    ): Long {
        if (!calendarBasedOffsets) {
            return baseMillis - (offset.value * offset.unit.millisMultiplier)
        }

        val calendar = Calendar.getInstance().apply { timeInMillis = baseMillis }
        when (offset.unit) {
            OffsetUnit.MINUTE -> calendar.add(Calendar.MINUTE, -offset.value)
            OffsetUnit.HOUR -> calendar.add(Calendar.HOUR_OF_DAY, -offset.value)
            OffsetUnit.DAY -> calendar.add(Calendar.DAY_OF_MONTH, -offset.value)
            OffsetUnit.WEEK -> calendar.add(Calendar.DAY_OF_MONTH, -7 * offset.value)
            OffsetUnit.MONTH -> calendar.add(Calendar.MONTH, -offset.value)
        }
        return calendar.timeInMillis
    }

    /**
     * Creates a new reminder along with its multi-schedule triggers, and registers alarms.
     */
    suspend fun createReminder(
        reminder: ReminderEntity,
        offsets: List<PredefinedOffset> = emptyList(),
        repeatType: RepeatType = RepeatType.NONE,
        repeatInterval: Int = 1,
        calendarBasedOffsets: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        // Validate recurring schedule specifications at the domain boundary as well as in the UI.
        if (repeatType != RepeatType.NONE && !RecurrenceEngine.validateRecurrence(repeatType, repeatInterval)) {
            throw IllegalArgumentException("بازه تکرار باید عددی بین ۱ تا ۳۶۵ باشد.")
        }
        if (reminder.sourceType.equals("MANUAL", ignoreCase = true) &&
            offsets.isEmpty() && repeatType != RepeatType.NONE
        ) {
            throw IllegalArgumentException("برای تکرار یادآوری، ابتدا باید حداقل یک زمان هشدار انتخاب شود.")
        }

        // Parse and validate reminder date & time before writing anything to Room.
        val baseMillis = parsePersianDateTimeToMillis(reminder.date, reminder.time)
        if (reminder.sourceType.equals("MANUAL", ignoreCase = true) && offsets.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val pastOffsets = offsets.filter { calculateOffsetTriggerMillis(baseMillis, it, calendarBasedOffsets) <= now }
            if (pastOffsets.isNotEmpty()) {
                throw IllegalArgumentException(
                    "زمان هشدار ${pastOffsets.joinToString("، ") { it.title }} گذشته است؛ تاریخ سررسید یا زمان‌های هشدار را اصلاح کنید."
                )
            }
        }

        // Generate schedule entities for each selected offset
        val schedules = offsets.map { offset ->
            val triggerMillis = calculateOffsetTriggerMillis(baseMillis, offset, calendarBasedOffsets)
            ReminderScheduleEntity(
                id = UUID.randomUUID().toString(),
                reminderId = reminder.id,
                triggerType = if (offset == PredefinedOffset.AT_TIME) "EXACT" else "BEFORE_EVENT",
                offsetValue = offset.value,
                offsetUnit = offset.unit.name,
                triggerDateTime = triggerMillis,
                repeatType = repeatType.name,
                repeatInterval = repeatInterval,
                enabled = true,
                createdAt = System.currentTimeMillis()
            )
        }

        repository.insertReminderWithSchedules(reminder, schedules)
        val scheduledCount = try {
            scheduler.schedule(reminder, schedules).also { count ->
                if (reminder.sourceType.equals("CHECK", ignoreCase = true) && schedules.any { it.enabled } && count == 0) {
                    throw IllegalStateException("هیچ آلارمی ثبت نشد؛ تنظیمات اعلان و مجوز آلارم دقیق را بررسی کنید.")
                }
            }
        } catch (e: Exception) {
            // The add form remains open on failure. Roll back this new draft so a retry
            // cannot create a duplicate reminder with no working alarm.
            Log.e("ReminderManager", "Alarm registration failed for new reminder ${reminder.id}; rolling back the saved draft.", e)
            runCatching {
                scheduler.cancel(reminder.id, schedules)
                NotificationDispatcher(context).cancelReminderNotifications(reminder.id, schedules)
                repository.deleteReminderWithSchedules(reminder.id)
            }.onFailure { rollbackError ->
                e.addSuppressed(rollbackError)
                Log.e("ReminderManager", "Failed to roll back reminder draft ${reminder.id}", rollbackError)
            }
            throw IllegalStateException(
                "ثبت هشدار ناموفق بود و فرم باز نگه داشته شد. مجوز آلارم دقیق و زمان‌های انتخابی را بررسی کنید. جزئیات: ${e.message ?: "خطای نامشخص"}",
                e
            )
        }
        Log.i("ReminderManager", "Created reminder ${reminder.title} with $scheduledCount future alarms scheduled.")
        scheduledCount
    }

    /**
     * Updates an existing reminder and regenerates its schedules.
     */
    suspend fun updateReminder(
        reminder: ReminderEntity,
        offsets: List<PredefinedOffset> = emptyList(),
        repeatType: RepeatType = RepeatType.NONE,
        repeatInterval: Int = 1,
        calendarBasedOffsets: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        if (repeatType != RepeatType.NONE && !RecurrenceEngine.validateRecurrence(repeatType, repeatInterval)) {
            throw IllegalArgumentException("بازه تکرار باید عددی بین ۱ تا ۳۶۵ باشد.")
        }
        val baseMillis = parsePersianDateTimeToMillis(reminder.date, reminder.time)
        if (reminder.sourceType.equals("MANUAL", ignoreCase = true) && offsets.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val pastOffsets = offsets.filter { calculateOffsetTriggerMillis(baseMillis, it, calendarBasedOffsets) <= now }
            if (pastOffsets.isNotEmpty()) {
                throw IllegalArgumentException(
                    "زمان هشدار ${pastOffsets.joinToString("، ") { it.title }} گذشته است؛ تاریخ سررسید یا زمان‌های هشدار را اصلاح کنید."
                )
            }
        }

        val existingReminder = repository.getReminderById(reminder.id)
            ?: throw NoSuchElementException("یادآور موردنظر برای ویرایش پیدا نشد.")
        if (existingReminder.userId != reminder.userId) {
            throw SecurityException("یادآور متعلق به کاربر فعلی نیست.")
        }
        val existingSchedules = repository.getSchedulesSync(reminder.id)
        scheduler.cancel(reminder.id, existingSchedules)
        NotificationDispatcher(context).cancelReminderNotifications(reminder.id, existingSchedules)

        val newSchedules = offsets.map { offset ->
            val triggerMillis = calculateOffsetTriggerMillis(baseMillis, offset, calendarBasedOffsets)
            ReminderScheduleEntity(
                id = UUID.randomUUID().toString(),
                reminderId = reminder.id,
                triggerType = if (offset == PredefinedOffset.AT_TIME) "EXACT" else "BEFORE_EVENT",
                offsetValue = offset.value,
                offsetUnit = offset.unit.name,
                triggerDateTime = triggerMillis,
                repeatType = repeatType.name,
                repeatInterval = repeatInterval,
                enabled = true,
                createdAt = System.currentTimeMillis()
            )
        }

        val scheduledCount = try {
            // Persist and arm as one recoverable operation. If either step fails,
            // restore the exact previous reminder and schedule rows.
            repository.updateReminderWithSchedules(reminder, newSchedules)
            scheduler.schedule(reminder, newSchedules).also { count ->
                if (reminder.sourceType.equals("CHECK", ignoreCase = true) &&
                    newSchedules.any { it.enabled } && count == 0
                ) {
                    throw IllegalStateException("هیچ آلارمی ثبت نشد؛ تنظیمات اعلان و مجوز آلارم دقیق را بررسی کنید.")
                }
            }
        } catch (e: Exception) {
            Log.e("ReminderManager", "Failed to update or schedule reminder ${reminder.id}; restoring previous state.", e)
            try {
                repository.updateReminderWithSchedules(existingReminder, existingSchedules)
                scheduler.cancel(reminder.id, newSchedules)
                scheduler.schedule(existingReminder, existingSchedules)
            } catch (rollbackEx: Exception) {
                e.addSuppressed(rollbackEx)
                Log.e("ReminderManager", "Failed to restore previous reminder during update rollback", rollbackEx)
            }
            throw e
        }
        Log.i("ReminderManager", "Updated reminder ${reminder.title} and rescheduled with $scheduledCount future alarms.")
        scheduledCount
    }

    /**
     * Deletes only the reminder linked to the specified installment.
     * Uses the standard deletion path so its exact PendingIntents and notifications are cancelled.
     */
    suspend fun deleteInstallmentReminder(installmentId: String) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderBySource("INSTALLMENT", installmentId)
            ?: return@withContext
        deleteReminder(reminder.id)
    }

    /**
     * Deletes a reminder and cancels its scheduled alarms.
     */
    suspend fun deleteReminder(reminderId: String) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderById(reminderId)
        val effectiveReminderId = reminder?.id ?: reminderId
        val schedules = repository.getSchedulesSync(reminderId)
        scheduler.cancel(effectiveReminderId, schedules)
        NotificationDispatcher(context).cancelReminderNotifications(effectiveReminderId, schedules)
        repository.deleteReminderWithSchedules(reminderId)
        if (reminder?.sourceType.equals("CALENDAR", ignoreCase = true)) {
            reminder?.sourceId?.let { eventId ->
                com.example.calendar.reminder.CalendarEventReminderScheduler(context).cancel(eventId)
                SessionManager.userId?.let { userId ->
                    AppDatabase.getDatabase(context).financialEventDao().deleteEventById(userId, eventId)
                }
            }
        }
        Log.i("ReminderManager", "Deleted reminder $reminderId and all its schedules.")
    }

    /**
     * Completes only the selected payment's reminder occurrence.
     * The loan-level reminder remains active and later unpaid payments keep
     * their configured schedules.
     */
    suspend fun completeInstallmentReminderOccurrence(
        reminderId: String,
        paymentId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderById(reminderId) ?: return@withContext false
        if (!reminder.sourceType.equals("INSTALLMENT", ignoreCase = true) ||
            reminder.sourceId.isNullOrBlank()
        ) return@withContext false

        val oldSchedules = repository.getSchedulesSync(reminderId)
        val completedScheduleIds = oldSchedules.filter { schedule ->
            val expectedId = UUID.nameUUIDFromBytes(
                "installment:${reminder.sourceId}:${paymentId}:${schedule.offsetValue}:${schedule.offsetUnit.uppercase()}"
                    .toByteArray(Charsets.UTF_8)
            ).toString()
            schedule.id == expectedId
        }.mapTo(mutableSetOf()) { it.id }

        if (completedScheduleIds.isEmpty()) return@withContext false

        val completedAt = System.currentTimeMillis()
        val updatedSchedules = oldSchedules.map { schedule ->
            if (schedule.id in completedScheduleIds) {
                // COMPLETED is a durable per-payment marker and its timestamp is
                // retained when the loan's remaining schedules are synchronized.
                schedule.copy(enabled = false, triggerType = "COMPLETED", createdAt = completedAt)
            } else schedule
        }
        // Disable the loan-level reminder only when no unpaid payment still has
        // an unfinished reminder occurrence. Payment status itself is never changed.
        val installmentRepository = LocalInstallmentRepository.instance
        installmentRepository.init(context)
        val installmentDataReady = withTimeoutOrNull(5_000L) {
            installmentRepository.installmentDataAvailable.first { it }
        } ?: false
        val installment = if (installmentDataReady) {
            installmentRepository.installments.value.firstOrNull { it.id == reminder.sourceId }
        } else null
        val hasUnfinishedPaymentReminder = installment?.paymentHistory
            ?.filter {
                it.status != InstallmentStatus.PAID &&
                    it.status != InstallmentStatus.COMPLETED
            }
            ?.any { payment ->
                val paymentSchedules = updatedSchedules.filter { schedule ->
                    val expectedId = UUID.nameUUIDFromBytes(
                        "installment:${reminder.sourceId}:${payment.id}:${schedule.offsetValue}:${schedule.offsetUnit.uppercase()}"
                            .toByteArray(Charsets.UTF_8)
                    ).toString()
                    schedule.id == expectedId
                }
                paymentSchedules.any { !it.triggerType.equals("COMPLETED", ignoreCase = true) }
            } ?: true

        val allPaymentRemindersFinished = installment != null && !hasUnfinishedPaymentReminder
        val updatedReminder = reminder.copy(
            status = if (allPaymentRemindersFinished) "DISABLED" else reminder.status,
            updatedAt = System.currentTimeMillis()
        )
        val schedulesToPersist = if (allPaymentRemindersFinished) {
            updatedSchedules.map { it.copy(enabled = false) }
        } else {
            updatedSchedules
        }

        // Cancel old PendingIntents before replacing the rows, then re-arm only
        // remaining future payment schedules while any unfinished occurrence remains.
        scheduler.cancel(reminder.id, oldSchedules)
        NotificationDispatcher(context).cancelReminderNotifications(reminder.id, oldSchedules)
        try {
            repository.updateReminderWithSchedules(updatedReminder, schedulesToPersist)
            val remainingFutureSchedules = schedulesToPersist.filter {
                it.enabled && it.triggerDateTime > System.currentTimeMillis()
            }
            if (remainingFutureSchedules.isNotEmpty()) {
                scheduler.schedule(updatedReminder, remainingFutureSchedules)
            }
            true
        } catch (e: Exception) {
            Log.e("ReminderManager", "Could not complete installment reminder occurrence $paymentId; restoring schedules.", e)
            runCatching {
                repository.updateReminderWithSchedules(reminder, oldSchedules)
                val restorable = oldSchedules.filter {
                    it.enabled && it.triggerDateTime > System.currentTimeMillis()
                }
                if (restorable.isNotEmpty()) scheduler.schedule(reminder, restorable)
            }.onFailure { rollbackError ->
                e.addSuppressed(rollbackError)
                Log.e("ReminderManager", "Failed to restore installment reminder schedules after completion error.", rollbackError)
            }
            throw e
        }
    }

    /**
     * Completes a single reminder action without allowing installment reminders
     * to fall through to whole-loan completion when the payment ID is absent.
     */
    suspend fun completeReminderAction(
        reminderId: String,
        installmentPaymentId: String?
    ): Boolean {
        val reminder = repository.getReminderById(reminderId) ?: return false
        val isInstallment = reminder.sourceType.equals("INSTALLMENT", ignoreCase = true) ||
            reminder.type.equals("INSTALLMENT", ignoreCase = true)

        return if (isInstallment) {
            val paymentId = installmentPaymentId?.takeIf { it.isNotBlank() }
                ?: return false
            completeInstallmentReminderOccurrence(reminderId, paymentId)
        } else {
            completeReminder(reminderId)
            true
        }
    }

    /**
     * Marks a reminder as completed.
     */
    suspend fun completeReminder(reminderId: String) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderById(reminderId)
        val effectiveReminderId = reminder?.id ?: reminderId
        val schedules = repository.getSchedulesSync(reminderId)
        if (reminder != null) {
            repository.updateReminderWithSchedules(
                reminder.copy(status = "COMPLETED", completedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()),
                schedules.map { it.copy(enabled = false) }
            )
        } else {
            repository.updateStatus(reminderId, "COMPLETED")
        }
        scheduler.cancel(effectiveReminderId, schedules)
        NotificationDispatcher(context).cancelReminderNotifications(effectiveReminderId, schedules)
        if (reminder?.sourceType.equals("CALENDAR", ignoreCase = true)) {
            reminder?.sourceId?.let { eventId ->
                SessionManager.userId?.let { userId ->
                    AppDatabase.getDatabase(context).financialEventDao().updateEventStatus(userId, eventId, "PAID")
                }
                com.example.calendar.reminder.CalendarEventReminderScheduler(context).cancel(eventId)
            }
        }
        Log.i("ReminderManager", "Completed reminder $reminderId.")
    }

    /**
     * Cancels a reminder.
     */
    suspend fun cancelReminder(reminderId: String) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderById(reminderId)
        val effectiveReminderId = reminder?.id ?: reminderId
        val schedules = repository.getSchedulesSync(reminderId)
        if (reminder != null) {
            repository.updateReminderWithSchedules(
                reminder.copy(status = "CANCELLED", cancelledAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()),
                schedules.map { it.copy(enabled = false) }
            )
        } else {
            repository.updateStatus(reminderId, "CANCELLED")
        }
        scheduler.cancel(effectiveReminderId, schedules)
        NotificationDispatcher(context).cancelReminderNotifications(effectiveReminderId, schedules)
        if (reminder?.sourceType.equals("CALENDAR", ignoreCase = true)) {
            reminder?.sourceId?.let { eventId ->
                SessionManager.userId?.let { userId ->
                    AppDatabase.getDatabase(context).financialEventDao().updateEventStatus(userId, eventId, "PAID")
                }
                com.example.calendar.reminder.CalendarEventReminderScheduler(context).cancel(eventId)
            }
        }
    }

    /**
     * Enables a reminder and re-arms its future schedules.
     */
    suspend fun enableReminder(reminderId: String) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderById(reminderId)
            ?: throw NoSuchElementException("یادآور با شناسه $reminderId یافت نشد.")
        val storedSchedules = repository.getSchedulesSync(reminderId)
        val baseSchedules = removeSnoozeSchedules(reminder, storedSchedules)
        if (baseSchedules.isEmpty()) {
            val reminderHasExplicitDelivery = reminder.notificationEnabled || reminder.fullScreenAlarmEnabled
            val isVehicleReminder = reminder.sourceType.equals("VEHICLE", ignoreCase = true)
            if (!reminderHasExplicitDelivery && !isVehicleReminder) {
                // A reminder intentionally saved without alerts can still be reactivated
                // as a visible list item; it must not acquire an implicit alarm.
                repository.updateStatus(reminderId, "ACTIVE")
                return@withContext
            }
            if (reminder.sourceType.equals("CALENDAR", ignoreCase = true)) {
                repository.updateStatus(reminderId, "ACTIVE")
                val eventId = reminder.sourceId
                val userId = SessionManager.userId
                if (eventId != null && userId != null) {
                    val event = AppDatabase.getDatabase(context).financialEventDao().getEventById(userId, eventId)
                    if (event != null && scheduler.hasConfiguredDelivery(reminder)) {
                        com.example.calendar.reminder.CalendarEventReminderScheduler(context).schedule(event.toDomain())
                    }
                }
                return@withContext
            }
            repository.updateStatus(reminderId, "DISABLED")
            throw IllegalStateException("زمان‌های هشدار این یادآور ذخیره نشده‌اند؛ برای جلوگیری از ساخت هشدار ناقص، ابتدا آن را ویرایش کنید.")
        }

        // Vehicle reminders use the app-wide delivery mode; other categories keep
        // their existing per-reminder delivery opt-in requirement.
        if (!reminder.sourceType.equals("VEHICLE", ignoreCase = true) &&
            !reminder.notificationEnabled && !reminder.fullScreenAlarmEnabled
        ) {
            repository.updateStatus(reminderId, "ACTIVE")
            return@withContext
        }
        val now = System.currentTimeMillis()
        val enabledReminder = reminder.copy(status = "ACTIVE", updatedAt = now)
        // A 7-day offset may already be past while shorter selected offsets are still valid.
        val enabledSchedules = baseSchedules.map { it.copy(enabled = it.triggerDateTime > now) }
        if (enabledSchedules.none { it.enabled }) {
            repository.updateReminderWithSchedules(
                enabledReminder.copy(status = "DISABLED"),
                enabledSchedules
            )
            throw IllegalStateException("تمام زمان‌های انتخاب‌شده گذشته‌اند؛ تاریخ سررسید یا ساعت اعلان را اصلاح کنید.")
        }
        // Keep the reminder active and its future schedules enabled even when the
        // app-wide delivery preference is NONE. A later global opt-in rebuilds these
        // persisted schedules; lack of a delivery channel is not an alarm failure.
        if (!scheduler.hasConfiguredDelivery(enabledReminder)) {
            repository.updateReminderWithSchedules(enabledReminder, enabledSchedules)
            Log.i(
                "ReminderManager",
                "Reminder $reminderId remains active with saved schedules; global delivery is currently disabled."
            )
            return@withContext
        }

        repository.updateReminderWithSchedules(enabledReminder, enabledSchedules)
        try {
            val scheduledCount = scheduler.schedule(enabledReminder, enabledSchedules)
            if (scheduledCount <= 0) {
                throw IllegalStateException("هیچ هشدار آینده‌ای در سیستم ثبت نشد. تاریخ یا مجوز آلارم دقیق را بررسی کنید.")
            }
            repository.updateStatus(reminderId, "ACTIVE")
        } catch (e: Exception) {
            Log.e("ReminderManager", "Failed to schedule alarms when enabling reminder $reminderId. Rolling back to disabled state.", e)
            try {
                val disabledSchedules = enabledSchedules.map { it.copy(enabled = false) }
                repository.updateReminderWithSchedules(enabledReminder.copy(status = "DISABLED"), disabledSchedules)
                scheduler.cancel(reminder.id, enabledSchedules)
                NotificationDispatcher(context).cancelReminderNotifications(reminder.id, enabledSchedules)
            } catch (rollbackEx: Exception) {
                Log.e("ReminderManager", "Failed to rollback reminder to DISABLED", rollbackEx)
            }
            throw e
        }
    }

    /**
     * Restores a completed/disabled reminder without silently enabling alert channels.
     * If the user had explicitly configured a delivery channel, its saved schedules are re-armed.
     */
    suspend fun restoreReminder(reminderId: String) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderById(reminderId)
            ?: throw NoSuchElementException("یادآور با شناسه $reminderId یافت نشد.")
        val storedSchedules = repository.getSchedulesSync(reminderId)
        // A completed reminder must lose its completion/cancellation timestamps when
        // restored; otherwise an ACTIVE reminder would retain contradictory lifecycle data.
        val reopenedReminder = reminder.copy(
            status = "ACTIVE",
            completedAt = null,
            cancelledAt = null,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateReminder(reopenedReminder)

        // Cancel temporary alarms before restoring only the original schedule series.
        val baseSchedules = removeSnoozeSchedules(reopenedReminder, storedSchedules)
        val reminderRequiresDelivery = reopenedReminder.notificationEnabled ||
            reopenedReminder.fullScreenAlarmEnabled ||
            reopenedReminder.sourceType.equals("VEHICLE", ignoreCase = true)
        if (reminderRequiresDelivery) {
            if (baseSchedules.isEmpty() && !reminder.sourceType.equals("CALENDAR", ignoreCase = true)) {
                repository.updateStatus(reminderId, "DISABLED")
                throw IllegalStateException("زمان‌بندی اصلی این یادآور پیدا نشد؛ برای جلوگیری از فعال‌سازی بدون هشدار، ابتدا آن را اصلاح کنید.")
            }
            enableReminder(reminderId)
        } else {
            repository.updateStatus(reminderId, "ACTIVE")
        }
    }

    /**
     * Disables a reminder and pauses its schedules without deleting them.
     */
    suspend fun disableReminder(reminderId: String) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderById(reminderId)
        val effectiveReminderId = reminder?.id ?: reminderId
        val schedules = repository.getSchedulesSync(reminderId)
        // Persist the disabled channel and disabled schedules before cancelling system alarms.
        if (reminder != null) {
            repository.updateReminderWithSchedules(
                reminder.copy(status = "DISABLED", updatedAt = System.currentTimeMillis()),
                schedules.map { it.copy(enabled = false) }
            )
        } else {
            repository.updateStatus(reminderId, "DISABLED")
        }
        scheduler.cancel(effectiveReminderId, schedules)
        NotificationDispatcher(context).cancelReminderNotifications(effectiveReminderId, schedules)
        if (reminder?.sourceType.equals("CALENDAR", ignoreCase = true)) {
            reminder?.sourceId?.let { com.example.calendar.reminder.CalendarEventReminderScheduler(context).cancel(it) }
        }
    }

    /**
     * Snoozes a reminder by scheduling a non-destructive temporary trigger.
     * Orders Room persistence BEFORE AlarmManager registration to prevent premature triggers.
     * Supports both preset durations and exact custom future timestamps.
     */
    suspend fun snoozeReminder(
        reminderId: String,
        option: SnoozeOption,
        customTargetMillis: Long? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderById(reminderId)
            ?: return@withContext Result.failure(IllegalStateException("یادآور با شناسه موردنظر یافت نشد."))

        if (!ReminderPolicyEngine.canSnooze(reminder, scheduler.hasConfiguredDelivery(reminder))) {
            return@withContext Result.failure(IllegalStateException("روش دریافت هشدار برای این یادآور فعال نیست."))
        }

        val now = System.currentTimeMillis()
        val triggerMillis = when (option) {
            SnoozeOption.MINUTES_15 -> now + (15 * 60 * 1000L)
            SnoozeOption.HOUR_1 -> now + (60 * 60 * 1000L)
            SnoozeOption.TOMORROW -> now + (24 * 60 * 60 * 1000L)
            SnoozeOption.THREE_DAYS -> now + (3 * 24 * 60 * 60 * 1000L)
            SnoozeOption.CUSTOM -> {
                if (customTargetMillis == null || customTargetMillis <= now) {
                    return@withContext Result.failure(IllegalArgumentException("زمان انتخابی برای تعویق نامعتبر یا در گذشته است."))
                }
                customTargetMillis
            }
        }

        if (triggerMillis <= now) {
            return@withContext Result.failure(IllegalArgumentException("زمان تعیین‌شده برای تعویق در گذشته است."))
        }

        val snoozeMinutes = (((triggerMillis - now) / 60000L).coerceAtLeast(1L)).toInt()
        val snoozeSchedule = ReminderScheduleEntity(
            id = "snooze_${UUID.randomUUID()}",
            reminderId = reminder.id,
            triggerType = "EXACT",
            offsetValue = snoozeMinutes,
            offsetUnit = "MINUTE",
            triggerDateTime = triggerMillis,
            repeatType = "NONE",
            enabled = true,
            createdAt = now
        )

        // Serialize replacement per reminder so repeated taps cannot leave multiple
        // active temporary alarms for the same reminder.
        val snoozeLock = reminderSnoozeLocks.computeIfAbsent(reminder.id) { Mutex() }
        snoozeLock.lock()
        try {
            val previousSnoozes = repository.getSchedulesSync(reminder.id)
                .filter { it.id.startsWith("snooze_") }

            // Cancel the exact PendingIntents before deleting their persisted rows.
            previousSnoozes.forEach { scheduler.cancelSchedule(it.id) }
            previousSnoozes.forEach { repository.deleteSchedule(it) }

            // Persist the replacement first so its receiver can resolve the schedule.
            try {
                repository.insertSchedule(snoozeSchedule)
                scheduler.scheduleSingle(reminder, snoozeSchedule)
            } catch (e: Exception) {
                Log.e("ReminderManager", "Failed to replace snooze alarm. Rolling back.", e)
                runCatching { scheduler.cancelSchedule(snoozeSchedule.id) }
                runCatching { repository.deleteSchedule(snoozeSchedule) }

                // If replacement fails, restore the previous snooze and its future alarm.
                previousSnoozes.forEach { oldSchedule ->
                    runCatching { repository.insertSchedule(oldSchedule) }
                        .onFailure { e.addSuppressed(it) }
                    if (oldSchedule.enabled &&
                        oldSchedule.triggerDateTime > System.currentTimeMillis() &&
                        reminder.status.equals("ACTIVE", ignoreCase = true)
                    ) {
                        runCatching { scheduler.scheduleSingle(reminder, oldSchedule) }
                            .onFailure { e.addSuppressed(it) }
                    }
                }
                return@withContext Result.failure(
                    IllegalStateException("خطا در جایگزینی هشدار تعویقی: ${e.message ?: "عدم امکان ثبت هشدار"}", e)
                )
            }

            Log.i(
                "ReminderManager",
                "Replaced ${previousSnoozes.size} previous snooze schedule(s) for ${reminder.title}; new trigger at $triggerMillis."
            )
            Result.success(triggerMillis)
        } finally {
            snoozeLock.unlock()
        }
    }

    /**
     * Rebuilds all persistent schedules from the database.
     */
    suspend fun rebuildAllSchedules() = withContext(Dispatchers.IO) {
        scheduler.rebuildAllSchedules()
    }

    // === Integrations with other modules ===

    /**
     * Synchronizes or creates a reminder for an installment.
     */
    suspend fun syncInstallmentReminder(
        installmentId: String,
        title: String,
        amount: Long,
        dueDatePersian: String,
        dueTimePersian: String? = null,
        selectedOffsets: List<PredefinedOffset>? = null,
        paymentDueDates: List<Pair<String, String>>? = null,
        amountUnit: AppCurrency = AppCurrency.TOMAN,
        useSelectedOffsetsExactly: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        val existing = repository.getReminderBySource("INSTALLMENT", installmentId)
        val effectiveDueTime = dueTimePersian?.takeIf { it.isNotBlank() }
            ?: existing?.time
            ?: defaultReminderTime()
        val oldSchedules = existing?.let { repository.getSchedulesSync(it.id) }.orEmpty()
        // The stored schedules are also the durable copy of the user's selected offsets.
        // Keep disabled entries for past triggers so a later date edit can restore them.
        val offsets = if (useSelectedOffsetsExactly) {
            selectedOffsets.orEmpty()
        } else {
            selectedOffsets?.takeIf { it.isNotEmpty() } ?: when {
            existing?.notificationEnabled == true -> oldSchedules.mapNotNull { stored ->
                PredefinedOffset.entries.find {
                    it.value == stored.offsetValue && it.unit.name.equals(stored.offsetUnit, ignoreCase = true)
                }
            }.distinct()
            existing == null -> {
                // A non-zero value here exists only after the user explicitly chose it in Settings.
                val preferredDays = AppPreferencesRepository.getInstance(context).installmentReminderDays.value
                val preferredOffset = when (preferredDays) {
                    1 -> PredefinedOffset.BEFORE_1_DAY
                    3 -> PredefinedOffset.BEFORE_3_DAYS
                    7 -> PredefinedOffset.BEFORE_7_DAYS
                    10 -> PredefinedOffset.BEFORE_10_DAYS
                    else -> null
                }
                listOfNotNull(preferredOffset)
            }
            else -> emptyList()
            }
        }

        val reminder = existing?.copy(
            title = "سررسید قسط: $title",
            amount = amount,
            amountUnit = amountUnit.name,
            description = "مبلغ قسط: ${MoneyFormatter.formatExact(MoneyAmount.of(amount, amountUnit))}",
            date = dueDatePersian,
            time = effectiveDueTime,
            notificationEnabled = offsets.isNotEmpty(),
            fullScreenAlarmEnabled = false,
            updatedAt = System.currentTimeMillis()
        ) ?: ReminderEntity(
            id = "rem_inst_$installmentId",
            title = "سررسید قسط: $title",
            description = "مبلغ قسط: ${MoneyFormatter.formatExact(MoneyAmount.of(amount, amountUnit))}",
            type = "INSTALLMENT",
            sourceType = "INSTALLMENT",
            sourceId = installmentId,
            status = "ACTIVE",
            date = dueDatePersian,
            time = effectiveDueTime,
            amount = amount,
            amountUnit = amountUnit.name,
            notificationEnabled = offsets.isNotEmpty(),
            fullScreenAlarmEnabled = false,
            smsEnabled = false
        )

        val payments = paymentDueDates ?: listOf("legacy" to dueDatePersian)
        val now = System.currentTimeMillis()
        // Once every payment is paid, complete the loan-level reminder and retain
        // its schedule preferences only as disabled history. This does not alter
        // per-payment trigger generation for loans that still have unpaid payments.
        val reminderToPersist = if (payments.isEmpty()) {
            reminder.copy(
                status = "DISABLED",
                completedAt = null,
                updatedAt = now
            )
        } else {
            reminder
        }
        val today = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // When there are no unpaid payments, keep disabled preference rows.
        // These rows are never armed; they only preserve selected offsets so a
        // payment reopened later can regain its original reminder configuration.
        val generatedSchedules = if (payments.isEmpty()) {
            val retainedSchedules = oldSchedules.filterNot { it.id.startsWith("snooze_") }
            if (retainedSchedules.isNotEmpty()) {
                retainedSchedules.map { it.copy(enabled = false, createdAt = now) }
            } else {
                offsets.map { offset ->
                    val stableId = UUID.nameUUIDFromBytes(
                        "installment:$installmentId:__preferences__:${offset.value}:${offset.unit.name}".toByteArray(Charsets.UTF_8)
                    ).toString()
                    ReminderScheduleEntity(
                        id = stableId,
                        reminderId = reminder.id,
                        triggerType = if (offset == PredefinedOffset.AT_TIME) "EXACT" else "BEFORE_EVENT",
                        offsetValue = offset.value,
                        offsetUnit = offset.unit.name,
                        triggerDateTime = now,
                        repeatType = "NONE",
                        repeatInterval = 1,
                        enabled = false,
                        createdAt = now
                    )
                }
            }
        } else payments.flatMap { (paymentId, paymentDate) ->
            val dueMillis = runCatching { parsePersianDateTimeToMillis(paymentDate, effectiveDueTime) }.getOrNull()
                ?: return@flatMap emptyList()
            val dueDay = Calendar.getInstance().apply {
                timeInMillis = dueMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            offsets.map { offset ->
                val triggerMillis = calculateOffsetTriggerMillis(dueMillis, offset, calendarBasedOffsets = true)
                val isDueDateStillCurrent = !dueDay.before(today)
                val shouldArm = isDueDateStillCurrent && triggerMillis > now
                val stableId = UUID.nameUUIDFromBytes(
                    "installment:$installmentId:$paymentId:${offset.value}:${offset.unit.name}".toByteArray(Charsets.UTF_8)
                ).toString()
                // Keep a user's explicit completion for this payment occurrence
                // across routine installment refreshes and date recalculations.
                val completedMarker = oldSchedules.firstOrNull {
                    it.id == stableId && it.triggerType == "COMPLETED"
                }
                val wasCompletedByUser = completedMarker != null
                ReminderScheduleEntity(
                    id = stableId,
                    reminderId = reminder.id,
                    triggerType = if (wasCompletedByUser) "COMPLETED"
                        else if (offset == PredefinedOffset.AT_TIME) "EXACT" else "BEFORE_EVENT",
                    offsetValue = offset.value,
                    offsetUnit = offset.unit.name,
                    triggerDateTime = triggerMillis,
                    repeatType = "NONE",
                    repeatInterval = 1,
                    enabled = shouldArm && !wasCompletedByUser,
                    createdAt = completedMarker?.createdAt ?: now
                )
            }
        }

        // Keep completed occurrences even after their payment leaves the unpaid
        // schedule. Otherwise a later loan refresh would erase completion history.
        val generatedScheduleIds = generatedSchedules.mapTo(mutableSetOf()) { it.id }
        val retainedCompletedSchedules = oldSchedules.filter {
            it.triggerType == "COMPLETED" && it.id !in generatedScheduleIds
        }
        val schedules = generatedSchedules + retainedCompletedSchedules

        // Cancel old PendingIntents before replacing Room rows. Each payment/offset pair
        // has its own stable ID, so paying or editing one payment cannot collide with others.
        if (existing != null) {
            try {
                scheduler.cancel(existing.id, oldSchedules)
                NotificationDispatcher(context).cancelReminderNotifications(existing.id, oldSchedules)
                repository.updateReminderWithSchedules(reminderToPersist, schedules)
            } catch (e: Exception) {
                Log.e("ReminderManager", "Failed to persist installment reminder changes for $installmentId; restoring previous alarms.", e)
                runCatching {
                    repository.updateReminderWithSchedules(existing, oldSchedules)
                    val restorableSchedules = oldSchedules.filter { it.enabled && it.triggerDateTime > now }
                    if (restorableSchedules.isNotEmpty()) {
                        scheduler.schedule(existing, restorableSchedules)
                    }
                }.onFailure { rollbackError ->
                    e.addSuppressed(rollbackError)
                    Log.e("ReminderManager", "Failed to restore installment reminder after persistence failure for $installmentId.", rollbackError)
                }
                throw e
            }
        } else {
            repository.insertReminderWithSchedules(reminder, schedules)
        }

        if (!reminder.status.equals("ACTIVE", ignoreCase = true) ||
            !reminder.notificationEnabled && !reminder.fullScreenAlarmEnabled
        ) {
            return@withContext 0
        }

        // Reminder opt-in only controls whether this installment has active schedules.
        // The global alert-delivery preference remains the single source of truth for
        // notification vs. full-screen alarm behavior; never change it from this flow.

        val futureSchedules = schedules.filter { it.enabled && it.triggerDateTime > now }
        if (futureSchedules.isEmpty()) {
            Log.i("ReminderManager", "No future installment alarms for $installmentId; reminder preferences retained.")
            return@withContext 0
        }
        try {
            val count = scheduler.schedule(reminder, futureSchedules)
            Log.i("ReminderManager", "Synced ${futureSchedules.size} installment schedules for $installmentId; armed=$count.")
            count
        } catch (e: Exception) {
            Log.e("ReminderManager", "Installment schedules could not be armed for $installmentId; restoring the previous reminder configuration.", e)
            runCatching {
                scheduler.cancel(reminder.id, schedules)
                NotificationDispatcher(context).cancelReminderNotifications(reminder.id, schedules)
                if (existing != null) {
                    repository.updateReminderWithSchedules(existing, oldSchedules)
                    val restorableSchedules = oldSchedules.filter { it.enabled && it.triggerDateTime > now }
                    if (restorableSchedules.isNotEmpty()) {
                        scheduler.schedule(existing, restorableSchedules)
                    }
                } else {
                    repository.deleteReminderWithSchedules(reminder.id)
                }
            }.onFailure { rollbackError ->
                e.addSuppressed(rollbackError)
                Log.e("ReminderManager", "Failed to restore installment reminder after scheduling failure for $installmentId.", rollbackError)
            }
            throw e
        }
    }

    /**
     * Refreshes an already opted-in installment reminder after a payment/date edit.
     * Never creates or enables a reminder as a side effect of editing payment history.
     */
    suspend fun refreshInstallmentReminder(
        installmentId: String,
        title: String,
        amount: Long,
        dueDatePersian: String,
        paymentDueDates: List<Pair<String, String>>,
        amountUnit: AppCurrency = AppCurrency.TOMAN
    ): Int = withContext(Dispatchers.IO) {
        val existing = repository.getReminderBySource("INSTALLMENT", installmentId)
            ?: return@withContext 0

        // Completing an installment reminder from its notification is an explicit
        // user action. Routine installment refreshes must preserve that decision;
        // an unpaid payment alone is not evidence that the user wants alerts re-enabled.
        syncInstallmentReminder(
            installmentId = installmentId,
            title = title,
            amount = amount,
            dueDatePersian = dueDatePersian,
            paymentDueDates = paymentDueDates,
            amountUnit = amountUnit
        )
    }

    /**
     * Legacy compatibility hook. A single payment must never complete the
     * reminder for the whole loan; payment repositories refresh the remaining
     * per-payment schedules after committing the payment instead.
     */
    @Deprecated(
        message = "Refresh the installment reminder with the current unpaid payment list instead."
    )
    suspend fun onInstallmentPaid(installmentId: String) = withContext(Dispatchers.IO) {
        Log.w(
            "ReminderManager",
            "Ignoring legacy whole-loan completion for $installmentId; per-payment schedules are refreshed by the installment repository."
        )
    }

    /**
     * Synchronizes a vehicle service reminder.
     */
    suspend fun syncVehicleServiceReminder(
        serviceId: String,
        vehicleName: String,
        serviceTitle: String,
        dueDatePersian: String,
        dueKilometer: Long? = null,
        currentKilometer: Long? = null,
        type: String = "MAINTENANCE",
        selectedOffsets: List<PredefinedOffset> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val existing = repository.getReminderBySource("VEHICLE", serviceId)
        val effectiveTime = existing?.time ?: defaultReminderTime()
        val reminder = existing?.copy(
            title = "$serviceTitle ($vehicleName)",
            date = dueDatePersian,
            time = effectiveTime,
            targetKilometer = dueKilometer,
            currentKilometer = currentKilometer,
            notificationEnabled = selectedOffsets.isNotEmpty(),
            fullScreenAlarmEnabled = false,
            status = "ACTIVE"
        ) ?: ReminderEntity(
            id = "rem_veh_$serviceId",
            title = "$serviceTitle ($vehicleName)",
            description = "سرویس دوره‌ای خودروی $vehicleName",
            type = type,
            sourceType = "VEHICLE",
            sourceId = serviceId,
            status = "ACTIVE",
            date = dueDatePersian,
            time = effectiveTime,
            targetKilometer = dueKilometer,
            currentKilometer = currentKilometer,
            notificationEnabled = selectedOffsets.isNotEmpty(),
            fullScreenAlarmEnabled = false,
            smsEnabled = false
        )

        val offsets = selectedOffsets

        if (existing == null) {
            createReminder(reminder, offsets)
        } else {
            updateReminder(reminder, offsets)
        }
    }

    /**
     * Synchronizes a vehicle insurance expiration reminder.
     */
    suspend fun syncInsuranceReminder(
        insuranceId: String,
        vehicleName: String,
        expirationDatePersian: String,
        selectedOffsets: List<PredefinedOffset> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val existing = repository.getReminderBySource("INSURANCE", insuranceId)
        val effectiveTime = existing?.time ?: defaultReminderTime()
        val reminder = existing?.copy(
            title = "تمدید بیمه خودرو: $vehicleName",
            date = expirationDatePersian,
            time = effectiveTime,
            notificationEnabled = selectedOffsets.isNotEmpty(),
            fullScreenAlarmEnabled = false,
            status = "ACTIVE"
        ) ?: ReminderEntity(
            id = "rem_ins_$insuranceId",
            title = "تمدید بیمه خودرو: $vehicleName",
            description = "سررسید انقضای بیمه‌نامه شخص ثالث / بدنه",
            type = "INSURANCE",
            sourceType = "INSURANCE",
            sourceId = insuranceId,
            status = "ACTIVE",
            date = expirationDatePersian,
            time = effectiveTime,
            notificationEnabled = selectedOffsets.isNotEmpty(),
            fullScreenAlarmEnabled = false,
            smsEnabled = false
        )

        val offsets = selectedOffsets

        if (existing == null) {
            createReminder(reminder, offsets)
        } else {
            updateReminder(reminder, offsets)
        }
    }

    /**
     * Synchronizes a check reminder.
     */
    suspend fun syncCheckReminder(
        checkId: String,
        title: String,
        amount: Long,
        dueDatePersian: String,
        type: String, // "RECEIVED" or "PAID"
        reminderDateTime: Long? = null,
        amountUnit: AppCurrency = AppCurrency.TOMAN
    ) = withContext(Dispatchers.IO) {
        val existing = repository.getReminderBySource("CHECK", checkId)
        
        // If reminder is turned off (null), delete existing reminder and cancel alarms
        if (reminderDateTime == null) {
            if (existing != null) {
                deleteReminder(existing.id)
            }
            return@withContext
        }

        val targetDateTime = reminderDateTime
        val persianDate = PersianCalendarHelper.fromEpochMillis(targetDateTime).toFormattedDate()
        val persianTime = PersianCalendarHelper.fromEpochMillis(targetDateTime).toFormattedTime()

        val reminder = existing?.copy(
            title = if (type == "RECEIVED") "وصول چک دریافتی: $title" else "پرداخت چک: $title",
            amount = amount,
            amountUnit = amountUnit.name,
            description = "مبلغ چک: ${MoneyFormatter.formatExact(MoneyAmount.of(amount, amountUnit))}",
            date = persianDate,
            time = persianTime,
            notificationEnabled = true,
            fullScreenAlarmEnabled = false,
            status = "ACTIVE"
        ) ?: ReminderEntity(
            id = "rem_chk_$checkId",
            title = if (type == "RECEIVED") "وصول چک دریافتی: $title" else "پرداخت چک: $title",
            description = "مبلغ چک: ${MoneyFormatter.formatExact(MoneyAmount.of(amount, amountUnit))}",
            type = "CHECK",
            sourceType = "CHECK",
            sourceId = checkId,
            status = "ACTIVE",
            date = persianDate,
            time = persianTime,
            amount = amount,
            amountUnit = amountUnit.name,
            notificationEnabled = true,
            fullScreenAlarmEnabled = false,
            smsEnabled = false
        )

        // Check reminders use exactly one trigger at the user-selected local date and minute.
        val offsets = listOf(PredefinedOffset.AT_TIME)

        // Editing check details must never silently reactivate a reminder the user disabled
        // from the reminder center (or already completed/cancelled).
        if (existing != null && !existing.status.equals("ACTIVE", ignoreCase = true)) {
            val oldSchedules = repository.getSchedulesSync(existing.id)
            scheduler.cancel(existing.id, oldSchedules)
            NotificationDispatcher(context).cancelReminderNotifications(existing.id, oldSchedules)
            val disabledSchedules = offsets.map { offset ->
                ReminderScheduleEntity(
                    id = UUID.randomUUID().toString(),
                    reminderId = existing.id,
                    triggerType = "EXACT",
                    offsetValue = offset.value,
                    offsetUnit = offset.unit.name,
                    triggerDateTime = targetDateTime,
                    repeatType = RepeatType.NONE.name,
                    repeatInterval = 1,
                    enabled = false,
                    createdAt = System.currentTimeMillis()
                )
            }
            repository.updateReminderWithSchedules(
                reminder.copy(status = existing.status, updatedAt = System.currentTimeMillis()),
                disabledSchedules
            )
            return@withContext
        }

        if (!NotificationDispatcher(context).hasNotificationPermission()) {
            throw IllegalStateException("اعلان‌های اندروید برای دارینو فعال نیستند؛ تنظیمات اعلان دستگاه را بررسی کنید.")
        }

        if (existing == null) {
            val scheduledCount = createReminder(reminder, offsets)
            if (scheduledCount <= 0) {
                throw IllegalStateException("آلارم دقیق چک در سیستم ثبت نشد.")
            }
        } else {
            val scheduledCount = updateReminder(reminder, offsets)
            if (scheduledCount <= 0) {
                throw IllegalStateException("آلارم دقیق چک در سیستم ثبت نشد.")
            }
        }
    }

    /**
     * Deletes only the reminder records associated with a deleted check.
     *
     * Legacy and smart rows are removed from visible reminder data atomically
     * under the same lock used by legacy migration. OS alarms are cancelled only
     * after the database transaction commits, so a failed cleanup can be retried.
     */
    suspend fun deleteCheckReminder(
        checkId: String,
        restoreCheckReminderId: String? = null,
        deleteCheckRecord: suspend (Long?) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val userId = SessionManager.userId
            ?: throw IllegalStateException("Authenticated user is required to delete a check reminder")

        val database = AppDatabase.getDatabase(context)
        // Serialize against legacy migration. Read the cancellation snapshot,
        // soft-delete the check, and remove both reminder stores in one Room
        // transaction so concurrent writes cannot escape the cleanup snapshot.
        val cleanupSnapshot = LocalReminderRepository.withLegacyMigrationLock {
            database.withTransaction {
                val smart = repository.getReminderBySource("CHECK", checkId)
                val schedules = smart?.let { repository.getCheckSchedulesSync(it.id) }.orEmpty()
                val legacy = checkId.toIntOrNull()?.let { numericId ->
                    database.reminderDao().getCheckRemindersForCheck(userId, numericId)
                }.orEmpty()
                val restoreReminder = restoreCheckReminderId?.let {
                    repository.getReminderBySource("CHECK", it)
                }
                val restoreReminderDateTime = restoreReminder?.let {
                    runCatching { parsePersianDateTimeToMillis(it.date, it.time) }.getOrNull()
                }
                deleteCheckRecord(restoreReminderDateTime)
                repository.deleteCheckReminderWithSchedulesAndLogs(checkId)
                // Only reminders that were ACTIVE immediately before replacement carry
                // this marker. Previously disabled/completed/cancelled reminders stay so.
                val shouldRestoreOriginalReminder = restoreReminder?.let {
                    it.status.equals("COMPLETED", ignoreCase = true) &&
                        it.cancelledAt != null &&
                        it.updatedAt == it.cancelledAt
                } == true
                Triple(Pair(smart, schedules), legacy, restoreReminder?.id?.takeIf {
                    shouldRestoreOriginalReminder
                })
            }
        }
        val smartReminder = cleanupSnapshot.first.first
        val smartSchedules = cleanupSnapshot.first.second
        val legacyReminders = cleanupSnapshot.second
        val restoreReminderId = cleanupSnapshot.third

        // Platform alarms and visible notifications are cancelled only after commit.
        val legacyRepository = com.example.data.repository.ReminderRepository(context)
        val platformNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val cancellationErrors = mutableListOf<Throwable>()
        legacyReminders.forEach { legacy ->
            runCatching { legacyRepository.deleteReminder(legacy.id) }
                .onFailure { error ->
                    cancellationErrors.add(error)
                    Log.e("ReminderManager", "Could not cancel legacy check reminder alarm ${legacy.id}.", error)
                }
            runCatching { platformNotificationManager.cancel("legacy_reminder:${legacy.id}", legacy.id) }
                .onFailure { error ->
                    cancellationErrors.add(error)
                    Log.e("ReminderManager", "Could not cancel legacy check reminder notification ${legacy.id}.", error)
                }
        }
        if (smartReminder != null) {
            runCatching { scheduler.cancel(smartReminder.id, smartSchedules) }
                .onFailure { error ->
                    cancellationErrors.add(error)
                    Log.e("ReminderManager", "Could not cancel smart check reminder alarms ${smartReminder.id}.", error)
                }
            runCatching {
                NotificationDispatcher(context).cancelReminderNotifications(smartReminder.id, smartSchedules)
            }.onFailure { error ->
                cancellationErrors.add(error)
                Log.e("ReminderManager", "Could not cancel smart check reminder notifications ${smartReminder.id}.", error)
            }
            Log.i("ReminderManager", "Deleted check reminder $checkId and all its schedules and delivery logs.")
        }

        // Remove the durable outbox entry only after every OS cancellation succeeds.
        // Otherwise the next authenticated session will retry the remaining cleanup.
        if (cancellationErrors.isEmpty()) {
            runCatching {
                database.checkReminderCleanupQueueDao().delete(userId, checkId)
            }.onFailure { error ->
                Log.w("ReminderManager", "Could not clear completed check reminder cleanup entry $checkId; it will be replayed safely.", error)
            }
        }

        // Deleting a replacement releases its original bounced check. Restore only
        // that original check's persisted reminder; restoreReminder re-arms only
        // saved future triggers and never invents a new notification schedule.
        restoreReminderId?.let { reminderId ->
            runCatching { restoreReminder(reminderId) }
                .onFailure { error ->
                    Log.e("ReminderManager", "Could not reactivate the original check reminder $reminderId after replacement deletion.", error)
                }
        }

        // Restore only the legacy reminders whose persisted pause snapshot still
        // matches an untouched, incomplete reminder on the released original check.
        runCatching {
            (repository as? LocalReminderRepository ?: LocalReminderRepository(context))
                .resumePendingLegacyCheckReminders(userId)
        }.onFailure { error ->
            Log.e("ReminderManager", "Could not restore paused legacy check reminders after check deletion.", error)
        }
    }

    /**
     * Pauses only an active check reminder while its check is replaced.
     * The cancellation timestamp is a durable marker allowing a later deletion
     * of the replacement to restore this reminder without reactivating one the
     * user had already disabled or completed.
     */
    suspend fun pauseCheckReminderForReplacement(checkId: String) = withContext(Dispatchers.IO) {
        val userId = SessionManager.userId
            ?: throw IllegalStateException("Authenticated user is required to pause a check reminder")
        val numericCheckId = checkId.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid check ID for reminder replacement")
        val database = AppDatabase.getDatabase(context)
        val now = System.currentTimeMillis()

        // Pause only active legacy rows and persist their exact IDs in the same
        // transaction. Existing snapshots are retained on repeated calls.
        val pausedLegacyReminders = LocalReminderRepository.withLegacyMigrationLock {
            database.withTransaction {
                val activeLegacy = database.reminderDao()
                    .getCheckRemindersForCheck(userId, numericCheckId)
                    .filter { it.enabled && it.completedAt == null && it.deletedAt == null }

                if (activeLegacy.isNotEmpty()) {
                    database.checkReminderReplacementPauseDao().insertAll(
                        activeLegacy.map { legacy ->
                            com.example.data.database.CheckReminderReplacementPauseEntity(
                                userId = userId,
                                checkId = numericCheckId,
                                legacyReminderId = legacy.id,
                                pausedAt = now
                            )
                        }
                    )
                    activeLegacy.forEach { legacy ->
                        database.reminderDao().updateReminder(
                            legacy.copy(
                                enabled = false,
                                updatedAt = now,
                                syncState = "PENDING_UPDATE"
                            )
                        )
                    }
                }
                activeLegacy
            }
        }

        // Cancel only the platform alarms/notifications belonging to those legacy IDs.
        val legacyScheduler = com.example.data.receiver.ReminderScheduler(context)
        val platformNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        pausedLegacyReminders.forEach { legacy ->
            runCatching { legacyScheduler.cancel(legacy.id) }
                .onFailure { Log.e("ReminderManager", "Could not cancel legacy check alarm ${legacy.id} during replacement.", it) }
            runCatching { platformNotificationManager.cancel("legacy_reminder:${legacy.id}", legacy.id) }
                .onFailure { Log.e("ReminderManager", "Could not cancel legacy check notification ${legacy.id} during replacement.", it) }
        }

        // Preserve the existing smart-reminder pause behavior, but do not return
        // early when no smart row exists: legacy reminders still need pausing.
        val reminder = repository.getReminderBySource("CHECK", checkId)
        if (reminder != null && reminder.status.equals("ACTIVE", ignoreCase = true)) {
            val schedules = repository.getCheckSchedulesSync(reminder.id)
            repository.updateReminderWithSchedules(
                reminder.copy(
                    status = "COMPLETED",
                    completedAt = now,
                    cancelledAt = now,
                    updatedAt = now
                ),
                schedules.map { it.copy(enabled = false) }
            )
            runCatching { scheduler.cancel(reminder.id, schedules) }
                .onFailure { Log.e("ReminderManager", "Could not cancel original check alarms during replacement.", it) }
            runCatching { NotificationDispatcher(context).cancelReminderNotifications(reminder.id, schedules) }
                .onFailure { Log.e("ReminderManager", "Could not cancel original check notifications during replacement.", it) }
        }
    }

    /**
     * Rolls back a reminder pause when check replacement did not commit.
     * The legacy recovery path verifies that the original check is still BOUNCED
     * and that each reminder row has not been edited, completed or deleted.
     */
    suspend fun restoreCheckReminderAfterFailedReplacement(checkId: String) = withContext(Dispatchers.IO) {
        val userId = SessionManager.userId
            ?: throw IllegalStateException("Authenticated user is required to restore a check reminder")
        val smartReminder = repository.getReminderBySource("CHECK", checkId)
        if (smartReminder != null &&
            smartReminder.status.equals("COMPLETED", ignoreCase = true) &&
            smartReminder.cancelledAt != null &&
            smartReminder.updatedAt == smartReminder.cancelledAt
        ) {
            restoreReminder(smartReminder.id)
        }
        (repository as? LocalReminderRepository ?: LocalReminderRepository(context))
            .resumePendingLegacyCheckReminders(userId)
    }

    /**
     * Triggered when a check is cleared, paid or canceled.
     */
    suspend fun onCheckSettled(checkId: String) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderBySource("CHECK", checkId)
        if (reminder != null) {
            completeReminder(reminder.id)
        }
    }

    companion object {
        fun parsePersianDateTimeToMillis(dateStr: String, timeStr: String): Long {
            val normalizedDate = com.example.util.IranianPhoneUtils.convertDigitsToEnglish(dateStr).trim()
            val hasValidDateShape = Regex("^(?:\\d{4}/\\d{2}/\\d{2}|\\d{8})$").matches(normalizedDate)
            if (!hasValidDateShape || !com.example.util.IranianDateUtils.isValidPersianDate(normalizedDate)) {
                throw IllegalArgumentException("تاریخ شمسی معتبر نیست: $dateStr")
            }
            val dateDigits = com.example.util.IranianDateUtils.cleanDateDigits(normalizedDate)
            val normalizedTime = com.example.util.IranianPhoneUtils.convertDigitsToEnglish(timeStr).trim()
            if (!Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$").matches(normalizedTime)) {
                throw IllegalArgumentException("ساعت معتبر نیست: $timeStr")
            }

            val year = dateDigits.substring(0, 4).toInt()
            val month = dateDigits.substring(4, 6).toInt()
            val day = dateDigits.substring(6, 8).toInt()
            val hour = normalizedTime.substringBefore(":").toInt()
            val minute = normalizedTime.substringAfter(":").toInt()
            return PersianCalendarHelper.jalaliToEpochMillis(year, month, day, hour, minute)
        }

        fun getSmartSuggestions(): List<SmartSuggestion> {
            return listOf(
                SmartSuggestion(
                    id = "sug_insurance",
                    title = "تمدید بیمه خودرو",
                    message = "بیمه شخص ثالث خودرو پژو ۲۰۷ در تاریخ ۱۴۰۵/۰۸/۱۵ منقضی می‌شود.",
                    type = ReminderType.INSURANCE,
                    defaultDate = "۱۴۰۵/۰۸/۱۵",
                    amount = 6_200_000L,
                    sourceType = "INSURANCE"
                ),
                SmartSuggestion(
                    id = "sug_oil",
                    title = "تعویض روغن موتور",
                    message = "با توجه به کارکرد اخیر، سرویس روغن موتور برای ۴,۸۰۰ کیلومتر آینده پیشنهاد می‌شود.",
                    type = ReminderType.MAINTENANCE,
                    defaultDate = "۱۴۰۵/۰۸/۰۵",
                    amount = 1_850_000L,
                    sourceType = "VEHICLE"
                ),
                SmartSuggestion(
                    id = "sug_checkup",
                    title = "معاینه فنی خودرو",
                    message = "اعتبار برگه معاینه فنی خودرو در انتهای ماه جاری به پایان می‌رسد.",
                    type = ReminderType.VEHICLE,
                    defaultDate = "۱۴۰۵/۰۷/۳۰",
                    sourceType = "VEHICLE"
                )
            )
        }
    }
}
