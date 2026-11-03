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
        assertEquals(2_600_000L, deposit?.amount)
        assertEquals(2_600_000L, deposit?.balanceAmount)
        assertEquals("1405/05/27", deposit?.dateText)
        assertEquals("09:09", deposit?.timeText)

        assertNotNull(withdrawal)
        assertEquals("بانک سامان", withdrawal?.bankName)
        assertEquals(TransactionType.EXPENSE, withdrawal?.type)
        assertEquals(1_000_000L, withdrawal?.amount)
        assertEquals(185_728L, withdrawal?.balanceAmount)
        assertEquals("1405/05/26", withdrawal?.dateText)
        assertEquals("17:02", withdrawal?.timeText)
    }

    @Test
    fun doesNotRoundBluRialAmountThatCannotBeRepresentedAsWholeToman() {
        val parsed = GenericBankSmsParser.parse(
            "blu-loan-repayment", "30001234",
            """بلو
فرزاد عزیز 25,533,522 ریال بابت بازپرداخت بدهی وام نقدی بلو، از حساب شما پرید.
موجودی: 2,466,478 ریال
۱۶:۵۸
۱۴۰۵.۰۷.۲۷""",
            com.example.util.PersianCalendarHelper.jalaliToEpochMillis(1405, 7, 27, 17, 0)
        )

        // The app stores transaction amounts as whole Tomans. Reject rather than
        // silently truncating 2,553,352.2 Tomans to a different financial amount.
        assertNull(parsed)
    }

}
