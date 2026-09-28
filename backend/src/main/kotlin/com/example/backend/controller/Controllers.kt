package com.example.backend.controller

import com.example.backend.dto.*
import com.example.backend.security.UserPrincipal
import com.example.backend.service.auth.AuthService
import com.example.backend.service.notification.NotificationService
import com.example.backend.service.phone.PhoneVerificationService
import com.example.backend.service.reminder.ReminderService
import com.example.backend.service.sms.SmsWebhookService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.register(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.refresh(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<ApiResponse<String>> {
        authService.logout(principal.id)
        return ResponseEntity.ok(ApiResponse.success("خروج با موفقیت انجام شد."))
    }
}

@RestController
@RequestMapping("/api/v1/phone")
class PhoneVerificationController(
    private val phoneVerificationService: PhoneVerificationService
) {

    @PostMapping("/send-otp")
    fun sendOtp(
        @Valid @RequestBody request: SendOtpRequest,
        @AuthenticationPrincipal principal: UserPrincipal?,
        servletRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<SendOtpResponse>> {
        val clientIp = servletRequest.getHeader("X-Forwarded-For") ?: servletRequest.remoteAddr
        val response = phoneVerificationService.sendOtp(request.phoneNumber, clientIp, principal?.id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/verify-otp")
    fun verifyOtp(
        @Valid @RequestBody request: VerifyOtpRequest,
        @AuthenticationPrincipal principal: UserPrincipal?
    ): ResponseEntity<ApiResponse<PhoneStatusResponse>> {
        val response = phoneVerificationService.verifyOtp(request.phoneNumber, request.code, principal?.id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/status")
    fun getStatus(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<PhoneStatusResponse>> {
        val response = phoneVerificationService.getPhoneStatus(principal.id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}

@RestController
@RequestMapping("/api/v1/reminders")
class ReminderController(
    private val reminderService: ReminderService
) {

    @GetMapping
    fun listReminders(
        @RequestParam(required = false) status: String?,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<ReminderDto>>> {
        val reminders = reminderService.getReminders(principal.id, status)
        return ResponseEntity.ok(ApiResponse.success(reminders))
    }

    @PostMapping
    fun createReminder(
        @Valid @RequestBody request: CreateReminderRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<ReminderDto>> {
        val created = reminderService.createReminder(principal.id, request)
        return ResponseEntity.ok(ApiResponse.success(created))
    }

    @GetMapping("/{id}")
    fun getReminder(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<ReminderDto>> {
        val reminder = reminderService.getReminderById(principal.id, id)
        return ResponseEntity.ok(ApiResponse.success(reminder))
    }

    @PutMapping("/{id}")
    fun updateReminder(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateReminderRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<ReminderDto>> {
        val updated = reminderService.updateReminder(principal.id, id, request)
        return ResponseEntity.ok(ApiResponse.success(updated))
    }

    @DeleteMapping("/{id}")
    fun deleteReminder(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<String>> {
        reminderService.deleteReminder(principal.id, id)
        return ResponseEntity.ok(ApiResponse.success("یادآور با موفقیت حذف شد."))
    }

    @PostMapping("/{id}/complete")
    fun completeReminder(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<ReminderDto>> {
        val completed = reminderService.completeReminder(principal.id, id)
        return ResponseEntity.ok(ApiResponse.success(completed))
    }

    @PostMapping("/{id}/snooze")
    fun snoozeReminder(
        @PathVariable id: UUID,
        @RequestBody(required = false) request: SnoozeReminderRequest?,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<ReminderDto>> {
        val snoozed = reminderService.snoozeReminder(principal.id, id, request ?: SnoozeReminderRequest())
        return ResponseEntity.ok(ApiResponse.success(snoozed))
    }

    @PostMapping("/{id}/enable")
    fun enableReminder(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<ReminderDto>> {
        val enabled = reminderService.setReminderEnabled(principal.id, id, true)
        return ResponseEntity.ok(ApiResponse.success(enabled))
    }

    @PostMapping("/{id}/disable")
    fun disableReminder(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<ReminderDto>> {
        val disabled = reminderService.setReminderEnabled(principal.id, id, false)
        return ResponseEntity.ok(ApiResponse.success(disabled))
    }

    @GetMapping("/{id}/sms-status")
    fun getSmsStatus(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<ReminderSmsStatusDto>> {
        val status = reminderService.getReminderSmsStatus(principal.id, id)
        return ResponseEntity.ok(ApiResponse.success(status))
    }
}

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    fun listNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "30") size: Int,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<NotificationDto>>> {
        val notifications = notificationService.getNotifications(principal.id, page, size)
        return ResponseEntity.ok(ApiResponse.success(notifications))
    }

    @PostMapping("/{id}/read")
    fun markAsRead(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<NotificationDto>> {
        val read = notificationService.markAsRead(principal.id, id)
        return ResponseEntity.ok(ApiResponse.success(read))
    }

    @PostMapping("/read-all")
    fun markAllAsRead(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<String>> {
        notificationService.markAllAsRead(principal.id)
        return ResponseEntity.ok(ApiResponse.success("تمام اعلانات خوانده شدند."))
    }
}

@RestController
@RequestMapping("/api/v1/webhooks")
class SmsWebhookController(
    private val smsWebhookService: SmsWebhookService
) {

    @PostMapping("/sms")
    fun handleSmsWebhook(
        @RequestBody payload: SmsWebhookPayload,
        @RequestHeader(value = "X-Webhook-Signature", required = false) signature: String?,
        servletRequest: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        val rawBody = payload.toString()
        val accepted = smsWebhookService.handleDeliveryWebhook(rawBody, signature, payload)
        return ResponseEntity.ok(
            mapOf(
                "status" to if (accepted) "ACCEPTED" else "REJECTED",
                "receivedAt" to System.currentTimeMillis()
            )
        )
    }
}
