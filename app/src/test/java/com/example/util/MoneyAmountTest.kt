package com.example.util

import com.example.ui.screens.settings.model.AppCurrency
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyAmountTest {

    @Test
    fun oneRialConvertsToOneTenthTomanWithoutLosingValue() {
        val rial = MoneyAmount.of(1L, AppCurrency.RIAL)
        val toman = rial.inUnit(AppCurrency.TOMAN)
        assertEquals(0, BigDecimal("0.1").compareTo(toman.value))
        assertEquals(0, rial.asRials().compareTo(toman.asRials()))
    }

    @Test
    fun fractionalTomanConvertsBackToExactRialAmount() {
        val toman = MoneyAmount(BigDecimal("123.4"), AppCurrency.TOMAN)
        val rial = toman.inUnit(AppCurrency.RIAL)
        assertEquals(0, BigDecimal("1234").compareTo(rial.value))
        assertEquals(0, toman.asRials().compareTo(rial.asRials()))
    }

    @Test
    fun unitRoundTripPreservesRialPrecision() {
        val original = MoneyAmount(BigDecimal("987654321"), AppCurrency.RIAL)
        val roundTrip = original.inUnit(AppCurrency.TOMAN).inUnit(AppCurrency.RIAL)
        assertEquals(0, original.value.compareTo(roundTrip.value))
    }

    @Test
    fun arithmeticRequiresExplicitMatchingUnits() {
        val toman = MoneyAmount.of(10L, AppCurrency.TOMAN)
        val rial = MoneyAmount.of(100L, AppCurrency.RIAL)
        assertThrows(IllegalArgumentException::class.java) { toman + rial }
    }

    @Test
    fun bothUnitsIdentifySameIsoCurrency() {
        assertEquals("IRR", MoneyAmount.of(1L, AppCurrency.TOMAN).iso4217Code)
        assertEquals("IRR", MoneyAmount.of(1L, AppCurrency.RIAL).iso4217Code)
    }
}
