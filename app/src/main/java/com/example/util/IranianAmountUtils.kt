package com.example.util

import java.math.BigDecimal
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
    fun formatWithCommas(
        rawDigits: String,
        inPersian: Boolean = (AppNumberFormatter.currentMode == com.example.ui.screens.settings.model.NumberDisplayMode.PERSIAN)
    ): String {
        val clean = cleanAmountDigits(rawDigits)
        if (clean.isEmpty()) return ""
        val parsed = clean.toLongOrNull() ?: return clean
        
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedEnglish = formatter.format(parsed)
        
        val isPersian = inPersian && (AppNumberFormatter.currentMode == com.example.ui.screens.settings.model.NumberDisplayMode.PERSIAN)
        return if (isPersian) {
            IranianPhoneUtils.convertDigitsToPersian(formattedEnglish)
        } else {
            IranianPhoneUtils.convertDigitsToEnglish(formattedEnglish)
        }
    }

    /**
     * Safely parses an amount string (which might contain commas, Persian digits, spaces) to Long.
     */
    fun parseAmountToLong(input: String): Long {
        val clean = cleanAmountDigits(input)
        return clean.toLongOrNull() ?: 0L
    }

    /** Keeps a trailing decimal separator while the user is typing. */
    fun cleanDecimalAmountInput(input: String, maxDigits: Int = 30): String {
        val normalized = IranianPhoneUtils.convertDigitsToEnglish(input)
        val output = StringBuilder()
        var decimalSeen = false
        var digitsSeen = 0
        normalized.forEach { char ->
            when {
                char in '0'..'9' && digitsSeen < maxDigits -> { output.append(char); digitsSeen++ }
                (char == '.' || char == '٫') && !decimalSeen -> {
                    if (output.isEmpty()) output.append('0')
                    output.append('.')
                    decimalSeen = true
                }
            }
        }
        val raw = output.toString()
        val integer = raw.substringBefore('.').trimStart('0').ifEmpty { "0" }
        return if (decimalSeen) integer + "." + raw.substringAfter('.', "") else integer
    }
    /**
     * Parses a non-negative decimal amount without losing precision.
     *
     * Accepts Persian/Arabic/Latin digits, comma or Arabic thousands separators,
     * and either "." or "٫" as the decimal separator. Commas are treated only
     * as grouping separators; malformed or multiple decimal separators are rejected.
     */
    fun parseAmountToBigDecimal(input: String): BigDecimal? {
        val normalizedDigits = IranianPhoneUtils.convertDigitsToEnglish(input)
        val compact = normalizedDigits
            .replace(",", "")
            .replace("٬", "")
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace("٫", ".")
        if (compact.isEmpty()) return null
        if (compact.count { it == '.' } > 1) return null
        if (compact.any { it != '.' && it !in '0'..'9' }) return null
        if (compact == "." || compact.endsWith(".")) return null
        val normalized = if (compact.startsWith(".")) "0$compact" else compact
        return runCatching { BigDecimal(normalized) }.getOrNull()
    }

    /**
     * Groups the integer part of a decimal amount while preserving every
     * fractional digit and applying the active numeral display mode.
     */
    fun formatDecimalWithCommas(input: String): String {
        val value = parseAmountToBigDecimal(input) ?: return ""
        val plain = value.stripTrailingZeros().toPlainString()
        val parts = plain.split('.', limit = 2)
        val grouped = parts[0].reversed().chunked(3).joinToString(",").reversed()
        val formatted = if (parts.size == 2) "$grouped.${parts[1]}" else grouped
        return AppNumberFormatter.formatDigits(formatted, AppNumberFormatter.currentMode)
    }

    /**
     * Formats an amount with currency unit for display.
     */
    fun formatDisplayAmount(amount: Long, unit: String? = null): String {
        return if (unit != null) {
            val formatted = formatWithCommas(amount.toString())
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
