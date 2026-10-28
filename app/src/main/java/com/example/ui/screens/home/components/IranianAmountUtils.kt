package com.example.ui.screens.home.components

/**
 * Helpers for normalizing and displaying transaction amounts entered with
 * Persian, Arabic, or Latin digits.
 */
internal object IranianAmountUtils {
    fun cleanAmountDigits(value: String): String = buildString {
        for (char in value) {
            when (char) {
                in '۰'..'۹' -> append(('0'.code + (char.code - '۰'.code)).toChar())
                in '٠'..'٩' -> append(('0'.code + (char.code - '٠'.code)).toChar())
                in '0'..'9' -> append(char)
            }
        }
    }

    fun formatWithCommas(value: String): String {
        val digits = cleanAmountDigits(value)
        if (digits.isEmpty()) return ""
        return digits.reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
    }
}
