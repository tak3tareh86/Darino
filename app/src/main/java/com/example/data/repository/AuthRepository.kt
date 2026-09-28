package com.example.data.repository

import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

data class PasswordRecoveryResult(
    val username: String,
    val newTemporaryPassword: String,
    val email: String
)

/**
 * High-level authentication states for the UI state machine.
 */
sealed class AuthState {
    object LoggedOut : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Core Authentication Repository abstraction for Username + Password authentication.
 */
interface AuthRepository {
    /**
     * Authenticates user with username and password credentials.
     */
    suspend fun login(username: String, password: String): Result<UserProfile>

    /**
     * Registers a new user with username, password, and optional email.
     */
    suspend fun register(username: String, password: String, email: String? = null): Result<UserProfile>

    /**
     * Recovers password by registered Gmail / email, generating a new temporary password.
     */
    suspend fun recoverPasswordByEmail(email: String): Result<PasswordRecoveryResult>

    /**
     * Retrieves the currently active user session from local storage.
     */
    suspend fun getCurrentUser(): UserProfile?

    /**
     * Observable flow of the active user profile.
     */
    fun observeCurrentUser(): Flow<UserProfile?>

    /**
     * Observable flow of high-level auth state.
     */
    fun observeAuthState(): Flow<AuthState>

    /**
     * Checks whether an active authenticated session exists locally.
     */
    suspend fun isLoggedIn(): Boolean

    /**
     * Clears local user session and returns to unauthenticated state.
     */
    suspend fun logout()
}
