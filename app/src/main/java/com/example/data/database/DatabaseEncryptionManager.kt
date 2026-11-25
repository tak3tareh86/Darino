package com.example.data.database

import android.content.Context
import android.os.StatFs
import android.system.Os
import android.system.OsConstants
import android.util.Base64
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
    private const val STATE_NEW_DATABASE = "NEW_DATABASE_PENDING"
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
        migrationMutex.withLock {
            val appContext = context.applicationContext
            System.loadLibrary("sqlcipher")
            val databaseFile = appContext.getDatabasePath(DATABASE_NAME)
            val temporaryFile = File(databaseFile.parentFile, DATABASE_NAME + ".encrypted.tmp")
            val plaintextBackup = File(databaseFile.parentFile, DATABASE_NAME + ".plaintext.bak")
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val state = prefs.getString(PREF_STATE, null)
            if (!databaseFile.exists() && !plaintextBackup.exists() &&
                state != null && state != STATE_NEW_DATABASE
            ) {
                throw IllegalStateException("دیتابیس ثبت‌شده پیدا نشد؛ برای جلوگیری از ساخت دیتابیس خالی، برنامه متوقف شد.")
            }
            val key = loadOrCreateDatabaseKey(appContext, databaseFile, temporaryFile, plaintextBackup)
            if (databaseFile.exists() && hasPlaintextSQLiteHeader(databaseFile) && state == STATE_NEW_DATABASE) {
                throw IllegalStateException("وضعیت پایگاه داده جدید با فایل قدیمی سازگار نیست؛ هیچ فایلی تغییر نکرد.")
            }

            if (databaseFile.exists() && !hasPlaintextSQLiteHeader(databaseFile)) {
                if (state == STATE_READY && !plaintextBackup.exists()) {
                    verifyEncryptedDatabaseFast(databaseFile, key)
                    return@withLock
                }
                validateEncryptedDatabase(appContext, databaseFile, key)
                validateWithRoom(appContext, databaseFile, key)
                // A plaintext copy is retained until this first successful encrypted Room open.
                if (plaintextBackup.exists() && !plaintextBackup.delete()) {
                    throw IllegalStateException("نسخه موقت بدون رمزگذاری پس از اعتبارسنجی قابل حذف نیست.")
                }
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
                validateEncryptedDatabase(appContext, databaseFile, key)
                check(prefs.edit().putString(PREF_STATE, STATE_READY).commit()) {
                    "ثبت وضعیت دیتابیس جدید ناموفق بود."
                }
                return@withLock
            }

            val sourceFile = if (databaseFile.exists()) databaseFile else plaintextBackup
            if (!hasPlaintextSQLiteHeader(sourceFile)) {
                throw IllegalStateException("وضعیت فایل دیتابیس با وضعیت مهاجرت سازگار نیست؛ هیچ فایلی تغییر نکرد.")
            }

            if (state == STATE_READY || state == STATE_INSTALLED) {
                throw IllegalStateException("وضعیت رمزگذاری با محتوای فایل دیتابیس همخوانی ندارد؛ برای جلوگیری از بازگشت ناامن، دیتابیس باز نشد.")
            }

            checkpointPlaintextDatabase(sourceFile)
            checkAvailableSpace(sourceFile)

            check(prefs.edit().putString(PREF_STATE, STATE_MIGRATING).commit()) {
                "ثبت وضعیت شروع مهاجرت ناموفق بود."
            }

            if (temporaryFile.exists() && !temporaryFile.delete()) {
                throw IllegalStateException("فایل موقت مهاجرت قبلی قابل پاک‌سازی نیست؛ دیتابیس اصلی حفظ شد.")
            }

            val sourceSnapshot: DatabaseSnapshot
            val sourceDb = CipherDatabase.openDatabase(
                sourceFile.absolutePath,
                null as ByteArray?,
                null,
                CipherDatabase.OPEN_READWRITE,
                null,
                null
            )
            try {
                assertIntegrity(sourceDb, "دیتابیس اصلی")
                assertNoForeignKeyViolations(sourceDb, "دیتابیس اصلی")
                sourceSnapshot = snapshot(sourceDb)

                val escapedPath = temporaryFile.absolutePath.replace("'", "''")
                sourceDb.execSQL(
                    "ATTACH DATABASE '" + escapedPath + "' AS encrypted KEY '" +
                        String(key, StandardCharsets.UTF_8) + "'"
                )
                try {
                    sourceDb.rawQuery("SELECT sqlcipher_export('encrypted')", null).use { cursor ->
                        check(cursor.moveToFirst()) { "انتقال اطلاعات به دیتابیس رمزگذاری‌شده ناموفق بود." }
                    }
                    assertRowsEquivalent(sourceDb)
                    sourceDb.execSQL("PRAGMA encrypted.user_version = " + sourceDb.version)
                } finally {
                    sourceDb.execSQL("DETACH DATABASE encrypted")
                }
            } finally {
                sourceDb.close()
            }

            syncFile(temporaryFile)
            validateEncryptedDatabase(appContext, temporaryFile, key)
            val encryptedSnapshot = openCipherSnapshot(temporaryFile, key)
            check(sourceSnapshot == encryptedSnapshot) {
                "ساختار یا تعداد رکوردهای دیتابیس رمزگذاری‌شده با منبع یکسان نیست؛ نسخه اصلی حفظ شد."
            }
            validateWithRoom(appContext, temporaryFile, key)
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
            if (plaintextBackup.exists() && !plaintextBackup.delete()) {
                throw IllegalStateException("دیتابیس رمزگذاری شد اما نسخه بدون رمزگذاری قابل حذف نیست؛ برنامه برای حفاظت از داده‌ها متوقف شد.")
            }
            syncDirectory(databaseFile.parentFile ?: appContext.filesDir)
            check(prefs.edit().putString(PREF_STATE, STATE_READY).commit()) {
                "ثبت وضعیت نهایی رمزگذاری دیتابیس ناموفق بود."
            }
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
            temporaryFile.exists() || plaintextBackup.exists()
        ) {
            throw IllegalStateException("کلید رمزگذاری دیتابیس مفقود است؛ هیچ دیتابیسی حذف یا بازسازی نشد.")
        }

        val initialState = if (databaseFile.exists() || plaintextBackup.exists()) {
            STATE_KEY_READY
        } else {
            STATE_NEW_DATABASE
        }
        val randomKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val passphrase = randomKey.joinToString("") { "%02x".format(it) }
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

    private fun checkAvailableSpace(file: File) {
        val stat = StatFs(file.parentFile?.absolutePath ?: file.absolutePath)
        val required = Math.addExact(Math.multiplyExact(file.length(), 2L), MIN_FREE_SPACE_BYTES)
        check(stat.availableBytes >= required) {
            "فضای خالی برای رمزگذاری امن دیتابیس کافی نیست؛ تمام اطلاعات حفظ شدند و امکان تلاش مجدد پس از آزادسازی فضا وجود دارد."
        }
    }

    /**
     * Normal launches verify that the persisted key still opens the encrypted file,
     * without running a full integrity scan over a potentially large database each time.
     */
    private fun verifyEncryptedDatabaseFast(file: File, key: ByteArray) {
        val database = CipherDatabase.openDatabase(
            file.absolutePath,
            key,
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

    private fun validateEncryptedDatabase(context: Context, file: File, key: ByteArray) {
        check(file.isFile && file.length() > 0L) { "فایل دیتابیس رمزگذاری‌شده ایجاد نشده است." }
        val database = CipherDatabase.openDatabase(
            file.absolutePath,
            key,
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
            assertIntegrity(database, "دیتابیس رمزگذاری‌شده")
            assertNoForeignKeyViolations(database, "دیتابیس رمزگذاری‌شده")
        } finally {
            database.close()
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
            val quoted = "\"" + table.replace("\"", "\"\"") + "\""
            val sourceOnly = "SELECT * FROM main." + quoted +
                " EXCEPT SELECT * FROM encrypted." + quoted + " LIMIT 1"
            val targetOnly = "SELECT * FROM encrypted." + quoted +
                " EXCEPT SELECT * FROM main." + quoted + " LIMIT 1"
            database.rawQuery(sourceOnly, null).use { cursor ->
                check(!cursor.moveToFirst()) {
                    "محتوای جدول " + table + " در نسخه رمزگذاری‌شده با منبع متفاوت است."
                }
            }
            database.rawQuery(targetOnly, null).use { cursor ->
                check(!cursor.moveToFirst()) {
                    "محتوای جدول " + table + " در نسخه اصلی با نسخه رمزگذاری‌شده متفاوت است."
                }
            }
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
            key,
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
        val roomDatabase = AppDatabase.buildEncrypted(context, file.absolutePath, key)
        try {
            roomDatabase.openHelper.writableDatabase
        } finally {
            roomDatabase.close()
        }
    }
}
