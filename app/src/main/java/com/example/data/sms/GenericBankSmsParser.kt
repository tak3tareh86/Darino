package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper

/**
 * Conservative fallback for bank SMS messages that do not have a user-defined template.
 * Sender IDs are not treated as a bank registry because shortcodes can change or be shared.
 */
object GenericBankSmsParser {
    private val bankAliases = linkedMapOf(
        "بانک ملت" to listOf("بانک ملت", "ملت بانک", "mellat bank", "mellat"),
        "بانک شهر" to listOf("بانک شهر", "shahr bank", "shahr"),
        "بانک ملی" to listOf("بانک ملی", "ملی ایران", "melli bank", "bank melli", "melli"),
        "بانک صادرات" to listOf("بانک صادرات", "صادرات ایران", "bank saderat", "saderat"),
        "بانک تجارت" to listOf("بانک تجارت", "tejarat bank", "tejarat"),
        "بانک سپه" to listOf("بانک سپه", "sepah bank", "sepah"),
        "بانک کشاورزی" to listOf("بانک کشاورزی", "keshavarzi"),
        "بانک مسکن" to listOf("بانک مسکن", "maskan bank", "maskan"),
        "بانک رفاه" to listOf("بانک رفاه", "رفاه کارگران", "refah bank", "refah"),
        "بانک پاسارگاد" to listOf("بانک پاسارگاد", "pasargad bank", "pasargad"),
        "بانک پارسیان" to listOf("بانک پارسیان", "parsian bank", "parsian"),
        "بانک سامان" to listOf("بانک سامان", "saman bank", "saman"),
        "بانک اقتصاد نوین" to listOf("اقتصاد نوین", "eghtesad novin"),
        "بانک کارآفرین" to listOf("بانک کارآفرین", "karafarin bank", "karafarin"),
        "بانک سینا" to listOf("بانک سینا", "sina bank", "sina"),
        "بانک دی" to listOf("بانک دی", "day bank"),
        "بانک سرمایه" to listOf("بانک سرمایه", "sarmayeh bank", "sarmayeh"),
        "بانک گردشگری" to listOf("بانک گردشگری", "gardeshgari bank", "gardeshgari"),
        "بانک ایران زمین" to listOf("بانک ایران زمین", "ایران زمین", "iran zamin"),
        "پست بانک ایران" to listOf("پست بانک", "post bank iran", "postbank"),
        "بانک خاورمیانه" to listOf("بانک خاورمیانه", "middle east bank", "khavarmianeh"),
        "بانک توسعه تعاون" to listOf("توسعه تعاون", "tose'e ta'avon", "tose taavon"),
        "بانک توسعه صادرات" to listOf("توسعه صادرات", "tose'e saderat"),
        "بانک صنعت و معدن" to listOf("صنعت و معدن", "sanat va madan"),
        "بانک قرض‌الحسنه مهر ایران" to listOf("مهر ایران", "mehr iran"),
        "بانک قرض‌الحسنه رسالت" to listOf("بانک رسالت", "قرض الحسنه رسالت", "resalat"),
        "مؤسسه اعتباری ملل" to listOf("موسسه اعتباری ملل", "مؤسسه اعتباری ملل", "اعتباری ملل", "melal"),
        "مؤسسه اعتباری نور" to listOf("موسسه اعتباری نور", "مؤسسه اعتباری نور", "اعتباری نور"),
        "مؤسسه اعتباری کوثر" to listOf("موسسه اعتباری کوثر", "مؤسسه اعتباری کوثر", "اعتباری کوثر")
    )

    private val transactionSignal = Regex(
        """واریز|واريز|برداشت|خرید|خريد|پرداخت|انتقال|تراکنش|کارت\s*به\s*کارت|پایا|ساتنا|حواله|deposit|withdraw|debit|credit|purchase|transfer""",
        RegexOption.IGNORE_CASE
    )
    private val amountLabel = Regex(
        """(?:مبلغ|مبـلغ|برداشت|واریز|واريز|خرید|خريد|پرداخت|انتقال\s*وجه|وجه\s*انتقالی|deposit|withdraw(?:al)?|debit|credit|purchase|transfer)\s*[:：]?\s*([0-9۰-۹٠-٩][0-9۰-۹٠-٩,،٬٫.]*)""",
        RegexOption.IGNORE_CASE
    )
    private val balanceLabel = Regex(
        """(?:مانده|موجودی|موجودى|balance)\s*[:：]?\s*([0-9۰-۹٠-٩][0-9۰-۹٠-٩,،٬٫.]*)""",
        RegexOption.IGNORE_CASE
    )
    private val timeRegex = Regex("""(?<![0-9۰-۹٠-٩])([0-2]?[0-9]:[0-5][0-9])(?::[0-5][0-9])?(?![0-9۰-۹٠-٩])""")
    private val dateRegex = Regex("""(?<![0-9۰-۹٠-٩])([0-9۰-۹٠-٩]{1,4})[/.-]([0-9۰-۹٠-٩]{1,2})[/.-]([0-9۰-۹٠-٩]{1,4})(?![0-9۰-۹٠-٩])""")

