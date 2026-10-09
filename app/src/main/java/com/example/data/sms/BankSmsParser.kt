package com.example.data.sms

import android.util.Log
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper
import java.util.regex.Pattern

object BankSmsParser {

    private val BANK_PATTERNS = listOf(
        "بانک ملت" to listOf("ملت", "mellat"),
        "بانک ملی" to listOf("ملی", "bmi", "melli"),
        "بانک سامان" to listOf("سامان", "saman"),
        "بانک پاسارگاد" to listOf("پاسارگاد", "pasargad"),
        "بانک صادرات" to listOf("صادرات", "saderat", "bsi"),
        "بانک تجارت" to listOf("تجارت", "tejarat"),
        "بلوبانک" to listOf("بلوبانک", "blubank", "بلو"),
        "بانک رسالت" to listOf("رسالت", "resalat"),
        "بانک سپه" to listOf("سپه", "sepah"),
        "بانک کشاورزی" to listOf("کشاورزی", "keshavarzi", "bki"),
        "بانک پارسیان" to listOf("پارسیان", "parsian"),
        "بانک آینده" to listOf("آینده", "ayandeh"),
        "بانک رفاه" to listOf("رفاه", "refah"),
        "بانک شهر" to listOf("بانک شهر", "shahr"),
        "بانک مسکن" to listOf("مسکن", "maskan"),
        "بانک دی" to listOf("بانک دی", "dey"),
        "بانک سینا" to listOf("سینا", "sina"),
        "پست بانک" to listOf("پست بانک", "postbank"),
        "بانک مهر ایران" to listOf("مهر ایران", "qmb", "mehreiran"),
        "بانک گردشگری" to listOf("گردشگری", "gardeshgari"),
        "بانک ایران زمین" to listOf("ایران زمین", "izbank"),
        "بانک کارآفرین" to listOf("کارآفرین", "karafarin"),
        "بانک اقتصاد نوین" to listOf("اقتصاد نوین", "enbank"),
        "بانک خاورمیانه" to listOf("خاورمیانه", "middleeastbank"),
        "بانک سرمایه" to listOf("بانک سرمایه", "sarmayeh")
    )

    fun isPotentialBankSms(sender: String, body: String): Boolean {
        val lowerSender = sender.lowercase()
        val lowerBody = body.lowercase()

        val hasBankKeyword = lowerBody.contains("بانک") ||
                lowerBody.contains("برداشت") ||
                lowerBody.contains("واریز") ||
                lowerBody.contains("مانده") ||
                lowerBody.contains("کارت") ||
                lowerBody.contains("حساب") ||
                lowerBody.contains("انتقال") ||
                lowerBody.contains("پایا") ||
                lowerBody.contains("ساتنا") ||
                lowerBody.contains("خرید") ||
                lowerBody.contains("مبلغ") ||
                lowerBody.contains("کسر") ||
                lowerBody.contains("پرداخت")

        val hasSenderIndicator = BANK_PATTERNS.any { (_, aliases) ->
            aliases.any { lowerSender.contains(it) || lowerBody.contains(it) }
        }

        val hasTransactionKeywords = (lowerBody.contains("واریز") || lowerBody.contains("برداشت") || lowerBody.contains("انتقال") || lowerBody.contains("خرید") || lowerBody.contains("کسر")) &&
                (lowerBody.contains("مانده") || lowerBody.contains("حساب") || lowerBody.contains("کارت") || lowerBody.contains("مبلغ"))

        return hasBankKeyword && (hasSenderIndicator || hasTransactionKeywords)
    }

