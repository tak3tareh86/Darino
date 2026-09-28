package com.example.data.security

/**
 * Validation rules and utilities for PIN and Pattern input.
 */
object SecurityValidators {

    fun validatePin(pin: String): PinValidationResult {
        if (pin.length != 4 && pin.length != 6) {
            return PinValidationResult.Invalid("طول رمز باید ۴ یا ۶ رقم باشد.")
        }
        if (!pin.all { it.isDigit() }) {
            return PinValidationResult.Invalid("رمز عبور فقط باید شامل ارقام باشد.")
        }

        // Check repetitive sequences like "0000" or "1111"
        if (pin.toSet().size == 1) {
            return PinValidationResult.Weak("رمز بسیار ساده است (تکرار یک رقم). لطفا رمز غیرقابل حدس انتخاب کنید.")
        }

        return PinValidationResult.Valid
    }

    fun validatePattern(points: List<Int>): PatternValidationResult {
        if (points.size < 4) {
            return PatternValidationResult.TooShort("الگو باید حداقل ۴ نقطه را به هم متصل کند.")
        }
        return PatternValidationResult.Valid
    }
}

sealed interface PinValidationResult {
    data object Valid : PinValidationResult
    data class Weak(val warning: String) : PinValidationResult
    data class Invalid(val message: String) : PinValidationResult
}

sealed interface PatternValidationResult {
    data object Valid : PatternValidationResult
    data class TooShort(val message: String) : PatternValidationResult
    data class Invalid(val message: String) : PatternValidationResult
}
