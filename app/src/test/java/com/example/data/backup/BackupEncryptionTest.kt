package com.example.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupEncryptionTest {
    @Test
    fun encryptedBackupRoundTripsWithCorrectPassphrase() {
        val original = """{"metadata":{"backupVersion":8},"records":[1,2,3]}""".toByteArray()
        val encrypted = BackupEncryption.encrypt(original, "123456".toCharArray())

        assertTrue(BackupEncryption.isEncrypted(encrypted))
        assertArrayEquals(
            original,
            BackupEncryption.decrypt(encrypted, "123456".toCharArray())
        )
    }

    @Test
    fun deviceBackupFormatIsRecognizedSeparatelyFromPortableBackups() {
        val automaticPayload = ByteArray(1 + 12 + 16 + 1).also { it[0] = 12 }

        assertTrue(DeviceBackupEncryption.isDeviceEncrypted(automaticPayload))
        assertTrue(!BackupEncryption.isEncrypted(automaticPayload))
        assertTrue(!DeviceBackupEncryption.isDeviceEncrypted("{\"metadata\":{}}".toByteArray()))
    }

    @Test
    fun encryptionRequiresExactlySixNumericDigits() {
        assertTrue(runCatching {
            BackupEncryption.encrypt("private".toByteArray(), "12345".toCharArray())
        }.isFailure)
        assertTrue(runCatching {
            BackupEncryption.encrypt("private".toByteArray(), "1234567".toCharArray())
        }.isFailure)
        assertTrue(runCatching {
            BackupEncryption.encrypt("private".toByteArray(), "12ab56".toCharArray())
        }.isFailure)
    }

    @Test
    fun encryptedBackupRejectsWrongPassphraseAndTampering() {
        val encrypted = BackupEncryption.encrypt("private".toByteArray(), "654321".toCharArray())

        assertTrue(runCatching {
            BackupEncryption.decrypt(encrypted, "111111".toCharArray())
        }.isFailure)

        val tampered = encrypted.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertTrue(runCatching {
            BackupEncryption.decrypt(tampered, "654321".toCharArray())
        }.isFailure)
    }
    @Test
    fun automaticBackupDiscoveryAcceptsDailyAndWeeklyFilesAcrossProfileTags() {
        assertTrue(
            BackupFolderStorage.isAutomaticBackupFilename(
                "daily_auto_backup_08c2e370473bad70b6fec359_latest.darino",
                "daily"
            )
        )
        assertTrue(
            BackupFolderStorage.isAutomaticBackupFilename(
                "weekly_auto_backup_0123456789abcdef01234567_latest.darino",
                "weekly"
            )
        )
    }

    @Test
    fun automaticBackupDiscoveryRejectsManualTemporaryAndMalformedFiles() {
        assertTrue(!BackupFolderStorage.isAutomaticBackupFilename("manual_backup.darino"))
        assertTrue(!BackupFolderStorage.isAutomaticBackupFilename(".daily_auto_backup_08c2e370473bad70b6fec359_latest.darino.tmp-1"))
        assertTrue(!BackupFolderStorage.isAutomaticBackupFilename("daily_auto_backup_not-a-hash_latest.darino"))
        assertTrue(
            !BackupFolderStorage.isAutomaticBackupFilename(
                "daily_auto_backup_08c2e370473bad70b6fec359_latest.darino",
                "weekly"
            )
        )
    }

}
