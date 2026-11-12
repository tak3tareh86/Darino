package com.example.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class LocalBackupRepository(
    private val context: Context
) : BackupRepository {

    private val db = AppDatabase.getDatabase(context)

    private fun getEffectiveUserId(): String {
        return SessionManager.userId
            ?: com.example.data.security.AuthSessionManager(context).getActiveSession()?.userId
            ?: throw IllegalStateException("Authenticated user is required")
    }

    companion object {
        private const val TAG = "LocalBackupRepository"
        private val restoreRecoveryMutex = kotlinx.coroutines.sync.Mutex()
        private const val MAX_BACKUP_JSON_BYTES = 64L * 1024 * 1024
        // Matches the portable backup payload ceiling, with room for device-encryption framing.
        private const val MAX_LOCAL_BACKUP_FILE_BYTES = MAX_BACKUP_JSON_BYTES + 1024L
    }

    /** Reads device-local backups with both a preflight and streaming size limit. */
    private fun readBoundedLocalBackup(file: File): ByteArray {
        require(file.isFile) { "فایل پشتیبان محلی معتبر نیست." }
        val initialLength = file.length()
        require(initialLength in 1..MAX_LOCAL_BACKUP_FILE_BYTES) {
            "حجم فایل پشتیبان محلی خالی یا بیشتر از حد مجاز است."
        }
        val output = ByteArrayOutputStream(initialLength.toInt())
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            var totalBytes = 0L
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                totalBytes += count
                require(totalBytes <= MAX_LOCAL_BACKUP_FILE_BYTES) {
                    "حجم فایل پشتیبان محلی از حد مجاز بیشتر است."
                }
                output.write(buffer, 0, count)
            }
        }
        require(output.size() > 0) { "فایل پشتیبان محلی خالی است." }
        return output.toByteArray()
    }

    private suspend fun writeEmergencyBackup() {
        val snapshot = createBackup()
        val json = BackupFileCodec.encode(snapshot)
        val jsonPayload = json.toByteArray(Charsets.UTF_8)
        require(jsonPayload.size.toLong() <= MAX_BACKUP_JSON_BYTES) {
            "حجم داده‌های پشتیبان اضطراری از حد مجاز بیشتر است."
        }
        val encrypted = DeviceBackupEncryption.encrypt(jsonPayload, context)
        require(encrypted.size.toLong() <= MAX_LOCAL_BACKUP_FILE_BYTES) {
            "حجم فایل پشتیبان اضطراری رمزگذاری‌شده از حد مجاز بیشتر است."
        }
        val userTag = BackupUserScope.tag(getEffectiveUserId())
        val directory = File(context.filesDir, "emergency_backups")
        check(directory.exists() || directory.mkdirs()) { "ساخت پوشه پشتیبان اضطراری ناموفق بود." }
        val destination = File(directory, "before-restore-$userTag-${System.currentTimeMillis()}.darino")
        val temporary = File(directory, "${destination.name}.tmp")
        FileOutputStream(temporary).use { stream ->
            stream.write(encrypted)
            stream.fd.sync()
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete()
            throw IllegalStateException("ذخیره پشتیبان اضطراری پیش از بازیابی ناموفق بود.")
        }
        directory.listFiles()?.filter {
            it.isFile && it.name.startsWith("before-restore-$userTag-") && it.name.endsWith(".darino")
        }?.sortedByDescending { it.name }?.drop(3)?.forEach { it.delete() }
    }

    /** Restores the newest daily or weekly device-bound automatic backup for the active user. */
    suspend fun restoreLatestAutomaticBackup() {
        val userTag = BackupUserScope.tag(getEffectiveUserId())
        val backup = withContext(Dispatchers.IO) {
            val selectedFolder = BackupFolderStorage.selectedFolderUri(context)
            val encryptedPayload = if (selectedFolder != null) {
                val selectedFolderBackup = BackupFolderStorage.latestBackup(
                    context,
                    listOf("daily_auto_backup_${userTag}_", "weekly_auto_backup_${userTag}_")
                ) ?: throw IllegalStateException("در پوشه انتخاب‌شده نسخه پشتیبان خودکار برای این حساب پیدا نشد.")
                context.contentResolver.openInputStream(selectedFolderBackup.uri)?.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_LOCAL_BACKUP_FILE_BYTES) {
                            "حجم فایل پشتیبان خودکار از حد مجاز بیشتر است."
                        }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                } ?: throw java.io.IOException("خواندن فایل پشتیبان از پوشه انتخاب‌شده ناموفق بود.")
            } else {
                // Backward compatibility only for installations that have not selected a folder yet.
                val legacyFile = File(context.filesDir, "auto_backups")
                    .listFiles { file ->
                        file.isFile &&
                            (file.name.startsWith("daily_auto_backup_${userTag}_") ||
                                file.name.startsWith("weekly_auto_backup_${userTag}_")) &&
                            file.extension == "darino"
                    }
                    ?.maxByOrNull { it.name.substringAfter("${userTag}_") }
                    ?: throw IllegalStateException("نسخه پشتیبان خودکار برای این حساب پیدا نشد.")
                readBoundedLocalBackup(legacyFile)
            }
            val json = DeviceBackupEncryption.decrypt(encryptedPayload).toString(Charsets.UTF_8)
            BackupFileCodec.decode(json)
        }
        restoreBackup(backup, RestoreMode.REPLACE)
    }

    /** Restores the newest device-bound emergency snapshot, retaining another snapshot first. */
    suspend fun restoreLatestEmergencyBackup() {
        val userTag = BackupUserScope.tag(getEffectiveUserId())
        val latest = withContext(Dispatchers.IO) {
            File(context.filesDir, "emergency_backups")
                .listFiles { file ->
                    file.isFile && file.name.startsWith("before-restore-$userTag-") && file.name.endsWith(".darino")
                }
                ?.maxByOrNull { it.name }
                ?: throw IllegalStateException("نسخه اضطراری قابل بازیابی پیدا نشد.")
        }
        val backup = withContext(Dispatchers.IO) {
            val json = DeviceBackupEncryption.decrypt(readBoundedLocalBackup(latest)).toString(Charsets.UTF_8)
            BackupFileCodec.decode(json)
        }
        restoreBackup(backup, RestoreMode.REPLACE)
    }

    private fun restoreSharedPreferencesSnapshot(
        preferences: SharedPreferences,
        snapshot: Map<String, *>
    ) {
        val editor = preferences.edit().clear()
        snapshot.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toMutableSet())
            }
        }
        check(editor.commit()) { "SharedPreferences rollback failed" }
    }

    /**
     * Keeps legacy-only vehicle JSON records while dropping only records already restored
     * from structured Room tables. Legacy receipt URIs are intentionally discarded:
     * Darino no longer supports file or image attachments.
     */
    private fun remapLegacyVehicleCollection(
        rawJson: String,
        structuredIds: Set<String>,
        vehicleIdMap: Map<String, String>
    ): String {
        if (rawJson.isBlank()) return "[]"
        return runCatching {
            val source = org.json.JSONArray(rawJson)
            val result = org.json.JSONArray()
            for (index in 0 until source.length()) {
                val original = source.optJSONObject(index) ?: continue
                val oldId = original.optString("id").takeIf { it.isNotBlank() && it != "null" }
                if (oldId != null && oldId in structuredIds) continue
                val item = org.json.JSONObject(original.toString())
                if (item.has("vehicleId") && !item.isNull("vehicleId")) {
                    val oldVehicleId = item.optString("vehicleId")
                    item.put("vehicleId", vehicleIdMap[oldVehicleId] ?: oldVehicleId)
                }
                item.remove("receiptImageUri")
                result.put(item)
            }
            result.toString()
        }.getOrDefault(rawJson)
    }

    override suspend fun createBackup(): DarinoBackup =
        withContext(Dispatchers.IO) {
            val userId = getEffectiveUserId()

            // Keep all table reads in one SQLite snapshot so related records cannot drift mid-backup.
            val snapshot = db.withTransaction {
            val accounts = db.accountDao().getAllAccountsRaw(userId)

            val checks = db.checkDao().getAllChecksList(userId)

            val transactions =
                db.transactionDao().getAllTransactionsForBackup(userId)

            val installments =
                db.installmentDao().getAllInstallmentsList(userId)

            val payments =
                db.installmentDao().getAllPaymentsList(userId)

            val vehicles =
                db.vehicleDao().getAllVehiclesForBackup(userId)

            val services =
                db.vehicleDao().getAllServicesForBackup(userId)

            val expenses =
                db.vehicleDao().getAllExpensesList(userId)

            val insurances =
                db.vehicleDao().getAllInsurancesList(userId)

            val inspections =
                db.vehicleDao().getAllInspectionsList(userId)

            val reminders =
                db.smartReminderDao().getAllRemindersList(userId)

            val schedules =
                db.smartReminderDao().getAllSchedulesList(userId)

            val legacyReminders = db.reminderDao().getAllRemindersForBackup(userId)
            val legacyReminderSchedules = db.reminderDao().getAllSchedulesForBackup(userId)

            val events =
                db.financialEventDao().getAllEventsList(userId)

            val financialHealthProfiles =
                db.financialHealthDao().getAllProfilesForUser(userId)

            db.loanCalculationDao().claimLegacyCalculations(userId)
            val loanCalculations =
                db.loanCalculationDao().getAllCalculationsList(userId)

            val userPreference =
                db.userPreferenceDao().getPreference(userId)
            val notificationLogs = db.notificationLogDao().getAllNotificationsForBackup(userId)
            val smsLogs = db.smsLogDao().getAllSmsLogsForBackup(userId)
            val vehicleStore = db.vehicleDao().getVehicleStore(userId)
            val reminderDeliveryLogs = db.smartReminderDao().getAllDeliveryLogsForBackup(userId)
            val appPreferences = AppPreferencesRepository.getInstance(context)
            val generalPrefs = context.getSharedPreferences("darino_general_preferences", Context.MODE_PRIVATE)
            val backupPrefs = context.getSharedPreferences("darino_backup_prefs", Context.MODE_PRIVATE)
            val userSuffix = "_$userId"
            val appSettings = AppSettingsBackup(
                themeMode = appPreferences.themeMode.value.name,
                language = appPreferences.language.value.name,
                currency = appPreferences.currency.value.name,
                alertDelivery = appPreferences.alertDelivery.value.name,
                navigationTransition = appPreferences.navTransition.value.name,
                notificationsEnabled = generalPrefs.getBoolean("pref_notifications_enabled", false),
                smsNotificationsEnabled = generalPrefs.getBoolean("pref_sms_notifications_enabled", false),
                notificationSoundEnabled = generalPrefs.getBoolean("pref_notification_sound_enabled", false),
                notificationVibrationEnabled = generalPrefs.getBoolean("pref_notification_vibration_enabled", false),
                quietHoursEnabled = generalPrefs.getBoolean("pref_quiet_hours_enabled", false),
                quietHoursStartMinute = generalPrefs.getInt("pref_quiet_hours_start_minute", 23 * 60),
                quietHoursEndMinute = generalPrefs.getInt("pref_quiet_hours_end_minute", 7 * 60),
                allowHighPriorityInQuietHours = generalPrefs.getBoolean("pref_allow_high_priority_quiet_hours", false),
                defaultReminderTimeMinute = generalPrefs.getInt("pref_default_reminder_time_minute", 9 * 60),
                installmentReminderDays = generalPrefs.getInt("pref_installment_reminder_days", 0),
                vehicleReminderKilometers = generalPrefs.getInt("pref_vehicle_reminder_kilometers", 0),
                autoBackupDailyEnabled = backupPrefs.getBoolean("auto_backup_daily", false),
                autoBackupWeeklyEnabled = backupPrefs.getBoolean("auto_backup_weekly", false),
                cleanSlatePerformed = appPreferences.isCleanSlatePerformed()
            )
            val financeMetadata = FinanceMetadataBackup(
                budgetsJson = generalPrefs.getString("pref_persisted_budgets$userSuffix", null),
                savingsGoalsJson = generalPrefs.getString("pref_persisted_savings_goals$userSuffix", null),
                recurringTransactionsJson = generalPrefs.getString("pref_persisted_recurring_txs$userSuffix", null),
                categoriesJson = generalPrefs.getString("pref_persisted_categories$userSuffix", null)
            )
            val smsRuleRepository = com.example.data.sms.UserBankSmsRuleRepository(context)
            val smsPatternRepository = com.example.data.sms.SmsPatternRepository(context)
            val smsRulesJson = smsRuleRepository.exportForBackup(userId)
            val smsPatternsJson = smsPatternRepository.exportForBackup(userId)
            val smsRecognitionStart = smsPatternRepository.getStoredRecognitionStartMillis(userId)
            val bankSmsState = com.example.data.sms.BankSmsRepository.getInstance(context).exportStateForBackup(userId)

            val totalRecords =
                accounts.size + checks.size + transactions.size +
                        installments.size +
                        payments.size +
                        vehicles.size +
                        services.size +
                        expenses.size +
                        insurances.size +
                        inspections.size +
                        reminders.size +
                        schedules.size +
                        events.size +
                        financialHealthProfiles.size +
                        loanCalculations.size + notificationLogs.size + smsLogs.size + reminderDeliveryLogs.size +
                        legacyReminders.size + legacyReminderSchedules.size +
                        (if (vehicleStore != null) 1 else 0) + (if (userPreference != null) 1 else 0) + 1 +
                        BackupValidator.countJsonArrayItems(financeMetadata.budgetsJson) +
                        BackupValidator.countJsonArrayItems(financeMetadata.savingsGoalsJson) +
                        BackupValidator.countJsonArrayItems(financeMetadata.recurringTransactionsJson) +
                        BackupValidator.countJsonArrayItems(financeMetadata.categoriesJson) +
                        BackupValidator.countJsonArrayItems(smsRulesJson) +
                        BackupValidator.countJsonArrayItems(smsPatternsJson) +
                        BackupValidator.countJsonArrayItems(bankSmsState.pendingSuggestionsJson)

            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 9,
                    appVersion = "1.0",
                    createdAt = System.currentTimeMillis(),
                    recordCount = totalRecords,
                    checksum = "",
                    currencyUnit = "PER_RECORD",
                    dateSystem = "JALALI_UI"
                ),
                accounts = accounts,
                checks = checks,
                transactions = transactions,
                installments = installments,
                installmentPayments = payments,
                vehicles = vehicles,
                vehicleServices = services,
                vehicleExpenses = expenses.map { it.copy(receiptImageUri = null) },
                vehicleInsurances = insurances,
                vehicleInspections = inspections,
                reminders = reminders,
                reminderSchedules = schedules,
                financialEvents = events,
                financialHealthProfiles = financialHealthProfiles,
                loanCalculations = loanCalculations,
                userPreference = userPreference,
                appSettings = appSettings,
                notificationLogs = notificationLogs,
                smsLogs = smsLogs,
                vehicleStore = vehicleStore?.copy(expensesJson = sanitizeLegacyExpenseJson(vehicleStore.expensesJson).orEmpty()),
                reminderDeliveryLogs = reminderDeliveryLogs,
                legacyReminders = legacyReminders,
                legacyReminderSchedules = legacyReminderSchedules,
                financeMetadata = financeMetadata,
                userBankSmsRulesJson = smsRulesJson,
                smsPatternsJson = smsPatternsJson,
                smsRecognitionStartMillis = smsRecognitionStart,
                bankSmsState = bankSmsState
            )
            }
            // File I/O is deliberately outside the SQLite transaction to avoid holding
            // database write locks while large receipt images are read.
            // New backups contain data records only; legacy receipt URI fields are excluded.
            BackupValidator.withChecksum(snapshot)
        }

    private fun sanitizeLegacyExpenseJson(rawJson: String?): String? {
        if (rawJson.isNullOrBlank()) return rawJson
        return runCatching {
            val expenses = org.json.JSONArray(rawJson)
            for (index in 0 until expenses.length()) {
                expenses.optJSONObject(index)?.remove("receiptImageUri")
            }
            expenses.toString()
        }.getOrDefault(rawJson)
    }

    private fun restoreJournalFile(userId: String): File =
        File(File(context.filesDir, "restore_recovery").apply { mkdirs() },
            "restore-${BackupUserScope.tag(userId)}.darino")

    /** Persist the intended restore payload before changing any non-transactional store. */
    private fun persistRestoreJournal(backup: DarinoBackup, userId: String) {
        val json = BackupFileCodec.encode(backup).toByteArray(Charsets.UTF_8)
        require(json.size.toLong() <= 64L * 1024 * 1024) {
            "حجم داده‌های بازیابی از حد مجاز بیشتر است."
        }
        val encrypted = DeviceBackupEncryption.encrypt(json, context)
        require(encrypted.size.toLong() <= MAX_LOCAL_BACKUP_FILE_BYTES) {
            "حجم فایل بازیابی موقت از حد مجاز بیشتر است."
        }
        val destination = restoreJournalFile(userId)
        val directory = destination.parentFile!!
        check(directory.exists() || directory.mkdirs()) { "ساخت پوشه بازیابی موقت ناموفق بود." }
        val temporary = File(directory, "${destination.name}.tmp")
        FileOutputStream(temporary).use { stream ->
            stream.write(encrypted)
            stream.fd.sync()
        }
        try {
            android.system.Os.rename(temporary.absolutePath, destination.absolutePath)
        } catch (error: Exception) {
            temporary.delete()
            throw IllegalStateException("ثبت نشانگر بازیابی امن ناموفق بود.", error)
        }
    }

    /**
     * Replays an interrupted replacement from its encrypted, durable target snapshot.
     * The operation is idempotent because REPLACE clears the active user's restored rows first.
     */
    suspend fun recoverInterruptedRestore() = withContext(Dispatchers.IO) {
        restoreRecoveryMutex.withLock {
            val userId = SessionManager.userId
                ?: com.example.data.security.AuthSessionManager(context).getActiveSession()?.userId
            if (userId.isNullOrBlank()) return@withLock
            val journal = restoreJournalFile(userId)
            if (!journal.isFile) return@withLock
            Log.w("LocalBackupRepository", "Recovering an interrupted backup restore for the active user.")
            // The emergency snapshot is the pre-restore source of old OS alarm IDs.
            // Cancel those IDs explicitly: after a crash following the Room commit, the
            // restored database no longer contains the old schedules needed to cancel them.
            val emergencyDirectory = File(context.filesDir, "emergency_backups")
            val emergencyFile = emergencyDirectory.listFiles { file ->
                file.isFile && file.name.startsWith("before-restore-${BackupUserScope.tag(userId)}-") &&
                    file.extension == "darino"
            }?.maxByOrNull { it.name }
                ?: throw IllegalStateException("نسخه اضطراری لازم برای بازیابی زمان‌بندی‌های قبلی پیدا نشد.")
            val previousJson = DeviceBackupEncryption.decrypt(readBoundedLocalBackup(emergencyFile))
                .toString(Charsets.UTF_8)
            val previousBackup = BackupFileCodec.decode(previousJson)
            val oldSmartScheduler = com.example.reminder.domain.ReminderScheduler(context)
            previousBackup.reminderSchedules.forEach { oldSmartScheduler.cancelSchedule(it.id) }
            val oldLegacyScheduler = com.example.data.receiver.ReminderScheduler(context)
            previousBackup.legacyReminders.forEach { oldLegacyScheduler.cancel(it.id) }
            val oldCalendarScheduler = com.example.calendar.reminder.CalendarEventReminderScheduler(context)
            previousBackup.financialEvents.forEach { oldCalendarScheduler.cancel(it.id) }

            val json = DeviceBackupEncryption.decrypt(readBoundedLocalBackup(journal)).toString(Charsets.UTF_8)
            val backup = BackupFileCodec.decode(json)
            restoreBackupInternal(backup, RestoreMode.REPLACE, recovering = true)
        }
    }

    override suspend fun restoreBackup(
        backup: DarinoBackup,
        mode: RestoreMode
    ) = restoreRecoveryMutex.withLock {
        restoreBackupInternal(backup, mode, recovering = false)
    }

    private suspend fun restoreBackupInternal(
        backup: DarinoBackup,
        mode: RestoreMode,
        recovering: Boolean
    ) {
        withContext(Dispatchers.IO) {
            when (val validation = BackupValidator.validate(backup)) {
                is ValidationResult.Valid -> Unit
                is ValidationResult.Invalid -> throw IllegalArgumentException(validation.reason)
            }
            require(mode == RestoreMode.REPLACE) {
                "ادغام پشتیبان تا زمان پیاده‌سازی تطبیق امن شناسه‌ها در دسترس نیست؛ برای جلوگیری از ایجاد رکوردهای تکراری، فقط جایگزینی کامل مجاز است."
            }
            val userId = getEffectiveUserId()
            if (!recovering) {
                check(!restoreJournalFile(userId).isFile) {
                    "بازیابی قبلی هنوز کامل نشده است؛ ابتدا برنامه را دوباره باز کنید تا فرایند بازیابی ادامه پیدا کند."
                }
            }
            val previousSmartSchedules = if (mode == RestoreMode.REPLACE) {
                db.smartReminderDao().getAllSchedulesList(userId)
            } else emptyList()
            val previousLegacyReminders = if (mode == RestoreMode.REPLACE) {
                db.reminderDao().getActiveRemindersSnapshot(userId)
            } else emptyList()
            val previousFinancialEvents = if (mode == RestoreMode.REPLACE) {
                db.financialEventDao().getAllEventsList(userId)
            } else emptyList()

            if (mode == RestoreMode.REPLACE && !recovering) {
                writeEmergencyBackup()
                // Durable encrypted redo record for stores that cannot share a transaction.
                persistRestoreJournal(backup, userId)
            }
            val generalPrefs = context.getSharedPreferences("darino_general_preferences", Context.MODE_PRIVATE)
            val backupPrefs = context.getSharedPreferences("darino_backup_prefs", Context.MODE_PRIVATE)
            val generalPrefsSnapshot = generalPrefs.all
            val backupPrefsSnapshot = backupPrefs.all
            val smsRuleRepository = com.example.data.sms.UserBankSmsRuleRepository(context)
            val smsPatternRepository = com.example.data.sms.SmsPatternRepository(context)
            val previousSmsRules = smsRuleRepository.exportForBackup(userId)
            val previousSmsPatterns = smsPatternRepository.exportForBackup(userId)
            val previousSmsRecognitionStart = smsPatternRepository.getStoredRecognitionStartMillis(userId)
            val bankSmsRepository = com.example.data.sms.BankSmsRepository.getInstance(context)
            val previousBankSmsState = bankSmsRepository.exportStateForBackup(userId)
            try {
                backup.appSettings?.let { settings ->
                    val editor = generalPrefs.edit()
                        .putString("pref_theme_mode", settings.themeMode)
                        .putString("pref_language", settings.language)
                        .putString("pref_currency", settings.currency)
                        .putString("pref_alert_delivery", settings.alertDelivery)
                        .putString("pref_nav_transition_animation", settings.navigationTransition)
                    fun putBooleanOrRemove(key: String, value: Boolean?) {
                        if (value == null) editor.remove(key) else editor.putBoolean(key, value)
                    }
                    fun putIntOrRemove(key: String, value: Int?) {
                        if (value == null) editor.remove(key) else editor.putInt(key, value)
                    }
                    putBooleanOrRemove("pref_notifications_enabled", settings.notificationsEnabled)
                    putBooleanOrRemove("pref_sms_notifications_enabled", settings.smsNotificationsEnabled)
                    putBooleanOrRemove("pref_notification_sound_enabled", settings.notificationSoundEnabled)
                    putBooleanOrRemove("pref_notification_vibration_enabled", settings.notificationVibrationEnabled)
                    putBooleanOrRemove("pref_quiet_hours_enabled", settings.quietHoursEnabled)
                    putIntOrRemove("pref_quiet_hours_start_minute", settings.quietHoursStartMinute)
                    putIntOrRemove("pref_quiet_hours_end_minute", settings.quietHoursEndMinute)
                    putBooleanOrRemove("pref_allow_high_priority_quiet_hours", settings.allowHighPriorityInQuietHours)
                    putIntOrRemove("pref_default_reminder_time_minute", settings.defaultReminderTimeMinute)
                    putIntOrRemove("pref_installment_reminder_days", settings.installmentReminderDays)
                    putIntOrRemove("pref_vehicle_reminder_kilometers", settings.vehicleReminderKilometers)
                    putBooleanOrRemove("pref_is_clean_slate", settings.cleanSlatePerformed)
                    check(editor.commit()) { "ذخیره تنظیمات برنامه بازیابی‌شده انجام نشد." }

                    val dailyEnabled = settings.autoBackupDailyEnabled ?: false
                    val weeklyEnabled = settings.autoBackupWeeklyEnabled ?: false
                    check(
                        backupPrefs.edit()
                            .putBoolean("auto_backup_daily", dailyEnabled)
                            .putBoolean("auto_backup_weekly", weeklyEnabled)
                            .commit()
                    ) { "ذخیره تنظیمات پشتیبان‌گیری بازیابی‌شده ناموفق بود." }
                    // WorkManager is reconciled only after the Room replacement commits.
                } ?: run {
                    // Legacy backup formats did not contain app preferences. Reset these
                    // to application defaults instead of leaking settings from the newer state.
                    check(
                        generalPrefs.edit()
                            .putString("pref_theme_mode", "SYSTEM")
                            .putString("pref_language", "PERSIAN")
                            .putString("pref_currency", "TOMAN")
                            .putString("pref_alert_delivery", "NONE")
                            .putString("pref_nav_transition_animation", "DYNAMIC")
                            .putBoolean("pref_notifications_enabled", false)
                            .putBoolean("pref_sms_notifications_enabled", false)
                            .putBoolean("pref_notification_sound_enabled", false)
                            .putBoolean("pref_notification_vibration_enabled", false)
                            .putBoolean("pref_quiet_hours_enabled", false)
                            .putInt("pref_quiet_hours_start_minute", 23 * 60)
                            .putInt("pref_quiet_hours_end_minute", 7 * 60)
                            .putBoolean("pref_allow_high_priority_quiet_hours", false)
                            .putInt("pref_default_reminder_time_minute", 9 * 60)
                            .putInt("pref_installment_reminder_days", 0)
                            .putInt("pref_vehicle_reminder_kilometers", 0)
                            .putBoolean("pref_is_clean_slate", false)
                            .commit()
                    ) { "بازنشانی تنظیمات عمومی ناموفق بود." }
                    check(
                        backupPrefs.edit()
                            .putBoolean("auto_backup_daily", false)
                            .putBoolean("auto_backup_weekly", false)
                            .commit()
                    ) { "بازنشانی تنظیمات پشتیبان‌گیری ناموفق بود." }
                    // Keep existing scheduled work untouched until the restore transaction commits.
                }

                val userSuffix = "_$userId"
                val financeEditor = generalPrefs.edit()
                if (backup.metadata.backupVersion >= 8) {
                    val finance = backup.financeMetadata
                        ?: throw IllegalArgumentException("اطلاعات مالی سفارشی در فایل پشتیبان وجود ندارد.")
                    fun putOrRemove(key: String, value: String?) {
                        if (value == null) financeEditor.remove(key) else financeEditor.putString(key, value)
                    }
                    putOrRemove("pref_persisted_budgets$userSuffix", finance.budgetsJson)
                    putOrRemove("pref_persisted_savings_goals$userSuffix", finance.savingsGoalsJson)
                    putOrRemove("pref_persisted_recurring_txs$userSuffix", finance.recurringTransactionsJson)
                    putOrRemove("pref_persisted_categories$userSuffix", finance.categoriesJson)
                } else {
                    financeEditor.remove("pref_persisted_budgets$userSuffix")
                        .remove("pref_persisted_savings_goals$userSuffix")
                        .remove("pref_persisted_recurring_txs$userSuffix")
                        .remove("pref_persisted_categories$userSuffix")
                }
                check(financeEditor.commit()) { "ذخیره تنظیمات مالی بازیابی‌شده انجام نشد." }
                smsRuleRepository.restoreFromBackup(
                    if (backup.metadata.backupVersion >= 8) backup.userBankSmsRulesJson else null,
                    userId
                )
                smsPatternRepository.restoreFromBackup(
                    if (backup.metadata.backupVersion >= 8) backup.smsPatternsJson else null,
                    if (backup.metadata.backupVersion >= 8) backup.smsRecognitionStartMillis else null,
                    userId
                )
                bankSmsRepository.restoreStateFromBackup(
                    if (backup.metadata.backupVersion >= 9) {
                        backup.bankSmsState
                            ?: throw IllegalArgumentException("سوابق پیامک بانکی در فایل پشتیبان وجود ندارد.")
                    } else {
                        BankSmsStateBackup()
                    },
                    userId
                )

                db.withTransaction {
                if (mode == RestoreMode.REPLACE) {
                    // REPLACE means every user-owned table is cleared first. A field
                    // absent from an older backup is treated as empty, never as "keep current".
                    db.loanCalculationDao().clearAllCalculations(userId)
                    db.installmentDao().clearAllPayments(userId)
                    db.installmentDao().clearAllInstallments(userId)
                    db.accountDao().clearAllAccounts(userId)
                    db.checkDao().clearAllChecks(userId)
                    db.transactionDao()
                        .clearAllTransactions(userId)

                    db.vehicleDao()
                        .clearAllInspections(userId)

                    db.vehicleDao()
                        .clearAllInsurances(userId)

                    db.vehicleDao()
                        .clearAllExpenses(userId)

                    db.vehicleDao()
                        .clearAllServices(userId)

                    db.vehicleDao()
                        .clearAllVehicles(userId)

                    db.smartReminderDao().clearAllSchedules(userId)
                    // Delivery logs are child history for smart reminders. Clear them on
                    // every replacement, including restores from older backup formats.
                    db.smartReminderDao().clearAllDeliveryLogsForBackup(userId)

                    db.smartReminderDao()
                        .clearAllReminders(userId)

                    db.reminderDao().clearAllSchedulesForBackup(userId)
                    db.reminderDao().clearAllRemindersForBackup(userId)

                    db.financialEventDao()
                        .clearAllEvents(userId)

                    db.notificationLogDao().clearAllNotificationsForBackup(userId)
                    db.smsLogDao().clearAllSmsLogsForBackup(userId)
                    db.vehicleDao().clearVehicleStoreForUser(userId)
                    db.financialHealthDao().clearProfilesForUser(userId)
                    db.userPreferenceDao().clearPreference(userId)
                }

                // Re-create auto-generated primary keys instead of risking collisions with another
                // local profile. Keep stable string IDs for cross-record references where available.
                val restoredAccountIds = mutableMapOf<String, String>()
                backup.accounts.forEach { account ->
                    val newId = db.accountDao().insertAccountForBackup(
                        account.copy(id = 0, userId = userId)
                    ).toString()
                    restoredAccountIds[account.id.toString()] = newId
                    account.stringId.takeIf { it.isNotBlank() }?.let { restoredAccountIds[it] = newId }
                }
                fun remapAccountId(oldId: String?): String? =
                    oldId?.let { restoredAccountIds[it] ?: it }

                val restoredCheckIds = mutableMapOf<Int, Int>()
                backup.checks.forEach { check ->
                    // Insert without old database IDs in relationship fields. Those IDs
                    // belong to the source installation and may collide after restore.
                    val newId = db.checkDao().insertCheck(
                        check.copy(
                            id = 0,
                            userId = userId,
                            accountId = remapAccountId(check.accountId),
                            replacedByCheckId = null,
                            replacesCheckId = null
                        )
                    ).toInt()
                    restoredCheckIds[check.id] = newId
                }
                // Reconnect both directions only after every new check ID is known.
                backup.checks.forEach { check ->
                    val restoredId = restoredCheckIds[check.id] ?: return@forEach
                    db.checkDao().updateCheck(
                        check.copy(
                            id = restoredId,
                            userId = userId,
                            accountId = remapAccountId(check.accountId),
                            replacedByCheckId = check.replacedByCheckId?.let { restoredCheckIds[it] },
                            replacesCheckId = check.replacesCheckId?.let { restoredCheckIds[it] }
                        )
                    )
                }

                backup.transactions.forEach { transaction ->
                    val remappedSourceId = if (transaction.sourceType.equals("CHECK", ignoreCase = true)) {
                        transaction.sourceId?.toIntOrNull()?.let { restoredCheckIds[it]?.toString() }
                            ?: transaction.sourceId
                    } else transaction.sourceId
                    db.transactionDao().insertTransaction(
                        transaction.copy(
                            id = 0,
                            userId = userId,
                            sourceId = remappedSourceId,
                            accountId = remapAccountId(transaction.accountId),
                            transferSourceAccountId = remapAccountId(transaction.transferSourceAccountId),
                            transferDestinationAccountId = remapAccountId(transaction.transferDestinationAccountId),
                            transferRecipientName = transaction.transferRecipientName
                        )
                    )
                }

                val restoredInstallmentIds = mutableMapOf<Int, Int>()
                val restoredInstallmentReferenceIds = mutableMapOf<String, String>()
                backup.installments.forEach { installment ->
                    val newId = db.installmentDao().insertInstallment(
                        installment.copy(id = 0, userId = userId)
                    ).toInt()
                    restoredInstallmentIds[installment.id] = newId
                    restoredInstallmentReferenceIds[installment.id.toString()] = newId.toString()
                    installment.serverId?.takeIf { it.isNotBlank() }?.let {
                        restoredInstallmentReferenceIds[it] = newId.toString()
                    }
                }

                backup.installmentPayments.forEach { payment ->
                    val newInstallmentId = restoredInstallmentIds[payment.installmentId]
                        ?: throw IllegalStateException("پرداخت قسط به قسط موجود در پشتیبان متصل نیست")
                    db.installmentDao().insertPayment(
                        payment.copy(id = 0, installmentId = newInstallmentId)
                    )
                }

                val restoredVehicleIds = mutableMapOf<String, String>()
                backup.vehicles.forEach { vehicle ->
                    val newId = db.vehicleDao().insertVehicle(
                        vehicle.copy(id = 0, userId = userId)
                    ).toInt()
                    restoredVehicleIds[vehicle.id.toString()] = newId.toString()
                    vehicle.serverId?.takeIf { it.isNotBlank() }?.let { serverId ->
                        restoredVehicleIds[serverId] = newId.toString()
                    }
                }
                fun remapVehicleId(oldId: String): String = restoredVehicleIds[oldId] ?: oldId

                val restoredServiceIds = mutableMapOf<String, String>()
                backup.vehicleServices.forEach { service ->
                    val newId = db.vehicleDao().insertService(
                        service.copy(id = 0, userId = userId, vehicleId = remapVehicleId(service.vehicleId))
                    ).toString()
                    restoredServiceIds[service.id.toString()] = newId
                    service.serverId?.takeIf { it.isNotBlank() }?.let { restoredServiceIds[it] = newId }
                }

                if (backup.vehicleExpenses.isNotEmpty()) {
                    db.vehicleDao().insertExpensesForBackup(
                        backup.vehicleExpenses.map {
                            it.copy(
                                userId = userId,
                                vehicleId = remapVehicleId(it.vehicleId),
                                receiptImageUri = null
                            )
                        }
                    )
                }

                if (backup.vehicleInsurances.isNotEmpty()) {
                    db.vehicleDao().insertInsurancesForBackup(
                        backup.vehicleInsurances.map {
                            it.copy(userId = userId, vehicleId = remapVehicleId(it.vehicleId))
                        }
                    )
                }

                if (backup.vehicleInspections.isNotEmpty()) {
                    db.vehicleDao().insertInspectionsForBackup(
                        backup.vehicleInspections.map {
                            it.copy(userId = userId, vehicleId = remapVehicleId(it.vehicleId))
                        }
                    )
                }

                // Smart reminder IDs are globally unique primary keys, not user-scoped.
                // Generate fresh IDs and remap every child reference so a backup can be
                // restored on the same device, another account, or repeatedly without collisions.
                val restoredSmartReminderIds = mutableMapOf<String, String>()
                backup.reminders.forEach { reminder ->
                    val newReminderId = java.util.UUID.randomUUID().toString()
                    restoredSmartReminderIds[reminder.id] = newReminderId
                    val remappedSourceId = when (reminder.sourceType?.uppercase()) {
                        "INSTALLMENT" -> reminder.sourceId
                            ?.let { restoredInstallmentReferenceIds[it] ?: it }
                        "CHECK" -> reminder.sourceId?.toIntOrNull()
                            ?.let { restoredCheckIds[it]?.toString() } ?: reminder.sourceId
                        "VEHICLE" -> reminder.sourceId?.let { restoredServiceIds[it] ?: restoredVehicleIds[it] ?: it }
                        "VEHICLE_SERVICE" -> reminder.sourceId?.let { restoredServiceIds[it] ?: it }
                        else -> reminder.sourceId
                    }
                    db.smartReminderDao().insertReminderForBackup(
                        reminder.copy(id = newReminderId, userId = userId, sourceId = remappedSourceId)
                    )
                }

                val restoredSmartScheduleIds = mutableMapOf<String, String>()
                backup.reminderSchedules.forEach { schedule ->
                    val newReminderId = restoredSmartReminderIds[schedule.reminderId]
                        ?: throw IllegalStateException("زمان‌بندی یادآور به یادآور موجود در فایل پشتیبان متصل نیست")
                    val newScheduleId = java.util.UUID.randomUUID().toString()
                    restoredSmartScheduleIds[schedule.id] = newScheduleId
                    db.smartReminderDao().insertScheduleForBackup(
                        schedule.copy(id = newScheduleId, reminderId = newReminderId)
                    )
                }

                val restoredLegacyReminderIds = mutableMapOf<Int, Int>()
                if (backup.metadata.backupVersion >= 6) {
                    backup.legacyReminders.forEach { reminder ->
                        val remappedSourceId = when (reminder.sourceType?.uppercase()) {
                            "INSTALLMENT" -> reminder.sourceId?.let { restoredInstallmentIds[it] ?: it }
                            "VEHICLE" -> reminder.sourceId?.let {
                                restoredVehicleIds[it.toString()]?.toIntOrNull() ?: it
                            }
                            "VEHICLE_SERVICE" -> reminder.sourceId?.let {
                                restoredServiceIds[it.toString()]?.toIntOrNull() ?: it
                            }
                            else -> reminder.sourceId
                        }
                        val newId = db.reminderDao().insertReminder(
                            reminder.copy(id = 0, userId = userId, sourceId = remappedSourceId)
                        ).toInt()
                        restoredLegacyReminderIds[reminder.id] = newId
                    }
                    backup.legacyReminderSchedules.forEach { schedule ->
                        val newReminderId = restoredLegacyReminderIds[schedule.reminderId]
                            ?: throw IllegalStateException("زمان‌بندی یادآور قدیمی به یادآور موجود متصل نیست")
                        db.reminderDao().insertSchedule(
                            schedule.copy(id = 0, reminderId = newReminderId)
                        )
                    }
                }

                backup.financialEvents.forEach { event ->
                    db.financialEventDao().insertEvent(
                        event.copy(userId = userId)
                    )
                }

                if (backup.metadata.backupVersion >= 5) {
                    backup.vehicleStore?.let { store ->
                        val restoredStore = store.copy(
                            userId = userId,
                            vehiclesJson = remapLegacyVehicleCollection(
                                store.vehiclesJson,
                                backup.vehicles.flatMap { listOf(it.id.toString(), it.serverId.orEmpty()) }.toSet(),
                                restoredVehicleIds
                            ),
                            servicesJson = remapLegacyVehicleCollection(
                                store.servicesJson,
                                backup.vehicleServices.flatMap { listOf(it.id.toString(), it.serverId.orEmpty()) }.toSet(),
                                restoredVehicleIds
                            ),
                            expensesJson = remapLegacyVehicleCollection(
                                store.expensesJson,
                                backup.vehicleExpenses.map { it.id }.toSet(),
                                restoredVehicleIds
                            ),
                            insurancesJson = remapLegacyVehicleCollection(
                                store.insurancesJson,
                                backup.vehicleInsurances.map { it.id }.toSet(),
                                restoredVehicleIds
                            ),
                            inspectionsJson = remapLegacyVehicleCollection(
                                store.inspectionsJson,
                                backup.vehicleInspections.map { it.id }.toSet(),
                                restoredVehicleIds
                            )
                        )
                        db.vehicleDao().insertVehicleStoreForBackup(restoredStore)
                    }
                    backup.reminderDeliveryLogs.forEach { log ->
                        val newReminderId = restoredSmartReminderIds[log.reminderId]
                            ?: throw IllegalStateException("سوابق ارسال به یادآور موجود در فایل پشتیبان متصل نیست")
                        // Historical delivery rows may refer to schedules that were
                        // intentionally deleted after delivery; keep those logs with no schedule link.
                        val newScheduleId = log.scheduleId?.let { oldScheduleId ->
                            restoredSmartScheduleIds[oldScheduleId]
                        }
                        db.smartReminderDao().insertDeliveryLogForBackup(
                            log.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                reminderId = newReminderId,
                                scheduleId = newScheduleId
                            )
                        )
                    }
                }

                if (backup.metadata.backupVersion >= 4) {
                    backup.notificationLogs.forEach { log ->
                        db.notificationLogDao().insertNotification(
                            log.copy(
                                id = 0,
                                userId = userId,
                                reminderId = log.reminderId?.let { restoredLegacyReminderIds[it] ?: it }
                            )
                        )
                    }
                    backup.smsLogs.forEach { log ->
                        db.smsLogDao().insertSmsLog(
                            log.copy(
                                id = 0,
                                userId = userId,
                                reminderId = log.reminderId?.let { restoredLegacyReminderIds[it] ?: it }
                            )
                        )
                    }
                }

                if (backup.metadata.backupVersion >= 3) {
                    backup.financialHealthProfiles.forEach { profile ->
                        db.financialHealthDao().saveProfile(profile.copy(userId = userId))
                    }
                    // loan_calculations uses a globally unique primary key rather than
                    // a composite (id, userId) key. Allocate fresh IDs on restore so importing
                    // another profile's backup can never REPLACE a different user's row.
                    backup.loanCalculations.forEach { calculation ->
                        db.loanCalculationDao().insertCalculation(
                            calculation.copy(
                                id = "calc_${java.util.UUID.randomUUID()}",
                                userId = userId
                            )
                        )
                    }
                    backup.userPreference?.let { preference ->
                        db.userPreferenceDao().insertOrUpdate(preference.copy(userId = userId))
                    }
                }
                }
            } catch (error: Exception) {
                runCatching { restoreSharedPreferencesSnapshot(generalPrefs, generalPrefsSnapshot) }
                    .exceptionOrNull()?.let(error::addSuppressed)
                runCatching { restoreSharedPreferencesSnapshot(backupPrefs, backupPrefsSnapshot) }
                    .exceptionOrNull()?.let(error::addSuppressed)
                runCatching { bankSmsRepository.restoreStateFromBackup(previousBankSmsState, userId) }
                    .exceptionOrNull()?.let(error::addSuppressed)
                runCatching { smsRuleRepository.restoreFromBackup(previousSmsRules, userId) }
                    .exceptionOrNull()?.let(error::addSuppressed)
                runCatching {
                    smsPatternRepository.restoreFromBackup(
                        previousSmsPatterns,
                        previousSmsRecognitionStart,
                        userId
                    )
                }.exceptionOrNull()?.let(error::addSuppressed)
                throw error
            }

            backup.appSettings?.let { settings ->
                val appPreferences = AppPreferencesRepository.getInstance(context)
                appPreferences.setThemeMode(com.example.ui.screens.settings.model.AppThemeMode.valueOf(settings.themeMode))
                appPreferences.setLanguage(com.example.ui.screens.settings.model.AppLanguage.valueOf(settings.language))
                appPreferences.setCurrency(com.example.ui.screens.settings.model.AppCurrency.valueOf(settings.currency))
                appPreferences.setAlertDelivery(com.example.ui.screens.settings.model.AlertDeliveryPreference.valueOf(settings.alertDelivery))
                appPreferences.setNavTransition(com.example.ui.screens.settings.model.NavigationTransitionAnimation.valueOf(settings.navigationTransition))
                settings.smsNotificationsEnabled?.let(appPreferences::setSmsNotificationsEnabled)
                settings.notificationSoundEnabled?.let(appPreferences::setNotificationSoundEnabled)
                settings.notificationVibrationEnabled?.let(appPreferences::setNotificationVibrationEnabled)
                settings.quietHoursEnabled?.let(appPreferences::setQuietHoursEnabled)
                if (settings.quietHoursStartMinute != null && settings.quietHoursEndMinute != null) {
                    appPreferences.setQuietHours(settings.quietHoursStartMinute, settings.quietHoursEndMinute)
                }
                settings.allowHighPriorityInQuietHours?.let(appPreferences::setAllowHighPriorityInQuietHours)
                settings.defaultReminderTimeMinute?.let(appPreferences::setDefaultReminderTimeMinute)
                settings.installmentReminderDays?.let(appPreferences::setInstallmentReminderDays)
                settings.vehicleReminderKilometers?.let(appPreferences::setVehicleReminderKilometers)
                settings.notificationsEnabled?.let(appPreferences::setNotificationsEnabled)
                settings.cleanSlatePerformed?.let(appPreferences::setCleanSlatePerformed)

                // Automatic workers are reconciled once, below, after settings and Room commit.
            } ?: run {
                val appPreferences = AppPreferencesRepository.getInstance(context)
                appPreferences.setThemeMode(com.example.ui.screens.settings.model.AppThemeMode.SYSTEM)
                appPreferences.setLanguage(com.example.ui.screens.settings.model.AppLanguage.PERSIAN)
                appPreferences.setCurrency(com.example.ui.screens.settings.model.AppCurrency.TOMAN)
                appPreferences.setAlertDelivery(com.example.ui.screens.settings.model.AlertDeliveryPreference.NONE)
                appPreferences.setNavTransition(com.example.ui.screens.settings.model.NavigationTransitionAnimation.DYNAMIC)
                appPreferences.setNotificationsEnabled(false)
                appPreferences.setSmsNotificationsEnabled(false)
                appPreferences.setNotificationSoundEnabled(false)
                appPreferences.setNotificationVibrationEnabled(false)
                appPreferences.setQuietHoursEnabled(false)
                appPreferences.setQuietHours(23 * 60, 7 * 60)
                appPreferences.setAllowHighPriorityInQuietHours(false)
                appPreferences.setDefaultReminderTimeMinute(9 * 60)
                appPreferences.setInstallmentReminderDays(0)
                appPreferences.setVehicleReminderKilometers(0)
                appPreferences.setCleanSlatePerformed(false)
            }

            // WorkManager state is device-local and cannot be rolled back with SharedPreferences.
            // Reconcile it only after the Room transaction and preference writes have committed.
            WeeklyBackupWorker.syncAutomaticBackups(context)

            // AlarmManager is outside the Room transaction: cancel old registrations only
            // after the database replacement commits, then rebuild from the restored rows.
            if (mode == RestoreMode.REPLACE) {
                val reminderScheduler = com.example.reminder.domain.ReminderScheduler(context)
                previousSmartSchedules.forEach { reminderScheduler.cancelSchedule(it.id) }
                val legacyScheduler = com.example.data.receiver.ReminderScheduler(context)
                previousLegacyReminders.forEach { legacyScheduler.cancel(it.id) }
            }
            com.example.reminder.domain.ReminderManager(context).rebuildAllSchedules()
            db.reminderDao().getActiveRemindersSnapshot(userId).forEach { reminder ->
                com.example.data.receiver.ReminderScheduler(context).schedule(reminder)
            }

            // Calendar-event alarms are OS registrations, not Room data. Rebuild them
            // after replacement so removed events cannot leave stale alarms behind.
            val calendarReminderScheduler = com.example.calendar.reminder.CalendarEventReminderScheduler(context)
            previousFinancialEvents.forEach { calendarReminderScheduler.cancel(it.id) }
            db.financialEventDao().getAllEventsList(userId).forEach { entity ->
                calendarReminderScheduler.schedule(entity.toDomain())
            }

            try {
                com.example.vehicle.data.VehicleRepository
                    .instance
                    .reloadFromDatabase(context)

                com.example.ui.screens.installments.data
                    .LocalInstallmentRepository
                    .instance
                    .reloadFromDatabase(context)
                com.example.ui.screens.finance.data.LocalFinanceRepository
                    .instance
                    .refreshMetadataForCurrentUser()
                com.example.ui.screens.finance.worker.RecurringTransactionWorker.enqueuePeriodic(context)
                com.example.ui.screens.finance.worker.RecurringTransactionWorker.enqueueOneTime(context)
            } catch (e: Exception) {
                Log.e(
                    "LocalBackupRepository",
                    "Failed to reload restored data; keeping the restore journal for startup replay",
                    e
                )
                throw e
            }
            if (mode == RestoreMode.REPLACE) {
                // Keep the journal until preferences, Room, repositories,
                // and OS schedulers have all been reconciled.
                val journal = restoreJournalFile(userId)
                check(!journal.exists() || journal.delete()) {
                    "پاک‌سازی نشانگر بازیابی پس از تکمیل عملیات ناموفق بود."
                }
            }
        }
    }

    override suspend fun exportCsvTransactions(): String =
        withContext(Dispatchers.IO) {
            val userId = getEffectiveUserId()

            val transactions =
                db.transactionDao()
                    .getAllTransactionsList(userId)

            buildString {
                appendLine(
                    "Time,Type,Category,AccountName,Amount,AmountUnit,Description"
                )

                transactions.forEach { transaction ->
                    appendLine(
                        "\"${transaction.timeFormatted}\"," +
                                "${transaction.type}," +
                                "\"${transaction.category}\"," +
                                "\"${transaction.accountName}\"," +
                                "${transaction.amount}," +
                                "${transaction.amountUnit}," +
                                "\"${transaction.description}\""
                    )
                }
            }
        }

    override suspend fun exportCsvInstallments(): String =
        withContext(Dispatchers.IO) {
            val userId = getEffectiveUserId()

            val installments =
                db.installmentDao()
                    .getAllInstallmentsList(userId)

            buildString {
                appendLine(
                    "Title,Provider,Amount,AmountUnit,TotalInstallments," +
                            "RemainingInstallments,NextDueDate,Status"
                )

                installments.forEach { installment ->
                    appendLine(
                        "\"${installment.title}\"," +
                                "\"${installment.providerName}\"," +
                                "${installment.amount}," +
                                "${installment.amountUnit}," +
                                "${installment.totalInstallments}," +
                                "${installment.remainingInstallments}," +
                                "${installment.nextDueDate}," +
                                "\"${installment.status}\""
                    )
                }
            }
        }

    override suspend fun exportCsvVehicleExpenses(): String =
        withContext(Dispatchers.IO) {
            val userId = getEffectiveUserId()

            val services =
                db.vehicleDao()
                    .getAllServicesList(userId)

            buildString {
                appendLine(
                    "VehicleId,Title,Type,DueDate,DueMileage,Status,Notes"
                )

                services.forEach { service ->
                    appendLine(
                        "${service.vehicleId}," +
                                "\"${service.title}\"," +
                                "\"${service.type}\"," +
                                "${service.dueDate ?: 0}," +
                                "${service.dueMileage ?: 0}," +
                                "\"${service.status}\"," +
                                "\"${service.notes ?: ""}\""
                    )
                }
            }
        }

    override suspend fun getLastBackupTime(): Long? =
        withContext(Dispatchers.IO) {
            val preferences = context.getSharedPreferences(
                "darino_backup_prefs",
                Context.MODE_PRIVATE
            )

            val userTag = BackupUserScope.tag(getEffectiveUserId())
            preferences.getLong("last_backup_time_$userTag", 0L)
                .takeIf { it != 0L }
        }

}