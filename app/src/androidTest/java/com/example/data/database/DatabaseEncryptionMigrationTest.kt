package com.example.data.database

import android.content.Context
import android.os.Process
import android.util.Log
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
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
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
import javax.crypto.KeyGenerator

@RunWith(AndroidJUnit4::class)
class DatabaseEncryptionMigrationTest {

    /**
     * Tests target the dedicated migrationTest application variant. Its package ID and
     * runtime UID are isolated from the installed production Darino application.
     */
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

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
    fun refusesToCreateFreshDatabaseWhenMigrationStateIsUnknown() = runBlocking {
        // Re-establish the empty-state precondition immediately before this scenario.
        clearIsolatedTestArtifacts()
        assertFalse("Test isolation failed: database file remains after cleanup.", databaseFile.exists())
        assertFalse("Test isolation failed: plaintext backup remains after cleanup.", plaintextBackup.exists())
        check(prefs.edit()
            .putString(PREF_STATE, "UNKNOWN_MIGRATION_STATE")
            .putString(PREF_WRAPPED_KEY, "unreadable-placeholder")
            .commit())

        var initializationRejected = false
        try {
            DatabaseEncryptionManager.initializeAndMigrate(context)
        } catch (_: IllegalStateException) {
            initializationRejected = true
        }

        assertTrue("An unknown persisted migration state must fail closed.", initializationRejected)
        assertFalse("An unknown state must never trigger creation of an empty database.", databaseFile.exists())
        assertEquals("UNKNOWN_MIGRATION_STATE", prefs.getString(PREF_STATE, null))
        assertEquals("unreadable-placeholder", prefs.getString(PREF_WRAPPED_KEY, null))
    }

    @Test
    fun refusesFreshDatabaseCreationWhenWrappingKeyExistsWithoutMetadata() = runBlocking {
        // Re-establish the empty-state precondition immediately before this scenario.
        clearIsolatedTestArtifacts()
        assertFalse("Test isolation failed: database file remains after cleanup.", databaseFile.exists())
        assertFalse("Test isolation failed: plaintext backup remains after cleanup.", plaintextBackup.exists())
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        generator.generateKey()

        val keyStoreAfterGeneration = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        Log.e(
            "DatabaseMigrationTest",
            "WRAPPING_KEY_DIAG package=${context.packageName} uid=${Process.myUid()} " +
                "targetUid=${context.applicationInfo.uid} aliasPresent=${keyStoreAfterGeneration.containsAlias(KEY_ALIAS)} " +
                "dbPath=${databaseFile.absolutePath} dbExists=${databaseFile.exists()} " +
                "prefsState=${prefs.getString(PREF_STATE, null)} wrappedKeyPresent=${prefs.contains(PREF_WRAPPED_KEY)}"
        )
        assertTrue(
            "Test precondition failed: generated Android Keystore alias is not visible to the migration process.",
            keyStoreAfterGeneration.containsAlias(KEY_ALIAS)
        )
        assertFalse("Test precondition failed: database must be absent before orphan-key scenario.", databaseFile.exists())
        assertFalse("Test precondition failed: migration preferences must be empty.", prefs.contains(PREF_STATE))
        assertFalse("Test precondition failed: wrapped key metadata must be absent.", prefs.contains(PREF_WRAPPED_KEY))

        var initializationRejected = false
        try {
            DatabaseEncryptionManager.initializeAndMigrate(context)
        } catch (_: IllegalStateException) {
            initializationRejected = true
        }

        assertTrue("An orphaned wrapping key must prevent silent database recreation.", initializationRejected)
        assertFalse("An orphaned wrapping key must not result in a new database.", databaseFile.exists())
        assertFalse("No new wrapped key metadata may be generated.", prefs.contains(PREF_WRAPPED_KEY))
        assertFalse("No migration state may be fabricated.", prefs.contains(PREF_STATE))
    }

