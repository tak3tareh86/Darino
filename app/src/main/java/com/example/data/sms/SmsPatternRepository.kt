package com.example.data.sms

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.security.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
    DATE("تاریخ"),
    TIME("ساعت"),
    SOURCE_CARD("کارت مبدأ"),
    DESTINATION_CARD("کارت مقصد"),
    SOURCE_ACCOUNT("حساب مبدأ"),
    DESTINATION_ACCOUNT("حساب مقصد"),
    TRACKING("کد پیگیری"),
    FIXED("عدد ثابت")
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
                    val message = item.optString("sampleMessage").trim()
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

    fun save(pattern: SmsPattern) {
        val patterns = getPatterns().toMutableList()
        val index = patterns.indexOfFirst { it.id == pattern.id }
        if (index >= 0) patterns[index] = pattern else patterns.add(0, pattern)
        persist(patterns)
    }

    fun create(
        name: String,
        type: SmsPatternType,
        sampleMessage: String,
        slots: List<SmsPatternSlot>
    ): SmsPattern {
        return SmsPattern(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            type = type,
            sampleMessage = sampleMessage.trim(),
            slots = slots.sortedBy { it.start }
        ).also(::save)
    }

    fun delete(id: String) {
        persist(getPatterns().filterNot { it.id == id })
    }

    private fun persist(patterns: List<SmsPattern>) {
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
        preferences.edit().putString(storageKey(), array.toString()).apply()
    }
}