package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.ApiClient
import com.example.data.api.NetworkErrorMapper
import com.example.data.api.NetworkLoginRequest
import com.example.data.api.NetworkRegisterRequest
import com.example.data.database.AppDatabase
import com.example.data.database.UserEntity
import com.example.data.model.UserProfile
import com.example.data.security.AuthSessionManager
import com.example.data.security.AuthSessionState
import com.example.data.security.SessionManager
import com.example.data.security.TokenManager
import com.example.util.PasswordHasher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Real Production Remote Authentication Repository.
 * Handles Username/Password registration and login via REST API, JWT token storage in KeyStore,
 * session persistence, and seamless local fallback when backend is offline.
 */
class RemoteAuthRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) : AuthRepository {

    private val userDao = database.userDao()
    private val sessionManager = AuthSessionManager(context)
    private val tokenManager = TokenManager.getInstance(context)
    private val localAuthRepository = MockAuthRepository(context, database)

    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    private val _currentUser = MutableStateFlow<UserProfile?>(null)

    init {
        restoreSessionState()
    }

    private fun restoreSessionState() {
        val sessionState = sessionManager.restoreSession()
        if (sessionState is AuthSessionState.Authenticated) {
            val session = sessionState.session
            val user = UserProfile(
                id = session.userId,
                username = session.username,
                phoneNumber = session.phoneNumber,
                phoneVerified = session.phoneVerified,
                createdAt = session.createdAt
            )
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
        } else {
            _currentUser.value = null
            _authState.value = AuthState.LoggedOut
        }
    }

    override suspend fun login(username: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim()
        if (cleanUsername.isBlank() || password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("نام کاربری و رمز عبور نمی‌توانند خالی باشند."))
        }

        _authState.value = AuthState.Authenticating

        // 1. Check local Room database first for offline-first instant reliability
        val localUser = userDao.findByUsername(cleanUsername)
        if (localUser != null && localUser.passwordHash != null && localUser.salt != null) {
            val passwordValid = PasswordHasher.verifyPassword(password, localUser.salt, localUser.passwordHash)
            if (passwordValid) {
                val userProfile = UserProfile(
                    id = localUser.id,
                    username = cleanUsername,
                    email = localUser.email,
                    phoneNumber = localUser.phoneNumber,
                    phoneVerified = true,
                    createdAt = localUser.verifiedAt ?: System.currentTimeMillis()
                )
                sessionManager.createSession(localUser.id, cleanUsername, localUser.phoneNumber)
                _currentUser.value = userProfile
                _authState.value = AuthState.Authenticated(userProfile)
                return@withContext Result.success(userProfile)
            } else {
                _authState.value = AuthState.Error("نام کاربری یا رمز عبور اشتباه است.")
                return@withContext Result.failure(IllegalArgumentException("نام کاربری یا رمز عبور اشتباه است."))
            }
        }

        // 2. If not found in local DB, attempt network login with remote backend
        try {
            val response = withTimeoutOrNull(4000L) {
                ApiClient.authApi.login(NetworkLoginRequest(usernameOrPhone = cleanUsername, password = password))
            }

            if (response != null && response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()?.data
                if (authData != null) {
                    tokenManager.saveTokens(authData.accessToken, authData.refreshToken)

                    val netUser = authData.user
                    val userId = netUser.id
                    val userProfile = UserProfile(
                        id = userId,
                        username = cleanUsername,
                        email = netUser.email,
                        phoneNumber = netUser.phoneNumber,
                        phoneVerified = netUser.phoneVerified
                    )

                    sessionManager.createSession(userId, userProfile.username, netUser.phoneNumber)

                    val salt = PasswordHasher.generateSalt()
                    val passwordHash = PasswordHasher.hashPassword(password, salt)

                    userDao.insertUser(
                        UserEntity(
                            id = userId,
                            username = cleanUsername,
                            email = netUser.email,
                            passwordHash = passwordHash,
                            salt = salt,
                            phoneNumber = netUser.phoneNumber,
                            phoneNumberMasked = netUser.phoneNumberMasked,
                            verificationStatus = "VERIFIED",
                            verifiedAt = System.currentTimeMillis()
                        )
                    )

                    _currentUser.value = userProfile
                    _authState.value = AuthState.Authenticated(userProfile)

                    return@withContext Result.success(userProfile)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w("RemoteAuthRepository", "Backend login unreachable (${e.message})")
        }

        _authState.value = AuthState.Error("نام کاربری یا رمز عبور اشتباه است.")
        return@withContext Result.failure(IllegalArgumentException("نام کاربری یا رمز عبور اشتباه است."))
    }

    override suspend fun register(username: String, password: String, email: String?): Result<UserProfile> = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim()
        val cleanEmail = email?.trim()?.lowercase()

        if (cleanUsername.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("نام کاربری نمی‌تواند خالی باشد."))
        }

        if (cleanUsername.length < 3) {
            return@withContext Result.failure(IllegalArgumentException("نام کاربری حداقل باید ۳ کاراکتر باشد."))
        }

