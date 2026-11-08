package com.financemanager.backend

import com.fasterxml.jackson.databind.ObjectMapper
import com.financemanager.backend.domain.SmsJob
import com.financemanager.backend.domain.SmsJobStatus
import com.financemanager.backend.repository.SmsJobRepository
import com.financemanager.backend.repository.SmsLogRepository
import com.financemanager.backend.sms.provider.SmsProviderFactory
import com.financemanager.backend.sms.provider.adapter.MockSmsProviderAdapter
import com.financemanager.backend.sms.queue.SmsJobProcessor
import com.financemanager.backend.sms.queue.SmsQueueService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.util.*

class SmsQueueAndRetryTest {

    private val smsJobRepository: SmsJobRepository = mock(SmsJobRepository::class.java)
    private val smsLogRepository: SmsLogRepository = mock(SmsLogRepository::class.java)
    private val smsProviderFactory: SmsProviderFactory = mock(SmsProviderFactory::class.java)
    private val objectMapper: ObjectMapper = ObjectMapper()
    private val mockSmsProvider = MockSmsProviderAdapter()

    private lateinit var smsQueueService: SmsQueueService
    private lateinit var smsJobProcessor: SmsJobProcessor

    @BeforeEach
    fun setup() {
        `when`(smsProviderFactory.getActiveProvider()).thenReturn(mockSmsProvider)
        smsQueueService = SmsQueueService(smsJobRepository, smsLogRepository, objectMapper)
        smsJobProcessor = SmsJobProcessor(
            smsJobRepository,
            smsLogRepository,
            smsProviderFactory,
            objectMapper,
            initialBackoffMs = 1000,
            backoffMultiplier = 2.0
        )
    }

    @Test
    fun `should enqueue SMS job with idempotency`() {
        val userId = "user-123"
        val phone = "09121234567"
        val idempotencyKey = "unique-key-456"

        `when`(smsJobRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty())
        `when`(smsJobRepository.save(any(SmsJob::class.java))).thenAnswer { invocation ->
            invocation.getArgument(0) as SmsJob
        }

        val job = smsQueueService.enqueueSms(
            userId = userId,
            reminderId = "rem-1",
            phoneNumber = phone,
            templateId = "general_reminder",
            payload = mapOf("token" to "Test Reminder"),
            idempotencyKey = idempotencyKey
        )

        assertNotNull(job)
        assertEquals("+989121234567", job.phoneNumber)
        assertEquals(SmsJobStatus.QUEUED, job.status)
        assertEquals(idempotencyKey, job.idempotencyKey)
        verify(smsJobRepository, times(1)).save(any(SmsJob::class.java))
    }

    @Test
    fun `should avoid duplicate enqueue if idempotency key exists`() {
        val existingJob = SmsJob(
            id = "existing-job-id",
            userId = "user-123",
            phoneNumber = "+989121234567",
            templateId = "general_reminder",
            idempotencyKey = "duplicate-key",
            status = SmsJobStatus.QUEUED
        )

        `when`(smsJobRepository.findByIdempotencyKey("duplicate-key")).thenReturn(Optional.of(existingJob))

        val resultJob = smsQueueService.enqueueSms(
            userId = "user-123",
            reminderId = null,
            phoneNumber = "09121234567",
            templateId = "general_reminder",
            payload = emptyMap(),
            idempotencyKey = "duplicate-key"
        )

        assertEquals("existing-job-id", resultJob.id)
        verify(smsJobRepository, never()).save(any(SmsJob::class.java))
    }
}
