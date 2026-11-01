package com.example.data.sms

/**
 * Matches an incoming bank SMS against the user's saved examples.
 *
 * Literal text is kept strict while numeric fields are treated as variable values.
 * If equally specific samples match, the result is considered ambiguous and rejected.
 */
object SmsPatternMatcher {

    private data class Candidate(
        val pattern: SmsPattern,
        val specificity: Int
    )

    private val variableNumber = Regex("""[*#]*[0-9][0-9*#.,٬،٫/-]*""")
    private const val MAX_VARIABLE_TOKEN_LENGTH = 40

    fun findBestMatch(message: String, patterns: List<SmsPattern>): SmsPattern? {
        val normalizedMessage = BankSmsParser.normalizeText(message)
        if (normalizedMessage.isBlank()) return null

        val candidates = patterns.mapNotNull { pattern ->
            val sample = BankSmsParser.normalizeText(pattern.sampleMessage)
            if (sample.isBlank()) return@mapNotNull null

            val literalLength = sample.length - variableNumber.findAll(sample).sumOf { it.value.length }
            // A sample made almost entirely of numbers is not a safe identifying template.
            if (literalLength < 4) return@mapNotNull null

            val regex = buildTemplateRegex(sample)
            if (regex.matches(normalizedMessage)) {
                Candidate(pattern, literalLength)
            } else {
                null
            }
        }

        if (candidates.isEmpty()) return null
        val bestSpecificity = candidates.maxOf { it.specificity }
        val bestCandidates = candidates.filter { it.specificity == bestSpecificity }

        // Never guess which bank/type the user intended when equally specific samples collide.
        return bestCandidates.singleOrNull()?.pattern
    }

    private fun buildTemplateRegex(sample: String): Regex {
        val expression = buildString {
            var cursor = 0
            variableNumber.findAll(sample).forEach { match ->
                append(Regex.escape(sample.substring(cursor, match.range.first)))
                append("""[*#]*[0-9][0-9*#.,٬،٫/-]{0,$MAX_VARIABLE_TOKEN_LENGTH}""")
                cursor = match.range.last + 1
            }
            append(Regex.escape(sample.substring(cursor)))
        }

        return Regex("^$expression$", RegexOption.IGNORE_CASE)
    }
}
