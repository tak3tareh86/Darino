package com.financemanager.backend.service

import com.financemanager.backend.domain.Transaction
import com.financemanager.backend.repository.TransactionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class TransactionServiceTest {

    @Mock
    lateinit var repository: TransactionRepository

    @Test
    fun `getById scopes lookup to authenticated user`() {
        val service = TransactionService(repository)
        val userA = "user-a"
        val userB = "user-b"
        val transaction = transaction(userB, "client-b")

        `when`(repository.findByIdAndUserIdAndDeletedAtIsNull("tx-b", userA))
            .thenReturn(Optional.empty())

        assertNull(service.getById("tx-b", userA))
        verify(repository).findByIdAndUserIdAndDeletedAtIsNull("tx-b", userA)
        verify(repository, never()).findByIdAndUserIdAndDeletedAtIsNull("tx-b", userB)
    }

    @Test
    fun `getAll scopes query to authenticated user`() {
        val service = TransactionService(repository)
        val userA = "user-a"

        `when`(repository.findAllByUserIdAndDeletedAtIsNullOrderByOccurredAtDesc(userA))
            .thenReturn(emptyList())

        assertTrue(service.getAll(userA).isEmpty())
        verify(repository).findAllByUserIdAndDeletedAtIsNullOrderByOccurredAtDesc(userA)
    }

    @Test
    fun `tombstone refuses transaction owned by another user`() {
        val service = TransactionService(repository)
        val userA = "user-a"

        `when`(repository.findByIdAndUserIdAndDeletedAtIsNull("tx-b", userA))
            .thenReturn(Optional.empty())

        assertFalse(service.tombstone("tx-b", userA))
        verify(repository, never()).save(any(Transaction::class.java))
    }

    @Test
    fun `tombstone marks only owned transaction as deleted`() {
        val service = TransactionService(repository)
        val userA = "user-a"
        val transaction = transaction(userA, "client-a")

        `when`(repository.findByIdAndUserIdAndDeletedAtIsNull("tx-a", userA))
            .thenReturn(Optional.of(transaction))

        assertTrue(service.tombstone("tx-a", userA))
        assertNotNull(transaction.deletedAt)
        assertEquals(transaction.deletedAt, transaction.updatedAt)
        verify(repository).save(transaction)
    }

    private fun transaction(userId: String, clientId: String) = Transaction(
        id = if (userId == "user-a") "tx-a" else "tx-b",
        userId = userId,
        clientId = clientId,
        amount = 1000,
        type = "EXPENSE",
        category = "TEST",
        accountName = "TEST",
        occurredAt = Instant.parse("2026-01-01T00:00:00Z")
    )
}
