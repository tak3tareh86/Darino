package com.example.data.sms

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.util.IranianAmountUtils
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
        "پست بانک" to listOf("پست بانک", "postbank")
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
                lowerBody.contains("مبلغ")

        val hasSenderIndicator = BANK_PATTERNS.any { (_, aliases) ->
            aliases.any { lowerSender.contains(it) || lowerBody.contains(it) }
        }

        return hasBankKeyword && hasSenderIndicator
    }

    fun parse(smsId: String, sender: String, body: String, timestampMillis: Long): BankSmsSuggestion? {
        try {
            val cleanBody = com.example.util.IranianPhoneUtils.convertDigitsToEnglish(body)
            val bankName = detectBankName(sender, body)

            val detectedType = detectTransactionType(cleanBody)
            val type = detectedType ?: TransactionType.EXPENSE
            val isTypeUncertain = (detectedType == null)
            val amountToman = extractAmountToman(cleanBody) ?: return null

            val category = suggestCategory(cleanBody, type)
            val formattedAmount = MoneyFormatter.formatSignedToman(amountToman, isExpense = (type == TransactionType.EXPENSE))

            val (dateText, timeText) = formatDateAndTime(timestampMillis)
            val (sourceAcc, destAcc) = extractAccounts(cleanBody, bankName)

            return BankSmsSuggestion(
                id = smsId,
                bankName = bankName,
                amount = amountToman,
                formattedAmount = formattedAmount,
                type = detectedType, // Passing detectedType directly (which is null if uncertain)
                isTypeUncertain = isTypeUncertain,
                smsText = body.trim(),
                dateText = dateText,
                timeText = timeText,
                category = category,
                sourceAccount = sourceAcc,
                destinationAccount = destAcc,
                rawSender = sender,
                parseError = if (isTypeUncertain) "نوع تراکنش به صورت خودکار تشخیص داده نشد؛ لطفاً بررسی کنید." else null
            )
        } catch (e: Exception) {
            Log.w("BankSmsParser", "Failed to parse SMS $smsId: ${e.message}")
            return BankSmsSuggestion(
                id = smsId,
                bankName = detectBankName(sender, body),
                amount = 0L,
                formattedAmount = "۰ تومان",
                type = null, // Uncertain on failure
                isTypeUncertain = true,
                smsText = body.trim(),
                dateText = "امروز",
                timeText = "نامشخص",
                category = "سایر",
                rawSender = sender,
                parseError = "خطا در استخراج خودکار جزئیات پیامک"
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
        val hasExpense = text.contains("برداشت") || text.contains("خرید") || text.contains("کسر") || text.contains("بدهکار") || text.contains("پرداخت") || text.contains("پایانه")
        val hasIncome = text.contains("واریز") || text.contains("بستانکار") || text.contains("واریز حقوق") || text.contains("سود سپرده")
        val hasTransfer = text.contains("انتقال") || text.contains("کارت به کارت") ||
                (text.contains("پایا") && !text.contains("پایانه")) ||
                text.contains("ساتنا")

        return when {
            hasExpense -> TransactionType.EXPENSE
            hasIncome -> TransactionType.INCOME
            hasTransfer -> TransactionType.TRANSFER
            else -> null
        }
    }

    private fun extractAmountToman(text: String): Long? {
        // Regex patterns to find amounts with commas or periods followed by currency or prefix
        val patterns = listOf(
            Pattern.compile("""(?:مبلغ|برداشت|واریز|خرید|انتقال)[\s:]*([0-9,.]+)\s*(ریال|تومان|تومان|Rls)?""", Pattern.CASE_INSENSITIVE),
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
                        // Rial to Toman
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

    private fun formatDateAndTime(timestampMillis: Long): Pair<String, String> {
        val jalali = PersianCalendarHelper.fromEpochMillis(timestampMillis)
        val date = jalali.toFormattedDate()
        val time = jalali.toFormattedTime()
        return Pair(date, time)
    }

    private fun extractAccounts(text: String, bankName: String): Pair<String?, String?> {
        var sourceAcc: String? = bankName
        var destAcc: String? = null

        val cardPattern = Pattern.compile("""(?:کارت|حساب)[\s:]*([*0-9-]{4,16})""")
        val matcher = cardPattern.matcher(text)
        if (matcher.find()) {
            val card = matcher.group(1)
            sourceAcc = "$bankName ($card)"
        }

        val toPattern = Pattern.compile("""(?:به|مقصد)[\s:]*(?:کارت|حساب)?[\s:]*([*0-9-]{4,16})""")
        val toMatcher = toPattern.matcher(text)
        if (toMatcher.find()) {
            destAcc = "کارت مقصد (${toMatcher.group(1)})"
        }

        return Pair(sourceAcc, destAcc)
    }
}
