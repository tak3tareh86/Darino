package com.example.data.backup

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Single, shared JSON boundary for backup files.
 * A backup is never accepted for writing or restoring until it round-trips and validates.
 */
object BackupFileCodec {
    private val adapter by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter(DarinoBackup::class.java)
    }

    fun encode(backup: DarinoBackup): String {
        val json = adapter.toJson(backup)
        require(json.isNotBlank()) { "محتوای پشتیبان خالی است." }
        val parsed = adapter.fromJson(json)
            ?: throw IllegalStateException("ساختار پشتیبان پس از تبدیل به JSON قابل خواندن نیست.")
        when (val validation = BackupValidator.validate(parsed)) {
            is ValidationResult.Valid -> Unit
            is ValidationResult.Invalid -> throw IllegalStateException(validation.reason)
        }
        require(parsed.metadata.checksum == backup.metadata.checksum) {
            "صحت اطلاعات پشتیبان پس از تبدیل به JSON تأیید نشد."
        }
        return json
    }

    fun decode(json: String): DarinoBackup {
        require(json.isNotBlank()) { "فایل پشتیبان خالی است یا صفر بایت حجم دارد." }
        val backup = adapter.fromJson(json)
            ?: throw IllegalArgumentException("ساختار فایل پشتیبان قابل شناسایی نیست.")
        when (val validation = BackupValidator.validate(backup, json)) {
            is ValidationResult.Valid -> Unit
            is ValidationResult.Invalid -> throw IllegalArgumentException(validation.reason)
        }
        return backup
    }
}
