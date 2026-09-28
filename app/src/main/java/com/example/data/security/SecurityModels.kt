package com.example.data.security

/**
 * Supported app lock and authentication methods.
 */
enum class UnlockMethod(val title: String, val description: String, val emoji: String) {
    PIN("رمز عددی (PIN)", "ورود با رمز ۴ یا ۶ رقمی امن", "🔢"),
    PATTERN("الگوی رسم‌شده", "اتصال حداقل ۴ نقطه روی شبکه ۳×۳", "📐"),
    BIOMETRIC("اثر انگشت / بیومتریک", "احراز هویت بیومتریک و سنسور اثر انگشت", "👆")
}

/**
 * Lock Timeout Options in seconds.
 */
enum class LockTimeoutOption(val seconds: Long, val title: String) {
    IMMEDIATELY(0L, "هر بار باز شدن برنامه"),
    ONE_MINUTE(60L, "بعد از ۱ دقیقه"),
    FIVE_MINUTES(300L, "بعد از ۵ دقیقه"),
    FIFTEEN_MINUTES(900L, "بعد از ۱۵ دقیقه"),
    THIRTY_MINUTES(1800L, "بعد از ۳۰ دقیقه");

    companion object {
        fun fromSeconds(sec: Long): LockTimeoutOption {
            return entries.firstOrNull { it.seconds == sec } ?: IMMEDIATELY
        }
    }
}

/**
 * Local security configuration domain model.
 */
data class LocalSecuritySettings(
    val appLockEnabled: Boolean = false,
    val pinEnabled: Boolean = false,
    val patternEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val defaultUnlockMethod: UnlockMethod = UnlockMethod.PIN,
    val lockTimeoutSeconds: Long = 0L,
    val lastBackgroundTimestamp: Long = 0L,
    val lastModifiedTimestamp: Long = System.currentTimeMillis()
)

/**
 * App session state machine for startup, authentication, lock, and main app navigation.
 */
sealed interface AppLockState {
    data object Checking : AppLockState
    data object Unauthenticated : AppLockState     // First launch or logged out -> Show LoginScreen
    data object Locked : AppLockState              // Authenticated user with lock enabled -> Show LockedAppScreen
    data object Unlocked : AppLockState            // Authenticated and unlocked -> Show Main App
}
