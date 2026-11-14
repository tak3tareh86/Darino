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
import android.util.Base64

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
            value is Map<*, *> && requiredKeys.all { key -> value.containsKey(key) && value[key] != null }
        }
    }

    private fun jsonArrayStringValues(rawJson: String?, key: String): List<String>? {
        if (rawJson.isNullOrBlank()) return emptyList()
        val values = runCatching { jsonArrayAdapter.fromJson(rawJson) }.getOrNull() ?: return null
        if (values.any { value ->
                value !is Map<*, *> ||
                    (value as Map<*, *>).let { it.containsKey(key) && it[key] != null && it[key] !is String }
            }
        ) return null
        return values.mapNotNull { (it as Map<*, *>)[key] as? String }
            .filter(String::isNotBlank)
    }

    private fun jsonObjectList(rawJson: String?): List<Map<*, *>>? {
        if (rawJson.isNullOrBlank()) return emptyList()
        val values = runCatching { jsonArrayAdapter.fromJson(rawJson) }.getOrNull() ?: return null
        if (values.any { it !is Map<*, *> }) return null
        return values.map { it as Map<*, *> }
    }

    private fun isFinanceMetadataValid(finance: FinanceMetadataBackup?): Boolean {
        if (finance == null) return false
        val budgets = jsonObjectList(finance.budgetsJson) ?: return false
        val savings = jsonObjectList(finance.savingsGoalsJson) ?: return false
        val recurring = jsonObjectList(finance.recurringTransactionsJson) ?: return false
        val categories = jsonObjectList(finance.categoriesJson) ?: return false
        val units = setOf("TOMAN", "RIAL")
        val transactionTypes = com.example.ui.screens.finance.model.TransactionType.values().map { it.name }.toSet()
        val frequencies = com.example.ui.screens.finance.model.RecurringFrequency.values().map { it.name }.toSet()
        val paymentMethods = com.example.ui.screens.finance.model.PaymentMethod.values().map { it.name }.toSet()
        val goalStatuses = com.example.ui.screens.finance.model.SavingsGoalStatus.values().map { it.name }.toSet()

        fun requiredString(item: Map<*, *>, key: String): Boolean =
            (item[key] as? String)?.isNotBlank() == true
        fun optionalStrings(item: Map<*, *>, vararg keys: String): Boolean =
            keys.all { item[it] == null || item[it] is String }
        fun optionalBoolean(item: Map<*, *>, key: String): Boolean =
            item[key] == null || item[key] is Boolean
        fun optionalEnum(item: Map<*, *>, key: String, allowed: Set<String>): Boolean {
            val value = item[key]
            return value == null || (value is String && value in allowed)
        }
        fun requiredEnum(item: Map<*, *>, key: String, allowed: Set<String>): Boolean {
            val value = item[key] as? String
            return value != null && value in allowed
        }
        fun uniqueIds(items: List<Map<*, *>>): Boolean {
            val ids = items.map { it["id"] as? String ?: return false }
            return ids.all(String::isNotBlank) && ids.size == ids.toSet().size
        }

        if (!uniqueIds(budgets) || budgets.any {
                !requiredString(it, "id") || !requiredString(it, "title") ||
                    it["amount"] !is Number || !optionalEnum(it, "amountUnit", units) ||
                    !optionalStrings(it, "categoryId", "categoryTitle", "period", "startDate", "endDate") ||
                    !optionalBoolean(it, "isEnabled")
            }) return false

        if (!uniqueIds(savings) || savings.any {
                !requiredString(it, "id") || !requiredString(it, "title") ||
                    it["targetAmount"] !is Number || it["currentAmount"] !is Number ||
                    !optionalEnum(it, "amountUnit", units) ||
                    !optionalStrings(it, "targetDate", "description", "iconEmoji") ||
                    !optionalEnum(it, "status", goalStatuses)
            }) return false

        if (!uniqueIds(recurring) || recurring.any {
                !requiredString(it, "id") || !requiredString(it, "title") ||
                    it["amount"] !is Number || !optionalEnum(it, "amountUnit", units) ||
                    !requiredEnum(it, "type", transactionTypes) ||
                    !requiredString(it, "categoryId") || !requiredString(it, "categoryTitle") ||
                    !requiredEnum(it, "frequency", frequencies) ||
                    !requiredString(it, "startDate") || !requiredString(it, "nextExecutionDate") ||
                    !optionalStrings(it, "endDate", "accountName", "accountId") ||
                    !optionalBoolean(it, "enabled") ||
                    !optionalEnum(it, "paymentMethod", paymentMethods)
            }) return false

        if (!uniqueIds(categories) || categories.any {
                !requiredString(it, "id") || !requiredString(it, "title") ||
                    !requiredString(it, "iconEmoji") ||
                    !requiredEnum(it, "type", transactionTypes) ||
                    (it["accentColor"] != null && it["accentColor"] !is Number) ||
                    !optionalBoolean(it, "isDefault") || !optionalBoolean(it, "isActive") ||
                    (it["subCategories"] != null && (
                        it["subCategories"] !is List<*> ||
                            (it["subCategories"] as List<*>).any { child -> child !is String }
                    ))
            }) return false

        return true
    }

    private fun isPendingBankSmsQueueValid(rawJson: String?): Boolean {
        if (rawJson.isNullOrBlank()) return true
        val values = runCatching { jsonArrayAdapter.fromJson(rawJson) }.getOrNull() ?: return false
        val validUnits = setOf("TOMAN", "RIAL")
        val validTypes = com.example.ui.screens.finance.model.TransactionType.values().map { it.name }.toSet()
        val seenIds = mutableSetOf<String>()
        for (value in values) {
            val item = value as? Map<*, *> ?: return false
            val id = item["id"] as? String ?: return false
            val patternId = item["patternId"] as? String ?: return false
            val bankName = item["bankName"] as? String ?: return false
            val smsText = item["smsText"] as? String ?: return false
            val amountUnit = item["amountUnit"] as? String ?: return false
            if (id.isBlank() || !seenIds.add(id) || patternId.isBlank() ||
                bankName.isBlank() || smsText.isBlank() || amountUnit !in validUnits ||
                item["amount"] !is Number || item["receivedAtMillis"] !is Number
            ) return false

            val balanceUnitValue = item["balanceAmountUnit"]
            if (balanceUnitValue != null && balanceUnitValue !is String) return false
            val balanceUnit = balanceUnitValue as? String
            if (!balanceUnit.isNullOrBlank() && balanceUnit !in validUnits) return false

            val rawAmountCandidates = item["amountCandidates"]
            if (rawAmountCandidates != null && rawAmountCandidates !is List<*>) return false
            val amountCandidates = rawAmountCandidates as? List<*> ?: emptyList<Any>()
            if (amountCandidates.any { it !is Number || it.toDouble() <= 0.0 }) return false

            val rawCandidateUnits = item["amountCandidateUnits"]
            if (rawCandidateUnits != null && rawCandidateUnits !is List<*>) return false
            val candidateUnits = rawCandidateUnits as? List<*> ?: emptyList<Any>()
            if (candidateUnits.isNotEmpty() &&
                (candidateUnits.size != amountCandidates.size || candidateUnits.any { it !is String || it !in validUnits })
            ) return false

            val typeValue = item["type"]
            if (typeValue != null && (typeValue !is String || typeValue !in validTypes)) return false
            val preserveValue = item["preserveAfterRestore"]
            if (preserveValue != null && preserveValue !is Boolean) return false
        }
        return values.size <= 50
    }

    private fun hasUniqueJsonIds(rawJson: String?): Boolean {
        if (rawJson.isNullOrBlank()) return true
        val ids = jsonArrayStringValues(rawJson, "id") ?: return false
        return ids.size == countJsonArrayItems(rawJson) && ids.size == ids.toSet().size
    }

    private fun <T> List<T>.hasDuplicates(): Boolean = size != toSet().size

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
        if (version !in 1..9) {
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
            in 7..9 -> backup.metadata.currencyUnit == "PER_RECORD"
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
        val bankSmsStateRecordCount = if (version >= 9) pendingBankSmsCount else 0
        val bankSmsStateValid = version < 9 || (
            bankSmsState != null &&
                pendingBankSmsCount in 0..50 &&
                isJsonObjectArray(
                    bankSmsState.pendingSuggestionsJson,
                    setOf("id", "patternId", "bankName", "amount", "amountUnit", "smsText", "receivedAtMillis")
                ) &&
                isPendingBankSmsQueueValid(bankSmsState.pendingSuggestionsJson) &&
                (jsonArrayStringValues(bankSmsState.pendingSuggestionsJson, "id")?.let {
                    it.size == pendingBankSmsCount && it.size == it.toSet().size
                } ?: false)
        )
        val finance = backup.financeMetadata
        val vehicleStore = backup.vehicleStore
        val vehicleStoreJsonValid = vehicleStore == null || (
            isJsonObjectArray(vehicleStore.vehiclesJson, setOf("id", "brand", "model", "year", "color", "plate", "currentMileage")) &&
                isJsonObjectArray(vehicleStore.servicesJson, setOf("id", "vehicleId", "title", "date", "mileage", "cost")) &&
                isJsonObjectArray(vehicleStore.expensesJson, setOf("id", "vehicleId", "title", "amount", "date")) &&
                isJsonObjectArray(vehicleStore.insurancesJson, setOf("id", "vehicleId", "company", "type", "startDate", "endDate", "amount")) &&
                isJsonObjectArray(vehicleStore.inspectionsJson, setOf("id", "vehicleId", "lastInspectionDate", "expiryDate", "cost")) &&
                listOf(
                    vehicleStore.vehiclesJson,
                    vehicleStore.servicesJson,
                    vehicleStore.expensesJson,
                    vehicleStore.insurancesJson,
                    vehicleStore.inspectionsJson
                ).all(::hasUniqueJsonIds)
        )
        val customCollectionsValid = version < 8 || (
            isFinanceMetadataValid(finance) &&
                isJsonObjectArray(backup.userBankSmsRulesJson, setOf("bankName")) &&
                isJsonObjectArray(backup.smsPatternsJson, setOf("name", "type", "sampleMessage"))
        )
        if (configurationRecordCounts.any { it < 0 } || pendingBankSmsCount < 0 ||
            !customCollectionsValid || !vehicleStoreJsonValid || !bankSmsStateValid ||
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
            val encodedData = attachment.base64Data
            val bytes = runCatching { Base64.decode(encodedData, Base64.NO_WRAP) }
                .getOrNull()
                ?.takeIf { Base64.encodeToString(it, Base64.NO_WRAP) == encodedData }
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

        // Primary-key collisions must be rejected before Room starts a replacement transaction.
        val duplicatePrimaryKeys =
            backup.accounts.map { it.id }.hasDuplicates() ||
                backup.checks.map { it.id }.hasDuplicates() ||
                backup.transactions.map { it.id }.hasDuplicates() ||
                backup.installments.map { it.id }.hasDuplicates() ||
                backup.installmentPayments.map { it.id }.hasDuplicates() ||
                backup.vehicles.map { it.id }.hasDuplicates() ||
                backup.vehicleServices.map { it.id }.hasDuplicates() ||
                backup.vehicleExpenses.map { it.id }.hasDuplicates() ||
                backup.vehicleInsurances.map { it.id }.hasDuplicates() ||
                backup.vehicleInspections.map { it.id }.hasDuplicates() ||
                backup.reminders.map { it.id }.hasDuplicates() ||
                backup.reminderSchedules.map { it.id }.hasDuplicates() ||
                backup.financialEvents.map { it.id }.hasDuplicates() ||
                backup.financialHealthProfiles.map { it.id }.hasDuplicates() ||
                backup.notificationLogs.map { it.id }.hasDuplicates() ||
                backup.smsLogs.map { it.id }.hasDuplicates() ||
                backup.reminderDeliveryLogs.map { it.id }.hasDuplicates() ||
                backup.legacyReminders.map { it.id }.hasDuplicates() ||
                backup.legacyReminderSchedules.map { it.id }.hasDuplicates()
        if (duplicatePrimaryKeys) {
            return ValidationResult.Invalid("فایل پشتیبان دارای شناسه تکراری در یکی از بخش‌ها است.")
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

        val legacyVehicleIds = jsonArrayStringValues(vehicleStore?.vehiclesJson, "id")
            ?: return ValidationResult.Invalid("شناسه خودروهای قدیمی در فایل پشتیبان معتبر نیست.")
        val structuredVehicleIds = buildSet {
            backup.vehicles.forEach { vehicle ->
                add(vehicle.id.toString())
                vehicle.serverId?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
        val allVehicleIds = structuredVehicleIds + legacyVehicleIds
        val structuredReferences = buildList {
            backup.vehicleServices.forEach { add(it.vehicleId) }
            backup.vehicleExpenses.forEach { add(it.vehicleId) }
            backup.vehicleInsurances.forEach { add(it.vehicleId) }
            backup.vehicleInspections.forEach { add(it.vehicleId) }
        }
        if (structuredReferences.any { it !in allVehicleIds }) {
            return ValidationResult.Invalid("فایل پشتیبان دارای اطلاعات خودرو بدون خودروی مرتبط است.")
        }

        // Older releases could leave stale child rows in the legacy JSON store after
        // migrating to Room or deleting a vehicle. When structured vehicle records exist,
        // those Room collections are authoritative; unmatched legacy rows are discarded
        // by the migration cleanup and must not make an otherwise restorable backup fail.
        // Purely legacy backups still require every legacy child to reference a legacy car.
        if (backup.vehicles.isEmpty()) {
            val legacyReferences = buildList {
                listOf(
                    vehicleStore?.servicesJson,
                    vehicleStore?.expensesJson,
                    vehicleStore?.insurancesJson,
                    vehicleStore?.inspectionsJson
                ).forEach { raw ->
                    jsonArrayStringValues(raw, "vehicleId")?.let { addAll(it) }
                }
            }
            if (legacyReferences.any { it !in legacyVehicleIds }) {
                return ValidationResult.Invalid("فایل پشتیبان دارای اطلاعات خودرو بدون خودروی مرتبط است.")
            }
        }

        // Reminder source links are historical/advisory references, not Room foreign keys.
        // A linked installment, check, vehicle or service may have been deleted while its
        // reminder remains in the user's history. Restore remaps a source ID when the
        // referenced row is present and otherwise preserves the original source ID.
        // Therefore, rejecting the entire backup for an orphaned source would lose valid
        // reminders and must not be part of backup validation.

        fun <T, K> hasDuplicates(items: List<T>, key: (T) -> K): Boolean =
            items.map(key).size != items.map(key).toSet().size

        if (hasDuplicates(backup.accounts) { it.stringId } ||
            hasDuplicates(backup.transactions.filter { it.stringId.isNotBlank() }) { it.stringId } ||
            hasDuplicates(backup.checks) { it.id } ||
            hasDuplicates(backup.installments) { it.id } ||
            hasDuplicates(backup.vehicles) { it.id } ||
            hasDuplicates(backup.vehicleServices) { it.id } ||
            hasDuplicates(backup.vehicleExpenses) { it.id } ||
            hasDuplicates(backup.vehicleInsurances) { it.id } ||
            hasDuplicates(backup.vehicleInspections) { it.id } ||
            hasDuplicates(backup.reminders) { it.id } ||
            hasDuplicates(backup.reminderSchedules) { it.id } ||
            hasDuplicates(backup.legacyReminders) { it.id } ||
            hasDuplicates(backup.financialEvents) { it.id } ||
            hasDuplicates(backup.financialHealthProfiles) { it.id } ||
            hasDuplicates(backup.loanCalculations) { it.id }
        ) {
            return ValidationResult.Invalid("فایل پشتیبان دارای شناسه‌های تکراری است و بازیابی ایمن نیست.")
        }
        if (hasDuplicates(backup.vehicles.filter { !it.serverId.isNullOrBlank() }) { it.serverId!! } ||
            hasDuplicates(backup.vehicleServices.filter { !it.serverId.isNullOrBlank() }) { it.serverId!! }
        ) {
            return ValidationResult.Invalid("شناسه‌های سروری خودرو یا سرویس در فایل پشتیبان تکراری هستند.")
        }

        val accountReferences = buildSet {
            backup.accounts.forEach {
                add(it.id.toString())
                if (it.stringId.isNotBlank()) add(it.stringId)
            }
        }
        val referencedAccountIds = buildList {
            backup.checks.mapNotNullTo(this) { it.accountId }
            backup.transactions.forEach { transaction ->
                transaction.accountId?.let(::add)
                transaction.transferSourceAccountId?.let(::add)
                transaction.transferDestinationAccountId?.let(::add)
            }
        }
        if (referencedAccountIds.any { it !in accountReferences }) {
            return ValidationResult.Invalid("یکی از تراکنش‌ها یا چک‌ها به حساب مالی موجود در فایل پشتیبان متصل نیست.")
        }
        if (backup.transactions.any { transaction ->
                transaction.sourceType.equals("CHECK", ignoreCase = true) &&
                    transaction.sourceId?.toIntOrNull()?.let { it !in checkIds } == true
            }) {
            return ValidationResult.Invalid("یکی از تراکنش‌های مرتبط با چک به چک موجود در فایل پشتیبان متصل نیست.")
        }
        val recurringAccountIds = jsonArrayStringValues(finance?.recurringTransactionsJson, "accountId")
            ?: return ValidationResult.Invalid("شناسه حساب در تراکنش‌های تکرارشونده معتبر نیست.")
        if (recurringAccountIds.any { it !in accountReferences }) {
            return ValidationResult.Invalid("یکی از تراکنش‌های تکرارشونده به حساب مالی موجود در فایل پشتیبان متصل نیست.")
        }
        if (backup.checks.any { !it.accountId.isNullOrBlank() && it.accountId !in accountReferences } ||
            backup.transactions.any {
                (!it.accountId.isNullOrBlank() && it.accountId !in accountReferences) ||
                    (!it.transferSourceAccountId.isNullOrBlank() && it.transferSourceAccountId !in accountReferences) ||
                    (!it.transferDestinationAccountId.isNullOrBlank() && it.transferDestinationAccountId !in accountReferences)
            }
        ) {
            return ValidationResult.Invalid("یکی از تراکنش‌ها یا چک‌ها به حساب مالی موجود در فایل پشتیبان متصل نیست.")
        }
        if (backup.transactions.any {
                it.sourceType == "CHECK" &&
                    !it.sourceId.isNullOrBlank() &&
                    it.sourceId !in checkIds.map { checkId -> checkId.toString() }.toSet()
            }
        ) {
            return ValidationResult.Invalid("تراکنش تسویه به چک موجود در فایل پشتیبان متصل نیست.")
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
