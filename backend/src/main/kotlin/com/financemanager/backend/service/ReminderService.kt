package com.financemanager.backend.service

import com.financemanager.backend.common.AppException
import com.financemanager.backend.common.ErrorCode
import com.financemanager.backend.domain.*
import com.financemanager.backend.repository.ReminderRepository
import com.financemanager.backend.repository.ReminderScheduleRepository
import com.financemanager.backend.repository.SmsJobRepository
import com.financemanager.backend.repository.UserRepository
import com.financemanager.backend.sms.queue.SmsQueueService
import com.financemanager.backend.util.IranianPhoneNormalizer
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class CreateReminderCommand(
    val type: ReminderType,
    val title: String,
    val description: String?,
    val dueAt: Instant,
    val timezone: String = "Asia/Tehran",
    val priority: Priority = Priority.NORMAL,
    val notificationEnabled: Boolean = true,
    val smsEnabled: Boolean = false,
    val phoneNumber: String? = null,
    val sourceType: String? = null,
    val sourceId: String? = null,
    val repeatRule: String = "NONE",
    val schedules: List<CreateScheduleCommand> = emptyList()
)

data class CreateScheduleCommand(
    val triggerType: TriggerType,
    val offsetValue: Int,
    val offsetUnit: OffsetUnit
)

