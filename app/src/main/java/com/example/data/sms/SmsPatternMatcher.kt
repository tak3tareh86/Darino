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

    fun detectNumericCandidates(message: String): List<NumericCandidate> {
        val normalized = BankSmsParser.normalizeText(message)
        if (normalized.isBlank()) return emptyList()

        val candidates = mutableListOf<NumericCandidate>()
        val dateRegex = Regex("""(?<![0-9])[0-9]{2,4}[/.-][0-9]{1,2}[/.-][0-9]{1,2}(?![0-9])""")
        val timeRegex = Regex("""(?<![0-9])[0-2]?[0-9]:[0-5][0-9](?::[0-5][0-9])?(?![0-9])""")
        dateRegex.findAll(normalized).forEach {
            candidates.add(NumericCandidate(it.range.first, it.range.last + 1, it.value))
        }
        val shortDateRegex = Regex("""(?<![0-9])[0-9]{1,2}[/.-][0-9]{1,2}(?![0-9])""")
        shortDateRegex.findAll(normalized).forEach { match ->
            if (candidates.none { match.range.first < it.endExclusive && match.range.last + 1 > it.start }) {
                candidates.add(NumericCandidate(match.range.first, match.range.last + 1, match.value))
            }
        }
        timeRegex.findAll(normalized).forEach {
            candidates.add(NumericCandidate(it.range.first, it.range.last + 1, it.value))
        }

        val numericRegex = Regex("""(?<![0-9])[+-]?[0-9][0-9,،٬٫. ]*(?![0-9])""")
        numericRegex.findAll(normalized).forEach { match ->
            if (candidates.none { match.range.first < it.endExclusive && match.range.last + 1 > it.start }) {
                val value = match.value.trim()
                val leading = match.value.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
                val trailing = match.value.indexOfLast { !it.isWhitespace() } + 1
                if (trailing > leading) {
                    candidates.add(
                        NumericCandidate(
                            match.range.first + leading,
                            match.range.first + trailing,
                            value
                        )
                    )
                }
            }
        }

        // Card/account/tracking values may contain masking symbols or separators.
        val identifierRegex = Regex("""(?<![0-9])[0-9*#][0-9*# .-]{2,32}[0-9*#](?![0-9])""")
        identifierRegex.findAll(normalized).forEach { match ->
            if (candidates.none { match.range.first < it.endExclusive && match.range.last + 1 > it.start }) {
                candidates.add(NumericCandidate(match.range.first, match.range.last + 1, match.value))
            }
        }
        return candidates.sortedBy { it.start }
    }

    fun findBestMatch(message: String, patterns: List<SmsPattern>): SmsPattern? {
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
                append("(")
                append(
                    when (slot.kind) {
                        SmsPatternSlotKind.AMOUNT_RIAL,
                        SmsPatternSlotKind.AMOUNT_TOMAN,
                        SmsPatternSlotKind.BALANCE_RIAL,
                        SmsPatternSlotKind.BALANCE_TOMAN -> """[+-]?[0-9][0-9,،٬٫.]*"""
                        SmsPatternSlotKind.DATE,
                        SmsPatternSlotKind.DATE_DAY_MONTH,
                        SmsPatternSlotKind.DATE_MONTH_DAY -> """(?:[0-9]{2,4}[/.-][0-9]{1,2}[/.-][0-9]{1,2}|[0-9]{1,2}[/.-][0-9]{1,2})"""
                        SmsPatternSlotKind.TIME -> """[0-2]?[0-9]:[0-5][0-9](?::[0-5][0-9])?"""
                        SmsPatternSlotKind.SOURCE_CARD,
                        SmsPatternSlotKind.DESTINATION_CARD,
                        SmsPatternSlotKind.SOURCE_ACCOUNT,
                        SmsPatternSlotKind.DESTINATION_ACCOUNT,
                        SmsPatternSlotKind.TRACKING -> """[0-9*#][0-9*# .-]{1,32}[0-9*#]"""
                        SmsPatternSlotKind.FIXED -> Regex.escape(sample.substring(slot.start, slot.endExclusive))
                    }
                )
                append(")")
                cursor = slot.endExclusive
            }
            append(Regex.escape(sample.substring(cursor)))
        }
        return Regex("^$expression$", RegexOption.IGNORE_CASE)
    }
}