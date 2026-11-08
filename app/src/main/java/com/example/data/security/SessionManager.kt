package com.example.data.security

import android.content.Context
import com.example.data.security.AuthSession
import com.example.data.security.AuthSessionManager
import com.example.data.security.AuthSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Compatibility facade for user-scoped repositories.
 *
 * The old server-backed session contract is now backed exclusively by the
 * persistent device-local AuthSessionManager. No tokens or network services
 * are involved. Keeping this facade preserves existing repository boundaries
 * while the application remains fully local.
 */
data class SessionUser(
    val id: String,
    val username: String,
    val fullName: String?,
    val phoneNumber: String?,
    val phoneNumberMasked: String? = null,
    val email: String? = null,
    val phoneVerified: Boolean = true
)

sealed class SessionState {
    object LoggedOut : SessionState()
    object Authenticating : SessionState()
    data class Authenticated(val user: SessionUser) : SessionState()
    data class PhoneVerificationRequired(val user: SessionUser) : SessionState()
    object SessionExpired : SessionState()
}

object SessionManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    @Volatile
    private var localManager: AuthSessionManager? = null

    @Volatile
    private var observerStarted = false

    val currentUser: SessionUser?
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
        val manager = AuthSessionManager.getInstance(context.applicationContext)
        synchronized(this) {
            localManager = manager
            publish(manager.restoreSession())
            if (!observerStarted) {
                observerStarted = true
                scope.launch {
                    manager.sessionState.collect(::publish)
                }
            }
        }
    }

    fun updateFullName(newFullName: String) {
        localManager?.updateFullName(newFullName)
    }

    fun logout() {
        localManager?.clearSession()
    }

    private fun publish(state: AuthSessionState) {
        _sessionState.value = when (state) {
            AuthSessionState.Loading -> SessionState.Authenticating
            AuthSessionState.Unauthenticated,
            AuthSessionState.LoggedOut -> SessionState.LoggedOut
            is AuthSessionState.Authenticated -> {
                val session = state.session
                val user = SessionUser(
                    id = session.userId,
                    username = session.username,
                    fullName = session.fullName,
                    phoneNumber = session.phoneNumber,
                    phoneVerified = session.phoneVerified
                )
                if (session.phoneVerified) SessionState.Authenticated(user)
                else SessionState.PhoneVerificationRequired(user)
            }
        }
    }
}