    fun parse(smsId: String, sender: String, body: String, timestampMillis: Long): BankSmsSuggestion? {
        val normalized = normalize(body)
        if (normalized.isBlank() || !transactionSignal.containsMatchIn(normalized)) return null

        // OTPs and failed/cancelled notices are not completed financial transactions.
        if (Regex("""رمز\s*(?:پویا|یکبار|یک\s*بار)|رمز\s*عبور|کد\s*تأیید|کد\s*تاييد|otp|one[- ]time password""", RegexOption.IGNORE_CASE)
                .containsMatchIn(normalized) ||
            Regex("""ناموفق|لغو\s*شد|انجام\s*نشد|تراکنش\s*رد\s*شد|failed|declined|cancelled""", RegexOption.IGNORE_CASE)
                .containsMatchIn(normalized)
        ) return null

        val amountMatch = amountLabel.findAll(normalized).singleOrNull() ?: return null
        val amount = parseDigits(amountMatch.groupValues[1]) ?: return null
        if (amount !in 1L..100_000_000_000_000L) return null

        val type = classifyType(normalized) ?: return null
        val amountUnitContext = normalized.substring(
            amountMatch.range.last + 1,
            minOf(normalized.length, amountMatch.range.last + 18)
        )
        val amountIsRial = Regex("""ریال|ريال|rial|rials""", RegexOption.IGNORE_CASE)
            .containsMatchIn(amountUnitContext)
        val amountToman = if (amountIsRial) {
            if (amount < 10L || amount % 10L != 0L) return null
            amount / 10L
        } else amount
        if (amountToman !in 1L..100_000_000_000L) return null

        val balanceMatch = balanceLabel.findAll(normalized).singleOrNull()
        val balanceRaw = balanceMatch?.let { parseDigits(it.groupValues[1]) }
        val balanceUnitContext = balanceMatch?.let {
            normalized.substring(it.range.last + 1, minOf(normalized.length, it.range.last + 18))
        }.orEmpty()
        val balanceIsRial = balanceRaw != null &&
            Regex("""ریال|ريال|rial|rials""", RegexOption.IGNORE_CASE).containsMatchIn(balanceUnitContext)
        val balance = balanceRaw?.let {
            if (balanceIsRial) it.takeIf { value -> value >= 10L && value % 10L == 0L }?.div(10L) else it
        }

        val bankName = identifyBank(normalized, sender)
        val receivedDateTime = PersianCalendarHelper.fromEpochMillis(timestampMillis)
        val dateParts = dateRegex.find(normalized)?.let { match ->
            parseJalaliDate(
                match.groupValues[1], match.groupValues[2], match.groupValues[3],
                receivedDateTime.year
            )
        } ?: Triple(receivedDateTime.year, receivedDateTime.month, receivedDateTime.day)
        val time = timeRegex.findAll(normalized).mapNotNull {
            val parts = IranianPhoneUtils.convertDigitsToEnglish(it.groupValues[1]).split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            if (hour in 0..23 && minute in 0..59) hour to minute else null
        }.firstOrNull() ?: (receivedDateTime.hour to receivedDateTime.minute)
        val hasExplicitDateOrTime = dateRegex.containsMatchIn(normalized) || timeRegex.containsMatchIn(normalized)
        val finalTimestamp = if (hasExplicitDateOrTime) {
            runCatching {
                PersianCalendarHelper.jalaliToEpochMillis(
                    dateParts.first, dateParts.second, dateParts.third, time.first, time.second
                )
            }.getOrNull() ?: timestampMillis
        } else timestampMillis

        val formatted = when (type) {
            TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(amountToman, isExpense = true)
            TransactionType.INCOME -> MoneyFormatter.formatSignedToman(amountToman, isExpense = false)
            TransactionType.TRANSFER -> MoneyFormatter.formatToman(amountToman)
        }
        val accountNumber = Regex("""(?:حساب|کارت)\s*[:：]?\s*([0-9۰-۹٠-٩*]{4,26})""")
            .find(normalized)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
        val tracking = Regex("""(?:پیگیری|رهگیری|شناسه)\s*[:：]?\s*([A-Za-z0-9۰-۹٠-٩-]{4,32})""", RegexOption.IGNORE_CASE)
            .find(normalized)?.groupValues?.get(1)

        return BankSmsSuggestion(
            id = smsId,
            bankName = bankName,
            amount = amountToman,
            formattedAmount = formatted,
            isAmountValid = true,
            type = type,
            isTypeUncertain = false,
            smsText = body,
            dateText = String.format("%04d/%02d/%02d", dateParts.first, dateParts.second, dateParts.third),
            timeText = String.format("%02d:%02d", time.first, time.second),
            category = when (type) {
                TransactionType.INCOME -> "واریز"
                TransactionType.EXPENSE -> "برداشت"
                TransactionType.TRANSFER -> "انتقال وجه"
            },
            sourceAccount = accountNumber?.let { "حساب/کارت $it" },
            rawSender = sender,
            timestampMillis = finalTimestamp,
            balanceAmount = balance,
            trackingCode = tracking
        )
    }

