package com.example.domain.backup

import com.example.data.backup.BackupRepository
import com.example.data.backup.BackupValidator
import com.example.data.backup.DarinoBackup
import com.example.data.backup.RestoreMode
import com.example.data.backup.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(private val repository: BackupRepository) {

    suspend fun createBackup(): DarinoBackup = withContext(Dispatchers.IO) {
        repository.createBackup()
    }

    suspend fun validateBackup(backup: DarinoBackup): ValidationResult = withContext(Dispatchers.IO) {
        BackupValidator.validate(backup)
    }

    suspend fun restoreBackup(backup: DarinoBackup, mode: RestoreMode) = withContext(Dispatchers.IO) {
        repository.restoreBackup(backup, mode)
    }

    suspend fun exportCsvTransactions(): String = withContext(Dispatchers.IO) {
        repository.exportCsvTransactions()
    }

    suspend fun exportCsvInstallments(): String = withContext(Dispatchers.IO) {
        repository.exportCsvInstallments()
    }

    suspend fun exportCsvVehicleExpenses(): String = withContext(Dispatchers.IO) {
        repository.exportCsvVehicleExpenses()
    }

    suspend fun getLastBackupTime(): Long? = withContext(Dispatchers.IO) {
        repository.getLastBackupTime()
    }

    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        repository.deleteAllData()
    }
}
