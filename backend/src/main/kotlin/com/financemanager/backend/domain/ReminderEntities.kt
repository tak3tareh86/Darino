package com.financemanager.backend.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "reminders")
data class Reminder(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var type: ReminderType = ReminderType.GENERAL,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "due_at", nullable = false)
    var dueAt: Instant,

    @Column(nullable = false, length = 50)
    var timezone: String = "Asia/Tehran",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var priority: Priority = Priority.NORMAL,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: ReminderStatus = ReminderStatus.ACTIVE,

    @Column(name = "notification_enabled", nullable = false)
    var notificationEnabled: Boolean = true,

    @Column(name = "sms_enabled", nullable = false)
    var smsEnabled: Boolean = false,

    @Column(name = "phone_number", length = 20)
    var phoneNumber: String? = null,

    @Column(name = "source_type", length = 50)
    var sourceType: String? = null,

    @Column(name = "source_id", length = 36)
    var sourceId: String? = null,

    @Column(name = "repeat_rule", nullable = false, length = 50)
    var repeatRule: String = "NONE",

    @Column(name = "completed_at")
    var completedAt: Instant? = null,

    @Column(name = "snoozed_until")
    var snoozedUntil: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "reminder", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var schedules: MutableList<ReminderSchedule> = mutableListOf()
)

@Entity
@Table(name = "reminder_schedules")
data class ReminderSchedule(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reminder_id", nullable = false)
    var reminder: Reminder,

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 50)
    var triggerType: TriggerType = TriggerType.BEFORE_DUE,

    @Column(name = "offset_value", nullable = false)
    var offsetValue: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "offset_unit", nullable = false, length = 20)
    var offsetUnit: OffsetUnit = OffsetUnit.DAYS,

    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: Instant,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(nullable = false)
    var executed: Boolean = false,

    @Column(name = "executed_at")
    var executedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class ReminderType {
    GENERAL,
    INSTALLMENT,
    VEHICLE,
    INSURANCE,
    MAINTENANCE,
    FUEL
}

enum class TriggerType {
    BEFORE_DUE,
    EXACT_TIME,
    AFTER_DUE
}

enum class OffsetUnit {
    MINUTES,
    HOURS,
    DAYS,
    WEEKS,
    MONTHS
}

enum class ReminderStatus {
    ACTIVE,
    COMPLETED,
    SNOOZED,
    CANCELLED
}

enum class Priority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}
