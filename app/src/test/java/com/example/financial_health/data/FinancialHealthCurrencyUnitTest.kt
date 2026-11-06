package com.example.financial_health.data

import com.example.ui.screens.settings.model.AppCurrency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FinancialHealthCurrencyUnitTest {
    @Test
    fun acceptsSupportedStoredUnits() {
        assertEquals(AppCurrency.TOMAN, parseStoredMoneyUnit("TOMAN"))
        assertEquals(AppCurrency.RIAL, parseStoredMoneyUnit("RIAL"))
    }

    @Test
    fun rejectsUnknownStoredUnitInsteadOfAssumingToman() {
        assertThrows(IllegalArgumentException::class.java) {
            parseStoredMoneyUnit("UNKNOWN")
        }
    }
}
