package com.example.finance

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionMigrationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var sqliteDb: SupportSQLiteDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // We build a fresh in-memory database. Room will create the v18 schema.
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        sqliteDb = db.openHelper.writableDatabase
    }

    @Test
    fun `Migration 17 to 18 preserves legacy blank stringId records`() {
        // 1. Prepare: Drop the unique index to simulate v17 state where duplicates were possible
        sqliteDb.execSQL("DROP INDEX IF EXISTS index_transactions_userId_stringId")

        // 2. Insert legacy records with empty stringId for the same user
        // According to requirement, these should NOT be treated as duplicates
        sqliteDb.execSQL("""
            INSERT INTO transactions (id, userId, amount, type, category, accountName, description, timestamp, timeFormatted, stringId, title, datePersian, paymentMethod, sourceType, isRecurring, syncState, updatedAt)
            VALUES (1, 'user_1', 1000, 'EXPENSE', 'Food', 'Card', 'Legacy 1', 1000, '10:00', '', 'Title 1', '1403/07/01', 'BANK_CARD', 'MANUAL', 0, 'SYNCED', 1000)
        """)
        sqliteDb.execSQL("""
            INSERT INTO transactions (id, userId, amount, type, category, accountName, description, timestamp, timeFormatted, stringId, title, datePersian, paymentMethod, sourceType, isRecurring, syncState, updatedAt)
            VALUES (2, 'user_1', 2000, 'EXPENSE', 'Food', 'Card', 'Legacy 2', 2000, '11:00', '', 'Title 2', '1403/07/01', 'BANK_CARD', 'MANUAL', 0, 'SYNCED', 2000)
        """)
        
        // 3. Insert a real duplicate (non-empty stringId) to ensure deduplication still works
        sqliteDb.execSQL("""
            INSERT INTO transactions (id, userId, amount, type, category, accountName, description, timestamp, timeFormatted, stringId, title, datePersian, paymentMethod, sourceType, isRecurring, syncState, updatedAt)
            VALUES (3, 'user_1', 3000, 'EXPENSE', 'Food', 'Card', 'Duplicate Old', 3000, '12:00', 'real_id', 'Title 3', '1403/07/01', 'BANK_CARD', 'MANUAL', 0, 'SYNCED', 3000)
        """)
        sqliteDb.execSQL("""
            INSERT INTO transactions (id, userId, amount, type, category, accountName, description, timestamp, timeFormatted, stringId, title, datePersian, paymentMethod, sourceType, isRecurring, syncState, updatedAt)
            VALUES (4, 'user_1', 4000, 'EXPENSE', 'Food', 'Card', 'Duplicate New', 4000, '13:00', 'real_id', 'Title 4', '1403/07/01', 'BANK_CARD', 'MANUAL', 0, 'SYNCED', 4000)
        """)

        // 4. Run the migration
        AppDatabase.MIGRATION_17_18.migrate(sqliteDb)

        // 5. Verify legacy records were preserved and updated with unique stringIds
        val cursorLegacy = sqliteDb.query("SELECT id, stringId FROM transactions WHERE id IN (1, 2)")
        assertEquals(2, cursorLegacy.count)
        while (cursorLegacy.moveToNext()) {
            val id = cursorLegacy.getInt(0)
            val stringId = cursorLegacy.getString(1)
            assertTrue("stringId should be generated for legacy record $id", stringId.startsWith("legacy_user_1_"))
            assertEquals("legacy_user_1_$id", stringId)
        }
        cursorLegacy.close()

        // 6. Verify non-empty duplicate was handled (kept the one with higher updatedAt or id)
        // Record 4 has updatedAt 4000, Record 3 has 3000. So Record 4 should stay.
        val cursorReal = sqliteDb.query("SELECT id FROM transactions WHERE stringId = 'real_id'")
        assertEquals(1, cursorReal.count)
        cursorReal.moveToFirst()
        assertEquals(4, cursorReal.getInt(0))
        cursorReal.close()

        // 7. Verify Unique Index is active (trying to insert a duplicate should fail)
        var failed = false
        try {
            sqliteDb.execSQL("""
                INSERT INTO transactions (userId, amount, type, category, accountName, description, timestamp, timeFormatted, stringId, title, datePersian, paymentMethod, sourceType, isRecurring, syncState, updatedAt)
                VALUES ('user_1', 5000, 'EXPENSE', 'Food', 'Card', 'Fail', 5000, '14:00', 'real_id', 'Fail', '1403/07/01', 'BANK_CARD', 'MANUAL', 0, 'SYNCED', 5000)
            """)
        } catch (e: Exception) {
            failed = true
        }
        assertTrue("Unique index should prevent duplicates after migration", failed)

        db.close()
    }
}
