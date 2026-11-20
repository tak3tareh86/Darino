package com.example.ui.screens.home.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.components.BottomNavItem
import com.example.ui.screens.home.domain.ObligationType
import com.example.ui.screens.home.domain.UpcomingObligationItem
import com.example.ui.screens.home.domain.UpcomingReminderItem
import com.example.ui.screens.installments.data.LocalInstallmentRepository
import com.example.ui.screens.installments.model.InstallmentCategory
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.screens.installments.model.InstallmentStatus
import com.example.ui.screens.installments.model.PaymentHistoryItem
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import kotlinx.coroutines.flow.firstOrNull
import com.example.util.IranianDateUtils
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.MoneyAmount
import java.math.BigInteger
import java.util.UUID
import com.example.util.PersianCalendarHelper
import com.example.vehicle.data.VehicleRepository

/**
 * Data repository for Home Dashboard.
 * Accesses underlying real modules cleanly without mock or hardcoded data.
 */
class HomeDashboardRepository(
    private val context: Context? = null,
    private val vehicleRepository: VehicleRepository = VehicleRepository.instance,
    private val financeRepository: LocalFinanceRepository = LocalFinanceRepository.instance,
    private val installmentRepository: LocalInstallmentRepository = LocalInstallmentRepository.instance
) {

    fun getUserFullName(): String {
        val user = SessionManager.currentUser
        return when {
            !user?.fullName.isNullOrBlank() -> user?.fullName ?: "کاربر گرامی"
            !user?.phoneNumber.isNullOrBlank() -> "کاربر ${user?.phoneNumber?.takeLast(4)}"
            else -> "کاربر گرامی"
        }
    }

    fun getMonthlyFinancials(): Triple<Long, Long, Long> {
        val transactions = financeRepository.getTransactions()
        val currentTxs = transactions.let {
            if (it is kotlinx.coroutines.flow.StateFlow) it.value else emptyList()
        }

        val currentPdt = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val currentYear = currentPdt.year
        val currentMonth = currentPdt.month

        var income = 0L
        var expense = 0L
        for (tx in currentTxs) {
            val isCurrentMonth = if (tx.dateMillis > 0L) {
                if (IranianDateUtils.isValidPersianDate(tx.datePersian)) {
                    val (py, pm, _) = IranianDateUtils.parsePersianDate(tx.datePersian)
                    py == currentYear && pm == currentMonth
                } else {
                    val txPdt = PersianCalendarHelper.fromEpochMillis(tx.dateMillis)
                    txPdt.year == currentYear && txPdt.month == currentMonth
                }
            } else if (tx.datePersian.isNotBlank()) {
                val (py, pm, _) = IranianDateUtils.parsePersianDate(tx.datePersian)
                py == currentYear && pm == currentMonth
            } else {
                false
            }

            if (!isCurrentMonth) continue

            when (tx.type) {
                TransactionType.INCOME -> income = Math.addExact(income, MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact())
                TransactionType.EXPENSE -> expense = Math.addExact(expense, MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact())
                TransactionType.TRANSFER -> {
                    // Transfers do not count towards income or expense
                }
            }
        }
        val balance = Math.subtractExact(income, expense)
        return Triple(income, expense, balance)
    }

    fun getSavingsRate(income: Long, expense: Long): Int {
        if (income <= 0) return 0
        val saved = BigInteger.valueOf(income).subtract(BigInteger.valueOf(expense)).max(BigInteger.ZERO)
        return saved.multiply(BigInteger.valueOf(100L))
            .divide(BigInteger.valueOf(income))
            .min(BigInteger.valueOf(100L))
            .toInt()
    }

    fun calculateDaysDifference(targetDatePersian: String): Int {
        return try {
            val (ty, tm, td) = IranianDateUtils.parsePersianDate(targetDatePersian)
            val targetMillis = PersianCalendarHelper.jalaliToEpochMillis(ty, tm, td, 12, 0)

            val nowPdt = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
            val todayMillis = PersianCalendarHelper.jalaliToEpochMillis(nowPdt.year, nowPdt.month, nowPdt.day, 12, 0)

            val diffMillis = targetMillis - todayMillis
            (diffMillis / (24L * 3600L * 1000L)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getUpcomingObligations(): List<UpcomingObligationItem> {
        val now = System.currentTimeMillis()
        val nowPdt = PersianCalendarHelper.fromEpochMillis(now)
        val todayEpochDay = PersianCalendarHelper.getJalaliEpochDay(
            nowPdt.year, nowPdt.month, nowPdt.day
        )
        val userId = SessionManager.userId
        val reminderDao = if (context != null && !userId.isNullOrBlank()) {
            runCatching { AppDatabase.getDatabase(context).smartReminderDao() }.getOrNull()
        } else null

        fun parsePersianDate(date: String): Triple<Int, Int, Int>? = runCatching {
            IranianDateUtils.parsePersianDate(date)
        }.getOrNull()

        fun parseReminderDateTime(date: String, time: String): Long? {
            val (year, month, day) = parsePersianDate(date) ?: return null
            val parts = IranianPhoneUtils.convertDigitsToEnglish(time)
                .trim()
                .split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val minute = parts.getOrNull(1)?.take(2)?.toIntOrNull() ?: 0
            if (hour !in 0..23 || minute !in 0..59) return null
            return runCatching {
                PersianCalendarHelper.jalaliToEpochMillis(year, month, day, hour, minute)
            }.getOrNull()
        }

        data class ObligationCandidate(
            val item: UpcomingObligationItem,
            val nextTriggerMillis: Long
        )

        val candidates = mutableListOf<ObligationCandidate>()
        val activeInstallments = installmentRepository.installments.value.filter {
            it.category in setOf(
                InstallmentCategory.BANK_LOANS,
                InstallmentCategory.HOME_LOANS,
                InstallmentCategory.CAR_INSURANCE,
                InstallmentCategory.MISC
            ) &&
                it.remainingInstallments > 0 &&
                it.status != InstallmentStatus.COMPLETED &&
                it.status != InstallmentStatus.PAID
        }

        for (installment in activeInstallments) {
            val reminder = if (reminderDao != null && !userId.isNullOrBlank()) {
                runCatching {
                    reminderDao.getReminderBySource(userId, "INSTALLMENT", installment.id)
                }.getOrNull()
            } else null
            val storedSchedules = if (reminder != null && reminder.status.equals("ACTIVE", ignoreCase = true) &&
                reminderDao != null && !userId.isNullOrBlank()
            ) {
                runCatching {
                    reminderDao.getSchedulesForReminderSync(userId, reminder.id)
                }.getOrDefault(emptyList())
            } else emptyList()

            val unpaidPayments = installment.paymentHistory.filter {
                it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED
            }
            val paymentRows = if (unpaidPayments.isNotEmpty()) unpaidPayments else listOf(null)

            for (payment in paymentRows) {
                val dueDate = payment?.dueDate?.takeIf { it.isNotBlank() } ?: installment.nextPaymentDate
                val parsedDate = parsePersianDate(dueDate) ?: continue
                val dueEpochDay = runCatching {
                    PersianCalendarHelper.getJalaliEpochDay(parsedDate.first, parsedDate.second, parsedDate.third)
                }.getOrNull() ?: continue

                // A commitment whose due date has passed must never appear in this card.
                if (dueEpochDay < todayEpochDay) continue

                val matchingSchedules = if (reminder != null && !reminder.sourceId.isNullOrBlank() && payment != null) {
                    val expectedPrefix = "installment:${reminder.sourceId}:${payment.id}:"
                    storedSchedules.filter { schedule ->
                        schedule.id == UUID.nameUUIDFromBytes(
                            "${expectedPrefix}${schedule.offsetValue}:${schedule.offsetUnit.uppercase()}"
                                .toByteArray(Charsets.UTF_8)
                        ).toString()
                    }
                } else emptyList()

                val nextStoredTrigger = matchingSchedules
                    .asSequence()
                    .filter { it.enabled && it.triggerDateTime > now }
                    .minByOrNull { it.triggerDateTime }

                // If schedules exist for this payment but all have already fired or are disabled,
                // do not resurrect an old trigger as the nearest reminder.
                val triggerMillis = when {
                    nextStoredTrigger != null -> nextStoredTrigger.triggerDateTime
                    matchingSchedules.isNotEmpty() -> null
                    else -> parseReminderDateTime(dueDate, reminder?.time ?: "09:00")
                } ?: continue

                if (triggerMillis <= now) continue

                val diff = (dueEpochDay - todayEpochDay).toInt()
                val relativeText = when {
                    diff == 0 -> "امروز"
                    diff == 1 -> "فردا"
                    else -> "${IranianPhoneUtils.convertDigitsToPersian(diff.toString())} روز دیگر"
                }
                val paymentAmount = if (payment != null && payment.amount > 0L) {
                    payment.amount
                } else {
                    installment.totalAmount / installment.totalInstallments.coerceAtLeast(1)
                }
                val formattedAmount = MoneyFormatter.formatExact(
                    MoneyAmount.of(paymentAmount, installment.amountUnit)
                )

                candidates.add(
                    ObligationCandidate(
                        item = UpcomingObligationItem(
                            id = "ob_loan_${installment.id}",
                            title = installment.title,
                            amount = paymentAmount,
                            amountUnit = installment.amountUnit,
                            formattedAmount = formattedAmount,
                            relativeDaysText = relativeText,
                            dueDatePersian = dueDate,
                            type = ObligationType.INSTALLMENT,
                            destinationTab = BottomNavItem.INSTALLMENTS,
                            iconRes = R.drawable.img_3d_bank,
                            accentColor = ColorUtils.blueAccent,
                            isOverdue = false,
                            rawId = installment.id,
                            bankName = installment.providerOrPerson.trim(),
                            totalInstallments = installment.totalInstallments,
                            remainingInstallments = installment.remainingInstallments,
                            scheduleItemId = payment?.id
                        ),
                        nextTriggerMillis = triggerMillis
                    )
                )
            }
        }

        // Pick the next future reminder by its actual scheduled epoch time.
        // This also advances to the next payment/reminder after an earlier trigger fires.
        return candidates
            .sortedWith(
                compareBy<ObligationCandidate> { it.nextTriggerMillis }
                    .thenBy { it.item.dueDatePersian }
                    .thenBy { it.item.id }
            )
            .map { it.item }
            .take(1)
    }

    suspend fun getUpcomingReminders(): List<UpcomingReminderItem> {
        val ctx = context ?: return emptyList()
        val userId = SessionManager.userId ?: return emptyList()
        val db = AppDatabase.getDatabase(ctx)
        val realReminders = try {
            db.smartReminderDao().getActiveRemindersList(userId)
        } catch (e: Exception) {
            emptyList()
        }

        if (realReminders.isEmpty()) {
            return emptyList()
        }

        val now = System.currentTimeMillis()
        val installments = installmentRepository.installments.value.associateBy { it.id }

        val itemsWithDiff = realReminders.mapNotNull { rem ->
            if (rem.status == "COMPLETED" || rem.status == "CANCELLED") return@mapNotNull null

            // Installment reminders have one stable schedule ID per payment and offset.
            // Resolve the selected payment by that ID; never associate a trigger with a
            // different installment payment merely because it is the nearest timestamp.
            val isInstallmentReminder =
                rem.sourceType.equals("INSTALLMENT", ignoreCase = true) ||
                    rem.type.equals("INSTALLMENT", ignoreCase = true)
            val installmentData = if (isInstallmentReminder) {
                rem.sourceId?.let { sourceId -> installments[sourceId] }
            } else null

            val installmentSchedule = if (isInstallmentReminder && installmentData != null && !rem.sourceId.isNullOrBlank()) {
                val schedules = runCatching {
                    db.smartReminderDao().getSchedulesForReminderSync(userId, rem.id)
                }.getOrDefault(emptyList())

                installmentData.paymentHistory
                    .asSequence()
                    .filter { payment ->
                        payment.status != InstallmentStatus.PAID &&
                            payment.status != InstallmentStatus.COMPLETED
                    }
                    .flatMap { payment ->
                        schedules.asSequence()
                            .filter { schedule ->
                                if (!schedule.enabled || schedule.triggerDateTime <= now) {
                                    false
                                } else {
                                    val expectedId = UUID.nameUUIDFromBytes(
                                        "installment:${rem.sourceId}:${payment.id}:${schedule.offsetValue}:${schedule.offsetUnit.uppercase()}"
                                            .toByteArray(Charsets.UTF_8)
                                    ).toString()
                                    schedule.id == expectedId
                                }
                            }
                            .map { schedule -> payment to schedule }
                    }
                    .minByOrNull { (_, schedule) -> schedule.triggerDateTime }
            } else null

            val fallbackUnpaidPayment = installmentData?.let { installment ->
                installment.paymentHistory
                    .asSequence()
                    .filter { it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED }
                    .minByOrNull { payment ->
                        calculateDaysDifference(payment.dueDate.ifBlank { installment.nextPaymentDate })
                    }
            }

            val installmentDueDate = if (isInstallmentReminder) {
                (installmentSchedule?.first?.dueDate
                    ?.takeIf { it.isNotBlank() }
                    ?: fallbackUnpaidPayment?.dueDate?.takeIf { it.isNotBlank() }
                    ?: installmentData?.nextPaymentDate?.takeIf { it.isNotBlank() }
                    ?: rem.date)
            } else null

            val actualTriggerMillis = installmentSchedule?.second?.triggerDateTime
            val actualReminderDate = actualTriggerMillis?.let { trigger ->
                PersianCalendarHelper.fromEpochMillis(trigger).toFormattedDate()
            }
            val actualReminderTime = actualTriggerMillis?.let { trigger ->
                PersianCalendarHelper.fromEpochMillis(trigger).toFormattedTime()
            }

            val effectiveDateForUrgency = actualReminderDate ?: installmentDueDate ?: rem.date
            val diff = calculateDaysDifference(effectiveDateForUrgency)
            val relativeText = when {
                diff < 0 -> "سررسید گذشته"
                diff == 0 -> "امروز"
                diff == 1 -> "فردا"
                else -> "${IranianPhoneUtils.convertDigitsToPersian(diff.toString())} روز دیگر"
            }
            val typeTitle = when (rem.type) {
                "INSTALLMENT" -> "قسط و وام"
                "VEHICLE", "SERVICE" -> "سرویس خودرو"
                "INSURANCE" -> "بیمه"
                "CHECK" -> "چک صیادی"
                else -> "یادآوری شخصی"
            }
            val iconRes = when (rem.type) {
                "INSTALLMENT" -> R.drawable.img_3d_bank
                "VEHICLE", "SERVICE" -> R.drawable.img_3d_oil
                "INSURANCE" -> R.drawable.img_3d_insurance
                else -> R.drawable.img_3d_bell_notification
            }
            val accentColor = when (rem.type) {
                "INSTALLMENT" -> ColorUtils.blueAccent
                "VEHICLE", "SERVICE" -> ExpenseRoseLight
                "INSURANCE" -> Color(0xFFF59E0B)
                else -> Color(0xFF6366F1)
            }

            Pair(
                UpcomingReminderItem(
                    id = rem.id,
                    title = rem.title,
                    relativeDaysText = relativeText,
                    timePersian = if (isInstallmentReminder) actualReminderTime.orEmpty() else rem.time,
                    typeTitle = typeTitle,
                    iconRes = iconRes,
                    accentColor = accentColor,
                    datePersian = installmentDueDate ?: rem.date,
                    dueDatePersian = installmentDueDate,
                    reminderDatePersian = actualReminderDate
                ),
                diff
            )
        }

        // Keep the existing overdue-first ordering, now using the actual next
        // installment trigger date when one is available.
        return itemsWithDiff.sortedWith(
            compareBy<Pair<UpcomingReminderItem, Int>> { (_, diff) ->
                if (diff < 0) 0 else 1
            }.thenBy { (_, diff) ->
                diff
            }.thenBy { (item, _) ->
                item.timePersian
            }
        ).map { it.first }.take(3)
    }

    suspend fun getUnreadNotificationCount(): Int {
        val ctx = context ?: return 0
        val userId = SessionManager.userId ?: return 0
        return try {
            val db = AppDatabase.getDatabase(ctx)
            db.notificationLogDao().getUnreadCount(userId).firstOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }
}

object ColorUtils {
    val blueAccent = Color(0xFF2563EB)
}
