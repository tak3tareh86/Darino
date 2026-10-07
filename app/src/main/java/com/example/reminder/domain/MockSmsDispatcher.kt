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
        println("SmsDispatcher: Mock SMS ignored for $phoneNumber. SMS functionality not configured in Mock dispatcher.")
        return SmsDispatchResult(
            success = false,
            providerMessageId = null,
            errorReason = "SMS_NOT_CONFIGURED"
        )
    }

    override suspend fun cancelSms(providerMessageId: String): Boolean {
        println("SmsDispatcher: Mock SMS cancelled: $providerMessageId")
        sentMessages[providerMessageId] = DeliveryStatus.CANCELLED
        return true
    }

    override suspend fun getStatus(providerMessageId: String): DeliveryStatus {
        return sentMessages[providerMessageId] ?: DeliveryStatus.NOT_SENT
    }
}

