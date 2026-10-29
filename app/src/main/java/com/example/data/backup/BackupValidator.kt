package com.example.data.backup

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.MessageDigest

object BackupValidator {

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    fun calculateChecksum(content: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Creates a checksum over the stable Moshi serialization with metadata.checksum blank.
     * The same canonical representation is used during restore validation.
     */
    fun withChecksum(backup: DarinoBackup): DarinoBackup {
        val unsignedBackup = backup.copy(
            metadata = backup.metadata.copy(checksum = "")
        )
        val adapter = moshi.adapter(DarinoBackup::class.java)
        val checksum = calculateChecksum(adapter.toJson(unsignedBackup))
        return unsignedBackup.copy(
            metadata = unsignedBackup.metadata.copy(checksum = checksum)
        )
    }

    fun validate(backup: DarinoBackup, rawJson: String? = null): ValidationResult {
        if (backup.metadata.backupVersion <= 0) {
            return ValidationResult.Invalid("نسخه پشتیبان نامعتبر است.")
        }
        if (backup.metadata.createdAt <= 0L || backup.metadata.recordCount < 0) {
            return ValidationResult.Invalid("اطلاعات فراداده فایل پشتیبان نامعتبر است.")
        }
        if (backup.metadata.currencyUnit != "TOMAN") {
            return ValidationResult.Invalid("واحد پول پشتیبان باید تومان باشد.")
        }

        if (backup.metadata.checksum.isNotBlank()) {
            val expectedChecksum = withChecksum(backup).metadata.checksum
            if (!backup.metadata.checksum.equals(expectedChecksum, ignoreCase = true)) {
                return ValidationResult.Invalid("صحت فایل پشتیبان تأیید نشد؛ ممکن است فایل ناقص یا دستکاری شده باشد.")
            }
        }

        return ValidationResult.Valid
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}
