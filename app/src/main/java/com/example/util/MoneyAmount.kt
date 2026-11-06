package com.example.util

import com.example.ui.screens.settings.model.AppCurrency
import java.math.BigDecimal

/**
 * Exact monetary value paired with its explicit Iranian unit.
 *
 * Toman and Rial are units of the same currency (ISO 4217: IRR), not
 * independently exchangeable currencies. Conversion is therefore an exact
 * scale conversion: 1 Toman = 10 Rials.
 *
 * BigDecimal is used so a Rial amount can be represented exactly in Toman
 * (for example, 1 Rial = 0.1 Toman) without truncation or floating point.
 */
data class MoneyAmount(
    val value: BigDecimal,
    val unit: AppCurrency
) {
    init {
        require(value.multiply(if (unit == AppCurrency.TOMAN) BigDecimal.TEN else BigDecimal.ONE)
            .stripTrailingZeros().scale() <= 0) {
            "Money precision cannot be smaller than one Rial."
        }
    }

    /** ISO 4217 identifies the underlying currency, independently of its unit. */
    val iso4217Code: String
        get() = ISO_4217_CODE

    /**
     * Convert this value to [targetUnit] without rounding or losing precision.
     * Division by ten is terminating in base ten, so the conversion is exact.
     */
    fun inUnit(targetUnit: AppCurrency): MoneyAmount {
        if (unit == targetUnit) return this
        val converted = when (unit) {
            AppCurrency.TOMAN -> value.multiply(BigDecimal.TEN)
            AppCurrency.RIAL -> value.divide(BigDecimal.TEN)
        }
        return MoneyAmount(converted, targetUnit)
    }

    /** Exact value expressed in Rial, the integer subdivision of IRR. */
    fun asRials(): BigDecimal = when (unit) {
        AppCurrency.TOMAN -> value.multiply(BigDecimal.TEN)
        AppCurrency.RIAL -> value
    }

    /**
     * Returns the exact Rial integer for persistence in integer-minor-unit stores.
     * Throws rather than rounding or overflowing when the value cannot be represented.
     */
    fun asRialsLongExact(): Long = asRials().longValueExact()

    /** Exact comparison across units; no implicit floating-point conversion. */
    fun compareValue(other: MoneyAmount): Int = asRials().compareTo(other.asRials())

    operator fun plus(other: MoneyAmount): MoneyAmount {
        require(unit == other.unit) {
            "Money values must use the same unit before arithmetic; convert explicitly."
        }
        return copy(value = value.add(other.value))
    }

    operator fun minus(other: MoneyAmount): MoneyAmount {
        require(unit == other.unit) {
            "Money values must use the same unit before arithmetic; convert explicitly."
        }
        return copy(value = value.subtract(other.value))
    }

    companion object {
        const val ISO_4217_CODE = "IRR"

        fun fromRials(value: BigDecimal, targetUnit: AppCurrency): MoneyAmount {
            val rialAmount = MoneyAmount(value, AppCurrency.RIAL)
            return rialAmount.inUnit(targetUnit)
        }

        fun of(value: Long, unit: AppCurrency): MoneyAmount =
            MoneyAmount(BigDecimal.valueOf(value), unit)

        fun of(value: BigDecimal, unit: AppCurrency): MoneyAmount =
            MoneyAmount(value, unit)

        fun fromRials(value: Long, targetUnit: AppCurrency): MoneyAmount =
            fromRials(BigDecimal.valueOf(value), targetUnit)
    }
}
