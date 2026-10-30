package com.example.util

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.screens.settings.model.AppCurrency
import com.example.ui.screens.settings.model.AppLanguage
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val LocalAppCurrency = compositionLocalOf { AppCurrency.TOMAN }
val LocalAppLanguage = compositionLocalOf { AppLanguage.PERSIAN }

/**
 * Central Money Formatter for Darino
 * Dynamically formats amounts based on selected Currency (Toman / Rial)
 * and selected Language (Persian / English).
 */
object MoneyFormatter {

    // Snapshot state makes every composable that formats money react to preference changes.
    // This prevents UI elements from retaining strings formatted with the previous currency.
    private val _activeCurrencyFlow = MutableStateFlow(AppCurrency.TOMAN)
    val activeCurrencyFlow: StateFlow<AppCurrency> = _activeCurrencyFlow.asStateFlow()

    private var currencySnapshot by mutableStateOf(AppCurrency.TOMAN)
    var activeCurrency: AppCurrency
        get() = currencySnapshot
        set(value) {
            currencySnapshot = value
            _activeCurrencyFlow.value = value
        }

    var activeLanguage: AppLanguage by mutableStateOf(AppLanguage.PERSIAN)

    var isPrivacyModeEnabled: Boolean by mutableStateOf(false)
        private set
    fun setPrivacyModeEnabled(enabled: Boolean) { isPrivacyModeEnabled = enabled }
    private fun maskedAmount(includeUnit: Boolean, currency: AppCurrency, language: AppLanguage): String =
        if (includeUnit) "•••• ${getUnitLabel(currency, language)}" else "••••"

    var activeNumberMode: com.example.ui.screens.settings.model.NumberDisplayMode by
        mutableStateOf(com.example.ui.screens.settings.model.NumberDisplayMode.PERSIAN)

    fun toPersianDigits(input: String): String {
        return AppNumberFormatter.formatDigits(input, activeNumberMode)
    }

    fun getUnitLabel(currency: AppCurrency = activeCurrency, language: AppLanguage = activeLanguage): String {
        return when (currency) {
            AppCurrency.TOMAN -> if (language == AppLanguage.ENGLISH) "Toman" else "تومان"
            AppCurrency.RIAL -> if (language == AppLanguage.ENGLISH) "IRR" else "ریال"
        }
    }

    /**
     * Converts an amount provided in Toman to the active currency value.
     * 1 Toman = 10 Rials.
     */
    fun convertFromToman(amountInToman: Long, targetCurrency: AppCurrency = activeCurrency): Long {
        return when (targetCurrency) {
            AppCurrency.RIAL -> when {
                amountInToman > Long.MAX_VALUE / 10L -> Long.MAX_VALUE
                amountInToman < Long.MIN_VALUE / 10L -> Long.MIN_VALUE
                else -> amountInToman * 10L
            }
            AppCurrency.TOMAN -> amountInToman
        }
    }

    /**
     * Converts an amount entered in the source currency to the base unit Toman.
     * 10 Rials = 1 Toman.
     */
    fun convertToToman(amountInSource: Long, sourceCurrency: AppCurrency = activeCurrency): Long {
        return when (sourceCurrency) {
            AppCurrency.RIAL -> amountInSource / 10L
            AppCurrency.TOMAN -> amountInSource
        }
    }

    /**
     * Formats an amount with currency unit and language-specific numerals.
     * Example (Toman, Fa): 18000000 -> "۱۸,۰۰۰,۰۰۰ تومان"
     * Example (Rial, Fa):  18000000 -> "۱۸۰,۰۰۰,۰۰۰ ریال"
     * Example (Toman, En): 18000000 -> "18,000,000 Toman"
     */
    fun format(amount: Long, showSign: Boolean = false): String {
        if (isPrivacyModeEnabled) return maskedAmount(true, activeCurrency, activeLanguage)
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
        if (isPrivacyModeEnabled) return maskedAmount(includeUnit, currency, language)
        val convertedAmount = convertFromToman(amount, currency)
        val absAmount = kotlin.math.abs(convertedAmount)
        val formattedNumber = formatNumberWithCommas(absAmount)
        val displayFormatted = AppNumberFormatter.formatDigits(formattedNumber, activeNumberMode)
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
        if (isPrivacyModeEnabled) return maskedAmount(includeUnit, currency, language)
        val convertedAmount = convertFromToman(amount, currency)
        val absAmount = kotlin.math.abs(convertedAmount)
        val formattedNumber = formatNumberWithCommas(absAmount)
        val displayFormatted = AppNumberFormatter.formatDigits(formattedNumber, activeNumberMode)
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
        if (isPrivacyModeEnabled) return maskedAmount(false, currency, language)
        val convertedAmount = convertFromToman(amount, currency)
        val absAmount = kotlin.math.abs(convertedAmount)
        val formattedNumber = formatNumberWithCommas(absAmount)
        val displayFormatted = AppNumberFormatter.formatDigits(formattedNumber, activeNumberMode)
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
        if (isPrivacyModeEnabled) return maskedAmount(true, currency, language)
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
                val num = AppNumberFormatter.formatDigits(formatted, activeNumberMode)
                val suffix = if (language == AppLanguage.ENGLISH) "B" else "میلیارد"
                "$sign$num $suffix $unitShort"
            }
            abs >= 1_000_000L -> {
                val m = abs / 1_000_000.0
                val formatted = String.format(Locale.US, "%.1f", m).trimEnd('0').trimEnd('.')
                val num = AppNumberFormatter.formatDigits(formatted, activeNumberMode)
                val suffix = if (language == AppLanguage.ENGLISH) "M" else "م"
                "$sign$num $suffix $unitShort"
            }
            abs >= 1_000L -> {
                val k = abs / 1_000.0
                val formatted = String.format(Locale.US, "%.0f", k)
                val num = AppNumberFormatter.formatDigits(formatted, activeNumberMode)
                val suffix = if (language == AppLanguage.ENGLISH) "K" else "هـ"
                "$sign$num $suffix $unitShort"
            }
            else -> {
                val num = AppNumberFormatter.formatDigits(abs.toString(), activeNumberMode)
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
