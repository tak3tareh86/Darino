package com.example.calendar

import com.example.calendar.data.FinancialEventEntity
import com.example.ui.screens.settings.model.AppCurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinancialEventCurrencyUnitTest {
    @Test
    fun preservesRialUnitForStoredCalendarAmounts() {
        val event = FinancialEventEntity(
            id = "rial-event",
            userId = "user",
            title = "هزینه",
            type = "EXPENSE",
            amount = 1_234L,
            date = "1405/08/10",
            amountUnit = AppCurrency.RIAL.name
        ).toDomain()

        assertEquals(1_234L, event.amount)
        assertEquals(AppCurrency.RIAL, event.amountUnit)
    }

    @Test
    fun unknownStoredUnitDoesNotExposeAmountAsToman() {
        val event = FinancialEventEntity(
            id = "unknown-unit-event",
            userId = "user",
            title = "هزینه",
            type = "EXPENSE",
            amount = 1_234L,
            date = "1405/08/10",
            amountUnit = "UNKNOWN"
        ).toDomain()

        assertNull(event.amount)
    }
}
