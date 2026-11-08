package com.financemanager.backend.sms.provider

data class SmsProviderResponse(
    val success: Boolean,
    val providerMessageId: String?,
    val status: String, // SUBMITTED, FAILED
    val rawResponse: String? = null,
    val errorMessage: String? = null,
    val isRetryable: Boolean = false
)

data class SmsDeliveryStatusResponse(
    val providerMessageId: String,
    val status: String, // SUBMITTED, SENT, DELIVERED, FAILED
    val deliveryTimestamp: Long? = null,
    val failureReason: String? = null
)

interface SmsProvider {
    val providerName: String

    fun sendTemplateMessage(
        toPhoneNumber: String,
        templateId: String,
        parameters: Map<String, String>
    ): SmsProviderResponse

    fun sendOtp(
        toPhoneNumber: String,
        otpCode: String
    ): SmsProviderResponse

    fun getDeliveryStatus(providerMessageId: String): SmsDeliveryStatusResponse

    fun verifyWebhookSignature(payload: String, signature: String?): Boolean
}
