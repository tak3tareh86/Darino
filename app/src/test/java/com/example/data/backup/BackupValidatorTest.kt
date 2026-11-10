package com.example.data.backup

import com.example.data.database.AccountEntity
import com.example.data.database.UserPreferenceEntity
import com.example.data.database.TransactionEntity
import com.example.data.database.VehicleStoreEntity
import com.example.reminder.data.ReminderDeliveryLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {
    @Test
    fun rejectsDuplicatePrimaryKeysBeforeRestore() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 7,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 2,
                    currencyUnit = "PER_RECORD"
                ),
                accounts = listOf(
                    AccountEntity(id = 7, userId = "backup-user", stringId = "account-a", name = "Cash", type = "CASH"),
                    AccountEntity(id = 7, userId = "backup-user", stringId = "account-b", name = "Bank", type = "BANK")
                )
            )
        )

        assertTrue(BackupValidator.validate(backup) is ValidationResult.Invalid)
    }

    @Test
    fun rejectsTransactionReferencesToMissingAccounts() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 7,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 1,
                    currencyUnit = "PER_RECORD"
                ),
                transactions = listOf(
                    TransactionEntity(
                        id = 1,
                        userId = "backup-user",
                        amount = 500,
                        accountId = "missing-account"
                    )
                )
            )
        )

        assertTrue(BackupValidator.validate(backup) is ValidationResult.Invalid)
    }

    @Test
    fun versionSevenBackupAcceptsPerRecordCurrencyMetadata() {
        val unsigned = DarinoBackup(
            metadata = BackupMetadata(
                backupVersion = 7,
                createdAt = 1L,
                recordCount = 0,
                currencyUnit = "PER_RECORD"
            )
        )
        val valid = BackupValidator.withChecksum(unsigned)
        assertEquals(ValidationResult.Valid, BackupValidator.validate(valid))
    }

    @Test
    fun versionSevenBackupRejectsLegacySingleUnitMetadata() {
        val unsigned = DarinoBackup(
            metadata = BackupMetadata(
                backupVersion = 7,
                createdAt = 1L,
                recordCount = 0,
                currencyUnit = "TOMAN"
            )
        )
        val valid = BackupValidator.withChecksum(unsigned)
        assertTrue(BackupValidator.validate(valid) is ValidationResult.Invalid)
    }



    @Test
    fun versionFourBackupWithPreferencesValidates() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 4,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 1,
                    checksum = "",
                    currencyUnit = "TOMAN"
                ),
                userPreference = UserPreferenceEntity(
                    userId = "backup-user",
                    numberDisplayMode = "PERSIAN"
                )
            )
        )

        assertEquals(1, backup.metadata.recordCount)
        assertTrue(BackupValidator.validate(backup) is ValidationResult.Valid)
    }

    @Test
    fun versionFourBackupCountsGeneralAppSettings() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 4,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 1,
                    checksum = "",
                    currencyUnit = "TOMAN"
                ),
                appSettings = AppSettingsBackup(
                    themeMode = "SYSTEM",
                    language = "PERSIAN",
                    currency = "TOMAN",
                    alertDelivery = "BOTH",
                    navigationTransition = "DYNAMIC"
                )
            )
        )

        assertEquals(1, backup.metadata.recordCount)
        assertTrue(BackupValidator.validate(backup) is ValidationResult.Valid)
    }

    @Test
    fun versionFourBackupRejectsRecordCountMismatch() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 4,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 0,
                    checksum = "",
                    currencyUnit = "TOMAN"
                ),
                userPreference = UserPreferenceEntity(
                    userId = "backup-user",
                    numberDisplayMode = "PERSIAN"
                )
            )
        )

        assertTrue(BackupValidator.validate(backup) is ValidationResult.Invalid)
    }
    @Test
    fun versionFiveBackupCountsVehicleStoreAndReminderDeliveryHistory() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 5,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 2,
                    checksum = "",
                    currencyUnit = "TOMAN"
                ),
                vehicleStore = VehicleStoreEntity(
                    key = "vehicle_data",
                    userId = "backup-user",
                    vehiclesJson = "[]"
                ),
                reminderDeliveryLogs = listOf(
                    ReminderDeliveryLogEntity(
                        id = "delivery-1",
                        reminderId = "reminder-1",
                        channel = "NOTIFICATION",
                        scheduledAt = 1_800_000_000_000L
                    )
                )
            )
        )

        assertEquals(2, backup.metadata.recordCount)
        assertTrue(BackupValidator.validate(backup) is ValidationResult.Valid)
    }

    @Test
    fun versionSixBackupIncludesLegacyReminderTablesAndValidates() {
        val legacyReminder = com.example.data.database.ReminderEntity(
            id = 12,
            userId = "backup-user",
            type = "GENERAL",
            title = "یادآور آزمایشی",
            description = "توضیحات",
            scheduledDateTime = 1_800_000_000_000L
        )
        val legacySchedule = com.example.data.database.ReminderScheduleEntity(
            id = 21,
            reminderId = 12,
            triggerDateTime = 1_800_000_000_000L,
            status = "PENDING",
            notificationStatus = "PENDING",
            smsStatus = "DISABLED"
        )
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 6,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 2,
                    checksum = "",
                    currencyUnit = "TOMAN"
                ),
                legacyReminders = listOf(legacyReminder),
                legacyReminderSchedules = listOf(legacySchedule)
            )
        )

        assertEquals(2, backup.metadata.recordCount)
        assertTrue(BackupValidator.validate(backup) is ValidationResult.Valid)
    }

    @Test
    fun versionSevenBackupRejectsUnknownPerRecordCurrencyUnit() {
        val unsigned = DarinoBackup(
            metadata = BackupMetadata(
                backupVersion = 7,
                createdAt = 1L,
                recordCount = 1,
                currencyUnit = "PER_RECORD"
            ),
            transactions = listOf(
                TransactionEntity(
                    userId = "backup-user",
                    amount = 1234L,
                    amountUnit = "UNKNOWN"
                )
            )
        )
        val validChecksum = BackupValidator.withChecksum(unsigned)

        assertTrue(BackupValidator.validate(validChecksum) is ValidationResult.Invalid)
    }

    @Test
    fun legacyVersionsOneThroughSevenRemainImportableAsJson() {
        for (version in 1..7) {
            val legacyBackup = BackupValidator.withChecksum(
                DarinoBackup(
                    metadata = BackupMetadata(
                        backupVersion = version,
                        createdAt = 1_800_000_000_000L,
                        recordCount = 0,
                        currencyUnit = if (version == 7) "PER_RECORD" else "TOMAN"
                    )
                )
            )

            val json = BackupFileCodec.encode(legacyBackup)
            val restored = BackupFileCodec.decode(json)

            assertEquals(version, restored.metadata.backupVersion)
            assertEquals(ValidationResult.Valid, BackupValidator.validate(restored, json))
        }
    }

    @Test
    fun versionEightLegacyJsonWithoutVersionNineSmsStateRemainsImportable() {
        val legacyBackup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 8,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 1,
                    currencyUnit = "PER_RECORD"
                ),
                appSettings = AppSettingsBackup(
                    themeMode = "SYSTEM",
                    language = "PERSIAN",
                    currency = "TOMAN",
                    alertDelivery = "NONE",
                    navigationTransition = "DYNAMIC"
                ),
                financeMetadata = FinanceMetadataBackup()
            )
        )

        val json = BackupFileCodec.encode(legacyBackup)
        assertFalse(json.contains("bankSmsState"))

        val restored = BackupFileCodec.decode(json)
        assertEquals(8, restored.metadata.backupVersion)
        assertEquals(null, restored.bankSmsState)
        assertEquals(ValidationResult.Valid, BackupValidator.validate(restored, json))
    }

    @Test
    fun backupJsonCodecRejectsEmptyFiles() {
        val result = runCatching { BackupFileCodec.decode("") }
        assertTrue(result.isFailure)
    }

    @Test
    fun versionEightCountsCustomFinanceAndSmsConfiguration() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 8,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 4,
                    currencyUnit = "PER_RECORD"
                ),
                appSettings = AppSettingsBackup(
                    themeMode = "SYSTEM",
                    language = "PERSIAN",
                    currency = "TOMAN",
                    alertDelivery = "NONE",
                    navigationTransition = "DYNAMIC"
                ),
                financeMetadata = FinanceMetadataBackup(
                    budgetsJson = """[{"id":"budget-1","title":"Budget","amount":100}]""",
                    savingsGoalsJson = "[]",
                    recurringTransactionsJson = "[]",
                    categoriesJson = "[]"
                ),
                userBankSmsRulesJson = """[{"bankName":"Bank","senders":["100"],"incomeMarkers":"income","expenseMarkers":"","transferMarkers":""}]""",
                smsPatternsJson = """[{"name":"Sample","type":"INCOME","sampleMessage":"SMS"}]"""
            )
        )

        assertTrue(BackupValidator.validate(backup) is ValidationResult.Valid)
    }

    @Test
    fun versionEightRejectsMalformedCustomConfigurationJson() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 8,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 1,
                    currencyUnit = "PER_RECORD"
                ),
                appSettings = AppSettingsBackup(
                    themeMode = "SYSTEM",
                    language = "PERSIAN",
                    currency = "TOMAN",
                    alertDelivery = "NONE",
                    navigationTransition = "DYNAMIC"
                ),
                financeMetadata = FinanceMetadataBackup(),
                userBankSmsRulesJson = "{not-an-array"
            )
        )

        assertTrue(BackupValidator.validate(backup) is ValidationResult.Invalid)
    }

    @Test
    fun versionEightValidatesReceiptAttachmentHashAndCount() {
        val bytes = "receipt-image".toByteArray()
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        val attachment = BackupAttachment(
            originalUri = "content://receipts/1",
            fileName = "receipt.png",
            mediaType = "image/png",
            sha256 = hash,
            base64Data = okio.ByteString.of(*bytes).base64()
        )
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 8,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 3,
                    currencyUnit = "PER_RECORD"
                ),
                appSettings = AppSettingsBackup(
                    themeMode = "SYSTEM",
                    language = "PERSIAN",
                    currency = "TOMAN",
                    alertDelivery = "NONE",
                    navigationTransition = "DYNAMIC"
                ),
                financeMetadata = FinanceMetadataBackup(),
                vehicleStore = VehicleStoreEntity(
                    key = "vehicle_data",
                    userId = "backup-user",
                    expensesJson = """[{"receiptImageUri":"content://receipts/1"}]"""
                ),
                attachments = listOf(attachment)
            )
        )

        assertTrue(BackupValidator.validate(backup) is ValidationResult.Valid)
    }

    @Test
    fun versionEightRejectsTamperedReceiptAttachment() {
        val bytes = "receipt-image".toByteArray()
        val attachment = BackupAttachment(
            originalUri = "content://receipts/1",
            fileName = "receipt.png",
            mediaType = "image/png",
            sha256 = "0".repeat(64),
            base64Data = okio.ByteString.of(*bytes).base64()
        )
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 8,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 2,
                    currencyUnit = "PER_RECORD"
                ),
                appSettings = AppSettingsBackup(
                    themeMode = "SYSTEM",
                    language = "PERSIAN",
                    currency = "TOMAN",
                    alertDelivery = "NONE",
                    navigationTransition = "DYNAMIC"
                ),
                financeMetadata = FinanceMetadataBackup(),
                attachments = listOf(attachment)
            )
        )

        assertTrue(BackupValidator.validate(backup) is ValidationResult.Invalid)
    }

    @Test
    fun versionEightRejectsMalformedFinanceRecordShapes() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 8,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 2,
                    currencyUnit = "PER_RECORD"
                ),
                appSettings = AppSettingsBackup(
                    themeMode = "SYSTEM",
                    language = "PERSIAN",
                    currency = "TOMAN",
                    alertDelivery = "NONE",
                    navigationTransition = "DYNAMIC"
                ),
                financeMetadata = FinanceMetadataBackup(
                    budgetsJson = """[{"id":"budget-1"}]"""
                )
            )
        )

        assertTrue(BackupValidator.validate(backup) is ValidationResult.Invalid)
    }

    @Test
    fun versionEightRejectsReceiptReferenceWithoutAttachment() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 8,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 2,
                    currencyUnit = "PER_RECORD"
                ),
                appSettings = AppSettingsBackup(
                    themeMode = "SYSTEM",
                    language = "PERSIAN",
                    currency = "TOMAN",
                    alertDelivery = "NONE",
                    navigationTransition = "DYNAMIC"
                ),
                financeMetadata = FinanceMetadataBackup(),
                vehicleStore = VehicleStoreEntity(
                    key = "vehicle_data",
                    userId = "backup-user",
                    expensesJson = """[{"receiptImageUri":"content://receipts/missing"}]"""
                )
            )
        )

        assertTrue(BackupValidator.validate(backup) is ValidationResult.Invalid)
    }

    @Test
    fun portableBackupDoesNotExposeLocalLoanOwnerId() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 8,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 2,
                    currencyUnit = "PER_RECORD"
                ),
                appSettings = AppSettingsBackup(
                    themeMode = "SYSTEM",
                    language = "PERSIAN",
                    currency = "TOMAN",
                    alertDelivery = "NONE",
                    navigationTransition = "DYNAMIC"
                ),
                financeMetadata = FinanceMetadataBackup(),
                loanCalculations = listOf(
                    com.example.loan.data.LoanCalculationEntity(
                        id = "loan-1",
                        title = "Loan",
                        loanType = "BANK_LOAN",
                        amount = 100_000L,
                        interestRate = 10.0,
                        duration = 12,
                        durationType = "MONTHS",
                        monthlyPayment = 9_000L,
                        totalPayment = 108_000L,
                        totalInterest = 8_000L,
                        realCostPercentage = 8.0,
                        userId = "private-user-id"
                    )
                )
            )
        )

        val json = BackupFileCodec.encode(backup)
        assertFalse(json.contains("private-user-id"))
        assertEquals(
            "legacy_unassigned",
            BackupFileCodec.decode(json).loanCalculations.single().userId
        )
    }

    @Test
    fun versionEightRejectsOutOfRangeReminderSettings() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 8,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 1,
                    currencyUnit = "PER_RECORD"
                ),
                appSettings = AppSettingsBackup(
                    themeMode = "SYSTEM",
                    language = "PERSIAN",
                    currency = "TOMAN",
                    alertDelivery = "NONE",
                    navigationTransition = "DYNAMIC",
                    defaultReminderTimeMinute = 1440
                ),
                financeMetadata = FinanceMetadataBackup()
            )
        )

        assertTrue(BackupValidator.validate(backup) is ValidationResult.Invalid)
    }

    @Test
    fun versionEightRejectsMalformedLegacyVehicleCollections() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 8,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 2,
                    currencyUnit = "PER_RECORD"
                ),
                appSettings = AppSettingsBackup(
                    themeMode = "SYSTEM",
                    language = "PERSIAN",
                    currency = "TOMAN",
                    alertDelivery = "NONE",
                    navigationTransition = "DYNAMIC"
                ),
                financeMetadata = FinanceMetadataBackup(),
                vehicleStore = VehicleStoreEntity(
                    key = "vehicle_data",
                    userId = "backup-user",
                    vehiclesJson = "{malformed"
                )
            )
        )

        assertTrue(BackupValidator.validate(backup) is ValidationResult.Invalid)
    }

    @Test
    fun versionNinePreservesAndValidatesPendingBankSmsSuggestions() {
        val backup = BackupValidator.withChecksum(
            DarinoBackup(
                metadata = BackupMetadata(
                    backupVersion = 9,
                    createdAt = 1_800_000_000_000L,
                    recordCount = 2,
                    currencyUnit = "PER_RECORD"
                ),
                appSettings = AppSettingsBackup(
                    themeMode = "SYSTEM",
                    language = "PERSIAN",
                    currency = "TOMAN",
                    alertDelivery = "NONE",
                    navigationTransition = "DYNAMIC"
                ),
                financeMetadata = FinanceMetadataBackup(),
                bankSmsState = BankSmsStateBackup(
                    pendingSuggestionsJson = """[{"id":"sms_1","patternId":"pattern_1","bankName":"Bank","amount":1000,"amountUnit":"TOMAN","smsText":"واریز","receivedAtMillis":1800000000000}]"""
                )
            )
        )

        assertTrue(BackupValidator.validate(backup) is ValidationResult.Valid)
        val decoded = BackupFileCodec.decode(BackupFileCodec.encode(backup))
        assertEquals(
            backup.bankSmsState?.pendingSuggestionsJson,
            decoded.bankSmsState?.pendingSuggestionsJson
        )
    }

}
