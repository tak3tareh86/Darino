package com.example.ui.screens.home.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.reminder.domain.ReminderManager
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
        val nowPdt = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val todayEpochDay = PersianCalendarHelper.getJalaliEpochDay(
            nowPdt.year, nowPdt.month, nowPdt.day
        )

        fun parsePersianDate(date: String): Triple<Int, Int, Int>? = runCatching {
            IranianDateUtils.parsePersianDate(date)
        }.getOrNull()

        data class ObligationCandidate(
            val item: UpcomingObligationItem,
            val dueEpochDay: Long,
            val installmentNumber: Int
        )

        val candidates = mutableListOf<ObligationCandidate>()
        val activeInstallments = installmentRepository.installments.value.filter { installment ->
            installment.category in setOf(
                InstallmentCategory.BANK_LOANS,
                InstallmentCategory.HOME_LOANS,
                InstallmentCategory.CAR_INSURANCE,
                InstallmentCategory.MISC
            ) &&
                installment.remainingInstallments > 0 &&
                installment.status != InstallmentStatus.COMPLETED &&
                installment.status != InstallmentStatus.PAID
        }

        for (installment in activeInstallments) {
            val unpaidPayments = installment.paymentHistory.filter { payment ->
                payment.status != InstallmentStatus.PAID &&
                    payment.status != InstallmentStatus.COMPLETED
            }

            // Use the actual unpaid payment rows. The legacy nextPaymentDate fallback
            // is only valid when no payment history exists at all.
            val paymentRows: List<PaymentHistoryItem?> = when {
                installment.paymentHistory.isEmpty() -> listOf(null)
                else -> unpaidPayments
            }

            for (payment in paymentRows) {
                val dueDate = payment?.dueDate?.takeIf { it.isNotBlank() }
                    ?: installment.nextPaymentDate.takeIf {
                        payment == null && installment.paymentHistory.isEmpty()
                    }
                    ?: continue
                val parsedDate = parsePersianDate(dueDate) ?: continue
                val dueEpochDay = runCatching {
                    PersianCalendarHelper.getJalaliEpochDay(
                        parsedDate.first, parsedDate.second, parsedDate.third
                    )
                }.getOrNull() ?: continue

                val diffDays = (dueEpochDay - todayEpochDay).toInt()
                val relativeText = when {
                    diffDays < 0 -> "${IranianPhoneUtils.convertDigitsToPersian(kotlin.math.abs(diffDays).toString())} روز گذشته"
                    diffDays == 0 -> "امروز"
                    diffDays == 1 -> "فردا"
                    else -> "${IranianPhoneUtils.convertDigitsToPersian(diffDays.toString())} روز دیگر"
                }

                val paymentAmount = payment?.amount?.takeIf { it > 0L }
                    ?: if (payment == null) {
                        installment.totalAmount / installment.totalInstallments.coerceAtLeast(1)
                    } else {
                        continue
                    }
                val formattedAmount = MoneyFormatter.formatExact(
                    MoneyAmount.of(paymentAmount, installment.amountUnit)
                )
                val paymentKey = payment?.id?.takeIf { it.isNotBlank() }
                    ?: "next_${dueDate.replace('/', '_')}"
                val obligationId = "ob_loan_${installment.id}_$paymentKey"

                candidates.add(
                    ObligationCandidate(
                        item = UpcomingObligationItem(
                            id = obligationId,
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
                            isOverdue = diffDays < 0,
                            rawId = installment.id,
                            bankName = installment.providerOrPerson.trim(),
                            totalInstallments = installment.totalInstallments,
                            remainingInstallments = installment.remainingInstallments,
                            scheduleItemId = payment?.id
                        ),
                        dueEpochDay = dueEpochDay,
                        installmentNumber = payment?.installmentNumber ?: Int.MAX_VALUE
                    )
                )
            }
        }

        // This card is a list of unpaid loan installments, not a reminder list.
        // Sort by the actual due date, then by installment number for same-day items.
        return candidates
            .sortedWith(
                compareBy<ObligationCandidate> { it.dueEpochDay }
                    .thenBy { it.installmentNumber }
                    .thenBy { it.item.rawId }
                    .thenBy { it.item.id }
            )
            .map { it.item }
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
        if (realReminders.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        val installments = installmentRepository.installments.value.associateBy { it.id }

        // Keep the exact timestamp used for sorting; date-only sorting can put a
        // later alert ahead of an earlier alert when they fall on different days.
        val itemsWithTrigger = realReminders.mapNotNull { reminder ->
            if (!reminder.status.equals("ACTIVE", ignoreCase = true)) return@mapNotNull null

            val isInstallmentReminder =
                reminder.sourceType.equals("INSTALLMENT", ignoreCase = true) ||
                    reminder.type.equals("INSTALLMENT", ignoreCase = true)

            var paymentId: String? = null
            var dueDate: String? = null
            var triggerMillis: Long? = null

            if (isInstallmentReminder) {
                val installment = reminder.sourceId?.let(installments::get)
                    ?: return@mapNotNull null
                val schedules = runCatching {
                    db.smartReminderDao().getSchedulesForReminderSync(userId, reminder.id)
                }.getOrDefault(emptyList())

                // An installment appears only when an actual, user-configured,
                // enabled future trigger exists for one of its unpaid payments.
                // Never substitute the payment due date for a missing/past alert.
                val nextOccurrence = installment.paymentHistory
                    .asSequence()
                    .filter { it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED }
                    .flatMap { payment ->
                        schedules.asSequence()
                            .filter { schedule ->
                                if (!schedule.enabled || schedule.triggerDateTime <= now) {
                                    false
                                } else {
                                    val expectedId = UUID.nameUUIDFromBytes(
                                        "installment:${reminder.sourceId}:${payment.id}:${schedule.offsetValue}:${schedule.offsetUnit.uppercase()}"
                                            .toByteArray(Charsets.UTF_8)
                                    ).toString()
                                    schedule.id == expectedId
                                }
                            }
                            .map { schedule -> Triple(payment, schedule, schedule.triggerDateTime) }
                    }
                    .minByOrNull { it.third }
                    ?: return@mapNotNull null

                paymentId = nextOccurrence.first.id
                dueDate = nextOccurrence.first.dueDate
                triggerMillis = nextOccurrence.third
            } else {
                val parsedTriggerMillis: Long? = try {
                    ReminderManager.parsePersianDateTimeToMillis(reminder.date, reminder.time)
                } catch (exception: Exception) {
                    null
                }
                triggerMillis = parsedTriggerMillis
                if (parsedTriggerMillis == null || parsedTriggerMillis <= now) return@mapNotNull null
            }

            val actualTrigger = triggerMillis ?: return@mapNotNull null
            if (actualTrigger <= now) return@mapNotNull null

            val triggerDate = PersianCalendarHelper.fromEpochMillis(actualTrigger)
            val diff = calculateDaysDifference(triggerDate.toFormattedDate())
            val relativeText = when {
                diff < 0 -> "سررسید گذشته"
                diff == 0 -> "امروز"
                diff == 1 -> "فردا"
                else -> "${IranianPhoneUtils.convertDigitsToPersian(diff.toString())} روز دیگر"
            }
            val typeTitle = when (reminder.type) {
                "INSTALLMENT" -> "قسط و وام"
                "VEHICLE", "SERVICE" -> "سرویس خودرو"
                "INSURANCE" -> "بیمه"
                "CHECK" -> "چک صیادی"
                else -> "یادآوری شخصی"
            }
            val iconRes = when (reminder.type) {
                "INSTALLMENT" -> R.drawable.img_3d_bank
                "VEHICLE", "SERVICE" -> R.drawable.img_3d_oil
                "INSURANCE" -> R.drawable.img_3d_insurance
                else -> R.drawable.img_3d_bell_notification
            }
            val accentColor = when (reminder.type) {
                "INSTALLMENT" -> ColorUtils.blueAccent
                "VEHICLE", "SERVICE" -> ExpenseRoseLight
                "INSURANCE" -> Color(0xFFF59E0B)
                else -> Color(0xFF6366F1)
            }

            val item = UpcomingReminderItem(
                id = reminder.id,
                title = reminder.title,
                relativeDaysText = relativeText,
                timePersian = if (isInstallmentReminder) triggerDate.toFormattedTime() else reminder.time,
                typeTitle = typeTitle,
                iconRes = iconRes,
                accentColor = accentColor,
                datePersian = dueDate ?: reminder.date,
                dueDatePersian = dueDate,
                reminderDatePersian = if (isInstallmentReminder) triggerDate.toFormattedDate() else null,
                installmentPaymentId = paymentId
            )
            item to actualTrigger
        }

        return itemsWithTrigger
            .sortedBy { it.second }
            .map { it.first }
            .take(3)
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
