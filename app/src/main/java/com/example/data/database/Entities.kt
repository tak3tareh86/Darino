package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String? = null,
    val email: String? = null,
    val passwordHash: String? = null,
    val salt: String? = null,
    val phoneNumber: String? = null,
    val phoneNumberMasked: String? = null,
    val verificationStatus: String = "VERIFIED", // "UNVERIFIED", "VERIFIED"
    val verifiedAt: Long? = System.currentTimeMillis()
)

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["userId", "stringId"], unique = true)]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val stringId: String,
    val name: String,
    val type: String, // "BANK", "CASH", "CARD", "OTHER"
    val bankName: String? = null,
    val accountNumberMasked: String? = null,
    val initialBalance: Long = 0L,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["userId", "stringId"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val amount: Long = 0L,
    val type: String = "EXPENSE", // "EXPENSE", "INCOME", "TRANSFER"
    val category: String = "",
    val accountName: String = "کارت بانکی",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = "",
    val stringId: String = "",
    val title: String = "",
    val subCategory: String? = null,
    val datePersian: String = "",
    val paymentMethod: String = "BANK_CARD",
    val sourceType: String = "MANUAL",
    val sourceId: String? = null,
    val isRecurring: Boolean = false,
    val syncState: String = "PENDING_UPSERT",
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val accountId: String? = null,
    val transferSourceAccountId: String? = null,
    val transferDestinationAccountId: String? = null
)

@Entity(tableName = "installments")
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serverId: String? = null,
    val syncState: String = "SYNCED", // "SYNCED", "PENDING_INSERT", "PENDING_UPDATE", "PENDING_DELETE"
    val userId: String,
    val category: String, // "LOAN_BANK", "LOAN_HOME", "VEHICLE_INSURANCE", "MISC"
    val providerName: String,
    val title: String,
    val amount: Long,
    val totalInstallments: Int,
    val paidInstallments: Int,
    val remainingInstallments: Int,
    val startDate: Long,
    val dueDay: Int,
    val nextDueDate: Long,
    val status: String, // "ACTIVE", "PAID", "OVERDUE"
    val notes: String?,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "installment_payments")
data class InstallmentPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val installmentId: Int,
    val installmentNumber: Int = 1,
    val amount: Long,
    val dueDate: Long,
    val paidDate: Long?,
    val status: String, // "PENDING", "PAID", "OVERDUE"
    val paymentReference: String?,
    val notes: String?
)

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serverId: String? = null,
    val syncState: String = "SYNCED",
    val userId: String,
    val brand: String,
    val model: String,
    val year: String,
    val plate: String,
    val currentMileage: Int,
    val notes: String?,
    val vin: String = "",
    val estimatedValue: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "vehicle_services")
data class VehicleServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serverId: String? = null,
    val syncState: String = "SYNCED",
    val userId: String = "",
    val vehicleId: String = "",
    val type: String, // "INSURANCE", "INSPECTION", "OIL", "SERVICE", "TIRES", "BATTERY", "MAINTENANCE", "TAX", "CUSTOM"
    val title: String,
    val serviceDate: Long? = null,
    val dueDate: Long? = null,
    val cost: Long = 0L,
    val dueMileage: Int? = null,
    val status: String = "PENDING", // "PENDING", "COMPLETED"
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "vehicle_expenses")
data class VehicleExpenseRoomEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val vehicleId: String,
    val title: String,
    val category: String,
    val amount: Long,
    val date: String,
    val description: String = "",
    val receiptImageUri: String? = null
)

@Entity(tableName = "vehicle_insurances")
data class VehicleInsuranceRoomEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val vehicleId: String,
    val company: String,
    val type: String,
    val startDate: String,
    val endDate: String,
    val amount: Long,
    val policyNumber: String = "",
    val reminderDays: String = "30,15,7"
)

@Entity(tableName = "vehicle_inspections")
data class VehicleInspectionRoomEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val vehicleId: String,
    val lastInspectionDate: String,
    val expiryDate: String,
    val cost: Long,
    val status: String = "معتبر",
    val centerName: String = ""
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serverId: String? = null,
    val syncState: String = "SYNCED", // "SYNCED", "PENDING_INSERT", "PENDING_UPDATE", "PENDING_DELETE", "FAILED"
    val userId: String,
    val type: String, // "GENERAL", "INSTALLMENT", "VEHICLE", "INSURANCE", "MAINTENANCE", "FUEL", "CUSTOM"
    val title: String,
    val description: String,
    val sourceId: Int? = null,
    val sourceType: String? = null, // "INSTALLMENT", "VEHICLE_SERVICE"
    val scheduledDateTime: Long,
    val timezone: String = "Asia/Tehran",
    val repeatRule: String? = "NONE", // "NONE", "DAILY", "WEEKLY", "MONTHLY"
    val enabled: Boolean = true,
    val notificationEnabled: Boolean = true,
    val smsEnabled: Boolean = false,
    val phoneNumber: String? = null,
    val phoneNumberMasked: String? = null,
    val smsDeliveryStatus: String? = null, // "QUEUED", "SENT", "DELIVERED", "FAILED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val deletedAt: Long? = null,
    val lastSyncedAt: Long? = null
)

@Entity(tableName = "reminder_schedules")
data class ReminderScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reminderId: Int,
    val triggerDateTime: Long,
    val status: String, // "PENDING", "SCHEDULED", "SENT", "DELIVERED", "FAILED", "CANCELLED"
    val notificationStatus: String,
    val smsStatus: String
)

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val serverId: String? = null,
    val syncState: String = "SYNCED",
    val reminderId: Int? = null,
    val title: String,
    val message: String,
    val type: String,
    val timestamp: Long,
    val isRead: Boolean,
    val source: String,
    val status: String,
    val deletedAt: Long? = null
)

@Entity(tableName = "sms_logs")
data class SmsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val reminderId: Int?,
    val providerMessageId: String?,
    val phoneNumber: String,
    val phoneNumberMasked: String? = null,
    val message: String,
    val status: String, // "QUEUED", "SENT", "DELIVERED", "FAILED"
    val sentAt: Long?,
    val deliveredAt: Long?,
    val failedAt: Long?,
    val failureReason: String?,
    val retryCount: Int = 0
)

@Entity(tableName = "user_notification_settings")
data class UserNotificationSettingsEntity(
    @PrimaryKey val userId: String,
    val androidNotificationsEnabled: Boolean = true,
    val generalRemindersEnabled: Boolean = true,
    val installmentRemindersEnabled: Boolean = true,
    val vehicleRemindersEnabled: Boolean = true,
    val smsRemindersEnabled: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "08:00",
    val allowImportantDuringQuietHours: Boolean = true
)

@Entity(
    tableName = "user_preferences",
    indices = [Index(value = ["userId"], unique = true)]
)
data class UserPreferenceEntity(
    @PrimaryKey val userId: String,
    val numberDisplayMode: String = "PERSIAN",
    val updatedAt: Long = System.currentTimeMillis()
)
