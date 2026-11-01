package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper

/**
 * The parser has no generic bank, transaction-type, amount, or fallback recognition.
 * It can only parse an SMS after an exact user-template match.
 */
object BankSmsParser {

    fun normalizeText(input: String): String {
        return IranianPhoneUtils.convertDigitsToEnglish(input)
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('−', '-')
            .replace('﹣', '-')
            .replace('－', '-')
            .replace('\u200C', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun parseMatchedPattern(
        smsId: String,
        sender: String,
        body: String,
        timestampMillis: Long,
        pattern: SmsPattern
    ): BankSmsSuggestion? {
        val fields = SmsPatternMatcher.extractMatchedFields(body, pattern) ?: return null

        val amountKind = when {
            fields.first(SmsPatternSlotKind.AMOUNT_RIAL) != null -> SmsPatternSlotKind.AMOUNT_RIAL
            fields.first(SmsPatternSlotKind.AMOUNT_TOMAN) != null -> SmsPatternSlotKind.AMOUNT_TOMAN
            else -> return null
        }
        val rawAmount = parsePositiveNumber(fields.first(amountKind)) ?: return null
        val amountToman = when (amountKind) {
            SmsPatternSlotKind.AMOUNT_RIAL -> {
                if (rawAmount < 10L || rawAmount % 10L != 0L) return null
                rawAmount / 10L
            }
            else -> rawAmount
        }.takeIf { it in 1L..100_000_000_000L } ?: return null

        val configuredType = when (pattern.type) {
            SmsPatternType.INCOME -> TransactionType.INCOME
            SmsPatternType.EXPENSE -> TransactionType.EXPENSE
            SmsPatternType.TRANSFER -> TransactionType.TRANSFER
        }

        val balanceAmount = when {
            fields.first(SmsPatternSlotKind.BALANCE_RIAL) != null -> {
                val raw = parsePositiveNumber(fields.first(SmsPatternSlotKind.BALANCE_RIAL))
                raw?.takeIf { it >= 10L && it % 10L == 0L }?.div(10L)
            }
            fields.first(SmsPatternSlotKind.BALANCE_TOMAN) != null ->
                parsePositiveNumber(fields.first(SmsPatternSlotKind.BALANCE_TOMAN))
            else -> null
        }

        val defaultDateTime = PersianCalendarHelper.fromEpochMillis(timestampMillis)
        val dateParts = fields.first(SmsPatternSlotKind.DATE)?.let(::parseJalaliDate)
            ?: Triple(defaultDateTime.year, defaultDateTime.month, defaultDateTime.day)
        val timeParts = fields.first(SmsPatternSlotKind.TIME)?.let(::parseClockTime)
            ?: (defaultDateTime.hour to defaultDateTime.minute)

        val hasExplicitDateOrTime =
            fields.first(SmsPatternSlotKind.DATE) != null ||
                fields.first(SmsPatternSlotKind.TIME) != null
        val finalTimestamp = if (hasExplicitDateOrTime) {
            runCatching {
                PersianCalendarHelper.jalaliToEpochMillis(
                    dateParts.first, dateParts.second, dateParts.third,
                    timeParts.first, timeParts.second
                )
            }.getOrNull() ?: return null
        } else timestampMillis

        val formattedAmount = when (configuredType) {
            TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(amountToman, isExpense = true)
            TransactionType.INCOME -> MoneyFormatter.formatSignedToman(amountToman, isExpense = false)
            TransactionType.TRANSFER -> MoneyFormatter.formatToman(amountToman)
        }

        fun account(kind: SmsPatternSlotKind, label: String): String? =
            fields.first(kind)?.replace(Regex("""\s+"""), "")?.takeIf { it.isNotBlank() }
                ?.let { "$label $it" }

        val sourceAccount = account(SmsPatternSlotKind.SOURCE_CARD, "کارت")
            ?: account(SmsPatternSlotKind.SOURCE_ACCOUNT, "حساب")
        val destinationAccount = account(SmsPatternSlotKind.DESTINATION_CARD, "کارت")
            ?: account(SmsPatternSlotKind.DESTINATION_ACCOUNT, "حساب")

        return BankSmsSuggestion(
            id = smsId,
            bankName = pattern.name,
            amount = amountToman,
            formattedAmount = formattedAmount,
            isAmountValid = true,
            type = configuredType,
            isTypeUncertain = false,
            smsText = body.trim(),
            dateText = String.format("%04d/%02d/%02d", dateParts.first, dateParts.second, dateParts.third),
            timeText = String.format("%02d:%02d", timeParts.first, timeParts.second),
            category = when (configuredType) {
                TransactionType.INCOME -> "واریز"
                TransactionType.EXPENSE -> "برداشت"
                TransactionType.TRANSFER -> "انتقال وجه"
            },
            sourceAccount = sourceAccount,
            destinationAccount = destinationAccount,
            rawSender = sender,
            parseError = null,
            timestampMillis = finalTimestamp,
            balanceAmount = balanceAmount,
            trackingCode = fields.first(SmsPatternSlotKind.TRACKING)
                ?.replace(Regex("""\s+"""), "")
        )
    }

    private fun parsePositiveNumber(value: String?): Long? {
        val digits = value?.trim()
            ?.removePrefix("+")
            ?.removePrefix("-")
            ?.replace(",", "")
            ?.replace("،", "")
            ?.replace("٬", "")
            ?.replace("٫", "")
            ?.replace(".", "")
            ?.replace(" ", "")
            ?: return null
        return digits.toLongOrNull()?.takeIf { it > 0L && it <= 1_000_000_000_000_000L }
    }

    private fun parseJalaliDate(value: String): Triple<Int, Int, Int>? {
        val match = Regex("""^([0-9]{2,4})[/.-]([0-9]{1,2})[/.-]([0-9]{1,2})$""")
            .matchEntire(value.trim()) ?: return null
        val yearText = match.groupValues[1]
        val yearValue = yearText.toIntOrNull() ?: return null
        val year = if (yearText.length == 2) 1400 + yearValue else yearValue
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val day = match.groupValues[3].toIntOrNull() ?: return null
        if (year !in 1300..1499 || month !in 1..12 || day !in 1..31) return null
        return Triple(year, month, day)
    }

    private fun parseClockTime(value: String): Pair<Int, Int>? {
        val match = Regex("""^([0-2]?[0-9]):([0-5][0-9])(?::[0-5][0-9])?$""")
            .matchEntire(value.trim()) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (hour !in 0..23) return null
        return hour to minute
    }
}