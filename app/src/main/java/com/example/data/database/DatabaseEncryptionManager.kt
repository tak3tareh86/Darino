package com.example.data.database

import android.content.Context
import android.database.Cursor
import android.os.StatFs
import android.system.Os
import android.system.OsConstants
import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

/**
 * Owns the one-time SQLite -> SQLCipher transition.
 *
 * The original database is never deleted or overwritten until the encrypted copy
 * has passed SQLCipher integrity checks, schema/data comparison, and Room validation.
 */
object DatabaseEncryptionManager {
    private const val TAG = "DatabaseEncryption"
    private const val DATABASE_NAME = "finance_app_database"
    private const val PREFS_NAME = "darino_database_encryption"
    private const val PREF_WRAPPED_KEY = "wrapped_database_key"
    private const val PREF_STATE = "migration_state"
    private const val KEY_ALIAS = "darino_database_key_v1"
    private const val STATE_KEY_READY = "KEY_READY_EXISTING_DATABASE"
    private const val STATE_NEW_DATABASE_PENDING = "NEW_DATABASE_PENDING"
    private const val STATE_NEW_DATABASE_CREATED = "NEW_DATABASE_CREATED"
    private const val STATE_MIGRATING = "MIGRATING"
    private const val STATE_INSTALLED = "ENCRYPTED_INSTALLED"
    private const val STATE_READY = "READY"
    private const val MIN_FREE_SPACE_BYTES = 16L * 1024L * 1024L
    private val migrationMutex = Mutex()

    private data class DatabaseSnapshot(
        val schema: List<String>,
        val rowCounts: Map<String, Long>
    )

