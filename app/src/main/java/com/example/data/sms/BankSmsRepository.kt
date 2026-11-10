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
import com.example.data.backup.BankSmsStateBackup
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
    private val userRuleRepository = UserBankSmsRuleRepository(context.applicationContext)

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
        // Restored suggestion IDs are not Android SMS-provider row IDs.
        if (smsId.startsWith("restored_backup_")) return
        val key = userKey(KEY_PROCESSED_SMS_IDS, userId) ?: return
        val current = (prefs.getStringSet(key, emptySet()) ?: emptySet()).toMutableSet()
        current.add(smsId)
        prefs.edit().putStringSet(key, current).apply()
    }

    fun markSmsDismissed(smsId: String, userId: String? = null) {
        if (smsId.startsWith("restored_backup_")) return
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
        val currentRules = userRuleRepository.getAll(userId)
        val defaultWindowStart = System.currentTimeMillis() - DEFAULT_SMS_PATTERN_LOOKBACK_MILLIS
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val type = item.optString("type").takeIf { it.isNotBlank() }
                        ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                    // Old queue entries without a unit predate explicit-unit persistence and
                    // are TOMAN by legacy contract. A present but unknown unit is rejected.
                    val rawAmountUnit = if (item.has("amountUnit")) item.optString("amountUnit") else "TOMAN"
                    val amountUnit = runCatching {
                        com.example.ui.screens.settings.model.AppCurrency.valueOf(rawAmountUnit)
                    }.getOrNull() ?: continue
                    val rawBalanceUnit = item.optString("balanceAmountUnit").takeIf { it.isNotBlank() }
                    val balanceAmountUnit = rawBalanceUnit?.let { raw ->
                        runCatching { com.example.ui.screens.settings.model.AppCurrency.valueOf(raw) }.getOrNull()
                    }
                    // A present but unknown balance unit must not fall back to the transaction unit.
                    if (rawBalanceUnit != null && balanceAmountUnit == null) continue
                    val rawAmountCandidates = item.optJSONArray("amountCandidates")
                    val amountCandidates = rawAmountCandidates?.let { values ->
                        buildList { for (i in 0 until values.length()) values.optLong(i).takeIf { it > 0L }?.let(::add) }
                    } ?: emptyList()
                    val rawCandidateUnits = item.optJSONArray("amountCandidateUnits")
                    val parsedCandidateUnits = rawCandidateUnits?.let { values ->
                        (0 until values.length()).map { index ->
                            runCatching {
                                com.example.ui.screens.settings.model.AppCurrency.valueOf(values.optString(index))
                            }.getOrNull()
                        }
                    }
                    if (!parsedCandidateUnits.isNullOrEmpty() &&
                        (parsedCandidateUnits.size != amountCandidates.size || parsedCandidateUnits.any { it == null })
                    ) continue
                    // Empty candidate-unit arrays are legacy data; the UI then uses the
                    // suggestion's validated primary unit for those candidates.
                    val amountCandidateUnits = parsedCandidateUnits?.filterNotNull() ?: emptyList()
                    add(
                        BankSmsSuggestion(
                            id = id,
                            patternId = item.optString("patternId"),
                            bankName = item.optString("bankName"),
                            amount = item.optLong("amount"),
                            amountUnit = amountUnit,
                            balanceAmountUnit = balanceAmountUnit,
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
                            receivedAtMillis = item.optLong("receivedAtMillis", item.optLong("timestampMillis", System.currentTimeMillis())),
                            balanceAmount = if (item.has("balanceAmount") && !item.isNull("balanceAmount")) item.optLong("balanceAmount") else null,
                            amountCandidates = amountCandidates,
                            amountCandidateUnits = amountCandidateUnits,
                            trackingCode = item.optString("trackingCode").takeIf { it.isNotBlank() && it != "null" },
                            preserveAfterRestore = item.optBoolean("preserveAfterRestore", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList()).mapNotNull { saved ->
            if (saved.preserveAfterRestore) return@mapNotNull saved
            // Restore configured-template suggestions only while their own rolling window is active.
            val eligiblePatterns = currentPatterns.filter {
                saved.receivedAtMillis >= System.currentTimeMillis() - it.lookbackMillis
            }
            if (saved.patternId.startsWith("user-rule:")) {
                val ruleId = saved.patternId.removePrefix("user-rule:")
                val eligibleRules = currentRules.filter { saved.receivedAtMillis >= System.currentTimeMillis() - it.lookbackMillis }
                val matches = UserBankSmsRuleMatcher.matchAll(saved.smsText, saved.rawSender.orEmpty(), eligibleRules)
                val match = matches.singleOrNull()?.takeIf { it.rule.id == ruleId } ?: return@mapNotNull null
                val parsed = UserRuleSmsAmountParser.parse(saved.id, saved.rawSender.orEmpty(), saved.smsText, saved.receivedAtMillis, match.rule, match.type) ?: return@mapNotNull null
                return@mapNotNull UserBankSmsRuleMatcher.applyMatch(parsed, match)
            }
            if (saved.patternId.isBlank()) return@mapNotNull null
            if (eligiblePatterns.none { it.id == saved.patternId }) return@mapNotNull null
            val origin = eligiblePatterns.singleOrNull { it.id == saved.patternId } ?: return@mapNotNull null
            val uniqueMatch = SmsPatternMatcher.findUniqueMatch(saved.smsText, eligiblePatterns, saved.rawSender)
                ?: return@mapNotNull null
            if (uniqueMatch.id != origin.id) return@mapNotNull null
            val parsed = BankSmsParser.parseMatchedPattern(saved.id, saved.rawSender.orEmpty(), saved.smsText, saved.receivedAtMillis, origin)
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
                    .put("amountUnit", suggestion.amountUnit.name)
                    .put("balanceAmountUnit", suggestion.balanceAmountUnit?.name ?: "")
                .put("amountCandidateUnits", JSONArray(suggestion.amountCandidateUnits.map { it.name }))
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
                    .put("receivedAtMillis", suggestion.receivedAtMillis)
                    .put("balanceAmount", suggestion.balanceAmount ?: JSONObject.NULL)
                    .put("amountCandidates", JSONArray(suggestion.amountCandidates))
                    .put("trackingCode", suggestion.trackingCode ?: JSONObject.NULL)
                    .put("preserveAfterRestore", suggestion.preserveAfterRestore)
            )
        }
        pendingQueuePreferences.edit().putString(key, array.toString()).apply()
    }

    /** Captures pending suggestions; provider row IDs remain device-local and are not exported. */
    @Synchronized
    fun exportStateForBackup(userId: String): BankSmsStateBackup {
        require(userId.isNotBlank())
        val queueKey = userKey(KEY_PENDING_SMS_QUEUE, userId)!!
        return BankSmsStateBackup(
            pendingSuggestionsJson = normalizePendingSuggestionsJson(
                pendingQueuePreferences.getString(queueKey, null)
            )
        )
    }

    private fun normalizePendingSuggestionsJson(raw: String?, markPreserved: Boolean = false): String? {
        if (raw == null) return null
        return runCatching {
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                // Older locally persisted suggestions predate explicit units and
                // receivedAtMillis; preserve their documented legacy defaults.
                if (!item.has("amountUnit") || item.isNull("amountUnit")) {
                    item.put("amountUnit", "TOMAN")
                }
                if (!item.has("receivedAtMillis") || item.isNull("receivedAtMillis")) {
                    item.put("receivedAtMillis", item.optLong("timestampMillis", System.currentTimeMillis()))
                }
                if (markPreserved) {
                    item.put("id", "restored_backup_${java.util.UUID.randomUUID()}")
                    item.put("preserveAfterRestore", true)
                }
            }
            array.toString()
        }.getOrElse { raw }
    }

    /** Restores the portable pending-suggestion queue with rollback on preference-write failure. */
    @Synchronized
    fun restoreStateFromBackup(state: BankSmsStateBackup, userId: String) {
        require(userId.isNotBlank())
        val queueKey = userKey(KEY_PENDING_SMS_QUEUE, userId)!!
        val previousQueue = pendingQueuePreferences.getString(queueKey, null)
        val restoredQueue = normalizePendingSuggestionsJson(state.pendingSuggestionsJson, markPreserved = true)
        try {
            val editor = pendingQueuePreferences.edit()
            if (restoredQueue == null) editor.remove(queueKey)
            else editor.putString(queueKey, restoredQueue)
            check(editor.commit()) { "ذخیره پیشنهادهای پیامکی بازیابی‌شده ناموفق بود." }
            val expectedState = state.copy(pendingSuggestionsJson = restoredQueue)
            check(exportStateForBackup(userId) == expectedState) {
                "تطبیق پیشنهادهای پیامکی پس از بازیابی ناموفق بود."
            }
            SmsPatternChangeNotifier.changes.tryEmit(Unit)
        } catch (error: Exception) {
            val rollback = pendingQueuePreferences.edit()
            if (previousQueue == null) rollback.remove(queueKey) else rollback.putString(queueKey, previousQueue)
            if (!rollback.commit()) {
                error.addSuppressed(IllegalStateException("بازگردانی پیشنهادهای پیامکی قبلی ناموفق بود."))
            }
            throw error
        }
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
        val rules = userRuleRepository.getAll(uid)
        val now = System.currentTimeMillis()
        // Query the longest active user-configured lookback window. Each rule/template
        // is filtered again against its own window before it can match.
        val lookbacks = patterns.map { it.lookbackMillis } + rules.map { it.lookbackMillis }
        val earliestPatternStart = if (lookbacks.isEmpty()) now else now - (lookbacks.maxOrNull() ?: DEFAULT_SMS_PATTERN_LOOKBACK_MILLIS)
        val effectiveReceivedAfterMillis = maxOf(
            earliestPatternStart,
            receivedAfterMillis ?: earliestPatternStart
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
                    // Credential/OTP messages must never reach either recognition path.
                    if (BankSmsParser.containsPasswordKeyword(body)) {
                        unmatchedCount++
                        continue
                    }
                    // Only explicit user-defined rules and legacy templates may recognize an SMS.
                    // Ambiguous matches are rejected rather than arbitrarily prioritized.
                    val eligibleRules = rules.filter { timestamp >= now - it.lookbackMillis }
                    val ruleMatches = UserBankSmsRuleMatcher.matchAll(body, address, eligibleRules)
                    if (ruleMatches.isNotEmpty()) {
                        val match = ruleMatches.singleOrNull()
                        if (match == null) { ambiguousCount++; continue }
                        val parsedByRule = UserRuleSmsAmountParser.parse(uniqueId, address, body, timestamp, match.rule, match.type)
                        if (parsedByRule != null) results.add(UserBankSmsRuleMatcher.applyMatch(parsedByRule, match)) else parseFailureCount++
                        continue
                    }
                    val eligiblePatterns = patterns.filter { timestamp >= now - it.lookbackMillis }
                    val matchingPatterns = SmsPatternMatcher.findMatchingPatterns(body, eligiblePatterns, address)
                    if (matchingPatterns.size > 1) {
                        ambiguousCount++
                        continue
                    }
                    val bodyMatchesConfiguredTemplate = eligiblePatterns.any {
                        SmsPatternMatcher.matchesTemplateBody(body, it)
                    }
                    val parsed = when {
                        matchingPatterns.size == 1 -> BankSmsParser.parseMatchedPattern(
                            uniqueId, address, body, timestamp, matchingPatterns.single()
                        )
                        bodyMatchesConfiguredTemplate -> null // Do not bypass an exact sender mismatch through generic parsing.
                        else -> null
                    }
                    if (parsed != null) {
                        results.add(parsed)
                    } else {
                        unmatchedCount++
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
