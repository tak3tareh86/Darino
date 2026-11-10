package com.example.data.sms

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.security.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow

const val DEFAULT_SMS_PATTERN_LOOKBACK_MILLIS = 24L * 60L * 60L * 1000L
object SmsPatternChangeNotifier {
    val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
}

val SMS_PATTERN_LOOKBACK_OPTIONS = listOf(
    1L * 24L * 60L * 60L * 1000L,
    3L * 24L * 60L * 60L * 1000L,
    7L * 24L * 60L * 60L * 1000L,
    30L * 24L * 60L * 60L * 1000L,
    90L * 24L * 60L * 60L * 1000L
)

enum class SmsPatternType(val title: String) {
    INCOME("واریز"),
    EXPENSE("برداشت"),
    TRANSFER("انتقال وجه")
}

enum class SmsPatternAccountSide(val title: String) {
    SOURCE("حساب مبدأ"),
    DESTINATION("حساب مقصد")
}

enum class SmsPatternSlotKind(val title: String) {
    AMOUNT_RIAL("مبلغ (ریال)"),
    AMOUNT_TOMAN("مبلغ (تومان)"),
    BALANCE_RIAL("موجودی (ریال)"),
    BALANCE_TOMAN("موجودی (تومان)"),
    DATE("تاریخ کامل"),
    DATE_DAY_MONTH("تاریخ (روز/ماه)"),
    DATE_MONTH_DAY("تاریخ (ماه/روز)"),
    DATE_DAY_MONTH_YEAR("تاریخ کامل (روز/ماه/سال)"),
    DATE_MONTH_DAY_YEAR("تاریخ کامل (ماه/روز/سال)"),
    DATE_YEAR_MONTH_DAY("تاریخ کامل (سال دورقمی/ماه/روز)"),
    TIME("ساعت"),
    SOURCE_CARD("کارت مبدأ"),
    DESTINATION_CARD("کارت مقصد"),
    SOURCE_ACCOUNT("حساب مبدأ"),
    DESTINATION_ACCOUNT("حساب مقصد"),
    TRACKING("کد پیگیری"),
    NUMBER("عدد متغیر"),
    FIXED("ثابت (بدون تغییر)")
}

data class SmsPatternSlot(
    val start: Int,
    val endExclusive: Int,
    val kind: SmsPatternSlotKind,
    /** Exact account/card identifiers permitted in this slot; empty for ordinary numeric slots. */
    val allowedValues: List<String> = emptyList()
)

data class SmsPattern(
    val id: String,
    val name: String,
    val type: SmsPatternType,
    val sampleMessage: String,
    val slots: List<SmsPatternSlot> = emptyList(),
    /** Bank display name; kept separate from the user's pattern label. */
    val bankName: String = name,
    /** Exact sender address copied from the SMS app; case and whitespace are significant. */
    val senderAddress: String = "",
    /** Optional account identifier associated with this pattern. */
    val accountNumber: String? = null,
    /** User-facing Jalali date mask, e.g. YY/MM/DD or YYYY/MM/DD. */
    val dateFormat: String = "",
    /** User-facing clock mask, e.g. HH:mm. */
    val timeFormat: String = "",
    /** User-selected side for the configured account number in transfer templates. */
    val transferAccountSide: SmsPatternAccountSide? = null,
    /** Rolling inbox lookback window for this template. Defaults to one day. */
    val lookbackMillis: Long = DEFAULT_SMS_PATTERN_LOOKBACK_MILLIS
)

/** Encrypted, account-scoped storage for explicitly configured SMS templates. */
class SmsPatternRepository(context: Context) {
    private val appContext = context.applicationContext
    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val preferences = EncryptedSharedPreferences.create(
        appContext,
        "darino_sms_patterns_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun recognitionStartKey(userId: String? = SessionManager.userId): String {
        val scopedUserId = userId?.takeIf { it.isNotBlank() } ?: "local"
        return "recognition_start_$scopedUserId"
    }

    /**
     * A stable 24-hour lower bound is created even before a template exists,
     * allowing generic recognition to scan the inbox without user-defined patterns.
     */
    @Synchronized
    fun getRecognitionStartMillis(userId: String? = SessionManager.userId): Long? {
        val key = recognitionStartKey(userId)
        preferences.getLong(key, 0L).takeIf { it > 0L }?.let { return it }
        val start = System.currentTimeMillis() - DEFAULT_SMS_PATTERN_LOOKBACK_MILLIS
        preferences.edit().putLong(key, start).commit()
        return start
    }

