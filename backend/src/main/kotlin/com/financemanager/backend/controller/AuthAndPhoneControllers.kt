package com.financemanager.backend.controller

import com.financemanager.backend.common.ApiResponse
import com.financemanager.backend.controller.dto.*
import com.financemanager.backend.security.UserPrincipal
import com.financemanager.backend.service.AuthService
import com.financemanager.backend.service.PhoneStatusDto
import com.financemanager.backend.service.PhoneVerificationService
import com.financemanager.backend.service.SendOtpResultDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "احراز هویت کاربران، ثبت نام، ورود و چرخش توکن")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    @Operation(summary = "ثبت نام کاربر جدید")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val result = authService.register(
            username = request.username,
            rawPhone = request.phoneNumber,
            passwordRaw = request.password,
            fullName = request.fullName
        )
        val response = AuthResponse(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            userId = result.userId,
            username = result.username,
            phoneNumber = result.phoneNumber,
            phoneVerified = result.phoneVerified
        )
        return ResponseEntity.ok(ApiResponse.success(response, "ثبت نام با موفقیت انجام شد"))
    }

    @PostMapping("/login")
    @Operation(summary = "ورود کاربر با نام کاربری/تلفن همراه و رمز عبور")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val result = authService.login(request.usernameOrPhone, request.password)
        val response = AuthResponse(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            userId = result.userId,
            username = result.username,
            phoneNumber = result.phoneNumber,
            phoneVerified = result.phoneVerified
        )
        return ResponseEntity.ok(ApiResponse.success(response, "ورود با موفقیت انجام شد"))
    }

    @PostMapping("/refresh")
    @Operation(summary = "دریافت توکن دسترسی جدید با توکن تازه‌سازی (Token Rotation)")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val result = authService.refresh(request.refreshToken)
        val response = AuthResponse(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            userId = result.userId,
            username = result.username,
            phoneNumber = result.phoneNumber,
            phoneVerified = result.phoneVerified
        )
        return ResponseEntity.ok(ApiResponse.success(response, "توکن با موفقیت تمدید شد"))
    }

    @PostMapping("/logout")
    @Operation(summary = "خروج کاربر و ابطال تمام توکن‌های تازه‌سازی")
    fun logout(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<ApiResponse<Unit>> {
        authService.logout(principal.id)
        return ResponseEntity.ok(ApiResponse.successMessage("خروج با موفقیت انجام شد"))
    }
}

@RestController
@RequestMapping("/api/v1/phone")
@Tag(name = "Phone Verification & OTP", description = "احراز شماره موبایل ایرانی، ارسال و تایید کد پیامک")
class PhoneVerificationController(
    private val phoneVerificationService: PhoneVerificationService
) {

    @PostMapping("/send-otp")
    @Operation(summary = "ارسال کد تایید پیامکی (OTP)")
    fun sendOtp(
        @Valid @RequestBody request: SendOtpRequest,
        @AuthenticationPrincipal principal: UserPrincipal?,
        servletRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<SendOtpResultDto>> {
        val ipAddress = servletRequest.remoteAddr
        val result = phoneVerificationService.sendOtp(request.phoneNumber, principal?.id, ipAddress)
        return ResponseEntity.ok(ApiResponse.success(result, "کد تایید با موفقیت ارسال شد"))
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "بررسی و تایید کد پیامکی OTP")
    fun verifyOtp(
        @Valid @RequestBody request: VerifyOtpRequest,
        @AuthenticationPrincipal principal: UserPrincipal?
    ): ResponseEntity<ApiResponse<Map<String, Boolean>>> {
        val verified = phoneVerificationService.verifyOtp(request.phoneNumber, request.code, principal?.id)
        return ResponseEntity.ok(ApiResponse.success(mapOf("verified" to verified), "شماره تلفن با موفقیت تایید شد"))
    }

    @GetMapping("/status")
    @Operation(summary = "دریافت وضعیت احراز هویت شماره همراه کاربر جاری")
    fun getStatus(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<ApiResponse<PhoneStatusDto>> {
        val status = phoneVerificationService.getStatus(principal.id)
        return ResponseEntity.ok(ApiResponse.success(status))
    }
}
