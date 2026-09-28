package com.example.calendar.domain

import android.content.Context
import com.example.calendar.data.FinancialCalendarRepository
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventStatus
import com.example.calendar.domain.model.FinancialEventType
import com.example.calendar.domain.model.ReminderBeforeOption
import com.example.ui.screens.installments.model.InstallmentMockDataSource
import com.example.ui.screens.installments.model.InstallmentStatus
import com.example.ui.screens.vehicle.model.VehicleMockDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CalendarManager(private val context: Context) {

    private val repository = FinancialCalendarRepository(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            seedInitialEventsIfEmpty()
        }
    }

    private suspend fun seedInitialEventsIfEmpty() {
        if (repository.getCount() == 0) {
            val (currentYear, currentMonth, _) = CalendarDateUtils.getCurrentJalaliDate()
            val initialList = mutableListOf<FinancialEvent>()

            // 1. Acceptance test specific event: قسط بانک مهر
            // 5 شهریور / 15 شهریور
            val monthStr = currentMonth.toString().padStart(2, '0')
            val yearStr = currentYear.toString()

            initialList.add(
                FinancialEvent(
                    id = "init_loan_mehr_5",
                    title = "قسط بانک مهر",
                    description = "سررسید قسط تسهیلات بانک مهر اقتصاد - کسر از حساب سپهر",
                    type = FinancialEventType.INSTALLMENT,
                    amount = 3_000_000,
                    date = "$yearStr/$monthStr/05",
                    time = "09:00",
                    repeatType = "MONTHLY",
                    reminderBefore = ReminderBeforeOption.THREE_DAYS,
                    sourceId = "inst_mehr",
                    status = FinancialEventStatus.PAID
                )
            )

            initialList.add(
                FinancialEvent(
                    id = "init_car_insurance_12",
                    title = "بیمه خودرو پژو ۲۰۶",
                    description = "سررسید قسط بیمه‌نامه شخص ثالث بیمه ایران",
                    type = FinancialEventType.VEHICLE,
                    amount = 850_000,
                    date = "$yearStr/$monthStr/12",
                    time = "11:30",
                    repeatType = "NONE",
                    reminderBefore = ReminderBeforeOption.SEVEN_DAYS,
                    sourceId = "veh_ins_206",
                    status = FinancialEventStatus.PAID
                )
            )

            initialList.add(
                FinancialEvent(
                    id = "init_loan_mehr_15",
                    title = "قسط وام بانک مهر",
                    description = "قسط شماره ۱۲ تسهیلات قرض‌الحسنه بانک مهر ایران",
                    type = FinancialEventType.INSTALLMENT,
                    amount = 3_000_000,
                    date = "$yearStr/$monthStr/15",
                    time = "10:00",
                    repeatType = "MONTHLY",
                    reminderBefore = ReminderBeforeOption.ONE_DAY,
                    sourceId = "inst_mehr_15",
                    status = FinancialEventStatus.PENDING,
                    isDueSoon = true
                )
            )

            initialList.add(
                FinancialEvent(
                    id = "init_car_service_20",
                    title = "سرویس دوره‌ای خودرو",
                    description = "تعویض روغن موتور، فیلتر هوا و فیلتر روغن (تعمیرگاه البرز)",
                    type = FinancialEventType.VEHICLE,
                    amount = 750_000,
                    date = "$yearStr/$monthStr/20",
                    time = "16:00",
                    repeatType = "NONE",
                    reminderBefore = ReminderBeforeOption.ONE_DAY,
                    sourceId = "veh_oil_206",
                    status = FinancialEventStatus.PENDING
                )
            )

            initialList.add(
                FinancialEvent(
                    id = "init_bill_payment_25",
                    title = "پرداخت قبوض و شارژ ساختمان",
                    description = "پرداخت قبض برق، گاز و شارژ ماهیانه واحد مسکونی",
                    type = FinancialEventType.EXPENSE,
                    amount = 580_000,
                    date = "$yearStr/$monthStr/25",
                    time = "10:00",
                    repeatType = "MONTHLY",
                    reminderBefore = ReminderBeforeOption.ONE_DAY,
                    sourceId = "exp_bill_25",
                    status = FinancialEventStatus.PENDING
                )
            )

            // Vehicle Inspection (معاینه فنی ۲۵ آبان)
            val nextMonthStr = ((currentMonth + 2).let { if (it > 12) it - 12 else it }).toString().padStart(2, '0')
            initialList.add(
                FinancialEvent(
                    id = "init_car_inspection_aban",
                    title = "معاینه فنی خودرو پژو ۲۰۶",
                    description = "مرکز معاینه فنی بیهقی - بررسی ترمزها و آلایندگی",
                    type = FinancialEventType.VEHICLE,
                    amount = 320_000,
                    date = "$yearStr/$nextMonthStr/25",
                    time = "08:30",
                    repeatType = "YEARLY",
                    reminderBefore = ReminderBeforeOption.SEVEN_DAYS,
                    sourceId = "veh_inspect",
                    status = FinancialEventStatus.PENDING
                )
            )

            // Reminder example: تمدید قرارداد و یادآور شخصی
            initialList.add(
                FinancialEvent(
                    id = "init_personal_reminder_18",
                    title = "یادآور: پیگیری واریز سود سپرده",
                    description = "بررسی پیامک سود ماهانه بانک ملت و انتقال به صندوق",
                    type = FinancialEventType.REMINDER,
                    amount = null,
                    date = "$yearStr/$monthStr/18",
                    time = "14:00",
                    repeatType = "MONTHLY",
                    reminderBefore = ReminderBeforeOption.ONE_DAY,
                    sourceId = "rem_profit",
                    status = FinancialEventStatus.PENDING
                )
            )

            repository.insertEvents(initialList)
        }
    }

    /**
     * Streams all unified events: persisted repository events + dynamic auto-synced
     * items from Installments and Vehicle services to guarantee complete cross-app sync.
     */
    fun getAllEvents(): Flow<List<FinancialEvent>> {
        return repository.getAllEvents().map { dbEvents ->
            val allEvents = mutableListOf<FinancialEvent>()
            val seenIds = mutableSetOf<String>()

            // 1. Add DB events
            for (ev in dbEvents) {
                seenIds.add(ev.id)
                allEvents.add(ev)
            }

            // 2. Cross-integrate from Installments module
            val (currentYear, currentMonth, _) = CalendarDateUtils.getCurrentJalaliDate()
            val monthStr = currentMonth.toString().padStart(2, '0')

            InstallmentMockDataSource.allInstallments.forEach { inst ->
                val eventId = "auto_inst_${inst.id}"
                if (!seenIds.contains(eventId) && !seenIds.contains(inst.id)) {
                    // Extract day from nextPaymentDate if possible, e.g. "۱۴۰۴/۰۷/۱۵"
                    val parsed = CalendarDateUtils.parseJalali(inst.nextPaymentDate)
                    val targetDate = if (parsed != null) {
                        CalendarDateUtils.formatJalali(currentYear, currentMonth, parsed.third)
                    } else {
                        CalendarDateUtils.formatJalali(currentYear, currentMonth, 15)
                    }

                    val status = when (inst.status) {
                        InstallmentStatus.PAID, InstallmentStatus.COMPLETED -> FinancialEventStatus.PAID
                        InstallmentStatus.OVERDUE -> FinancialEventStatus.OVERDUE
                        else -> FinancialEventStatus.PENDING
                    }

                    val monthlyAmount = inst.monthlyPaymentFormatted
                        .replace(" تومان", "")
                        .replace(",", "")
                        .replace("،", "")
                        .trim()
                    val amountLong = com.example.util.IranianPhoneUtils.convertDigitsToEnglish(monthlyAmount).toLongOrNull()

                    val autoEvent = FinancialEvent(
                        id = eventId,
                        title = inst.title,
                        description = "قسط ${inst.providerOrPerson} - مانده: ${inst.remainingInstallments} قسط",
                        type = FinancialEventType.INSTALLMENT,
                        amount = amountLong ?: 3_000_000L,
                        date = targetDate,
                        time = "10:00",
                        repeatType = "MONTHLY",
                        reminderBefore = ReminderBeforeOption.THREE_DAYS,
                        sourceId = inst.id,
                        status = status,
                        isDueSoon = inst.status == InstallmentStatus.DUE_SOON,
                        isOverdue = inst.status == InstallmentStatus.OVERDUE
                    )
                    allEvents.add(autoEvent)
                    seenIds.add(eventId)
                }
            }

            // 3. Cross-integrate from Vehicle module
            VehicleMockDataSource.sampleVehicles.forEach { vehicle ->
                val insuranceEventId = "auto_veh_ins_${vehicle.id}"
                if (!seenIds.contains(insuranceEventId)) {
                    val insParsed = CalendarDateUtils.parseJalali(vehicle.insurance.expiryDatePersian)
                    val insDate = if (insParsed != null) {
                        CalendarDateUtils.formatJalali(currentYear, currentMonth, insParsed.third)
                    } else {
                        CalendarDateUtils.formatJalali(currentYear, currentMonth, 10)
                    }

                    allEvents.add(
                        FinancialEvent(
                            id = insuranceEventId,
                            title = "بیمه ${vehicle.name} (${vehicle.insurance.provider})",
                            description = "تمدید ${vehicle.insurance.title} - پلاک ${vehicle.licensePlate}",
                            type = FinancialEventType.VEHICLE,
                            amount = 2_850_000L,
                            date = insDate,
                            time = "11:00",
                            repeatType = "YEARLY",
                            reminderBefore = ReminderBeforeOption.SEVEN_DAYS,
                            sourceId = vehicle.id,
                            status = if (vehicle.insurance.isExpiringSoon) FinancialEventStatus.PENDING else FinancialEventStatus.PAID,
                            isDueSoon = vehicle.insurance.isExpiringSoon
                        )
                    )
                    seenIds.add(insuranceEventId)
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
        val newStatus = if (event.status == FinancialEventStatus.PAID) {
            FinancialEventStatus.PENDING
        } else {
            FinancialEventStatus.PAID
        }
        repository.updateEvent(event.copy(status = newStatus))
    }
}
