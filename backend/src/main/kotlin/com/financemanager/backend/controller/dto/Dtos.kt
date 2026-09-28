package com.financemanager.backend.controller.dto

import com.financemanager.backend.domain.OffsetUnit
import com.financemanager.backend.domain.Priority
import com.financemanager.backend.domain.ReminderType
import com.financemanager.backend.domain.TriggerType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

// Auth DTOs
data class RegisterRequest(
    @field:NotBlank(message = "نام کاربری الزامی است")
    @field:Size(min = 3, max = 50, message = "نام کاربری باید بین ۳ تا ۵۰ کاراکتر باشد")
    val username: String,

    val phoneNumber: String? = null,

    @field:NotBlank(message = "رمز عبور الزامی است")
    @field:Size(min = 6, message = "رمز عبور باید حداقل ۶ کاراکتر باشد")
    val password: String,

    val fullName: String? = null
)

data class LoginRequest(
    @field:NotBlank(message = "نام کاربری یا شماره موبایل الزامی است")
    val usernameOrPhone: String,

    @field:NotBlank(message = "رمز عبور الزامی است")
    val password: String
)

data class RefreshTokenRequest(
    @field:NotBlank(message = "توکن تازه‌سازی الزامی است")
    val refreshToken: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val userId: String,
    val username: String,
    val phoneNumber: String?,
    val phoneVerified: Boolean
)

// Phone DTOs
data class SendOtpRequest(
    @field:NotBlank(message = "شماره موبایل الزامی است")
    val phoneNumber: String
)

data class VerifyOtpRequest(
    @field:NotBlank(message = "شماره موبایل الزامی است")
    val phoneNumber: String,

    @field:NotBlank(message = "کد تأیید الزامی است")
    val code: String
)

// Reminder DTOs
data class ReminderRequest(
    val type: ReminderType = ReminderType.GENERAL,
    @field:NotBlank(message = "عنوان یادآور الزامی است")
    val title: String,
    val description: String? = null,
    @field:NotNull(message = "زمان سررسید الزامی است")
    val dueAt: Instant,
    val timezone: String = "Asia/Tehran",
    val priority: Priority = Priority.NORMAL,
    val notificationEnabled: Boolean = true,
    val smsEnabled: Boolean = false,
    val phoneNumber: String? = null,
    val repeatRule: String = "NONE",
    val schedules: List<ScheduleRequest> = emptyList()
)

data class ScheduleRequest(
    val triggerType: TriggerType = TriggerType.BEFORE_DUE,
    val offsetValue: Int = 0,
    val offsetUnit: OffsetUnit = OffsetUnit.DAYS
)

data class SnoozeRequest(
    val snoozeMinutes: Long = 60
)

// Vehicle DTOs
data class VehicleRequest(
    @field:NotBlank(message = "نام خودرو الزامی است")
    val name: String,
    @field:NotBlank(message = "برند خودرو الزامی است")
    val brand: String,
    val modelYear: String? = null,
    val plateNumber: String? = null,
    val odometerKm: Long = 0,
    val color: String? = null
)

// Installment DTOs
data class InstallmentRequest(
    @field:NotBlank(message = "عنوان قسط الزامی است")
    val title: String,
    @field:NotBlank(message = "نام ارائه‌دهنده الزامی است")
    val providerName: String,
    val category: String = "LOAN",
    @field:NotNull(message = "مبلغ کل الزامی است")
    val totalAmount: BigDecimal,
    @field:NotNull(message = "مبلغ هر قسط الزامی است")
    val monthlyPayment: BigDecimal,
    val totalInstallments: Int,
    val paidInstallments: Int = 0,
    val dueDay: Int,
    val startDate: LocalDate
)

// SMS Webhook DTO
data class SmsWebhookPayload(
    val messageId: String,
    val status: String,
    val timestamp: Long? = null,
    val signature: String? = null
)
