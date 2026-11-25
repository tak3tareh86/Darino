package com.example.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

    @Test
    fun migratesExistingRoomDatabaseAndFailsClosedWhenWrappedKeyIsMissing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
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
        } finally {
            encryptedRoom.close()
        }

        val encryptedSequenceDb = AppDatabase.getDatabase(context)
        try {
            encryptedSequenceDb.openHelper.readableDatabase.query(
                "SELECT seq FROM sqlite_sequence WHERE name = 'checks'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(777L, cursor.getLong(0))
            }
        } finally {
            encryptedSequenceDb.close()
        }

        assertFalse("Plaintext backup must be removed after the first successful Room open.", plaintextBackup.exists())
        assertTrue(databaseFile.isFile && databaseFile.length() > 16L)
        val header = ByteArray(16)
        RandomAccessFile(databaseFile, "r").use { it.readFully(header) }
        assertFalse(
            "Installed database must not retain the SQLite plaintext header.",
            header.contentEquals("SQLite format 3\u0000".toByteArray(StandardCharsets.US_ASCII))
        )

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
        assertTrue(header.contentEquals(headerAfterKeyLoss))
    }
}
