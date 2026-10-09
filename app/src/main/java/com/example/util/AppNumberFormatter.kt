package com.example.util

import androidx.compose.runtime.compositionLocalOf
import com.example.ui.screens.settings.model.NumberDisplayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

val LocalNumberDisplayMode = compositionLocalOf { NumberDisplayMode.PERSIAN }

/**
 * Central Number & Digit Formatter for Darino
 * Coordinates global display of numbers (Persian: ۰۱۲۳۴۵۶۷۸۹ vs English: 0123456789)
 * across all financial, calendar, reminder, vehicle, and UI components.
 */
object AppNumberFormatter {

    @Volatile
    var currentMode: NumberDisplayMode = NumberDisplayMode.PERSIAN
        set(value) {
            field = value
            _modeFlow.value = value
            IranianPhoneUtils.activeNumberDisplayMode = value
            MoneyFormatter.activeNumberMode = value
        }

    private val _modeFlow = MutableStateFlow(NumberDisplayMode.PERSIAN)
    val modeFlow: StateFlow<NumberDisplayMode> = _modeFlow.asStateFlow()

    fun setMode(mode: NumberDisplayMode) {
        currentMode = mode
    }

    /**
     * Formats all digits in any input string according to the active [NumberDisplayMode].
     * If [NumberDisplayMode.PERSIAN], replaces ASCII 0-9 and Arabic ٠-٩ with Persian ۰-۹.
     * If [NumberDisplayMode.ENGLISH], replaces Persian ۰-۹ and Arabic ٠-٩ with ASCII 0-9.
     */
    fun formatDigits(input: String, mode: NumberDisplayMode = currentMode): String {
        return when (mode) {
            NumberDisplayMode.PERSIAN -> IranianPhoneUtils.forceConvertDigitsToPersian(input)
            NumberDisplayMode.ENGLISH -> IranianPhoneUtils.convertDigitsToEnglish(input)
        }
    }

    /**
     * Formats a Long with 3-digit comma separation and selected digit script.
     * e.g. 1500000 -> "۱,۵۰۰,۰۰۰" or "1,500,000"
     */
    fun formatNumber(number: Long, mode: NumberDisplayMode = currentMode): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ',' }
        val formatted = DecimalFormat("#,###", symbols).format(number)
        return formatDigits(formatted, mode)
    }

    fun formatNumber(number: Int, mode: NumberDisplayMode = currentMode): String {
        return formatNumber(number.toLong(), mode)
    }

    fun formatNumber(number: Double, decimals: Int = 2, mode: NumberDisplayMode = currentMode): String {
        val pattern = if (decimals > 0) "#,##0." + "0".repeat(decimals) else "#,##0"
        val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ',' }
        val formatted = DecimalFormat(pattern, symbols).format(number)
        return formatDigits(formatted, mode)
    }

    /**
     * Formats percentages with the appropriate sign and digits.
     * e.g. 85 -> "۸۵٪" (Persian) or "85%" (English)
     */
    fun formatPercent(percent: Number, mode: NumberDisplayMode = currentMode): String {
        val formatted = percent.toInt().toString()
        val digits = formatDigits(formatted, mode)
        return if (mode == NumberDisplayMode.PERSIAN) "$digits٪" else "$digits%"
    }
}