    fun parse(smsId: String, sender: String, body: String, timestampMillis: Long): BankSmsSuggestion? {
        try {
            val cleanBody = IranianPhoneUtils.convertDigitsToEnglish(body)
            val bankName = detectBankName(sender, body)

            val detectedType = detectTransactionType(cleanBody)
            val isTypeUncertain = (detectedType == null)
            val amountToman = extractAmountToman(cleanBody)

            val isAmountUncertain = (amountToman == null || amountToman <= 0L)
            val effectiveAmount = amountToman ?: 0L

            val category = suggestCategory(cleanBody, detectedType ?: TransactionType.EXPENSE)
            val formattedAmount = when {
                isAmountUncertain -> "مبلغ نامشخص"
                detectedType == TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(effectiveAmount, isExpense = true)
                detectedType == TransactionType.INCOME -> MoneyFormatter.formatSignedToman(effectiveAmount, isExpense = false)
                else -> MoneyFormatter.formatToman(effectiveAmount)
            }

            val (dateText, timeText, finalTimestamp) = formatDateAndTime(timestampMillis, cleanBody)
            val (sourceAcc, destAcc) = extractAccounts(cleanBody, bankName)

            val errorMsg = when {
                isTypeUncertain && isAmountUncertain -> "نوع و مبلغ تراکنش به صورت خودکار تشخیص داده نشد؛ لطفاً تراکنش را بررسی و تکمیل کنید."
                isTypeUncertain -> "نوع تراکنش به صورت خودکار تشخیص داده نشد؛ لطفاً نوع را مشخص کنید."
                isAmountUncertain -> "مبلغ تراکنش به صورت خودکار تشخیص داده نشد؛ لطفاً مبلغ را مشخص کنید."
                else -> null
            }

            return BankSmsSuggestion(
                id = smsId,
                bankName = bankName,
                amount = effectiveAmount,
                formattedAmount = formattedAmount,
                type = detectedType,
                isTypeUncertain = (isTypeUncertain || isAmountUncertain),
                smsText = body.trim(),
                dateText = dateText,
                timeText = timeText,
                category = category,
                sourceAccount = sourceAcc,
                destinationAccount = destAcc,
                rawSender = sender,
                parseError = errorMsg,
                timestampMillis = finalTimestamp
            )
        } catch (e: Exception) {
            // Keep logs secure: avoid printing SMS body or card numbers
            Log.w("BankSmsParser", "Failed to parse SMS $smsId")
            val defaultPdt = PersianCalendarHelper.fromEpochMillis(timestampMillis)
            return BankSmsSuggestion(
                id = smsId,
                bankName = detectBankName(sender, body),
                amount = 0L,
                formattedAmount = "۰ تومان",
                type = null,
                isTypeUncertain = true,
                smsText = body.trim(),
                dateText = defaultPdt.toFormattedDate(),
                timeText = defaultPdt.toFormattedTime(),
                category = "سایر",
                rawSender = sender,
                parseError = "خطا در استخراج خودکار جزئیات پیامک",
                timestampMillis = timestampMillis
            )
        }
    }

    private fun detectBankName(sender: String, body: String): String {
        val lowerSender = sender.lowercase()
        val lowerBody = body.lowercase()

        for ((displayName, aliases) in BANK_PATTERNS) {
            if (aliases.any { lowerSender.contains(it) || lowerBody.contains(it) }) {
                return displayName
            }
        }
        return if (sender.isNotBlank()) "بانک ($sender)" else "پیامک بانکی"
    }

    fun detectTransactionType(text: String): TransactionType? {
        val textWithoutTerminal = text.replace("پایانه", "")

        val isTransfer = text.contains("انتقال") || text.contains("کارت به کارت") ||
                text.contains("ساتنا") || textWithoutTerminal.contains("پایا")
        val isIncome = text.contains("واریز") || text.contains("بستانکار") ||
                text.contains("سود سپرده") || text.contains("واریز حقوق")
        val isExpense = text.contains("برداشت") || text.contains("خرید") ||
                text.contains("کسر") || text.contains("بدهکار") ||
                text.contains("پرداخت") || text.contains("پایانه")

        return when {
            isTransfer && (text.contains("به کارت") || text.contains("به حساب") || text.contains("کارت به کارت") || !isIncome) -> TransactionType.TRANSFER
            isTransfer && isIncome -> TransactionType.INCOME
            isExpense && !isIncome -> TransactionType.EXPENSE
            isIncome && !isExpense -> TransactionType.INCOME
            isExpense -> TransactionType.EXPENSE
            isIncome -> TransactionType.INCOME
            else -> null
        }
    }

