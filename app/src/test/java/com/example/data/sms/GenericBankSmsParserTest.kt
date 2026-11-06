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

        val parsed = GenericBankSmsParser.parse("sms-1", "30001234", body, com.example.util.PersianCalendarHelper.jalaliToEpochMillis(1405, 8, 11, 12, 0))

        assertNotNull(parsed)
        assertEquals("بانک ناشناس", parsed?.bankName)
        assertEquals(TransactionType.EXPENSE, parsed?.type)
        assertEquals(50_000L, parsed?.amount)
        assertEquals(7_963_304L, parsed?.balanceAmount)
        assertEquals(listOf(50_000L, 7_963_304L), parsed?.amountCandidates)
        assertEquals("1405/08/11", parsed?.dateText)
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
        assertEquals(120_000L, persian?.amount)
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

        val cardToCard = GenericBankSmsParser.parse(
            "sms-11", "30001234",
            "کارت به کارت 35,000 تومان",
            1_700_000_000_000L
        )
        assertEquals(TransactionType.TRANSFER, cardToCard?.type)
        assertEquals(35_000L, cardToCard?.amount)

        val linkedTransfer = GenericBankSmsParser.parse(
            "sms-13", "30001234",
            "کارت به کارت از کارت 6037991111111111 به کارت 6037992222222222 مبلغ 25,000 تومان",
            1_700_000_000_000L
        )
        assertEquals("حساب/کارت 6037991111111111", linkedTransfer?.sourceAccount)
        assertEquals("حساب/کارت 6037992222222222", linkedTransfer?.destinationAccount)
    }

    @Test
    fun keepsCurrencyUnitsForEachAmountCandidateInMixedUnitSms() {
        val parsed = GenericBankSmsParser.parse(
            "sms-mixed-candidate-units",
            "30001234",
            "برداشت 123,456 ریال از حساب؛ مانده 789,012 تومان",
            1_700_000_000_000L
        )

        assertNotNull(parsed)
        assertEquals(123_456L, parsed?.amount)
        assertEquals(789_012L, parsed?.balanceAmount)
        assertEquals(
            listOf(com.example.ui.screens.settings.model.AppCurrency.RIAL, com.example.ui.screens.settings.model.AppCurrency.TOMAN),
            parsed?.amountCandidateUnits
        )
    }

    @Test
    fun keepsUnitsDistinctWhenTransactionAndBalanceHaveTheSameNumber() {
        val parsed = GenericBankSmsParser.parse(
            "sms-same-amount-different-units",
            "30001234",
            "برداشت 1,234 ریال از حساب؛ مانده 1,234 تومان",
            1_700_000_000_000L
        )

        assertNotNull(parsed)
        assertEquals(1_234L, parsed?.amount)
        assertEquals(1_234L, parsed?.balanceAmount)
        assertEquals(
            listOf(
                com.example.ui.screens.settings.model.AppCurrency.RIAL,
                com.example.ui.screens.settings.model.AppCurrency.TOMAN
            ),
            parsed?.amountCandidateUnits
        )
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
        assertNull(
            GenericBankSmsParser.parse(
                "sms-12", "30001234",
                "برداشت مبلغ 1,000 بابت واریز قبلی",
                1_700_000_000_000L
            )
        )
    }

    @Test
    fun extractsUnlistedBankAndCreditInstitutionNamesFromMessageBody() {
        val bank = GenericBankSmsParser.parse(
            "sms-9", "30001234",
            "بانک نمونه واریز 15,000 تومان",
            1_700_000_000_000L
        )
        val institution = GenericBankSmsParser.parse(
            "sms-10", "30001234",
            "مؤسسه اعتباری نمونه برداشت 2,000 تومان",
            1_700_000_000_000L
        )

        assertEquals("بانک نمونه", bank?.bankName)
        assertEquals("مؤسسه اعتباری نمونه", institution?.bankName)
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

    @Test
    fun rejectsAnyMessageContainingPasswordKeywordRegardlessOfFollowingText() {
        val samples = listOf(
            "بانک ملت برداشت 25,000 تومان رمز",
            "بانک شهر برداشت 25,000 تومان رمز پویا",
            "برداشت 25,000 تومان - رمز خرید اینترنتی شما",
            "برداشت 25,000 تومان؛ لطفاً رمز ورود را وارد کنید"
        )

        samples.forEachIndexed { index, body ->
            assertNull(GenericBankSmsParser.parse("password-$index", "30001234", body, 1_700_000_000_000L))
        }
    }

    @Test
    fun recognizesBluBankDepositAndWithdrawalMessagesAndMapsBrandToSaman() {
        val receivedAt = com.example.util.PersianCalendarHelper.jalaliToEpochMillis(1405, 5, 27, 12, 0)
        val deposit = GenericBankSmsParser.parse(
            "blu-deposit", "30001234",
            """بلو
واریز پول
فرزاد عزیز، 26,000,000 ریال به حساب شما نشست.
موجودی: 26,000,000 ریال
۹:۰۹
۱۴۰۵.۰۵.۲۷""",
            receivedAt
        )
        val withdrawal = GenericBankSmsParser.parse(
            "blu-withdrawal", "30001234",
            """بلو
برداشت پول
فرزاد عزیز، 10,000,000 ریال از حساب شما پرید.
موجودی: 1,857,282 ریال
۱۷:۰۲
۱۴۰۵.۰۵.۲۶""",
            receivedAt
        )

        assertNotNull(deposit)
        assertEquals("بانک سامان", deposit?.bankName)
        assertEquals(TransactionType.INCOME, deposit?.type)
        assertEquals(26_000_000L, deposit?.amount)
        assertEquals(26_000_000L, deposit?.balanceAmount)
        assertEquals("1405/05/27", deposit?.dateText)
        assertEquals("09:09", deposit?.timeText)

        assertNotNull(withdrawal)
        assertEquals("بانک سامان", withdrawal?.bankName)
        assertEquals(TransactionType.EXPENSE, withdrawal?.type)
        assertEquals(10_000_000L, withdrawal?.amount)
        assertNull(withdrawal?.balanceAmount)
        assertEquals("1405/05/26", withdrawal?.dateText)
        assertEquals("17:02", withdrawal?.timeText)
    }

    @Test
    fun preservesExactBluRialAmountThatCannotBeRepresentedAsWholeToman() {
        val parsed = GenericBankSmsParser.parse(
            "blu-loan-repayment", "30001234",
            """بلو
فرزاد عزیز 25,533,522 ریال بابت بازپرداخت بدهی وام نقدی بلو، از حساب شما پرید.
موجودی: 2,466,478 ریال
۱۶:۵۸
۱۴۰۵.۰۷.۲۷""",
            com.example.util.PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 27, 17, 0)
        )

        assertNotNull(parsed)
        assertEquals(25_533_522L, parsed?.amount)
        assertEquals(2_466_478L, parsed?.balanceAmount)
    }


    @Test
    fun recognizesResalatSignedDepositAndWithdrawalMessages() {
        val receivedAt = com.example.util.PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 13, 12, 16)
        val deposit = GenericBankSmsParser.parse(
            "resalat-deposit",
            "ResalatBank",
            """10.8805139.1
+80,000,000 
07/13_12:16
مانده: 381,835,350""",
            receivedAt
        )
        val withdrawal = GenericBankSmsParser.parse(
            "resalat-withdrawal",
            "ResalatBank",
            """10.8805139.1
-750,000 
07/13_12:16
مانده: 381,085,350""",
            receivedAt
        )

        assertNotNull(deposit)
        assertEquals("بانک قرض‌الحسنه رسالت", deposit?.bankName)
        assertEquals(TransactionType.INCOME, deposit?.type)
        assertEquals(80_000_000L, deposit?.amount)
        assertEquals(381_835_350L, deposit?.balanceAmount)
        assertEquals("1405/07/13", deposit?.dateText)
        assertEquals("12:16", deposit?.timeText)

        assertNotNull(withdrawal)
        assertEquals("بانک قرض‌الحسنه رسالت", withdrawal?.bankName)
        assertEquals(TransactionType.EXPENSE, withdrawal?.type)
        assertEquals(750_000L, withdrawal?.amount)
        assertEquals(381_085_350L, withdrawal?.balanceAmount)
        assertEquals("1405/07/13", withdrawal?.dateText)
        assertEquals("12:16", withdrawal?.timeText)
    }


}
