package com.example.util

import com.example.ui.screens.settings.model.AppCurrency
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertNotNull
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
    fun exactRialStorageConversionDoesNotRoundOrOverflow() {
        assertEquals(1234L, MoneyAmount(BigDecimal("123.4"), AppCurrency.TOMAN).asRialsLongExact())
        assertThrows(ArithmeticException::class.java) {
            MoneyAmount.of(Long.MAX_VALUE, AppCurrency.TOMAN).asRialsLongExact()
        }
    }

    @Test
    fun comparisonPreservesValueAcrossDifferentUnits() {
        assertEquals(0, MoneyAmount.of(10L, AppCurrency.TOMAN)
            .compareValue(MoneyAmount.of(100L, AppCurrency.RIAL)))
        assertEquals(-1, MoneyAmount.of(99L, AppCurrency.RIAL)
            .compareValue(MoneyAmount.of(10L, AppCurrency.TOMAN)))
    }

    @Test
    fun decimalParserPreservesFractionalToman() {
        val parsed = IranianAmountUtils.parseAmountToBigDecimal("۱۲۳٫۴")
        assertNotNull(parsed)
        assertEquals(0, BigDecimal("123.4").compareTo(parsed!!))
    }

    @Test
    fun decimalParserAcceptsGroupedEnglishInput() {
        val parsed = IranianAmountUtils.parseAmountToBigDecimal("1,234.50")
        assertNotNull(parsed)
        assertEquals(0, BigDecimal("1234.50").compareTo(parsed!!))
    }

    @Test
    fun decimalParserRejectsMalformedValues() {
        assertEquals(null, IranianAmountUtils.parseAmountToBigDecimal("1.2.3"))
        assertEquals(null, IranianAmountUtils.parseAmountToBigDecimal("۱۲۳٫"))
        assertEquals(null, IranianAmountUtils.parseAmountToBigDecimal("1,2"))
        assertEquals(null, IranianAmountUtils.parseAmountToBigDecimal("12,34"))
        assertEquals(null, IranianAmountUtils.parseAmountToBigDecimal("1,234,56"))
        assertEquals(null, IranianAmountUtils.parseAmountToBigDecimal("1,234 567"))
        assertEquals(null, IranianAmountUtils.parseAmountToBigDecimal("12abc"))
    }

    @Test
    fun decimalParserAcceptsPersianDigitsAndArabicGrouping() {
        val parsed = IranianAmountUtils.parseAmountToBigDecimal("۱٬۲۳۴٫۵")
        assertNotNull(parsed)
        assertEquals(0, BigDecimal("1234.5").compareTo(parsed!!))
    }

    @Test
    fun decimalParserAcceptsProperSpaceGrouping() {
        val parsed = IranianAmountUtils.parseAmountToBigDecimal("1 234 567.8")
        assertNotNull(parsed)
        assertEquals(0, BigDecimal("1234567.8").compareTo(parsed!!))
    }

    @Test
    fun decimalFormatterPreservesFractionalDigits() {
        assertEquals("۱۲۳,۴۵۶٫۷", IranianPhoneUtils.convertDigitsToPersian(
            IranianAmountUtils.formatDecimalWithCommas("123456.7").replace(".", "٫")
        ))
    }

    @Test
    fun decimalInputKeepsTrailingZerosWhileEditing() {
        assertEquals("۱۲٫۰", IranianAmountUtils.formatDecimalWithCommas("12.0"))
        assertEquals("۱۲٫۰۵", IranianAmountUtils.formatDecimalWithCommas("12.05"))
        assertEquals("۱۲٫", IranianAmountUtils.formatDecimalWithCommas("12."))
    }

    @Test
    fun valuesCannotRepresentFractionsSmallerThanOneRial() {
        assertThrows(IllegalArgumentException::class.java) {
            MoneyAmount(BigDecimal("0.01"), AppCurrency.TOMAN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MoneyAmount(BigDecimal("0.1"), AppCurrency.RIAL)
        }
    }

    @Test
    fun bothUnitsIdentifySameIsoCurrency() {
        assertEquals("IRR", MoneyAmount.of(1L, AppCurrency.TOMAN).iso4217Code)
        assertEquals("IRR", MoneyAmount.of(1L, AppCurrency.RIAL).iso4217Code)
        assertEquals("IRR", AppCurrency.TOMAN.code)
        assertEquals("IRR", AppCurrency.RIAL.code)
    }
}
