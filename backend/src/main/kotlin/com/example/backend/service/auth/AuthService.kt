package com.example.backend.service.auth

import com.example.backend.domain.RefreshToken
import com.example.backend.domain.User
import com.example.backend.dto.*
import com.example.backend.exception.ApiException
import com.example.backend.exception.ErrorCode
import com.example.backend.repository.RefreshTokenRepository
import com.example.backend.repository.UserRepository
import com.example.backend.security.JwtTokenProvider
import com.example.backend.service.phone.PhoneNormalizationService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val phoneNormalizationService: PhoneNormalizationService
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val canonicalPhone = request.phoneNumber?.let {
            phoneNormalizationService.normalizeIranianPhoneNumber(it)
        }

        if (request.email != null && userRepository.existsByEmail(request.email)) {
            throw ApiException(ErrorCode.USER_ALREADY_EXISTS, "کاربری با این ایمیل قبلاً ثبت نام کرده است.")
        }
        if (canonicalPhone != null && userRepository.existsByPhoneNumber(canonicalPhone)) {
            throw ApiException(ErrorCode.USER_ALREADY_EXISTS, "کاربری با این شماره همراه قبلاً ثبت نام کرده است.")
        }

        val user = User(
            email = request.email,
            phoneNumber = canonicalPhone,
            phoneVerified = false,
            passwordHash = passwordEncoder.encode(request.password),
            fullName = request.fullName,
            timezone = request.timezone ?: "Asia/Tehran"
        )
        val savedUser = userRepository.save(user)

        return generateAuthResponse(savedUser)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val identifier = request.identifier.trim()
        val user = if (identifier.contains("@")) {
            userRepository.findByEmail(identifier)
        } else {
            val normalized = try {
                phoneNormalizationService.normalizeIranianPhoneNumber(identifier)
            } catch (e: Exception) {
                identifier
            }
            userRepository.findByPhoneNumber(normalized)
        }.orElseThrow { ApiException(ErrorCode.INVALID_CREDENTIALS) }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ApiException(ErrorCode.INVALID_CREDENTIALS)
        }

        if (!user.isActive) {
            throw ApiException(ErrorCode.FORBIDDEN, "حساب کاربری شما مسدود شده است.")
        }

        return generateAuthResponse(user)
    }

    @Transactional
    fun refresh(request: RefreshTokenRequest): AuthResponse {
        val tokenHash = hashToken(request.refreshToken)
        val refreshToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
            .orElseThrow { ApiException(ErrorCode.UNAUTHORIZED, "توکن بازنشانی نامعتبر یا منقضی است.") }

        if (Instant.now().isAfter(refreshToken.expiresAt)) {
            refreshToken.revoked = true
            refreshTokenRepository.save(refreshToken)
            throw ApiException(ErrorCode.UNAUTHORIZED, "توکن بازنشانی منقضی شده است.")
        }

        // Token rotation: revoke current refresh token and generate a new pair
        refreshToken.revoked = true
        refreshTokenRepository.save(refreshToken)

        return generateAuthResponse(refreshToken.user)
    }

    @Transactional
    fun logout(userId: UUID) {
        val tokens = refreshTokenRepository.findByUserId(userId)
        tokens.forEach { it.revoked = true }
        refreshTokenRepository.saveAll(tokens)
    }

    private fun generateAuthResponse(user: User): AuthResponse {
        val accessToken = jwtTokenProvider.generateAccessToken(user)
        val rawRefreshToken = UUID.randomUUID().toString() + UUID.randomUUID().toString()
        val tokenHash = hashToken(rawRefreshToken)

        val expiresAt = Instant.now().plusMillis(jwtTokenProvider.refreshExpirationMs)
        val refreshToken = RefreshToken(
            user = user,
            tokenHash = tokenHash,
            expiresAt = expiresAt
        )
        refreshTokenRepository.save(refreshToken)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = rawRefreshToken,
            expiresInMs = jwtTokenProvider.accessExpirationMs,
            user = UserDto(
                id = user.id!!,
                email = user.email,
                phoneNumber = user.phoneNumber,
                phoneVerified = user.phoneVerified,
                fullName = user.fullName,
                timezone = user.timezone,
                createdAt = user.createdAt
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(token.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
