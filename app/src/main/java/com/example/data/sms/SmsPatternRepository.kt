package com.example.data.sms

import android.content.Context
import com.example.data.security.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class SmsPatternType(val title: String) {
    INCOME("واریز"),
    EXPENSE("برداشت")
}

data class SmsPattern(
    val id: String,
    val name: String,
    val type: SmsPatternType,
    val sampleMessage: String
)

/**
 * Local, account-scoped storage for user-provided bank SMS examples.
 * This repository only manages examples; it does not read the inbox or parse transactions.
 */
class SmsPatternRepository(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("darino_sms_patterns", Context.MODE_PRIVATE)

    private fun storageKey(): String {
        val userId = SessionManager.userId?.takeIf { it.isNotBlank() } ?: "local"
        return "patterns_$userId"
    }

    fun getPatterns(): List<SmsPattern> {
        val raw = preferences.getString(storageKey(), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val type = runCatching {
                        SmsPatternType.valueOf(item.optString("type"))
                    }.getOrNull() ?: continue
                    val name = item.optString("name").trim()
                    val message = item.optString("sampleMessage").trim()
                    if (name.isNotEmpty() && message.isNotEmpty()) {
                        add(
                            SmsPattern(
                                id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                                name = name,
                                type = type,
                                sampleMessage = message
                            )
                        )
                    }
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

    fun create(name: String, type: SmsPatternType, sampleMessage: String): SmsPattern {
        return SmsPattern(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            type = type,
            sampleMessage = sampleMessage.trim()
        ).also(::save)
    }

    fun delete(id: String) {
        persist(getPatterns().filterNot { it.id == id })
    }

    private fun persist(patterns: List<SmsPattern>) {
        val array = JSONArray()
        patterns.forEach { pattern ->
            array.put(
                JSONObject()
                    .put("id", pattern.id)
                    .put("name", pattern.name)
                    .put("type", pattern.type.name)
                    .put("sampleMessage", pattern.sampleMessage)
            )
        }
        preferences.edit().putString(storageKey(), array.toString()).apply()
    }
}
