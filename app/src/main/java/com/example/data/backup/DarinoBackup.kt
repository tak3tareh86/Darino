package com.example.data.backup

import com.example.data.database.TransactionEntity
import com.example.data.database.InstallmentEntity
import com.example.data.database.InstallmentPaymentEntity
import com.example.data.database.VehicleEntity
import com.example.data.database.VehicleServiceEntity
import com.example.reminder.data.ReminderEntity
import com.example.reminder.data.ReminderScheduleEntity
import com.example.calendar.data.FinancialEventEntity

data class DarinoBackup(
    val metadata: BackupMetadata,
    val transactions: List<TransactionEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val installmentPayments: List<InstallmentPaymentEntity> = emptyList(),
    val vehicles: List<VehicleEntity> = emptyList(),
    val vehicleServices: List<VehicleServiceEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val reminderSchedules: List<ReminderScheduleEntity> = emptyList(),
    val financialEvents: List<FinancialEventEntity> = emptyList()
)
