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
                TransactionType.INCOME -> income += tx.amount
                TransactionType.EXPENSE -> expense += tx.amount
                TransactionType.TRANSFER -> {
                    // Transfers do not count towards income or expense
                }
            }
        }
        val balance = income - expense
        return Triple(income, expense, balance)
    }

    fun getSavingsRate(income: Long, expense: Long): Int {
        if (income <= 0) return 0
        val saved = (income - expense).coerceAtLeast(0)
        return ((saved.toDouble() / income.toDouble()) * 100).toInt()
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

    fun getUpcomingObligations(): List<UpcomingObligationItem> {
        val list = mutableListOf<UpcomingObligationItem>()

        // 1. Only Real Bank Installments from LocalInstallmentRepository
        val realBankInstallments = installmentRepository.installments.value.filter {
            it.category == InstallmentCategory.BANK_LOANS &&
            it.remainingInstallments > 0 &&
            it.status != InstallmentStatus.COMPLETED &&
            it.status != InstallmentStatus.PAID
        }
        for (item in realBankInstallments) {
            val unpaidPayments = item.paymentHistory.filter {
                it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED
            }
            val nearestUnpaid = if (unpaidPayments.isNotEmpty()) {
                unpaidPayments.minWithOrNull(
                    compareBy<PaymentHistoryItem> { p ->
                        val date = p.dueDate.ifBlank { item.nextPaymentDate }
                        val diff = calculateDaysDifference(date)
                        if (diff < 0) 0 else if (diff == 0) 1 else 2
                    }.thenBy { p ->
                        val date = p.dueDate.ifBlank { item.nextPaymentDate }
                        calculateDaysDifference(date)
                    }.thenBy { p ->
                        p.dueDate.ifBlank { item.nextPaymentDate }
                    }.thenBy { p ->
                        p.installmentNumber
                    }
                )
            } else null

            val effectiveDueDate = nearestUnpaid?.dueDate?.ifBlank { item.nextPaymentDate } ?: item.nextPaymentDate
            val paymentAmount = if (nearestUnpaid != null && nearestUnpaid.amount > 0L) {
                nearestUnpaid.amount
            } else {
                item.totalAmount / item.totalInstallments.coerceAtLeast(1)
            }
            val formattedAmount = MoneyFormatter.formatToman(paymentAmount)
            val bankName = item.providerOrPerson.trim()

            val diff = calculateDaysDifference(effectiveDueDate)
            val isOverdue = diff < 0 || item.status == InstallmentStatus.OVERDUE
            val relativeText = when {
                diff < 0 -> "${IranianPhoneUtils.convertDigitsToPersian((-diff).toString())} روز گذشته (معوق)"
                diff == 0 -> "امروز"
                diff == 1 -> "فردا"
                else -> "${IranianPhoneUtils.convertDigitsToPersian(diff.toString())} روز دیگر"
            }

            list.add(
                UpcomingObligationItem(
                    id = "ob_loan_${item.id}",
                    title = item.title,
                    amount = paymentAmount,
                    formattedAmount = formattedAmount,
                    relativeDaysText = relativeText,
                    dueDatePersian = effectiveDueDate,
                    type = ObligationType.INSTALLMENT,
                    destinationTab = BottomNavItem.INSTALLMENTS,
                    iconRes = R.drawable.img_3d_bank,
                    accentColor = ColorUtils.blueAccent,
                    isOverdue = isOverdue,
                    rawId = item.id,
                    bankName = bankName,
                    totalInstallments = item.totalInstallments,
                    remainingInstallments = item.remainingInstallments,
                    scheduleItemId = nearestUnpaid?.id
                )
            )
        }

        // Sort by priority/urgency:
        // 1. Overdue (diff < 0) priority 0
        // 2. Due today (diff == 0) priority 1
        // 3. Upcoming (diff > 0) priority 2
        // Tie-break deterministically by dueDatePersian and id
        return list.sortedWith(
            compareBy<UpcomingObligationItem> { item ->
                val diff = calculateDaysDifference(item.dueDatePersian)
                if (diff < 0) 0 else if (diff == 0) 1 else 2
            }.thenBy { item ->
                val diff = calculateDaysDifference(item.dueDatePersian)
                diff
            }.thenBy { item ->
                item.dueDatePersian
            }.thenBy { item ->
                item.id
            }
        ).take(1)
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

        val itemsWithDiff = realReminders.mapNotNull { rem ->
            if (rem.status == "COMPLETED" || rem.status == "CANCELLED") return@mapNotNull null
            val diff = calculateDaysDifference(rem.date)
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
                    timePersian = rem.time,
                    typeTitle = typeTitle,
                    iconRes = iconRes,
                    accentColor = accentColor,
                    datePersian = rem.date
                ),
                diff
            )
        }

        // Sort by:
        // 1. overdue/missed (diff < 0)
        // 2. nearest upcoming date (diff >= 0)
        // 3. time
        return itemsWithDiff.sortedWith(
            compareBy<Pair<UpcomingReminderItem, Int>> { (_, diff) ->
                if (diff < 0) 0 else 1
            }.thenBy { (_, diff) ->
                if (diff < 0) diff else diff
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
