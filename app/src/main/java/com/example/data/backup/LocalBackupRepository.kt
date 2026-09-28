package com.example.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.database.TransactionEntity
import com.example.data.database.InstallmentEntity
import com.example.data.database.InstallmentPaymentEntity
import com.example.data.database.VehicleEntity
import com.example.data.database.VehicleServiceEntity
import com.example.reminder.data.ReminderEntity
import com.example.reminder.data.ReminderScheduleEntity
import com.example.calendar.data.FinancialEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalBackupRepository(private val context: Context) : BackupRepository {
    private val db = AppDatabase.getDatabase(context)

    override suspend fun createBackup(): DarinoBackup = withContext(Dispatchers.IO) {
        val txs = db.transactionDao().getAllTransactionsList()
        val installments = db.installmentDao().getAllInstallmentsList()
        val payments = emptyList<InstallmentPaymentEntity>() // or db queries if available
        val vehicles = db.vehicleDao().getAllVehiclesList()
        val services = db.vehicleDao().getAllServicesList()
        val reminders = db.smartReminderDao().getAllRemindersList()
        val schedules = db.smartReminderDao().getAllSchedulesList()
        val events = db.financialEventDao().getAllEventsList()

        val totalRecords = txs.size + installments.size + vehicles.size + services.size + reminders.size + events.size

        val metadata = BackupMetadata(
            backupVersion = 1,
            appVersion = "1.0",
            createdAt = System.currentTimeMillis(),
            recordCount = totalRecords,
            checksum = "",
            currencyUnit = "TOMAN",
            dateSystem = "JALALI_UI"
        )

        DarinoBackup(
            metadata = metadata,
            transactions = txs,
            installments = installments,
            installmentPayments = payments,
            vehicles = vehicles,
            vehicleServices = services,
            reminders = reminders,
            reminderSchedules = schedules,
            financialEvents = events
        )
    }

    override suspend fun restoreBackup(backup: DarinoBackup, mode: RestoreMode) = withContext(Dispatchers.IO) {
        db.withTransaction {
            if (mode == RestoreMode.REPLACE) {
                // Clear existing tables
                db.transactionDao().clearAllTransactions()
                db.installmentDao().clearAllInstallments()
                db.vehicleDao().clearAllVehicles()
                db.vehicleDao().clearAllServices()
                db.smartReminderDao().clearAllReminders()
                db.financialEventDao().clearAllEvents()
            }

            // Insert backup data
            for (tx in backup.transactions) {
                if (mode == RestoreMode.REPLACE) {
                    db.transactionDao().insertTransaction(tx)
                } else {
                    db.transactionDao().insertTransaction(tx) // or upsert
                }
            }
            for (inst in backup.installments) {
                db.installmentDao().insertInstallment(inst)
            }
            for (v in backup.vehicles) {
                db.vehicleDao().insertVehicle(v)
            }
            for (s in backup.vehicleServices) {
                db.vehicleDao().insertService(s)
            }
            for (r in backup.reminders) {
                db.smartReminderDao().insertReminder(r)
            }
            for (sched in backup.reminderSchedules) {
                db.smartReminderDao().insertSchedule(sched)
            }
            for (ev in backup.financialEvents) {
                db.financialEventDao().insertEvent(ev)
            }
        }
    }

    override suspend fun exportCsvTransactions(): String = withContext(Dispatchers.IO) {
        val txs = db.transactionDao().getAllTransactionsList()
        val sb = StringBuilder()
        sb.append("Time,Type,Category,AccountName,Amount,Description\n")
        for (tx in txs) {
            sb.append("\"${tx.timeFormatted}\",${tx.type},\"${tx.category}\",\"${tx.accountName}\",${tx.amount},\"${tx.description}\"\n")
        }
        sb.toString()
    }

    override suspend fun exportCsvInstallments(): String = withContext(Dispatchers.IO) {
        val insts = db.installmentDao().getAllInstallmentsList()
        val sb = StringBuilder()
        sb.append("Title,Provider,Amount,TotalInstallments,RemainingInstallments,NextDueDate,Status\n")
        for (i in insts) {
            sb.append("\"${i.title}\",\"${i.providerName}\",${i.amount},${i.totalInstallments},${i.remainingInstallments},${i.nextDueDate},\"${i.status}\"\n")
        }
        sb.toString()
    }

    override suspend fun exportCsvVehicleExpenses(): String = withContext(Dispatchers.IO) {
        val services = db.vehicleDao().getAllServicesList()
        val sb = StringBuilder()
        sb.append("VehicleId,Title,Type,DueDate,DueMileage,Status,Notes\n")
        for (s in services) {
            sb.append("${s.vehicleId},\"${s.title}\",\"${s.type}\",${s.dueDate ?: 0},${s.dueMileage ?: 0},\"${s.status}\",\"${s.notes ?: ""}\"\n")
        }
        sb.toString()
    }

    override suspend fun getLastBackupTime(): Long? = withContext(Dispatchers.IO) {
        // Can be stored in SharedPrefs or DB metadata
        val prefs = context.getSharedPreferences("darino_backup_prefs", Context.MODE_PRIVATE)
        val time = prefs.getLong("last_backup_time", 0L)
        if (time == 0L) null else time
    }

    override suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.transactionDao().clearAllTransactions()
            db.installmentDao().clearAllInstallments()
            db.vehicleDao().clearAllVehicles()
            db.vehicleDao().clearAllServices()
            db.smartReminderDao().clearAllReminders()
            db.financialEventDao().clearAllEvents()
        }
    }
}