    /**
     * Iranian bank dates commonly use YY/MM/DD, DD/MM/YY, or DD/MM/YYYY.
     * Two-digit leading years are resolved against the receipt's Jalali year.
     */
    private fun parseJalaliDate(
        firstText: String,
        secondText: String,
        thirdText: String,
        referenceYear: Int
    ): Triple<Int, Int, Int>? {
        val first = IranianPhoneUtils.convertDigitsToEnglish(firstText).toIntOrNull() ?: return null
        val second = IranianPhoneUtils.convertDigitsToEnglish(secondText).toIntOrNull() ?: return null
        val third = IranianPhoneUtils.convertDigitsToEnglish(thirdText).toIntOrNull() ?: return null
        val year: Int
        val month: Int
        val day: Int
        when {
            firstText.length == 4 -> {
                year = first
                month = second
                day = third
            }
            thirdText.length == 4 -> {
                year = third
                month = second
                day = first
            }
            else -> {
                val century = (referenceYear / 100) * 100
                var resolved = century + first
                if (resolved > referenceYear) resolved -= 100
                year = resolved
                month = second
                day = third
            }
        }
        if (year !in 1300..1599 || month !in 1..12 ||
            day !in 1..PersianCalendarHelper.getDaysInMonth(year, month)
        ) return null
        return Triple(year, month, day)
    }

    private fun classifyType(message: String): TransactionType? = when {
        Regex("""کارت\s*به\s*کارت|انتقال|پایا|ساتنا|حواله|transfer""", RegexOption.IGNORE_CASE).containsMatchIn(message) ->
            TransactionType.TRANSFER
        Regex("""برداشت|خرید|پرداخت|withdraw|debit|purchase""", RegexOption.IGNORE_CASE).containsMatchIn(message) ->
            TransactionType.EXPENSE
        Regex("""واریز|واريز|دریافت|دريافت|deposit|credited|credit""", RegexOption.IGNORE_CASE).containsMatchIn(message) ->
            TransactionType.INCOME
        else -> null
    }

    private fun identifyBank(message: String, sender: String): String {
        val haystack = normalize("$message $sender").lowercase()
        return bankAliases.entries.firstOrNull { (_, aliases) ->
            aliases.any { alias ->
                val candidate = normalize(alias).lowercase()
                if (candidate.any { it in 'a'..'z' }) {
                    Regex("""(?<![a-z])${Regex.escape(candidate)}(?![a-z])""").containsMatchIn(haystack)
                } else candidate in haystack
            }
        }?.key ?: "بانک ناشناس"
    }

    private fun normalize(value: String): String =
        BankSmsParser.normalizeText(value).replace('ك', 'ک').replace('ة', 'ه')

    private fun parseDigits(value: String): Long? {
        val digits = IranianPhoneUtils.convertDigitsToEnglish(value).filter { it in '0'..'9' }
        return digits.toLongOrNull()?.takeIf { it > 0L && it <= 1_000_000_000_000_000L }
    }
}
