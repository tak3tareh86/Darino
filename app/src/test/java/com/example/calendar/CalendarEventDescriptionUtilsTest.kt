package com.example.calendar

import com.example.calendar.domain.CalendarEventDescriptionUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarEventDescriptionUtilsTest {
    @Test
    fun removesDuplicatePersianDigitAmountAndItsLabelButKeepsDescription() {
        val result = CalendarEventDescriptionUtils.removeDuplicateAmount(
            description = "مبلغ چک: ۱٬۲۳۴٬۵۰۰ ریال بابت تعمیر خودرو",
            amount = 1_234_500L
        )

        assertEquals("بابت تعمیر خودرو", result)
    }

    @Test
    fun removesAmountWithCurrencyAndKeepsSurroundingText() {
        val result = CalendarEventDescriptionUtils.removeDuplicateAmount(
            description = "پرداخت قسط 1,234,500 تومان برای قرارداد",
            amount = 1_234_500L
        )

        assertEquals("پرداخت برای قرارداد", result)
    }

    @Test
    fun doesNotRemoveUnrelatedNumberOrDate() {
        val description = "جلسه در تاریخ ۱۴۰۵/۰۸/۱۰، شماره پرونده ۱۲۳۴"
        assertEquals(
            description,
            CalendarEventDescriptionUtils.removeDuplicateAmount(description, 1_234L)
        )
    }

    @Test
    fun preservesDescriptionWhenAmountIsNotPresent() {
        val description = "هزینه تعمیر خودرو"
        assertEquals(
            description,
            CalendarEventDescriptionUtils.removeDuplicateAmount(description, 1_234L)
        )
    }
}