@Service
class ReminderService(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduleRepository: ReminderScheduleRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val smsQueueService: SmsQueueService,
    private val smsJobRepository: SmsJobRepository
) {
    private val logger = LoggerFactory.getLogger(ReminderService::class.java)

    @Transactional(readOnly = true)
    fun getUserReminders(userId: String): List<Reminder> {
        return reminderRepository.findAllByUserIdOrderByDueAtAsc(userId)
    }

    @Transactional(readOnly = true)
    fun getReminderById(id: String, userId: String): Reminder {
        return reminderRepository.findByIdAndUserId(id, userId)
            .orElseThrow { AppException(ErrorCode.NOT_FOUND, "یادآور با شناسه $id یافت نشد.") }
    }

    @Transactional
    fun createReminder(userId: String, command: CreateReminderCommand): Reminder {
        val user = userRepository.findById(userId)
            .orElseThrow { AppException(ErrorCode.NOT_FOUND, "کاربر یافت نشد.") }

        val phoneToUse = if (command.smsEnabled) {
            val phone = command.phoneNumber ?: user.phoneNumber
            if (phone.isNullOrBlank() || !user.phoneVerified) {
                throw AppException(ErrorCode.PHONE_NOT_VERIFIED, "برای فعال‌سازی ارسال پیامک، باید ابتدا شماره همراه خود را با کد تأیید احراز نمایید.")
            }
            IranianPhoneNormalizer.normalize(phone)
        } else {
            command.phoneNumber?.let { IranianPhoneNormalizer.normalize(it) }
        }

        val reminder = Reminder(
            userId = userId,
            type = command.type,
            title = command.title,
            description = command.description,
            dueAt = command.dueAt,
            timezone = command.timezone,
            priority = command.priority,
            status = ReminderStatus.ACTIVE,
            notificationEnabled = command.notificationEnabled,
            smsEnabled = command.smsEnabled,
            phoneNumber = phoneToUse,
            sourceType = command.sourceType,
            sourceId = command.sourceId,
            repeatRule = command.repeatRule
        )

        val savedReminder = reminderRepository.save(reminder)

        // Generate schedules
        val scheduleEntities = if (command.schedules.isNotEmpty()) {
            command.schedules.map { sCmd ->
                val scheduledAt = calculateScheduledTime(command.dueAt, sCmd.triggerType, sCmd.offsetValue, sCmd.offsetUnit)
                ReminderSchedule(
                    reminder = savedReminder,
                    triggerType = sCmd.triggerType,
                    offsetValue = sCmd.offsetValue,
                    offsetUnit = sCmd.offsetUnit,
                    scheduledAt = scheduledAt,
                    enabled = true,
                    executed = false
                )
            }
        } else {
            // Default 1 schedule: at exact time
            listOf(
                ReminderSchedule(
                    reminder = savedReminder,
                    triggerType = TriggerType.EXACT_TIME,
                    offsetValue = 0,
                    offsetUnit = OffsetUnit.MINUTES,
                    scheduledAt = command.dueAt,
                    enabled = true,
                    executed = false
                )
            )
        }

        savedReminder.schedules.addAll(scheduleEntities)
        val finalReminder = reminderRepository.save(savedReminder)

        logger.info("Reminder created: {} with {} schedules", finalReminder.id, scheduleEntities.size)
        return finalReminder
    }

    @Transactional
    fun updateReminder(id: String, userId: String, command: CreateReminderCommand): Reminder {
        val reminder = getReminderById(id, userId)

        reminder.title = command.title
        reminder.description = command.description
        reminder.dueAt = command.dueAt
        reminder.type = command.type
        reminder.priority = command.priority
        reminder.notificationEnabled = command.notificationEnabled
        reminder.smsEnabled = command.smsEnabled
        reminder.repeatRule = command.repeatRule
        reminder.updatedAt = Instant.now()

        // Recalculate schedules if provided
        if (command.schedules.isNotEmpty()) {
            reminder.schedules.clear()
            val newSchedules = command.schedules.map { sCmd ->
                val scheduledAt = calculateScheduledTime(command.dueAt, sCmd.triggerType, sCmd.offsetValue, sCmd.offsetUnit)
                ReminderSchedule(
                    reminder = reminder,
                    triggerType = sCmd.triggerType,
                    offsetValue = sCmd.offsetValue,
                    offsetUnit = sCmd.offsetUnit,
                    scheduledAt = scheduledAt,
                    enabled = true,
                    executed = false
                )
            }
            reminder.schedules.addAll(newSchedules)
        }

        return reminderRepository.save(reminder)
    }

    @Transactional
    fun deleteReminder(id: String, userId: String) {
        val reminder = getReminderById(id, userId)
        reminderRepository.delete(reminder)
        logger.info("Reminder deleted: {}", id)
    }

    @Transactional
    fun markComplete(id: String, userId: String): Reminder {
        val reminder = getReminderById(id, userId)
        reminder.status = ReminderStatus.COMPLETED
        reminder.completedAt = Instant.now()
        reminder.updatedAt = Instant.now()
        return reminderRepository.save(reminder)
    }

    @Transactional
    fun snooze(id: String, userId: String, snoozeDurationMinutes: Long = 60): Reminder {
        val reminder = getReminderById(id, userId)
        val snoozeTime = Instant.now().plus(Duration.ofMinutes(snoozeDurationMinutes))
        reminder.status = ReminderStatus.SNOOZED
        reminder.snoozedUntil = snoozeTime
        reminder.updatedAt = Instant.now()

        // Add a snooze schedule trigger
        val snoozeSchedule = ReminderSchedule(
            reminder = reminder,
            triggerType = TriggerType.EXACT_TIME,
            offsetValue = 0,
            offsetUnit = OffsetUnit.MINUTES,
            scheduledAt = snoozeTime,
            enabled = true,
            executed = false
        )
        reminder.schedules.add(snoozeSchedule)

        return reminderRepository.save(reminder)
    }

    @Transactional
    fun toggleStatus(id: String, userId: String, enable: Boolean): Reminder {
        val reminder = getReminderById(id, userId)
        reminder.status = if (enable) ReminderStatus.ACTIVE else ReminderStatus.CANCELLED
        reminder.updatedAt = Instant.now()
        return reminderRepository.save(reminder)
    }

    fun getSmsStatusForReminder(id: String, userId: String): List<SmsJob> {
        val reminder = getReminderById(id, userId)
        return smsJobRepository.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50))
            .content.filter { it.reminderId == reminder.id }
    }

    /**
     * Periodic dispatcher: checks all pending reminder schedules and dispatches notifications & SMS
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    fun dispatchDueReminders() {
        val now = Instant.now()
        val pending = reminderScheduleRepository.findPendingSchedulesToExecute(now)

        for (schedule in pending) {
            val reminder = schedule.reminder
            try {
                // 1. Create In-App Notification if enabled
                if (reminder.notificationEnabled) {
                    notificationService.createNotification(
                        userId = reminder.userId,
                        reminderId = reminder.id,
                        title = "یادآور: ${reminder.title}",
                        body = reminder.description ?: "سررسید موعد فرا رسیده است.",
                        type = reminder.type.name,
                        actionUrl = "/reminders/${reminder.id}"
                    )
                }

                // 2. Queue SMS if enabled and phone verified
                if (reminder.smsEnabled && !reminder.phoneNumber.isNullOrBlank()) {
                    val template = when (reminder.type) {
                        ReminderType.INSTALLMENT -> "installment_reminder"
                        ReminderType.VEHICLE, ReminderType.MAINTENANCE, ReminderType.INSURANCE -> "vehicle_reminder"
                        else -> "general_reminder"
                    }

                    val payload = mapOf(
                        "token" to reminder.title,
                        "token2" to (reminder.description ?: "سررسید موعد"),
                        "token3" to reminder.type.name
                    )

                    smsQueueService.enqueueSms(
                        userId = reminder.userId,
                        reminderId = reminder.id,
                        phoneNumber = reminder.phoneNumber!!,
                        templateId = template,
                        payload = payload,
                        idempotencyKey = "rem_${reminder.id}_sch_${schedule.id}"
                    )
                }

                // Mark schedule as executed
                schedule.executed = true
                schedule.executedAt = Instant.now()
                reminderScheduleRepository.save(schedule)

            } catch (e: Exception) {
                logger.error("Failed executing schedule {} for reminder {}: {}", schedule.id, reminder.id, e.message)
            }
        }
    }

    private fun calculateScheduledTime(
        dueAt: Instant,
        triggerType: TriggerType,
        offsetValue: Int,
        offsetUnit: OffsetUnit
    ): Instant {
        if (triggerType == TriggerType.EXACT_TIME || offsetValue == 0) {
            return dueAt
        }

        val chronoUnit = when (offsetUnit) {
            OffsetUnit.MINUTES -> ChronoUnit.MINUTES
            OffsetUnit.HOURS -> ChronoUnit.HOURS
            OffsetUnit.DAYS -> ChronoUnit.DAYS
            OffsetUnit.WEEKS -> ChronoUnit.WEEKS
            OffsetUnit.MONTHS -> ChronoUnit.DAYS // approximated 30 days
        }

        val multiplier = if (offsetUnit == OffsetUnit.MONTHS) 30L * offsetValue else offsetValue.toLong()

        return if (triggerType == TriggerType.BEFORE_DUE) {
            dueAt.minus(multiplier, if (offsetUnit == OffsetUnit.MONTHS) ChronoUnit.DAYS else chronoUnit)
        } else {
            dueAt.plus(multiplier, if (offsetUnit == OffsetUnit.MONTHS) ChronoUnit.DAYS else chronoUnit)
        }
    }
}
