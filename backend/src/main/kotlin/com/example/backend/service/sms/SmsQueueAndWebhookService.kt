package com.example.backend.service.sms

import com.example.backend.domain.Reminder
import com.example.backend.domain.ReminderSchedule
import com.example.backend.domain.SmsJob
import com.example.backend.domain.SmsLog
import com.example.backend.domain.User
import com.example.backend.dto.SmsWebhookPayload
import com.example.backend.exception.ApiException
import com.example.backend.exception.ErrorCode
import com.example.backend.repository.ReminderRepository
import com.example.backend.repository.SmsJobRepository
import com.example.backend.repository.SmsLogRepository
import com.example.backend.service.phone.PhoneNormalizationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class SmsProviderFactory(
    @Value("\${app.sms.provider:mock}") private val configuredProvider: String,
    private val mockSmsProvider: MockSmsProvider,
    private val iranianSmsProvider: IranianSmsProviderAdapter
) {
    fun getProvider(): SmsProvider {
        return when (configuredProvider.lowercase()) {
            "iranian_gateway", "kavenegar", "farazsms", "ippanel" -> iranianSmsProvider
            "mock" -> mockSmsProvider
            else -> mockSmsProvider
        }
    }
}

@Service
class SmsQueueService(
    private val smsJobRepository: SmsJobRepository,
    private val smsLogRepository: SmsLogRepository,
    private val smsProviderFactory: SmsProviderFactory,
    private val phoneNormalizationService: PhoneNormalizationService
) {
    private val log = LoggerFactory.getLogger(SmsQueueService::class.java)

    /**
     * Enqueues an SMS job idempotently.
     * If an identical idempotency key exists, returns the existing job without creating duplicates.
     */
    @Transactional
    fun enqueueSmsJob(
        idempotencyKey: String,
        user: User,
        reminder: Reminder?,
        schedule: ReminderSchedule?,
        rawPhoneNumber: String,
        template: SmsTemplate,
        payload: Map<String, Any>
    ): SmsJob {
        // Enforce verified phone rule
        if (!user.phoneVerified) {
            log.warn("Attempted to enqueue SMS for unverified user ${user.id}. Aborting.")
            throw ApiException(ErrorCode.PHONE_NOT_VERIFIED, "شماره همراه کاربر احراز هویت نشده است.")
        }

        val existing = smsJobRepository.findByIdempotencyKey(idempotencyKey)
        if (existing.isPresent) {
            log.info("SmsJob with idempotency key '$idempotencyKey' already exists. Reusing job ID: ${existing.get().id}")
            return existing.get()
        }

        val canonicalPhone = phoneNormalizationService.normalizeIranianPhoneNumber(rawPhoneNumber)

        val job = SmsJob(
            idempotencyKey = idempotencyKey,
            user = user,
            reminder = reminder,
            schedule = schedule,
            phoneNumber = canonicalPhone,
            templateId = template.name,
            payload = payload,
            status = "QUEUED",
            attemptCount = 0,
            maxAttempts = 3,
            queuedAt = Instant.now()
        )

        return smsJobRepository.save(job)
    }

    /**
     * Background SMS Worker: Processes queued jobs and applies exponential retry backoff.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun processSmsQueue() {
        val now = Instant.now()
        val pageable = PageRequest.of(0, 20)
        val jobs = smsJobRepository.findJobsToProcess(now, pageable)

        if (jobs.isEmpty()) return

        log.debug("Processing ${jobs.size} SMS jobs from queue")
        val provider = smsProviderFactory.getProvider()

        for (job in jobs) {
            processSingleJob(job, provider)
        }
    }

    private fun processSingleJob(job: SmsJob, provider: SmsProvider) {
        job.status = "PROCESSING"
        job.attemptCount += 1
        job.submittedAt = Instant.now()
        job.providerName = provider.providerName

        try {
            val templateEnum = try {
                SmsTemplate.valueOf(job.templateId)
            } catch (e: Exception) {
                SmsTemplate.GENERAL_REMINDER
            }

            val result = provider.sendTemplateMessage(
                toPhoneNumber = job.phoneNumber,
                template = templateEnum,
                payload = job.payload
            )

            if (result.success && result.providerMessageId != null) {
                job.status = "SENT"
                job.providerMessageId = result.providerMessageId
                job.sentAt = Instant.now()
                job.failureReason = null

                createOrUpdateSmsLog(job, "SENT")
                log.info("SmsJob ${job.id} dispatched successfully. ProviderMsgId: ${result.providerMessageId}")
            } else {
                handleJobFailure(job, result.errorMessage ?: "Gateway returned unsuccessful result", result.isTemporaryError)
            }
        } catch (ex: Exception) {
            log.error("Exception processing SmsJob ${job.id}", ex)
            handleJobFailure(job, ex.message ?: "Unknown worker exception", isTemporary = true)
        }

        job.updatedAt = Instant.now()
        smsJobRepository.save(job)
    }

    private fun handleJobFailure(job: SmsJob, reason: String, isTemporary: Boolean) {
        job.failureReason = reason

        if (isTemporary && job.attemptCount < job.maxAttempts) {
            // Exponential backoff: 30s, 2m, 8m
            val backoffSeconds = Math.pow(4.0, job.attemptCount.toDouble()).toLong() * 10
            job.nextRetryAt = Instant.now().plus(Duration.ofSeconds(backoffSeconds))
            job.status = "QUEUED"
            log.warn("SmsJob ${job.id} failed (temporary). Retry scheduled in $backoffSeconds seconds. Reason: $reason")
        } else {
            job.status = "FAILED"
            job.failedAt = Instant.now()
            log.error("SmsJob ${job.id} permanently FAILED after ${job.attemptCount} attempts. Reason: $reason")
        }

        createOrUpdateSmsLog(job, job.status)
    }

    private fun createOrUpdateSmsLog(job: SmsJob, status: String) {
        val masked = phoneNormalizationService.maskPhoneNumber(job.phoneNumber)
        val logEntry = SmsLog(
            smsJob = job,
            user = job.user,
            reminder = job.reminder,
            phoneNumberMasked = masked,
            templateId = job.templateId,
            provider = job.providerName ?: "UNKNOWN",
            providerMessageId = job.providerMessageId,
            status = status,
            failureReason = job.failureReason
        )
        smsLogRepository.save(logEntry)
    }
}

@Service
class SmsWebhookService(
    private val smsJobRepository: SmsJobRepository,
    private val smsLogRepository: SmsLogRepository,
    private val smsProviderFactory: SmsProviderFactory
) {
    private val log = LoggerFactory.getLogger(SmsWebhookService::class.java)

    /**
     * Processes inbound SMS delivery reports idempotently.
     */
    @Transactional
    fun handleDeliveryWebhook(rawBody: String, signature: String?, payload: SmsWebhookPayload): Boolean {
        val provider = smsProviderFactory.getProvider()
        
        // 1. Signature validation
        if (!provider.validateWebhookSignature(rawBody, signature)) {
            log.warn("Invalid webhook signature from provider ${payload.provider}")
            return false
        }

        val providerMsgId = payload.providerMessageId
        val jobOpt = smsJobRepository.findByProviderMessageId(providerMsgId)

        if (jobOpt.isEmpty) {
            log.warn("Received webhook for unknown providerMessageId: $providerMsgId")
            return true // Acknowledge to prevent provider storm
        }

        val job = jobOpt.get()
        val normalizedStatus = payload.status.uppercase()

        // 2. Prevent invalid backwards state transition (e.g. DELIVERED cannot go back to SENT)
        if (job.status == "DELIVERED") {
            log.info("SmsJob ${job.id} is already in final DELIVERED state. Ignoring duplicate webhook.")
            return true
        }

        when (normalizedStatus) {
            "DELIVERED", "1" -> {
                job.status = "DELIVERED"
                job.deliveredAt = Instant.now()
                job.failureReason = null
                log.info("SmsJob ${job.id} marked DELIVERED by provider confirmation.")
            }
            "FAILED", "UNDELIVERED", "0" -> {
                job.status = "FAILED"
                job.failedAt = Instant.now()
                job.failureReason = payload.errorCode ?: "Provider reported delivery failure"
                log.warn("SmsJob ${job.id} marked FAILED by provider report.")
            }
            "SENT", "2" -> {
                job.status = "SENT"
                if (job.sentAt == null) job.sentAt = Instant.now()
            }
        }

        job.updatedAt = Instant.now()
        smsJobRepository.save(job)

        // Update Log record
        smsLogRepository.findByProviderMessageId(providerMsgId).ifPresent { logItem ->
            logItem.status = job.status
            logItem.failureReason = job.failureReason
            logItem.updatedAt = Instant.now()
            smsLogRepository.save(logItem)
        }

        return true
    }
}
