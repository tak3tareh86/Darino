package com.example.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// Common API Response wrapper
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val timestamp: Instant = Instant.now()
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)
        fun <T> failure(code: String, message: String): ApiResponse<T> =
            ApiResponse(success = false, error = ApiError(code, message))
    }
}

data class ApiError(
    val code: String,
    val message: String
)

// Auth DTOs
data class RegisterRequest(
    val email: String?,
    val phoneNumber: String?,
    @field:NotBlank val password: String,
    val fullName: String?,
    val timezone: String? = "Asia/Tehran"
)

data class LoginRequest(
    val identifier: String, // email or phone number
    @field:NotBlank val password: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInMs: Long,
    val user: UserDto
)

data class RefreshTokenRequest(
    @field:NotBlank val refreshToken: String
)

data class UserDto(
    val id: UUID,
    val email: String?,
    val phoneNumber: String?,
    val phoneVerified: Boolean,
    val fullName: String?,
    val timezone: String,
    val createdAt: Instant
)

// Phone & OTP DTOs
data class SendOtpRequest(
    @field:NotBlank val phoneNumber: String
)

data class SendOtpResponse(
    val phoneNumberMasked: String,
    val status: String,
    val resendAvailableInSeconds: Long,
    val expiresInSeconds: Long
)

data class VerifyOtpRequest(
    @field:NotBlank val phoneNumber: String,
    @field:NotBlank
    @field:Pattern(regexp = "^[0-9]{5,6}$", message = "کد تایید باید ۵ یا ۶ رقم باشد")
    val code: String
)

data class PhoneStatusResponse(
    val phoneNumber: String?,
    val phoneNumberMasked: String?,
    val isVerified: Boolean,
    val verifiedAt: Instant?
)

// Reminder DTOs
data class CreateReminderRequest(
    @field:NotBlank val type: String, // GENERAL, INSTALLMENT, VEHICLE, INSURANCE, MAINTENANCE
    @field:NotBlank val title: String,
    val description: String?,
    val sourceType: String? = null,
    val sourceId: UUID? = null,
    @field:NotNull val dueAt: Instant,
    val timezone: String = "Asia/Tehran",
    val priority: String = "NORMAL",
    val notificationEnabled: Boolean = true,
    val smsEnabled: Boolean = false,
    val phoneNumber: String? = null,
    val schedules: List<CreateReminderScheduleRequest>? = null
)

data class UpdateReminderRequest(
    val title: String?,
    val description: String?,
    val dueAt: Instant?,
    val priority: String?,
    val notificationEnabled: Boolean?,
    val smsEnabled: Boolean?,
    val phoneNumber: String?
)

data class CreateReminderScheduleRequest(
    @field:NotBlank val triggerType: String, // BEFORE_DUE, EXACT_TIME, RECURRING
    val offsetValue: Int = 0,
    val offsetUnit: String = "DAYS", // MINUTES, HOURS, DAYS, WEEKS, MONTHS
    val scheduledAt: Instant?,
    val notificationEnabled: Boolean = true,
    val smsEnabled: Boolean = false
)

data class SnoozeReminderRequest(
    val durationMinutes: Long = 15,
    val customScheduledAt: Instant? = null
)

data class ReminderDto(
    val id: UUID,
    val type: String,
    val title: String,
    val description: String?,
    val sourceType: String?,
    val sourceId: UUID?,
    val dueAt: Instant,
    val timezone: String,
    val priority: String,
    val status: String,
    val notificationEnabled: Boolean,
    val smsEnabled: Boolean,
    val phoneNumber: String?,
    val phoneNumberMasked: String?,
    val completedAt: Instant?,
    val schedules: List<ReminderScheduleDto>,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ReminderScheduleDto(
    val id: UUID,
    val triggerType: String,
    val offsetValue: Int,
    val offsetUnit: String,
    val scheduledAt: Instant,
    val notificationEnabled: Boolean,
    val smsEnabled: Boolean,
    val status: String,
    val lastTriggeredAt: Instant?
)

data class ReminderSmsStatusDto(
    val reminderId: UUID,
    val smsEnabled: Boolean,
    val phoneVerified: Boolean,
    val jobs: List<SmsJobSummaryDto>
)

data class SmsJobSummaryDto(
    val id: UUID,
    val phoneNumberMasked: String,
    val status: String,
    val provider: String?,
    val providerMessageId: String?,
    val attemptCount: Int,
    val failureReason: String?,
    val queuedAt: Instant,
    val sentAt: Instant?,
    val deliveredAt: Instant?
)

// Notification DTOs
data class NotificationDto(
    val id: UUID,
    val title: String,
    val message: String,
    val type: String,
    val status: String,
    val reminderId: UUID?,
    val readAt: Instant?,
    val createdAt: Instant
)

// SMS Webhook DTO
data class SmsWebhookPayload(
    val provider: String,
    val providerMessageId: String,
    val status: String, // SUBMITTED, SENT, DELIVERED, FAILED
    val errorCode: String? = null,
    val timestamp: Long? = null,
    val signature: String? = null
)
