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
        NotificationLogEntity::class,
        SmsLogEntity::class,
        com.example.loan.data.LoanCalculationEntity::class,
        com.example.calendar.data.FinancialEventEntity::class,
        com.example.financial_health.data.FinancialHealthEntity::class,
        com.example.reminder.data.ReminderEntity::class,
        com.example.reminder.data.ReminderScheduleEntity::class,
        com.example.reminder.data.ReminderDeliveryLogEntity::class,
        ReminderEntity::class,
        ReminderScheduleEntity::class,
        VehicleExpenseRoomEntity::class,
        VehicleInsuranceRoomEntity::class,
        VehicleInspectionRoomEntity::class,
        VehicleStoreEntity::class
    ],
    version = 17,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun smsLogDao(): SmsLogDao
    abstract fun loanCalculationDao(): com.example.loan.data.LoanCalculationDao
    abstract fun financialEventDao(): com.example.calendar.data.FinancialEventDao
    abstract fun financialHealthDao(): com.example.financial_health.data.FinancialHealthDao
    abstract fun smartReminderDao(): com.example.reminder.data.ReminderDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN stringId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN subCategory TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN datePersian TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'BANK_CARD'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'MANUAL'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS financial_events")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS financial_events (
                        id TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        type TEXT NOT NULL,
                        amount INTEGER,
                        date TEXT NOT NULL,
                        time TEXT,
                        repeatType TEXT NOT NULL,
                        reminderBefore TEXT NOT NULL,
                        sourceId TEXT,
                        createdAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        PRIMARY KEY(id, userId)
                    )
                """.trimIndent())

                db.execSQL("DROP TABLE IF EXISTS financial_health_profile")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS financial_health_profile (
                        id TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        monthlyIncome INTEGER NOT NULL,
                        monthlyInstallments INTEGER NOT NULL,
                        fixedExpenses INTEGER NOT NULL,
                        financialPressure REAL NOT NULL,
                        healthStatus TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(id, userId)
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Reserved schema step for installations that received version 15 without the scoped calendar/profile tables.
            }
        }

        private val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vehicles ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE installments ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reminders ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicle_services ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicle_expenses ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicle_insurances ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicle_inspections ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicle_store ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                // Legacy child records and the global legacy store were not owner-bound.
                db.execSQL("DELETE FROM transactions")
                db.execSQL("DELETE FROM installments")
                db.execSQL("DELETE FROM reminders")
                db.execSQL("DELETE FROM vehicle_services")
                db.execSQL("DELETE FROM vehicle_expenses")
                db.execSQL("DELETE FROM vehicle_insurances")
                db.execSQL("DELETE FROM vehicle_inspections")
                db.execSQL("DELETE FROM vehicle_store")
                db.execSQL("DELETE FROM vehicles WHERE userId = '' OR userId = 'default_user'")
            }
        }

        private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notification_logs ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sms_logs ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                // Legacy notification/SMS rows were not owner-bound. Clear them rather than risk cross-account exposure.
                db.execSQL("DELETE FROM notification_logs")
                db.execSQL("DELETE FROM sms_logs")
            }
        }

        private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE smart_reminders ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                // Existing smart reminders were not owner-bound. Clear them rather than risk cross-account exposure.
                db.execSQL("DELETE FROM smart_reminder_delivery_logs")
                db.execSQL("DELETE FROM smart_reminder_schedules")
                db.execSQL("DELETE FROM smart_reminders")
            }
        }

        private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN syncState TEXT NOT NULL DEFAULT 'PENDING_UPSERT'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN deletedAt INTEGER")
            }
        }

        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `vehicle_expenses` (
                        `id` TEXT NOT NULL,
                        `vehicleId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `receiptImageUri` TEXT DEFAULT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `vehicle_insurances` (
                        `id` TEXT NOT NULL,
                        `vehicleId` TEXT NOT NULL,
                        `company` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `endDate` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `policyNumber` TEXT NOT NULL DEFAULT '',
                        `reminderDays` TEXT NOT NULL DEFAULT '30,15,7',
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `vehicle_inspections` (
                        `id` TEXT NOT NULL,
                        `vehicleId` TEXT NOT NULL,
                        `lastInspectionDate` TEXT NOT NULL,
                        `expiryDate` TEXT NOT NULL,
                        `cost` INTEGER NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'معتبر',
                        `centerName` TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_app_database"
                )
                     .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
