package com.financemanager.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.financemanager.backend.domain.BazaarSubscription
import com.financemanager.backend.repository.BazaarSubscriptionRepository
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.concurrent.TimeUnit

data class BazaarSubscriptionSnapshot(
    val productId: String,
    val initiationTimestampMillis: Long,
    val validUntilTimestampMillis: Long,
    val autoRenewing: Boolean,
    val active: Boolean,
    val verifiedAtMillis: Long,
    val serverTimeMillis: Long
)

@Service
class BazaarSubscriptionService(
    private val repository: BazaarSubscriptionRepository,
    private val objectMapper: ObjectMapper,
    @Value("${app.bazaar.client-id:}") private val clientId: String,
    @Value("${app.bazaar.client-secret:}") private val clientSecret: String,
    @Value("${app.bazaar.refresh-token:}") private val refreshToken: String,
    @Value("${app.bazaar.package-name:}") private val packageName: String,
    @Value("${app.bazaar.subscription-id:darino_yearly_subscription}") private val subscriptionId: String,
    @Value("${app.jwt.secret}") private val encryptionSecret: String
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    @Volatile private var cachedAccessToken: String? = null
    @Volatile private var accessTokenExpiresAtMillis: Long = 0L

    @Transactional
    fun verifyAndSave(userId: String, productId: String, purchaseToken: String): BazaarSubscriptionSnapshot {
        if (productId != subscriptionId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "شناسه محصول اشتراک معتبر نیست.")
        }
        if (purchaseToken.isBlank() || purchaseToken.length > 4096) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "توکن خرید معتبر نیست.")
        }

        val result = fetchSubscription(purchaseToken)
        val now = System.currentTimeMillis()
        val initiation = result.path("initiationTimestampMsec").asLong(0L)
        val validUntil = result.path("validUntilTimestampMsec").asLong(0L)
        if (initiation <= 0L || validUntil <= 0L) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "پاسخ اعتبارسنجی بازار فاقد تاریخ معتبر اشتراک است.")
        }

        val active = validUntil > now
        val existing = repository.findByUserId(userId).orElse(null)
        val entity = existing?.apply {
            this.productId = productId
            this.purchaseTokenEncrypted = encryptToken(purchaseToken)
            this.initiationTimestampMillis = initiation
            this.validUntilTimestampMillis = validUntil
            this.autoRenewing = result.path("autoRenewing").asBoolean(false)
            this.status = if (active) "ACTIVE" else "EXPIRED"
            this.verifiedAt = Instant.now()
        } ?: BazaarSubscription(
            userId = userId,
            productId = productId,
            purchaseTokenEncrypted = encryptToken(purchaseToken),
            initiationTimestampMillis = initiation,
            validUntilTimestampMillis = validUntil,
            autoRenewing = result.path("autoRenewing").asBoolean(false),
            status = if (active) "ACTIVE" else "EXPIRED"
        )
        return toSnapshot(repository.save(entity))
    }

    @Transactional
    fun getCurrent(userId: String): BazaarSubscriptionSnapshot? {
        val saved = repository.findByUserId(userId).orElse(null) ?: return null
        // Revalidate against Bazaar on every explicit refresh; never extend validity locally.
        return verifyAndSave(userId, saved.productId, decryptToken(saved.purchaseTokenEncrypted))
    }

    private fun fetchSubscription(token: String): com.fasterxml.jackson.databind.JsonNode {
        val accessToken = getAccessToken()
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("pardakht.cafebazaar.ir")
            .addPathSegments("devapi/v2/api/applications")
            .addPathSegment(packageName)
            .addPathSegment("subscriptions")
            .addPathSegment(subscriptionId)
            .addPathSegment("purchases")
            .addPathSegment(token)
            .addPathSegment("")
            .addQueryParameter("access_token", accessToken)
            .build()

        val request = Request.Builder().url(url).get().build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "اعتبارسنجی اشتراک توسط کافه‌بازار ناموفق بود (HTTP ${response.code}).")
            }
            val json = objectMapper.readTree(body)
            if (json.hasNonNull("error") || json.hasNonNull("error_description")) {
                throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "کافه‌بازار توکن اشتراک را تأیید نکرد.")
            }
            return json
        }
    }

    @Synchronized
    private fun getAccessToken(): String {
        val now = System.currentTimeMillis()
        cachedAccessToken?.takeIf { now < accessTokenExpiresAtMillis - 60_000L }?.let { return it }
        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank() || packageName.isBlank()) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "تنظیمات اتصال امن کافه‌بازار روی سرور تکمیل نشده است.")
        }

        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("refresh_token", refreshToken)
            .build()
        val request = Request.Builder()
            .url("https://pardakht.cafebazaar.ir/devapi/v2/auth/token/")
            .post(form)
            .build()

        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "دریافت توکن دسترسی کافه‌بازار ناموفق بود.")
            }
            val json = objectMapper.readTree(body)
            val token = json.path("access_token").asText("")
            val expiresIn = json.path("expires_in").asLong(3600L)
            if (token.isBlank()) {
                throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "کافه‌بازار توکن دسترسی معتبر برنگرداند.")
            }
            cachedAccessToken = token
            accessTokenExpiresAtMillis = now + expiresIn.coerceAtLeast(60L) * 1000L
            return token
        }
    }

    private fun encryptionKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256").digest(encryptionSecret.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }

    private fun encryptToken(token: String): String {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    private fun decryptToken(encoded: String): String {
        try {
            val payload = Base64.getDecoder().decode(encoded)
            require(payload.size > 12) { "Stored purchase token is invalid." }
            val iv = payload.copyOfRange(0, 12)
            val ciphertext = payload.copyOfRange(12, payload.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(128, iv))
            return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (error: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "بازیابی امن توکن اشتراک امکان‌پذیر نیست.")
        }
    }

    private fun toSnapshot(entity: BazaarSubscription): BazaarSubscriptionSnapshot {
        val now = System.currentTimeMillis()
        return BazaarSubscriptionSnapshot(
            productId = entity.productId,
            initiationTimestampMillis = entity.initiationTimestampMillis,
            validUntilTimestampMillis = entity.validUntilTimestampMillis,
            autoRenewing = entity.autoRenewing,
            active = entity.status == "ACTIVE" && entity.validUntilTimestampMillis > now,
            verifiedAtMillis = entity.verifiedAt.toEpochMilli(),
            serverTimeMillis = now
        )
    }
}
