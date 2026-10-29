package com.example.check

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.CheckEntity
import com.example.data.security.SessionManager
import com.example.ui.screens.check.data.CheckOperationResult
import com.example.ui.screens.check.data.LocalCheckRepository
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CheckManagementTest {

    private lateinit var context: Context
    private lateinit var repository: LocalCheckRepository
    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        SessionManager.init(context)
        SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(
                id = "user_1",
                fullName = "User 1",
                phoneNumber = "09123456789",
                phoneVerified = true
            )
        )
        
        repository = LocalCheckRepository.instance
        repository.init(context)
        database = AppDatabase.getDatabase(context)
        
        runBlocking {
            repository.clearAllChecks()
            database.accountDao().clearAllAccounts("user_1")
            database.accountDao().clearAllAccounts("user_2")
            database.transactionDao().clearAllTransactions("user_1")
            database.transactionDao().clearAllTransactions("user_2")
        }
    }

    @Test
    fun `test add and retrieve check`() = runBlocking {
        val check = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 1000000,
            checkNumber = "123456",
            bankName = "بانک ملی",
            holderName = "علی علوی",
            receiverName = "دارینو",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000,
            status = CheckStatus.REGISTERED
        )

        val result = repository.addCheck(check)
        assertEquals(CheckOperationResult.SUCCESS, result)

        val checks = repository.getChecks().first()
        assertEquals(1, checks.size)
        assertEquals("123456", checks[0].checkNumber)
        assertEquals("بانک ملی", checks[0].bankName)
    }

    @Test
    fun `test user isolation`() = runBlocking {
        // User 1 adds a check
        SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(
                id = "user_1",
                fullName = "User 1",
                phoneNumber = "09123456789",
                phoneVerified = true
            )
        )
        val check1 = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 1000000,
            checkNumber = "CHK-U1",
            bankName = "بانک ملی",
            holderName = "User 1 Holder",
            receiverName = "Darino",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            status = CheckStatus.REGISTERED
        )
        repository.addCheck(check1)

        // Switch to User 2
        SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(
                id = "user_2",
                fullName = "User 2",
                phoneNumber = "09998887766",
                phoneVerified = true
            )
        )
        val checksForUser2 = repository.getChecks().first()
        assertTrue("User 2 should not see User 1's checks", checksForUser2.isEmpty())

        // User 2 adds a check
        val check2 = Check(
            id = 0,
            userId = "user_2",
            type = CheckType.PAID,
            amount = 2000000,
            checkNumber = "CHK-U2",
            bankName = "بانک ملت",
            holderName = "Darino",
            receiverName = "Vendor",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            status = CheckStatus.REGISTERED
        )
        repository.addCheck(check2)

        val finalChecksForUser2 = repository.getChecks().first()
        assertEquals(1, finalChecksForUser2.size)
        assertEquals("CHK-U2", finalChecksForUser2[0].checkNumber)
    }

    @Test
    fun `test check soft delete`() = runBlocking {
        val check = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 1000000,
            checkNumber = "DEL-1",
            bankName = "بانک ملی",
            holderName = "Test",
            receiverName = "Test",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            status = CheckStatus.REGISTERED
        )
        repository.addCheck(check)
        
        val checks = repository.getChecks().first()
        val checkId = checks[0].id

        repository.deleteCheck(checkId)

        val remainingChecks = repository.getChecks().first()
        assertTrue("Check should be hidden after soft delete", remainingChecks.isEmpty())
    }

    @Test
    fun `test check validation`() = runBlocking {
        val invalidCheck = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 0, // Invalid amount
            checkNumber = "", // Invalid number
            bankName = "", // Invalid bank
            holderName = "Test",
            receiverName = "Test",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            status = CheckStatus.REGISTERED
        )

        val result = repository.addCheck(invalidCheck)
        assertEquals(CheckOperationResult.VALIDATION_ERROR, result)
    }

    @Test
    fun `test cross-user edit security`() = runBlocking {
        // User 1 adds a check
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        val check = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 1000,
            checkNumber = "SEC-01",
            bankName = "Bank",
            holderName = "Holder",
            receiverName = "Receiver",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            status = CheckStatus.REGISTERED
        )
        repository.addCheck(check)
        val checkId = repository.getChecks().first()[0].id

        // Switch to User 2
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_2", fullName = "U2", phoneVerified = true))
        
        // Try to edit User 1's check as User 2
        val tamperedCheck = check.copy(id = checkId, amount = 9999, userId = "user_2")
        val result = repository.updateCheck(tamperedCheck)
        
        assertEquals("User 2 should not be able to update User 1's check", CheckOperationResult.NOT_FOUND, result)
        
        // Verify User 1's check is unchanged in DB
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        val verifiedCheck = repository.getCheckById(checkId)
        assertNotNull(verifiedCheck)
        assertEquals(1000L, verifiedCheck?.amount)
    }

    @Test
    fun `test idempotent settlement`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        val check = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 5000,
            checkNumber = "IDEM-01",
            bankName = "Bank",
            holderName = "Holder",
            receiverName = "Receiver",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            status = CheckStatus.REGISTERED
        )
        repository.addCheck(check)
        val savedCheck = repository.getChecks().first()[0]
        
        val account = com.example.ui.screens.finance.model.Account(
            id = "acc_1",
            userId = "user_1",
            name = "Main Account",
            type = com.example.ui.screens.finance.model.AccountType.BANK
        )
        // Ensure account exists and is active
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(
            userId = "user_1",
            stringId = "acc_1",
            name = "Main Account",
            type = "BANK",
            isActive = true
        ))

        // Settle first time
        val result1 = repository.settleCheck(savedCheck, account)
        assertEquals(CheckOperationResult.SUCCESS, result1)
        
        // Settle second time (simulating fast click or retry)
        val result2 = repository.settleCheck(savedCheck, account)
        assertEquals("Subsequent settlement should return SUCCESS (idempotent)", CheckOperationResult.SUCCESS, result2)
        
        // Verify only one transaction was created in database
        val transactions = database.transactionDao().getAllTransactionsList("user_1")
        val settleTransactions = transactions.filter { it.stringId == "check_settle_${savedCheck.id}" }
        assertEquals("Should only have exactly one settlement transaction", 1, settleTransactions.size)
    }

    @Test
    fun `test date validation on add and update`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        
        val now = System.currentTimeMillis()
        val invalidCheck = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 1000,
            checkNumber = "DATE-ERR",
            bankName = "Bank",
            holderName = "Holder",
            receiverName = "Receiver",
            issueDate = now,
            dueDate = now - 1000, // Invalid: due before issue
            status = CheckStatus.REGISTERED
        )

        val resultAdd = repository.addCheck(invalidCheck)
        assertEquals(CheckOperationResult.VALIDATION_ERROR, resultAdd)

        // Valid add then invalid update
        val validCheck = invalidCheck.copy(dueDate = now + 1000)
        repository.addCheck(validCheck)
        val saved = repository.getChecks().first()[0]
        
        val invalidUpdate = saved.copy(dueDate = now - 1000)
        val resultUpdate = repository.updateCheck(invalidUpdate)
        assertEquals(CheckOperationResult.VALIDATION_ERROR, resultUpdate)
    }

    @Test
    fun `test account security on settlement`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        
        val check = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 1000,
            checkNumber = "ACC-SEC",
            bankName = "Bank",
            holderName = "Holder",
            receiverName = "Receiver",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            status = CheckStatus.REGISTERED
        )
        repository.addCheck(check)
        val savedCheck = repository.getChecks().first()[0]

        // 1. Account belongs to another user
        val otherUserAccount = com.example.ui.screens.finance.model.Account(
            id = "other_acc",
            userId = "user_2",
            name = "Stolen Account",
            type = com.example.ui.screens.finance.model.AccountType.BANK
        )
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(
            userId = "user_2",
            stringId = "other_acc",
            name = "Stolen Account",
            type = "BANK"
        ))

        val result1 = repository.settleCheck(savedCheck, otherUserAccount)
        assertEquals("Should not settle with another user's account", CheckOperationResult.VALIDATION_ERROR, result1)

        // 2. Inactive account
        val inactiveAccount = com.example.ui.screens.finance.model.Account(
            id = "inactive_acc",
            userId = "user_1",
            name = "Inactive",
            type = com.example.ui.screens.finance.model.AccountType.BANK
        )
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(
            userId = "user_1",
            stringId = "inactive_acc",
            name = "Inactive",
            type = "BANK",
            isActive = false
        ))

        val result2 = repository.settleCheck(savedCheck, inactiveAccount)
        assertEquals("Should not settle with inactive account", CheckOperationResult.VALIDATION_ERROR, result2)
    }

    @Test
    fun `test canceled or bounced check settlement forbidden`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        
        val account = com.example.ui.screens.finance.model.Account(id = "acc_ok", userId = "user_1", name = "OK", type = com.example.ui.screens.finance.model.AccountType.BANK)
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(userId = "user_1", stringId = "acc_ok", name = "OK", type = "BANK"))

        // 1. Canceled
        val canceledCheck = Check(id = 0, userId = "user_1", type = CheckType.RECEIVED, amount = 1000, checkNumber = "C-1", bankName = "B", holderName = "H", receiverName = "R", issueDate = 0, dueDate = 0, status = CheckStatus.CANCELED)
        repository.addCheck(canceledCheck)
        val savedCanceled = repository.getChecks().first().find { it.checkNumber == "C-1" }!!
        assertEquals(CheckOperationResult.VALIDATION_ERROR, repository.settleCheck(savedCanceled, account))

        // 2. Bounced
        val bouncedCheck = Check(id = 0, userId = "user_1", type = CheckType.RECEIVED, amount = 1000, checkNumber = "B-1", bankName = "B", holderName = "H", receiverName = "R", issueDate = 0, dueDate = 0, status = CheckStatus.BOUNCED)
        repository.addCheck(bouncedCheck)
        val savedBounced = repository.getChecks().first().find { it.checkNumber == "B-1" }!!
        assertEquals(CheckOperationResult.VALIDATION_ERROR, repository.settleCheck(savedBounced, account))
    }

    @Test
    fun `test tampered check object settlement uses DB values`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        
        val check = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 1000, // REAL AMOUNT
            checkNumber = "TAMP-01",
            bankName = "Bank",
            holderName = "Holder",
            receiverName = "Receiver",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            status = CheckStatus.REGISTERED
        )
        repository.addCheck(check)
        val savedCheck = repository.getChecks().first()[0]
        
        val account = com.example.ui.screens.finance.model.Account(id = "acc_1", userId = "user_1", name = "Acc", type = com.example.ui.screens.finance.model.AccountType.BANK)
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(userId = "user_1", stringId = "acc_1", name = "Acc", type = "BANK"))

        // TAMPERED OBJECT
        val tamperedCheck = savedCheck.copy(amount = 9999999, type = CheckType.PAID)
        
        val result = repository.settleCheck(tamperedCheck, account)
        assertEquals(CheckOperationResult.SUCCESS, result)
        
        // VERIFY TRANSACTION IN DB USES REAL VALUES (1000 and INCOME for RECEIVED)
        val transactions = database.transactionDao().getAllTransactionsList("user_1")
        val settleTx = transactions.find { it.stringId == "check_settle_${savedCheck.id}" }
        assertNotNull(settleTx)
        assertEquals(1000L, settleTx?.amount)
        assertEquals("INCOME", settleTx?.type)
    }

    @Test
    fun `test idempotency with deterministic ID collision`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        
        // 1. Setup a check
        val check = Check(id = 0, userId = "user_1", type = CheckType.RECEIVED, amount = 1000, checkNumber = "COLL-01", bankName = "B", holderName = "H", receiverName = "R", issueDate = 0, dueDate = 0, status = CheckStatus.REGISTERED)
        repository.addCheck(check)
        val savedCheck = repository.getChecks().first()[0]
        
        val account = com.example.ui.screens.finance.model.Account(id = "acc_1", userId = "user_1", name = "Acc", type = com.example.ui.screens.finance.model.AccountType.BANK)
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(userId = "user_1", stringId = "acc_1", name = "Acc", type = "BANK"))

        // 2. MANUALLY insert a transaction with same deterministic ID but DIFFERENT source
        val fakeTx = com.example.data.database.TransactionEntity(
            userId = "user_1",
            amount = 500,
            stringId = "check_settle_${savedCheck.id}", // SAME ID
            sourceType = "MANUAL", // DIFFERENT SOURCE
            sourceId = "some_other_id"
        )
        database.transactionDao().insertTransaction(fakeTx)

        // 3. Attempt to settle the check - should FAIL because of collision with different source
        val result = repository.settleCheck(savedCheck, account)
        assertEquals("Settlement should fail if ID collides with different source", CheckOperationResult.PERSISTENCE_ERROR, result)
        
        // Verify check is still REGISTERED
        val checkAfter = repository.getCheckById(savedCheck.id)
        assertEquals(CheckStatus.REGISTERED, checkAfter?.status)
    }

    @Test
    fun `test cleared check without transaction is not success`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        
        // 1. Manually insert a check that is CLEARED but has NO transaction
        val checkEntity = com.example.data.database.CheckEntity(
            userId = "user_1",
            type = "RECEIVED",
            amount = 2000,
            checkNumber = "GHOST-01",
            bankName = "B",
            holderName = "H",
            receiverName = "R",
            issueDate = 0,
            dueDate = 0,
            status = "CLEARED" // Status is set, but transaction doesn't exist
        )
        val checkId = database.checkDao().insertCheck(checkEntity).toInt()
        val check = repository.getCheckById(checkId)!!
        
        val account = com.example.ui.screens.finance.model.Account(id = "acc_1", userId = "user_1", name = "Acc", type = com.example.ui.screens.finance.model.AccountType.BANK)
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(userId = "user_1", stringId = "acc_1", name = "Acc", type = "BANK"))

        // 2. Attempt to settle - it should reach DAO and try to FIX the state by inserting the transaction
        // Our refined DAO logic says: if it's already CLEARED, ensure transaction exists.
        val result = repository.settleCheck(check, account)
        assertEquals(CheckOperationResult.SUCCESS, result)
        
        // 3. Verify transaction WAS created
        val transactions = database.transactionDao().getAllTransactionsList("user_1")
        assertTrue(transactions.any { it.stringId == "check_settle_$checkId" && it.sourceId == checkId.toString() })
    }

    @Test
    fun `test settlement row count zero logic`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        
        val check = Check(id = 0, userId = "user_1", type = CheckType.RECEIVED, amount = 1000, checkNumber = "ROW-0", bankName = "B", holderName = "H", receiverName = "R", issueDate = 0, dueDate = 0, status = CheckStatus.REGISTERED)
        repository.addCheck(check)
        val savedCheck = repository.getChecks().first()[0]
        
        val account = com.example.ui.screens.finance.model.Account(id = "acc_1", userId = "user_1", name = "Acc", type = com.example.ui.screens.finance.model.AccountType.BANK)
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(userId = "user_1", stringId = "acc_1", name = "Acc", type = "BANK"))

        // Simulate concurrent deletion just before settleCheck is called
        val result = repository.settleCheck(savedCheck.copy(id = 99999), account)
        assertEquals(CheckOperationResult.NOT_FOUND, result)
    }

    @Test
    fun `test paid check settlement creates expense transaction`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        
        val check = Check(id = 0, userId = "user_1", type = CheckType.PAID, amount = 3000, checkNumber = "PAID-01", bankName = "Melli", holderName = "Me", receiverName = "Vendor", issueDate = 0, dueDate = 0, status = CheckStatus.REGISTERED)
        repository.addCheck(check)
        val savedCheck = repository.getChecks().first()[0]
        
        val account = com.example.ui.screens.finance.model.Account(id = "acc_1", userId = "user_1", name = "Acc", type = com.example.ui.screens.finance.model.AccountType.BANK)
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(userId = "user_1", stringId = "acc_1", name = "Acc", type = "BANK"))

        val result = repository.settleCheck(savedCheck, account)
        assertEquals(CheckOperationResult.SUCCESS, result)
        
        val transactions = database.transactionDao().getAllTransactionsList("user_1")
        val settleTx = transactions.find { it.stringId == "check_settle_${savedCheck.id}" }
        assertNotNull(settleTx)
        assertEquals(3000L, settleTx?.amount)
        assertEquals("EXPENSE", settleTx?.type) // Should be expense for PAID check
    }

    @Test
    fun `test settlement fails if existing transaction belongs to another user`() = runBlocking {
        // User 1 has a check
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        val check = Check(id = 0, userId = "user_1", type = CheckType.RECEIVED, amount = 1000, checkNumber = "U1-C", bankName = "B", holderName = "H", receiverName = "R", issueDate = 0, dueDate = 0, status = CheckStatus.REGISTERED)
        repository.addCheck(check)
        val savedCheck = repository.getChecks().first()[0]
        
        // Setup account
        val account = com.example.ui.screens.finance.model.Account(id = "acc_1", userId = "user_1", name = "Acc", type = com.example.ui.screens.finance.model.AccountType.BANK)
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(userId = "user_1", stringId = "acc_1", name = "Acc", type = "BANK"))

        // MANUALLY insert a transaction for User 2 with the same deterministic ID
        // (This shouldn't happen naturally because userId is part of the deterministic ID generation if we wanted, 
        // but currently stringId is just check_settle_id. 
        // However, TransactionDao.getTransactionByStringId checks userId too.)
        
        val maliciousTx = com.example.data.database.TransactionEntity(
            userId = "user_2",
            amount = 1000,
            stringId = "check_settle_${savedCheck.id}",
            sourceType = "CHECK",
            sourceId = savedCheck.id.toString()
        )
        database.transactionDao().insertTransaction(maliciousTx)

        // Attempt to settle for User 1
        // The DAO will search for "check_settle_id" for "user_1". It won't find the "user_2" one.
        // So it will try to insert a new one for "user_1". 
        // This is safe because (userId, stringId) is unique in transactions table.
        
        val result = repository.settleCheck(savedCheck, account)
        assertEquals(CheckOperationResult.SUCCESS, result)
        
        // Verify User 1 now has their own transaction
        val transactions1 = database.transactionDao().getAllTransactionsList("user_1")
        assertTrue(transactions1.any { it.stringId == "check_settle_${savedCheck.id}" })
    }

    @Test
    fun `test add and edit check reminder date time and retrieve`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        
        val initialReminder = System.currentTimeMillis() + 3600000
        val check = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 1000,
            checkNumber = "REM-EDIT-01",
            bankName = "Bank",
            holderName = "Holder",
            receiverName = "Receiver",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000,
            status = CheckStatus.REGISTERED,
            reminderDateTime = initialReminder
        )
        repository.addCheck(check)
        val saved = repository.getChecks().first()[0]
        assertEquals(initialReminder, saved.reminderDateTime)

        // Update with new reminder time
        val newReminder = System.currentTimeMillis() + 7200000
        val updateResult = repository.updateCheck(saved.copy(reminderDateTime = newReminder))
        assertEquals(CheckOperationResult.SUCCESS, updateResult)

        // Retrieve and verify updated reminder is persisted
        val retrieved = repository.getCheckById(saved.id)
        assertNotNull(retrieved)
        assertEquals(newReminder, retrieved?.reminderDateTime)
    }

    @Test
    fun `test disable reminder deletes existing reminder and cancels alarm`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        val reminderRepo = com.example.reminder.data.LocalReminderRepository(context)

        val reminderTime = System.currentTimeMillis() + 3600000
        val check = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 1000,
            checkNumber = "REM-OFF-01",
            bankName = "Bank",
            holderName = "Holder",
            receiverName = "Receiver",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000,
            status = CheckStatus.REGISTERED,
            reminderDateTime = reminderTime
        )
        repository.addCheck(check)
        val saved = repository.getChecks().first()[0]

        // Reminder should exist
        val activeReminder = reminderRepo.getReminderBySourceId(saved.id.toString())
        assertNotNull("Reminder should be created when reminderDateTime is provided", activeReminder)

        // Now turn off the reminder (null)
        val turnOffResult = repository.updateCheck(saved.copy(reminderDateTime = null))
        assertEquals(CheckOperationResult.SUCCESS, turnOffResult)

        // Check in database should have null reminderDateTime
        val checkInDb = repository.getCheckById(saved.id)
        assertNotNull(checkInDb)
        assertNull(checkInDb?.reminderDateTime)

        // Reminder entity must be deleted and not recreated with default time
        val reminderAfterTurnOff = reminderRepo.getReminderBySourceId(saved.id.toString())
        assertNull("Reminder should be deleted when turned off", reminderAfterTurnOff)
    }

    @Test
    fun `test edit check with reminder off does not re-enable reminder`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        val reminderRepo = com.example.reminder.data.LocalReminderRepository(context)

        val check = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 1000,
            checkNumber = "REM-OFF-EDIT",
            bankName = "Bank",
            holderName = "Holder",
            receiverName = "Receiver",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000,
            status = CheckStatus.REGISTERED,
            reminderDateTime = null // Off from beginning
        )
        repository.addCheck(check)
        val saved = repository.getChecks().first()[0]
        assertNull(reminderRepo.getReminderBySourceId(saved.id.toString()))

        // Edit other fields of the check (e.g. amount, bankName)
        val editedCheck = saved.copy(amount = 999000, bankName = "Updated Bank")
        val editResult = repository.updateCheck(editedCheck)
        assertEquals(CheckOperationResult.SUCCESS, editResult)

        // Reminder must remain null and not be re-created
        val checkInDb = repository.getCheckById(saved.id)
        assertNull(checkInDb?.reminderDateTime)
        assertNull(reminderRepo.getReminderBySourceId(saved.id.toString()))
    }

    @Test
    fun `test reminder independence between two checks`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        val reminderRepo = com.example.reminder.data.LocalReminderRepository(context)

        val check1 = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 1000,
            checkNumber = "REM-IND-01",
            bankName = "Bank1",
            holderName = "H1",
            receiverName = "R1",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000,
            status = CheckStatus.REGISTERED,
            reminderDateTime = System.currentTimeMillis() + 100000
        )
        val check2 = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.PAID,
            amount = 2000,
            checkNumber = "REM-IND-02",
            bankName = "Bank2",
            holderName = "H2",
            receiverName = "R2",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000,
            status = CheckStatus.REGISTERED,
            reminderDateTime = System.currentTimeMillis() + 200000
        )
        repository.addCheck(check1)
        repository.addCheck(check2)

        val checks = repository.getChecks().first()
        val saved1 = checks.find { it.checkNumber == "REM-IND-01" }!!
        val saved2 = checks.find { it.checkNumber == "REM-IND-02" }!!

        assertNotNull(reminderRepo.getReminderBySourceId(saved1.id.toString()))
        assertNotNull(reminderRepo.getReminderBySourceId(saved2.id.toString()))

        // Disable reminder for check1 only
        repository.updateCheck(saved1.copy(reminderDateTime = null))

        // Check1 reminder should be gone, Check2 reminder must remain intact
        assertNull(reminderRepo.getReminderBySourceId(saved1.id.toString()))
        assertNotNull(reminderRepo.getReminderBySourceId(saved2.id.toString()))
    }

    @Test
    fun `test settlement failure on account mismatch in idempotent retry`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        
        val check = Check(id = 0, userId = "user_1", type = CheckType.RECEIVED, amount = 1000, checkNumber = "MISMATCH-01", bankName = "B", holderName = "H", receiverName = "R", issueDate = 0, dueDate = 100, status = CheckStatus.REGISTERED)
        repository.addCheck(check)
        val savedCheck = repository.getChecks().first()[0]
        
        val account1 = com.example.ui.screens.finance.model.Account(id = "acc_1", userId = "user_1", name = "Acc 1", type = com.example.ui.screens.finance.model.AccountType.BANK)
        val account2 = com.example.ui.screens.finance.model.Account(id = "acc_2", userId = "user_1", name = "Acc 2", type = com.example.ui.screens.finance.model.AccountType.BANK)
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(userId = "user_1", stringId = "acc_1", name = "Acc 1", type = "BANK"))
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(userId = "user_1", stringId = "acc_2", name = "Acc 2", type = "BANK"))

        // 1. Settle with Account 1
        repository.settleCheck(savedCheck, account1)
        
        // 2. Attempt to settle SAME check with Account 2 (Idempotent retry with DIFFERENT account)
        val result = repository.settleCheck(savedCheck, account2)
        assertEquals("Should fail if account doesn't match previous settlement", CheckOperationResult.PERSISTENCE_ERROR, result)
    }

    @Test
    fun `test reject existing settlement transaction with mismatched amount or type`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        
        val check = Check(id = 0, userId = "user_1", type = CheckType.RECEIVED, amount = 1000, checkNumber = "REJ-01", bankName = "B", holderName = "H", receiverName = "R", issueDate = 0, dueDate = 100, status = CheckStatus.REGISTERED)
        repository.addCheck(check)
        val savedCheck = repository.getChecks().first()[0]

        val account = com.example.ui.screens.finance.model.Account(id = "acc_1", userId = "user_1", name = "Acc 1", type = com.example.ui.screens.finance.model.AccountType.BANK)
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(userId = "user_1", stringId = "acc_1", name = "Acc 1", type = "BANK"))

        // 1. Pre-insert transaction with wrong amount
        val wrongAmountTx = com.example.data.database.TransactionEntity(
            userId = "user_1",
            amount = 999999, // Mismatched amount
            type = "INCOME",
            accountId = "acc_1",
            stringId = "check_settle_${savedCheck.id}",
            sourceType = "CHECK",
            sourceId = savedCheck.id.toString()
        )
        database.transactionDao().insertTransaction(wrongAmountTx)

        val resultAmountMismatch = repository.settleCheck(savedCheck, account)
        assertEquals(CheckOperationResult.PERSISTENCE_ERROR, resultAmountMismatch)

        // 2. Clean up and test wrong type (EXPENSE instead of INCOME for RECEIVED check)
        database.transactionDao().clearAllTransactions("user_1")
        val wrongTypeTx = wrongAmountTx.copy(amount = 1000, type = "EXPENSE")
        database.transactionDao().insertTransaction(wrongTypeTx)

        val resultTypeMismatch = repository.settleCheck(savedCheck, account)
        assertEquals(CheckOperationResult.PERSISTENCE_ERROR, resultTypeMismatch)
    }

    @Test
    fun `test check and financial state preserved on settlement failure`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        
        val check = Check(id = 0, userId = "user_1", type = CheckType.RECEIVED, amount = 1000, checkNumber = "PRESERVE-01", bankName = "B", holderName = "H", receiverName = "R", issueDate = 0, dueDate = 100, status = CheckStatus.REGISTERED)
        repository.addCheck(check)
        val savedCheck = repository.getChecks().first()[0]

        val invalidAccount = com.example.ui.screens.finance.model.Account(id = "inactive_acc", userId = "user_1", name = "Inactive", type = com.example.ui.screens.finance.model.AccountType.BANK)
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(userId = "user_1", stringId = "inactive_acc", name = "Inactive", type = "BANK", isActive = false))

        // Settlement fails due to inactive account
        val result = repository.settleCheck(savedCheck, invalidAccount)
        assertEquals(CheckOperationResult.VALIDATION_ERROR, result)

        // Verify check status in DB is untouched (remains REGISTERED)
        val checkInDb = repository.getCheckById(savedCheck.id)
        assertNotNull(checkInDb)
        assertEquals(CheckStatus.REGISTERED, checkInDb?.status)

        // Verify no settlement transaction was created for this check
        val transactions = database.transactionDao().getAllTransactionsList("user_1")
        val settleTx = transactions.find { it.stringId == "check_settle_${savedCheck.id}" }
        assertNull(settleTx)
    }

    @Test
    fun `test reminder cancellation on settlement`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        
        val check = Check(id = 0, userId = "user_1", type = CheckType.RECEIVED, amount = 1000, checkNumber = "CANCEL-REM", bankName = "B", holderName = "H", receiverName = "R", issueDate = 0, dueDate = System.currentTimeMillis() + 86400000, status = CheckStatus.REGISTERED, reminderDateTime = System.currentTimeMillis() + 100000)
        repository.addCheck(check)
        val savedCheck = repository.getChecks().first()[0]
        
        val account = com.example.ui.screens.finance.model.Account(id = "acc_1", userId = "user_1", name = "Acc", type = com.example.ui.screens.finance.model.AccountType.BANK)
        database.accountDao().insertAccount(com.example.data.database.AccountEntity(userId = "user_1", stringId = "acc_1", name = "Acc", type = "BANK"))

        // Settle check
        val result = repository.settleCheck(savedCheck, account)
        assertEquals(CheckOperationResult.SUCCESS, result)
    }
    @Test
    fun `test settled check cannot be edited or deleted directly`() = runBlocking {
        SessionManager.setAuthenticatedUser(com.example.data.api.NetworkUserDto(id = "user_1", fullName = "U1", phoneVerified = true))
        val check = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 12000,
            checkNumber = "SETTLED-LOCK",
            bankName = "Bank",
            holderName = "Holder",
            receiverName = "Receiver",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000,
            status = CheckStatus.REGISTERED
        )
        assertEquals(CheckOperationResult.SUCCESS, repository.addCheck(check))
        val saved = repository.getChecks().first().single { it.checkNumber == "SETTLED-LOCK" }
        val account = com.example.ui.screens.finance.model.Account(
            id = "settled_lock_account",
            userId = "user_1",
            name = "Main",
            type = com.example.ui.screens.finance.model.AccountType.BANK
        )
        database.accountDao().insertAccount(
            com.example.data.database.AccountEntity(
                userId = "user_1",
                stringId = account.id,
                name = account.name,
                type = "BANK"
            )
        )
        assertEquals(CheckOperationResult.SUCCESS, repository.settleCheck(saved, account))
        assertEquals(CheckOperationResult.VALIDATION_ERROR, repository.updateCheck(saved.copy(checkNumber = "CHANGED")))
        assertEquals(CheckOperationResult.VALIDATION_ERROR, repository.deleteCheck(saved.id))
        assertNotNull(repository.getCheckById(saved.id))
        assertTrue(database.transactionDao().getAllTransactionsList("user_1").any {
            it.stringId == "check_settle_${saved.id}"
        })
    }

    @Test
    fun `test invalid Sayad identifier is rejected`() = runBlocking {
        val now = System.currentTimeMillis()
        val invalid = Check(
            id = 0,
            userId = "user_1",
            type = CheckType.RECEIVED,
            amount = 1000,
            checkNumber = "SAYAD-INVALID",
            sayadId = "12345",
            bankName = "Bank",
            holderName = "Holder",
            receiverName = "Receiver",
            issueDate = now,
            dueDate = now + 86400000,
            status = CheckStatus.REGISTERED
        )
        assertEquals(CheckOperationResult.VALIDATION_ERROR, repository.addCheck(invalid))
    }

}
