package com.financemanager.backend.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false, unique = true, length = 100)
    var username: String,

    @Column(unique = true, length = 255)
    var email: String? = null,

    @Column(name = "phone_number", unique = true, length = 20)
    var phoneNumber: String? = null,

    @Column(name = "phone_verified", nullable = false)
    var phoneVerified: Boolean = false,

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,

    @Column(name = "full_name", length = 150)
    var fullName: String? = null,

    @Column(nullable = false, length = 50)
    var role: String = "ROLE_USER",

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    val tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(nullable = false)
    var revoked: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "phone_verifications")
data class PhoneVerification(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "user_id")
    var userId: String? = null,

    @Column(name = "phone_number", nullable = false, length = 20)
    val phoneNumber: String,

    @Column(name = "otp_hash", nullable = false, length = 255)
    var otpHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: VerificationStatus = VerificationStatus.PENDING,

    @Column(nullable = false)
    var attempts: Int = 0,

    @Column(name = "max_attempts", nullable = false)
    val maxAttempts: Int = 5,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "resend_available_at", nullable = false)
    var resendAvailableAt: Instant,

    @Column(name = "verified_at")
    var verifiedAt: Instant? = null,

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class VerificationStatus {
    PENDING,
    VERIFIED,
    EXPIRED,
    LOCKED
}
