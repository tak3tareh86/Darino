package com.example.data.backup

import com.example.calendar.data.FinancialEventEntity
import com.example.data.database.InstallmentEntity
import com.example.data.database.InstallmentPaymentEntity
import com.example.data.database.TransactionEntity
import com.example.data.database.VehicleEntity
import com.example.data.database.VehicleExpenseRoomEntity
import com.example.data.database.VehicleInspectionRoomEntity
import com.example.data.database.VehicleInsuranceRoomEntity
import com.example.data.database.VehicleServiceEntity
import com.example.reminder.data.ReminderEntity
import com.example.reminder.data.ReminderScheduleEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.MessageDigest

object BackupValidator {

    private val moshi: Moshi by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }

    fun calculateChecksum(content: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun withChecksum(backup: DarinoBackup): DarinoBackup {
        val unsignedBackup = backup.copy(metadata = backup.metadata.copy(checksum = ""))
        val checksum = calculateChecksum(moshi.adapter(DarinoBackup::class.java).toJson(unsignedBackup))
        return unsignedBackup.copy(metadata = unsignedBackup.metadata.copy(checksum = checksum))
    }

    fun validate(backup: DarinoBackup, rawJson: String? = null): ValidationResult {
        val version = backup.metadata.backupVersion
        if (version !in 1..2) {
            return ValidationResult.Invalid("نسخه فایل پشتیبان پشتیبانی نمی‌شود.")
        }
        if (backup.metadata.createdAt <= 0L || backup.metadata.recordCount < 0) {
            return ValidationResult.Invalid("اطلاعات فراداده فایل پشتیبان نامعتبر است.")
        }
        if (backup.metadata.currencyUnit != "TOMAN") {
            return ValidationResult.Invalid("واحد پول فایل پشتیبان معتبر نیست.")
        }
        if (backup.metadata.checksum.isBlank()) {
            return ValidationResult.Invalid("فایل پشتیبان امضای صحت‌سنجی ندارد یا ناقص است.")
        }

        val expectedCount = if (version == 1) {
            backup.transactions.size + backup.installments.size +
                backup.installmentPayments.size + backup.vehicles.size +
                backup.vehicleServices.size + backup.vehicleExpenses.size +
                backup.vehicleInsurances.size + backup.vehicleInspections.size +
                backup.reminders.size + backup.reminderSchedules.size +
                backup.financialEvents.size
        } else {
            backup.accounts.size + backup.checks.size +
                backup.transactions.size + backup.installments.size +
                backup.installmentPayments.size + backup.vehicles.size +
                backup.vehicleServices.size + backup.vehicleExpenses.size +
                backup.vehicleInsurances.size + backup.vehicleInspections.size +
                backup.reminders.size + backup.reminderSchedules.size +
                backup.financialEvents.size
        }
        if (backup.metadata.recordCount != expectedCount) {
            return ValidationResult.Invalid("تعداد رکوردهای فایل با اطلاعات ثبت‌شده در سربرگ آن مطابقت ندارد.")
        }

        val expectedChecksum = if (version == 1) {
            legacyV1Checksum(backup)
        } else {
            withChecksum(backup).metadata.checksum
        }
        if (!backup.metadata.checksum.equals(expectedChecksum, ignoreCase = true)) {
            return ValidationResult.Invalid("صحت فایل پشتیبان تأیید نشد؛ فایل ممکن است ناقص یا تغییر داده شده باشد.")
        }

        // Basic relationship checks prevent malformed files from reaching database writes.
        val installmentIds = backup.installments.map { it.id }.toSet()
        if (backup.installmentPayments.any { it.installmentId !in installmentIds }) {
            return ValidationResult.Invalid("فایل پشتیبان دارای پرداخت قسط بدون قسط مرتبط است.")
        }
        val vehicleIds = backup.vehicles.map { it.id.toString() }.toSet() +
            backup.vehicles.mapNotNull { it.serverId }.toSet()
        if (backup.vehicleServices.any { it.vehicleId !in vehicleIds }) {
            return ValidationResult.Invalid("فایل پشتیبان دارای سرویس خودرو بدون خودروی مرتبط است.")
        }
        if (backup.vehicleExpenses.any { it.vehicleId !in vehicleIds } ||
            backup.vehicleInsurances.any { it.vehicleId !in vehicleIds } ||
            backup.vehicleInspections.any { it.vehicleId !in vehicleIds }) {
            return ValidationResult.Invalid("فایل پشتیبان دارای اطلاعات خودرو بدون خودروی مرتبط است.")
        }

        return ValidationResult.Valid
    }

    private fun legacyV1Checksum(backup: DarinoBackup): String {
        val legacy = LegacyDarinoBackupV1(
            metadata = backup.metadata.copy(checksum = ""),
            transactions = backup.transactions,
            installments = backup.installments,
            installmentPayments = backup.installmentPayments,
            vehicles = backup.vehicles,
            vehicleServices = backup.vehicleServices,
            vehicleExpenses = backup.vehicleExpenses,
            vehicleInsurances = backup.vehicleInsurances,
            vehicleInspections = backup.vehicleInspections,
            reminders = backup.reminders,
            reminderSchedules = backup.reminderSchedules,
            financialEvents = backup.financialEvents
        )
        return calculateChecksum(moshi.adapter(LegacyDarinoBackupV1::class.java).toJson(legacy))
    }
}

private data class LegacyDarinoBackupV1(
    val metadata: BackupMetadata,
    val transactions: List<TransactionEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val installmentPayments: List<InstallmentPaymentEntity> = emptyList(),
    val vehicles: List<VehicleEntity> = emptyList(),
    val vehicleServices: List<VehicleServiceEntity> = emptyList(),
    val vehicleExpenses: List<VehicleExpenseRoomEntity> = emptyList(),
    val vehicleInsurances: List<VehicleInsuranceRoomEntity> = emptyList(),
    val vehicleInspections: List<VehicleInspectionRoomEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val reminderSchedules: List<ReminderScheduleEntity> = emptyList(),
    val financialEvents: List<FinancialEventEntity> = emptyList()
)

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}
