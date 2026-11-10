package com.example.data.backup

import com.example.calendar.data.FinancialEventEntity
import com.example.data.database.InstallmentEntity
import com.example.data.database.AccountEntity
import com.example.data.database.CheckEntity
import com.example.data.database.InstallmentPaymentEntity
import com.example.data.database.TransactionEntity
import com.example.data.database.VehicleEntity
import com.example.data.database.VehicleExpenseRoomEntity
import com.example.data.database.VehicleInspectionRoomEntity
import com.example.data.database.VehicleInsuranceRoomEntity
import com.example.data.database.VehicleServiceEntity
import com.example.reminder.data.ReminderEntity
import com.example.reminder.data.ReminderScheduleEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.MessageDigest
import okio.ByteString

object BackupValidator {

    private val moshi: Moshi by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }
    private val jsonArrayAdapter by lazy {
        moshi.adapter<List<Any>>(Types.newParameterizedType(List::class.java, Any::class.java))
    }

    /** Returns -1 for malformed JSON; null represents an intentionally empty collection. */
    fun countJsonArrayItems(rawJson: String?): Int {
        if (rawJson == null) return 0
        return runCatching { jsonArrayAdapter.fromJson(rawJson)?.size ?: -1 }.getOrDefault(-1)
    }

    fun isJsonObjectArray(rawJson: String?, requiredKeys: Set<String> = emptySet()): Boolean {
        if (rawJson.isNullOrBlank()) return true
        val values = runCatching { jsonArrayAdapter.fromJson(rawJson) }.getOrNull() ?: return false
        return values.all { value ->
            value is Map<*, *> && requiredKeys.all { it in value.keys }
        }
    }

    private fun jsonArrayStringValues(rawJson: String?, key: String): List<String>? {
        if (rawJson.isNullOrBlank()) return emptyList()
        val values = runCatching { jsonArrayAdapter.fromJson(rawJson) }.getOrNull() ?: return null
        if (values.any { it !is Map<*, *> }) return null
        return values.mapNotNull { (it as Map<*, *>)[key] as? String }
            .filter(String::isNotBlank)
    }

    fun calculateChecksum(content: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun withChecksum(backup: DarinoBackup): DarinoBackup {
        val unsignedBackup = backup.copy(metadata = backup.metadata.copy(checksum = ""))
        val checksum = when (unsignedBackup.metadata.backupVersion) {
            1 -> legacyV1Checksum(unsignedBackup)
            2 -> legacyV2Checksum(unsignedBackup)
            3 -> legacyV3Checksum(unsignedBackup)
            4 -> legacyV4Checksum(unsignedBackup)
            5 -> legacyV5Checksum(unsignedBackup)
            6, 7 -> legacyV6Checksum(unsignedBackup)
            8 -> legacyV8Checksum(unsignedBackup)
            else -> calculateChecksum(moshi.adapter(DarinoBackup::class.java).toJson(unsignedBackup))
        }
        return unsignedBackup.copy(metadata = unsignedBackup.metadata.copy(checksum = checksum))
    }

    fun validate(backup: DarinoBackup, rawJson: String? = null): ValidationResult {
        val version = backup.metadata.backupVersion
        if (version !in 1..9 {
            return ValidationResult.Invalid("نسخه فایل پشتیبان پشتیبانی نمی‌شود.")
        }
        if (backup.metadata.createdAt <= 0L || backup.metadata.recordCount < 0) {
            return ValidationResult.Invalid("اطلاعات فراداده فایل پشتیبان نامعتبر است.")
        }
        if (version >= 8 && (backup.appSettings == null || backup.financeMetadata == null)) {
            return ValidationResult.Invalid("فایل پشتیبان کامل نیست و بخشی از تنظیمات اصلی را ندارد.")
        }
        val validCurrencyMetadata = when (version) {
            in 1..6 -> backup.metadata.currencyUnit == "TOMAN"
            in 7..8 -> backup.metadata.currencyUnit == "PER_RECORD"
            else -> false
        }
        if (!validCurrencyMetadata) {
            return ValidationResult.Invalid("فراداده واحد پول فایل پشتیبان معتبر نیست.")
        }

        // Reject unknown per-record units before restore reaches Room. Silently
        // falling back to Toman here would change the monetary value of a record.
        val validUnits = setOf("TOMAN", "RIAL")
        val hasInvalidCurrencyUnit =
            backup.accounts.any { it.initialBalanceUnit !in validUnits } ||
            backup.checks.any { it.amountUnit !in validUnits } ||
            backup.transactions.any { it.amountUnit !in validUnits } ||
            backup.installments.any { it.amountUnit !in validUnits } ||
            backup.installmentPayments.any { it.amountUnit !in validUnits } ||
            backup.vehicles.any { it.estimatedValueUnit !in validUnits } ||
            backup.vehicleServices.any { it.costUnit !in validUnits } ||
            backup.vehicleExpenses.any { it.amountUnit !in validUnits } ||
            backup.vehicleInsurances.any { it.amountUnit !in validUnits } ||
            backup.vehicleInspections.any { it.costUnit !in validUnits } ||
            backup.reminders.any { it.amountUnit !in validUnits } ||
            backup.financialEvents.any { it.amountUnit !in validUnits } ||
            backup.financialHealthProfiles.any { it.amountUnit !in validUnits } ||
            backup.loanCalculations.any { it.amountUnit !in validUnits } ||
            (backup.appSettings?.currency?.let { it !in validUnits } ?: false)
        if (hasInvalidCurrencyUnit) {
            return ValidationResult.Invalid("فایل پشتیبان شامل رکوردی با واحد پول ناشناخته است؛ برای جلوگیری از تغییر مبلغ، بازیابی متوقف شد.")
        }
        val settings = backup.appSettings
        val invalidAppSettings = settings != null && (
            com.example.ui.screens.settings.model.AppThemeMode.values().none { it.name == settings.themeMode } ||
                com.example.ui.screens.settings.model.AppLanguage.values().none { it.name == settings.language } ||
                settings.currency !in validUnits ||
                com.example.ui.screens.settings.model.AlertDeliveryPreference.values().none { it.name == settings.alertDelivery } ||
                com.example.ui.screens.settings.model.NavigationTransitionAnimation.values().none { it.name == settings.navigationTransition } ||
                (settings.quietHoursStartMinute != null && settings.quietHoursStartMinute !in 0..1439) ||
                (settings.quietHoursEndMinute != null && settings.quietHoursEndMinute !in 0..1439) ||
                (settings.defaultReminderTimeMinute != null && settings.defaultReminderTimeMinute !in 0..1439) ||
                (settings.installmentReminderDays != null && settings.installmentReminderDays !in setOf(0, 1, 3, 7, 10)) ||
                (settings.vehicleReminderKilometers != null && settings.vehicleReminderKilometers !in setOf(0, 200, 500, 1000))
        )
        if (invalidAppSettings) {
            return ValidationResult.Invalid("تنظیمات برنامه در فایل پشتیبان معتبر نیستند.")
        }
        if (backup.metadata.checksum.isBlank()) {
            return ValidationResult.Invalid("فایل پشتیبان امضای صحت‌سنجی ندارد یا ناقص است.")
        }

        val expectedCount = if (version == 1) {
            backup.transactions.size + backup.installments.size +
                backup.installmentPayments.size + backup.vehicles.size +
                backup.vehicleServices.size + backup.vehicleExpenses.size +
                backup.vehicleInsurances.size + backup.vehicleInspections.size +
                backup.reminders.size + backup.reminderSchedules.size +
                backup.financialEvents.size
        } else {
            backup.accounts.size + backup.checks.size +
                backup.transactions.size + backup.installments.size +
                backup.installmentPayments.size + backup.vehicles.size +
                backup.vehicleServices.size + backup.vehicleExpenses.size +
                backup.vehicleInsurances.size + backup.vehicleInspections.size +
                backup.reminders.size + backup.reminderSchedules.size +
                backup.financialEvents.size +
                (if (version >= 3) backup.financialHealthProfiles.size +
                    backup.loanCalculations.size + (if (backup.userPreference != null) 1 else 0) +
                    (if (backup.appSettings != null) 1 else 0) else 0) +
                (if (version >= 4) backup.notificationLogs.size + backup.smsLogs.size else 0) +
                (if (version >= 5) backup.reminderDeliveryLogs.size + (if (backup.vehicleStore != null) 1 else 0) else 0) +
                (if (version >= 6) backup.legacyReminders.size + backup.legacyReminderSchedules.size else 0)
        }
        val configurationRecordCounts = if (version >= 8) {
            listOf(
                backup.financeMetadata?.budgetsJson,
                backup.financeMetadata?.savingsGoalsJson,
                backup.financeMetadata?.recurringTransactionsJson,
                backup.financeMetadata?.categoriesJson,
                backup.userBankSmsRulesJson,
                backup.smsPatternsJson
            ).map(::countJsonArrayItems)
        } else emptyList()
        val bankSmsState = backup.bankSmsState
        val pendingBankSmsCount = if (version >= 9) {
            countJsonArrayItems(bankSmsState?.pendingSuggestionsJson)
        } else 0
        val bankSmsStateRecordCount = if (version >= 9 && bankSmsState != null) {
            bankSmsState.processedSmsIds.size + bankSmsState.dismissedSmsIds.size + pendingBankSmsCount
        } else 0
        val bankSmsStateValid = version < 9 || (
            bankSmsState != null &&
                bankSmsState.processedSmsIds.none(String::isBlank) &&
                bankSmsState.processedSmsIds.size == bankSmsState.processedSmsIds.toSet().size &&
                bankSmsState.dismissedSmsIds.none(String::isBlank) &&
                bankSmsState.dismissedSmsIds.size == bankSmsState.dismissedSmsIds.toSet().size &&
                pendingBankSmsCount in 0..50 &&
                isJsonObjectArray(
                    bankSmsState.pendingSuggestionsJson,
                    setOf("id", "patternId", "bankName", "amount", "amountUnit", "smsText", "receivedAtMillis")
                ) &&
                (jsonArrayStringValues(bankSmsState.pendingSuggestionsJson, "id")?.let {
                    it.size == pendingBankSmsCount && it.size == it.toSet().size
                } ?: false)
        )
        val finance = backup.financeMetadata
        val customCollectionsValid = version < 8 || (
            isJsonObjectArray(finance?.budgetsJson, setOf("id", "title", "amount")) &&
                isJsonObjectArray(finance?.savingsGoalsJson, setOf("id", "title", "targetAmount", "currentAmount")) &&
                isJsonObjectArray(finance?.recurringTransactionsJson, setOf("id", "title", "amount", "type", "frequency", "startDate", "nextExecutionDate")) &&
                isJsonObjectArray(finance?.categoriesJson, setOf("id", "title", "iconEmoji", "type")) &&
                isJsonObjectArray(backup.userBankSmsRulesJson, setOf("bankName")) &&
                isJsonObjectArray(backup.smsPatternsJson, setOf("name", "type", "sampleMessage")) &&
                isJsonObjectArray(backup.vehicleStore?.vehiclesJson) &&
                isJsonObjectArray(backup.vehicleStore?.servicesJson) &&
                isJsonObjectArray(backup.vehicleStore?.expensesJson) &&
                isJsonObjectArray(backup.vehicleStore?.insurancesJson) &&
                isJsonObjectArray(backup.vehicleStore?.inspectionsJson)
        )
        if (configurationRecordCounts.any { it < 0 } || pendingBankSmsCount < 0 ||
            !customCollectionsValid || !bankSmsStateValid ||
            (backup.smsRecognitionStartMillis != null && backup.smsRecognitionStartMillis <= 0L)
        ) {
            return ValidationResult.Invalid("تنظیمات سفارشی فایل پشتیبان ساختار معتبر ندارند.")
        }
        val extraConfigurationRecords = configurationRecordCounts.sum() + bankSmsStateRecordCount
        val attachmentUris = mutableSetOf<String>()
        var totalAttachmentBytes = 0L
        for (attachment in backup.attachments) {
            if (version < 8 || attachment.originalUri.isBlank() ||
                attachment.fileName.isBlank() || attachment.mediaType.isBlank() ||
                !attachment.sha256.matches(Regex("[a-fA-F0-9]{64}")) ||
                !attachmentUris.add(attachment.originalUri)
            ) {
                return ValidationResult.Invalid("اطلاعات یکی از فایل‌های پیوست معتبر نیست.")
            }
            val bytes = ByteString.decodeBase64(attachment.base64Data)?.toByteArray()
                ?: return ValidationResult.Invalid("کدگذاری یکی از فایل‌های پیوست معتبر نیست.")
            if (bytes.isEmpty() || bytes.size > 10 * 1024 * 1024) {
                return ValidationResult.Invalid("حجم یکی از فایل‌های پیوست معتبر نیست.")
            }
            totalAttachmentBytes += bytes.size
            if (totalAttachmentBytes > 30L * 1024 * 1024) {
                return ValidationResult.Invalid("حجم کل فایل‌های پیوست بیش از حد مجاز است.")
            }
            val actualHash = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            if (!actualHash.equals(attachment.sha256, ignoreCase = true)) {
                return ValidationResult.Invalid("صحت یکی از فایل‌های پیوست تأیید نشد.")
            }
        }
        val expectedAttachmentReferences = backup.vehicleExpenses
            .mapNotNull { it.receiptImageUri?.takeIf(String::isNotBlank) } +
            (if (version >= 8) {
                jsonArrayStringValues(backup.vehicleStore?.expensesJson, "receiptImageUri")
                    ?: return ValidationResult.Invalid("ساختار پیوست‌های خودرو در فایل پشتیبان معتبر نیست.")
            } else emptyList())
        if (version >= 8 && expectedAttachmentReferences.any { it !in attachmentUris }) {
            return ValidationResult.Invalid("یکی از تصاویر رسید در فایل پشتیبان وجود ندارد.")
        }
        if (backup.metadata.recordCount != expectedCount + extraConfigurationRecords + backup.attachments.size) {
            return ValidationResult.Invalid("تعداد رکوردهای فایل با اطلاعات ثبت‌شده در سربرگ آن مطابقت ندارد.")
        }

        val expectedChecksum = when (version) {
            1 -> legacyV1Checksum(backup)
            2 -> legacyV2Checksum(backup)
            3 -> legacyV3Checksum(backup)
            4 -> legacyV4Checksum(backup)
            5 -> legacyV5Checksum(backup)
            6, 7 -> legacyV6Checksum(backup)
            8 -> legacyV8Checksum(backup)
            else -> withChecksum(backup).metadata.checksum
        }
        if (!backup.metadata.checksum.equals(expectedChecksum, ignoreCase = true)) {
            return ValidationResult.Invalid("صحت فایل پشتیبان تأیید نشد؛ فایل ممکن است ناقص یا تغییر داده شده باشد.")
        }

        // Basic relationship checks prevent malformed files from reaching database writes.
        val installmentIds = backup.installments.map { it.id }.toSet()
        if (backup.installmentPayments.any { it.installmentId !in installmentIds }) {
            return ValidationResult.Invalid("فایل پشتیبان دارای پرداخت قسط بدون قسط مرتبط است.")
        }

        val loanCalculationIds = backup.loanCalculations.map { it.id }
        if (loanCalculationIds.size != loanCalculationIds.toSet().size) {
            return ValidationResult.Invalid("فایل پشتیبان دارای محاسبات وام تکراری است.")
        }
        val smartReminderIds = backup.reminders.map { it.id }.toSet()
        if (backup.reminderDeliveryLogs.any { it.reminderId !in smartReminderIds }) {
            return ValidationResult.Invalid("سوابق ارسال به یادآور موجود در فایل پشتیبان متصل نیستند.")
        }
        val checkIds = backup.checks.map { it.id }.toSet()
        if (backup.checks.any {
                (it.replacedByCheckId != null && it.replacedByCheckId !in checkIds) ||
                    (it.replacesCheckId != null && it.replacesCheckId !in checkIds)
            }
        ) {
            return ValidationResult.Invalid("ارتباط زنجیره جایگزینی چک‌ها در فایل پشتیبان ناقص است.")
        }
        val reminderIds = backup.reminders.map { it.id }.toSet()
        if (backup.reminderSchedules.any { it.reminderId !in reminderIds }) {
            return ValidationResult.Invalid("فایل پشتیبان دارای زمان‌بندی یادآور بدون یادآور مرتبط است.")
        }
        if (backup.metadata.backupVersion >= 6) {
            val legacyReminderIds = backup.legacyReminders.map { it.id }.toSet()
            if (backup.legacyReminderSchedules.any { it.reminderId !in legacyReminderIds }) {
                return ValidationResult.Invalid("فایل پشتیبان دارای زمان‌بندی قدیمی بدون یادآور مرتبط است.")
            }
        }

        val vehicleIds = buildSet {
            backup.vehicles.forEach { vehicle ->
                add(vehicle.id.toString())
                vehicle.serverId?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
        val referencedVehicleIds = buildList {
            backup.vehicleServices.forEach { add(it.vehicleId) }
            backup.vehicleExpenses.forEach { add(it.vehicleId) }
            backup.vehicleInsurances.forEach { add(it.vehicleId) }
            backup.vehicleInspections.forEach { add(it.vehicleId) }
        }
        if (referencedVehicleIds.any { it !in vehicleIds }) {
            return ValidationResult.Invalid("فایل پشتیبان دارای اطلاعات خودرو بدون خودروی مرتبط است.")
        }

        return ValidationResult.Valid
    }

    private fun legacyV8Checksum(backup: DarinoBackup): String {
        val legacy = LegacyDarinoBackupV8(
            metadata = backup.metadata.copy(checksum = ""),
            accounts = backup.accounts,
            checks = backup.checks,
            transactions = backup.transactions,
            installments = backup.installments,
            installmentPayments = backup.installmentPayments,
            vehicles = backup.vehicles,
            vehicleServices = backup.vehicleServices,
            vehicleExpenses = backup.vehicleExpenses,
            vehicleInsurances = backup.vehicleInsurances,
            vehicleInspections = backup.vehicleInspections,
            reminders = backup.reminders,
            reminderSchedules = backup.reminderSchedules,
            financialEvents = backup.financialEvents,
            financialHealthProfiles = backup.financialHealthProfiles,
            loanCalculations = backup.loanCalculations,
            userPreference = backup.userPreference,
            appSettings = backup.appSettings,
            notificationLogs = backup.notificationLogs,
            smsLogs = backup.smsLogs,
            vehicleStore = backup.vehicleStore,
            reminderDeliveryLogs = backup.reminderDeliveryLogs,
            legacyReminders = backup.legacyReminders,
            legacyReminderSchedules = backup.legacyReminderSchedules,
            financeMetadata = backup.financeMetadata,
            userBankSmsRulesJson = backup.userBankSmsRulesJson,
            smsPatternsJson = backup.smsPatternsJson,
            smsRecognitionStartMillis = backup.smsRecognitionStartMillis,
            attachments = backup.attachments
        )
        return calculateChecksum(moshi.adapter(LegacyDarinoBackupV8::class.java).toJson(legacy))
    }

    private fun legacyV6Checksum(backup: DarinoBackup): String {
        val legacy = LegacyDarinoBackupV6(
            metadata = backup.metadata.copy(checksum = ""),
            accounts = backup.accounts,
            checks = backup.checks,
            transactions = backup.transactions,
            installments = backup.installments,
            installmentPayments = backup.installmentPayments,
            vehicles = backup.vehicles,
            vehicleServices = backup.vehicleServices,
            vehicleExpenses = backup.vehicleExpenses,
            vehicleInsurances = backup.vehicleInsurances,
            vehicleInspections = backup.vehicleInspections,
            reminders = backup.reminders,
            reminderSchedules = backup.reminderSchedules,
            financialEvents = backup.financialEvents,
            financialHealthProfiles = backup.financialHealthProfiles,
            loanCalculations = backup.loanCalculations,
            userPreference = backup.userPreference,
            appSettings = backup.appSettings,
            notificationLogs = backup.notificationLogs,
            smsLogs = backup.smsLogs,
            vehicleStore = backup.vehicleStore,
            reminderDeliveryLogs = backup.reminderDeliveryLogs,
            legacyReminders = backup.legacyReminders,
            legacyReminderSchedules = backup.legacyReminderSchedules
        )
        return calculateChecksum(moshi.adapter(LegacyDarinoBackupV6::class.java).toJson(legacy))
    }

    private fun legacyV5Checksum(backup: DarinoBackup): String {
        val legacy = LegacyDarinoBackupV5(
            metadata = backup.metadata.copy(checksum = ""),
            accounts = backup.accounts,
            checks = backup.checks,
            transactions = backup.transactions,
            installments = backup.installments,
            installmentPayments = backup.installmentPayments,
            vehicles = backup.vehicles,
            vehicleServices = backup.vehicleServices,
            vehicleExpenses = backup.vehicleExpenses,
            vehicleInsurances = backup.vehicleInsurances,
            vehicleInspections = backup.vehicleInspections,
            reminders = backup.reminders,
            reminderSchedules = backup.reminderSchedules,
            financialEvents = backup.financialEvents,
            financialHealthProfiles = backup.financialHealthProfiles,
            loanCalculations = backup.loanCalculations,
            userPreference = backup.userPreference,
            appSettings = backup.appSettings,
            notificationLogs = backup.notificationLogs,
            smsLogs = backup.smsLogs,
            vehicleStore = backup.vehicleStore,
            reminderDeliveryLogs = backup.reminderDeliveryLogs
        )
        return calculateChecksum(moshi.adapter(LegacyDarinoBackupV5::class.java).toJson(legacy))
    }

    private fun legacyV4Checksum(backup: DarinoBackup): String {
        val legacy = LegacyDarinoBackupV4(
            metadata = backup.metadata.copy(checksum = ""),
            accounts = backup.accounts,
            checks = backup.checks,
            transactions = backup.transactions,
            installments = backup.installments,
            installmentPayments = backup.installmentPayments,
            vehicles = backup.vehicles,
            vehicleServices = backup.vehicleServices,
            vehicleExpenses = backup.vehicleExpenses,
            vehicleInsurances = backup.vehicleInsurances,
            vehicleInspections = backup.vehicleInspections,
            reminders = backup.reminders,
            reminderSchedules = backup.reminderSchedules,
            financialEvents = backup.financialEvents,
            financialHealthProfiles = backup.financialHealthProfiles,
            loanCalculations = backup.loanCalculations,
            userPreference = backup.userPreference,
            appSettings = backup.appSettings,
            notificationLogs = backup.notificationLogs,
            smsLogs = backup.smsLogs
        )
        return calculateChecksum(moshi.adapter(LegacyDarinoBackupV4::class.java).toJson(legacy))
    }

    private fun legacyV3Checksum(backup: DarinoBackup): String {
        val legacy = LegacyDarinoBackupV3(
            metadata = backup.metadata.copy(checksum = ""),
            accounts = backup.accounts,
            checks = backup.checks,
            transactions = backup.transactions,
            installments = backup.installments,
            installmentPayments = backup.installmentPayments,
            vehicles = backup.vehicles,
            vehicleServices = backup.vehicleServices,
            vehicleExpenses = backup.vehicleExpenses,
            vehicleInsurances = backup.vehicleInsurances,
            vehicleInspections = backup.vehicleInspections,
            reminders = backup.reminders,
            reminderSchedules = backup.reminderSchedules,
            financialEvents = backup.financialEvents,
            financialHealthProfiles = backup.financialHealthProfiles,
            loanCalculations = backup.loanCalculations,
            userPreference = backup.userPreference,
            appSettings = backup.appSettings
        )
        return calculateChecksum(moshi.adapter(LegacyDarinoBackupV3::class.java).toJson(legacy))
    }

    private fun legacyV2Checksum(backup: DarinoBackup): String {
        val legacy = LegacyDarinoBackupV2(
            metadata = backup.metadata.copy(checksum = ""),
            accounts = backup.accounts,
            checks = backup.checks,
            transactions = backup.transactions,
            installments = backup.installments,
            installmentPayments = backup.installmentPayments,
            vehicles = backup.vehicles,
            vehicleServices = backup.vehicleServices,
            vehicleExpenses = backup.vehicleExpenses,
            vehicleInsurances = backup.vehicleInsurances,
            vehicleInspections = backup.vehicleInspections,
            reminders = backup.reminders,
            reminderSchedules = backup.reminderSchedules,
            financialEvents = backup.financialEvents
        )
        return calculateChecksum(moshi.adapter(LegacyDarinoBackupV2::class.java).toJson(legacy))
    }

    private fun legacyV1Checksum(backup: DarinoBackup): String {
        val legacy = LegacyDarinoBackupV1(
            metadata = backup.metadata.copy(checksum = ""),
            transactions = backup.transactions,
            installments = backup.installments,
            installmentPayments = backup.installmentPayments,
            vehicles = backup.vehicles,
            vehicleServices = backup.vehicleServices,
            vehicleExpenses = backup.vehicleExpenses,
            vehicleInsurances = backup.vehicleInsurances,
            vehicleInspections = backup.vehicleInspections,
            reminders = backup.reminders,
            reminderSchedules = backup.reminderSchedules,
            financialEvents = backup.financialEvents
        )
        return calculateChecksum(moshi.adapter(LegacyDarinoBackupV1::class.java).toJson(legacy))
    }
}

private data class LegacyDarinoBackupV8(
    val metadata: BackupMetadata,
    val accounts: List<AccountEntity> = emptyList(),
    val checks: List<CheckEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val installmentPayments: List<InstallmentPaymentEntity> = emptyList(),
    val vehicles: List<VehicleEntity> = emptyList(),
    val vehicleServices: List<VehicleServiceEntity> = emptyList(),
    val vehicleExpenses: List<VehicleExpenseRoomEntity> = emptyList(),
    val vehicleInsurances: List<VehicleInsuranceRoomEntity> = emptyList(),
    val vehicleInspections: List<VehicleInspectionRoomEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val reminderSchedules: List<ReminderScheduleEntity> = emptyList(),
    val financialEvents: List<FinancialEventEntity> = emptyList(),
    val financialHealthProfiles: List<com.example.financial_health.data.FinancialHealthEntity> = emptyList(),
    val loanCalculations: List<com.example.loan.data.LoanCalculationEntity> = emptyList(),
    val userPreference: com.example.data.database.UserPreferenceEntity? = null,
    val appSettings: AppSettingsBackup? = null,
    val notificationLogs: List<com.example.data.database.NotificationLogEntity> = emptyList(),
    val smsLogs: List<com.example.data.database.SmsLogEntity> = emptyList(),
    val vehicleStore: com.example.data.database.VehicleStoreEntity? = null,
    val reminderDeliveryLogs: List<com.example.reminder.data.ReminderDeliveryLogEntity> = emptyList(),
    val legacyReminders: List<com.example.data.database.ReminderEntity> = emptyList(),
    val legacyReminderSchedules: List<com.example.data.database.ReminderScheduleEntity> = emptyList(),
    val financeMetadata: FinanceMetadataBackup? = null,
    val userBankSmsRulesJson: String? = null,
    val smsPatternsJson: String? = null,
    val smsRecognitionStartMillis: Long? = null,
    val attachments: List<BackupAttachment> = emptyList()
)

private data class LegacyDarinoBackupV6(
    val metadata: BackupMetadata,
    val accounts: List<AccountEntity> = emptyList(),
    val checks: List<CheckEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val installmentPayments: List<InstallmentPaymentEntity> = emptyList(),
    val vehicles: List<VehicleEntity> = emptyList(),
    val vehicleServices: List<VehicleServiceEntity> = emptyList(),
    val vehicleExpenses: List<VehicleExpenseRoomEntity> = emptyList(),
    val vehicleInsurances: List<VehicleInsuranceRoomEntity> = emptyList(),
    val vehicleInspections: List<VehicleInspectionRoomEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val reminderSchedules: List<ReminderScheduleEntity> = emptyList(),
    val financialEvents: List<FinancialEventEntity> = emptyList(),
    val financialHealthProfiles: List<com.example.financial_health.data.FinancialHealthEntity> = emptyList(),
    val loanCalculations: List<com.example.loan.data.LoanCalculationEntity> = emptyList(),
    val userPreference: com.example.data.database.UserPreferenceEntity? = null,
    val appSettings: AppSettingsBackup? = null,
    val notificationLogs: List<com.example.data.database.NotificationLogEntity> = emptyList(),
    val smsLogs: List<com.example.data.database.SmsLogEntity> = emptyList(),
    val vehicleStore: com.example.data.database.VehicleStoreEntity? = null,
    val reminderDeliveryLogs: List<com.example.reminder.data.ReminderDeliveryLogEntity> = emptyList(),
    val legacyReminders: List<com.example.data.database.ReminderEntity> = emptyList(),
    val legacyReminderSchedules: List<com.example.data.database.ReminderScheduleEntity> = emptyList()
)

private data class LegacyDarinoBackupV5(
    val metadata: BackupMetadata,
    val accounts: List<AccountEntity> = emptyList(),
    val checks: List<CheckEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val installmentPayments: List<InstallmentPaymentEntity> = emptyList(),
    val vehicles: List<VehicleEntity> = emptyList(),
    val vehicleServices: List<VehicleServiceEntity> = emptyList(),
    val vehicleExpenses: List<VehicleExpenseRoomEntity> = emptyList(),
    val vehicleInsurances: List<VehicleInsuranceRoomEntity> = emptyList(),
    val vehicleInspections: List<VehicleInspectionRoomEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val reminderSchedules: List<ReminderScheduleEntity> = emptyList(),
    val financialEvents: List<FinancialEventEntity> = emptyList(),
    val financialHealthProfiles: List<com.example.financial_health.data.FinancialHealthEntity> = emptyList(),
    val loanCalculations: List<com.example.loan.data.LoanCalculationEntity> = emptyList(),
    val userPreference: com.example.data.database.UserPreferenceEntity? = null,
    val appSettings: AppSettingsBackup? = null,
    val notificationLogs: List<com.example.data.database.NotificationLogEntity> = emptyList(),
    val smsLogs: List<com.example.data.database.SmsLogEntity> = emptyList(),
    val vehicleStore: com.example.data.database.VehicleStoreEntity? = null,
    val reminderDeliveryLogs: List<com.example.reminder.data.ReminderDeliveryLogEntity> = emptyList()
)

private data class LegacyDarinoBackupV4(
    val metadata: BackupMetadata,
    val accounts: List<AccountEntity> = emptyList(),
    val checks: List<CheckEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val installmentPayments: List<InstallmentPaymentEntity> = emptyList(),
    val vehicles: List<VehicleEntity> = emptyList(),
    val vehicleServices: List<VehicleServiceEntity> = emptyList(),
    val vehicleExpenses: List<VehicleExpenseRoomEntity> = emptyList(),
    val vehicleInsurances: List<VehicleInsuranceRoomEntity> = emptyList(),
    val vehicleInspections: List<VehicleInspectionRoomEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val reminderSchedules: List<ReminderScheduleEntity> = emptyList(),
    val financialEvents: List<FinancialEventEntity> = emptyList(),
    val financialHealthProfiles: List<com.example.financial_health.data.FinancialHealthEntity> = emptyList(),
    val loanCalculations: List<com.example.loan.data.LoanCalculationEntity> = emptyList(),
    val userPreference: com.example.data.database.UserPreferenceEntity? = null,
    val appSettings: AppSettingsBackup? = null,
    val notificationLogs: List<com.example.data.database.NotificationLogEntity> = emptyList(),
    val smsLogs: List<com.example.data.database.SmsLogEntity> = emptyList()
)

private data class LegacyDarinoBackupV3(
    val metadata: BackupMetadata,
    val accounts: List<AccountEntity> = emptyList(),
    val checks: List<CheckEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val installmentPayments: List<InstallmentPaymentEntity> = emptyList(),
    val vehicles: List<VehicleEntity> = emptyList(),
    val vehicleServices: List<VehicleServiceEntity> = emptyList(),
    val vehicleExpenses: List<VehicleExpenseRoomEntity> = emptyList(),
    val vehicleInsurances: List<VehicleInsuranceRoomEntity> = emptyList(),
    val vehicleInspections: List<VehicleInspectionRoomEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val reminderSchedules: List<ReminderScheduleEntity> = emptyList(),
    val financialEvents: List<FinancialEventEntity> = emptyList(),
    val financialHealthProfiles: List<com.example.financial_health.data.FinancialHealthEntity> = emptyList(),
    val loanCalculations: List<com.example.loan.data.LoanCalculationEntity> = emptyList(),
    val userPreference: com.example.data.database.UserPreferenceEntity? = null,
    val appSettings: AppSettingsBackup? = null
)

private data class LegacyDarinoBackupV2(
    val metadata: BackupMetadata,
    val accounts: List<AccountEntity> = emptyList(),
    val checks: List<CheckEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val installmentPayments: List<InstallmentPaymentEntity> = emptyList(),
    val vehicles: List<VehicleEntity> = emptyList(),
    val vehicleServices: List<VehicleServiceEntity> = emptyList(),
    val vehicleExpenses: List<VehicleExpenseRoomEntity> = emptyList(),
    val vehicleInsurances: List<VehicleInsuranceRoomEntity> = emptyList(),
    val vehicleInspections: List<VehicleInspectionRoomEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val reminderSchedules: List<ReminderScheduleEntity> = emptyList(),
    val financialEvents: List<FinancialEventEntity> = emptyList()
)

private data class LegacyDarinoBackupV1(
    val metadata: BackupMetadata,
    val transactions: List<TransactionEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val installmentPayments: List<InstallmentPaymentEntity> = emptyList(),
    val vehicles: List<VehicleEntity> = emptyList(),
    val vehicleServices: List<VehicleServiceEntity> = emptyList(),
    val vehicleExpenses: List<VehicleExpenseRoomEntity> = emptyList(),
    val vehicleInsurances: List<VehicleInsuranceRoomEntity> = emptyList(),
    val vehicleInspections: List<VehicleInspectionRoomEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val reminderSchedules: List<ReminderScheduleEntity> = emptyList(),
    val financialEvents: List<FinancialEventEntity> = emptyList()
)

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}
