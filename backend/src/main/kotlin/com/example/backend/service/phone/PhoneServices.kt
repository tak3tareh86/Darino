package com.example.backend.service.phone

import com.example.backend.domain.PhoneVerification
import com.example.backend.domain.User
import com.example.backend.dto.PhoneStatusResponse
import com.example.backend.dto.SendOtpResponse
import com.example.backend.exception.ApiException
import com.example.backend.exception.ErrorCode
import com.example.backend.repository.PhoneVerificationRepository
import com.example.backend.repository.UserRepository
import com.example.backend.service.sms.SmsProviderFactory
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class PhoneNormalizationService {
    
    /**
     * Normalizes Iranian mobile phone numbers to strict E.164 format (+989xxxxxxxxx)
     */
    fun normalizeIranianPhoneNumber(rawPhone: String): String {
        val digitsOnly = rawPhone.replace(Regex("[^0-9+]"), "").trim()
        
        val normalized = when {
            digitsOnly.startsWith("+98") && digitsOnly.length == 13 -> digitsOnly
            digitsOnly.startsWith("0098") && digitsOnly.length == 14 -> "+98" + digitsOnly.substring(4)
            digitsOnly.startsWith("09") && digitsOnly.length == 11 -> "+98" + digitsOnly.substring(1)
            digitsOnly.startsWith("9") && digitsOnly.length == 10 -> "+98$digitsOnly"
            else -> throw ApiException(
                ErrorCode.INVALID_PHONE_NUMBER,
                "شماره همراه وارد شده معتبر نیست. لطفاً شماره موبایل ایرانی ۱۱ رقمی وارد نمایید."
            )
        }

        // Validate Iranian mobile operator prefixes: 90, 91, 92, 93, 94, 99
        val nationalPart = normalized.substring(3) // 10 digits starting with 9
        if (!nationalPart.matches(Regex("^9(0[1-5]|1[0-9]|2[0-3]|3[0-9]|9[0-9])[0-9]{7}$"))) {
            throw ApiException(
                ErrorCode.INVALID_PHONE_NUMBER,
                "پیش‌شماره موبایل وارد شده متعلق به اپراتورهای معتبر کشور نیست."
            )
        }

        return normalized
    }

    fun maskPhoneNumber(normalizedPhone: String): String {
        if (normalizedPhone.length < 10) return normalizedPhone
        val local = if (normalizedPhone.startsWith("+98")) "0" + normalizedPhone.substring(3) else normalizedPhone
        return if (local.length == 11) {
            local.substring(0, 4) + "••••" + local.substring(8)
        } else {
            normalizedPhone.take(4) + "••••" + normalizedPhone.takeLast(3)
        }
    }
}

