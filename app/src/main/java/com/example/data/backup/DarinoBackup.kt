package com.example.data.backup

import com.example.data.database.AccountEntity
import com.example.data.database.CheckEntity
import com.example.data.database.TransactionEntity
import com.example.data.database.InstallmentEntity
import com.example.data.database.InstallmentPaymentEntity
import com.example.data.database.VehicleEntity
import com.example.data.database.VehicleServiceEntity
import com.example.data.database.VehicleExpenseRoomEntity
import com.example.data.database.VehicleInsuranceRoomEntity
import com.example.data.database.VehicleInspectionRoomEntity
import com.example.reminder.data.ReminderEntity
import com.example.reminder.data.ReminderScheduleEntity
import com.example.calendar.data.FinancialEventEntity
import com.example.financial_health.data.FinancialHealthEntity
import com.example.loan.data.LoanCalculationEntity
import com.example.data.database.UserPreferenceEntity
import com.example.data.database.NotificationLogEntity
import com.example.data.database.SmsLogEntity
import com.example.data.database.VehicleStoreEntity
import com.example.reminder.data.ReminderDeliveryLogEntity

data class DarinoBackup(
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
    val financialHealthProfiles: List<FinancialHealthEntity> = emptyList(),
    val loanCalculations: List<LoanCalculationEntity> = emptyList(),
    val userPreference: UserPreferenceEntity? = null,
    val appSettings: AppSettingsBackup? = null,
    val notificationLogs: List<NotificationLogEntity> = emptyList(),
    val smsLogs: List<SmsLogEntity> = emptyList(),
    val vehicleStore: VehicleStoreEntity? = null,
    val reminderDeliveryLogs: List<ReminderDeliveryLogEntity> = emptyList(),
    val legacyReminders: List<com.example.data.database.ReminderEntity> = emptyList(),
    val legacyReminderSchedules: List<com.example.data.database.ReminderScheduleEntity> = emptyList(),
    // These are user-owned configuration records stored outside Room.
    val financeMetadata: FinanceMetadataBackup? = null,
    val userBankSmsRulesJson: String? = null,
    val smsPatternsJson: String? = null,
    val smsRecognitionStartMillis: Long? = null,
    val attachments: List<BackupAttachment> = emptyList(),
    val bankSmsState: BankSmsStateBackup? = null
)

/** User-visible settings. Nullable additions preserve compatibility with older backup files. */
data class AppSettingsBackup(
    val themeMode: String,
    val language: String,
    val currency: String,
    val alertDelivery: String,
    val navigationTransition: String,
    val notificationsEnabled: Boolean? = null,
    val smsNotificationsEnabled: Boolean? = null,
    val notificationSoundEnabled: Boolean? = null,
    val notificationVibrationEnabled: Boolean? = null,
    val quietHoursEnabled: Boolean? = null,
    val quietHoursStartMinute: Int? = null,
    val quietHoursEndMinute: Int? = null,
    val allowHighPriorityInQuietHours: Boolean? = null,
    val defaultReminderTimeMinute: Int? = null,
    val installmentReminderDays: Int? = null,
    val vehicleReminderKilometers: Int? = null,
    val autoBackupDailyEnabled: Boolean? = null,
    val autoBackupWeeklyEnabled: Boolean? = null,
    val cleanSlatePerformed: Boolean? = null
)

/** JSON snapshots of custom finance preferences stored in darino_general_preferences. */
data class FinanceMetadataBackup(
    val budgetsJson: String? = null,
    val savingsGoalsJson: String? = null,
    val recurringTransactionsJson: String? = null,
    val categoriesJson: String? = null
)

/** Binary user attachments referenced by URI fields in database records. */
data class BackupAttachment(
    val originalUri: String,
    val fileName: String,
    val mediaType: String,
    val sha256: String,
    val base64Data: String
)

/** User-scoped bank-SMS review state; message bodies remain inside the encrypted backup. */
data class BankSmsStateBackup(
    // SMS provider row IDs are device-local and deliberately are not transferred.
    val pendingSuggestionsJson: String? = null
)
