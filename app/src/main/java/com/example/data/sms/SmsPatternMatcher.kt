package com.example.data.sms

/**
 * Matches an incoming SMS against a sample explicitly entered by the user.
 *
 * The sample is a fixed template. Only the numeric value belonging to an amount
 * label, a calendar date, or a clock time may vary. Other numbers (card/account
 * suffixes, tracking codes, terminal IDs, etc.) remain literal text.
 */
object SmsPatternMatcher {

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
    private val timeValue = Regex(
        """(?:(?<=ساعت )|(?<=ساعت:))?[0-2]?[0-9]:[0-5][0-9]"""
    )

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
                add(VariableSpan(match.range.first, match.range.last + 1, "time"))
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
                        else -> """[0-9][0-9,،٬٫.]*"""
                    }
                )
                cursor = span.endExclusive
            }
            append(Regex.escape(sample.substring(cursor)))
        }
        return Regex("^$expression$", RegexOption.IGNORE_CASE)
    }
}