    private fun extractAmountToman(text: String): Long? {
        val cleanNumber = { s: String ->
            s.replace(",", "")
                .replace("،", "")
                .replace("٫", "")
                .replace(".", "")
                .trim()
        }

        // 1. Explicit transaction amount pattern with valid transaction prefix
        val txPattern = Pattern.compile(
            """(?:مبلغ|برداشت|واریز|خرید|انتقال|کسر|پرداخت|بدهکار|بستانکار)[\s:]*[+-]?\s*([0-9,.\u060C\u066B]+)\s*(ریال|تومان|Rls|IRR)?""",
            Pattern.CASE_INSENSITIVE
        )

        val txMatcher = txPattern.matcher(text)
        val candidateAmounts = mutableListOf<Long>()

        while (txMatcher.find()) {
            val numStr = cleanNumber(txMatcher.group(1) ?: continue)
            val rawNumber = numStr.toLongOrNull() ?: continue
            if (rawNumber <= 0L) continue

            val unit = txMatcher.group(2)?.trim()
            val toman = when {
                unit?.contains("ریال", ignoreCase = true) == true ||
                unit?.contains("Rls", ignoreCase = true) == true ||
                unit?.contains("IRR", ignoreCase = true) == true -> {
                    (rawNumber / 10L).coerceAtLeast(1L)
                }
                else -> rawNumber
            }
            candidateAmounts.add(toman)
        }

        // If candidates with explicit transaction prefixes are found:
        if (candidateAmounts.isNotEmpty()) {
            val distinctAmounts = candidateAmounts.distinct()
            return if (distinctAmounts.size == 1) {
                distinctAmounts.first()
            } else {
                // Multiple conflicting transaction amounts in the same SMS -> ambiguous, do not guess
                null
            }
        }

        // 2. Fallback: Search for numbers followed by explicit currency (ریال or تومان),
        // excluding balance, card, or tracking code prefixes
        val currencyPattern = Pattern.compile(
            """([0-9,.\u060C\u066B]{3,15})\s*(ریال|تومان|Rls|IRR)""",
            Pattern.CASE_INSENSITIVE
        )
        val curMatcher = currencyPattern.matcher(text)
        val fallbackAmounts = mutableListOf<Long>()

        while (curMatcher.find()) {
            val startIdx = curMatcher.start()
            val prefixWindow = text.substring((startIdx - 15).coerceAtLeast(0), startIdx)
            // Exclude if prefixed with balance, tracking, card or account
            if (prefixWindow.contains("مانده") || prefixWindow.contains("موجودی") ||
                prefixWindow.contains("پیگیری") || prefixWindow.contains("مرجع") ||
                prefixWindow.contains("ارجاع") || prefixWindow.contains("کارت") ||
                prefixWindow.contains("حساب")
            ) {
                continue
            }

            val numStr = cleanNumber(curMatcher.group(1) ?: continue)
            val rawNumber = numStr.toLongOrNull() ?: continue
            if (rawNumber <= 0L) continue

            val unit = curMatcher.group(2)?.trim()
            val toman = when {
                unit?.contains("ریال", ignoreCase = true) == true ||
                unit?.contains("Rls", ignoreCase = true) == true ||
                unit?.contains("IRR", ignoreCase = true) == true -> {
                    (rawNumber / 10L).coerceAtLeast(1L)
                }
                else -> rawNumber
            }
            fallbackAmounts.add(toman)
        }

        if (fallbackAmounts.isNotEmpty()) {
            val distinctFallback = fallbackAmounts.distinct()
            return if (distinctFallback.size == 1) {
                distinctFallback.first()
            } else {
                null
            }
        }

        return null
    }

    private fun suggestCategory(text: String, type: TransactionType): String {
        return when (type) {
            TransactionType.INCOME -> {
                when {
                    text.contains("حقوق") || text.contains("دستمزد") -> "حقوق و دستمزد"
                    text.contains("سود") || text.contains("سپرده") -> "سود سپرده"
                    text.contains("یارانه") -> "یارانه و کمک‌معیشتی"
                    else -> "درآمد و واریز"
                }
            }
            TransactionType.TRANSFER -> "انتقال بین‌بانکی"
            TransactionType.EXPENSE -> {
                when {
                    text.contains("افق کوروش") || text.contains("فروشگاه") || text.contains("هایپر") || text.contains("سوپر") || text.contains("دیلی مارکت") -> "سوپرمارکت و خرید"
                    text.contains("بنزین") || text.contains("جایگاه") || text.contains("پمپ") -> "خودرو و سوخت"
                    text.contains("اسنپ") || text.contains("تپسی") || text.contains("تاکسی") || text.contains("مترو") -> "حمل و نقل"
                    text.contains("رستوران") || text.contains("کافه") || text.contains("فست فود") || text.contains("اسنپ فود") -> "غذا و رستوران"
                    text.contains("داروخانه") || text.contains("درمان") || text.contains("بیمارستان") || text.contains("پزشک") -> "سلامت و درمان"
                    text.contains("قبض") || text.contains("همراه اول") || text.contains("ایرانسل") || text.contains("مخابرات") -> "قبوض و خدمات"
                    text.contains("قسط") || text.contains("تسهیلات") || text.contains("وام") -> "اقساط و تسهیلات"
                    else -> "خرید روزمره"
                }
            }
        }
    }

