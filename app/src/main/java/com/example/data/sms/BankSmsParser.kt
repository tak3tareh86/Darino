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

            val isAmountValid = (amountToman != null && amountToman > 0L)
            val effectiveAmount = amountToman ?: 0L

            val category = suggestCategory(cleanBody, detectedType ?: TransactionType.EXPENSE)
            val formattedAmount = when {
                !isAmountValid -> "مبلغ نامشخص"
                detectedType == TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(effectiveAmount, isExpense = true)
                detectedType == TransactionType.INCOME -> MoneyFormatter.formatSignedToman(effectiveAmount, isExpense = false)
                else -> MoneyFormatter.formatToman(effectiveAmount)
            }

            val (dateText, timeText, finalTimestamp) = formatDateAndTime(timestampMillis, cleanBody)
            val (sourceAcc, destAcc) = extractAccounts(cleanBody, bankName)

            val errorMsg = when {
                isTypeUncertain && !isAmountValid -> "نوع و مبلغ تراکنش به صورت خودکار تشخیص داده نشد؛ لطفاً تراکنش را بررسی و تکمیل کنید."
                isTypeUncertain -> "نوع تراکنش به صورت خودکار تشخیص داده نشد؛ لطفاً نوع را مشخص کنید."
                !isAmountValid -> "مبلغ تراکنش به صورت خودکار تشخیص داده نشد؛ لطفاً مبلغ را مشخص کنید."
                else -> null
            }

            return BankSmsSuggestion(
                id = smsId,
                bankName = bankName,
                amount = effectiveAmount,
                formattedAmount = formattedAmount,
                isAmountValid = isAmountValid,
                type = detectedType,
                isTypeUncertain = isTypeUncertain,
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
                formattedAmount = "مبلغ نامشخص",
                isAmountValid = false,
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

    /**
     * Context-aware detection of transaction type.
     * Prevents keyword-order bias from turning ambiguous messages into definite types.
     */
    fun detectTransactionType(text: String): TransactionType? {
        val lower = text.lowercase()

        // 1. Reversal / Cancellation / Failure -> Ambiguous, leave to user
        if (lower.contains("لغو") || lower.contains("ناموفق") ||
            lower.contains("برگشت") || lower.contains("عدم موفقیت") ||
            lower.contains("تراکنش ناموفق")
        ) {
            return null
        }

        // 2. Clear POS or Merchant Purchases -> Unambiguous Expense
        if (lower.contains("خرید از پایانه") || lower.contains("خرید پایانه") ||
            lower.contains("خرید اینترنتی") || lower.contains("خرید شارژ") ||
            lower.contains("خرید کالا") || lower.contains("خرید خدمات")
        ) {
            return TransactionType.EXPENSE
        }

        // 3. Clear Transfers
        val hasTransferWord = lower.contains("انتقال") || lower.contains("کارت به کارت") ||
                lower.contains("پایا") || lower.contains("ساتنا")

        if (hasTransferWord) {
            // Explicit incoming transfer to user's account
            if (lower.contains("انتقال به حساب شما") || lower.contains("واریز از طریق پایا") ||
                lower.contains("واریز از طریق ساتنا") || lower.contains("واریز پایا") ||
                lower.contains("واریز ساتنا") || lower.contains("واریز از طریق انتقال")
            ) {
                return TransactionType.INCOME
            }
            // Explicit outgoing transfer from user to another account
            if (lower.contains("انتقال از حساب شما") || lower.contains("انتقال وجه به") ||
                lower.contains("کارت به کارت به") || lower.contains("انتقال به کارت") ||
                lower.contains("انتقال به حساب")
            ) {
                return TransactionType.TRANSFER
            }
            // General "انتقال وجه" or "کارت به کارت" without directional indicators
            if (lower.contains("انتقال وجه") || lower.contains("کارت به کارت")) {
                if (lower.contains("برداشت") || lower.contains("کسر")) {
                    return TransactionType.TRANSFER
                } else if (lower.contains("واریز") || lower.contains("بستانکار")) {
                    return TransactionType.INCOME
                }
                return TransactionType.TRANSFER
            }
        }

        // 4. Deposits / Income
        val hasDepositWord = lower.contains("واریز") || lower.contains("بستانکار") ||
                lower.contains("سود سپرده") || lower.contains("واریز حقوق") ||
                lower.contains("یارانه")

        // 5. Withdrawals / Expenses
        val hasWithdrawWord = lower.contains("برداشت") || lower.contains("کسر") ||
                lower.contains("بدهکار") || lower.contains("پرداخت قبض") ||
                lower.contains("پرداخت قبوض") || lower.contains("پرداخت صورتحساب") ||
                lower.contains("خرید")

        // Disambiguation
        if (hasDepositWord && !hasWithdrawWord) {
            return TransactionType.INCOME
        }

        if (hasWithdrawWord && !hasDepositWord) {
            return TransactionType.EXPENSE
        }

        // Conflicting or ambiguous words present in the same SMS -> leave to user
        return null
    }

    private enum class CurrencyType {
        TOMAN,
        RIAL,
        UNKNOWN
    }

    /**
     * Robust extraction of transaction amount in Toman.
     * Separates amount extraction from transaction type.
     * Strictly verifies currency and conversion without arbitrary rounding or truncated digits.
     * Masks balances, card numbers, accounts, and tracking/reference IDs.
     */
    fun extractAmountToman(text: String): Long? {
        val cleanNumber = { s: String ->
            s.replace(",", "")
                .replace("،", "")
                .replace("٫", "")
                .replace("٬", "")
                .replace(".", "")
                .trim()
        }

        // Step 1: Mask known non-transaction numbers in a working copy of the text
        var maskedText = text

        // Mask 16-digit card numbers or 4-digit card masks
        maskedText = maskedText.replace(Regex("""(?i)(?:کارت|card)[\s:]*([0-9\s*#-]{4,19})""")) { m ->
            "کارت: [MASKED_CARD]"
        }

        // Mask account numbers
        maskedText = maskedText.replace(Regex("""(?i)(?:حساب|شماره\s*حساب|به\s*حساب|از\s*حساب)[\s:]*([0-9]{5,20})""")) { m ->
            "حساب: [MASKED_ACC]"
        }

        // Mask balances and available funds
        maskedText = maskedText.replace(
            Regex("""(?i)(?:مانده(?:ی|\s+حساب|\s+واقعی|\s+کل)?|موجودی(?:ی|\s+حساب|\s+فعلی|\s+کل)?|موجودي)[\s:]*[+-]?\s*([0-9,.\u060C\u066B\u066C]+)\s*(?:ریال|تومان|Rls|IRR)?""")
        ) { m ->
            "[MASKED_BALANCE]"
        }

        // Mask tracking, reference, and bill/terminal IDs
        maskedText = maskedText.replace(
            Regex("""(?i)(?:پیگیری|کد\s*پیگیری|شماره\s*پیگیری|مرجع|شناسه\s*مرجع|شماره\s*مرجع|ارجاع|شماره\s*ارجاع|کد\s*ارجاع|شناسه\s*قبض|شناسه\s*پرداخت|ترمینال|پایانه)[\s:]*([0-9]+)""")
        ) { m ->
            "[MASKED_REF]"
        }

        // Mask fees (کارمزد)
        maskedText = maskedText.replace(
            Regex("""(?i)(?:کارمزد)[\s:]*([0-9,.\u060C\u066B\u066C]+)\s*(?:ریال|تومان|Rls|IRR)?""")
        ) { m ->
            "[MASKED_FEE]"
        }

        // Step 2: Determine global message currency if unambiguous
        val hasGlobalRial = text.contains("ریال", ignoreCase = true) || text.contains("Rls", ignoreCase = true) || text.contains("IRR", ignoreCase = true)
        val hasGlobalToman = text.contains("تومان", ignoreCase = true) || text.contains("Toman", ignoreCase = true)

        val globalCurrency = when {
            hasGlobalToman && !hasGlobalRial -> CurrencyType.TOMAN
            hasGlobalRial && !hasGlobalToman -> CurrencyType.RIAL
            else -> CurrencyType.UNKNOWN
        }

        // Step 3: Match candidate transaction amounts with explicit transaction prefixes
        val txPattern = Pattern.compile(
            """(?:مبلغ|برداشت|واریز|خرید|انتقال|کسر|پرداخت|بدهکار|بستانکار)[\s:]*[+-]?\s*([0-9,.\u060C\u066B\u066C]+)\s*(ریال|تومان|Rls|IRR)?""",
            Pattern.CASE_INSENSITIVE
        )

        val txMatcher = txPattern.matcher(maskedText)
        val candidates = mutableListOf<Long>()

        while (txMatcher.find()) {
            val numStr = cleanNumber(txMatcher.group(1) ?: continue)
            val rawNumber = numStr.toLongOrNull() ?: continue
            if (rawNumber <= 0L) continue

            val unitGroup = txMatcher.group(2)?.trim()
            val detectedUnit = when {
                unitGroup?.contains("ریال", ignoreCase = true) == true ||
                unitGroup?.contains("Rls", ignoreCase = true) == true ||
                unitGroup?.contains("IRR", ignoreCase = true) == true -> CurrencyType.RIAL

                unitGroup?.contains("تومان", ignoreCase = true) == true ||
                unitGroup?.contains("Toman", ignoreCase = true) == true -> CurrencyType.TOMAN

                else -> globalCurrency
            }

            when (detectedUnit) {
                CurrencyType.TOMAN -> {
                    candidates.add(rawNumber)
                }
                CurrencyType.RIAL -> {
                    // Strict Rial to Toman conversion:
                    // Must be at least 10 Rials and cleanly divisible by 10 without rounding or dropping digits.
                    if (rawNumber >= 10L && rawNumber % 10L == 0L) {
                        candidates.add(rawNumber / 10L)
                    } else {
                        // Fraction of a Toman or indivisible -> cannot convert without arbitrary rounding -> reject
                    }
                }
                CurrencyType.UNKNOWN -> {
                    // Currency cannot be determined with certainty. Do not guess 10x difference -> reject
                }
            }
        }

        // If candidates with explicit transaction prefixes are found
        if (candidates.isNotEmpty()) {
            val distinct = candidates.distinct()
            return if (distinct.size == 1) {
                distinct.first()
            } else {
                // Multiple conflicting amounts found -> ambiguous
                null
            }
        }

        // Step 4: Fallback - search for numbers with explicit attached currency
        val fallbackPattern = Pattern.compile(
            """([0-9,.\u060C\u066B\u066C]{3,15})\s*(ریال|تومان|Rls|IRR)""",
            Pattern.CASE_INSENSITIVE
        )
        val curMatcher = fallbackPattern.matcher(maskedText)
        val fallbackCandidates = mutableListOf<Long>()

        while (curMatcher.find()) {
            val numStr = cleanNumber(curMatcher.group(1) ?: continue)
            val rawNumber = numStr.toLongOrNull() ?: continue
            if (rawNumber <= 0L) continue

            val unit = curMatcher.group(2)?.trim()
            val isRial = unit?.contains("ریال", ignoreCase = true) == true ||
                    unit?.contains("Rls", ignoreCase = true) == true ||
                    unit?.contains("IRR", ignoreCase = true) == true

            if (isRial) {
                if (rawNumber >= 10L && rawNumber % 10L == 0L) {
                    fallbackCandidates.add(rawNumber / 10L)
                }
            } else {
                fallbackCandidates.add(rawNumber)
            }
        }

        if (fallbackCandidates.isNotEmpty()) {
            val distinctFallback = fallbackCandidates.distinct()
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

        val extractedDate = if (dateMatcher.find()) {
            val y = dateMatcher.group(1)?.toIntOrNull() ?: defaultPdt.year
            val m = dateMatcher.group(2)?.toIntOrNull() ?: defaultPdt.month
            val d = dateMatcher.group(3)?.toIntOrNull() ?: defaultPdt.day
            Triple(y, m, d)
        } else null

        val extractedTime = if (timeMatcher.find()) {
            timeMatcher.group(1)
        } else null

        val finalDateText = if (extractedDate != null) {
            String.format("%04d/%02d/%02d", extractedDate.first, extractedDate.second, extractedDate.third)
        } else {
            defaultPdt.toFormattedDate()
        }

        val finalTimeText = extractedTime ?: defaultPdt.toFormattedTime()

        val finalTimestamp = if (extractedDate != null) {
            val (h, min) = if (extractedTime != null && extractedTime.contains(":")) {
                val parts = extractedTime.split(":")
                Pair(parts[0].toIntOrNull() ?: 12, parts[1].toIntOrNull() ?: 0)
            } else {
                Pair(defaultPdt.hour, defaultPdt.minute)
            }
            try {
                PersianCalendarHelper.jalaliToEpochMillis(
                    extractedDate.first,
                    extractedDate.second,
                    extractedDate.third,
                    h,
                    min
                )
            } catch (e: Exception) {
                timestampMillis
            }
        } else {
            timestampMillis
        }

        return Triple(finalDateText, finalTimeText, finalTimestamp)
    }

    private fun extractAccounts(cleanBody: String, defaultBankName: String): Pair<String?, String?> {
        val cardPattern = Pattern.compile("""(?i)(?:کارت|card)[\s:]*([0-9*#-]{4,19})""")
        val cardMatcher = cardPattern.matcher(cleanBody)
        val foundCards = mutableListOf<String>()

        while (cardMatcher.find()) {
            val card = cardMatcher.group(1)?.trim()
            if (!card.isNullOrBlank() && card.length >= 4) {
                foundCards.add("کارت $card")
            }
        }

        val sourceAccount = if (foundCards.isNotEmpty()) foundCards.first() else defaultBankName
        val destAccount = if (foundCards.size > 1) foundCards[1] else null

        return Pair(sourceAccount, destAccount)
    }
}
