package com.example.data.sms

/**
 * Learns variable amount/date/time fields from one user-provided SMS sample.
 * Every other token (including account/card numbers) remains literal.
 */
object SmsPatternMatcher {

    data class TemplateToken(val start: Int, val endExclusive: Int, val value: String)

    data class MatchedFields(val values: Map<SmsPatternSlotKind, List<String>>) {
        fun first(kind: SmsPatternSlotKind): String? = values[kind]?.firstOrNull()
    }

    fun tokenizeTemplateForManualSlotSelection(message: String): List<TemplateToken> {
        if (message.isEmpty()) return emptyList()
        return Regex("""\\S+""").findAll(message).map {
            TemplateToken(it.range.first, it.range.last + 1, it.value)
        }.toList()
    }

    /**
     * Infers only the three fields that naturally change between bank notifications.
     * Explicit amount labels/currency outrank transaction verbs. Account/card-number
     * positions and balance positions are never considered amount candidates.
     */
    fun inferVariableSlots(message: String): List<SmsPatternSlot> {
        val normalized = BankSmsParser.normalizeText(message)
        val tokens = tokenizeTemplateForManualSlotSelection(normalized)
        if (tokens.isEmpty()) return emptyList()

        val slots = mutableListOf<SmsPatternSlot>()
        fun clean(token: String) = token.trimEnd(',', '،', ';', ':', '.', ')', ']', '}', '؛')
            .trimStart('(', '[', '{')

        val dateRegex = Regex("""^\\d{1,4}[/.-]\\d{1,2}(?:[/.-]\\d{1,4})?$""")
        val timeRegex = Regex("""^([01]?\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?$""")
        val dateToken = tokens.firstOrNull { dateRegex.matches(clean(it.value)) }
        if (dateToken != null) {
            val parts = clean(dateToken.value).split(Regex("[/.-]"))
            val kind = when {
                parts.size == 2 -> SmsPatternSlotKind.DATE_DAY_MONTH
                parts[0].length == 4 -> SmsPatternSlotKind.DATE
                parts[2].length == 4 -> SmsPatternSlotKind.DATE_DAY_MONTH_YEAR
                else -> SmsPatternSlotKind.DATE_DAY_MONTH_YEAR
            }
            slots += SmsPatternSlot(dateToken.start, dateToken.endExclusive, kind)
        }

        val timeToken = tokens.firstOrNull { timeRegex.matches(clean(it.value)) }
        if (timeToken != null) {
            slots += SmsPatternSlot(timeToken.start, timeToken.endExclusive, SmsPatternSlotKind.TIME)
        }

        val amountLabel = Regex("""مبلغ|مبـلغ|وجه|میزان""")
        val transactionLabel = Regex("""واریز|برداشت|خرید|پرداخت|انتقال|تراکنش""")
        val currencyLabel = Regex("""ریال|تومان|تومن""")
        val balanceLabel = Regex("""موجودی|مانده|اعتبار|قابل برداشت""")
        val accountLabel = Regex("""حساب|کارت|شماره|شبا|پیگیری""")
        val numberToken = Regex("""^[+−-]?[\\d۰-۹٠-٩][\\d۰-۹٠-٩,،٬٫.]*([\\p{L}]+)?[,،;:)]?$""")

        data class Candidate(val token: TemplateToken, val score: Int, val kind: SmsPatternSlotKind)
        val candidates = tokens.mapIndexedNotNull { index, token ->
            val value = clean(token.value)
            if (token == dateToken || token == timeToken || !numberToken.matches(value)) return@mapIndexedNotNull null
            val digits = value.count { it.isDigit() || it in '۰'..'۹' || it in '٠'..'٩' }
            if (digits == 0) return@mapIndexedNotNull null

            val previous = tokens.subList(maxOf(0, index - 3), index).map { clean(it.value) }
            val next = tokens.getOrNull(index + 1)?.let { clean(it.value) }.orEmpty()
            val previousText = previous.joinToString(" ")
            val nextText = next
            if (balanceLabel.containsMatchIn(previousText) || balanceLabel.containsMatchIn(nextText)) return@mapIndexedNotNull null
            // A number immediately introduced as an account/card/IBAN/reference is fixed.
            if (previous.lastOrNull()?.let { accountLabel.containsMatchIn(it) } == true) return@mapIndexedNotNull null
            if (previous.takeLast(2).joinToString(" ").let { Regex("""(شماره\\s+(?:حساب|کارت)|حساب\\s+شماره|کارت\\s+شماره)""").containsMatchIn(it) }) return@mapIndexedNotNull null

            val explicitAmountDistance = previous.indexOfLast { amountLabel.containsMatchIn(it) }
            val transactionDistance = previous.indexOfLast { transactionLabel.containsMatchIn(it) }
            val hasCurrency = currencyLabel.containsMatchIn(nextText) ||
                currencyLabel.containsMatchIn(token.value) ||
                previous.lastOrNull()?.let { currencyLabel.containsMatchIn(it) } == true
            if (explicitAmountDistance < 0 && transactionDistance < 0 && !hasCurrency) return@mapIndexedNotNull null

            // The nearest explicit "amount" label wins; digit length never increases priority.
            val score = when {
                explicitAmountDistance >= 0 -> 100 - (previous.lastIndex - explicitAmountDistance) * 10 + if (hasCurrency) 20 else 0
                hasCurrency -> 80
                transactionDistance >= 0 -> 50 - (previous.lastIndex - transactionDistance) * 10
                else -> 0
            }
            val kind = if (currencyLabel.containsMatchIn(nextText) ||
                currencyLabel.containsMatchIn(token.value) ||
                previous.lastOrNull()?.contains("ریال") == true
            ) SmsPatternSlotKind.AMOUNT_RIAL else SmsPatternSlotKind.AMOUNT_TOMAN
            Candidate(token, score, kind)
        }

        val best = candidates.maxByOrNull { it.score }
        if (best != null && best.score > 0) {
            slots += SmsPatternSlot(best.token.start, best.token.endExclusive, best.kind)
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
