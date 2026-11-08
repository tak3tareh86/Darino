package com.financemanager.backend.sms.provider

import com.financemanager.backend.sms.provider.adapter.FarazSmsProviderAdapter
import com.financemanager.backend.sms.provider.adapter.KavenegarSmsProviderAdapter
import com.financemanager.backend.sms.provider.adapter.MockSmsProviderAdapter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SmsProviderFactory(
    @Value("\${app.sms.provider:mock}") private val configuredProviderName: String,
    private val mockSmsProvider: MockSmsProviderAdapter,
    private val kavenegarSmsProvider: KavenegarSmsProviderAdapter,
    private val farazSmsProvider: FarazSmsProviderAdapter
) {
    private val logger = LoggerFactory.getLogger(SmsProviderFactory::class.java)

    fun getActiveProvider(): SmsProvider {
        val provider = when (configuredProviderName.lowercase().trim()) {
            "kavenegar", "kaveh_negar" -> kavenegarSmsProvider
            "faraz", "farazsms", "ippanel" -> farazSmsProvider
            "mock", "dev", "test" -> mockSmsProvider
            else -> {
                logger.warn("Unknown SMS_PROVIDER '{}'. Falling back to MOCK provider.", configuredProviderName)
                mockSmsProvider
            }
        }
        return provider
    }
}