@Service
class PhoneVerificationService(
    private val phoneVerificationRepository: PhoneVerificationRepository,
    private val userRepository: UserRepository,
    private val normalizationService: PhoneNormalizationService,
    private val passwordEncoder: PasswordEncoder,
    private val smsProviderFactory: SmsProviderFactory
) {
    private val log = LoggerFactory.getLogger(PhoneVerificationService::class.java)
    private val secureRandom = SecureRandom()

    private val otpExpirationMinutes = 3L
    private val resendCooldownSeconds = 90L
    private val maxAttempts = 5
    private val maxRequestsPerHour = 5

    @Transactional
    fun sendOtp(rawPhoneNumber: String, clientIp: String?, currentUserId: UUID?): SendOtpResponse {
        val canonicalPhone = normalizationService.normalizeIranianPhoneNumber(rawPhoneNumber)
        val now = Instant.now()

        // 1. Rate Limiting Check (Phone & IP)
        val oneHourAgo = now.minus(Duration.ofHours(1))
        val recentPhoneRequests = phoneVerificationRepository.countRequestsSince(canonicalPhone, oneHourAgo)
        if (recentPhoneRequests >= maxRequestsPerHour) {
            throw ApiException(ErrorCode.RATE_LIMIT_EXCEEDED, "تعداد درخواست‌های کد تایید برای این شماره بیش از حد مجاز است. لطفاً یک ساعت دیگر تلاش کنید.")
        }

        if (clientIp != null) {
            val recentIpRequests = phoneVerificationRepository.countIpRequestsSince(clientIp, oneHourAgo)
            if (recentIpRequests >= 20) {
                throw ApiException(ErrorCode.RATE_LIMIT_EXCEEDED, "تعداد درخواست‌ها از این آدرس بیش از حد مجاز است.")
            }
        }

        // 2. Cooldown check on previous OTP
        val lastVerification = phoneVerificationRepository.findTopByPhoneNumberAndStatusOrderByCreatedAtDesc(canonicalPhone, "PENDING")
        if (lastVerification.isPresent) {
            val prev = lastVerification.get()
            if (now.isBefore(prev.resendAvailableAt)) {
                val remainingSeconds = Duration.between(now, prev.resendAvailableAt).seconds
                throw ApiException(
                    ErrorCode.OTP_COOLDOWN_ACTIVE,
                    "لطفاً $remainingSeconds ثانیه دیگر برای دریافت مجدد کد شکیبا باشید."
                )
            }
            // Invalidate old pending verification
            prev.status = "EXPIRED"
            phoneVerificationRepository.save(prev)
        }

        // 3. Generate secure OTP (5 digits: 10000..99999)
        val otpCode = (10000 + secureRandom.nextInt(90000)).toString()
        val otpHash = passwordEncoder.encode(otpCode)

        val currentUser = currentUserId?.let { userRepository.findById(it).orElse(null) }

        val verification = PhoneVerification(
            user = currentUser,
            phoneNumber = canonicalPhone,
            otpHash = otpHash,
            status = "PENDING",
            attemptsCount = 0,
            maxAttempts = maxAttempts,
            expiresAt = now.plus(Duration.ofMinutes(otpExpirationMinutes)),
            resendAvailableAt = now.plus(Duration.ofSeconds(resendCooldownSeconds)),
            ipAddress = clientIp
        )
        phoneVerificationRepository.save(verification)

        // 4. Dispatch OTP via SMS Provider
        val provider = smsProviderFactory.getProvider()
        try {
            provider.sendOtp(canonicalPhone, otpCode)
        } catch (ex: Exception) {
            log.error("Failed to send OTP SMS to $canonicalPhone via provider ${provider.providerName}", ex)
            throw ApiException(ErrorCode.SMS_PROVIDER_ERROR, "خطا در ارسال پیامک کد تایید. لطفاً مجدداً تلاش کنید.")
        }

        return SendOtpResponse(
            phoneNumberMasked = normalizationService.maskPhoneNumber(canonicalPhone),
            status = "PENDING",
            resendAvailableInSeconds = resendCooldownSeconds,
            expiresInSeconds = otpExpirationMinutes * 60
        )
    }

    @Transactional
    fun verifyOtp(rawPhoneNumber: String, code: String, currentUserId: UUID?): PhoneStatusResponse {
        val canonicalPhone = normalizationService.normalizeIranianPhoneNumber(rawPhoneNumber)
        val now = Instant.now()

        val verification = phoneVerificationRepository.findTopByPhoneNumberAndStatusOrderByCreatedAtDesc(canonicalPhone, "PENDING")
            .orElseThrow {
                ApiException(ErrorCode.OTP_INVALID, "کد تاییدی برای این شماره ثبت نشده یا منقضی شده است.")
            }

        if (now.isAfter(verification.expiresAt)) {
            verification.status = "EXPIRED"
            phoneVerificationRepository.save(verification)
            throw ApiException(ErrorCode.OTP_EXPIRED, "کد تایید منقضی شده است. لطفاً درخواست کد مجدد دهید.")
        }

        if (verification.attemptsCount >= verification.maxAttempts) {
            verification.status = "LOCKED"
            phoneVerificationRepository.save(verification)
            throw ApiException(ErrorCode.OTP_MAX_ATTEMPTS_REACHED, "تعداد تلاش‌های ناموفق بیش از حد مجاز است.")
        }

        val isValid = passwordEncoder.matches(code, verification.otpHash)
        if (!isValid) {
            verification.attemptsCount += 1
            if (verification.attemptsCount >= verification.maxAttempts) {
                verification.status = "LOCKED"
            }
            phoneVerificationRepository.save(verification)
            throw ApiException(ErrorCode.OTP_INVALID, "کد تایید وارد شده نادرست است.")
        }

        // Verification Succeeded
        verification.status = "VERIFIED"
        verification.verifiedAt = now
        phoneVerificationRepository.save(verification)

        // Update User state if authenticated
        if (currentUserId != null) {
            val user = userRepository.findById(currentUserId).orElse(null)
            if (user != null) {
                user.phoneNumber = canonicalPhone
                user.phoneVerified = true
                user.phoneVerifiedAt = now
                userRepository.save(user)
            }
        }

        return PhoneStatusResponse(
            phoneNumber = canonicalPhone,
            phoneNumberMasked = normalizationService.maskPhoneNumber(canonicalPhone),
            isVerified = true,
            verifiedAt = now
        )
    }

    @Transactional(readOnly = true)
    fun getPhoneStatus(userId: UUID): PhoneStatusResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ApiException(ErrorCode.NOT_FOUND, "کاربر یافت نشد.") }
        
        val masked = user.phoneNumber?.let { normalizationService.maskPhoneNumber(it) }
        return PhoneStatusResponse(
            phoneNumber = user.phoneNumber,
            phoneNumberMasked = masked,
            isVerified = user.phoneVerified,
            verifiedAt = user.phoneVerifiedAt
        )
    }
}
