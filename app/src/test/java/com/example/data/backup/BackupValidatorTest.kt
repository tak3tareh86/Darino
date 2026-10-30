package com.example.data.backup

import com.example.data.database.UserPreferenceEntity
import com.example.data.database.VehicleStoreEntity
import com.example.reminder.data.ReminderDeliveryLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {

    @Test
    fun versionFourBackupWithPreferencesValidates() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 4,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 1,
                    checksum = "",
                    currencyUnit = "TOMAN"
                ),
                userPreference = UserPreferenceEntity(
                    userId = "backup-user",
                    numberDisplayMode = "PERSIAN"
                )
            )
        )

        assertEquals(1, backup.metadata.recordCount)
        assertTrue(BackupValidator.validate(backup) is ValidationResult.Valid)
    }

    @Test
    fun versionFourBackupCountsGeneralAppSettings() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 4,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 1,
                    checksum = "",
                    currencyUnit = "TOMAN"
                ),
                appSettings = AppSettingsBackup(
                    themeMode = "SYSTEM",
                    language = "PERSIAN",
                    currency = "TOMAN",
                    alertDelivery = "BOTH",
                    navigationTransition = "DYNAMIC"
                )
            )
        )

        assertEquals(1, backup.metadata.recordCount)
        assertTrue(BackupValidator.validate(backup) is ValidationResult.Valid)
    }

    @Test
    fun versionFourBackupRejectsRecordCountMismatch() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 4,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 0,
                    checksum = "",
                    currencyUnit = "TOMAN"
                ),
                userPreference = UserPreferenceEntity(
                    userId = "backup-user",
                    numberDisplayMode = "PERSIAN"
                )
            )
        )

        assertTrue(BackupValidator.validate(backup) is ValidationResult.Invalid)
    }
    @Test
    fun versionFiveBackupCountsVehicleStoreAndReminderDeliveryHistory() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 5,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 2,
                    checksum = "",
                    currencyUnit = "TOMAN"
                ),
                vehicleStore = VehicleStoreEntity(
                    key = "vehicle_data",
                    userId = "backup-user",
                    vehiclesJson = "[]"
                ),
                reminderDeliveryLogs = listOf(
                    ReminderDeliveryLogEntity(
                        id = "delivery-1",
                        reminderId = "reminder-1",
                        channel = "NOTIFICATION",
                        scheduledAt = 1_800_000_000_000L
                    )
                )
            )
        )

        assertEquals(2, backup.metadata.recordCount)
        assertTrue(BackupValidator.validate(backup) is ValidationResult.Valid)
    }

    @Test
    fun versionSixBackupIncludesLegacyReminderTablesAndValidates() {
        val legacyReminder = com.example.data.database.ReminderEntity(
            id = 12,
            userId = "backup-user",
            type = "GENERAL",
            title = "یادآور آزمایشی",
            description = "توضیحات",
            scheduledDateTime = 1_800_000_000_000L
        )
        val legacySchedule = com.example.data.database.ReminderScheduleEntity(
            id = 21,
            reminderId = 12,
            triggerDateTime = 1_800_000_000_000L,
            status = "PENDING",
            notificationStatus = "PENDING",
            smsStatus = "DISABLED"
        )
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 6,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 2,
                    checksum = "",
                    currencyUnit = "TOMAN"
                ),
                legacyReminders = listOf(legacyReminder),
                legacyReminderSchedules = listOf(legacySchedule)
            )
        )

        assertEquals(2, backup.metadata.recordCount)
        assertTrue(BackupValidator.validate(backup) is ValidationResult.Valid)
    }

    @Test
    fun backupJsonCodecRejectsEmptyFiles() {
        val result = runCatching { BackupFileCodec.decode("") }
        assertTrue(result.isFailure)
    }

}
