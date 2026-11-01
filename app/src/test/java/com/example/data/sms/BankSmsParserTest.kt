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
    fun clearlyLabeledPositiveSignedAmountPassesBankEvidence() {
        assertEquals(
            true,
            BankSmsParser.hasValidBankEvidence(
                "20008434",
                "مبلغ تراکنش: +۱۲۵,۰۰۰ ریال"
            )
        )
    }

    @Test
    fun clearlyLabeledNegativeSignedAmountPassesBankEvidence() {
        assertEquals(
            true,
            BankSmsParser.hasValidBankEvidence(
                "20008434",
                "مبلغ تراکنش: -۱۲۵,۰۰۰ ریال"
            )
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
    fun senderShortcodeIdentifiesBankWithoutNameInBody() {
        assertEquals(
            "بانک سامان",
            BankSmsParser.detectBankName("20008434", "واریز وجه به حساب شما")
        )
    }

    @Test
    fun bankNameInBodyIdentifiesBankWithUnknownSender() {
        assertEquals(
            "بانک سامان",
            BankSmsParser.detectBankName("unknown-sender", "بانک سامان؛ واریز وجه")
        )
    }

    @Test
    fun conflictingSenderAndBodyBankIdentityIsRejected() {
        assertEquals(
            false,
            BankSmsParser.hasValidBankEvidence(
                "20004000",
                "بانک سامان برداشت از حساب، مبلغ تراکنش: ۱,۰۰۰ ریال، مانده: ۲,۰۰۰ ریال"
            )
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
