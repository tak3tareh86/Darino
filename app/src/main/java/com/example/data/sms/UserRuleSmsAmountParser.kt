package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper

/**
 * Amount-only extraction for messages already authorized by an explicit user rule.
 * Transaction type and bank identity are supplied by that rule; no generic classification is used.
 */
object UserRuleSmsAmountParser {
    fun parse(
        smsId: String,
        sender: String,
        body: String,
        receivedAtMillis: Long,
        rule: UserBankSmsRule,
        type: TransactionType?
    ): BankSmsSuggestion? {
        if (BankSmsParser.containsPasswordKeyword(body)) return null
        val normalized = IranianPhoneUtils.convertDigitsToEnglish(body)
        if (Regex("""رمز\s*(?:پویا|یکبار|یک\s*بار)|رمز\s*عبور|کد\s*تأیید|کد\s*تاييد|otp|ناموفق|لغو\s*شد|انجام\s*نشد|تراکنش\s*رد\s*شد|failed|declined|cancelled""", RegexOption.IGNORE_CASE).containsMatchIn(normalized)) return null

        val number = """([0-9][0-9,،٬٫.]*)"""
        // Some banks describe a deposit as "30,000,000 ریال به حساب شما نشست"
        // and then list the balance separately. Prefer the amount tied to that
        // explicit transaction phrase instead of treating the balance as a rival.
        val depositNarrative = Regex(
            """$number\s*(?:ریال|ريال)\s*به\s*حساب\s*شما\s*نشست""",
            RegexOption.IGNORE_CASE
        ).findAll(normalized).toList()
        val labeled = Regex("""(?:مبلغ(?:\s*تراکنش)?|مبـلغ|برداشت(?:\s*وجه)?|واریز(?:\s*وجه)?|واريز|دریافت(?:\s*وجه)?|دريافت|خرید|خريد|پرداخت|انتقال(?:\s*وجه)?|وجه|deposit|withdraw(?:al)?|debit|credit|purchase|transfer)\s*[:：]?\s*$number""", RegexOption.IGNORE_CASE).findAll(normalized).toList()
        val amountMatch = when {
            depositNarrative.size == 1 -> depositNarrative.single()
            depositNarrative.isNotEmpty() -> null
            labeled.size == 1 -> labeled.single()
            labeled.isNotEmpty() -> null
            else -> {
                val currencySuffix = Regex("""$number\s*(?:ریال|ريال|تومان|تومن|rial|rials|toman)""", RegexOption.IGNORE_CASE).findAll(normalized).toList()
                val currencyPrefix = Regex("""(?:ریال|ريال|تومان|تومن|rial|rials|toman)\s*[:：]?\s*$number""", RegexOption.IGNORE_CASE).findAll(normalized).toList()
                val currencyMatches = currencySuffix + currencyPrefix
                currencyMatches.singleOrNull() ?: Regex("""[+-]\s*$number""").findAll(normalized).toList().singleOrNull()
            }
        }
        val parsedAmount: Long? = amountMatch?.let { matched ->
            val rawAmount = matched.groupValues.lastOrNull() ?: return@let null
            val amount = rawAmount.filter(Char::isDigit).toLongOrNull()
                ?.takeIf { it in 1L..100_000_000_000_000L } ?: return@let null
            val numericRange = matched.groups.lastOrNull()?.range ?: return@let null
            val unitRegex = Regex("""ریال|ريال|تومان|تومن|rial|rials|toman""", RegexOption.IGNORE_CASE)
            val prefixStart = (numericRange.first - 12).coerceAtLeast(0)
            val suffixEnd = (numericRange.last + 13).coerceAtMost(normalized.length)
            val prefixText = normalized.substring(prefixStart, numericRange.first)
            val suffixText = normalized.substring(numericRange.last + 1, suffixEnd)
            val prefixUnit = unitRegex.findAll(prefixText).lastOrNull()
            val suffixUnit = unitRegex.find(suffixText)
            val isRial = when {
                prefixUnit != null && suffixUnit == null -> Regex("""ریال|ريال|rial|rials""", RegexOption.IGNORE_CASE).matches(prefixUnit.value)
                suffixUnit != null && prefixUnit == null -> Regex("""ریال|ريال|rial|rials""", RegexOption.IGNORE_CASE).matches(suffixUnit.value)
                prefixUnit != null && suffixUnit != null -> return@let null
                else -> false
            }
            val toman = if (isRial) {
                if (amount < 10L || amount % 10L != 0L) return@let null
                amount / 10L
            } else amount
            toman.takeIf { it in 1L..100_000_000_000L }
        }
        val amountIsValid = parsedAmount != null
        val toman = parsedAmount ?: 0L

        val dateTime = PersianCalendarHelper.fromEpochMillis(receivedAtMillis)
        val time = Regex("""(?<![0-9])([0-2]?[0-9]:[0-5][0-9])""").find(normalized)?.groupValues?.get(1)
            ?: String.format("%02d:%02d", dateTime.hour, dateTime.minute)
        val date = String.format("%04d/%02d/%02d", dateTime.year, dateTime.month, dateTime.day)
        val formatted = if (!amountIsValid) "" else when (type) {
            TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(toman, isExpense = true)
            TransactionType.INCOME -> MoneyFormatter.formatSignedToman(toman, isExpense = false)
            TransactionType.TRANSFER, null -> MoneyFormatter.formatToman(toman)
        }
        return BankSmsSuggestion(
            id = smsId,
            patternId = "user-rule:" + rule.id,
            bankName = rule.bankName,
            amount = toman,
            formattedAmount = formatted,
            isAmountValid = amountIsValid,
            type = type,
            isTypeUncertain = type == null,
            smsText = body,
            dateText = date,
            timeText = time,
            category = when (type) {
                TransactionType.INCOME -> "واریز"
                TransactionType.EXPENSE -> "برداشت"
                TransactionType.TRANSFER -> "انتقال وجه"
                null -> "نیازمند بررسی"
            },
            rawSender = sender,
            timestampMillis = receivedAtMillis,
            receivedAtMillis = receivedAtMillis
        )
    }
}
