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
        type: TransactionType
    ): BankSmsSuggestion? {
        if (BankSmsParser.containsPasswordKeyword(body)) return null
        val normalized = IranianPhoneUtils.convertDigitsToEnglish(body)
        if (Regex("""رمز\s*(?:پویا|یکبار|یک\s*بار)|رمز\s*عبور|کد\s*تأیید|کد\s*تاييد|otp|ناموفق|لغو\s*شد|انجام\s*نشد|تراکنش\s*رد\s*شد|failed|declined|cancelled""", RegexOption.IGNORE_CASE).containsMatchIn(normalized)) return null

        val number = """([0-9][0-9,،٬٫.]*)"""
        val labeled = Regex("""(?:مبلغ(?:\s*تراکنش)?|مبـلغ|برداشت(?:\s*وجه)?|واریز(?:\s*وجه)?|واريز|دریافت(?:\s*وجه)?|دريافت|خرید|خريد|پرداخت|انتقال(?:\s*وجه)?|وجه|deposit|withdraw(?:al)?|debit|credit|purchase|transfer)\s*[:：]?\s*$number""", RegexOption.IGNORE_CASE).findAll(normalized).toList()
        val amountMatch = labeled.singleOrNull() ?: if (labeled.isEmpty()) {
            val currency = Regex("""$number\s*(?:ریال|ريال|تومان|تومن|rial|rials|toman)""", RegexOption.IGNORE_CASE).findAll(normalized).toList()
            currency.singleOrNull() ?: Regex("""[+-]\s*$number""").findAll(normalized).toList().singleOrNull()
        } else null
        val rawAmount = amountMatch?.groupValues?.lastOrNull() ?: return null
        val amount = rawAmount.filter(Char::isDigit).toLongOrNull()?.takeIf { it in 1L..100_000_000_000_000L } ?: return null
        val context = amountMatch.value + normalized.substring(amountMatch.range.last + 1, minOf(normalized.length, amountMatch.range.last + 18))
        val isRial = Regex("""ریال|ريال|rial|rials""", RegexOption.IGNORE_CASE).containsMatchIn(context)
        val toman = if (isRial) {
            if (amount < 10L || amount % 10L != 0L) return null
            amount / 10L
        } else amount
        if (toman !in 1L..100_000_000_000L) return null

        val dateTime = PersianCalendarHelper.fromEpochMillis(receivedAtMillis)
        val time = Regex("""(?<![0-9])([0-2]?[0-9]:[0-5][0-9])""").find(normalized)?.groupValues?.get(1)
            ?: dateTime.toFormattedTime()
        val date = String.format("%04d/%02d/%02d", dateTime.year, dateTime.month, dateTime.day)
        val formatted = when (type) {
            TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(toman, isExpense = true)
            TransactionType.INCOME -> MoneyFormatter.formatSignedToman(toman, isExpense = false)
            TransactionType.TRANSFER -> MoneyFormatter.formatToman(toman)
        }
        return BankSmsSuggestion(
            id = smsId,
            patternId = "user-rule:" + rule.id,
            bankName = rule.bankName,
            amount = toman,
            formattedAmount = formatted,
            isAmountValid = true,
            type = type,
            isTypeUncertain = false,
            smsText = body,
            dateText = date,
            timeText = time,
            category = when (type) {
                TransactionType.INCOME -> "واریز"
                TransactionType.EXPENSE -> "برداشت"
                TransactionType.TRANSFER -> "انتقال وجه"
            },
            rawSender = sender,
            timestampMillis = receivedAtMillis,
            receivedAtMillis = receivedAtMillis
        )
    }
}
