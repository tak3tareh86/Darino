package com.example.reminder.domain

interface SmsDispatcher {
    /**
     * Dispatches an SMS reminder. In production, this connects to a backend SMS gateway.
     * In local/mock mode, this logs the delivery safely without requiring device SMS permissions.
     */
    suspend fun sendSms(
        phoneNumber: String,
        message: String,
        reminderType: String
    ): SmsDispatchResult

    /**
     * Cancels a pending SMS.
     */
    suspend fun cancelSms(providerMessageId: String): Boolean

    /**
     * Gets the delivery status of an SMS from the provider.
     */
    suspend fun getStatus(providerMessageId: String): DeliveryStatus
}
