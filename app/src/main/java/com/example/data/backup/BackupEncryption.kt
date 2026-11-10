package com.example.data.backup

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets

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
        runCatching { Mac.getInstance("HmacSHA256") }.isSuccess

    private fun deriveKey(
        passphrase: CharArray,
        salt: ByteArray,
        iterations: Int,
        kdfId: Byte
    ): SecretKeySpec {
        val hmacAlgorithm = if (kdfId == KDF_PBKDF2_SHA256) "HmacSHA256" else "HmacSHA1"
        val encodedPassword = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(passphrase))
        val passwordBytes = ByteArray(encodedPassword.remaining()).also(encodedPassword::get)
        try {
            val mac = Mac.getInstance(hmacAlgorithm)
            mac.init(SecretKeySpec(passwordBytes, hmacAlgorithm))
            val hashLength = mac.macLength
            val derived = ByteArray(32)
            val blockCount = (derived.size + hashLength - 1) / hashLength
            for (blockIndex in 1..blockCount) {
                val blockInput = ByteBuffer.allocate(salt.size + 4).put(salt).putInt(blockIndex).array()
                var u = mac.doFinal(blockInput)
                val accumulator = u.copyOf()
                repeat(iterations - 1) {
                    u = mac.doFinal(u)
                    for (index in accumulator.indices) {
                        accumulator[index] = (accumulator[index].toInt() xor u[index].toInt()).toByte()
                    }
                }
                val offset = (blockIndex - 1) * hashLength
                val copyLength = minOf(hashLength, derived.size - offset)
                accumulator.copyInto(derived, offset, 0, copyLength)
                u.fill(0)
                accumulator.fill(0)
            }
            return SecretKeySpec(derived, "AES").also { derived.fill(0) }
        } finally {
            passwordBytes.fill(0)
        }
    }
}
