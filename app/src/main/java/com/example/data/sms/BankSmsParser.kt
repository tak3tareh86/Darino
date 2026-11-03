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

    fun normalizeText(input: String): String =
        IranianPhoneUtils.convertDigitsToEnglish(input)
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('\u00A0', ' ')
            .trim()

    /** Any SMS containing the Persian password keyword is excluded from bank recognition. */
    fun containsPasswordKeyword(message: String): Boolean =
        normalizeText(message).contains("رمز")

    fun parseMatchedPattern(
        smsId: String,
        sender: String,
        body: String,
        timestampMillis: Long,
        pattern: SmsPattern
    ): BankSmsSuggestion? {
        if (containsPasswordKeyword(body)) return null
        // Sender IDs entered by the user are exact identifiers. Case, spaces,
        // Persian characters and punctuation are all significant.
        if (pattern.senderAddress.isBlank() || pattern.senderAddress != sender) return null
        return try {
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
                    val raw = parseNonNegativeNumber(fields.first(SmsPatternSlotKind.BALANCE_RIAL))
                    raw?.takeIf { it % 10L == 0L }?.div(10L)
                }
                fields.first(SmsPatternSlotKind.BALANCE_TOMAN) != null ->
                    parseNonNegativeNumber(fields.first(SmsPatternSlotKind.BALANCE_TOMAN))
                else -> null
            }

            val defaultDateTime = PersianCalendarHelper.fromEpochMillis(timestampMillis)
            val dateParts = when {
                fields.first(SmsPatternSlotKind.DATE) != null ->
                    parseJalaliDate(fields.first(SmsPatternSlotKind.DATE)!!, defaultDateTime.year, dayFirst = false)
                fields.first(SmsPatternSlotKind.DATE_DAY_MONTH) != null ->
                    parseJalaliDate(fields.first(SmsPatternSlotKind.DATE_DAY_MONTH)!!, defaultDateTime.year, dayFirst = true)
                fields.first(SmsPatternSlotKind.DATE_MONTH_DAY) != null ->
                    parseJalaliDate(fields.first(SmsPatternSlotKind.DATE_MONTH_DAY)!!, defaultDateTime.year, dayFirst = false)
                fields.first(SmsPatternSlotKind.DATE_DAY_MONTH_YEAR) != null -> {
                    val p = IranianPhoneUtils.convertDigitsToEnglish(fields.first(SmsPatternSlotKind.DATE_DAY_MONTH_YEAR)!!).trim().trimEnd(',', '،', ';', ':', '.', ')', ']').split(Regex("[/.-]"))
                    if (p.size != 3) null else parseJalaliDate("${p[2]}/${p[1]}/${p[0]}", defaultDateTime.year, dayFirst = false)
                }
                fields.first(SmsPatternSlotKind.DATE_MONTH_DAY_YEAR) != null -> {
                    val p = IranianPhoneUtils.convertDigitsToEnglish(fields.first(SmsPatternSlotKind.DATE_MONTH_DAY_YEAR)!!).trim().trimEnd(',', '،', ';', ':', '.', ')', ']').split(Regex("[/.-]"))
                    if (p.size != 3) null else parseJalaliDate("${p[2]}/${p[0]}/${p[1]}", defaultDateTime.year, dayFirst = false)
                }
                fields.first(SmsPatternSlotKind.DATE_YEAR_MONTH_DAY) != null -> {
                    val p = IranianPhoneUtils.convertDigitsToEnglish(fields.first(SmsPatternSlotKind.DATE_YEAR_MONTH_DAY)!!).trim().trimEnd(',', '،', ';', ':', '.', ')', ']').split(Regex("[/.-]"))
                    if (p.size != 3) null else parseJalaliDate("${p[0]}/${p[1]}/${p[2]}", defaultDateTime.year, dayFirst = false)
                }
                else -> Triple(defaultDateTime.year, defaultDateTime.month, defaultDateTime.day)
            } ?: return null
            val timeValue = fields.first(SmsPatternSlotKind.TIME)
            val timeParts = if (timeValue != null) {
                parseClockTime(timeValue) ?: return null
            } else {
                defaultDateTime.hour to defaultDateTime.minute
            }

            val hasExplicitDateOrTime =
                fields.first(SmsPatternSlotKind.DATE) != null ||
                    fields.first(SmsPatternSlotKind.DATE_DAY_MONTH) != null ||
                    fields.first(SmsPatternSlotKind.DATE_MONTH_DAY) != null ||
                    fields.first(SmsPatternSlotKind.DATE_DAY_MONTH_YEAR) != null ||
                    fields.first(SmsPatternSlotKind.DATE_MONTH_DAY_YEAR) != null ||
                    fields.first(SmsPatternSlotKind.DATE_YEAR_MONTH_DAY) != null ||
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

            val configuredAccount = pattern.accountNumber?.takeIf { it.isNotBlank() }?.let { "حساب $it" }
            val sourceAccount = account(SmsPatternSlotKind.SOURCE_CARD, "کارت")
                ?: account(SmsPatternSlotKind.SOURCE_ACCOUNT, "حساب")
                ?: configuredAccount?.takeIf { configuredType == TransactionType.EXPENSE }
            val destinationAccount = account(SmsPatternSlotKind.DESTINATION_CARD, "کارت")
                ?: account(SmsPatternSlotKind.DESTINATION_ACCOUNT, "حساب")
                ?: configuredAccount?.takeIf { configuredType == TransactionType.INCOME }

            BankSmsSuggestion(
                id = smsId,
                patternId = pattern.id,
                bankName = pattern.bankName.ifBlank { pattern.name },
                amount = amountToman,
                formattedAmount = formattedAmount,
                isAmountValid = true,
                type = configuredType,
                isTypeUncertain = false,
                smsText = body,
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
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePositiveNumber(value: String?): Long? =
        parseNonNegativeNumber(value)?.takeIf { it > 0L }

    private fun parseNonNegativeNumber(value: String?): Long? {
        // Keep only decimal digits: this also accepts values such as ۱۰۰۰تومان,
        // (1,000), and amounts followed by a currency suffix without changing the
        // fixed surrounding template text. A zero balance is valid, but a zero
        // transaction amount is rejected by parsePositiveNumber.
        val digits = value?.let(IranianPhoneUtils::convertDigitsToEnglish)
            ?.filter { it in '0'..'9' }
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return digits.toLongOrNull()?.takeIf { it <= 1_000_000_000_000_000L }
    }

    private fun parseJalaliDate(
        value: String,
        defaultYear: Int,
        dayFirst: Boolean
    ): Triple<Int, Int, Int>? {
        val parts = IranianPhoneUtils.convertDigitsToEnglish(value).trim()
            .trimEnd(',', '،', ';', ':', '.', ')', ']').split(Regex("[/.-]"))
        val year: Int
        val month: Int
        val day: Int
        if (parts.size == 2) {
            year = defaultYear
            val first = parts[0].toIntOrNull() ?: return null
            val second = parts[1].toIntOrNull() ?: return null
            month = if (dayFirst) second else first
            day = if (dayFirst) first else second
        } else if (parts.size == 3) {
            val yearText = parts[0]
            val yearValue = yearText.toIntOrNull() ?: return null
            year = if (yearText.length == 2) (resolveTwoDigitJalaliYear(yearValue, defaultYear) ?: return null) else yearValue
            month = parts[1].toIntOrNull() ?: return null
            day = parts[2].toIntOrNull() ?: return null
        } else {
            return null
        }
        if (year !in 1300..1599 || month !in 1..12 || day !in 1..PersianCalendarHelper.getDaysInMonth(year, month)) return null
        return Triple(year, month, day)
    }

    /**
     * Resolve YY against the Persian year of SMS receipt: suffix 05 maps to
     * 1405 during year 1405; suffix 06 maps to 1406 after the new year begins.
     */
    private fun resolveTwoDigitJalaliYear(twoDigitYear: Int, referenceYear: Int): Int? {
        if (twoDigitYear !in 0..99) return null
        val century = (referenceYear / 100) * 100
        var candidate = century + twoDigitYear
        // A bank SMS may arrive shortly after Nowruz with a transaction date in
        // the previous Jalali year, but it must never resolve to a future year.
        if (candidate > referenceYear) candidate -= 100
        return candidate.takeIf { it in (referenceYear - 1)..referenceYear }
    }

    private fun parseClockTime(value: String): Pair<Int, Int>? {
        val match = Regex("""^([0-2]?[0-9]):([0-5][0-9])(?::[0-5][0-9])?$""")
            .matchEntire(IranianPhoneUtils.convertDigitsToEnglish(value).trim().trimEnd(',', '،', ';', '.', ')', ']')) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (hour !in 0..23) return null
        return hour to minute
    }
}