    @Test
    fun refusesToCreateFreshDatabaseWhenOrphanedSidecarsExist() = runBlocking {
        val orphanedArtifacts = listOf(
            databaseFile to File(databaseFile.absolutePath + "-wal"),
            plaintextBackup to File(plaintextBackup.absolutePath + "-shm")
        )

        orphanedArtifacts.forEach { (ownerFile, orphanedArtifact) ->
            // Each sidecar scenario starts from a completely clean isolated package.
            clearIsolatedTestArtifacts()
            assertFalse("Test precondition failed: sidecar owner must be absent.", ownerFile.exists())
            assertFalse("Test precondition failed: primary database must be absent.", databaseFile.exists())
            assertFalse("Test precondition failed: plaintext backup must be absent.", plaintextBackup.exists())

            check(orphanedArtifact.parentFile?.exists() == true || orphanedArtifact.parentFile?.mkdirs() == true)
            orphanedArtifact.writeBytes(byteArrayOf(1, 2, 3, 4))
            assertTrue("Test precondition failed: sidecar was not created.", orphanedArtifact.isFile)
            assertTrue("Test precondition failed: orphaned sidecar was not recognized before initialization.", DatabaseEncryptionManager.hasOrphanedDatabaseSidecars(ownerFile))
            assertFalse("Test precondition failed: sidecar owner unexpectedly exists.", ownerFile.exists())

            var initializationRejected = false
            try {
                DatabaseEncryptionManager.initializeAndMigrate(context)
            } catch (_: IllegalStateException) {
                initializationRejected = true
            }

            assertTrue(
                "An orphaned sidecar must stop initialization (owner=${ownerFile.name}, " +
                    "sidecar=${orphanedArtifact.name}, ownerExists=${ownerFile.exists()}, " +
                    "sidecarExists=${orphanedArtifact.exists()}).",
                initializationRejected
            )
            assertFalse("No empty database may be created when only a sidecar exists.", databaseFile.exists())
            assertFalse("No plaintext backup may be fabricated.", plaintextBackup.exists())
            assertTrue("The orphaned sidecar must remain untouched for recovery.", orphanedArtifact.exists())
            assertFalse("A database key must not be generated for an ambiguous database state.", prefs.contains(PREF_WRAPPED_KEY))
            assertFalse("Migration state must not be advanced for an ambiguous database state.", prefs.contains(PREF_STATE))
        }
    }

    @Test
    fun insufficientStoragePreflightPreservesSourceTempAndMigrationMetadata() = runBlocking {
        val plaintextRoom = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        ).build()
        try {
            plaintextRoom.openHelper.writableDatabase
        } finally {
            plaintextRoom.close()
        }

        val sourceBefore = databaseFile.readBytes()
        val temporaryMarker = byteArrayOf(9, 8, 7, 6, 5, 4)
        temporaryFile.writeBytes(temporaryMarker)
        val temporaryBefore = temporaryFile.readBytes()
        val stateBefore = prefs.all.toMap()

        var rejectedForLowStorage = false
        try {
            // Simulate a filesystem with zero free bytes without filling the emulator.
            DatabaseEncryptionManager.checkAvailableSpace(
                databaseFile,
                temporaryFile,
                availableBytesOverride = 0L
            )
        } catch (_: IllegalStateException) {
            rejectedForLowStorage = true
        }

        assertTrue("Low-storage preflight must reject migration.", rejectedForLowStorage)
        assertTrue("The plaintext source database must remain byte-for-byte unchanged.", sourceBefore.contentEquals(databaseFile.readBytes()))
        assertTrue("The existing temporary artifact must remain byte-for-byte unchanged.", temporaryBefore.contentEquals(temporaryFile.readBytes()))
        assertEquals("Migration preferences must not change during low-storage preflight.", stateBefore, prefs.all.toMap())
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

        // Simulate process death after the encrypted file was installed and the durable
        // INSTALLED marker was written, but before plaintext cleanup and READY commit.
        legacySnapshot.copyTo(plaintextBackup, overwrite = true)
        check(prefs.edit().putString(PREF_STATE, STATE_INSTALLED).commit())
        DatabaseEncryptionManager.initializeAndMigrate(context)
        assertFalse("Post-install recovery must delete plaintext only after encrypted Room validation.", plaintextBackup.exists())
        assertEquals("Post-install recovery must persist READY only after validation.", STATE_READY, prefs.getString(PREF_STATE, null))
        val postInstallRecoveredRoom = AppDatabase.buildEncrypted(
            context,
            databaseFile.absolutePath,
            DatabaseEncryptionManager.requireReadyKey(context)
        )
        try {
            postInstallRecoveredRoom.openHelper.readableDatabase.query(
                "SELECT numberDisplayMode FROM user_preferences WHERE userId = ?",
                arrayOf("migration-test-user")
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("PERSIAN", cursor.getString(0))
            }
        } finally {
            postInstallRecoveredRoom.close()
        }
        legacySnapshot.delete()

        val headerBeforeKeyLoss = ByteArray(16)
        RandomAccessFile(databaseFile, "r").use { it.readFully(headerBeforeKeyLoss) }

