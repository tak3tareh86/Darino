package com.financemanager.backend.common

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val timestamp: Instant = Instant.now()
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> =
            ApiResponse(success = true, message = message, data = data)

        fun successMessage(message: String): ApiResponse<Unit> =
            ApiResponse(success = true, message = message, data = null)
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
    val timestamp: Instant = Instant.now()
)

enum class ErrorCode {
    VALIDATION_ERROR,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    RATE_LIMITED,
    PHONE_NOT_VERIFIED,
    OTP_INVALID,
    OTP_EXPIRED,
    OTP_LOCKED,
    INTERNAL_SERVER_ERROR
}

open class AppException(
    val errorCode: ErrorCode,
    override val message: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST,
    val details: Map<String, String>? = null
) : RuntimeException(message)

class ResourceNotFoundException(entity: String, id: String) :
    AppException(ErrorCode.NOT_FOUND, "$entity not found with id: $id", HttpStatus.NOT_FOUND)

class PhoneNotVerifiedException(message: String = "شماره همراه احراز نشده است. لطفاً ابتدا کد تأیید پیامک را وارد کنید.") :
    AppException(ErrorCode.PHONE_NOT_VERIFIED, message, HttpStatus.FORBIDDEN)

class RateLimitExceededException(message: String = "تعداد درخواست‌های شما بیش از حد مجاز است. لطفاً کمی بعد تلاش کنید.") :
    AppException(ErrorCode.RATE_LIMITED, message, HttpStatus.TOO_MANY_REQUESTS)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AppException::class)
    fun handleAppException(ex: AppException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            code = ex.errorCode.name,
            message = ex.message,
            details = ex.details
        )
        return ResponseEntity.status(ex.status).body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = mutableMapOf<String, String>()
        ex.bindingResult.allErrors.forEach { error ->
            val fieldName = (error as? FieldError)?.field ?: "field"
            val errorMessage = error.defaultMessage ?: "مقدار نامعتبر است"
            errors[fieldName] = errorMessage
        }
        val errorResponse = ErrorResponse(
            code = ErrorCode.VALIDATION_ERROR.name,
            message = "اطلاعات ورودی نامعتبر است",
            details = errors
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            code = ErrorCode.VALIDATION_ERROR.name,
            message = ex.message ?: "پارامترهای ارسالی نامعتبر است"
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ErrorResponse> {
        // Safe logging without exposing internal stack trace to client
        val errorResponse = ErrorResponse(
            code = ErrorCode.INTERNAL_SERVER_ERROR.name,
            message = "خطای غیرمنتظره در سرور رخ داده است. لطفاً مجدداً تلاش فرمایید."
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
