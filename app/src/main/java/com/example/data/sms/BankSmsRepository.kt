package com.example.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.security.SessionManager
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.domain.BankSmsSuggestion
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BankSmsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("darino_bank_sms_prefs", Context.MODE_PRIVATE)
    private val pendingQueuePreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        "darino_pending_bank_sms_secure",
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val patternRepository = SmsPatternRepository(context.applicationContext)

    companion object {
        private const val KEY_PROCESSED_SMS_IDS = "processed_sms_ids"
        private const val KEY_DISMISSED_SMS_IDS = "dismissed_sms_ids"
        private const val KEY_PENDING_SMS_QUEUE = "pending_sms_queue_v2"
        private const val KEY_PERMISSION_REQUESTED = "permission_requested_before"
        private const val MAX_PERSISTED_PENDING_SMS = 50

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

    /**
     * Pending suggestions are encrypted at rest and isolated by authenticated user.
     * The raw SMS body is sensitive and must never be written to ordinary preferences or logs.
     */
    fun getPendingSmsSuggestions(userId: String? = null): List<BankSmsSuggestion> {
        val key = userKey(KEY_PENDING_SMS_QUEUE, userId) ?: return emptyList()
        val raw = pendingQueuePreferences.getString(key, null) ?: return emptyList()
        val currentPatterns = patternRepository.getPatterns(userId)
        val recognitionStartMillis = patternRepository.getRecognitionStartMillis(userId)
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val type = item.optString("type").takeIf { it.isNotBlank() }
                        ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                    add(
                        BankSmsSuggestion(
                            id = id,
                            patternId = item.optString("patternId"),
                            bankName = item.optString("bankName"),
                            amount = item.optLong("amount"),
                            formattedAmount = item.optString("formattedAmount"),
                            isAmountValid = item.optBoolean("isAmountValid"),
                            type = type,
                            isTypeUncertain = item.optBoolean("isTypeUncertain"),
                            smsText = item.optString("smsText"),
                            dateText = item.optString("dateText"),
                            timeText = item.optString("timeText"),
                            category = item.optString("category", "سایر"),
                            sourceAccount = item.optString("sourceAccount").takeIf { it.isNotBlank() && it != "null" },
                            destinationAccount = item.optString("destinationAccount").takeIf { it.isNotBlank() && it != "null" },
                            rawSender = item.optString("rawSender").takeIf { it.isNotBlank() && it != "null" },
                            parseError = item.optString("parseError").takeIf { it.isNotBlank() && it != "null" },
                            timestampMillis = item.optLong("timestampMillis", System.currentTimeMillis()),
                            balanceAmount = if (item.has("balanceAmount") && !item.isNull("balanceAmount")) item.optLong("balanceAmount") else null,
                            trackingCode = item.optString("trackingCode").takeIf { it.isNotBlank() && it != "null" }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList()).mapNotNull { saved ->
            // Do not restore pending suggestions created from SMS outside the active window.
            if (saved.timestampMillis < recognitionStartMillis) return@mapNotNull null
            if (saved.patternId.isBlank()) {
                return@mapNotNull GenericBankSmsParser.parse(
                    saved.id, saved.rawSender.orEmpty(), saved.smsText, saved.timestampMillis
                )
            }
            val origin = currentPatterns.singleOrNull { it.id == saved.patternId } ?: return@mapNotNull null
            val uniqueMatch = SmsPatternMatcher.findUniqueMatch(saved.smsText, currentPatterns)
                ?: return@mapNotNull null
            if (uniqueMatch.id != origin.id) return@mapNotNull null
            val parsed = BankSmsParser.parseMatchedPattern(saved.id, saved.rawSender.orEmpty(), saved.smsText, saved.timestampMillis, origin)
                ?: return@mapNotNull null
            parsed.copy(
                category = saved.category,
                sourceAccount = saved.sourceAccount ?: parsed.sourceAccount,
                destinationAccount = saved.destinationAccount ?: parsed.destinationAccount
            )
        }
    }

    fun savePendingSmsSuggestions(userId: String? = null, suggestions: List<BankSmsSuggestion>) {
        val key = userKey(KEY_PENDING_SMS_QUEUE, userId) ?: return
        val array = JSONArray()
        suggestions.takeLast(MAX_PERSISTED_PENDING_SMS).forEach { suggestion ->
            array.put(
                JSONObject()
                    .put("id", suggestion.id)
                    .put("patternId", suggestion.patternId)
                    .put("bankName", suggestion.bankName)
                    .put("amount", suggestion.amount)
                    .put("formattedAmount", suggestion.formattedAmount)
                    .put("isAmountValid", suggestion.isAmountValid)
                    .put("type", suggestion.type?.name ?: JSONObject.NULL)
                    .put("isTypeUncertain", suggestion.isTypeUncertain)
                    .put("smsText", suggestion.smsText)
                    .put("dateText", suggestion.dateText)
                    .put("timeText", suggestion.timeText)
                    .put("category", suggestion.category)
                    .put("sourceAccount", suggestion.sourceAccount ?: JSONObject.NULL)
                    .put("destinationAccount", suggestion.destinationAccount ?: JSONObject.NULL)
                    .put("rawSender", suggestion.rawSender ?: JSONObject.NULL)
                    .put("parseError", suggestion.parseError ?: JSONObject.NULL)
                    .put("timestampMillis", suggestion.timestampMillis)
                    .put("balanceAmount", suggestion.balanceAmount ?: JSONObject.NULL)
                    .put("trackingCode", suggestion.trackingCode ?: JSONObject.NULL)
            )
        }
        pendingQueuePreferences.edit().putString(key, array.toString()).apply()
    }

    fun removePendingSmsSuggestion(smsId: String, userId: String? = null) {
        savePendingSmsSuggestions(userId, getPendingSmsSuggestions(userId).filterNot { it.id == smsId })
    }

    suspend fun readInboxBankSms(
        userId: String? = null,
        receivedAfterMillis: Long? = null
    ): List<BankSmsSuggestion> = withContext(Dispatchers.IO) {
        val uid = userId ?: SessionManager.userId
        if (uid.isNullOrBlank()) {
            return@withContext emptyList()
        }

        if (!hasSmsPermission()) {
            return@withContext emptyList()
        }

        val patterns = patternRepository.getPatterns(uid)
        val recognitionStartMillis = patternRepository.getRecognitionStartMillis(uid)
            ?: return@withContext emptyList()
        // The per-user boundary is mandatory; caller filters may only narrow the scan.
        val effectiveReceivedAfterMillis = maxOf(
            recognitionStartMillis,
            receivedAfterMillis ?: recognitionStartMillis
        )

        val processedIds = getProcessedSmsIds(uid)
        val dismissedIds = getDismissedSmsIds(uid)
        val ignoredIds = processedIds + dismissedIds

        val results = mutableListOf<BankSmsSuggestion>()
        var scannedCount = 0
        var unmatchedCount = 0
        var ambiguousCount = 0
        var parseFailureCount = 0

        try {
            val uri = Uri.parse("content://sms/inbox")
            val projection = arrayOf("_id", "address", "body", "date")
            // Safe order: do not use LIMIT inside sortOrder string as OEM providers may reject it
            val cursor = context.contentResolver.query(
                uri,
                projection,
                "date >= ?",
                arrayOf(effectiveReceivedAfterMillis.toString()),
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
                    val timestamp = c.getLong(dateIdx)
                    // If the provider cannot supply a trustworthy timestamp, fail closed:
                    // an undated row must never be treated as a newly received SMS.
                    if (timestamp < effectiveReceivedAfterMillis) continue

                    val uniqueId = if (!id.isNullOrBlank()) "sms_$id" else "sms_${address.hashCode()}_$timestamp"
                    if (ignoredIds.contains(uniqueId)) {
                        continue
                    }

                    scannedCount++
                    // A saved user pattern is required before a bank SMS can enter the review queue.
                    // Ambiguous matches are rejected; no priority or fuzzy fallback is applied.
                    val matchingPatterns = SmsPatternMatcher.findMatchingPatterns(body, patterns)
                    if (matchingPatterns.isEmpty()) {
                        unmatchedCount++
                        continue
                    }
                    if (matchingPatterns.size != 1) {
                        ambiguousCount++
                        continue
                    }
                    val parsed = BankSmsParser.parseMatchedPattern(
                        uniqueId,
                        address,
                        body,
                        timestamp,
                        matchingPatterns.single()
                    )
                    if (parsed != null) {
                        results.add(parsed)
                    } else {
                        parseFailureCount++
                    }
                }
            }
            Log.d(
                "BankSmsRepository",
                "SMS scan summary: scanned=$scannedCount unmatched=$unmatchedCount ambiguous=$ambiguousCount parseFailed=$parseFailureCount accepted=${results.size}"
            )
        } catch (e: SecurityException) {
            Log.w("BankSmsRepository", "Permission revoked while querying SMS inbox")
        } catch (e: Exception) {
            Log.e("BankSmsRepository", "Error reading SMS inbox", e)
        }

        return@withContext results
    }
}
