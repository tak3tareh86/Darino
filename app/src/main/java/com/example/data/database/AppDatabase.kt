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
        com.example.reminder.data.ReminderDeliveryLogEntity::class,
        VehicleExpenseRoomEntity::class,
        VehicleInsuranceRoomEntity::class,
        VehicleInspectionRoomEntity::class,
        VehicleStoreEntity::class
    ],
    version = 12,
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
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
