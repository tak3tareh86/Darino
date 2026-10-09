package com.example.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.security.SessionManager
import com.example.ui.screens.home.domain.BankSmsSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BankSmsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("darino_bank_sms_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROCESSED_SMS_IDS = "processed_sms_ids"
        private const val KEY_DISMISSED_SMS_IDS = "dismissed_sms_ids"
        private const val KEY_PERMISSION_REQUESTED = "permission_requested_before"

        @Volatile
        private var INSTANCE: BankSmsRepository? = null

        fun getInstance(context: Context): BankSmsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BankSmsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun userKey(key: String, userId: String?): String? {
        val uid = userId ?: SessionManager.userId ?: return null
        return "${key}_${uid}"
    }

    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun wasPermissionRequestedBefore(): Boolean {
        return prefs.getBoolean(KEY_PERMISSION_REQUESTED, false)
    }

    fun markPermissionRequested() {
        prefs.edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply()
    }

    fun getProcessedSmsIds(userId: String? = null): Set<String> {
        val key = userKey(KEY_PROCESSED_SMS_IDS, userId) ?: return emptySet()
        return prefs.getStringSet(key, emptySet()) ?: emptySet()
    }

    fun getDismissedSmsIds(userId: String? = null): Set<String> {
        val key = userKey(KEY_DISMISSED_SMS_IDS, userId) ?: return emptySet()
        return prefs.getStringSet(key, emptySet()) ?: emptySet()
    }

    fun markSmsProcessed(smsId: String, userId: String? = null) {
        val key = userKey(KEY_PROCESSED_SMS_IDS, userId) ?: return
        val current = (prefs.getStringSet(key, emptySet()) ?: emptySet()).toMutableSet()
        current.add(smsId)
        prefs.edit().putStringSet(key, current).apply()
    }

    fun markSmsDismissed(smsId: String, userId: String? = null) {
        val key = userKey(KEY_DISMISSED_SMS_IDS, userId) ?: return
        val current = (prefs.getStringSet(key, emptySet()) ?: emptySet()).toMutableSet()
        current.add(smsId)
        prefs.edit().putStringSet(key, current).apply()
    }

    suspend fun readInboxBankSms(userId: String? = null): List<BankSmsSuggestion> = withContext(Dispatchers.IO) {
        val uid = userId ?: SessionManager.userId
        if (uid.isNullOrBlank()) {
            return@withContext emptyList()
        }

        if (!hasSmsPermission()) {
            return@withContext emptyList()
        }

        val processedIds = getProcessedSmsIds(uid)
        val dismissedIds = getDismissedSmsIds(uid)
        val ignoredIds = processedIds + dismissedIds

        val results = mutableListOf<BankSmsSuggestion>()

        try {
            val uri = Uri.parse("content://sms/inbox")
            val projection = arrayOf("_id", "address", "body", "date")
            // Safe order: do not use LIMIT inside sortOrder string as OEM providers may reject it
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC"
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndex("_id")
                val addressIdx = c.getColumnIndex("address")
                val bodyIdx = c.getColumnIndex("body")
                val dateIdx = c.getColumnIndex("date")

                while (c.moveToNext() && results.size < 50) {
                    val id = if (idIdx != -1) c.getString(idIdx) else null
                    val address = if (addressIdx != -1) c.getString(addressIdx) else ""
                    val body = if (bodyIdx != -1) c.getString(bodyIdx) else ""
                    val timestamp = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()

                    val uniqueId = if (!id.isNullOrBlank()) "sms_$id" else "sms_${address.hashCode()}_$timestamp"
                    if (ignoredIds.contains(uniqueId)) {
                        continue
                    }

                    if (BankSmsParser.isPotentialBankSms(address, body)) {
                        val parsed = BankSmsParser.parse(uniqueId, address, body, timestamp)
                        if (parsed != null) {
                            results.add(parsed)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w("BankSmsRepository", "Permission revoked while querying SMS inbox")
        } catch (e: Exception) {
            Log.e("BankSmsRepository", "Error reading SMS inbox")
        }

        return@withContext results
    }
}
