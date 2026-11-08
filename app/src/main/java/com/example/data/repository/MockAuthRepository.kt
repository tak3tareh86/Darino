package com.example.data.repository

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.UserEntity
import com.example.data.model.UserProfile
import com.example.data.security.AuthSessionManager
import com.example.data.security.AuthSessionState
import com.example.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Local Authentication Repository using Room Database and AuthSessionManager for persistent sessions.
 */
class MockAuthRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) : AuthRepository {

    private val userDao = database.userDao()
    private val sessionManager = AuthSessionManager(context)

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
        delay(300) // Brief UI feedback delay

        val existingUser = userDao.findByUsername(cleanUsername)
        if (existingUser == null || existingUser.passwordHash == null || existingUser.salt == null) {
            _authState.value = AuthState.Error("نام کاربری یا رمز عبور اشتباه است.")
            return@withContext Result.failure(IllegalArgumentException("نام کاربری یا رمز عبور اشتباه است."))
        }

        val passwordValid = PasswordHasher.verifyPassword(password, existingUser.salt, existingUser.passwordHash)
        if (!passwordValid) {
            _authState.value = AuthState.Error("نام کاربری یا رمز عبور اشتباه است.")
            return@withContext Result.failure(IllegalArgumentException("نام کاربری یا رمز عبور اشتباه است."))
        }

        val userProfile = UserProfile(
            id = existingUser.id,
            username = cleanUsername,
            email = existingUser.email,
            phoneNumber = existingUser.phoneNumber,
            phoneVerified = true,
            createdAt = existingUser.verifiedAt ?: System.currentTimeMillis()
        )

        sessionManager.createSession(existingUser.id, cleanUsername, existingUser.phoneNumber)

        _currentUser.value = userProfile
        _authState.value = AuthState.Authenticated(userProfile)

        Result.success(userProfile)
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

        if (cleanEmail != null && cleanEmail.isNotBlank()) {
            val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            if (!cleanEmail.matches(emailRegex)) {
                return@withContext Result.failure(IllegalArgumentException("فرمت ایمیل نامعتبر است."))
            }
            val existingEmail = userDao.findByEmail(cleanEmail)
            if (existingEmail != null) {
                return@withContext Result.failure(IllegalArgumentException("این ایمیل قبلاً برای حساب دیگری ثبت شده است."))
            }
        }

        _authState.value = AuthState.Authenticating
        delay(400)

        val existing = userDao.findByUsername(cleanUsername)
        if (existing != null) {
            _authState.value = AuthState.Error("این نام کاربری قبلاً استفاده شده است.")
            return@withContext Result.failure(IllegalArgumentException("این نام کاربری قبلاً استفاده شده است."))
        }

        val salt = PasswordHasher.generateSalt()
        val passwordHash = PasswordHasher.hashPassword(password, salt)
        val userId = "usr_loc_${System.currentTimeMillis()}"

        val userEntity = UserEntity(
            id = userId,
            username = cleanUsername,
            email = cleanEmail,
            passwordHash = passwordHash,
            salt = salt,
            verificationStatus = "VERIFIED",
            verifiedAt = System.currentTimeMillis()
        )

        userDao.insertUser(userEntity)

        val userProfile = UserProfile(
            id = userId,
            username = cleanUsername,
            email = cleanEmail,
            phoneVerified = true,
            createdAt = System.currentTimeMillis()
        )

        sessionManager.createSession(userId, cleanUsername)

        _currentUser.value = userProfile
        _authState.value = AuthState.Authenticated(userProfile)

        Result.success(userProfile)
    }

    override suspend fun recoverPasswordByEmail(email: String): Result<PasswordRecoveryResult> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("لطفاً ایمیل یا جیمیل خود را وارد کنید."))
        }
        val user = userDao.findByEmail(cleanEmail)
        if (user == null) {
            return@withContext Result.failure(IllegalArgumentException("هیچ حساب کاربری با این ایمیل یافت نشد."))
        }

        val newTempPassword = "Darino@" + (1000..9999).random()
        val newSalt = PasswordHasher.generateSalt()
        val newHash = PasswordHasher.hashPassword(newTempPassword, newSalt)
        userDao.updatePassword(user.id, newHash, newSalt)

        val username = user.username ?: "کاربر"
        Result.success(
            PasswordRecoveryResult(
                username = username,
                newTemporaryPassword = newTempPassword,
                email = cleanEmail
            )
        )
    }

    override suspend fun getCurrentUser(): UserProfile? = withContext(Dispatchers.IO) {
        val session = sessionManager.getActiveSession()
        if (session != null) {
            val profile = UserProfile(
                id = session.userId,
                username = session.username,
                phoneNumber = session.phoneNumber,
                phoneVerified = session.phoneVerified,
                createdAt = session.createdAt
            )
            _currentUser.value = profile
            profile
        } else {
            _currentUser.value = null
            null
        }
    }

    override fun observeCurrentUser(): Flow<UserProfile?> = _currentUser.asStateFlow()

    override fun observeAuthState(): Flow<AuthState> = _authState.asStateFlow()

    override suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        sessionManager.hasValidSession()
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        sessionManager.clearSession()
        _currentUser.value = null
        _authState.value = AuthState.LoggedOut
    }
}
