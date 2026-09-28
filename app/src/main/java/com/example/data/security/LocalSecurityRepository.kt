package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/**
 * Repository interface for local security settings and credentials.
 */
interface LocalSecurityRepository {
    val securitySettings: StateFlow<LocalSecuritySettings>

    fun isPinConfigured(): Boolean
    fun verifyPin(pin: String): Boolean
    fun setPin(pin: String)
    fun removePin()

    fun isPatternConfigured(): Boolean
    fun verifyPattern(patternPoints: List<Int>): Boolean
    fun setPattern(patternPoints: List<Int>)
    fun removePattern()

    fun setBiometricEnabled(enabled: Boolean)
    fun setDefaultUnlockMethod(method: UnlockMethod)
    fun setAppLockEnabled(enabled: Boolean)

    fun setLockTimeoutSeconds(seconds: Long)
    fun recordBackgroundTimestamp(timestamp: Long = System.currentTimeMillis())
    fun shouldLockAppOnResume(currentTime: Long = System.currentTimeMillis()): Boolean

    fun hasAnyActiveLock(): Boolean
}

class MockLocalSecurityRepository(
    private val context: Context
) : LocalSecurityRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        updateState()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private val _securitySettings = MutableStateFlow(loadSettings())
    override val securitySettings: StateFlow<LocalSecuritySettings> = _securitySettings.asStateFlow()

    private fun loadSettings(): LocalSecuritySettings {
        val pinHash = prefs.getString(KEY_PIN_HASH, null)
        val patternHash = prefs.getString(KEY_PATTERN_HASH, null)
        val biometric = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        val defaultMethodName = prefs.getString(KEY_DEFAULT_METHOD, UnlockMethod.PIN.name) ?: UnlockMethod.PIN.name
        val defaultMethod = try {
            UnlockMethod.valueOf(defaultMethodName)
        } catch (e: Exception) {
            UnlockMethod.PIN
        }
        val appLockEnabled = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        val timeoutSec = prefs.getLong(KEY_LOCK_TIMEOUT, 0L)
        val lastBg = prefs.getLong(KEY_LAST_BG_TIME, 0L)

        val pinEnabled = !pinHash.isNullOrEmpty()
        val patternEnabled = !patternHash.isNullOrEmpty()

        return LocalSecuritySettings(
            appLockEnabled = appLockEnabled && (pinEnabled || patternEnabled || biometric),
            pinEnabled = pinEnabled,
            patternEnabled = patternEnabled,
            biometricEnabled = biometric,
            defaultUnlockMethod = defaultMethod,
            lockTimeoutSeconds = timeoutSec,
            lastBackgroundTimestamp = lastBg,
            lastModifiedTimestamp = prefs.getLong(KEY_LAST_MODIFIED, System.currentTimeMillis())
        )
    }

    private fun updateState() {
        val updated = loadSettings()
        _securitySettings.value = updated
    }

    override fun isPinConfigured(): Boolean {
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrEmpty()
    }

    override fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val incomingHash = hashString(pin)
        return storedHash == incomingHash
    }

    override fun setPin(pin: String) {
        val hash = hashString(pin)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_APP_LOCK_ENABLED, true)
            .putLong(KEY_LAST_MODIFIED, System.currentTimeMillis())
            .apply()
        updateState()
    }

    override fun removePin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .putLong(KEY_LAST_MODIFIED, System.currentTimeMillis())
            .apply()
        updateState()
    }

    override fun isPatternConfigured(): Boolean {
        return !prefs.getString(KEY_PATTERN_HASH, null).isNullOrEmpty()
    }

    override fun verifyPattern(patternPoints: List<Int>): Boolean {
        val storedHash = prefs.getString(KEY_PATTERN_HASH, null) ?: return false
        val incomingHash = hashString(patternPoints.joinToString(","))
        return storedHash == incomingHash
    }

    override fun setPattern(patternPoints: List<Int>) {
        val hash = hashString(patternPoints.joinToString(","))
        prefs.edit()
            .putString(KEY_PATTERN_HASH, hash)
            .putBoolean(KEY_APP_LOCK_ENABLED, true)
            .putLong(KEY_LAST_MODIFIED, System.currentTimeMillis())
            .apply()
        updateState()
    }

    override fun removePattern() {
        prefs.edit()
            .remove(KEY_PATTERN_HASH)
            .putLong(KEY_LAST_MODIFIED, System.currentTimeMillis())
            .apply()
        updateState()
    }

    override fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .putBoolean(KEY_APP_LOCK_ENABLED, enabled || isPinConfigured() || isPatternConfigured())
            .putLong(KEY_LAST_MODIFIED, System.currentTimeMillis())
            .apply()
        updateState()
    }

    override fun setDefaultUnlockMethod(method: UnlockMethod) {
        prefs.edit()
            .putString(KEY_DEFAULT_METHOD, method.name)
            .putLong(KEY_LAST_MODIFIED, System.currentTimeMillis())
            .apply()
        updateState()
    }

    override fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, enabled)
            .putLong(KEY_LAST_MODIFIED, System.currentTimeMillis())
            .apply()
        updateState()
    }

    override fun setLockTimeoutSeconds(seconds: Long) {
        prefs.edit()
            .putLong(KEY_LOCK_TIMEOUT, seconds)
            .putLong(KEY_LAST_MODIFIED, System.currentTimeMillis())
            .apply()
        updateState()
    }

    override fun recordBackgroundTimestamp(timestamp: Long) {
        prefs.edit()
            .putLong(KEY_LAST_BG_TIME, timestamp)
            .apply()
        updateState()
    }

    override fun shouldLockAppOnResume(currentTime: Long): Boolean {
        if (!hasAnyActiveLock()) return false
        val settings = _securitySettings.value
        val lastBg = settings.lastBackgroundTimestamp
        if (lastBg <= 0L) return true
        val elapsedMillis = currentTime - lastBg
        val requiredTimeoutMillis = settings.lockTimeoutSeconds * 1000L
        return elapsedMillis >= requiredTimeoutMillis
    }

    override fun hasAnyActiveLock(): Boolean {
        val current = _securitySettings.value
        return current.appLockEnabled && (current.pinEnabled || current.patternEnabled || current.biometricEnabled)
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "local_app_security_prefs"
        private const val KEY_PIN_HASH = "sec_pin_hash"
        private const val KEY_PATTERN_HASH = "sec_pattern_hash"
        private const val KEY_BIOMETRIC_ENABLED = "sec_biometric_enabled"
        private const val KEY_DEFAULT_METHOD = "sec_default_unlock_method"
        private const val KEY_APP_LOCK_ENABLED = "sec_app_lock_enabled"
        private const val KEY_LOCK_TIMEOUT = "sec_lock_timeout"
        private const val KEY_LAST_BG_TIME = "sec_last_bg_time"
        private const val KEY_LAST_MODIFIED = "sec_last_modified"
    }
}
