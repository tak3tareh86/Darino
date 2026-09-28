package com.example.backend.service.reminder

import com.example.backend.domain.Notification
import com.example.backend.domain.Reminder
import com.example.backend.domain.ReminderSchedule
import com.example.backend.domain.User
import com.example.backend.dto.*
import com.example.backend.exception.ApiException
import com.example.backend.exception.ErrorCode
import com.example.backend.repository.NotificationRepository
import com.example.backend.repository.ReminderRepository
import com.example.backend.repository.ReminderScheduleRepository
import com.example.backend.repository.SmsJobRepository
import com.example.backend.repository.UserRepository
import com.example.backend.service.phone.PhoneNormalizationService
import com.example.backend.service.sms.SmsQueueService
import com.example.backend.service.sms.SmsTemplate
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class ReminderService(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduleRepository: ReminderScheduleRepository,
    private val userRepository: UserRepository,
    private val smsJobRepository: SmsJobRepository,
    private val phoneNormalizationService: PhoneNormalizationService
) {
    private val log = LoggerFactory.getLogger(ReminderService::class.java)

    @Transactional(readOnly = true)
    fun getReminders(userId: UUID, status: String?): List<ReminderDto> {
        val reminders = if (status != null) {
            reminderRepository.findByUserIdAndStatus(userId, status)
        } else {
            reminderRepository.findByUserId(userId)
        }
        return reminders.map { it.toDto(phoneNormalizationService) }
    }

    @Transactional(readOnly = true)
    fun getReminderById(userId: UUID, reminderId: UUID): ReminderDto {
        val reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
            .orElseThrow { ApiException(ErrorCode.NOT_FOUND, "یادآور مورد نظر یافت نشد.") }
        return reminder.toDto(phoneNormalizationService)
    }

    @Transactional
    fun createReminder(userId: UUID, request: CreateReminderRequest): ReminderDto {
        val user = userRepository.findById(userId)
            .orElseThrow { ApiException(ErrorCode.NOT_FOUND, "کاربر یافت نشد.") }

        val canonicalPhone = request.phoneNumber?.let {
            phoneNormalizationService.normalizeIranianPhoneNumber(it)
        } ?: user.phoneNumber

        val reminder = Reminder(
            user = user,
            type = request.type,
            title = request.title,
            description = request.description,
            sourceType = request.sourceType,
            sourceId = request.sourceId,
            dueAt = request.dueAt,
            timezone = request.timezone,
            priority = request.priority,
            status = "ACTIVE",
            notificationEnabled = request.notificationEnabled,
            smsEnabled = request.smsEnabled,
            phoneNumber = canonicalPhone
        )

        val savedReminder = reminderRepository.save(reminder)

        // Generate default schedules if none provided
        val schedulesToCreate = request.schedules ?: generateDefaultSchedules(request.dueAt, request.notificationEnabled, request.smsEnabled)
        val createdSchedules = schedulesToCreate.map { scheduleReq ->
            val scheduledTime = calculateScheduledTime(request.dueAt, scheduleReq.triggerType, scheduleReq.offsetValue, scheduleReq.offsetUnit, scheduleReq.scheduledAt)
            ReminderSchedule(
                reminder = savedReminder,
                triggerType = scheduleReq.triggerType,
                offsetValue = scheduleReq.offsetValue,
                offsetUnit = scheduleReq.offsetUnit,
                scheduledAt = scheduledTime,
                notificationEnabled = scheduleReq.notificationEnabled,
                smsEnabled = scheduleReq.smsEnabled,
                status = "PENDING"
            )
        }
        reminderScheduleRepository.saveAll(createdSchedules)
        savedReminder.schedules = createdSchedules.toMutableList()

        return savedReminder.toDto(phoneNormalizationService)
    }

    @Transactional
    fun updateReminder(userId: UUID, reminderId: UUID, request: UpdateReminderRequest): ReminderDto {
        val reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
            .orElseThrow { ApiException(ErrorCode.NOT_FOUND, "یادآور یافت نشد.") }

        request.title?.let { reminder.title = it }
        request.description?.let { reminder.description = it }
        request.dueAt?.let { reminder.dueAt = it }
        request.priority?.let { reminder.priority = it }
        request.notificationEnabled?.let { reminder.notificationEnabled = it }
        request.smsEnabled?.let { reminder.smsEnabled = it }
        request.phoneNumber?.let {
            reminder.phoneNumber = phoneNormalizationService.normalizeIranianPhoneNumber(it)
        }
        reminder.updatedAt = Instant.now()

        return reminderRepository.save(reminder).toDto(phoneNormalizationService)
    }

    @Transactional
    fun deleteReminder(userId: UUID, reminderId: UUID) {
        val reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
            .orElseThrow { ApiException(ErrorCode.NOT_FOUND, "یادآور یافت نشد.") }
        reminderRepository.delete(reminder)
    }

    @Transactional
    fun completeReminder(userId: UUID, reminderId: UUID): ReminderDto {
        val reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
            .orElseThrow { ApiException(ErrorCode.NOT_FOUND, "یادآور یافت نشد.") }

        reminder.status = "COMPLETED"
        reminder.completedAt = Instant.now()
        reminder.updatedAt = Instant.now()

        // Cancel any pending schedules
        reminder.schedules.forEach { if (it.status == "PENDING") it.status = "CANCELLED" }
        return reminderRepository.save(reminder).toDto(phoneNormalizationService)
    }

    @Transactional
    fun snoozeReminder(userId: UUID, reminderId: UUID, request: SnoozeReminderRequest): ReminderDto {
        val reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
            .orElseThrow { ApiException(ErrorCode.NOT_FOUND, "یادآور یافت نشد.") }

        val newScheduledTime = request.customScheduledAt ?: Instant.now().plus(Duration.ofMinutes(request.durationMinutes))

        reminder.status = "ACTIVE"
        val snoozeSchedule = ReminderSchedule(
            reminder = reminder,
            triggerType = "EXACT_TIME",
            offsetValue = 0,
            offsetUnit = "MINUTES",
            scheduledAt = newScheduledTime,
            notificationEnabled = reminder.notificationEnabled,
            smsEnabled = reminder.smsEnabled,
            status = "PENDING"
        )
        reminderScheduleRepository.save(snoozeSchedule)
        reminder.schedules.add(snoozeSchedule)
        reminder.updatedAt = Instant.now()

        return reminderRepository.save(reminder).toDto(phoneNormalizationService)
    }

    @Transactional
    fun setReminderEnabled(userId: UUID, reminderId: UUID, enabled: Boolean): ReminderDto {
        val reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
            .orElseThrow { ApiException(ErrorCode.NOT_FOUND, "یادآور یافت نشد.") }

        reminder.status = if (enabled) "ACTIVE" else "CANCELLED"
        reminder.updatedAt = Instant.now()
        return reminderRepository.save(reminder).toDto(phoneNormalizationService)
    }

    @Transactional(readOnly = true)
    fun getReminderSmsStatus(userId: UUID, reminderId: UUID): ReminderSmsStatusDto {
        val reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
            .orElseThrow { ApiException(ErrorCode.NOT_FOUND, "یادآور یافت نشد.") }

        val jobs = smsJobRepository.findByReminderIdOrderByCreatedAtDesc(reminderId)
        return ReminderSmsStatusDto(
            reminderId = reminder.id!!,
            smsEnabled = reminder.smsEnabled,
            phoneVerified = reminder.user.phoneVerified,
            jobs = jobs.map { job ->
                SmsJobSummaryDto(
                    id = job.id!!,
                    phoneNumberMasked = phoneNormalizationService.maskPhoneNumber(job.phoneNumber),
                    status = job.status,
                    provider = job.providerName,
                    providerMessageId = job.providerMessageId,
                    attemptCount = job.attemptCount,
                    failureReason = job.failureReason,
                    queuedAt = job.queuedAt,
                    sentAt = job.sentAt,
                    deliveredAt = job.deliveredAt
                )
            }
        )
    }

    private fun generateDefaultSchedules(
        dueAt: Instant,
        notificationEnabled: Boolean,
        smsEnabled: Boolean
    ): List<CreateReminderScheduleRequest> {
        return listOf(
            CreateReminderScheduleRequest("BEFORE_DUE", 3, "DAYS", null, notificationEnabled, smsEnabled),
            CreateReminderScheduleRequest("BEFORE_DUE", 1, "DAYS", null, notificationEnabled, smsEnabled),
            CreateReminderScheduleRequest("BEFORE_DUE", 0, "DAYS", null, notificationEnabled, smsEnabled)
        )
    }

    private fun calculateScheduledTime(
        dueAt: Instant,
        triggerType: String,
        offsetValue: Int,
        offsetUnit: String,
        customScheduledAt: Instant?
    ): Instant {
        if (customScheduledAt != null) return customScheduledAt
        if (triggerType == "EXACT_TIME") return dueAt

        val unit = when (offsetUnit.uppercase()) {
            "MINUTES" -> ChronoUnit.MINUTES
            "HOURS" -> ChronoUnit.HOURS
            "DAYS" -> ChronoUnit.DAYS
            "WEEKS" -> ChronoUnit.WEEKS
            "MONTHS" -> ChronoUnit.MONTHS
            else -> ChronoUnit.DAYS
        }

        return dueAt.minus(offsetValue.toLong(), unit)
    }
}

