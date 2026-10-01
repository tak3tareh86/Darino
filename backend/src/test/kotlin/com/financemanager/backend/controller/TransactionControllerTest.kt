package com.financemanager.backend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.financemanager.backend.domain.Transaction
import com.financemanager.backend.security.UserPrincipal
import com.financemanager.backend.service.TransactionService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.util.UUID
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ExtendWith(MockitoExtension::class)
class TransactionControllerTest {

    @Mock
    lateinit var service: TransactionService

    private lateinit var mvc: MockMvc
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.standaloneSetup(TransactionController(service)).build()
    }

    @Test
    fun `get by id passes authenticated principal id to service`() {
        val principal = principal("user-a")
        `when`(service.getById("tx-1", "user-a")).thenReturn(transaction("user-a"))

        mvc.perform(
            get("/api/v1/transactions/tx-1")
                .with(user(principal))
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)

        verify(service).getById("tx-1", "user-a")
        verify(service, never()).getById("tx-1", "user-b")
    }

    @Test
    fun `foreign transaction is returned as not found`() {
        val principal = principal("user-a")
        `when`(service.getById("tx-b", "user-a")).thenReturn(null)

        mvc.perform(
            get("/api/v1/transactions/tx-b")
                .with(user(principal))
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound)

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
        id = UUID.randomUUID().toString(),
        userId = userId,
        clientId = "client-1",
        amount = 1000,
        type = "EXPENSE",
        category = "TEST",
        accountName = "TEST",
        occurredAt = Instant.parse("2026-01-01T00:00:00Z")
    )
}
