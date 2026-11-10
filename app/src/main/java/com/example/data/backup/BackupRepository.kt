package com.example.data.backup

interface BackupRepository {

    suspend fun createBackup(): DarinoBackup

    suspend fun restoreBackup(
        backup: DarinoBackup,
        mode: RestoreMode
    )

    suspend fun exportCsvTransactions(): String

    suspend fun exportCsvInstallments(): String

    suspend fun exportCsvVehicleExpenses(): String

    suspend fun getLastBackupTime(): Long?

}

enum class RestoreMode {
    REPLACE,

    /**
     * Reserved for a future implementation. The current repository intentionally rejects
     * MERGE because safe cross-table ID remapping is not implemented; callers must use REPLACE.
     */
    MERGE
}