    /**
     * Must complete before any application component is allowed to obtain AppDatabase.
     */
    suspend fun initializeAndMigrate(context: Context) = withContext(Dispatchers.IO) {
        try {
            migrationMutex.withLock {
            val appContext = context.applicationContext
            System.loadLibrary("sqlcipher")
            val databaseFile = appContext.getDatabasePath(DATABASE_NAME)
            val temporaryFile = File(databaseFile.parentFile, DATABASE_NAME + ".encrypted.tmp")
            val plaintextBackup = File(databaseFile.parentFile, DATABASE_NAME + ".plaintext.bak")
            // An orphaned WAL/SHM/journal file without its owning database is an
            // ambiguous interrupted state. Never treat it as a clean first install.
            // Check every owner/sidecar pair directly before any database API can
            // create a file or clean up SQLite sidecars. A missing owner with any
            // remaining WAL/SHM/journal is an ambiguous state and must fail closed.
            // Use the same strict predicate exercised by instrumentation tests.
            // This rejects sidecars whose owner is absent, truncated, or otherwise
            // not a usable database before preferences/Keystore can create new state.
            val orphanedOwner = listOf(databaseFile, plaintextBackup, temporaryFile)
                .firstOrNull(::hasOrphanedDatabaseSidecars)
            check(orphanedOwner == null) {
                "فایل جانبی دیتابیس بدون فایل اصلی معتبر پیدا شد؛ برای جلوگیری از ساخت دیتابیس خالی، برنامه متوقف شد."
            }
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val state = prefs.getString(PREF_STATE, null)
            val keyWasAlreadyStored = prefs.contains(PREF_WRAPPED_KEY)
            val noRecoverableDatabase = !databaseFile.exists() && !plaintextBackup.exists()
            if (noRecoverableDatabase && (
                    (state != null && state != STATE_NEW_DATABASE_PENDING) ||
                    (state == null && keyWasAlreadyStored) ||
                    (state == null && !keyWasAlreadyStored && hasExistingWrappingKey()) ||
                    hasOrphanedDatabaseSidecars(temporaryFile)
                )
            ) {
                throw IllegalStateException(
                    "وضعیت یا کلید قبلی دیتابیس بدون فایل قابل بازیابی پیدا شد؛ برای جلوگیری از ساخت دیتابیس خالی، برنامه متوقف شد."
                )
            }

            // Check capacity before generating key metadata, checkpointing SQLite,
            // or deleting a stale encrypted temp. Low-space exits preserve every file.
            val plaintextSourceForPreflight = when {
                databaseFile.exists() && hasPlaintextSQLiteHeader(databaseFile) -> databaseFile
                !databaseFile.exists() && plaintextBackup.exists() &&
                    hasPlaintextSQLiteHeader(plaintextBackup) -> plaintextBackup
                else -> null
            }
            plaintextSourceForPreflight?.let { checkAvailableSpace(it, temporaryFile) }

            val key = loadOrCreateDatabaseKey(appContext, databaseFile, temporaryFile, plaintextBackup)
            if (databaseFile.exists() && hasPlaintextSQLiteHeader(databaseFile) &&
                (state == STATE_NEW_DATABASE_PENDING || state == STATE_NEW_DATABASE_CREATED)
            ) {
                throw IllegalStateException("وضعیت پایگاه داده جدید با فایل قدیمی سازگار نیست؛ هیچ فایلی تغییر نکرد.")
            }

            if (databaseFile.exists() && !hasPlaintextSQLiteHeader(databaseFile)) {
                if (state == STATE_READY && !plaintextBackup.exists()) {
                    verifyEncryptedDatabaseFast(databaseFile, key)
                    return@withLock
                }
                validateEncryptedDatabase(appContext, databaseFile, key)
                validateWithRoom(appContext, databaseFile, key)
                checkpointEncryptedDatabase(databaseFile, key)
                syncFile(databaseFile)
                // A plaintext copy is retained until this first successful encrypted Room open.
                if (plaintextBackup.exists() && !plaintextBackup.delete()) {
                    throw IllegalStateException("نسخه موقت بدون رمزگذاری پس از اعتبارسنجی قابل حذف نیست.")
                }
                syncDirectory(databaseFile.parentFile ?: appContext.filesDir)
                check(prefs.edit().putString(PREF_STATE, STATE_READY).commit()) {
                    "ثبت وضعیت نهایی رمزگذاری دیتابیس ناموفق بود."
                }
                return@withLock
            }

            if (!databaseFile.exists() && !plaintextBackup.exists()) {
                if (temporaryFile.exists() || state == STATE_MIGRATING ||
                    state == STATE_INSTALLED || state == STATE_READY
                ) {
                    throw IllegalStateException("وضعیت مهاجرت ناقص است و منبع قابل بازیابی وجود ندارد؛ هیچ دیتابیس خالی ساخته نشد.")
                }
                // First installation: create the database encrypted from its first byte.
                val encryptedDb = AppDatabase.buildEncrypted(
                    appContext,
                    databaseFile.absolutePath,
                    key
                )
                try {
                    encryptedDb.openHelper.writableDatabase
                } finally {
                    encryptedDb.close()
                }
                syncFile(databaseFile)
                syncDirectory(databaseFile.parentFile ?: appContext.filesDir)
                check(prefs.edit().putString(PREF_STATE, STATE_NEW_DATABASE_CREATED).commit()) {
                    "ثبت وضعیت ایجاد پایگاه داده رمزگذاری‌شده ناموفق بود."
                }
                validateEncryptedDatabase(appContext, databaseFile, key)
                checkpointEncryptedDatabase(databaseFile, key)
                syncFile(databaseFile)
                syncDirectory(databaseFile.parentFile ?: appContext.filesDir)
                check(prefs.edit().putString(PREF_STATE, STATE_READY).commit()) {
                    "ثبت وضعیت دیتابیس جدید ناموفق بود."
                }
                return@withLock
            }

            val sourceFile = if (databaseFile.exists()) databaseFile else plaintextBackup
            if (!hasPlaintextSQLiteHeader(sourceFile)) {
                throw IllegalStateException("وضعیت فایل دیتابیس با وضعیت مهاجرت سازگار نیست؛ هیچ فایلی تغییر نکرد.")
            }

            if (state == STATE_READY) {
                throw IllegalStateException("وضعیت رمزگذاری با محتوای فایل دیتابیس همخوانی ندارد؛ برای جلوگیری از بازگشت ناامن، دیتابیس باز نشد.")
            }

            checkpointPlaintextDatabase(sourceFile)
            // A stale encrypted temp is disposable only while the validated plaintext
            // source remains intact. Remove its main file and every SQLite sidecar.
            deleteDatabaseArtifacts(temporaryFile)

            check(prefs.edit().putString(PREF_STATE, STATE_MIGRATING).commit()) {
                "ثبت وضعیت شروع مهاجرت ناموفق بود."
            }

            val sourceSnapshot: DatabaseSnapshot
            val sourceDb = CipherDatabase.openDatabase(
                sourceFile.absolutePath,
                null,
                CipherDatabase.OPEN_READWRITE,
                null
            )
            try {
                assertIntegrity(sourceDb, "دیتابیس اصلی")
                assertNoForeignKeyViolations(sourceDb, "دیتابیس اصلی")
                sourceSnapshot = snapshot(sourceDb)
                val sourceVersion = sourceDb.version
                val sourceAutoVacuum = readPragmaLong(sourceDb, "PRAGMA main.auto_vacuum")
                val sourceApplicationId = readPragmaLong(sourceDb, "PRAGMA main.application_id")
                val sourceSecureDelete = readPragmaLong(sourceDb, "PRAGMA main.secure_delete")

                // Reserve the destination inside the app-private database directory before
                // ATTACH. This makes filesystem creation failures explicit while the plaintext
                // source is still intact, instead of relying on SQLite to create the file implicitly.
                val temporaryDirectory = temporaryFile.parentFile
                check(
                    temporaryDirectory != null &&
                        (temporaryDirectory.isDirectory || temporaryDirectory.mkdirs()) &&
                        temporaryDirectory.canWrite()
                ) {
                    "پوشه موقت رمزگذاری دیتابیس قابل دسترسی نیست؛ دیتابیس اصلی حفظ شد."
                }
                // Probe that the directory can create files, then remove the empty
                // placeholder. SQLCipher ATTACH must create and initialize the encrypted
                // destination itself; an existing zero-byte file can be rejected as NOTADB.
                check(!temporaryFile.exists() && temporaryFile.createNewFile()) {
                    "فایل موقت رمزگذاری دیتابیس قابل ایجاد نیست؛ دیتابیس اصلی حفظ شد."
                }
                check(temporaryFile.delete()) {
                    "فایل آزمایشی موقت دیتابیس قابل حذف نیست؛ دیتابیس اصلی حفظ شد."
                }
                // Create a valid encrypted SQLCipher database before ATTACH. The
                // SQLCipher Android API explicitly supports this creation path and
                // avoids relying on ATTACH to create a not-yet-existing file.
                // SQLCipher APIs may retain or clear the supplied passphrase buffer.
                // Pass defensive copies so the migration key remains stable for ATTACH,
                // validation, Room, checkpoint, and crash-recovery operations.
                val destinationDb = CipherDatabase.openOrCreateDatabase(
                    temporaryFile.absolutePath,
                    key.copyOf(),
                    null,
                    null
                )
                try {
                    // Opening a new SQLCipher database may leave a zero-byte file
                    // until its first page is written. Persist a harmless header
                    // value so ATTACH receives an initialized encrypted database.
                    destinationDb.rawQuery("PRAGMA user_version = 1", null).use { cursor ->
                        while (cursor.moveToNext()) {
                            // Drain the PRAGMA cursor to execute the assignment.
                        }
                    }
                } finally {
                    destinationDb.close()
                }
                check(temporaryFile.isFile && temporaryFile.length() > 16L) {
                    "فایل مقصد رمزگذاری‌شده توسط SQLCipher ایجاد نشد؛ دیتابیس اصلی حفظ شد."
                }
                val escapedPath = temporaryFile.absolutePath.replace("'", "''")
                executeMigrationStatement(
                    sourceDb,
                    "ATTACH encrypted destination",
                    "ATTACH DATABASE '" + escapedPath + "' AS encrypted KEY '" +
                        String(key, StandardCharsets.UTF_8) + "'"
                )
                try {
                    // sqlcipher_export does not copy auto_vacuum or user_version.
                    // Set target storage properties before creating the exported schema.
                    executeMigrationPragma(
                        sourceDb, "set encrypted.auto_vacuum",
                        "PRAGMA encrypted.auto_vacuum = " + sourceAutoVacuum
                    )
                    executeMigrationPragma(
                        sourceDb, "set encrypted.secure_delete",
                        "PRAGMA encrypted.secure_delete = " + sourceSecureDelete
                    )
                    sourceDb.rawQuery("SELECT sqlcipher_export('encrypted')", null).use { cursor ->
                        check(cursor.moveToFirst()) { "انتقال اطلاعات به دیتابیس رمزگذاری‌شده ناموفق بود." }
                    }
                    assertRowsEquivalent(sourceDb)
                    executeMigrationPragma(
                        sourceDb, "set encrypted.application_id",
                        "PRAGMA encrypted.application_id = " + sourceApplicationId
                    )
                    executeMigrationPragma(
                        sourceDb, "set encrypted.user_version",
                        "PRAGMA encrypted.user_version = " + sourceVersion
                    )
                } finally {
                    executeMigrationStatement(
                        sourceDb, "DETACH encrypted destination", "DETACH DATABASE encrypted"
                    )
                }
            } finally {
                sourceDb.close()
            }
            // The export may consume substantially more space than the initial preflight
            // predicted. Re-check the real filesystem before checkpointing or swapping files.
            // Any failure here leaves the original plaintext database as the sole source.
            checkRemainingSpace(databaseFile.parentFile ?: appContext.filesDir)

            // Ensure the source has no residual plaintext WAL/SHM sidecars before the file swap.
            checkpointPlaintextDatabase(sourceFile)
            // Persist the fully checkpointed plaintext source before the atomic rename
            // that moves it to the recovery backup. This strengthens power-loss durability.
            syncFile(sourceFile)
            syncDirectory(sourceFile.parentFile ?: appContext.filesDir)

            syncFile(temporaryFile)
            validateEncryptedDatabase(appContext, temporaryFile, key)
            val encryptedSnapshot = openCipherSnapshot(temporaryFile, key)
            check(sourceSnapshot == encryptedSnapshot) {
                "ساختار یا تعداد رکوردهای دیتابیس رمزگذاری‌شده با منبع یکسان نیست؛ نسخه اصلی حفظ شد."
            }
            validateWithRoom(appContext, temporaryFile, key)
            // Validation and Room opening can also create temporary filesystem activity.
            // Preserve a safety reserve before the final encrypted checkpoint and atomic swap.
            checkRemainingSpace(databaseFile.parentFile ?: appContext.filesDir)
            checkpointEncryptedDatabase(temporaryFile, key)
            syncFile(temporaryFile)

            // Atomic same-directory rename keeps the plaintext source recoverable on interruption.
            if (databaseFile.exists()) {
                if (plaintextBackup.exists()) {
                    throw IllegalStateException("نسخه پشتیبان بدون رمزگذاری از اجرای قبلی موجود است؛ هیچ فایلی جایگزین نشد.")
                }
                check(databaseFile.renameTo(plaintextBackup)) {
                    "انتقال دیتابیس اصلی به نسخه محافظت‌شده ناموفق بود؛ دیتابیس اصلی حفظ شد."
                }
                syncDirectory(databaseFile.parentFile ?: appContext.filesDir)
            }
            if (!temporaryFile.renameTo(databaseFile)) {
                if (!databaseFile.exists() && plaintextBackup.exists()) {
                    plaintextBackup.renameTo(databaseFile)
                }
                throw IllegalStateException("جایگزینی دیتابیس رمزگذاری‌شده ناموفق بود؛ نسخه اصلی حفظ شد.")
            }
            syncDirectory(databaseFile.parentFile ?: appContext.filesDir)

            check(prefs.edit().putString(PREF_STATE, STATE_INSTALLED).commit()) {
                "ثبت وضعیت نصب دیتابیس رمزگذاری‌شده ناموفق بود."
            }

            // Keep the plaintext backup until the final Room open against the installed path succeeds.
            validateEncryptedDatabase(appContext, databaseFile, key)
            validateWithRoom(appContext, databaseFile, key)
            checkpointEncryptedDatabase(databaseFile, key)
            syncFile(databaseFile)
            if (plaintextBackup.exists() && !plaintextBackup.delete()) {
                throw IllegalStateException("دیتابیس رمزگذاری شد اما نسخه بدون رمزگذاری قابل حذف نیست؛ برنامه برای حفاظت از داده‌ها متوقف شد.")
            }
            syncDirectory(databaseFile.parentFile ?: appContext.filesDir)
            check(prefs.edit().putString(PREF_STATE, STATE_READY).commit()) {
                "ثبت وضعیت نهایی رمزگذاری دیتابیس ناموفق بود."
            }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Callers may log caught exceptions. Never let a native SQLCipher message,
            // SQL statement, or nested cause escape the migration boundary.
            throw IllegalStateException(
                "راه‌اندازی امن پایگاه داده ناموفق بود؛ جزئیات داخلی برای جلوگیری از افشای اطلاعات ثبت نشد."
            )
        }
    }

    /**
     * Returns the key only after the durable READY marker exists.
     * AppDatabase calls this guard to prevent accidental plaintext Room opens.
     */
    fun requireReadyKey(context: Context): ByteArray {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        check(prefs.getString(PREF_STATE, null) == STATE_READY) {
            "رمزگذاری دیتابیس هنوز کامل و اعتبارسنجی نشده است؛ دسترسی به دیتابیس مسدود شد."
        }
        val databaseFile = context.applicationContext.getDatabasePath(DATABASE_NAME)
        check(databaseFile.isFile && databaseFile.length() > 16L && !hasPlaintextSQLiteHeader(databaseFile)) {
            "فایل دیتابیس رمزگذاری‌شده موجود یا معتبر نیست؛ از ایجاد خودکار دیتابیس خالی جلوگیری شد."
        }
        System.loadLibrary("sqlcipher")
        return decryptStoredKey(context.applicationContext)
    }

    private fun loadOrCreateDatabaseKey(
        context: Context,
        databaseFile: File,
        temporaryFile: File,
        plaintextBackup: File
    ): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wrapped = prefs.getString(PREF_WRAPPED_KEY, null)
        if (wrapped != null) return decryptStoredKey(context)
        val existingState = prefs.getString(PREF_STATE, null)
        if (existingState != null) {
            throw IllegalStateException("کلید رمزگذاری دیتابیس مفقود است؛ هیچ دیتابیسی حذف یا بازسازی نشد.")
        }

        // Never generate a replacement key for an encrypted or interrupted database.
        if ((databaseFile.exists() && !hasPlaintextSQLiteHeader(databaseFile)) ||
            hasAnyDatabaseArtifact(temporaryFile) || plaintextBackup.exists()
        ) {
            throw IllegalStateException("کلید رمزگذاری دیتابیس مفقود است؛ هیچ دیتابیسی حذف یا بازسازی نشد.")
        }

        val initialState = if (databaseFile.exists() || plaintextBackup.exists()) {
            STATE_KEY_READY
        } else {
            STATE_NEW_DATABASE_PENDING
        }
        val randomKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val passphrase = randomKey.joinToString("") { "%02x".format(it.toInt() and 0xff) }
            .toByteArray(StandardCharsets.UTF_8)
        val secretKey = getOrCreateWrappingKey(context)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(passphrase)
        val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + "." +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(
            prefs.edit()
                .putString(PREF_WRAPPED_KEY, payload)
                .putString(PREF_STATE, initialState)
                .commit()
        ) { "ذخیره امن کلید دیتابیس ناموفق بود." }
        randomKey.fill(0)
        return passphrase
    }

