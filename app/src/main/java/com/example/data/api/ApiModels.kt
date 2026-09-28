package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NetworkApiResponse<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: T? = null,
    @Json(name = "error") val error: NetworkApiError? = null,
    @Json(name = "timestamp") val timestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class NetworkApiError(
    @Json(name = "code") val code: String,
    @Json(name = "message") val message: String,
    @Json(name = "details") val details: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class NetworkRegisterRequest(
    @Json(name = "username") val username: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null,
    @Json(name = "password") val password: String,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "timezone") val timezone: String? = "Asia/Tehran"
)

@JsonClass(generateAdapter = true)
data class NetworkLoginRequest(
    @Json(name = "usernameOrPhone") val usernameOrPhone: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class NetworkRefreshTokenRequest(
    @Json(name = "refreshToken") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class NetworkAuthResponse(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String,
    @Json(name = "tokenType") val tokenType: String = "Bearer",
    @Json(name = "expiresInMs") val expiresInMs: Long = 86400000L,
    @Json(name = "user") val user: NetworkUserDto
)

@JsonClass(generateAdapter = true)
data class NetworkUserDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null,
    @Json(name = "phoneNumberMasked") val phoneNumberMasked: String? = null,
    @Json(name = "phoneVerified") val phoneVerified: Boolean = false,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "timezone") val timezone: String = "Asia/Tehran",
    @Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NetworkSendOtpRequest(
    @Json(name = "phoneNumber") val phoneNumber: String
)

@JsonClass(generateAdapter = true)
data class NetworkSendOtpResponse(
    @Json(name = "phoneNumberMasked") val phoneNumberMasked: String,
    @Json(name = "status") val status: String,
    @Json(name = "resendAvailableInSeconds") val resendAvailableInSeconds: Long = 60,
    @Json(name = "expiresInSeconds") val expiresInSeconds: Long = 120
)

@JsonClass(generateAdapter = true)
data class NetworkVerifyOtpRequest(
    @Json(name = "phoneNumber") val phoneNumber: String,
    @Json(name = "code") val code: String
)

@JsonClass(generateAdapter = true)
data class NetworkPhoneStatusResponse(
    @Json(name = "phoneNumber") val phoneNumber: String?,
    @Json(name = "phoneNumberMasked") val phoneNumberMasked: String?,
    @Json(name = "isVerified") val isVerified: Boolean,
    @Json(name = "verifiedAt") val verifiedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NetworkCreateReminderRequest(
    @Json(name = "type") val type: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "sourceType") val sourceType: String? = null,
    @Json(name = "sourceId") val sourceId: String? = null,
    @Json(name = "dueAt") val dueAt: String,
    @Json(name = "timezone") val timezone: String = "Asia/Tehran",
    @Json(name = "repeatRule") val repeatRule: String? = "NONE",
    @Json(name = "priority") val priority: String = "NORMAL",
    @Json(name = "notificationEnabled") val notificationEnabled: Boolean = true,
    @Json(name = "smsEnabled") val smsEnabled: Boolean = false,
    @Json(name = "phoneNumber") val phoneNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class NetworkUpdateReminderRequest(
    @Json(name = "type") val type: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "dueAt") val dueAt: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "repeatRule") val repeatRule: String? = null,
    @Json(name = "notificationEnabled") val notificationEnabled: Boolean? = null,
    @Json(name = "smsEnabled") val smsEnabled: Boolean? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class NetworkReminderDto(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "dueAt") val dueAt: String,
    @Json(name = "timezone") val timezone: String = "Asia/Tehran",
    @Json(name = "repeatRule") val repeatRule: String? = "NONE",
    @Json(name = "status") val status: String = "ACTIVE",
    @Json(name = "notificationEnabled") val notificationEnabled: Boolean = true,
    @Json(name = "smsEnabled") val smsEnabled: Boolean = false,
    @Json(name = "phoneNumber") val phoneNumber: String? = null,
    @Json(name = "phoneNumberMasked") val phoneNumberMasked: String? = null,
    @Json(name = "smsDeliveryStatus") val smsDeliveryStatus: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "completedAt") val completedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NetworkSmsStatusDto(
    @Json(name = "reminderId") val reminderId: String,
    @Json(name = "providerMessageId") val providerMessageId: String? = null,
    @Json(name = "status") val status: String, // "QUEUED", "SENT", "DELIVERED", "FAILED"
    @Json(name = "phoneNumberMasked") val phoneNumberMasked: String? = null,
    @Json(name = "sentAt") val sentAt: String? = null,
    @Json(name = "deliveredAt") val deliveredAt: String? = null,
    @Json(name = "failureReason") val failureReason: String? = null
)

@JsonClass(generateAdapter = true)
data class NetworkNotificationDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "message") val message: String,
    @Json(name = "type") val type: String,
    @Json(name = "status") val status: String = "DELIVERED",
    @Json(name = "reminderId") val reminderId: String? = null,
    @Json(name = "readAt") val readAt: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NetworkSendTestSmsRequest(
    @Json(name = "phoneNumber") val phoneNumber: String,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class NetworkSmsSendResponse(
    @Json(name = "providerMessageId") val providerMessageId: String,
    @Json(name = "status") val status: String,
    @Json(name = "delivered") val delivered: Boolean
)
