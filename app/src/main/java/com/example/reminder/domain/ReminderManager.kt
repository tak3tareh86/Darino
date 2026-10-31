package com.example.reminder.domain

import android.content.Context
import android.util.Log
import com.example.reminder.data.LocalReminderRepository
import com.example.reminder.data.ReminderEntity
import com.example.reminder.data.ReminderRepository
import com.example.reminder.data.ReminderScheduleEntity
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ReminderManager(
    private val context: Context,
    private val repository: ReminderRepository = LocalReminderRepository(context),
    private val scheduler: ReminderScheduler = ReminderScheduler(context)
) {

    /**
     * Creates a new reminder along with its multi-schedule triggers, and registers alarms.
     */
    suspend fun createReminder(
        reminder: ReminderEntity,
        offsets: List<PredefinedOffset> = listOf(PredefinedOffset.AT_TIME),
        repeatType: RepeatType = RepeatType.NONE,
        repeatInterval: Int = 1
    ): Int = withContext(Dispatchers.IO) {
        // Parse reminder date & time to base epoch millis
        val baseMillis = parsePersianDateTimeToMillis(reminder.date, reminder.time)

        // Generate schedule entities for each selected offset
        val schedules = offsets.map { offset ->
            val triggerMillis = baseMillis - (offset.value * offset.unit.millisMultiplier)
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
            scheduler.schedule(reminder, schedules)
        } catch (e: Exception) {
            // Keep the reminder and its schedules in Room. Removing them here made a
            // missing exact-alarm permission silently destroy user data.
            Log.e("ReminderManager", "Reminder ${reminder.id} was saved locally but alarm registration failed. Keeping it for recovery/retry.", e)
            throw IllegalStateException(
                "یادآور ذخیره شد، اما ثبت هشدار در سیستم ناموفق بود. مجوز آلارم دقیق را در تنظیمات فعال کنید و دوباره برنامه را باز کنید. جزئیات: ${e.message ?: "خطای نامشخص"}",
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
        offsets: List<PredefinedOffset> = listOf(PredefinedOffset.AT_TIME),
        repeatType: RepeatType = RepeatType.NONE,
        repeatInterval: Int = 1
    ): Int = withContext(Dispatchers.IO) {
        val existingReminder = repository.getReminderById(reminder.id)
        val existingSchedules = repository.getSchedulesSync(reminder.id)
        scheduler.cancel(reminder.id, existingSchedules)
        NotificationDispatcher(context).cancelReminderNotifications(reminder.id)

        val baseMillis = parsePersianDateTimeToMillis(reminder.date, reminder.time)
        val newSchedules = offsets.map { offset ->
            val triggerMillis = baseMillis - (offset.value * offset.unit.millisMultiplier)
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

        repository.updateReminderWithSchedules(reminder, newSchedules)
        val scheduledCount = try {
            scheduler.schedule(reminder, newSchedules)
        } catch (e: Exception) {
            Log.e("ReminderManager", "Failed to schedule alarms during update for reminder ${reminder.id}. Rolling back to previous state.", e)
            if (existingReminder != null) {
                try {
                    repository.updateReminderWithSchedules(existingReminder, existingSchedules)
                    scheduler.schedule(existingReminder, existingSchedules)
                } catch (rollbackEx: Exception) {
                    Log.e("ReminderManager", "Failed to restore previous reminder during update rollback", rollbackEx)
                }
            }
            throw e
        }
        Log.i("ReminderManager", "Updated reminder ${reminder.title} and rescheduled with $scheduledCount future alarms.")
        scheduledCount
    }

    /**
     * Deletes a reminder and cancels its scheduled alarms.
     */
    suspend fun deleteReminder(reminderId: String) = withContext(Dispatchers.IO) {
        val schedules = repository.getSchedulesSync(reminderId)
        scheduler.cancel(reminderId, schedules)
        NotificationDispatcher(context).cancelReminderNotifications(reminderId)
        repository.deleteReminderWithSchedules(reminderId)
        Log.i("ReminderManager", "Deleted reminder $reminderId and all its schedules.")
    }

    /**
     * Marks a reminder as completed.
     */
    suspend fun completeReminder(reminderId: String) = withContext(Dispatchers.IO) {
        repository.updateStatus(reminderId, "COMPLETED")
        val schedules = repository.getSchedulesSync(reminderId)
        scheduler.cancel(reminderId, schedules)
        NotificationDispatcher(context).cancelReminderNotifications(reminderId)
        Log.i("ReminderManager", "Completed reminder $reminderId.")
    }

    /**
     * Cancels a reminder.
     */
    suspend fun cancelReminder(reminderId: String) = withContext(Dispatchers.IO) {
        repository.updateStatus(reminderId, "CANCELLED")
        val schedules = repository.getSchedulesSync(reminderId)
        scheduler.cancel(reminderId, schedules)
        NotificationDispatcher(context).cancelReminderNotifications(reminderId)
    }

    /**
     * Enables a reminder and re-arms its future schedules.
     */
    suspend fun enableReminder(reminderId: String) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderById(reminderId)
            ?: throw NoSuchElementException("یادآور با شناسه $reminderId یافت نشد.")
        val storedSchedules = repository.getSchedulesSync(reminderId)
        if (storedSchedules.isEmpty()) {
            repository.updateStatus(reminderId, "DISABLED")
            throw IllegalStateException("هیچ زمان‌بندی‌ای برای این یادآور ذخیره نشده است.")
        }

        // Preserve the user's per-reminder delivery choices when re-arming a disabled reminder.
        if (!reminder.notificationEnabled && !reminder.fullScreenAlarmEnabled && !reminder.smsEnabled) {
            throw IllegalStateException("برای فعال‌سازی یادآور، حداقل یکی از روش‌های اعلان، آلارم تمام‌صفحه یا پیامک باید فعال باشد.")
        }
        val enabledReminder = reminder.copy(status = "ACTIVE", updatedAt = System.currentTimeMillis())
        val enabledSchedules = storedSchedules.map { it.copy(enabled = true) }
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
                scheduler.cancel(reminderId, enabledSchedules)
            } catch (rollbackEx: Exception) {
                Log.e("ReminderManager", "Failed to rollback reminder to DISABLED", rollbackEx)
            }
            throw e
        }
    }

    /**
     * Disables a reminder and pauses its schedules without deleting them.
     */
    suspend fun disableReminder(reminderId: String) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderById(reminderId)
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
        scheduler.cancel(reminderId, schedules)
        NotificationDispatcher(context).cancelReminderNotifications(reminderId)
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

        if (!reminder.notificationEnabled && !reminder.fullScreenAlarmEnabled && !reminder.smsEnabled) {
            return@withContext Result.failure(IllegalStateException("برای این یادآور هیچ روش اعلانی فعال نشده است."))
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

        // 1. SAFE ORDERING: Insert schedule in Room database FIRST so it is guaranteed available to receiver
        repository.insertSchedule(snoozeSchedule)

        // 2. Register alarm with AlarmManager SECOND; perform compensation rollback if scheduling fails
        try {
            scheduler.scheduleSingle(reminder, snoozeSchedule)
        } catch (e: Exception) {
            Log.e("ReminderManager", "Failed to schedule alarm in AlarmManager for snooze. Rolling back Room schedule.", e)
            try {
                repository.deleteSchedule(snoozeSchedule)
            } catch (rollbackEx: Exception) {
                Log.e("ReminderManager", "Failed to delete schedule during rollback", rollbackEx)
            }
            return@withContext Result.failure(
                IllegalStateException("خطا در زمان‌بندی هشدار در سیستم: ${e.message ?: "عدم امکان ثبت هشدار"}", e)
            )
        }

        Log.i("ReminderManager", "Snoozed reminder ${reminder.title} to $triggerMillis ($snoozeMinutes min).")
        Result.success(triggerMillis)
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
        dueTimePersian: String = "۰۹:۰۰",
        selectedOffsets: List<PredefinedOffset>? = null
    ): Int = withContext(Dispatchers.IO) {
        val existing = repository.getReminderBySourceId(installmentId)
        val offsets = selectedOffsets ?: if (existing != null) {
            repository.getSchedulesSync(existing.id).mapNotNull { stored ->
                PredefinedOffset.entries.find {
                    it.value == stored.offsetValue && it.unit.name.equals(stored.offsetUnit, ignoreCase = true)
                }
            }.distinct()
        } else {
            val preferredDays = AppPreferencesRepository.getInstance(context).installmentReminderDays.value
            val preferredOffset = when (preferredDays) {
                1 -> PredefinedOffset.BEFORE_1_DAY
                3 -> PredefinedOffset.BEFORE_3_DAYS
                7 -> PredefinedOffset.BEFORE_7_DAYS
                10 -> PredefinedOffset.BEFORE_10_DAYS
                else -> PredefinedOffset.BEFORE_7_DAYS
            }
            listOf(preferredOffset, PredefinedOffset.AT_TIME)
        }

        if (offsets.isEmpty()) {
            if (existing != null) {
                val oldSchedules = repository.getSchedulesSync(existing.id)
                scheduler.cancel(existing.id, oldSchedules)
                val disabled = existing.copy(
                    notificationEnabled = false,
                    fullScreenAlarmEnabled = false,
                    smsEnabled = false,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateReminderWithSchedules(disabled, emptyList())
            }
            Log.i("ReminderManager", "syncInstallmentReminder: no selected alert times for $installmentId; no alarms scheduled.")
            return@withContext 0
        }

        val reminder = existing?.copy(
            title = "سررسید قسط: $title",
            amount = amount,
            date = dueDatePersian,
            time = dueTimePersian,
            status = "ACTIVE"
        ) ?: ReminderEntity(
            id = "rem_inst_$installmentId",
            title = "سررسید قسط: $title",
            description = "مبلغ قسط: ${MoneyFormatter.formatToman(amount)}",
            type = "INSTALLMENT",
            sourceType = "INSTALLMENT",
            sourceId = installmentId,
            priority = "HIGH",
            status = "ACTIVE",
            date = dueDatePersian,
            time = dueTimePersian,
            amount = amount,
            notificationEnabled = false,
            smsEnabled = false
        )

        val scheduledCount = if (existing == null) {
            createReminder(reminder, offsets)
        } else {
            updateReminder(reminder, offsets)
        }

        return@withContext scheduledCount
    }

    /**
     * Triggered when an installment is paid. Automatically completes the active reminder.
     */
    suspend fun onInstallmentPaid(installmentId: String) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderBySourceId(installmentId)
        if (reminder != null) {
            completeReminder(reminder.id)
            Log.i("ReminderManager", "Marked reminder for installment $installmentId as COMPLETED.")
        }
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
        type: String = "MAINTENANCE"
    ) = withContext(Dispatchers.IO) {
        val existing = repository.getReminderBySourceId(serviceId)
        val reminder = existing?.copy(
            title = "$serviceTitle ($vehicleName)",
            date = dueDatePersian,
            targetKilometer = dueKilometer,
            currentKilometer = currentKilometer,
            status = "ACTIVE"
        ) ?: ReminderEntity(
            id = "rem_veh_$serviceId",
            title = "$serviceTitle ($vehicleName)",
            description = "سرویس دوره‌ای خودروی $vehicleName",
            type = type,
            sourceType = "VEHICLE",
            sourceId = serviceId,
            priority = "NORMAL",
            status = "ACTIVE",
            date = dueDatePersian,
            time = "۱۰:۰۰",
            targetKilometer = dueKilometer,
            currentKilometer = currentKilometer,
            notificationEnabled = false,
            smsEnabled = false
        )

        val offsets = listOf(
            PredefinedOffset.BEFORE_3_DAYS,
            PredefinedOffset.BEFORE_1_DAY,
            PredefinedOffset.AT_TIME
        )

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
        expirationDatePersian: String
    ) = withContext(Dispatchers.IO) {
        val existing = repository.getReminderBySourceId(insuranceId)
        val reminder = existing?.copy(
            title = "تمدید بیمه خودرو: $vehicleName",
            date = expirationDatePersian,
            status = "ACTIVE"
        ) ?: ReminderEntity(
            id = "rem_ins_$insuranceId",
            title = "تمدید بیمه خودرو: $vehicleName",
            description = "سررسید انقضای بیمه‌نامه شخص ثالث / بدنه",
            type = "INSURANCE",
            sourceType = "INSURANCE",
            sourceId = insuranceId,
            priority = "HIGH",
            status = "ACTIVE",
            date = expirationDatePersian,
            time = "۰۹:۰۰",
            notificationEnabled = false,
            smsEnabled = false
        )

        val offsets = listOf(
            PredefinedOffset.BEFORE_30_DAYS,
            PredefinedOffset.BEFORE_14_DAYS,
            PredefinedOffset.BEFORE_7_DAYS,
            PredefinedOffset.BEFORE_1_DAY,
            PredefinedOffset.AT_TIME
        )

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
        reminderDateTime: Long? = null
    ) = withContext(Dispatchers.IO) {
        val existing = repository.getReminderBySourceId(checkId)
        
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
            date = persianDate,
            time = persianTime,
            status = "ACTIVE"
        ) ?: ReminderEntity(
            id = "rem_chk_$checkId",
            title = if (type == "RECEIVED") "وصول چک دریافتی: $title" else "پرداخت چک: $title",
            description = "مبلغ چک: ${MoneyFormatter.format(amount)}",
            type = "CHECK",
            sourceType = "CHECK",
            sourceId = checkId,
            priority = "HIGH",
            status = "ACTIVE",
            date = persianDate,
            time = persianTime,
            amount = amount,
            notificationEnabled = false,
            smsEnabled = false
        )

        // For custom check reminders, we only use EXACT time (AT_TIME offset)
        // unless it's the default one (could keep BEFORE_1_DAY if we wanted, but requirement says "independent time")
        val offsets = listOf(PredefinedOffset.AT_TIME)

        if (existing == null) {
            createReminder(reminder, offsets)
        } else {
            updateReminder(reminder, offsets)
        }
    }

    /**
     * Triggered when a check is cleared, paid or canceled.
     */
    suspend fun onCheckSettled(checkId: String) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderBySourceId(checkId)
        if (reminder != null) {
            completeReminder(reminder.id)
        }
    }

    companion object {
        fun parsePersianDateTimeToMillis(dateStr: String, timeStr: String): Long {
            val cleanDate = dateStr.replace("۱", "1").replace("۲", "2").replace("۳", "3")
                .replace("۴", "4").replace("۵", "5").replace("۶", "6").replace("۷", "7")
                .replace("۸", "8").replace("۹", "9").replace("۰", "0")
            val cleanTime = timeStr.replace("۱", "1").replace("۲", "2").replace("۳", "3")
                .replace("۴", "4").replace("۵", "5").replace("۶", "6").replace("۷", "7")
                .replace("۸", "8").replace("۹", "9").replace("۰", "0")

            val dateParts = cleanDate.split("/")
            if (dateParts.size != 3) throw IllegalArgumentException("Invalid date format: $dateStr")
            val timeParts = cleanTime.split(":")
            if (timeParts.size != 2) throw IllegalArgumentException("Invalid time format: $timeStr")

            val jy = dateParts[0].trim().toInt()
            val jm = dateParts[1].trim().toInt()
            val jd = dateParts[2].trim().toInt()

            val hour = timeParts[0].trim().toInt()
            val minute = timeParts[1].trim().toInt()

            return PersianCalendarHelper.jalaliToEpochMillis(jy, jm, jd, hour, minute)
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
