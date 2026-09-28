package com.example.data.backup

object BackupMigrationManager {

    fun migrate(backup: DarinoBackup): DarinoBackup {
        var current = backup
        // Future migrations (e.g. version 1 to version 2) can be chained here
        while (current.metadata.backupVersion < 1) {
            // perform migration step
            current = current.copy(
                metadata = current.metadata.copy(backupVersion = 1)
            )
        }
        return current
    }
}
