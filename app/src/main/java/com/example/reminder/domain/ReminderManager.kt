package com.example.reminder.domain

import android.content.Context
import android.util.Log
import com.example.reminder.data.LocalReminderRepository
import com.example.reminder.data.ReminderEntity
import com.example.reminder.data.ReminderRepository
import com.example.reminder.data.ReminderScheduleEntity
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
        repository.insertReminder(reminder)

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

        repository.insertSchedules(schedules)
        scheduler.schedule(reminder, schedules)
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
        val existingSchedules = repository.getSchedulesSync(reminder.id)
        scheduler.cancel(reminder.id, existingSchedules)
        repository.deleteSchedulesByReminderId(reminder.id)

        repository.updateReminder(reminder)

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

        repository.insertSchedules(newSchedules)
        scheduler.schedule(reminder, newSchedules)
        Log.i("ReminderManager", "Updated reminder ${reminder.title} and rescheduled.")
    }

    /**
     * Deletes a reminder and cancels its scheduled alarms.
     */
    suspend fun deleteReminder(reminderId: String) = withContext(Dispatchers.IO) {
        val schedules = repository.getSchedulesSync(reminderId)
        scheduler.cancel(reminderId, schedules)
        repository.deleteSchedulesByReminderId(reminderId)
        repository.deleteReminderById(reminderId)
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
        repository.updateStatus(reminderId, "ACTIVE")
        val reminder = repository.getReminderById(reminderId) ?: return@withContext
        val schedules = repository.getSchedulesSync(reminderId)
        scheduler.schedule(reminder, schedules)
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
     */
    suspend fun snoozeReminder(
        reminderId: String,
        option: SnoozeOption,
        customMinutes: Int? = null
    ) = withContext(Dispatchers.IO) {
        val reminder = repository.getReminderById(reminderId) ?: return@withContext
        val minutes = when (option) {
            SnoozeOption.MINUTES_15 -> 15
            SnoozeOption.HOUR_1 -> 60
            SnoozeOption.TOMORROW -> 24 * 60
            SnoozeOption.THREE_DAYS -> 3 * 24 * 60
            SnoozeOption.CUSTOM -> customMinutes ?: 15
        }

        val snoozeSchedule = scheduler.snooze(reminder, minutes)
        repository.insertSchedule(snoozeSchedule)
        Log.i("ReminderManager", "Snoozed reminder ${reminder.title} for $minutes minutes.")
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
        dueTimePersian: String = "۰۹:۰۰"
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
            description = "مبلغ قسط: $amount تومان",
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

        val offsets = listOf(
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
            return try {
                val cleanDate = dateStr.replace("۱", "1").replace("۲", "2").replace("۳", "3")
                    .replace("۴", "4").replace("۵", "5").replace("۶", "6").replace("۷", "7")
                    .replace("۸", "8").replace("۹", "9").replace("۰", "0")
                val cleanTime = timeStr.replace("۱", "1").replace("۲", "2").replace("۳", "3")
                    .replace("۴", "4").replace("۵", "5").replace("۶", "6").replace("۷", "7")
                    .replace("۸", "8").replace("۹", "9").replace("۰", "0")

                val dateParts = cleanDate.split("/").map { it.trim().toInt() }
                val timeParts = cleanTime.split(":").map { it.trim().toInt() }

                val jy = dateParts.getOrElse(0) { 1405 }
                val jm = dateParts.getOrElse(1) { 1 }
                val jd = dateParts.getOrElse(2) { 1 }

                val hour = timeParts.getOrElse(0) { 9 }
                val minute = timeParts.getOrElse(1) { 0 }

                PersianCalendarHelper.jalaliToEpochMillis(jy, jm, jd, hour, minute)
            } catch (e: Exception) {
                System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
            }
        }

        fun getInitialSmartReminders(): List<ReminderEntity> {
            return listOf(
                ReminderEntity(
                    id = "rem_inst_mehr",
                    title = "قسط وام بانک مهر ایران",
                    description = "پرداخت قسط ماهیانه وام قرض‌الحسنه",
                    type = "INSTALLMENT",
                    sourceType = "INSTALLMENT",
                    sourceId = "inst_mehr_01",
                    priority = "HIGH",
                    status = "ACTIVE",
                    date = "۱۴۰۵/۰۷/۳۰",
                    time = "۰۹:۰۰",
                    amount = 3_500_000L,
                    notificationEnabled = true,
                    smsEnabled = false
                ),
                ReminderEntity(
                    id = "rem_ins_peugeot",
                    title = "تمدید بیمه شخص ثالث پژو ۲۰۷",
                    description = "سررسید بیمه‌نامه سالانه خودرو",
                    type = "INSURANCE",
                    sourceType = "INSURANCE",
                    sourceId = "ins_peugeot_01",
                    priority = "HIGH",
                    status = "ACTIVE",
                    date = "۱۴۰۵/۰۸/۱۵",
                    time = "۱۰:۰۰",
                    amount = 6_200_000L,
                    notificationEnabled = true,
                    smsEnabled = false
                ),
                ReminderEntity(
                    id = "rem_srv_oil",
                    title = "تعویض روغن موتور و فیلترها",
                    description = "سرویس دوره‌ای دنا پلاس در کیلومتر ۶۵,۰۰۰",
                    type = "MAINTENANCE",
                    sourceType = "VEHICLE",
                    sourceId = "srv_oil_01",
                    priority = "NORMAL",
                    status = "ACTIVE",
                    date = "۱۴۰۵/۰۸/۰۲",
                    time = "۱۶:۳۰",
                    amount = 1_850_000L,
                    targetKilometer = 65_000L,
                    currentKilometer = 60_200L,
                    notificationEnabled = true,
                    smsEnabled = false
                ),
                ReminderEntity(
                    id = "rem_fin_bill",
                    title = "پرداخت قبض آب و برق",
                    description = "شناسه قبض و پرداخت در سامانه خدمات شهری",
                    type = "FINANCE",
                    sourceType = "MANUAL",
                    priority = "NORMAL",
                    status = "ACTIVE",
                    date = "۱۴۰۵/۰۷/۲۸",
                    time = "۱۱:۰۰",
                    amount = 450_000L,
                    notificationEnabled = true,
                    smsEnabled = false
                ),
                ReminderEntity(
                    id = "rem_per_doctor",
                    title = "چکاپ شش‌ماهه دندانپزشکی",
                    description = "نوبت ویزیت مطب دکتر رضایی",
                    type = "PERSONAL",
                    sourceType = "MANUAL",
                    priority = "LOW",
                    status = "ACTIVE",
                    date = "۱۴۰۵/۰۸/۱۰",
                    time = "۱۸:۰۰",
                    notificationEnabled = true,
                    smsEnabled = false
                )
            )
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