/**
 * Background Scheduler: Evaluates due schedules and triggers notifications and SMS.
 */
@Service
class ReminderSchedulerService(
    private val reminderScheduleRepository: ReminderScheduleRepository,
    private val notificationRepository: NotificationRepository,
    private val smsQueueService: SmsQueueService
) {
    private val log = LoggerFactory.getLogger(ReminderSchedulerService::class.java)

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Transactional
    fun triggerDueSchedules() {
        val now = Instant.now()
        val dueSchedules = reminderScheduleRepository.findDueSchedules(now)

        if (dueSchedules.isEmpty()) return

        log.info("Found ${dueSchedules.size} due reminder schedules to trigger")

        for (schedule in dueSchedules) {
            val reminder = schedule.reminder
            val user = reminder.user

            // 1. Create In-App Notification if enabled
            if (schedule.notificationEnabled && reminder.notificationEnabled) {
                val notification = Notification(
                    user = user,
                    reminder = reminder,
                    title = "یادآور: ${reminder.title}",
                    message = reminder.description ?: "سررسید یادآور ${reminder.title}",
                    type = reminder.type,
                    status = "UNREAD"
                )
                notificationRepository.save(notification)
            }

            // 2. Dispatch SMS if enabled
            if (schedule.smsEnabled && reminder.smsEnabled) {
                if (user.phoneVerified && !reminder.phoneNumber.isNullOrBlank()) {
                    val template = when (reminder.type.uppercase()) {
                        "INSTALLMENT" -> SmsTemplate.INSTALLMENT_REMINDER
                        "VEHICLE", "INSURANCE", "MAINTENANCE" -> SmsTemplate.VEHICLE_REMINDER
                        else -> SmsTemplate.GENERAL_REMINDER
                    }

                    val payload = mapOf(
                        "title" to reminder.title,
                        "description" to (reminder.description ?: ""),
                        "dueAt" to reminder.dueAt.toString()
                    )

                    val idempotencyKey = "sms_rem_${reminder.id}_sch_${schedule.id}_${schedule.offsetValue}${schedule.offsetUnit}"
                    try {
                        smsQueueService.enqueueSmsJob(
                            idempotencyKey = idempotencyKey,
                            user = user,
                            reminder = reminder,
                            schedule = schedule,
                            rawPhoneNumber = reminder.phoneNumber!!,
                            template = template,
                            payload = payload
                        )
                    } catch (e: Exception) {
                        log.error("Failed to enqueue SMS for reminder ${reminder.id}", e)
                    }
                } else {
                    log.warn("SMS is enabled for reminder ${reminder.id} but user ${user.id} phone is not verified. Skipping SMS.")
                }
            }

            schedule.status = "TRIGGERED"
            schedule.lastTriggeredAt = now
            reminderScheduleRepository.save(schedule)
        }
    }
}

fun Reminder.toDto(phoneNormalizationService: PhoneNormalizationService): ReminderDto {
    return ReminderDto(
        id = this.id!!,
        type = this.type,
        title = this.title,
        description = this.description,
        sourceType = this.sourceType,
        sourceId = this.sourceId,
        dueAt = this.dueAt,
        timezone = this.timezone,
        priority = this.priority,
        status = this.status,
        notificationEnabled = this.notificationEnabled,
        smsEnabled = this.smsEnabled,
        phoneNumber = this.phoneNumber,
        phoneNumberMasked = this.phoneNumber?.let { phoneNormalizationService.maskPhoneNumber(it) },
        completedAt = this.completedAt,
        schedules = this.schedules.map { it.toDto() },
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun ReminderSchedule.toDto(): ReminderScheduleDto {
    return ReminderScheduleDto(
        id = this.id!!,
        triggerType = this.triggerType,
        offsetValue = this.offsetValue,
        offsetUnit = this.offsetUnit,
        scheduledAt = this.scheduledAt,
        notificationEnabled = this.notificationEnabled,
        smsEnabled = this.smsEnabled,
        status = this.status,
        lastTriggeredAt = this.lastTriggeredAt
    )
}
