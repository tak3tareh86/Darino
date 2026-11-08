package com.financemanager.backend.controller

import com.financemanager.backend.domain.Transaction
import com.financemanager.backend.security.UserPrincipal
import com.financemanager.backend.service.TransactionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class TransactionControllerTest {

    @Mock
    lateinit var service: TransactionService

    @Test
    fun `get by id uses authenticated principal id`() {
        val controller = TransactionController(service)
        val principal = principal("user-a")
        val transaction = transaction("user-a")

        `when`(service.getById("tx-1", "user-a")).thenReturn(transaction)

        val response = controller.one(principal, "tx-1")

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(service).getById("tx-1", "user-a")
        verify(service, never()).getById("tx-1", "user-b")
    }

    @Test
    fun `missing transaction is not found instead of leaking another users data`() {
        val controller = TransactionController(service)
        val principal = principal("user-a")

        `when`(service.getById("tx-b", "user-a")).thenReturn(null)

        val response = controller.one(principal, "tx-b")

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        verify(service).getById("tx-b", "user-a")
    }

    private fun principal(id: String) = UserPrincipal(
        id = id,
        usernameVal = id,
        phoneNumberVal = null,
        isPhoneVerifiedVal = true,
        passwordVal = "unused",
        authoritiesVal = emptyList()
    )

    private fun transaction(userId: String) = Transaction(
        id = "tx-1",
        userId = userId,
        clientId = "client-1",
        amount = 1000,
        type = "EXPENSE",
        category = "TEST",
        accountName = "TEST",
        occurredAt = Instant.parse("2026-01-01T00:00:00Z")
    )
}
