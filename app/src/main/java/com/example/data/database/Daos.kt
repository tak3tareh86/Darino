package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE username = :username COLLATE BINARY LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("UPDATE users SET passwordHash = :passwordHash, salt = :salt WHERE id = :userId")
    suspend fun updatePassword(userId: String, passwordHash: String, salt: String)

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getFirstUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(userId: String): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun clearAllTransactions(userId: String)

    @Query("SELECT * FROM transactions WHERE userId = :userId AND syncState != 'SYNCED' ORDER BY updatedAt ASC")
    suspend fun getPendingSyncTransactions(userId: String): List<TransactionEntity>

    @Query("UPDATE transactions SET syncState = 'SYNCED', updatedAt = :updatedAt, deletedAt = :deletedAt WHERE userId = :userId AND stringId = :stringId")
    suspend fun markTransactionSynced(userId: String, stringId: String, updatedAt: Long, deletedAt: Long?)

    @Query("UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncState = 'PENDING_DELETE' WHERE userId = :userId AND stringId = :stringId")
    suspend fun softDeleteByStringId(userId: String, stringId: String, deletedAt: Long, updatedAt: Long)

    @Query("SELECT * FROM transactions WHERE userId = :userId AND stringId = :stringId LIMIT 1")
    suspend fun getTransactionIncludingDeleted(userId: String, stringId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE userId = :userId AND (stringId = :stringId OR CAST(id AS TEXT) = :stringId)")
    suspend fun deleteByStringId(userId: String, stringId: String)

    @Query("SELECT * FROM transactions WHERE userId = :userId AND (stringId = :stringId OR CAST(id AS TEXT) = :stringId) LIMIT 1")
    suspend fun getTransactionByStringId(userId: String, stringId: String): TransactionEntity?
}

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments WHERE deletedAt IS NULL ORDER BY nextDueDate ASC")
    fun getAllInstallments(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments")
    suspend fun getAllInstallmentsList(): List<InstallmentEntity>

    @Query("DELETE FROM installments")
    suspend fun clearAllInstallments()

    @Query("SELECT * FROM installments WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getInstallmentById(id: Int): InstallmentEntity?


    @Query("SELECT * FROM installments WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncInstallments(): List<InstallmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: InstallmentEntity): Long

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)

    @Query("UPDATE installments SET deletedAt = :deletedAt, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteInstallment(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteInstallment(installment: InstallmentEntity)

    // Payments
    @Query("SELECT * FROM installment_payments WHERE installmentId = :installmentId ORDER BY dueDate ASC")
    fun getPaymentsForInstallment(installmentId: Int): Flow<List<InstallmentPaymentEntity>>

    @Query("SELECT * FROM installment_payments")
    suspend fun getAllPaymentsList(): List<InstallmentPaymentEntity>

    @Query("DELETE FROM installment_payments")
    suspend fun clearAllPayments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: InstallmentPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<InstallmentPaymentEntity>)
}

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE deletedAt IS NULL")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles")
    suspend fun getAllVehiclesList(): List<VehicleEntity>

    @Query("SELECT * FROM vehicle_services")
    suspend fun getAllServicesList(): List<VehicleServiceEntity>

    @Query("SELECT * FROM vehicle_expenses")
    suspend fun getAllExpensesList(): List<VehicleExpenseRoomEntity>

    @Query("SELECT * FROM vehicle_insurances")
    suspend fun getAllInsurancesList(): List<VehicleInsuranceRoomEntity>

    @Query("SELECT * FROM vehicle_inspections")
    suspend fun getAllInspectionsList(): List<VehicleInspectionRoomEntity>

    @Query("DELETE FROM vehicles")
    suspend fun clearAllVehicles()

    @Query("DELETE FROM vehicle_services")
    suspend fun clearAllServices()

    @Query("DELETE FROM vehicle_expenses")
    suspend fun clearAllExpenses()

    @Query("DELETE FROM vehicle_insurances")
    suspend fun clearAllInsurances()

    @Query("DELETE FROM vehicle_inspections")
    suspend fun clearAllInspections()

    @Query("DELETE FROM vehicle_expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<VehicleExpenseRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: VehicleExpenseRoomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsurances(insurances: List<VehicleInsuranceRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsurance(insurance: VehicleInsuranceRoomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspections(inspections: List<VehicleInspectionRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(inspection: VehicleInspectionRoomEntity)

    @Query("SELECT * FROM vehicles WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getVehicleById(id: Int): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncVehicles(): List<VehicleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Query("UPDATE vehicles SET deletedAt = :deletedAt, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteVehicle(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)

    // Services
    @Query("SELECT * FROM vehicle_services WHERE vehicleId = :vehicleId AND deletedAt IS NULL ORDER BY dueDate ASC")
    fun getServicesForVehicle(vehicleId: Int): Flow<List<VehicleServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: VehicleServiceEntity)

    @Query("SELECT * FROM vehicle_store WHERE `key` = :key LIMIT 1")
    suspend fun getVehicleStore(key: String = "vehicle_data"): VehicleStoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleStore(store: VehicleStoreEntity)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE deletedAt IS NULL ORDER BY scheduledDateTime ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND deletedAt IS NULL ORDER BY scheduledDateTime ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND deletedAt IS NULL ORDER BY scheduledDateTime ASC")
    suspend fun getActiveRemindersSnapshot(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getReminderById(id: Int): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE serverId = :serverId LIMIT 1")
    suspend fun getReminderByServerId(serverId: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET deletedAt = :deletedAt, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteReminder(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun hardDeleteReminder(id: Int)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET syncState = :syncState, serverId = :serverId, lastSyncedAt = :lastSyncedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, serverId: String?, syncState: String, lastSyncedAt: Long)

    @Query("UPDATE reminders SET smsDeliveryStatus = :status WHERE id = :id")
    suspend fun updateSmsStatus(id: Int, status: String)

    @Query("UPDATE reminders SET enabled = :enabled, updatedAt = :updatedAt, syncState = 'PENDING_UPDATE' WHERE id = :id")
    suspend fun toggleEnabled(id: Int, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE reminders SET completedAt = :completedAt, updatedAt = :updatedAt, syncState = 'PENDING_UPDATE' WHERE id = :id")
    suspend fun markCompleted(id: Int, completedAt: Long?, updatedAt: Long = System.currentTimeMillis())

    // Schedules
    @Query("SELECT * FROM reminder_schedules WHERE reminderId = :reminderId")
    suspend fun getSchedulesForReminder(reminderId: Int): List<ReminderScheduleEntity>

    @Query("SELECT * FROM reminder_schedules WHERE triggerDateTime >= :start AND triggerDateTime <= :end AND status = 'PENDING'")
    suspend fun getPendingSchedulesInRange(start: Long, end: Long): List<ReminderScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ReminderScheduleEntity)

    @Query("UPDATE reminder_schedules SET status = :status, notificationStatus = :notifStatus, smsStatus = :smsStatus WHERE id = :id")
    suspend fun updateScheduleStatus(id: Int, status: String, notifStatus: String, smsStatus: String)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs WHERE deletedAt IS NULL ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationLogEntity>>

    @Query("SELECT COUNT(*) FROM notification_logs WHERE isRead = 0 AND deletedAt IS NULL")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT * FROM notification_logs WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncNotifications(): List<NotificationLogEntity>

    @Query("SELECT * FROM notification_logs WHERE serverId = :serverId LIMIT 1")
    suspend fun getNotificationByServerId(serverId: String): NotificationLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(log: NotificationLogEntity): Long

    @Query("UPDATE notification_logs SET isRead = 1, syncState = 'PENDING_UPDATE' WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("UPDATE notification_logs SET isRead = 1, syncState = 'PENDING_UPDATE'")
    suspend fun markAllAsRead()

    @Query("UPDATE notification_logs SET syncState = :syncState, serverId = :serverId WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, serverId: String?, syncState: String)

    @Query("DELETE FROM notification_logs WHERE id = :id")
    suspend fun deleteNotification(id: Int)
}

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY id DESC")
    fun getAllSmsLogs(): Flow<List<SmsLogEntity>>

    @Query("SELECT * FROM sms_logs WHERE providerMessageId = :providerId LIMIT 1")
    suspend fun getLogByProviderId(providerId: String): SmsLogEntity?

    @Query("SELECT * FROM sms_logs WHERE reminderId = :reminderId ORDER BY id DESC LIMIT 1")
    suspend fun getLatestLogForReminder(reminderId: Int): SmsLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsLog(log: SmsLogEntity): Long

    @Query("UPDATE sms_logs SET status = :status, providerMessageId = :providerId, sentAt = :sentAt, deliveredAt = :deliveredAt, failedAt = :failedAt, failureReason = :reason, retryCount = :retryCount WHERE id = :id")
    suspend fun updateSmsLog(
        id: Int,
        status: String,
        providerId: String?,
        sentAt: Long?,
        deliveredAt: Long?,
        failedAt: Long?,
        reason: String?,
        retryCount: Int
    )

    @Query("UPDATE sms_logs SET status = :status, deliveredAt = :deliveredAt WHERE providerMessageId = :providerId")
    suspend fun updateStatusByProviderId(providerId: String, status: String, deliveredAt: Long?)
}

@Entity(tableName = "vehicle_store")
data class VehicleStoreEntity(
    @PrimaryKey val key: String = "vehicle_data",
    val vehiclesJson: String = "",
    val servicesJson: String = "",
    val expensesJson: String = "",
    val insurancesJson: String = "",
    val inspectionsJson: String = ""
)
