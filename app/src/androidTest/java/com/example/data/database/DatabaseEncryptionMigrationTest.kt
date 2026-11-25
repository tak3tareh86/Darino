package com.example.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class DatabaseEncryptionMigrationTest {

    // Runs against a disposable instrumentation database; no installed user database is accessed.
    @Test
    fun migratesExistingRoomDatabaseAndFailsClosedWhenWrappedKeyIsMissing() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseFile = context.getDatabasePath("finance_app_database")
        val temporaryFile = File(databaseFile.parentFile, "finance_app_database.encrypted.tmp")
        val plaintextBackup = File(databaseFile.parentFile, "finance_app_database.plaintext.bak")
        val prefs = context.getSharedPreferences("darino_database_encryption", Context.MODE_PRIVATE)

        context.deleteDatabase("finance_app_database")
        temporaryFile.delete()
        plaintextBackup.delete()
        File(databaseFile.absolutePath + "-wal").delete()
        File(databaseFile.absolutePath + "-shm").delete()
        prefs.edit().clear().commit()
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            if (containsAlias("darino_database_key_v1")) deleteEntry("darino_database_key_v1")
        }

        // Build a genuine version-52 plaintext Room database with a persisted user row.
        val legacySnapshot = File(context.cacheDir, "darino-legacy-source.db")
        val plaintextRoom = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "finance_app_database"
        ).build()
        try {
            val plainDatabase = plaintextRoom.openHelper.writableDatabase
            plainDatabase.execSQL(
                "INSERT INTO user_preferences (userId, numberDisplayMode, updatedAt) " +
                    "VALUES ('migration-test-user', 'PERSIAN', 123)"
            )
            // Preserve AUTOINCREMENT high-water marks even when the highest row was deleted.
            plainDatabase.execSQL(
                "INSERT INTO checks (id, userId, type, amount, amountUnit, checkNumber, bankName, " +
                    "holderName, receiverName, issueDate, dueDate, status, createdAt, updatedAt) " +
                    "VALUES (777, 'migration-test-user', 'RECEIVED', 1, 'TOMAN', 'migration-check', " +
                    "'test-bank', 'holder', 'receiver', 1, 2, 'REGISTERED', 1, 1)"
            )
            plainDatabase.execSQL("DELETE FROM checks WHERE id = 777")
        } finally {
            plaintextRoom.close()
        }
        databaseFile.copyTo(legacySnapshot, overwrite = true)

        DatabaseEncryptionManager.initializeAndMigrate(context)

        val encryptedRoom = AppDatabase.getDatabase(context)
        try {
            encryptedRoom.openHelper.readableDatabase.query(
                "SELECT numberDisplayMode, updatedAt FROM user_preferences WHERE userId = ?",
                arrayOf("migration-test-user")
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("PERSIAN", cursor.getString(0))
                assertEquals(123L, cursor.getLong(1))
            }
            encryptedRoom.openHelper.readableDatabase.query(
                "SELECT seq FROM sqlite_sequence WHERE name = 'checks'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(777L, cursor.getLong(0))
            }
        } finally {
            encryptedRoom.close()
        }


        assertFalse("Plaintext backup must be removed after the first successful Room open.", plaintextBackup.exists())
        assertTrue(databaseFile.isFile && databaseFile.length() > 16L)
        val header = ByteArray(16)
        RandomAccessFile(databaseFile, "r").use { it.readFully(header) }
        assertFalse(
            "Installed database must not retain the SQLite plaintext header.",
            header.contentEquals("SQLite format 3\u0000".toByteArray(StandardCharsets.US_ASCII))
        )

        // Simulate a process death after the plaintext file was moved aside but before
        // the encrypted temporary file was installed. Recovery must resume from the backup.
        legacySnapshot.copyTo(plaintextBackup, overwrite = true)
        databaseFile.copyTo(temporaryFile, overwrite = true)
        // Simulate stale SQLite sidecars left beside the encrypted temp after process death.
        File(temporaryFile.absolutePath + "-wal").writeBytes(byteArrayOf(1, 2, 3))
        File(temporaryFile.absolutePath + "-shm").writeBytes(byteArrayOf(4, 5, 6))
        File(temporaryFile.absolutePath + "-journal").writeBytes(byteArrayOf(7, 8, 9))
        assertTrue("Test setup must remove the main database file.", databaseFile.delete())
        prefs.edit().putString("migration_state", "MIGRATING").commit()
        DatabaseEncryptionManager.initializeAndMigrate(context)
        assertFalse("Recovery must remove stale encrypted WAL sidecars.", File(temporaryFile.absolutePath + "-wal").exists())
        assertFalse("Recovery must remove stale encrypted SHM sidecars.", File(temporaryFile.absolutePath + "-shm").exists())
        assertFalse("Recovery must remove stale encrypted journal sidecars.", File(temporaryFile.absolutePath + "-journal").exists())

        val recoveredRoom = AppDatabase.buildEncrypted(
            context,
            databaseFile.absolutePath,
            DatabaseEncryptionManager.requireReadyKey(context)
        )
        try {
            recoveredRoom.openHelper.readableDatabase.query(
                "SELECT numberDisplayMode FROM user_preferences WHERE userId = ?",
                arrayOf("migration-test-user")
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("PERSIAN", cursor.getString(0))
            }
            recoveredRoom.openHelper.readableDatabase.query(
                "SELECT seq FROM sqlite_sequence WHERE name = 'checks'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(777L, cursor.getLong(0))
            }
        } finally {
            recoveredRoom.close()
        }
        assertFalse("Recovery must remove the plaintext backup only after Room validation.", plaintextBackup.exists())
        assertFalse("Recovery must consume the temporary encrypted file.", temporaryFile.exists())
        legacySnapshot.delete()
        val headerBeforeKeyLoss = ByteArray(16)
        RandomAccessFile(databaseFile, "r").use { it.readFully(headerBeforeKeyLoss) }

        // Losing the wrapped key must not recreate or replace the encrypted database.
        prefs.edit().remove("wrapped_database_key").commit()
        var failedClosed = false
        try {
            DatabaseEncryptionManager.initializeAndMigrate(context)
        } catch (_: IllegalStateException) {
            failedClosed = true
        }
        assertTrue("Missing key must stop database initialization.", failedClosed)
        assertTrue("Encrypted database must remain on disk after key loss.", databaseFile.isFile)
        val headerAfterKeyLoss = ByteArray(16)
        RandomAccessFile(databaseFile, "r").use { it.readFully(headerAfterKeyLoss) }
        assertTrue(headerBeforeKeyLoss.contentEquals(headerAfterKeyLoss))
    }
}
