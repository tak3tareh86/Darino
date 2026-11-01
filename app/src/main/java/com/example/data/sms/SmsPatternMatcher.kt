package com.example.data.sms

/**
 * Structural matcher for templates explicitly configured by the user.
 * Only saved slot ranges may vary. All text outside those ranges is literal.
 */
object SmsPatternMatcher {

    data class NumericCandidate(
        val start: Int,
        val endExclusive: Int,
        val value: String
    )

    data class MatchedFields(
        val values: Map<SmsPatternSlotKind, List<String>>
    ) {
        fun first(kind: SmsPatternSlotKind): String? = values[kind]?.firstOrNull()
    }

    /**
     * Editor-only whitespace token boundaries. No token is classified or inferred:
     * the user explicitly selects the exact token and assigns its semantic slot kind.
     */
    fun detectNumericCandidates(message: String): List<NumericCandidate> {
        if (message.isEmpty()) return emptyList()
        return Regex("""\S+""").findAll(message).map { match ->
            NumericCandidate(match.range.first, match.range.last + 1, match.value)
        }.toList()
    }

    fun findUniqueMatch(message: String, patterns: List<SmsPattern>): SmsPattern? {
        val normalizedMessage = BankSmsParser.normalizeText(message)
        if (normalizedMessage.isBlank()) return null

        val matches = patterns.filter { pattern ->
            val sample = BankSmsParser.normalizeText(pattern.sampleMessage)
            val slots = validSlots(sample, pattern.slots)
            if (sample.isBlank() || slots.isEmpty() ||
                slots.count { it.kind == SmsPatternSlotKind.AMOUNT_RIAL || it.kind == SmsPatternSlotKind.AMOUNT_TOMAN } != 1
            ) {
                false
            } else {
                buildTemplateRegex(sample, slots).matches(normalizedMessage)
            }
        }
        // Ambiguous templates are not resolved by scoring or priority.
        return matches.singleOrNull()
    }

    fun extractMatchedFields(message: String, pattern: SmsPattern): MatchedFields? {
        val sample = BankSmsParser.normalizeText(pattern.sampleMessage)
        val normalizedMessage = BankSmsParser.normalizeText(message)
        if (sample.isBlank() || normalizedMessage.isBlank()) return null
        val slots = validSlots(sample, pattern.slots)
        if (slots.isEmpty() ||
            slots.count { it.kind == SmsPatternSlotKind.AMOUNT_RIAL || it.kind == SmsPatternSlotKind.AMOUNT_TOMAN } != 1
        ) return null

        val match = buildTemplateRegex(sample, slots).matchEntire(normalizedMessage) ?: return null
        val values = mutableMapOf<SmsPatternSlotKind, MutableList<String>>()
        slots.forEachIndexed { index, slot ->
            val value = match.groups[index + 1]?.value ?: return null
            values.getOrPut(slot.kind) { mutableListOf() }.add(value)
        }
        return MatchedFields(values)
    }

    private fun validSlots(
        sample: String,
        configured: List<SmsPatternSlot>
    ): List<SmsPatternSlot> {
        val sorted = configured
            .filter { it.kind != SmsPatternSlotKind.FIXED }
            .sortedBy { it.start }
        if (sorted.isEmpty()) return emptyList()
        if (sorted.groupBy { slot ->
                when (slot.kind) {
                    SmsPatternSlotKind.AMOUNT_RIAL, SmsPatternSlotKind.AMOUNT_TOMAN -> "amount"
                    SmsPatternSlotKind.BALANCE_RIAL, SmsPatternSlotKind.BALANCE_TOMAN -> "balance"
                    SmsPatternSlotKind.DATE, SmsPatternSlotKind.DATE_DAY_MONTH, SmsPatternSlotKind.DATE_MONTH_DAY -> "date"
                    SmsPatternSlotKind.SOURCE_CARD, SmsPatternSlotKind.SOURCE_ACCOUNT -> "source"
                    SmsPatternSlotKind.DESTINATION_CARD, SmsPatternSlotKind.DESTINATION_ACCOUNT -> "destination"
                    else -> slot.kind.name
                }
            }.values.any { it.size > 1 }) return emptyList()
        var previousEnd = 0
        for (slot in sorted) {
            if (slot.start < previousEnd || slot.start < 0 ||
                slot.endExclusive <= slot.start || slot.endExclusive > sample.length
            ) return emptyList()
            previousEnd = slot.endExclusive
        }
        return sorted
    }

    private fun buildTemplateRegex(sample: String, slots: List<SmsPatternSlot>): Regex {
        val expression = buildString {
            var cursor = 0
            slots.forEach { slot ->
                append(Regex.escape(sample.substring(cursor, slot.start)))
                // Slot contents are opaque here. Meaning is assigned only by the
                // explicitly selected slot kind after this exact-position capture.
                append("([\\s\\S]+?)")
                cursor = slot.endExclusive
            }
            append(Regex.escape(sample.substring(cursor)))
        }
        return Regex("^$expression$")
    }
}