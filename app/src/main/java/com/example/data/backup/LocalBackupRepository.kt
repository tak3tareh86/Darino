package com.example.data.backup

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.data.preferences.AppPreferencesRepository
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

            // Keep all table reads in one SQLite snapshot so related records cannot drift mid-backup.
            db.withTransaction {
            val accounts = db.accountDao().getAllAccountsRaw(userId)

            val checks = db.checkDao().getAllChecksList(userId)

            val transactions =
                db.transactionDao().getAllTransactionsForBackup(userId)

            val installments =
                db.installmentDao().getAllInstallmentsList(userId)

            val payments =
                db.installmentDao().getAllPaymentsList(userId)

            val vehicles =
                db.vehicleDao().getAllVehiclesForBackup(userId)

            val services =
                db.vehicleDao().getAllServicesForBackup(userId)

            val expenses =
                db.vehicleDao().getAllExpensesList(userId)

            val insurances =
                db.vehicleDao().getAllInsurancesList(userId)

            val inspections =
                db.vehicleDao().getAllInspectionsList(userId)

            val reminders =
                db.smartReminderDao().getAllRemindersList(userId)

            val schedules =
                db.smartReminderDao().getAllSchedulesList(userId)

            val events =
                db.financialEventDao().getAllEventsList(userId)

            val financialHealthProfiles =
                db.financialHealthDao().getAllProfilesForUser(userId)

            val loanCalculations =
                db.loanCalculationDao().getAllCalculationsList()

            val userPreference =
                db.userPreferenceDao().getPreference(userId)
            val notificationLogs = db.notificationLogDao().getAllNotificationsForBackup(userId)
            val smsLogs = db.smsLogDao().getAllSmsLogsForBackup(userId)
            val vehicleStore = db.vehicleDao().getVehicleStore(userId)
            val reminderDeliveryLogs = db.smartReminderDao().getAllDeliveryLogsForBackup(userId)
            val appPreferences = AppPreferencesRepository.getInstance(context)
            val appSettings = AppSettingsBackup(
                themeMode = appPreferences.themeMode.value.name,
                language = appPreferences.language.value.name,
                currency = appPreferences.currency.value.name,
                alertDelivery = appPreferences.alertDelivery.value.name,
                navigationTransition = appPreferences.navTransition.value.name
            )

            val totalRecords =
                accounts.size + checks.size + transactions.size +
                        installments.size +
                        payments.size +
                        vehicles.size +
                        services.size +
                        expenses.size +
                        insurances.size +
                        inspections.size +
                        reminders.size +
                        schedules.size +
                        events.size +
                        financialHealthProfiles.size +
                        loanCalculations.size + notificationLogs.size + smsLogs.size + reminderDeliveryLogs.size +
                        (if (vehicleStore != null) 1 else 0) + (if (userPreference != null) 1 else 0) + 1

            BackupValidator.withChecksum(DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 5,
                    appVersion = "1.0",
                    createdAt = System.currentTimeMillis(),
                    recordCount = totalRecords,
                    checksum = "",
                    currencyUnit = "TOMAN",
                    dateSystem = "JALALI_UI"
                ),
                accounts = accounts,
                checks = checks,
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
                financialEvents = events,
                financialHealthProfiles = financialHealthProfiles,
                loanCalculations = loanCalculations,
                userPreference = userPreference,
                appSettings = appSettings,
                notificationLogs = notificationLogs,
                smsLogs = smsLogs,
                vehicleStore = vehicleStore,
                reminderDeliveryLogs = reminderDeliveryLogs
            ))
            }
        }

    override suspend fun restoreBackup(
        backup: DarinoBackup,
        mode: RestoreMode
    ) {
        withContext(Dispatchers.IO) {
            when (val validation = BackupValidator.validate(backup)) {
                is ValidationResult.Valid -> Unit
                is ValidationResult.Invalid -> throw IllegalArgumentException(validation.reason)
            }
            val userId = getEffectiveUserId()

            db.withTransaction {
                if (mode == RestoreMode.REPLACE) {
                    db.installmentDao().clearAllPayments(userId)
                    db.installmentDao().clearAllInstallments(userId)
                    if (backup.metadata.backupVersion >= 2) {
                        db.accountDao().clearAllAccounts(userId)
                        db.checkDao().clearAllChecks(userId)
                    }
                    db.transactionDao()
                        .clearAllTransactions(userId)

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

                    db.smartReminderDao().clearAllSchedules(userId)
                    if (backup.metadata.backupVersion >= 5) {
                        // Delivery logs are linked by reminder ID without a foreign key; clear them before reminders.
                        db.smartReminderDao().clearAllDeliveryLogsForBackup(userId)
                    }

                    db.smartReminderDao()
                        .clearAllReminders(userId)

                    db.financialEventDao()
                        .clearAllEvents(userId)

                    if (backup.metadata.backupVersion >= 4) {
                        db.notificationLogDao().clearAllNotificationsForBackup(userId)
                        db.smsLogDao().clearAllSmsLogsForBackup(userId)
                    }
                    if (backup.metadata.backupVersion >= 5) {
                        db.vehicleDao().clearVehicleStoreForUser(userId)
                    }
                    if (backup.metadata.backupVersion >= 3) {
                        db.financialHealthDao().clearProfilesForUser(userId)
                        db.userPreferenceDao().clearPreference(userId)
                    }
                }

                // Re-create auto-generated primary keys instead of risking collisions with another
                // local profile. Keep stable string IDs for cross-record references where available.
                val restoredAccountIds = mutableMapOf<String, String>()
                backup.accounts.forEach { account ->
                    val newId = db.accountDao().insertAccountForBackup(
                        account.copy(id = 0, userId = userId)
                    ).toString()
                    restoredAccountIds[account.id.toString()] = newId
                    restoredAccountIds[account.stringId] = newId
                }
                fun remapAccountId(oldId: String?): String? =
                    oldId?.let { restoredAccountIds[it] ?: it }

                val restoredCheckIds = mutableMapOf<Int, Int>()
                backup.checks.forEach { check ->
                    val newId = db.checkDao().insertCheck(
                        check.copy(id = 0, userId = userId, accountId = remapAccountId(check.accountId))
                    ).toInt()
                    restoredCheckIds[check.id] = newId
                }

                backup.transactions.forEach { transaction ->
                    val remappedSourceId = if (transaction.sourceType == "CHECK") {
                        transaction.sourceId?.toIntOrNull()?.let { restoredCheckIds[it]?.toString() }
                            ?: transaction.sourceId
                    } else transaction.sourceId
                    db.transactionDao().insertTransaction(
                        transaction.copy(
                            id = 0,
                            userId = userId,
                            sourceId = remappedSourceId,
                            accountId = remapAccountId(transaction.accountId),
                            transferSourceAccountId = remapAccountId(transaction.transferSourceAccountId),
                            transferDestinationAccountId = remapAccountId(transaction.transferDestinationAccountId)
                        )
                    )
                }

                val restoredInstallmentIds = mutableMapOf<Int, Int>()
                backup.installments.forEach { installment ->
                    val newId = db.installmentDao().insertInstallment(
                        installment.copy(id = 0, userId = userId)
                    ).toInt()
                    restoredInstallmentIds[installment.id] = newId
                }

                backup.installmentPayments.forEach { payment ->
                    val newInstallmentId = restoredInstallmentIds[payment.installmentId]
                        ?: throw IllegalStateException("پرداخت قسط به قسط موجود در پشتیبان متصل نیست")
                    db.installmentDao().insertPayment(
                        payment.copy(id = 0, installmentId = newInstallmentId)
                    )
                }

                val restoredVehicleIds = mutableMapOf<String, String>()
                backup.vehicles.forEach { vehicle ->
                    val newId = db.vehicleDao().insertVehicle(
                        vehicle.copy(id = 0, userId = userId)
                    ).toInt()
                    restoredVehicleIds[vehicle.id.toString()] = newId.toString()
                    vehicle.serverId?.takeIf { it.isNotBlank() }?.let { serverId ->
                        restoredVehicleIds[serverId] = newId.toString()
                    }
                }
                fun remapVehicleId(oldId: String): String = restoredVehicleIds[oldId] ?: oldId

                backup.vehicleServices.forEach { service ->
                    db.vehicleDao().insertService(
                        service.copy(id = 0, userId = userId, vehicleId = remapVehicleId(service.vehicleId))
                    )
                }

                if (backup.vehicleExpenses.isNotEmpty()) {
                    db.vehicleDao().insertExpenses(
                        backup.vehicleExpenses.map {
                            it.copy(userId = userId, vehicleId = remapVehicleId(it.vehicleId))
                        }
                    )
                }

                if (backup.vehicleInsurances.isNotEmpty()) {
                    db.vehicleDao().insertInsurances(
                        backup.vehicleInsurances.map {
                            it.copy(userId = userId, vehicleId = remapVehicleId(it.vehicleId))
                        }
                    )
                }

                if (backup.vehicleInspections.isNotEmpty()) {
                    db.vehicleDao().insertInspections(
                        backup.vehicleInspections.map {
                            it.copy(userId = userId, vehicleId = remapVehicleId(it.vehicleId))
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

                if (backup.metadata.backupVersion >= 5) {
                    backup.vehicleStore?.let { db.vehicleDao().insertVehicleStore(it.copy(userId = userId)) }
                    backup.reminderDeliveryLogs.forEach { db.smartReminderDao().insertDeliveryLog(it) }
                }

                if (backup.metadata.backupVersion >= 4) {
                    backup.notificationLogs.forEach { log ->
                        db.notificationLogDao().insertNotification(log.copy(id = 0, userId = userId))
                    }
                    backup.smsLogs.forEach { log ->
                        db.smsLogDao().insertSmsLog(log.copy(id = 0, userId = userId))
                    }
                }

                if (backup.metadata.backupVersion >= 3) {
                    backup.financialHealthProfiles.forEach { profile ->
                        db.financialHealthDao().saveProfile(profile.copy(userId = userId))
                    }
                    backup.loanCalculations.forEach { calculation ->
                        db.loanCalculationDao().insertCalculation(calculation)
                    }
                    backup.userPreference?.let { preference ->
                        db.userPreferenceDao().insertOrUpdate(preference.copy(userId = userId))
                    }
                    backup.appSettings?.let { settings ->
                        val appPreferences = com.example.data.preferences.AppPreferencesRepository.getInstance(context)
                        runCatching { com.example.ui.screens.settings.model.AppThemeMode.valueOf(settings.themeMode) }
                            .getOrNull()?.let { appPreferences.setThemeMode(it) }
                        runCatching { com.example.ui.screens.settings.model.AppLanguage.valueOf(settings.language) }
                            .getOrNull()?.let { appPreferences.setLanguage(it) }
                        runCatching { com.example.ui.screens.settings.model.AppCurrency.valueOf(settings.currency) }
                            .getOrNull()?.let { appPreferences.setCurrency(it) }
                        runCatching { com.example.ui.screens.settings.model.AlertDeliveryPreference.valueOf(settings.alertDelivery) }
                            .getOrNull()?.let { appPreferences.setAlertDelivery(it) }
                        runCatching { com.example.ui.screens.settings.model.NavigationTransitionAnimation.valueOf(settings.navigationTransition) }
                            .getOrNull()?.let { appPreferences.setNavTransition(it) }
                    }
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

}