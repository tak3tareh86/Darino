package com.example.data.sms

/**
 * Learns changing numeric fields from one user-provided bank SMS sample.
 * Account/card identifiers remain fixed unless the user explicitly adds them to a strict allow-list.
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
                parts.last().length == 4 -> SmsPatternSlotKind.DATE_DAY_MONTH_YEAR
                else -> SmsPatternSlotKind.DATE_YEAR_MONTH_DAY
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
        val accountLabel = Regex("""(?:شماره\s*)?(?:حساب|کارت|شبا)""")
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
            // Long identifiers following an explicit source phrase are account numbers,
            // not transaction amounts when the source-account label is explicit.
            val compactNumber = number.value.filter { it in '0'..'9' || it in '۰'..'۹' || it in '٠'..'٩' }
            val explicitAccountSource = Regex("""(?:برداشت|واریز|انتقال)\s+از\s*[:：]?\s*$""").containsMatchIn(contextBefore) ||
                Regex("""(?:واریز|انتقال)\s+به\s*[:：]?\s*$""").containsMatchIn(contextBefore)
            if (compactNumber.length >= 8 && explicitAccountSource) return@forEach

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

    /** Detect one account/card identifier explicitly introduced by its surrounding label. */
    fun inferAccountIdentifierRange(message: String): IntRange? {
        val candidates = Regex("""[0-9۰-۹٠-٩]{6,26}""").findAll(message).filter { match ->
            val before = message.substring(maxOf(0, match.range.first - 36), match.range.first)
            val explicitAccountLabel =
                Regex("""(?:شماره\s*)?(?:حساب|کارت|شبا)\s*[:：]?\s*$""").containsMatchIn(before)
            val explicitSourceLabel =
                Regex("""(?:برداشت|واریز|انتقال)\s+از\s*[:：]?\s*$""").containsMatchIn(before) ||
                    Regex("""(?:واریز|انتقال)\s+به\s*[:：]?\s*$""").containsMatchIn(before)
            (explicitAccountLabel || (explicitSourceLabel && match.value.length >= 8))
        }.toList()
        return candidates.singleOrNull()?.range
    }

    private fun normalizeDigits(value: String): String = buildString {
        value.forEach { char ->
            append(when (char) {
                in '0'..'9' -> char
                in '۰'..'۹' -> ('0'.code + (char.code - '۰'.code)).toChar()
                in '٠'..'٩' -> ('0'.code + (char.code - '٠'.code)).toChar()
                else -> char
            })
        }
    }

    /** Convert the sample identifier into an explicit allow-list slot. */
    fun configureAllowedAccountNumbers(
        message: String,
        inferredSlots: List<SmsPatternSlot>,
        enteredValues: List<String>
    ): List<SmsPatternSlot> {
        val range = inferAccountIdentifierRange(message) ?: return inferredSlots
        val sampleValue = message.substring(range.first, range.last + 1)
        fun digits(value: String) = normalizeDigits(value).filter { it in '0'..'9' }
        val allowed = (enteredValues + sampleValue).map(::digits).filter { it.isNotBlank() }.distinct()
        if (allowed.none { it == digits(sampleValue) }) return inferredSlots
        val context = message.substring(maxOf(0, range.first - 36), range.first)
        val kind = if (context.contains("کارت")) SmsPatternSlotKind.SOURCE_CARD else SmsPatternSlotKind.SOURCE_ACCOUNT
        return (inferredSlots.filterNot { range.first < it.endExclusive && range.last + 1 > it.start } +
            SmsPatternSlot(range.first, range.last + 1, kind, allowed)).sortedBy { it.start }
    }
    fun matchesTemplateBody(message: String, pattern: SmsPattern): Boolean {
        val normalizedMessage = normalizeForMatching(message)
        val sample = pattern.sampleMessage
        val slots = validSlots(sample, pattern.slots)
        return normalizedMessage.isNotBlank() && sample.isNotBlank() && slots.isNotEmpty() &&
            slots.count {
                it.kind == SmsPatternSlotKind.AMOUNT_RIAL || it.kind == SmsPatternSlotKind.AMOUNT_TOMAN
            } == 1 && buildTemplateRegex(sample, slots).matches(normalizedMessage)
    }

    fun findMatchingPatterns(
        message: String,
        patterns: List<SmsPattern>,
        sender: String? = null
    ): List<SmsPattern> = patterns.filter { pattern ->
        // Sender IDs are exact identifiers: preserve case, spaces and punctuation.
        pattern.senderAddress.isBlank() || pattern.senderAddress == sender
    }.filter { matchesTemplateBody(message, it) }

    fun findUniqueMatch(message: String, patterns: List<SmsPattern>): SmsPattern? =
        findMatchingPatterns(message, patterns).singleOrNull()

    fun extractMatchedFields(message: String, pattern: SmsPattern): MatchedFields? {
        val sample = pattern.sampleMessage
        val normalizedMessage = normalizeForMatching(message)
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
        val accountKinds = setOf(
            SmsPatternSlotKind.SOURCE_ACCOUNT, SmsPatternSlotKind.DESTINATION_ACCOUNT,
            SmsPatternSlotKind.SOURCE_CARD, SmsPatternSlotKind.DESTINATION_CARD
        )
        var previousEnd = 0
        for (slot in sorted) {
            if (slot.start < previousEnd || slot.start < 0 ||
                slot.endExclusive <= slot.start || slot.endExclusive > sample.length
            ) return emptyList()
            previousEnd = slot.endExclusive
        }
        if (sorted.any { slot ->
                slot.kind in accountKinds &&
                    (slot.allowedValues.isEmpty() ||
                        slot.allowedValues.none { normalizeDigits(it) == normalizeDigits(sample.substring(slot.start, slot.endExclusive)) })
            }) return emptyList()
        return sorted
    }

    /**
     * Date/time separators and all non-numeric template text remain literal.
     * Amount/balance grouping marks may repeat with the digit count, but only
     * using the same grouping character and valid three-digit groups.
     */
    private fun StringBuilder.appendVariableNumberPattern(value: String, slot: SmsPatternSlot) {
        val kind = slot.kind
        val accountKinds = setOf(
            SmsPatternSlotKind.SOURCE_ACCOUNT, SmsPatternSlotKind.DESTINATION_ACCOUNT,
            SmsPatternSlotKind.SOURCE_CARD, SmsPatternSlotKind.DESTINATION_CARD
        )
        if (kind in accountKinds) {
            append("(?:")
            append(slot.allowedValues.distinct().joinToString("|") { allowed ->
                normalizeDigits(allowed).map { digit ->
                    when (digit) {
                        in '0'..'9' -> "[" + digit + (('۰'.code + digit.code - '0'.code).toChar()) + (('٠'.code + digit.code - '0'.code).toChar()) + "]"
                        else -> Regex.escape(digit.toString())
                    }
                }.joinToString("")
            })
            append(")")
            return
        }
        val fixedDigitWidth = kind == SmsPatternSlotKind.DATE ||
            kind == SmsPatternSlotKind.DATE_DAY_MONTH ||
            kind == SmsPatternSlotKind.DATE_MONTH_DAY ||
            kind == SmsPatternSlotKind.DATE_DAY_MONTH_YEAR ||
            kind == SmsPatternSlotKind.DATE_MONTH_DAY_YEAR ||
            kind == SmsPatternSlotKind.DATE_YEAR_MONTH_DAY ||
            kind == SmsPatternSlotKind.TIME

        // Amount/balance grouping marks are numeric formatting, so their count
        // must grow or shrink with the amount (50,000 -> 1,000,000 -> 10,000).
        // The exact grouping character used by the sample remains constrained.
        val amountKinds = setOf(
            SmsPatternSlotKind.AMOUNT_RIAL, SmsPatternSlotKind.AMOUNT_TOMAN,
            SmsPatternSlotKind.BALANCE_RIAL, SmsPatternSlotKind.BALANCE_TOMAN
        )
        if (kind in amountKinds) {
            val numericRun = Regex("""[0-9۰-۹٠-٩][0-9۰-۹٠-٩,،٬]*""").find(value)
            val separators = numericRun?.value?.filter { it == ',' || it == '،' || it == '٬' }?.toList()?.distinct().orEmpty()
            if (numericRun != null && separators.size == 1) {
                val separator = Regex.escape(separators.single().toString())
                val digit = "[0-9۰-۹٠-٩]"
                append(Regex.escape(value.substring(0, numericRun.range.first)))
                append("(?:")
                append(digit).append("+|")
                append(digit).append("{1,3}(?:").append(separator).append(digit).append("{3})+")
                append(")")
                append(Regex.escape(value.substring(numericRun.range.last + 1)))
                return
            }
        }

        var cursor = 0
        Regex("""[0-9۰-۹٠-٩]+""").findAll(value).forEach { digits ->
            append(Regex.escape(value.substring(cursor, digits.range.first)))
            if (fixedDigitWidth) append("[0-9۰-۹٠-٩]{" + digits.value.length + "}")
            else append("[0-9۰-۹٠-٩]+")
            cursor = digits.range.last + 1
        }
        append(Regex.escape(value.substring(cursor)))
    }

    /**
     * Character-for-character normalization preserves offsets stored in templates.
     * Whitespace runs match flexibly, including LF/CRLF and non-breaking spaces.
     */
    private fun normalizeForMatching(value: String): String = buildString(value.length) {
        value.forEach { char ->
            append(when (char) {
                'ي', 'ى' -> 'ی'
                'ك' -> 'ک'
                '\u00A0', '\u2007', '\u202F', '\u200C', '\u200D' -> ' '
                in '۰'..'۹' -> ('0'.code + char.code - '۰'.code).toChar()
                in '٠'..'٩' -> ('0'.code + char.code - '٠'.code).toChar()
                else -> char
            })
        }
    }

    private fun appendFixedTextPattern(target: StringBuilder, value: String) {
        var cursor = 0
        Regex("""\s+""").findAll(value).forEach { whitespace ->
            target.append(Regex.escape(value.substring(cursor, whitespace.range.first)))
            target.append("""\s+""")
            cursor = whitespace.range.last + 1
        }
        target.append(Regex.escape(value.substring(cursor)))
    }

    private fun buildTemplateRegex(sample: String, slots: List<SmsPatternSlot>): Regex {
        val normalizedSample = normalizeForMatching(sample)
        val expression = buildString {
            var cursor = 0
            slots.forEach { slot ->
                appendFixedTextPattern(this, normalizedSample.substring(cursor, slot.start))
                append('(')
                appendVariableNumberPattern(normalizedSample.substring(slot.start, slot.endExclusive), slot)
                append(')')
                cursor = slot.endExclusive
            }
            appendFixedTextPattern(this, normalizedSample.substring(cursor))
        }
        return Regex("^$expression$")
    }
}
