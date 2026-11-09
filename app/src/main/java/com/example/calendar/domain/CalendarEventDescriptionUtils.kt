package com.example.calendar.domain

import com.example.util.IranianPhoneUtils

/**
 * Removes a duplicated event amount from its description without discarding
 * unrelated descriptive text.
 */
internal object CalendarEventDescriptionUtils {
    private val amountToken = Regex("[0-9][0-9,٬،.]*")
    private val amountLabelAtEnd = Regex("(?:مبلغ\\s*)?(?:چک|قسط)\\s*[:：]?\\s*$|مبلغ\\s*[:：]?\\s*$")
    private val currencyAtStart = Regex("^\\s*(?:تومان|ریال)")

    fun removeDuplicateAmount(description: String, amount: Long?): String {
        if (description.isBlank() || amount == null || amount <= 0L) return description

        val normalized = IranianPhoneUtils.convertDigitsToEnglish(description)
        val matchingToken = amountToken.findAll(normalized).firstOrNull { match ->
            match.value.filter(Char::isDigit).toLongOrNull() == amount
        } ?: return description

        val tokenStart = matchingToken.range.first
        val tokenEnd = matchingToken.range.last + 1
        val before = description.substring(0, tokenStart)
        val after = description.substring(tokenEnd)

        // Require monetary context immediately beside the number. This avoids
        // mistaking an unrelated date, installment index, or reference number
        // elsewhere in the description for the displayed amount.
        val hasAmountLabel = amountLabelAtEnd.containsMatchIn(before)
        val hasCurrencyUnit = currencyAtStart.containsMatchIn(after)
        if (!hasAmountLabel && !hasCurrencyUnit) return description

        val prefixMatch = amountLabelAtEnd.find(before)
        val suffixMatch = currencyAtStart.find(after)
        val cleanStart = prefixMatch?.range?.first ?: tokenStart
        val cleanEnd = if (suffixMatch != null) {
            tokenEnd + suffixMatch.range.last + 1
        } else {
            tokenEnd
        }

        return (description.substring(0, cleanStart) + description.substring(cleanEnd))
            .replace(Regex("[ \\t]{2,}"), " ")
            .trim()
            .trim(',', '،', ':', '：', '-', '•')
            .trim()
    }
}
