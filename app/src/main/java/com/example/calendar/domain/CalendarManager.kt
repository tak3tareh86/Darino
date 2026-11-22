package com.example.calendar.domain

import android.content.Context
import com.example.calendar.data.FinancialCalendarRepository
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventStatus
import com.example.calendar.domain.model.FinancialEventType
import com.example.calendar.domain.model.ReminderBeforeOption
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.util.IranianPhoneUtils
import com.example.util.PersianCalendarHelper
import com.example.ui.screens.settings.model.AppCurrency
import com.example.vehicle.data.VehicleRepository
import com.example.vehicle.data.VehicleServiceEntity
import com.example.vehicle.data.VehicleInsuranceEntity
import com.example.vehicle.data.VehicleInspectionEntity
import com.example.vehicle.data.VehicleExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

private fun parseCalendarCurrencyUnit(value: String): AppCurrency? = when (value) {
    AppCurrency.TOMAN.name -> AppCurrency.TOMAN
    AppCurrency.RIAL.name -> AppCurrency.RIAL
    else -> null
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

class CalendarManager(private val context: Context) {

    private val repository = FinancialCalendarRepository(context)

    /** Streams persisted calendar events together with user-owned smart reminders, installments, and vehicle dossier entries. */
    fun getAllEvents(): Flow<List<FinancialEvent>> {
        val database = AppDatabase.getDatabase(context)
        val userId = SessionManager.userId ?: return flowOf(emptyList())

        // Ensure VehicleRepository is initialized for local database persistence if needed
        VehicleRepository.instance.initDatabase(context)

        val dbEventsFlow = repository.getAllEvents()
        val remindersFlow = database.smartReminderDao().getAllReminders(userId)
        val installmentDao = database.installmentDao()
        val installmentsFlow = installmentDao.getAllInstallments(userId)
        val installmentPaymentsFlow = installmentDao.getAllPayments(userId)
        val installmentDataFlow = combine(installmentsFlow, installmentPaymentsFlow) { installments, payments ->
            installments to payments
        }
        val vehicleServicesFlow = VehicleRepository.instance.services
        val vehicleInsurancesFlow = VehicleRepository.instance.insurances
        val vehicleInspectionsFlow = VehicleRepository.instance.inspections
        val vehicleExpensesFlow = VehicleRepository.instance.expenses

        return combine(
            dbEventsFlow,
            remindersFlow,
            installmentDataFlow,
            combine(
                vehicleServicesFlow,
                vehicleInsurancesFlow,
                vehicleInspectionsFlow,
                vehicleExpensesFlow
            ) { svcs, ins, insp, exp ->
                Quadruple(svcs, ins, insp, exp)
            }
        ) { dbEvents, reminders, installmentData, vehicleData ->
            val allEvents = mutableListOf<FinancialEvent>()
            val seenIds = mutableSetOf<String>()

            // Legacy calendar events that have been adopted into the canonical reminder store
            // are shown only through their linked reminder record.
            val linkedCalendarEventIds = reminders
                .filter { it.sourceType.equals("CALENDAR", ignoreCase = true) }
                .mapNotNull { it.sourceId }
                .toSet()

            // Installment calendar entries are always generated from the user's
            // persisted paymentHistory rows below. They must not depend on whether
            // Smart Reminders are configured.

            // 1. Add DB events that have not yet been adopted into the reminder store.
            for (ev in dbEvents) {
                if (ev.id !in linkedCalendarEventIds && seenIds.add(ev.id)) {
                    allEvents.add(ev)
                }
            }

            // 2. Cross-integrate Smart Reminders
            // Vehicle insurance and inspection records are rendered from their dossier
            // entities below, where expiry date, configured reminder offsets and time
            // are all available. Suppress only their linked smart-reminder duplicates.
            // This is calendar presentation de-duplication only; persisted reminders,
            // schedules and alarm delivery remain untouched.
            val vehicleExpiryRecordIds = (
                vehicleData.second.map { it.id } + vehicleData.third.map { it.id }
            ).toSet()

            reminders.forEach { rem ->
                // Installment schedule rows above are the calendar source of truth.
                // Vehicle expiry records are also represented by their dossier entities.
                if (rem.sourceType.equals("INSTALLMENT", ignoreCase = true)) return@forEach
                if (
                    (rem.sourceType.equals("VEHICLE", ignoreCase = true) ||
                        rem.sourceType.equals("INSURANCE", ignoreCase = true)) &&
                    rem.sourceId?.let { it in vehicleExpiryRecordIds } == true
                ) return@forEach
                val eventId = "auto_rem_${rem.id}"
                if (seenIds.add(eventId)) {
                    val normDate = CalendarDateUtils.normalizeDate(rem.date)
                    val normTime = IranianPhoneUtils.convertDigitsToEnglish(rem.time)

                    val reminderAmountUnit = parseCalendarCurrencyUnit(rem.amountUnit)
                    val status = when (rem.status) {
                        "COMPLETED", "CANCELLED" -> FinancialEventStatus.PAID
                        else -> FinancialEventStatus.PENDING
                    }

                    allEvents.add(
                        FinancialEvent(
                            id = eventId,
                            title = rem.title,
                            description = rem.description,
                            type = when (rem.type.uppercase()) {
                                "INSTALLMENT", "LOAN" -> FinancialEventType.INSTALLMENT
                                "VEHICLE", "MAINTENANCE", "FUEL", "INSURANCE" -> FinancialEventType.VEHICLE
                                "FINANCE", "EXPENSE" -> FinancialEventType.EXPENSE
                                else -> FinancialEventType.REMINDER
                            },
                            amount = rem.amount.takeIf { reminderAmountUnit != null },
                            amountUnit = reminderAmountUnit ?: AppCurrency.TOMAN,
                            date = normDate,
                            time = normTime,
                            repeatType = "NONE",
                            reminderBefore = ReminderBeforeOption.NONE,
                            reminderLabel = if (rem.notificationEnabled || rem.fullScreenAlarmEnabled || rem.smsEnabled) "هشدار تنظیم شده" else null,
                            sourceId = rem.id.toString(),
                            status = status
                        )
                    )
                }
            }

            // 3. Cross-integrate the actual user-entered installment schedule.
            // Never synthesize an installment by dividing contract principal or
            // by using the contract's nextDueDate. Each calendar item corresponds
            // to one persisted installment payment row and keeps its entered amount/date.
            installmentData.first.forEach { inst ->
                val providerSuffix = if (inst.providerName.isNotBlank()) " (${inst.providerName})" else ""
                val title = "${inst.title}$providerSuffix"
                // The linked smart reminder is the source of truth for the user-selected
                // installment reminder time. Match both local and server IDs because the
                // reminder repository may persist either form as sourceId.
                val installmentReminderTime = reminders.firstOrNull { reminder ->
                    reminder.sourceType.equals("INSTALLMENT", ignoreCase = true) &&
                        (reminder.sourceId == inst.id.toString() ||
                            (!inst.serverId.isNullOrBlank() && reminder.sourceId == inst.serverId))
                }?.time
                    ?.takeIf { it.isNotBlank() }
                    ?.let { IranianPhoneUtils.convertDigitsToEnglish(it) }
                    ?: "09:00"

                installmentData.second.asSequence().filter { it.installmentId == inst.id }.forEach { payment ->
                    val paymentAmountUnit = parseCalendarCurrencyUnit(payment.amountUnit)
                    val eventId = "auto_inst_${inst.id}_${payment.id}"
                    if (seenIds.add(eventId)) {
                        val status = when (payment.status) {
                            "PAID", "COMPLETED" -> FinancialEventStatus.PAID
                            "OVERDUE" -> FinancialEventStatus.OVERDUE
                            else -> FinancialEventStatus.PENDING
                        }
                        allEvents.add(
                            FinancialEvent(
                                id = eventId,
                                title = title,
                                description = "قسط ${payment.installmentNumber} از ${inst.totalInstallments}",
                                type = FinancialEventType.INSTALLMENT,
                                amount = payment.amount.takeIf { paymentAmountUnit != null },
                                amountUnit = paymentAmountUnit ?: AppCurrency.TOMAN,
                                date = PersianCalendarHelper.fromEpochMillis(payment.dueDate).let {
                                    CalendarDateUtils.formatJalali(it.year, it.month, it.day)
                                },
                                time = installmentReminderTime,
                                repeatType = "NONE",
                                reminderBefore = ReminderBeforeOption.NONE,
                                sourceId = payment.id.toString(),
                                status = status
                            )
                        )
                    }
                }
            }

            // 4. Cross-integrate Vehicle Services
            vehicleData.first.forEach { svc ->
                val eventId = "auto_veh_svc_${svc.id}"
                if (seenIds.add(eventId)) {
                    // Calendar service-history cards must stay on the actual service date.
                    // The next reminder is represented by the reminder record below.
                    val targetDateStr = svc.date
                    val normDate = CalendarDateUtils.normalizeDate(targetDateStr)

                    allEvents.add(
                        FinancialEvent(
                            id = eventId,
                            title = "سرویس خودرو: ${svc.title}",
                            description = svc.description.ifBlank { "سرویس ${svc.serviceType.title}" },
                            type = FinancialEventType.VEHICLE,
                            amount = svc.cost,
                            amountUnit = svc.costUnit,
                            date = normDate,
                            // Vehicle service records store a date only; no service time is entered.
                            time = null,
                            repeatType = "NONE",
                            reminderBefore = ReminderBeforeOption.NONE,
                            reminderLabel = if (svc.isReminderEnabled && !svc.nextReminderDate.isNullOrBlank()) {
                                "در تاریخ تعیین‌شده"
                            } else {
                                null
                            },
                            sourceId = svc.id,
                            status = when (svc.status.uppercase()) {
                                "COMPLETED" -> FinancialEventStatus.PAID
                                "OVERDUE" -> FinancialEventStatus.OVERDUE
                                else -> FinancialEventStatus.PENDING
                            }
                        )
                    )
                }
            }

            // 5. Cross-integrate Vehicle Insurances
            vehicleData.second.forEach { ins ->
                val eventId = "auto_veh_ins_${ins.id}"
                if (seenIds.add(eventId)) {
                    val normDate = CalendarDateUtils.normalizeDate(ins.endDate)

                    allEvents.add(
                        FinancialEvent(
                            id = eventId,
                            title = "تمدید ${ins.type} خودرو",
                            description = "شرکت ${ins.company} - بیمه‌نامه: ${ins.policyNumber}",
                            type = FinancialEventType.VEHICLE,
                            amount = ins.amount,
                            amountUnit = ins.amountUnit,
                            date = normDate,
                            // Keep the configured vehicle reminder time; never inject a
                            // calendar-only default that differs from the user's setting.
                            time = ins.reminderTime.takeIf { ins.reminderEnabled }
                                ?.let { IranianPhoneUtils.convertDigitsToEnglish(it) },
                            repeatType = "YEARLY",
                            reminderBefore = ReminderBeforeOption.NONE,
                            reminderLabel = if (ins.reminderEnabled && ins.reminderDays.isNotEmpty()) {
                                ins.reminderDays.distinct().sortedDescending().joinToString("، ") { days ->
                                    if (days == 0) "روز سررسید"
                                    else "${IranianPhoneUtils.convertDigitsToPersian(days.toString())} روز قبل"
                                }
                            } else {
                                null
                            },
                            sourceId = ins.id,
                            status = FinancialEventStatus.PENDING
                        )
                    )
                }
            }

            // 6. Cross-integrate Vehicle Inspections
            vehicleData.third.forEach { insp ->
                val eventId = "auto_veh_insp_${insp.id}"
                if (seenIds.add(eventId)) {
                    val normDate = CalendarDateUtils.normalizeDate(insp.expiryDate)

                    allEvents.add(
                        FinancialEvent(
                            id = eventId,
                            title = "معاینه فنی خودرو",
                            description = "مرکز ${insp.centerName} (${insp.status})",
                            type = FinancialEventType.VEHICLE,
                            amount = insp.cost,
                            amountUnit = insp.costUnit,
                            date = normDate,
                            // Use the time saved with this inspection reminder.
                            time = insp.reminderTime.takeIf { insp.reminderEnabled }
                                ?.let { IranianPhoneUtils.convertDigitsToEnglish(it) },
                            repeatType = "YEARLY",
                            reminderBefore = ReminderBeforeOption.NONE,
                            reminderLabel = if (insp.reminderEnabled && insp.reminderDays.isNotEmpty()) {
                                insp.reminderDays.distinct().sortedDescending().joinToString("، ") { days ->
                                    if (days == 0) "روز سررسید"
                                    else "${IranianPhoneUtils.convertDigitsToPersian(days.toString())} روز قبل"
                                }
                            } else {
                                null
                            },
                            sourceId = insp.id,
                            status = FinancialEventStatus.PENDING
                        )
                    )
                }
            }

            // 7. Cross-integrate Vehicle Expenses
            vehicleData.fourth.forEach { exp ->
                val eventId = "auto_veh_exp_${exp.id}"
                if (seenIds.add(eventId)) {
                    val normDate = CalendarDateUtils.normalizeDate(exp.date)

                    allEvents.add(
                        FinancialEvent(
                            id = eventId,
                            title = "هزینه خودرو: ${exp.title}",
                            description = exp.description,
                            type = FinancialEventType.VEHICLE,
                            amount = exp.amount,
                            amountUnit = exp.amountUnit,
                            date = normDate,
                            time = "09:00",
                            repeatType = "NONE",
                            reminderBefore = ReminderBeforeOption.NONE,
                            sourceId = exp.id,
                            status = FinancialEventStatus.PAID
                        )
                    )
                }
            }

            allEvents.sortedWith(
                compareBy<FinancialEvent> { CalendarDateUtils.normalizeDate(it.date) }
                    .thenBy { it.time ?: "00:00" }
            )
        }.flowOn(Dispatchers.IO)
    }

    suspend fun addEvent(event: FinancialEvent) {
        if (!event.id.startsWith("auto_")) {
            repository.insertEvent(event)
            com.example.calendar.reminder.CalendarEventReminderScheduler(context).schedule(event)
        }
    }

    suspend fun updateEvent(event: FinancialEvent) {
        if (!event.id.startsWith("auto_")) {
            com.example.calendar.reminder.CalendarEventReminderScheduler(context).cancel(event.id)
            repository.updateEvent(event)
            com.example.calendar.reminder.CalendarEventReminderScheduler(context).schedule(event)
        } else {
            toggleStatus(event)
        }
    }

    suspend fun deleteEvent(id: String) {
        if (id.startsWith("auto_rem_")) {
            com.example.reminder.domain.ReminderManager(context).deleteReminder(id.removePrefix("auto_rem_"))
        } else if (!id.startsWith("auto_")) {
            com.example.calendar.reminder.CalendarEventReminderScheduler(context).cancel(id)
            repository.deleteEventById(id)
        }
    }

    suspend fun toggleStatus(event: FinancialEvent) {
        if (SessionManager.userId == null) return

        if (event.id.startsWith("auto_rem_")) {
            val reminderId = event.id.removePrefix("auto_rem_")
            val manager = com.example.reminder.domain.ReminderManager(context)
            if (event.status == FinancialEventStatus.PAID) {
                manager.restoreReminder(reminderId)
            } else {
                manager.completeReminder(reminderId)
            }
        } else if (event.id.startsWith("auto_veh_svc_")) {
            // A service can only be completed from the calendar while its persisted
            // lifecycle status is still pending. Reminder enablement is independent.
            if (event.status != FinancialEventStatus.PAID) {
                val svcId = event.id.removePrefix("auto_veh_svc_")
                val todayPersian = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
                VehicleRepository.instance.completeService(svcId, todayPersian)
            }
        } else if (event.id.startsWith("auto_")) {
            // Aggregated installment contracts, insurance, inspections and vehicle
            // expenses do not expose a valid per-event status mutation here.
            return
        } else {
            val newStatus = if (event.status == FinancialEventStatus.PAID) {
                FinancialEventStatus.PENDING
            } else {
                FinancialEventStatus.PAID
            }
            val updatedEvent = event.copy(status = newStatus)
            repository.updateEvent(updatedEvent)
            val scheduler = com.example.calendar.reminder.CalendarEventReminderScheduler(context)
            if (newStatus == FinancialEventStatus.PAID) scheduler.cancel(event.id) else scheduler.schedule(updatedEvent)
        }
    }
}