        if (!cleanUsername.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            return@withContext Result.failure(IllegalArgumentException("نام کاربری فقط می‌تواند شامل حروف انگلیسی، اعداد و زیرخط (_) باشد."))
        }

        if (password.length < 8) {
            return@withContext Result.failure(IllegalArgumentException("رمز عبور باید حداقل ۸ کاراکتر باشد."))
        }

        val existing = userDao.findByUsername(cleanUsername)
        if (existing != null) {
            _authState.value = AuthState.Error("این نام کاربری قبلاً استفاده شده است.")
            return@withContext Result.failure(IllegalArgumentException("این نام کاربری قبلاً استفاده شده است."))
        }

        if (cleanEmail != null && cleanEmail.isNotBlank()) {
            val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            if (!cleanEmail.matches(emailRegex)) {
                return@withContext Result.failure(IllegalArgumentException("فرمت ایمیل نامعتبر است."))
            }
            val existingEmail = userDao.findByEmail(cleanEmail)
            if (existingEmail != null) {
                _authState.value = AuthState.Error("این ایمیل قبلاً برای حساب دیگری ثبت شده است.")
                return@withContext Result.failure(IllegalArgumentException("این ایمیل قبلاً برای حساب دیگری ثبت شده است."))
            }
        }

        _authState.value = AuthState.Authenticating

        try {
            val response = withTimeoutOrNull(4000L) {
                ApiClient.authApi.register(
                    NetworkRegisterRequest(
                        username = cleanUsername,
                        email = cleanEmail,
                        phoneNumber = null,
                        password = password,
                        fullName = cleanUsername
                    )
                )
            }

            if (response != null && response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()?.data
                if (authData != null) {
                    tokenManager.saveTokens(authData.accessToken, authData.refreshToken)

                    val netUser = authData.user
                    val userId = netUser.id
                    val userProfile = UserProfile(
                        id = userId,
                        username = cleanUsername,
                        email = cleanEmail ?: netUser.email,
                        phoneNumber = netUser.phoneNumber,
                        phoneVerified = true
                    )

                    sessionManager.createSession(userId, cleanUsername, netUser.phoneNumber)

                    val salt = PasswordHasher.generateSalt()
                    val passwordHash = PasswordHasher.hashPassword(password, salt)

                    userDao.insertUser(
                        UserEntity(
                            id = userId,
                            username = cleanUsername,
                            email = cleanEmail ?: netUser.email,
                            passwordHash = passwordHash,
                            salt = salt,
                            phoneNumber = netUser.phoneNumber,
                            phoneNumberMasked = netUser.phoneNumberMasked,
                            verificationStatus = "VERIFIED",
                            verifiedAt = System.currentTimeMillis()
                        )
                    )

                    _currentUser.value = userProfile
                    _authState.value = AuthState.Authenticated(userProfile)

                    return@withContext Result.success(userProfile)
                }
            }

            if (response != null && !response.isSuccessful) {
                val mappedError = NetworkErrorMapper.mapHttpResponse(response)
                val serverMsg = response.body()?.error?.message ?: mappedError.userFacingMessage
                _authState.value = AuthState.Error(serverMsg)
                return@withContext Result.failure(Exception(serverMsg))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w("RemoteAuthRepository", "Backend register unreachable (${e.message}), registering in local DB.")
        }

        // Offline Fallback: Register locally in Room DB
        val localResult = localAuthRepository.register(cleanUsername, password, cleanEmail)
        localResult.fold(
            onSuccess = { user ->
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
            },
            onFailure = { err ->
                _authState.value = AuthState.Error(err.message ?: "خطا در ثبت نام.")
            }
        )
        return@withContext localResult
    }

    override suspend fun recoverPasswordByEmail(email: String): Result<PasswordRecoveryResult> = withContext(Dispatchers.IO) {
        localAuthRepository.recoverPasswordByEmail(email)
    }

    override suspend fun getCurrentUser(): UserProfile? = withContext(Dispatchers.IO) {
        val session = sessionManager.getActiveSession()
        if (session != null) {
            UserProfile(
                id = session.userId,
                username = session.username,
                phoneNumber = session.phoneNumber,
                phoneVerified = session.phoneVerified,
                createdAt = session.createdAt
            )
        } else null
    }

    override fun observeCurrentUser(): Flow<UserProfile?> = _currentUser.asStateFlow()

    override fun observeAuthState(): Flow<AuthState> = _authState.asStateFlow()

    override suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        sessionManager.hasValidSession()
    }

    override suspend fun logout(): Unit = withContext(Dispatchers.IO) {
        // 1. Immediately clear local session, tokens, and state for instantaneous UI response
        tokenManager.clear()
        sessionManager.clearSession()
        _currentUser.value = null
        _authState.value = AuthState.LoggedOut

        // 2. Fire-and-forget remote logout notification in background without blocking UI
        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ApiClient.authApi.logout()
                } catch (e: Exception) {
                    Log.w("RemoteAuthRepository", "Remote logout notify failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w("RemoteAuthRepository", "Server logout endpoint call skipped: ${e.message}")
        }
        Unit
    }
}
