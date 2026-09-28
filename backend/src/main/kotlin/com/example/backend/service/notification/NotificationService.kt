package com.example.backend.service.notification

import com.example.backend.domain.Notification
import com.example.backend.dto.NotificationDto
import com.example.backend.exception.ApiException
import com.example.backend.exception.ErrorCode
import com.example.backend.repository.NotificationRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {

    @Transactional(readOnly = true)
    fun getNotifications(userId: UUID, page: Int = 0, size: Int = 30): List<NotificationDto> {
        val pageable = PageRequest.of(page, size)
        val notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        return notifications.content.map { it.toDto() }
    }

    @Transactional
    fun markAsRead(userId: UUID, notificationId: UUID): NotificationDto {
        val notification = notificationRepository.findByIdAndUserId(notificationId, userId)
            .orElseThrow { ApiException(ErrorCode.NOT_FOUND, "اعلان مورد نظر یافت نشد.") }

        notification.status = "READ"
        notification.readAt = Instant.now()
        return notificationRepository.save(notification).toDto()
    }

    @Transactional
    fun markAllAsRead(userId: UUID) {
        val unreadList = notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "UNREAD")
        val now = Instant.now()
        unreadList.forEach {
            it.status = "READ"
            it.readAt = now
        }
        notificationRepository.saveAll(unreadList)
    }

    @Transactional(readOnly = true)
    fun getUnreadCount(userId: UUID): Long {
        return notificationRepository.countByUserIdAndStatus(userId, "UNREAD")
    }
}

fun Notification.toDto(): NotificationDto {
    return NotificationDto(
        id = this.id!!,
        title = this.title,
        message = this.message,
        type = this.type,
        status = this.status,
        reminderId = this.reminder?.id,
        readAt = this.readAt,
        createdAt = this.createdAt
    )
}
