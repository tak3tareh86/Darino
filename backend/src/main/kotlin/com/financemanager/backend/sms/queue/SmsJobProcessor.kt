package com.financemanager.backend.sms.queue

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.financemanager.backend.domain.SmsJob
import com.financemanager.backend.domain.SmsJobStatus
import com.financemanager.backend.repository.SmsJobRepository
import com.financemanager.backend.repository.SmsLogRepository
import com.financemanager.backend.sms.provider.SmsProviderFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import kotlin.math.pow
import kotlin.random.Random

@Component
class SmsJobProcessor(
    private val smsJobRepository: SmsJobRepository,
    private val smsLogRepository: SmsLogRepository,
    private val smsProviderFactory: SmsProviderFactory,
    private val objectMapper: ObjectMapper,
    @Value("\${app.sms.queue.initial-backoff-ms:10000}") private val initialBackoffMs: Long,
    @Value("\${app.sms.queue.backoff-multiplier:2.5}") private val backoffMultiplier: Double
) {
    private val logger = LoggerFactory.getLogger(SmsJobProcessor::class.java)

    /**
     * Polls ready SMS jobs and dispatches to configured Iranian/Mock SMS provider.
     */
    @Scheduled(fixedDelayString = "\${app.sms.queue.poll-interval-ms:5000}")
    fun processPendingSmsJobs() {
        val pageable = PageRequest.of(0, 20)
        val readyJobs = smsJobRepository.findJobsReadyForProcessing(Instant.now(), pageable)

        if (readyJobs.isNotEmpty()) {
            logger.debug("Processing {} pending SMS jobs from queue", readyJobs.size)
            for (job in readyJobs) {
                try {
                    processSingleJob(job.id)
                } catch (e: Exception) {
                    logger.error("Error processing SMS job {}: {}", job.id, e.message)
                }
            }
        }
    }

    @Transactional
    fun processSingleJob(jobId: String) {
        val jobOptional = smsJobRepository.findById(jobId)
        if (jobOptional.isEmpty) return
        val job = jobOptional.get()

        if (job.status != SmsJobStatus.QUEUED && job.status != SmsJobStatus.FAILED) {
            return
        }

        job.status = SmsJobStatus.PROCESSING
        job.attemptCount += 1
        smsJobRepository.saveAndFlush(job)

        val provider = smsProviderFactory.getActiveProvider()
        job.providerName = provider.providerName

        val params: Map<String, String> = try {
            objectMapper.readValue(job.payload, object : TypeReference<Map<String, String>>() {})
        } catch (e: Exception) {
            emptyMap()
        }

        val response = if (job.templateId == "otp_verify_code" && params.containsKey("token")) {
            provider.sendOtp(job.phoneNumber, params["token"] ?: "")
        } else {
            provider.sendTemplateMessage(job.phoneNumber, job.templateId, params)
        }

        if (response.success && response.providerMessageId != null) {
            job.status = SmsJobStatus.SUBMITTED
            job.providerMessageId = response.providerMessageId
            job.submittedAt = Instant.now()
            job.failureReason = null
            smsJobRepository.save(job)

            // Update SmsLog
            val logOpt = smsLogRepository.findAllByUserIdOrderByCreatedAtDesc(job.userId, PageRequest.of(0, 10))
                .content.find { it.smsJobId == job.id }
            if (logOpt != null) {
                logOpt.provider = provider.providerName
                logOpt.providerMessageId = response.providerMessageId
                logOpt.status = "SUBMITTED"
                logOpt.updatedAt = Instant.now()
                smsLogRepository.save(logOpt)
            }

            logger.info("SMS Job {} successfully submitted to provider {} (ProviderMsgId: {})", job.id, provider.providerName, response.providerMessageId)
        } else {
            // Handle Failure with Controlled Exponential Backoff
            val isRetryable = response.isRetryable && job.attemptCount < job.maxAttempts
            if (isRetryable) {
                val backoffSeconds = (initialBackoffMs / 1000.0 * backoffMultiplier.pow(job.attemptCount.toDouble())).toLong()
                val jitter = Random.nextLong(1, 5)
                val nextRetry = Instant.now().plus(Duration.ofSeconds(backoffSeconds + jitter))

                job.status = SmsJobStatus.FAILED
                job.nextRetryAt = nextRetry
                job.failureReason = response.errorMessage ?: "Temporary gateway failure"
                smsJobRepository.save(job)

                logger.warn("SMS Job {} failed temporarily on attempt {}. Next retry scheduled at {} (Reason: {})",
                    job.id, job.attemptCount, nextRetry, job.failureReason)
            } else {
                job.status = SmsJobStatus.FAILED
                job.failedAt = Instant.now()
                job.failureReason = response.errorMessage ?: "Permanent gateway failure or max retry limit reached"
                smsJobRepository.save(job)

                // Update SmsLog
                val logOpt = smsLogRepository.findAllByUserIdOrderByCreatedAtDesc(job.userId, PageRequest.of(0, 10))
                    .content.find { it.smsJobId == job.id }
                if (logOpt != null) {
                    logOpt.status = "FAILED"
                    logOpt.failureReason = job.failureReason
                    logOpt.updatedAt = Instant.now()
                    smsLogRepository.save(logOpt)
                }

                logger.error("SMS Job {} permanently failed after {} attempts. Reason: {}", job.id, job.attemptCount, job.failureReason)
            }
        }
    }
}
