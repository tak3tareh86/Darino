package com.financemanager.backend.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sms_jobs")
data class SmsJob(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "reminder_id")
    val reminderId: String? = null,

    @Column(name = "phone_number", nullable = false, length = 20)
    val phoneNumber: String,

    @Column(name = "template_id", nullable = false, length = 100)
    val templateId: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    var payload: String = "{}",

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    val idempotencyKey: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: SmsJobStatus = SmsJobStatus.QUEUED,

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0,

    @Column(name = "max_attempts", nullable = false)
    val maxAttempts: Int = 4,

    @Column(name = "next_retry_at")
    var nextRetryAt: Instant? = null,

    @Column(name = "provider_name", length = 50)
    var providerName: String? = null,

    @Column(name = "provider_message_id", length = 100)
    var providerMessageId: String? = null,

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    var failureReason: String? = null,

    @Column(name = "queued_at", nullable = false)
    val queuedAt: Instant = Instant.now(),

    @Column(name = "submitted_at")
    var submittedAt: Instant? = null,

    @Column(name = "sent_at")
    var sentAt: Instant? = null,

    @Column(name = "delivered_at")
    var deliveredAt: Instant? = null,

    @Column(name = "failed_at")
    var failedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "sms_logs")
data class SmsLog(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "sms_job_id")
    val smsJobId: String? = null,

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "reminder_id")
    val reminderId: String? = null,

    @Column(name = "phone_number_masked", nullable = false, length = 30)
    val phoneNumberMasked: String,

    @Column(name = "template_id", nullable = false, length = 100)
    val templateId: String,

    @Column(nullable = false, length = 50)
    val provider: String,

    @Column(name = "provider_message_id", length = 100)
    var providerMessageId: String? = null,

    @Column(nullable = false, length = 30)
    var status: String,

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    var failureReason: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

enum class SmsJobStatus {
    QUEUED,
    PROCESSING,
    SUBMITTED,
    SENT,
    DELIVERED,
    FAILED,
    EXPIRED
}

enum class SmsTemplateType(val defaultPatternId: String) {
    GENERAL_REMINDER("general_reminder"),
    INSTALLMENT_REMINDER("installment_reminder"),
    VEHICLE_REMINDER("vehicle_reminder"),
    OTP_VERIFICATION("otp_verify_code")
}
