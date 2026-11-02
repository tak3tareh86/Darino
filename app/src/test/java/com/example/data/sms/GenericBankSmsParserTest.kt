package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GenericBankSmsParserTest {

    @Test
    fun recognizesCompactMultilineWithdrawalWithoutSavedTemplateOrKnownSender() {
        val body = """حساب4785914725
برداشت50,000
مانده7,963,304
05/08/11-06:53"""

        val parsed = GenericBankSmsParser.parse("sms-1", "30001234", body, 1_700_000_000_000L)

        assertNotNull(parsed)
        assertEquals("بانک ناشناس", parsed?.bankName)
        assertEquals(TransactionType.EXPENSE, parsed?.type)
        assertEquals(50_000L, parsed?.amount)
        assertEquals(7_963_304L, parsed?.balanceAmount)
        assertEquals("06:53", parsed?.timeText)
        assertEquals("حساب/کارت 4785914725", parsed?.sourceAccount)
        assertEquals("", parsed?.patternId)
    }

    @Test
    fun recognizesPersianAndEnglishBankNamesAndRialAmounts() {
        val persian = GenericBankSmsParser.parse(
            "sms-2", "30001234",
            "بانک ملت: واریز 120,000 ریال به حساب شما",
            1_700_000_000_000L
        )
        val english = GenericBankSmsParser.parse(
            "sms-3", "MELLAT",
            "Deposit 250,000 toman to your account",
            1_700_000_000_000L
        )

        assertEquals("بانک ملت", persian?.bankName)
        assertEquals(TransactionType.INCOME, persian?.type)
        assertEquals(12_000L, persian?.amount)
        assertEquals("بانک ملت", english?.bankName)
        assertEquals(TransactionType.INCOME, english?.type)
        assertEquals(250_000L, english?.amount)
    }

    @Test
    fun classifiesTransfersSeparatelyFromDepositsAndWithdrawals() {
        val parsed = GenericBankSmsParser.parse(
            "sms-4", "30001234",
            "انتقال وجه مبلغ 25,000 تومان با موفقیت انجام شد",
            1_700_000_000_000L
        )

        assertEquals(TransactionType.TRANSFER, parsed?.type)
        assertEquals(25_000L, parsed?.amount)
    }

    @Test
    fun rejectsAmbiguousAmountsOtpAndFailedTransactions() {
        assertNull(
            GenericBankSmsParser.parse(
                "sms-5", "30001234",
                "برداشت 1,000 واریز 2,000",
                1_700_000_000_000L
            )
        )
        assertNull(
            GenericBankSmsParser.parse(
                "sms-6", "30001234",
                "رمز پویا جهت خرید 50,000 تومان: 123456",
                1_700_000_000_000L
            )
        )
        assertNull(
            GenericBankSmsParser.parse(
                "sms-7", "30001234",
                "برداشت 50,000 تومان ناموفق بود",
                1_700_000_000_000L
            )
        )
    }

    @Test
    fun normalizesPersianDigitsAndArabicLetterVariantsInGenericParser() {
        val parsed = GenericBankSmsParser.parse(
            "sms-8", "MELLAT",
            "برداشت۵۰٬۰۰۰ تومان از حساب",
            1_700_000_000_000L
        )

        assertEquals(TransactionType.EXPENSE, parsed?.type)
        assertEquals(50_000L, parsed?.amount)
        assertEquals("بانک ملت", parsed?.bankName)
    }
}
