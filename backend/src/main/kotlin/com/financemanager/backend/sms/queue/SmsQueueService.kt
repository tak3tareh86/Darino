package com.financemanager.backend.sms.queue

import com.fasterxml.jackson.databind.ObjectMapper
import com.financemanager.backend.domain.SmsJob
import com.financemanager.backend.domain.SmsJobStatus
import com.financemanager.backend.domain.SmsLog
import com.financemanager.backend.repository.SmsJobRepository
import com.financemanager.backend.repository.SmsLogRepository
import com.financemanager.backend.util.IranianPhoneNormalizer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class SmsQueueService(
    private val smsJobRepository: SmsJobRepository,
    private val smsLogRepository: SmsLogRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(SmsQueueService::class.java)

    /**
     * Enqueues an SMS message with strong idempotency protection.
     */
    @Transactional
    fun enqueueSms(
        userId: String,
        reminderId: String?,
        phoneNumber: String,
        templateId: String,
        payload: Map<String, String>,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): SmsJob {
        val normalizedPhone = IranianPhoneNormalizer.normalize(phoneNumber)
            ?: throw IllegalArgumentException("Invalid Iranian phone number: $phoneNumber")

        // 1. Check idempotency: if job already exists with this idempotency key, return it without duplicate enqueue
        val existingJob = smsJobRepository.findByIdempotencyKey(idempotencyKey)
        if (existingJob.isPresent) {
            logger.info("SMS Job with idempotency key {} already exists. Skipping duplicate queueing.", idempotencyKey)
            return existingJob.get()
        }

        val jsonPayload = objectMapper.writeValueAsString(payload)

        val smsJob = SmsJob(
            userId = userId,
            reminderId = reminderId,
            phoneNumber = normalizedPhone,
            templateId = templateId,
            payload = jsonPayload,
            idempotencyKey = idempotencyKey,
            status = SmsJobStatus.QUEUED,
            attemptCount = 0,
            maxAttempts = 4,
            queuedAt = Instant.now()
        )

        val savedJob = smsJobRepository.save(smsJob)

        // Create corresponding audit/sms log
        val maskedPhone = IranianPhoneNormalizer.mask(normalizedPhone)
        val smsLog = SmsLog(
            smsJobId = savedJob.id,
            userId = userId,
            reminderId = reminderId,
            phoneNumberMasked = maskedPhone,
            templateId = templateId,
            provider = "PENDING_QUEUE",
            status = "QUEUED",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        smsLogRepository.save(smsLog)

        logger.info("Enqueued SMS Job ID: {} for user: {} (Phone: {}, Template: {})", savedJob.id, userId, maskedPhone, templateId)
        return savedJob
    }
}
