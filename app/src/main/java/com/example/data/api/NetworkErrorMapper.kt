package com.example.data.api

import android.util.Log
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

enum class NetworkErrorType {
    NETWORK_UNAVAILABLE,  // Device internet disconnected / UnknownHostException
    SERVER_UNREACHABLE,   // ConnectException / No listener on port/IP
    TIMEOUT,              // SocketTimeoutException
    SSL_ERROR,            // SSL Handshake failure
    HTTP_400_BAD_REQUEST, // Invalid phone or parameters
    HTTP_401_UNAUTHORIZED,// Session or token invalid
    HTTP_403_FORBIDDEN,   // Access forbidden
    HTTP_404_NOT_FOUND,   // Endpoint not found
    HTTP_429_RATE_LIMIT,  // Rate limit exceeded
    HTTP_500_SERVER_ERROR,// Internal server error
    HTTP_503_UNAVAILABLE, // Service unavailable
    UNKNOWN_ERROR
}

data class DetailedNetworkError(
    val type: NetworkErrorType,
    val httpCode: Int? = null,
    val userFacingMessage: String,
    val technicalMessage: String
)

object NetworkErrorMapper {
    private const val TAG = "NetworkErrorMapper"

    fun mapThrowable(throwable: Throwable, baseUrl: String = ""): DetailedNetworkError {
        Log.e(TAG, "Network exception encountered: ${throwable.javaClass.simpleName} - ${throwable.message}")

        return when (throwable) {
            is UnknownHostException -> DetailedNetworkError(
                type = NetworkErrorType.NETWORK_UNAVAILABLE,
                userFacingMessage = "اتصال اینترنت برقرار نیست. لطفاً شبکه خود را بررسی کنید.",
                technicalMessage = "UnknownHostException: Unable to resolve host domain ($baseUrl)"
            )
            is ConnectException -> DetailedNetworkError(
                type = NetworkErrorType.SERVER_UNREACHABLE,
                userFacingMessage = "سرور در دسترس نیست. لطفاً از روشن بودن سرور یا صحت آدرس API اطمینان حاصل کنید.",
                technicalMessage = "ConnectException: Failed to connect to server at $baseUrl"
            )
            is SocketTimeoutException -> DetailedNetworkError(
                type = NetworkErrorType.TIMEOUT,
                userFacingMessage = "ارتباط با سرور زمان‌بر شد. لطفاً دوباره تلاش کنید.",
                technicalMessage = "SocketTimeoutException: Read/Write timeout occurred while waiting for response"
            )
            is SSLHandshakeException, is SSLException -> DetailedNetworkError(
                type = NetworkErrorType.SSL_ERROR,
                userFacingMessage = "خطای گواهی امنیتی SSL در ارتباط با سرور.",
                technicalMessage = "SSLException: SSL handshake failed"
            )
            is HttpException -> mapHttpCode(throwable.code(), throwable.message())
            is IOException -> DetailedNetworkError(
                type = NetworkErrorType.NETWORK_UNAVAILABLE,
                userFacingMessage = "خطا در تبادل داده شبکه. لطفاً اتصال اینترنت را بررسی کنید.",
                technicalMessage = "IOException: ${throwable.localizedMessage}"
            )
            else -> DetailedNetworkError(
                type = NetworkErrorType.UNKNOWN_ERROR,
                userFacingMessage = "خطایی ناخواسته در ارتباط با سرور رخ داد. لطفاً مجدداً تلاش کنید.",
                technicalMessage = "Exception: ${throwable.localizedMessage}"
            )
        }
    }

    fun mapHttpResponse(response: Response<*>): DetailedNetworkError {
        val code = response.code()
        val errorBody = response.errorBody()?.string()
        Log.w(TAG, "HTTP Response Error Code $code: $errorBody")
        return mapHttpCode(code, errorBody)
    }

    fun mapHttpCode(code: Int, rawBody: String? = null): DetailedNetworkError {
        return when (code) {
            400 -> DetailedNetworkError(
                type = NetworkErrorType.HTTP_400_BAD_REQUEST,
                httpCode = code,
                userFacingMessage = "شماره موبایل یا اطلاعات وارد شده معتبر نیست.",
                technicalMessage = "HTTP 400 Bad Request"
            )
            401 -> DetailedNetworkError(
                type = NetworkErrorType.HTTP_401_UNAUTHORIZED,
                httpCode = code,
                userFacingMessage = "دسترسی غیرمجاز. نشست کاربری شما معتبر نیست.",
                technicalMessage = "HTTP 401 Unauthorized"
            )
            403 -> DetailedNetworkError(
                type = NetworkErrorType.HTTP_403_FORBIDDEN,
                httpCode = code,
                userFacingMessage = "دسترسی به این عملیات محدود شده است.",
                technicalMessage = "HTTP 403 Forbidden"
            )
            404 -> DetailedNetworkError(
                type = NetworkErrorType.HTTP_404_NOT_FOUND,
                httpCode = code,
                userFacingMessage = "آدرس سرویس مورد نظر روی سرور یافت نشد.",
                technicalMessage = "HTTP 404 Not Found"
            )
            429 -> DetailedNetworkError(
                type = NetworkErrorType.HTTP_429_RATE_LIMIT,
                httpCode = code,
                userFacingMessage = "تعداد درخواست‌های شما بیش از حد مجاز است. لطفاً کمی صبر کرده و سپس تلاش کنید.",
                technicalMessage = "HTTP 429 Too Many Requests"
            )
            500 -> DetailedNetworkError(
                type = NetworkErrorType.HTTP_500_SERVER_ERROR,
                httpCode = code,
                userFacingMessage = "سرور با خطای داخلی مواجه شده است. لطفاً دقایقی دیگر تلاش کنید.",
                technicalMessage = "HTTP 500 Internal Server Error"
            )
            503 -> DetailedNetworkError(
                type = NetworkErrorType.HTTP_503_UNAVAILABLE,
                httpCode = code,
                userFacingMessage = "سرور موقتاً در حال بروزرسانی یا خارج از دسترس است.",
                technicalMessage = "HTTP 503 Service Unavailable"
            )
            else -> DetailedNetworkError(
                type = NetworkErrorType.UNKNOWN_ERROR,
                httpCode = code,
                userFacingMessage = "خطا در دریافت پاسخ از سرور (کد خطا: $code)",
                technicalMessage = "HTTP $code Error"
            )
        }
    }
}
