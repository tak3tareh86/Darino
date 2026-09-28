package com.example.backend.service.sms

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Mock SMS Provider for local development and CI testing.
 * Clearly identified as "MOCK". Never pretends to be a real Iranian SMS gateway.
 */
@Component("mockSmsProvider")
class MockSmsProvider : SmsProvider {
    private val log = LoggerFactory.getLogger(MockSmsProvider::class.java)
    override val providerName: String = "MOCK"

    // In-memory test tracker for delivery simulation
    private val deliveredIds = ConcurrentHashMap<String, SmsDeliveryStatus>()

    override fun sendTemplateMessage(
        toPhoneNumber: String,
        template: SmsTemplate,
        payload: Map<String, Any>
    ): SmsProviderResult {
        val mockId = "mock_msg_${UUID.randomUUID().toString().take(8)}"
        deliveredIds[mockId] = SmsDeliveryStatus.SENT
        
        log.info("[MOCK SMS PROVIDER] Dispatched pattern '${template.name}' to $toPhoneNumber with payload: $payload. ProviderMsgId: $mockId")
        return SmsProviderResult(
            success = true,
            providerMessageId = mockId
        )
    }

    override fun sendOtp(toPhoneNumber: String, otpCode: String): SmsProviderResult {
        val mockId = "mock_otp_${UUID.randomUUID().toString().take(8)}"
        deliveredIds[mockId] = SmsDeliveryStatus.DELIVERED
        
        // Note: For development/testing logs only, indicates MOCK behavior without exposing in client API
        log.info("[MOCK SMS PROVIDER] OTP sent to $toPhoneNumber. ProviderMsgId: $mockId. (Development Mock Provider Active)")
        return SmsProviderResult(
            success = true,
            providerMessageId = mockId
        )
    }

    override fun getDeliveryStatus(providerMessageId: String): SmsDeliveryStatus {
        return deliveredIds[providerMessageId] ?: SmsDeliveryStatus.DELIVERED
    }

    override fun validateWebhookSignature(rawBody: String, signature: String?): Boolean {
        return true
    }

    fun setSimulatedStatus(messageId: String, status: SmsDeliveryStatus) {
        deliveredIds[messageId] = status
    }
}
