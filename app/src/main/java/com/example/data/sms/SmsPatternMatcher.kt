package com.example.data.sms

/**
 * Learns variable amount/date/time fields from one user-provided SMS sample.
 * All other text remains literal, including account and card identifiers.
 */
object SmsPatternMatcher {

    data class TemplateToken(val start: Int, val endExclusive: Int, val value: String)

    data class MatchedFields(val values: Map<SmsPatternSlotKind, List<String>>) {
        fun first(kind: SmsPatternSlotKind): String? = values[kind]?.firstOrNull()
    }

    fun tokenizeTemplateForManualSlotSelection(message: String): List<TemplateToken> {
        if (message.isEmpty()) return emptyList()
        return Regex("""\S+""").findAll(message).map {
            TemplateToken(it.range.first, it.range.last + 1, it.value)
        }.toList()
    }

    /**
     * Automatically marks only amount, date and time as variable.
     * Card/account numbers and every other token are deliberately kept fixed.
     */
    fun inferVariableSlots(message: String): List<SmsPatternSlot> {
        val tokens = tokenizeTemplateForManualSlotSelection(message)
        if (tokens.isEmpty()) return emptyList()
        val slots = mutableListOf<SmsPatternSlot>()
        val dateRegex = Regex("""^\d{1,4}[/.-]\d{1,2}(?:[/.-]\d{2,4})?[،,;:]?$""")
        val timeRegex = Regex("""^\d{1,2}:\d{2}(?::\d{2})?$""")
        val numericRegex = Regex("""^[+−-]?[\d۰-۹٠-٩][\d۰-۹٠-٩,،٬٫.]*$""")
        val dateToken = tokens.firstOrNull { dateRegex.matches(it.value.trimEnd(',', '،', ';')) }
        if (dateToken != null) {
            val parts = dateToken.value.trimEnd(',', '،', ';').split(Regex("[/.-]"))
            val kind = when {
                parts.size == 2 -> SmsPatternSlotKind.DATE_DAY_MONTH
                parts.size == 3 && parts[2].length == 4 -> SmsPatternSlotKind.DATE_DAY_MONTH_YEAR
                parts.size == 3 && parts[0].length == 4 -> SmsPatternSlotKind.DATE
                parts.size == 3 -> SmsPatternSlotKind.DATE_DAY_MONTH_YEAR
                else -> SmsPatternSlotKind.DATE
            }
            slots += SmsPatternSlot(dateToken.start, dateToken.endExclusive, kind)
        }
        val timeToken = tokens.firstOrNull { timeRegex.matches(it.value.trimEnd(',', '،', ';')) }
        if (timeToken != null) {
            slots += SmsPatternSlot(timeToken.start, timeToken.endExclusive, SmsPatternSlotKind.TIME)
        }

        val candidates = tokens.mapIndexedNotNull { index, token ->
            val clean = token.value.trim(',', '،', ';', ':', '(', ')', '[', ']')
            if (!numericRegex.matches(clean) || token == dateToken || token == timeToken) return@mapIndexedNotNull null
            val digits = clean.count { it.isDigit() || it in '۰'..'۹' || it in '٠'..'٩' }
            // Long card/account identifiers are fixed template data, not amounts.
            if (digits !in 4..13) return@mapIndexedNotNull null
            val context = tokens.subList(maxOf(0, index - 3), index).joinToString(" ") { it.value } +
                " " + tokens.getOrNull(index + 1)?.value.orEmpty()
            val hasAmountLabel = Regex("""(مبلغ|وجه|واریز|برداشت|خرید|پرداخت|انتقال|ریال|تومان)""").containsMatchIn(context)
            val currency = tokens.getOrNull(index + 1)?.value.orEmpty()
            val kind = if (currency.contains("ریال")) SmsPatternSlotKind.AMOUNT_RIAL
                else SmsPatternSlotKind.AMOUNT_TOMAN
            if (!hasAmountLabel) return@mapIndexedNotNull null
            Triple(token, digits, if (hasAmountLabel) 1000 + digits else digits)
                .let { Triple(it.first, it.second, Pair(it.third, kind)) }
        }
        val amount = candidates.maxByOrNull { it.third.first }?.first
        if (amount != null) {
            val candidate = candidates.first { it.first == amount }
            slots += SmsPatternSlot(
                amount.start,
                amount.endExclusive,
                candidate.third.second
            )
        }
        return slots.sortedBy { it.start }
    }

    fun findUniqueMatch(message: String, patterns: List<SmsPattern>): SmsPattern? {
        val normalizedMessage = BankSmsParser.normalizeText(message)
        if (normalizedMessage.isBlank()) return null
        val matches = patterns.filter { pattern ->
            val sample = BankSmsParser.normalizeText(pattern.sampleMessage)
            val slots = validSlots(sample, pattern.slots)
            sample.isNotBlank() && slots.isNotEmpty() &&
                slots.count { it.kind == SmsPatternSlotKind.AMOUNT_RIAL || it.kind == SmsPatternSlotKind.AMOUNT_TOMAN } == 1 &&
                buildTemplateRegex(sample, slots).matches(normalizedMessage)
        }
        return matches.singleOrNull()
    }

    fun extractMatchedFields(message: String, pattern: SmsPattern): MatchedFields? {
        val sample = BankSmsParser.normalizeText(pattern.sampleMessage)
        val normalizedMessage = BankSmsParser.normalizeText(message)
        if (sample.isBlank() || normalizedMessage.isBlank()) return null
        val slots = validSlots(sample, pattern.slots)
        if (slots.isEmpty() || slots.count {
                it.kind == SmsPatternSlotKind.AMOUNT_RIAL || it.kind == SmsPatternSlotKind.AMOUNT_TOMAN
            } != 1) return null
        val match = buildTemplateRegex(sample, slots).matchEntire(normalizedMessage) ?: return null
        val values = mutableMapOf<SmsPatternSlotKind, MutableList<String>>()
        slots.forEachIndexed { index, slot ->
            val value = match.groups[index + 1]?.value ?: return null
            values.getOrPut(slot.kind) { mutableListOf() }.add(value)
        }
        return MatchedFields(values)
    }

    private fun validSlots(sample: String, configured: List<SmsPatternSlot>): List<SmsPatternSlot> {
        val sorted = configured.filter { it.kind != SmsPatternSlotKind.FIXED }.sortedBy { it.start }
        if (sorted.isEmpty()) return emptyList()
        var previousEnd = 0
        for (slot in sorted) {
            if (slot.start < previousEnd || slot.start < 0 ||
                slot.endExclusive <= slot.start || slot.endExclusive > sample.length
            ) return emptyList()
            if (sample.substring(slot.start, slot.endExclusive).any { it.isWhitespace() }) return emptyList()
            if (slot.start > 0 && !sample[slot.start - 1].isWhitespace()) return emptyList()
            if (slot.endExclusive < sample.length && !sample[slot.endExclusive].isWhitespace()) return emptyList()
            previousEnd = slot.endExclusive
        }
        return sorted
    }

    private fun buildTemplateRegex(sample: String, slots: List<SmsPatternSlot>): Regex {
        val expression = buildString {
            var cursor = 0
            slots.forEach { slot ->
                append(Regex.escape(sample.substring(cursor, slot.start)))
                append("([^\\s]+)")
                cursor = slot.endExclusive
            }
            append(Regex.escape(sample.substring(cursor)))
        }
        return Regex("^$expression$")
    }
}
