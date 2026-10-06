package com.example.data.backup

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalBackupRepository(
    private val context: Context
) : BackupRepository {

    private val db = AppDatabase.getDatabase(context)

    private fun getEffectiveUserId(): String {
        return SessionManager.userId
            ?: com.example.data.security.AuthSessionManager(context).getActiveSession()?.userId
            ?: throw IllegalStateException("Authenticated user is required")
    }

    override suspend fun createBackup(): DarinoBackup =
        withContext(Dispatchers.IO) {
            val userId = getEffectiveUserId()

            val transactions =
                db.transactionDao().getAllTransactionsList(userId)

            val installments =
                db.installmentDao().getAllInstallmentsList(userId)

            val payments =
                db.installmentDao().getAllPaymentsList(userId)

            val vehicles =
                db.vehicleDao().getAllVehiclesList(userId)

            val services =
                db.vehicleDao().getAllServicesList(userId)

            val expenses =
                db.vehicleDao().getAllExpensesList(userId)

            val insurances =
                db.vehicleDao().getAllInsurancesList(userId)

            val inspections =
                db.vehicleDao().getAllInspectionsList(userId)

            val reminders =
                db.smartReminderDao().getAllRemindersList(userId)

            val schedules =
                db.smartReminderDao()
                    .getAllFutureActiveSchedules(userId)

            val events =
                db.financialEventDao().getAllEventsList(userId)

            val totalRecords =
                transactions.size +
                        installments.size +
                        payments.size +
                        vehicles.size +
                        services.size +
                        expenses.size +
                        insurances.size +
                        inspections.size +
                        reminders.size +
                        events.size

            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 1,
                    appVersion = "1.0",
                    createdAt = System.currentTimeMillis(),
                    recordCount = totalRecords,
                    checksum = "",
                    currencyUnit = "TOMAN",
                    dateSystem = "JALALI_UI"
                ),
                transactions = transactions,
                installments = installments,
                installmentPayments = payments,
                vehicles = vehicles,
                vehicleServices = services,
                vehicleExpenses = expenses,
                vehicleInsurances = insurances,
                vehicleInspections = inspections,
                reminders = reminders,
                reminderSchedules = schedules,
                financialEvents = events
            )
        }

    override suspend fun restoreBackup(
        backup: DarinoBackup,
        mode: RestoreMode
    ) {
        withContext(Dispatchers.IO) {
            val userId = getEffectiveUserId()

            db.withTransaction {
                if (mode == RestoreMode.REPLACE) {
                    db.transactionDao()
                        .clearAllTransactions(userId)

                    db.installmentDao()
                        .clearAllPayments(userId)

                    db.installmentDao()
                        .clearAllInstallments(userId)

                    db.vehicleDao()
                        .clearAllInspections(userId)

                    db.vehicleDao()
                        .clearAllInsurances(userId)

                    db.vehicleDao()
                        .clearAllExpenses(userId)

                    db.vehicleDao()
                        .clearAllServices(userId)

                    db.vehicleDao()
                        .clearAllVehicles(userId)

                    db.smartReminderDao()
                        .clearAllReminders(userId)

                    db.financialEventDao()
                        .clearAllEvents(userId)
                }

                backup.transactions.forEach { transaction ->
                    db.transactionDao().insertTransaction(
                        transaction.copy(userId = userId)
                    )
                }

                backup.installments.forEach { installment ->
                    db.installmentDao().insertInstallment(
                        installment.copy(userId = userId)
                    )
                }

                backup.installmentPayments.forEach { payment ->
                    db.installmentDao().insertPayment(payment)
                }

                backup.vehicles.forEach { vehicle ->
                    db.vehicleDao().insertVehicle(
                        vehicle.copy(userId = userId)
                    )
                }

                backup.vehicleServices.forEach { service ->
                    db.vehicleDao().insertService(
                        service.copy(userId = userId)
                    )
                }

                if (backup.vehicleExpenses.isNotEmpty()) {
                    db.vehicleDao().insertExpenses(
                        backup.vehicleExpenses.map {
                            it.copy(userId = userId)
                        }
                    )
                }

                if (backup.vehicleInsurances.isNotEmpty()) {
                    db.vehicleDao().insertInsurances(
                        backup.vehicleInsurances.map {
                            it.copy(userId = userId)
                        }
                    )
                }

                if (backup.vehicleInspections.isNotEmpty()) {
                    db.vehicleDao().insertInspections(
                        backup.vehicleInspections.map {
                            it.copy(userId = userId)
                        }
                    )
                }

                backup.reminders.forEach { reminder ->
                    db.smartReminderDao().insertReminder(
                        reminder.copy(userId = userId)
                    )
                }

                backup.reminderSchedules.forEach { schedule ->
                    db.smartReminderDao().insertSchedule(schedule)
                }

                backup.financialEvents.forEach { event ->
                    db.financialEventDao().insertEvent(
                        event.copy(userId = userId)
                    )
                }
            }

            try {
                com.example.vehicle.data.VehicleRepository
                    .instance
                    .reloadFromDatabase(context)

                com.example.ui.screens.installments.data
                    .LocalInstallmentRepository
                    .instance
                    .reloadFromDatabase(context)
            } catch (e: Exception) {
                Log.e(
                    "LocalBackupRepository",
                    "Failed to reload restored data",
                    e
                )
            }
        }
    }

    override suspend fun exportCsvTransactions(): String =
        withContext(Dispatchers.IO) {
            val userId = getEffectiveUserId()

            val transactions =
                db.transactionDao()
                    .getAllTransactionsList(userId)

            buildString {
                appendLine(
                    "Time,Type,Category,AccountName,Amount,Description"
                )

                transactions.forEach { transaction ->
                    appendLine(
                        "\"${transaction.timeFormatted}\"," +
                                "${transaction.type}," +
                                "\"${transaction.category}\"," +
                                "\"${transaction.accountName}\"," +
                                "${transaction.amount}," +
                                "\"${transaction.description}\""
                    )
                }
            }
        }

    override suspend fun exportCsvInstallments(): String =
        withContext(Dispatchers.IO) {
            val userId = getEffectiveUserId()

            val installments =
                db.installmentDao()
                    .getAllInstallmentsList(userId)

            buildString {
                appendLine(
                    "Title,Provider,Amount,TotalInstallments," +
                            "RemainingInstallments,NextDueDate,Status"
                )

                installments.forEach { installment ->
                    appendLine(
                        "\"${installment.title}\"," +
                                "\"${installment.providerName}\"," +
                                "${installment.amount}," +
                                "${installment.totalInstallments}," +
                                "${installment.remainingInstallments}," +
                                "${installment.nextDueDate}," +
                                "\"${installment.status}\""
                    )
                }
            }
        }

    override suspend fun exportCsvVehicleExpenses(): String =
        withContext(Dispatchers.IO) {
            val userId = getEffectiveUserId()

            val services =
                db.vehicleDao()
                    .getAllServicesList(userId)

            buildString {
                appendLine(
                    "VehicleId,Title,Type,DueDate,DueMileage,Status,Notes"
                )

                services.forEach { service ->
                    appendLine(
                        "${service.vehicleId}," +
                                "\"${service.title}\"," +
                                "\"${service.type}\"," +
                                "${service.dueDate ?: 0}," +
                                "${service.dueMileage ?: 0}," +
                                "\"${service.status}\"," +
                                "\"${service.notes ?: ""}\""
                    )
                }
            }
        }

    override suspend fun getLastBackupTime(): Long? =
        withContext(Dispatchers.IO) {
            val preferences = context.getSharedPreferences(
                "darino_backup_prefs",
                Context.MODE_PRIVATE
            )

            preferences
                .getLong("last_backup_time", 0L)
                .takeIf { it != 0L }
        }

    override suspend fun deleteAllData() {
        withContext(Dispatchers.IO) {
            val userId = getEffectiveUserId()

            db.withTransaction {
                db.transactionDao()
                    .clearAllTransactions(userId)

                db.installmentDao()
                    .clearAllPayments(userId)

                db.installmentDao()
                    .clearAllInstallments(userId)

                db.vehicleDao()
                    .clearAllInspections(userId)

                db.vehicleDao()
                    .clearAllInsurances(userId)

                db.vehicleDao()
                    .clearAllExpenses(userId)

                db.vehicleDao()
                    .clearAllServices(userId)

                db.vehicleDao()
                    .clearAllVehicles(userId)

                db.smartReminderDao()
                    .clearAllReminders(userId)

                db.financialEventDao()
                    .clearAllEvents(userId)
            }
        }
    }
}