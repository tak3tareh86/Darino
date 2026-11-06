package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.ui.screens.settings.model.AppCurrency
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
        "بانک سامان" to listOf("بانک سامان", "saman bank", "saman", "بلو", "بلوبانک", "blue bank", "blu bank", "blu"),
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
        "بانک قرض‌الحسنه رسالت" to listOf("بانک رسالت", "قرض الحسنه رسالت", "resalat", "resalatbank"),
        "مؤسسه اعتباری ملل" to listOf("موسسه اعتباری ملل", "مؤسسه اعتباری ملل", "اعتباری ملل", "melal"),
        "مؤسسه اعتباری نور" to listOf("موسسه اعتباری نور", "مؤسسه اعتباری نور", "اعتباری نور"),
        "مؤسسه اعتباری کوثر" to listOf("موسسه اعتباری کوثر", "مؤسسه اعتباری کوثر", "اعتباری کوثر")
    )

    private val groupedAmountRegex = Regex("""(?<!\d)[+-]?\d{1,3}(?:[,،٬٫.]\d{3})+(?![\d])""")

    private fun extractGroupedAmounts(message: String): List<Long> =
        groupedAmountRegex.findAll(normalize(message))
            .mapNotNull { parseDigits(it.value) }
            .filter { it in 1L..100_000_000_000_000L }
            .distinct()
            .toList()

    private val transactionSignal = Regex(
        """واریز|واريز|دریافت|دريافت|برداشت|خرید|خريد|پرداخت|انتقال|تراکنش|کارت\s*به\s*کارت|پایا|ساتنا|حواله|نشست|پرید|بازپرداخت|وام|deposit|withdraw|debit|credit|purchase|transfer|[+-][0-9۰-۹٠-٩]""",
        RegexOption.IGNORE_CASE
    )
    private val amountLabel = Regex(
        """(?:مبلغ(?:\s*تراکنش)?|مبـلغ|برداشت(?:\s*وجه)?|واریز(?:\s*وجه)?|واريز|دریافت(?:\s*وجه)?|دريافت|خرید|خريد|پرداخت|انتقال|کارت\s*به\s*کارت|انتقال\s*وجه|وجه\s*انتقالی|deposit|withdraw(?:al)?|debit|credit|purchase|transfer)\s*[:：]?\s*([0-9۰-۹٠-٩][0-9۰-۹٠-٩,،٬٫.]*)""",
        RegexOption.IGNORE_CASE
    )
    private val signedAmount = Regex("""[+-]\s*([0-9۰-۹٠-٩][0-9۰-۹٠-٩,،٬٫.]*)""")
    private val balanceLabel = Regex(
        """(?:مانده|موجودی|موجودى|balance)\s*[:：]?\s*([0-9۰-۹٠-٩][0-9۰-۹٠-٩,،٬٫.]*)""",
        RegexOption.IGNORE_CASE
    )
    private val timeRegex = Regex("""(?<![0-9۰-۹٠-٩])([0-2]?[0-9]:[0-5][0-9])(?::[0-5][0-9])?(?![0-9۰-۹٠-٩])""")
    private val dateRegex = Regex("""(?<![0-9۰-۹٠-٩])([0-9۰-۹٠-٩]{1,4})[/.-]([0-9۰-۹٠-٩]{1,2})[/.-]([0-9۰-۹٠-٩]{1,4})(?![0-9۰-۹٠-٩])""")
    private val monthDayDateRegex = Regex("""(?<![0-9۰-۹٠-٩])([0-9۰-۹٠-٩]{1,2})/([0-9۰-۹٠-٩]{1,2})(?![0-9۰-۹٠-٩])""")

    fun parse(smsId: String, sender: String, body: String, timestampMillis: Long, forcedType: TransactionType? = null, forcedBankName: String? = null): BankSmsSuggestion? {
        // Reject every message containing "رمز", regardless of the following words.
        if (BankSmsParser.containsPasswordKeyword(body)) return null
        val normalized = normalize(body)
        if (normalized.isBlank() || !transactionSignal.containsMatchIn(normalized)) return null

        // OTPs and failed/cancelled notices are not completed financial transactions.
        if (Regex("""رمز\s*(?:پویا|یکبار|یک\s*بار)|رمز\s*عبور|کد\s*تأیید|کد\s*تاييد|otp|one[- ]time password""", RegexOption.IGNORE_CASE)
                .containsMatchIn(normalized) ||
            Regex("""ناموفق|لغو\s*شد|انجام\s*نشد|تراکنش\s*رد\s*شد|failed|declined|cancelled""", RegexOption.IGNORE_CASE)
                .containsMatchIn(normalized)
        ) return null

        // Some banks (including Blu) put the amount before a descriptive verb,
        // rather than immediately after a label such as "برداشت" or "واریز".
        // Prefer the established labeled format; otherwise select only a currency
        // amount whose following sentence explicitly describes a completed credit/debit.
        val labeledAmounts = amountLabel.findAll(normalized).toList()
        val amountMatch = (
            labeledAmounts.singleOrNull()
                ?: if (labeledAmounts.isEmpty()) {
                    val contextualAmounts = Regex(
                        """([0-9۰-۹٠-٩][0-9۰-۹٠-٩,،٬٫.]*)\s*(?:ریال|ريال|rial|rials)""",
                        RegexOption.IGNORE_CASE
                    ).findAll(normalized).filter { match ->
                        val followingText = normalized.substring(
                            match.range.last + 1,
                            minOf(normalized.length, match.range.last + 100)
                        )
                        Regex("""به\s*حساب\s*شما\s*نشست|از\s*حساب\s*شما\s*پرید""")
                            .containsMatchIn(followingText)
                    }.toList()
                    contextualAmounts.singleOrNull() ?: signedAmount.findAll(normalized).toList().singleOrNull()
                } else null
            ) ?: return null
        val amount = parseDigits(amountMatch.groupValues[1]) ?: return null
        if (amount !in 1L..100_000_000_000_000L) return null

        val type = forcedType ?: classifyType(normalized) ?: return null
        // Preserve the exact numeric value found in the SMS. Currency labels never
        // change the extracted amount; both Rial and Toman messages keep their digits.
        val amountValue = amount
        val amountUnit = if (Regex("""(?:ریال|ريال|rial|rials)""", RegexOption.IGNORE_CASE).containsMatchIn(normalized) && !Regex("""(?:تومان|تومن|toman|tomans)""", RegexOption.IGNORE_CASE).containsMatchIn(normalized)) AppCurrency.RIAL else AppCurrency.TOMAN
        if (amountValue !in 1L..100_000_000_000_000L) return null

        val balanceMatch = balanceLabel.findAll(normalized).singleOrNull()
        val balanceRaw = balanceMatch?.let { parseDigits(it.groupValues[1]) }
        // Balance values follow the same raw-number rule as transaction amounts.
        val balance = balanceRaw

        val bankName = forcedBankName?.takeIf { it.isNotBlank() } ?: identifyBank(normalized, sender)
        val receivedDateTime = PersianCalendarHelper.fromEpochMillis(timestampMillis)
        val dateParts = dateRegex.find(normalized)?.let { match ->
            parseJalaliDate(
                match.groupValues[1], match.groupValues[2], match.groupValues[3],
                receivedDateTime.year
            )
        } ?: monthDayDateRegex.find(normalized)?.let { match ->
            val month = IranianPhoneUtils.convertDigitsToEnglish(match.groupValues[1]).toIntOrNull()
            val day = IranianPhoneUtils.convertDigitsToEnglish(match.groupValues[2]).toIntOrNull()
            if (month != null && day != null && month in 1..12 &&
                day in 1..PersianCalendarHelper.getDaysInMonth(receivedDateTime.year, month)
            ) Triple(receivedDateTime.year, month, day) else null
        } ?: Triple(receivedDateTime.year, receivedDateTime.month, receivedDateTime.day)
        val time = timeRegex.findAll(normalized).mapNotNull {
            val parts = IranianPhoneUtils.convertDigitsToEnglish(it.groupValues[1]).split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            if (hour in 0..23 && minute in 0..59) hour to minute else null
        }.firstOrNull() ?: (receivedDateTime.hour to receivedDateTime.minute)
        val hasExplicitDateOrTime = dateRegex.containsMatchIn(normalized) || monthDayDateRegex.containsMatchIn(normalized) || timeRegex.containsMatchIn(normalized)
        val finalTimestamp = if (hasExplicitDateOrTime) {
            runCatching {
                PersianCalendarHelper.jalaliToEpochMillis(
                    dateParts.first, dateParts.second, dateParts.third, time.first, time.second
                )
            }.getOrNull() ?: timestampMillis
        } else timestampMillis

        val formatted = when (type) {
            TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(amountValue, isExpense = true, currency = com.example.ui.screens.settings.model.AppCurrency.TOMAN)
            TransactionType.INCOME -> MoneyFormatter.formatSignedToman(amountValue, isExpense = false, currency = com.example.ui.screens.settings.model.AppCurrency.TOMAN)
            TransactionType.TRANSFER -> MoneyFormatter.formatToman(amountValue, currency = com.example.ui.screens.settings.model.AppCurrency.TOMAN)
        }
        val destinationAccountNumber = Regex(
            """(?:به|مقصد)\s*(?:حساب|کارت)\s*[:：]?\s*([0-9۰-۹٠-٩*]{4,26})"""
        ).find(normalized)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
        val sourceAccountNumber = Regex(
            """(?:از|مبدأ|مبدا)\s*(?:حساب|کارت)\s*[:：]?\s*([0-9۰-۹٠-٩*]{4,26})"""
        ).find(normalized)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            ?: if (destinationAccountNumber == null) {
                Regex("""(?:حساب|کارت)\s*[:：]?\s*([0-9۰-۹٠-٩*]{4,26})""")
                    .find(normalized)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            } else null
        val tracking = Regex("""(?:پیگیری|رهگیری|شناسه)\s*[:：]?\s*([A-Za-z0-9۰-۹٠-٩-]{4,32})""", RegexOption.IGNORE_CASE)
            .find(normalized)?.groupValues?.get(1)

        return BankSmsSuggestion(
            id = smsId,
            bankName = bankName,
            amount = amountValue,
            amountUnit = amountUnit,
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
            sourceAccount = sourceAccountNumber?.let { "حساب/کارت $it" },
            destinationAccount = destinationAccountNumber?.let { "حساب/کارت $it" },
            rawSender = sender,
            timestampMillis = finalTimestamp,
            receivedAtMillis = timestampMillis,
            balanceAmount = balance,
            amountCandidates = (extractGroupedAmounts(body) + listOfNotNull(amountValue, balance)).distinct(),
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
                if (resolved !in (referenceYear - 1)..referenceYear) return null
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

    private fun classifyType(message: String): TransactionType? {
        val isTransfer = Regex(
            """کارت\s*به\s*کارت|انتقال|پایا|ساتنا|حواله|transfer""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(message)
        val hasPositiveSignedAmount = Regex("""(?:^|[\s_])\+\s*[0-9۰-۹٠-٩]""").containsMatchIn(message)
        val hasNegativeSignedAmount = Regex("""(?:^|[\s_])-\s*[0-9۰-۹٠-٩]""").containsMatchIn(message)
        val isExpense = hasNegativeSignedAmount || Regex(
            """برداشت|خرید|پرداخت|پرید|بازپرداخت|کسر\s*شد|کم\s*شد|withdraw|debit|purchase""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(message)
        val isIncome = hasPositiveSignedAmount || Regex(
            """واریز|واريز|دریافت|دريافت|نشست|وارد\s*حساب\s*شد|deposit|credited|credit""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(message)
        return when {
            isTransfer -> TransactionType.TRANSFER
            isExpense && isIncome -> null
            isExpense -> TransactionType.EXPENSE
            isIncome -> TransactionType.INCOME
            else -> null
        }
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
        }?.key ?: extractBankNameFromText(haystack) ?: "بانک ناشناس"
    }

    /**
     * Handles institutions not yet present in the alias map when the message explicitly
     * names them (for example, "بانک نمونه" or "مؤسسه اعتباری نمونه").
     */
    private fun extractBankNameFromText(text: String): String? {
        val stopWords = setOf(
            "واریز", "واريز", "برداشت", "خرید", "خريد", "پرداخت", "انتقال",
            "مبلغ", "تراکنش", "حساب", "کارت", "شماره", "پیامک", "موفق"
        )
        val institution = Regex(
            """(موسسه\s+اعتباری|مؤسسه\s+اعتباری)\s+([آ-یA-Za-z]+(?:\s+[آ-یA-Za-z]+){0,2})"""
        ).find(text)
        if (institution != null) {
            val name = institution.groupValues[2].split(Regex("""\s+"""))
                .takeWhile { it !in stopWords }
                .joinToString(" ")
            if (name.isNotBlank()) return "مؤسسه اعتباری $name"
        }
        val bank = Regex("""بانک\s+([آ-یA-Za-z]+(?:\s+[آ-یA-Za-z]+){0,2})""").find(text)
        if (bank != null) {
            val name = bank.groupValues[1].split(Regex("""\s+"""))
                .takeWhile { it !in stopWords }
                .joinToString(" ")
            if (name.isNotBlank()) return "بانک $name"
        }
        val englishBank = Regex("""bank\s+([a-z]+(?:\s+[a-z]+){0,2})""", RegexOption.IGNORE_CASE).find(text)
        if (englishBank != null) {
            val name = englishBank.groupValues[1].split(Regex("""\s+"""))
                .takeWhile { it.lowercase() !in setOf("deposit", "withdrawal", "transfer", "payment", "transaction", "account") }
                .joinToString(" ")
            if (name.isNotBlank()) return "بانک $name"
        }
        return null
    }

    private fun normalize(value: String): String =
        BankSmsParser.normalizeText(value).replace('ك', 'ک').replace('ة', 'ه')

    private fun parseDigits(value: String): Long? {
        val digits = IranianPhoneUtils.convertDigitsToEnglish(value).filter { it in '0'..'9' }
        return digits.toLongOrNull()?.takeIf { it > 0L && it <= 1_000_000_000_000_000L }
    }
}
