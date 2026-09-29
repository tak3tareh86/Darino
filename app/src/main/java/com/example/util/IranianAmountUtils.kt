package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Utility functions for amount inputs and formatting:
 * - Persian/Arabic/English digits normalization
 * - Automatic 3-digit comma separation (1,000,000 / ۱,۰۰۰,۰۰۰)
 * - Safe numeric parsing (to Long)
 * - Conversion of numeric amounts to Persian words (حروف فارسی)
 */
object IranianAmountUtils {

    /**
     * Extracts numeric digits from any mixed input (converting Persian/Arabic digits to 0-9),
     * removes non-digits (commas, spaces, letters), and limits max length.
     */
    fun cleanAmountDigits(input: String, maxLength: Int = 15): String {
        val englishStr = IranianPhoneUtils.convertDigitsToEnglish(input)
        val digits = englishStr.filter { it in '0'..'9' }
        if (digits.isEmpty()) return ""
        
        // Remove leading zeros if number has multiple digits (e.g. "0500" -> "500")
        val trimmed = digits.trimStart('0')
        val result = if (trimmed.isEmpty()) "0" else trimmed
        return if (result.length > maxLength) result.substring(0, maxLength) else result
    }

    /**
     * Formats a raw digit string or number with 3-digit comma grouping.
     * Example: "1250000" -> "1,250,000" (or in Persian: "۱,۲۵۰,۰۰۰")
     */
    fun formatWithCommas(rawDigits: String, inPersian: Boolean = true): String {
        val clean = cleanAmountDigits(rawDigits)
        if (clean.isEmpty()) return ""
        val parsed = clean.toLongOrNull() ?: return clean
        
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedEnglish = formatter.format(parsed)
        
        return if (inPersian) {
            IranianPhoneUtils.convertDigitsToPersian(formattedEnglish)
        } else {
            formattedEnglish
        }
    }

    /**
     * Safely parses an amount string (which might contain commas, Persian digits, spaces) to Long.
     */
    fun parseAmountToLong(input: String): Long {
        val clean = cleanAmountDigits(input)
        return clean.toLongOrNull() ?: 0L
    }

    /**
     * Formats an amount with currency unit for display.
     */
    fun formatDisplayAmount(amount: Long, unit: String? = null): String {
        return if (unit != null) {
            val formatted = formatWithCommas(amount.toString(), inPersian = true)
            "$formatted $unit"
        } else {
            MoneyFormatter.formatToman(amount, includeUnit = true)
        }
    }

    private val units = arrayOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه")
    private val teens = arrayOf("ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده")
    private val tens = arrayOf("", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    private val hundreds = arrayOf("", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
    private val thousands = arrayOf("", "هزار", "میلیون", "میلیارد", "تریلیون")

    /**
     * Converts an amount number to Persian words.
     * Example: 1500000 -> "یک میلیون و پانصد هزار تومان" (or "پانزده میلیون ریال")
     */
    fun amountToPersianWords(amount: Long, unit: String? = null): String {
        val effectiveUnit = unit ?: MoneyFormatter.getUnitLabel()
        val convertedAmount = if (unit == null) MoneyFormatter.convertFromToman(amount) else amount
        if (convertedAmount == 0L) return "صفر $effectiveUnit"
        if (convertedAmount < 0L) return "منفی " + amountToPersianWords(-convertedAmount, effectiveUnit)
        
        var num = convertedAmount
        val parts = mutableListOf<String>()
        var groupIndex = 0

        while (num > 0) {
            val group = (num % 1000).toInt()
            if (group != 0) {
                val groupWord = threeDigitsToWords(group)
                val unitName = thousands.getOrElse(groupIndex) { "" }
                val fullGroup = if (unitName.isNotEmpty()) "$groupWord $unitName" else groupWord
                parts.add(0, fullGroup)
            }
            num /= 1000
            groupIndex++
        }

        val resultWords = parts.joinToString(" و ")
        return if (effectiveUnit.isNotEmpty()) "$resultWords $effectiveUnit" else resultWords
    }

    private fun threeDigitsToWords(n: Int): String {
        val words = mutableListOf<String>()
        val h = n / 100
        val t = (n % 100) / 10
        val u = n % 10

        if (h > 0) words.add(hundreds[h])

        if (t == 1) {
            words.add(teens[u])
        } else {
            if (t > 1) words.add(tens[t])
            if (u > 0) words.add(units[u])
        }

        return words.joinToString(" و ")
    }
}
