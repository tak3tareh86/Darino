package com.financemanager.backend.sms.provider.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import com.financemanager.backend.sms.provider.SmsDeliveryStatusResponse
import com.financemanager.backend.sms.provider.SmsProvider
import com.financemanager.backend.sms.provider.SmsProviderResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component("farazSmsProvider")
class FarazSmsProviderAdapter(
    @Value("\${app.sms.api-key:}") private val apiKey: String,
    @Value("\${app.sms.sender:+9830000000}") private val sender: String,
    @Value("\${app.sms.webhook-secret:}") private val webhookSecret: String,
    private val objectMapper: ObjectMapper
) : SmsProvider {

    private val logger = LoggerFactory.getLogger(FarazSmsProviderAdapter::class.java)

    override val providerName: String = "FARAZ_SMS"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun sendTemplateMessage(
        toPhoneNumber: String,
        templateId: String,
        parameters: Map<String, String>
    ): SmsProviderResponse {
        if (apiKey.isBlank()) {
            return SmsProviderResponse(
                success = false,
                providerMessageId = null,
                status = "FAILED",
                errorMessage = "FarazSMS API key is not configured in environment.",
                isRetryable = false
            )
        }

        try {
            val url = "https://ippanel.com/api/select"
            val payload = mapOf(
                "op" to "pattern",
                "user" to apiKey,
                "pass" to apiKey,
                "fromNum" to sender,
                "toNum" to toPhoneNumber,
                "patternCode" to templateId,
                "inputData" to listOf(parameters)
            )

            val jsonBody = objectMapper.writeValueAsString(payload)
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val trackingCode = body.trim().replace("\"", "")
                    logger.info("FarazSMS accepted pattern message. TrackingCode: {}", trackingCode)
                    return SmsProviderResponse(
                        success = true,
                        providerMessageId = trackingCode,
                        status = "SUBMITTED",
                        rawResponse = body,
                        isRetryable = false
                    )
                } else {
                    val isRetryable = response.code >= 500
                    return SmsProviderResponse(
                        success = false,
                        providerMessageId = null,
                        status = "FAILED",
                        errorMessage = "Faraz HTTP Error ${response.code}",
                        rawResponse = body,
                        isRetryable = isRetryable
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error connecting to FarazSMS gateway: {}", e.message)
            return SmsProviderResponse(
                success = false,
                providerMessageId = null,
                status = "FAILED",
                errorMessage = e.message,
                isRetryable = true
            )
        }
    }

    override fun sendOtp(toPhoneNumber: String, otpCode: String): SmsProviderResponse {
        return sendTemplateMessage(
            toPhoneNumber = toPhoneNumber,
            templateId = "otp_code",
            parameters = mapOf("code" to otpCode)
        )
    }

    override fun getDeliveryStatus(providerMessageId: String): SmsDeliveryStatusResponse {
        return SmsDeliveryStatusResponse(providerMessageId = providerMessageId, status = "DELIVERED")
    }

    override fun verifyWebhookSignature(payload: String, signature: String?): Boolean {
        return webhookSecret.isBlank() || signature == webhookSecret
    }
}
