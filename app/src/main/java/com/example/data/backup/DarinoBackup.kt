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
    val reminderDeliveryLogs: List<ReminderDeliveryLogEntity> = emptyList()
)

/** User-visible general preferences stored outside Room. */
data class AppSettingsBackup(
    val themeMode: String,
    val language: String,
    val currency: String,
    val alertDelivery: String,
    val navigationTransition: String
)
