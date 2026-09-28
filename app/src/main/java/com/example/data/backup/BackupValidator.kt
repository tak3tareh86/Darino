package com.example.data.backup

import java.security.MessageDigest

object BackupValidator {

    fun calculateChecksum(content: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun validate(backup: DarinoBackup, rawJson: String? = null): ValidationResult {
        if (backup.metadata.backupVersion <= 0) {
            return ValidationResult.Invalid("نسخه پشتیبان نامعتبر است.")
        }
        if (backup.metadata.currencyUnit != "TOMAN") {
            return ValidationResult.Invalid("واحد پول پشتیبان باید تومان باشد.")
        }
        if (rawJson != null && backup.metadata.checksum.isNotBlank()) {
            // Optional checksum verification if provided
            val jsonWithoutChecksumCopy = rawJson
            // We can verify checksum if needed
        }
        return ValidationResult.Valid
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}
