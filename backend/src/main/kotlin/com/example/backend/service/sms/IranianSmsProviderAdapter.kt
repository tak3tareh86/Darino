package com.example.backend.service.sms

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Production Iranian SMS Gateway Adapter.
 * Supports Iranian mobile operators (MCI / Hamrah-e Aval, Irancell, Rightel, Shatel)
 * using server-side pattern/template verification APIs (FarazSMS / KavehNegar / IPPanel compliant).
 * 
 * Credentials are NEVER placed in the client APK.
 */
@Component("iranianSmsProvider")
class IranianSmsProviderAdapter(
    @Value("\${app.sms.api-base-url}") private val baseUrl: String,
    @Value("\${app.sms.api-key:}") private val apiKey: String,
    @Value("\${app.sms.api-secret:}") private val apiSecret: String,
    @Value("\${app.sms.sender:}") private val defaultSender: String,
    @Value("\${app.sms.webhook-secret:}") private val webhookSecret: String,
    @Value("\${app.sms.patterns.otp:1001}") private val patternOtp: String,
    @Value("\${app.sms.patterns.installment:1002}") private val patternInstallment: String,
    @Value("\${app.sms.patterns.vehicle:1003}") private val patternVehicle: String,
    @Value("\${app.sms.patterns.general:1004}") private val patternGeneral: String
) : SmsProvider {

    private val log = LoggerFactory.getLogger(IranianSmsProviderAdapter::class.java)
    override val providerName: String = "IRANIAN_GATEWAY"

    private val restClient by lazy {
        RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "AccessKey $apiKey")
            .defaultHeader("Content-Type", "application/json")
            .build()
    }

    override fun sendTemplateMessage(
        toPhoneNumber: String,
        template: SmsTemplate,
        payload: Map<String, Any>
    ): SmsProviderResult {
        val patternCode = getPatternCodeForTemplate(template)
        log.info("Sending template '${template.name}' (code: $patternCode) to $toPhoneNumber via Iranian SMS Gateway")

        return try {
            val requestBody = mapOf(
                "pattern_code" to patternCode,
                "originator" to defaultSender,
                "recipient" to toPhoneNumber,
                "values" to payload
            )

            // In production environment with configured API Gateway:
            if (apiKey.isBlank() || apiKey == "placeholder_api_key_do_not_hardcode") {
                log.warn("Iranian SMS API Key is not configured. Simulating gateway error response.")
                return SmsProviderResult(
                    success = false,
                    providerMessageId = null,
                    isTemporaryError = false,
                    errorCode = "CREDENTIALS_MISSING",
                    errorMessage = "کلید احراز هویت درگاه پیامک پیکربندی نشده است."
                )
            }

            val response = restClient.post()
                .uri("/patterns/send")
                .body(requestBody)
                .retrieve()
                .body(Map::class.java)

            val trackingCode = response?.get("tracking_code")?.toString()
                ?: response?.get("message_id")?.toString()

            if (trackingCode != null) {
                SmsProviderResult(
                    success = true,
                    providerMessageId = trackingCode
                )
            } else {
                val error = response?.get("error")?.toString() ?: "Unknown gateway response"
                SmsProviderResult(
                    success = false,
                    providerMessageId = null,
                    isTemporaryError = isTemporaryFailure(error),
                    errorCode = "GATEWAY_ERROR",
                    errorMessage = error
                )
            }
        } catch (ex: Exception) {
            log.error("Network or HTTP failure while calling Iranian SMS Gateway: ${ex.message}")
            SmsProviderResult(
                success = false,
                providerMessageId = null,
                isTemporaryError = true,
                errorCode = "HTTP_TIMEOUT",
                errorMessage = "خطا در ارتباط با سرور درگاه پیامک: ${ex.message}"
            )
        }
    }

    override fun sendOtp(toPhoneNumber: String, otpCode: String): SmsProviderResult {
        return sendTemplateMessage(
            toPhoneNumber = toPhoneNumber,
            template = SmsTemplate.OTP_VERIFICATION,
            payload = mapOf("code" to otpCode)
        )
    }

    override fun getDeliveryStatus(providerMessageId: String): SmsDeliveryStatus {
        if (apiKey.isBlank()) return SmsDeliveryStatus.UNKNOWN
        return try {
            val response = restClient.get()
                .uri("/messages/$providerMessageId/status")
                .retrieve()
                .body(Map::class.java)

            val statusStr = response?.get("status")?.toString()?.uppercase()
            when (statusStr) {
                "DELIVERED", "1" -> SmsDeliveryStatus.DELIVERED
                "SENT", "2" -> SmsDeliveryStatus.SENT
                "FAILED", "UNDELIVERED", "0" -> SmsDeliveryStatus.FAILED
                else -> SmsDeliveryStatus.SUBMITTED
            }
        } catch (ex: Exception) {
            log.warn("Failed to fetch delivery status for $providerMessageId: ${ex.message}")
            SmsDeliveryStatus.UNKNOWN
        }
    }

    override fun validateWebhookSignature(rawBody: String, signature: String?): Boolean {
        if (webhookSecret.isBlank() || signature == null) return true
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(webhookSecret.toByteArray(), "HmacSHA256")
            mac.init(secretKey)
            val hash = mac.doFinal(rawBody.toByteArray())
            val expectedSig = hash.joinToString("") { "%02x".format(it) }
            MessageDigest.isEqual(expectedSig.toByteArray(), signature.toByteArray())
        } catch (ex: Exception) {
            log.error("Failed to compute webhook signature", ex)
            false
        }
    }

    private fun getPatternCodeForTemplate(template: SmsTemplate): String {
        return when (template) {
            SmsTemplate.OTP_VERIFICATION -> patternOtp
            SmsTemplate.INSTALLMENT_REMINDER -> patternInstallment
            SmsTemplate.VEHICLE_REMINDER -> patternVehicle
            SmsTemplate.GENERAL_REMINDER -> patternGeneral
        }
    }

    private fun isTemporaryFailure(errorMessage: String): Boolean {
        val lower = errorMessage.lowercase()
        return lower.contains("timeout") || lower.contains("busy") || lower.contains("rate limit") || lower.contains("connection")
    }
}