        // Losing the wrapped key must not recreate or replace the encrypted database.
        val wrappedKeyBeforeKeyLoss = checkNotNull(prefs.getString(PREF_WRAPPED_KEY, null))
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

        // Restore the original wrapped key metadata, then independently simulate loss
        // of the Android Keystore key that protects it.
        check(prefs.edit().putString(PREF_WRAPPED_KEY, wrappedKeyBeforeKeyLoss).commit())
        val wrappedKeyBeforeKeystoreLoss = wrappedKeyBeforeKeyLoss
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            check(containsAlias(KEY_ALIAS))
            deleteEntry(KEY_ALIAS)
        }
        var failedClosedOnKeystoreLoss = false
        try {
            DatabaseEncryptionManager.initializeAndMigrate(context)
        } catch (_: IllegalStateException) {
            failedClosedOnKeystoreLoss = true
        }
        assertTrue("Missing Android Keystore key must stop database initialization.", failedClosedOnKeystoreLoss)
        assertTrue("Encrypted database must remain after Android Keystore key loss.", databaseFile.isFile)
        val headerAfterKeystoreLoss = ByteArray(16)
        RandomAccessFile(databaseFile, "r").use { it.readFully(headerAfterKeystoreLoss) }
        assertTrue(headerBeforeKeyLoss.contentEquals(headerAfterKeystoreLoss))
        assertEquals(wrappedKeyBeforeKeystoreLoss, prefs.getString(PREF_WRAPPED_KEY, null))
    }

    private fun assertTestStorageIsIsolated() {
        assertEquals(
            "Migration tests must run under the dedicated migrationTest application ID.",
            MIGRATION_TEST_APPLICATION_ID,
            context.packageName
        )
        assertNotEquals(
            "Migration tests must never target the production Darino application ID.",
            PRODUCTION_APPLICATION_ID,
            context.packageName
        )
        assertEquals(
            "Android Keystore operations must execute under the isolated target application's UID.",
            context.applicationInfo.uid,
            Process.myUid()
        )
    }

    private fun clearIsolatedTestArtifacts() {
        // These paths are guarded above and belong only to the instrumentation package.
        // Use SQLCipher's database-aware deletion first so its own handles and
        // sidecar naming rules are respected, then remove any remaining test artifacts.
        // First ask Android's database helper to remove the database and its
        // framework-managed sidecars, then let SQLCipher remove any cipher-specific
        // artifacts. The final explicit pass below verifies every path is gone.
        context.deleteDatabase(DATABASE_NAME)
        if (databaseFile.exists()) {
            CipherDatabase.deleteDatabase(databaseFile)
        }
        listOf(databaseFile, temporaryFile, plaintextBackup).forEach { file ->
            listOf(
                file,
                File(file.absolutePath + "-wal"),
                File(file.absolutePath + "-shm"),
                File(file.absolutePath + "-journal")
            ).forEach { artifact ->
                if (artifact.exists()) check(artifact.deleteRecursively()) {
                    "Could not clean isolated migration-test artifact: ${artifact.absolutePath}"
                }
            }
        }
        File(context.cacheDir, "darino-legacy-source.db").delete()
        check(prefs.edit().clear().commit()) { "Could not clear isolated migration-test preferences." }
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            if (containsAlias(KEY_ALIAS)) deleteEntry(KEY_ALIAS)
        }
        check(!databaseFile.exists()) { "Isolated database file survived test cleanup: ${databaseFile.absolutePath}" }
        check(!temporaryFile.exists()) { "Encrypted temporary file survived test cleanup: ${temporaryFile.absolutePath}" }
        check(!plaintextBackup.exists()) { "Plaintext backup survived test cleanup: ${plaintextBackup.absolutePath}" }
    }

    private companion object {
        const val PRODUCTION_APPLICATION_ID = "com.aistudio.financemanager.vkqjxp"
        const val MIGRATION_TEST_APPLICATION_ID = "com.aistudio.financemanager.vkqjxp.migrationtest"
        const val DATABASE_NAME = "finance_app_database"
        const val PREFS_NAME = "darino_database_encryption"
        const val PREF_WRAPPED_KEY = "wrapped_database_key"
        const val PREF_STATE = "migration_state"
        const val STATE_MIGRATING = "MIGRATING"
        const val STATE_INSTALLED = "ENCRYPTED_INSTALLED"
        const val STATE_READY = "READY"
        const val KEY_ALIAS = "darino_database_key_v1"
    }
}
