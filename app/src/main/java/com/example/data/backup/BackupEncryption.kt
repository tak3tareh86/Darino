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
    private const val ITERATIONS = 310_000
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 1_000_000
    private const val SALT_SIZE = 16
    private const val NONCE_SIZE = 12
    private const val TAG_BITS = 128
    private const val HEADER_SIZE = 8 + 1 + 4 + SALT_SIZE + NONCE_SIZE

    fun encrypt(plainText: ByteArray, passphrase: CharArray): ByteArray {
        require(passphrase.size >= 8) { "رمز پشتیبان باید حداقل ۸ نویسه داشته باشد." }
        val salt = ByteArray(SALT_SIZE).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(NONCE_SIZE).also(SecureRandom()::nextBytes)
        val header = ByteBuffer.allocate(HEADER_SIZE)
            .put(magic)
            .put(FORMAT_VERSION)
            .putInt(ITERATIONS)
            .put(salt)
            .put(nonce)
            .array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt, ITERATIONS), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plainText)
    }

    fun decrypt(payload: ByteArray, passphrase: CharArray): ByteArray {
        require(payload.size > HEADER_SIZE + TAG_BITS / 8) { "فایل پشتیبان رمزگذاری‌شده ناقص است." }
        val buffer = ByteBuffer.wrap(payload)
        val storedMagic = ByteArray(magic.size).also(buffer::get)
        require(storedMagic.contentEquals(magic)) { "ساختار فایل پشتیبان شناخته‌شده نیست." }
        require(buffer.get() == FORMAT_VERSION) { "نسخه رمزگذاری فایل پشتیبان پشتیبانی نمی‌شود." }
        val iterations = buffer.int
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "پارامتر امنیتی فایل پشتیبان معتبر نیست." }
        val salt = ByteArray(SALT_SIZE).also(buffer::get)
        val nonce = ByteArray(NONCE_SIZE).also(buffer::get)
        val header = payload.copyOfRange(0, HEADER_SIZE)
        val cipherText = payload.copyOfRange(HEADER_SIZE, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt, iterations), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(header)
        return cipher.doFinal(cipherText)
    }

    fun isEncrypted(payload: ByteArray): Boolean =
        payload.size >= magic.size && payload.copyOfRange(0, magic.size).contentEquals(magic)

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        return try {
            val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            SecretKeySpec(encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