    private fun formatDateAndTime(timestampMillis: Long, cleanBody: String): Triple<String, String, Long> {
        val defaultPdt = PersianCalendarHelper.fromEpochMillis(timestampMillis)

        // Try extracting reliable Jalali date (e.g. 1403/07/15) AND reliable time (e.g. ساعت 14:30)
        val datePattern = Pattern.compile("""\b(140[0-9])[/-](0?[1-9]|1[0-2])[/-](0?[1-9]|[12][0-9]|3[01])\b""")
        val timePattern = Pattern.compile("""(?:ساعت[\s:]+)([0-2]?[0-9]:[0-5][0-9])""")

        val dateMatcher = datePattern.matcher(cleanBody)
        val timeMatcher = timePattern.matcher(cleanBody)

        if (dateMatcher.find() && timeMatcher.find()) {
            val y = dateMatcher.group(1).toIntOrNull()
            val m = dateMatcher.group(2).toIntOrNull()
            val d = dateMatcher.group(3).toIntOrNull()
            val rawTime = timeMatcher.group(1)
            val timeParts = rawTime?.split(":")
            val hour = timeParts?.getOrNull(0)?.toIntOrNull()
            val minute = timeParts?.getOrNull(1)?.toIntOrNull()

            if (y != null && m != null && d != null && hour != null && minute != null) {
                try {
                    val parsedEpoch = PersianCalendarHelper.jalaliToEpochMillis(y, m, d, hour, minute)
                    val pdt = PersianCalendarHelper.fromEpochMillis(parsedEpoch)
                    return Triple(pdt.toFormattedDate(), pdt.toFormattedTime(), parsedEpoch)
                } catch (e: Exception) {
                    // Fallback to timestampMillis
                }
            }
        }

        // Reliable fallback: use SMS reception timestamp
        return Triple(defaultPdt.toFormattedDate(), defaultPdt.toFormattedTime(), timestampMillis)
    }

    private fun extractAccounts(text: String, bankName: String): Pair<String?, String?> {
        var sourceAcc: String? = bankName
        var destAcc: String? = null

        // 1. Destination card/account pattern (e.g. به کارت 6037..., به حساب 123..., مقصد...)
        val toPattern = Pattern.compile("""(?:(?:به|مقصد)\s*(?:شماره\s*)?(?:کارت|حساب)?|واریز\s*به)[\s:]*([*0-9-]{4,26})""")
        val toMatcher = toPattern.matcher(text)
        var destDigits: String? = null
        if (toMatcher.find()) {
            val d = toMatcher.group(1)?.trim()
            if (!d.isNullOrBlank()) {
                destDigits = d
                destAcc = "کارت مقصد ($d)"
            }
        }

        // 2. Source card/account pattern (e.g. از کارت 123..., از حساب 456..., مبدأ...)
        val fromPattern = Pattern.compile("""(?:(?:از|مبدأ)\s*(?:شماره\s*)?(?:کارت|حساب)?|(?:کارت|حساب))[\s:]*([*0-9-]{4,26})""")
        val fromMatcher = fromPattern.matcher(text)
        while (fromMatcher.find()) {
            val candidate = fromMatcher.group(1)?.trim()
            if (!candidate.isNullOrBlank() && candidate != destDigits) {
                sourceAcc = "$bankName ($candidate)"
                break
            }
        }

        return Pair(sourceAcc, destAcc)
    }
}
