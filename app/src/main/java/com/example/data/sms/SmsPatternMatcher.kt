package com.example.data.sms

/**
 * Learns changing numeric fields from one bank SMS sample.
 * Account/card identifiers remain literal; transaction direction is supplied by the user.
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
     * Every numeric value is learned as variable except numbers identified as
     * account/card identifiers. Amount and balance retain separate semantic kinds.
     */
    fun inferVariableSlots(message: String): List<SmsPatternSlot> {
        val normalized = message
        if (normalized.isBlank()) return emptyList()

        val slots = mutableListOf<SmsPatternSlot>()
        val tokens = tokenizeTemplateForManualSlotSelection(normalized)
        fun clean(value: String) = value.trim(' ', ',', '،', ';', ':', '.', ')', ']', '}', '؛', '(', '[', '{')

        val dateRegex = Regex("""[0-9۰-۹٠-٩]{1,4}[/.-][0-9۰-۹٠-٩]{1,2}(?:[/.-][0-9۰-۹٠-٩]{1,4})?""")
        val timeRegex = Regex("""(?:[01]?[0-9۰-۹٠-٩]|2[0-3]):[0-5]?[0-9۰-۹٠-٩](?::[0-5]?[0-9۰-۹٠-٩])?""")
        val dateRanges = dateRegex.findAll(normalized).toList()
        val timeRanges = timeRegex.findAll(normalized).toList()

        dateRanges.forEach { date ->
            val parts = date.value.split(Regex("[/.-]"))
            val kind = when {
                parts.size == 2 -> SmsPatternSlotKind.DATE_DAY_MONTH
                parts[0].length == 4 -> SmsPatternSlotKind.DATE
                else -> SmsPatternSlotKind.DATE_DAY_MONTH_YEAR
            }
            slots += SmsPatternSlot(date.range.first, date.range.last + 1, kind)
        }
        timeRanges.forEach { time ->
            if (slots.none { time.range.first < it.endExclusive && time.range.last + 1 > it.start }) {
                slots += SmsPatternSlot(time.range.first, time.range.last + 1, SmsPatternSlotKind.TIME)
            }
        }

        val amountLabel = Regex("""مبلغ|مبـلغ|وجه|میزان""")
        val transactionLabel = Regex("""واریز|برداشت|خرید|پرداخت|انتقال|تراکنش""")
        val currencyLabel = Regex("""ریال|ريال|تومان|تومن""")
        val rialLabel = Regex("""ریال|ريال""")
        val balanceLabel = Regex("""موجودی|مانده|اعتبار|قابل\s*برداشت""")
        val accountLabel = Regex("""حساب|کارت|شماره|شبا""")
        val trackingLabel = Regex("""پیگیری|رهگیری|شناسه""")
        val numberRegex = Regex("""[\d۰-۹٠-٩][\d۰-۹٠-٩,،٬٫.]*""")

        data class NumericCandidate(
            val start: Int,
            val end: Int,
            val kind: SmsPatternSlotKind,
            val score: Int
        )

        val candidates = mutableListOf<NumericCandidate>()
        var previousNumericEnd = 0
        // Labels are evaluated only within the current field segment (after the
        // previous numeric value). This keeps each user's account/card, amount,
        // balance and tracking fields independent even when all values share a line.
        val semanticLabelRegex = Regex(
            """شماره\s*کارت|شماره\s*حساب|کارت|حساب|شبا|قابل\s*برداشت|موجودی|مانده|اعتبار|مبلغ|مبـلغ|وجه|میزان|واریز|برداشت|خرید|پرداخت|انتقال|تراکنش|پیگیری|رهگیری|شناسه"""
        )
        val accountSemanticLabelRegex = Regex("""شماره\s*کارت|شماره\s*حساب|کارت|حساب|شبا""")
        numberRegex.findAll(normalized).forEach { number ->
            val start = number.range.first
            val end = number.range.last + 1
            val contextStart = maxOf(previousNumericEnd, start - 80)
            val contextBefore = normalized.substring(contextStart, start)
            previousNumericEnd = end
            if (slots.any { start < it.endExclusive && end > it.start }) return@forEach

            val tokenIndex = tokens.indexOfFirst { start >= it.start && start < it.endExclusive }
            val token = tokens.getOrNull(tokenIndex)
            val prefix = if (token != null) normalized.substring(token.start, start) else ""
            val suffix = if (token != null) normalized.substring(end, token.endExclusive) else ""
            val previousToken = tokens.getOrNull(tokenIndex - 1)?.value.orEmpty()
            val nextToken = tokens.getOrNull(tokenIndex + 1)?.value.orEmpty()

            val contextAfter = "$suffix $nextToken"
            // Use the nearest semantic label since the preceding numeric field.
            // Looking across several whitespace tokens can leak "balance" into
            // the following transaction amount (or vice versa).
            val nearestLabel = semanticLabelRegex.findAll(contextBefore).lastOrNull()?.value
            // Never vary a number explicitly attached to an account/card label.
            // A transaction label in the current field context takes precedence over
            // an account label on the previous line/token.
            val currentFieldHasTransactionLabel =
                transactionLabel.containsMatchIn(prefix) || amountLabel.containsMatchIn(prefix) ||
                    (nearestLabel != null && (transactionLabel.matches(nearestLabel) || amountLabel.matches(nearestLabel)))
            val currentFieldHasAccountLabel =
                accountLabel.containsMatchIn(prefix) ||
                    (nearestLabel != null && accountSemanticLabelRegex.matches(nearestLabel)) ||
                    accountLabel.containsMatchIn(clean(previousToken))

            // A nearby transaction/amount label wins over an account label from
            // an earlier part of the template. Otherwise the account/card value
            // remains fixed and is never learned as a changing numeric field.
            if (currentFieldHasAccountLabel && !currentFieldHasTransactionLabel) return@forEach

            val isBalance = (nearestLabel != null && balanceLabel.matches(nearestLabel)) ||
                balanceLabel.containsMatchIn(contextAfter)
            val isAmount = nearestLabel != null && amountLabel.matches(nearestLabel)
            val isTransactionAmount = nearestLabel != null && transactionLabel.matches(nearestLabel)
            val isTracking = nearestLabel != null && trackingLabel.matches(nearestLabel)

            val hasRial = rialLabel.containsMatchIn(contextAfter) ||
                rialLabel.containsMatchIn(contextBefore)
            val hasCurrency = currencyLabel.containsMatchIn(contextAfter) ||
                currencyLabel.containsMatchIn(contextBefore)

            val kind = when {
                isBalance -> if (hasRial) SmsPatternSlotKind.BALANCE_RIAL else SmsPatternSlotKind.BALANCE_TOMAN
                isAmount || isTransactionAmount || hasCurrency ->
                    if (hasRial) SmsPatternSlotKind.AMOUNT_RIAL else SmsPatternSlotKind.AMOUNT_TOMAN
                isTracking -> SmsPatternSlotKind.TRACKING
                else -> SmsPatternSlotKind.NUMBER
            }
            val score = when {
                isAmount -> 120 + if (hasCurrency) 20 else 0
                isTransactionAmount -> 100 + if (hasCurrency) 20 else 0
                hasCurrency -> 90
                isBalance -> 80
                isTracking -> 70
                else -> 10
            }
            candidates += NumericCandidate(start, end, kind, score)
        }

        // Only one numeric field is the transaction amount. Balance and all other
        // numbers are still variable, but are never confused with the transaction amount.
        val explicitlyLabeledAmount = candidates
            .filter { it.kind == SmsPatternSlotKind.AMOUNT_RIAL || it.kind == SmsPatternSlotKind.AMOUNT_TOMAN }
            .maxByOrNull { it.score }
        // Some banks send a bare amount with no currency or semantic prefix.
        // If it is the only remaining numeric field after date/time and account
        // identifiers have been excluded, it is the only safe generic fallback.
        // Multiple unlabeled numbers remain ambiguous and are never guessed.
        val amount = explicitlyLabeledAmount
            ?: candidates.singleOrNull { it.kind == SmsPatternSlotKind.NUMBER }
        candidates.forEach { candidate ->
            val isAmountCandidate =
                candidate.kind == SmsPatternSlotKind.AMOUNT_RIAL ||
                    candidate.kind == SmsPatternSlotKind.AMOUNT_TOMAN
            val storedKind = when {
                candidate == amount && !isAmountCandidate -> SmsPatternSlotKind.AMOUNT_TOMAN
                candidate == amount -> candidate.kind
                isAmountCandidate -> SmsPatternSlotKind.NUMBER
                else -> candidate.kind
            }
            // Preserve every numeric value as a variable. Only the strongest
            // amount candidate is assigned the transaction-amount role; other
            // amount-looking values stay variable but cannot overwrite the amount.
            slots += SmsPatternSlot(candidate.start, candidate.end, storedKind)
        }
        return slots.distinctBy { it.start to it.endExclusive }.sortedBy { it.start }
    }

    fun findUniqueMatch(message: String, patterns: List<SmsPattern>): SmsPattern? {
        val normalizedMessage = message
        if (normalizedMessage.isBlank()) return null
        val matches = patterns.filter { pattern ->
            val sample = pattern.sampleMessage
            val slots = validSlots(sample, pattern.slots)
            sample.isNotBlank() && slots.isNotEmpty() &&
                slots.count { it.kind == SmsPatternSlotKind.AMOUNT_RIAL || it.kind == SmsPatternSlotKind.AMOUNT_TOMAN } == 1 &&
                buildTemplateRegex(sample, slots).matches(normalizedMessage)
        }
        return matches.singleOrNull()
    }

    fun extractMatchedFields(message: String, pattern: SmsPattern): MatchedFields? {
        val sample = pattern.sampleMessage
        val normalizedMessage = message
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
            previousEnd = slot.endExclusive
        }
        return sorted
    }

    /**
     * Variable fields may change their digits, but every separator inside the
     * field (comma, decimal mark, slash, colon, etc.) remains literal.
     */
    private fun StringBuilder.appendVariableNumberPattern(value: String) {
        var cursor = 0
        Regex("""[0-9۰-۹٠-٩]+""").findAll(value).forEach { digits ->
            append(Regex.escape(value.substring(cursor, digits.range.first)))
            append("[0-9۰-۹٠-٩]+")
            cursor = digits.range.last + 1
        }
        append(Regex.escape(value.substring(cursor)))
    }

    private fun buildTemplateRegex(sample: String, slots: List<SmsPatternSlot>): Regex {
        val expression = buildString {
            var cursor = 0
            slots.forEach { slot ->
                append(Regex.escape(sample.substring(cursor, slot.start)))
                append('(')
                appendVariableNumberPattern(sample.substring(slot.start, slot.endExclusive))
                append(')')
                cursor = slot.endExclusive
            }
            append(Regex.escape(sample.substring(cursor)))
        }
        return Regex("^$expression$")
    }
}
