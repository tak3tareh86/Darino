package com.financemanager.backend.repository

import com.financemanager.backend.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, String> {
    fun findByUsername(username: String): Optional<User>
    fun findByPhoneNumber(phoneNumber: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
    fun existsByUsername(username: String): Boolean
    fun existsByPhoneNumber(phoneNumber: String): Boolean
    fun existsByEmail(email: String): Boolean
}

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, String> {
    fun findByTokenHash(tokenHash: String): Optional<RefreshToken>
    fun deleteByUserId(userId: String): Long
}

@Repository
interface PhoneVerificationRepository : JpaRepository<PhoneVerification, String> {
    fun findTopByPhoneNumberOrderByCreatedAtDesc(phoneNumber: String): Optional<PhoneVerification>
    fun countByPhoneNumberAndCreatedAtAfter(phoneNumber: String, after: Instant): Long
    fun countByIpAddressAndCreatedAtAfter(ipAddress: String, after: Instant): Long
}

@Repository
interface ReminderRepository : JpaRepository<Reminder, String> {
    fun findAllByUserIdOrderByDueAtAsc(userId: String): List<Reminder>
    fun findAllByUserIdAndStatus(userId: String, status: ReminderStatus): List<Reminder>
    fun findByIdAndUserId(id: String, userId: String): Optional<Reminder>
    fun findBySourceTypeAndSourceId(sourceType: String, sourceId: String): Optional<Reminder>
}

@Repository
interface ReminderScheduleRepository : JpaRepository<ReminderSchedule, String> {
    fun findAllByReminderId(reminderId: String): List<ReminderSchedule>
    
    @Query("""
        SELECT s FROM ReminderSchedule s
        JOIN FETCH s.reminder r
        WHERE s.enabled = true
          AND s.executed = false
          AND s.scheduledAt <= :now
          AND r.status = 'ACTIVE'
    """)
    fun findPendingSchedulesToExecute(@Param("now") now: Instant): List<ReminderSchedule>
}

@Repository
interface NotificationRepository : JpaRepository<Notification, String> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: String, pageable: Pageable): Page<Notification>
    fun countByUserIdAndStatus(userId: String, status: NotificationStatus): Long
    fun findByIdAndUserId(id: String, userId: String): Optional<Notification>
}

@Repository
interface NotificationLogRepository : JpaRepository<NotificationLog, String>

@Repository
interface SmsJobRepository : JpaRepository<SmsJob, String> {
    fun findByIdempotencyKey(idempotencyKey: String): Optional<SmsJob>
    fun findByProviderMessageId(providerMessageId: String): Optional<SmsJob>
    fun findAllByUserIdOrderByCreatedAtDesc(userId: String, pageable: Pageable): Page<SmsJob>

    @Query("""
        SELECT j FROM SmsJob j 
        WHERE j.status = 'QUEUED' 
           OR (j.status = 'FAILED' AND j.attemptCount < j.maxAttempts AND j.nextRetryAt <= :now)
        ORDER BY j.queuedAt ASC
    """)
    fun findJobsReadyForProcessing(@Param("now") now: Instant, pageable: Pageable): List<SmsJob>
}

@Repository
interface SmsLogRepository : JpaRepository<SmsLog, String> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: String, pageable: Pageable): Page<SmsLog>
    fun findByProviderMessageId(providerMessageId: String): Optional<SmsLog>
}

@Repository
interface VehicleRepository : JpaRepository<Vehicle, String> {
    fun findAllByUserId(userId: String): List<Vehicle>
    fun findByIdAndUserId(id: String, userId: String): Optional<Vehicle>
}

@Repository
interface InstallmentRepository : JpaRepository<Installment, String> {
    fun findAllByUserId(userId: String): List<Installment>
    fun findByIdAndUserId(id: String, userId: String): Optional<Installment>
}

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, String>
