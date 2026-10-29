package com.example.data.backup

import com.example.data.database.UserPreferenceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {

    @Test
    fun versionThreeBackupWithPreferencesValidates() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 3,
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
    fun versionThreeBackupCountsGeneralAppSettings() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 3,
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
    fun versionThreeBackupRejectsRecordCountMismatch() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 3,
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
}
