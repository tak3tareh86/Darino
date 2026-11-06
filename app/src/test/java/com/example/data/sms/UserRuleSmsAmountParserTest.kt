package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.settings.model.AppCurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UserRuleSmsAmountParserTest {

    private val rule = UserBankSmsRule(
        id = "test-rule",
        bankName = "بانک آزمایشی",
        incomeMarkers = "واریز/نشست",
        expenseMarkers = "برداشت/کسر",
        transferMarkers = "انتقال/کارت"
    )

    @Test
    fun extractsTransactionAndBalanceFromThreeDigitGroupedNumbers() {
        val parsed = UserRuleSmsAmountParser.parse(
            smsId = "sms-1",
            sender = "30001234",
            body = "برداشت50,000 تومان از حساب؛ مانده7,963,304 تومان",
            receivedAtMillis = 1_700_000_000_000L,
            rule = rule,
            type = TransactionType.EXPENSE
        )

        assertNotNull(parsed)
        assertEquals(50_000L, parsed?.amount)
        assertEquals(7_963_304L, parsed?.balanceAmount)
        assertEquals(listOf(50_000L, 7_963_304L), parsed?.amountCandidates)
    }

    @Test
    fun recognizesPersianDigitsAndPersianThousandsSeparator() {
        val parsed = UserRuleSmsAmountParser.parse(
            smsId = "sms-2",
            sender = "30001234",
            body = "واریز ۱۲۵٬۰۰۰٬۰۰۰ ریال؛ موجودی ۱۳۰٬۰۰۰٬۰۰۰ ریال",
            receivedAtMillis = 1_700_000_000_000L,
            rule = rule,
            type = TransactionType.INCOME
        )

        assertNotNull(parsed)
        assertEquals(125_000_000L, parsed?.amount)
        assertEquals(130_000_000L, parsed?.balanceAmount)
        assertEquals(listOf(125_000_000L, 130_000_000L), parsed?.amountCandidates)
    }

    @Test
    fun detectsTransactionAndBalanceUnitsIndependently() {
        val parsed = UserRuleSmsAmountParser.parse(
            smsId = "sms-mixed-units",
            sender = "30001234",
            body = "برداشت ۱۲۳٬۴۵۶ ریال از حساب بانکی؛ مانده نهایی ۷۸۹٬۰۱۲ تومان",
            receivedAtMillis = 1_700_000_000_000L,
            rule = rule,
            type = TransactionType.EXPENSE
        )

        assertNotNull(parsed)
        assertEquals(AppCurrency.RIAL, parsed?.amountUnit)
        assertEquals(AppCurrency.TOMAN, parsed?.balanceAmountUnit)
    }

    @Test
    fun exposesAllGroupedAmountsWhenMessageContainsMoreThanTwo() {
        val parsed = UserRuleSmsAmountParser.parse(
            smsId = "sms-3",
            sender = "30001234",
            body = "مبلغ تراکنش 25,000؛ کارمزد 1,000؛ مانده 500,000",
            receivedAtMillis = 1_700_000_000_000L,
            rule = rule,
            type = TransactionType.EXPENSE
        )

        assertNotNull(parsed)
        assertEquals(listOf(25_000L, 1_000L, 500_000L), parsed?.amountCandidates)
        assertEquals(25_000L, parsed?.amount)
        assertEquals(500_000L, parsed?.balanceAmount)
    }
}
