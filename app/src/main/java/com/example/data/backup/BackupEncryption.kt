package com.example.data.backup

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable authenticated encryption for user-exported backups.
 * The passphrase is never persisted; the file carries only KDF parameters, salt and nonce.
 */
object BackupEncryption {
    private val magic = byteArrayOf(0x44, 0x41, 0x52, 0x49, 0x4E, 0x4F, 0x42, 0x4B) // DARINOBK
    private const val FORMAT_VERSION: Byte = 1
    private const val KDF_PBKDF2_SHA256: Byte = 1
    private const val KDF_PBKDF2_SHA1: Byte = 2
    private const val ITERATIONS = 310_000
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 1_000_000
    private const val SALT_SIZE = 16
    private const val NONCE_SIZE = 12
    private const val TAG_BITS = 128
    private const val HEADER_SIZE = 8 + 1 + 1 + 4 + SALT_SIZE + NONCE_SIZE

    fun encrypt(plainText: ByteArray, passphrase: CharArray): ByteArray {
        try {
            require(passphrase.size >= 12) { "رمز پشتیبان باید حداقل ۱۲ نویسه داشته باشد." }
            val kdfId = if (supportsSha256Pbkdf2()) KDF_PBKDF2_SHA256 else KDF_PBKDF2_SHA1
            val salt = ByteArray(SALT_SIZE).also(SecureRandom()::nextBytes)
            val nonce = ByteArray(NONCE_SIZE).also(SecureRandom()::nextBytes)
            val header = ByteBuffer.allocate(HEADER_SIZE)
                .put(magic)
                .put(FORMAT_VERSION)
                .put(kdfId)
                .putInt(ITERATIONS)
                .put(salt)
                .put(nonce)
                .array()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt, ITERATIONS, kdfId), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(header)
            return header + cipher.doFinal(plainText)
        } finally {
            passphrase.fill('\u0000')
        }
    }

    fun decrypt(payload: ByteArray, passphrase: CharArray): ByteArray {
        try {
            require(payload.size > HEADER_SIZE + TAG_BITS / 8) { "فایل پشتیبان رمزگذاری‌شده ناقص است." }
            val buffer = ByteBuffer.wrap(payload)
            val storedMagic = ByteArray(magic.size).also(buffer::get)
            require(storedMagic.contentEquals(magic)) { "ساختار فایل پشتیبان شناخته‌شده نیست." }
            require(buffer.get() == FORMAT_VERSION) { "نسخه رمزگذاری فایل پشتیبان پشتیبانی نمی‌شود." }
            val kdfId = buffer.get()
            require(kdfId == KDF_PBKDF2_SHA256 || kdfId == KDF_PBKDF2_SHA1) {
                "روش مشتق‌سازی کلید فایل پشتیبان پشتیبانی نمی‌شود."
            }
            val iterations = buffer.int
            require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "پارامتر امنیتی فایل پشتیبان معتبر نیست." }
            val salt = ByteArray(SALT_SIZE).also(buffer::get)
            val nonce = ByteArray(NONCE_SIZE).also(buffer::get)
            val header = payload.copyOfRange(0, HEADER_SIZE)
            val cipherText = payload.copyOfRange(HEADER_SIZE, payload.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt, iterations, kdfId), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(header)
            return cipher.doFinal(cipherText)
        } finally {
            passphrase.fill('\u0000')
        }
    }

    fun isEncrypted(payload: ByteArray): Boolean =
        payload.size >= magic.size && payload.copyOfRange(0, magic.size).contentEquals(magic)

    private fun supportsSha256Pbkdf2(): Boolean =
        runCatching { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256") }.isSuccess

    private fun deriveKey(
        passphrase: CharArray,
        salt: ByteArray,
        iterations: Int,
        kdfId: Byte
    ): SecretKeySpec {
        val algorithm = if (kdfId == KDF_PBKDF2_SHA256) {
            "PBKDF2WithHmacSHA256"
        } else {
            "PBKDF2WithHmacSHA1"
        }
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        return try {
            val encoded = SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
            SecretKeySpec(encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
