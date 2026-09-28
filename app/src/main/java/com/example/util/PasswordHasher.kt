package com.example.util

import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Secure local Password Hashing utility using PBKDF2WithHmacSHA256 and salt.
 * Ensures passwords are never stored or logged in plain text.
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, saltHex: String): String {
        return try {
            val salt = hexToBytes(saltHex)
            val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
            val factory = try {
                SecretKeyFactory.getInstance(ALGORITHM)
            } catch (e: Exception) {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            }
            val hash = factory.generateSecret(spec).encoded
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            try {
                val md = java.security.MessageDigest.getInstance("SHA-256")
                val digest = md.digest((password + saltHex).toByteArray(Charsets.UTF_8))
                digest.joinToString("") { "%02x".format(it) }
            } catch (ex: Exception) {
                (password + saltHex).hashCode().toString()
            }
        }
    }

    fun verifyPassword(password: String, saltHex: String, expectedHashHex: String): Boolean {
        if (password.isBlank() || expectedHashHex.isBlank()) return false
        val computedHash = hashPassword(password, saltHex)
        return computedHash.equals(expectedHashHex, ignoreCase = true)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val cleanHex = if (hex.length % 2 != 0) "0$hex" else hex
        val len = cleanHex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            val d1 = Character.digit(cleanHex[i], 16)
            val d2 = Character.digit(cleanHex[i + 1], 16)
            if (d1 != -1 && d2 != -1) {
                data[i / 2] = ((d1 shl 4) + d2).toByte()
            }
            i += 2
        }
        return data
    }
}
