package com.example.util

import androidx.compose.runtime.compositionLocalOf
import com.example.ui.screens.settings.model.AppCurrency
import com.example.ui.screens.settings.model.AppLanguage
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

val LocalAppCurrency = compositionLocalOf { AppCurrency.TOMAN }
val LocalAppLanguage = compositionLocalOf { AppLanguage.PERSIAN }

/**
 * Central Money Formatter for Darino
 * Dynamically formats amounts based on selected Currency (Toman / Rial)
 * and selected Language (Persian / English).
 */
object MoneyFormatter {

    @Volatile
    var activeCurrency: AppCurrency = AppCurrency.TOMAN

    @Volatile
    var activeLanguage: AppLanguage = AppLanguage.PERSIAN

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(input: String): String {
        val builder = java.lang.StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                builder.append(persianDigits[ch - '0'])
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    fun getUnitLabel(currency: AppCurrency = activeCurrency, language: AppLanguage = activeLanguage): String {
        return when (currency) {
            AppCurrency.TOMAN -> if (language == AppLanguage.ENGLISH) "Toman" else "تومان"
            AppCurrency.RIAL -> if (language == AppLanguage.ENGLISH) "IRR" else "ریال"
            AppCurrency.USD -> "$"
            AppCurrency.EUR -> "€"
        }
    }

    /**
     * Converts an amount provided in Toman to the active currency value.
     * 1 Toman = 10 Rials.
     */
    fun convertFromToman(amountInToman: Long, targetCurrency: AppCurrency = activeCurrency): Long {
        return when (targetCurrency) {
            AppCurrency.RIAL -> amountInToman * 10L
            else -> amountInToman
        }
    }

    /**
     * Formats an amount with currency unit and language-specific numerals.
     * Example (Toman, Fa): 18000000 -> "۱۸,۰۰۰,۰۰۰ تومان"
     * Example (Rial, Fa):  18000000 -> "۱۸۰,۰۰۰,۰۰۰ ریال"
     * Example (Toman, En): 18000000 -> "18,000,000 Toman"
     */
    fun format(amount: Long, showSign: Boolean = false): String {
        val formatted = formatToman(amount, includeUnit = true)
        return when {
            showSign && amount > 0 -> "+$formatted"
            else -> formatted
        }
    }

    fun formatToman(
        amount: Long,
        includeUnit: Boolean = true,
        currency: AppCurrency = activeCurrency,
        language: AppLanguage = activeLanguage
    ): String {
        val convertedAmount = convertFromToman(amount, currency)
        val absAmount = kotlin.math.abs(convertedAmount)
        val formattedNumber = formatNumberWithCommas(absAmount)
        val displayFormatted = if (language == AppLanguage.PERSIAN) toPersianDigits(formattedNumber) else formattedNumber
        val prefix = if (convertedAmount < 0) "−" else ""
        val unit = getUnitLabel(currency, language)

        return if (includeUnit) {
            if (language == AppLanguage.ENGLISH) {
                "$prefix$displayFormatted $unit"
            } else {
                "$prefix$displayFormatted $unit"
            }
        } else {
            "$prefix$displayFormatted"
        }
    }

    /**
     * Formats an amount with explicit sign:
     * e.g. for Income: "+۱۸,۰۰۰,۰۰۰ تومان" or "+۱۸۰,۰۰۰,۰۰۰ ریال"
     */
    fun formatSignedToman(
        amount: Long,
        isExpense: Boolean = false,
        includeUnit: Boolean = true,
        currency: AppCurrency = activeCurrency,
        language: AppLanguage = activeLanguage
    ): String {
        val convertedAmount = convertFromToman(amount, currency)
        val absAmount = kotlin.math.abs(convertedAmount)
        val formattedNumber = formatNumberWithCommas(absAmount)
        val displayFormatted = if (language == AppLanguage.PERSIAN) toPersianDigits(formattedNumber) else formattedNumber
        val sign = if (isExpense || convertedAmount < 0) "−" else "+"
        val unit = getUnitLabel(currency, language)

        return if (includeUnit) {
            "$sign$displayFormatted $unit"
        } else {
            "$sign$displayFormatted"
        }
    }

    /**
     * Formats raw number with commas.
     */
    fun formatTomanRaw(
        amount: Long,
        currency: AppCurrency = activeCurrency,
        language: AppLanguage = activeLanguage
    ): String {
        val convertedAmount = convertFromToman(amount, currency)
        val absAmount = kotlin.math.abs(convertedAmount)
        val formattedNumber = formatNumberWithCommas(absAmount)
        val displayFormatted = if (language == AppLanguage.PERSIAN) toPersianDigits(formattedNumber) else formattedNumber
        return if (convertedAmount < 0) "−$displayFormatted" else displayFormatted
    }

    /**
     * Formats compact amounts for small badges / charts:
     * e.g. 1,500,000 Toman -> "۱.۵ م ت" or "۱۵ م ریال"
     */
    fun formatCompactToman(
        amount: Long,
        currency: AppCurrency = activeCurrency,
        language: AppLanguage = activeLanguage
    ): String {
        val converted = convertFromToman(amount, currency)
        val abs = kotlin.math.abs(converted)
        val sign = if (converted < 0) "−" else ""
        val unitShort = if (currency == AppCurrency.RIAL) {
            if (language == AppLanguage.ENGLISH) "IRR" else "ریال"
        } else {
            if (language == AppLanguage.ENGLISH) "T" else "ت"
        }

        return when {
            abs >= 1_000_000_000L -> {
                val b = abs / 1_000_000_000.0
                val formatted = String.format(Locale.US, "%.1f", b).trimEnd('0').trimEnd('.')
                val num = if (language == AppLanguage.PERSIAN) toPersianDigits(formatted) else formatted
                val suffix = if (language == AppLanguage.ENGLISH) "B" else "میلیارد"
                "$sign$num $suffix $unitShort"
            }
            abs >= 1_000_000L -> {
                val m = abs / 1_000_000.0
                val formatted = String.format(Locale.US, "%.1f", m).trimEnd('0').trimEnd('.')
                val num = if (language == AppLanguage.PERSIAN) toPersianDigits(formatted) else formatted
                val suffix = if (language == AppLanguage.ENGLISH) "M" else "م"
                "$sign$num $suffix $unitShort"
            }
            abs >= 1_000L -> {
                val k = abs / 1_000.0
                val formatted = String.format(Locale.US, "%.0f", k)
                val num = if (language == AppLanguage.PERSIAN) toPersianDigits(formatted) else formatted
                val suffix = if (language == AppLanguage.ENGLISH) "K" else "هـ"
                "$sign$num $suffix $unitShort"
            }
            else -> {
                val num = if (language == AppLanguage.PERSIAN) toPersianDigits(abs.toString()) else abs.toString()
                "$sign$num $unitShort"
            }
        }
    }

    private fun formatNumberWithCommas(number: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        symbols.groupingSeparator = ','
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(number)
    }
}
