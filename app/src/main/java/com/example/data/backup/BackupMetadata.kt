package com.example.data.backup

data class BackupMetadata(
    val backupVersion: Int = 1,
    val appVersion: String = "1.0",
    val createdAt: Long = System.currentTimeMillis(),
    val recordCount: Int = 0,
    val checksum: String = "",
    val currencyUnit: String = "TOMAN",
    val dateSystem: String = "JALALI_UI",
    val deviceInfo: String = "Android Local"
)

/** Non-reversible account tag for isolating device-local backup files without exposing user IDs. */
object BackupUserScope {
    fun tag(userId: String): String {
        require(userId.isNotBlank())
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(userId.toByteArray(Charsets.UTF_8))
            .take(12)
            .joinToString("") { "%02x".format(it) }
    }
}
