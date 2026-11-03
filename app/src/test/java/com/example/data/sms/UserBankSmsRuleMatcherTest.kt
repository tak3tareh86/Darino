package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class UserBankSmsRuleMatcherTest {
    private fun rule(
        senders: List<String> = listOf("777383"),
        income: String = "واریز/نشست",
        expense: String = "برداشت/پرید",
        transfer: String = "انتقال/کارت_به_کارت"
    ) = UserBankSmsRule(
        bankName = "بانک آزمایشی",
        senderAddresses = senders,
        incomeMarkers = income,
        expenseMarkers = expense,
        transferMarkers = transfer
    )

    @Test fun numericSenderIgnoresSpacesAndPersianDigitGlyphsButRemainsExact() {
        assertTrue(UserBankSmsRuleMatcher.senderMatches("777383", " ۷۷۷ ۳۸۳ "))
        assertFalse(UserBankSmsRuleMatcher.senderMatches("777383", "777384"))
    }

    @Test fun textSenderTrimsEdgesButPreservesCase() {
        assertTrue(UserBankSmsRuleMatcher.senderMatches(" BankSMS ", "BankSMS"))
        assertFalse(UserBankSmsRuleMatcher.senderMatches("BankSMS", "banksms"))
    }

    @Test fun markerInputRequiresTwoTokensAndSlashSeparatorOnly() {
        assertEquals(listOf("برداشت", "پرید"), UserBankSmsRuleMatcher.parseMarkers("برداشت/پرید"))
        assertNull(UserBankSmsRuleMatcher.parseMarkers("برداشت"))
        assertNull(UserBankSmsRuleMatcher.parseMarkers("برداشت پرید"))
        assertNull(UserBankSmsRuleMatcher.parseMarkers("برداشت.پرید"))
    }

    @Test fun hyphenAndUnderscoreAreEquivalentAndWhitespaceInSmsIsIgnored() {
        val configured = rule(expense = "-/برداشت")
        val result = UserBankSmsRuleMatcher.match("عملیات برداشت انجام شد، مبلغ - ۵۰۰۰۰", "777383", listOf(configured))
        assertEquals(TransactionType.EXPENSE, result?.type)
    }

    @Test fun allMarkersMustAppearInTheSameSmsAndTypeMustBeUnambiguous() {
        val configured = rule()
        assertNull(UserBankSmsRuleMatcher.match("فقط برداشت انجام شد", "777383", listOf(configured)))
        assertEquals(
            TransactionType.EXPENSE,
            UserBankSmsRuleMatcher.match("برداشت انجام شد و از حساب پرید", "777383", listOf(configured))?.type
        )
        val conflicting = rule(income = "برداشت/نشست")
        assertNull(UserBankSmsRuleMatcher.match("برداشت نشست پرید", "777383", listOf(conflicting)))
    }
}
