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
    ): ReminderEntity = withContext(Dispatchers.IO) {
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
        try {
            scheduler.schedule(reminder, schedules)
        } catch (e: Exception) {
            Log.e("ReminderManager", "Failed to schedule alarms for reminder ${reminder.id}. Performing compensation rollback.", e)
            try {
                repository.deleteReminderWithSchedules(reminder.id)
            } catch (rollbackEx: Exception) {
                Log.e("ReminderManager", "Failed to rollback reminder during creation failure", rollbackEx)
            }
            throw e
        }
        Log.i("ReminderManager", "Created reminder ${reminder.title} with ${schedules.size} schedule triggers.")
        reminder
    }

    /**
     * Updates an existing reminder and regenerates its schedules.
     */
    suspend fun updateReminder(
        reminder: ReminderEntity,
        offsets: List<PredefinedOffset> = listOf(PredefinedOffset.AT_TIME),
        repeatType: RepeatType = RepeatType.NONE,
        repeatInterval: Int = 1
    ) = withContext(Dispatchers.IO) {
        val existingReminder = repository.getReminderById(reminder.id)
        val existingSchedules = repository.getSchedulesSync(reminder.id)
        scheduler.cancel(reminder.id, existingSchedules)

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
        try {
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
        Log.i("ReminderManager", "Updated reminder ${reminder.title} and rescheduled.")
    }

    /**
     * Deletes a reminder and cancels its scheduled alarms.
     */
    suspend fun deleteReminder(reminderId: String) = withContext(Dispatchers.IO) {
        val schedules = repository.getSchedulesSync(reminderId)
        scheduler.cancel(reminderId, schedules)
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
        Log.i("ReminderManager", "Completed reminder $reminderId.")
    }

    /**
     * Cancels a reminder.
     */
    suspend fun cancelReminder(reminderId: String) = withContext(Dispatchers.IO) {
        repository.updateStatus(reminderId, "CANCELLED")
        val schedules = repository.getSchedulesSync(reminderId)
        scheduler.cancel(reminderId, schedules)
    }

    /**
     * Enables a reminder and re-arms its future schedules.
     */
    suspend fun enableReminder(reminderId: String) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderById(reminderId) ?: return@withContext
        val schedules = repository.getSchedulesSync(reminderId)
        repository.updateStatus(reminderId, "ACTIVE")
        try {
            scheduler.schedule(reminder, schedules)
        } catch (e: Exception) {
            Log.e("ReminderManager", "Failed to schedule alarms when enabling reminder $reminderId. Rolling back status.", e)
            try {
                repository.updateStatus(reminderId, "DISABLED")
            } catch (rollbackEx: Exception) {
                Log.e("ReminderManager", "Failed to rollback status to DISABLED", rollbackEx)
            }
            throw e
        }
    }

    /**
     * Disables a reminder and pauses its schedules without deleting them.
     */
    suspend fun disableReminder(reminderId: String) = withContext(Dispatchers.IO) {
        repository.updateStatus(reminderId, "DISABLED")
        val schedules = repository.getSchedulesSync(reminderId)
        scheduler.cancel(reminderId, schedules)
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
    ) = withContext(Dispatchers.IO) {
        val existing = repository.getReminderBySourceId(installmentId)
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
            notificationEnabled = true,
            smsEnabled = false
        )

        val offsets = selectedOffsets?.takeIf { it.isNotEmpty() } ?: listOf(
            PredefinedOffset.BEFORE_7_DAYS,
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
            notificationEnabled = true,
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
            notificationEnabled = true,
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
