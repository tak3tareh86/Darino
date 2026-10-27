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
        
        runBlocking {
            repository.clearAllChecks()
        }
        
        database = AppDatabase.getDatabase(context)
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
}
