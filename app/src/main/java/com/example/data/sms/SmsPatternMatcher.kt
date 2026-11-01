package com.example.data.sms

/**
 * Matches an incoming SMS against a sample explicitly entered by the user.
 *
 * The sample is a fixed template. Only values explicitly following amount, date,
 * time, card/account, or tracking-code labels may vary. Their labels and positions,
 * and every other character/number in the sample, remain fixed.
 */
object SmsPatternMatcher {

    data class MatchedAmount(val numericValue: String, val currency: String?)

    private data class Candidate(
        val pattern: SmsPattern,
        val specificity: Int
    )

    private data class VariableSpan(
        val start: Int,
        val endExclusive: Int,
        val kind: String
    )

    private val amountLabel = Regex(
        """(?:مبلغ(?:\s+(?:اصلی|تراکنش|انتقال|برداشت|واریز|خرید|پرداخت))?|به\s*مبلغ)\s*[:：]?\s*[+-]?\s*[0-9][0-9,،٬٫.]*""",
        RegexOption.IGNORE_CASE
    )
    private val dateValue = Regex(
        """(?<![0-9])(?:13|14)[0-9]{2}[/.-](?:0?[1-9]|1[0-2])[/.-](?:0?[1-9]|[12][0-9]|3[01])(?![0-9])"""
    )

    // Labels and their positions stay literal; only the following identifier value varies.
    private val cardOrAccountLabel = Regex(
        """(?:شماره\\s*کارت|کارت\\s*(?:مبدأ|مبدا|مقصد)|از\\s*کارت|به\\s*کارت|شماره\\s*حساب|حساب\\s*(?:مبدأ|مبدا|مقصد)|از\\s*حساب|به\\s*حساب)\\s*[:：#-]?\\s*[0-9*#][0-9*# -]{1,32}[0-9*#]"""
    )
    private val trackingLabel = Regex(
        """(?:کد|شماره)\\s*(?:پیگیری|رهگیری)|شناسه\\s*(?:پیگیری|رهگیری)"""
    )
    private val identifierValue = Regex("""[0-9*#][0-9*# -]{1,32}[0-9*#]""")
    private val timeValue = Regex(
        """ساعت\s*[:：]?\s*[0-2]?[0-9]:[0-5][0-9]"""
    )

    fun extractMatchedAmount(message: String, pattern: SmsPattern): MatchedAmount? {
        val sample = BankSmsParser.normalizeText(pattern.sampleMessage)
        val normalizedMessage = BankSmsParser.normalizeText(message)
        if (sample.isBlank() || normalizedMessage.isBlank()) return null

        val spans = findVariableSpans(sample)
        val amountSpan = spans.firstOrNull { it.kind == "amount" } ?: return null
        val match = buildTemplateRegex(sample, spans).matchEntire(normalizedMessage) ?: return null
        val amountGroup = spans.takeWhile { it != amountSpan }.count { it.kind == "amount" } + 1
        val numericValue = match.groupValues.getOrNull(amountGroup)?.takeIf { it.isNotBlank() } ?: return null
        val suffix = sample.substring(amountSpan.endExclusive)
        val currency = Regex("""^\s*(ریال|تومان|Rls|IRR)""", RegexOption.IGNORE_CASE)
            .find(suffix)?.groupValues?.get(1)
        return MatchedAmount(numericValue, currency)
    }

    fun findBestMatch(message: String, patterns: List<SmsPattern>): SmsPattern? {
        val normalizedMessage = BankSmsParser.normalizeText(message)
        if (normalizedMessage.isBlank()) return null

        val candidates = patterns.mapNotNull { pattern ->
            val sample = BankSmsParser.normalizeText(pattern.sampleMessage)
            if (sample.isBlank()) return@mapNotNull null

            val spans = findVariableSpans(sample)
            val regex = buildTemplateRegex(sample, spans)
            if (regex.matches(normalizedMessage)) {
                val specificity = sample.length - spans.sumOf { it.endExclusive - it.start }
                Candidate(pattern, specificity)
            } else {
                null
            }
        }

        if (candidates.isEmpty()) return null
        val bestSpecificity = candidates.maxOf { it.specificity }
        return candidates.filter { it.specificity == bestSpecificity }
            .singleOrNull()?.pattern
    }

    private fun findVariableSpans(sample: String): List<VariableSpan> {
        val possible = buildList {
            amountLabel.findAll(sample).forEach { match ->
                // Keep the amount label and separators fixed; vary only its numeric value.
                val number = Regex("""[0-9][0-9,،٬٫.]*""")
                    .find(match.value) ?: return@forEach
                add(
                    VariableSpan(
                        match.range.first + number.range.first,
                        match.range.first + number.range.last + 1,
                        "amount"
                    )
                )
            }
            dateValue.findAll(sample).forEach { match ->
                add(VariableSpan(match.range.first, match.range.last + 1, "date"))
            }
            timeValue.findAll(sample).forEach { match ->
                val time = Regex("""[0-2]?[0-9]:[0-5][0-9]""").find(match.value)
                    ?: return@forEach
                add(
                    VariableSpan(
                        match.range.first + time.range.first,
                        match.range.first + time.range.last + 1,
                        "time"
                    )
                )
            }

            cardOrAccountLabel.findAll(sample).forEach { match ->
                val value = identifierValue.find(match.value) ?: return@forEach
                add(
                    VariableSpan(
                        match.range.first + value.range.first,
                        match.range.first + value.range.last + 1,
                        "identifier"
                    )
                )
            }

            trackingLabel.findAll(sample).forEach { labelMatch ->
                val tailStart = labelMatch.range.last + 1
                val tail = sample.substring(tailStart)
                val separator = Regex("""^\\s*[:：#-]?\\s*""").find(tail) ?: return@forEach
                val value = identifierValue.find(tail, separator.range.last + 1) ?: return@forEach
                add(
                    VariableSpan(
                        tailStart + value.range.first,
                        tailStart + value.range.last + 1,
                        "identifier"
                    )
                )
            }
        }.sortedBy { it.start }

        val accepted = mutableListOf<VariableSpan>()
        for (span in possible) {
            if (accepted.none { span.start < it.endExclusive && span.endExclusive > it.start }) {
                accepted.add(span)
            }
        }
        return accepted
    }

    private fun buildTemplateRegex(sample: String, spans: List<VariableSpan>): Regex {
        val expression = buildString {
            var cursor = 0
            for (span in spans) {
                append(Regex.escape(sample.substring(cursor, span.start)))
                append(
                    when (span.kind) {
                        "date" -> """[0-9]{4}[/.-][0-9]{1,2}[/.-][0-9]{1,2}"""
                        "time" -> """[0-2]?[0-9]:[0-5][0-9]"""
                        "identifier" -> """[0-9*#][0-9*# -]{1,32}[0-9*#]"""
                        else -> """([0-9][0-9,،٬٫.]*)"""
                    }
                )
                cursor = span.endExclusive
            }
            append(Regex.escape(sample.substring(cursor)))
        }
        return Regex("^$expression$", RegexOption.IGNORE_CASE)
    }
}
