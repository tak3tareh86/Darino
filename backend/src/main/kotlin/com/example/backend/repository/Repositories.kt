package com.example.backend.repository

import com.example.backend.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): Optional<User>
    fun findByPhoneNumber(phoneNumber: String): Optional<User>
    fun existsByEmail(email: String): Boolean
    fun existsByPhoneNumber(phoneNumber: String): Boolean
}

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByTokenHashAndRevokedFalse(tokenHash: String): Optional<RefreshToken>
    fun findByUserId(userId: UUID): List<RefreshToken>
}

@Repository
interface PhoneVerificationRepository : JpaRepository<PhoneVerification, UUID> {
    fun findTopByPhoneNumberAndStatusOrderByCreatedAtDesc(phoneNumber: String, status: String): Optional<PhoneVerification>
    fun findTopByPhoneNumberOrderByCreatedAtDesc(phoneNumber: String): Optional<PhoneVerification>
    
    @Query("SELECT COUNT(pv) FROM PhoneVerification pv WHERE pv.phoneNumber = :phoneNumber AND pv.createdAt > :since")
    fun countRequestsSince(phoneNumber: String, since: Instant): Long

    @Query("SELECT COUNT(pv) FROM PhoneVerification pv WHERE pv.ipAddress = :ipAddress AND pv.createdAt > :since")
    fun countIpRequestsSince(ipAddress: String, since: Instant): Long
}

@Repository
interface VehicleRepository : JpaRepository<Vehicle, UUID> {
    fun findByUserIdAndIsActiveTrue(userId: UUID): List<Vehicle>
    fun findByIdAndUserId(id: UUID, userId: UUID): Optional<Vehicle>
}

@Repository
interface InstallmentRepository : JpaRepository<Installment, UUID> {
    fun findByUserIdAndStatus(userId: UUID, status: String): List<Installment>
    fun findByUserId(userId: UUID): List<Installment>
    fun findByIdAndUserId(id: UUID, userId: UUID): Optional<Installment>
}

@Repository
interface ReminderRepository : JpaRepository<Reminder, UUID> {
    fun findByUserId(userId: UUID): List<Reminder>
    fun findByUserIdAndStatus(userId: UUID, status: String): List<Reminder>
    fun findByIdAndUserId(id: UUID, userId: UUID): Optional<Reminder>
    
    @Query("SELECT r FROM Reminder r WHERE r.userId = :userId AND r.dueAt BETWEEN :start AND :end")
    fun findByUserIdAndDueAtBetween(userId: UUID, start: Instant, end: Instant): List<Reminder>
}

@Repository
interface ReminderScheduleRepository : JpaRepository<ReminderSchedule, UUID> {
    fun findByReminderId(reminderId: UUID): List<ReminderSchedule>
    
    @Query("SELECT s FROM ReminderSchedule s JOIN FETCH s.reminder r WHERE s.status = 'PENDING' AND s.scheduledAt <= :now AND r.status = 'ACTIVE'")
    fun findDueSchedules(now: Instant): List<ReminderSchedule>
}

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<Notification>
    fun findByUserIdAndStatusOrderByCreatedAtDesc(userId: UUID, status: String): List<Notification>
    fun countByUserIdAndStatus(userId: UUID, status: String): Long
    fun findByIdAndUserId(id: UUID, userId: UUID): Optional<Notification>
}

@Repository
interface SmsJobRepository : JpaRepository<SmsJob, UUID> {
    fun findByIdempotencyKey(idempotencyKey: String): Optional<SmsJob>
    fun findByProviderMessageId(providerMessageId: String): Optional<SmsJob>
    
    @Query("SELECT j FROM SmsJob j WHERE j.status IN ('QUEUED', 'PROCESSING') AND (j.nextRetryAt IS NULL OR j.nextRetryAt <= :now) ORDER BY j.queuedAt ASC")
    fun findJobsToProcess(now: Instant, pageable: Pageable): List<SmsJob>
    
    fun findByReminderIdOrderByCreatedAtDesc(reminderId: UUID): List<SmsJob>
}

@Repository
interface SmsLogRepository : JpaRepository<SmsLog, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<SmsLog>
    fun findByProviderMessageId(providerMessageId: String): Optional<SmsLog>
}

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<AuditLog>
}
