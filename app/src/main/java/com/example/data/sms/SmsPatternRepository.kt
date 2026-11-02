package com.example.data.sms

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.security.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val SMS_RECOGNITION_LOOKBACK_MILLIS = 24L * 60L * 60L * 1000L

enum class SmsPatternType(val title: String) {
    INCOME("واریز"),
    EXPENSE("برداشت"),
    TRANSFER("انتقال وجه")
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
    val kind: SmsPatternSlotKind
)

data class SmsPattern(
    val id: String,
    val name: String,
    val type: SmsPatternType,
    val sampleMessage: String,
    val slots: List<SmsPatternSlot> = emptyList()
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
     * Returns the fixed per-user lower bound for SMS recognition. Existing installations
     * that already have templates but no timestamp are safely migrated to a 24-hour window
     * from the first access after this update; the timestamp is then permanent until the
     * user removes every template.
     */
    @Synchronized
    fun getRecognitionStartMillis(userId: String? = SessionManager.userId): Long? {
        val patterns = getPatterns(userId)
        if (patterns.isEmpty()) return null
        val key = recognitionStartKey(userId)
        preferences.getLong(key, 0L).takeIf { it > 0L }?.let { return it }
        val start = System.currentTimeMillis() - SMS_RECOGNITION_LOOKBACK_MILLIS
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
                            if (start >= 0 && end > start && end <= message.length) {
                                add(SmsPatternSlot(start, end, kind))
                            }
                        }
                    }
                    add(
                        SmsPattern(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            name = name,
                            type = type,
                            sampleMessage = message,
                            slots = slots.sortedBy { it.start }
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
                .putLong(recognitionStartKey(userId), System.currentTimeMillis() - SMS_RECOGNITION_LOOKBACK_MILLIS)
                .commit()
        } else if (patterns.isNotEmpty()) {
            getRecognitionStartMillis(userId)
        }
        if (index >= 0) patterns[index] = pattern else patterns.add(0, pattern)
        persist(patterns, userId)
    }

    fun create(
        name: String,
        type: SmsPatternType,
        sampleMessage: String,
        slots: List<SmsPatternSlot>,
        userId: String? = SessionManager.userId
    ): SmsPattern {
        return SmsPattern(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            type = type,
            sampleMessage = sampleMessage,
            slots = slots.sortedBy { it.start }
        ).also { save(it, userId) }
    }

    @Synchronized
    fun delete(id: String, userId: String? = SessionManager.userId) {
        val remaining = getPatterns(userId).filterNot { it.id == id }
        persist(remaining, userId)
        if (remaining.isEmpty()) {
            preferences.edit().remove(recognitionStartKey(userId)).commit()
        }
    }

    /**
     * Replaces the complete account-scoped template snapshot and its recognition boundary.
     * The values in the backup are portable; this method writes them into this device's
     * own EncryptedSharedPreferences/Android Keystore namespace.
     */
    @Synchronized
    fun restoreAll(
        patterns: List<SmsPattern>,
        recognitionStartMillis: Long?,
        userId: String
    ) {
        require(userId.isNotBlank()) { "A user ID is required to restore SMS patterns." }
        require(patterns.map { it.id }.distinct().size == patterns.size) {
            "Duplicate SMS pattern IDs in backup."
        }
        patterns.forEach { pattern ->
            require(pattern.id.isNotBlank() && pattern.name.isNotBlank() && pattern.sampleMessage.isNotEmpty())
            var previousEnd = 0
            pattern.slots.sortedBy { it.start }.forEach { slot ->
                require(slot.start >= previousEnd && slot.start >= 0 &&
                    slot.endExclusive > slot.start && slot.endExclusive <= pattern.sampleMessage.length) {
                    "Invalid SMS pattern slot in backup."
                }
                previousEnd = slot.endExclusive
            }
        }
        require(patterns.isEmpty() || (recognitionStartMillis != null && recognitionStartMillis > 0L)) {
            "SMS recognition start time is missing from backup."
        }

        val array = JSONArray()
        patterns.forEach { pattern ->
            val slots = JSONArray()
            pattern.slots.forEach { slot ->
                slots.put(
                    JSONObject()
                        .put("start", slot.start)
                        .put("endExclusive", slot.endExclusive)
                        .put("kind", slot.kind.name)
                )
            }
            array.put(
                JSONObject()
                    .put("id", pattern.id)
                    .put("name", pattern.name)
                    .put("type", pattern.type.name)
                    .put("sampleMessage", pattern.sampleMessage)
                    .put("slots", slots)
            )
        }
        val editor = preferences.edit().putString(storageKey(userId), array.toString())
        if (patterns.isEmpty()) {
            editor.remove(recognitionStartKey(userId))
        } else {
            editor.putLong(recognitionStartKey(userId), recognitionStartMillis!!)
        }
        check(editor.commit()) { "Could not persist restored SMS patterns securely." }
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
                )
            }
            array.put(
                JSONObject()
                    .put("id", pattern.id)
                    .put("name", pattern.name)
                    .put("type", pattern.type.name)
                    .put("sampleMessage", pattern.sampleMessage)
                    .put("slots", slots)
            )
        }
        preferences.edit().putString(storageKey(userId), array.toString()).apply()
    }
}