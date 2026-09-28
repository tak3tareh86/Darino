package com.financemanager.backend.sms.provider.adapter

import com.financemanager.backend.sms.provider.SmsDeliveryStatusResponse
import com.financemanager.backend.sms.provider.SmsProvider
import com.financemanager.backend.sms.provider.SmsProviderResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component("mockSmsProvider")
class MockSmsProviderAdapter : SmsProvider {

    private val logger = LoggerFactory.getLogger(MockSmsProviderAdapter::class.java)

    override val providerName: String = "MOCK"

    override fun sendTemplateMessage(
        toPhoneNumber: String,
        templateId: String,
        parameters: Map<String, String>
    ): SmsProviderResponse {
        val mockId = "mock_msg_${UUID.randomUUID().toString().substring(0, 8)}"
        logger.info("[MOCK-SMS-PROVIDER] Simulating pattern SMS dispatch. To: {}, Template: {}, Params: {}, MockMsgId: {}", 
            toPhoneNumber, templateId, parameters, mockId)
        
        return SmsProviderResponse(
            success = true,
            providerMessageId = mockId,
            status = "SUBMITTED",
            rawResponse = "{\"status\": 200, \"message\": \"[MOCK] Dispatched to simulated queue\"}",
            isRetryable = false
        )
    }

    override fun sendOtp(toPhoneNumber: String, otpCode: String): SmsProviderResponse {
        val mockId = "mock_otp_${UUID.randomUUID().toString().substring(0, 8)}"
        logger.info("[MOCK-SMS-PROVIDER] Simulating OTP SMS dispatch. To: {}, MockMsgId: {} (OTP value redacted in logs)", 
            toPhoneNumber, mockId)
        
        return SmsProviderResponse(
            success = true,
            providerMessageId = mockId,
            status = "SUBMITTED",
            rawResponse = "{\"status\": 200, \"message\": \"[MOCK] OTP queued\"}",
            isRetryable = false
        )
    }

    override fun getDeliveryStatus(providerMessageId: String): SmsDeliveryStatusResponse {
        logger.info("[MOCK-SMS-PROVIDER] Checking delivery status for {}", providerMessageId)
        return SmsDeliveryStatusResponse(
            providerMessageId = providerMessageId,
            status = "DELIVERED",
            deliveryTimestamp = System.currentTimeMillis()
        )
    }

    override fun verifyWebhookSignature(payload: String, signature: String?): Boolean {
        // Mock provider always validates simulated signatures
        return true
    }
}
