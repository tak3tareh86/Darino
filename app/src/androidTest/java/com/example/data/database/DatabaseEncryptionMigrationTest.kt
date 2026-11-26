package com.example.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class DatabaseEncryptionMigrationTest {

    /**
     * Instrumentation APK context has its own package data directory and UID-scoped
     * Android Keystore. Never use targetContext here: it points at real Darino data.
     */
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    private val databaseFile: File
        get() = context.getDatabasePath(DATABASE_NAME)

    private val temporaryFile: File
        get() = File(databaseFile.parentFile, DATABASE_NAME + ".encrypted.tmp")

    private val plaintextBackup: File
        get() = File(databaseFile.parentFile, DATABASE_NAME + ".plaintext.bak")

    private val prefs
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Before
    fun prepareIsolatedTestStorage() {
        assertTestStorageIsIsolated()
        clearIsolatedTestArtifacts()
    }

    @After
    fun cleanIsolatedTestStorage() {
        assertTestStorageIsIsolated()
        clearIsolatedTestArtifacts()
    }

    @Test
    fun migratesExistingRoomDatabaseAndFailsClosedWhenWrappedKeyIsMissing() = runBlocking {
        // Build a genuine version-52 plaintext Room database in the instrumentation
        // package's private storage, never in the installed Darino package.
        val legacySnapshot = File(context.cacheDir, "darino-legacy-source.db")
        val plaintextRoom = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
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

        assertFalse("Plaintext backup must be removed after the first successful Room validation.", plaintextBackup.exists())
        assertTrue(databaseFile.isFile && databaseFile.length() > 16L)
        val header = ByteArray(16)
        RandomAccessFile(databaseFile, "r").use { it.readFully(header) }
        assertFalse(
            "Installed database must not retain the SQLite plaintext header.",
            header.contentEquals("SQLite format 3\u0000".toByteArray(StandardCharsets.US_ASCII))
        )

        // Simulate process death after the plaintext file was moved aside but before
        // the encrypted temporary file was installed. Recovery must resume from backup.
        legacySnapshot.copyTo(plaintextBackup, overwrite = true)
        databaseFile.copyTo(temporaryFile, overwrite = true)
        // Simulate stale SQLite sidecars left beside the encrypted temp after process death.
        File(temporaryFile.absolutePath + "-wal").writeBytes(byteArrayOf(1, 2, 3))
        File(temporaryFile.absolutePath + "-shm").writeBytes(byteArrayOf(4, 5, 6))
        File(temporaryFile.absolutePath + "-journal").writeBytes(byteArrayOf(7, 8, 9))
        assertTrue("Test setup must remove the isolated main database file.", databaseFile.delete())
        prefs.edit().putString(PREF_STATE, STATE_MIGRATING).commit()
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
        assertFalse("Recovery must remove plaintext backup only after Room validation.", plaintextBackup.exists())
        assertFalse("Recovery must consume the temporary encrypted file.", temporaryFile.exists())
        legacySnapshot.delete()

        val headerBeforeKeyLoss = ByteArray(16)
        RandomAccessFile(databaseFile, "r").use { it.readFully(headerBeforeKeyLoss) }

        // Losing the wrapped key must not recreate or replace the encrypted database.
        prefs.edit().remove(PREF_WRAPPED_KEY).commit()
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

    private fun assertTestStorageIsIsolated() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotEquals(
            "Instrumentation tests must not run with the Darino application package context.",
            targetContext.packageName,
            context.packageName
        )
        assertNotEquals(
            "Instrumentation database path must never resolve to the installed Darino database.",
            targetContext.getDatabasePath(DATABASE_NAME).canonicalPath,
            databaseFile.canonicalPath
        )
    }

    private fun clearIsolatedTestArtifacts() {
        // These paths are guarded above and belong only to the instrumentation package.
        context.deleteDatabase(DATABASE_NAME)
        listOf(databaseFile, temporaryFile, plaintextBackup).forEach { file ->
            listOf(
                file,
                File(file.absolutePath + "-wal"),
                File(file.absolutePath + "-shm"),
                File(file.absolutePath + "-journal")
            ).forEach { artifact ->
                if (artifact.exists()) check(artifact.delete()) {
                    "Could not clean isolated migration-test artifact: ${artifact.name}"
                }
            }
        }
        File(context.cacheDir, "darino-legacy-source.db").delete()
        check(prefs.edit().clear().commit()) { "Could not clear isolated migration-test preferences." }
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            if (containsAlias(KEY_ALIAS)) deleteEntry(KEY_ALIAS)
        }
    }

    private companion object {
        const val DATABASE_NAME = "finance_app_database"
        const val PREFS_NAME = "darino_database_encryption"
        const val PREF_WRAPPED_KEY = "wrapped_database_key"
        const val PREF_STATE = "migration_state"
        const val STATE_MIGRATING = "MIGRATING"
        const val KEY_ALIAS = "darino_database_key_v1"
    }
}
