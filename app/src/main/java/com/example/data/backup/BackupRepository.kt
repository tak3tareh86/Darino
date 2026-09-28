package com.example.data.backup

enum class RestoreMode {
    REPLACE,
    MERGE
}

interface BackupRepository {
    suspend fun createBackup(): DarinoBackup
    suspend fun restoreBackup(backup: DarinoBackup, mode: RestoreMode)
    suspend fun exportCsvTransactions(): String
    suspend fun exportCsvInstallments(): String
    suspend fun exportCsvVehicleExpenses(): String
    suspend fun getLastBackupTime(): Long?
    suspend fun deleteAllData()
}
