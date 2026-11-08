package com.example.data.api

import retrofit2.Response
import retrofit2.http.*

interface HealthApi {
    @GET("api/v1/health")
    suspend fun checkHealth(): Response<Map<String, String>>
}

interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: NetworkRegisterRequest): Response<NetworkApiResponse<NetworkAuthResponse>>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: NetworkLoginRequest): Response<NetworkApiResponse<NetworkAuthResponse>>

    @POST("api/v1/auth/refresh")
    suspend fun refreshTokens(@Body request: NetworkRefreshTokenRequest): Response<NetworkApiResponse<NetworkAuthResponse>>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<NetworkApiResponse<String>>
}

interface PhoneApi {
    @POST("api/v1/phone/send-otp")
    suspend fun sendOtp(@Body request: NetworkSendOtpRequest): Response<NetworkApiResponse<NetworkSendOtpResponse>>

    @POST("api/v1/phone/verify-otp")
    suspend fun verifyOtp(@Body request: NetworkVerifyOtpRequest): Response<NetworkApiResponse<NetworkPhoneStatusResponse>>

    @GET("api/v1/phone/status")
    suspend fun getStatus(): Response<NetworkApiResponse<NetworkPhoneStatusResponse>>
}


interface TransactionApi {
    @GET("api/v1/transactions")
    suspend fun listTransactions(): Response<NetworkApiResponse<List<NetworkTransactionDto>>>

    @POST("api/v1/transactions")
    suspend fun upsertTransaction(@Body request: NetworkTransactionRequest): Response<NetworkApiResponse<NetworkTransactionDto>>

    @PUT("api/v1/transactions/{id}")
    suspend fun updateTransaction(@Path("id") id: String, @Body request: NetworkTransactionRequest): Response<NetworkApiResponse<NetworkTransactionDto>>

    @DELETE("api/v1/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): Response<NetworkApiResponse<Unit>>
}

interface ReminderApi {
    @GET("api/v1/reminders")
    suspend fun listReminders(@Query("status") status: String? = null): Response<NetworkApiResponse<List<NetworkReminderDto>>>

    @POST("api/v1/reminders")
    suspend fun createReminder(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: NetworkCreateReminderRequest
    ): Response<NetworkApiResponse<NetworkReminderDto>>

    @PUT("api/v1/reminders/{id}")
    suspend fun updateReminder(
        @Path("id") id: String,
        @Body request: NetworkUpdateReminderRequest
    ): Response<NetworkApiResponse<NetworkReminderDto>>

    @GET("api/v1/reminders/{id}")
    suspend fun getReminder(@Path("id") id: String): Response<NetworkApiResponse<NetworkReminderDto>>

    @DELETE("api/v1/reminders/{id}")
    suspend fun deleteReminder(@Path("id") id: String): Response<NetworkApiResponse<String>>

    @POST("api/v1/reminders/{id}/complete")
    suspend fun completeReminder(@Path("id") id: String): Response<NetworkApiResponse<NetworkReminderDto>>

    @POST("api/v1/reminders/{id}/snooze")
    suspend fun snoozeReminder(
        @Path("id") id: String,
        @Body durationMinutes: Map<String, Long> = mapOf("durationMinutes" to 15L)
    ): Response<NetworkApiResponse<NetworkReminderDto>>

    @GET("api/v1/reminders/{id}/sms-status")
    suspend fun getReminderSmsStatus(@Path("id") id: String): Response<NetworkApiResponse<NetworkSmsStatusDto>>
}

interface NotificationApi {
    @GET("api/v1/notifications")
    suspend fun listNotifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<NetworkApiResponse<List<NetworkNotificationDto>>>

    @POST("api/v1/notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: String): Response<NetworkApiResponse<NetworkNotificationDto>>

    @POST("api/v1/notifications/read-all")
    suspend fun markAllAsRead(): Response<NetworkApiResponse<String>>
}

interface SmsApi {
    @POST("api/v1/sms/send")
    suspend fun sendTestSms(@Body request: NetworkSendTestSmsRequest): Response<NetworkApiResponse<NetworkSmsSendResponse>>

    @GET("api/v1/sms/status/{providerMessageId}")
    suspend fun getSmsStatus(@Path("providerMessageId") providerMessageId: String): Response<NetworkApiResponse<NetworkSmsStatusDto>>
}
