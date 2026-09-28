package com.example.backend.service.sms

enum class SmsTemplate(val key: String) {
    GENERAL_REMINDER("GENERAL_REMINDER"),
    INSTALLMENT_REMINDER("INSTALLMENT_REMINDER"),
    VEHICLE_REMINDER("VEHICLE_REMINDER"),
    OTP_VERIFICATION("OTP_VERIFICATION")
}

data class SmsProviderResult(
    val success: Boolean,
    val providerMessageId: String?,
    val isTemporaryError: Boolean = false,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

enum class SmsDeliveryStatus {
    SUBMITTED,
    SENT,
    DELIVERED,
    FAILED,
    EXPIRED,
    UNKNOWN
}

interface SmsProvider {
    val providerName: String

    fun sendTemplateMessage(
        toPhoneNumber: String,
        template: SmsTemplate,
        payload: Map<String, Any>
    ): SmsProviderResult

    fun sendOtp(toPhoneNumber: String, otpCode: String): SmsProviderResult

    fun getDeliveryStatus(providerMessageId: String): SmsDeliveryStatus

    fun validateWebhookSignature(rawBody: String, signature: String?): Boolean
}
