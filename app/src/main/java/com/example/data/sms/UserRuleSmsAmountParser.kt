package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper

/**
 * Conservative amount extraction for SMS messages already matched by an explicit user rule.
 *
 * Candidates are scored by their semantic context. Balance, available-credit, date, time,
 * account and reference-number contexts are excluded. Ambiguous candidates fail closed.
 */
object UserRuleSmsAmountParser {
    private data class AmountCandidate(
        val amountValue: Long,
        val score: Int,
        val start: Int,
        val end: Int
    )

    private val numberRegex = Regex("""(?<![\p{L}\d])([+-]?\d[\d,،٬٫.\u00A0 ]*)(?![\p{L}\d])""")
    private val rialRegex = Regex("""(?:ریال|ريال|rial|rials)""", RegexOption.IGNORE_CASE)
    private val tomanRegex = Regex("""(?:تومان|تومن|toman|tomans)""", RegexOption.IGNORE_CASE)
    private val amountLabelRegex = Regex(
        """(?:مبلغ(?:\s*تراکنش)?|مبـلغ|مبلغ\s*واریزی|مبلغ\s*برداشت|amount|transaction\s*amount)""",
        RegexOption.IGNORE_CASE
    )
    private val transactionContextRegex = Regex(
        """(?:واریز|واريز|واریزی|برداشت|خرید|خريد|پرداخت|انتقال|دریافت|دريافت|نشست|واریز\s*شد|برداشت\s*شد|deposit|withdraw(?:al)?|debit|credit|purchase|transfer|paid)""",
        RegexOption.IGNORE_CASE
    )
    private val balanceContextRegex = Regex(
        """(?:موجودی|مانده|مانده\s*حساب|موجودی\s*حساب|قابل\s*برداشت|اعتبار\s*باقی|available\s*balance|balance|remaining|رصید)""",
        RegexOption.IGNORE_CASE
    )
    private val irrelevantContextRegex = Regex(
        """(?:شماره\s*کارت|کارت\s*شماره|حساب|شبا|پیگیری|پیامک|مرجع|شناسه|کد|تاریخ|ساعت|شماره|account|card|reference|tracking|terminal|date|time|otp)""",
        RegexOption.IGNORE_CASE
    )

    private fun normalizeBody(value: String): String = IranianPhoneUtils.convertDigitsToEnglish(value)
        .replace('ي', 'ی')
        .replace('ى', 'ی')
        .replace('ك', 'ک')
        .replace('ؤ', 'و')
        .replace('ۀ', 'ه')
        .replace('ة', 'ه')
        .replace('٫', '.')
        .replace('٬', ',')
        .replace('،', ',')

    private fun extractAmount(body: String): Long? {
        val text = normalizeBody(body)
        val candidates = numberRegex.findAll(text).mapNotNull { match ->
            val raw = match.groupValues[1].trim().trimEnd(',', '.', '،')
            val digits = raw.filter { it in '0'..'9' }
            val amount = digits.toLongOrNull()?.takeIf { it in 1L..100_000_000_000_000L }
                ?: return@mapNotNull null

            val left = (match.range.first - 38).coerceAtLeast(0)
            val right = (match.range.last + 39).coerceAtMost(text.length)
            val context = text.substring(left, right)
            val nearLeft = text.substring((match.range.first - 18).coerceAtLeast(0), match.range.first)
            val nearRight = text.substring(match.range.last + 1, (match.range.last + 19).coerceAtMost(text.length))
            val before = text.substring(left, match.range.first)
            val after = text.substring(match.range.last + 1, right)
            val hasAmountLabel = amountLabelRegex.containsMatchIn(before.takeLast(32))
            val hasTransactionContext = transactionContextRegex.containsMatchIn(context)
            if (balanceContextRegex.containsMatchIn(nearLeft) ||
                (balanceContextRegex.containsMatchIn(nearRight) && !hasAmountLabel && !hasTransactionContext)
            ) return@mapNotNull null
            val hasCurrency = rialRegex.containsMatchIn(after.take(16)) ||
                tomanRegex.containsMatchIn(after.take(16)) ||
                rialRegex.containsMatchIn(before.takeLast(16)) ||
                tomanRegex.containsMatchIn(before.takeLast(16))
            val hasIrrelevantContext = irrelevantContextRegex.containsMatchIn(before.takeLast(20))

            // A number next to a balance or account identifier is never promoted to a transaction amount.
            if (hasIrrelevantContext && !hasAmountLabel && !hasTransactionContext) return@mapNotNull null

            val score = when {
                hasAmountLabel -> 100
                hasTransactionContext && hasCurrency -> 90
                hasTransactionContext -> 75
                hasCurrency -> 55
                else -> 0
            }
            if (score == 0) return@mapNotNull null

            val unitText = (before.takeLast(16) + " " + after.take(16))
            val isRial = rialRegex.containsMatchIn(unitText)
            val isToman = tomanRegex.containsMatchIn(unitText)
            if (isRial && isToman) return@mapNotNull null
            // Keep the exact digits from the message, irrespective of Rial/Toman suffix.
            amount.takeIf { it in 1L..100_000_000_000_000L }?.let {
                AmountCandidate(it, score, match.range.first, match.range.last + 1)
            }
        }.toList()

        if (candidates.isEmpty()) return null
        val highestScore = candidates.maxOf { it.score }
        val best = candidates.filter { it.score == highestScore }
        // Multiple equally plausible amounts must not silently create a wrong transaction.
        return best.singleOrNull()?.amountValue
    }

    fun parse(
        smsId: String,
        sender: String,
        body: String,
        receivedAtMillis: Long,
        rule: UserBankSmsRule,
        type: TransactionType?
    ): BankSmsSuggestion? {
        if (BankSmsParser.containsPasswordKeyword(body)) return null
        val normalized = normalizeBody(body)
        if (Regex(
                """رمز\s*(?:پویا|یکبار|یک\s*بار)|رمز\s*عبور|کد\s*تأیید|کد\s*تاييد|otp|ناموفق|لغو\s*شد|انجام\s*نشد|تراکنش\s*رد\s*شد|failed|declined|cancelled""",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(normalized)
        ) return null

        val amount = extractAmount(normalized)
        val amountIsValid = amount != null
        val amountValue = amount ?: 0L
        val dateTime = PersianCalendarHelper.fromEpochMillis(receivedAtMillis)
        val time = Regex("""(?<![0-9])([0-2]?[0-9]:[0-5][0-9])""")
            .find(normalized)?.groupValues?.get(1)
            ?: String.format("%02d:%02d", dateTime.hour, dateTime.minute)
        val date = String.format("%04d/%02d/%02d", dateTime.year, dateTime.month, dateTime.day)
        val formatted = if (!amountIsValid) "" else when (type) {
            TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(amountValue, isExpense = true, currency = com.example.ui.screens.settings.model.AppCurrency.TOMAN)
            TransactionType.INCOME -> MoneyFormatter.formatSignedToman(amountValue, isExpense = false, currency = com.example.ui.screens.settings.model.AppCurrency.TOMAN)
            TransactionType.TRANSFER, null -> MoneyFormatter.formatToman(amountValue, currency = com.example.ui.screens.settings.model.AppCurrency.TOMAN)
        }

        return BankSmsSuggestion(
            id = smsId,
            patternId = "user-rule:" + rule.id,
            bankName = rule.bankName,
            amount = amountValue,
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
