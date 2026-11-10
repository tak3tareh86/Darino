package com.example.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupEncryptionTest {
    @Test
    fun encryptedBackupRoundTripsWithCorrectPassphrase() {
        val original = """{"metadata":{"backupVersion":8},"records":[1,2,3]}""".toByteArray()
        val encrypted = BackupEncryption.encrypt(original, "correct horse battery".toCharArray())

        assertTrue(BackupEncryption.isEncrypted(encrypted))
        assertArrayEquals(
            original,
            BackupEncryption.decrypt(encrypted, "correct horse battery".toCharArray())
        )
    }

    @Test
    fun encryptedBackupRejectsWrongPassphraseAndTampering() {
        val encrypted = BackupEncryption.encrypt("private".toByteArray(), "strong-passphrase".toCharArray())

        assertTrue(runCatching {
            BackupEncryption.decrypt(encrypted, "wrong-passphrase".toCharArray())
        }.isFailure)

        val tampered = encrypted.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertTrue(runCatching {
            BackupEncryption.decrypt(tampered, "strong-passphrase".toCharArray())
        }.isFailure)
    }
}