    private fun storageKey(userId: String? = SessionManager.userId): String {
        val scopedUserId = userId?.takeIf { it.isNotBlank() } ?: "local"
        return "patterns_$scopedUserId"
    }

    fun getPatterns(userId: String? = SessionManager.userId): List<SmsPattern> {
        val raw = preferences.getString(storageKey(userId), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val type = runCatching { SmsPatternType.valueOf(item.optString("type")) }.getOrNull() ?: continue
                    val name = item.optString("name").trim()
                    val message = item.optString("sampleMessage")
                    if (name.isEmpty() || message.isEmpty()) continue
                    val slotsJson = item.optJSONArray("slots") ?: JSONArray()
                    val slots = buildList {
                        for (slotIndex in 0 until slotsJson.length()) {
                            val slot = slotsJson.optJSONObject(slotIndex) ?: continue
                            val kind = runCatching {
                                SmsPatternSlotKind.valueOf(slot.optString("kind"))
                            }.getOrNull() ?: continue
                            val start = slot.optInt("start", -1)
                            val end = slot.optInt("endExclusive", -1)
                            val allowedJson = slot.optJSONArray("allowedValues") ?: JSONArray()
                            val storedAllowedValues = buildList {
                                for (valueIndex in 0 until allowedJson.length()) {
                                    allowedJson.optString(valueIndex).trim().takeIf { it.isNotEmpty() }?.let { add(it) }
                                }
                            }.distinct()
                            if (start >= 0 && end > start && end <= message.length) {
                                val accountKinds = setOf(
                                    SmsPatternSlotKind.SOURCE_ACCOUNT, SmsPatternSlotKind.DESTINATION_ACCOUNT,
                                    SmsPatternSlotKind.SOURCE_CARD, SmsPatternSlotKind.DESTINATION_CARD
                                )
                                // Migrate older account slots to a safe fixed allow-list containing
                                // only their original sample value.
                                val allowedValues = if (kind in accountKinds && storedAllowedValues.isEmpty()) {
                                    listOf(message.substring(start, end))
                                } else storedAllowedValues
                                add(SmsPatternSlot(start, end, kind, allowedValues))
                            }
                        }
                    }
                    add(
                        SmsPattern(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            name = name,
                            type = type,
                            sampleMessage = message,
                            slots = slots.sortedBy { it.start },
                            bankName = item.optString("bankName").ifBlank { name },
                            senderAddress = item.optString("senderAddress"),
                            accountNumber = item.optString("accountNumber").takeIf { it.isNotBlank() && it != "null" },
                            dateFormat = item.optString("dateFormat"),
                            timeFormat = item.optString("timeFormat"),
                            transferAccountSide = runCatching {
                                SmsPatternAccountSide.valueOf(item.optString("transferAccountSide"))
                            }.getOrNull(),
                            lookbackMillis = item.optLong("lookbackMillis", DEFAULT_SMS_PATTERN_LOOKBACK_MILLIS)
                                .coerceAtLeast(DEFAULT_SMS_PATTERN_LOOKBACK_MILLIS)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(pattern: SmsPattern, userId: String? = SessionManager.userId) {
        val patterns = getPatterns(userId).toMutableList()
        val index = patterns.indexOfFirst { it.id == pattern.id }
        if (index < 0 && patterns.isEmpty()) {
            // Start one day before the first template is registered, even if READ_SMS
            // permission is granted later. The permission date must not shift this window.
            preferences.edit()
                .putLong(recognitionStartKey(userId), System.currentTimeMillis() - DEFAULT_SMS_PATTERN_LOOKBACK_MILLIS)
                .commit()
        } else if (patterns.isNotEmpty()) {
            getRecognitionStartMillis(userId)
        }
        if (index >= 0) patterns[index] = pattern else patterns.add(0, pattern)
        persist(patterns, userId)
        SmsPatternChangeNotifier.changes.tryEmit(Unit)
    }

    fun create(
        name: String,
        type: SmsPatternType,
        sampleMessage: String,
        slots: List<SmsPatternSlot>,
        userId: String? = SessionManager.userId,
        bankName: String = name,
        senderAddress: String = "",
        accountNumber: String? = null,
        dateFormat: String = "",
        timeFormat: String = "",
        transferAccountSide: SmsPatternAccountSide? = null,
        lookbackMillis: Long = DEFAULT_SMS_PATTERN_LOOKBACK_MILLIS
    ): SmsPattern {
        return SmsPattern(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            type = type,
            sampleMessage = sampleMessage,
            slots = slots.sortedBy { it.start },
            bankName = bankName.trim(),
            senderAddress = senderAddress,
            accountNumber = accountNumber?.trim()?.takeIf { it.isNotEmpty() },
            dateFormat = dateFormat.trim(),
            timeFormat = timeFormat.trim(),
            transferAccountSide = transferAccountSide,
            lookbackMillis = lookbackMillis.coerceAtLeast(DEFAULT_SMS_PATTERN_LOOKBACK_MILLIS)
        ).also { save(it, userId) }
    }

    @Synchronized
    fun delete(id: String, userId: String? = SessionManager.userId) {
        val remaining = getPatterns(userId).filterNot { it.id == id }
        persist(remaining, userId)
        if (remaining.isEmpty()) {
            preferences.edit().remove(recognitionStartKey(userId)).commit()
        }
        SmsPatternChangeNotifier.changes.tryEmit(Unit)
    }

    private fun persist(patterns: List<SmsPattern>, userId: String? = SessionManager.userId) {
        val array = JSONArray()
        patterns.forEach { pattern ->
            val slots = JSONArray()
            pattern.slots.forEach { slot ->
                slots.put(
                    JSONObject()
                        .put("start", slot.start)
                        .put("endExclusive", slot.endExclusive)
                        .put("kind", slot.kind.name)
                        .put("allowedValues", JSONArray(slot.allowedValues))
                )
            }
            array.put(
                JSONObject()
                    .put("id", pattern.id)
                    .put("name", pattern.name)
                    .put("type", pattern.type.name)
                    .put("sampleMessage", pattern.sampleMessage)
                    .put("slots", slots)
                    .put("bankName", pattern.bankName)
                    .put("senderAddress", pattern.senderAddress)
                    .put("accountNumber", pattern.accountNumber ?: JSONObject.NULL)
                    .put("dateFormat", pattern.dateFormat)
                    .put("timeFormat", pattern.timeFormat)
                    .put("transferAccountSide", pattern.transferAccountSide?.name ?: JSONObject.NULL)
                    .put("lookbackMillis", pattern.lookbackMillis)
            )
        }
        preferences.edit().putString(storageKey(userId), array.toString()).apply()
    }

    /** Exports the plaintext JSON value; restore writes it back through encrypted preferences. */
    @Synchronized
    fun exportForBackup(userId: String): String? = preferences.getString(storageKey(userId), null)

    @Synchronized
    fun getStoredRecognitionStartMillis(userId: String): Long? =
        preferences.getLong(recognitionStartKey(userId), 0L).takeIf { it > 0L }

    @Synchronized
    fun restoreFromBackup(rawJson: String?, recognitionStartMillis: Long?, userId: String) {
        val storageKey = storageKey(userId)
        val startKey = recognitionStartKey(userId)
        val previousJson = preferences.getString(storageKey, null)
        val previousStart = preferences.getLong(startKey, 0L)
        if (rawJson == null) {
            check(preferences.edit().remove(storageKey).remove(startKey).commit()) {
                "پاک‌سازی الگوهای پیامک قبلی هنگام بازیابی ناموفق بود."
            }
            SmsPatternChangeNotifier.changes.tryEmit(Unit)
            return
        }
        val array = JSONArray(rawJson)
        val editor = preferences.edit().putString(storageKey, rawJson)
        if (recognitionStartMillis != null && recognitionStartMillis > 0L) {
            editor.putLong(startKey, recognitionStartMillis)
        } else {
            editor.remove(startKey)
        }
        check(editor.commit()) { "ذخیره الگوهای پیامک بازیابی‌شده انجام نشد." }
        if (getPatterns(userId).size != array.length()) {
            val rollback = preferences.edit()
            if (previousJson == null) rollback.remove(storageKey) else rollback.putString(storageKey, previousJson)
            if (previousStart > 0L) rollback.putLong(startKey, previousStart) else rollback.remove(startKey)
            check(rollback.commit()) { "بازگردانی الگوهای پیامک قبلی پس از خطا ناموفق بود." }
            throw IllegalArgumentException("الگوهای پیامک فایل پشتیبان معتبر نیستند.")
        }
        SmsPatternChangeNotifier.changes.tryEmit(Unit)
    }

}