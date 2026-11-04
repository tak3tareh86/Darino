package com.example.data.sms

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.security.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class UserBankSmsRule(
    val id: String = UUID.randomUUID().toString(),
    val bankName: String,
    // Legacy field retained for source compatibility with older callers/backups.
    val senderAddresses: List<String> = emptyList(),
    val incomeMarkers: String,
    val expenseMarkers: String,
    val transferMarkers: String,
    val lookbackMillis: Long = DEFAULT_SMS_PATTERN_LOOKBACK_MILLIS,
    val textSenderAddresses: List<String> = emptyList(),
    val numericSenderAddresses: List<String> = emptyList()
)

class UserBankSmsRuleRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            appContext,
            "darino_user_bank_sms_rules",
            MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun key(userId: String? = SessionManager.userId) =
        "rules_" + (userId?.takeIf { it.isNotBlank() } ?: "local")

    @Synchronized
    fun getAll(userId: String? = SessionManager.userId): List<UserBankSmsRule> {
        val raw = prefs.getString(key(userId), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val senders = item.optJSONArray("senders") ?: JSONArray()
                    val addresses = buildList {
                        for (j in 0 until senders.length()) {
                            senders.optString(j).trim().takeIf { it.isNotEmpty() }?.let(::add)
                        }
                    }.distinct()
                    val hasSplitSenders = item.has("textSenders") || item.has("numericSenders")
                    fun readSenders(name: String): List<String> {
                        val values = item.optJSONArray(name) ?: JSONArray()
                        return buildList {
                            for (j in 0 until values.length()) {
                                values.optString(j).trim().takeIf { it.isNotEmpty() }?.let(::add)
                            }
                        }.distinct()
                    }
                    val legacyText = if (hasSplitSenders) emptyList() else addresses.filterNot(UserBankSmsRuleMatcher::isNumericSender)
                    val legacyNumeric = if (hasSplitSenders) emptyList() else addresses.filter(UserBankSmsRuleMatcher::isNumericSender)
                    val textSenders = (readSenders("textSenders") + legacyText).distinct()
                    val numericSenders = (readSenders("numericSenders") + legacyNumeric).distinct()
                    val rule = UserBankSmsRule(
                        id = item.optString("id"),
                        bankName = item.optString("bankName").trim(),
                        senderAddresses = (textSenders + numericSenders).distinct(),
                        textSenderAddresses = textSenders,
                        numericSenderAddresses = numericSenders,
                        incomeMarkers = item.optString("incomeMarkers"),
                        expenseMarkers = item.optString("expenseMarkers"),
                        transferMarkers = item.optString("transferMarkers"),
                        lookbackMillis = item.optLong("lookbackMillis", DEFAULT_SMS_PATTERN_LOOKBACK_MILLIS).coerceIn(SMS_PATTERN_LOOKBACK_OPTIONS.first(), SMS_PATTERN_LOOKBACK_OPTIONS.last())
                    )
                    if (rule.bankName.isNotBlank() && (rule.textSenderAddresses.isNotEmpty() || rule.numericSenderAddresses.isNotEmpty())) add(rule)
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(rule: UserBankSmsRule, userId: String? = SessionManager.userId) {
        require(rule.bankName.isNotBlank())
        require(rule.textSenderAddresses.isNotEmpty() || rule.numericSenderAddresses.isNotEmpty() || rule.senderAddresses.isNotEmpty())
        require(UserBankSmsRuleMatcher.parseMarkers(rule.incomeMarkers) != null)
        require(UserBankSmsRuleMatcher.parseMarkers(rule.expenseMarkers) != null)
        require(UserBankSmsRuleMatcher.parseMarkers(rule.transferMarkers) != null)
        val legacy = rule.senderAddresses.map(String::trim).filter(String::isNotEmpty)
        val textSenders = (rule.textSenderAddresses + legacy.filterNot(UserBankSmsRuleMatcher::isNumericSender))
            .map(String::trim).filter(String::isNotEmpty).distinct()
        val numericSenders = (rule.numericSenderAddresses + legacy.filter(UserBankSmsRuleMatcher::isNumericSender))
            .map(String::trim).filter(String::isNotEmpty).distinct()
        val normalized = rule.copy(
            bankName = rule.bankName.trim(),
            senderAddresses = (textSenders + numericSenders).distinct(),
            textSenderAddresses = textSenders,
            numericSenderAddresses = numericSenders
        )
        require(normalized.bankName.isNotBlank())
        require(normalized.textSenderAddresses.isNotEmpty() || normalized.numericSenderAddresses.isNotEmpty())
        val all = getAll(userId).toMutableList()
        val index = all.indexOfFirst { it.id == normalized.id }
        if (index >= 0) all[index] = normalized else all.add(normalized)
        persist(all, userId)
        SmsPatternChangeNotifier.changes.tryEmit(Unit)
    }

    @Synchronized
    fun delete(id: String, userId: String? = SessionManager.userId) {
        persist(getAll(userId).filterNot { it.id == id }, userId)
        SmsPatternChangeNotifier.changes.tryEmit(Unit)
    }

    private fun persist(rules: List<UserBankSmsRule>, userId: String?) {
        val array = JSONArray()
        rules.forEach { rule ->
            array.put(
                JSONObject()
                    .put("id", rule.id)
                    .put("bankName", rule.bankName)
                    .put("senders", JSONArray(rule.senderAddresses))
                    .put("textSenders", JSONArray(rule.textSenderAddresses))
                    .put("numericSenders", JSONArray(rule.numericSenderAddresses))
                    .put("incomeMarkers", rule.incomeMarkers)
                    .put("expenseMarkers", rule.expenseMarkers)
                    .put("transferMarkers", rule.transferMarkers)
                    .put("lookbackMillis", rule.lookbackMillis)
            )
        }
        prefs.edit().putString(key(userId), array.toString()).apply()
    }
}
