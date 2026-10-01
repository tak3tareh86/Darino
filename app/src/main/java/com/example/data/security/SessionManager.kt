package com.example.data.security

import android.content.Context
import android.util.Log
import com.example.data.api.NetworkAuthResponse
import com.example.data.api.NetworkUserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SessionState {
    object LoggedOut : SessionState()
    object Authenticating : SessionState()
    data class Authenticated(val user: NetworkUserDto) : SessionState()
    data class PhoneVerificationRequired(val user: NetworkUserDto) : SessionState()
    object SessionExpired : SessionState()
}

object SessionManager {
    private const val TAG = "SessionManager"

    private var tokenManager: TokenManager? = null

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    val accessToken: String?
        get() = tokenManager?.getAccessToken()

    val refreshToken: String?
        get() = tokenManager?.getRefreshToken()

    val baseUrl: String
        get() = tokenManager?.getBaseUrl() ?: TokenManager.DEFAULT_BASE_URL

    val currentUser: NetworkUserDto?
        get() = when (val state = _sessionState.value) {
            is SessionState.Authenticated -> state.user
            is SessionState.PhoneVerificationRequired -> state.user
            else -> null
        }

    val userId: String?
        get() = currentUser?.id

    val isPhoneVerified: Boolean
        get() = currentUser?.phoneVerified == true

    fun init(context: Context) {
        val tm = TokenManager.getInstance(context)
        tokenManager = tm

        val token = tm.getAccessToken()
        val user = tm.getUser()

        if (token != null && user != null) {
            if (user.phoneVerified) {
                _sessionState.value = SessionState.Authenticated(user)
                Log.i(TAG, "Restored active session for user: ${user.phoneNumber ?: user.email}")
            } else {
                _sessionState.value = SessionState.PhoneVerificationRequired(user)
                Log.i(TAG, "Restored session needing phone verification for user: ${user.phoneNumber ?: user.email}")
            }
        } else {
            _sessionState.value = SessionState.LoggedOut
        }
    }

    fun setBaseUrl(url: String) {
        tokenManager?.saveBaseUrl(url)
    }

    fun onAuthenticating() {
        _sessionState.value = SessionState.Authenticating
    }

    fun onLoginSuccess(authResponse: NetworkAuthResponse) {
        tokenManager?.saveTokens(
            accessToken = authResponse.accessToken,
            refreshToken = authResponse.refreshToken,
            expiresInMs = authResponse.expiresInMs
        )
        tokenManager?.saveUser(authResponse.user)

        if (authResponse.user.phoneVerified) {
            _sessionState.value = SessionState.Authenticated(authResponse.user)
        } else {
            _sessionState.value = SessionState.PhoneVerificationRequired(authResponse.user)
        }
        Log.i(TAG, "Login successful for user: ${authResponse.user.id}")
    }

    fun onPhoneVerified(updatedUser: NetworkUserDto) {
        tokenManager?.saveUser(updatedUser)
        _sessionState.value = SessionState.Authenticated(updatedUser)
        Log.i(TAG, "Phone verified for user: ${updatedUser.id}")
    }

    fun onTokensRefreshed(newAccessToken: String, newRefreshToken: String, expiresInMs: Long = 0) {
        tokenManager?.saveTokens(newAccessToken, newRefreshToken, expiresInMs)
        Log.i(TAG, "Tokens refreshed and saved securely")
    }

    fun onSessionExpired() {
        tokenManager?.clear()
        _sessionState.value = SessionState.SessionExpired
        Log.w(TAG, "Session expired, tokens cleared")
    }

    fun logout() {
        tokenManager?.clear()
        _sessionState.value = SessionState.LoggedOut
        Log.i(TAG, "User logged out")
    }

    fun updateFullName(newFullName: String) {
        val user = currentUser ?: run {
            Log.w(TAG, "Ignoring full-name update because there is no authenticated user")
            return
        }
        val updatedUser = user.copy(fullName = newFullName)
        tokenManager?.saveUser(updatedUser)
        
        when (val state = _sessionState.value) {
            is SessionState.Authenticated -> {
                _sessionState.value = SessionState.Authenticated(updatedUser)
            }
            is SessionState.PhoneVerificationRequired -> {
                _sessionState.value = SessionState.PhoneVerificationRequired(updatedUser)
            }
            else -> {
                _sessionState.value = SessionState.Authenticated(updatedUser)
            }
        }
        Log.i(TAG, "Updated user full name to: $newFullName")
    }
}