    private fun decryptStoredKey(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val payload = prefs.getString(PREF_WRAPPED_KEY, null)
            ?: throw IllegalStateException("کلید رمزگذاری دیتابیس پیدا نشد؛ اطلاعات دست‌نخورده باقی ماندند.")
        val parts = payload.split('.', limit = 2)
        check(parts.size == 2) { "ساختار کلید رمزگذاری دیتابیس معتبر نیست." }
        try {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getExistingWrappingKey(), GCMParameterSpec(128, iv))
            return cipher.doFinal(encrypted)
        } catch (error: Exception) {
            throw IllegalStateException("کلید دیتابیس قابل بازیابی نیست؛ دیتابیس حذف یا بازسازی نشد.", error)
        }
    }

    private fun getOrCreateWrappingKey(context: Context): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
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
        return generator.generateKey()
    }

    private fun getExistingWrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            ?: throw IllegalStateException("کلید Android Keystore مفقود است.")
    }

    private fun hasPlaintextSQLiteHeader(file: File): Boolean {
        if (!file.isFile || file.length() < 16L) return false
        val header = ByteArray(16)
        RandomAccessFile(file, "r").use { it.readFully(header) }
        return header.contentEquals("SQLite format 3\u0000".toByteArray(StandardCharsets.US_ASCII))
    }

    private fun checkpointPlaintextDatabase(file: File) {
        val database = android.database.sqlite.SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
        )
        try {
            database.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 0 && cursor.getInt(1) == cursor.getInt(2)) {
                    "ثبت تغییرات WAL کامل نشد؛ مهاجرت متوقف شد و دیتابیس اصلی حفظ شد."
                }
            }
            database.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getString(0) == "ok") {
                    "بررسی سلامت دیتابیس اصلی ناموفق بود؛ هیچ تغییری اعمال نشد."
                }
            }
        } finally {
            database.close()
        }
        val walFile = File(file.absolutePath + "-wal")
        val shmFile = File(file.absolutePath + "-shm")
        val journalFile = File(file.absolutePath + "-journal")
        if (walFile.exists()) {
            check(walFile.length() == 0L && walFile.delete()) {
                "فایل WAL پس از checkpoint هنوز داده دارد یا قابل پاک‌سازی نیست؛ دیتابیس اصلی حفظ شد."
            }
        }
        if (shmFile.exists()) {
            check(shmFile.delete()) {
                "فایل موقت SHM قابل پاک‌سازی نیست؛ دیتابیس اصلی حفظ شد."
            }
        }
        if (journalFile.exists()) {
            check(journalFile.delete()) {
                "فایل Journal پس از checkpoint قابل پاک‌سازی نیست؛ دیتابیس اصلی حفظ شد."
            }
        }
    }

    private fun syncFile(file: File) {
        RandomAccessFile(file, "rw").use { it.fd.sync() }
    }

    private fun syncDirectory(directory: File) {
        val descriptor = Os.open(directory.absolutePath, OsConstants.O_RDONLY, 0)
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }

    private fun databaseSidecars(file: File): List<File> = listOf(
        File(file.absolutePath + "-wal"),
        File(file.absolutePath + "-shm"),
        File(file.absolutePath + "-journal")
    )

    private fun hasAnyDatabaseArtifact(file: File): Boolean =
        file.exists() || databaseSidecars(file).any { it.exists() }

    internal fun hasOrphanedDatabaseSidecars(file: File): Boolean {
        val sidecarsExist = databaseSidecars(file).any { it.exists() }
        if (!sidecarsExist) return false

        // A zero-length/truncated placeholder is not a recoverable database owner
        // for WAL/SHM/journal files. Treat it as ambiguous rather than initializing
        // over it and silently accepting an orphaned sidecar set.
        return !file.isFile || file.length() <= 16L
    }

    private fun hasExistingWrappingKey(): Boolean {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        // Check for the actual secret-key entry, not only the alias metadata.
        // An orphaned key must block first-install database creation.
        // Any surviving alias is evidence of a prior encryption attempt. Treat it
        // as an orphan even if Android Keystore cannot currently unwrap the entry.
        return keyStore.containsAlias(KEY_ALIAS)
    }

    private fun deleteDatabaseArtifacts(file: File) {
        (listOf(file) + databaseSidecars(file)).forEach { artifact ->
            if (artifact.exists() && !artifact.delete()) {
                throw IllegalStateException(
                    "فایل موقت دیتابیس یا فایل جانبی آن قابل پاک‌سازی نیست؛ نسخه اصلی حفظ شد: ${artifact.name}"
                )
            }
        }
    }

    internal fun checkAvailableSpace(
        file: File,
        reclaimableTemporaryFile: File,
        availableBytesOverride: Long? = null
    ) {
        val stat = StatFs(file.parentFile?.absolutePath ?: file.absolutePath)
        val sourceSidecars = databaseSidecars(file)
        val sourceFootprint = sourceSidecars.fold(file.length()) { total, sidecar ->
            Math.addExact(total, if (sidecar.isFile) sidecar.length() else 0L)
        }
        val required = Math.addExact(Math.multiplyExact(sourceFootprint, 2L), MIN_FREE_SPACE_BYTES)
        // A stale temp and its SQLite sidecars are reclaimable only after this
        // preflight succeeds. Until then, every existing file remains untouched.
        val reclaimableBytes = databaseSidecars(reclaimableTemporaryFile).fold(
            if (reclaimableTemporaryFile.isFile) reclaimableTemporaryFile.length() else 0L
        ) { total, sidecar ->
            Math.addExact(total, if (sidecar.isFile) sidecar.length() else 0L)
        }
        val availableBytes = availableBytesOverride ?: stat.availableBytes
        check(Math.addExact(availableBytes, reclaimableBytes) >= required) {
            "فضای خالی برای رمزگذاری امن دیتابیس کافی نیست؛ هیچ فایل یا تنظیم مهاجرتی تغییر نکرد و پس از آزادسازی فضا می‌توان دوباره تلاش کرد."
        }
    }

    /**
     * Keeps a safety reserve after export and before the atomic file swap.
     * A low-space failure is deliberately non-destructive: the plaintext source,
     * migration marker, and encrypted temporary file are left for a safe retry.
     */
    internal fun checkRemainingSpace(
        directory: File,
        availableBytesOverride: Long? = null
    ) {
        val availableBytes = availableBytesOverride ?: StatFs(directory.absolutePath).availableBytes
        check(availableBytes >= MIN_FREE_SPACE_BYTES) {
            "فضای آزاد پس از انتقال اطلاعات کافی نیست؛ فایل اصلی حفظ شد و پس از آزادسازی فضا می‌توان دوباره تلاش کرد."
        }
    }

    /**
     * Normal launches verify that the persisted key still opens the encrypted file,
     * without running a full integrity scan over a potentially large database each time.
     */
    private fun verifyEncryptedDatabaseFast(file: File, key: ByteArray) {
        val database = CipherDatabase.openDatabase(
            file.absolutePath,
            key.copyOf(),
            null,
            CipherDatabase.OPEN_READWRITE,
            null,
            null
        )
        try {
            database.rawQuery("PRAGMA cipher_status", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 1) {
                    "SQLCipher کلید دیتابیس را تأیید نکرد."
                }
            }
            database.rawQuery("SELECT COUNT(*) FROM sqlite_master", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getLong(0) > 0L) {
                    "ساختار دیتابیس رمزگذاری‌شده قابل خواندن نیست."
                }
            }
        } finally {
            database.close()
        }
    }

    private fun checkpointEncryptedDatabase(file: File, key: ByteArray) {
        val database = CipherDatabase.openDatabase(
            file.absolutePath,
            key.copyOf(),
            null,
            CipherDatabase.OPEN_READWRITE,
            null,
            null
        )
        try {
            database.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 0 && cursor.getInt(1) == cursor.getInt(2)) {
                    "ثبت تغییرات WAL دیتابیس رمزگذاری‌شده کامل نشد؛ نسخه اصلی بدون رمزگذاری حفظ شد."
                }
            }
        } finally {
            database.close()
        }
        val walFile = File(file.absolutePath + "-wal")
        val shmFile = File(file.absolutePath + "-shm")
        val journalFile = File(file.absolutePath + "-journal")
        if (walFile.exists()) {
            check(walFile.length() == 0L && walFile.delete()) {
                "WAL دیتابیس رمزگذاری‌شده پس از checkpoint قابل پاک‌سازی نیست."
            }
        }
        if (shmFile.exists()) {
            check(shmFile.delete()) {
                "SHM دیتابیس رمزگذاری‌شده قابل پاک‌سازی نیست."
            }
        }
        if (journalFile.exists()) {
            check(journalFile.delete()) {
                "Journal دیتابیس رمزگذاری‌شده قابل پاک‌سازی نیست."
            }
        }
    }

    private fun validateEncryptedDatabase(context: Context, file: File, key: ByteArray) {
        check(file.isFile && file.length() > 0L) { "فایل دیتابیس رمزگذاری‌شده ایجاد نشده است." }
        val database = CipherDatabase.openDatabase(
            file.absolutePath,
            key.copyOf(),
            null,
            CipherDatabase.OPEN_READWRITE,
            null,
            null
        )
        try {
            database.rawQuery("PRAGMA cipher_status", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 1) {
                    "SQLCipher رمزگذاری فعال را تأیید نکرد."
                }
            }
            assertCipherIntegrity(database)
            assertIntegrity(database, "دیتابیس رمزگذاری‌شده")
            assertNoForeignKeyViolations(database, "دیتابیس رمزگذاری‌شده")
        } finally {
            database.close()
        }
    }

    /**
     * Diagnostic wrapper: preserves the SQL operation and adds its exact label to
     * instrumentation failures so unsupported execSQL statements can be identified.
     */
    internal fun executeMigrationStatement(
        database: CipherDatabase,
        label: String,
        sql: String
    ) {
        try {
            database.execSQL(sql)
        } catch (error: Exception) {
            // Native SQLite errors may echo the ATTACH statement, which contains the key.
            // Do not retain the original exception as a cause: its message can reach Logcat.
            throw IllegalStateException(
                "SQLCipher migration statement failed [$label] (${error.javaClass.simpleName})"
            )
        }
    }

    /**
     * PRAGMA assignments can return a result set in SQLCipher's Android API.
     * Execute them through rawQuery rather than execSQL, which expects an update count.
     */
    private fun executeMigrationPragma(
        database: CipherDatabase,
        label: String,
        sql: String
    ) {
        try {
            database.rawQuery(sql, null).use { cursor ->
                // rawQuery executes lazily; advancing the cursor ensures the PRAGMA
                // assignment is actually applied before the encrypted export proceeds.
                while (cursor.moveToNext()) {
                    // PRAGMA assignments normally return no rows; drain any result safely.
                }
            }
        } catch (error: Exception) {
            // Never include SQL or retain the native cause; diagnostics must not expose SQL text.
            throw IllegalStateException(
                "SQLCipher migration PRAGMA failed [$label] (${error.javaClass.simpleName})"
            )
        }
    }

    private fun readPragmaLong(database: CipherDatabase, sql: String): Long {
        return database.rawQuery(sql, null).use { cursor ->
            check(cursor.moveToFirst()) { "خواندن ویژگی‌های فایل دیتابیس ناموفق بود." }
            cursor.getLong(0)
        }
    }

    private fun assertCipherIntegrity(database: CipherDatabase) {
        database.rawQuery("PRAGMA cipher_integrity_check", null).use { cursor ->
            check(!cursor.moveToFirst()) {
                "بررسی صحت رمزنگاری صفحات دیتابیس ناموفق بود؛ نسخه اصلی حفظ شد."
            }
        }
    }

    private fun assertIntegrity(database: CipherDatabase, label: String) {
        database.rawQuery("PRAGMA integrity_check", null).use { cursor ->
            check(cursor.moveToFirst() && cursor.getString(0) == "ok") {
                "بررسی سلامت " + label + " ناموفق بود."
            }
        }
    }

    private fun assertNoForeignKeyViolations(database: CipherDatabase, label: String) {
        database.rawQuery("PRAGMA foreign_key_check", null).use { cursor ->
            check(!cursor.moveToFirst()) {
                "رابطه کلیدهای خارجی در " + label + " ناسازگار است."
            }
        }
    }

    /**
     * Compare actual row values in both directions, not only table counts.
     * Count comparison separately protects duplicate-row multiplicity.
     */
    /**
     * Compare row multisets including duplicate counts and SQLite storage classes.
     * GROUP BY uses typeof() and BINARY collation so case-folding or numeric coercion
     * cannot make distinct stored values appear equivalent.
     */
    private fun assertRowsEquivalent(database: CipherDatabase) {
        val tables = mutableListOf<String>()
        database.rawQuery(
            "SELECT name FROM main.sqlite_master WHERE type = 'table' " +
                "AND name NOT LIKE 'sqlite_%' ORDER BY name",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) tables.add(cursor.getString(0))
        }
        val hasSequence = database.rawQuery(
            "SELECT 1 FROM main.sqlite_master WHERE type = 'table' AND name = 'sqlite_sequence'",
            null
        ).use { it.moveToFirst() }
        if (hasSequence) tables.add("sqlite_sequence")

        tables.forEach { table ->
            val quotedTable = "\"" + table.replace("\"", "\"\"") + "\""
            val columns = database.rawQuery(
                "SELECT * FROM main." + quotedTable + " LIMIT 0",
                null
            ).use { cursor ->
                (0 until cursor.columnCount).map { cursor.getColumnName(it) }
            }
            check(columns.isNotEmpty()) { "جدول " + table + " ستون قابل مقایسه ندارد." }

            val quotedColumns = columns.map { "\"" + it.replace("\"", "\"\"") + "\"" }
            val groupTerms = quotedColumns.flatMap { column ->
                listOf("typeof(" + column + ")", column + " COLLATE BINARY")
            }.joinToString(", ")
            val selectTerms = quotedColumns.flatMap { column ->
                listOf("typeof(" + column + ")", column)
            }.joinToString(", ")
            val sourceSql = "SELECT " + selectTerms + ", COUNT(*) FROM main." + quotedTable +
                " GROUP BY " + groupTerms + " ORDER BY " + groupTerms
            val targetSql = "SELECT " + selectTerms + ", COUNT(*) FROM encrypted." + quotedTable +
                " GROUP BY " + groupTerms + " ORDER BY " + groupTerms

            database.rawQuery(sourceSql, null).use { sourceCursor ->
                database.rawQuery(targetSql, null).use { targetCursor ->
                    while (true) {
                        val sourceHasRow = sourceCursor.moveToNext()
                        val targetHasRow = targetCursor.moveToNext()
                        check(sourceHasRow == targetHasRow) {
                            "تعداد ردیف‌های متمایز جدول " + table + " پس از رمزگذاری تغییر کرده است."
                        }
                        if (!sourceHasRow) break

                        for (index in columns.indices) {
                            val typeIndex = index * 2
                            val valueIndex = typeIndex + 1
                            check(sourceCursor.getString(typeIndex) == targetCursor.getString(typeIndex)) {
                                "نوع ذخیره‌سازی ستون " + columns[index] + " در جدول " + table + " تغییر کرده است."
                            }
                            check(sqliteValuesEqual(sourceCursor, valueIndex, targetCursor, valueIndex)) {
                                "مقدار ستون " + columns[index] + " در جدول " + table + " تغییر کرده است."
                            }
                        }

                        val countIndex = columns.size * 2
                        check(sourceCursor.getLong(countIndex) == targetCursor.getLong(countIndex)) {
                            "تعداد نسخه‌های تکراری یک ردیف در جدول " + table + " تغییر کرده است."
                        }
                    }
                }
            }
        }
    }

    private fun sqliteValuesEqual(
        left: Cursor,
        leftIndex: Int,
        right: Cursor,
        rightIndex: Int
    ): Boolean {
        if (left.getType(leftIndex) != right.getType(rightIndex)) return false
        return when (left.getType(leftIndex)) {
            Cursor.FIELD_TYPE_NULL -> true
            Cursor.FIELD_TYPE_INTEGER -> left.getLong(leftIndex) == right.getLong(rightIndex)
            Cursor.FIELD_TYPE_FLOAT ->
                java.lang.Double.doubleToLongBits(left.getDouble(leftIndex)) ==
                    java.lang.Double.doubleToLongBits(right.getDouble(rightIndex))
            Cursor.FIELD_TYPE_STRING -> left.getString(leftIndex) == right.getString(rightIndex)
            Cursor.FIELD_TYPE_BLOB -> {
                val leftBlob = left.getBlob(leftIndex)
                val rightBlob = right.getBlob(rightIndex)
                leftBlob.contentEquals(rightBlob)
            }
            else -> false
        }
    }

    private fun snapshot(database: CipherDatabase): DatabaseSnapshot {
        val schema = mutableListOf<String>()
        val tables = mutableListOf<String>()
        database.rawQuery(
            "SELECT type, name, tbl_name, COALESCE(sql, '') FROM sqlite_master " +
                "WHERE name NOT LIKE 'sqlite_%' ORDER BY type, name",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                schema.add(
                    cursor.getString(0) + "|" + cursor.getString(1) + "|" +
                        cursor.getString(2) + "|" + cursor.getString(3)
                )
                if (cursor.getString(0) == "table") tables.add(cursor.getString(1))
            }
        }
        val counts = linkedMapOf<String, Long>()
        tables.forEach { table ->
            val quoted = "\"" + table.replace("\"", "\"\"") + "\""
            database.rawQuery("SELECT COUNT(*) FROM " + quoted, null).use { cursor ->
                check(cursor.moveToFirst())
                counts[table] = cursor.getLong(0)
            }
        }
        return DatabaseSnapshot(schema, counts)
    }

    private fun openCipherSnapshot(file: File, key: ByteArray): DatabaseSnapshot {
        val database = CipherDatabase.openDatabase(
            file.absolutePath,
            key.copyOf(),
            null,
            CipherDatabase.OPEN_READWRITE,
            null,
            null
        )
        return try {
            snapshot(database)
        } finally {
            database.close()
        }
    }

    private fun validateWithRoom(context: Context, file: File, key: ByteArray) {
        val roomDatabase = AppDatabase.buildEncrypted(context, file.absolutePath, key.copyOf())
        try {
            roomDatabase.openHelper.writableDatabase
        } finally {
            roomDatabase.close()
        }
    }
}
