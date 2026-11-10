package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class LocalBackupRepository(
    private val context: Context
) : BackupRepository {

    private val db = AppDatabase.getDatabase(context)

    private fun getEffectiveUserId(): String {
        return SessionManager.userId
            ?: com.example.data.security.AuthSessionManager(context).getActiveSession()?.userId
            ?: throw IllegalStateException("Authenticated user is required")
    }

    private fun captureAttachments(
        expenses: List<com.example.data.database.VehicleExpenseRoomEntity>
    ): List<BackupAttachment> {
        val uniqueUris = expenses.mapNotNull { it.receiptImageUri?.takeIf(String::isNotBlank) }.distinct()
        var totalBytes = 0L
        return uniqueUris.map { rawUri ->
            val uri = Uri.parse(rawUri)
            val input = if (uri.scheme == null) {
                File(rawUri).takeIf { it.isFile }?.inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            } ?: throw IllegalStateException("فایل پیوست رسید قابل خواندن نیست: $rawUri")
            val bytes = input.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var read: Int
                var size = 0
                while (stream.read(buffer).also { read = it } != -1) {
                    size += read
                    require(size <= MAX_ATTACHMENT_BYTES) { "حجم یکی از فایل‌های پیوست از حد مجاز بیشتر است." }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
            totalBytes += bytes.size
            require(totalBytes <= MAX_TOTAL_ATTACHMENT_BYTES) { "حجم کل فایل‌های پیوست برای پشتیبان‌گیری بیش از حد مجاز است." }
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            val rawName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
                ?: "receipt-$digest.bin"
            val safeName = rawName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
            BackupAttachment(
                originalUri = rawUri,
                fileName = safeName,
                mediaType = context.contentResolver.getType(uri) ?: "application/octet-stream",
                sha256 = digest,
                base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
            )
        }
    }

    companion object {
        private const val MAX_ATTACHMENT_BYTES = 20 * 1024 * 1024
        private const val MAX_TOTAL_ATTACHMENT_BYTES = 100 * 1024 * 1024
    }

    private data class RestoredAttachments(
        val uriMap: Map<String, String>,
        val createdFiles: List<File>
    )

    private fun restoreAttachments(attachments: List<BackupAttachment>): RestoredAttachments {
        if (attachments.isEmpty()) return RestoredAttachments(emptyMap(), emptyList())
        val directory = File(context.filesDir, "backup_attachments")
        check(directory.exists() || directory.mkdirs()) { "ساخت پوشه فایل‌های پیوست ناموفق بود." }
        val uriMap = linkedMapOf<String, String>()
        val createdFiles = mutableListOf<File>()
        try {
            attachments.forEach { attachment ->
                val bytes = Base64.decode(attachment.base64Data, Base64.DEFAULT)
                val extension = attachment.fileName.substringAfterLast('.', "bin")
                    .lowercase().filter { it.isLetterOrDigit() }.take(8).ifBlank { "bin" }
                val destination = File(directory, "${attachment.sha256}.$extension")
                if (!destination.exists()) {
                    val temporary = File(directory, "${destination.name}.tmp")
                    FileOutputStream(temporary).use { stream ->
                        stream.write(bytes)
                        stream.fd.sync()
                    }
                    if (!temporary.renameTo(destination)) {
                        temporary.delete()
                        throw IllegalStateException("ذخیره فایل پیوست بازیابی‌شده ناموفق بود.")
                    }
                    createdFiles.add(destination)
                }
                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.example.fileprovider",
                    destination
                ).toString()
                uriMap[attachment.originalUri] = contentUri
            }
            return RestoredAttachments(uriMap, createdFiles)
        } catch (error: Exception) {
            createdFiles.forEach { it.delete() }
            throw error
        }
    }

    private fun writeEmergencyBackup() {
        val snapshot = createBackup()
        val json = BackupFileCodec.encode(snapshot)
        val encrypted = DeviceBackupEncryption.encrypt(json.toByteArray(Charsets.UTF_8))
        val directory = File(context.filesDir, "emergency_backups")
        check(directory.exists() || directory.mkdirs()) { "ساخت پوشه پشتیبان اضطراری ناموفق بود." }
        val destination = File(directory, "before-restore-${System.currentTimeMillis()}.darino")
        val temporary = File(directory, "${destination.name}.tmp")
        FileOutputStream(temporary).use { stream ->
            stream.write(encrypted)
            stream.fd.sync()
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete()
            throw IllegalStateException("ذخیره پشتیبان اضطراری پیش از بازیابی ناموفق بود.")
        }
        directory.listFiles()?.filter { it.isFile && it.name.endsWith(".darino") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(3)
            ?.forEach { it.delete() }
    }

    /** Restores the newest device-bound emergency snapshot, retaining another snapshot first. */
    suspend fun restoreLatestEmergencyBackup() {
        val latest = withContext(Dispatchers.IO) {
            File(context.filesDir, "emergency_backups")
                .listFiles { file -> file.isFile && file.name.endsWith(".darino") }
                ?.maxByOrNull { it.lastModified() }
                ?: throw IllegalStateException("نسخه اضطراری قابل بازیابی پیدا نشد.")
        }
        val backup = withContext(Dispatchers.IO) {
            val json = DeviceBackupEncryption.decrypt(latest.readBytes()).toString(Charsets.UTF_8)
            BackupFileCodec.decode(json)
        }
        restoreBackup(backup, RestoreMode.REPLACE)
    }

    private fun remapAttachmentUris(json: String, uriMap: Map<String, String>): String =
        uriMap.entries.fold(json) { value, (oldUri, newUri) -> value.replace(oldUri, newUri) }

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

            val loanCalculations =
                db.loanCalculationDao().getAllCalculationsList()

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
                autoBackupWeeklyEnabled = backupPrefs.getBoolean("auto_backup_weekly", true)
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
                        BackupValidator.countJsonArrayItems(smsPatternsJson)

            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 8,
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
                vehicleExpenses = expenses,
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
                vehicleStore = vehicleStore,
                reminderDeliveryLogs = reminderDeliveryLogs,
                legacyReminders = legacyReminders,
                legacyReminderSchedules = legacyReminderSchedules,
                financeMetadata = financeMetadata,
                userBankSmsRulesJson = smsRulesJson,
                smsPatternsJson = smsPatternsJson,
                smsRecognitionStartMillis = smsRecognitionStart
            )
            }
            // File I/O is deliberately outside the SQLite transaction to avoid holding
            // database write locks while large receipt images are read.
            val attachments = captureAttachments(snapshot.vehicleExpenses)
            BackupValidator.withChecksum(
                snapshot.copy(
                    metadata = snapshot.metadata.copy(recordCount = snapshot.metadata.recordCount + attachments.size),
                    attachments = attachments
                )
            )
        }

    override suspend fun restoreBackup(
        backup: DarinoBackup,
        mode: RestoreMode
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
            val previousSmartSchedules = if (mode == RestoreMode.REPLACE) {
                db.smartReminderDao().getAllSchedulesList(userId)
            } else emptyList()
            val previousLegacyReminders = if (mode == RestoreMode.REPLACE) {
                db.reminderDao().getActiveRemindersSnapshot(userId)
            } else emptyList()

            if (mode == RestoreMode.REPLACE) writeEmergencyBackup()
            val restoredAttachments = restoreAttachments(backup.attachments)
            try {
                db.withTransaction {
                if (mode == RestoreMode.REPLACE) {
                    if (backup.metadata.backupVersion >= 3) {
                        db.loanCalculationDao().clearAllCalculations()
                    }
                    db.installmentDao().clearAllPayments(userId)
                    db.installmentDao().clearAllInstallments(userId)
                    if (backup.metadata.backupVersion >= 2) {
                        db.accountDao().clearAllAccounts(userId)
                        db.checkDao().clearAllChecks(userId)
                    }
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

                    if (backup.metadata.backupVersion >= 6) {
                        db.reminderDao().clearAllSchedulesForBackup(userId)
                        db.reminderDao().clearAllRemindersForBackup(userId)
                    }

                    db.financialEventDao()
                        .clearAllEvents(userId)

                    if (backup.metadata.backupVersion >= 4) {
                        db.notificationLogDao().clearAllNotificationsForBackup(userId)
                        db.smsLogDao().clearAllSmsLogsForBackup(userId)
                    }
                    if (backup.metadata.backupVersion >= 5) {
                        db.vehicleDao().clearVehicleStoreForUser(userId)
                    }
                    if (backup.metadata.backupVersion >= 3) {
                        db.financialHealthDao().clearProfilesForUser(userId)
                        db.userPreferenceDao().clearPreference(userId)
                    }
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
                    val remappedSourceId = if (transaction.sourceType == "CHECK") {
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
                            transferDestinationAccountId = remapAccountId(transaction.transferDestinationAccountId)
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
                                receiptImageUri = it.receiptImageUri?.let { oldUri ->
                                    restoredAttachments.uriMap[oldUri] ?: oldUri
                                }
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
                    val remappedSourceId = when (reminder.sourceType) {
                        "INSTALLMENT" -> reminder.sourceId
                            ?.let { restoredInstallmentReferenceIds[it] ?: it }
                        "CHECK" -> reminder.sourceId?.toIntOrNull()
                            ?.let { restoredCheckIds[it]?.toString() } ?: reminder.sourceId
                        "VEHICLE" -> reminder.sourceId?.let { restoredVehicleIds[it] ?: it }
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
                        val remappedSourceId = when (reminder.sourceType) {
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
                        db.vehicleDao().insertVehicleStoreForBackup(
                            store.copy(
                                userId = userId,
                                expensesJson = remapAttachmentUris(store.expensesJson, restoredAttachments.uriMap)
                            )
                        )
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
                    // Keep stable IDs: REPLACE clears this global table first, while MERGE
                    // skips IDs already present so importing the same file twice is idempotent.
                    val existingLoanCalculationIds = if (mode == RestoreMode.MERGE) {
                        db.loanCalculationDao().getAllCalculationsList().map { it.id }.toSet()
                    } else emptySet()
                    backup.loanCalculations.forEach { calculation ->
                        if (calculation.id !in existingLoanCalculationIds) {
                            db.loanCalculationDao().insertCalculation(calculation)
                        }
                    }
                    backup.userPreference?.let { preference ->
                        db.userPreferenceDao().insertOrUpdate(preference.copy(userId = userId))
                    }
                }
                }
            } catch (error: Exception) {
                restoredAttachments.createdFiles.forEach { it.delete() }
                throw error
            }

            if (backup.metadata.backupVersion >= 8) {
                val generalPrefs = context.getSharedPreferences("darino_general_preferences", Context.MODE_PRIVATE)
                val backupPrefs = context.getSharedPreferences("darino_backup_prefs", Context.MODE_PRIVATE)
                val userSuffix = "_$userId"
                if (mode == RestoreMode.REPLACE) {
                    generalPrefs.edit()
                        .remove("pref_persisted_budgets$userSuffix")
                        .remove("pref_persisted_savings_goals$userSuffix")
                        .remove("pref_persisted_recurring_txs$userSuffix")
                        .remove("pref_persisted_categories$userSuffix")
                        .commit()
                }
                backup.financeMetadata?.let { finance ->
                    val editor = generalPrefs.edit()
                    fun putOrRemove(key: String, value: String?) {
                        if (value == null) editor.remove(key) else editor.putString(key, value)
                    }
                    putOrRemove("pref_persisted_budgets$userSuffix", finance.budgetsJson)
                    putOrRemove("pref_persisted_savings_goals$userSuffix", finance.savingsGoalsJson)
                    putOrRemove("pref_persisted_recurring_txs$userSuffix", finance.recurringTransactionsJson)
                    putOrRemove("pref_persisted_categories$userSuffix", finance.categoriesJson)
                    check(editor.commit()) { "ذخیره تنظیمات مالی بازیابی‌شده انجام نشد." }
                }

                val smsRuleRepository = com.example.data.sms.UserBankSmsRuleRepository(context)
                val smsPatternRepository = com.example.data.sms.SmsPatternRepository(context)
                smsRuleRepository.restoreFromBackup(backup.userBankSmsRulesJson, userId)
                smsPatternRepository.restoreFromBackup(
                    backup.smsPatternsJson,
                    backup.smsRecognitionStartMillis,
                    userId
                )
            }

            backup.appSettings?.let { settings ->
                val appPreferences = AppPreferencesRepository.getInstance(context)
                runCatching { com.example.ui.screens.settings.model.AppThemeMode.valueOf(settings.themeMode) }
                    .getOrNull()?.let { appPreferences.setThemeMode(it) }
                runCatching { com.example.ui.screens.settings.model.AppLanguage.valueOf(settings.language) }
                    .getOrNull()?.let { appPreferences.setLanguage(it) }
                runCatching { com.example.ui.screens.settings.model.AppCurrency.valueOf(settings.currency) }
                    .getOrNull()?.let { appPreferences.setCurrency(it) }
                runCatching { com.example.ui.screens.settings.model.AlertDeliveryPreference.valueOf(settings.alertDelivery) }
                    .getOrNull()?.let { appPreferences.setAlertDelivery(it) }
                runCatching { com.example.ui.screens.settings.model.NavigationTransitionAnimation.valueOf(settings.navigationTransition) }
                    .getOrNull()?.let { appPreferences.setNavTransition(it) }
                settings.smsNotificationsEnabled?.let(appPreferences::setSmsNotificationsEnabled)
                settings.notificationSoundEnabled?.let(appPreferences::setNotificationSoundEnabled)
                settings.notificationVibrationEnabled?.let(appPreferences::setNotificationVibrationEnabled)
                settings.quietHoursEnabled?.let(appPreferences::setQuietHoursEnabled)
                if (settings.quietHoursStartMinute != null && settings.quietHoursEndMinute != null) {
                    appPreferences.setQuietHours(settings.quietHoursStartMinute, settings.quietHoursEndMinute)
                }
                settings.allowHighPriorityInQuietHours?.let(appPreferences::setAllowHighPriorityInQuietHours)
                settings.defaultReminderTimeMinute?.let(appPreferences::setDefaultReminderTimeMinute)
                settings.installmentReminderDays?.let { runCatching { appPreferences.setInstallmentReminderDays(it) } }
                settings.vehicleReminderKilometers?.let { runCatching { appPreferences.setVehicleReminderKilometers(it) } }
                settings.notificationsEnabled?.let(appPreferences::setNotificationsEnabled)

                settings.autoBackupDailyEnabled?.let { enabled ->
                    context.getSharedPreferences("darino_backup_prefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("auto_backup_daily", enabled).commit()
                    if (enabled) WeeklyBackupWorker.enqueueDaily(context) else WeeklyBackupWorker.cancelDaily(context)
                }
                settings.autoBackupWeeklyEnabled?.let { enabled ->
                    context.getSharedPreferences("darino_backup_prefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("auto_backup_weekly", enabled).commit()
                    if (enabled) WeeklyBackupWorker.enqueueWeekly(context) else WeeklyBackupWorker.cancelWeekly(context)
                }
            }

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
                    "Failed to reload restored data",
                    e
                )
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

            preferences
                .getLong("last_backup_time", 0L)
                .takeIf { it != 0L }
        }

}