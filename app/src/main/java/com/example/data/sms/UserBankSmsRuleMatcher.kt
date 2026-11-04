package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.util.IranianPhoneUtils

data class UserBankSmsRuleMatch(val rule: UserBankSmsRule, val type: TransactionType?)

object UserBankSmsRuleMatcher {
    private fun digits(value: String): String = IranianPhoneUtils.convertDigitsToEnglish(value)
        .filter { it in '0'..'9' }

    fun isNumericSender(value: String): Boolean {
        val compact = compactSender(value)
        val digits = compact.removePrefix("+")
        return digits.isNotEmpty() && digits.all { it in '0'..'9' }
    }

    private fun compactSender(value: String): String = buildString {
        IranianPhoneUtils.convertDigitsToEnglish(value).forEach { ch ->
            when {
                ch.isWhitespace() || Character.getType(ch) == Character.FORMAT.toInt() -> Unit
                else -> append(ch)
            }
        }
    }

    private fun normalizeNumericSender(value: String): String? {
        val compact = compactSender(value)
        val digits = compact.removePrefix("+")
        if (digits.isEmpty() || !digits.all { it in '0'..'9' }) return null
        return IranianPhoneUtils.normalizeIranianPhoneNumber(digits) ?: digits
    }

    private fun normalizeSenderText(value: String): String = value.trim()
        .filterNot { Character.getType(it) == Character.FORMAT.toInt() }
        .replace('ي', 'ی')
        .replace('ى', 'ی')
        .replace('ك', 'ک')

    fun senderMatches(configured: String, actual: String): Boolean {
        if (configured.isBlank() || actual.isBlank()) return false
        val configuredNumeric = isNumericSender(configured)
        val actualNumeric = isNumericSender(actual)
        if (configuredNumeric != actualNumeric) return false
        return if (configuredNumeric) {
            normalizeNumericSender(configured) == normalizeNumericSender(actual)
        } else {
            normalizeSenderText(configured) == normalizeSenderText(actual)
        }
    }

    fun parseMarkers(raw: String): List<String>? {
        if (raw.isBlank()) return null
        val values = raw.split('/')
        if (values.size < 2 || values.any { it.isBlank() }) return null
        val normalized = values.map { normalizeMarker(it) }
        if (normalized.any { it.isEmpty() } || normalized.distinct().size < 2) return null
        if (values.any { token -> token.any { !(it.isLetterOrDigit() || it == '+' || it == '_' || it == '-') } }) return null
        return normalized.distinct()
    }

    private fun normalizeMarker(value: String): String = normalizeBody(value).replace('-', '_')

    private fun normalizeBody(value: String): String = IranianPhoneUtils.convertDigitsToEnglish(value)
        .replace('ي', 'ی').replace('ك', 'ک').filterNot(Char::isWhitespace)

    fun matchAll(body: String, sender: String, rules: List<UserBankSmsRule>): List<UserBankSmsRuleMatch> {
        val normalizedBody = normalizeBody(body).replace('-', '_')
        val matching = rules.mapNotNull { rule ->
            val textSenders = if (rule.textSenderAddresses.isNotEmpty() || rule.numericSenderAddresses.isNotEmpty()) {
                rule.textSenderAddresses
            } else {
                rule.senderAddresses.filterNot(::isNumericSender)
            }
            val numericSenders = if (rule.textSenderAddresses.isNotEmpty() || rule.numericSenderAddresses.isNotEmpty()) {
                rule.numericSenderAddresses
            } else {
                rule.senderAddresses.filter(::isNumericSender)
            }
            val senderMatched = when {
                isNumericSender(sender) -> numericSenders.any { senderMatches(it, sender) }
                else -> textSenders.any { senderMatches(it, sender) }
            }
            if (!senderMatched) return@mapNotNull null
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
