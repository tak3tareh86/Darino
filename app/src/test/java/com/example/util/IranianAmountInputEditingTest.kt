package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class IranianAmountInputEditingTest {

    @Test
    fun deletingDigitFromIncompleteThousandsGroupKeepsRemainingDigits() {
        assertEquals("1234", IranianAmountUtils.cleanDecimalAmountInputWhileEditing("۱,۲۳۴"))
        assertEquals("123", IranianAmountUtils.cleanDecimalAmountInputWhileEditing("۱,۲۳"))
    }

    @Test
    fun typingPastThousandsSeparatorDoesNotClearAmount() {
        assertEquals("12345", IranianAmountUtils.cleanDecimalAmountInputWhileEditing("۱۲,۳۴۵"))
        assertEquals("123456", IranianAmountUtils.cleanDecimalAmountInputWhileEditing("۱۲,۳۴۵۶"))
    }

    @Test
    fun editingKeepsDecimalFractionAndTrailingSeparator() {
        assertEquals("1234.5", IranianAmountUtils.cleanDecimalAmountInputWhileEditing("۱,۲۳۴٫۵"))
        assertEquals("1234.", IranianAmountUtils.cleanDecimalAmountInputWhileEditing("۱,۲۳۴."))
    }

    @Test
    fun editingStillRejectsInvalidCharactersAndMultipleDecimalSeparators() {
        assertEquals("", IranianAmountUtils.cleanDecimalAmountInputWhileEditing("12a34"))
        assertEquals("", IranianAmountUtils.cleanDecimalAmountInputWhileEditing("1.2.3"))
    }
}
