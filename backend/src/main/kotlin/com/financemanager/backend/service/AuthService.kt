package com.financemanager.backend.service

import com.financemanager.backend.common.AppException
import com.financemanager.backend.common.ErrorCode
import com.financemanager.backend.domain.RefreshToken
import com.financemanager.backend.domain.User
import com.financemanager.backend.repository.RefreshTokenRepository
import com.financemanager.backend.repository.UserRepository
import com.financemanager.backend.security.JwtTokenProvider
import com.financemanager.backend.util.IranianPhoneNormalizer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class AuthTokensDto(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val username: String,
    val phoneNumber: String?,
    val phoneVerified: Boolean
)

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    @Value("\${app.jwt.refresh-expiration-ms:604800000}") private val refreshExpirationMs: Long
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional
    fun register(username: String, rawPhone: String?, passwordRaw: String, fullName: String?): AuthTokensDto {
        val normalizedUsername = username.trim().lowercase()
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw AppException(ErrorCode.CONFLICT, "این نام کاربری قبلاً ثبت شده است.", HttpStatus.CONFLICT)
        }

        val normalizedPhone = rawPhone?.let {
            val norm = IranianPhoneNormalizer.normalize(it)
                ?: throw AppException(ErrorCode.VALIDATION_ERROR, "شماره موبایل وارد شده نامعتبر است.")
            if (userRepository.existsByPhoneNumber(norm)) {
                throw AppException(ErrorCode.CONFLICT, "این شماره همراه قبلاً ثبت شده است.", HttpStatus.CONFLICT)
            }
            norm
        }

        val user = User(
            username = normalizedUsername,
            phoneNumber = normalizedPhone,
            phoneVerified = false,
            passwordHash = passwordEncoder.encode(passwordRaw),
            fullName = fullName,
            role = "ROLE_USER"
        )
        val savedUser = userRepository.save(user)
        logger.info("New user registered: {} (ID: {})", savedUser.username, savedUser.id)

        return generateTokensForUser(savedUser)
    }

    @Transactional
    fun login(usernameOrPhone: String, passwordRaw: String): AuthTokensDto {
        val identifier = usernameOrPhone.trim()
        val normalizedPhone = IranianPhoneNormalizer.normalize(identifier)

        val user = (if (normalizedPhone != null) {
            userRepository.findByPhoneNumber(normalizedPhone)
        } else {
            userRepository.findByUsername(identifier.lowercase())
        }).orElseThrow {
            AppException(ErrorCode.UNAUTHORIZED, "نام کاربری یا رمز عبور اشتباه است.", HttpStatus.UNAUTHORIZED)
        }

        if (!passwordEncoder.matches(passwordRaw, user.passwordHash)) {
            throw AppException(ErrorCode.UNAUTHORIZED, "نام کاربری یا رمز عبور اشتباه است.", HttpStatus.UNAUTHORIZED)
        }

        if (!user.isActive) {
            throw AppException(ErrorCode.FORBIDDEN, "حساب کاربری شما غیرفعال شده است.", HttpStatus.FORBIDDEN)
        }

        logger.info("User logged in: {}", user.username)
        return generateTokensForUser(user)
    }

    @Transactional
    fun refresh(refreshTokenRaw: String): AuthTokensDto {
        val tokenHash = hashToken(refreshTokenRaw)
        val storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow { AppException(ErrorCode.UNAUTHORIZED, "توکن نامعتبر یا منقضی شده است.", HttpStatus.UNAUTHORIZED) }

        if (storedToken.revoked || storedToken.expiresAt.isBefore(Instant.now())) {
            throw AppException(ErrorCode.UNAUTHORIZED, "توکن منقضی یا لغو شده است.", HttpStatus.UNAUTHORIZED)
        }

        // Token Rotation: revoke previous refresh token immediately
        storedToken.revoked = true
        refreshTokenRepository.save(storedToken)

        val user = storedToken.user
        return generateTokensForUser(user)
    }

    @Transactional
    fun logout(userId: String) {
        refreshTokenRepository.deleteByUserId(userId)
        logger.info("User logged out and tokens revoked: {}", userId)
    }

    private fun generateTokensForUser(user: User): AuthTokensDto {
        val accessToken = jwtTokenProvider.generateAccessToken(user.id, user.username, user.role)
        
        val rawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString()
        val tokenHash = hashToken(rawRefreshToken)
        val refreshToken = RefreshToken(
            user = user,
            tokenHash = tokenHash,
            expiresAt = Instant.now().plus(Duration.ofMillis(refreshExpirationMs))
        )
        refreshTokenRepository.save(refreshToken)

        return AuthTokensDto(
            accessToken = accessToken,
            refreshToken = rawRefreshToken,
            userId = user.id,
            username = user.username,
            phoneNumber = user.phoneNumber,
            phoneVerified = user.phoneVerified
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(token.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
