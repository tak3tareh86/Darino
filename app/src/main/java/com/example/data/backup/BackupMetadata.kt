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
