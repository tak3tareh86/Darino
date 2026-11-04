package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.util.IranianPhoneUtils

data class UserBankSmsRuleMatch(val rule: UserBankSmsRule, val type: TransactionType?)

object UserBankSmsRuleMatcher {
    private fun digits(value: String): String = IranianPhoneUtils.convertDigitsToEnglish(value)
        .filter { it in '0'..'9' }

    fun senderMatches(configured: String, actual: String): Boolean {
        val expected = configured.trim()
        val received = actual.trim()
        if (expected.isEmpty() || received.isEmpty()) return false
        fun numericDigits(value: String): String? {
            val withoutLeadingPlus = value.removePrefix("+")
            val isNumeric = withoutLeadingPlus.isNotEmpty() &&
                withoutLeadingPlus.all {
                    it.isWhitespace() || it in '0'..'9' || it in '۰'..'۹' || it in '٠'..'٩'
                }
            return if (isNumeric) digits(withoutLeadingPlus) else null
        }
        val expectedDigits = numericDigits(expected)
        val receivedDigits = numericDigits(received)
        return if (expectedDigits != null && receivedDigits != null) expectedDigits == receivedDigits
        else normalizeSenderText(expected) == normalizeSenderText(received)
    }

    private fun normalizeSenderText(value: String): String = value
        .replace('ي', 'ی')
        .replace('ى', 'ی')
        .replace('ك', 'ک')

    fun parseMarkers(raw: String): List<String>? {
        if (raw.isBlank()) return null
        val values = raw.split('/')
        if (values.size < 2 || values.any { it.isBlank() }) return null
        val normalized = values.map { normalizeMarker(it) }
        if (normalized.any { it.isEmpty() } || normalized.distinct().size < 2) return null
        if (values.any { token -> token.filterNot(Char::isWhitespace).any { !(it.isLetterOrDigit() || it == '+' || it == '_' || it == '-') } }) return null
        return normalized.distinct()
    }

    private fun normalizeMarker(value: String): String = normalizeBody(value).replace('-', '_')

    private fun normalizeBody(value: String): String = IranianPhoneUtils.convertDigitsToEnglish(value)
        .replace('ي', 'ی').replace('ك', 'ک').filterNot(Char::isWhitespace)

    fun matchAll(body: String, sender: String, rules: List<UserBankSmsRule>): List<UserBankSmsRuleMatch> {
        val normalizedBody = normalizeBody(body).replace('-', '_')
        val matching = rules.mapNotNull { rule ->
            if (rule.senderAddresses.none { senderMatches(it, sender) }) return@mapNotNull null
            val categories = listOf(
                TransactionType.INCOME to parseMarkers(rule.incomeMarkers),
                TransactionType.EXPENSE to parseMarkers(rule.expenseMarkers),
                TransactionType.TRANSFER to parseMarkers(rule.transferMarkers)
            )
            val matchedTypes = categories.filter { (_, markers) ->
                markers != null && markers.all { marker -> normalizedBody.contains(marker) }
            }.map { it.first }
            if (matchedTypes.isEmpty()) null else rule to matchedTypes
        }
        return matching.map { (rule, types) -> UserBankSmsRuleMatch(rule, types.singleOrNull()) }
    }

    fun match(body: String, sender: String, rules: List<UserBankSmsRule>): UserBankSmsRuleMatch? {
        return matchAll(body, sender, rules).singleOrNull()?.takeIf { it.type != null }
    }

    fun applyMatch(parsed: BankSmsSuggestion, result: UserBankSmsRuleMatch): BankSmsSuggestion = parsed.copy(
        bankName = result.rule.bankName,
        patternId = "user-rule:" + result.rule.id,
        type = result.type,
        isTypeUncertain = result.type == null,
        category = when (result.type) {
            TransactionType.INCOME -> "واریز"
            TransactionType.EXPENSE -> "برداشت"
            TransactionType.TRANSFER -> "انتقال وجه"
            null -> "نیازمند بررسی"
        }
    )
}
