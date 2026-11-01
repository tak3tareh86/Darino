package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BankSmsParserTest {

    @Test
    fun positiveSignedAmountIsIncome() {
        assertEquals(
            TransactionType.INCOME,
            BankSmsParser.detectTransactionType("مبلغ تراکنش: +۱,۲۵۰,۰۰۰ ریال")
        )
    }

    @Test
    fun negativeSignedAmountIsExpense() {
        assertEquals(
            TransactionType.EXPENSE,
            BankSmsParser.detectTransactionType("مبلغ تراکنش: -۱,۲۵۰,۰۰۰ ریال")
        )
    }

    @Test
    fun signedAccountBalanceAloneIsNotATransactionDirection() {
        assertNull(
            BankSmsParser.detectTransactionType("مانده حساب: +۱,۲۵۰,۰۰۰ ریال")
        )
    }

    @Test
    fun conflictingExplicitDirectionAndSignIsRejected() {
        assertNull(
            BankSmsParser.detectTransactionType("برداشت از حساب، مبلغ تراکنش: +۱,۲۵۰,۰۰۰ ریال")
        )
    }

    @Test
    fun colloquialBlueBankOutgoingPhraseIsExpense() {
        assertEquals(
            TransactionType.EXPENSE,
            BankSmsParser.detectTransactionType("۱۲۵,۰۰۰ ریال از حساب شما پرید")
        )
    }

    @Test
    fun colloquialBlueBankIncomingPhraseIsIncome() {
        assertEquals(
            TransactionType.INCOME,
            BankSmsParser.detectTransactionType("۱۲۵,۰۰۰ ریال به حساب شما نشست")
        )
    }
}
