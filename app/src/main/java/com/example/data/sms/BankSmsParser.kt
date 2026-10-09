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
            val amountToman = extractAmountToman(cleanBody) ?: return null

            val category = suggestCategory(cleanBody, detectedType ?: TransactionType.EXPENSE)
            val formattedAmount = when (detectedType) {
                TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(amountToman, isExpense = true)
                TransactionType.INCOME -> MoneyFormatter.formatSignedToman(amountToman, isExpense = false)
                TransactionType.TRANSFER -> MoneyFormatter.formatToman(amountToman)
                null -> MoneyFormatter.formatToman(amountToman)
            }

            val (dateText, timeText) = formatDateAndTime(timestampMillis, cleanBody)
            val (sourceAcc, destAcc) = extractAccounts(cleanBody, bankName)

            return BankSmsSuggestion(
                id = smsId,
                bankName = bankName,
                amount = amountToman,
                formattedAmount = formattedAmount,
                type = detectedType,
                isTypeUncertain = isTypeUncertain,
                smsText = body.trim(),
                dateText = dateText,
                timeText = timeText,
                category = category,
                sourceAccount = sourceAcc,
                destinationAccount = destAcc,
                rawSender = sender,
                parseError = if (isTypeUncertain) "نوع تراکنش به صورت خودکار تشخیص داده نشد؛ لطفاً بررسی کنید." else null,
                timestampMillis = timestampMillis
            )
        } catch (e: Exception) {
            // Keep logs secure: avoid printing SMS body or card numbers
            Log.w("BankSmsParser", "Failed to parse SMS $smsId: ${e.message}")
            return BankSmsSuggestion(
                id = smsId,
                bankName = detectBankName(sender, body),
                amount = 0L,
                formattedAmount = "۰ تومان",
                type = null,
                isTypeUncertain = true,
                smsText = body.trim(),
                dateText = "امروز",
                timeText = "نامشخص",
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
        val patterns = listOf(
            Pattern.compile("""(?:مبلغ|برداشت|واریز|خرید|انتقال|کسر|پرداخت|بدهکار|بستانکار)[\s:]*[+-]?\s*([0-9,.]+)\s*(ریال|تومان|Rls)?""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""([0-9,.]+)\s*(ریال|تومان|Rls)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""([0-9]{4,12})[\s]*(?:ریال|تومان)?""")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val numStr = matcher.group(1)?.replace(",", "")?.replace(".", "")?.trim() ?: continue
                val rawNumber = numStr.toLongOrNull() ?: continue
                if (rawNumber <= 0L) continue

                val unit = if (matcher.groupCount() >= 2) matcher.group(2)?.trim() else null
                return when {
                    unit?.contains("ریال", ignoreCase = true) == true || unit?.contains("Rls", ignoreCase = true) == true -> {
                        (rawNumber / 10L).coerceAtLeast(1L)
                    }
                    unit?.contains("تومان", ignoreCase = true) == true -> {
                        rawNumber
                    }
                    rawNumber > 1_000_000_000L -> {
                        // Usually large Iranian numbers without unit are Rials
                        (rawNumber / 10L)
                    }
                    else -> rawNumber
                }
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

    private fun formatDateAndTime(timestampMillis: Long, cleanBody: String): Pair<String, String> {
        val jalali = PersianCalendarHelper.fromEpochMillis(timestampMillis)
        val date = jalali.toFormattedDate()

        // Check if SMS contains an explicit time like 14:30 or ساعت 14:30
        val timePattern = Pattern.compile("""(?:ساعت[\s:]*)?([0-2]?[0-9]:[0-5][0-9])""")
        val matcher = timePattern.matcher(cleanBody)
        val time = if (matcher.find()) {
            val t = matcher.group(1) ?: jalali.toFormattedTime()
            IranianPhoneUtils.convertDigitsToPersian(t)
        } else {
            jalali.toFormattedTime()
        }

        return Pair(date, time)
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
