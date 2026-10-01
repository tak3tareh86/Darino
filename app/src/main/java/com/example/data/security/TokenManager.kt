package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.data.api.NetworkUserDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keystore-backed secure token and credential manager.
 * Uses hardware-backed AndroidKeyStore with AES-GCM 256-bit encryption.
 */
class TokenManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val userAdapter = moshi.adapter(NetworkUserDto::class.java)

    init {
        try {
            ensureKeyExists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Android KeyStore key", e)
        }
    }

    private fun ensureKeyExists() {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
            Log.i(TAG, "Generated secure AES key in Android KeyStore")
        }
    }

    private fun getSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey ?: run {
                ensureKeyExists()
                val newEntry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
                newEntry.secretKey
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining Keystore key, recreating alias", e)
            ensureKeyExists()
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            entry.secretKey
        }
    }

    private fun encrypt(plainText: String?): String? {
        if (plainText == null) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Combined IV + Encrypted bytes in format "Base64(IV):Base64(Ciphertext)"
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            "$ivBase64:$cipherBase64"
        } catch (e: Exception) {
            Log.e(TAG, "Cryptographic failure: unable to encrypt with Android KeyStore AES-GCM", e)
            null
        }
    }

    private fun decrypt(encryptedCombined: String?): String? {
        if (encryptedCombined.isNullOrBlank()) return null
        return try {
            if (encryptedCombined.contains(":")) {
                val parts = encryptedCombined.split(":", limit = 2)
                val iv = Base64.decode(parts[0], Base64.NO_WRAP)
                val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)

                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
                val decryptedBytes = cipher.doFinal(cipherBytes)
                String(decryptedBytes, Charsets.UTF_8)
            } else {
                Log.w(TAG, "Rejecting insecure unencrypted or legacy token representation")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Decryption error: authentication tag mismatch or corrupted ciphertext", e)
            null
        }
    }

    fun saveTokens(accessToken: String, refreshToken: String, expiresInMs: Long = 0) {
        val encryptedAccess = encrypt(accessToken)
        val encryptedRefresh = encrypt(refreshToken)
        val expiryTimestamp = if (expiresInMs > 0) System.currentTimeMillis() + expiresInMs else 0L

        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, encryptedAccess)
            .putString(KEY_REFRESH_TOKEN, encryptedRefresh)
            .putLong(KEY_TOKEN_EXPIRY, expiryTimestamp)
            .apply()
    }

    fun getAccessToken(): String? {
        val encrypted = prefs.getString(KEY_ACCESS_TOKEN, null)
        return decrypt(encrypted)
    }

    fun getRefreshToken(): String? {
        val encrypted = prefs.getString(KEY_REFRESH_TOKEN, null)
        return decrypt(encrypted)
    }

    fun isTokenExpired(): Boolean {
        val expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        if (expiry == 0L) return false
        // Consider expired 30 seconds before actual expiration
        return System.currentTimeMillis() >= (expiry - 30_000)
    }

    fun saveUser(user: NetworkUserDto) {
        try {
            val json = userAdapter.toJson(user)
            val encrypted = encrypt(json)
            prefs.edit()
                .putString(KEY_USER_DATA, encrypted)
                .putString(KEY_USER_ID, user.id)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize and save user", e)
        }
    }

    fun getUser(): NetworkUserDto? {
        val encrypted = prefs.getString(KEY_USER_DATA, null) ?: return null
        val json = decrypt(encrypted) ?: return null
        return try {
            userAdapter.fromJson(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse stored user", e)
            null
        }
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun saveBaseUrl(url: String) {
        val formattedUrl = if (!url.endsWith("/")) "$url/" else url
        prefs.edit().putString(KEY_BASE_URL, formattedUrl).apply()
    }

    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .remove(KEY_USER_DATA)
            .remove(KEY_USER_ID)
            .apply()
        Log.i(TAG, "Cleared secure auth tokens and session data")
    }

    companion object {
        private const val TAG = "TokenManager"
        private const val PREFS_NAME = "secure_auth_prefs"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "finance_app_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        private const val KEY_ACCESS_TOKEN = "enc_access_token"
        private const val KEY_REFRESH_TOKEN = "enc_refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry_timestamp"
        private const val KEY_USER_DATA = "enc_user_data"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_BASE_URL = "api_base_url"

        const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/"

        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                val instance = TokenManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
