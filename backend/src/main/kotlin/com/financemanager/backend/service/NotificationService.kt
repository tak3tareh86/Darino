package com.financemanager.backend.service

import com.financemanager.backend.domain.Notification
import com.financemanager.backend.domain.NotificationLog
import com.financemanager.backend.domain.NotificationStatus
import com.financemanager.backend.repository.NotificationLogRepository
import com.financemanager.backend.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationLogRepository: NotificationLogRepository
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    fun getUserNotifications(userId: String, page: Int = 0, size: Int = 20): Page<Notification> {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
    }

    fun getUnreadCount(userId: String): Long {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD)
    }

    @Transactional
    fun createNotification(
        userId: String,
        reminderId: String?,
        title: String,
        body: String,
        type: String = "REMINDER",
        actionUrl: String? = null
    ): Notification {
        val notification = Notification(
            userId = userId,
            reminderId = reminderId,
            title = title,
            body = body,
            type = type,
            status = NotificationStatus.UNREAD,
            actionUrl = actionUrl,
            createdAt = Instant.now()
        )
        val saved = notificationRepository.save(notification)

        val log = NotificationLog(
            notificationId = saved.id,
            userId = userId,
            channel = "IN_APP",
            deliveryStatus = "DELIVERED",
            createdAt = Instant.now()
        )
        notificationLogRepository.save(log)

        logger.info("Notification created for user: {} (Title: {})", userId, title)
        return saved
    }

    @Transactional
    fun markAsRead(notificationId: String, userId: String): Notification {
        val notification = notificationRepository.findByIdAndUserId(notificationId, userId)
            .orElseThrow { IllegalArgumentException("Notification not found") }

        notification.status = NotificationStatus.READ
        notification.readAt = Instant.now()
        return notificationRepository.save(notification)
    }

    @Transactional
    fun markAllAsRead(userId: String) {
        val unreadPage = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100))
        for (notif in unreadPage.content) {
            if (notif.status == NotificationStatus.UNREAD) {
                notif.status = NotificationStatus.READ
                notif.readAt = Instant.now()
            }
        }
        notificationRepository.saveAll(unreadPage.content)
    }
}
