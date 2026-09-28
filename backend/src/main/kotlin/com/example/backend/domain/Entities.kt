package com.example.backend.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(unique = true)
    var email: String? = null,

    @Column(name = "phone_number", unique = true)
    var phoneNumber: String? = null,

    @Column(name = "phone_verified", nullable = false)
    var phoneVerified: Boolean = false,

    @Column(name = "phone_verified_at")
    var phoneVerifiedAt: Instant? = null,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = "",

    @Column(name = "full_name")
    var fullName: String? = null,

    @Column(nullable = false)
    var timezone: String = "Asia/Tehran",

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "token_hash", nullable = false, unique = true)
    val tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(nullable = false)
    var revoked: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "phone_verifications")
data class PhoneVerification(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @Column(name = "phone_number", nullable = false)
    val phoneNumber: String,

    @Column(name = "otp_hash", nullable = false)
    var otpHash: String,

    @Column(nullable = false)
    var status: String = "PENDING", // PENDING, VERIFIED, EXPIRED, LOCKED

    @Column(name = "attempts_count", nullable = false)
    var attemptsCount: Int = 0,

    @Column(name = "max_attempts", nullable = false)
    val maxAttempts: Int = 5,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "resend_available_at", nullable = false)
    var resendAvailableAt: Instant,

    @Column(name = "verified_at")
    var verifiedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "ip_address")
    val ipAddress: String? = null
)

@Entity
@Table(name = "vehicles")
data class Vehicle(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var brand: String,

    @Column(name = "model_year")
    var modelYear: String? = null,

    @Column(name = "plate_number")
    var plateNumber: String? = null,

    @Column(name = "current_mileage", nullable = false)
    var currentMileage: Int = 0,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "installments")
data class Installment(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    var title: String,

    @Column(name = "provider_name", nullable = false)
    var providerName: String,

    @Column(nullable = false)
    var category: String = "BANK_LOAN",

    @Column(name = "total_amount", nullable = false)
    var totalAmount: BigDecimal,

    @Column(name = "installment_amount", nullable = false)
    var installmentAmount: BigDecimal,

    @Column(name = "total_installments", nullable = false)
    var totalInstallments: Int,

    @Column(name = "paid_installments", nullable = false)
    var paidInstallments: Int = 0,

    @Column(name = "remaining_installments", nullable = false)
    var remainingInstallments: Int,

    @Column(name = "next_due_date", nullable = false)
    var nextDueDate: LocalDate,

    @Column(nullable = false)
    var status: String = "ACTIVE",

    @Column
    var notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "reminders")
data class Reminder(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    var type: String, // GENERAL, INSTALLMENT, VEHICLE, INSURANCE, MAINTENANCE, FUEL

    @Column(nullable = false)
    var title: String,

    @Column
    var description: String? = null,

    @Column(name = "source_type")
    var sourceType: String? = null,

    @Column(name = "source_id")
    var sourceId: UUID? = null,

    @Column(name = "due_at", nullable = false)
    var dueAt: Instant,

    @Column(nullable = false)
    var timezone: String = "Asia/Tehran",

    @Column(nullable = false)
    var priority: String = "NORMAL", // LOW, NORMAL, HIGH, URGENT

    @Column(nullable = false)
    var status: String = "ACTIVE", // ACTIVE, COMPLETED, SNOOZED, CANCELLED

    @Column(name = "notification_enabled", nullable = false)
    var notificationEnabled: Boolean = true,

    @Column(name = "sms_enabled", nullable = false)
    var smsEnabled: Boolean = false,

    @Column(name = "phone_number")
    var phoneNumber: String? = null,

    @Column(name = "completed_at")
    var completedAt: Instant? = null,

    @OneToMany(mappedBy = "reminder", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var schedules: MutableList<ReminderSchedule> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "reminder_schedules")
data class ReminderSchedule(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reminder_id", nullable = false)
    val reminder: Reminder,

    @Column(name = "trigger_type", nullable = false)
    var triggerType: String, // BEFORE_DUE, EXACT_TIME, RECURRING

    @Column(name = "offset_value", nullable = false)
    var offsetValue: Int = 0,

    @Column(name = "offset_unit", nullable = false)
    var offsetUnit: String = "DAYS", // MINUTES, HOURS, DAYS, WEEKS, MONTHS

    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: Instant,

    @Column(name = "notification_enabled", nullable = false)
    var notificationEnabled: Boolean = true,

    @Column(name = "sms_enabled", nullable = false)
    var smsEnabled: Boolean = false,

    @Column(nullable = false)
    var status: String = "PENDING", // PENDING, TRIGGERED, SKIPPED, CANCELLED

    @Column(name = "last_triggered_at")
    var lastTriggeredAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "notifications")
data class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reminder_id")
    val reminder: Reminder? = null,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val message: String,

    @Column(nullable = false)
    val type: String,

    @Column(nullable = false)
    var status: String = "UNREAD", // UNREAD, READ, ACTIONED, EXPIRED

    @Column(name = "read_at")
    var readAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "sms_jobs")
data class SmsJob(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "idempotency_key", nullable = false, unique = true)
    val idempotencyKey: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reminder_id")
    val reminder: Reminder? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    val schedule: ReminderSchedule? = null,

    @Column(name = "phone_number", nullable = false)
    val phoneNumber: String,

    @Column(name = "template_id", nullable = false)
    val templateId: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    var payload: Map<String, Any> = mapOf(),

    @Column(nullable = false)
    var status: String = "QUEUED", // QUEUED, PROCESSING, SUBMITTED, SENT, DELIVERED, FAILED, EXPIRED

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0,

    @Column(name = "max_attempts", nullable = false)
    val maxAttempts: Int = 3,

    @Column(name = "provider_name")
    var providerName: String? = null,

    @Column(name = "provider_message_id")
    var providerMessageId: String? = null,

    @Column(name = "next_retry_at")
    var nextRetryAt: Instant? = null,

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

    @Column(name = "failure_reason")
    var failureReason: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "sms_logs")
data class SmsLog(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sms_job_id")
    val smsJob: SmsJob? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reminder_id")
    val reminder: Reminder? = null,

    @Column(name = "phone_number_masked", nullable = false)
    val phoneNumberMasked: String,

    @Column(name = "template_id", nullable = false)
    val templateId: String,

    @Column(nullable = false)
    val provider: String,

    @Column(name = "provider_message_id")
    var providerMessageId: String? = null,

    @Column(nullable = false)
    var status: String,

    @Column(name = "failure_reason")
    var failureReason: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "audit_logs")
data class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null,

    @Column(nullable = false)
    val action: String,

    @Column(name = "entity_type", nullable = false)
    val entityType: String,

    @Column(name = "entity_id")
    val entityId: String? = null,

    @Column(name = "ip_address")
    val ipAddress: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val details: Map<String, Any>? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
