package com.example.reminder.domain

import java.util.UUID

class MockSmsDispatcher : SmsDispatcher {

    private val sentMessages = mutableMapOf<String, DeliveryStatus>()

    override suspend fun sendSms(
        phoneNumber: String,
        message: String,
        reminderType: String
    ): SmsDispatchResult {
        val mockId = "sms_mock_${UUID.randomUUID().toString().substring(0, 8)}"
        println("SmsDispatcher: Simulated successful SMS to $phoneNumber with ID $mockId: $message")
        sentMessages[mockId] = DeliveryStatus.DELIVERED
        return SmsDispatchResult(
            success = true,
            providerMessageId = mockId,
            errorReason = null
        )
    }

    override suspend fun cancelSms(providerMessageId: String): Boolean {
        println("SmsDispatcher: Mock SMS cancelled: $providerMessageId")
        sentMessages[providerMessageId] = DeliveryStatus.CANCELLED
        return true
    }

    override suspend fun getStatus(providerMessageId: String): DeliveryStatus {
        return sentMessages[providerMessageId] ?: DeliveryStatus.FAILED
    }
}

