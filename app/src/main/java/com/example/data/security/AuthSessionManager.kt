package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthSession(
    val userId: String,
    val username: String,
    val phoneNumber: String? = null,
    val phoneVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

sealed interface AuthSessionState {
    data object Loading : AuthSessionState
    data object Unauthenticated : AuthSessionState
    data class Authenticated(val session: AuthSession) : AuthSessionState
    data object LoggedOut : AuthSessionState
}

class AuthSessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Loading)
    val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

    init {
        restoreSession()
    }

    fun restoreSession(): AuthSessionState {
        val userId = prefs.getString(KEY_USER_ID, null)
        val username = prefs.getString(KEY_USERNAME, null)
        val phone = prefs.getString(KEY_PHONE, null)
        val isVerified = prefs.getBoolean(KEY_VERIFIED, true)
        val createdAt = prefs.getLong(KEY_CREATED_AT, 0L)
        val lastLoginAt = prefs.getLong(KEY_LAST_LOGIN_AT, 0L)

        return if (!userId.isNullOrBlank() && !username.isNullOrBlank()) {
            val session = AuthSession(
                userId = userId,
                username = username,
                phoneNumber = phone,
                phoneVerified = isVerified,
                createdAt = if (createdAt > 0) createdAt else System.currentTimeMillis(),
                lastLoginAt = if (lastLoginAt > 0) lastLoginAt else System.currentTimeMillis()
            )
            _sessionState.value = AuthSessionState.Authenticated(session)
            AuthSessionState.Authenticated(session)
        } else {
            _sessionState.value = AuthSessionState.Unauthenticated
            AuthSessionState.Unauthenticated
        }
    }

    fun createSession(userId: String, username: String, phoneNumber: String? = null): AuthSession {
        val now = System.currentTimeMillis()
        val existingCreatedAt = prefs.getLong(KEY_CREATED_AT, 0L)
        val createdAt = if (existingCreatedAt > 0) existingCreatedAt else now

        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PHONE, phoneNumber)
            .putBoolean(KEY_VERIFIED, true)
            .putLong(KEY_CREATED_AT, createdAt)
            .putLong(KEY_LAST_LOGIN_AT, now)
            .apply()

        val session = AuthSession(
            userId = userId,
            username = username,
            phoneNumber = phoneNumber,
            phoneVerified = true,
            createdAt = createdAt,
            lastLoginAt = now
        )
        _sessionState.value = AuthSessionState.Authenticated(session)
        SessionManager.setAuthenticatedUser(
            com.example.data.api.NetworkUserDto(
                id = userId,
                fullName = username,
                email = null,
                phoneNumber = phoneNumber,
                phoneVerified = true
            )
        )
        return session
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_PHONE)
            .remove(KEY_VERIFIED)
            .remove(KEY_LAST_LOGIN_AT)
            .apply()
        _sessionState.value = AuthSessionState.LoggedOut
        SessionManager.logout()
    }

    fun hasValidSession(): Boolean {
        return restoreSession() is AuthSessionState.Authenticated
    }

    fun getActiveSession(): AuthSession? {
        val state = _sessionState.value
        return if (state is AuthSessionState.Authenticated) state.session else {
            val restored = restoreSession()
            if (restored is AuthSessionState.Authenticated) restored.session else null
        }
    }

    companion object {
        private const val PREFS_NAME = "darino_persistent_auth_session"
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USERNAME = "auth_username"
        private const val KEY_PHONE = "auth_user_phone"
        private const val KEY_VERIFIED = "auth_phone_verified"
        private const val KEY_CREATED_AT = "auth_created_at"
        private const val KEY_LAST_LOGIN_AT = "auth_last_login_at"
    }
}
