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

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserByIdSync(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND deletedAt IS NULL ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllTransactionsForBackup(userId: String): List<TransactionEntity>

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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE userId = :userId AND (stringId = :stringId OR CAST(id AS TEXT) = :stringId)")
    suspend fun deleteByStringId(userId: String, stringId: String)

    @Query("SELECT * FROM transactions WHERE userId = :userId AND deletedAt IS NULL AND (stringId = :stringId OR CAST(id AS TEXT) = :stringId) LIMIT 1")
    suspend fun getTransactionByStringId(userId: String, stringId: String): TransactionEntity?
}

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments WHERE userId = :userId AND deletedAt IS NULL ORDER BY nextDueDate ASC")
    fun getAllInstallments(userId: String): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE userId = :userId")
    suspend fun getAllInstallmentsList(userId: String): List<InstallmentEntity>

    @Query("DELETE FROM installments WHERE userId = :userId")
    suspend fun clearAllInstallments(userId: String)

    @Query("SELECT * FROM installments WHERE userId = :userId AND id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getInstallmentById(userId: String, id: Int): InstallmentEntity?


    @Query("SELECT * FROM installments WHERE userId = :userId AND syncState != 'SYNCED'")
    suspend fun getPendingSyncInstallments(userId: String): List<InstallmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: InstallmentEntity): Long

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)

    @Query("UPDATE installments SET deletedAt = :deletedAt, syncState = 'PENDING_DELETE' WHERE userId = :userId AND id = :id")
    suspend fun softDeleteInstallment(userId: String, id: Int, deletedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteInstallment(installment: InstallmentEntity)

    // Payments
    @Query("SELECT p.* FROM installment_payments p INNER JOIN installments i ON i.id = p.installmentId WHERE i.userId = :userId AND p.installmentId = :installmentId ORDER BY p.dueDate ASC")
    fun getPaymentsForInstallment(userId: String, installmentId: Int): Flow<List<InstallmentPaymentEntity>>

    @Query("SELECT p.* FROM installment_payments p INNER JOIN installments i ON i.id = p.installmentId WHERE i.userId = :userId")
    suspend fun getAllPaymentsList(userId: String): List<InstallmentPaymentEntity>

    @Query("DELETE FROM installment_payments WHERE installmentId IN (SELECT id FROM installments WHERE userId = :userId)")
    suspend fun clearAllPayments(userId: String)

    @Query("DELETE FROM installments WHERE userId = :userId AND (id = :id OR serverId = :serverId)")
    suspend fun deleteInstallmentById(userId: String, id: Int, serverId: String)

    @Query("DELETE FROM installment_payments WHERE installmentId IN (SELECT id FROM installments WHERE userId = :userId AND (id = :id OR serverId = :serverId)) OR (installmentId = :id AND :id > 0)")
    suspend fun deletePaymentsForInstallment(userId: String, id: Int, serverId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: InstallmentPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<InstallmentPaymentEntity>)
}

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE userId = :userId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllVehicles(userId: String): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE userId = :userId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getAllVehiclesList(userId: String): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getAllVehiclesForBackup(userId: String): List<VehicleEntity>

    @Query("SELECT * FROM vehicle_services WHERE userId = :userId AND deletedAt IS NULL ORDER BY dueDate ASC")
    fun getAllServices(userId: String): Flow<List<VehicleServiceEntity>>

    @Query("SELECT * FROM vehicle_services WHERE userId = :userId AND deletedAt IS NULL ORDER BY dueDate ASC")
    suspend fun getAllServicesList(userId: String): List<VehicleServiceEntity>

    @Query("SELECT * FROM vehicle_services WHERE userId = :userId ORDER BY dueDate ASC")
    suspend fun getAllServicesForBackup(userId: String): List<VehicleServiceEntity>

    @Query("SELECT * FROM vehicle_expenses WHERE userId = :userId ORDER BY date DESC")
    fun getAllExpenses(userId: String): Flow<List<VehicleExpenseRoomEntity>>

    @Query("SELECT * FROM vehicle_expenses WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllExpensesList(userId: String): List<VehicleExpenseRoomEntity>

    @Query("SELECT * FROM vehicle_insurances WHERE userId = :userId ORDER BY endDate ASC")
    fun getAllInsurances(userId: String): Flow<List<VehicleInsuranceRoomEntity>>

    @Query("SELECT * FROM vehicle_insurances WHERE userId = :userId ORDER BY endDate ASC")
    suspend fun getAllInsurancesList(userId: String): List<VehicleInsuranceRoomEntity>

    @Query("SELECT * FROM vehicle_inspections WHERE userId = :userId ORDER BY expiryDate ASC")
    fun getAllInspections(userId: String): Flow<List<VehicleInspectionRoomEntity>>

    @Query("SELECT * FROM vehicle_inspections WHERE userId = :userId ORDER BY expiryDate ASC")
    suspend fun getAllInspectionsList(userId: String): List<VehicleInspectionRoomEntity>

    @Query("SELECT * FROM vehicles WHERE userId = :userId AND (serverId = :vehicleId OR id = :vehicleId) AND deletedAt IS NULL LIMIT 1")
    suspend fun getVehicleByServerId(userId: String, vehicleId: String): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE userId = :userId AND id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getVehicleById(userId: String, id: Int): VehicleEntity?

    @Query("SELECT * FROM vehicle_services WHERE userId = :userId AND (serverId = :serviceId OR id = :serviceId) AND deletedAt IS NULL LIMIT 1")
    suspend fun getServiceByServerId(userId: String, serviceId: String): VehicleServiceEntity?

    @Query("SELECT * FROM vehicle_expenses WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getExpenseById(userId: String, id: String): VehicleExpenseRoomEntity?

    @Query("SELECT * FROM vehicle_insurances WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getInsuranceById(userId: String, id: String): VehicleInsuranceRoomEntity?

    @Query("SELECT * FROM vehicle_inspections WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getInspectionById(userId: String, id: String): VehicleInspectionRoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity): Int

    @Query("UPDATE vehicles SET currentMileage = :newMileage, updatedAt = :updatedAt WHERE userId = :userId AND (serverId = :vehicleId OR id = :vehicleId)")
    suspend fun updateMileage(userId: String, vehicleId: String, newMileage: Int, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM vehicles WHERE userId = :userId AND (serverId = :vehicleId OR id = :vehicleId)")
    suspend fun deleteVehicleByServerId(userId: String, vehicleId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: VehicleServiceEntity): Long

    @Update
    suspend fun updateService(service: VehicleServiceEntity): Int

    @Query("UPDATE vehicle_services SET notes = :notes, updatedAt = :updatedAt WHERE userId = :userId AND (serverId = :serviceId OR id = :serviceId)")
    suspend fun updateServiceNotes(userId: String, serviceId: String, notes: String?, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE vehicle_services SET status = 'COMPLETED', reminderEnabled = 0, serviceDate = :completedDateMs, dueDate = NULL, dueMileage = NULL, updatedAt = :updatedAt WHERE userId = :userId AND (serverId = :serviceId OR id = :serviceId)")
    suspend fun completeService(userId: String, serviceId: String, completedDateMs: Long, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM vehicle_services WHERE userId = :userId AND (serverId = :serviceId OR id = :serviceId)")
    suspend fun deleteServiceByServerId(userId: String, serviceId: String): Int

    @Query("DELETE FROM vehicle_services WHERE userId = :userId AND vehicleId = :vehicleId")
    suspend fun deleteServicesByVehicleId(userId: String, vehicleId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<VehicleExpenseRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpensesForBackup(expenses: List<VehicleExpenseRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: VehicleExpenseRoomEntity)

    @Update
    suspend fun updateExpense(expense: VehicleExpenseRoomEntity): Int

    @Query("DELETE FROM vehicle_expenses WHERE userId = :userId AND id = :id")
    suspend fun deleteExpenseById(userId: String, id: String): Int

    @Query("DELETE FROM vehicle_expenses WHERE userId = :userId AND vehicleId = :vehicleId")
    suspend fun deleteExpensesByVehicleId(userId: String, vehicleId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsurances(insurances: List<VehicleInsuranceRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInsurancesForBackup(insurances: List<VehicleInsuranceRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsurance(insurance: VehicleInsuranceRoomEntity)

    @Update
    suspend fun updateInsurance(insurance: VehicleInsuranceRoomEntity): Int

    @Query("UPDATE vehicle_insurances SET startDate = :startDate, endDate = :endDate WHERE userId = :userId AND id = :insuranceId")
    suspend fun renewInsurance(userId: String, insuranceId: String, startDate: String, endDate: String): Int

    @Query("DELETE FROM vehicle_insurances WHERE userId = :userId AND id = :id")
    suspend fun deleteInsuranceById(userId: String, id: String): Int

    @Query("DELETE FROM vehicle_insurances WHERE userId = :userId AND vehicleId = :vehicleId")
    suspend fun deleteInsurancesByVehicleId(userId: String, vehicleId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspections(inspections: List<VehicleInspectionRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInspectionsForBackup(inspections: List<VehicleInspectionRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(inspection: VehicleInspectionRoomEntity)

    @Update
    suspend fun updateInspection(inspection: VehicleInspectionRoomEntity): Int

    @Query("DELETE FROM vehicle_inspections WHERE userId = :userId AND id = :id")
    suspend fun deleteInspectionById(userId: String, id: String): Int

    @Query("DELETE FROM vehicle_inspections WHERE userId = :userId AND vehicleId = :vehicleId")
    suspend fun deleteInspectionsByVehicleId(userId: String, vehicleId: String): Int

    @Query("DELETE FROM vehicles WHERE userId = :userId")
    suspend fun clearAllVehicles(userId: String)

    @Query("DELETE FROM vehicle_services WHERE userId = :userId")
    suspend fun clearAllServices(userId: String)

    @Query("DELETE FROM vehicle_expenses WHERE userId = :userId")
    suspend fun clearAllExpenses(userId: String)

    @Query("DELETE FROM vehicle_insurances WHERE userId = :userId")
    suspend fun clearAllInsurances(userId: String)

    @Query("DELETE FROM vehicle_inspections WHERE userId = :userId")
    suspend fun clearAllInspections(userId: String)

    @Query("SELECT * FROM vehicle_services WHERE userId = :userId AND vehicleId = :vehicleId AND deletedAt IS NULL ORDER BY dueDate ASC")
    fun getServicesForVehicle(userId: String, vehicleId: String): Flow<List<VehicleServiceEntity>>

    @Query("SELECT * FROM vehicles WHERE userId = :userId AND syncState != 'SYNCED'")
    suspend fun getPendingSyncVehicles(userId: String): List<VehicleEntity>

    @Query("UPDATE vehicles SET deletedAt = :deletedAt, syncState = 'PENDING_DELETE' WHERE userId = :userId AND id = :id")
    suspend fun softDeleteVehicle(userId: String, id: Int, deletedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)

    @Query("SELECT serverId FROM vehicle_services WHERE userId = :userId AND vehicleId = :vehicleId")
    suspend fun getServiceServerIdsByVehicleId(userId: String, vehicleId: String): List<String>

    @Query("SELECT id FROM vehicle_insurances WHERE userId = :userId AND vehicleId = :vehicleId")
    suspend fun getInsuranceIdsByVehicleId(userId: String, vehicleId: String): List<String>

    @Query("SELECT id FROM vehicle_inspections WHERE userId = :userId AND vehicleId = :vehicleId")
    suspend fun getInspectionIdsByVehicleId(userId: String, vehicleId: String): List<String>

    @Query("DELETE FROM vehicle_services WHERE userId = :userId AND NOT EXISTS (SELECT 1 FROM vehicles WHERE vehicles.userId = vehicle_services.userId AND vehicles.deletedAt IS NULL AND (vehicles.serverId = vehicle_services.vehicleId OR CAST(vehicles.id AS TEXT) = vehicle_services.vehicleId))")
    suspend fun deleteOrphanVehicleServices(userId: String)

    @Query("DELETE FROM vehicle_expenses WHERE userId = :userId AND NOT EXISTS (SELECT 1 FROM vehicles WHERE vehicles.userId = vehicle_expenses.userId AND vehicles.deletedAt IS NULL AND (vehicles.serverId = vehicle_expenses.vehicleId OR CAST(vehicles.id AS TEXT) = vehicle_expenses.vehicleId))")
    suspend fun deleteOrphanVehicleExpenses(userId: String)

    @Query("DELETE FROM vehicle_insurances WHERE userId = :userId AND NOT EXISTS (SELECT 1 FROM vehicles WHERE vehicles.userId = vehicle_insurances.userId AND vehicles.deletedAt IS NULL AND (vehicles.serverId = vehicle_insurances.vehicleId OR CAST(vehicles.id AS TEXT) = vehicle_insurances.vehicleId))")
    suspend fun deleteOrphanVehicleInsurances(userId: String)

    @Query("DELETE FROM vehicle_inspections WHERE userId = :userId AND NOT EXISTS (SELECT 1 FROM vehicles WHERE vehicles.userId = vehicle_inspections.userId AND vehicles.deletedAt IS NULL AND (vehicles.serverId = vehicle_inspections.vehicleId OR CAST(vehicles.id AS TEXT) = vehicle_inspections.vehicleId))")
    suspend fun deleteOrphanVehicleInspections(userId: String)

    @Query("SELECT * FROM vehicle_store WHERE userId = :userId AND key = :key LIMIT 1")
    suspend fun getVehicleStore(userId: String, key: String = "vehicle_data"): VehicleStoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleStore(store: VehicleStoreEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVehicleStoreForBackup(store: VehicleStoreEntity): Long

    @Query("DELETE FROM vehicle_store WHERE userId = :userId")
    suspend fun clearVehicleStoreForUser(userId: String)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId")
    suspend fun getAllRemindersForBackup(userId: String): List<ReminderEntity>

    @Query("SELECT s.* FROM reminder_schedules s INNER JOIN reminders r ON r.id = s.reminderId WHERE r.userId = :userId")
    suspend fun getAllSchedulesForBackup(userId: String): List<ReminderScheduleEntity>

    @Query("DELETE FROM reminder_schedules WHERE reminderId IN (SELECT id FROM reminders WHERE userId = :userId)")
    suspend fun clearAllSchedulesForBackup(userId: String)

    @Query("DELETE FROM reminders WHERE userId = :userId")
    suspend fun clearAllRemindersForBackup(userId: String)

    @Query("SELECT * FROM reminders WHERE userId = :userId AND deletedAt IS NULL ORDER BY scheduledDateTime ASC")
    fun getAllReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND enabled = 1 AND deletedAt IS NULL ORDER BY scheduledDateTime ASC")
    fun getActiveReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND enabled = 1 AND deletedAt IS NULL ORDER BY scheduledDateTime ASC")
    suspend fun getActiveRemindersSnapshot(userId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getReminderById(userId: String, id: Int): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE userId = :userId AND serverId = :serverId LIMIT 1")
    suspend fun getReminderByServerId(userId: String, serverId: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE userId = :userId AND syncState != 'SYNCED'")
    suspend fun getPendingSyncReminders(userId: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET deletedAt = :deletedAt, syncState = 'PENDING_DELETE' WHERE userId = :userId AND id = :id")
    suspend fun softDeleteReminder(userId: String, id: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM reminders WHERE userId = :userId AND id = :id")
    suspend fun hardDeleteReminder(userId: String, id: Int)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET syncState = :syncState, serverId = :serverId, lastSyncedAt = :lastSyncedAt WHERE userId = :userId AND id = :id")
    suspend fun updateSyncStatus(userId: String, id: Int, serverId: String?, syncState: String, lastSyncedAt: Long)

    @Query("UPDATE reminders SET smsDeliveryStatus = :status WHERE userId = :userId AND id = :id")
    suspend fun updateSmsStatus(userId: String, id: Int, status: String)

    @Query("UPDATE reminders SET enabled = :enabled, updatedAt = :updatedAt, syncState = 'PENDING_UPDATE' WHERE userId = :userId AND id = :id")
    suspend fun toggleEnabled(userId: String, id: Int, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE reminders SET completedAt = :completedAt, updatedAt = :updatedAt, syncState = 'PENDING_UPDATE' WHERE userId = :userId AND id = :id")
    suspend fun markCompleted(userId: String, id: Int, completedAt: Long?, updatedAt: Long = System.currentTimeMillis())

    // Schedules
    @Query("SELECT s.* FROM reminder_schedules s INNER JOIN reminders r ON r.id = s.reminderId WHERE r.userId = :userId AND s.reminderId = :reminderId")
    suspend fun getSchedulesForReminder(userId: String, reminderId: Int): List<ReminderScheduleEntity>

    @Query("SELECT * FROM reminder_schedules WHERE triggerDateTime >= :start AND triggerDateTime <= :end AND status = 'PENDING'")
    suspend fun getPendingSchedulesInRange(start: Long, end: Long): List<ReminderScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ReminderScheduleEntity)

    @Query("UPDATE reminder_schedules SET status = :status, notificationStatus = :notifStatus, smsStatus = :smsStatus WHERE id = :id AND reminderId IN (SELECT id FROM reminders WHERE userId = :userId)")
    suspend fun updateScheduleStatus(userId: String, id: Int, status: String, notifStatus: String, smsStatus: String)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllNotificationsForBackup(userId: String): List<NotificationLogEntity>

    @Query("DELETE FROM notification_logs WHERE userId = :userId")
    suspend fun clearAllNotificationsForBackup(userId: String)

    @Query("SELECT * FROM notification_logs WHERE userId = :userId AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getAllNotifications(userId: String): Flow<List<NotificationLogEntity>>

    @Query("SELECT COUNT(*) FROM notification_logs WHERE userId = :userId AND isRead = 0 AND deletedAt IS NULL")
    fun getUnreadCount(userId: String): Flow<Int>

    @Query("SELECT * FROM notification_logs WHERE userId = :userId AND syncState != 'SYNCED'")
    suspend fun getPendingSyncNotifications(userId: String): List<NotificationLogEntity>

    @Query("SELECT * FROM notification_logs WHERE userId = :userId AND serverId = :serverId LIMIT 1")
    suspend fun getNotificationByServerId(userId: String, serverId: String): NotificationLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(log: NotificationLogEntity): Long

    @Query("UPDATE notification_logs SET isRead = 1, syncState = 'PENDING_UPDATE' WHERE userId = :userId AND id = :id")
    suspend fun markAsRead(userId: String, id: Int)

    @Query("UPDATE notification_logs SET isRead = 1, syncState = 'PENDING_UPDATE' WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)

    @Query("UPDATE notification_logs SET syncState = :syncState, serverId = :serverId WHERE userId = :userId AND id = :id")
    suspend fun updateSyncStatus(userId: String, id: Int, serverId: String?, syncState: String)

    @Query("UPDATE notification_logs SET deletedAt = :deletedAt WHERE userId = :userId AND id = :id")
    suspend fun deleteNotification(userId: String, id: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notification_logs SET deletedAt = :deletedAt WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun deleteAllNotifications(userId: String, deletedAt: Long = System.currentTimeMillis())
}

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs WHERE userId = :userId ORDER BY id DESC")
    suspend fun getAllSmsLogsForBackup(userId: String): List<SmsLogEntity>

    @Query("DELETE FROM sms_logs WHERE userId = :userId")
    suspend fun clearAllSmsLogsForBackup(userId: String)

    @Query("SELECT * FROM sms_logs WHERE userId = :userId ORDER BY id DESC")
    fun getAllSmsLogs(userId: String): Flow<List<SmsLogEntity>>

    @Query("SELECT * FROM sms_logs WHERE userId = :userId AND providerMessageId = :providerId LIMIT 1")
    suspend fun getLogByProviderId(userId: String, providerId: String): SmsLogEntity?

    @Query("SELECT * FROM sms_logs WHERE userId = :userId AND reminderId = :reminderId ORDER BY id DESC LIMIT 1")
    suspend fun getLatestLogForReminder(userId: String, reminderId: Int): SmsLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsLog(log: SmsLogEntity): Long

    @Query("UPDATE sms_logs SET status = :status, providerMessageId = :providerId, sentAt = :sentAt, deliveredAt = :deliveredAt, failedAt = :failedAt, failureReason = :reason, retryCount = :retryCount WHERE userId = :userId AND id = :id")
    suspend fun updateSmsLog(
        userId: String,
        id: Int,
        status: String,
        providerId: String?,
        sentAt: Long?,
        deliveredAt: Long?,
        failedAt: Long?,
        reason: String?,
        retryCount: Int
    )

    @Query("UPDATE sms_logs SET status = :status, deliveredAt = :deliveredAt WHERE userId = :userId AND providerMessageId = :providerId")
    suspend fun updateStatusByProviderId(userId: String, providerId: String, status: String, deliveredAt: Long?)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE userId = :userId AND deletedAt IS NULL ORDER BY name ASC")
    fun getAllAccountsFlow(userId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE userId = :userId AND deletedAt IS NULL ORDER BY name ASC")
    suspend fun getAllAccountsList(userId: String): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE userId = :userId AND stringId = :stringId AND deletedAt IS NULL LIMIT 1")
    suspend fun getAccountByStringId(userId: String, stringId: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE userId = :userId AND name = :name AND deletedAt IS NULL LIMIT 1")
    suspend fun getAccountByName(userId: String, name: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE userId = :userId")
    suspend fun getAllAccountsRaw(userId: String): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccountForBackup(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE userId = :userId AND stringId = :stringId")
    suspend fun softDeleteAccount(userId: String, stringId: String, deletedAt: Long, updatedAt: Long)

    @Query("DELETE FROM accounts WHERE userId = :userId")
    suspend fun clearAllAccounts(userId: String)
}

@Entity(tableName = "vehicle_store", primaryKeys = ["key", "userId"])
data class VehicleStoreEntity(
    val key: String = "vehicle_data",
    val userId: String = "",
    val vehiclesJson: String = "",
    val servicesJson: String = "",
    val expensesJson: String = "",
    val insurancesJson: String = "",
    val inspectionsJson: String = ""
)

@Dao
interface UserPreferenceDao {
    @Query("SELECT * FROM user_preferences WHERE userId = :userId LIMIT 1")
    fun getPreferenceFlow(userId: String): Flow<UserPreferenceEntity?>

    @Query("SELECT * FROM user_preferences WHERE userId = :userId LIMIT 1")
    suspend fun getPreference(userId: String): UserPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(preference: UserPreferenceEntity)

    @Query("DELETE FROM user_preferences WHERE userId = :userId")
    suspend fun clearPreference(userId: String)
}

@Dao
interface CheckDao {
    @Query("SELECT * FROM checks WHERE userId = :userId AND deletedAt IS NULL ORDER BY dueDate ASC")
    fun getAllChecks(userId: String): Flow<List<CheckEntity>>

    @Query("SELECT * FROM checks WHERE userId = :userId ORDER BY dueDate ASC")
    suspend fun getAllChecksList(userId: String): List<CheckEntity>

    @Query("SELECT * FROM checks WHERE userId = :userId AND id = :checkId LIMIT 1")
    suspend fun getCheckById(userId: String, checkId: Int): CheckEntity?

    @Query("SELECT * FROM checks WHERE userId = :userId AND sayadId = :sayadId AND deletedAt IS NULL LIMIT 1")
    suspend fun getCheckBySayadId(userId: String, sayadId: String): CheckEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE userId = :userId AND sourceType = 'CHECK' AND sourceId = :checkId AND deletedAt IS NULL)")
    suspend fun hasSettlementTransactionForCheck(userId: String, checkId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE userId = :userId AND sourceType = 'CHECK' AND sourceId = :checkId AND deletedAt IS NOT NULL)")
    suspend fun hasDeletedSettlementTransactionForCheck(userId: String, checkId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheck(check: CheckEntity): Long

    @Query("DELETE FROM checks WHERE userId = :userId AND id = :checkId")
    suspend fun deleteCheckById(userId: String, checkId: Int): Int

    @Update
    suspend fun updateCheck(check: CheckEntity)

    @Query("""
        UPDATE checks 
        SET type = :type, amount = :amount, amountUnit = :amountUnit, checkNumber = :checkNumber, sayadId = :sayadId, 
            bankName = :bankName, branch = :branch, holderName = :holderName, 
            receiverName = :receiverName, issueDate = :issueDate, dueDate = :dueDate, 
            status = :status, notes = :notes, accountId = :accountId, 
            reminderDateTime = :reminderDateTime, updatedAt = :updatedAt 
        WHERE id = :id AND userId = :userId
    """)
    suspend fun updateCheckWithOwner(
        id: Int,
        userId: String,
        type: String,
        amount: Long,
        amountUnit: String,
        checkNumber: String,
        sayadId: String?,
        bankName: String,
        branch: String?,
        holderName: String,
        receiverName: String,
        issueDate: Long,
        dueDate: Long,
        status: String,
        notes: String?,
        accountId: String?,
        reminderDateTime: Long?,
        updatedAt: Long
    ): Int

    @Query("""
        UPDATE checks
        SET status = 'CLEARED', accountId = :accountId, reminderDateTime = NULL, updatedAt = :updatedAt
        WHERE id = :checkId AND userId = :userId AND deletedAt IS NULL
          AND status IN ('REGISTERED', 'PENDING', 'BOUNCED')
          AND replacedByCheckId IS NULL
          AND type = :type AND amount = :amount AND amountUnit = :amountUnit
          AND checkNumber = :checkNumber AND bankName = :bankName
    """)
    suspend fun markCheckClearedForSettlement(
        checkId: Int,
        userId: String,
        type: String,
        amount: Long,
        amountUnit: String,
        checkNumber: String,
        bankName: String,
        accountId: String?,
        updatedAt: Long
    ): Int

    @Query("UPDATE checks SET status = 'REPLACED', replacedByCheckId = :replacementId, reminderDateTime = NULL, updatedAt = :updatedAt WHERE userId = :userId AND id = :checkId AND status = 'BOUNCED' AND replacedByCheckId IS NULL AND deletedAt IS NULL")
    suspend fun markCheckReplaced(userId: String, checkId: Int, replacementId: Int, updatedAt: Long): Int

    @Transaction
    suspend fun replaceBouncedCheck(userId: String, originalId: Int, replacement: CheckEntity): Int {
        val original = getCheckById(userId, originalId) ?: return 0
        if (hasSettlementTransactionForCheck(userId, originalId.toString())) return 0
        if (original.userId != userId || original.deletedAt != null ||
            original.status != "BOUNCED" || original.replacedByCheckId != null ||
            replacement.userId != userId || replacement.type != original.type ||
            replacement.amount <= 0L || replacement.checkNumber.trim() == original.checkNumber.trim() ||
            replacement.status != "REGISTERED") {
            return 0
        }
        val normalizedSayadId = replacement.sayadId?.trim().orEmpty()
        if (normalizedSayadId.isNotBlank() && getCheckBySayadId(userId, normalizedSayadId) != null) {
            return 0
        }
        val insertedId = insertCheck(replacement.copy(id = 0, replacedByCheckId = null, replacesCheckId = originalId))
        if (insertedId <= 0L) throw IllegalStateException("Replacement check insert failed")
        val updated = markCheckReplaced(userId, originalId, insertedId.toInt(), System.currentTimeMillis())
        if (updated != 1) throw IllegalStateException("Original check state changed during replacement")
        return insertedId.toInt()
    }

    @Transaction
    suspend fun rollbackCheckReplacement(userId: String, originalId: Int, replacementId: Int, originalReminderDateTime: Long?): Boolean {
        val originalRows = updateOriginalAfterReplacementRollback(userId, originalId, originalReminderDateTime, System.currentTimeMillis())
        if (originalRows != 1) throw IllegalStateException("Original check rollback failed")
        val replacementRows = deleteCheckById(userId, replacementId)
        if (replacementRows != 1) throw IllegalStateException("Replacement check rollback failed")
        return true
    }

    @Query("UPDATE checks SET status = 'BOUNCED', replacedByCheckId = NULL, reminderDateTime = :reminderDateTime, updatedAt = :updatedAt WHERE userId = :userId AND id = :originalId AND status = 'REPLACED'")
    suspend fun updateOriginalAfterReplacementRollback(userId: String, originalId: Int, reminderDateTime: Long?, updatedAt: Long): Int

    @Query("UPDATE checks SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE userId = :userId AND id = :checkId")
    suspend fun softDeleteCheck(userId: String, checkId: Int, deletedAt: Long, updatedAt: Long): Int

    @Query("SELECT * FROM checks WHERE userId = :userId AND status IN ('REGISTERED', 'PENDING', 'BOUNCED') AND deletedAt IS NULL")
    suspend fun getActiveChecks(userId: String): List<CheckEntity>

    @Query("DELETE FROM checks WHERE userId = :userId")
    suspend fun clearAllChecks(userId: String)

    @Transaction
    suspend fun settleCheckWithoutTransaction(check: CheckEntity): Boolean {
        val checkId = check.id.toString()
        if (hasSettlementTransactionForCheck(check.userId, checkId) ||
            hasDeletedSettlementTransactionForCheck(check.userId, checkId)) return false

        val current = getCheckById(check.userId, check.id) ?: return false
        if (current.userId != check.userId || current.deletedAt != null ||
            current.replacedByCheckId != null ||
            current.type != check.type ||
            current.amount != check.amount ||
            current.amountUnit != check.amountUnit) return false

        if (current.status == "CLEARED") return current.accountId == null
        if (current.status !in listOf("REGISTERED", "PENDING", "BOUNCED")) return false

        val rows = markCheckClearedForSettlement(
            checkId = check.id,
            userId = check.userId,
            type = current.type,
            amount = current.amount,
            amountUnit = current.amountUnit,
            checkNumber = current.checkNumber,
            bankName = current.bankName,
            accountId = null,
            updatedAt = check.updatedAt
        )
        if (rows != 1) return false

        val finalState = getCheckById(check.userId, check.id)
        return finalState != null && finalState.userId == check.userId &&
            finalState.deletedAt == null && finalState.status == "CLEARED" &&
            finalState.accountId == null && finalState.replacedByCheckId == null
    }

    @Transaction
    suspend fun settleCheckWithTransaction(
        check: CheckEntity,
        transaction: TransactionEntity,
        transactionDao: TransactionDao,
        accountDao: AccountDao
    ): Boolean {
        if (check.type != "RECEIVED" && check.type != "PAID") return false
        val expectedType = if (check.type == "RECEIVED") "INCOME" else "EXPENSE"
        val checkId = check.id.toString()
        val expectedTransactionId = "check_settle_${check.id}"
        val accountId = check.accountId?.takeIf { it.isNotBlank() } ?: return false

        if (check.status != "CLEARED" ||
            transaction.userId != check.userId ||
            transaction.stringId != expectedTransactionId ||
            transaction.sourceType != "CHECK" ||
            transaction.sourceId != checkId ||
            transaction.accountId != accountId ||
            transaction.amount != check.amount ||
            transaction.amountUnit != check.amountUnit ||
            transaction.type != expectedType ||
            transaction.category != (if (expectedType == "INCOME") "other_income" else "misc")) return false

        val current = getCheckById(check.userId, check.id) ?: return false
        if (current.userId != check.userId || current.deletedAt != null ||
            current.replacedByCheckId != null ||
            current.type != check.type ||
            current.amount != check.amount ||
            current.amountUnit != check.amountUnit ||
            current.checkNumber != check.checkNumber ||
            current.bankName != check.bankName) return false

        val activeAccount = accountDao.getAccountByStringId(check.userId, accountId)
            ?: return false
        if (activeAccount.userId != check.userId || !activeAccount.isActive ||
            activeAccount.deletedAt != null || activeAccount.name != transaction.accountName) return false

        // A soft-deleted settlement is historical data, not permission to restore
        // or silently create another transaction with the same business meaning.
        val historicalTx = transactionDao.getTransactionIncludingDeleted(check.userId, expectedTransactionId)
        if (historicalTx?.deletedAt != null) return false

        val existingTx = historicalTx?.takeIf { it.deletedAt == null }
        if (existingTx != null) {
            if (existingTx.userId != check.userId ||
                existingTx.sourceType != "CHECK" ||
                existingTx.sourceId != checkId ||
                existingTx.accountId != accountId ||
                existingTx.amount != check.amount ||
                existingTx.amountUnit != check.amountUnit ||
                existingTx.type != expectedType) return false

            if (current.status == "CLEARED") {
                return current.accountId == accountId
            }
            if (current.status !in listOf("REGISTERED", "PENDING", "BOUNCED")) return false

            markCheckClearedForSettlement(
                checkId = check.id,
                userId = check.userId,
                type = current.type,
                amount = current.amount,
                amountUnit = current.amountUnit,
                checkNumber = current.checkNumber,
                bankName = current.bankName,
                accountId = accountId,
                updatedAt = check.updatedAt
            )
            val finalState = getCheckById(check.userId, check.id)
            return finalState != null && finalState.userId == check.userId &&
                finalState.deletedAt == null && finalState.status == "CLEARED" &&
                finalState.accountId == accountId && finalState.replacedByCheckId == null
        }

        // A different active or deleted CHECK transaction linked to this check
        // must block a second ledger entry as well.
        if (hasSettlementTransactionForCheck(check.userId, checkId) ||
            hasDeletedSettlementTransactionForCheck(check.userId, checkId)) return false

        if (current.status !in listOf("REGISTERED", "PENDING", "BOUNCED")) return false

        val updatedRows = markCheckClearedForSettlement(
            checkId = check.id,
            userId = check.userId,
            type = current.type,
            amount = current.amount,
            amountUnit = current.amountUnit,
            checkNumber = current.checkNumber,
            bankName = current.bankName,
            accountId = accountId,
            updatedAt = check.updatedAt
        )
        if (updatedRows != 1) return false

        transactionDao.insertTransaction(transaction)

        val finalCheckState = getCheckById(check.userId, check.id)
        val finalTransaction = transactionDao.getTransactionByStringId(check.userId, expectedTransactionId)
        return finalCheckState != null && finalCheckState.userId == check.userId &&
            finalCheckState.deletedAt == null && finalCheckState.status == "CLEARED" &&
            finalCheckState.accountId == accountId && finalCheckState.replacedByCheckId == null &&
            finalTransaction != null && finalTransaction.deletedAt == null &&
            finalTransaction.sourceType == "CHECK" && finalTransaction.sourceId == checkId &&
            finalTransaction.accountId == accountId && finalTransaction.amount == check.amount &&
            finalTransaction.amountUnit == check.amountUnit && finalTransaction.type == expectedType
    }

}
