package com.example.backend

import com.example.backend.domain.User
import com.example.backend.dto.SmsWebhookPayload
import com.example.backend.exception.ApiException
import com.example.backend.repository.SmsJobRepository
import com.example.backend.repository.SmsLogRepository
import com.example.backend.service.phone.PhoneNormalizationService
import com.example.backend.service.sms.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.util.Optional
import java.util.UUID

class SmsQueueAndWebhookTest {

    private val smsJobRepository = mock(SmsJobRepository::class.java)
    private val smsLogRepository = mock(SmsLogRepository::class.java)
    private val mockSmsProvider = MockSmsProvider()
    private val phoneNormalizationService = PhoneNormalizationService()

    private val smsProviderFactory = SmsProviderFactory(
        configuredProvider = "mock",
        mockSmsProvider = mockSmsProvider,
        iranianSmsProvider = mock(IranianSmsProviderAdapter::class.java)
    )

    private lateinit var queueService: SmsQueueService
    private lateinit var webhookService: SmsWebhookService

    @Before
    fun setup() {
        queueService = SmsQueueService(
            smsJobRepository = smsJobRepository,
            smsLogRepository = smsLogRepository,
            smsProviderFactory = smsProviderFactory,
            phoneNormalizationService = phoneNormalizationService
        )

        webhookService = SmsWebhookService(
            smsJobRepository = smsJobRepository,
            smsLogRepository = smsLogRepository,
            smsProviderFactory = smsProviderFactory
        )
    }

    @Test
    fun testUnverifiedUserCannotEnqueueSms() {
        val unverifiedUser = User(
            id = UUID.randomUUID(),
            email = "test@example.com",
            phoneNumber = "+989121234567",
            phoneVerified = false,
            passwordHash = "hash"
        )

        assertThrows(ApiException::class.java) {
            queueService.enqueueSmsJob(
                idempotencyKey = "key_1",
                user = unverifiedUser,
                reminder = null,
                schedule = null,
                rawPhoneNumber = "09121234567",
                template = SmsTemplate.GENERAL_REMINDER,
                payload = emptyMap()
            )
        }
    }

    @Test
    fun testDuplicateWebhookDoesNotChangeDeliveredState() {
        val verifiedUser = User(
            id = UUID.randomUUID(),
            email = "user@test.com",
            phoneVerified = true,
            passwordHash = "hash"
        )

        val job = com.example.backend.domain.SmsJob(
            id = UUID.randomUUID(),
            idempotencyKey = "idemp_1",
            user = verifiedUser,
            phoneNumber = "+989121234567",
            templateId = "GENERAL_REMINDER",
            status = "DELIVERED",
            providerMessageId = "prov_123"
        )

        `when`(smsJobRepository.findByProviderMessageId("prov_123")).thenReturn(Optional.of(job))

        val duplicateWebhook = SmsWebhookPayload(
            provider = "MOCK",
            providerMessageId = "prov_123",
            status = "SENT"
        )

        val processed = webhookService.handleDeliveryWebhook("raw", null, duplicateWebhook)
        assertTrue(processed)
        assertEquals("DELIVERED", job.status) // State must remain DELIVERED
    }
}
