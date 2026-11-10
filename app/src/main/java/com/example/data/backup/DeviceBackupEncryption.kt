package com.example.data.backup

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** Device-bound encryption for private automatic snapshots; portable exports use BackupEncryption. */
object DeviceBackupEncryption {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "darino_auto_backup_v1"
    private const val TAG_BITS = 128

    fun encrypt(plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val nonce = cipher.iv
        val cipherText = cipher.doFinal(plainText)
        return ByteBuffer.allocate(1 + nonce.size + cipherText.size)
            .put(nonce.size.toByte())
            .put(nonce)
            .put(cipherText)
            .array()
    }

    fun decrypt(payload: ByteArray): ByteArray {
        require(payload.size > 1 + 12 + TAG_BITS / 8) { "Automatic backup file is incomplete" }
        val buffer = ByteBuffer.wrap(payload)
        val nonceSize = buffer.get().toInt() and 0xff
        require(nonceSize == 12 && payload.size > 1 + nonceSize + TAG_BITS / 8) {
            "Automatic backup encryption header is invalid"
        }
        val nonce = ByteArray(nonceSize).also(buffer::get)
        val cipherText = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getExistingKey(), javax.crypto.spec.GCMParameterSpec(TAG_BITS, nonce))
        return cipher.doFinal(cipherText)
    }

    private fun getExistingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            ?: throw IllegalStateException("Automatic backup encryption key is unavailable")
    }

    private fun getOrCreateKey(): SecretKey {
        runCatching { getExistingKey() }.getOrNull()?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
