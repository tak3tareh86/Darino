package com.financemanager.backend.sms.provider.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import com.financemanager.backend.sms.provider.SmsDeliveryStatusResponse
import com.financemanager.backend.sms.provider.SmsProvider
import com.financemanager.backend.sms.provider.SmsProviderResponse
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.concurrent.TimeUnit

@Component("kavenegarSmsProvider")
class KavenegarSmsProviderAdapter(
    @Value("\${app.sms.api-key:}") private val apiKey: String,
    @Value("\${app.sms.api-base-url:https://api.kavenegar.com/v1}") private val baseUrl: String,
    @Value("\${app.sms.sender:+9830000000}") private val defaultSender: String,
    @Value("\${app.sms.webhook-secret:}") private val webhookSecret: String,
    private val objectMapper: ObjectMapper
) : SmsProvider {

    private val logger = LoggerFactory.getLogger(KavenegarSmsProviderAdapter::class.java)

    override val providerName: String = "KAVENEGAR"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
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
                errorMessage = "Kavenegar API key is not configured in environment.",
                isRetryable = false
            )
        }

        try {
            val url = "$baseUrl/$apiKey/verify/lookup.json"
            val formBuilder = FormBody.Builder()
                .add("receptor", toPhoneNumber)
                .add("template", templateId)

            // Map parameters to token, token2, token3, token10, token20 as required by Kavenegar Lookup
            parameters["token"]?.let { formBuilder.add("token", it) }
            parameters["token2"]?.let { formBuilder.add("token2", it) }
            parameters["token3"]?.let { formBuilder.add("token3", it) }
            parameters["token10"]?.let { formBuilder.add("token10", it) }
            parameters["token20"]?.let { formBuilder.add("token20", it) }

            // If arbitrary tokens are named (e.g., amount, date, vehicleName), map them dynamically
            var tokenIdx = 1
            parameters.forEach { (k, v) ->
                if (!listOf("token", "token2", "token3", "token10", "token20").contains(k)) {
                    val keyName = if (tokenIdx == 1) "token" else "token$tokenIdx"
                    formBuilder.add(keyName, v)
                    tokenIdx++
                }
            }

            val request = Request.Builder()
                .url(url)
                .post(formBuilder.build())
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                val statusCode = response.code

                if (response.isSuccessful) {
                    val jsonNode = objectMapper.readTree(bodyString)
                    val returnNode = jsonNode.path("return")
                    val status = returnNode.path("status").asInt(200)

                    if (status == 200) {
                        val entriesNode = jsonNode.path("entries")
                        val messageId = if (entriesNode.isArray && entriesNode.size() > 0) {
                            entriesNode[0].path("messageid").asText()
                        } else {
                            "kav_${System.currentTimeMillis()}"
                        }

                        logger.info("Kavenegar template SMS accepted. MessageId: {}, Receptor: {}", messageId, toPhoneNumber)
                        return SmsProviderResponse(
                            success = true,
                            providerMessageId = messageId,
                            status = "SUBMITTED",
                            rawResponse = bodyString,
                            isRetryable = false
                        )
                    } else {
                        val errorMsg = returnNode.path("message").asText("Unknown gateway error")
                        val isRetryable = status in listOf(500, 502, 503, 504)
                        logger.error("Kavenegar returned error status {}: {}", status, errorMsg)
                        return SmsProviderResponse(
                            success = false,
                            providerMessageId = null,
                            status = "FAILED",
                            rawResponse = bodyString,
                            errorMessage = errorMsg,
                            isRetryable = isRetryable
                        )
                    }
                } else {
                    val isRetryable = statusCode >= 500 || statusCode == 429
                    logger.warn("Kavenegar HTTP call failed with code {}: {}", statusCode, bodyString)
                    return SmsProviderResponse(
                        success = false,
                        providerMessageId = null,
                        status = "FAILED",
                        errorMessage = "HTTP $statusCode from Kavenegar gateway",
                        isRetryable = isRetryable
                    )
                }
            }
        } catch (e: IOException) {
            logger.error("Network IO error while sending SMS to Kavenegar: {}", e.message)
            return SmsProviderResponse(
                success = false,
                providerMessageId = null,
                status = "FAILED",
                errorMessage = "Network timeout or connection error: ${e.message}",
                isRetryable = true
            )
        } catch (e: Exception) {
            logger.error("Unexpected error in Kavenegar adapter: {}", e.message)
            return SmsProviderResponse(
                success = false,
                providerMessageId = null,
                status = "FAILED",
                errorMessage = e.message,
                isRetryable = false
            )
        }
    }

    override fun sendOtp(toPhoneNumber: String, otpCode: String): SmsProviderResponse {
        return sendTemplateMessage(
            toPhoneNumber = toPhoneNumber,
            templateId = "otp_verify_code",
            parameters = mapOf("token" to otpCode)
        )
    }

    override fun getDeliveryStatus(providerMessageId: String): SmsDeliveryStatusResponse {
        if (apiKey.isBlank()) {
            return SmsDeliveryStatusResponse(providerMessageId = providerMessageId, status = "FAILED", failureReason = "API key missing")
        }

        try {
            val url = "$baseUrl/$apiKey/sms/status.json?messageid=$providerMessageId"
            val request = Request.Builder().url(url).get().build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonNode = objectMapper.readTree(body)
                    val entries = jsonNode.path("entries")
                    if (entries.isArray && entries.size() > 0) {
                        val statusInt = entries[0].path("status").asInt()
                        // Kavenegar status: 10=Delivered, 4=Sent to operator, 1=Queued, 100=Failed
                        val normalizedStatus = when (statusInt) {
                            10 -> "DELIVERED"
                            4, 5 -> "SENT"
                            1, 2 -> "SUBMITTED"
                            else -> "FAILED"
                        }
                        return SmsDeliveryStatusResponse(
                            providerMessageId = providerMessageId,
                            status = normalizedStatus,
                            deliveryTimestamp = System.currentTimeMillis()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Could not query delivery status for {}: {}", providerMessageId, e.message)
        }

        return SmsDeliveryStatusResponse(providerMessageId = providerMessageId, status = "SUBMITTED")
    }

    override fun verifyWebhookSignature(payload: String, signature: String?): Boolean {
        if (webhookSecret.isBlank()) return false
        // Validate shared secret or HMAC header
        return signature == webhookSecret
    }
}
