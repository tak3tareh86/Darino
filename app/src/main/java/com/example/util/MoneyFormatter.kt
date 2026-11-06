package com.example.util

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.screens.settings.model.AppCurrency
import com.example.ui.screens.settings.model.AppLanguage
import java.math.BigDecimal
import java.math.RoundingMode
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

    private var privacyModeState: Boolean by mutableStateOf(false)
    val isPrivacyModeEnabled: Boolean
        get() = privacyModeState

    fun setPrivacyModeEnabled(enabled: Boolean) { privacyModeState = enabled }
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
     * Formats an exact money value after an explicit unit conversion.
     *
     * Unlike the legacy Long/Toman helpers, this API preserves fractional Toman
     * values (for example, 1 Rial = 0.1 Toman) and never uses floating point.
     */
    fun formatExact(
        amount: MoneyAmount,
        targetCurrency: AppCurrency = activeCurrency,
        includeUnit: Boolean = true,
        language: AppLanguage = activeLanguage
    ): String {
        if (isPrivacyModeEnabled) return maskedAmount(includeUnit, targetCurrency, language)

        val converted = amount.inUnit(targetCurrency).value
        val absolute = converted.abs().stripTrailingZeros()
        val plain = absolute.toPlainString()
        val parts = plain.split('.', limit = 2)
        val groupedInteger = parts[0].reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
        val exactNumber = if (parts.size == 2) "$groupedInteger.${parts[1]}" else groupedInteger
        val number = AppNumberFormatter.formatDigits(exactNumber, activeNumberMode)
        val sign = if (converted.signum() < 0) "−" else ""
        if (!includeUnit) return "$sign$number"
        return "$sign$number ${getUnitLabel(targetCurrency, language)}"
    }

    /**
     * Converts an amount provided in Toman to the active currency value.
     * 1 Toman = 10 Rials.
     */
    fun convertFromToman(amountInToman: Long, targetCurrency: AppCurrency = activeCurrency): Long {
        return MoneyAmount.of(amountInToman, AppCurrency.TOMAN).inUnit(targetCurrency).value.longValueExact()
    }

    /**
     * Converts an amount entered in the source currency to the base unit Toman.
     * 10 Rials = 1 Toman.
     */
    fun convertToToman(amountInSource: Long, sourceCurrency: AppCurrency = activeCurrency): Long {
        return MoneyAmount.of(amountInSource, sourceCurrency).inUnit(AppCurrency.TOMAN).value.longValueExact()
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
        return formatExact(MoneyAmount.of(amount, AppCurrency.TOMAN), currency, includeUnit, language)
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
        val absolute = BigDecimal.valueOf(amount).abs()
        val formatted = formatExact(MoneyAmount(absolute, AppCurrency.TOMAN), currency, includeUnit = false, language = language)
        val sign = if (isExpense || amount < 0L) "−" else "+"
        return if (includeUnit) "$sign$formatted ${getUnitLabel(currency, language)}" else "$sign$formatted"
    }

    /**
     * Formats an amount that is already expressed in the selected currency.
     * Unlike the Toman-based formatters, this method never converts or scales it.
     */
    fun formatRawAmount(
        amount: Long,
        includeUnit: Boolean = true,
        currency: AppCurrency = activeCurrency,
        language: AppLanguage = activeLanguage
    ): String {
        if (isPrivacyModeEnabled) return maskedAmount(includeUnit, currency, language)
        return formatExact(MoneyAmount.of(amount, currency), currency, includeUnit, language)
    }

    /**
     * Formats an already-selected-currency amount with an income/expense sign,
     * without converting between Rial and Toman.
     */
    fun formatRawSignedAmount(
        amount: Long,
        isExpense: Boolean = false,
        includeUnit: Boolean = true,
        currency: AppCurrency = activeCurrency,
        language: AppLanguage = activeLanguage
    ): String {
        if (isPrivacyModeEnabled) return maskedAmount(includeUnit, currency, language)
        val absolute = BigDecimal.valueOf(amount).abs()
        val formatted = formatExact(MoneyAmount(absolute, currency), currency, includeUnit = false, language = language)
        val sign = if (isExpense || amount < 0L) "−" else "+"
        return if (includeUnit) "$sign$formatted ${getUnitLabel(currency, language)}" else "$sign$formatted"
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
        return formatExact(MoneyAmount.of(amount, AppCurrency.TOMAN), currency, includeUnit = false, language = language)
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
        val converted = MoneyAmount.of(amount, AppCurrency.TOMAN).inUnit(currency).value
        val abs = converted.abs()
        val sign = if (converted.signum() < 0) "−" else ""
        val unitShort = if (currency == AppCurrency.RIAL) {
            if (language == AppLanguage.ENGLISH) "IRR" else "ریال"
        } else {
            if (language == AppLanguage.ENGLISH) "T" else "ت"
        }

        return when {
            abs >= BigDecimal.valueOf(1_000_000_000L) -> {
                val formatted = abs.divide(BigDecimal.valueOf(1_000_000_000L), 1, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString()
                val num = AppNumberFormatter.formatDigits(formatted, activeNumberMode)
                val suffix = if (language == AppLanguage.ENGLISH) "B" else "میلیارد"
                "$sign$num $suffix $unitShort"
            }
            abs >= BigDecimal.valueOf(1_000_000L) -> {
                val formatted = abs.divide(BigDecimal.valueOf(1_000_000L), 1, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString()
                val num = AppNumberFormatter.formatDigits(formatted, activeNumberMode)
                val suffix = if (language == AppLanguage.ENGLISH) "M" else "م"
                "$sign$num $suffix $unitShort"
            }
            abs >= BigDecimal.valueOf(1_000L) -> {
                val formatted = abs.divide(BigDecimal.valueOf(1_000L), 0, RoundingMode.HALF_UP).toPlainString()
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
