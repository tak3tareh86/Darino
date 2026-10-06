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
        val installmentsFlow = database.installmentDao().getAllInstallments(userId)
        val vehicleServicesFlow = VehicleRepository.instance.services
        val vehicleInsurancesFlow = VehicleRepository.instance.insurances
        val vehicleInspectionsFlow = VehicleRepository.instance.inspections
        val vehicleExpensesFlow = VehicleRepository.instance.expenses

        return combine(
            dbEventsFlow,
            remindersFlow,
            installmentsFlow,
            combine(
                vehicleServicesFlow,
                vehicleInsurancesFlow,
                vehicleInspectionsFlow,
                vehicleExpensesFlow
            ) { svcs, ins, insp, exp ->
                Quadruple(svcs, ins, insp, exp)
            }
        ) { dbEvents, reminders, installments, vehicleData ->
            val allEvents = mutableListOf<FinancialEvent>()
            val seenIds = mutableSetOf<String>()

            // 1. Add DB events from financial_events table
            for (ev in dbEvents) {
                if (seenIds.add(ev.id)) {
                    allEvents.add(ev)
                }
            }

            // 2. Cross-integrate Smart Reminders
            reminders.forEach { rem ->
                val eventId = "auto_rem_${rem.id}"
                if (seenIds.add(eventId)) {
                    val normDate = CalendarDateUtils.normalizeDate(rem.date)
                    val normTime = IranianPhoneUtils.convertDigitsToEnglish(rem.time)

                    val status = when (rem.status) {
                        "COMPLETED", "CANCELLED" -> FinancialEventStatus.PAID
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
                            sourceId = rem.id.toString(),
                            status = status
                        )
                    )
                }
            }

            // 3. Cross-integrate Installments / Loans
            installments.forEach { inst ->
                val eventId = "auto_inst_${inst.id}"
                if (seenIds.add(eventId)) {
                    val formattedDate = if (inst.nextDueDate > 0) {
                        CalendarDateUtils.normalizeDate(
                            PersianCalendarHelper.fromEpochMillis(inst.nextDueDate).toFormattedDate()
                        )
                    } else {
                        CalendarDateUtils.normalizeDate(
                            PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
                        )
                    }

                    val providerSuffix = if (inst.providerName.isNotBlank()) " (${inst.providerName})" else ""
                    val title = "${inst.title}$providerSuffix"
                    val desc = "اقساط ${inst.title} - پرداخت شده: ${inst.paidInstallments} از ${inst.totalInstallments}"

                    val monthlyAmount = if (inst.totalInstallments > 0) {
                        inst.amount / inst.totalInstallments
                    } else {
                        inst.amount
                    }

                    val status = when (inst.status) {
                        "PAID", "COMPLETED" -> FinancialEventStatus.PAID
                        "OVERDUE" -> FinancialEventStatus.OVERDUE
                        else -> FinancialEventStatus.PENDING
                    }

                    allEvents.add(
                        FinancialEvent(
                            id = eventId,
                            title = title,
                            description = desc,
                            type = FinancialEventType.INSTALLMENT,
                            amount = monthlyAmount,
                            date = formattedDate,
                            time = "09:00",
                            repeatType = "MONTHLY",
                            reminderBefore = ReminderBeforeOption.THREE_DAYS,
                            sourceId = inst.id.toString(),
                            status = status
                        )
                    )
                }
            }

            // 4. Cross-integrate Vehicle Services
            vehicleData.first.forEach { svc ->
                val eventId = "auto_veh_svc_${svc.id}"
                if (seenIds.add(eventId)) {
                    val targetDateStr = svc.nextReminderDate?.takeIf { it.isNotBlank() } ?: svc.date
                    val normDate = CalendarDateUtils.normalizeDate(targetDateStr)

                    allEvents.add(
                        FinancialEvent(
                            id = eventId,
                            title = "سرویس خودرو: ${svc.title}",
                            description = svc.description.ifBlank { "سرویس ${svc.serviceType.title}" },
                            type = FinancialEventType.VEHICLE,
                            amount = svc.cost,
                            date = normDate,
                            time = "09:00",
                            repeatType = "NONE",
                            reminderBefore = ReminderBeforeOption.THREE_DAYS,
                            sourceId = svc.id,
                            status = if (svc.isReminderEnabled) {
                                FinancialEventStatus.PENDING
                            } else {
                                FinancialEventStatus.PAID
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
                            date = normDate,
                            time = "09:00",
                            repeatType = "YEARLY",
                            reminderBefore = ReminderBeforeOption.SEVEN_DAYS,
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
                            date = normDate,
                            time = "09:00",
                            repeatType = "YEARLY",
                            reminderBefore = ReminderBeforeOption.SEVEN_DAYS,
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
                            date = normDate,
                            time = "09:00",
                            repeatType = "NONE",
                            reminderBefore = ReminderBeforeOption.ONE_DAY,
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
        }
    }

    suspend fun updateEvent(event: FinancialEvent) {
        if (!event.id.startsWith("auto_")) {
            repository.updateEvent(event)
        } else {
            toggleStatus(event)
        }
    }

    suspend fun deleteEvent(id: String) {
        if (!id.startsWith("auto_")) {
            repository.deleteEventById(id)
        }
    }

    suspend fun toggleStatus(event: FinancialEvent) {
        val database = AppDatabase.getDatabase(context)
        val userId = SessionManager.userId ?: return

        if (event.id.startsWith("auto_rem_")) {
            val reminderId = event.id.removePrefix("auto_rem_")
            val newReminderStatus = if (event.status == FinancialEventStatus.PAID) "ACTIVE" else "COMPLETED"
            database.smartReminderDao().updateStatus(
                userId = userId,
                id = reminderId,
                status = newReminderStatus,
                completedAt = if (newReminderStatus == "COMPLETED") System.currentTimeMillis() else null,
                cancelledAt = null
            )
        } else if (event.id.startsWith("auto_inst_")) {
            val instIdStr = event.id.removePrefix("auto_inst_")
            val instId = instIdStr.toIntOrNull()
            if (instId != null) {
                val inst = database.installmentDao().getInstallmentById(userId, instId)
                if (inst != null) {
                    val newStatus = if (event.status == FinancialEventStatus.PAID) "PENDING" else "PAID"
                    database.installmentDao().updateInstallment(inst.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
                    com.example.ui.screens.installments.data.LocalInstallmentRepository.instance.reloadFromDatabase(context)
                }
            }
        } else if (event.id.startsWith("auto_veh_")) {
            if (event.id.startsWith("auto_veh_svc_")) {
                val svcId = event.id.removePrefix("auto_veh_svc_")
                val todayPersian = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
                VehicleRepository.instance.completeService(svcId, todayPersian)
            }
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

