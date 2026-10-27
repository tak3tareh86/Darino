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
}
