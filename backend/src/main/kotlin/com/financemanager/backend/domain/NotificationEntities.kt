package com.financemanager.backend.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notifications")
data class Notification(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "reminder_id")
    var reminderId: String? = null,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    var body: String,

    @Column(nullable = false, length = 50)
    var type: String = "REMINDER",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: NotificationStatus = NotificationStatus.UNREAD,

    @Column(name = "action_url")
    var actionUrl: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "read_at")
    var readAt: Instant? = null
)

@Entity
@Table(name = "notification_logs")
data class NotificationLog(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "notification_id", nullable = false)
    val notificationId: String,

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(nullable = false, length = 30)
    val channel: String, // PUSH, IN_APP, EMAIL

    @Column(name = "delivery_status", nullable = false, length = 30)
    val deliveryStatus: String,

    @Column(columnDefinition = "TEXT")
    val details: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class NotificationStatus {
    UNREAD,
    READ,
    ARCHIVED
}
