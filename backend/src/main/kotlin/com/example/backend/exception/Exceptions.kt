package com.example.backend.exception

import com.example.backend.dto.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

enum class ErrorCode(val httpStatus: HttpStatus, val defaultMessage: String) {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "احراز هویت انجام نشده یا منقضی شده است."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "شما دسترسی لازم برای این عملیات را ندارید."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "منبع درخواستی یافت نشد."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "اطلاعات ورودی نامعتبر است."),
    PHONE_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "شماره همراه باید قبل از ارسال پیامک تایید شود."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "شماره موبایل وارد شده نامعتبر است."),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "کد تایید منقضی شده است."),
    OTP_INVALID(HttpStatus.BAD_REQUEST, "کد تایید وارد شده اشتباه است."),
    OTP_MAX_ATTEMPTS_REACHED(HttpStatus.TOO_MANY_REQUESTS, "تعداد دفعات تلاش بیش از حد مجاز است. لطفاً مجدداً کد درخواست کنید."),
    OTP_COOLDOWN_ACTIVE(HttpStatus.TOO_MANY_REQUESTS, "لطفاً تا پایان زمان انتظار برای درخواست کد جدید شکیبا باشید."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "کاربری با این مشخصات قبلاً ثبت نام کرده است."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "نام کاربری یا رمز عبور اشتباه است."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "تعداد درخواست‌ها بیش از حد مجاز است."),
    SMS_PROVIDER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "خطا در ارتباط با درگاه پیامک."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "خطای غیرمنتظره در سرور رخ داده است.")
}

class ApiException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.defaultMessage,
    val details: Map<String, Any>? = null
) : RuntimeException(message)

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("API Exception [${ex.errorCode.name}]: ${ex.message}")
        return ResponseEntity.status(ex.errorCode.httpStatus)
            .body(ApiResponse.failure(ex.errorCode.name, ex.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Unit>> {
        val errorMessage = ex.bindingResult.allErrors.joinToString("; ") { error ->
            if (error is FieldError) "${error.field}: ${error.defaultMessage}" else error.defaultMessage ?: "خطای اعتبارسنجی"
        }
        log.warn("Validation failure: $errorMessage")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR.name, errorMessage))
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthException(ex: AuthenticationException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.failure(ErrorCode.UNAUTHORIZED.name, ex.message ?: "عدم دسترسی"))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.failure(ErrorCode.FORBIDDEN.name, "شما مجوز دسترسی به این رکورد را ندارید."))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ApiResponse<Unit>> {
        log.error("Unhandled server exception", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR.name, "خطایی در سیستم رخ داده است. لطفاً بعداً تلاش کنید."))
    }
}
