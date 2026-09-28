package com.example.reminder.domain

import com.example.util.IranianPhoneUtils
import java.util.UUID

class MockSmsDispatcher : SmsDispatcher {

    private val sentMessages = mutableMapOf<String, DeliveryStatus>()

    override suspend fun sendSms(
        phoneNumber: String,
        message: String,
        reminderType: String
    ): SmsDispatchResult {
        val maskedPhone = IranianPhoneUtils.maskPhoneNumber(phoneNumber)
        println("SmsDispatcher: Mock SMS queued for $maskedPhone (Type: $reminderType): $message")

        val providerMessageId = "sms_mock_${UUID.randomUUID()}"
        sentMessages[providerMessageId] = DeliveryStatus.DELIVERED

        return SmsDispatchResult(
            success = true,
            providerMessageId = providerMessageId,
            errorReason = null
        )
    }

    override suspend fun cancelSms(providerMessageId: String): Boolean {
        println("SmsDispatcher: Mock SMS cancelled: $providerMessageId")
        sentMessages[providerMessageId] = DeliveryStatus.CANCELLED
        return true
    }

    override suspend fun getStatus(providerMessageId: String): DeliveryStatus {
        return sentMessages[providerMessageId] ?: DeliveryStatus.DELIVERED
    }
}

