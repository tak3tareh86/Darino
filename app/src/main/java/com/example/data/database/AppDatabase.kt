package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        InstallmentEntity::class,
        InstallmentPaymentEntity::class,
        VehicleEntity::class,
        VehicleServiceEntity::class,
        ReminderEntity::class,
        ReminderScheduleEntity::class,
        NotificationLogEntity::class,
        SmsLogEntity::class,
        UserNotificationSettingsEntity::class,
        com.example.loan.data.LoanCalculationEntity::class,
        com.example.calendar.data.FinancialEventEntity::class,
        com.example.financial_health.data.FinancialHealthEntity::class,
        com.example.reminder.data.ReminderEntity::class,
        com.example.reminder.data.ReminderScheduleEntity::class,
        com.example.reminder.data.ReminderDeliveryLogEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun reminderDao(): ReminderDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun smsLogDao(): SmsLogDao
    abstract fun loanCalculationDao(): com.example.loan.data.LoanCalculationDao
    abstract fun financialEventDao(): com.example.calendar.data.FinancialEventDao
    abstract fun financialHealthDao(): com.example.financial_health.data.FinancialHealthDao
    abstract fun smartReminderDao(): com.example.reminder.data.ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_app_database"
                )
                    .fallbackToDestructiveMigration() // Simple approach for this template
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
