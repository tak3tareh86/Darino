package com.financemanager.backend.service

import com.financemanager.backend.common.AppException
import com.financemanager.backend.common.ErrorCode
import com.financemanager.backend.domain.PhoneVerification
import com.financemanager.backend.domain.VerificationStatus
import com.financemanager.backend.repository.PhoneVerificationRepository
import com.financemanager.backend.repository.UserRepository
import com.financemanager.backend.sms.queue.SmsQueueService
import com.financemanager.backend.util.IranianPhoneNormalizer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant

data class SendOtpResultDto(
    val phoneNumberMasked: String,
    val expiresInSeconds: Long,
    val resendCooldownSeconds: Long
)

data class PhoneStatusDto(
    val phoneNumber: String?,
    val phoneNumberMasked: String,
    val isVerified: Boolean,
    val operatorName: String
)

@Service
class PhoneVerificationService(
    private val phoneVerificationRepository: PhoneVerificationRepository,
    private val userRepository: UserRepository,
    private val smsQueueService: SmsQueueService,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${app.phone.otp.expiration-seconds:180}") private val expirationSeconds: Long,
    @Value("\${app.phone.otp.resend-cooldown-seconds:60}") private val resendCooldownSeconds: Long,
    @Value("\${app.phone.otp.max-attempts:5}") private val maxAttempts: Int,
    @Value("\${app.phone.otp.rate-limit-per-hour:5}") private val rateLimitPerHour: Long
) {
    private val logger = LoggerFactory.getLogger(PhoneVerificationService::class.java)
    private val secureRandom = SecureRandom()

    @Transactional
    fun sendOtp(rawPhoneNumber: String, userId: String?, ipAddress: String?): SendOtpResultDto {
        val normalized = IranianPhoneNormalizer.normalize(rawPhoneNumber)
            ?: throw AppException(ErrorCode.VALIDATION_ERROR, "شماره تلفن همراه نامعتبر است. فرمت صحیح: 09xxxxxxxxx")

        val oneHourAgo = Instant.now().minus(Duration.ofHours(1))

        // Rate limit checks
        val phoneCountLastHour = phoneVerificationRepository.countByPhoneNumberAndCreatedAtAfter(normalized, oneHourAgo)
        if (phoneCountLastHour >= rateLimitPerHour) {
            throw AppException(ErrorCode.RATE_LIMITED, "تعداد درخواست‌های کد تایید برای این شماره بیش از حد مجاز است. لطفاً بعداً تلاش کنید.", HttpStatus.TOO_MANY_REQUESTS)
        }

        if (ipAddress != null) {
            val ipCountLastHour = phoneVerificationRepository.countByIpAddressAndCreatedAtAfter(ipAddress, oneHourAgo)
            if (ipCountLastHour >= 20) {
                throw AppException(ErrorCode.RATE_LIMITED, "تعداد درخواست‌ها از این آدرس بیش از حد مجاز است.", HttpStatus.TOO_MANY_REQUESTS)
            }
        }

        // Check if there is an active OTP within cooldown
        val latestOpt = phoneVerificationRepository.findTopByPhoneNumberOrderByCreatedAtDesc(normalized)
        if (latestOpt.isPresent) {
            val latest = latestOpt.get()
            if (latest.status == VerificationStatus.PENDING && latest.resendAvailableAt.isAfter(Instant.now())) {
                val remainingSeconds = Duration.between(Instant.now(), latest.resendAvailableAt).seconds
                throw AppException(ErrorCode.RATE_LIMITED, "لطفاً $remainingSeconds ثانیه دیگر تا ارسال مجدد کد صبر نمایید.", HttpStatus.TOO_MANY_REQUESTS)
            }
        }

        // Generate 5-digit cryptographically secure OTP
        val otpNumber = 10000 + secureRandom.nextInt(90000)
        val otpCode = otpNumber.toString()
        val hashedOtp = passwordEncoder.encode(otpCode)

        val now = Instant.now()
        val verification = PhoneVerification(
            userId = userId,
            phoneNumber = normalized,
            otpHash = hashedOtp,
            status = VerificationStatus.PENDING,
            attempts = 0,
            maxAttempts = maxAttempts,
            expiresAt = now.plus(Duration.ofSeconds(expirationSeconds)),
            resendAvailableAt = now.plus(Duration.ofSeconds(resendCooldownSeconds)),
            ipAddress = ipAddress,
            createdAt = now
        )
        phoneVerificationRepository.save(verification)

        // Queue OTP SMS Job asynchronously with strong idempotency
        val effectiveUserId = userId ?: "ANONYMOUS_VERIFY"
        smsQueueService.enqueueSms(
            userId = effectiveUserId,
            reminderId = null,
            phoneNumber = normalized,
            templateId = "otp_verify_code",
            payload = mapOf("token" to otpCode),
            idempotencyKey = "otp_${verification.id}"
        )

        logger.info("OTP verification created for {} (masked: {})", normalized, IranianPhoneNormalizer.mask(normalized))

        return SendOtpResultDto(
            phoneNumberMasked = IranianPhoneNormalizer.mask(normalized),
            expiresInSeconds = expirationSeconds,
            resendCooldownSeconds = resendCooldownSeconds
        )
    }

    @Transactional
    fun verifyOtp(rawPhoneNumber: String, code: String, userId: String?): Boolean {
        val normalized = IranianPhoneNormalizer.normalize(rawPhoneNumber)
            ?: throw AppException(ErrorCode.VALIDATION_ERROR, "شماره تلفن همراه نامعتبر است.")

        val verification = phoneVerificationRepository.findTopByPhoneNumberOrderByCreatedAtDesc(normalized)
            .orElseThrow { AppException(ErrorCode.OTP_INVALID, "کد تایید معتبری برای این شماره یافت نشد.") }

        if (verification.status == VerificationStatus.VERIFIED) {
            return true
        }

        if (verification.status == VerificationStatus.LOCKED) {
            throw AppException(ErrorCode.OTP_LOCKED, "تعداد دفعات ورود اشتباه بیش از حد مجاز بوده و این کد قفل شده است. لطفاً کد جدید درخواست کنید.")
        }

        if (verification.expiresAt.isBefore(Instant.now())) {
            verification.status = VerificationStatus.EXPIRED
            phoneVerificationRepository.save(verification)
            throw AppException(ErrorCode.OTP_EXPIRED, "کد تایید منقضی شده است. لطفاً کد جدید دریافت کنید.")
        }

        if (verification.attempts >= verification.maxAttempts) {
            verification.status = VerificationStatus.LOCKED
            phoneVerificationRepository.save(verification)
            throw AppException(ErrorCode.OTP_LOCKED, "کد تایید قفل شد. لطفاً مجدداً تلاش نمایید.")
        }

        verification.attempts += 1

        val cleanCode = IranianPhoneNormalizer.cleanDigits(code)
        if (!passwordEncoder.matches(cleanCode, verification.otpHash)) {
            phoneVerificationRepository.save(verification)
            val remaining = verification.maxAttempts - verification.attempts
            throw AppException(ErrorCode.OTP_INVALID, "کد تایید اشتباه است. (فرصت باقی‌مانده: $remaining بار)")
        }

        // Successful Verification
        verification.status = VerificationStatus.VERIFIED
        verification.verifiedAt = Instant.now()
        phoneVerificationRepository.save(verification)

        // If authenticated user is attached, link and mark verified
        if (userId != null) {
            userRepository.findById(userId).ifPresent { user ->
                user.phoneNumber = normalized
                user.phoneVerified = true
                user.updatedAt = Instant.now()
                userRepository.save(user)
            }
        }

        logger.info("Phone {} successfully verified", IranianPhoneNormalizer.mask(normalized))
        return true
    }

    fun getStatus(userId: String): PhoneStatusDto {
        val user = userRepository.findById(userId)
            .orElseThrow { AppException(ErrorCode.NOT_FOUND, "کاربر یافت نشد") }

        val operator = IranianPhoneNormalizer.detectOperator(user.phoneNumber)
        return PhoneStatusDto(
            phoneNumber = user.phoneNumber,
            phoneNumberMasked = IranianPhoneNormalizer.mask(user.phoneNumber),
            isVerified = user.phoneVerified,
            operatorName = operator.persianName
        )
    }
}
