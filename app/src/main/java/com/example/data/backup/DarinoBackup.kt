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
import com.example.data.sms.SmsPattern

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
    // SMS templates are exported as portable data, then re-encrypted on the destination device.
    val smsPatterns: List<SmsPattern> = emptyList(),
    val smsRecognitionStartMillis: Long? = null,
    // Contains raw SMS content for suggestions that were still awaiting user review.
    val pendingBankSmsSuggestions: List<PendingBankSmsBackup> = emptyList()
)

/** User-visible general preferences stored outside Room. */
data class AppSettingsBackup(
    val themeMode: String,
    val language: String,
    val currency: String,
    val alertDelivery: String,
    val navigationTransition: String
)

/** Portable representation of a pending SMS review item; independent of UI-layer models. */
data class PendingBankSmsBackup(
    val id: String,
    val bankName: String,
    val amount: Long,
    val formattedAmount: String,
    val isAmountValid: Boolean,
    val type: String?,
    val isTypeUncertain: Boolean,
    val smsText: String,
    val dateText: String,
    val timeText: String,
    val category: String,
    val sourceAccount: String?,
    val destinationAccount: String?,
    val rawSender: String?,
    val parseError: String?,
    val timestampMillis: Long,
    val balanceAmount: Long?,
    val trackingCode: String?,
    val patternId: String
)
